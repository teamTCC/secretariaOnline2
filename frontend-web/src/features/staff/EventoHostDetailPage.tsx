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

type Detail = {
  id: string
  titulo?: string
  descricao?: string
  attendanceMode?: string
  estado?: string
  chCreditadas?: number
  _links?: unknown
}

type WindowOpened = { mensagem?: string; closeAt?: string; secret?: string | null; qrToken?: string | null }

export function EventoHostDetailPage() {
  const { id = '' } = useParams()
  const qc = useQueryClient()
  const [durationSeconds, setDuration] = useState('900')
  const [lastWindow, setLastWindow] = useState<WindowOpened | null>(null)
  const [last, setLast] = useState<unknown>()

  const detail = useQuery({
    queryKey: queryKeys.evento(id),
    queryFn: () => api<Detail>(`/events/${id}`),
    enabled: Boolean(id),
  })

  const links = normalizeLinks(detail.data?._links)
  const dual = (detail.data?.attendanceMode ?? '').includes('DUAL')

  function invalidate() {
    void qc.invalidateQueries({ queryKey: queryKeys.evento(id) })
    void qc.invalidateQueries({ queryKey: ['events'] })
  }

  const openWin = useMutation({
    mutationFn: (href: string) =>
      api<WindowOpened>(href, { method: 'POST', body: { durationSeconds: Number(durationSeconds) } }),
    onSuccess: (d) => {
      setLastWindow(d)
      setLast(d)
      invalidate()
    },
    onError: (e) => setLast(e),
  })

  const closeEv = useMutation({
    mutationFn: (href: string) => api(href, { method: 'POST' }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: (e) => setLast(e),
  })

  const problem = [detail.error, openWin.error, closeEv.error].reverse().find((e) => isProblem(e))

  return (
    <Page title={`evento host ${id}`}>
      <p>
        <Link to="/prof/eventos">lista host</Link>
        {' · '}
        <Link to={`/eventos/${id}/presenca`}>sessão aluno</Link>
        {' · '}
        GET /events/:id · POST windows/entry|exit · POST close
      </p>
      <p>
        {detail.data?.titulo} — {detail.data?.attendanceMode} — {detail.data?.estado}
      </p>
      <p>
        AGENDADO: HateoasBar deve ter abrir-janela-entrada (host). Force POST se o rel faltar. Abrir entrada promove
        para EM_ANDAMENTO; aí aparecem encerrar-evento (e abrir-janela-saida se DUAL).
      </p>
      {detail.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
      <label>
        durationSeconds
        <input type="number" value={durationSeconds} onChange={(e) => setDuration(e.target.value)} />
      </label>
      <HateoasBar
        links={links}
        onAction={(rel, href) => {
          if (rel.startsWith('abrir-janela')) openWin.mutate(href)
          else if (rel === 'encerrar-evento') closeEv.mutate(href)
          else setLast({ rel, href, hint: 'GET/editar — sem PATCH neste harness' })
        }}
      />
      <div className="row">
        <button
          type="button"
          disabled={!id || openWin.isPending}
          onClick={() => openWin.mutate(`/events/${id}/attendance/windows/entry`)}
        >
          forçar POST windows/entry
        </button>
        <button
          type="button"
          disabled={!id || openWin.isPending || !dual}
          onClick={() => openWin.mutate(`/events/${id}/attendance/windows/exit`)}
        >
          forçar POST windows/exit
        </button>
        <button
          type="button"
          disabled={!id || closeEv.isPending}
          onClick={() => closeEv.mutate(`/events/${id}/close`)}
        >
          forçar POST close
        </button>
      </div>
      {lastWindow?.secret || lastWindow?.qrToken ? (
        <>
          <h2>PIN / QR (prova)</h2>
          {lastWindow.secret ? <pre className="secret">{lastWindow.secret}</pre> : null}
          {lastWindow.qrToken ? <pre className="secret">{lastWindow.qrToken}</pre> : null}
        </>
      ) : null}
      <h2>última janela</h2>
      <JsonPanel data={lastWindow} />
      <h2>última mutação</h2>
      <JsonPanel data={last} />
      <h2>GET /events/{id}</h2>
      <JsonPanel data={detail.error ?? detail.data} />
    </Page>
  )
}
