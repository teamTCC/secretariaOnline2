import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Row = { id: string; assunto?: string; estado?: string; descricao?: string }
type Envelope = { content?: Row[]; page?: { number: number } }

export function StaffTicketsPage() {
  const qc = useQueryClient()
  const [estado, setEstado] = useState('')
  const [resposta, setResposta] = useState('Recebemos o chamado. Verifique o formato PDF nativo e tente de novo.')
  const [ticketId, setTicketId] = useState('')
  const [last, setLast] = useState<unknown>()

  const list = useQuery({
    queryKey: queryKeys.ticketsStaff(estado),
    queryFn: () => api<Envelope>(`/support/tickets${estado ? `?estado=${encodeURIComponent(estado)}` : ''}`),
  })

  function invalidate() {
    void qc.invalidateQueries({ queryKey: ['support', 'tickets'] })
  }

  const respond = useMutation({
    mutationFn: () =>
      api(`/support/tickets/${ticketId}/respond`, { method: 'PATCH', body: { resposta } }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: setLast,
  })

  const close = useMutation({
    mutationFn: () => api(`/support/tickets/${ticketId}/close`, { method: 'PATCH' }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: setLast,
  })

  const problem = [list.error, respond.error, close.error].reverse().find((e) => isProblem(e))

  return (
    <Page title="tickets staff">
      <p>
        GET /support/tickets (fila) · PATCH :id/respond {`{ resposta }`} · PATCH :id/close. Aluno neste path → 403.
        Abrir ticket continua em /faq.
      </p>
      <label>
        estado
        <input value={estado} onChange={(e) => setEstado(e.target.value)} placeholder="ABERTO" />
      </label>
      {list.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
      <table>
        <thead>
          <tr>
            <th>id</th>
            <th>assunto</th>
            <th>estado</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {(list.data?.content ?? []).map((r) => (
            <tr key={r.id}>
              <td>{r.id}</td>
              <td>{r.assunto}</td>
              <td>{r.estado}</td>
              <td>
                <button type="button" onClick={() => setTicketId(r.id)}>
                  usar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <label>
        ticketId
        <input value={ticketId} onChange={(e) => setTicketId(e.target.value)} />
      </label>
      <label>
        resposta
        <textarea value={resposta} onChange={(e) => setResposta(e.target.value)} />
      </label>
      <div className="row">
        <button type="button" disabled={!ticketId || respond.isPending} onClick={() => respond.mutate()}>
          PATCH respond
        </button>
        <button type="button" disabled={!ticketId || close.isPending} onClick={() => close.mutate()}>
          PATCH close
        </button>
      </div>
      <h2>última mutação</h2>
      <JsonPanel data={last} />
      <h2>fila</h2>
      <JsonPanel data={list.error ?? list.data} />
    </Page>
  )
}
