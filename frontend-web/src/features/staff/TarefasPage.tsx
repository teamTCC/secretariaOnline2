import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Row = { id: string; titulo?: string; estado?: string; prioridade?: string; prazoEm?: string }
type Envelope = { content?: Row[]; page?: { number: number } }

export function TarefasPage() {
  const qc = useQueryClient()
  const [estado, setEstado] = useState('')
  const [titulo, setTitulo] = useState('Conferir diplomas — lote julho')
  const [prioridade, setPrioridade] = useState('ALTA')
  const [prazoEm, setPrazo] = useState('2026-09-25T17:00:00Z')
  const [pasteId, setPasteId] = useState('')
  const [moveTo, setMoveTo] = useState('EM_ANDAMENTO')
  const [last, setLast] = useState<unknown>()

  const list = useQuery({
    queryKey: queryKeys.tasks(estado),
    queryFn: () => api<Envelope>(`/tasks${estado ? `?estado=${encodeURIComponent(estado)}` : ''}`),
  })

  function invalidate() {
    void qc.invalidateQueries({ queryKey: ['tasks'] })
  }

  const create = useMutation({
    mutationFn: () => api('/tasks', { method: 'POST', body: { titulo, prioridade, prazoEm } }),
    onSuccess: (d) => {
      setLast(d)
      const id = (d as { id?: string }).id
      if (id) setPasteId(id)
      invalidate()
    },
    onError: setLast,
  })

  const patch = useMutation({
    mutationFn: () => api(`/tasks/${pasteId}`, { method: 'PATCH', body: { estado: moveTo } }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: setLast,
  })

  const del = useMutation({
    mutationFn: () => api(`/tasks/${pasteId}`, { method: 'DELETE' }),
    onSuccess: (d) => {
      setLast(d ?? { deleted: pasteId })
      invalidate()
    },
    onError: setLast,
  })

  const problem = [list.error, create.error, patch.error, del.error].reverse().find((e) => isProblem(e))

  return (
    <Page title="tarefas">
      <p>GET /tasks (não /tarefas) · POST · PATCH coluna · DELETE só PENDENTE. task.manage.</p>
      <label>
        filtro estado
        <input value={estado} onChange={(e) => setEstado(e.target.value)} placeholder="PENDENTE" />
      </label>
      {(list.isPending || create.isPending || patch.isPending || del.isPending) && <p>carregando</p>}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
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
          prioridade
          <input value={prioridade} onChange={(e) => setPrioridade(e.target.value)} />
        </label>
        <label>
          prazoEm
          <input value={prazoEm} onChange={(e) => setPrazo(e.target.value)} />
        </label>
        <button type="submit">POST /tasks</button>
      </form>
      <table>
        <thead>
          <tr>
            <th>id</th>
            <th>titulo</th>
            <th>estado</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {(list.data?.content ?? []).map((r) => (
            <tr key={r.id}>
              <td>{r.id}</td>
              <td>{r.titulo}</td>
              <td>{r.estado}</td>
              <td>
                <button type="button" onClick={() => setPasteId(r.id)}>
                  usar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <label>
        taskId
        <input value={pasteId} onChange={(e) => setPasteId(e.target.value)} />
      </label>
      <label>
        PATCH estado
        <select value={moveTo} onChange={(e) => setMoveTo(e.target.value)}>
          <option>PENDENTE</option>
          <option>EM_ANDAMENTO</option>
          <option>CONCLUIDA</option>
        </select>
      </label>
      <div className="row">
        <button type="button" disabled={!pasteId || patch.isPending} onClick={() => patch.mutate()}>
          PATCH /tasks/:id
        </button>
        <button type="button" disabled={!pasteId || del.isPending} onClick={() => del.mutate()}>
          DELETE /tasks/:id
        </button>
      </div>
      <h2>última mutação</h2>
      <JsonPanel data={last} />
      <h2>lista</h2>
      <JsonPanel data={list.error ?? list.data} />
    </Page>
  )
}
