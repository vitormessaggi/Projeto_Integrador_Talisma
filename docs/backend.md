# Backend

A API REST em `Dev BackEnd/imoveisapi` é compartilhada pelo site e pelo aplicativo Android. O prefixo público do contrato é `/api/v1`.

## Tecnologias

Java 25, Spring Boot 4.1.2 e Maven Wrapper. Spring MVC recebe as requisições, Spring Security valida os JWTs, JPA e `JdbcClient` acessam o MySQL, Flyway versiona o esquema e Actuator publica o estado de saúde. A API usa SMTP para e-mails e AWS SDK para fotos no S3.

## Estrutura

| Caminho dentro do backend | Responsabilidade |
|---|---|
| `src/main/java/br/com/talisma/api/imoveisapi/catalogo` | Vitrine, detalhes e filtros de imóveis publicados. |
| `src/main/java/br/com/talisma/api/imoveisapi/gestao` | Proprietários, anúncios, ofertas, fotos e revisão administrativa. |
| `src/main/java/br/com/talisma/api/imoveisapi/identidade` | Cadastro, e-mail, sessão, papéis, favoritos e privacidade. |
| `src/main/java/br/com/talisma/api/imoveisapi/atendimento` | Leads, visitas e fila de notificações. |
| `src/main/resources/` | Configuração Spring, perfis e textos legais versionados. |
| `src/test/java/` | Testes de integração da API e testes do armazenamento. |
| `docs/openapi.json` | Contrato HTTP usado pelo frontend e pelo Android. |

As migrations V1–V6 estão em `Dev BancoDados/DDL` e `Dev BancoDados/DML`. O Maven as inclui no JAR. A API aplica Flyway na inicialização local. Na AWS o perfil `migrar` aplica o esquema antes do início do perfil `aws`, que usa a conta de execução. [Banco e migrations](../Dev%20BancoDados/README.md)

## Como montar o backend

1. Prepare o MySQL com o schema `talisma` e o Mailpit. Use outro schema para testes.
2. Configure a conexão e a chave JWT. No ambiente local, o script do repositório pode gerar a configuração privada.
3. Execute `mvnw.cmd spring-boot:run` na pasta `imoveisapi` e confira `/actuator/health`.
4. Use `provisionar-admin` uma vez por base para criar o primeiro administrador. Cadastre o corretor e use o fluxo de rascunho, envio para revisão e publicação.
5. Ao mudar o contrato, atualize o OpenAPI e regenere os tipos do frontend. Confira também os modelos e as rotas Android.

O upload pela API registra a foto e sua ordem no MySQL. No ambiente local os arquivos ficam em `fotos-local`. Na AWS cada foto nova usa a chave `imoveis/<id>/<uuid>.<extensão>` no S3 privado. As chaves antigas continuam disponíveis no S3.

Use `APP_FOTOS_DIR` para trocar o diretório de fotos local. Para consultar ou atualizar o contrato gerado, inicie a API local com `APP_API_DOCS=true` e acesse `/v3/api-docs`.

[Contrato OpenAPI](../Dev%20BackEnd/imoveisapi/docs/openapi.json) · [Execução detalhada](rodar-aplicacao.md)
