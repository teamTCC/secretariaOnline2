# T-F2-001 — Dashboard do Egresso e Reemissão de Certificados

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F2 — Egresso/US-F2-001-DASHBOARD-EGRESSO.md`](../../foundationDocs/sequenceDiagrams/F2 — Egresso/US-F2-001-DASHBOARD-EGRESSO.md)  
> **Status:** ✅ Implementado — endpoint BFF `GET /bff/dashboard/egresso`

---

## O que os diagramas especificam

### F2.1-D01 — `GET /alumni/me` (Dashboard read-only)

```
GET /alumni/me
Authorization: Bearer eyJhbGci...  (hasAuthority('alumni.view_own'))
```

Endpoint agrega em uma única chamada: dados pessoais do egresso, diploma (com `_links.download`), certificados (com `_links.reemitir` por item) e dados de colação.

**JSON de saída esperado (200):**

```json
{
  "nome": "Ana Paula Souza",
  "curso": "Tecnologia em Análise e Desenvolvimento de Sistemas",
  "conclusaoEm": "2026-07-15",
  "cra": 8.42,
  "diploma": {
    "id": "a3bb189e-...",
    "fileName": "diploma_ana_souza.pdf",
    "_links": {
      "download": "/alumni/me/diploma/download"
    }
  },
  "certificados": [
    {
      "id": "7c9e6679-...",
      "titulo": "Palestra: IA na Engenharia",
      "chCreditadas": 4.0,
      "emitidoEm": "2026-05-10",
      "hash": "abc123...",
      "_links": {
        "reemitir": "/certificates/7c9e6679-.../reissue",
        "verificar": "/publico/verificar-certificado/abc123..."
      }
    }
  ],
  "colacao": {
    "dataColacao": "2026-07-15",
    "local": "Auditório do Setor"
  },
  "_links": {
    "self": "/alumni/me"
  }
}
```

> Dashboard **estritamente read-only** — nenhum CTA de criação de solicitação, formativa ou estágio. O egresso não tem `request.open`, `formative.submit`, etc.

---

### F2.1-D02 — Download do Diploma (`GET /alumni/me/diploma/download`)

```
GET /alumni/me/diploma/download
Authorization: Bearer eyJhbGci...  (alumni.view_own)
```

Backend gera URL pré-assinada no MinIO (TTL=900s) para o PDF do diploma armazenado pela secretaria (ver US-F5-005). **Não regenera o PDF** — usa o existente.

**JSON de saída (200):**

```json
{
  "url": "https://minio.ufpr.br/diplomas/diploma_abc123.pdf?X-Amz-Signature=...",
  "fileName": "diploma_ana_souza.pdf",
  "expiresIn": 900
}
```

---

### F2.1-D03 — Reemitir Certificado (`POST /certificates/{id}/reissue`)

```
POST /certificates/7c9e6679-.../reissue
Authorization: Bearer eyJhbGci...  (alumni.view_own)
```

Recupera URL pré-assinada do certificado existente no MinIO **sem criar novo certificado**. O hash SHA-256 e a assinatura ED25519 são os originais.

**JSON de saída (200):**

```json
{
  "url": "https://minio.ufpr.br/certificados/cert_7c9e6679.pdf?X-Amz-Signature=...",
  "hash": "abc123...",
  "fileName": "certificado_palestra_ia_ana.pdf",
  "expiresIn": 900
}
```

> Proteção IDOR: `WHERE id = certId AND owner = alumniId` — sem acesso a certificados de outros egressos.

---

### F2.1-D04 — 403: Egresso tenta rota de aluno ativo

Este fluxo é **frontend-only** (RouteGuard verifica authorities do JWT em memória). Se houver bypass direto à API, o `@PreAuthorize` no controller retorna `403 Problem Details`:

```json
HTTP/1.1 403 Forbidden
Content-Type: application/problem+json

{
  "type": "https://secretariaonline.ufpr.br/errors/access_denied",
  "title": "Acesso negado",
  "status": 403,
  "detail": "Capability request.open ausente."
}
```

---

## Transition ALUNO → EGRESSO (depende de US-F5-005)

A transição que transforma um aluno em egresso (registrar colação, revogar capabilities de aluno, conceder `alumni.view_own`) é responsabilidade da secretaria via US-F5-005. Após essa transição, o egresso pode usar os endpoints desta HU.

---

## O que precisa ser implementado

| Arquivo a criar | Descrição |
|----------------|-----------|
| `modules/alumni/api/AlumniController.kt` | Controller com endpoints `/alumni/me`, `/alumni/me/diploma/download` |
| `modules/alumni/application/GetAlumniProfileUseCase.kt` | Agrega dados do egresso com IDOR guard |
| `modules/alumni/application/DiplomaDownloadUseCase.kt` | Presigned URL MinIO para diploma |
| `modules/alumni/application/ReissueCertificateUseCase.kt` | Presigned URL MinIO para certificado existente |
| Migração | Tabela `diploma` (ou usar `certificate` já existente) |

---

## Relação com outros módulos

- Download de diploma → [`MinioStorageService.kt`](../../backend/modules/arquivos/src/main/kotlin/br/ufpr/sept/so2/modules/arquivos/MinioStorageService.kt) já implementado
- Reemissão de certificado → [T-10.4-CERTIFICADO](../transversal/T-10.4-CERTIFICADO.md) deve ser implementado primeiro
- Transição de aluno para egresso → T-F5 (Secretaria)

---

## Checklist de Verificação

- [ ] `GET /alumni/me` → `200` com dados, diploma, certificados, `_links`
- [ ] `GET /alumni/me/diploma/download` → URL presigned MinIO TTL=900s
- [ ] `POST /certificates/{id}/reissue` → URL presigned do certificado existente
- [ ] IDOR guard: `owner = alumniId` em ambos os endpoints de download
- [ ] `alumni.view_own` obrigatório (403 sem authority)
- [ ] 403 para rotas de aluno ativo (defense-in-depth via `@PreAuthorize`)
