# 01 — IDs, credenciais e como descobrir UUIDs

Os IDs do sistema são **UUIDv7 gerados em runtime**. Nada nesta pasta é um UUID real da sua máquina — você copia da resposta HTTP ou do SQL.

Arquivo de variáveis: [`ambiente/local.json`](ambiente/local.json).

---

## Credenciais seed (únicas no Flyway)

| Perfil | Identificador | Senha | Fonte |
|--------|---------------|-------|--------|
| Admin | `admin@ufpr.br` | `Admin@123456` | [`V011__seed_demo_data.sql`](../backend/app/src/main/resources/db/migration/V011__seed_demo_data.sql) |

> O hash Argon2id no seed é um **placeholder**. Se o login admin falhar com 401, o usuário existe mas a senha não bate — recrie o hash (veja [02](02-bootstrap-usuarios-demo.md) § “Admin não autentica”).

Todos os outros usuários (`ana.aluno@ufpr.br`, professor, secretaria, …) **não vêm no seed**. Crie-os com [02-bootstrap-usuarios-demo.md](02-bootstrap-usuarios-demo.md).

Identificador de login aceita: e-mail UFPR, e-mail pessoal **ou GRR numérico** (`20210001`). O backend faz `.trim().lowercase()`.

---

## Como preencher o Environment depois de cada GET/POST

| Variável | Onde nasce |
|----------|------------|
| `accessToken` | `POST /auth/login` → `accessToken` |
| `xsrfToken` | `GET /auth/csrf` → `token` (ou cookie `XSRF-TOKEN`) |
| `userId` / `alunoId` | `GET /me` → `id` |
| `cursoId` | `GET /academico/cursos` → item `sigla=TADS` → `id` |
| `requestTypeId` | `GET /requests/types` → item `code=DECLARACAO_MATRICULA` → `id` |
| `requestId` | `POST /requests` ou `POST /requests/draft` → `id` |
| `requestAno` / `requestNumero` | `GET /requests/{id}/protocol` → `"2026/0042"` |
| `eventoId` | `POST /events` → `id` |
| `pinEntrada` | `POST /events/{id}/attendance/windows/entry` → campo `secret` da janela |
| `formativaId` | `POST /formativas` → `id` |
| `internshipId` | `POST /internships` → `id` |
| `tccId` | `POST /tccs` → `id` |
| `certificateHash` | `GET /certificates/mine` → `hashSha256` |
| `serviceRecordId` | `POST /service-records` ou `GET /me/service-records` |
| `deliveryId` | `GET /communications/me` → `deliveryId` |
| `outboxId` | `GET /admin/outbox` → `id` |
| `resetToken` | Mailhog: link `/nova-senha?token=` **ou** payload do outbox `iam.password_reset_requested` |

---

## SQL — descobrir IDs na base local

Usuário Postgres seed: `secretaria` / `localdev`, database `secretaria_dev`.

```sql
-- Usuários
SELECT id, nome, email, grr, ativo, senha_alterada
FROM usuario
ORDER BY created_at DESC;

-- Roles de um usuário
SELECT u.email, r.code
FROM usuario u
JOIN usuario_role ur ON ur.usuario_id = u.id
JOIN role r ON r.id = ur.role_id;

-- Cursos (seed TADS e ES)
SELECT id, nome, sigla, id_coordenador FROM curso;

-- Período letivo ativo
SELECT id, ano, semestre, inicio, fim, ativo
FROM periodo_letivo
WHERE ativo = true;

-- Tipos de solicitação (seed: SEGUNDA_CHAMADA, TRANCAMENTO_DISCIPLINA, DECLARACAO_MATRICULA)
SELECT id, code, descricao, ativo, prazo_dias FROM request_type;

-- Solicitações (protocolo = ano/numero_anual)
SELECT id, ano, numero_anual, request_type_code, estado, id_solicitante, id_curso
FROM request
ORDER BY created_at DESC
LIMIT 20;

-- Eventos + janelas (PIN/QR ficam no JSONB)
SELECT id, titulo, estado, attendance_mode, validation_windows
FROM event_attendance
ORDER BY created_at DESC
LIMIT 10;

-- Certificados (hash público)
SELECT id, id_aluno, hash_sha256, ch_creditadas, origem, issued_at
FROM certificate
ORDER BY issued_at DESC
LIMIT 10;

-- Outbox (e-mail ainda não enviado)
SELECT id, event_type, status, attempts, created_at, payload
FROM outbox_event
ORDER BY created_at DESC
LIMIT 20;

-- Token de reset (está no payload JSON, não numa tabela própria)
SELECT id, payload->>'token' AS reset_token, payload->>'email' AS email, status
FROM outbox_event
WHERE event_type = 'iam.password_reset_requested'
ORDER BY created_at DESC
LIMIT 5;
```

---

## Links úteis (dev)

| Recurso | URL |
|---------|-----|
| Health | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) |
| Swagger | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| OpenAPI | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |
| JWKS | [http://localhost:8080/.well-known/jwks.json](http://localhost:8080/.well-known/jwks.json) |
| Verificar certificado (troque o hash) | `http://localhost:8080/publico/verificar-certificado/{{certificateHash}}` |
| Verificar protocolo | `http://localhost:8080/publico/solicitacoes/{{requestAno}}/{{requestNumero}}` |
| Mailhog | [http://localhost:8025](http://localhost:8025) |
| Transações (implementação) | [`transaçõesBackend/README.md`](../transaçõesBackend/README.md) |
| Diagramas | [`foundationDocs/sequenceDiagrams/README.md`](../foundationDocs/sequenceDiagrams/README.md) |

---

## Papéis × authorities (o que cada token consegue chamar)

O JWT claim `authorities` é a lista FGAC. Sem a authority, o endpoint responde **403** `application/problem+json`.

| Perfil típico | Authorities que o teste usa |
|---------------|-----------------------------|
| ALUNO | `dashboard.view_own`, `request.open`, `request.view_own`, `formative.submit`, `formative.view_own`, `attendance.check_in`, `internship.view_own`, `tcc.view_own`, `communication.read`, `user.update_own_profile`, `user.update_own_password` |
| PROFESSOR | `dashboard.view_self_professor`, `event.manage`, `event.host`, `request.deliberate`, `communication.publish_class`, `tcc.supervise`, `internship.supervise` |
| SECRETARIA | `dashboard.view_secretary`, `request.view_curso`, `request.deliberate`, `user.manage_students`, `user.reset_password`, `import.run`, `export.run`, `task.manage`, `report.view_secretary` |
| COORDENADOR | `course.config`, `report.view_coordinator` |
| CAAF | `formative.review` |
| COE | `internship.review` |
| ADMIN | `system.admin`, `iam.manage_roles`, `user.manage_all`, `audit.read`, `request_type.manage` |
| EGRESSO | `alumni.view_own` (sem `request.open`) |

A matriz real está em [`V010__seed_authorities_roles.sql`](../backend/app/src/main/resources/db/migration/V010__seed_authorities_roles.sql). Para inspecionar no token: cole o JWT em [https://jwt.io](https://jwt.io) (só em máquina local; o token é RS256).

---

## Placeholders usados nos JSON

Qualquer string `{{assim}}` no body deve existir no Environment. Se o HTTPie mandar o texto literal `{{cursoId}}`, o backend responde 400 (UUID inválido).
