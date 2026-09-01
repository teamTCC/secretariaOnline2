const KEY = 'so2.deviceUuid'

/** Anti-share UNIQUE(id_evento, device_uuid). Não é JWT. */
export function deviceUuid(): string {
  let v = localStorage.getItem(KEY)
  if (!v) {
    v = crypto.randomUUID()
    localStorage.setItem(KEY, v)
  }
  return v
}
