import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Row = { id: string; nome?: string; email?: string; grr?: string; ativo?: boolean; roles?: string[] }
type Envelope = { content?: Row[]; page?: { number: number; totalPages?: number; totalElements?: number } }

export function UsuariosPage() {
  const qc = useQueryClient()
  const { id: routeId } = useParams()
  const [email, setEmail] = useState('')
  const [nome, setNome] = useState('')
  const [page, setPage] = useState(0)
  const [detailId, setDetailId] = useState(routeId ?? '')
  const [last, setLast] = useState<unknown>()

  const list = useQuery({
    queryKey: queryKeys.usuarios({ email, nome, page }),
    queryFn: () => {
      const qs = new URLSearchParams()
      qs.set('page', String(page))
      qs.set('size', '20')
      if (email) qs.set('email', email)
      if (nome) qs.set('nome', nome)
      return api<Envelope>(`/usuarios?${qs}`)
    },
  })

  const detail = useQuery({
    queryKey: queryKeys.usuario(detailId),
    queryFn: () => api(`/usuarios/${detailId}`),
    enabled: Boolean(detailId),
  })

  const status = useMutation({
    mutationFn: (ativo: boolean) => api(`/usuarios/${detailId}/status`, { method: 'PATCH', body: { ativo } }),
    onSuccess: (d) => {
      setLast(d)
      void qc.invalidateQueries({ queryKey: ['usuarios'] })
    },
    onError: setLast,
  })

  const reset = useMutation({
    mutationFn: () => api(`/usuarios/${detailId}/reset-password`, { method: 'POST' }),
    onSuccess: setLast,
    onError: setLast,
  })

  const problem = [list.error, detail.error, status.error, reset.error].reverse().find((e) => isProblem(e))

  return (
    <Page title="usuarios">
      <p>
        GET /usuarios · GET /usuarios/:id · PATCH /usuarios/:id/status (as-built, não PATCH /usuarios/:id) · POST
        reset-password. Sem user.manage_* → 403.
      </p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          setPage(0)
          void list.refetch()
        }}
      >
        <label>
          email
          <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="ana.aluno@ufpr.br" />
        </label>
        <label>
          nome
          <input value={nome} onChange={(e) => setNome(e.target.value)} />
        </label>
        <button type="submit">GET /usuarios</button>
      </form>
      {(list.isPending || detail.isFetching || status.isPending || reset.isPending) && <p>carregando</p>}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
      <table>
        <thead>
          <tr>
            <th>id</th>
            <th>nome</th>
            <th>email</th>
            <th>ativo</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {(list.data?.content ?? []).map((r) => (
            <tr key={r.id}>
              <td>
                <Link to={`/usuarios/${r.id}`}>{r.id}</Link>
              </td>
              <td>{r.nome}</td>
              <td>{r.email}</td>
              <td>{String(r.ativo)}</td>
              <td>
                <button type="button" onClick={() => setDetailId(r.id)}>
                  GET detalhe
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="row">
        <button type="button" disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>
          prev
        </button>
        <span>page {page}</span>
        <button type="button" onClick={() => setPage((p) => p + 1)}>
          next
        </button>
      </div>
      <label>
        id colado
        <input value={detailId} onChange={(e) => setDetailId(e.target.value)} />
      </label>
      <div className="row">
        <button type="button" disabled={!detailId || status.isPending} onClick={() => status.mutate(false)}>
          PATCH status ativo=false
        </button>
        <button type="button" disabled={!detailId || status.isPending} onClick={() => status.mutate(true)}>
          PATCH status ativo=true
        </button>
        <button type="button" disabled={!detailId || reset.isPending} onClick={() => reset.mutate()}>
          POST reset-password
        </button>
      </div>
      <h2>detalhe</h2>
      <JsonPanel data={detail.error ?? detail.data} />
      <h2>última mutação</h2>
      <JsonPanel data={last} />
      <h2>lista</h2>
      <JsonPanel data={list.error ?? list.data} />
    </Page>
  )
}
