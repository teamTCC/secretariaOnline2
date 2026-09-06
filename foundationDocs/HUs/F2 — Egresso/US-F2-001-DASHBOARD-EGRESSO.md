# US-F2-001 — Dashboard do Egresso e Reemissão de Certificados

| Campo | Valor |
|-------|-------|
| **ID** | US-F2-001 |
| **Épico** | EGRESSO-DASH |
| **Tela** | F2.1 — `/egresso/inicio` |
| **Prioridade** | P2 |
| **Plataforma** | Web + Mobile |
| **Capability** | `alumni.view_own` |
| **API primária** | `GET /alumni/me` |
| **Frames Figma** | [Default/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=166-13644) · [Skeleton/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=166-13997) · [Empty/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=166-14115) · [Default/Mobile](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=298-14168) |
| **Spec de tela** | `telasFigma/telas2/F2.1-egresso-inicio.md` |
| **Origem** | Nova — preenche o vácuo da Árvore 5 do legado (egresso ficava sem UI) |
| **Pré-condição** | Secretaria executou F5.11 (registrar diploma) → `usuario.role = EGRESSO`, capabilities de aluno revogadas |

---

## 1. História de Usuário

> **Como** um ex-aluno (egresso) da UFPR SEPT que concluiu o curso,  
> **Quero** acessar um painel com meu histórico acadêmico, diploma, certificados e dados de colação,  
> **Para** ter acesso permanente aos meus documentos de forma segura, podendo reemiti-los quando necessário sem precisar comparecer presencialmente à secretaria.

---

## 2. Regras de Negócio

### Transição para o perfil Egresso

| ID | Regra |
|----|-------|
| **RN-F2.1-01** | A transição de `ALUNO` para `EGRESSO` é disparada pela secretaria ao executar F5.11 (registrar diploma). O sistema revoga automaticamente todas as capabilities de aluno (`dashboard.view_own`, `request.open`, `formative.submit`, etc.) e concede `alumni.view_own`. |
| **RN-F2.1-02** | Após a transição, qualquer tentativa de acesso a rotas exclusivas de aluno (ex.: `/solicitacoes/nova`, `/formativas/nova`) retorna 403 e redireciona para `/erro/403`. |
| **RN-F2.1-03** | O egresso mantém acesso a: `/perfil`, `/perfil/seguranca`, `/perfil/notificacoes`, `/certificados`, e os verificadores públicos (`/publico/verificar-*`). |

### Dashboard read-only

| ID | Regra |
|----|-------|
| **RN-F2.1-04** | O dashboard do egresso é **estritamente read-only**: sem formulários de criação, sem CTAs de "nova" ação. Todos os badges exibem estado "Concluído" (variant neutral ou success). |
| **RN-F2.1-05** | Os dados exibidos no dashboard são: histórico acadêmico resumido (curso, período de conclusão, CRA se disponível), diploma (número, data), certificados emitidos durante o curso, e dados de colação (data, turma). |
| **RN-F2.1-06** | O endpoint `GET /alumni/me` retorna apenas os dados do egresso autenticado, sem dados de outros alunos. Nenhuma listagem de turma ou histórico de terceiros é acessível. |

### Reemissão de certificados e diploma

| ID | Regra |
|----|-------|
| **RN-F2.1-07** | "Reemitir PDF" **não** gera um novo certificado com nova chave de assinatura — **regenera o mesmo PDF** com o mesmo hash SHA-256 e a mesma assinatura ED25519 já existente. O documento é idêntico ao original e continua verificável em `/publico/verificar-certificado/:hash`. |
| **RN-F2.1-08** | A reemissão é útil quando o egresso perdeu o arquivo original. O backend usa o certificado já armazenado no MinIO para gerar a URL de download. |
| **RN-F2.1-09** | O botão "Reemitir PDF" aparece **somente** se `_links.reemitir` estiver presente na resposta do endpoint (controle HATEOAS). |
| **RN-F2.1-10** | Para o diploma (diferente do certificado de atividade), o botão de download gera URL pré-assinada do MinIO com o PDF do diploma oficial. |

---

## 3. Critérios de Aceitação

### CA-01 — Acesso ao dashboard após transição para Egresso

```gherkin
Dado que a secretaria registrou o diploma do aluno (F5.11)
  E o sistema atribuiu role = EGRESSO ao usuário
Quando o egresso faz login
Então é redirecionado para /egresso/inicio (não para /inicio do aluno)
  E o dashboard exibe dados read-only: curso concluído, data de conclusão, diploma, certificados
  E NÃO exibe botões de criação (Nova solicitação, Nova formativa, etc.)
```

### CA-02 — Conteúdo do dashboard do egresso

```gherkin
Dado que o egresso está em /egresso/inicio
Quando a página carrega
Então exibe KpiRow com indicadores históricos:
  - Total de horas formativas validadas (somente leitura)
  - Número de certificados emitidos
  - Situação do diploma (DS/Badge "Emitido" — success)
  E exibe seção "Diploma": número, data de emissão, botão Download (se _links.download)
  E exibe seção "Certificados": lista dos certificados com botão Reemitir PDF (se _links.reemitir)
  E exibe seção "Dados de colação": data, turma (se disponível)
  E todos os badges de status exibem variante "Concluído" (neutral ou success)
```

### CA-03 — Reemitir certificado

```gherkin
Dado que o egresso está em /egresso/inicio
  E um certificado tem _links.reemitir na resposta
Quando clica em "Reemitir PDF"
Então o sistema regenera a URL pré-assinada do MinIO para o arquivo já existente
  E o download do PDF inicia com o mesmo conteúdo, hash SHA-256 e assinatura ED25519 do original
  E o certificado reemitido continua verificável em /publico/verificar-certificado/:hash
  E NÃO é criado um novo registro de certificado na base de dados
```

### CA-04 — Tentativa de acessar rotas exclusivas de aluno

```gherkin
Dado que o usuário tem role = EGRESSO
Quando tenta acessar /solicitacoes/nova, /formativas ou /estagios
Então o sistema retorna 403 Forbidden
  E redireciona para /erro/403
  E exibe mensagem: "Você não tem permissão para acessar este recurso."
  E o botão "Ir ao início" da tela de erro leva para /egresso/inicio
```

### CA-05 — Egresso acessa perfil e certificados (rotas reaproveitadas)

```gherkin
Dado que o egresso está em /egresso/inicio
Quando navega para /perfil
Então pode editar e-mail pessoal, telefone e foto (capability user.update_own_profile mantida)
  E NÃO vê opções de notificações de solicitação ou formativa (capabilities revogadas)

Quando navega para /certificados
Então vê a lista completa dos certificados emitidos durante o curso
  E pode baixar cada um via botão Download
```

### CA-06 — Loading e estado vazio

```gherkin
Dado que o egresso está em /egresso/inicio
Quando a API está em processamento
Então exibe DS/Skeleton cobrindo os blocos do dashboard

Dado que o egresso não tem certificados
Então a seção Certificados exibe DS/EmptyState: "Nenhum certificado emitido durante o curso."
```

### CA-07 — Acessibilidade (WCAG 2.1 AA)

```gherkin
Dado que o egresso usa leitor de tela
Então os campos read-only possuem atributo aria-readonly="true" ou são renderizados como texto estático
  E a estrutura de headings é: H1 "Olá, {nome}" → H2 por seção (Diploma, Certificados, Colação)
  E os botões de download possuem aria-label descritivo: "Baixar certificado de [atividade]"
```

---

## 4. Componentes de UI (Design System)

| Componente | Variante | Uso |
|------------|---------|-----|
| `DS/KpiCard` | read-only (sem progress interativo) | Indicadores históricos |
| `DS/Card` | `default` | Seções Diploma, Certificados, Colação |
| `DS/Badge` | `success` / `neutral` | Estado "Concluído" / "Emitido" |
| `DS/Button` | `variant=secondary` | Download e Reemitir PDF |
| `DS/Skeleton` | blocos | Estado de loading |
| `DS/EmptyState` | — | Seções sem dados |

---

## 5. Contrato de API

**Request:**
```http
GET /alumni/me
Cookie: access_token={jwt}
```

**Response (200 OK):**
```json
{
  "nome": "Ana Silva",
  "curso": "Tecnologia em Análise e Desenvolvimento de Sistemas",
  "matricula": "GRR20201234",
  "conclusaoEm": "2026-02-15",
  "cra": 8.7,
  "diploma": {
    "numero": "DIP-2026-00042",
    "emitidoEm": "2026-03-10",
    "_links": {
      "download": { "href": "/alumni/me/diploma/download" }
    }
  },
  "colacao": {
    "data": "2026-03-22",
    "turma": "Turma 2026-1 TADS"
  },
  "certificados": [
    {
      "id": "...",
      "tipo": "Participação em Evento",
      "atividade": "Workshop de DevOps",
      "emitidoEm": "2025-10-05",
      "_links": {
        "download": { "href": "/certificates/.../download" },
        "reemitir": { "href": "/certificates/.../reissue" }
      }
    }
  ]
}
```

---

## 6. Fora de escopo desta história

- Acesso a dados acadêmicos detalhados (histórico de disciplinas, notas) — esses dados continuam no SIGA/UFPR Virtual
- Solicitação de segunda via de diploma — processo externo formal via secretaria presencial
- Comunicação com ex-professores/colegas pelo sistema — fora do escopo
- Reativação do perfil de aluno — não previsto; se necessário, é processo administrativo manual

---

## 7. Definição de Pronto (DoD)

- [ ] Frame Figma F2.1 aprovado (derivado de F1.1 com anotação READ ONLY)
- [ ] `GET /alumni/me` documentado em OpenAPI com todos os campos
- [ ] Transição ALUNO → EGRESSO testada em integração (F5.11 → capabilities revogadas + `alumni.view_own` concedida)
- [ ] Rotas de aluno retornam 403 quando acessadas por egresso
- [ ] Reemissão de certificado: mesmo hash/assinatura, sem novo registro na base
- [ ] Acessibilidade: campos read-only com aria-readonly, headings hierárquicos

---

## 8. Referências

| Recurso | Link / Caminho |
|---------|---------------|
| Spec de tela | `telasFigma/telas2/F2.1-egresso-inicio.md` |
| Fluxo do egresso | `foundationDocs/analysis/fluxos_por_perfil.md` §3 F2.1 |
| Mapa de rotas | `foundationDocs/analysis/telas.md` §4 F2.1 |
| Trigger (secretaria) | Coberto em US-F5 (registrar diploma — F5.11) |
| Certificados aluno | [US-F1-010](../F1/US-F1-010-CERTIFICADOS.md) |
| Verificação pública | [US-F0-007](../F0/US-F0-007-VERIFICAR-CERTIFICADO.md) |
| Análise arquitetural §11 | `foundationDocs/analysis/analise_arquitetural_secretariaonline2.md` §11 |
| Página Figma F2 | [Telas / F2 — Egresso](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=132-18387) |
| Frame principal | [F2.1 — Dashboard Egresso / Default / Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=166-13644) |
