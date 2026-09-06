# T-F8-001 — Busca Global (Command Palette)

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F8 — Cross-cutting/US-F8-001-BUSCA-GLOBAL.md`](../../foundationDocs/sequenceDiagrams/F8 — Cross-cutting/US-F8-001-BUSCA-GLOBAL.md)  
> **Status:** ✅ `SearchController` → `SearchQuery` via ports BFF (não JPA no controller)

---

## Características do endpoint

- **Sem capability fixa:** qualquer usuário autenticado pode chamar (`@PreAuthorize` autenticado)
- **FGAC no Query:** cada índice filtrado pelas capabilities do JWT
- **Debounce:** 200ms no cliente antes de disparar a chamada
- **Timeout:** 5s no servidor (`CompletableFuture.get(5, SECONDS)` → `{ timedOut: true }`) e no cliente (`AbortController`)
- **Extension PostgreSQL:** `pg_trgm` + índices GIN (V015) em `usuario.nome/email`, `event_attendance.titulo`, `request.request_type_code`, `curso.nome`

---

## Camada as-built

| Papel | Classe |
|-------|--------|
| HTTP | [`bff/SearchController.kt`](../../backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/SearchController.kt) |
| Query | [`bff/application/SearchQuery.kt`](../../backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/application/SearchQuery.kt) |
| Ports | `IamBffReadPort`, `SolicitacaoBffReadPort`, `PresencaBffReadPort`, `AcademicoReadPort` |

Não existe `SearchUseCase`. O BFF **não** injeta `*JpaRepository` de outro módulo.

`GET /search?q=&types=&page=&size=`

FGAC real:

| Tipo | Quem vê |
|------|---------|
| `USUARIO` | só `user.manage_students`, `user.manage_all` ou `system.admin` |
| `REQUEST` | staff (`request.view_curso` / `request.deliberate`) vê todas; aluno só as próprias (`idSolicitante`) |
| `EVENTO` / `CURSO` | qualquer autenticado |

Resposta: lista plana `{ type, id, title, subtitle, href }` (não os arrays agrupados do diagrama antigo).

---

## F8.1-D01 — Happy path

```
GET /search?q=joão&page=0&size=10
Cookie: access_token=…
```

`SearchQuery.execute` consulta os ports cujo `types` foi pedido (default: `USUARIO,EVENTO,REQUEST,CURSO`).

**JSON de saída (200):**

```json
{
  "query": "joão",
  "results": [
    { "type": "REQUEST", "id": "…", "title": "DECLARACAO_MATRICULA", "subtitle": "2026/0001", "href": "/requests/…" }
  ],
  "totalResults": 1
}
```

Timeout: `{ timedOut: true }` com o que deu tempo de juntar.

---

## F8.1-D02 — Fan-out FGAC: perfil Aluno

Com token de Aluno (sem `user.manage_*`):

| Índice | Comportamento |
|--------|----------------|
| `USUARIO` | omitido (sem query no IAM) |
| `REQUEST` | só `idSolicitante = userId` |
| `EVENTO` / `CURSO` | qualquer autenticado |

---

## F8.1-D03 — Sem resultados

`q` vazio ou sem match → **200** `{ results: [], totalResults: 0 }` — não 404.

---

## Checklist de Verificação

- [x] `GET /search?q=jo` → `200` com `results[]` via `SearchQuery` + ports
- [x] Perfil Aluno: sem resultados `USUARIO`; `REQUEST` só as próprias
- [x] Secretaria/Admin: pode buscar usuários
- [x] `q` em branco → `{ results: [], totalResults: 0 }`
- [x] Índices `pg_trgm` GIN (V015)
- [x] Timeout 5s no servidor (`timedOut: true`)
