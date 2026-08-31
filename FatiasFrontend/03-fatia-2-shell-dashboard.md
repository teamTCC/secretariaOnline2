# Fatia 2 — Shell, `/me`, primeiro acesso, dashboards BFF, FGAC cruzado

**Objetivo da demo:** cada perfil vê **só** o BFF dele; o outro devolve 403 na UI. Logout mata Redis `sid` (próximo GET 401).

**Pré-requisito:** Fatias 0–1.  
**Oráculos:** `httpie/F1-aluno/T-F1-001-dashboard.md`, `T-F1-002`, `T-F1-003` · F2/F3/F5 dashboards.

---

## 1. Whitelist

| Arquivo | Notas |
|---------|--------|
| `shared/auth/session.ts` | `getFlags()`, `setFlags()`, `clear()` — só booleans |
| `shared/auth/AuthGuard.tsx` | se 401 após refresh → `/login`; se flags first-access → `/primeiro-acesso` |
| `shared/ui/Shell.tsx` | nav **de `_links` do `/me` + dashboard**, não de role. Fallback: 6 âncoras fixas (`/dashboard`, `/solicitacoes`, `/login`) porque o harness precisa navegar antes do BFF |
| `features/dashboard/DashboardPage.tsx` | **uma** página; o path BFF vem de `?perfil=` ou da 1ª role |
| `features/publico/PrimeiroAcessoPage.tsx` | `POST /auth/first-access` |
| `features/perfil/MePage.tsx` | T-F1-003 **inteiro**: GET/PATCH `/me`, avatar, senha, notifications, FCM, data-export + logout |
| `app/router.tsx` | rotas autenticadas wrap `AuthGuard` |

Nav fixa permitida nesta versão de testes (não é produto): Login, Dashboard, Solicitações, Me, Contato. **Ações de negócio** continuam só via `_links`.

---

## 2. `GET /me`

Controller `ProfileQuery`. 200: `id`, `nome`, `email`, `grr`, `roles[]`, `authorities[]`, `metadata` (JSONB: `idCurso`, `aceite_lgpd_em`, …), `_links`.

As-built: `_links` strings (`update-profile`, `change-password`, …). HTTPie antigo cita HAL — `normalizeLinks` da fatia 0 cobre.

`PATCH /me` + CSRF: `{ nome, metadata }`. Não mandar senha aqui.

Restante T-F1-003 **na mesma página** (forms nativos empilhados + JsonPanel):

| Ação | Path |
|------|------|
| Avatar presign | `POST /me/avatar` `{ contentType }` → PUT `uploadUrl` (MinIO) |
| Trocar senha | `POST /me/password` `{ senhaAtual, novaSenha }` — atual errada 400; fraca/reuse 422 |
| Preferências | `PATCH /me/notifications` `{ emailEnabled, pushEnabled, inAppEnabled }` |
| FCM (T-10.5) | `POST /me/fcm-token` `{ fcmToken, plataforma: "WEB" }` e `DELETE` com o mesmo body |
| Export LGPD | `POST /me/data-export` → poll `GET /me/data-export/{jobId}` (`READY` / `EXPIRED`) |

`POST /auth/logout` + CSRF → 204, cookies limpos, Redis session gone. UI: botão na Shell; depois `navigate('/login')`.

---

## 3. Primeiro acesso

Login com senha provisória → `{ mustChangePassword: true }` (ou `mustAcceptLgpd`). Cookies **já** existem.

```
POST /auth/first-access   // autenticado + CSRF
{ "novaSenha": "…", "aceiteLgpd": true }
```

- `aceiteLgpd: false` → **400**
- senha curta/fraca → **422**
- 200 `{ mensagem: "Primeiro acesso concluído com sucesso." }`
- Relogar: flags false; `GET /me` → `metadata.aceite_lgpd_em` ISO-8601

Usuários bootstrap (`AlunoS3nh@Forte!`) em geral **já** passaram first-access. A equipe testa criando usuário novo (admin fatia 7) ou aceitando que a página existe e o 400/422 são visíveis com payload forçado.

---

## 4. Dashboards BFF — um GET, nunca fan-out

| Perfil | GET | Authority | Cache Redis 60s |
|--------|-----|-----------|-----------------|
| Aluno | `/bff/dashboard/aluno` | `dashboard.view_own` | `aluno:{uuid}` |
| Professor | `/bff/dashboard/professor` | `dashboard.view_self_professor` | `professor:{uuid}` |
| Secretaria | `/bff/dashboard/secretaria` | `dashboard.view_secretary` | `secretaria:static` |
| Egresso | `/bff/dashboard/egresso` | `alumni.view_own` | `egresso:{uuid}` |

**Não existe** dashboard coordenador/admin BFF separado nesta tabela. Coord usa `/courses/{id}/config` + `/reports/coordinator` (fatia 7). Admin usa telas IAM.

Envelope aluno (as-built):

```json
{
  "kpis": { "horasFormativas": { "atual": 0, "requerido": 120, "percentual": 0 }, "atendimentosPendentes": 0 },
  "pendencias": [],
  "eventos": [],
  "ultimasSolicitacoes": [],
  "_links": {
    "self": "/bff/dashboard/aluno",
    "novaSolicitacao": "/requests/types",
    "formativas": "/formativas/minhas",
    "eventos": "/events?audience=me"
  }
}
```

`novaSolicitacao` **só** se JWT tem `request.open`.

Pendências: cada item tem **`_link`** (singular). Clique → `navigate(_link)`.

Se um port interno falhar: campo `null`, `_degraded: true`, **ainda 200**. UI: texto “degradado” + JSON. Resposta degradada **não** é cacheada.

Professor 200: `meusEventos`, `solicitacoesPendentes`, `_links.novoEvento` = `/events`.

Secretaria 200: `kpis.emTriagem`, `emDeliberacao`, `_links.solicitacoes`, `_links.usuarios`.

Egresso: **sem** `_links.novaSolicitacao`.

### Como escolher o BFF na UI de testes

Não inferir 10 papéis. Ordem:

1. Query `GET /me`
2. Se `authorities` contém `dashboard.view_secretary` → secretaria
3. Senão `dashboard.view_self_professor` → professor
4. Senão `alumni.view_own` → egresso
5. Senão `dashboard.view_own` → aluno
6. Senão mostrar `/me` JSON e lista de authorities (admin sem dashboard BFF)

Botão extra na DashboardPage: “forçar GET /bff/dashboard/aluno” mesmo logado como prof — **deve** pintar `ProblemBanner` 403. Isto é o teste FGAC para a equipe.

---

## 5. `DashboardPage` — visual

```
<Page title="Dashboard">
  <HateoasBar links={data._links} onAction={(rel, href) => navigate(href)} />
  <p>degraded: {String(data._degraded)}</p>
  <JsonPanel data={data} />
</Page>
```

Sem KPI cards. A equipe lê `kpis` no JSON.

Invalidar query após mutações futuras: `queryClient.invalidateQueries({ queryKey: queryKeys.dashboard(perfil) })`. TTL 60s no back: segundo load rápido é esperado.

---

## 6. Aceite

- [ ] Aluno: dashboard 200, `_links.novaSolicitacao` presente
- [ ] Professor no dashboard aluno: 403 visível
- [ ] Aluno no dashboard secretaria: 403
- [ ] Logout: cookie some; F5 em `/dashboard` → login
- [ ] `GET /me` no JsonPanel com `roles`
- [ ] Shell tem botão Sair que manda CSRF no POST logout
- [ ] PATCH `/me`, POST senha, PATCH notifications, POST/DELETE fcm-token, POST data-export disparáveis
- [ ] Avatar: presign 200; PUT MinIO (ou storageKey colável se CORS falhar)

## 7. Não fazer

- KpiRow, gráficos, DashboardA.
- Chamar `/requests` + `/events` + `/formativas` no mount do dashboard.
- `if (role === 'ADMIN')` para esconder nav de aluno.
