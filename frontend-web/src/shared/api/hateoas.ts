export function normalizeLinks(raw: unknown): Record<string, string> {
  if (!raw) return {}
  if (Array.isArray(raw)) {
    return Object.fromEntries(
      (raw as { rel?: string; href?: string }[]).map((x) => [x.rel ?? '', x.href ?? '']),
    )
  }
  const out: Record<string, string> = {}
  for (const [k, v] of Object.entries(raw as object)) {
    out[k] = typeof v === 'string' ? v : ((v as { href?: string })?.href ?? '')
  }
  return out
}

export const actionFromRel = (rel: string) => rel.replace(/-/g, '_').toUpperCase()

export function hrefOf(links: Record<string, string> | undefined, rel: string) {
  return links?.[rel]
}

/** API href (`/requests/{id}`) → rota da SPA. Pendências do BFF e `_links` do dashboard usam path de API. */
export function uiPathFromHref(href: string): string {
  let path = href
  let search = ''
  try {
    const u = href.startsWith('http') ? new URL(href) : new URL(href, 'http://spa.local')
    path = u.pathname
    search = u.search
  } catch {
    const q = href.indexOf('?')
    if (q >= 0) {
      path = href.slice(0, q)
      search = href.slice(q)
    }
  }
  const typeCode = path.match(/^\/requests\/types\/([^/]+)$/)
  if (path === '/requests/types' || typeCode) {
    return typeCode ? `/solicitacoes/nova?code=${encodeURIComponent(typeCode[1])}` : '/solicitacoes/nova'
  }
  const req = path.match(/^\/requests\/([0-9a-fA-F-]{36})$/)
  if (req) return `/solicitacoes/${req[1]}`
  if (path === '/requests') return '/solicitacoes'
  if (path === '/formativas' || path === '/formativas/minhas') return '/formativas'
  const evSess = path.match(/^\/events\/([0-9a-fA-F-]{36})(?:\/attendance\/session)?$/)
  if (evSess) return `/eventos/${evSess[1]}/presenca`
  if (path === '/events') return `/eventos${search}`
  if (path === '/certificates' || path === '/certificates/mine') return '/certificados'
  const certPub = path.match(/^\/publico\/verificar-certificado\/(.+)$/)
  if (certPub) return `/publico/verificar-certificado/${certPub[1]}`
  if (path === '/me/service-records' || path === '/service-records') return '/atendimentos'
  if (path === '/communications/me' || path === '/communications') return '/comunicados'
  if (path === '/faq') return '/faq'
  if (path === '/internships' || path === '/internships/mine') return '/estagios'
  const intern = path.match(/^\/internships\/([0-9a-fA-F-]{36})(?:\/.*)?$/)
  if (intern) return `/estagios/${intern[1]}`
  if (path === '/tccs' || path === '/tccs/mine') return '/tccs'
  const tcc = path.match(/^\/tccs\/([0-9a-fA-F-]{36})(?:\/.*)?$/)
  if (tcc) return `/tccs/${tcc[1]}`
  const diploma = path.match(/^\/graduations\/([0-9a-fA-F-]{36})\/diploma-url$/)
  if (diploma) return `/dashboard?diploma=${diploma[1]}`
  return `${path}${search}`
}

const META_RELS = new Set(['self', 'events', 'attachments'])

export function useActions(links: Record<string, string> | undefined) {
  const n = links ?? {}
  const has = (rel: string) => Boolean(n[rel])
  const href = (rel: string) => n[rel]
  const actionRels = Object.keys(n).filter((r) => !META_RELS.has(r))
  return { has, href, actionRels, all: n }
}
