# Roteiro de testes em dispositivo — T09 (EAP 2.6.1)

Execução do roteiro manual da seção 6.11 do plano (fases 1 a 5) no app **Talismã Imóveis**, modo demonstração ligado
(contas da D27: `cliente@teste.com` / `1234`, `corretor@teste.com` / `1234`, `bloqueada@teste.com`; código `1234`).
Data: 03–04/11/2026. Base testada: `b55c2c4` + correções desta task (commits na tabela de defeitos).

## Ambientes

| AVD | Android | Tela | Observação |
|---|---|---|---|
| `Talisma_API36` | 16 (API 36) | 1080×2400, 420 dpi | roteiro completo, retrato/paisagem, fonte 130% em todas as telas |
| `Talisma_API24` | 7.0 (API 24, minSdk) | 1080×2400, 420 dpi | roteiro completo, retrato/paisagem, fonte 130% em todas as telas |
| `Pixel_9_Pro` | 15 (API 35) | 1280×2856 | fases 1–4 completas e fase 5 parcial (ver pendências) |
| Aparelho físico | — | — | **pendente para o grupo** (nenhum aparelho disponível nesta execução) |

Como foi feito: navegação por `adb shell input`, conferência do texto e do `contentDescription` por `uiautomator dump`
e capturas por `adb exec-out screencap`. Rotação com `settings put system user_rotation`; fonte com
`settings put system font_scale 1.3` (voltou a `1.0` no fim). Os cenários de "sem conexão" usam o switch
**Simular sem conexão** do diálogo Modo demonstração (5 toques em "Versão 1.0").

## Checklist por fase × AVD

Legenda: ✅ ok · ⚠️ ok com observação · ⏳ pendente · — não se aplica

### Fase 1 — Fundação

| Verificação | API 36 | API 24 | Pixel 9 Pro |
|---|---|---|---|
| App abre na Home | ✅ | ✅ (após correção D5) | ✅ |
| Bottom nav troca as 4 telas sem empilhar (uma instância de cada; Voltar na Home sai do app) | ✅ | ✅ | ✅ |
| Cores, fontes Poppins e ícones conforme protótipos | ✅ | ✅ | ✅ |
| Pedido de localização só na 1ª abertura (diálogo → permissão do sistema) | ✅ | ✅ | ✅ |
| Negar a localização não quebra nada | ✅ | ✅ | ✅ |
| Girar não fecha o app | ✅ | ✅ | ✅ |

### Fase 2 — Vitrine e busca

| Verificação | API 36 | API 24 | Pixel 9 Pro |
|---|---|---|---|
| Alugar + Apartamento + São Paulo → lista filtrada (só locação/apartamento/SP) | ✅ | ✅ | ✅ |
| Chips filtram (preço, tipo, quartos, cidade com buscas recentes) | ✅ | ✅ | ✅ |
| Preço mín. > máx. mostra "O preço mínimo não pode ser maior que o máximo." | ✅ | ✅ | ✅ |
| Rolagem carrega a página seguinte (12 à venda = 10 + 2) | ✅ | ✅ | ✅ |
| "Ver todos" abre Imóveis sem filtro | ✅ | ✅ | ✅ |
| Detalhe: miniaturas, galeria com contador "1/4" e deslize, Ver mais | ✅ | ✅ | ✅ |
| Compartilhar abre o seletor do sistema | ✅ | ✅ | ✅ |
| CTA "Comprar agora" (venda) / "Alugar agora" (locação) | ✅ | ✅ | ✅ |
| Girar mantém filtro e posição da lista | ✅ | ✅ | ✅ |

### Fase 3 — Dados locais

| Verificação | API 36 | API 24 | Pixel 9 Pro |
|---|---|---|---|
| Favoritar em Home/Imóveis/Detalhe reflete na aba Favoritos | ✅ | ✅ | ✅ |
| Remover com Snackbar "Removido · Desfazer"; Desfazer restaura | ✅ | ✅ | ✅ |
| Fechar e reabrir o app mantém os favoritos | ✅ | ✅ | ✅ |
| Simular sem conexão → lista e detalhe vistos com banner "Você está offline — mostrando dados salvos" | ✅ | ✅ | ✅ |
| Nada visto + sem conexão → `estado_tela` OFFLINE ("Sem conexão. Verifique sua internet." + Tentar novamente) | ✅ | ✅ | ✅ |

### Fase 4 — Conta e integração

| Verificação | API 36 | API 24 | Pixel 9 Pro |
|---|---|---|---|
| Login cliente; senha errada mostra "E-mail ou senha incorretos." no campo | ✅ | ✅ | ✅ |
| Conta nova: 3 senhas erradas → "Conta bloqueada após 3 tentativas…" (e senha certa continua bloqueada) | ✅ | ✅ | ✅ |
| `bloqueada@teste.com` mostra o diálogo de bloqueio | ✅ | ✅ | ✅ |
| Cadastro → Verificar e-mail (código 1234, reenviar com contagem 30 s) → logado | ✅ | ✅ | ✅ |
| Esqueci senha → código → Nova senha → Login com "Senha alterada. Entre com a nova senha" | ✅ | ✅ | ✅ |
| Alterar senha (mín. 6 caracteres) e Dados pessoais (telefone com máscara) | ✅ | ✅ | ✅ |
| Solicitar visita (data, período, telefone, CPF quando não há) → diálogo → Meus contatos "Pendente" | ✅ | ✅ | ✅ |
| Após 2 min "Confirmada"; Cancelar (diálogo) → "Cancelada" | ✅ | ✅ | ✅ |
| Favoritos de deslogado aparecem após o login | ✅ | ⚠️ não repetido (dados limpos antes) | ⚠️ não repetido |
| Sair (confirmação) limpa a sessão; Perfil deslogado com convite Entrar/Cadastrar | ✅ | ✅ | ✅ |
| 5 toques em "Versão 1.0" abrem o Modo demonstração | ✅ | ✅ | ✅ |
| Modo desligado e sem API → erro no `estado_tela` (Home e Imóveis) | ✅ (após correção D6) | ⚠️ erro só após o timeout de 15 s | ✅ |

### Fase 5 — Recursos do dispositivo

| Verificação | API 36 | API 24 | Pixel 9 Pro |
|---|---|---|---|
| `corretor@teste.com` no modo corretor → Selecionar imóvel ("Olá, Corretor Teste", "4 fotos") | ✅ | ✅ | ✅ |
| Cliente no modo corretor → "Esta conta não é de corretor." | ✅ | ✅ | ✅ |
| Galeria adiciona fotos (seletor de fotos do sistema) | ✅ | ⚠️ ver observação O3 | ✅ |
| Câmera adiciona foto (permissão pedida ao tocar) | ✅ | ✅ | ✅ |
| Remover foto nova | ✅ | ✅ | ✅ |
| Enviar mostra estado/progresso e "N foto(s) enviada(s)." | ✅ | ✅ | ✅ |
| Simular sem conexão → falha "toque para reenviar" → reenvio ok | ✅ | ⏳ ver O4 | ⏳ emulador travou (O5) |
| Fotos enviadas aparecem em "Fotos atuais" e na galeria do Detalhe | ✅ | ✅ | ✅ |
| Mapa com marcador e atribuição OSM | ✅ | ✅ | ⏳ (O5) |
| Imóvel 14 sem coordenadas | ⚠️ O1 | ⚠️ O1 | ⏳ (O5) |
| "Perto de mim" pede a permissão negada antes e ordena por distância ("a 0,4 km"…) | ✅ | ✅ | ⏳ (O5) |
| Fotos pendentes sobrevivem à morte do processo (`onSaveInstanceState`) | ✅ | — | — |

### Transversais (todas as telas)

| Verificação | API 36 | API 24 | Pixel 9 Pro |
|---|---|---|---|
| Retrato e paisagem sem corte nem sobreposição | ✅ (após D2–D4) | ✅ | ✅ (retrato/paisagem nas fases 1–3) |
| Fonte do sistema em 130% sem texto cortado | ✅ (após D1–D4) | ✅ | ⏳ (O5) |
| `contentDescription` nos botões de ícone (voltar, compartilhar, favoritar, fotos, remover, galeria) | ✅ | ✅ | ✅ |
| Rótulo acessível dos spinners da Home (`labelFor`) | ✅ (D7) | ✅ | ✅ |

Conferência de acessibilidade feita por `uiautomator dump`: nenhum botão de ícone sem rótulo. Os campos
(`TextInputLayout`) expõem o hint como rótulo; os cartões (`cardImovel`) são lidos pelo conteúdo (título,
local, preço). TalkBack ligado de fato **não** foi usado (pendente para o aparelho físico).

## Defeitos encontrados e correções

| # | Defeito | Onde | Correção | Commit |
|---|---|---|---|---|
| D1 | Fonte 130%: nos cartões de imóvel (lista, Favoritos, destaques e área do corretor) o título em 2 linhas fazia o preço/"N fotos" sobrepor a localização (altura fixa 112/220 dp) | todas as APIs | cartões com altura **mínima** (112/220 dp) e preço ancorado abaixo da localização | `e0c52ab` |
| D2 | Home em paisagem: o cartão de destaque (220 dp) não cabia na altura e o preço ficava cortado | todas | coluna de destaques em `ScrollView` no `layout-land` | `e0c52ab` |
| D3 | Fonte 130%: "Cadastre-se aqui" quebrava no meio ("Cadas/tre-se") no Login; idem risco em "Acesse a área interna" e "Reenviar código" | todas | linhas rótulo + link: o rótulo encolhe e quebra, o link fica inteiro | `e0c52ab` |
| D4 | Fonte 130%: "Comprar" cortado nas abas da Home em paisagem; "Enviar e-m…" no Fale com a imobiliária; hints "Preço míni…" na faixa de preço | todas | padding lateral menor nas abas e nos botões do sheet; hints "Mínimo (R$)"/"Máximo (R$)" | `e0c52ab` |
| D5 | API 24: Home, Detalhe e Fotos do imóvel abriam já rolados (o `RecyclerView` dentro do `ScrollView` pegava o foco inicial) — a Home escondia o título | API 24 | o conteúdo rolável segura o foco inicial | `4f2ddae` |
| D6 | Ao alternar o Modo demonstração no Perfil, Home e Imóveis continuavam mostrando os dados da fonte anterior ao voltar pela barra inferior | todas | Home e Imóveis recarregam no `onResume` se o modo mudou | `e0c52ab` |
| D7 | TalkBack: spinners "Tipo de imóvel"/"Onde deseja morar?" sem rótulo | todas | `android:labelFor` nos rótulos | `e0c52ab` |
| D8 | API 24: preço formatado "R$450.000" (sem espaço; ICU antigo) | API 24 | `Formatos.moeda` garante o espaço após "R$" | `4f2ddae` |

`gradlew assembleDebug` e `lintDebug` sem erro após as correções (só avisos já existentes + `InefficientWeight`
intencional nas linhas rótulo + link, que precisam de `wrap_content` para encolher só quando falta espaço).

## Observações (não são defeitos do app)

- **O1 — Imóvel 14 ("Fazenda para Lazer", sem lat/long):** o emulador tem `Geocoder` com rede, então o endereço
  "Interior de São Paulo" é geocodificado e o mapa aparece no centro de SP (e "Perto de mim" mostra "a 0,9 km").
  É o comportamento da D14/D15 ("sem lat/long: tenta Geocoder; se falhar mostra só o endereço"). Para ver o caso
  "só endereço", testar sem rede no aparelho.
- **O2 — API 24, Modo demonstração desligado:** com a API ausente, o erro "Sem conexão…" aparece só depois do
  timeout de 15 s (`Api`), porque a conexão a `10.0.2.2:8080` fica pendente nesse emulador. No API 36/35 o erro é imediato.
- **O3 — Galeria vazia no emulador não é falha do app** (aviso do Marcio). No API 24 o seletor
  (`ACTION_OPEN_DOCUMENT`, fallback do `PickVisualMedia`) não listou as imagens enviadas por `adb push` mesmo após o
  media scan; o envio no API 24 foi testado pela **câmera**. No API 36 e no Pixel a galeria foi testada com imagens
  colocadas antes (`adb push … /sdcard/Pictures/` + `MEDIA_SCANNER_SCAN_FILE`).
- **O4 — API 24, falha/reenvio de foto:** o roteiro troca "Simular sem conexão" editando a preferência com o processo
  morto; no API 24 o `am kill` não encerrou o processo e a troca não teve efeito, então o envio foi bem-sucedido.
  O fluxo de falha e reenvio é o mesmo código e foi verificado no API 36.
- **O5 — Pixel_9_Pro travou** durante a fase 5 (o Marcio estava instalando drivers de CPU/GPU; foi reaberto a frio
  e não respondeu mais ao `uiautomator`). Ficaram pendentes no Pixel: falha/reenvio de foto, mapa, imóvel 14,
  "Perto de mim" e fonte 130%. Todos foram verificados no API 36 e no API 24.
- **O6 — API 24 abre o teclado sozinho** no primeiro campo de Login/Solicitar visita (foco inicial em modo toque do
  Android < 9). Comportamento padrão da plataforma, sem impacto no fluxo.
- **O7 —** com sessão de corretor, o Perfil mostra os itens de cliente e não há atalho de volta à área do corretor
  sem "Sair" (o plano não prevê o atalho; não foi criado).

## Pendências para o grupo

1. Rodar o roteiro em **aparelho físico** (e com TalkBack ligado de verdade).
2. No Pixel_9_Pro (ou aparelho): falha/reenvio de foto, mapa, imóvel 14, "Perto de mim" e fonte 130%.
3. Fase 6 da 6.11 ("APK release instala por cima"): feita na T10 no `Talisma_API36` — release assinado com o
   keystore de demonstração instalado (após desinstalar o debug, de outra assinatura) e reinstalado por cima com
   `adb install -r`; Home e Detalhe ok (`prints/t10/`). Falta repetir no API 24 e no aparelho físico.

## Prints (`Dev Mobile/docs/prints/t09/`, reduzidos a 50%)

- `a36_*`: API 36 — fases 1–5 e `a36_fonte130_*` (todas as telas a 130%, retrato e `_land`), `a36_*_fonte130.png`
  (telas corrigidas), `a36_home_sem_api.png` (D6), `a36_f5_falhou.png` (foto com falha, toque para reenviar).
- `a24_offline_sem_cache.png` / `px_offline_sem_cache.png`: nada visto + sem conexão → estado OFFLINE.
- `a24_*`: API 24 — `a24_home_topo.png` (D5 corrigido), fases 1–5 e `a24_fonte130_*` (retrato e paisagem a 130%).
- `px_*`: Pixel_9_Pro — fases 1–5 (parcial).
