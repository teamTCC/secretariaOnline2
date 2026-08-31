import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

export function ForgotPage() {
  const [email, setEmail] = useState('')
  const m = useMutation({
    mutationFn: () =>
      api<{ mensagem: string }>('/auth/forgot-password', {
        method: 'POST',
        body: { email },
        skipRefresh: true,
      }),
  })

  return (
    <Page title="recuperar senha">
      <p>
        <Link to="/login">login</Link>
        {' · '}
        <Link to="/nova-senha">colar token</Link>
      </p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          m.mutate()
        }}
      >
        <label>
          email
          <input
            name="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </label>
        <button type="submit" disabled={m.isPending}>
          enviar
        </button>
      </form>
      {m.isPending && <p>carregando</p>}
      {m.isSuccess ? <p>se existir, enviaremos link</p> : null}
      <ProblemBanner problem={isProblem(m.error) ? m.error : null} />
      <JsonPanel data={m.error ?? m.data} />
    </Page>
  )
}
