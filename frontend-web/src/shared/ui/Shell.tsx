import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, Outlet, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { normalizeLinks, uiPathFromHref } from '../api/hateoas'
import { isProblem } from '../api/problem'
import { queryKeys } from '../api/queryKeys'
import { clear } from '../auth/session'
import { HateoasBar } from './HateoasBar'
import { ProblemBanner } from './ProblemBanner'

export function Shell() {
  const nav = useNavigate()
  const qc = useQueryClient()
  const me = useQuery({
    queryKey: queryKeys.me,
    queryFn: () => api<{ _links?: unknown }>('/me'),
  })

  const logout = useMutation({
    mutationFn: () => api<{ mensagem?: string }>('/auth/logout', { method: 'POST' }),
    onSuccess: () => {
      clear()
      qc.clear()
      nav('/login', { replace: true })
    },
  })

  return (
    <>
      <nav className="row">
        <Link to="/login">Login</Link>
        <Link to="/dashboard">Dashboard</Link>
        <Link to="/solicitacoes">Solicitações</Link>
        <Link to="/formativas">Formativas</Link>
        <Link to="/eventos">Eventos</Link>
        <Link to="/certificados">Certificados</Link>
        <Link to="/atendimentos">Atendimentos</Link>
        <Link to="/comunicados">Inbox</Link>
        <Link to="/faq">FAQ</Link>
        <Link to="/me">Me</Link>
        <Link to="/contato">Contato</Link>
        <button type="button" disabled={logout.isPending} onClick={() => logout.mutate()}>
          Sair
        </button>
      </nav>
      <HateoasBar
        links={normalizeLinks(me.data?._links)}
        onAction={(_rel, href) => {
          const ui = uiPathFromHref(href)
          nav(ui.startsWith('/me') ? '/me' : ui)
        }}
      />
      {logout.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(logout.error) ? logout.error : null} />
      <Outlet />
    </>
  )
}
