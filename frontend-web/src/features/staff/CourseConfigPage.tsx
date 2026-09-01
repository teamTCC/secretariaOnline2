import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { normalizeLinks } from '../../shared/api/hateoas'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { HateoasBar } from '../../shared/ui/HateoasBar'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Cfg = {
  courseId?: string
  sigla?: string
  horasFormativasMinimas?: number
  duracaoCalendario?: string
  bancaMembrosExternos?: number
  bancaModalidade?: string
  regimento?: string | null
  _links?: unknown
}

type DiscEnvelope = { content?: { id: string; codigo?: string; nome?: string }[] }

export function CourseConfigPage() {
  const { id = 'tads' } = useParams()
  const nav = useNavigate()
  const qc = useQueryClient()
  const [horas, setHoras] = useState('150')
  const [duracao, setDuracao] = useState('15_SEMANAS')
  const [banca, setBanca] = useState('1')
  const [modalidade, setMod] = useState('PRESENCIAL')
  const [regimento, setReg] = useState('')
  const [search, setSearch] = useState('')
  const [patched, setPatched] = useState<unknown>()

  const cfg = useQuery({
    queryKey: queryKeys.courseConfig(id),
    queryFn: () => api<Cfg>(`/courses/${id}/config`),
  })

  useEffect(() => {
    const d = cfg.data
    if (!d) return
    if (d.horasFormativasMinimas != null) setHoras(String(d.horasFormativasMinimas))
    if (d.duracaoCalendario) setDuracao(d.duracaoCalendario)
    if (d.bancaMembrosExternos != null) setBanca(String(d.bancaMembrosExternos))
    if (d.bancaModalidade) setMod(d.bancaModalidade)
    setReg(d.regimento ?? '')
  }, [cfg.data])

  const discs = useQuery({
    queryKey: queryKeys.disciplinasLookup({ search, idCurso: cfg.data?.courseId }),
    queryFn: () => {
      const qs = new URLSearchParams()
      if (search) qs.set('search', search)
      if (cfg.data?.courseId) qs.set('idCurso', cfg.data.courseId)
      return api<DiscEnvelope>(`/academico/disciplinas?${qs}`)
    },
  })

  const patch = useMutation({
    mutationFn: () =>
      api<Cfg>(`/courses/${id}/config`, {
        method: 'PATCH',
        body: {
          horasFormativasMinimas: Number(horas),
          duracaoCalendario: duracao,
          bancaMembrosExternos: Number(banca),
          bancaModalidade: modalidade,
          regimento: regimento || null,
        },
      }),
    onSuccess: (d) => {
      setPatched(d)
      void qc.invalidateQueries({ queryKey: queryKeys.courseConfig(id) })
    },
    onError: setPatched,
  })

  const data = cfg.data
  return (
    <Page title={`curso config ${id}`}>
      <p>
        GET/PATCH /courses/{'{id|sigla}'}/config. Ownership: id_coordenador == currentUser (admin bypass). Aluno → 403.
        Lookup GET /academico/disciplinas?search= (sem idCurso = searchActiveAll).
      </p>
      <label>
        path id
        <input
          defaultValue={id}
          onBlur={(e) => {
            if (e.target.value && e.target.value !== id) nav(`/cursos/${e.target.value}/config`)
          }}
        />
      </label>
      {cfg.isPending && <p>carregando</p>}
      <ProblemBanner
        problem={isProblem(cfg.error) ? cfg.error : isProblem(patch.error) ? patch.error : isProblem(discs.error) ? discs.error : null}
      />
      <HateoasBar
        links={normalizeLinks(data?._links)}
        onAction={(rel, href) => {
          if (rel === 'update') patch.mutate()
          else window.location.assign(href)
        }}
      />
      <form
        onSubmit={(e) => {
          e.preventDefault()
          patch.mutate()
        }}
      >
        <label>
          horasFormativasMinimas
          <input value={horas} onChange={(e) => setHoras(e.target.value)} />
        </label>
        <label>
          duracaoCalendario
          <select value={duracao} onChange={(e) => setDuracao(e.target.value)}>
            <option>15_SEMANAS</option>
            <option>18_SEMANAS</option>
          </select>
        </label>
        <label>
          bancaMembrosExternos
          <select value={banca} onChange={(e) => setBanca(e.target.value)}>
            <option>1</option>
            <option>2</option>
          </select>
        </label>
        <label>
          bancaModalidade
          <select value={modalidade} onChange={(e) => setMod(e.target.value)}>
            <option>PRESENCIAL</option>
            <option>REMOTO</option>
            <option>HÍBRIDO</option>
          </select>
        </label>
        <label>
          regimento
          <textarea value={regimento} onChange={(e) => setReg(e.target.value)} />
        </label>
        <button type="submit" disabled={patch.isPending}>
          PATCH /courses/{id}/config
        </button>
      </form>
      <label>
        disciplinas search
        <input value={search} onChange={(e) => setSearch(e.target.value)} />
      </label>
      <h2>PATCH</h2>
      <JsonPanel data={patched} />
      <h2>config</h2>
      <JsonPanel data={cfg.error ?? data} />
      <h2>disciplinas</h2>
      <JsonPanel data={discs.error ?? discs.data} />
    </Page>
  )
}
