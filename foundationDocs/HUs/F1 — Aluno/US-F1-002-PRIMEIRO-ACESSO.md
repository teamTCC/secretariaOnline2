# US-F1-002 — Primeiro Acesso: Definir Senha e Aceitar LGPD

| Campo | Valor |
|-------|-------|
| **ID** | US-F1-002 |
| **Épico** | ALUNO-ONBOARD |
| **Tela** | F1.2 — `/primeiro-acesso` |
| **Prioridade** | **P0 — MVP v1** |
| **Plataforma** | Web + Mobile |
| **Capability** | `auth.first_access` |
| **API primária** | `POST /auth/first-access` |
| **Frames Figma** | [Default/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=58-1235) · [Default/Mobile](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=140-12715) |
| **Spec de tela** | `telasFigma/telas1/F1.2-primeiro-acesso.md` |
| **Pré-condição** | Usuário logado com `mustChangePassword = true` (US-F0-001 CA-02) |

---

## 1. História de Usuário

> **Como** um aluno que está acessando o sistema pela primeira vez (ou após reset administrativo),  
> **Quero** definir minha senha pessoal e confirmar o aceite da política de privacidade (LGPD),  
> **Para** desbloquear o acesso ao sistema e garantir que meus dados são tratados com meu consentimento informado.

---

## 2. Regras de Negócio

### Bloqueio de navegação

| ID | Regra |
|----|-------|
| **RN-F1.2-01** | Enquanto `usuario.senha_alterada = false`, o sistema **bloqueia** qualquer navegação para rotas protegidas que não seja `/primeiro-acesso`. Toda tentativa de acesso a outras rotas redireciona para `/primeiro-acesso`. |
| **RN-F1.2-02** | O `AppLayout` nesta tela opera sem os links da sidebar habilitados — sidebar exibe apenas o logo institucional. Não há como escapar da tela sem concluir o fluxo. |

### Requisitos de senha (idênticos a US-F0-003)

| ID | Regra |
|----|-------|
| **RN-F1.2-03** | A nova senha deve ter mínimo 12 caracteres, conter maiúscula, minúscula, número e caractere especial. |
| **RN-F1.2-04** | Os campos "Nova senha" e "Confirmar senha" devem ser idênticos. |
| **RN-F1.2-05** | A nova senha não pode ser igual à senha temporária gerada pelo sistema (comparação de hashes Argon2id). |

### LGPD

| ID | Regra |
|----|-------|
| **RN-F1.2-06** | O checkbox de aceite da política de privacidade (LGPD) é **obrigatório**. O botão "Continuar" permanece `disabled` enquanto o checkbox não estiver marcado. |
| **RN-F1.2-07** | O link para a política de privacidade deve abrir em modal ou nova aba. O sistema registra `usuario.metadata.aceite_lgpd_em = now()` com IP e User-Agent ao concluir o fluxo. |
| **RN-F1.2-08** | O aceite LGPD é irrevogável neste fluxo; o usuário não pode desmarcar após o submit. Gerenciamento de consentimento posterior é feito em `/perfil/privacidade` (fora do escopo do MVP). |

### Conclusão

| ID | Regra |
|----|-------|
| **RN-F1.2-09** | Ao concluir, o backend marca `senha_alterada = true`, registra `aceite_lgpd_em`, emite evento `iam.first_access_completed` e redireciona para `/inicio`. |
| **RN-F1.2-10** | Na segunda tentativa de acesso (ex.: sessão expirou antes de concluir), o sistema pode exibir CAPTCHA antes de permitir a tentativa. |

---

## 3. Critérios de Aceitação

### CA-01 — Tela exibida corretamente após login com mustChangePassword

```gherkin
Dado que o aluno fez login e recebeu mustChangePassword = true (US-F0-001 CA-02)
Quando é redirecionado para /primeiro-acesso
Então vê o card centralizado com:
  - Ícone Shield + título "Primeiro acesso"
  - Texto explicativo sobre a necessidade de definir senha
  - Campo "Nova senha" com medidor de força e toggle de visibilidade
  - Campo "Confirmar senha"
  - Checkbox LGPD com link para a política de privacidade
  - Botão "Continuar" desabilitado até os requisitos serem cumpridos
  E a sidebar não tem links de navegação ativados
```

### CA-02 — Botão habilitado somente com todos os requisitos cumpridos

```gherkin
Dado que o aluno está em /primeiro-acesso
Quando preenche os campos de senha com senha forte válida
  E os dois campos coincidem
  E marca o checkbox de aceite LGPD
Então o botão "Continuar" fica habilitado (variant=primary)

Quando desmarca o checkbox de LGPD
Então o botão "Continuar" volta a ficar desabilitado
```

### CA-03 — Validação de senha (frontend em tempo real)

```gherkin
Dado que o aluno digita no campo "Nova senha"
Então o medidor de força (DS/Progress 4 segmentos) atualiza em tempo real
  E cada requisito da lista (12 chars, maiúscula, minúscula, número, especial) exibe ✓ ao ser cumprido

Dado que o aluno preenche "Confirmar senha" diferente da nova senha
  E clica em "Continuar"
Então exibe mensagem inline "As senhas não coincidem" sem realizar chamada à API
```

### CA-04 — Conclusão bem-sucedida

```gherkin
Dado que o aluno preencheu senha forte com confirmação igual e marcou o checkbox LGPD
Quando clica em "Continuar"
Então o sistema realiza POST /auth/first-access { novaSenha, aceiteTermos: true }
  E ao receber 200 OK:
    - senha_alterada = true no backend
    - aceite_lgpd_em = now() registrado com IP e User-Agent
    - evento iam.first_access_completed emitido no audit_log
  E redireciona para /inicio (US-F1-001)
  E a partir deste momento todos os links da sidebar estão habilitados
```

### CA-05 — Tentativa de navegar para outra rota durante o bloqueio

```gherkin
Dado que o aluno tem mustChangePassword = true e está em /primeiro-acesso
Quando tenta navegar para /inicio ou qualquer rota protegida (digitando URL diretamente)
Então é redirecionado de volta para /primeiro-acesso
  E a mensagem na tela permanece intacta
```

### CA-06 — Acessibilidade

```gherkin
Dado que o aluno usa apenas teclado para navegar
Então a ordem de tab é: nova senha → confirmar senha → checkbox LGPD → link política → botão Continuar
  E erros de validação são anunciados via aria-live="assertive"
  E o checkbox possui label clicável com área de toque de 44px
```

---

## 4. Componentes de UI (Design System)

| Componente | Variante | Uso |
|------------|---------|-----|
| `DS/Input` | `type=password` | Nova senha e confirmação |
| `DS/Progress` | 4 segmentos | Medidor de força da senha |
| `DS/Checkbox` | `required` | Aceite LGPD |
| `DS/Button` | `variant=primary, disabled/enabled` | Botão Continuar |
| `DS/Card` | `max-w 560px` | Container central |

---

## 5. Contrato de API

```http
POST /auth/first-access
Cookie: access_token={jwt}
Content-Type: application/json

{
  "novaSenha": "MinhaNovaSenh@2026",
  "aceiteTermos": true
}
```

**Response (200 OK):**
```json
{ "message": "Primeiro acesso concluído com sucesso." }
```

---

## 6. Fora de escopo

- Confirmação de e-mail pessoal neste fluxo — opcional, pode ser feito em `/perfil` depois
- Configuração de 2FA no primeiro acesso — fase posterior
- Tour guiado / onboarding interativo — não previsto no MVP

---

## 7. Definição de Pronto (DoD)

- [ ] Frame Figma aprovado: Default + Submitting
- [ ] Bloqueio de navegação testado: todas as rotas protegidas redirecionam para /primeiro-acesso
- [ ] `FirstAccessUseCase` com cobertura ≥ 85%
- [ ] `aceite_lgpd_em`, IP e User-Agent gravados corretamente
- [ ] Evento `iam.first_access_completed` em `audit_log`
- [ ] Medidor de força de senha validado no backend (não só frontend)

---

## 8. Referências

| Recurso | Link / Caminho |
|---------|---------------|
| Spec de tela | `telasFigma/telas1/F1.2-primeiro-acesso.md` |
| Fluxo aluno | `foundationDocs/analysis/fluxos_por_perfil.md` §2 F1.1 |
| História de origem | [US-F0-001](../F0/US-F0-001-LOGIN.md) — Login (mustChangePassword) |
| Página Figma F1 | [Telas / F1 — Aluno](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=48-339) |
| Frame principal | [F1.2 — Primeiro acesso / Default / Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=58-1235) |
