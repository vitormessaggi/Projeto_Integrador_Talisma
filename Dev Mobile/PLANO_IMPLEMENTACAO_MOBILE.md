# Plano de Implementação — App Mobile Talismã Imóveis (Android nativo, Java)

> Status: **pronto para execução por agente orquestrador — decisões D1–D46, telas, dados, rotas, roteiros e guia de execução definidos; nada pendente**
> Responsável: Marcio Petigrosso (EAP 2 — Desenvolvimento Mobile)
> Última atualização: 03/11/2026

---

## 1. Fontes analisadas

| Fonte | Caminho | Uso para o mobile |
|---|---|---|
| Especificação de Software v2.0 (27/11/2025) | `Eng. Software/3-Especificação de software/V2.0/` | RFs, RNs, segurança (JWT, HTTPS, RBAC), identidade visual |
| Termo de Abertura | `Eng. Software/2-Termo de abertura de projeto/` | Objetivo, escopo, riscos |
| Cronograma | `Eng. Software/1-Cronograma do projeto/` | Equipe e papéis |
| EAP | `Gestão de Projetos/EAP_Projeto_Integrador.pdf` | Pacotes 2.1–2.6 (fonte oficial do escopo mobile) |
| Gantt | `Gestão de Projetos/projeto_integrador_gantt.docx` | Datas (mobile 08/09/2026 → 09/12/2026) |
| Protótipos Mobile (8 telas, 390×844) | `Eng. Software/4 - Protótipos/Mobile/` | Telas e fluxo de navegação |
| API | `Dev BackEnd/imoveisapi/` | Estado atual da integração |
| Site | `Dev FrontEnd/talisma-imoveis/` | Cores, fonte, logo e dados de exemplo |
| App Expo Talismã (3º sem.) | `github.com/ViniciusPorcionato/3_ADS_SENAI` → `Desenvolvimento Mobile/ImobiliariaTalismaProject` | Fotos e dados de 6 imóveis, logos PNG, texto "Sobre" (D23) |
| Site Talismã (1º sem.) | `github.com/ViniciusPorcionato/1_ADS_SENAI` → `ProjetoIntegrador/WEB/Project-Imobiliaria` | Poppins, logos SVG, ícones, fotos e dados de 9 imóveis, Termos, contato (D23) |
| Projeto do professor (4º sem.) | `github.com/petzada/mobile4sem` → `MeuAPP/` e roteiros `01.md`–`05_Laboratorio.md` | **Arquitetura, build e boas práticas cobradas** (D6, D7, seção 8) |

## 2. Síntese do que já existe

### 2.1 Requisitos e regras de negócio aplicáveis ao app
| Item | Conteúdo | Impacto no app |
|---|---|---|
| RF-SITE-001 / RN-003 | Pesquisa de imóveis; filtro de preço por faixa (mín./máx.), só valores monetários | Filtro "Faixa de preço" da tela Imóveis |
| RF-SITE-003 / RN-004 | Toda solicitação de contato gera um lead e notifica o corretor do imóvel | Botão "Comprar agora — você será direcionado a um corretor" |
| RN-001 | Só imóvel aprovado por administrador é publicado | App lista apenas o que a API devolver como publicado |
| RN-002 | Bloqueio após 3 tentativas falhas (usuário interno) | Tratar resposta de conta bloqueada no login |
| Segurança | HTTPS/TLS obrigatório, JWT, Bcrypt (servidor), RBAC | Cliente HTTP com token Bearer; cleartext só em debug |
| Considerações finais | Sem pagamento online; contratação presencial | "Comprar/Alugar" = contato com corretor, nunca checkout |

### 2.2 Identidade visual (spec 4.4.3 + site)
- Cor principal borgonha **#8D001A** (variações no site: #6B0014, #6C0015, #AF001F, #FF234E).
- Fundo branco, texto preto/cinza escuro, cartões cinza claro.
- Fonte **Poppins** (TTFs em `Dev FrontEnd/talisma-imoveis/src/assets/fonts/Poppins/`, licença OFL).
- Logo: `Logo-Red.svg` / `Logo-White.svg` — já convertidos para Vector Drawable em `Dev Mobile/recursos/drawable/` (D23).

### 2.3 Telas prototipadas (mobile)
| Tela | Elementos | Pacote EAP |
|---|---|---|
| Home | Abas Comprar/Alugar, selects "Tipo de imóvel" e "Onde deseja morar?", Buscar, carrossel "Imóveis em Destaque" + "Ver todos" | 2.1.2, 2.2.1 |
| Imóveis | Abas À venda/Locação, chips Faixa de preço/Tipo/Quartos/Cidade, lista de cards | 2.2.1 |
| Detalhe do imóvel | Voltar, favoritar, foto principal + 3 miniaturas, título, preço, quartos/banheiros/vagas/área, descrição "Ver mais", CTA "Comprar agora" | 2.2.2 |
| Favoritos | Lista de salvos com coração | 2.3.1 |
| Perfil | Nome/e-mail, Dados pessoais, Meus favoritos, Meus contatos com corretores, Alterar senha, Termos e Condições, Sair | 2.3.3 |
| Login do cliente | E-mail, senha, Esqueci minha senha, Cadastre-se, "É corretor? Acesse a área interna" | 2.4.1 |
| Cadastro | Nome, CPF, e-mail, senha, confirmar senha | 2.4.1 |
| Autenticação por e-mail | Código de 4 dígitos, Reenviar código, Cancelar | 2.4.1 |

Navegação inferior: Início · Imóveis · Favoritos · Perfil.

### 2.4 Estado da API (`Dev BackEnd/imoveisapi`)
- Spring Boot 3.4.11, Java 21, Web + Data JPA + H2 + Lombok + DevTools.
- Código: só `ImoveisapiApplication` e uma classe de exemplo `Animal.java`. **Nenhuma entidade, endpoint, autenticação ou documentação (OpenAPI) existe.**
- `application.properties` só com o nome da aplicação (sem porta, banco ou CORS).

### 2.5 Estado do site (`Dev FrontEnd/talisma-imoveis`)
- Next.js 16 / React 19; só a Home (dados fixos no código, selects desabilitados), Header e Footer. Nenhuma chamada à API.

## 3. Lacunas e inconsistências encontradas

### 3.1 Documentação
1. A **Especificação v2.0 não menciona o app mobile** (objetivo, RFs, telas e requisito de hardware falam só de site e navegador).
2. Tecnologia do back-end em branco na seção 3-B; o glossário cita **Node.js**, mas a API é **Java + Spring Boot**.
3. **EAP e Gantt divergem** no mobile: Gantt usa "2.2 Visão e busca de usuários", "2.3 Dados, leitura do app", "2.4.2 Manipulação de usuários e objetos", "2.5 Segurança do dispositivo / Cadastro de fatores de login"; a EAP usa vitrine, favoritos, imóveis e visitas, fotos e mapa.
4. **Cronograma**: hoje (03/11/2026) o Gantt previa o mobile já em 2.4.3; entrega final 09/12/2026 (≈ 5 semanas). O app ainda não começou.
5. No Gantt, a coluna Concluído está em 0% para todas as frentes e os papéis aparecem como "Indefinida".

### 3.2 Protótipos × EAP
| Pacote EAP | Situação no protótipo |
|---|---|
| 2.1.3 Permissões do dispositivo | Sem tela/fluxo (câmera, localização) |
| 2.2.1 Filtros | Chips sem tela de seleção (faixa de preço, quartos, cidade) |
| 2.4.2 Visitas | **Nenhuma tela de agendar visita**; CTA só "direciona a um corretor" |
| 2.4.3 Erros/carregamento | Sem estados de carregando, vazio, erro, offline |
| 2.5.1/2.5.2 Captura e upload de fotos | **Sem tela**; cliente não cadastra imóvel (cadastro de imóvel é do corretor, IMG-09 web) |
| 2.5.3 Mapa e endereço | Detalhe não mostra endereço nem mapa |
| 2.5.4 Imóveis próximos | Sem tela/elemento |
| Perfil | Dados pessoais, Meus contatos, Alterar senha, Termos e Esqueci senha **sem protótipo mobile** |
| Detalhe | CTA "Comprar agora" não cobre imóvel de locação |

### 3.3 Integração
- Não há contrato de API (endpoints, payloads, paginação, filtros, formato de erro, JWT para cliente, envio do código por e-mail, upload multipart, lat/long do imóvel).
- A spec define JWT para "usuários internos"; login de **cliente** não está especificado.
- Cadastro pede **CPF** (dado pessoal sensível — LGPD; validação e mascaramento).

## 4. Prontidão para iniciar

**Nada está bloqueado.** Tudo que dependia da API (autenticação, visitas, favoritos remotos, upload, e-mail com código, lat/long) roda no **modo simulado** (D24–D27), com o mesmo fluxo de telas da versão real; as rotas ficam isoladas em `ApiRotas` (D25) para ligar a API sem refazer telas. As telas sem protótipo estão especificadas na seção 6.5, as regras na 6.6 e a execução pelo orquestrador na seção 11.

## 5. Decisões

| # | Tema | Decisão |
|---|---|---|
| D1 | Escopo | A **EAP (2.1–2.6)** é a referência oficial do escopo mobile; a divergência do Gantt será reportada ao PO para correção. |
| D2 | Prazo | Datas do Gantt/EAP **desconsideradas**. Entrega do app **completo** (todos os pacotes da EAP), implementado **em fases** sequenciais, sem corte de escopo. |
| D3 | Fotos (2.5.1/2.5.2) | Usadas pelo **corretor no app**: "É corretor? Acesse a área interna" → login de corretor → escolher imóvel → tirar/escolher fotos → enviar à API. Área do corretor limitada a esse fluxo. |
| D4 | Visitas (2.4.2) | CTA do detalhe abre a tela **Solicitar visita** (data/período preferido + mensagem) → POST na API, gerando o lead (RN-004). Pedidos aparecem em "Meus contatos com corretores". Rótulo do botão: "Comprar agora" (venda) / "Alugar agora" (locação). |
| D5 | Repositório | Projeto Android Studio em **`Dev Mobile/talisma-app`** neste repositório. |
| D6 | SDK / Java | **Igual ao projeto do professor (`mobile4sem/MeuAPP`)**: minSdk 24, compileSdk/targetSdk 36, Java 11, AGP 8.13.0, Gradle 8.14.3, Gradle Kotlin DSL com dependências inline. Testes: emulador API 36 (principal) + API 24 (compatibilidade). Revisada duas vezes (35 → 26/35 → padrão do professor). |
| D7 | Arquitetura | **Uma Activity por tela** no padrão do professor (`AppCompatActivity`, `EdgeToEdge.enable` + insets em `R.id.main`, `findViewById`, listeners com lambda, Intent explícita), layouts XML + Material Components. Bottom nav (`BottomNavigationView`) nas 4 Activities principais. Dados via **Repository mínimo**: poucas classes concretas (ex.: `ImovelRepository`) que decidem por `AppConfig.USAR_DADOS_SIMULADOS` entre o JSON simulado e a API — sem interfaces genéricas nem camadas extras. |
| D8 | Bibliotecas | **Só recursos nativos** para rede, dados e imagens: `HttpURLConnection` + `org.json` (HTTP/JSON), `SQLiteOpenHelper` (favoritos, cache, sessão), carregamento de imagem manual com cache em memória (`LruCache`) e disco. Execução em segundo plano com `ExecutorService` + `Handler` da main thread. Mantidos apenas AndroidX/Material Components para UI (D7). |
| D9 | Login | Vitrine aberta: **Home, Imóveis, Detalhe e Favoritos sem login**. Solicitar visita e os itens pessoais do Perfil exigem login (redireciona e volta à ação após entrar); a aba Perfil abre deslogada com convite (D31). |
| D10 | Favoritos | **Local + sincronização**: sempre gravados no SQLite (funcionam sem login/offline); quando logado, envia/recebe da API e mescla (união local ∪ remoto; remoção registrada para envio). Exige endpoints de favoritos no back-end — incluídos na lista de necessidades (seção 7). |
| D11 | Telas sem protótipo | Esqueci senha, Dados pessoais, Alterar senha, Meus contatos, Termos e Condições (e as demais novas: Solicitar visita, filtros, área do corretor, mapa) **criadas no padrão visual atual** (cartão branco sobre borgonha para autenticação; lista/formulário simples para perfil) e submetidas à validação do grupo. |
| D12 | CPF | **Opcional no cadastro**. Se vazio, é pedido ao solicitar visita. Quando informado: máscara + validação dos dígitos verificadores, nunca em log/cache local, exibido mascarado em Dados pessoais. |
| D13 | Definição da API | O mobile entrega uma **lista de necessidades por tela** (seção 7: dados e operações, sem URLs nem JSON); **o back-end define rotas e formato**. O app usa modelos próprios e converte o JSON dentro de cada Repository (um método por recurso), então só o Repository muda quando o formato for definido; roda com **repositório simulado** e liga cada endpoint à medida que for publicado. Revisada após análise (antes: contrato completo proposto pelo mobile). |
| D14 | Mapa (2.5.3) | **OpenStreetMap embutido via osmdroid** na tela de detalhe (marcador no imóvel + endereço). Exceção explícita a D8 (única biblioteca externa além de AndroidX/Material). Sem chave de API; exige `User-Agent` identificado e atribuição "© OpenStreetMap contributors". |
| D15 | Imóveis próximos (2.5.4) | Chip **"Perto de mim"** em Imóveis; permissão de localização pedida **no início do app** (2.1.3), com explicação, e o app segue funcionando se negada (chip pede de novo). Localização via `LocationManager` nativo; **distância calculada e ordenada no app** (`Location.distanceBetween`). Da API só se exige **lat/long de cada imóvel** — indispensável também para o mapa (D14); fallback: `Geocoder` nativo sobre o endereço quando lat/long vier vazio. |
| D16 | Filtros | **Filtragem e paginação na API** (finalidade, tipo, faixa de preço mín./máx. — RN-003, quartos, cidade). No repositório simulado e no cache offline o mesmo filtro roda no app. |
| D17 | Release (2.6.2) | **APK de release assinado** com o keystore de demonstração (D42). Sem AAB por ora. |
| D18 | Testes (2.6.1) | **Somente roteiro de teste manual** em aparelho/emulador (checklist por tela e por fase); sem testes automatizados. |
| D19 | CI | **Sem CI** para o app; build e APK gerados no Android Studio de quem desenvolve. |
| D20 | Rotação (2.2.3) | **Todas as telas giram sem perder estado** (`onSaveInstanceState` para filtros, posição, campos e contador de foto); `layout-land` só onde o retrato quebra (Home, Detalhe, Login/Cadastro/Verificar e-mail); `values-sw600dp/dimens.xml` para tablet. |
| D21 | Idiomas | **Só pt-BR**, mas com todo texto em `strings.xml` (placeholders numerados `%1$s`, `translatable="false"` para marca, URL, telefone), números e moeda via `NumberFormat` — deixa a tradução possível sem refatorar. |
| D22 | Pacote | `namespace`/`applicationId` **`br.com.talisma.imoveis`** (alinhado à API `br.com.talisma.api`); nome do app "Talismã Imóveis". |
| D23 | Recursos reaproveitados | Fontes, logos, ícones, 15 imóveis de exemplo (60 fotos) e textos (Termos, dados da empresa) dos projetos dos semestres anteriores ficam em `Dev Mobile/recursos/` e são copiados para o app na Fase 1. Cores seguem a spec v2.0 (`#8D001A`), não o `#C70039` do app Expo antigo. |
| D24 | Modo simulado | `ServidorSimulado` cobre **tudo de ponta a ponta** com SQLite (contas, visitas, favoritos remotos, fotos), atraso de ~600 ms e falha de rede simulável; a demonstração roda inteira sem API (6.7). |
| D25 | Rotas da API | REST provisório no padrão Spring, **todas as rotas em `ApiRotas`** e conversão em `deJson`/`paraJson` dos modelos (6.9). Enviado ao back-end como sugestão, sem obrigá-lo; trocar = editar só esses pontos. |
| D26 | Alternância simulado/API | Diálogo **"Modo demonstração"** escondido no Perfil (5 toques na versão, logado ou deslogado), salvo em `Preferencias`; padrão ligado enquanto não houver API; ao alternar, limpa sessão e cache. URL da API em `BuildConfig` vinda de `local.properties` (padrão `http://10.0.2.2:8080`). |
| D27 | Contas de teste | `assets/fake/usuarios.json`: **cliente@teste.com / 1234**, **corretor@teste.com / 1234**, **bloqueada@teste.com** (bloqueada, RN-002); código de verificação fixo **1234**; novos cadastros funcionam. Senhas do seed só valem no modo simulado. |
| D28 | Paginação | Rolagem infinita, **10 por página**, indicador no rodapé, formato de página do Spring (`content`, `number`, `size`, `totalPages`). |
| D29 | Esqueci senha | Três passos no cartão do Login: e-mail → código de 4 dígitos (`VerificarEmailActivity` em modo redefinição) → nova senha → Login com aviso. |
| D30 | Solicitar visita | Resumo do imóvel, **data** (a partir de amanhã), **período** Manhã/Tarde/Noite, **telefone/WhatsApp obrigatório** (salvo no perfil), CPF se faltar, mensagem opcional → diálogo de confirmação. |
| D31 | Perfil deslogado | Cabeçalho borgonha com convite "Entre para salvar visitas e favoritos", botões Entrar e Cadastrar; itens Fale com a imobiliária e Termos; versão no rodapé. |
| D32 | Filtros | `BottomSheetDialog` **por chip** (preço mín./máx., tipo, quartos 1/2/3/4+, cidade com buscas recentes), Limpar/Aplicar; chip ativo borgonha com o valor; "Perto de mim" é chip de alternância. |
| D33 | Área do corretor | Login no mesmo cartão (modo CORRETOR, título "Área do corretor"); depois telas brancas com toolbar borgonha, sem bottom nav: Selecionar imóvel → Fotos (grade, Câmera/Galeria, remover, enviar com progresso); Sair no topo. |
| D34 | Senha | Mínimo **6 caracteres** e confirmação igual; o servidor valida de novo. |
| D35 | Sessão | Fica logado **até tocar Sair ou receber 401**; sem refresh token. |
| D36 | Cache | **Sempre online; cache só quando a rede falha**, com banner "Você está offline". Guarda a última lista de cada filtro, destaques, 30 detalhes e fotos até 50 MB. |
| D37 | Meus contatos | Status Pendente/Confirmada/Concluída/Cancelada; **cancelar** enquanto Pendente ou Confirmada; no simulado confirma 2 min após criar. |
| D38 | Intents implícitas | **Compartilhar** no Detalhe (`ACTION_SEND`) e **Fale com a imobiliária** no Perfil com Ligar (`ACTION_DIAL`) e E-mail (`ACTION_SENDTO`). |
| D39 | Orquestração | **Orquestrador + subagentes**: **Fable (low)** para tasks de UI/telas/design e **Opus (medium)** para as demais; cada um implementa a task, revisa contra o plano, corrige, commita e devolve só "concluída"; até 2 em paralelo nos pares sem arquivos em comum (11.4). Revisada duas vezes. |
| D40 | Critério de pronto | Por task: `assembleDebug` + `lintDebug` sem erro, telas conferidas no `Talisma_API36` com prints e revisão básica. Por PR: o mesmo na ponta da branch + roteiro 6.11 da parte correspondente (11.6). |
| D41 | Git | Branch de integração **`mobile`**; PRs **draft** para `mobile`; **o time revisa, aprova e faz o merge** — orquestrador e subagentes nunca aprovam nem mergeiam; `mobile` → `main` a critério do grupo. |
| D42 | Assinatura | **Keystore de demonstração commitado** em `talisma-app/keystore/` (uso só acadêmico, nunca na Play Store): qualquer colega gera o mesmo release e as versões instalam por cima. |
| D43 | Tasks | 10 tasks (T01–T10) por item da EAP, tipo UI ou Geral, com worktree e pares paralelos (T02‖T03, T04‖T05, T06‖T07) na 11.4. |
| D44 | PRs empilhados | PR1 nasce de `docs/plano-mobile`, cada PR seguinte da ponta do anterior; todos apontam para `mobile` com "Depende de"; merge pelo time em ordem PR1→PR4 com "Create a merge commit" (11.5). |
| D45 | Quantidade de PRs | **4 PRs por escopo**: PR1 Fundação e vitrine (plano + T01–T04, EAP 2.1–2.3), PR2 Conta e integração (T05–T06, 2.4), PR3 Recursos do dispositivo (T07–T08, 2.5), PR4 Qualidade e entrega (T09–T10, 2.6). Só a task que fecha o PR faz push e abre o PR. |
| D46 | Worktrees | **Só 2 worktrees fixos** (`.claude/worktrees/mobile-a` e `mobile-b`) reutilizados no processo inteiro; a trilha paralela parte sempre da ponta atual da trilha principal e é integrada por rebase + fast-forward local pelo orquestrador, garantindo que nenhuma task comece com código desatualizado (11.5). |

## 6. Plano de implementação

### 6.1 Stack consolidada (padrão do professor — `mobile4sem/MeuAPP`)
| Item | Escolha |
|---|---|
| Projeto | Template **Empty Views Activity**, Java, Gradle **Kotlin DSL** com dependências inline (sem version catalog), AGP 8.13.0, Gradle 8.14.3 — copiar `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/wrapper/` e `.gitignore` de `mobile4sem/MeuAPP` e ajustar nomes (D6) |
| Linguagem / SDK | **Java 11**, minSdk 24, compileSdk/targetSdk 36 (D6) |
| Pacote | `br.com.talisma.imoveis`; `rootProject.name = "Talisma Imoveis"`; `app_name` "Talismã Imóveis" (D22) |
| Dependências | `androidx.appcompat:appcompat:1.7.1`, `com.google.android.material:material:1.12.0`, `androidx.activity:activity:1.9.3`, `androidx.constraintlayout:constraintlayout:2.2.1` (iguais ao MeuAPP) + `androidx.recyclerview:recyclerview:1.3.2` + `org.osmdroid:osmdroid-android:6.1.20` (D14). **Nada além disso** |
| UI | Uma `AppCompatActivity` por tela, `ConstraintLayout` raiz, tema próprio sobre `Theme.Material3.Light.NoActionBar` (app só claro), `BottomNavigationView` nas 4 telas principais (D7) |
| Navegação | Intent explícita + `putExtra`/`getIntent`; Intent implícita para compartilhar/ligar/e-mail (D38); Activity Result API (`registerForActivityResult`) para login com retorno, permissões, câmera e galeria |
| Rede | `HttpURLConnection` + `org.json` em `Tarefas` (ExecutorService + Handler) (D8) |
| Dados locais | `SQLiteOpenHelper` (`BancoLocal`) + `SharedPreferences` (`Sessao`, `Preferencias`) (D8) |
| Imagens | `CarregadorImagem`: `assets/fake/fotos/` ou URL, decodificação com `inSampleSize`, `LruCache` + cache em disco de até 50 MB (D8, D36) |
| Localização / câmera | `LocationManager` (`getLastKnownLocation` + `requestSingleUpdate`); `ActivityResultContracts.TakePicture` + `FileProvider`; `PickVisualMedia` (D3, D15) |

### 6.2 Estrutura do projeto (`Dev Mobile/talisma-app`)
Plana como no MeuAPP: Activities no pacote raiz; só dados e utilitários ganham subpacote.
```
talisma-app/
├── build.gradle.kts · settings.gradle.kts · gradle.properties · gradle/wrapper/ · .gitignore · README.md
├── keystore/talisma-demo.jks · keystore/keystore.properties          # chave de DEMONSTRAÇÃO commitada (D42, Fase 6)
└── app/
    ├── build.gradle.kts            # namespace/applicationId br.com.talisma.imoveis; buildFeatures.buildConfig = true;
    │                               # buildConfigField API_URL (de local.properties, padrão http://10.0.2.2:8080); signingConfig demo
    └── src/main/
        ├── AndroidManifest.xml     # HomeActivity LAUNCHER; permissões; FileProvider; adjustResize nos formulários;
        │                           # networkSecurityConfig (cleartext só para 10.0.2.2 e localhost)
        ├── assets/fake/            # imoveis.json, usuarios.json, fotos/*.jpg (de Dev Mobile/recursos — D23, D27)
        ├── java/br/com/talisma/imoveis/
        │   ├── HomeActivity  ImoveisActivity  DetalheImovelActivity  GaleriaActivity  FavoritosActivity  PerfilActivity
        │   ├── LoginActivity (modo cliente/corretor)  CadastroActivity  VerificarEmailActivity (modo cadastro/redefinição)
        │   ├── EsqueciSenhaActivity  NovaSenhaActivity  AlterarSenhaActivity  DadosPessoaisActivity  TermosActivity
        │   ├── SolicitarVisitaActivity  MeusContatosActivity  SelecionarImovelActivity  FotosImovelActivity
        │   ├── ImovelAdapter (lista e carrossel)  FotoAdapter (galeria e miniaturas)  ContatoAdapter  FotoEnvioAdapter
        │   ├── dados/
        │   │   ├── Imovel  Filtro  Usuario  Visita  FotoEnvio  Pagina                 # modelos (POJOs)
        │   │   ├── ImovelRepository  ContaRepository  VisitaRepository  FavoritoRepository  FotoRepository
        │   │   ├── ServidorSimulado      # "API falsa": lê assets/fake, grava nas tabelas sim_* do BancoLocal, atraso 600 ms (D24)
        │   │   ├── Api                   # HttpURLConnection: get/post/put/patch/delete JSON, multipart, Bearer, timeouts 15 s
        │   │   ├── ApiRotas              # TODAS as rotas provisórias (D25) — único ponto a editar quando a API sair
        │   │   ├── ApiException          # status HTTP + mensagem
        │   │   ├── BancoLocal            # SQLiteOpenHelper: favoritos, cache, buscas, tabelas sim_*
        │   │   ├── Sessao                # token e usuário logado (SharedPreferences "sessao")
        │   │   └── Preferencias          # modo demonstração, localização já pedida (SharedPreferences "config")
        │   └── util/
        │       ├── Tarefas  CarregadorImagem  Validacoes  Formatos  BarraNavegacao  EstadoTela  Localizacao
        └── res/
            ├── layout/ · layout-land/  # activity_*.xml, item_*.xml, sheet_*.xml, estado_tela.xml
            ├── values/ · values-sw600dp/  # strings, colors, dimens, themes, styles · dimens de tablet
            ├── drawable/  font/  mipmap-*/  xml/ (file_paths.xml, network_security_config.xml)
```
**Repository mínimo (D7):** cada Repository tem métodos simples (`listar(filtro, pagina, Callback)`) e decide internamente: `Preferencias.modoDemonstracao()` → `ServidorSimulado`; senão → `Api` + `ApiRotas`. A conversão JSON ↔ modelo fica num método `deJson`/`paraJson` do próprio modelo. Callback único: `interface Callback<T> { void sucesso(T dado); void erro(ApiException e); }` declarado em `Tarefas`.

**Regras de código (seção 8):** ids nomeados (`btnBuscar`, `edtEmail`, `imgCapa`, `txtPreco`, `rvImoveis`, `chipPreco`); nenhum texto, cor ou dimensão fixa no XML; hierarquia ≤ 3 níveis; formulários em `ScrollView`; `Log` com `TAG` sem dado pessoal; `EdgeToEdge.enable(this)` + insets em `R.id.main` em toda Activity.

### 6.3 Mapa de telas e navegação
| Activity | Origem | Login | Bottom nav | EAP |
|---|---|---|---|---|
| `HomeActivity` (LAUNCHER) | protótipo | não | Início | 2.1.2, 2.1.3, 2.2.1 |
| `ImoveisActivity` + sheets de filtro | protótipo + D32 | não | Imóveis | 2.2.1, 2.3.3, 2.5.4 |
| `DetalheImovelActivity` | protótipo + D14/D38 | não | — | 2.2.2, 2.5.3 |
| `GaleriaActivity` | nova | não | — | 2.2.2 |
| `FavoritosActivity` | protótipo | não | Favoritos | 2.3.1 |
| `PerfilActivity` (logado / convite — D31) | protótipo + D31 | não | Perfil | 2.3.3 |
| `LoginActivity` (extra `modo` = CLIENTE \| CORRETOR) | protótipo + D33 | — | — | 2.4.1, 2.5.1 |
| `CadastroActivity`, `VerificarEmailActivity` (extra `modo` = CADASTRO \| REDEFINICAO) | protótipo | — | — | 2.4.1 |
| `EsqueciSenhaActivity`, `NovaSenhaActivity` | novas (D29) | — | — | 2.4.1 |
| `AlterarSenhaActivity`, `DadosPessoaisActivity`, `MeusContatosActivity` | novas | sim | — | 2.4.1, 2.4.2 |
| `TermosActivity` | nova | não | — | 2.4.1 |
| `SolicitarVisitaActivity` | nova (D30) | sim | — | 2.4.2 |
| `SelecionarImovelActivity`, `FotosImovelActivity` | novas (D33) | corretor | — | 2.5.1, 2.5.2 |

Fluxos principais:
- Home → Buscar / Ver todos → Imóveis (filtro por extra `filtro` em JSON) → Detalhe (extra `imovel_id`) → Galeria (extras `imovel_id`, `posicao`).
- Detalhe → CTA → (sem login: Login com retorno) → Solicitar visita → diálogo de confirmação → volta ao Detalhe.
- Perfil (convite) → Entrar → Login → (sucesso) volta ao Perfil logado. Login → Cadastre-se → Cadastro → Verificar e-mail → Perfil logado.
- Login → Esqueci minha senha → EsqueciSenha → VerificarEmail(REDEFINICAO) → NovaSenha → Login com aviso.
- Login → "É corretor? Acesse a área interna" → Login(CORRETOR) → SelecionarImovel → FotosImovel. Sessão de corretor abre direto em SelecionarImovel ao tocar na área interna.
- Bottom nav: `BarraNavegacao.configurar(activity, R.id.nav_x)` abre a Activity com `FLAG_ACTIVITY_REORDER_TO_FRONT` e `overridePendingTransition(0, 0)`; Voltar na Home sai do app.
- Login com retorno: quem exige login chama `LoginActivity` via `registerForActivityResult`; `RESULT_OK` continua a ação.

### 6.4 Design system (extraído dos protótipos)
**Cores (`colors.xml`)**
| Nome | Valor | Uso |
|---|---|---|
| `borgonha` | `#8D001A` | primária: botões, abas ativas, preços, cabeçalhos, ícone ativo |
| `borgonha_escuro` | `#6B0014` | pressionado, ícones de contato |
| `borgonha_claro` | `#AF001F` | detalhes do logo, links |
| `texto` | `#1E1E1E` | títulos e texto principal |
| `texto_secundario` | `#6B6B6B` | localização, legendas, ícones inativos |
| `superficie` | `#F2F2F2` | cartões, campos, chips de característica |
| `borda` | `#D9D9D9` | contorno de campos, chips e divisórias |
| `placeholder_foto` | `#DADADA` | fundo enquanto a foto carrega |
| `branco` | `#FFFFFF` | fundo das telas, texto sobre borgonha |
| `sucesso` / `alerta` / `erro` | `#1E7D32` / `#B26A00` / `#B3261E` | status de visita e mensagens |

**Tipografia (`res/font`: poppins_regular/medium/semibold/bold; família `@font/poppins`)**
| Estilo | Tamanho / peso | Uso |
|---|---|---|
| `Texto.Destaque` | 26sp Bold | "O imóvel que você procura está aqui!" |
| `Texto.TituloTela` | 22sp SemiBold | "Imóveis", "Favoritos", "Detalhes" (18sp na toolbar) |
| `Texto.TituloSecao` | 18sp SemiBold borgonha | "Imóveis em Destaque", "Verifique seu e-mail" |
| `Texto.TituloCartao` | 15sp SemiBold | título do imóvel no cartão |
| `Texto.Preco` | 16sp Bold borgonha (22sp no Detalhe) | preço |
| `Texto.Rotulo` | 13sp Medium | rótulos de campo |
| `Texto.Corpo` | 14sp Regular | textos e descrições |
| `Texto.Legenda` | 12sp Regular secundário | localização, notas |

**Dimensões (`dimens.xml`; `values-sw600dp` aumenta margem para 48dp e colunas da lista para 2)**: margem de tela 20dp · espaçamento 8/12/16/24dp · raio cartão 12dp · raio botão 10dp · raio campo 8dp · raio cartão de autenticação 16dp · altura de botão e campo 52dp (mín. 48dp) · cartão de lista 112dp de altura com foto de 120dp · carrossel 200×220dp · foto principal do Detalhe 16:10 · miniatura 72dp.

**Componentes (`styles.xml`)**
- `Botao.Primario`: fundo borgonha, texto branco 15sp SemiBold, raio 10dp. `Botao.Contorno`: borda e texto borgonha (Alugar inativo, Sair).
- `Campo`: `TextInputLayout` OutlinedBox, fundo `superficie`, borda `borda`, raio 8dp, erro abaixo do campo.
- `Abas`: par de botões lado a lado (`MaterialButtonToggleGroup`), selecionado borgonha/branco, outro branco/borgonha (Home) ou cinza (Imóveis).
- `Chip.Filtro`: contorno `borda`, texto 13sp; ativo: fundo borgonha, texto branco, rótulo com o valor.
- `CartaoImovel` (horizontal): foto à esquerda 120dp, título, localização e preço à direita; coração opcional no canto (Favoritos).
- `CartaoDestaque` (vertical): foto 16:10, título, localização e preço.
- `CartaoAuth`: cartão branco raio 16dp, centralizado sobre fundo borgonha, com `ic_marcador_casa` 48dp + "Talismã Imóveis" 22sp SemiBold.
- `ItemMenu` (Perfil): linha de 56dp, texto 15sp, seta `>` borgonha, divisória `borda`.
- `estado_tela.xml`: `ProgressBar` borgonha, ícone, texto e `Botao.Contorno` "Tentar novamente"; variantes: CARREGANDO, VAZIO, ERRO, OFFLINE (`EstadoTela.mostrar(view, tipo, mensagem)`).
- Banner offline: faixa `alerta` 32dp no topo, "Você está offline — mostrando dados salvos".
- Bottom nav: fundo branco, borda superior `borda`, ícones outline (`ic_inicio`, `ic_busca`, `ic_favorito`, `ic_perfil` — Material Symbols), ativo borgonha com rótulo SemiBold.
- Ícones extras (Material Symbols, vetor): voltar, coração cheio/vazio, compartilhar, câmera, galeria, lixeira, localização, cama, banheira, carro, área, calendário, check, sair, nuvem-off.

### 6.5 Especificação das telas
Telas do protótipo seguem o PNG correspondente em `Eng. Software/4 - Protótipos/Mobile/`; telas novas seguem a descrição abaixo com os componentes da seção 6.4. Toda tela de dados usa `estado_tela.xml`.

**HomeActivity** (protótipo Home) — barra branca com `ic_marcador_casa` + "Talismã" 20sp SemiBold; bloco borgonha com `Texto.Destaque` branco e cartão branco: `Abas` Comprar | Alugar (`tgFinalidade`), "Tipo de imóvel" (`spnTipo`: Selecione, Casa, Apartamento, Terreno), "Onde deseja morar?" (`spnCidade`: Selecione + cidades do repositório), `btnBuscar`. Abaixo, "Imóveis em Destaque" + link "Ver todos" (`txtVerTodos`) e `rvDestaques` horizontal com `CartaoDestaque`. Primeira abertura: diálogo explicando o uso da localização → pedido de permissão (D15). Paisagem: bloco de busca à esquerda e destaques à direita.

**ImoveisActivity** (protótipo Imóveis) — título "Imóveis"; `Abas` À venda | Locação; linha rolável de chips: Faixa de preço, Tipo, Quartos, Cidade, Perto de mim (D32); `rvImoveis` com `CartaoImovel`, rolagem infinita de 10 em 10 (D28) com `ProgressBar` no rodapé. Toque no cartão → Detalhe. Vazio: "Nenhum imóvel encontrado com esses filtros" + "Limpar filtros". Com "Perto de mim" ativo, cada cartão mostra "a X km" e a lista vem ordenada por distância (D15). Filtro aplicado é salvo em buscas recentes (D24/2.3.3); ao abrir sem extra, usa o último filtro.
- `sheet_preco.xml`: "Faixa de preço", dois campos R$ (mín., máx.), erro se mín. > máx. (RN-003), Limpar | Aplicar.
- `sheet_tipo.xml`: opções Casa, Apartamento, Terreno (`RadioGroup`), Limpar | Aplicar.
- `sheet_quartos.xml`: chips 1, 2, 3, 4+ (seleção única), Limpar | Aplicar.
- `sheet_cidade.xml`: lista de cidades com `RadioButton` + "Buscas recentes" (últimas 5) no topo, Limpar | Aplicar.

**DetalheImovelActivity** (protótipo Detalhe) — barra: voltar, "Detalhes", compartilhar (`btnCompartilhar` — D38), coração (`btnFavorito`). `imgPrincipal` 16:10 + `rvMiniaturas` (72dp) — toque abre Galeria. Título, preço (`/mês` em locação), linha de 4 `superficie` com Quartos, Banheiros, Vagas, Área. "Descrição" com 4 linhas e "Ver mais"/"Ver menos". Seção "Localização": endereço com ícone + `MapView` osmdroid 180dp com marcador, zoom 15, sem rolagem do mapa ao rolar a tela; sem lat/long: tenta `Geocoder`, se falhar mostra só o endereço (D14, D15). Rodapé fixo: `btnInteresse` "Comprar agora" | "Alugar agora" + legenda "Você será direcionado a um corretor" (D4). Paisagem: fotos à esquerda, informações roláveis à direita.

**GaleriaActivity** (nova) — fundo preto, foto em tela cheia com deslize horizontal (`RecyclerView` + `PagerSnapHelper`), contador "2/4" no topo e botão fechar branco.

**FavoritosActivity** (protótipo Favoritos) — título "Favoritos", subtítulo "Imóveis que você salvou"; `CartaoImovel` com coração cheio (toque remove com `Snackbar` "Removido · Desfazer"). Vazio: ícone coração + "Você ainda não salvou imóveis" + botão "Explorar imóveis" → Imóveis. Funciona offline (D10).

**PerfilActivity** (protótipo Perfil + D31) — logado: cabeçalho borgonha com avatar (ícone branco em círculo), nome e e-mail; itens: Dados pessoais, Meus favoritos (→ aba Favoritos), Meus contatos com corretores, Alterar senha, Fale com a imobiliária (D38: sheet com telefone, e-mail, endereço, horário e botões Ligar/Enviar e-mail), Termos e Condições; `Botao.Contorno` "Sair" (confirmação); rodapé "Versão 1.0". Deslogado: cabeçalho com ícone genérico e "Entre para salvar visitas e favoritos", botões Entrar (primário branco sobre borgonha) e Cadastrar (contorno branco); itens só Fale com a imobiliária e Termos; rodapé "Versão 1.0". **5 toques na versão** abrem o diálogo "Modo demonstração" com `MaterialSwitch` e o aviso "Ao alternar, a sessão e os dados salvos serão limpos" (D26).

**LoginActivity** (protótipo Login do cliente) — fundo borgonha, `CartaoAuth`, "Acesse sua conta", E-mail, Senha (com olho), `btnEntrar`, "Esqueci minha senha", "Não possui uma conta? Cadastre-se aqui", divisória, "É corretor? Acesse a área interna". Modo CORRETOR: título "Área do corretor", esconde cadastro/esqueci/corretor e mostra "Sou cliente" (volta). Erros: credencial inválida no campo senha; conta bloqueada em diálogo "Conta bloqueada após 3 tentativas. Procure a imobiliária." (RN-002); e-mail não verificado → VerificarEmail(CADASTRO); perfil diferente do modo → "Esta conta não é de corretor/cliente".

**CadastroActivity** (protótipo Cadastro) — voltar, `CartaoAuth`, "Cadastro de Usuário", Nome, CPF (opcional, máscara 000.000.000-00, valida dígitos — D12), E-mail, Senha (mín. 6 — D34), Confirmar senha, aceite "Li e aceito os Termos e Condições" (link → Termos; obrigatório), `btnCadastrar` → VerificarEmail(CADASTRO). E-mail já cadastrado: erro no campo.

**VerificarEmailActivity** (protótipo Autenticação por e-mail) — voltar, `CartaoAuth`, "Verifique seu e-mail", texto com o e-mail em negrito, 4 campos de 1 dígito com avanço automático e colar, `btnEnviar`, "Não recebeu? Reenviar código" (bloqueado 30 s com contagem), "Cancelar". CADASTRO → login automático → volta ao fluxo de origem; REDEFINICAO → NovaSenha.

**EsqueciSenhaActivity** (nova, D29) — `CartaoAuth`, "Esqueceu sua senha?", "Informe seu e-mail para receber um código", E-mail, `btnEnviarCodigo`, "Voltar ao login". **NovaSenhaActivity** (nova) — `CartaoAuth`, "Crie uma nova senha", Nova senha, Confirmar, `btnSalvar` → Login com `Snackbar` "Senha alterada. Entre com a nova senha".

**AlterarSenhaActivity** (nova) — toolbar branca com voltar e "Alterar senha"; Senha atual, Nova senha, Confirmar; `btnSalvar`; sucesso → `Toast` e fecha. **DadosPessoaisActivity** (nova) — toolbar "Dados pessoais"; Nome, E-mail (somente leitura), CPF (mascarado `***.***.***-12` com "Editar" se vazio), Telefone/WhatsApp (máscara (00) 00000-0000); `btnSalvar`. **TermosActivity** (nova) — toolbar "Termos e Condições"; `ScrollView` com seções de `recursos/textos/termos_e_condicoes.md` em `strings.xml`.

**SolicitarVisitaActivity** (nova, D30) — toolbar "Solicitar visita"; cartão-resumo do imóvel (foto 64dp, título, preço); "Data preferida" (campo que abre `MaterialDatePicker`, a partir de amanhã); "Período" com chips Manhã, Tarde, Noite; Telefone/WhatsApp (obrigatório, pré-preenchido); CPF (aparece só se o usuário não tiver); Mensagem (opcional, 300 caracteres com contador); `btnEnviarSolicitacao`. Sucesso: diálogo com ícone check "Solicitação enviada! Um corretor entrará em contato para confirmar a visita." + botões "Ver meus contatos" | "OK".

**MeusContatosActivity** (nova, D37) — toolbar "Meus contatos"; `ContatoAdapter`: foto 64dp, título do imóvel, "12/11/2026 · Tarde", chip de status (Pendente `alerta`, Confirmada `sucesso`, Concluída `texto_secundario`, Cancelada `erro`), botão "Cancelar" quando Pendente/Confirmada (diálogo de confirmação). Toque abre o Detalhe. Vazio: "Você ainda não solicitou visitas".

**SelecionarImovelActivity** (nova, D33) — toolbar borgonha "Área do corretor" com "Sair"; saudação "Olá, {nome}"; lista `CartaoImovel` dos imóveis do corretor com contagem "4 fotos"; toque → FotosImovel. **FotosImovelActivity** (nova) — toolbar borgonha com voltar e título do imóvel; "Fotos atuais" (grade 3 colunas, somente leitura) e "Novas fotos" (grade 3 colunas com lixeira por foto e estado: aguardando, enviando com `ProgressBar`, enviada ✓, falhou ↻); botões `btnCamera` e `btnGaleria` (até 10 por envio); `btnEnviarFotos` ("Enviar 3 fotos"). Cada foto é redimensionada para no máximo 1600 px e JPEG 80% antes do envio; falha permite tocar para reenviar (2.5.2).

### 6.6 Comportamentos e regras
| Regra | Definição |
|---|---|
| Senha | mínimo 6 caracteres; confirmar igual (D34) |
| E-mail | `Patterns.EMAIL_ADDRESS` |
| CPF | opcional; máscara; dígitos verificadores; nunca em log/cache; exibido mascarado (D12) |
| Telefone | obrigatório só na visita; máscara (00) 00000-0000; salvo no perfil (D30) |
| Sessão | dura até Sair ou 401 (D35) |
| 401 | limpa sessão, abre Login com retorno e mensagem "Sua sessão expirou" |
| Cache | sempre online; cache só se a rede falhar; banner offline (D36) |
| Favoritos | local sempre; logado: ao entrar e a cada alteração, envia pendentes e baixa os remotos (união; remoções marcadas) (D10) |
| Visitas | status PENDENTE → CONFIRMADA → CONCLUIDA, ou CANCELADA; cancelar só PENDENTE/CONFIRMADA (D37) |
| Paginação | 10 por página, carrega ao faltar 3 itens para o fim (D28) |
| Permissões | localização na 1ª abertura da Home; câmera ao tocar `btnCamera`; negada → explicação + atalho para configurações; app segue funcionando |
| Rotação | estado salvo em `onSaveInstanceState` (filtro, página, posição, campos, fotos pendentes) (D20) |
| Formatos | moeda `NumberFormat.getCurrencyInstance(new Locale("pt","BR"))` sem centavos; "/mês" em locação; área "65 m²"; distância "a 1,2 km" |
| Mensagens de erro | rede: "Sem conexão. Verifique sua internet."; servidor: "Algo deu errado. Tente novamente."; 4xx: mensagem da API |

### 6.7 Modo simulado e alternância (D24, D26, D27)
- `ServidorSimulado` implementa as mesmas operações da seção 6.9, com `Thread.sleep(600)` dentro de `Tarefas`, lendo `assets/fake/imoveis.json` e `usuarios.json` e gravando nas tabelas `sim_*`.
- Na primeira execução copia `usuarios.json` para `sim_usuario`. Login errado incrementa `tentativas`; na 3ª falha marca `bloqueado` (RN-002). Código de verificação sempre `1234`. Token simulado: `"simulado-" + id`.
- Visita PENDENTE vira CONFIRMADA quando lida 2 min após `criada_em`. Upload grava a foto em `filesDir/fotos_enviadas/` e a registra em `sim_foto`; as fotos enviadas aparecem em "Fotos atuais" e na galeria do Detalhe.
- Erro simulável: `Preferencias.simularFalhaRede` (no mesmo diálogo do Modo demonstração, switch "Simular sem conexão") faz o servidor simulado lançar erro de rede — usado no roteiro para testar estados e cache.
- Padrão: modo demonstração **ligado** enquanto não houver API. Ao alternar: limpa `Sessao`, tabelas de cache e favoritos não sincronizados exibem aviso antes.

### 6.8 Modelo de dados
**Modelos (POJOs com `deJson(JSONObject)` / `paraJson()`)**
| Classe | Campos |
|---|---|
| `Imovel` | `long id; String titulo, finalidade (VENDA\|LOCACAO), tipo (CASA\|APARTAMENTO\|TERRENO); double preco; String bairro, cidade, uf, endereco; int quartos, banheiros, vagas; Double area; String descricao; Double latitude, longitude; boolean destaque; long corretorId; List<String> fotos` (+ `Float distanciaKm` calculado no app) |
| `Filtro` | `String finalidade, tipo, cidade; Double precoMin, precoMax; Integer quartosMin; boolean pertoDeMim` |
| `Pagina<T>` | `List<T> itens; int pagina, tamanho, totalPaginas` |
| `Usuario` | `long id; String nome, email, cpf, telefone, perfil (CLIENTE\|CORRETOR)` |
| `Visita` | `long id, imovelId; String imovelTitulo, imovelFoto, data (yyyy-MM-dd), periodo (MANHA\|TARDE\|NOITE), telefone, mensagem, status, criadaEm` |
| `FotoEnvio` | `Uri uri; String estado (AGUARDANDO\|ENVIANDO\|ENVIADA\|FALHOU); int progresso` |

**`BancoLocal` (SQLite, versão 1)**
| Tabela | Colunas |
|---|---|
| `favorito` | `imovel_id INTEGER PK, json TEXT, sincronizado INTEGER, removido INTEGER, atualizado_em INTEGER` |
| `cache_lista` | `chave TEXT PK` (filtro + página serializados), `json TEXT, atualizado_em INTEGER` |
| `cache_imovel` | `id INTEGER PK, json TEXT, visto_em INTEGER` (mantém os 30 mais recentes) |
| `busca_recente` | `id INTEGER PK AUTOINCREMENT, filtro_json TEXT UNIQUE, criada_em INTEGER` (mantém 10) |
| `sim_usuario` | `id INTEGER PK AUTOINCREMENT, nome, email UNIQUE, senha, cpf, telefone, perfil, bloqueado INTEGER, email_verificado INTEGER, tentativas INTEGER` |
| `sim_favorito` | `usuario_id INTEGER, imovel_id INTEGER, PK(usuario_id, imovel_id)` |
| `sim_visita` | `id INTEGER PK AUTOINCREMENT, usuario_id, imovel_id, data, periodo, telefone, cpf, mensagem, status, criada_em INTEGER` |
| `sim_foto` | `id INTEGER PK AUTOINCREMENT, imovel_id INTEGER, caminho TEXT, enviada_em INTEGER` |

**SharedPreferences**: `sessao` → `token`, `usuario_json`; `config` → `modo_demonstracao` (padrão true), `simular_falha_rede`, `localizacao_pedida`.

### 6.9 Rotas provisórias da API (`ApiRotas` — D25)
Sugestão enviada ao back-end; o app só depende deste arquivo e dos `deJson`. Base: `BuildConfig.API_URL`. JSON em UTF-8; erro padrão `{ "status": 400, "mensagem": "..." }`; autenticadas com `Authorization: Bearer <token>`.
| Operação | Método e rota | Corpo / parâmetros | Resposta |
|---|---|---|---|
| Listar imóveis | `GET /api/imoveis` | `finalidade, tipo, cidade, precoMin, precoMax, quartosMin, page, size=10` | `{ content: [Imovel], number, size, totalPages }` |
| Destaques | `GET /api/imoveis/destaques` | — | `[Imovel]` |
| Detalhe | `GET /api/imoveis/{id}` | — | `Imovel` |
| Opções de filtro | `GET /api/imoveis/filtros` | — | `{ tipos: [..], cidades: [..] }` |
| Cadastro | `POST /api/auth/cadastro` | `{ nome, cpf?, email, senha }` | `201`; `409` e-mail existente |
| Verificar e-mail | `POST /api/auth/verificar-email` | `{ email, codigo }` | `{ token, usuario }` |
| Reenviar código | `POST /api/auth/reenviar-codigo` | `{ email }` | `204` |
| Login | `POST /api/auth/login` | `{ email, senha }` | `{ token, usuario }`; `401` inválido, `423` bloqueada, `403` e-mail não verificado |
| Esqueci senha | `POST /api/auth/esqueci-senha` | `{ email }` | `204` |
| Redefinir senha | `POST /api/auth/redefinir-senha` | `{ email, codigo, novaSenha }` | `204` |
| Perfil | `GET` / `PUT /api/usuarios/me` | `{ nome, cpf?, telefone? }` | `Usuario` |
| Alterar senha | `PUT /api/usuarios/me/senha` | `{ senhaAtual, novaSenha }` | `204` |
| Favoritos | `GET /api/usuarios/me/favoritos`; `PUT` / `DELETE /api/usuarios/me/favoritos/{imovelId}` | — | `[Imovel]` / `204` |
| Solicitar visita | `POST /api/visitas` | `{ imovelId, data, periodo, telefone, cpf?, mensagem? }` | `201 Visita` (gera lead — RN-004) |
| Minhas visitas | `GET /api/visitas/minhas` | — | `[Visita]` |
| Cancelar visita | `PATCH /api/visitas/{id}/cancelar` | — | `Visita` |
| Imóveis do corretor | `GET /api/corretor/imoveis` | — (perfil CORRETOR) | `[Imovel]` |
| Enviar foto | `POST /api/imoveis/{id}/fotos` | `multipart/form-data`, campo `arquivo` (JPEG) | `201 { url }` |

### 6.10 Fases (conteúdo de cada fase; tasks, PRs e ordem de execução na seção 11.4)

**Fase 1 — Fundação do app (EAP 2.1)**
1. **2.1.1 Ambiente e estrutura base:** projeto conforme 6.1/6.2; `assets/fake` copiado de `Dev Mobile/recursos`; `Tarefas`, `Api`, `ApiRotas`, `ApiException`, `BancoLocal` (todas as tabelas), `Sessao`, `Preferencias`, `ServidorSimulado` (leitura de imóveis), `CarregadorImagem`, `Formatos`, `Validacoes`, `EstadoTela`; README do app.
2. **2.1.2 Navegação e identidade visual:** design system 6.4 completo (cores, fontes, estilos, ícones, logos, ícone do app via Image Asset a partir de `ic_marcador_casa`); `BarraNavegacao`; as 4 Activities principais navegáveis com layout final do topo e conteúdo provisório; `strings.xml` (D21).
3. **2.1.3 Permissões do dispositivo:** manifesto (`INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `CAMERA`), `FileProvider`, `network_security_config`; pedido de localização na 1ª abertura da Home (diálogo + Activity Result API); `Localizacao` utilitário.

**Fase 2 — Vitrine e busca (EAP 2.2)**
1. **2.2.1** Home e Imóveis completas (6.5), filtros em sheets, paginação, buscas recentes.
2. **2.2.2** Detalhe (sem mapa — Fase 5) e Galeria; compartilhar (D38); CTA abre `SolicitarVisitaActivity` provisória (só título) até a Fase 4.
3. **2.2.3** `layout-land` de Home, Detalhe e telas de autenticação; `values-sw600dp`; acessibilidade; `onSaveInstanceState`.

**Fase 3 — Dados locais (EAP 2.3)**
1. **2.3.1** Favoritos locais (coração em Home, Imóveis, Detalhe; tela Favoritos com desfazer).
2. **2.3.2** Cache conforme D36 e banner offline; "Simular sem conexão" no diálogo de demonstração.
3. **2.3.3** Buscas recentes e último filtro; `Sessao` e `Preferencias` prontas para a Fase 4.

**Fase 4 — Conta e integração (EAP 2.4)**
1. **2.4.1** Login (cliente), Cadastro, VerificarEmail, EsqueciSenha, NovaSenha, AlterarSenha, DadosPessoais, Termos, Perfil logado/deslogado, Modo demonstração, Fale com a imobiliária, tratamento de 401 e RN-002 — no `ServidorSimulado` e na `Api`.
2. **2.4.2** `ImovelRepository` e demais Repositories completos nos dois modos; SolicitarVisita, MeusContatos, sincronização de favoritos.
3. **2.4.3** `estado_tela` em todas as telas de dados, mensagens da 6.6, validação de formulários.

**Fase 5 — Recursos do dispositivo (EAP 2.5)**
1. **2.5.1** Login modo CORRETOR, SelecionarImovel, FotosImovel com câmera e galeria.
2. **2.5.2** Upload com progresso e reenvio (multipart na `Api`; cópia local no simulado).
3. **2.5.3** Endereço + mapa osmdroid no Detalhe (`Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID)`, atribuição visível).
4. **2.5.4** Chip "Perto de mim" com distância e ordenação.

**Fase 6 — Qualidade e entrega (EAP 2.6)**
1. **2.6.1** Roteiro consolidado (6.11) em emulador e aparelho físico; correções.
2. **2.6.2** Keystore de demonstração com `keytool` em `keystore/` + `keystore.properties` commitados (D42), `signingConfig` no release, `versionCode 1` / `versionName "1.0"`, `gradlew assembleRelease`; APK anexado a um GitHub Release `mobile-v1.0` (com aprovação do Marcio).
3. **2.6.3** README do app (requisitos, abrir no Android Studio, contas de teste, modo demonstração, apontar para a API, gerar release), manual do usuário com prints de todas as telas (`Dev Mobile/docs/manual-usuario.md`), roteiro de demonstração (`Dev Mobile/docs/roteiro-demonstracao.md`).

### 6.11 Roteiros de teste manual (checklist de cada fase)
Ambiente: emulador `Talisma_API36` para todas as tasks; na T09 também `Talisma_API24` e `Pixel_9_Pro` (API 35) e, se disponível, um aparelho físico do grupo. Em toda tela: retrato, paisagem, fonte do sistema em 130%, TalkBack nos botões principais.
| Fase | Verificações |
|---|---|
| 1 | App abre na Home; bottom nav troca as 4 telas sem empilhar; cores, fontes e ícones iguais aos protótipos; pedido de localização aparece só na 1ª abertura; negar não quebra; girar não fecha o app |
| 2 | Comprar/Alugar + tipo + cidade → lista filtrada; cada chip filtra; preço mín. > máx. mostra erro; rolagem carrega mais 10; Ver todos; Detalhe com galeria, Ver mais, compartilhar abre o seletor do sistema; CTA muda entre Comprar/Alugar; girar mantém filtro e posição |
| 3 | Favoritar em Home/Imóveis/Detalhe reflete na aba Favoritos; desfazer funciona; fechar e reabrir mantém; "Simular sem conexão" → lista e detalhe vistos aparecem com banner; nada visto → estado offline |
| 4 | Login com cliente@teste.com/1234; senha errada 3× em conta nova bloqueia; bloqueada@teste.com mostra bloqueio; cadastro → código 1234 → logado; esqueci senha completo; alterar senha e dados; Solicitar visita (data, período, telefone) → Meus contatos como Pendente → após 2 min Confirmada → cancelar; favoritos de deslogado aparecem após login; Sair limpa; Perfil deslogado com convite; 5 toques na versão abrem o Modo demonstração; com modo desligado e sem API, erros aparecem no `estado_tela` |
| 5 | corretor@teste.com no modo corretor; cliente no modo corretor é recusado; câmera e galeria adicionam fotos; remover; enviar mostra progresso; "Simular sem conexão" → falha e reenvio; fotos enviadas aparecem no Detalhe; mapa com marcador; imóvel 14 sem coordenadas mostra só endereço; "Perto de mim" pede permissão se negada e ordena por distância |
| 6 | Todos os itens acima em API 36, API 24 e aparelho físico; APK release instala por cima da versão anterior |

### 6.12 Pendências externas (não bloqueiam — o modo simulado cobre)
| Item | Responsável | Efeito enquanto não existir |
|---|---|---|
| Endpoints reais (sugestão em 6.9) | Back-end | App roda em modo demonstração; ligar a API = desligar o modo e ajustar `ApiRotas`/`deJson` se o formato mudar |
| Lat/long, envio de e-mail, RBAC de corretor, upload | Back-end | Simulados localmente |
| Gantt e Especificação sem o mobile (D1) | PO | Nenhum para a implementação |
| Validação visual das telas novas (D11) | Grupo | Implementação segue 6.5; ajustes viram tarefas depois |
| Telefone e e-mail reais da imobiliária | PO | Usa os de `recursos/textos/empresa.md` |

## 7. Lista de necessidades do app para a API (D13)

Dados e operações que o app consome. Rotas, nomes e formato JSON ficam a cargo do back-end; a sugestão concreta (D25) está na seção 6.9. Inclui também o telefone do cliente (D30) e o cancelamento de visita (D37).

| Tela / recurso | Operação | Dados necessários |
|---|---|---|
| Home, Imóveis | Listar imóveis publicados (RN-001) com filtros e paginação | Filtros: finalidade (venda/locação), tipo, cidade, preço mín./máx., quartos. Item: id, título, cidade, bairro/região, preço, finalidade, foto de capa, lat/long |
| Home | Listar destaques | Mesmo item da listagem |
| Home, filtros | Opções de filtro | Tipos de imóvel e cidades disponíveis |
| Detalhe | Obter imóvel | Item + descrição, quartos, banheiros, vagas, área m², endereço completo, lat/long, lista de fotos (URLs) |
| Cadastro | Criar conta de cliente | Nome, CPF (opcional), e-mail, senha; erro de e-mail já cadastrado |
| Verificar e-mail | Validar código / reenviar | E-mail, código de 4 dígitos |
| Login | Autenticar cliente | E-mail, senha → token JWT + dados básicos; erros: credencial inválida, conta bloqueada (RN-002), e-mail não verificado |
| Esqueci / Alterar senha | Solicitar redefinição; trocar senha logado | E-mail; senha atual + nova |
| Dados pessoais | Ler e atualizar perfil | Nome, e-mail, CPF |
| Favoritos | Listar, adicionar, remover (logado) | IDs de imóveis |
| Solicitar visita | Criar solicitação (gera lead + notifica corretor — RN-004) | Imóvel, data/período preferido, mensagem, CPF se ainda não informado |
| Meus contatos | Listar solicitações do cliente | Imóvel, data, status, corretor responsável |
| Área do corretor | Autenticar corretor; listar imóveis do corretor; enviar fotos (multipart) | Credenciais; id do imóvel; arquivos JPEG |
| Geral | Padrão de erro e autenticação | Código HTTP + mensagem legível; token Bearer; HTTPS em produção |

## 8. Conformidade com o padrão do professor (`mobile4sem`)

| Prática cobrada nas aulas | Onde o plano garante |
|---|---|
| Template Empty Views Activity, Java, Gradle Kotlin DSL, AGP 8.13.0 / Gradle 8.14.3, Java 11, minSdk 24, target/compile 36 | D6, 6.1, Fase 1.1 |
| Dependências mínimas (AppCompat, Activity, ConstraintLayout, Material) | 6.1 — só RecyclerView e osmdroid a mais, ambas justificadas (lista paginada; D14) |
| Uma `AppCompatActivity` por tela, `EdgeToEdge.enable` + insets em `R.id.main`, `findViewById`, listeners com lambda | D7, 6.2 |
| Navegação por Intent explícita, `putExtra`/`getIntent`; Intent implícita para recursos do sistema; Activity Result API | 6.1, Fases 2 e 5 |
| `ConstraintLayout` como raiz, hierarquia rasa (≤ 3 níveis), `0dp` com restrições, `dimensionRatio` nas fotos | 6.2 (regras), Fase 2 |
| Todo texto em `strings.xml` (placeholders numerados, `translatable="false"`), espaçamentos em `dimens.xml`, cores por `@color` | D21, 6.2 |
| Qualificadores `layout-land` e `values-sw600dp`; rotação sem perder estado com `onSaveInstanceState` | D20, Fase 2.3 |
| Formulário longo em `ScrollView` + `adjustResize` | 6.2 |
| `contentDescription`, alvo ≥ 48 dp, ids com nome (`btnSalvar`, `edtPreco`, `imgProduto`) | 6.2, Fase 2.3 |
| `Log` com `TAG` (nada de `System.out`), nunca senha/token/dado pessoal | 6.2, D12 |
| Ícone do app gerado por Image Asset | Fase 1.2 |
| Verificação rodando no emulador (sem testes automatizados) | D18, Fase 6.1 |
| Fluxo de Git do README: branch `<tipo>/<nome-kebab>`, commits semânticos em português, PR em draft | Processo do app (seção 10) |

## 9. Revisão final — cobertura da EAP e dos requisitos

### 9.1 EAP 2 — Desenvolvimento Mobile, item a item
| Item EAP | Entrega no plano | Fase | Situação |
|---|---|---|---|
| 2.1.1 Ambiente e estrutura base | Projeto no padrão MeuAPP, estrutura 6.2, infraestrutura e dados simulados | 1.1 | ✅ coberto |
| 2.1.2 Navegação e identidade visual | Bottom nav das 4 telas, Intents, cores/fonte/logos (D23), tema, ícone | 1.2 | ✅ coberto |
| 2.1.3 Permissões do dispositivo | Internet, localização (1ª abertura), câmera (corretor), Activity Result API, app funcional sem permissão | 1.3 | ✅ coberto |
| 2.2.1 Listagem com filtro de buscas | Home + Imóveis com abas, filtros (RN-003) e paginação | 2.1 | ✅ coberto |
| 2.2.2 Detalhe do imóvel e galeria | Detalhe completo + galeria em tela cheia + CTA | 2.2 | ✅ coberto |
| 2.2.3 Layout responsivo e acessível | Rotação (D20), tablet, acessibilidade | 2.3 | ✅ coberto |
| 2.3.1 Favoritos do cliente | SQLite local + sincronização (D10) | 3.1 / 4.2 | ✅ coberto (sincronização simulada — D24) |
| 2.3.2 Cache de vitrine (offline) | Lista, destaques, detalhes e fotos em cache | 3.2 | ✅ coberto |
| 2.3.3 Buscas e sessões persistidas | Buscas recentes, últimos filtros, sessão | 3.3 | ✅ coberto |
| 2.4.1 Cliente HTTP e autenticação | `Api` nativa, JWT Bearer, login/cadastro/verificação/senha, 401 | 4.1 | ✅ coberto (completo no simulado — D24; real via 6.9) |
| 2.4.2 Endpoints de imóveis e visitas | Imóveis via API, Solicitar visita (RN-004), Meus contatos | 4.2 | ✅ coberto (completo no simulado — D24; real via 6.9) |
| 2.4.3 Erros, carregamentos e estados | `estado_tela.xml` em todas as telas de dados, validação de formulários | 4.3 | ✅ coberto |
| 2.5.1 Captura de fotos do imóvel | Área do corretor, câmera/galeria, compressão | 5.1 | ✅ coberto |
| 2.5.2 Upload das fotos para a API | Multipart com progresso e nova tentativa | 5.2 | ✅ coberto (completo no simulado — D24; real via 6.9) |
| 2.5.3 Mapa e endereço do imóvel | osmdroid + endereço no Detalhe | 5.3 | ✅ coberto (lat/long do seed; real via API) |
| 2.5.4 Imóveis próximos ao usuário | "Perto de mim" com distância calculada no app | 5.4 | ✅ coberto |
| 2.6.1 Testes em dispositivo | Roteiro manual por fase e consolidado | 6.1 | ✅ coberto |
| 2.6.2 Build de release (APK/AAB) | APK assinado (AAB fora por D17; o item aceita um dos dois) | 6.2 | ✅ coberto |
| 2.6.3 Documentação e demonstração | README, manual com prints, roteiro de demo | 6.3 | ✅ coberto |

Todos os 19 itens da EAP 2 têm entrega, fase e critério. Todos funcionam de ponta a ponta no modo simulado; ligar a API real é trocar o modo e, se o back-end mudar o formato, ajustar `ApiRotas` e `deJson` (6.9).

### 9.2 Requisitos funcionais e regras de negócio (Especificação v2.0)
| Requisito | Aplicação no app | Onde |
|---|---|---|
| RF-SITE-001 / RN-003 — Pesquisa; preço por faixa mín./máx., só valores monetários | Filtro de faixa de preço com campos numéricos em R$ e validação mín. ≤ máx. | Fase 2.1, D16 |
| RF-SITE-003 / RN-004 — Contato gera lead e notifica o corretor | Solicitar visita envia o pedido que a API transforma em lead | D4, Fase 4.2 |
| RF-ADM-001 / RN-001 — Só imóvel aprovado é publicado | App só exibe o que a API devolve como publicado; dados simulados representam imóveis já aprovados | Seção 7 |
| RF-ADM-002 / RN-002 — Bloqueio após 3 falhas (usuário interno) | Login do corretor (e do cliente) mostra a mensagem de conta bloqueada devolvida pela API | Fases 4.1 e 5.1 |
| Segurança — HTTPS, JWT, RBAC, Bcrypt | HTTPS no release, Bearer em toda chamada autenticada, área do corretor só para perfil corretor; hash é no servidor | 6.2, Fases 4.1 e 5.1 |
| Identidade visual 4.4.3 | Borgonha `#8D001A`, fundo branco, Poppins, logo no topo | D23, Fase 1.2 |
| Considerações finais — sem pagamento online | CTA leva ao corretor, nunca a checkout; Termos reforçam | D4, Termos |

### 9.3 Ajustes feitos nesta revisão
- Retirada a `SplashActivity`: o Android 12+ já mostra a splash do sistema, e uma Activity extra geraria splash dupla. A `HomeActivity` é a LAUNCHER e pede a localização na primeira abertura.
- Incluídos `ScrollView` + `adjustResize` nos formulários e `buildFeatures.buildConfig = true` (exigido no AGP 8 para `buildConfigField`).
- RN-002 e RBAC aplicados também ao login do corretor.
- Sem lacunas restantes de escopo; as dependências externas estão na seção 6.12 e nenhuma bloqueia a implementação.
- Rodada de fechamento (D24–D42): modo simulado completo, rotas provisórias, especificação de todas as telas (6.5), design system (6.4), modelo de dados (6.8), roteiros (6.11) e guia do orquestrador (seção 11). Tema fixo claro (`Theme.Material3.Light.NoActionBar`), pois os protótipos não têm modo escuro.

## 10. Processo do app
- 4 branches `feat/mobile-prN-<escopo>` empilhadas e 2 worktrees fixos (11.5); commits semânticos em português, um ou mais por task, com o id no escopo (`feat(t04): adiciona carrossel de destaques`); PR draft para `mobile`; merge só pelo time (D41, D44–D46).
- Uma fase por vez; cada fase termina com o roteiro manual aprovado e prints para o manual do usuário.

## 11. Guia para o agente orquestrador

### 11.1 Pré-requisitos (já atendidos em 03/11/2026)
- Android SDK 36, `cmdline-tools/latest`, JDK do Android Studio (`C:\Program Files\Android\Android Studio\jbr`); toolchain validado compilando `mobile4sem/MeuAPP`.
- Emuladores: `Talisma_API36` (API 36, principal), `Talisma_API24` (API 24, compatibilidade) e `Pixel_9_Pro` (API 35).
- Plano, recursos e prompt commitados na branch `docs/plano-mobile`.

### 11.2 Papéis (D39)
- **Orquestrador:** não escreve código do app. Prepara os 2 worktrees, dispara as tasks na ordem da 11.4, integra localmente os commits da trilha paralela (11.5), confere cada devolução e, no fim, valida os 4 PRs **sem aprovar nem fazer merge**.
- **Subagente UI (Fable, esforço low):** tasks UI — layouts, telas, estilos, design, navegação visual.
- **Subagente Geral (Opus, esforço medium):** tasks Geral — dados, simulado, API, regras, permissões, build, testes, docs.
- O subagente implementa só a sua task no worktree indicado, faz revisão básica contra o plano, corrige, **commita** e devolve: task, worktree, hash do último commit e "concluída — pode seguir" (ou "bloqueado: <pergunta>"). Só a **task que fecha um PR** faz push e abre o PR (11.4).
- No máximo **2 subagentes ao mesmo tempo**, um em cada worktree, só nos pares da 11.4. Um build Gradle e um emulador por vez: se os dois precisarem, o segundo espera.

### 11.3 Regras para todos os agentes
- Escopo: só `Dev Mobile/`. **Nunca alterar `Dev BackEnd/`, `Dev FrontEnd/`, `Eng. Software/` ou `Gestão de Projetos/`.**
- Seguir D1–D46, estrutura 6.2, design system 6.4, telas 6.5, regras 6.6, simulado 6.7, dados 6.8, rotas 6.9 e a conformidade da seção 8. Nenhuma dependência além das da 6.1. Código simples, sem camadas ou padrões fora do plano.
- Antes de começar, o subagente confirma `git -C <worktree> status` limpo e `git log -1` igual ao hash informado pelo orquestrador (codebase atualizada).
- Tasks em paralelo não editam o mesmo arquivo; strings novas da task Geral que roda junto com uma UI vão em `res/values/strings_<tNN>.xml`.
- Dúvida que o plano não responde: devolver "bloqueado"; o orquestrador pergunta ao Marcio. Ninguém inventa regra de negócio.

### 11.4 Tasks, PRs e paralelismo (D43, D45)
| PR (branch) | Task | EAP | Tipo | Worktree | Roda junto com | Escopo |
|---|---|---|---|---|---|---|
| **PR1 Fundação e vitrine** (`feat/mobile-pr1-fundacao-vitrine`) | T01 Base | 2.1.1 | Geral | A | — | Gradle/wrapper/.gitignore do MeuAPP, manifest base, `assets/fake`, modelos, `Api`, `ApiRotas`, `ApiException`, `BancoLocal` (todas as tabelas), `Sessao`, `Preferencias`, `ServidorSimulado` (leitura), `Tarefas`, `CarregadorImagem`, `Formatos`, `Validacoes`, `EstadoTela` (classe), README do app |
| PR1 | T02 Identidade e navegação | 2.1.2 | UI | A | T03 | `res/values` (cores, estilos, temas, dimens, strings), fontes, drawables, ícone, `BarraNavegacao`, 4 telas principais com topo final, `estado_tela.xml` |
| PR1 | T03 Dados da vitrine | 2.2.1, 2.3.1–2.3.3, 2.5.4 | Geral | B | T02 | `ImovelRepository` e `ServidorSimulado` completos (lista, filtros, paginação, destaques, detalhe, opções), `FavoritoRepository` local, cache offline, buscas recentes, `Localizacao` + distância, "Simular sem conexão" |
| PR1 (**fecha**) | T04 Telas da vitrine | 2.1.3, 2.2.1–2.2.3, 2.3.1 | UI | A | T05 | Home, Imóveis + sheets + Perto de mim, Detalhe (sem mapa), Galeria, Favoritos, banner offline, pedido de localização, permissões no manifest, `layout-land`, `values-sw600dp`, acessibilidade |
| **PR2 Conta e integração** (`feat/mobile-pr2-conta-integracao`) | T05 Conta e visitas (dados) | 2.4.1–2.4.3 | Geral | B | T04 | `ContaRepository`, `VisitaRepository`, sincronização de favoritos, contas/visitas no `ServidorSimulado`, 401, RN-002, mensagens 6.6 |
| PR2 (**fecha**) | T06 Telas de conta | 2.4.1–2.4.3 | UI | A | T07 | Login (cliente), Cadastro, Verificar e-mail, Esqueci/Nova/Alterar senha, Dados pessoais, Termos, Perfil logado/deslogado, Modo demonstração, Fale com a imobiliária, Solicitar visita, Meus contatos, `estado_tela` em todas as telas de dados |
| **PR3 Recursos do dispositivo** (`feat/mobile-pr3-recursos-dispositivo`) | T07 Fotos (dados) | 2.5.2 | Geral | B | T06 | `FotoRepository`, multipart na `Api`, fotos no `ServidorSimulado`, compressão JPEG, `FileProvider` |
| PR3 (**fecha**) | T08 Corretor e mapa | 2.5.1, 2.5.3 | UI | A | — | Login modo CORRETOR, Selecionar imóvel, Fotos do imóvel, mapa osmdroid + endereço no Detalhe |
| **PR4 Qualidade e entrega** (`feat/mobile-pr4-qualidade-entrega`) | T09 Testes em dispositivo | 2.6.1 | Geral | A | — | Roteiro 6.11 completo em `Talisma_API36`, `Talisma_API24` e `Pixel_9_Pro`; correções |
| PR4 (**fecha**) | T10 Release e documentação | 2.6.2, 2.6.3 | Geral | A | — | Keystore de demonstração (D42), `assembleRelease`, README final, `Dev Mobile/docs/manual-usuario.md` com prints, `Dev Mobile/docs/roteiro-demonstracao.md` |

O PR1 também leva os commits do plano (`docs/plano-mobile`), pois a branch nasce dela. Total: **4 PRs**.

### 11.5 Worktrees, integração e PRs empilhados (D44, D46)
- **Dois worktrees fixos para o processo inteiro**, criados pelo orquestrador e reutilizados por todas as tasks: `.claude/worktrees/mobile-a` (trilha principal, sempre na branch do PR em andamento) e `.claude/worktrees/mobile-b` (trilha paralela, branch local temporária `tmp/mobile-tNN`). Subagentes **não** usam `isolation: "worktree"`; recebem o caminho do worktree.
- **Sequência de branches:** `mobile` (criada de `main` se não existir) ← PR1 nasce de `docs/plano-mobile`; PR2 nasce da ponta do PR1; PR3 da ponta do PR2; PR4 da ponta do PR3. Todos os PRs são **draft com base `mobile`**.
- **Trilha paralela (B):** antes de cada task, o orquestrador faz `git -C mobile-b switch -C tmp/mobile-tNN <ponta atual da trilha A>`. Ao terminar, o orquestrador rebaseia `tmp/mobile-tNN` sobre a ponta da branch de destino e avança essa branch só por fast-forward (`git -C mobile-a merge --ff-only tmp/mobile-tNN`); arquivos disjuntos tornam o rebase limpo; conflito = parar e perguntar ao Marcio. A branch `tmp` é apagada em seguida. Isso é integração local entre branches de feature, **não** merge de PR.
- **Cruzando PRs:** T05 começa em B a partir da ponta do PR1 depois de T02+T03; quando T04 fecha o PR1, o orquestrador cria a branch do PR2 em A a partir da ponta do PR1 e integra T05 nela antes de T06. O mesmo vale para T07 → PR3.
- **Fechamento de um PR:** a task que fecha roda o critério 11.6 na ponta da branch, faz `git push -u origin <branch>` e abre `gh pr create --draft --base mobile` com título `[PRn] <nome>`, corpo com itens da EAP, tasks incluídas, "Depende de: #PR anterior", ordem de merge, roteiro marcado e prints. Correções posteriores entram como novos commits no mesmo PR.
- **Merge (só o time):** em ordem PR1 → PR2 → PR3 → PR4, com **"Create a merge commit"** (squash quebra a pilha). Enquanto o anterior não for mergeado, o diff do PR seguinte mostra também os commits dele.
- No fim, o orquestrador remove os dois worktrees com `python -I ~/.claude/hooks/worktree-cleanup.py --only <caminho> --min-idle-min 0` (nunca `git worktree remove` à mão).

### 11.6 Critério de pronto
- **Cada task:** `gradlew assembleDebug` e `gradlew lintDebug` sem erro (sem "Hardcoded text" nem "Missing constraints"); telas da task conferidas no `Talisma_API36` (retrato e paisagem) com prints salvos em `Dev Mobile/docs/prints/<tNN>/`; revisão básica contra o plano com correções commitadas.
- **Cada PR (task que fecha):** o mesmo na ponta da branch com todas as tasks do PR integradas + a parte do roteiro 6.11 correspondente executada e marcada no corpo do PR.

### 11.7 Encerramento
Após T10: o orquestrador confere que os 4 PRs estão abertos, draft, base `mobile`, empilhados na ordem certa, com build e lint verdes na ponta do PR4, checklist preenchido e nenhum arquivo fora de `Dev Mobile/`; publica no PR1 um comentário com a ordem de merge e o estado de cada PR; remove os worktrees (11.5); **não aprova nem mergeia**. O app fica pronto para o time mergear em ordem e abrir `Dev Mobile/talisma-app` no Android Studio.

## 12. Tarefas do restante do time
As tarefas de cada frente para juntar o app ao restante do Projeto Integrador estão em [`TAREFAS_EQUIPE.md`](TAREFAS_EQUIPE.md).
