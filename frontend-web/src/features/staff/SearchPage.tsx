import { useQuery } from '@tanstack/react-query'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { uiPathFromHref } from '../../shared/api/hateoas'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Hit = { type: string; id: string; title?: string; subtitle?: string; href?: string }
type Envelope = { query?: string; results?: Hit[]; totalResults?: number; timedOut?: boolean }

export function SearchPage() {
  const nav = useNavigate()
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? 'ana'
  const types = params.get('types') ?? ''
  const enabled = q.trim().length > 0

  const search = useQuery({
    queryKey: queryKeys.search({ q, types }),
    queryFn: () => {
      const qs = new URLSearchParams()
      qs.set('q', q)
      qs.set('page', '0')
      qs.set('size', '10')
      if (types) qs.set('types', types)
      return api<Envelope>(`/search?${qs}`)
    },
    enabled,
  })

  const hits = search.data?.results ?? []
  const hasUsuario = hits.some((h) => h.type === 'USUARIO')

  return (
    <Page title="busca">
      <p>
        GET /search?q=&types=. types: USUARIO,EVENTO,REQUEST,CURSO. FGAC: aluno sem type=USUARIO; secretaria/admin
        sim. Clique href → navigate.
      </p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
        }}
      >
        <label>
          q
          <input
            value={q}
            onChange={(e) => {
              const next = new URLSearchParams(params)
              next.set('q', e.target.value)
              setParams(next)
            }}
          />
        </label>
        <label>
          types
          <input
            value={types}
            onChange={(e) => {
              const next = new URLSearchParams(params)
              if (e.target.value) next.set('types', e.target.value)
              else next.delete('types')
              setParams(next)
            }}
            placeholder="USUARIO"
          />
        </label>
      </form>
      <div className="row">
        {['', 'USUARIO', 'EVENTO', 'REQUEST', 'CURSO'].map((t) => (
          <button
            key={t || 'all'}
            type="button"
            onClick={() => {
              const next = new URLSearchParams(params)
              if (t) next.set('types', t)
              else next.delete('types')
              setParams(next)
            }}
          >
            {t || 'todos'}
          </button>
        ))}
      </div>
      {search.isPending && enabled && <p>carregando</p>}
      <ProblemBanner problem={isProblem(search.error) ? search.error : null} />
      <p>
        timedOut: {String(Boolean(search.data?.timedOut))} · tem USUARIO: {String(hasUsuario)}
      </p>
      <ul>
        {hits.map((h) => (
          <li key={`${h.type}-${h.id}`}>
            <button type="button" disabled={!h.href} onClick={() => h.href && nav(uiPathFromHref(h.href))}>
              {h.type} {h.title} {h.subtitle} {h.href}
            </button>
          </li>
        ))}
      </ul>
      <JsonPanel data={search.error ?? search.data} />
    </Page>
  )
}
