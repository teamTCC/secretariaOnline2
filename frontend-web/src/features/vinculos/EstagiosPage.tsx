import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Row = {
  id: string
  empresa?: string
  cargo?: string
  estado?: string
  inicio?: string
  fim?: string
}

type Envelope = { content?: Row[]; page?: { number: number; totalPages: number; totalElements?: number } }

export function EstagiosPage() {
  const qc = useQueryClient()
  const [empresa, setEmpresa] = useState('Empresa XYZ Ltda.')
  const [cargo, setCargo] = useState('Dev Backend')
  const [cargaHorariaSemanal, setCarga] = useState('20')
  const [inicio, setInicio] = useState('2026-03-01')
  const [observacoes, setObs] = useState('Estágio obrigatório TADS')
  const [created, setCreated] = useState<unknown>()

  const list = useQuery({
    queryKey: queryKeys.internshipsMine(0),
    queryFn: () => api<Envelope>('/internships/mine?page=0&size=20'),
  })

  const create = useMutation({
    mutationFn: () =>
      api('/internships', {
        method: 'POST',
        body: {
          empresa,
          cargo,
          cargaHorariaSemanal: Number(cargaHorariaSemanal),
          inicio,
          observacoes: observacoes || null,
        },
      }),
    onSuccess: (d) => {
      setCreated(d)
      void qc.invalidateQueries({ queryKey: ['internships', 'mine'] })
    },
  })

  return (
    <Page title="estagios">
      <p>POST /internships · GET /internships/mine — rota React /estagios, fetch nunca /estagios</p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          create.mutate()
        }}
      >
        <label>
          empresa
          <input value={empresa} onChange={(e) => setEmpresa(e.target.value)} />
        </label>
        <label>
          cargo
          <input value={cargo} onChange={(e) => setCargo(e.target.value)} />
        </label>
        <label>
          cargaHorariaSemanal
          <input type="number" min={1} value={cargaHorariaSemanal} onChange={(e) => setCarga(e.target.value)} />
        </label>
        <label>
          inicio
          <input type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} />
        </label>
        <label>
          observacoes
          <textarea value={observacoes} onChange={(e) => setObs(e.target.value)} />
        </label>
        <button type="submit" disabled={create.isPending}>
          POST /internships
        </button>
      </form>
      {(list.isPending || create.isPending) && <p>carregando</p>}
      <ProblemBanner problem={isProblem(create.error) ? create.error : isProblem(list.error) ? list.error : null} />
      <table>
        <thead>
          <tr>
            <th>id</th>
            <th>empresa</th>
            <th>cargo</th>
            <th>estado</th>
          </tr>
        </thead>
        <tbody>
          {(list.data?.content ?? []).map((r) => (
            <tr key={r.id}>
              <td>
                <Link to={`/estagios/${r.id}`}>{r.id}</Link>
              </td>
              <td>{r.empresa}</td>
              <td>{r.cargo}</td>
              <td>{r.estado}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <h2>created</h2>
      {created && typeof created === 'object' && created !== null && 'id' in created ? (
        <p>
          <Link to={`/estagios/${(created as { id: string }).id}`}>abrir {(created as { id: string }).id}</Link>
        </p>
      ) : null}
      <JsonPanel data={created} />
      <h2>mine</h2>
      <JsonPanel data={list.error ?? list.data} />
    </Page>
  )
}
