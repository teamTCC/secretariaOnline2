export type SessionFlags = {
  mustChangePassword: boolean
  mustAcceptLgpd: boolean
}

const KEY = 'so2.session.flags'

export function setFlags(flags: SessionFlags): void {
  sessionStorage.setItem(KEY, JSON.stringify(flags))
}

export function getFlags(): SessionFlags | null {
  const raw = sessionStorage.getItem(KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as SessionFlags
  } catch {
    return null
  }
}

export function clear(): void {
  sessionStorage.removeItem(KEY)
}

/** @deprecated use setFlags */
export const saveSessionFlags = setFlags
/** @deprecated use getFlags */
export const readSessionFlags = getFlags
/** @deprecated use clear */
export const clearSessionFlags = clear

export function afterAuthRedirect(flags: SessionFlags): string {
  if (flags.mustChangePassword || flags.mustAcceptLgpd) return '/primeiro-acesso'
  return '/dashboard'
}
