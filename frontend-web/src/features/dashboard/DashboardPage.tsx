import { useMutation, useQuery } from '@tanstack/react-query'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { hrefOf, normalizeLinks, uiPathFromHref } from '../../shared/api/hateoas'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { HateoasBar } from '../../shared/ui/HateoasBar'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'
import { useState } from 'react'

type Me = {
  roles?: string[]
  authorities?: string[]
  _links?: unknown
}

type Pendencia = { id?: string; tipo?: string; estado?: string; _link?: string }

type Dashboard = {
  _links?: unknown
  _degraded?: boolean
  pendencias?: Pendencia[] | null
}

const BFFS = ['aluno', 'professor', 'secretaria', 'egresso'] as const

function perfilFromMe(me: Me | undefined): string | null {
  if (!me) return null
  const auths = me.authorities ?? []
  const roles = me.roles ?? []
  if (auths.includes('dashboard.view_secretary') || roles.includes('SECRETARIO')) return 'secretaria'
  if (auths.includes('dashboard.view_self_professor') || roles.includes('PROFESSOR')) return 'professor'
  if (auths.includes('alumni.view_own') || roles.includes('EGRESSO')) return 'egresso'
  if (auths.includes('dashboard.view_own') || roles.includes('ALUNO')) return 'aluno'
  return null
}

export function DashboardPage() {
  const nav = useNavigate()
  const [params, setParams] = useSearchParams()
  const forced = params.get('perfil')

  const me = useQuery({
    queryKey: queryKeys.me,
    queryFn: () => api<Me>('/me'),
  })

  const inferred = perfilFromMe(me.data)
  const perfil = forced || inferred
  const dash = useQuery({
    queryKey: queryKeys.dashboard(perfil ?? '_'),
    queryFn: () => api<Dashboard>(`/bff/dashboard/${perfil}`),
    enabled: Boolean(perfil),
  })

  const data = dash.data
  const links = normalizeLinks(data?._links)
  const [graduationId, setGraduationId] = useState(params.get('diploma') ?? '')
  const [diploma, setDiploma] = useState<unknown>()

  const diplomaUrl = useMutation({
    mutationFn: (gid: string) => api(`/graduations/${gid}/diploma-url`),
    onSuccess: setDiploma,
    onError: setDiploma,
  })

  return (
    <Page title="Dashboard">
      <p>
        perfil BFF: {perfil ?? '(nenhum — admin/coord sem dashboard)'}
        {forced ? ' (forçado)' : ''}
      </p>
      <div className="row">
        {BFFS.map((p) => (
          <button
            key={p}
            type="button"
            onClick={() => {
              const next = new URLSearchParams(params)
              next.set('perfil', p)
              setParams(next)
            }}
          >
            forçar GET /bff/dashboard/{p}
          </button>
        ))}
        <button
          type="button"
          onClick={() => {
            const next = new URLSearchParams(params)
            next.delete('perfil')
            setParams(next)
          }}
        >
          usar perfil do /me
        </button>
      </div>
      {(me.isPending || dash.isFetching) && <p>carregando</p>}
      <ProblemBanner problem={isProblem(me.error) ? me.error : null} />
      <ProblemBanner problem={isProblem(dash.error) ? dash.error : null} />
      {data ? (
        <>
          <HateoasBar
            links={links}
            onAction={(rel, href) => {
              if (rel === 'download') {
                void api(href).then(setDiploma).catch(setDiploma)
                return
              }
              nav(uiPathFromHref(href))
            }}
          />
          <p>degraded: {String(Boolean(data._degraded))}</p>
          <p>
            novaSolicitacao: {hrefOf(links, 'novaSolicitacao') ?? '(ausente)'}
            {perfil === 'egresso' ? ' — egresso não abre solicitação' : ''}
          </p>
          <form
            onSubmit={(e) => {
              e.preventDefault()
              if (graduationId) diplomaUrl.mutate(graduationId)
            }}
          >
            <label>
              GET /graduations/:id/diploma-url (colação fatia 7)
              <input value={graduationId} onChange={(e) => setGraduationId(e.target.value)} />
            </label>
            <button type="submit" disabled={diplomaUrl.isPending || !graduationId}>
              diploma-url
            </button>
          </form>
          <h2>diploma</h2>
          <JsonPanel data={diploma} />
          {data.pendencias?.length ? (
            <ul>
              {data.pendencias.map((p) => (
                <li key={p.id ?? p._link}>
                  <button type="button" disabled={!p._link} onClick={() => p._link && nav(uiPathFromHref(p._link))}>
                    {p.tipo ?? p.id} {p.estado} {p._link}
                  </button>
                </li>
              ))}
            </ul>
          ) : null}
        </>
      ) : null}
      {!perfil && me.data ? <p>sem BFF para este usuário — JSON do /me abaixo</p> : null}
      <JsonPanel data={dash.error ?? data ?? me.data} />
    </Page>
  )
}
