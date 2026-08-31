export type Problem = {
  type?: string
  title: string
  status: number
  detail?: string
  instance?: string
  timestamp?: string
  incidentId?: string
  retryAfterSeconds?: number
  erros?: unknown
}

declare module '@tanstack/react-query' {
  interface Register {
    defaultError: Problem
  }
}

export function isProblem(x: unknown): x is Problem {
  if (typeof x === 'string') {
    return x.toLowerCase().includes('application/problem+json')
  }
  if (!x || typeof x !== 'object') return false
  const o = x as Record<string, unknown>
  return typeof o.title === 'string' && typeof o.status === 'number'
}

export function isProblemContentType(contentType: string | null | undefined): boolean {
  return isProblem(contentType ?? '')
}
