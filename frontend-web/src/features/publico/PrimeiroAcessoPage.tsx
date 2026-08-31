import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { getFlags, setFlags } from '../../shared/auth/session'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

export function PrimeiroAcessoPage() {
  const nav = useNavigate()
  const qc = useQueryClient()
  const [novaSenha, setNovaSenha] = useState('')
  const [aceiteLgpd, setAceiteLgpd] = useState(true)

  const m = useMutation({
    mutationFn: () =>
      api<{ mensagem: string }>('/auth/first-access', {
        method: 'POST',
        body: { novaSenha, aceiteLgpd },
      }),
    onSuccess: () => {
      setFlags({ mustChangePassword: false, mustAcceptLgpd: false })
      qc.invalidateQueries({ queryKey: queryKeys.me })
      nav('/dashboard', { replace: true })
    },
  })

  return (
    <Page title="primeiro acesso">
      <p>flags: {JSON.stringify(getFlags())}</p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          m.mutate()
        }}
      >
        <label>
          novaSenha
          <input
            name="novaSenha"
            type="password"
            value={novaSenha}
            onChange={(e) => setNovaSenha(e.target.value)}
            autoComplete="new-password"
          />
        </label>
        <label>
          <input
            type="checkbox"
            checked={aceiteLgpd}
            onChange={(e) => setAceiteLgpd(e.target.checked)}
          />
          aceiteLgpd
        </label>
        <button type="submit" disabled={m.isPending}>
          POST /auth/first-access
        </button>
      </form>
      {m.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(m.error) ? m.error : null} />
      {getFlags()?.mustChangePassword === false ? (
        <p>
          <button
            type="button"
            onClick={() => {
              setFlags({ mustChangePassword: false, mustAcceptLgpd: false })
              nav('/dashboard', { replace: true })
            }}
          >
            ir ao dashboard (first-access já feito)
          </button>
        </p>
      ) : null}
      <JsonPanel data={m.error ?? m.data} />
    </Page>
  )
}
