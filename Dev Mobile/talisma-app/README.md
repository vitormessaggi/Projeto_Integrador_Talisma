# Talismã Imóveis — app Android

App nativo (Java) da imobiliária Talismã, parte do Projeto Integrador (EAP 2 — Desenvolvimento Mobile).
Plano completo e decisões (D1–D46): [`../PLANO_IMPLEMENTACAO_MOBILE.md`](../PLANO_IMPLEMENTACAO_MOBILE.md).

Documentação: [manual do usuário](../docs/manual-usuario.md) · [roteiro de demonstração](../docs/roteiro-demonstracao.md)
· [roteiro de testes em dispositivo](../docs/roteiro-testes.md).

## Requisitos

- Android Studio com o JDK embutido (`jbr`) e Android SDK 36.
- Emulador API 36 (principal) ou API 24 (compatibilidade), ou aparelho com Android 7.0+.

Stack igual ao projeto do professor (`mobile4sem/MeuAPP`): template Empty Views Activity, Java 11,
Gradle Kotlin DSL com dependências inline, AGP 8.13.0, Gradle 8.14.3, minSdk 24, compile/target 36.
Dependências: AppCompat, Material, Activity, ConstraintLayout, RecyclerView e osmdroid — nada além.

## Abrir e rodar

1. No Android Studio: **File → Open** → `Dev Mobile/talisma-app`.
2. O Android Studio cria o `local.properties` com o caminho do SDK (fica fora do git).
3. Rode a configuração `app` no emulador.

Pela linha de comando (Git Bash), dentro de `Dev Mobile/talisma-app`:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew assembleDebug
./gradlew lintDebug
```

## Modo demonstração e API

- Enquanto a API não tiver os endpoints, o app roda no **modo demonstração** (padrão ligado): o
  `ServidorSimulado` lê `app/src/main/assets/fake/` (`imoveis.json`, `usuarios.json`, `fotos/`) e grava
  nas tabelas `sim_*` do SQLite, com atraso de ~600 ms.
- **Contas de teste** (só no modo demonstração): `cliente@teste.com` / `1234`, `corretor@teste.com` / `1234`
  e `bloqueada@teste.com` (conta bloqueada — RN-002). Código de verificação de e-mail: `1234`.
- **Apontar para a API real:** em `local.properties`, adicione `api.url=http://SEU_HOST:8080`
  (padrão `http://10.0.2.2:8080`, o `localhost` do PC visto pelo emulador). A URL vai para
  `BuildConfig.API_URL`; todas as rotas ficam em `dados/ApiRotas.java` e a conversão JSON nos
  métodos `deJson`/`paraJson` dos modelos.

- **Alternar modo demonstração × API:** no Perfil (logado ou não), toque **5 vezes em "Versão 1.0"**. O diálogo
  liga/desliga o modo demonstração e o **Simular sem conexão**; ao alternar, a sessão e o cache são limpos.

## Gerar o release

O release é um **APK assinado com o keystore de demonstração** commitado em `keystore/` (D17, D42): uso só
acadêmico, **nunca** publicar na Play Store com essa chave. Como todos usam a mesma chave, qualquer colega gera
o mesmo release e as versões instalam por cima umas das outras (o APK debug tem outra assinatura: desinstale-o
antes de instalar o release).

- `keystore/talisma-demo.jks` (alias `talisma-demo`) e `keystore/keystore.properties` (senhas) são lidos pelo
  `signingConfig` do `app/build.gradle.kts`. Versão: `versionCode 1` / `versionName "1.0"`.
- Android Studio: **Build → Generate App Bundles or APKs → Build APK(s)** com a variante `release`, ou:

```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
# conferir a assinatura (build-tools do SDK):
"$LOCALAPPDATA/Android/Sdk/build-tools/36.0.0/apksigner.bat" verify --print-certs app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```

- O APK não vai para o git (`*.apk` no `.gitignore`). Para distribuir, anexe-o a um **GitHub Release
  `mobile-v1.0`** (só com aprovação do Marcio), por exemplo:
  `gh release create mobile-v1.0 app/build/outputs/apk/release/app-release.apk --target mobile --title "App mobile v1.0"`.

## Estrutura

```
keystore/                         # keystore de DEMONSTRAÇÃO (D42) e keystore.properties
app/src/main/
├── assets/fake/                  # dados e fotos do modo demonstração
├── java/br/com/talisma/imoveis/
│   ├── *Activity                 # uma Activity por tela
│   ├── dados/                    # modelos, Api, ApiRotas, ApiException, BancoLocal, Sessao,
│   │                             # Preferencias, ServidorSimulado e Repositories
│   └── util/                     # Tarefas, CarregadorImagem, Formatos, Validacoes, EstadoTela, BarraNavegacao,
│                                 # Localizacao
└── res/                          # layouts, strings, cores, dimens, temas, estilos (6.4), fontes Poppins, ícones
```

- `util/Tarefas`: `ExecutorService` + `Handler` da main thread; callback único `Tarefas.Callback<T>`.
- `dados/BancoLocal`: favoritos, cache da vitrine, buscas recentes e tabelas `sim_*` (SQLite versão 1).
- `dados/Sessao` e `dados/Preferencias`: SharedPreferences `sessao` e `config`.
- `dados/ImovelRepository`: lista (filtros, 10 por página), destaques, detalhe e opções; cache usado só
  quando a rede falha (resultado com `offline = true`); buscas recentes e último filtro.
- `dados/FavoritoRepository`: favoritos locais no SQLite (funcionam sem login e offline).
- `util/Localizacao`: localização do aparelho, distância ("Perto de mim") e `Geocoder` sem lat/long.
- "Simular sem conexão" (`Preferencias.setSimularFalhaRede`): o `ServidorSimulado` passa a falhar como sem rede.
- `util/EstadoTela`: estados CARREGANDO, VAZIO, ERRO e OFFLINE sobre `layout/estado_tela.xml`.
- `util/BarraNavegacao`: bottom nav das 4 telas principais (`configurar(this, R.id.nav_x)`): abre a aba com `FLAG_ACTIVITY_REORDER_TO_FRONT` sem animação; Voltar na Home sai do app, nas outras abas volta à Home.
