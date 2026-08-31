import { useMutation } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

export function ResetPage() {
  const [params] = useSearchParams()
  const urlToken = params.get('token') ?? ''
  const [token, setToken] = useState(urlToken)
  const [novaSenha, setNovaSenha] = useState('')

  useEffect(() => {
    if (urlToken) setToken(urlToken)
  }, [urlToken])
  const m = useMutation({
    mutationFn: () =>
      api<{ mensagem: string }>('/auth/reset-password', {
        method: 'POST',
        body: { token, novaSenha },
        skipRefresh: true,
      }),
  })

  return (
    <Page title="nova senha">
      <p>
        <Link to="/login">login</Link>
        {' · '}
        <Link to="/recuperar-senha">forgot</Link>
      </p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          m.mutate()
        }}
      >
        <label>
          colar token JWT
          <textarea name="token" value={token} onChange={(e) => setToken(e.target.value)} rows={4} />
        </label>
        <label>
          nova senha
          <input
            name="novaSenha"
            type="password"
            value={novaSenha}
            onChange={(e) => setNovaSenha(e.target.value)}
          />
        </label>
        <button type="submit" disabled={m.isPending}>
          redefinir
        </button>
      </form>
      {m.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(m.error) ? m.error : null} />
      <JsonPanel data={m.error ?? m.data} />
    </Page>
  )
}
