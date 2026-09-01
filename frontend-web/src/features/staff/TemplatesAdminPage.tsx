import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Row = { id: string; codigo?: string; titulo?: string; canal?: string; versao?: number }

export function TemplatesAdminPage() {
  const qc = useQueryClient()
  const [codigo, setCodigo] = useState('fatia7.teste.deferido')
  const [titulo, setTitulo] = useState('Solicitação deferida (teste)')
  const [assunto, setAssunto] = useState('Sua solicitação {{protocolo}} foi deferida')
  const [corpo, setCorpo] = useState('Olá {{nome}}, o estado novo é {{estadoNovo}}.')
  const [canal, setCanal] = useState('EMAIL')
  const [id, setId] = useState('')
  const [last, setLast] = useState<unknown>()

  const list = useQuery({
    queryKey: queryKeys.templates,
    queryFn: () => api<Row[]>('/communication-templates'),
  })
  const versions = useQuery({
    queryKey: queryKeys.templateVersions(id),
    queryFn: () => api(`/communication-templates/${id}/versions`),
    enabled: Boolean(id),
  })

  const create = useMutation({
    mutationFn: () =>
      api<{ id: string }>('/communication-templates', {
        method: 'POST',
        body: { codigo, titulo, assunto, corpo, canal },
      }),
    onSuccess: (d) => {
      setLast(d)
      if (d.id) setId(d.id)
      void qc.invalidateQueries({ queryKey: queryKeys.templates })
    },
    onError: setLast,
  })

  const revise = useMutation({
    mutationFn: () =>
      api(`/communication-templates/${id}/revisions`, {
        method: 'POST',
        body: { assunto, corpo },
      }),
    onSuccess: (d) => {
      setLast(d)
      void qc.invalidateQueries({ queryKey: ['communication-templates'] })
    },
    onError: setLast,
  })

  const problem = [list.error, versions.error, create.error, revise.error].reverse().find((e) => isProblem(e))

  return (
    <Page title="communication-templates">
      <p>GET /communication-templates (path real) · POST · POST :id/revisions · GET versions. T-F7-004.</p>
      {list.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
      <form
        onSubmit={(e) => {
          e.preventDefault()
          create.mutate()
        }}
      >
        <label>
          codigo
          <input value={codigo} onChange={(e) => setCodigo(e.target.value)} />
        </label>
        <label>
          titulo
          <input value={titulo} onChange={(e) => setTitulo(e.target.value)} />
        </label>
        <label>
          assunto
          <input value={assunto} onChange={(e) => setAssunto(e.target.value)} />
        </label>
        <label>
          corpo
          <textarea value={corpo} onChange={(e) => setCorpo(e.target.value)} />
        </label>
        <label>
          canal
          <input value={canal} onChange={(e) => setCanal(e.target.value)} />
        </label>
        <button type="submit">POST /communication-templates</button>
      </form>
      <table>
        <thead>
          <tr>
            <th>codigo</th>
            <th>id</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {(list.data ?? []).map((t) => (
            <tr key={t.id}>
              <td>{t.codigo}</td>
              <td>{t.id}</td>
              <td>
                <button type="button" onClick={() => setId(t.id)}>
                  versões
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <label>
        templateId
        <input value={id} onChange={(e) => setId(e.target.value)} />
      </label>
      <button type="button" disabled={!id || revise.isPending} onClick={() => revise.mutate()}>
        POST revisions
      </button>
      <h2>última mutação</h2>
      <JsonPanel data={last} />
      <h2>versions</h2>
      <JsonPanel data={versions.error ?? versions.data} />
      <h2>lista</h2>
      <JsonPanel data={list.error ?? list.data} />
    </Page>
  )
}
