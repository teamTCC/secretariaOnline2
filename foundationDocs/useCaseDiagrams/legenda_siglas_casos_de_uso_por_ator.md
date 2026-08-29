# Legenda de siglas — por ator (P1, P2, P3)

Fonte canónica dos textos: [`diagrama_casos_de_uso_secretariaonline2.puml`](diagrama_casos_de_uso_secretariaonline2.puml).  
Cada página do ficheiro PlantUML gera um PNG: `diagrama_casos_de_uso_secretariaonline2_p1.png`, `_p2.png`, `_p3.png`.

> **Nota:** a legenda inclui **UC-AUT-07** (exchange OTT). Os ficheiros PlantUML e os PNGs **podem estar desatualizados** em relação a esta tabela — não regenerar PNG sem revisão do `.puml`.

---

## P1 — visão por módulo (`diagrama_casos_de_uso_secretariaonline2_p1`)

Tabela ordenada por **ator** (A1 → A9, depois S6). Dentro de cada ator, as siglas **UC-** estão em ordem alfabética.

| Ator | Sigla UC | Descrição (nome no diagrama) |
|------|-----------|--------------------------------|
| **A1** Visitante | UC-AUT-01 | Autenticar-se |
| **A1** Visitante | UC-AUT-02 | Recuperar senha |
| **A1** Visitante | UC-AUT-03 | Redefinir senha |
| **A1** Visitante | UC-AUT-07 | Trocar OTT (deep-link) por sessão |
| **A1** Visitante | UC-CRT-02 | Verificar protocolo (público) |
| **A1** Visitante | UC-CRT-03 | Verificar certificado (público) |
| **A1** Visitante | UC-PUB-01 | Página institucional / erro |
| **A2** Aluno | UC-ADM-10 | Busca global |
| **A2** Aluno | UC-ADM-11 | Suporte / FAQ |
| **A2** Aluno | UC-ATD-02 | Ciência do atendimento |
| **A2** Aluno | UC-AUT-01 | Autenticar-se |
| **A2** Aluno | UC-AUT-04 | Completar primeiro acesso |
| **A2** Aluno | UC-AUT-05 | Perfil e segurança |
| **A2** Aluno | UC-AUT-06 | Preferências de notificação |
| **A2** Aluno | UC-COM-01 | Hub de comunicação |
| **A2** Aluno | UC-CRT-01 | Certificados próprios |
| **A2** Aluno | UC-DASH-01 | Dashboard contextual (BFF) |
| **A2** Aluno | UC-EST-01 | Estágio (aluno) |
| **A2** Aluno | UC-FOR-01 | Submeter formativa |
| **A2** Aluno | UC-FOR-02 | Acompanhar formativa |
| **A2** Aluno | UC-PRE-03 | Confirmar presença (aluno) |
| **A2** Aluno | UC-SOL-01 | Abrir solicitação (wizard) |
| **A2** Aluno | UC-SOL-03 | Acompanhar solicitação |
| **A2** Aluno | UC-SOL-05 | Gerar protocolo PDF |
| **A2** Aluno | UC-TCC-01 | TCC (aluno) |
| **A3** Egresso | UC-AUT-01 | Autenticar-se |
| **A3** Egresso | UC-AUT-05 | Perfil e segurança |
| **A3** Egresso | UC-AUT-06 | Preferências de notificação |
| **A3** Egresso | UC-CRT-01 | Certificados próprios |
| **A3** Egresso | UC-EGR-01 | Dashboard egresso |
| **A4** Professor | UC-ADM-10 | Busca global |
| **A4** Professor | UC-AUT-01 | Autenticar-se |
| **A4** Professor | UC-AUT-05 | Perfil e segurança |
| **A4** Professor | UC-AUT-06 | Preferências de notificação |
| **A4** Professor | UC-COM-01 | Hub de comunicação |
| **A4** Professor | UC-COM-02 | Comunicado de turma |
| **A4** Professor | UC-DASH-01 | Dashboard contextual (BFF) |
| **A4** Professor | UC-EST-02 | Orientar / parecer estágio |
| **A4** Professor | UC-PRE-01 | CRUD de eventos (event.manage) |
| **A4** Professor | UC-PRE-02 | Operar validação no evento (QR/PIN, janelas) |
| **A4** Professor | UC-PRE-04 | Encerrar evento / regras |
| **A4** Professor | UC-SOL-03 | Acompanhar solicitação |
| **A4** Professor | UC-SOL-04 | Deliberar solicitação |
| **A4** Professor | UC-TCC-02 | Orientar / examinar TCC |
| **A5** Membro CAAF | UC-AUT-01 | Autenticar-se |
| **A5** Membro CAAF | UC-AUT-05 | Perfil e segurança |
| **A5** Membro CAAF | UC-AUT-06 | Preferências de notificação |
| **A5** Membro CAAF | UC-FOR-03 | Revisar formativa (só CAAF) |
| **A5** Membro CAAF | UC-FOR-04 | Painel comissão CAAF |
| **A5** Membro CAAF | UC-SOL-03 | Acompanhar solicitação |
| **A6** Membro COE | UC-AUT-01 | Autenticar-se |
| **A6** Membro COE | UC-AUT-05 | Perfil e segurança |
| **A6** Membro COE | UC-AUT-06 | Preferências de notificação |
| **A6** Membro COE | UC-EST-02 | Orientar / parecer estágio |
| **A6** Membro COE | UC-EST-03 | Painel comissão COE |
| **A7** Secretaria | UC-ADM-06 | Importar planilhas |
| **A7** Secretaria | UC-ADM-07 | Exportar relatórios |
| **A7** Secretaria | UC-ADM-08 | Estatísticas secretaria |
| **A7** Secretaria | UC-ADM-10 | Busca global |
| **A7** Secretaria | UC-ADM-12 | Tarefas internas |
| **A7** Secretaria | UC-ATD-01 | Registrar atendimento |
| **A7** Secretaria | UC-AUT-01 | Autenticar-se |
| **A7** Secretaria | UC-AUT-05 | Perfil e segurança |
| **A7** Secretaria | UC-AUT-06 | Preferências de notificação |
| **A7** Secretaria | UC-CAD-01 | CRUD alunos |
| **A7** Secretaria | UC-CAD-02 | CRUD cursos |
| **A7** Secretaria | UC-CAD-03 | CRUD disciplinas |
| **A7** Secretaria | UC-CAD-04 | Períodos e calendário |
| **A7** Secretaria | UC-COM-01 | Hub de comunicação |
| **A7** Secretaria | UC-DASH-01 | Dashboard contextual (BFF) |
| **A7** Secretaria | UC-EGR-02 | Colação / diploma |
| **A7** Secretaria | UC-EGR-03 | Listar / exportar egressos |
| **A7** Secretaria | UC-PRE-01 | CRUD de eventos (event.manage) |
| **A7** Secretaria | UC-PRE-02 | Operar validação no evento (QR/PIN, janelas) |
| **A7** Secretaria | UC-PRE-04 | Encerrar evento / regras |
| **A7** Secretaria | UC-SOL-02 | Abrir solicitação interna |
| **A7** Secretaria | UC-SOL-03 | Acompanhar solicitação |
| **A7** Secretaria | UC-SOL-04 | Deliberar solicitação |
| **A7** Secretaria | UC-SOL-06 | Triar fila de solicitações |
| **A7** Secretaria | UC-SOL-07 | Autorização de imagem (lote) |
| **A8** Coordenador | UC-ADM-09 | Relatórios coordenação |
| **A8** Coordenador | UC-ADM-10 | Busca global |
| **A8** Coordenador | UC-AUT-01 | Autenticar-se |
| **A8** Coordenador | UC-AUT-05 | Perfil e segurança |
| **A8** Coordenador | UC-AUT-06 | Preferências de notificação |
| **A8** Coordenador | UC-CAD-04 | Períodos e calendário |
| **A8** Coordenador | UC-CAD-05 | Parâmetros do curso |
| **A8** Coordenador | UC-COM-01 | Hub de comunicação |
| **A8** Coordenador | UC-DASH-01 | Dashboard contextual (BFF) |
| **A8** Coordenador | UC-PRE-02 | Operar validação no evento (QR/PIN, janelas) |
| **A8** Coordenador | UC-SOL-04 | Deliberar solicitação |
| **A8** Coordenador | UC-SOL-06 | Triar fila de solicitações |
| **A9** Administrador | UC-ADM-01 | CRUD usuários global |
| **A9** Administrador | UC-ADM-02 | FGAC (perfis / authorities) |
| **A9** Administrador | UC-ADM-03 | Tipos de solicitação |
| **A9** Administrador | UC-ADM-04 | Outbox / jobs |
| **A9** Administrador | UC-ADM-05 | Audit log |
| **A9** Administrador | UC-AUT-01 | Autenticar-se |
| **A9** Administrador | UC-AUT-05 | Perfil e segurança |
| **A9** Administrador | UC-AUT-06 | Preferências de notificação |
| **A9** Administrador | UC-COM-03 | Templates de comunicação |
| **A9** Administrador | UC-PRE-01 | CRUD de eventos (event.manage) |
| **A9** Administrador | UC-PRE-04 | Encerrar evento / regras |
| **S6** Verificador externo | UC-CRT-02 | Verificar protocolo (público) |
| **S6** Verificador externo | UC-CRT-03 | Verificar certificado (público) |


## P2 — sistemas externos (`diagrama_casos_de_uso_secretariaonline2_p2`)

Nesta página os **atores** são os sistemas **S1–S5**. Ordenação: **S1 → S5**. O diagrama usa agrupamentos **z1–z4** (não são siglas UC formais).

| Ator (sistema) | Nome no diagrama | Ref. agrupamento | Siglas UC / âmbito | Papel da ligação (rótulo) |
|----------------|------------------|------------------|---------------------|---------------------------|
| **S1** | IAM | z1 | UC-AUT-01 … UC-AUT-07 | tokens / sessão / OTT |
| **S2** | Notificações Hub | z2 | UC-SOL-01 … UC-SOL-07 | e-mail / push / in-app |
| **S3** | Motor workflow | z2 | UC-SOL-01 … UC-SOL-07 | workflow_json / transições |
| **S4** | Emissor certificados | z3 | UC-CRT-01, UC-SOL-05, UC-FOR-02, UC-PRE-04 | hash / assinatura / PDF |
| **S5** | Object storage | z4 | Anexos / PDFs (SOL, FOR, EST, TCC) | blobs (S3 / MinIO) |

**Legenda dos agrupamentos (apenas P2)**

| ID | Texto no oval | Significado |
|----|----------------|-------------|
| z1 | Grupo UC-AUT-01 a UC-AUT-06 | Identidade e acesso (IAM externo). **Legenda:** também UC-AUT-07; oval PlantUML pode não listar AUT-07. |
| z2 | Grupo UC-SOL-01 a UC-SOL-07 | Solicitações / workflow |
| z3 | UC-CRT-01, UC-SOL-05, UC-FOR-02, UC-PRE-04 | Emissão ou fecho com PDF/assinatura |
| z4 | Anexos / PDFs (SOL, FOR, EST, TCC) | Armazenamento de blobs |

**Nota (diagrama):** S2 também consome **Outbox** para entregas consistentes com o banco (ADR Outbox), conforme nota no `.puml`.

---

## P3 — include e extend (`diagrama_casos_de_uso_secretariaonline2_p3`)

Esta página **não** desenha atores humanos; a tabela segue ordem **alfabética por sigla UC base** (caso de uso principal), depois pontos de extensão / include.

| Sigla UC (base ou alvo) | Tipo (UML) | Contraparte no diagrama | Rótulo / descrição |
|-------------------------|------------|---------------------------|---------------------|
| UC-AUT-01 | Caso de uso base | — | Autenticar-se |
| UC-AUT-07 | **extend** / relacionado → UC-AUT-01 / UC-SOL-04 | `POST /auth/ott` | Trocar OTT (deep-link) por sessão |
| UC-AUT-04 | **extend** → UC-AUT-01 | primeiro acesso | `(extend) mustChangePassword` |
| UC-SOL-01 | Caso de uso base | — | Abrir solicitação |
| — | **extend** → UC-SOL-01 | Ext.: salvar rascunho (condicional) | `(extend) rascunho opcional` |
| UC-SOL-04 | Caso de uso base | — | Deliberar solicitação |
| — | **extend** → UC-SOL-04 | Ext.: deep-link JWT 1-uso (`?ott=` + `POST /auth/ott`) | `(extend) deep-link JWT 1-uso` (PNG pode ainda mostrar `?token=`) |
| — | **include** → UC-SOL-04 | Inc.: validar capability + workflow / motor S3 | `(include) capability + workflow S3` |

**Nota (diagrama):** o include lógico de autorização/workflow também aparece noutros UC (ex.: UC-PRE-03, UC-FOR-03, UC-EGR-02); ver secção 4.1 em [`casos_de_uso.md`](casos_de_uso.md).
