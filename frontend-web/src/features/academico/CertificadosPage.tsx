import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { normalizeLinks } from '../../shared/api/hateoas'
import { isProblem, type Problem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { HateoasBar } from '../../shared/ui/HateoasBar'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Cert = {
  id: string
  idEvento?: string | null
  origem?: string
  hashSha256?: string
  chCreditadas?: number
  issuedAt?: string
  _links?: unknown
}

export function CertificadosPage() {
  const [foreignId, setForeignId] = useState('00000000-0000-7000-8000-000000000001')
  const [download, setDownload] = useState<unknown>()
  const [idor, setIdor] = useState<unknown>()
  const [verify, setVerify] = useState<unknown>()
  const [lastProblem, setLastProblem] = useState<Problem | null>(null)

  const mine = useQuery({
    queryKey: queryKeys.certificados,
    queryFn: () => api<Cert[]>('/certificates/mine'),
  })

  const getDownload = useMutation({
    mutationFn: (href: string) => api<{ downloadUrl?: string }>(href),
    onSuccess: (d) => {
      setLastProblem(null)
      setDownload(d)
    },
    onError: (e) => {
      setLastProblem(isProblem(e) ? e : null)
      setDownload(e)
    },
  })

  const tryIdor = useMutation({
    mutationFn: (id: string) => api(`/certificates/${id}/download-url`),
    onSuccess: (d) => {
      setLastProblem(null)
      setIdor(d)
    },
    onError: (e) => {
      setLastProblem(isProblem(e) ? e : null)
      setIdor(e)
    },
  })

  const doVerify = useMutation({
    mutationFn: (hash: string) => api(`/publico/verificar-certificado/${hash}`),
    onSuccess: (d) => {
      setLastProblem(null)
      setVerify(d)
    },
    onError: (e) => {
      setLastProblem(isProblem(e) ? e : null)
      setVerify(e)
    },
  })

  return (
    <Page title="certificados">
      <p>
        GET /certificates/mine · download-url (IDOR: cert de outro aluno → 403; UUID inexistente → 404) · verify
        público. Ed25519 efêmera: cert de outra JVM → INVALID.
      </p>
      {mine.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(mine.error) ? mine.error : lastProblem} />
      {(mine.data ?? []).map((c) => {
        const links = normalizeLinks(c._links)
        return (
          <fieldset key={c.id}>
            <legend>
              {c.id} {c.origem} {c.hashSha256?.slice(0, 12)}…
            </legend>
            <HateoasBar
              links={links}
              onAction={(rel, href) => {
                if (rel === 'download') getDownload.mutate(href)
                if (rel === 'verify' && c.hashSha256) doVerify.mutate(c.hashSha256)
              }}
            />
            {c.hashSha256 ? (
              <p>
                <Link to={`/publico/verificar-certificado/${c.hashSha256}`}>abrir verify público</Link>
              </p>
            ) : null}
            <JsonPanel data={c} />
          </fieldset>
        )
      })}
      <form
        onSubmit={(e) => {
          e.preventDefault()
          tryIdor.mutate(foreignId)
        }}
      >
        <label>
          tentar id aleatório (IDOR)
          <input value={foreignId} onChange={(e) => setForeignId(e.target.value)} />
        </label>
        <button type="submit">GET download-url alheio</button>
      </form>
      <h2>download próprio</h2>
      <JsonPanel data={download} />
      <h2>IDOR</h2>
      <JsonPanel data={idor} />
      <h2>verify</h2>
      <JsonPanel data={verify} />
      <h2>mine</h2>
      <JsonPanel data={mine.error ?? mine.data} />
    </Page>
  )
}
