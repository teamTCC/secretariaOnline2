import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Row = {
  id: string
  idAluno?: string
  titulo?: string
  categoria?: string
  cargaHoraria?: number
  dataRealizacao?: string
  _links?: unknown
}
type Envelope = { content?: Row[]; page?: { number: number; totalElements?: number }; _links?: unknown }

export function CaafPoolPage() {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [pasteId, setPasteId] = useState('')
  const [selected, setSelected] = useState<string[]>([])
  const [acao, setAcao] = useState('APROVAR')
  const [parecer, setParecer] = useState('Lote CAAF — comprovantes conferidos (fatia 6).')
  const [last, setLast] = useState<unknown>()

  const pool = useQuery({
    queryKey: queryKeys.caafPool(page),
    queryFn: () => api<Envelope>(`/commissions/caaf/pool?page=${page}&size=20`),
  })
  const stats = useQuery({
    queryKey: queryKeys.caafStats,
    queryFn: () => api('/commissions/caaf/stats'),
  })

  function invalidate() {
    void qc.invalidateQueries({ queryKey: ['commissions', 'caaf'] })
  }

  const claim = useMutation({
    mutationFn: (activityId: string) => api(`/commissions/caaf/${activityId}/claim`, { method: 'POST' }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: (e) => setLast(e),
  })

  const batch = useMutation({
    mutationFn: () =>
      api('/commissions/caaf/batch-review', {
        method: 'POST',
        body: { ids: selected.length ? selected : pasteId ? [pasteId] : [], acao, parecer },
      }),
    onSuccess: (d) => {
      setLast(d)
      setSelected([])
      invalidate()
    },
    onError: (e) => setLast(e),
  })

  const rows = pool.data?.content ?? []
  const problem = [pool.error, stats.error, claim.error, batch.error].reverse().find((e) => isProblem(e))

  return (
    <Page title="CAAF pool">
      <p>
        GET /commissions/caaf/pool · POST claim · POST batch-review · GET stats. formative.review. Path errado
        /commissions/caaf/dashboard → 404.
      </p>
      {(pool.isPending || stats.isPending || claim.isPending || batch.isPending) && <p>carregando</p>}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
      <table>
        <thead>
          <tr>
            <th />
            <th>id</th>
            <th>titulo</th>
            <th>categoria</th>
            <th>ch</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id}>
              <td>
                <input
                  type="checkbox"
                  checked={selected.includes(r.id)}
                  onChange={(e) =>
                    setSelected((cur) => (e.target.checked ? [...cur, r.id] : cur.filter((x) => x !== r.id)))
                  }
                />
              </td>
              <td>{r.id}</td>
              <td>{r.titulo}</td>
              <td>{r.categoria}</td>
              <td>{r.cargaHoraria}</td>
              <td>
                <button type="button" disabled={claim.isPending} onClick={() => claim.mutate(r.id)}>
                  POST claim
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
      <form
        onSubmit={(e) => {
          e.preventDefault()
          if (pasteId) claim.mutate(pasteId)
        }}
      >
        <label>
          claim id colado
          <input value={pasteId} onChange={(e) => setPasteId(e.target.value)} />
        </label>
        <button type="submit" disabled={!pasteId || claim.isPending}>
          POST /commissions/caaf/:id/claim
        </button>
      </form>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          batch.mutate()
        }}
      >
        <label>
          acao
          <select value={acao} onChange={(e) => setAcao(e.target.value)}>
            <option value="APROVAR">APROVAR</option>
            <option value="REJEITAR">REJEITAR</option>
          </select>
        </label>
        <label>
          parecer
          <textarea value={parecer} onChange={(e) => setParecer(e.target.value)} />
        </label>
        <p>ids lote: {selected.join(', ') || '(nenhum checkbox — usa id colado se houver)'}</p>
        <button type="submit" disabled={batch.isPending}>
          POST /commissions/caaf/batch-review
        </button>
      </form>
      <h2>última mutação</h2>
      <JsonPanel data={last} />
      <h2>stats</h2>
      <JsonPanel data={stats.error ?? stats.data} />
      <h2>pool</h2>
      <JsonPanel data={pool.error ?? pool.data} />
    </Page>
  )
}
