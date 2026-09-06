# T-F2-001 — Dashboard do egresso

> **Transação:** [`T-F2-001`](../../transaçõesBackend/F2%20—%20Egresso/T-F2-001-DASHBOARD-EGRESSO.md)  
> **Diagrama:** [`US-F2-001`](../../foundationDocs/sequenceDiagrams/F2%20—%20Egresso/US-F2-001-DASHBOARD-EGRESSO.md)  
> **IDs:** usuário com role `EGRESSO` (depois da colação [T-F5-005](../F5-secretaria/T-F5-005-egressos-diplomas.md))

Login egresso (`POST /auth/login`):

```json
{
  "identificador": "ana.egressa@ufpr.br",
  "senha": "EgressoS3nh@Forte!"
}
```

Authority `alumni.view_own` (V016). Controller: `DashboardEgressoController` + `DashboardEgressoQuery`.

---

## Passo 1 — BFF (implementado)

Cookie `access_token` da session **ou**:

```
GET {{baseUrl}}/bff/dashboard/egresso
Authorization: Bearer {{accessTokenEgresso}}
```

**Esperado 200** com blocos de diploma/certificados quando existirem, **sem** `_links.novaSolicitacao`. Cache chave `egresso:{uuid}`.

Aluno ativo chamando esta rota → **403**. Egresso em `GET /bff/dashboard/aluno` → **403**.

---

## Passo 2 — Contrato do diagrama (`/alumni/me`)

O diagrama cita `GET /alumni/me`. Se o path não existir no Swagger, use só o BFF acima. Confira [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) tag Alumni/Egresso.

Diploma (quando a colação gerou PDF):

```
GET {{baseUrl}}/graduations/{{graduationId}}/diploma-url
```

(secretaria/admin) **ou** o `_links.download` devolvido no BFF.

Certificados do egresso continuam em `GET /certificates/mine` (mesmo controller; IDOR por `idAluno`).

---

## Checklist

- [ ] BFF egresso 200
- [ ] 403 cruzado aluno ↔ egresso
- [ ] Diploma URL após [T-F5-005](../F5-secretaria/T-F5-005-egressos-diplomas.md)
