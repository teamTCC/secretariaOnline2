import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

const TIPOS = ['AVISO', 'URGENTE', 'INFORMATIVO'] as const

type Curso = { id: string; nome?: string; sigla?: string }
type Me = { metadata?: { idCurso?: string } }
type Published = { id?: string; entregas?: number }

export function PublicarAvisoPage() {
  const [titulo, setTitulo] = useState('Aviso da turma TADS — fatia 6')
  const [conteudo, setConteudo] = useState('Prazo de formativas encerra sexta-feira às 18h.')
  const [tipo, setTipo] = useState<(typeof TIPOS)[number]>('AVISO')
  const [cursoId, setCursoId] = useState('')
  const [omitCurso, setOmitCurso] = useState(false)
  const [created, setCreated] = useState<unknown>()

  const me = useQuery({ queryKey: queryKeys.me, queryFn: () => api<Me>('/me') })
  const cursos = useQuery({
    queryKey: queryKeys.cursos,
    queryFn: () => api<Curso[]>('/academico/cursos'),
  })

  useEffect(() => {
    if (cursoId) return
    const fromMe = me.data?.metadata?.idCurso
    if (fromMe) {
      setCursoId(fromMe)
      return
    }
    const tads = cursos.data?.find((c) => c.sigla === 'TADS')
    if (tads) setCursoId(tads.id)
  }, [me.data, cursos.data, cursoId])

  const publish = useMutation({
    mutationFn: () =>
      api<Published>('/communications', {
        method: 'POST',
        body: {
          titulo,
          conteudo,
          tipo,
          ...(omitCurso ? {} : { cursoId: cursoId || null }),
        },
      }),
    onSuccess: setCreated,
    onError: setCreated,
  })

  return (
    <Page title="publicar comunicado">
      <p>
        POST /communications — communication.publish_class precisa cursoId (422 sem). Admin communication.publish aceita
        sem curso. Aluno vê em GET /communications/me (inbox).
      </p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          publish.mutate()
        }}
      >
        <label>
          titulo
          <input value={titulo} onChange={(e) => setTitulo(e.target.value)} />
        </label>
        <label>
          conteudo
          <textarea value={conteudo} onChange={(e) => setConteudo(e.target.value)} />
        </label>
        <label>
          tipo
          <select value={tipo} onChange={(e) => setTipo(e.target.value as (typeof TIPOS)[number])}>
            {TIPOS.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </label>
        <label>
          cursoId
          <select value={cursoId} onChange={(e) => setCursoId(e.target.value)} disabled={omitCurso}>
            <option value="">—</option>
            {(cursos.data ?? []).map((c) => (
              <option key={c.id} value={c.id}>
                {c.sigla} — {c.nome} ({c.id})
              </option>
            ))}
          </select>
        </label>
        <label>
          <input type="checkbox" checked={omitCurso} onChange={(e) => setOmitCurso(e.target.checked)} /> omitir cursoId
          (422 se só publish_class)
        </label>
        <button type="submit" disabled={publish.isPending}>
          POST /communications
        </button>
      </form>
      {publish.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(publish.error) ? publish.error : null} />
      {created && typeof created === 'object' && created !== null && 'id' in created ? (
        <p>
          id {(created as Published).id} entregas={(created as Published).entregas}
        </p>
      ) : null}
      <h2>created</h2>
      <JsonPanel data={created} />
    </Page>
  )
}
