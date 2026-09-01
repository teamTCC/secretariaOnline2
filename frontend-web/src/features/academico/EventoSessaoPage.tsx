import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { normalizeLinks } from '../../shared/api/hateoas'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { HateoasBar } from '../../shared/ui/HateoasBar'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'
import { deviceUuid } from './deviceUuid'

type Session = {
  idEvento?: string
  estado?: string
  attendanceMode?: string
  entryConfirmedAt?: string | null
  exitConfirmedAt?: string | null
  isComplete?: boolean
  _links?: unknown
}

export function EventoSessaoPage() {
  const { id } = useParams()
  const qc = useQueryClient()
  const [pin, setPin] = useState('')
  const [qrToken, setQrToken] = useState('')
  const [last, setLast] = useState<unknown>()
  const uuid = deviceUuid()

  const sess = useQuery({
    queryKey: queryKeys.eventoSessao(id ?? ''),
    queryFn: () => api<Session>(`/events/${id}/attendance/session`),
    enabled: Boolean(id),
  })

  const body = () => ({
    pin: pin || null,
    qrToken: qrToken || null,
    deviceUuid: uuid,
  })

  const post = useMutation({
    mutationFn: (href: string) => api(href, { method: 'POST', body: body() }),
    onSuccess: (d) => {
      setLast(d)
      void qc.invalidateQueries({ queryKey: queryKeys.eventoSessao(id ?? '') })
    },
    onError: (e) => setLast(e),
  })

  const links = normalizeLinks(sess.data?._links)

  return (
    <Page title="presença">
      <p>
        <Link to="/eventos">eventos</Link>
        {' · '}
        GET /events/{id}/attendance/session · POST entry/exit/qr/validate
      </p>
      <p>
        deviceUuid (localStorage): {uuid} · estado {sess.data?.estado} · mode {sess.data?.attendanceMode}
      </p>
      <p>sem janela: rel confirmar-entrada some. AGENDADO + POST entry → 409. PIN/QR colados do host.</p>
      {sess.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(sess.error) ? sess.error : isProblem(post.error) ? post.error : null} />
      <HateoasBar
        links={links}
        onAction={(_rel, href) => {
          post.mutate(href)
        }}
      />
      <label>
        pin
        <input value={pin} onChange={(e) => setPin(e.target.value)} />
      </label>
      <label>
        qrToken
        <input value={qrToken} onChange={(e) => setQrToken(e.target.value)} />
      </label>
      <div className="row">
        <button type="button" disabled={!id || post.isPending} onClick={() => post.mutate(`/events/${id}/attendance/entry`)}>
          forçar POST entry
        </button>
        <button type="button" disabled={!id || post.isPending} onClick={() => post.mutate(`/events/${id}/attendance/exit`)}>
          forçar POST exit
        </button>
        <button
          type="button"
          disabled={!id || post.isPending}
          onClick={() => post.mutate(`/events/${id}/attendance/qr/validate`)}
        >
          forçar POST qr/validate
        </button>
      </div>
      <h2>sessão</h2>
      <JsonPanel data={sess.error ?? sess.data} />
      <h2>última mutação</h2>
      <JsonPanel data={last} />
    </Page>
  )
}
