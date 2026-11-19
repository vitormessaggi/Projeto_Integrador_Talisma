# Validação — 18/11/2026

## Executado neste computador

| Verificação | Resultado e limite |
|---|---|
| Backend `mvnw.cmd verify` | 8 testes aprovados: 6 de integração e 2 de armazenamento/configuração. Inclui cadastro/aceite, exportação própria, isolamento, exclusão e revogação de sessão, além das regras existentes. |
| Migrations V1–V6 | Aplicadas/validadas em MySQL 8.0.46 descartável na porta 33307, esquema exclusivo `talisma_test`. Banco pessoal existente não foi usado. |
| Perfil `migrar` | JAR executado sem servidor HTTP, com credenciais de migrations separadas; esquema validado e processo encerrado com código zero. |
| Frontend | `npm run typecheck`, `npm run lint` e `npm run build` aprovados. OpenAPI regenerado da aplicação em execução e tipos TypeScript atualizados. |
| Playwright de UI | 7 cenários aprovados: responsividade/teclado, área protegida, cadastro minimizado, persistência/revisão da escolha e mapa bloqueado até autorização e removido na revogação. API HTTP de fixture determinística nos testes de UI; não simula persistência AWS. |
| Playwright integrado à API real local | 1 cenário adicional aprovado com conta fictícia descartável: login autenticado, solicitação de acesso, download JSON, exclusão, cookies removidos e token antigo recusado com 401. A conta foi excluída pelo teste. |
| E-mail/Mailpit ponta a ponta | Não executado: não havia Mailpit configurado nesta validação. O teste existente fica condicionado a `E2E_MAILPIT_URL` e API real. Não foi contado como aprovado. |
| Android | `:app:assembleDebug` e `:app:lintDebug` aprovados com JDK 21 e SDK 36. O relatório tem 84 avisos e zero erros; avisos de layout/API/recursos devem ser tratados em manutenção. Não havia dispositivo/emulador conectado para validação funcional nativa. |
| Documentos e scripts | `sincronizar-legais.py --check`, análise sintática dos scripts Python e `git diff --check` aprovados. O programa de migração ainda precisa de ensaio com o dump real e o RDS. |

O JDK 21 e o Chromium de testes foram obtidos das distribuições oficiais para executar as verificações. O JDK 21 foi extraído em `.local`, ignorado pelo Git. Java 25 permanece o runtime do backend. Os serviços temporários de testes são independentes da instância MySQL original.

## Artefatos

- API empacotada: `Dev BackEnd/imoveisapi/target/imoveisapi-0.0.1-SNAPSHOT.jar`.
- APK debug: `Dev Mobile/talisma-app/app/build/outputs/apk/debug/app-debug.apk`.
- Lint Android: `Dev Mobile/talisma-app/app/build/reports/lint-results-debug.html`.
- Relatórios JUnit: `Dev BackEnd/imoveisapi/target/surefire-reports/`.

Esses arquivos de build são locais e não devem ser adicionados ao Git.

## Reproduzir

Use um esquema exclusivo chamado `talisma_test`, forneça `TEST_DB_URL`, `TEST_DB_USER` e `TEST_DB_PASSWORD` e execute `mvnw.cmd verify`. O runner recusa ativar os testes de integração por URL que não termine nesse esquema. Em CI com Docker, `mvnw -Ptestcontainers verify` usa MySQL 8.4 descartável.

No frontend, execute typecheck, lint, build e `npm run e2e`. Sem `API_URL`, os testes de UI sobem a fixture local. Para exercitar dados reais de teste, configure `API_URL` para API de testes e forneça `E2E_CONTA_EMAIL`/`E2E_CONTA_SENHA` de conta fictícia previamente verificada. `dados.spec.ts` **exclui essa conta**. O teste de mapa na API real requer `E2E_IMOVEL_ID` de anúncio publicado com coordenadas. Nunca use conta pessoal ou banco operacional.

## Pendências da implantação AWS

O usuário escolheu criar os serviços seguindo `docs/aws-academy.md`. Nenhum recurso foi criado na conta, nem dados locais foram transferidos para a nuvem nesta execução. O backend ficou preparado para configuração remota; a mudança efetiva depende dos endpoints e do bucket criados no Learner Lab.

Na conta, ainda validar: build da imagem Docker (Docker indisponível neste terminal), permissões do papel EC2, TLS do RDS 8.4, criação/leitura/remoção privada no S3, compensação de falhas, certificado IP e renovação, restore e conciliação de exclusões, saldo, reinício do laboratório e pipeline em GitHub Actions. Arquivos de pipeline foram preparados; não foram executados no GitHub.

Antes de apresentar, faça o roteiro integrado com frontend local e Android físico: cadastrar cliente fictício, verificar código no Mailpit pelo túnel, registrar aceite, publicar imóvel como administrador, buscar/favoritar, solicitar/confirmar visita, enviar foto pelo corretor, exportar dados e excluir a conta. Guarde evidências sem senhas, tokens ou documentos.

## Limpeza realizada

Removidos `.env.example`, `Animal.java`, validador CPF Java sem uso após minimização, prompt de orquestração mobile desatualizado, tela/layout antigos de termos substituídos e strings de conteúdo obsoleto. Removidos 15 arquivos de pesos/itálicos Poppins sem importação no Next.js; preservados Regular, SemiBold, Bold e a licença OFL. Documentação histórica, keystore acadêmico ainda usado pelo Gradle e migrations anteriores foram preservados. Nenhum `.env` privado foi apagado.

A alteração pré-existente de uma linha na rota pública de fotos do frontend foi preservada.
