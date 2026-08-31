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

export function ProtocoloPage() {
  const { ano: anoParam, numero: numeroParam } = useParams()
  const nav = useNavigate()
  const [ano, setAno] = useState(anoParam ?? '2026')
  const [numero, setNumero] = useState(numeroParam ?? '42')
  const ready = Boolean(anoParam && numeroParam)

  useEffect(() => {
    if (anoParam) setAno(anoParam)
    if (numeroParam) setNumero(numeroParam)
  }, [anoParam, numeroParam])

  const q = useQuery({
    queryKey: queryKeys.protocolo(anoParam ?? '', numeroParam ?? ''),
    queryFn: () => api(`/publico/solicitacoes/${anoParam}/${numeroParam}`),
    enabled: ready,
  })

  return (
    <Page title="protocolo público">
      <p>
        <Link to="/login">login</Link>
      </p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          nav(`/publico/solicitacoes/${ano}/${numero}`)
        }}
      >
        <label>
          ano
          <input name="ano" value={ano} onChange={(e) => setAno(e.target.value)} />
        </label>
        <label>
          numero (sem zero à esquerda)
          <input name="numero" value={numero} onChange={(e) => setNumero(e.target.value)} />
        </label>
        <button type="submit">GET</button>
      </form>
      {q.isPending && ready && <p>carregando</p>}
      <ProblemBanner problem={isProblem(q.error) ? q.error : null} />
      <HateoasBar links={normalizeLinks((q.data as { _links?: unknown } | undefined)?._links)} />
      <JsonPanel data={q.error ?? q.data} />
    </Page>
  )
}
