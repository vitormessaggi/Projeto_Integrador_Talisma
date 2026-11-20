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

As migrations V1–V6 estão em `Dev BancoDados/DDL` e `Dev BancoDados/DML`. O Maven as inclui no JAR. Na EC2, ative `aws,migrar` para aplicar o esquema antes de iniciar a API. O perfil padrão `aws` usa a conta de execução no Aurora MySQL. [Banco e migrations](../Dev%20BancoDados/README.md)

## Como montar o backend

1. Prepare o schema `talisma` no Aurora privado e o SMTP do Mailpit na EC2.
2. Configure o arquivo privado de runtime com a origem JDBC, as credenciais, a chave JWT e o bucket.
3. Execute `infra/aws/deploy.sh` na EC2 e confira `/actuator/health` na porta interna 8080.
4. Use `provisionar-admin` uma vez por base para criar o primeiro administrador. Cadastre o corretor e use o fluxo de rascunho, envio para revisão e publicação.
5. Ao mudar o contrato, atualize o OpenAPI e regenere os tipos do frontend. Confira também os modelos e as rotas Android.

O upload pela API registra a foto e sua ordem no Aurora. Cada foto nova usa a chave `imoveis/<id>/<uuid>.<extensão>` no S3 privado. As chaves antigas continuam disponíveis no S3.

Os testes de integração usam um banco descartável pelo perfil de teste. A publicação mantém o Actuator interno. O Nginx encaminha `/api/v1/` ao Spring Boot e `POST /chat` à API da Lia na mesma EC2.

[Contrato OpenAPI](../Dev%20BackEnd/imoveisapi/docs/openapi.json) · [Execução detalhada](rodar-aplicacao.md)
