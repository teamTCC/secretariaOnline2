import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem, type Problem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { afterAuthRedirect, setFlags, type SessionFlags } from '../../shared/auth/session'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

const exchanges = new Map<string, Promise<SessionFlags>>()

function exchangeOtt(token: string): Promise<SessionFlags> {
  const existing = exchanges.get(token)
  if (existing) return existing
  const p = api<SessionFlags>('/auth/ott', {
    method: 'POST',
    body: { token },
    skipRefresh: true,
  })
  exchanges.set(token, p)
  p.catch(() => {
    exchanges.delete(token)
  })
  return p
}

export function OttPage() {
  const [params] = useSearchParams()
  const tokenFromUrl = params.get('token') ?? params.get('ott') ?? ''
  const [token, setToken] = useState(tokenFromUrl)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<Problem | null>(null)
  const [data, setData] = useState<SessionFlags | undefined>()
  const nav = useNavigate()
  const qc = useQueryClient()

  useEffect(() => {
    if (tokenFromUrl) setToken(tokenFromUrl)
  }, [tokenFromUrl])

  useEffect(() => {
    const t = tokenFromUrl.trim()
    if (!t) return
    let cancelled = false
    setPending(true)
    setError(null)
    void exchangeOtt(t)
      .then((flags) => {
        if (cancelled) return
        setFlags(flags)
        qc.removeQueries({ queryKey: queryKeys.me })
        qc.removeQueries({ queryKey: ['dashboard'] })
        setData(flags)
        nav(afterAuthRedirect(flags), { replace: true })
      })
      .catch((e: unknown) => {
        if (cancelled) return
        setError(isProblem(e) ? e : null)
      })
      .finally(() => {
        if (!cancelled) setPending(false)
      })
    return () => {
      cancelled = true
    }
  }, [tokenFromUrl, nav, qc])

  return (
    <Page title="ott">
      <p>
        <Link to="/login">login</Link>
      </p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          const t = token.trim()
          if (!t) return
          setPending(true)
          setError(null)
          void exchangeOtt(t)
            .then((flags) => {
              setFlags(flags)
              qc.removeQueries({ queryKey: queryKeys.me })
              qc.removeQueries({ queryKey: ['dashboard'] })
              setData(flags)
              nav(afterAuthRedirect(flags), { replace: true })
            })
            .catch((err: unknown) => {
              setError(isProblem(err) ? err : null)
            })
            .finally(() => setPending(false))
        }}
      >
        <label>
          colar token JWT
          <textarea name="token" value={token} onChange={(e) => setToken(e.target.value)} rows={4} />
        </label>
        <button type="submit" disabled={pending}>
          POST /auth/ott
        </button>
      </form>
      {pending && <p>carregando</p>}
      <ProblemBanner problem={error} />
      <JsonPanel data={error ?? data} />
    </Page>
  )
}
