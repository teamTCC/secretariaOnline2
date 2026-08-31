import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { actionFromRel, normalizeLinks, uiPathFromHref } from '../../shared/api/hateoas'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { AttachmentUpload } from '../../shared/ui/AttachmentUpload'
import { HateoasBar } from '../../shared/ui/HateoasBar'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'
import { DynamicForm, requiredAttachments, type JsonSchema } from './DynamicForm'

type Detail = {
  id: string
  protocolo?: string
  tipoCode?: string
  estado?: string
  dados?: Record<string, unknown>
  parecer?: string | null
  formSchema?: JsonSchema
  prazoEm?: string
  _links?: unknown
}

type Protocol = { protocolo?: string; _links?: unknown }
type EventRow = { tipo?: string; estadoAnterior?: string; estadoNovo?: string; createdAt?: string }
type Att = { id: string; categoria?: string; nomeOriginal?: string; storageKey?: string }

export function SolicitacaoDetailPage() {
  const { id = '' } = useParams()
  const nav = useNavigate()
  const qc = useQueryClient()
  const [parecer, setParecer] = useState('')
  const [dados, setDados] = useState<Record<string, unknown> | null>(null)
  const [last, setLast] = useState<unknown>()
  const [forcedAction, setForcedAction] = useState('DEFERIR')

  const detail = useQuery({
    queryKey: queryKeys.request(id),
    queryFn: () => api<Detail>(`/requests/${id}`),
    enabled: Boolean(id),
  })
  const events = useQuery({
    queryKey: queryKeys.requestEvents(id),
    queryFn: () => api<EventRow[]>(`/requests/${id}/events`),
    enabled: Boolean(id),
  })
  const atts = useQuery({
    queryKey: queryKeys.requestAttachments(id),
    queryFn: () => api<Att[]>(`/requests/${id}/attachments`),
    enabled: Boolean(id),
  })
  const protocol = useQuery({
    queryKey: queryKeys.requestProtocol(id),
    queryFn: () => api<Protocol>(`/requests/${id}/protocol`),
    enabled: Boolean(id),
  })

  const formValues = dados ?? detail.data?.dados ?? {}
  const schema = detail.data?.formSchema
  const links = normalizeLinks(detail.data?._links)
  const cats = schema ? requiredAttachments(schema) : []

  function invalidate() {
    setDados(null)
    void qc.invalidateQueries({ queryKey: queryKeys.request(id) })
    void qc.invalidateQueries({ queryKey: queryKeys.requestEvents(id) })
    void qc.invalidateQueries({ queryKey: queryKeys.requestAttachments(id) })
    void qc.invalidateQueries({ queryKey: queryKeys.requestProtocol(id) })
    void qc.invalidateQueries({ queryKey: queryKeys.requests({}) })
  }

  const transition = useMutation({
    mutationFn: (action: string) =>
      api(`/requests/${id}/transitions`, {
        method: 'POST',
        body: { action, parecer: parecer || null },
      }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: (e) => setLast(e),
  })

  const submit = useMutation({
    mutationFn: () => api(`/requests/${id}/submit`, { method: 'POST' }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: (e) => setLast(e),
  })

  const patchDraft = useMutation({
    mutationFn: () =>
      api(`/requests/${id}/draft`, { method: 'PATCH', body: { dados: formValues } }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: (e) => setLast(e),
  })

  async function onHateoas(rel: string, href: string) {
    if (rel === 'submit') {
      submit.mutate()
      return
    }
    if (rel === 'update-draft') {
      patchDraft.mutate()
      return
    }
    if (rel === 'upload-url') {
      setLast({ hint: 'use o AttachmentUpload abaixo', href })
      return
    }
    if (href.includes('/transitions')) {
      transition.mutate(actionFromRel(rel))
      return
    }
    const ui = uiPathFromHref(href)
    if (ui.startsWith('/solicitacoes') || ui.startsWith('/publico/')) nav(ui)
  }

  const problem = [detail.error, events.error, atts.error, protocol.error, transition.error, submit.error, patchDraft.error]
    .reverse()
    .find((e) => isProblem(e))

  return (
    <Page title={`solicitacao ${detail.data?.protocolo ?? id}`}>
      <p>
        <Link to="/solicitacoes">lista</Link>
      </p>
      {detail.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
      <p>
        {detail.data?.tipoCode} — {detail.data?.estado}
      </p>
      <label>
        parecer (reusado por defer/deny/ajuste)
        <input value={parecer} onChange={(e) => setParecer(e.target.value)} />
      </label>
      <HateoasBar links={links} onAction={(rel, href) => void onHateoas(rel, href)} />
      {links['update-draft'] ? (
        <button type="button" disabled={patchDraft.isPending} onClick={() => patchDraft.mutate()}>
          PATCH draft
        </button>
      ) : null}
      {links.submit ? (
        <button type="button" disabled={submit.isPending} onClick={() => submit.mutate()}>
          POST submit
        </button>
      ) : null}
      <form
        onSubmit={(e) => {
          e.preventDefault()
          transition.mutate(forcedAction)
        }}
      >
        <p>harness: POST action forçada (DEFERIR deve 422)</p>
        <input value={forcedAction} onChange={(e) => setForcedAction(e.target.value)} />
        <button type="submit">POST /transitions action=</button>
      </form>
      {schema ? (
        <DynamicForm
          schema={schema}
          value={formValues}
          onChange={(v) => setDados(v)}
          idCurso={undefined}
        />
      ) : null}
      <p>anexos GET /requests/{id}/attachments</p>
      <ul>
        {(atts.data ?? []).map((a) => (
          <li key={a.id}>
            {a.categoria} {a.nomeOriginal} {a.storageKey}
            <button
              type="button"
              onClick={() => {
                void api<{ downloadUrl: string }>(`/requests/${id}/attachments/${a.id}/download-url`)
                  .then((d) => setLast(d))
                  .catch((e: unknown) => setLast(e))
              }}
            >
              download-url
            </button>
            <button
              type="button"
              onClick={() => {
                void api(`/requests/${id}/attachments/${a.id}`, { method: 'DELETE' })
                  .then(() => {
                    setLast({ deleted: a.id })
                    invalidate()
                  })
                  .catch((e: unknown) => setLast(e))
              }}
            >
              DELETE
            </button>
          </li>
        ))}
      </ul>
      {(cats.length ? cats : ['OUTRO']).map((c) => (
        <AttachmentUpload
          key={c}
          requestId={id}
          categoria={c}
          onReady={() => invalidate()}
        />
      ))}
      <p>events (ABERTURA esperado)</p>
      <ul>
        {(events.data ?? []).map((e, i) => (
          <li key={i}>
            {e.tipo} {e.estadoAnterior} → {e.estadoNovo} {e.createdAt}
          </li>
        ))}
      </ul>
      <p>protocolo</p>
      {protocol.data?._links ? (
        <HateoasBar
          links={normalizeLinks(protocol.data._links)}
          onAction={(_rel, href) => nav(uiPathFromHref(href))}
        />
      ) : null}
      <JsonPanel data={protocol.data} />
      <p>última mutação</p>
      <JsonPanel data={last} />
      <p>GET /requests/{id}</p>
      <JsonPanel data={detail.error ?? detail.data} />
      <p>GET events</p>
      <JsonPanel data={events.data} />
      <p>GET attachments</p>
      <JsonPanel data={atts.data} />
    </Page>
  )
}
