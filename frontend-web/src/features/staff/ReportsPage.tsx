import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

export function ReportsPage() {
  const [params, setParams] = useSearchParams()
  const kind = params.get('kind') === 'coordinator' ? 'coordinator' : 'secretary'
  const periodo = params.get('periodo') ?? '2026-2'
  const curso = params.get('curso') ?? 'TADS'

  const report = useQuery({
    queryKey: queryKeys.reports(kind, { periodo, curso }),
    queryFn: () => {
      const qs = new URLSearchParams()
      if (periodo) qs.set('periodo', periodo)
      if (curso) qs.set('curso', curso)
      return api(`/reports/${kind}?${qs}`)
    },
  })

  return (
    <Page title="relatorios">
      <p>
        GET /reports/secretary · GET /reports/coordinator (não RelatoriosController). Query as-built: periodo + curso
        (não from/to). SQL timestamptz. 500 42P18 = regressão.
      </p>
      <div className="row">
        <button
          type="button"
          onClick={() => {
            const next = new URLSearchParams(params)
            next.set('kind', 'secretary')
            setParams(next)
          }}
        >
          secretary
        </button>
        <button
          type="button"
          onClick={() => {
            const next = new URLSearchParams(params)
            next.set('kind', 'coordinator')
            setParams(next)
          }}
        >
          coordinator
        </button>
      </div>
      <label>
        periodo
        <input
          value={periodo}
          onChange={(e) => {
            const next = new URLSearchParams(params)
            next.set('periodo', e.target.value)
            setParams(next)
          }}
        />
      </label>
      <label>
        curso
        <input
          value={curso}
          onChange={(e) => {
            const next = new URLSearchParams(params)
            next.set('curso', e.target.value)
            setParams(next)
          }}
        />
      </label>
      {report.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(report.error) ? report.error : null} />
      <JsonPanel data={report.error ?? report.data} />
    </Page>
  )
}
