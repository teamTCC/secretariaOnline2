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

const META_RELS = new Set(['self', 'events', 'attachments', 'public'])

export function useActions(links: Record<string, string> | undefined) {
  const n = links ?? {}
  const has = (rel: string) => Boolean(n[rel])
  const href = (rel: string) => n[rel]
  const actionRels = Object.keys(n).filter((r) => !META_RELS.has(r))
  return { has, href, actionRels, all: n }
}
