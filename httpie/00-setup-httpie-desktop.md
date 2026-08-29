# 00 — Setup HTTPie Desktop

> Tutorial de ambiente. Não chama nenhuma transação de negócio — só deixa a coleção pronta.

## 1. Pré-requisitos

| Serviço | URL | Como saber que está up |
|---------|-----|------------------------|
| API Spring Boot | `http://localhost:8080` | `GET /actuator/health` → `{ "status": "UP" }` |
| Swagger UI | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | lista de tags (IAM, BFF — Dashboard, …) |
| OpenAPI | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | JSON grande |
| Postgres | `localhost:5432` / `secretaria_dev` | `SELECT 1` |
| Redis | `localhost:6379` | `redis-cli ping` → `PONG` (session store + cache BFF) |
| MinIO | `http://localhost:9000` | console em `:9001` (se o compose subir) |

Mailhog/Mailpit **não** estão em `ops/docker-compose.yml` (as-built 2026-08). Tokens de e-mail/OTT saem do payload em `GET /admin/outbox`. Um catcher SMTP em `:8025` é opcional e fora do compose operacional.

Se a API não sobe, não avance nos tutoriais — todos os status codes abaixo assumem o backend local.

## 2. Instalar o HTTPie Desktop

1. Baixe em [https://httpie.io/desktop](https://httpie.io/desktop).
2. Abra o app. Crie um **workspace** chamado `secretariaOnline2`.
3. Crie uma **Collection** `SO2 — transações`.

Opcional: **File → Import → OpenAPI** e cole `http://localhost:8080/v3/api-docs`. Isso gera os requests; os tutoriais desta pasta ainda são necessários para **ordem**, **CSRF**, **IDs** e **respostas esperadas**.

## 3. Environment (variáveis)

1. No HTTPie: **Environments → New** → nome `local`.
2. Abra [`ambiente/local.json`](ambiente/local.json), copie as chaves e valores.
3. Ative o environment `local` no canto da coleção.

Variáveis que você vai atualizar o tempo todo:

| Variável | Origem |
|----------|--------|
| `{{baseUrl}}` | fixo `http://localhost:8080` |
| `{{accessToken}}` | **opcional** — valor do cookie `access_token` (login **não** devolve JWT no JSON). Use só para Bearer fallback |
| `{{xsrfToken}}` | cookie `XSRF-TOKEN` OU body de `GET /auth/csrf` |
| `{{refreshToken}}` | cookie `refresh_token` (httpOnly, `Path=/auth`) — o Desktop lê o cookie jar |
| `{{userId}}`, `{{cursoId}}`, `{{requestId}}`, … | respostas GET/POST — catálogo em [01](01-ids-credenciais-e-ambiente.md) |

## 4. Headers padrão da coleção

Crie estes headers no **nível da Collection** (herdados por todos os requests):

```
Accept: application/json
Content-Type: application/json
```

**Não** coloque `Authorization` na coleção inteira — requests públicos (`/auth/**`, `/publico/**`) quebram ou ficam confusos.

Caminho preferido: **cookie jar** — `POST /auth/login` grava `access_token` (HttpOnly, `Path=/`) e o Desktop reenvia o cookie. Bearer é fallback (Swagger, CLI sem session):

```
Authorization: Bearer {{accessToken}}
```

Mutações autenticadas (`POST` / `PATCH` / `PUT` / `DELETE` **exceto** login/refresh/forgot/reset):

```
X-XSRF-TOKEN: {{xsrfToken}}
```

## 5. Cookie jar (obrigatório)

HTTPie Desktop precisa **guardar cookies** entre requests:

1. Settings da coleção → **Cookies** → Enable cookie jar.
2. `GET /auth/csrf` grava `XSRF-TOKEN` (não httpOnly).
3. `POST /auth/login` grava `access_token` (HttpOnly, `Path=/`) **e** `refresh_token` (HttpOnly, `Path=/auth`).
4. Copie o valor de `XSRF-TOKEN` para `{{xsrfToken}}` (o header Double Submit precisa ecoar o cookie).

Isentos de CSRF (não precisam do header): `/auth/login`, `/auth/refresh`, `/auth/forgot-password`, `/auth/reset-password`, Swagger, Actuator, JWKS.

`POST /publico/contato` **exige** CSRF.

## 6. Request 0 — sanity check

Crie o request `00 health`:

- Method: `GET`
- URL: `{{baseUrl}}/actuator/health`
- Auth: none

**Esperado:**

```json
{
  "status": "UP"
}
```

Status `200`. Se falhar, o backend não está no ar.

## 7. Request 1 — CSRF

Crie `01 csrf`:

- Method: `GET`
- URL: `{{baseUrl}}/auth/csrf`

**Esperado 200:**

```json
{
  "token": "a1b2c3d4-…",
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf"
}
```

Headers de resposta incluem `Set-Cookie: XSRF-TOKEN=…; Path=/; SameSite=Lax`.

**No HTTPie:** abra Cookies, copie o valor de `XSRF-TOKEN` → cole em `{{xsrfToken}}` do environment.

## 8. Request 2 — Login (dual cookie)

Cole no Body:

```json
{
  "identificador": "admin@ufpr.br",
  "senha": "Admin@123456"
}
```

- Method: `POST`
- URL: `{{baseUrl}}/auth/login`
- Body: JSON

**Esperado 200:**

```
Set-Cookie: access_token=eyJhbGci…; HttpOnly; Path=/; SameSite=Lax; Max-Age=900
Set-Cookie: refresh_token=…; HttpOnly; Path=/auth; SameSite=Lax; Max-Age=604800

{
  "mustChangePassword": false,
  "mustAcceptLgpd": false
}
```

**Não há `accessToken` no JSON.** O cookie jar reenvia `access_token` em `GET /me` e nos dashboards.

Fallback Bearer (Swagger / CLI): abra Cookies, copie o valor de `access_token` → `{{accessToken}}`. Detalhe: [T-F0-001](F0-publico/T-F0-001-login.md).

`POST /auth/refresh` **não** aceita body — lê o cookie `refresh_token` e grava um novo par + novo `sid` no Redis.

## 9. Como colar JSON nesta pasta

1. Abra o tutorial da transação.
2. Copie o bloco JSON (ou CSV) do passo.
3. No request HTTPie: aba **Body** → **JSON** (ou **Multipart** para CSV).
4. Substitua `{{placeholders}}` pelos IDs do [catálogo](01-ids-credenciais-e-ambiente.md) **ou** use o Environment se o HTTPie interpolar `{{var}}` no body (Desktop faz isso).

## 10. Como ler `_links` HATEOAS

Respostas autenticadas trazem `_links` como **mapa rel → string** (path). **Não** é HAL `{ rel: { href } }`. **Só chame a ação se a chave existir.**

Exemplo: aluno em `GET /requests/{id}` vê `self`, `events`, `attachments`. Professor com `request.deliberate` vê `defer`, `deny`, etc. (valores = `/requests/{id}/transitions`).

No HTTPie, depois de um GET de detalhe:

1. Copie a **URL string** do `_links` (não um campo `href`).
2. Crie o próximo request com esse path.
3. Rels de workflow são POST em `/transitions`; o body está no mesmo tutorial.

Isso é o contrato FGAC: a UI (e o teste manual) é cega a papéis.

## 11. Auth no HTTPie (três jeitos)

| Jeito | Quando usar |
|-------|-------------|
| Cookie `access_token` (cookie jar) | **padrão** — login grava; requests autenticados reenviam |
| Header `Authorization: Bearer {{accessToken}}` | fallback se o jar não reenviar o cookie |
| Cookie `refresh_token` | só `POST /auth/refresh` (`Path=/auth`) |

Nunca cole o JWT no chat, em print ou em commit.

## 12. Multipart (CSV / arquivos)

Requests de importação (`POST /imports/alunos`) e upload MinIO **não** usam `Content-Type: application/json`.

No HTTPie Desktop:

- Body → **Multipart** → campo `file` tipo File.
- Remova o header `Content-Type: application/json` daquele request (o Desktop gera `multipart/form-data` sozinho).

PUT no MinIO (URL presignada): cole a `uploadUrl` **inteira** na URL do request (é absoluta, não use `{{baseUrl}}`). Body = binary file. Header `Content-Type` = o mesmo que você pediu no presign (`application/pdf`, `image/jpeg`, …).

## 13. Status codes que você vai ver o tempo todo

| HTTP | Significado neste projeto |
|------|---------------------------|
| 200 | OK |
| 201 | Recurso criado (pegue o `id`) |
| 202 | Aceito; trabalho assíncrono (outbox / export) |
| 204 | Sem body (delete) |
| 400 | Validação Jakarta / argumento ilegal |
| 401 | JWT ausente/inválido **ou** credenciais genéricas (anti-enumeração) |
| 403 | Autenticado, sem authority FGAC |
| 404 | Recurso inexistente |
| 409 | Conflito (lote, unique GRR/CPF, bulk-deliberate) |
| 422 | Regra de negócio (senha fraca, reuso, schema) |
| 429 | Rate limit — leia `retryAfterSeconds` e header `Retry-After` |
| 500 | Bug; body tem `incidentId` (`INC-yyyy-xxxx`) |

Erros 4xx/5xx vêm como RFC 7807 `application/problem+json`. Tutorial: [T-F0-005](F0-publico/T-F0-005-erros.md).

## 14. Próximo passo

1. Preencha o environment com o login admin ([01](01-ids-credenciais-e-ambiente.md)).
2. Crie usuários demo ([02](02-bootstrap-usuarios-demo.md)).
3. Siga [T-F0-001](F0-publico/T-F0-001-login.md).
