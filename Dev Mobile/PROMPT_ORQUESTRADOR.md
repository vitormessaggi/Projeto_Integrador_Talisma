Você é o **orquestrador** da implementação do app mobile Android nativo (Java) do projeto Talismã Imóveis.

**Repositório:** `C:\Users\Marcio\Desktop\Projeto_Integrador_Talisma` (GitHub `vitormessaggi/Projeto_Integrador_Talisma`).
**Fonte única de verdade:** `Dev Mobile/PLANO_IMPLEMENTACAO_MOBILE.md` (branch `docs/plano-mobile`), decisões D1–D46. Leia o documento inteiro, em especial as seções 5, 6, 8 e 11. Recursos em `Dev Mobile/recursos/`. Arquitetura de referência do professor: `C:\Users\Marcio\Desktop\mobile4sem\MeuAPP`.

**Objetivo:** app completo em `Dev Mobile/talisma-app`, compilando e pronto para eu testar no Android Studio, entregue em **4 PRs draft empilhados para `mobile`** (11.4).

**Seu papel (você não escreve código do app):**
1. Preparação: se `mobile` não existir no remoto, crie-a a partir de `main` e faça push. Crie só **2 worktrees fixos**: `.claude/worktrees/mobile-a` na nova branch `feat/mobile-pr1-fundacao-vitrine` (a partir de `docs/plano-mobile`) e `.claude/worktrees/mobile-b` (trilha paralela). Reutilize-os até o fim; não use `isolation: "worktree"`.
2. Execute T01–T10 na ordem da 11.4. Em paralelo, só T02‖T03, T04‖T05 e T06‖T07 (A + B), nunca mais de 2 subagentes; um build Gradle e um emulador por vez.
3. Modelos: tasks **UI** → `model: fable`, `effort: low`; tasks **Geral** → `model: opus`, `effort: medium`.
4. Antes de cada task em B: `git -C mobile-b switch -C tmp/mobile-tNN <ponta atual de A>`. Ao concluir, rebaseie `tmp/mobile-tNN` sobre a ponta da branch de destino, avance-a só com `merge --ff-only` em A e apague a `tmp` (11.5). Conflito: pare e me pergunte.
5. Ao fechar o PR1, crie em A a branch do PR seguinte a partir da ponta do anterior (PR2 ← PR1, PR3 ← PR2, PR4 ← PR3) e integre nela a task de B que pertence a esse PR antes da próxima task de A.
6. Prompt de cada subagente: linha da task na 11.4, seções do plano a ler, caminho do worktree, hash esperado do `HEAD`, regras 11.3, critério 11.6 e: "Confirme `status` limpo e `HEAD` = <hash>. Implemente só esta task, revise contra o plano, corrija o desalinhado e commite (semântico, em português, `feat(tNN): …`). Não faça push nem abra PR, exceto se for a task que fecha o PR: aí rode o critério na ponta, faça push e abra `gh pr create --draft --base mobile` com título `[PRn] …` e corpo com itens da EAP, tasks, 'Depende de', ordem de merge, roteiro marcado e prints. Devolva apenas: task, hash final, nº do PR (se abriu) e 'concluída — pode seguir' ou 'bloqueado: <pergunta>'."
7. A cada devolução, confira o hash (e o PR: draft, base `mobile`, branch certa) e dispare a próxima. "Bloqueado": pergunte para mim e só continue com a resposta.

**Regras inegociáveis:**
- Só `Dev Mobile/` muda; nunca `Dev BackEnd/`, `Dev FrontEnd/`, `Eng. Software/` ou `Gestão de Projetos/`.
- Nenhum merge de PR, aprovação ou `gh pr ready`, nem seu nem dos subagentes; o time revisa e mergeia (PR1→PR4, "Create a merge commit").
- Padrão do professor (seção 8), só as dependências da 6.1, código simples, nenhuma regra de negócio fora do plano.

**Encerramento:** após T10, valide os 4 PRs: abertos, draft, base `mobile`, empilhados na ordem, "Depende de" correto, `assembleDebug`/`lintDebug` verdes na ponta do PR4, checklist preenchido e nada fora de `Dev Mobile/`. Falhas: subagente de correção no worktree A, novo commit no PR afetado (e rebase dos PRs seguintes só se necessário, avisando no PR). Comente no PR1 a ordem de merge e o estado de cada PR, remova os worktrees com `python -I ~/.claude/hooks/worktree-cleanup.py --only <caminho> --min-idle-min 0` e me entregue os links dos PRs e como abrir o app (contas de teste da D27).
