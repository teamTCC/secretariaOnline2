import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Row = {
  id: string
  idAluno?: string
  empresa?: string
  cargo?: string
  cargaHorariaSemanal?: number
  inicio?: string
  estado?: string
}
type Envelope = { content?: Row[]; page?: { number: number; totalElements?: number }; _links?: unknown }
type UsuarioPage = { content?: { id: string; email?: string; nome?: string }[] }

export function CoePoolPage() {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<string[]>([])
  const [idSupervisor, setSupervisor] = useState('')
  const [emailLookup, setEmailLookup] = useState('prof.ana@ufpr.br')
  const [pasteId, setPasteId] = useState('')
  const [last, setLast] = useState<unknown>()

  const pool = useQuery({
    queryKey: queryKeys.coePool(page),
    queryFn: () => api<Envelope>(`/commissions/coe/pool?page=${page}&size=20`),
  })
  const stats = useQuery({
    queryKey: queryKeys.coeStats,
    queryFn: () => api('/commissions/coe/stats'),
  })

  function invalidate() {
    void qc.invalidateQueries({ queryKey: ['commissions', 'coe'] })
  }

  const lookup = useMutation({
    mutationFn: () => api<UsuarioPage>(`/usuarios?email=${encodeURIComponent(emailLookup)}&page=0&size=5`),
    onSuccess: (d) => {
      setLast(d)
      const id = d.content?.[0]?.id
      if (id) setSupervisor(id)
    },
    onError: (e) => setLast(e),
  })

  const assign = useMutation({
    mutationFn: (internshipId: string) =>
      api(`/commissions/coe/${internshipId}/assign-supervisor`, {
        method: 'POST',
        body: { idSupervisor },
      }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: (e) => setLast(e),
  })

  const bulk = useMutation({
    mutationFn: () =>
      api('/commissions/coe/bulk-assign', {
        method: 'POST',
        body: {
          internshipIds: selected.length ? selected : pasteId ? [pasteId] : [],
          idSupervisor,
        },
      }),
    onSuccess: (d) => {
      setLast(d)
      setSelected([])
      invalidate()
    },
    onError: (e) => setLast(e),
  })

  const conclude = useMutation({
    mutationFn: (internshipId: string) => api(`/internships/${internshipId}/conclude`, { method: 'POST' }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: (e) => setLast(e),
  })

  const rows = pool.data?.content ?? []
  const problem = [pool.error, stats.error, lookup.error, assign.error, bulk.error, conclude.error]
    .reverse()
    .find((e) => isProblem(e))

  return (
    <Page title="COE pool">
      <p>
        GET /commissions/coe/pool · POST assign-supervisor · POST bulk-assign · GET stats · POST internships/:id/conclude.
        internship.review. Prof sem review → 403. GET /usuarios?email= precisa user.manage_* (admin).
      </p>
      {(pool.isPending || stats.isPending) && <p>carregando</p>}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
      <form
        onSubmit={(e) => {
          e.preventDefault()
          lookup.mutate()
        }}
      >
        <label>
          GET /usuarios?email=
          <input value={emailLookup} onChange={(e) => setEmailLookup(e.target.value)} />
        </label>
        <button type="submit" disabled={lookup.isPending}>
          lookup supervisor
        </button>
      </form>
      <label>
        idSupervisor
        <input value={idSupervisor} onChange={(e) => setSupervisor(e.target.value)} />
      </label>
      <table>
        <thead>
          <tr>
            <th />
            <th>id</th>
            <th>empresa</th>
            <th>cargo</th>
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
              <td>
                <Link to={`/estagios/${r.id}`}>{r.id}</Link>
              </td>
              <td>{r.empresa}</td>
              <td>{r.cargo}</td>
              <td>
                <button type="button" disabled={!idSupervisor || assign.isPending} onClick={() => assign.mutate(r.id)}>
                  assign-supervisor
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
          if (pasteId) assign.mutate(pasteId)
        }}
      >
        <label>
          internshipId colado
          <input value={pasteId} onChange={(e) => setPasteId(e.target.value)} />
        </label>
        <button type="submit" disabled={!pasteId || !idSupervisor || assign.isPending}>
          POST assign-supervisor
        </button>
        <button type="button" disabled={!idSupervisor || bulk.isPending} onClick={() => bulk.mutate()}>
          POST bulk-assign
        </button>
        <button type="button" disabled={!pasteId || conclude.isPending} onClick={() => conclude.mutate(pasteId)}>
          POST /internships/:id/conclude
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
