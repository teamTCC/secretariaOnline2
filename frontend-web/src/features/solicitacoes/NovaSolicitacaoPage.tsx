import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { AttachmentUpload, type AttachmentInput } from '../../shared/ui/AttachmentUpload'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'
import { DynamicForm, requiredAttachments, type JsonSchema } from './DynamicForm'

type TypeRow = {
  id: string
  code: string
  descricao?: string
  formSchema?: JsonSchema
}

type TypeDetail = TypeRow & { formSchema?: JsonSchema }

type Curso = { id: string; nome?: string; sigla?: string }

type Me = { metadata?: { idCurso?: string } }

type Created = { id: string; estado?: string }

export function NovaSolicitacaoPage() {
  const nav = useNavigate()
  const [params, setParams] = useSearchParams()
  const code = params.get('code') ?? ''
  const [idCurso, setIdCurso] = useState('')
  const [dados, setDados] = useState<Record<string, unknown>>({})
  const [mode, setMode] = useState<'open' | 'draft'>('open')
  const [onBehalf, setOnBehalf] = useState('')
  const [pendingAtts, setPendingAtts] = useState<AttachmentInput[]>([])
  const [draftId, setDraftId] = useState<string>()
  const [seededCurso, setSeededCurso] = useState(false)

  const me = useQuery({ queryKey: queryKeys.me, queryFn: () => api<Me>('/me') })
  const types = useQuery({
    queryKey: queryKeys.requestTypes,
    queryFn: () => api<TypeRow[]>('/requests/types'),
  })
  const type = useQuery({
    queryKey: queryKeys.requestType(code),
    queryFn: () => api<TypeDetail>(`/requests/types/${code}`),
    enabled: Boolean(code),
  })
  const cursos = useQuery({
    queryKey: queryKeys.cursos,
    queryFn: () => api<Curso[]>('/academico/cursos'),
  })

  useEffect(() => {
    if (seededCurso || !me.data) return
    const fromMe = me.data.metadata?.idCurso
    if (fromMe) {
      setIdCurso(fromMe)
      setSeededCurso(true)
    }
  }, [me.data, seededCurso])

  const schema = type.data?.formSchema ?? types.data?.find((t) => t.code === code)?.formSchema
  const cats = schema ? requiredAttachments(schema) : []
  const extraCats = [...new Set([...cats, ...pendingAtts.map((a) => a.categoria)])]

  const save = useMutation({
    mutationFn: async () => {
      const idRequestType = type.data?.id ?? types.data?.find((t) => t.code === code)?.id
      if (!idRequestType) {
        throw {
          type: 'https://secretariaonline.ufpr.br/errors/validation-error',
          title: 'Tipo ausente',
          status: 400,
          detail: 'Selecione um code de GET /requests/types',
        }
      }
      if (!idCurso) {
        throw {
          type: 'https://secretariaonline.ufpr.br/errors/validation-error',
          title: 'Curso ausente',
          status: 400,
          detail: 'Selecione idCurso (GET /academico/cursos)',
        }
      }
      const body = {
        idRequestType,
        idCurso,
        dados,
        attachments: pendingAtts,
        idSolicitanteOnBehalf: onBehalf || null,
      }
      if (mode === 'draft') {
        if (draftId) {
          return api<Created>(`/requests/${draftId}/draft`, { method: 'PATCH', body: { dados } })
        }
        return api<Created>('/requests/draft', { method: 'POST', body })
      }
      return api<Created>('/requests', { method: 'POST', body })
    },
    onSuccess: async (d) => {
      if (d.id && pendingAtts.length && d.estado === 'RASCUNHO') {
        for (const att of pendingAtts) {
          try {
            await api(`/requests/${d.id}/attachments/confirm`, { method: 'POST', body: att })
          } catch {
            /* confirm visível no detalhe se falhar */
          }
        }
      }
      if (mode === 'draft') setDraftId(d.id)
      nav(`/solicitacoes/${d.id}`)
    },
  })

  return (
    <Page title="nova solicitacao">
      <p>
        <Link to="/solicitacoes">lista</Link>
      </p>
      {types.isPending && <p>carregando types</p>}
      <ProblemBanner
        problem={isProblem(types.error) ? types.error : isProblem(type.error) ? type.error : isProblem(save.error) ? save.error : null}
      />
      <label>
        code (GET /requests/types)
        <select
          value={code}
          onChange={(e) => {
            const next = new URLSearchParams(params)
            if (e.target.value) next.set('code', e.target.value)
            else next.delete('code')
            setParams(next)
            setDados({})
            setDraftId(undefined)
            setPendingAtts([])
          }}
        >
          <option value="">— {types.data?.length ?? 0} tipos —</option>
          {(types.data ?? []).map((t) => (
            <option key={t.code} value={t.code}>
              {t.code} — {t.descricao}
            </option>
          ))}
        </select>
      </label>
      <p>types no catálogo: {(types.data ?? []).map((t) => t.code).join(', ') || '(vazio)'}</p>
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
        idSolicitanteOnBehalf (opcional — request.internal_open)
        <input value={onBehalf} onChange={(e) => setOnBehalf(e.target.value)} />
      </label>
      {schema ? (
        <DynamicForm schema={schema} value={dados} onChange={setDados} idCurso={idCurso} />
      ) : code ? (
        <p>carregando schema de {code}</p>
      ) : null}
      {extraCats.length ? (
        extraCats.map((c) => (
          <AttachmentUpload
            key={c}
            requestId={draftId}
            categoria={c}
            onReady={(att) =>
              setPendingAtts((prev) => [...prev.filter((x) => x.categoria !== att.categoria), att])
            }
          />
        ))
      ) : (
        <AttachmentUpload
          requestId={draftId}
          categoria="OUTRO"
          onReady={(att) => setPendingAtts((prev) => [...prev, att])}
        />
      )}
      <p>anexos pendentes (inline no POST): {pendingAtts.map((a) => a.categoria).join(', ') || 'nenhum'}</p>
      <fieldset>
        <legend>modo</legend>
        <label>
          <input
            type="radio"
            name="mode"
            checked={mode === 'open'}
            onChange={() => setMode('open')}
          />
          abrir já POST /requests
        </label>
        <label>
          <input
            type="radio"
            name="mode"
            checked={mode === 'draft'}
            onChange={() => setMode('draft')}
          />
          rascunho POST /requests/draft
        </label>
      </fieldset>
      <button type="button" disabled={save.isPending || !code} onClick={() => save.mutate()}>
        {mode === 'draft' ? (draftId ? 'PATCH draft' : 'POST draft') : 'POST /requests'}
      </button>
      <JsonPanel data={save.error ?? save.data ?? type.data ?? types.data} />
    </Page>
  )
}
