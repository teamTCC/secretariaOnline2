import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
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
  assunto?: string
  tipo?: string
  estado?: string
  descricao?: string
  acknowledgedAt?: string | null
  _links?: unknown
}

type Envelope = { content?: Row[]; page?: { number: number; totalPages: number }; _links?: unknown }

export function AtendimentosPage() {
  const qc = useQueryClient()
  const [assunto, setAssunto] = useState('Revisão de matrícula')
  const [descricao, setDescricao] = useState('Quero conferir disciplinas do período 2026/2.')
  const [tipo, setTipo] = useState('AGENDAMENTO')
  const [status, setStatus] = useState('')
  const [alias, setAlias] = useState(false)
  const [created, setCreated] = useState<unknown>()
  const [ack, setAck] = useState<unknown>()
  const [idAluno, setIdAluno] = useState('1bafbb82-a473-4170-8433-c13cebc22562')
  const [balcaoAssunto, setBalcaoAssunto] = useState('Revisão de matrícula')
  const [balcaoDesc, setBalcaoDesc] = useState('Atendimento de balcão')
  const [balcaoTipo, setBalcaoTipo] = useState('PRESENCIAL')
  const [balcao, setBalcao] = useState<unknown>()

  const path = alias
    ? `/service-records?aluno=me${status ? `&status=${encodeURIComponent(status)}` : ''}&page=0&size=20`
    : `/me/service-records${status ? `?status=${encodeURIComponent(status)}` : '?page=0&size=20'}`

  const list = useQuery({
    queryKey: [...queryKeys.atendimentos(status), alias],
    queryFn: () => api<Envelope>(path),
  })

  const schedule = useMutation({
    mutationFn: () =>
      api('/me/service-records', {
        method: 'POST',
        body: { assunto, descricao, tipo },
      }),
    onSuccess: (d) => {
      setCreated(d)
      void qc.invalidateQueries({ queryKey: ['service-records'] })
    },
  })

  const acknowledge = useMutation({
    mutationFn: (href: string) => api(href, { method: 'POST' }),
    onSuccess: (d) => {
      setAck(d)
      void qc.invalidateQueries({ queryKey: ['service-records'] })
    },
  })

  const balcaoCreate = useMutation({
    mutationFn: () =>
      api('/service-records', {
        method: 'POST',
        body: { idAluno, assunto: balcaoAssunto, tipo: balcaoTipo, descricao: balcaoDesc },
      }),
    onSuccess: (d) => {
      setBalcao(d)
      void qc.invalidateQueries({ queryKey: ['service-records'] })
    },
    onError: setBalcao,
  })

  return (
    <Page title="atendimentos">
      <p>
        POST /me/service-records · GET /me/service-records · alias GET /service-records?aluno=me · _links.acknowledge só
        PENDENTE_CIENCIA. Balcão: POST /service-records {`{ idAluno, assunto, tipo: PRESENCIAL }`} → PENDENTE_CIENCIA.
      </p>
      <h2>balcão (secretaria)</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          balcaoCreate.mutate()
        }}
      >
        <label>
          idAluno
          <input value={idAluno} onChange={(e) => setIdAluno(e.target.value)} />
        </label>
        <label>
          assunto
          <input value={balcaoAssunto} onChange={(e) => setBalcaoAssunto(e.target.value)} />
        </label>
        <label>
          descricao
          <textarea value={balcaoDesc} onChange={(e) => setBalcaoDesc(e.target.value)} />
        </label>
        <label>
          tipo
          <input value={balcaoTipo} onChange={(e) => setBalcaoTipo(e.target.value)} />
        </label>
        <button type="submit" disabled={balcaoCreate.isPending}>
          POST /service-records
        </button>
      </form>
      <JsonPanel data={balcao} />
      <form
        onSubmit={(e) => {
          e.preventDefault()
          schedule.mutate()
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
        <label>
          tipo
          <input value={tipo} onChange={(e) => setTipo(e.target.value)} />
        </label>
        <button type="submit" disabled={schedule.isPending}>
          POST agendar
        </button>
      </form>
      <div className="row">
        <label>
          status
          <input
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            placeholder="PENDENTE_CIENCIA"
          />
        </label>
        <label>
          <input type="checkbox" checked={alias} onChange={(e) => setAlias(e.target.checked)} />
          alias GET /service-records?aluno=me
        </label>
      </div>
      {(list.isPending || schedule.isPending || acknowledge.isPending || balcaoCreate.isPending) && <p>carregando</p>}
      <ProblemBanner
        problem={
          isProblem(schedule.error)
            ? schedule.error
            : isProblem(acknowledge.error)
              ? acknowledge.error
              : isProblem(balcaoCreate.error)
                ? balcaoCreate.error
                : isProblem(list.error)
                  ? list.error
                  : null
        }
      />
      {(list.data?.content ?? []).map((r) => {
        const links = normalizeLinks(r._links)
        return (
          <fieldset key={r.id}>
            <legend>
              {r.id} {r.estado} {r.assunto}
            </legend>
            <HateoasBar
              links={links}
              onAction={(rel, href) => {
                if (rel === 'acknowledge') acknowledge.mutate(href)
              }}
            />
          </fieldset>
        )
      })}
      <h2>created</h2>
      <JsonPanel data={created} />
      <h2>acknowledge</h2>
      <JsonPanel data={ack} />
      <h2>lista</h2>
      <JsonPanel data={list.error ?? list.data} />
    </Page>
  )
}
