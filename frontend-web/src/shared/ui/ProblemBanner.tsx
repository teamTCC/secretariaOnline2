import type { Problem } from '../api/problem'

type Props = { problem: Problem | null | undefined }

export function ProblemBanner({ problem }: Props) {
  if (!problem) return null
  return (
    <div className="danger">
      <strong>
        {problem.title}
        {problem.status ? ` (${problem.status})` : ''}
      </strong>
      {problem.detail ? <p>{problem.detail}</p> : null}
      {problem.incidentId ? (
        <p>
          incidentId: <a href={`/erro/${problem.incidentId}`}>{problem.incidentId}</a>
        </p>
      ) : null}
      {problem.retryAfterSeconds != null ? <p>tente em {problem.retryAfterSeconds}s</p> : null}
      {problem.erros != null ? (
        <ul>
          {(Array.isArray(problem.erros) ? problem.erros : [problem.erros]).map((e, i) => (
            <li key={i}>
              {typeof e === 'string'
                ? e
                : e && typeof e === 'object' && 'mensagem' in e
                  ? `${'campo' in e ? String((e as { campo?: unknown }).campo) : ''}: ${String((e as { mensagem?: unknown }).mensagem)}`
                  : JSON.stringify(e)}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  )
}
