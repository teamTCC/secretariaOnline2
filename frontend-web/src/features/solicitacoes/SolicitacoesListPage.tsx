import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { normalizeLinks } from '../../shared/api/hateoas'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { HateoasBar } from '../../shared/ui/HateoasBar'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Row = {
  id: string
  protocolo?: string
  tipoCode?: string
  estado?: string
  prazoEm?: string
  _links?: unknown
}

type Envelope = {
  content?: Row[]
  page?: { number: number; size: number; totalElements: number; totalPages: number }
  _links?: { next?: string; prev?: string; self?: string; first?: string; last?: string }
}

type Curso = { id: string; nome?: string; sigla?: string }
type TypeRow = { code: string }

export function SolicitacoesListPage() {
  const [params, setParams] = useSearchParams()
  const estado = params.get('estado') ?? ''
  const typeCode = params.get('typeCode') ?? ''
  const idCurso = params.get('idCurso') ?? ''
  const page = Number(params.get('page') ?? '0') || 0
  const [selected, setSelected] = useState<string[]>([])
  const [bulkAction, setBulkAction] = useState('DEFER')
  const [parecer, setParecer] = useState('')
  const [bulkResult, setBulkResult] = useState<unknown>()
  const [bulkErr, setBulkErr] = useState<unknown>()

  const filters = useMemo(
    () => ({ estado: estado || undefined, typeCode: typeCode || undefined, idCurso: idCurso || undefined, page }),
    [estado, typeCode, idCurso, page],
  )

  const list = useQuery({
    queryKey: queryKeys.requests(filters),
    queryFn: () => {
      const qs = new URLSearchParams()
      qs.set('page', String(page))
      qs.set('size', '20')
      if (estado) qs.set('estado', estado)
      if (typeCode) qs.set('typeCode', typeCode)
      if (idCurso) qs.set('idCurso', idCurso)
      return api<Envelope>(`/requests?${qs}`)
    },
  })

  const types = useQuery({
    queryKey: queryKeys.requestTypes,
    queryFn: () => api<TypeRow[]>('/requests/types'),
  })

  const cursos = useQuery({
    queryKey: queryKeys.cursos,
    queryFn: () => api<Curso[]>('/academico/cursos'),
  })

  const rows = list.data?.content ?? []
  const canBulk = rows.some((r) => Boolean(normalizeLinks(r._links).bulk_deliberate))

  function setFilter(k: string, v: string) {
    const next = new URLSearchParams(params)
    if (v) next.set(k, v)
    else next.delete(k)
    next.set('page', '0')
    setParams(next)
  }

  function goPage(n: number) {
    const next = new URLSearchParams(params)
    next.set('page', String(n))
    setParams(next)
  }

  return (
    <Page title="solicitacoes">
      <p>
        <Link to="/solicitacoes/nova">nova</Link>
      </p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
        }}
      >
        <label>
          estado
          <input value={estado} onChange={(e) => setFilter('estado', e.target.value)} />
        </label>
        <label>
          typeCode
          <select value={typeCode} onChange={(e) => setFilter('typeCode', e.target.value)}>
            <option value="">—</option>
            {(types.data ?? []).map((t) => (
              <option key={t.code} value={t.code}>
                {t.code}
              </option>
            ))}
          </select>
        </label>
        <label>
          idCurso
          <select value={idCurso} onChange={(e) => setFilter('idCurso', e.target.value)}>
            <option value="">—</option>
            {(cursos.data ?? []).map((c) => (
              <option key={c.id} value={c.id}>
                {c.sigla} — {c.nome}
              </option>
            ))}
          </select>
        </label>
      </form>
      {list.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(list.error) ? list.error : isProblem(bulkErr) ? bulkErr : null} />
      <HateoasBar
        links={normalizeLinks(list.data?._links)}
        onAction={(rel, href) => {
          try {
            const u = new URL(href, window.location.origin)
            const p = u.searchParams.get('page')
            if (p != null && ['next', 'prev', 'first', 'last', 'self'].includes(rel)) {
              goPage(Number(p))
            }
          } catch {
            /* ignore */
          }
        }}
      />
      <table>
        <thead>
          <tr>
            {canBulk ? <th /> : null}
            <th>protocolo</th>
            <th>tipoCode</th>
            <th>estado</th>
            <th>prazoEm</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => {
            const links = normalizeLinks(r._links)
            const bulk = Boolean(links.bulk_deliberate)
            return (
              <tr key={r.id}>
                {canBulk ? (
                  <td>
                    {bulk ? (
                      <input
                        type="checkbox"
                        checked={selected.includes(r.id)}
                        onChange={(e) =>
                          setSelected(
                            e.target.checked ? [...selected, r.id] : selected.filter((x) => x !== r.id),
                          )
                        }
                      />
                    ) : null}
                  </td>
                ) : null}
                <td>{r.protocolo}</td>
                <td>{r.tipoCode}</td>
                <td>{r.estado}</td>
                <td>{r.prazoEm}</td>
                <td>
                  <Link to={`/solicitacoes/${r.id}`}>detalhe</Link>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
      <div className="row">
        <button type="button" disabled={page <= 0} onClick={() => goPage(page - 1)}>
          prev
        </button>
        <span>
          page {list.data?.page?.number ?? page} / {list.data?.page?.totalPages ?? '?'}
        </span>
        <button
          type="button"
          disabled={!list.data?._links?.next}
          onClick={() => goPage(page + 1)}
        >
          next
        </button>
      </div>
      {canBulk ? (
        <form
          onSubmit={(e) => {
            e.preventDefault()
            setBulkErr(undefined)
            void api('/requests/bulk-deliberate', {
              method: 'PATCH',
              body: { ids: selected, action: bulkAction, parecer: parecer || null },
            })
              .then((d) => {
                setBulkResult(d)
                void list.refetch()
              })
              .catch((err: unknown) => setBulkErr(err))
          }}
        >
          <p>PATCH /requests/bulk-deliberate (só se _links.bulk_deliberate)</p>
          <label>
            action
            <input value={bulkAction} onChange={(e) => setBulkAction(e.target.value)} />
          </label>
          <label>
            parecer
            <input value={parecer} onChange={(e) => setParecer(e.target.value)} />
          </label>
          <button type="submit" disabled={selected.length === 0}>
            PATCH bulk ({selected.length})
          </button>
        </form>
      ) : null}
      <JsonPanel data={bulkResult} />
      <JsonPanel data={list.error ?? list.data} />
    </Page>
  )
}
