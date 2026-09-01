import { useMutation, useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Row = { id: string; status?: string; eventType?: string; lastError?: string }
type Envelope = { content?: Row[]; page?: { number: number; totalElements?: number } }

export function OutboxAdminPage() {
  const [params, setParams] = useSearchParams()
  const status = params.get('status') ?? 'PENDING'
  const page = Number(params.get('page') ?? '0') || 0
  const eventId = params.get('id') ?? ''

  const list = useQuery({
    queryKey: queryKeys.outbox(status, page),
    queryFn: () => api<Envelope>(`/admin/outbox?status=${encodeURIComponent(status)}&page=${page}&size=20`),
  })

  const detail = useQuery({
    queryKey: ['admin', 'outbox', 'detail', eventId],
    queryFn: () => api(`/admin/outbox/${eventId}`),
    enabled: Boolean(eventId),
  })

  const health = useQuery({
    queryKey: ['actuator', 'health'],
    queryFn: () => api('/actuator/health'),
  })

  const retry = useMutation({
    mutationFn: (id: string) => api(`/admin/outbox/${id}/retry`, { method: 'POST' }),
  })

  function setStatus(s: string) {
    const next = new URLSearchParams(params)
    next.set('status', s)
    next.set('page', '0')
    setParams(next)
  }

  const problem = [list.error, detail.error, retry.error].reverse().find((e) => isProblem(e))

  return (
    <Page title="admin outbox">
      <p>
        GET /admin/outbox default PENDING. Sem ?status=PROCESSED a lista parece vazia. GET /actuator/health (T-F7).
      </p>
      <div className="row">
        {['PENDING', 'PROCESSED', 'DEAD'].map((s) => (
          <button type="button" key={s} onClick={() => setStatus(s)}>
            {s}
          </button>
        ))}
      </div>
      {list.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
      <table>
        <thead>
          <tr>
            <th>id</th>
            <th>status</th>
            <th>type</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {(list.data?.content ?? []).map((r) => (
            <tr key={r.id}>
              <td>{r.id}</td>
              <td>{r.status}</td>
              <td>{r.eventType}</td>
              <td>
                <button
                  type="button"
                  onClick={() => {
                    const next = new URLSearchParams(params)
                    next.set('id', r.id)
                    setParams(next)
                  }}
                >
                  detalhe
                </button>
                {status === 'DEAD' ? (
                  <button type="button" onClick={() => retry.mutate(r.id)}>
                    retry
                  </button>
                ) : null}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="row">
        <button
          type="button"
          disabled={page <= 0}
          onClick={() => {
            const next = new URLSearchParams(params)
            next.set('page', String(page - 1))
            setParams(next)
          }}
        >
          prev
        </button>
        <span>page {page}</span>
        <button
          type="button"
          onClick={() => {
            const next = new URLSearchParams(params)
            next.set('page', String(page + 1))
            setParams(next)
          }}
        >
          next
        </button>
      </div>
      <h2>detalhe</h2>
      <JsonPanel data={detail.error ?? detail.data} />
      <h2>lista status={status}</h2>
      <JsonPanel data={list.error ?? list.data} />
      <h2>actuator/health</h2>
      <JsonPanel data={health.error ?? health.data} />
    </Page>
  )
}
