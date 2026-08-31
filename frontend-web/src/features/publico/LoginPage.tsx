import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { afterAuthRedirect, setFlags, type SessionFlags } from '../../shared/auth/session'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

const DEMOS = [
  { label: 'Admin', identificador: 'admin@ufpr.br', senha: 'Admin@123456' },
  { label: 'Aluno', identificador: 'ana.aluno@ufpr.br', senha: 'AlunoS3nh@Forte!' },
  { label: 'Aluno GRR', identificador: 'GRR20210001', senha: 'AlunoS3nh@Forte!' },
  { label: 'Prof', identificador: 'prof.ana@ufpr.br', senha: 'ProfS3nh@Forte!' },
  { label: 'Sec', identificador: 'secretaria@ufpr.br', senha: 'SecrS3nh@Forte!' },
  { label: 'Coord', identificador: 'coord.tads@ufpr.br', senha: 'CoordS3nh@Forte!' },
  { label: 'Egresso', identificador: 'ana.egressa@ufpr.br', senha: 'EgressoS3nh@Forte!' },
]

export function LoginPage() {
  const nav = useNavigate()
  const qc = useQueryClient()
  const [identificador, setIdentificador] = useState('')
  const [senha, setSenha] = useState('')

  const m = useMutation({
    mutationFn: () =>
      api<SessionFlags>('/auth/login', {
        method: 'POST',
        body: { identificador, senha },
        skipRefresh: true,
      }),
    onSuccess: (data) => {
      setFlags(data)
      qc.removeQueries({ queryKey: queryKeys.me })
      qc.removeQueries({ queryKey: ['dashboard'] })
      nav(afterAuthRedirect(data), { replace: true })
    },
  })

  return (
    <Page title="login">
      <p>
        <Link to="/recuperar-senha">recuperar senha</Link>
        {' · '}
        <Link to="/contato">contato</Link>
        {' · '}
        <Link to="/publico/verificar-certificado">jwks/cert</Link>
        {' · '}
        <Link to="/health-front">health-front</Link>
      </p>
      {import.meta.env.DEV ? (
        <label>
          preencher demo
          <select
            defaultValue=""
            onChange={(e) => {
              if (e.target.value === '') return
              const demo = DEMOS[Number(e.target.value)]
              if (!demo) return
              setIdentificador(demo.identificador)
              setSenha(demo.senha)
            }}
          >
            <option value="">—</option>
            {DEMOS.map((d, i) => (
              <option key={d.label} value={i}>
                {d.label}
              </option>
            ))}
          </select>
        </label>
      ) : null}
      <form
        onSubmit={(e) => {
          e.preventDefault()
          m.mutate()
        }}
      >
        <label>
          identificador
          <input
            name="identificador"
            value={identificador}
            onChange={(e) => setIdentificador(e.target.value)}
            autoComplete="username"
          />
        </label>
        <label>
          senha
          <input
            name="senha"
            type="password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            autoComplete="current-password"
          />
        </label>
        <button type="submit" disabled={m.isPending}>
          entrar
        </button>
      </form>
      {m.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(m.error) ? m.error : null} />
      <JsonPanel data={m.error ?? m.data} />
    </Page>
  )
}
