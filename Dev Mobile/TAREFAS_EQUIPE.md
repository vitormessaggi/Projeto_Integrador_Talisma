# Tarefas do time para integrar o app mobile

> Base: plano `PLANO_IMPLEMENTACAO_MOBILE.md` (D1–D46). Responsáveis conforme o Gantt (`Gestão de Projetos/projeto_integrador_gantt.docx`) e o Cronograma; se o grupo redistribuiu as frentes, vale a distribuição atual.
> O app roda inteiro em **modo demonstração** (dados simulados no aparelho), então nenhuma tarefa abaixo bloqueia a implementação mobile; elas servem para ligar o app ao restante do projeto.

## 1. Todo o time — revisão e merge dos PRs do mobile
| # | Tarefa | Como |
|---|---|---|
| 1.1 | Revisar os 4 PRs draft com base `mobile`: **PR1** Fundação e vitrine (inclui o plano) → **PR2** Conta e integração → **PR3** Recursos do dispositivo → **PR4** Qualidade e entrega | Ler o corpo do PR (itens da EAP, roteiro marcado, prints em `Dev Mobile/docs/prints/`) |
| 1.2 | Aprovar e fazer o merge **em ordem PR1 → PR4**, sempre com **"Create a merge commit"** | Squash quebra a pilha; o diff de cada PR só fica "limpo" depois que o anterior é mergeado |
| 1.3 | Testar o app depois de cada merge | Android Studio → Open → `Dev Mobile/talisma-app` → Run no emulador. Contas: `cliente@teste.com` / `1234`, `corretor@teste.com` / `1234`, `bloqueada@teste.com`; código de verificação `1234`. Ou instalar o APK do GitHub Release `mobile-v1.0` (após o PR4) |
| 1.4 | Decidir quando `mobile` vai para `main` | PR `mobile` → `main` aberto pelo grupo após o PR4 |

## 2. Gestão / Documentação (Vitor — Product Owner)
| # | Tarefa | Origem |
|---|---|---|
| 2.1 | Corrigir os pacotes do mobile no Gantt para refletir a EAP 2.1–2.6 | D1, plano 3.1 |
| 2.2 | Incluir o app mobile na Especificação de Software (objetivo, requisitos de hardware do usuário, telas mobile) e corrigir o glossário (Node.js → Java/Spring Boot) | plano 3.1 |
| 2.3 | Validar as telas criadas sem protótipo (plano 6.5) — ajustes viram issues para o mobile | D11 |
| 2.4 | Aprovar o texto de Termos e Condições e informar telefone/e-mail reais da imobiliária | `recursos/textos/` |

## 3. Desenvolvimento Web — API REST (frente do back-end, EAP 3.1–3.2)
| # | Tarefa | Referência |
|---|---|---|
| 3.1 | Implementar os recursos que o app consome. A sugestão de rotas e JSON está no plano **6.9** (padrão Spring); se o formato for outro, documentar (EAP 3.1.3) e avisar o mobile, que ajusta só `ApiRotas` e os `deJson` | D13, D25, plano 7 |
| 3.2 | Imóvel com `latitude`/`longitude`, `endereco`, `destaque`, `corretorId`, fotos (URLs); listagem com filtros e paginação (`content`, `number`, `size`, `totalPages`) | D15, D16, D28 |
| 3.3 | Autenticação JWT para **cliente e corretor**, com o perfil no usuário; bloqueio após 3 falhas (RN-002, resposta 423); e-mail não verificado (403) | D35, RN-002 |
| 3.4 | Envio do código de 4 dígitos por e-mail (cadastro e redefinição de senha) | D29 |
| 3.5 | Visitas: criar (gera lead e notifica o corretor — RN-004), listar as do cliente, cancelar; telefone do cliente | D4, D30, D37 |
| 3.6 | Favoritos do usuário (listar, adicionar, remover) e upload de fotos multipart (campo `arquivo`) restrito ao corretor do imóvel (RBAC) | D10, D3 |
| 3.7 | Ambiente acessível ao emulador (`http://10.0.2.2:8080` em desenvolvimento) e HTTPS no deploy (EAP 3.5) | D26 |
| 3.8 | Avisar o mobile quando cada recurso estiver publicado: o teste é desligar o **Modo demonstração** (Perfil → 5 toques na versão) | D26 |

## 4. Desenvolvimento Web — Site (Front-end)
| # | Tarefa |
|---|---|
| 4.1 | Manter a mesma identidade do app (borgonha `#8D001A`, Poppins, logos) — os recursos convertidos estão em `Dev Mobile/recursos/` |
| 4.2 | O link "É corretor? Acesse a área interna" do app leva à área do corretor **no próprio app** (fotos); o restante da área interna continua no site |

## 5. Cibersegurança (EAP 1)
| # | Tarefa |
|---|---|
| 5.1 | Incluir o app no mapeamento de endpoints e na análise (EAP 1.1): token em `SharedPreferences` privada, tráfego em claro só no debug para `10.0.2.2`/`localhost`, logs sem dados pessoais |
| 5.2 | Registrar como risco aceito o **keystore de demonstração commitado** (D42), uso só acadêmico |
| 5.3 | Testar os endpoints usados pelo app (login, upload, visitas) nos testes de invasão controlados (EAP 1.3) |

## 6. Chatbot (EAP 4)
| # | Tarefa |
|---|---|
| 6.1 | O app não integra o chatbot no escopo atual. Se o grupo decidir usá-lo como canal (EAP 4.4.1), o ponto de entrada natural é "Fale com a imobiliária" no Perfil — isso vira uma nova decisão e uma task extra do mobile |
