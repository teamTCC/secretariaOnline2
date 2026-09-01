import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { normalizeLinks } from '../../shared/api/hateoas'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { HateoasBar } from '../../shared/ui/HateoasBar'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Student = {
  id: string
  nome?: string
  email?: string
  eligible?: boolean
  bloqueios?: { razao?: string; detalhe?: string }[]
}
type Grad = {
  id: string
  idAluno?: string
  estado?: string
  diplomaHashSha256?: string
  _links?: unknown
}
type Envelope<T> = { content?: T[]; page?: { number: number } }
type Periodo = { id?: string }
type Curso = { id: string; sigla?: string }

export function GraduationsPage() {
  const qc = useQueryClient()
  const [params, setParams] = useSearchParams()
  const [alunoIds, setAlunoIds] = useState('')
  const [idCurso, setIdCurso] = useState('')
  const [periodoId, setPeriodoId] = useState('')
  const [gradId, setGradId] = useState(params.get('id') ?? '')
  const [last, setLast] = useState<unknown>()

  const students = useQuery({
    queryKey: queryKeys.studentsElig('true'),
    queryFn: () => api<Envelope<Student>>('/students?eligibleForGraduation=true&page=0&size=50'),
  })
  const grads = useQuery({
    queryKey: queryKeys.graduations(),
    queryFn: () => api<Envelope<Grad>>('/graduations?page=0&size=20'),
  })
  const egressos = useQuery({
    queryKey: ['secretaria', 'egressos'],
    queryFn: () => api('/secretaria/egressos?page=0&size=20'),
  })
  const periodo = useQuery({
    queryKey: ['academico', 'periodos', 'ativo'],
    queryFn: () => api<Periodo>('/academico/periodos/ativo'),
  })
  const cursos = useQuery({
    queryKey: queryKeys.cursos,
    queryFn: () => api<Curso[]>('/academico/cursos'),
  })

  useEffect(() => {
    if (!periodoId && periodo.data?.id) setPeriodoId(periodo.data.id)
  }, [periodo.data, periodoId])
  useEffect(() => {
    if (idCurso) return
    const tads = cursos.data?.find((c) => c.sigla === 'TADS')
    if (tads) setIdCurso(tads.id)
  }, [cursos.data, idCurso])

  const colar = useMutation({
    mutationFn: () =>
      api('/graduations', {
        method: 'POST',
        body: {
          alunoIds: alunoIds.split(/[\s,]+/).filter(Boolean),
          idCurso,
          dataColacao: '2026-07-15',
          livro: '12',
          folha: '34',
          ata: '001/2026',
          periodoId: periodoId || null,
        },
      }),
    onSuccess: (d) => {
      setLast(d)
      void qc.invalidateQueries({ queryKey: ['graduations'] })
      void qc.invalidateQueries({ queryKey: ['students'] })
    },
    onError: setLast,
  })

  const diploma = useMutation({
    mutationFn: (id: string) => api(`/graduations/${id}/diploma-url`),
    onSuccess: setLast,
    onError: setLast,
  })

  const deliver = useMutation({
    mutationFn: (id: string) => api(`/graduations/${id}/confirm-delivery`, { method: 'PATCH' }),
    onSuccess: (d) => {
      setLast(d)
      void qc.invalidateQueries({ queryKey: ['graduations'] })
    },
    onError: setLast,
  })

  const problem = [students.error, grads.error, egressos.error, colar.error, diploma.error, deliver.error]
    .reverse()
    .find((e) => isProblem(e))

  return (
    <Page title="colação / diplomas">
      <p>
        GET /students?eligibleForGraduation=true (bloqueios visíveis) · POST /graduations · GET /graduations · GET
        diploma-url · PATCH confirm-delivery · GET /secretaria/egressos.
      </p>
      {(students.isPending || grads.isPending) && <p>carregando</p>}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
      <table>
        <thead>
          <tr>
            <th>aluno</th>
            <th>eligible</th>
            <th>bloqueios</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {(students.data?.content ?? []).map((s) => (
            <tr key={s.id}>
              <td>
                {s.nome} {s.email}
              </td>
              <td>{String(s.eligible)}</td>
              <td>{(s.bloqueios ?? []).map((b) => b.razao).join(', ') || '—'}</td>
              <td>
                <button type="button" onClick={() => setAlunoIds(s.id)}>
                  usar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <label>
        alunoIds
        <input value={alunoIds} onChange={(e) => setAlunoIds(e.target.value)} />
      </label>
      <label>
        idCurso
        <input value={idCurso} onChange={(e) => setIdCurso(e.target.value)} />
      </label>
      <label>
        periodoId
        <input value={periodoId} onChange={(e) => setPeriodoId(e.target.value)} />
      </label>
      <button type="button" disabled={!alunoIds || colar.isPending} onClick={() => colar.mutate()}>
        POST /graduations
      </button>
      <h2>registros</h2>
      <table>
        <thead>
          <tr>
            <th>id</th>
            <th>estado</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {(grads.data?.content ?? []).map((g) => (
            <tr key={g.id}>
              <td>{g.id}</td>
              <td>{g.estado}</td>
              <td>
                <HateoasBar
                  links={normalizeLinks(g._links)}
                  onAction={(rel, href) => {
                    if (rel === 'diploma-url' || rel === 'download') diploma.mutate(g.id)
                    else if (rel === 'confirm-delivery') deliver.mutate(g.id)
                    else void api(href).then(setLast).catch(setLast)
                  }}
                />
                <button type="button" onClick={() => diploma.mutate(g.id)}>
                  diploma-url
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <label>
        graduationId colado
        <input
          value={gradId}
          onChange={(e) => {
            setGradId(e.target.value)
            const next = new URLSearchParams(params)
            if (e.target.value) next.set('id', e.target.value)
            else next.delete('id')
            setParams(next)
          }}
        />
      </label>
      <div className="row">
        <button type="button" disabled={!gradId} onClick={() => diploma.mutate(gradId)}>
          GET diploma-url
        </button>
        <button type="button" disabled={!gradId} onClick={() => deliver.mutate(gradId)}>
          PATCH confirm-delivery
        </button>
      </div>
      <h2>última mutação</h2>
      <JsonPanel data={last} />
      <h2>elegíveis + bloqueios</h2>
      <JsonPanel data={students.error ?? students.data} />
      <h2>graduations</h2>
      <JsonPanel data={grads.error ?? grads.data} />
      <h2>egressos</h2>
      <JsonPanel data={egressos.error ?? egressos.data} />
    </Page>
  )
}
