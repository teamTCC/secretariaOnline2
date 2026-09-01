import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type TypeRow = {
  id: string
  code: string
  descricao?: string
  ativo?: boolean
  prazoDias?: number
  formSchema?: unknown
  workflowJson?: unknown
}

const DEFAULT_SCHEMA = `{
  "type": "object",
  "properties": {
    "finalidade": {
      "type": "string",
      "title": "Finalidade",
      "enum": ["BOLSA", "CONVENIO", "OUTRO"]
    }
  },
  "required": ["finalidade"]
}`

const DEFAULT_WF = `{
  "initial": "ABERTA",
  "states": ["RASCUNHO", "ABERTA", "EM_TRIAGEM", "DEFERIDA", "ARQUIVADA"],
  "transitions": [
    { "from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"] },
    { "from": "EM_TRIAGEM", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"] }
  ]
}`

export function RequestTypesAdminPage() {
  const qc = useQueryClient()
  const [code, setCode] = useState(`FATIA7_${Date.now().toString(36).toUpperCase()}`)
  const [descricao, setDescricao] = useState('Tipo rascunho fatia 7')
  const [prazoDias, setPrazo] = useState('5')
  const [formSchema, setSchema] = useState(DEFAULT_SCHEMA)
  const [workflowJson, setWf] = useState(DEFAULT_WF)
  const [selectedId, setSelected] = useState('')
  const [last, setLast] = useState<unknown>()

  const list = useQuery({
    queryKey: queryKeys.adminRequestTypes,
    queryFn: () => api<TypeRow[]>('/request-types'),
  })

  function parseJson(label: string, raw: string): unknown {
    try {
      return JSON.parse(raw)
    } catch {
      throw {
        type: 'https://secretariaonline.ufpr.br/errors/validation-error',
        title: `JSON inválido: ${label}`,
        status: 400,
        detail: 'Corrija o textarea antes de POST/PATCH',
      }
    }
  }

  function invalidate() {
    void qc.invalidateQueries({ queryKey: queryKeys.adminRequestTypes })
    void qc.invalidateQueries({ queryKey: queryKeys.requestTypes })
  }

  const create = useMutation({
    mutationFn: () =>
      api<TypeRow>('/request-types', {
        method: 'POST',
        body: {
          code,
          descricao,
          prazoDias: Number(prazoDias),
          formSchema: parseJson('formSchema', formSchema),
          workflowJson: parseJson('workflowJson', workflowJson),
        },
      }),
    onSuccess: (d) => {
      setLast(d)
      if (d.id) setSelected(d.id)
      invalidate()
    },
    onError: setLast,
  })

  const patch = useMutation({
    mutationFn: () =>
      api(`/request-types/${selectedId}`, {
        method: 'PATCH',
        body: {
          code,
          descricao,
          prazoDias: Number(prazoDias),
          formSchema: parseJson('formSchema', formSchema),
          workflowJson: parseJson('workflowJson', workflowJson),
        },
      }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: setLast,
  })

  const publish = useMutation({
    mutationFn: () => api(`/request-types/${selectedId}/publish`, { method: 'POST' }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: setLast,
  })

  const del = useMutation({
    mutationFn: () => api(`/request-types/${selectedId}`, { method: 'DELETE' }),
    onSuccess: (d) => {
      setLast(d ?? { deleted: selectedId })
      invalidate()
    },
    onError: setLast,
  })

  const alunoTypes = useQuery({
    queryKey: queryKeys.requestTypes,
    queryFn: () => api<TypeRow[]>('/requests/types'),
  })

  const problem = [list.error, create.error, patch.error, publish.error, del.error]
    .reverse()
    .find((e) => isProblem(e))

  return (
    <Page title="admin request-types">
      <p>
        GET/POST /request-types · PATCH · POST :id/publish (V019 snapshot) · DELETE. Aluno GET /requests/types só
        ativo=true. Textarea JSON — sem editor visual.
      </p>
      {list.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
      <table>
        <thead>
          <tr>
            <th>code</th>
            <th>ativo</th>
            <th>id</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {(list.data ?? []).map((t) => (
            <tr key={t.id}>
              <td>{t.code}</td>
              <td>{String(t.ativo)}</td>
              <td>{t.id}</td>
              <td>
                <button
                  type="button"
                  onClick={() => {
                    setSelected(t.id)
                    setCode(t.code)
                    setDescricao(t.descricao ?? '')
                    setPrazo(String(t.prazoDias ?? 10))
                    setSchema(JSON.stringify(t.formSchema ?? {}, null, 2))
                    setWf(JSON.stringify(t.workflowJson ?? {}, null, 2))
                  }}
                >
                  usar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <label>
        id
        <input value={selectedId} onChange={(e) => setSelected(e.target.value)} />
      </label>
      <label>
        code
        <input value={code} onChange={(e) => setCode(e.target.value)} />
      </label>
      <label>
        descricao
        <input value={descricao} onChange={(e) => setDescricao(e.target.value)} />
      </label>
      <label>
        prazoDias
        <input value={prazoDias} onChange={(e) => setPrazo(e.target.value)} />
      </label>
      <label>
        formSchema
        <textarea rows={10} value={formSchema} onChange={(e) => setSchema(e.target.value)} />
      </label>
      <label>
        workflowJson
        <textarea rows={10} value={workflowJson} onChange={(e) => setWf(e.target.value)} />
      </label>
      <div className="row">
        <button type="button" disabled={create.isPending} onClick={() => create.mutate()}>
          POST rascunho
        </button>
        <button type="button" disabled={!selectedId || patch.isPending} onClick={() => patch.mutate()}>
          PATCH
        </button>
        <button type="button" disabled={!selectedId || publish.isPending} onClick={() => publish.mutate()}>
          POST publish
        </button>
        <button type="button" disabled={!selectedId || del.isPending} onClick={() => del.mutate()}>
          DELETE
        </button>
      </div>
      <p>GET /requests/types (catálogo aluno, ativo): {(alunoTypes.data ?? []).map((t) => t.code).join(', ')}</p>
      <h2>última mutação</h2>
      <JsonPanel data={last} />
      <h2>admin list</h2>
      <JsonPanel data={list.error ?? list.data} />
    </Page>
  )
}
