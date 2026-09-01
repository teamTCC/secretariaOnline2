import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

export function AuditPage() {
  const [params, setParams] = useSearchParams()
  const acao = params.get('acao') ?? ''
  const idAtor = params.get('idAtor') ?? ''
  const page = Number(params.get('page') ?? '0') || 0

  const audit = useQuery({
    queryKey: queryKeys.audit({ acao, idAtor, page }),
    queryFn: () => {
      const qs = new URLSearchParams()
      qs.set('page', String(page))
      qs.set('size', '20')
      if (acao) qs.set('acao', acao)
      if (idAtor) qs.set('idAtor', idAtor)
      return api(`/admin/audit?${qs}`)
    },
  })

  return (
    <Page title="admin audit">
      <p>GET /admin/audit (as-built; não /audit). audit.read. Filtros acao / idAtor.</p>
      <label>
        acao
        <input
          value={acao}
          onChange={(e) => {
            const next = new URLSearchParams(params)
            if (e.target.value) next.set('acao', e.target.value)
            else next.delete('acao')
            next.set('page', '0')
            setParams(next)
          }}
          placeholder="LOGIN_SUCCESS"
        />
      </label>
      <label>
        idAtor
        <input
          value={idAtor}
          onChange={(e) => {
            const next = new URLSearchParams(params)
            if (e.target.value) next.set('idAtor', e.target.value)
            else next.delete('idAtor')
            next.set('page', '0')
            setParams(next)
          }}
        />
      </label>
      {audit.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(audit.error) ? audit.error : null} />
      <div className="row">
        <button
          type="button"
          disabled={page <= 0}
          onClick={() => {
            const next = new URLSearchParams(params)
            next.set('page', String(page - 1))
            setParams(next)
          }}
        >
          prev
        </button>
        <span>page {page}</span>
        <button
          type="button"
          onClick={() => {
            const next = new URLSearchParams(params)
            next.set('page', String(page + 1))
            setParams(next)
          }}
        >
          next
        </button>
      </div>
      <JsonPanel data={audit.error ?? audit.data} />
    </Page>
  )
}
