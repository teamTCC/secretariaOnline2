import { useQuery } from '@tanstack/react-query'
import { api } from '../shared/api/client'
import { isProblem } from '../shared/api/problem'
import { queryKeys } from '../shared/api/queryKeys'
import { JsonPanel } from '../shared/ui/JsonPanel'
import { Page } from '../shared/ui/Page'
import { ProblemBanner } from '../shared/ui/ProblemBanner'

type CsrfResponse = { token: string; headerName: string; parameterName: string }

export function HealthPage() {
  const q = useQuery({
    queryKey: queryKeys.csrf,
    queryFn: () => api<CsrfResponse>('/auth/csrf'),
    enabled: false,
  })

  return (
    <Page title="health-front">
      <div className="row">
        <button type="button" onClick={() => void q.refetch()}>
          GET /auth/csrf
        </button>
      </div>
      {q.isFetching && <p>carregando</p>}
      <ProblemBanner problem={isProblem(q.error) ? q.error : null} />
      <JsonPanel data={q.data} />
    </Page>
  )
}
