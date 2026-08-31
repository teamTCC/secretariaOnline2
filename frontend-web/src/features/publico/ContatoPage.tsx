import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { normalizeLinks } from '../../shared/api/hateoas'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { HateoasBar } from '../../shared/ui/HateoasBar'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type ContatoInfo = {
  nome: string
  endereco: string
  telefone: string
  email: string
  horario: string
  _links?: unknown
}

type ContatoBody = { nome: string; email: string; assunto: string; mensagem: string }

export function ContatoPage() {
  const [form, setForm] = useState<ContatoBody>({
    nome: 'Ana Silva',
    email: 'ana.aluno@ufpr.br',
    assunto: 'Horário de atendimento',
    mensagem: 'A secretaria atende no sábado?',
  })

  const q = useQuery({
    queryKey: queryKeys.contato,
    queryFn: () => api<ContatoInfo>('/publico/contato'),
  })

  const withCsrf = useMutation({
    mutationFn: (body: ContatoBody) =>
      api('/publico/contato', { method: 'POST', body, skipRefresh: true }),
  })

  const noCsrf = useMutation({
    mutationFn: (body: ContatoBody) =>
      api('/publico/contato', { method: 'POST', body, skipRefresh: true, skipCsrf: true }),
  })

  const last = noCsrf.error ?? noCsrf.data ?? withCsrf.error ?? withCsrf.data ?? q.data
  const problem = [noCsrf.error, withCsrf.error, q.error].find(isProblem)

  return (
    <Page title="contato">
      <p>
        <Link to="/login">login</Link>
      </p>
      {q.isPending && <p>carregando</p>}
      <ProblemBanner problem={problem} />
      <HateoasBar
        links={normalizeLinks(q.data?._links)}
        onAction={() => {
          noCsrf.reset()
          withCsrf.mutate(form)
        }}
      />
      <JsonPanel data={q.data} />
      <form
        onSubmit={(e) => {
          e.preventDefault()
          noCsrf.reset()
          withCsrf.mutate(form)
        }}
      >
        <label>
          nome
          <input
            name="nome"
            value={form.nome}
            onChange={(e) => setForm({ ...form, nome: e.target.value })}
          />
        </label>
        <label>
          email
          <input
            name="email"
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
          />
        </label>
        <label>
          assunto
          <input
            name="assunto"
            value={form.assunto}
            onChange={(e) => setForm({ ...form, assunto: e.target.value })}
          />
        </label>
        <label>
          mensagem
          <textarea
            name="mensagem"
            value={form.mensagem}
            onChange={(e) => setForm({ ...form, mensagem: e.target.value })}
            rows={4}
          />
        </label>
        <div className="row">
          <button type="submit" disabled={withCsrf.isPending}>
            enviar com CSRF
          </button>
          <button
            type="button"
            disabled={noCsrf.isPending}
            onClick={() => {
              withCsrf.reset()
              noCsrf.mutate(form)
            }}
          >
            enviar sem CSRF
          </button>
        </div>
      </form>
      {(withCsrf.isPending || noCsrf.isPending) && <p>carregando</p>}
      <JsonPanel data={last === q.data ? undefined : last} />
    </Page>
  )
}
