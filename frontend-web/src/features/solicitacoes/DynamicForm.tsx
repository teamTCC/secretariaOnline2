import { useQuery } from '@tanstack/react-query'
import { api } from '../../shared/api/client'
import { queryKeys } from '../../shared/api/queryKeys'

export type JsonSchema = {
  type?: string
  title?: string
  description?: string
  enum?: unknown[]
  format?: string
  properties?: Record<string, JsonSchema>
  items?: JsonSchema
  required?: string[]
  minLength?: number
  minItems?: number
  minimum?: number
  maximum?: number
  'x-ui'?: { widget?: string; endpoint?: string; rows?: number }
  'x-required-attachments'?: string[]
}

type Props = {
  schema: JsonSchema
  value: Record<string, unknown>
  onChange: (next: Record<string, unknown>) => void
  idCurso?: string
}

type Entity = { id?: string; codigo?: string; nome?: string }

function widgetOf(s: JsonSchema): string {
  const w = s['x-ui']?.widget
  if (w) return w
  if (s.enum) return 'select'
  if (s.format === 'date') return 'date-picker'
  if (s.type === 'boolean') return 'boolean'
  if (s.type === 'number' || s.type === 'integer') return 'number'
  if (s.type === 'array') return 'multi-select-table'
  return 'string'
}

function requiredAttachments(schema: JsonSchema): string[] {
  const raw = schema['x-required-attachments']
  return Array.isArray(raw) ? raw.map(String) : []
}

function lookupItems(data: unknown): Entity[] {
  if (Array.isArray(data)) return data as Entity[]
  if (data && typeof data === 'object' && Array.isArray((data as { content?: unknown }).content)) {
    return (data as { content: Entity[] }).content
  }
  return []
}

function EntitySelect({
  schema,
  value,
  onChange,
  idCurso,
  label,
}: {
  schema: JsonSchema
  value: unknown
  onChange: (v: string) => void
  idCurso?: string
  label: string
}) {
  const endpoint = schema['x-ui']?.endpoint ?? '/academico/disciplinas'
  let path = endpoint
  try {
    const u = new URL(endpoint, 'http://spa.local')
    if (idCurso && !u.searchParams.get('idCurso')) u.searchParams.set('idCurso', idCurso)
    path = `${u.pathname}${u.search}`
  } catch {
    path = endpoint
  }
  const q = useQuery({
    queryKey: queryKeys.disciplinas({ endpoint: path, idCurso }),
    queryFn: () => api<unknown>(path),
  })
  const items = lookupItems(q.data)
  const str = typeof value === 'string' ? value : ''
  return (
    <label>
      {label}
      <input
        list={`${label}-dl`}
        value={str}
        onChange={(e) => onChange(e.target.value)}
        placeholder="UUID — ou escolha na lista"
      />
      <datalist id={`${label}-dl`}>
        {items.map((it) => (
          <option key={it.id} value={it.id ?? ''}>
            {it.codigo} — {it.nome}
          </option>
        ))}
      </datalist>
    </label>
  )
}

function Field({
  name,
  schema,
  value,
  onChange,
  required,
  idCurso,
}: {
  name: string
  schema: JsonSchema
  value: unknown
  onChange: (v: unknown) => void
  required: boolean
  idCurso?: string
}) {
  const label = `${schema.title ?? name}${required ? ' *' : ''}`
  const widget = widgetOf(schema)

  if (widget === 'select') {
    const opts = (schema.enum ?? []).map(String)
    return (
      <label>
        {label}
        <select value={typeof value === 'string' ? value : ''} onChange={(e) => onChange(e.target.value)}>
          <option value="">—</option>
          {opts.map((o) => (
            <option key={o} value={o}>
              {o}
            </option>
          ))}
        </select>
      </label>
    )
  }

  if (widget === 'textarea') {
    return (
      <label>
        {label}
        <textarea
          rows={schema['x-ui']?.rows ?? 4}
          value={typeof value === 'string' ? value : ''}
          onChange={(e) => onChange(e.target.value)}
        />
      </label>
    )
  }

  if (widget === 'date-picker') {
    return (
      <label>
        {label}
        <input
          type="date"
          value={typeof value === 'string' ? value : ''}
          onChange={(e) => onChange(e.target.value)}
        />
      </label>
    )
  }

  if (widget === 'boolean') {
    return (
      <label>
        <input
          type="checkbox"
          checked={Boolean(value)}
          onChange={(e) => onChange(e.target.checked)}
        />
        {label}
      </label>
    )
  }

  if (widget === 'number') {
    return (
      <label>
        {label}
        <input
          type="number"
          step={schema.type === 'integer' ? 1 : 'any'}
          value={typeof value === 'number' ? value : ''}
          onChange={(e) => {
            const n = e.target.value
            onChange(n === '' ? undefined : Number(n))
          }}
        />
      </label>
    )
  }

  if (widget === 'entity-select') {
    return (
      <EntitySelect
        schema={schema}
        value={value}
        onChange={onChange}
        idCurso={idCurso}
        label={label}
      />
    )
  }

  if (widget === 'multi-select-table') {
    const rows = Array.isArray(value) ? (value as Record<string, unknown>[]) : []
    const itemSchema = schema.items ?? { type: 'object', properties: {} }
    const cols = itemSchema.properties ?? {}
    const req = new Set(itemSchema.required ?? [])
    return (
      <fieldset>
        <legend>{label}</legend>
        <table>
          <thead>
            <tr>
              {Object.keys(cols).map((c) => (
                <th key={c}>{cols[c]?.title ?? c}</th>
              ))}
              <th />
            </tr>
          </thead>
          <tbody>
            {rows.map((row, i) => (
              <tr key={i}>
                {Object.entries(cols).map(([c, cs]) => (
                  <td key={c}>
                    <Field
                      name={c}
                      schema={cs}
                      value={row[c]}
                      required={req.has(c)}
                      idCurso={idCurso}
                      onChange={(v) => {
                        const next = rows.map((r, j) => (j === i ? { ...r, [c]: v } : r))
                        onChange(next)
                      }}
                    />
                  </td>
                ))}
                <td>
                  <button
                    type="button"
                    onClick={() => onChange(rows.filter((_, j) => j !== i))}
                  >
                    remover
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <button
          type="button"
          onClick={() => onChange([...rows, {}])}
        >
          adicionar linha
        </button>
      </fieldset>
    )
  }

  if (widget !== 'string') {
    return (
      <label>
        {label} (JSON)
        <textarea
          rows={4}
          value={value == null ? '' : JSON.stringify(value, null, 2)}
          onChange={(e) => {
            const t = e.target.value
            if (!t) {
              onChange(undefined)
              return
            }
            try {
              onChange(JSON.parse(t) as unknown)
            } catch {
              onChange(t)
            }
          }}
        />
      </label>
    )
  }

  return (
    <label>
      {label}
      <input
        value={typeof value === 'string' ? value : value == null ? '' : String(value)}
        onChange={(e) => onChange(e.target.value)}
      />
    </label>
  )
}

export function DynamicForm({ schema, value, onChange, idCurso }: Props) {
  const props = schema.properties ?? {}
  const req = new Set(schema.required ?? [])
  const cats = requiredAttachments(schema)
  return (
    <div>
      {cats.length ? <p>anexos obrigatórios (UX): {cats.join(', ')}</p> : null}
      {Object.entries(props).map(([name, field]) => (
        <Field
          key={name}
          name={name}
          schema={field}
          value={value[name]}
          required={req.has(name)}
          idCurso={idCurso}
          onChange={(v) => {
            const next = { ...value, [name]: v }
            if (v === undefined) delete next[name]
            onChange(next)
          }}
        />
      ))}
    </div>
  )
}

export { requiredAttachments }
