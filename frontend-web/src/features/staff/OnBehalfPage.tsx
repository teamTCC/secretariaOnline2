import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type TypeRow = { id: string; code: string; descricao?: string }
type Curso = { id: string; nome?: string; sigla?: string }
type UsuarioPage = { content?: { id: string; email?: string; nome?: string }[] }
type Created = { id: string; idSolicitante?: string; estado?: string }

export function OnBehalfPage() {
  const nav = useNavigate()
  const [email, setEmail] = useState('ana.aluno@ufpr.br')
  const [alunoId, setAlunoId] = useState('')
  const [code, setCode] = useState('DECLARACAO_MATRICULA')
  const [idCurso, setIdCurso] = useState('')
  const [finalidade, setFinalidade] = useState('BOLSA')
  const [observacoes, setObs] = useState('Aberta pelo balcão')
  const [created, setCreated] = useState<Created | unknown>()

  const types = useQuery({
    queryKey: queryKeys.requestTypes,
    queryFn: () => api<TypeRow[]>('/requests/types'),
  })
  const cursos = useQuery({
    queryKey: queryKeys.cursos,
    queryFn: () => api<Curso[]>('/academico/cursos'),
  })

  useEffect(() => {
    if (idCurso) return
    const tads = cursos.data?.find((c) => c.sigla === 'TADS')
    if (tads) setIdCurso(tads.id)
  }, [cursos.data, idCurso])

  const lookup = useMutation({
    mutationFn: () => api<UsuarioPage>(`/usuarios?email=${encodeURIComponent(email)}&page=0&size=5`),
    onSuccess: (d) => {
      const id = d.content?.[0]?.id
      if (id) setAlunoId(id)
    },
  })

  const open = useMutation({
    mutationFn: () => {
      const idRequestType = types.data?.find((t) => t.code === code)?.id
      return api<Created>('/requests', {
        method: 'POST',
        body: {
          idRequestType,
          idCurso,
          idSolicitanteOnBehalf: alunoId,
          dados: { finalidade, observacoes },
        },
      })
    },
    onSuccess: (d) => {
      setCreated(d)
      if (d.id) nav(`/solicitacoes/${d.id}`)
    },
    onError: setCreated,
  })

  const problem = [types.error, cursos.error, lookup.error, open.error].reverse().find((e) => isProblem(e))

  return (
    <Page title="on-behalf">
      <p>
        GET /usuarios?email= → id · POST /requests com idSolicitanteOnBehalf. Authority request.internal_open |
        request.open_on_behalf. Detalhe: idSolicitante = aluno, não a secretaria.{' '}
        <Link to="/solicitacoes/nova">wizard também aceita o campo</Link>
      </p>
      {(types.isPending || open.isPending) && <p>carregando</p>}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
      <form
        onSubmit={(e) => {
          e.preventDefault()
          lookup.mutate()
        }}
      >
        <label>
          GET /usuarios?email=
          <input value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <button type="submit" disabled={lookup.isPending}>
          lookup aluno
        </button>
      </form>
      <label>
        idSolicitanteOnBehalf
        <input value={alunoId} onChange={(e) => setAlunoId(e.target.value)} />
      </label>
      <label>
        code
        <select value={code} onChange={(e) => setCode(e.target.value)}>
          {(types.data ?? []).map((t) => (
            <option key={t.code} value={t.code}>
              {t.code}
            </option>
          ))}
        </select>
      </label>
      <label>
        idCurso
        <select value={idCurso} onChange={(e) => setIdCurso(e.target.value)}>
          <option value="">—</option>
          {(cursos.data ?? []).map((c) => (
            <option key={c.id} value={c.id}>
              {c.sigla} — {c.nome}
            </option>
          ))}
        </select>
      </label>
      <label>
        finalidade
        <input value={finalidade} onChange={(e) => setFinalidade(e.target.value)} />
      </label>
      <label>
        observacoes
        <input value={observacoes} onChange={(e) => setObs(e.target.value)} />
      </label>
      <button type="button" disabled={!alunoId || !idCurso || open.isPending} onClick={() => open.mutate()}>
        POST /requests on-behalf
      </button>
      <JsonPanel data={created ?? lookup.data} />
    </Page>
  )
}
