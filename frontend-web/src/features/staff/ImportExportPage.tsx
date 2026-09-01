import { useMutation, useQuery } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { api, BASE } from '../../shared/api/client'
import { isProblem, type Problem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Job = { jobId?: string; id?: string; status?: string; kind?: string; _links?: unknown }

function jobIdOf(d: Job | undefined) {
  return d?.jobId ?? d?.id
}

export function ImportExportPage() {
  const loc = useLocation()
  const isExport = loc.pathname.startsWith('/export')
  const [kind, setKind] = useState('alunos')
  const [importJobId, setImportJobId] = useState('')
  const [exportJobId, setExportJobId] = useState('')
  const [templateText, setTemplate] = useState('')
  const [last, setLast] = useState<unknown>()
  const fileRef = useRef<HTMLInputElement>(null)

  const importJob = useQuery({
    queryKey: queryKeys.importJob(importJobId),
    queryFn: () => api<Job>(`/imports/${importJobId}`),
    enabled: Boolean(importJobId),
  })

  const exportJob = useQuery({
    queryKey: queryKeys.exportJob(exportJobId),
    queryFn: () => api<Job>(`/exports/${exportJobId}`),
    enabled: Boolean(exportJobId),
    refetchInterval: (q) => (q.state.data?.status === 'PROCESSANDO' ? 2000 : false),
  })

  const exportsList = useQuery({
    queryKey: queryKeys.exportsList,
    queryFn: () => api('/exports'),
    enabled: isExport,
  })

  const upload = useMutation({
    mutationFn: () => {
      const file = fileRef.current?.files?.[0]
      if (!file) {
        throw {
          type: 'https://secretariaonline.ufpr.br/errors/validation-error',
          title: 'CSV ausente',
          status: 400,
          detail: 'Selecione um arquivo no input file',
        } satisfies Problem
      }
      const fd = new FormData()
      fd.append('file', file)
      return api<Job>(`/imports/${kind}`, { method: 'POST', body: fd })
    },
    onSuccess: (d) => {
      setLast(d)
      const id = jobIdOf(d)
      if (id) setImportJobId(id)
    },
    onError: setLast,
  })

  const confirm = useMutation({
    mutationFn: () => api(`/imports/${importJobId}/confirm`, { method: 'POST' }),
    onSuccess: (d) => {
      setLast(d)
      void importJob.refetch()
    },
    onError: setLast,
  })

  const requestExport = useMutation({
    mutationFn: () => api<Job>(`/exports/${kind}`, { method: 'POST' }),
    onSuccess: (d) => {
      setLast(d)
      const id = jobIdOf(d)
      if (id) setExportJobId(id)
    },
    onError: setLast,
  })

  const download = useMutation({
    mutationFn: () => api(`/exports/${exportJobId}/download`),
    onSuccess: setLast,
    onError: setLast,
  })

  async function loadTemplate() {
    try {
      const r = await fetch(`${BASE}/imports/templates/${kind}`, { credentials: 'include' })
      const text = await r.text()
      if (!r.ok) {
        try {
          setLast(JSON.parse(text))
        } catch {
          setLast({ status: r.status, body: text })
        }
        return
      }
      setTemplate(text)
      setLast({ template: kind, bytes: text.length })
    } catch (e) {
      setLast(e)
    }
  }

  const problem = [
    importJob.error,
    exportJob.error,
    exportsList.error,
    upload.error,
    confirm.error,
    requestExport.error,
    download.error,
  ]
    .reverse()
    .find((e) => isProblem(e))

  return (
    <Page title={isExport ? 'export CSV' : 'import CSV'}>
      <p>
        Import: GET /imports/templates/:kind · POST /imports/:kind (multipart file) · GET job · POST confirm. Export:
        POST /exports/:kind · poll GET · GET download. Não parsear CSV no React — JsonPanel do job.
      </p>
      <label>
        kind
        <select value={kind} onChange={(e) => setKind(e.target.value)}>
          <option>alunos</option>
          <option>professores</option>
          <option>egressos</option>
          <option>solicitacoes</option>
        </select>
      </label>
      {(upload.isPending || requestExport.isPending || importJob.isFetching || exportJob.isFetching) && (
        <p>carregando</p>
      )}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
      {!isExport ? (
        <>
          <button type="button" onClick={() => void loadTemplate()}>
            GET /imports/templates/{kind}
          </button>
          {templateText ? <pre className="json">{templateText}</pre> : null}
          <input ref={fileRef} type="file" accept=".csv,text/csv" />
          <button type="button" disabled={upload.isPending} onClick={() => upload.mutate()}>
            POST /imports/{kind}
          </button>
          <label>
            importJobId
            <input value={importJobId} onChange={(e) => setImportJobId(e.target.value)} />
          </label>
          <button type="button" disabled={!importJobId || confirm.isPending} onClick={() => confirm.mutate()}>
            POST confirm
          </button>
          <h2>import job</h2>
          <JsonPanel data={importJob.error ?? importJob.data} />
        </>
      ) : (
        <>
          <button type="button" disabled={requestExport.isPending} onClick={() => requestExport.mutate()}>
            POST /exports/{kind}
          </button>
          <label>
            exportJobId
            <input value={exportJobId} onChange={(e) => setExportJobId(e.target.value)} />
          </label>
          <button type="button" disabled={!exportJobId || download.isPending} onClick={() => download.mutate()}>
            GET download
          </button>
          <h2>export job</h2>
          <JsonPanel data={exportJob.error ?? exportJob.data} />
          <h2>histórico</h2>
          <JsonPanel data={exportsList.error ?? exportsList.data} />
        </>
      )}
      <h2>última mutação</h2>
      <JsonPanel data={last} />
    </Page>
  )
}
