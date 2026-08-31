import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { normalizeLinks } from '../../shared/api/hateoas'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { readSessionFlags } from '../../shared/auth/session'
import { HateoasBar } from '../../shared/ui/HateoasBar'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

export function MeRawPage() {
  const q = useQuery({
    queryKey: queryKeys.me,
    queryFn: () => api<{ _links?: unknown }>('/me'),
  })

  return (
    <Page title="me-raw">
      <p>
        <Link to="/login">login</Link>
        {' · '}
        <Link to="/contato">contato</Link>
      </p>
      {q.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(q.error) ? q.error : null} />
      <HateoasBar links={normalizeLinks(q.data?._links)} />
      <p>session flags</p>
      <JsonPanel data={readSessionFlags()} />
      <JsonPanel data={q.error ?? q.data} />
    </Page>
  )
}
