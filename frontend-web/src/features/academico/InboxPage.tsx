import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../../shared/api/client'
import { hrefOf, normalizeLinks } from '../../shared/api/hateoas'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { HateoasBar } from '../../shared/ui/HateoasBar'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Item = {
  deliveryId?: string
  idCommunication?: string
  communicationId?: string
  canal?: string
  status?: string
  deliveredAt?: string
  readAt?: string | null
  _links?: unknown
}

type Envelope = { content?: Item[]; page?: unknown }

function readHref(item: Item): string | undefined {
  const links = normalizeLinks(item._links)
  return hrefOf(links, 'read') ?? hrefOf(links, 'marcar-lido')
}

export function InboxPage() {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [detail, setDetail] = useState<unknown>()

  const inbox = useQuery({
    queryKey: queryKeys.inbox(page),
    queryFn: () => api<Envelope>(`/communications/me?page=${page}&size=20`),
  })
  const unread = useQuery({
    queryKey: queryKeys.inboxUnread,
    queryFn: () => api('/communications/me/unread-count'),
  })

  const mark = useMutation({
    mutationFn: (href: string) => api(href, { method: 'PATCH' }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['communications'] })
    },
  })

  return (
    <Page title="inbox">
      <p>GET /communications/me · unread-count · marcar lido pelo _links (PATCH). Publicar é fatia 6/7.</p>
      {(inbox.isPending || unread.isPending || mark.isPending) && <p>carregando</p>}
      <ProblemBanner
        problem={isProblem(inbox.error) ? inbox.error : isProblem(mark.error) ? mark.error : isProblem(unread.error) ? unread.error : null}
      />
      <p>unread-count: {JSON.stringify(unread.data)}</p>
      {(inbox.data?.content ?? []).map((it) => {
        const links = normalizeLinks(it._links)
        const href = readHref(it)
        const commId = it.idCommunication ?? it.communicationId
        return (
          <fieldset key={it.deliveryId}>
            <legend>
              {it.deliveryId} readAt={String(it.readAt ?? 'null')}
            </legend>
            <HateoasBar
              links={links}
              onAction={(rel, h) => {
                if (rel === 'read' || rel === 'marcar-lido') mark.mutate(h)
                if (rel === 'self' && commId) {
                  void api(`/communications/${commId}`).then(setDetail).catch(setDetail)
                }
              }}
            />
            {href && !it.readAt ? (
              <button type="button" onClick={() => mark.mutate(href)}>
                PATCH read
              </button>
            ) : null}
            {commId ? (
              <button
                type="button"
                onClick={() => void api(`/communications/${commId}`).then(setDetail).catch(setDetail)}
              >
                GET /communications/{commId}
              </button>
            ) : null}
            <JsonPanel data={it} />
          </fieldset>
        )
      })}
      <div className="row">
        <button type="button" disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>
          prev
        </button>
        <span>page {page}</span>
        <button type="button" onClick={() => setPage((p) => p + 1)}>
          next
        </button>
      </div>
      <h2>unread</h2>
      <JsonPanel data={unread.error ?? unread.data} />
      <h2>detalhe comunicado</h2>
      <JsonPanel data={detail} />
      <h2>inbox</h2>
      <JsonPanel data={inbox.error ?? inbox.data} />
    </Page>
  )
}
