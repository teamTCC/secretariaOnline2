import { isProblem, isProblemContentType, type Problem } from './problem'

export const BASE =
  (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') ??
  'http://localhost:8080'

const CSRF_EXEMPT = new Set([
  '/auth/login',
  '/auth/refresh',
  '/auth/ott',
  '/auth/forgot-password',
  '/auth/reset-password',
])

const UNSAFE = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

export type ApiInit = {
  method?: string
  body?: unknown
  headers?: HeadersInit
  skipRefresh?: boolean
  skipCsrf?: boolean
  retriedRefresh?: boolean
  retriedCsrf?: boolean
}

function pathnameOf(path: string): string {
  const raw = path.startsWith('http') ? new URL(path).pathname : path
  return raw.split('?')[0] ?? raw
}

function url(path: string): string {
  if (path.startsWith('http')) return path
  return `${BASE}${path.startsWith('/') ? path : `/${path}`}`
}

function readCookie(name: string): string | undefined {
  const prefix = `${name}=`
  for (const part of document.cookie.split(';')) {
    const s = part.trim()
    if (!s.startsWith(prefix)) continue
    return s.slice(prefix.length)
  }
}

function csrfExempt(path: string): boolean {
  return CSRF_EXEMPT.has(pathnameOf(path))
}

function networkProblem(e: unknown): Problem {
  return {
    type: 'https://secretariaonline.ufpr.br/errors/network',
    title: 'Falha de rede',
    status: 0,
    detail: e instanceof Error ? e.message : 'Não foi possível conectar à API',
  }
}

async function parseProblem(response: Response): Promise<Problem> {
  const retryHeader = response.headers.get('Retry-After')
  const retryAfterSeconds = retryHeader ? Number(retryHeader) : undefined
  let body: unknown
  try {
    const text = await response.text()
    body = text ? JSON.parse(text) : undefined
  } catch {
    body = undefined
  }
  if (isProblem(body)) {
    if (Number.isFinite(retryAfterSeconds) && body.retryAfterSeconds == null) {
      body.retryAfterSeconds = retryAfterSeconds
    }
    return body
  }
  return {
    type: 'https://secretariaonline.ufpr.br/errors/http',
    title: response.statusText || 'Erro HTTP',
    status: response.status,
    detail: typeof body === 'string' ? body : undefined,
    retryAfterSeconds: Number.isFinite(retryAfterSeconds) ? retryAfterSeconds : undefined,
  }
}

async function ensureCsrfCookie(): Promise<string | undefined> {
  const existing = readCookie('XSRF-TOKEN')
  if (existing) return existing
  await api('/auth/csrf', { skipRefresh: true })
  return readCookie('XSRF-TOKEN')
}

export async function api<T>(path: string, init: ApiInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)

  if (UNSAFE.has(method) && !csrfExempt(path) && !init.skipCsrf) {
    const token = readCookie('XSRF-TOKEN') ?? (await ensureCsrfCookie())
    if (token) headers.set('X-XSRF-TOKEN', token)
  }

  let payload: BodyInit | undefined
  if (init.body !== undefined && init.body !== null) {
    if (typeof init.body === 'string' || init.body instanceof FormData || init.body instanceof Blob) {
      payload = init.body
    } else {
      payload = JSON.stringify(init.body)
      if (!headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
    }
  }

  let response: Response
  try {
    response = await fetch(url(path), { method, headers, body: payload, credentials: 'include' })
  } catch (e) {
    throw networkProblem(e)
  }

  const canRefresh =
    response.status === 401 &&
    !init.skipRefresh &&
    !init.retriedRefresh &&
    pathnameOf(path) !== '/auth/login' &&
    pathnameOf(path) !== '/auth/refresh'

  if (canRefresh) {
    try {
      await api('/auth/refresh', { method: 'POST', skipRefresh: true })
    } catch (e) {
      throw isProblem(e) ? e : await parseProblem(response)
    }
    return api<T>(path, { ...init, retriedRefresh: true })
  }

  // as-built AccessDeniedHandler omits "csrf" in detail — retry mutations once after re-bootstrap
  if (
    response.status === 403 &&
    UNSAFE.has(method) &&
    !csrfExempt(path) &&
    !init.skipCsrf &&
    !init.retriedCsrf
  ) {
    await api('/auth/csrf', { skipRefresh: true })
    return api<T>(path, { ...init, retriedCsrf: true })
  }

  const ct = response.headers.get('content-type')
  if (!response.ok || isProblemContentType(ct)) {
    throw await parseProblem(response)
  }

  if (response.status === 204) return undefined as T
  const text = await response.text()
  if (!text) return undefined as T
  return JSON.parse(text) as T
}
