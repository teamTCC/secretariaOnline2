# Regra obrigatória — Contexto de página no Figma MCP

**Aplicável a todos os prompts em `telasFigma/promptsTelas/` (F0–F8).**

---

## Problema a evitar

Chamadas `use_figma` que importam componentes (`importComponentSetByKeyAsync`, `importComponentByKeyAsync`) e chamam `createInstance()` **sem trocar de página** deixam instâncias órfãs na **primeira página do arquivo** (geralmente `Page 1`), fora dos frames de tela.

As instâncias do DS devem existir **somente dentro dos frames de tela** na página da fase (ex.: `Telas / F0 — Público`), nunca soltas na página padrão.

---

## Regra imutável

> **Toda instância `DS/*` ou `Shell/*` criada no arquivo `secretariaOnline2` deve ser filha direta ou indireta de um frame de tela** (`F0.x — …`, `F1.x — …`, etc.) na página correta da fase.

---

## Protocolo para scripts `use_figma`

### 1. Antes de qualquer escrita (criar frame, instanciar componente)

```js
const targetPage = figma.root.children.find((p) => p.name === "Telas / F0 — Público");
await figma.setCurrentPageAsync(targetPage);
```

Substitua o nome da página pelo da fase atual (`Telas / F1 — Aluno`, `Telas / F3 — Professor`, etc.).

`figma.currentPage` **reinicia na primeira página** a cada nova chamada `use_figma`. Sempre chame `setCurrentPageAsync` no início de **cada** script que cria ou altera nós no canvas.

### 2. Scripts somente de leitura / descoberta do DS

Preferir `search_design_system`, `get_metadata` e `get_screenshot` — **não** criar instâncias no canvas só para inspecionar props.

Se `createInstance()` for inevitável para inspecionar `componentProperties`:

- criar na página da fase, dentro de um frame temporário `__scratch__`, **ou**
- remover a instância ao final do mesmo script (`node.remove()`).

**Nunca** deixar instâncias de teste na `Page 1` ou fora dos frames de entrega.

### 3. Montagem de telas

Ordem correta:

1. `setCurrentPageAsync(página da fase)`
2. Criar ou localizar o frame `Fx.y — Nome / Variante`
3. Instanciar `Shell/*` e `DS/*` **como filhos desse frame** (ou de `Main/*` dentro dele)
4. `return` com os IDs dos frames criados

### 4. Ao finalizar a fase

Verificar que a `Page 1` (e qualquer página que não seja da fase) **não contém** instâncias órfãs:

```js
const orphans = figma.currentPage.children.filter(
  (n) => n.type === "INSTANCE" && !n.parent?.name?.startsWith("F")
);
return { orphanCount: orphans.length, names: orphans.map((n) => n.name) };
```

Se `orphanCount > 0`, remover antes de entregar.

---

## Checklist rápido (agente)

- [ ] Página da fase criada antes de instanciar (`Telas / F{n} — …`)
- [ ] `setCurrentPageAsync` na primeira linha de todo script de escrita
- [ ] Nenhuma instância top-level fora dos frames `F*.x — …`
- [ ] Scripts de inspeção não deixam lixo no arquivo
- [ ] `Page 1` vazia (ou renomeada/removida) ao concluir a fase

---

## Não fazer

- Não usar `Page 1` como área de staging ou lixeira de componentes
- Não instanciar `DS/Button`, `DS/Input`, `Shell/AuthLayout`, etc. no root de uma página
- Não assumir que `figma.currentPage` permanece entre chamadas MCP
