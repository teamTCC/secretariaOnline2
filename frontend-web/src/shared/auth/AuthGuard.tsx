import { useQuery } from '@tanstack/react-query'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { api } from '../api/client'
import { isProblem } from '../api/problem'
import { queryKeys } from '../api/queryKeys'
import { clear, getFlags } from './session'

export function AuthGuard() {
  const loc = useLocation()
  const q = useQuery({
    queryKey: queryKeys.me,
    queryFn: () => api('/me'),
  })

  if (q.isPending) return <p>carregando</p>

  if (isProblem(q.error) && q.error.status === 401) {
    clear()
    return <Navigate to="/login" replace state={{ from: loc.pathname }} />
  }

  const flags = getFlags()
  // as-built FirstAccessUseCase exige senhaAlterada=false. mustAcceptLgpd sozinho
  // (metadata sem aceite_lgpd_em) não consegue completar o POST — não prender a SPA.
  if (flags?.mustChangePassword && loc.pathname !== '/primeiro-acesso') {
    return <Navigate to="/primeiro-acesso" replace />
  }

  return <Outlet />
}
