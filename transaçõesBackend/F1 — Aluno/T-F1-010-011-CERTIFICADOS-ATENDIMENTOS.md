# T-F1-010 / T-F1-011 — Certificados e Atendimentos

> **Diagramas:**  
> - [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-010-CERTIFICADOS.md`](../../foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-010-CERTIFICADOS.md)  
> - [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-011-ATENDIMENTOS.md`](../../foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-011-ATENDIMENTOS.md)  
> **Status:** ✅ Certificados do aluno (`/certificates/mine`) | ✅ Atendimentos (`ServiceRecordController`)

---

## Certificados (F1.10)

Emissão automática ao encerrar evento: [T-10.4-CERTIFICADO](../transversal/T-10.4-CERTIFICADO.md).

### Controller

[`presenca/api/CertificateController.kt`](../../backend/modules/presenca/src/main/kotlin/br/ufpr/sept/so2/modules/presenca/api/CertificateController.kt)

### Endpoints

```
GET /certificates/mine
Authorization: Bearer …   (isAuthenticated)
```

Lista só os certificados do `currentUserId()`.

```json
[
  {
    "id": "uuid",
    "idEvento": "uuid",
    "hashSha256": "a1b2…",
    "chCreditadas": 4.0,
    "issuedAt": "2026-08-13T20:00:00Z",
    "_links": {
      "download": "/certificates/{id}/download-url",
      "verify": "/publico/verificar-certificado/{hash}"
    }
  }
]
```

```
GET /certificates/{id}/download-url
```

- 403 se `idAluno != currentUserId()`
- URL MinIO presignada, TTL **15 min**

O dashboard do aluno aponta `_links.certificados` → `/certificates/mine`.

---

## Atendimentos (F1.11)

A secretaria **registra** o atendimento (`POST /service-records`, capability `user.manage_students`) — ver [T-F5-SECRETARIA](../F5 — Secretaria/T-F5-SECRETARIA.md).  
O aluno **consulta, agenda e dá ciência**.

### Agendar (aluno)

```
POST /me/service-records
{ "assunto": "Revisão de matrícula", "descricao": "…", "tipo": "AGENDAMENTO" }
```

Cria registro com `estado=AGENDADO`, `idSecretario=null`, outbox `atendimentos.created`.

### Controller

[`iam/api/ServiceRecordController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/ServiceRecordController.kt)

### Listar os meus (`aluno=me`)

```
GET /service-records?aluno=me&status=PENDENTE_CIENCIA
Authorization: Bearer …  (service_record.view_own)
```

Alias: `GET /me/service-records?status=PENDENTE_CIENCIA`.

```json
{
  "content": [
    {
      "id": "uuid",
      "assunto": "Revisão de matrícula",
      "tipo": "PRESENCIAL",
      "estado": "PENDENTE_CIENCIA",
      "status": "PENDENTE_CIENCIA",
      "createdAt": "2026-08-13T18:00:00Z",
      "_links": {
        "self": "/service-records/{id}",
        "acknowledge": "/service-records/{id}/acknowledge"
      }
    }
  ]
}
```

`_links.acknowledge` só aparece se `estado=PENDENTE_CIENCIA`.

### Dar ciência

```
POST /service-records/{id}/acknowledge
```

- 403 se `idAluno != currentUserId()`
- Transição `PENDENTE_CIENCIA` → `CIENTE` + `acknowledgedAt`
- `audit_log` `SERVICE_RECORD_ACKNOWLEDGED` (com IP)

Dashboard do aluno inclui `kpis.atendimentosPendentes` (count `PENDENTE_CIENCIA`).

Criação pela secretaria enfileira `atendimentos.created` (e-mail + in-app).

---

## Checklist de Verificação

- [x] `GET /certificates/mine` → lista do aluno autenticado
- [x] `GET /certificates/{id}/download-url` → presign 15 min, só o dono
- [x] Verificação pública em `/publico/verificar-certificado/{hash}`
- [x] `GET /service-records?aluno=me` + `_links.acknowledge`
- [x] `POST /service-records/{id}/acknowledge` (ownership + audit)
- [x] `POST /me/service-records` — agendamento self-service (estado `AGENDADO`)
