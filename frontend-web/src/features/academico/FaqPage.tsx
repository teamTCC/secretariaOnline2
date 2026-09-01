import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Faq = { id: string; categoria?: string; pergunta?: string; resposta?: string; ordem?: number }

export function FaqPage() {
  const qc = useQueryClient()
  const [categoria, setCategoria] = useState('')
  const [assunto, setAssunto] = useState('Erro ao submeter atividade formativa')
  const [descricao, setDescricao] = useState('Ao tentar enviar o comprovante do minicurso, recebo erro 500.')
  const [created, setCreated] = useState<unknown>()

  const faq = useQuery({
    queryKey: queryKeys.faq(categoria),
    queryFn: () => api<Faq[]>(`/faq${categoria ? `?categoria=${encodeURIComponent(categoria)}` : ''}`),
  })
  const mine = useQuery({
    queryKey: queryKeys.ticketsMine,
    queryFn: () => api('/support/tickets/mine'),
  })

  const open = useMutation({
    mutationFn: () =>
      api('/support/tickets', {
        method: 'POST',
        body: { assunto, descricao },
      }),
    onSuccess: (d) => {
      setCreated(d)
      void qc.invalidateQueries({ queryKey: ['support', 'tickets'] })
    },
  })

  return (
    <Page title="faq / tickets">
      <p>GET /faq (não /support/faq) · POST /support/tickets {`{ assunto, descricao }`} · GET /support/tickets/mine</p>
      <label>
        categoria (opcional)
        <input value={categoria} onChange={(e) => setCategoria(e.target.value)} />
      </label>
      {faq.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(faq.error) ? faq.error : isProblem(open.error) ? open.error : isProblem(mine.error) ? mine.error : null} />
      <ul>
        {(faq.data ?? []).map((f) => (
          <li key={f.id}>
            <strong>{f.pergunta}</strong> [{f.categoria}] — {f.resposta}
          </li>
        ))}
      </ul>
      <h2>abrir ticket (aluno)</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          open.mutate()
        }}
      >
        <label>
          assunto
          <input value={assunto} onChange={(e) => setAssunto(e.target.value)} />
        </label>
        <label>
          descricao
          <textarea value={descricao} onChange={(e) => setDescricao(e.target.value)} />
        </label>
        <button type="submit" disabled={open.isPending}>
          POST /support/tickets
        </button>
      </form>
      {(mine.isPending || open.isPending) && <p>carregando</p>}
      <h2>created</h2>
      <JsonPanel data={created} />
      <h2>mine</h2>
      <JsonPanel data={mine.error ?? mine.data} />
      <h2>faq</h2>
      <JsonPanel data={faq.error ?? faq.data} />
    </Page>
  )
}
