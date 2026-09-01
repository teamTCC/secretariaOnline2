import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { AttachmentUpload } from '../../shared/ui/AttachmentUpload'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

const CATEGORIAS = [
  'PALESTRA',
  'EXTENSAO',
  'PESQUISA',
  'MONITORIA',
  'PUBLICACAO',
  'EVENTO',
  'EMPRESA_JUNIOR',
  'ESTAGIO_NAO_OBRIGATORIO',
  'REPRESENTACAO_DISCENTE',
  'OUTROS',
]

type Row = {
  id: string
  titulo?: string
  categoria?: string
  cargaHoraria?: number
  estado?: string
  dataRealizacao?: string
}

type Envelope = { content?: Row[]; page?: { number: number; totalPages: number } }

export function FormativasPage() {
  const qc = useQueryClient()
  const [titulo, setTitulo] = useState('Palestra: Machine Learning Aplicado')
  const [descricao, setDescricao] = useState('Participação na palestra promovida pelo DINF')
  const [categoria, setCategoria] = useState('PALESTRA')
  const [cargaHoraria, setCargaHoraria] = useState('4')
  const [dataRealizacao, setDataRealizacao] = useState('2026-06-15')
  const [storageKey, setStorageKey] = useState('')
  const [created, setCreated] = useState<unknown>()

  const list = useQuery({
    queryKey: queryKeys.formativas(0),
    queryFn: () => api<Envelope>('/formativas/minhas?page=0&size=20'),
  })
  const resumo = useQuery({
    queryKey: queryKeys.formativasResumo,
    queryFn: () => api('/formativas/resumo'),
  })

  const submit = useMutation({
    mutationFn: () =>
      api('/formativas', {
        method: 'POST',
        body: {
          titulo,
          descricao,
          categoria,
          cargaHoraria: Number(cargaHoraria),
          dataRealizacao,
          storageKeyComprovante: storageKey || null,
        },
      }),
    onSuccess: (d) => {
      setCreated(d)
      void qc.invalidateQueries({ queryKey: ['formativas'] })
    },
  })

  return (
    <Page title="formativas">
      <p>GET /formativas/minhas · POST /formativas · presign comprovante MinIO</p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          submit.mutate()
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
          categoria
          <select value={categoria} onChange={(e) => setCategoria(e.target.value)}>
            {CATEGORIAS.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </label>
        <label>
          cargaHoraria
          <input type="number" step="0.5" value={cargaHoraria} onChange={(e) => setCargaHoraria(e.target.value)} />
        </label>
        <label>
          dataRealizacao
          <input type="date" value={dataRealizacao} onChange={(e) => setDataRealizacao(e.target.value)} />
        </label>
        <AttachmentUpload
          categoria="COMPROVANTE"
          presignPath="/formativas/comprovantes/presigned-url"
          onReady={(att) => setStorageKey(att.storageKey)}
        />
        <label>
          storageKeyComprovante
          <input value={storageKey} onChange={(e) => setStorageKey(e.target.value)} />
        </label>
        <button type="submit" disabled={submit.isPending}>
          POST /formativas
        </button>
      </form>
      {(list.isPending || resumo.isPending || submit.isPending) && <p>carregando</p>}
      <ProblemBanner problem={isProblem(submit.error) ? submit.error : isProblem(list.error) ? list.error : null} />
      <table>
        <thead>
          <tr>
            <th>id</th>
            <th>titulo</th>
            <th>categoria</th>
            <th>ch</th>
            <th>estado</th>
          </tr>
        </thead>
        <tbody>
          {(list.data?.content ?? []).map((r) => (
            <tr key={r.id}>
              <td>{r.id}</td>
              <td>{r.titulo}</td>
              <td>{r.categoria}</td>
              <td>{r.cargaHoraria}</td>
              <td>{r.estado}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <h2>created</h2>
      <JsonPanel data={created} />
      <h2>resumo</h2>
      <JsonPanel data={resumo.error ?? resumo.data} />
      <h2>minhas</h2>
      <JsonPanel data={list.error ?? list.data} />
    </Page>
  )
}
