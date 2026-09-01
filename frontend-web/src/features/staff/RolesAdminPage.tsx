import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../../shared/api/client'
import { isProblem } from '../../shared/api/problem'
import { queryKeys } from '../../shared/api/queryKeys'
import { JsonPanel } from '../../shared/ui/JsonPanel'
import { Page } from '../../shared/ui/Page'
import { ProblemBanner } from '../../shared/ui/ProblemBanner'

type Role = { id: string; code?: string; descricao?: string; authorities?: string[] }
type Auth = { code: string; descricao?: string }

export function RolesAdminPage() {
  const qc = useQueryClient()
  const [code, setCode] = useState('MONITOR')
  const [descricao, setDescricao] = useState('Monitor de disciplina — fatia 7')
  const [roleId, setRoleId] = useState('')
  const [authCodes, setAuthCodes] = useState('dashboard.view_own,communication.read')
  const [userId, setUserId] = useState('')
  const [userRoles, setUserRoles] = useState('ALUNO')
  const [last, setLast] = useState<unknown>()

  const roles = useQuery({
    queryKey: queryKeys.adminRoles,
    queryFn: () => api<Role[]>('/admin/roles'),
  })
  const authorities = useQuery({
    queryKey: queryKeys.adminAuthorities,
    queryFn: () => api<Auth[]>('/admin/autoridades'),
  })

  function invalidate() {
    void qc.invalidateQueries({ queryKey: ['admin'] })
  }

  const create = useMutation({
    mutationFn: () => api('/admin/roles', { method: 'POST', body: { code, descricao } }),
    onSuccess: (d) => {
      setLast(d)
      const id = (d as { id?: string }).id
      if (id) setRoleId(id)
      invalidate()
    },
    onError: setLast,
  })

  const setAuths = useMutation({
    mutationFn: () =>
      api(`/admin/roles/${roleId}/authorities`, {
        method: 'PATCH',
        body: { authorityCodes: authCodes.split(',').map((s) => s.trim()).filter(Boolean) },
      }),
    onSuccess: (d) => {
      setLast(d)
      invalidate()
    },
    onError: setLast,
  })

  const assign = useMutation({
    mutationFn: () =>
      api(`/admin/usuarios/${userId}/roles`, {
        method: 'PUT',
        body: { roleCodes: userRoles.split(',').map((s) => s.trim()).filter(Boolean) },
      }),
    onSuccess: setLast,
    onError: setLast,
  })

  const problem = [roles.error, authorities.error, create.error, setAuths.error, assign.error]
    .reverse()
    .find((e) => isProblem(e))

  return (
    <Page title="admin roles">
      <p>GET /admin/roles (alias /admin/perfis) · GET /admin/autoridades · POST roles · PATCH authorities · PUT user roles. iam.manage_roles.</p>
      {(roles.isPending || authorities.isPending) && <p>carregando</p>}
      <ProblemBanner problem={isProblem(problem) ? problem : null} />
      <form
        onSubmit={(e) => {
          e.preventDefault()
          create.mutate()
        }}
      >
        <label>
          code
          <input value={code} onChange={(e) => setCode(e.target.value)} />
        </label>
        <label>
          descricao
          <input value={descricao} onChange={(e) => setDescricao(e.target.value)} />
        </label>
        <button type="submit">POST /admin/roles</button>
      </form>
      <table>
        <thead>
          <tr>
            <th>code</th>
            <th>id</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {(roles.data ?? []).map((r) => (
            <tr key={r.id}>
              <td>{r.code}</td>
              <td>{r.id}</td>
              <td>
                <button type="button" onClick={() => setRoleId(r.id)}>
                  usar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <label>
        roleId
        <input value={roleId} onChange={(e) => setRoleId(e.target.value)} />
      </label>
      <label>
        authorityCodes (vírgula)
        <input value={authCodes} onChange={(e) => setAuthCodes(e.target.value)} />
      </label>
      <button type="button" disabled={!roleId || setAuths.isPending} onClick={() => setAuths.mutate()}>
        PATCH /admin/roles/:id/authorities
      </button>
      <label>
        userId
        <input value={userId} onChange={(e) => setUserId(e.target.value)} />
      </label>
      <label>
        roleCodes
        <input value={userRoles} onChange={(e) => setUserRoles(e.target.value)} />
      </label>
      <button type="button" disabled={!userId || assign.isPending} onClick={() => assign.mutate()}>
        PUT /admin/usuarios/:id/roles
      </button>
      <h2>última mutação</h2>
      <JsonPanel data={last} />
      <h2>roles</h2>
      <JsonPanel data={roles.error ?? roles.data} />
      <h2>autoridades</h2>
      <JsonPanel data={authorities.error ?? authorities.data} />
    </Page>
  )
}
