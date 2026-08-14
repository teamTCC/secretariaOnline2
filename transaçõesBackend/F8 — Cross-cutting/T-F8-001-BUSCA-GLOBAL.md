# T-F8-001 — Busca Global (Command Palette)

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F8 — Cross-cutting/US-F8-001-BUSCA-GLOBAL.md`](../../foundationDocs/sequenceDiagrams/F8 — Cross-cutting/US-F8-001-BUSCA-GLOBAL.md)  
> **Status:** ✅ Implementado — SearchController fan-out via BFF com FGAC (usuários e solicitações)

---

## Características do endpoint

- **Sem capability fixa:** qualquer usuário autenticado pode chamar
- **FGAC dinâmico no use case:** cada índice filtrado pelas capabilities do JWT
- **Debounce:** 200ms no cliente antes de disparar a chamada
- **Timeout:** 5s no servidor (`CompletableFuture.get(5, SECONDS)` → `{ timedOut: true }`) e no cliente (`AbortController`).
- **Extension PostgreSQL:** `pg_trgm` + índices GIN (V015) em `usuario.nome/email`, `event_attendance.titulo`, `request.request_type_code`, `curso.nome`.

---

## O que está implementado

`GET /search?q=&types=&page=&size=` em [`bff/SearchController.kt`](../../backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/SearchController.kt).

FGAC real:

| Tipo | Quem vê |
|------|---------|
| `USUARIO` | só `user.manage_students`, `user.manage_all` ou `system.admin` |
| `REQUEST` | staff (`request.view_curso` / `request.deliberate`) vê todas; aluno só as próprias (`idSolicitante`) |
| `EVENTO` / `CURSO` | qualquer autenticado |

Resposta: lista plana `{ type, id, title, subtitle, href }` (não os arrays agrupados do diagrama).

---

## O que os diagramas especificam

### F8.1-D01 — Happy path: fan-out paralelo com resultados agrupados

```
GET /search?q=joão&limit=5
Authorization: Bearer eyJhbGci...
```

O `SearchUseCase` executa **4 queries em paralelo** (Kotlin coroutines), cada uma com sua própria cláusula de capability:

```kotlin
// SearchUseCase.kt (pseudo-código)
suspend fun search(q: String, capabilities: Set<String>): SearchResultDto {
    val results = coroutineScope {
        val alunos = async { searchAlunos(q, capabilities) }
        val requests = async { searchRequests(q, capabilities) }
        val eventos = async { searchEventos(q, capabilities) }
        val usuarios = async {
            if (capabilities.contains("user.manage_all"))
                searchUsuarios(q) else emptyList()
        }
        SearchResultDto(alunos.await(), requests.await(), eventos.await(), usuarios.await())
    }
    return results
}
```

**JSON de saída (200):**

```json
{
  "alunos": [
    { "id": "abc-...", "nome": "João da Silva", "grr": "GRR20220001", "href": "/alunos/abc-..." },
    { "id": "def-...", "nome": "João Pereira", "grr": "GRR20221234", "href": "/alunos/def-..." }
  ],
  "solicitacoes": [
    { "id": "ghi-...", "protocolo": "SOL-2025-018", "tipo": "APROVEITAMENTO", "alunoNome": "João Alves", "href": "/solicitacoes/ghi-..." }
  ],
  "eventos": [
    { "id": "jkl-...", "titulo": "Palestra: IA por João Melo", "data": "2026-09-10", "href": "/eventos/jkl-..." }
  ],
  "usuarios": []
}
```

> **`usuarios: []`** para perfis sem `user.manage_all` — não é erro, é FGAC intencional.

---

### F8.1-D02 — Fan-out FGAC: perfil Aluno

Com token de Aluno (`student.view_own + request.view_own + event.view`, sem `user.manage_all`):

| Índice | Cláusula extra | Motivo |
|--------|----------------|--------|
| `alunos` | `WHERE grr = :userGrr` | Aluno só vê a si mesmo |
| `solicitacoes` | `WHERE student_id = :userId` | Aluno só vê suas solicitações |
| `eventos` | Sem restrição adicional | Eventos são públicos para logados |
| `usuarios` | **Omitido** | Sem `user.manage_all` |

O use case avalia `capabilities.contains("user.manage_all")` **antes de montar o plano de queries** — sem toque no banco para saber o que omitir.

---

### F8.1-D03 — Sem resultados (empty state pós-API)

```
GET /search?q=xyzxyz123&limit=5
Authorization: Bearer eyJhbGci...
```

**JSON de saída (200) — mesmo status HTTP, arrays vazios:**

```json
{
  "alunos": [],
  "solicitacoes": [],
  "eventos": [],
  "usuarios": []
}
```

> HTTP `200` — ausência de resultados é resposta válida, não `404`.

---

### F8.1-D04 — Timeout 5s (AbortController no cliente)

O servidor **não precisa implementar nada especial**. O cliente cancela a conexão TCP após 5s. O `SearchController` pode continuar processando, mas a resposta é descartada pelo cliente.

O frontend exibe `DS/EmptyState` com mensagem de erro de rede orientando a tentar novamente.

---

## Migração necessária

```sql
-- V0XX__search_indexes.sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Índices GIN para busca ILIKE eficiente
CREATE INDEX idx_students_nome_trgm ON students USING GIN (nome gin_trgm_ops);
CREATE INDEX idx_events_titulo_trgm ON events USING GIN (titulo gin_trgm_ops);
-- requests: busca por protocolo (prefixo) + tipo (equals)
CREATE INDEX idx_requests_protocolo ON requests (protocolo text_pattern_ops);
```

---

## DTOs esperados

```kotlin
data class SearchResultDto(
    val alunos: List<SearchItemDto>,
    val solicitacoes: List<SearchItemDto>,
    val eventos: List<SearchItemDto>,
    val usuarios: List<SearchItemDto>
)

data class SearchItemDto(
    val id: UUID,
    val label: String,      // nome / protocolo / título
    val sublabel: String?,  // GRR / tipo / data
    val href: String        // navegação React Router
)
```

---

## O que precisa ser implementado

| Arquivo a criar | Descrição |
|----------------|-----------|
| `modules/bff/api/SearchController.kt` | `GET /search?q=&limit=` com `@PreAuthorize("isAuthenticated()")` |
| `modules/bff/application/SearchUseCase.kt` | Fan-out paralelo com FGAC por capability |
| `modules/bff/application/SearchIndex*.kt` | Adaptadores por índice (alunos, requests, eventos, usuarios) |
| Migração SQL | `pg_trgm` extension + índices GIN |

---

## Checklist de Verificação

- [x] `GET /search?q=jo` → `200` com `results[]`
- [x] Perfil Aluno: sem resultados `USUARIO`; `REQUEST` só as próprias
- [x] Secretaria/Admin: pode buscar usuários
- [x] `q` em branco → `{ results: [], totalResults: 0 }`
- [x] Índices `pg_trgm` GIN (V015); queries ainda usam LIKE (GIN acelera ILIKE)
- [x] Timeout 5s no servidor (`timedOut: true`)
