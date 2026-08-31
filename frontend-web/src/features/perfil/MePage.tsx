import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { api } from '../../shared/api/client'
import { normalizeLinks } from '../../shared/api/hateoas'
import { isProblem, type Problem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { getFlags } from '../../shared/auth/session'
import { HateoasBar } from '../../shared/ui/HateoasBar'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Me = {
  id?: string
  nome?: string
  email?: string
  grr?: string | null
  roles?: string[]
  authorities?: string[]
  metadata?: unknown
  _links?: unknown
}

type AvatarResp = { uploadUrl: string; storageKey: string }
type ExportStarted = { jobId: string; downloadUrl?: string | null }

function lastProblem(...xs: unknown[]): Problem | null {
  for (let i = xs.length - 1; i >= 0; i--) {
    const x = xs[i]
    if (isProblem(x)) return x
  }
  return null
}

export function MePage() {
  const qc = useQueryClient()
  const me = useQuery({
    queryKey: queryKeys.me,
    queryFn: () => api<Me>('/me'),
  })

  const [nome, setNome] = useState('')
  const [metadataJson, setMetadataJson] = useState('{"telefone":"41999990000"}')
  const [senhaAtual, setSenhaAtual] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [emailEnabled, setEmailEnabled] = useState(true)
  const [pushEnabled, setPushEnabled] = useState(false)
  const [inAppEnabled, setInAppEnabled] = useState(true)
  const [fcmToken, setFcmToken] = useState('fake-fcm-token-web-001')
  const [plataforma, setPlataforma] = useState('WEB')
  const [jobId, setJobId] = useState('')
  const [avatarFile, setAvatarFile] = useState<File | null>(null)
  const [avatarPut, setAvatarPut] = useState<unknown>()
  const [seeded, setSeeded] = useState(false)
  const [lastMutation, setLastMutation] = useState<unknown>()

  useEffect(() => {
    if (seeded || !me.data) return
    setSeeded(true)
    if (me.data.nome) setNome(me.data.nome)
    if (me.data.metadata != null) setMetadataJson(JSON.stringify(me.data.metadata, null, 2))
  }, [me.data, seeded])

  const patchMe = useMutation({
    mutationFn: () => {
      let metadata: unknown
      try {
        metadata = JSON.parse(metadataJson) as unknown
      } catch {
        throw {
          type: 'https://secretariaonline.ufpr.br/errors/validation-error',
          title: 'JSON inválido',
          status: 400,
          detail: 'metadata não é JSON válido',
        }
      }
      return api('/me', {
        method: 'PATCH',
        body: { nome: nome || me.data?.nome, metadata },
      })
    },
    onSuccess: (d) => {
      setLastMutation(d)
      void qc.invalidateQueries({ queryKey: queryKeys.me })
    },
  })

  const password = useMutation({
    mutationFn: () =>
      api('/me/password', { method: 'POST', body: { senhaAtual, novaSenha } }),
    onSuccess: () => setLastMutation({ status: 204 }),
    onError: (e) => setLastMutation(e),
  })

  const notif = useMutation({
    mutationFn: () =>
      api('/me/notifications', {
        method: 'PATCH',
        body: { emailEnabled, pushEnabled, inAppEnabled },
      }),
    onSuccess: (d) => setLastMutation(d),
  })

  const fcmPost = useMutation({
    mutationFn: () =>
      api('/me/fcm-token', { method: 'POST', body: { fcmToken, plataforma } }),
    onSuccess: (d) => setLastMutation(d),
  })

  const fcmDel = useMutation({
    mutationFn: () => api('/me/fcm-token', { method: 'DELETE', body: { fcmToken } }),
    onSuccess: (d) => setLastMutation(d),
  })

  const exportStart = useMutation({
    mutationFn: () => api<ExportStarted>('/me/data-export', { method: 'POST' }),
    onSuccess: (d) => {
      setJobId(d.jobId)
      setLastMutation(d)
    },
  })

  const exportStatus = useQuery({
    queryKey: queryKeys.dataExport(jobId),
    queryFn: () => api(`/me/data-export/${jobId}`),
    enabled: Boolean(jobId),
  })

  useEffect(() => {
    if (exportStatus.data) setLastMutation(exportStatus.data)
  }, [exportStatus.data])

  const avatar = useMutation({
    mutationFn: async () => {
      const presign = await api<AvatarResp>('/me/avatar', { method: 'POST' })
      if (!avatarFile) return { presign, put: 'sem arquivo — cole storageKey / faça PUT manual' }
      try {
        const res = await fetch(presign.uploadUrl, {
          method: 'PUT',
          body: avatarFile,
          headers: { 'Content-Type': avatarFile.type || 'image/jpeg' },
        })
        setAvatarPut({ status: res.status, ok: res.ok })
        if (!res.ok) {
          return { presign, put: `PUT MinIO ${res.status} — CORS/MinIO; storageKey colável` }
        }
        return { presign, put: `PUT MinIO ${res.status}` }
      } catch (e) {
        const msg = e instanceof Error ? e.message : String(e)
        setAvatarPut({ error: msg })
        return { presign, put: `PUT falhou (${msg}) — storageKey colável` }
      }
    },
    onSuccess: (d) => setLastMutation(d),
  })

  const problem = lastProblem(
    me.error,
    patchMe.error,
    password.error,
    notif.error,
    fcmPost.error,
    fcmDel.error,
    exportStart.error,
    exportStatus.error,
    avatar.error,
  )

  return (
    <Page title="me">
      {me.isPending && <p>carregando</p>}
      <ProblemBanner problem={problem} />
      <HateoasBar links={normalizeLinks(me.data?._links)} />
      <p>session flags</p>
      <JsonPanel data={getFlags()} />
      <p>GET /me</p>
      <JsonPanel data={me.error ?? me.data} />

      <form
        onSubmit={(e) => {
          e.preventDefault()
          patchMe.mutate()
        }}
      >
        <p>PATCH /me</p>
        <label>
          nome
          <input
            name="nome"
            value={nome}
            placeholder={me.data?.nome}
            onChange={(e) => setNome(e.target.value)}
          />
        </label>
        <label>
          metadata JSON
          <textarea
            name="metadata"
            rows={4}
            value={metadataJson}
            onChange={(e) => setMetadataJson(e.target.value)}
          />
        </label>
        <button type="submit" disabled={patchMe.isPending}>
          PATCH /me
        </button>
      </form>

      <form
        onSubmit={(e) => {
          e.preventDefault()
          void avatar.mutate()
        }}
      >
        <p>POST /me/avatar + PUT MinIO</p>
        <input
          type="file"
          accept="image/*"
          onChange={(e) => setAvatarFile(e.target.files?.[0] ?? null)}
        />
        <button type="submit" disabled={avatar.isPending}>
          POST /me/avatar
        </button>
      </form>
      {avatarPut !== undefined ? <JsonPanel data={avatarPut} /> : null}

      <form
        onSubmit={(e) => {
          e.preventDefault()
          password.mutate()
        }}
      >
        <p>POST /me/password (204 as-built)</p>
        <label>
          senhaAtual
          <input
            name="senhaAtual"
            type="password"
            value={senhaAtual}
            onChange={(e) => setSenhaAtual(e.target.value)}
          />
        </label>
        <label>
          novaSenha
          <input
            name="novaSenha"
            type="password"
            value={novaSenha}
            onChange={(e) => setNovaSenha(e.target.value)}
          />
        </label>
        <button type="submit" disabled={password.isPending}>
          POST /me/password
        </button>
      </form>
      {password.isSuccess ? <JsonPanel data={{ status: 204 }} /> : null}

      <form
        onSubmit={(e) => {
          e.preventDefault()
          notif.mutate()
        }}
      >
        <p>PATCH /me/notifications</p>
        <label>
          <input type="checkbox" checked={emailEnabled} onChange={(e) => setEmailEnabled(e.target.checked)} />
          emailEnabled
        </label>
        <label>
          <input type="checkbox" checked={pushEnabled} onChange={(e) => setPushEnabled(e.target.checked)} />
          pushEnabled
        </label>
        <label>
          <input type="checkbox" checked={inAppEnabled} onChange={(e) => setInAppEnabled(e.target.checked)} />
          inAppEnabled
        </label>
        <button type="submit" disabled={notif.isPending}>
          PATCH /me/notifications
        </button>
      </form>

      <form
        onSubmit={(e) => {
          e.preventDefault()
        }}
      >
        <p>FCM T-10.5</p>
        <label>
          fcmToken
          <input name="fcmToken" value={fcmToken} onChange={(e) => setFcmToken(e.target.value)} />
        </label>
        <label>
          plataforma
          <input name="plataforma" value={plataforma} onChange={(e) => setPlataforma(e.target.value)} />
        </label>
        <button type="button" disabled={fcmPost.isPending} onClick={() => fcmPost.mutate()}>
          POST /me/fcm-token
        </button>
        <button type="button" disabled={fcmDel.isPending} onClick={() => fcmDel.mutate()}>
          DELETE /me/fcm-token
        </button>
      </form>

      <form
        onSubmit={(e) => {
          e.preventDefault()
          exportStart.mutate()
        }}
      >
        <p>POST /me/data-export + poll</p>
        <label>
          jobId
          <input name="jobId" value={jobId} onChange={(e) => setJobId(e.target.value)} />
        </label>
        <button type="submit" disabled={exportStart.isPending}>
          POST /me/data-export
        </button>
        <button
          type="button"
          disabled={!jobId || exportStatus.isFetching}
          onClick={() => void exportStatus.refetch()}
        >
          GET /me/data-export/{'{jobId}'}
        </button>
      </form>

      <p>última mutação</p>
      <JsonPanel data={lastMutation} />
    </Page>
  )
}
