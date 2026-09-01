import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Curso = { id: string; nome?: string; sigla?: string }
type Me = { metadata?: { idCurso?: string } }
type Row = { id: string; titulo?: string; estado?: string; idOrientador?: string; dataDefesa?: string }
type Envelope = { content?: Row[]; page?: { number: number; totalElements?: number } }

export function TccsPage() {
  const qc = useQueryClient()
  const [titulo, setTitulo] = useState('Modernização de Sistema Acadêmico — SecretariaOnline2')
  const [idCurso, setIdCurso] = useState('')
  const [created, setCreated] = useState<unknown>()
  const [staff, setStaff] = useState<unknown>()
  const [staffEstado, setStaffEstado] = useState('EM_ANDAMENTO')

  const me = useQuery({ queryKey: queryKeys.me, queryFn: () => api<Me>('/me') })
  const cursos = useQuery({
    queryKey: queryKeys.cursos,
    queryFn: () => api<Curso[]>('/academico/cursos'),
  })
  const mine = useQuery({
    queryKey: queryKeys.tccsMine,
    queryFn: () => api<Row[]>('/tccs/mine'),
  })

  useEffect(() => {
    if (idCurso || !me.data?.metadata?.idCurso) return
    setIdCurso(me.data.metadata.idCurso)
  }, [me.data, idCurso])

  const create = useMutation({
    mutationFn: () =>
      api('/tccs', {
        method: 'POST',
        body: { titulo, idCurso },
      }),
    onSuccess: (d) => {
      setCreated(d)
      void qc.invalidateQueries({ queryKey: ['tccs'] })
    },
  })

  const listStaff = useMutation({
    mutationFn: () => api<Envelope>(`/tccs?estado=${encodeURIComponent(staffEstado)}&page=0&size=20`),
    onSuccess: setStaff,
    onError: setStaff,
  })

  const mineRows = Array.isArray(mine.data) ? mine.data : []

  return (
    <Page title="tccs">
      <p>
        POST /tccs (tcc.supervise) · GET /tccs/mine (aluno) · GET /tccs (orientador/sec). Aluno sem supervise → 403 no
        POST. Path HTTP nunca /tcc/me.
      </p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          create.mutate()
        }}
      >
        <label>
          titulo
          <input value={titulo} onChange={(e) => setTitulo(e.target.value)} />
        </label>
        <label>
          idCurso
          <select value={idCurso} onChange={(e) => setIdCurso(e.target.value)}>
            <option value="">—</option>
            {(cursos.data ?? []).map((c) => (
              <option key={c.id} value={c.id}>
                {c.sigla} — {c.nome} ({c.id})
              </option>
            ))}
          </select>
        </label>
        <button type="submit" disabled={create.isPending || !idCurso}>
          POST /tccs
        </button>
      </form>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          listStaff.mutate()
        }}
      >
        <label>
          GET /tccs estado
          <input value={staffEstado} onChange={(e) => setStaffEstado(e.target.value)} />
        </label>
        <button type="submit" disabled={listStaff.isPending}>
          GET /tccs
        </button>
      </form>
      {(mine.isPending || create.isPending) && <p>carregando</p>}
      <ProblemBanner
        problem={isProblem(create.error) ? create.error : isProblem(listStaff.error) ? listStaff.error : null}
      />
      <h2>mine</h2>
      <table>
        <thead>
          <tr>
            <th>id</th>
            <th>titulo</th>
            <th>estado</th>
          </tr>
        </thead>
        <tbody>
          {mineRows.map((r) => (
            <tr key={r.id}>
              <td>
                <Link to={`/tccs/${r.id}`}>{r.id}</Link>
              </td>
              <td>{r.titulo}</td>
              <td>{r.estado}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <h2>created</h2>
      {created && typeof created === 'object' && created !== null && 'id' in created ? (
        <p>
          <Link to={`/tccs/${(created as { id: string }).id}`}>abrir {(created as { id: string }).id}</Link>
        </p>
      ) : null}
      <JsonPanel data={created} />
      <h2>GET /tccs (staff)</h2>
      <JsonPanel data={staff} />
      <h2>GET /tccs/mine</h2>
      <JsonPanel data={mine.error ?? mine.data} />
    </Page>
  )
}
