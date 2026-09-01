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
  titulo?: string
  estado?: string
  hashSha256Pdf?: string | null
  _links?: unknown
}

export function TccDetailPage() {
  const { id = '' } = useParams()
  const qc = useQueryClient()
  const [last, setLast] = useState<unknown>()
  const [idAluno, setIdAluno] = useState('')
  const [papelAluno, setPapelAluno] = useState('AUTOR')
  const [idProfessor, setIdProfessor] = useState('')
  const [papelBanca, setPapelBanca] = useState('BANCA')
  const [nota, setNota] = useState('9.5')
  const [aprovado, setAprovado] = useState(true)
  const [notaFinal, setNotaFinal] = useState('9.2')
  const [titulo, setTitulo] = useState('')
  const [dataDefesa, setDataDefesa] = useState('')

  const detail = useQuery({
    queryKey: queryKeys.tcc(id),
    queryFn: () => api<Detail>(`/tccs/${id}`),
    enabled: Boolean(id),
  })

  const links = normalizeLinks(detail.data?._links)

  function invalidate() {
    void qc.invalidateQueries({ queryKey: queryKeys.tcc(id) })
    void qc.invalidateQueries({ queryKey: ['tccs'] })
  }

  const mut = useMutation({
    mutationFn: (args: { href: string; method: string; body?: unknown }) =>
      api(args.href, { method: args.method, body: args.body }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: (e) => setLast(e),
  })

  function onHateoas(rel: string, href: string) {
    if (rel === 'submit-final-url') {
      setLast({ hint: 'use AttachmentUpload abaixo', href })
      return
    }
    if (rel === 'add-member') {
      if (!idAluno) {
        setLast({ hint: 'preencha idAluno', href })
        return
      }
      mut.mutate({
        href,
        method: 'POST',
        body: { idAluno, papel: papelAluno },
      })
      return
    }
    if (rel === 'add-examiner') {
      mut.mutate({ href, method: 'POST', body: { idProfessor, papel: papelBanca } })
      return
    }
    if (rel === 'grade') {
      mut.mutate({ href, method: 'PATCH', body: { nota: Number(nota) } })
      return
    }
    if (rel === 'approve') {
      mut.mutate({ href, method: 'PATCH', body: { aprovado, notaFinal: Number(notaFinal) } })
      return
    }
    if (rel === 'update') {
      mut.mutate({
        href,
        method: 'PATCH',
        body: { titulo: titulo || null, dataDefesa: dataDefesa || null },
      })
    }
  }

  const problem = [detail.error, mut.error].reverse().find((e) => isProblem(e))

  return (
    <Page title={`tcc ${id}`}>
      <p>
        <Link to="/tccs">lista</Link> · GET /tccs/{id} · PDF via _links.submit-final-url · banca/membros pelos rels
      </p>
      {detail.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
      <p>
        {detail.data?.titulo} — {detail.data?.estado} pdf={detail.data?.hashSha256Pdf?.slice(0, 12) ?? '(nenhum)'}
      </p>
      <HateoasBar links={links} onAction={onHateoas} />
      {links['add-member'] ? (
        <fieldset>
          <legend>POST members</legend>
          <label>
            idAluno
            <input value={idAluno} onChange={(e) => setIdAluno(e.target.value)} placeholder="uuid do aluno" />
          </label>
          <label>
            papel
            <input value={papelAluno} onChange={(e) => setPapelAluno(e.target.value)} />
          </label>
          <button
            type="button"
            disabled={mut.isPending}
            onClick={() => {
              if (!idAluno) {
                setLast({ hint: 'preencha idAluno' })
                return
              }
              mut.mutate({
                href: links['add-member'],
                method: 'POST',
                body: { idAluno, papel: papelAluno },
              })
            }}
          >
            POST /tccs/{id}/members
          </button>
        </fieldset>
      ) : null}
      {links['add-examiner'] ? (
        <fieldset>
          <legend>POST examiners</legend>
          <label>
            idProfessor
            <input value={idProfessor} onChange={(e) => setIdProfessor(e.target.value)} />
          </label>
          <label>
            papel
            <input value={papelBanca} onChange={(e) => setPapelBanca(e.target.value)} />
          </label>
          <button
            type="button"
            disabled={mut.isPending || !idProfessor}
            onClick={() =>
              mut.mutate({
                href: links['add-examiner'],
                method: 'POST',
                body: { idProfessor, papel: papelBanca },
              })
            }
          >
            POST /tccs/{id}/examiners
          </button>
        </fieldset>
      ) : null}
      {links.grade ? (
        <fieldset>
          <legend>PATCH grade</legend>
          <label>
            nota
            <input type="number" step="0.1" value={nota} onChange={(e) => setNota(e.target.value)} />
          </label>
          <button
            type="button"
            disabled={mut.isPending}
            onClick={() => mut.mutate({ href: links.grade, method: 'PATCH', body: { nota: Number(nota) } })}
          >
            PATCH /tccs/{id}/grade
          </button>
        </fieldset>
      ) : null}
      {links.approve ? (
        <fieldset>
          <legend>PATCH approve</legend>
          <label>
            aprovado
            <input type="checkbox" checked={aprovado} onChange={(e) => setAprovado(e.target.checked)} />
          </label>
          <label>
            notaFinal
            <input type="number" step="0.1" value={notaFinal} onChange={(e) => setNotaFinal(e.target.value)} />
          </label>
          <button
            type="button"
            disabled={mut.isPending}
            onClick={() =>
              mut.mutate({
                href: links.approve,
                method: 'PATCH',
                body: { aprovado, notaFinal: Number(notaFinal) },
              })
            }
          >
            PATCH /tccs/{id}/approve
          </button>
        </fieldset>
      ) : null}
      {links.update ? (
        <fieldset>
          <legend>PATCH update</legend>
          <label>
            titulo
            <input value={titulo} onChange={(e) => setTitulo(e.target.value)} />
          </label>
          <label>
            dataDefesa
            <input type="date" value={dataDefesa} onChange={(e) => setDataDefesa(e.target.value)} />
          </label>
          <button
            type="button"
            disabled={mut.isPending}
            onClick={() =>
              mut.mutate({
                href: links.update,
                method: 'PATCH',
                body: { titulo: titulo || null, dataDefesa: dataDefesa || null },
              })
            }
          >
            PATCH /tccs/{id}
          </button>
        </fieldset>
      ) : null}
      <p>PDF final — POST /tccs/{id}/submit-final/url + PUT MinIO + confirm (tcc.upload_final + membro)</p>
      <AttachmentUpload
        categoria="PDF_FINAL"
        presignPath={`/tccs/${id}/submit-final/url`}
        confirmPath={`/tccs/${id}/submit-final/confirm`}
        buildPresignBody={({ file }) => ({ nomeOriginal: file.name })}
        buildConfirmBody={(att: AttachmentInput) => ({
          storageKey: att.storageKey,
          sha256: att.sha256,
        })}
        onReady={() => invalidate()}
      />
      <h2>last</h2>
      <JsonPanel data={last} />
      <h2>detail</h2>
      <JsonPanel data={detail.error ?? detail.data} />
    </Page>
  )
}
