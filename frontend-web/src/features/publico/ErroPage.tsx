import { Link, useParams } from 'react-router-dom'
import { Page } from '../../shared/ui/Page'

export function ErroPage() {
  const { incidentId } = useParams()
  return (
    <Page title="erro">
      <p>
        <Link to="/login">login</Link>
      </p>
      <p>incidentId: {incidentId ?? '(nenhum)'}</p>
      <p>GET público de incidente não existe — só UI para correlacionar logs.</p>
    </Page>
  )
}
