# T-F6 — Coordenação (config do curso + relatórios)

> **Transações:** [`T-F6-001`](../../transaçõesBackend/F6%20—%20Coordenação/T-F6-001-CONFIGURAR-CURSO.md) · [`T-F6-002`](../../transaçõesBackend/F6%20—%20Coordenação/T-F6-002-RELATORIOS.md)  
> **Diagramas:** [`US-F6-001`](../../foundationDocs/sequenceDiagrams/F6%20—%20Coordenação/US-F6-001-CONFIGURAR-CURSO.md) · [`US-F6-002`](../../foundationDocs/sequenceDiagrams/F6%20—%20Coordenação/US-F6-002-RELATORIOS.md)  
> **IDs:** `{{cursoId}}` ou sigla `TADS`, `{{alunoId}}`, `{{disciplinaId}}`, `{{coordenadorId}}`  

Login coordenador (`POST /auth/login`):

```json
{
  "identificador": "coord.tads@ufpr.br",
  "senha": "CoordS3nh@Forte!"
}
```

`{id}` do path aceita **UUID ou sigla**. Ownership: `curso.id_coordenador == currentUserId()` (admin bypass). Senão **403**.

SQL para amarrar o dono: ver [02 bootstrap](../02-bootstrap-usuarios-demo.md) Passo F.

---

## Config

```
GET {{baseUrl}}/courses/tads/config
Authorization: Bearer {{accessTokenCoordenador}}
```

**Esperado 200:**

```json
{
  "courseId": "…",
  "sigla": "TADS",
  "horasFormativasMinimas": 120,
  "duracaoCalendario": "15_SEMANAS",
  "bancaMembrosExternos": 1,
  "bancaModalidade": "PRESENCIAL",
  "regimento": null,
  "_links": {
    "self": "/courses/tads/config",
    "update": "/courses/tads/config"
  }
}
```

Copie `courseId` → `{{cursoId}}`.

```
PATCH {{baseUrl}}/courses/tads/config
X-XSRF-TOKEN: {{xsrfToken}}
```

Cole no Body:

```json
{
  "horasFormativasMinimas": 150,
  "duracaoCalendario": "15_SEMANAS",
  "bancaMembrosExternos": 1,
  "bancaModalidade": "PRESENCIAL"
}
```

Validação: horas `[0,1000]`, duração `15_SEMANAS|18_SEMANAS`, banca `1|2`, modalidade `PRESENCIAL|REMOTO|HÍBRIDO`. Audit `COURSE_CONFIG_UPDATED`. **Não** recalcula colações antigas.

---

## CRUD acadêmico legado

```
GET  {{baseUrl}}/academico/cursos/{{cursoId}}
PATCH {{baseUrl}}/academico/cursos/{{cursoId}}
POST {{baseUrl}}/academico/disciplinas
GET  {{baseUrl}}/academico/periodos-letivos
POST {{baseUrl}}/academico/periodos-letivos
```

Disciplina: 

```json
{
  "nome": "Cálculo Diferencial e Integral I",
  "codigo": "TADS-CAL1",
  "idCurso": "{{cursoId}}",
  "cargaHorariaTotal": 60,
  "creditos": 4
}
```

Campos obrigatórios: `cargaHorariaTotal` + `creditos`. Período: 

```json
{
  "ano": 2026,
  "semestre": 2,
  "inicio": "2026-08-01",
  "fim": "2026-12-31"
}
```

Copie ids → `{{disciplinaId}}` / `{{periodoId}}`.

---

## Histórico escolar (critério de colação)

```
GET {{baseUrl}}/academico/alunos/{{alunoId}}/historico
PUT {{baseUrl}}/academico/alunos/{{alunoId}}/historico/{{disciplinaId}}
```

Cole no Body:

```json
{
  "estado": "CONCLUIDA"
}
```

Valores: `CURSANDO` | `CONCLUIDA` | `REPROVADA`.

Para o aluno ficar elegível, **todas** as disciplinas ativas do curso precisam estar `CONCLUIDA`.

---

## Relatório do coordenador

```
GET {{baseUrl}}/reports/coordinator?periodo=2026-2&curso=TADS
Authorization: Bearer {{accessTokenCoordenador}}
```

Authority `report.view_coordinator`. Atalho institucional:

```
GET {{baseUrl}}/academico/relatorios/curso
```

Secretaria: [T-F5-011](../F5-secretaria/T-F5-011-estatisticas.md).
