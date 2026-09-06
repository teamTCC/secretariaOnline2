# Legenda de siglas — por tipo e nome (ordem alfabética)

Fonte canónica: [`diagrama_casos_de_uso_secretariaonline2.puml`](diagrama_casos_de_uso_secretariaonline2.puml).  
Ordenação: primeiro **tipo** (categoria), depois **sigla** em ordem alfabética (ex.: UC-CRT-02 antes de UC-CRT-03).

> **Nota:** UC-AUT-07 está nesta legenda. Os PNG gerados a partir do PlantUML **podem atrasar** — não assumir que o diagrama gráfico já contém o caso.

---

## Tipo: Ator primário (diagrama P1)

| Sigla | Nome no diagrama |
|-------|-------------------|
| A1 | Visitante |
| A2 | Aluno |
| A3 | Egresso |
| A4 | Professor |
| A5 | Membro CAAF |
| A6 | Membro COE |
| A7 | Secretaria |
| A8 | Coordenador |
| A9 | Administrador |

---

## Tipo: Sistema externo ou verificador (diagrama P2 e P1)

| Sigla | Nome no diagrama | Onde |
|-------|-------------------|------|
| S1 | IAM | P2 |
| S2 | Notificações Hub | P2 |
| S3 | Motor workflow | P2 |
| S4 | Emissor certificados | P2 |
| S5 | Object storage | P2 |
| S6 | Verificador externo | P1 |

---

## Tipo: Caso de uso — sigla formal UC-* (diagrama P1; texto idêntico nos diagramas por ator)

| Sigla | Descrição |
|-------|-----------|
| UC-ADM-01 | CRUD usuários global |
| UC-ADM-02 | FGAC (perfis / authorities) |
| UC-ADM-03 | Tipos de solicitação |
| UC-ADM-04 | Outbox / jobs |
| UC-ADM-05 | Audit log |
| UC-ADM-06 | Importar planilhas |
| UC-ADM-07 | Exportar relatórios |
| UC-ADM-08 | Estatísticas secretaria |
| UC-ADM-09 | Relatórios coordenação |
| UC-ADM-10 | Busca global |
| UC-ADM-11 | Suporte / FAQ |
| UC-ADM-12 | Tarefas internas |
| UC-ATD-01 | Registrar atendimento |
| UC-ATD-02 | Ciência do atendimento |
| UC-AUT-01 | Autenticar-se |
| UC-AUT-02 | Recuperar senha |
| UC-AUT-03 | Redefinir senha |
| UC-AUT-04 | Completar primeiro acesso |
| UC-AUT-05 | Perfil e segurança |
| UC-AUT-06 | Preferências de notificação |
| UC-AUT-07 | Trocar OTT (deep-link) por sessão |
| UC-CAD-01 | CRUD alunos |
| UC-CAD-02 | CRUD cursos |
| UC-CAD-03 | CRUD disciplinas |
| UC-CAD-04 | Períodos e calendário |
| UC-CAD-05 | Parâmetros do curso |
| UC-COM-01 | Hub de comunicação |
| UC-COM-02 | Comunicado de turma |
| UC-COM-03 | Templates de comunicação |
| UC-CRT-01 | Certificados próprios |
| UC-CRT-02 | Verificar protocolo (público) |
| UC-CRT-03 | Verificar certificado (público) |
| UC-DASH-01 | Dashboard contextual (BFF) |
| UC-EGR-01 | Dashboard egresso |
| UC-EGR-02 | Colação / diploma |
| UC-EGR-03 | Listar / exportar egressos |
| UC-EST-01 | Estágio (aluno) |
| UC-EST-02 | Orientar / parecer estágio |
| UC-EST-03 | Painel comissão COE |
| UC-FOR-01 | Submeter formativa |
| UC-FOR-02 | Acompanhar formativa |
| UC-FOR-03 | Revisar formativa (só CAAF) |
| UC-FOR-04 | Painel comissão CAAF |
| UC-PRE-01 | CRUD de eventos (event.manage) |
| UC-PRE-02 | Operar validação no evento (QR/PIN, janelas) |
| UC-PRE-03 | Confirmar presença (aluno) |
| UC-PRE-04 | Encerrar evento / regras |
| UC-PUB-01 | Página institucional / erro |
| UC-SOL-01 | Abrir solicitação (wizard) |
| UC-SOL-02 | Abrir solicitação interna |
| UC-SOL-03 | Acompanhar solicitação |
| UC-SOL-04 | Deliberar solicitação |
| UC-SOL-05 | Gerar protocolo PDF |
| UC-SOL-06 | Triar fila de solicitações |
| UC-SOL-07 | Autorização de imagem (lote) |
| UC-TCC-01 | TCC (aluno) |
| UC-TCC-02 | Orientar / examinar TCC |

---

## Tipo: Agrupamento lógico — só diagrama P2

Estes **não** são códigos UC oficiais; são identificadores internos do oval no PlantUML.

| Sigla | Texto no diagrama | UC referenciadas (resumo) |
|-------|-------------------|---------------------------|
| z1 | Grupo UC-AUT-01 a UC-AUT-06 | UC-AUT-01 … UC-AUT-06 (PlantUML). Legenda: também UC-AUT-07. |
| z2 | Grupo UC-SOL-01 a UC-SOL-07 | UC-SOL-01 … UC-SOL-07 |
| z3 | UC-CRT-01, UC-SOL-05, UC-FOR-02, UC-PRE-04 | UC-CRT-01, UC-SOL-05, UC-FOR-02, UC-PRE-04 |
| z4 | Anexos / PDFs (SOL, FOR, EST, TCC) | âmbito funcional SOL, FOR, EST, TCC |

---

## Tipo: Refinamento include / extend — diagrama P3

Elementos desenhados além dos códigos UC da tabela anterior (stereótipos e rótulos do `.puml`).

| Sigla / identificador | Tipo UML | Caso de uso base | Rótulo no diagrama |
|-----------------------|----------|------------------|---------------------|
| UC-AUT-04 | extend | UC-AUT-01 | `(extend) mustChangePassword` |
| UC-AUT-07 | relacionado | UC-AUT-01 / deep-link | `POST /auth/ott` — troca OTT por sessão (ausente no PNG até o `.puml` ser atualizado) |
| *(Ext. rascunho)* | extend | UC-SOL-01 | `(extend) rascunho opcional` — oval: "Ext.: salvar rascunho (condicional)" |
| *(Ext. deep-link)* | extend | UC-SOL-04 | `(extend) deep-link JWT 1-uso` — oval PlantUML pode ainda mostrar `?token=`; as-built: `?ott=` + `POST /auth/ott` |
| *(Inc. capability)* | include | UC-SOL-04 | `(include) capability + workflow S3` — oval: "Inc.: validar capability + workflow / motor S3" |

**Nota:** UC-AUT-01, UC-SOL-01 e UC-SOL-04 também aparecem como **casos de uso base** na secção "Caso de uso — sigla formal" acima; esta secção só acrescenta as **relações** modeladas em P3. Includes análogos noutros UC: ver [`casos_de_uso.md`](casos_de_uso.md) secção 4.1.

---

## Referência rápida: pacotes de módulo (nomes no P1, não são UC)

Usados como **pacotes** no diagrama P1 (`M_PUB`, `M_IAM`, …). Úteis para ler o PNG.

| ID no `.puml` | Nome no diagrama |
|---------------|------------------|
| M_PUB | Conteúdo público |
| M_DASH | Dashboards |
| M_IAM | Identidade e acesso |
| M_SOL | Solicitações / workflow |
| M_FOR | Formativas |
| M_EST | Estágio |
| M_TCC | TCC |
| M_PRES | Presença em eventos v4.1 |
| M_COM | Comunicação |
| M_ACAD | Cadastro acadêmico |
| M_DIP | Egresso / diploma |
| M_ATD | Atendimento |
| M_CERT | Certificados / verificação pública |
| M_ADM | Administração e operação |
| GLEFT | Módulos (esquerda) — agrupador |
| GRIGHT | Módulos (direita) — agrupador |
| MID | Atores (primários + S6) — agrupador |
