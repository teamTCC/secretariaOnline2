import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

const MODES = ['SECRET_SINGLE', 'SECRET_DUAL', 'QR_SINGLE', 'QR_DUAL'] as const

type Curso = { id: string; nome?: string; sigla?: string }
type Me = { metadata?: { idCurso?: string } }
type Row = {
  id: string
  titulo?: string
  attendanceMode?: string
  estado?: string
  chCreditadas?: number
}
type Envelope = { content?: Row[]; page?: { number: number; totalElements?: number } }
type Created = { id?: string; _links?: unknown }

function isoOffsetHours(h: number) {
  return new Date(Date.now() + h * 3600_000).toISOString()
}

export function EventosHostPage() {
  const qc = useQueryClient()
  const [titulo, setTitulo] = useState('Palestra: IA na Engenharia')
  const [descricao, setDescricao] = useState('Host fatia 6 — ajuste inicio/fim para agora.')
  const [idCurso, setIdCurso] = useState('')
  const [attendanceMode, setMode] = useState<(typeof MODES)[number]>('SECRET_SINGLE')
  const [chCreditadas, setCh] = useState('4')
  const [inicioEm, setInicio] = useState(isoOffsetHours(-1))
  const [fimEm, setFim] = useState(isoOffsetHours(4))
  const [created, setCreated] = useState<unknown[]>([])

  const me = useQuery({ queryKey: queryKeys.me, queryFn: () => api<Me>('/me') })
  const cursos = useQuery({
    queryKey: queryKeys.cursos,
    queryFn: () => api<Curso[]>('/academico/cursos'),
  })
  const list = useQuery({
    queryKey: queryKeys.eventosHost({ host: 'me' }),
    queryFn: () => api<Envelope>('/events?host=me&page=0&size=50'),
  })

  useEffect(() => {
    if (idCurso) return
    const fromMe = me.data?.metadata?.idCurso
    if (fromMe) {
      setIdCurso(fromMe)
      return
    }
    const tads = cursos.data?.find((c) => c.sigla === 'TADS')
    if (tads) setIdCurso(tads.id)
  }, [me.data, cursos.data, idCurso])

  function body(mode: string, title: string) {
    return {
      titulo: title,
      descricao,
      idCurso: idCurso || null,
      attendanceMode: mode,
      chCreditadas: Number(chCreditadas),
      inicioEm,
      fimEm,
    }
  }

  const create = useMutation({
    mutationFn: (payload: { mode: string; title: string }) =>
      api<Created>('/events', { method: 'POST', body: body(payload.mode, payload.title) }),
    onSuccess: (d) => {
      setCreated((prev) => [d, ...prev])
      void qc.invalidateQueries({ queryKey: ['events'] })
    },
  })

  const createFour = useMutation({
    mutationFn: async () => {
      const out: Created[] = []
      for (const mode of MODES) {
        const d = await api<Created>('/events', {
          method: 'POST',
          body: body(mode, `Fatia6 ${mode}`),
        })
        out.push(d)
      }
      return out
    },
    onSuccess: (rows) => {
      setCreated((prev) => [...rows, ...prev])
      void qc.invalidateQueries({ queryKey: ['events'] })
    },
  })

  const lastErr = createFour.error ?? create.error ?? list.error

  return (
    <Page title="eventos (host)">
      <p>
        GET /events?host=me · POST /events (event.manage). Janela/PIN no detalhe. Aluno confirma em
        /eventos/:id/presenca.
      </p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          create.mutate({ mode: attendanceMode, title: titulo })
        }}
      >
        <label>
          titulo
          <input value={titulo} onChange={(e) => setTitulo(e.target.value)} />
        </label>
        <label>
          descricao
          <textarea value={descricao} onChange={(e) => setDescricao(e.target.value)} />
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
        <label>
          attendanceMode
          <select value={attendanceMode} onChange={(e) => setMode(e.target.value as (typeof MODES)[number])}>
            {MODES.map((m) => (
              <option key={m} value={m}>
                {m}
              </option>
            ))}
          </select>
        </label>
        <label>
          chCreditadas
          <input type="number" step="0.5" value={chCreditadas} onChange={(e) => setCh(e.target.value)} />
        </label>
        <label>
          inicioEm
          <input value={inicioEm} onChange={(e) => setInicio(e.target.value)} />
        </label>
        <label>
          fimEm
          <input value={fimEm} onChange={(e) => setFim(e.target.value)} />
        </label>
        <button type="submit" disabled={create.isPending}>
          POST /events
        </button>
        <button
          type="button"
          disabled={createFour.isPending || !idCurso}
          onClick={() => createFour.mutate()}
        >
          POST os 4 attendanceMode
        </button>
      </form>
      {(list.isPending || create.isPending || createFour.isPending) && <p>carregando</p>}
      <ProblemBanner problem={isProblem(lastErr) ? lastErr : null} />
      <table>
        <thead>
          <tr>
            <th>id</th>
            <th>titulo</th>
            <th>mode</th>
            <th>estado</th>
          </tr>
        </thead>
        <tbody>
          {(list.data?.content ?? []).map((r) => (
            <tr key={r.id}>
              <td>
                <Link to={`/prof/eventos/${r.id}`}>{r.id}</Link>
              </td>
              <td>{r.titulo}</td>
              <td>{r.attendanceMode}</td>
              <td>{r.estado}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <h2>created</h2>
      {created.map((row, i) =>
        row && typeof row === 'object' && 'id' in row ? (
          <p key={String((row as Created).id) + i}>
            <Link to={`/prof/eventos/${(row as Created).id}`}>{(row as Created).id}</Link>
          </p>
        ) : null,
      )}
      <JsonPanel data={created} />
      <h2>host=me</h2>
      <JsonPanel data={list.error ?? list.data} />
    </Page>
  )
}
