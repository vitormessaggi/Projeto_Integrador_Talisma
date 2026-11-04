# Implantação local

Pré-requisitos no Windows: JDK 25, Node.js 24, MySQL 8.4 e Mailpit. Os scripts também foram validados no MySQL 8.0.44 disponível na máquina de desenvolvimento.

## Banco

Crie os esquemas `talisma` e `talisma_test` com `utf8mb4_0900_ai_ci`. No Workbench, crie contas com senhas próprias, limitadas ao respectivo esquema. A conta usada para migrations precisa criar e alterar tabelas e índices nesse esquema. Use `talisma_test` exclusivamente para testes.

## E-mail

[Baixe o Mailpit para Windows](https://mailpit.axllent.org/docs/install/) e execute no PowerShell:

```powershell
.\mailpit.exe --listen 127.0.0.1:8025 --smtp 127.0.0.1:1025
```

Abra http://127.0.0.1:8025 para ler os códigos e notificações. A API usa o SMTP local por padrão.

## Backend

Na pasta do backend, configure as variáveis no terminal atual:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/<pasta do JDK 25>'
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/talisma?connectionTimeZone=UTC'
$env:DB_USER = 'talisma_app'
$env:DB_PASSWORD = Read-Host 'Senha MySQL' -MaskInput
$env:APP_JWT_KEY = [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Reutilize a mesma chave entre reinícios para preservar a validade dos tokens já emitidos. O Flyway aplica as migrations quando a API inicia. Para provisionar o primeiro administrador:

```powershell
$env:ADMIN_NOME = Read-Host 'Nome do administrador'
$env:ADMIN_EMAIL = Read-Host 'E-mail do administrador'
$env:ADMIN_SENHA = Read-Host 'Senha do administrador' -MaskInput
$env:SPRING_PROFILES_ACTIVE = 'provisionar-admin'
.\mvnw.cmd spring-boot:run
Remove-Item Env:SPRING_PROFILES_ACTIVE, Env:ADMIN_SENHA
.\mvnw.cmd spring-boot:run
```

API em http://127.0.0.1:8080. Consulte `/actuator/health` para verificar aplicação e banco. Use `APP_API_DOCS=true` para disponibilizar `/v3/api-docs` localmente e gerar novamente o contrato. Configure `APP_FOTOS_DIR` se desejar guardar imagens fora da pasta do backend. O diretório padrão é `fotos-local`.

## Frontend

Na pasta do frontend, execute `npm ci` e `npm run dev`. Abra http://localhost:3000. Defina `API_URL` quando usar outra porta no backend.

## Roteiro de demonstração

1. Entre com o administrador provisionado e cadastre um corretor fictício.
2. Entre com o corretor, cadastre proprietário e imóvel com venda e locação, ou use o seed em `Dev BancoDados/DML/demo`. Adicione fotos e envie o anúncio para revisão.
3. Na área de aprovação do administrador, publique o anúncio. Procure-o no catálogo em compra e locação.
4. Crie um cliente fictício, verifique a conta pelo Mailpit e entre. Favorite o imóvel, envie um contato e solicite visita.
5. Confira os e-mails na caixa de testes. Confirme a visita como corretor e cancele-a como cliente.
6. Edite uma oferta como corretor e confira que o imóvel saiu da vitrine até nova aprovação.

## Validação

Configure `TEST_DB_URL`, `TEST_DB_USER` e `TEST_DB_PASSWORD` para o esquema exclusivo `talisma_test` e execute `./mvnw.cmd test`. Os testes de integração rodam somente quando a URL aponta para esse esquema. As transações dos cenários são revertidas ao fim do teste.

No frontend, execute `npm run typecheck`, `npm run lint` e `npm run build`. Para Playwright, instale Chromium com `npx playwright install chromium`, mantenha API e site rodando e execute `npm run e2e`.

Em CI com Docker ativo, execute `./mvnw.cmd -Ptestcontainers test` para rodar os mesmos cenários em um MySQL 8.4 descartável.
