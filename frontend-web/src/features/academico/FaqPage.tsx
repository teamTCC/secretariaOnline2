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
  const [faqPergunta, setFaqPergunta] = useState('Como solicitar aproveitamento de disciplina?')
  const [faqResposta, setFaqResposta] = useState('Acesse Solicitações > Nova > Aproveitamento.')
  const [faqCategoria, setFaqCategoria] = useState('SOLICITACOES')
  const [faqId, setFaqId] = useState('')
  const [adminLast, setAdminLast] = useState<unknown>()

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

  const createFaq = useMutation({
    mutationFn: () =>
      api('/faq', {
        method: 'POST',
        body: { pergunta: faqPergunta, resposta: faqResposta, categoria: faqCategoria, ordem: 1 },
      }),
    onSuccess: (d) => {
      setAdminLast(d)
      const id = (d as { id?: string }).id
      if (id) setFaqId(id)
      void qc.invalidateQueries({ queryKey: ['faq'] })
    },
    onError: setAdminLast,
  })

  const patchFaq = useMutation({
    mutationFn: () => api(`/faq/${faqId}`, { method: 'PATCH', body: { resposta: faqResposta, ordem: 2 } }),
    onSuccess: (d) => {
      setAdminLast(d)
      void qc.invalidateQueries({ queryKey: ['faq'] })
    },
    onError: setAdminLast,
  })

  const deleteFaq = useMutation({
    mutationFn: () => api(`/faq/${faqId}`, { method: 'DELETE' }),
    onSuccess: (d) => {
      setAdminLast(d ?? { deleted: faqId })
      void qc.invalidateQueries({ queryKey: ['faq'] })
    },
    onError: setAdminLast,
  })

  return (
    <Page title="faq / tickets">
      <p>GET /faq (não /support/faq) · POST /support/tickets {`{ assunto, descricao }`} · GET /support/tickets/mine</p>
      <label>
        categoria (opcional)
        <input value={categoria} onChange={(e) => setCategoria(e.target.value)} />
      </label>
      {faq.isPending && <p>carregando</p>}
      <ProblemBanner
        problem={
          isProblem(faq.error)
            ? faq.error
            : isProblem(open.error)
              ? open.error
              : isProblem(mine.error)
                ? mine.error
                : isProblem(createFaq.error)
                  ? createFaq.error
                  : isProblem(patchFaq.error)
                    ? patchFaq.error
                    : isProblem(deleteFaq.error)
                      ? deleteFaq.error
                      : null
        }
      />
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
      <h2>FAQ admin (system.admin)</h2>
      <label>
        pergunta
        <input value={faqPergunta} onChange={(e) => setFaqPergunta(e.target.value)} />
      </label>
      <label>
        resposta
        <textarea value={faqResposta} onChange={(e) => setFaqResposta(e.target.value)} />
      </label>
      <label>
        categoria
        <input value={faqCategoria} onChange={(e) => setFaqCategoria(e.target.value)} />
      </label>
      <label>
        faqId
        <input value={faqId} onChange={(e) => setFaqId(e.target.value)} />
      </label>
      <div className="row">
        <button type="button" disabled={createFaq.isPending} onClick={() => createFaq.mutate()}>
          POST /faq
        </button>
        <button type="button" disabled={!faqId || patchFaq.isPending} onClick={() => patchFaq.mutate()}>
          PATCH /faq/:id
        </button>
        <button type="button" disabled={!faqId || deleteFaq.isPending} onClick={() => deleteFaq.mutate()}>
          DELETE /faq/:id
        </button>
      </div>
      <JsonPanel data={adminLast} />
      <h2>created ticket</h2>
      <JsonPanel data={created} />
      <h2>mine</h2>
      <JsonPanel data={mine.error ?? mine.data} />
      <h2>faq</h2>
      <JsonPanel data={faq.error ?? faq.data} />
    </Page>
  )
}
