import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { normalizeLinks } from '../../shared/api/hateoas'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { AttachmentUpload, type AttachmentInput } from '../../shared/ui/AttachmentUpload'
import { HateoasBar } from '../../shared/ui/HateoasBar'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Detail = {
  id: string
  empresa?: string
  cargo?: string
  estado?: string
  _links?: unknown
}

type Doc = { id: string; tipo?: string; storageKey?: string; sha256?: string; nomeOriginal?: string }

export function EstagioDetailPage() {
  const { id = '' } = useParams()
  const qc = useQueryClient()
  const [last, setLast] = useState<unknown>()
  const [cargo, setCargo] = useState('')
  const [carga, setCarga] = useState('')
  const [fim, setFim] = useState('')
  const [obs, setObs] = useState('')

  const detail = useQuery({
    queryKey: queryKeys.internship(id),
    queryFn: () => api<Detail>(`/internships/${id}`),
    enabled: Boolean(id),
  })
  const docs = useQuery({
    queryKey: queryKeys.internshipDocs(id),
    queryFn: () => api<Doc[]>(`/internships/${id}/documents`),
    enabled: Boolean(id),
  })

  const links = normalizeLinks(detail.data?._links)

  function invalidate() {
    void qc.invalidateQueries({ queryKey: queryKeys.internship(id) })
    void qc.invalidateQueries({ queryKey: queryKeys.internshipDocs(id) })
    void qc.invalidateQueries({ queryKey: ['internships', 'mine'] })
  }

  const conclude = useMutation({
    mutationFn: (href: string) => api(href, { method: 'POST' }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: (e) => setLast(e),
  })

  const patch = useMutation({
    mutationFn: () =>
      api(`/internships/${id}`, {
        method: 'PATCH',
        body: {
          cargo: cargo || null,
          cargaHorariaSemanal: carga ? Number(carga) : null,
          fim: fim || null,
          observacoes: obs || null,
        },
      }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: (e) => setLast(e),
  })

  const forceConclude = useMutation({
    mutationFn: () => api(`/internships/${id}/conclude`, { method: 'POST' }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: (e) => setLast(e),
  })

  function onHateoas(rel: string, href: string) {
    if (rel === 'conclude') {
      conclude.mutate(href)
      return
    }
    if (rel === 'documents') {
      void docs.refetch()
      return
    }
    if (rel === 'update') {
      patch.mutate()
    }
  }

  const problem = [detail.error, docs.error, conclude.error, patch.error, forceConclude.error]
    .reverse()
    .find((e) => isProblem(e))

  return (
    <Page title={`estagio ${id}`}>
      <p>
        <Link to="/estagios">lista mine</Link> · GET /internships/{id} · docs MinIO · conclude só se _links
      </p>
      {detail.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
      <p>
        {detail.data?.empresa} — {detail.data?.estado}
      </p>
      <HateoasBar links={links} onAction={onHateoas} />
      {links.update ? (
        <form
          onSubmit={(e) => {
            e.preventDefault()
            patch.mutate()
          }}
        >
          <p>PATCH (supervisor/COE — _links.update)</p>
          <label>
            cargo
            <input value={cargo} onChange={(e) => setCargo(e.target.value)} />
          </label>
          <label>
            cargaHorariaSemanal
            <input type="number" value={carga} onChange={(e) => setCarga(e.target.value)} />
          </label>
          <label>
            fim
            <input type="date" value={fim} onChange={(e) => setFim(e.target.value)} />
          </label>
          <label>
            observacoes
            <textarea value={obs} onChange={(e) => setObs(e.target.value)} />
          </label>
          <button type="submit" disabled={patch.isPending}>
            PATCH /internships/{id}
          </button>
        </form>
      ) : null}
      <p>harness FGAC: POST conclude URL crua (aluno → 403; COE → 200)</p>
      <button type="button" disabled={forceConclude.isPending} onClick={() => forceConclude.mutate()}>
        POST /internships/{id}/conclude
      </button>
      <p>documentos GET /internships/{id}/documents</p>
      <ul>
        {(docs.data ?? []).map((d) => (
          <li key={d.id}>
            {d.tipo} {d.nomeOriginal ?? d.storageKey} {d.sha256?.slice(0, 12)}
          </li>
        ))}
      </ul>
      <AttachmentUpload
        categoria="CONTRATO"
        presignPath={`/internships/${id}/documents/upload-url`}
        confirmPath={`/internships/${id}/documents`}
        buildPresignBody={({ file, categoria }) => ({
          tipo: categoria,
          nomeOriginal: file.name,
          contentType: file.type || 'application/pdf',
        })}
        buildConfirmBody={(att: AttachmentInput) => ({
          tipo: att.categoria,
          storageKey: att.storageKey,
          sha256: att.sha256,
          nomeOriginal: att.nomeOriginal,
        })}
        onReady={() => invalidate()}
      />
      <h2>last</h2>
      <JsonPanel data={last} />
      <h2>detail</h2>
      <JsonPanel data={detail.error ?? detail.data} />
      <h2>documents</h2>
      <JsonPanel data={docs.error ?? docs.data} />
    </Page>
  )
}
