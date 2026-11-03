# Talismã Imóveis — app Android

App nativo (Java) da imobiliária Talismã, parte do Projeto Integrador (EAP 2 — Desenvolvimento Mobile).
Plano completo e decisões (D1–D46): [`../PLANO_IMPLEMENTACAO_MOBILE.md`](../PLANO_IMPLEMENTACAO_MOBILE.md).

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

## Estrutura

```
app/src/main/
├── assets/fake/                  # dados e fotos do modo demonstração
├── java/br/com/talisma/imoveis/
│   ├── *Activity                 # uma Activity por tela
│   ├── dados/                    # modelos, Api, ApiRotas, ApiException, BancoLocal, Sessao,
│   │                             # Preferencias, ServidorSimulado e Repositories
│   └── util/                     # Tarefas, CarregadorImagem, Formatos, Validacoes, EstadoTela
└── res/                          # layouts, strings, cores, temas
```

- `util/Tarefas`: `ExecutorService` + `Handler` da main thread; callback único `Tarefas.Callback<T>`.
- `dados/BancoLocal`: favoritos, cache da vitrine, buscas recentes e tabelas `sim_*` (SQLite versão 1).
- `dados/Sessao` e `dados/Preferencias`: SharedPreferences `sessao` e `config`.
- `util/EstadoTela`: estados CARREGANDO, VAZIO, ERRO e OFFLINE sobre `layout/estado_tela.xml`.
