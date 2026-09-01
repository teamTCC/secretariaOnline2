import { useState } from 'react'
import { api } from '../api/client'
import { JsonPanel } from './JsonPanel'

export type AttachmentInput = {
  storageKey: string
  sha256: string
  nomeOriginal: string
  contentType: string
  categoria: string
  tamanhoBytes: number
}

type Presign = { uploadUrl: string; storageKey: string }

type PresignCtx = { file: File; hash: string; categoria: string }

type Props = {
  requestId?: string
  categoria: string
  onReady?: (att: AttachmentInput) => void
  /** override — formativas: `/formativas/comprovantes/presigned-url` */
  presignPath?: string
  /** internships/TCC: body diferente de `{ filename, contentType }` */
  buildPresignBody?: (ctx: PresignCtx) => unknown
  confirmPath?: string
  buildConfirmBody?: (att: AttachmentInput) => unknown
}

async function sha256Hex(file: File): Promise<string> {
  const buf = await crypto.subtle.digest('SHA-256', await file.arrayBuffer())
  return [...new Uint8Array(buf)].map((b) => b.toString(16).padStart(2, '0')).join('')
}

export function AttachmentUpload({
  requestId,
  categoria: categoriaProp,
  onReady,
  presignPath,
  buildPresignBody,
  confirmPath,
  buildConfirmBody,
}: Props) {
  const [categoria, setCategoria] = useState(categoriaProp)
  const [file, setFile] = useState<File | null>(null)
  const [pending, setPending] = useState(false)
  const [last, setLast] = useState<unknown>()
  const [storageKey, setStorageKey] = useState('')
  const [sha256, setSha256] = useState('')
  const [uploadUrl, setUploadUrl] = useState('')
  const [nomeOriginal, setNomeOriginal] = useState('')
  const [contentType, setContentType] = useState('application/pdf')
  const [tamanhoBytes, setTamanhoBytes] = useState(0)

  async function presignAndPut() {
    if (!file) return
    setPending(true)
    try {
      const hash = await sha256Hex(file)
      const ct = file.type || 'application/pdf'
      const body = buildPresignBody
        ? buildPresignBody({ file, hash, categoria })
        : {
            filename: file.name,
            contentType: ct,
            sha256: hash,
            sizeBytes: file.size,
            categoria,
          }
      const path =
        presignPath ??
        (requestId ? `/requests/${requestId}/attachments/upload-url` : '/requests/attachments/presigned-url')
      const presign = await api<Presign>(path, { method: 'POST', body })
      setUploadUrl(presign.uploadUrl)
      setStorageKey(presign.storageKey)
      setSha256(hash)
      setNomeOriginal(file.name)
      setContentType(ct)
      setTamanhoBytes(file.size)
      let putOk = false
      let put: unknown = 'sem PUT'
      try {
        const res = await fetch(presign.uploadUrl, {
          method: 'PUT',
          body: file,
          headers: { 'Content-Type': ct },
        })
        putOk = res.ok
        put = { status: res.status, ok: res.ok }
        if (!res.ok) put = { status: res.status, hint: 'CORS/MinIO — cole storageKey após PUT via HTTPie' }
      } catch (e) {
        put = {
          error: e instanceof Error ? e.message : String(e),
          hint: 'PUT falhou (CORS). Use uploadUrl abaixo e o botão incluir/confirm depois.',
        }
      }
      const att: AttachmentInput = {
        storageKey: presign.storageKey,
        sha256: hash,
        nomeOriginal: file.name,
        contentType: ct,
        categoria,
        tamanhoBytes: file.size,
      }
      let confirm: unknown
      if (putOk && (confirmPath || requestId)) {
        try {
          confirm = await postConfirm(att)
        } catch (e) {
          confirm = e
        }
      }
      if (putOk) onReady?.(att)
      setLast({ presign, put, confirm, att })
    } catch (e) {
      setLast(e)
    } finally {
      setPending(false)
    }
  }

  async function postConfirm(att: AttachmentInput) {
    if (confirmPath) {
      return api(confirmPath, {
        method: 'POST',
        body: buildConfirmBody ? buildConfirmBody(att) : att,
      })
    }
    return api(`/requests/${requestId}/attachments/confirm`, { method: 'POST', body: att })
  }

  async function confirmOnly() {
    if ((!requestId && !confirmPath) || !storageKey || !sha256) return
    setPending(true)
    try {
      const att: AttachmentInput = {
        storageKey,
        sha256,
        nomeOriginal: nomeOriginal || 'arquivo',
        contentType,
        categoria,
        tamanhoBytes: tamanhoBytes || 1,
      }
      const confirm = await postConfirm(att)
      onReady?.(att)
      setLast({ confirm, att })
    } catch (e) {
      setLast(e)
    } finally {
      setPending(false)
    }
  }

  return (
    <fieldset>
      <legend>anexo {categoriaProp}</legend>
      <label>
        categoria
        <input value={categoria} onChange={(e) => setCategoria(e.target.value)} />
      </label>
      <input
        type="file"
        onChange={(e) => {
          const f = e.target.files?.[0] ?? null
          setFile(f)
          if (f) {
            setNomeOriginal(f.name)
            setContentType(f.type || 'application/pdf')
            setTamanhoBytes(f.size)
          }
        }}
      />
      <div className="row">
        <button type="button" disabled={pending || !file} onClick={() => void presignAndPut()}>
          {requestId || confirmPath ? 'presign + PUT + confirm' : 'presign órfão + PUT'}
        </button>
      </div>
      <p>fallback CORS — cole storageKey após PUT manual</p>
      <label>
        uploadUrl
        <input value={uploadUrl} onChange={(e) => setUploadUrl(e.target.value)} />
      </label>
      <label>
        storageKey
        <input value={storageKey} onChange={(e) => setStorageKey(e.target.value)} />
      </label>
      <label>
        sha256
        <input value={sha256} onChange={(e) => setSha256(e.target.value)} />
      </label>
      {requestId || confirmPath ? (
        <button type="button" disabled={pending || !storageKey} onClick={() => void confirmOnly()}>
          POST confirm
        </button>
      ) : (
        <button
          type="button"
          disabled={pending || !storageKey || !sha256}
          onClick={() =>
            onReady?.({
              storageKey,
              sha256,
              nomeOriginal: nomeOriginal || 'arquivo',
              contentType,
              categoria,
              tamanhoBytes: tamanhoBytes || 1,
            })
          }
        >
          incluir no POST
        </button>
      )}
      <JsonPanel data={last} />
    </fieldset>
  )
}
