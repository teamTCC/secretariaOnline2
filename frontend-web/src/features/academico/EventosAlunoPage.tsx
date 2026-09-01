import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Row = {
  id: string
  titulo?: string
  attendanceMode?: string
  estado?: string
  chCreditadas?: number
  inicioEm?: string
  fimEm?: string
}

type Envelope = { content?: Row[]; page?: { number: number; totalPages: number } }

export function EventosAlunoPage() {
  const nav = useNavigate()
  const [params] = useSearchParams()
  const audience = params.get('audience') ?? 'me'
  const [pasteId, setPasteId] = useState('')

  const list = useQuery({
    queryKey: queryKeys.eventos({ audience }),
    queryFn: () => api<Envelope>(`/events?audience=${encodeURIComponent(audience)}&page=0&size=20`),
  })

  return (
    <Page title="eventos (aluno)">
      <p>
        GET /events?audience=me — precisa metadata.idCurso = curso do evento. Host abre janela na fatia 6 (ou HTTPie
        T-F1-009). Sem janela: sessão sem rel confirmar-entrada.
      </p>
      <p>
        modos testáveis: SECRET_SINGLE · SECRET_DUAL · QR_SINGLE · QR_DUAL (colar PIN/qrToken da tela do prof)
      </p>
      {list.isPending && <p>carregando</p>}
      <ProblemBanner problem={isProblem(list.error) ? list.error : null} />
      <table>
        <thead>
          <tr>
            <th>id</th>
            <th>titulo</th>
            <th>mode</th>
            <th>estado</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {(list.data?.content ?? []).map((r) => (
            <tr key={r.id}>
              <td>{r.id}</td>
              <td>{r.titulo}</td>
              <td>{r.attendanceMode}</td>
              <td>{r.estado}</td>
              <td>
                <Link to={`/eventos/${r.id}/presenca`}>sessão</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          if (pasteId) nav(`/eventos/${pasteId}/presenca`)
        }}
      >
        <label>
          colar eventId
          <input value={pasteId} onChange={(e) => setPasteId(e.target.value)} />
        </label>
        <button type="submit">abrir sessão</button>
      </form>
      <JsonPanel data={list.error ?? list.data} />
    </Page>
  )
}
