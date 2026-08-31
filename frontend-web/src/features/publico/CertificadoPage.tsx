import { useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { normalizeLinks } from '../../shared/api/hateoas'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { HateoasBar } from '../../shared/ui/HateoasBar'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

export function CertificadoPage() {
  const { hash: hashParam } = useParams()
  const nav = useNavigate()
  const [hash, setHash] = useState(hashParam ?? '')

  useEffect(() => {
    if (hashParam) setHash(hashParam)
  }, [hashParam])

  const jwks = useQuery({
    queryKey: queryKeys.jwks,
    queryFn: () => api('/.well-known/jwks.json'),
  })

  const cert = useQuery({
    queryKey: queryKeys.certificado(hashParam ?? ''),
    queryFn: () => api(`/publico/verificar-certificado/${hashParam}`),
    enabled: Boolean(hashParam),
  })

  return (
    <Page title="verificar certificado">
      <p>
        <Link to="/login">login</Link>
      </p>
      <p>em dev a chave Ed25519 é efêmera — reinício da JVM invalida certs antigos (INVALID).</p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          if (hash) nav(`/publico/verificar-certificado/${hash}`)
        }}
      >
        <label>
          hashSha256
          <input name="hash" value={hash} onChange={(e) => setHash(e.target.value)} />
        </label>
        <button type="submit">GET certificado</button>
      </form>
      {cert.isPending && hashParam ? <p>carregando</p> : null}
      <ProblemBanner problem={isProblem(cert.error) ? cert.error : isProblem(jwks.error) ? jwks.error : null} />
      <HateoasBar links={normalizeLinks((cert.data as { _links?: unknown } | undefined)?._links)} />
      <h2>certificado</h2>
      <JsonPanel data={cert.error ?? cert.data} />
      <h2>jwks</h2>
      {jwks.isPending && <p>carregando</p>}
      <JsonPanel data={jwks.error ?? jwks.data} />
    </Page>
  )
}
