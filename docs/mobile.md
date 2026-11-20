# Aplicativo Android

O aplicativo nativo está em `Dev Mobile/talisma-app`. Ele oferece catálogo, filtros, galeria, favoritos, conta, pedido de visita, gestão de fotos pelo corretor e atendimento pela Lia.

## Tecnologias

Java 11 como nível de linguagem do código Android, JDK 21 para executar o Gradle, Gradle Wrapper 8.14.3, Android Gradle Plugin 8.13.0 e SDK 36 para compilar. O aplicativo suporta Android 7.0 ou posterior (`minSdk 24`). A interface usa Android Views em XML, AndroidX e Material Components. O app usa SQLite para cache e favoritos locais e osmdroid para mapas.

## Estrutura

| Caminho | Responsabilidade |
|---|---|
| `app/src/main/java/br/com/talisma/imoveis/` | Activities, adapters e navegação. |
| `app/src/main/java/br/com/talisma/imoveis/dados/` | API, modelos e repositórios, cache, cofre de credenciais e servidor simulado. |
| `app/src/main/java/br/com/talisma/imoveis/util/` | Carregamento de imagens, localização, tarefas e estados de tela. |
| `app/src/main/res/` | Layouts, fontes, recursos visuais e política de rede. |
| `app/src/main/assets/` | Dados e fotos da demonstração, além de termos e privacidade. |
| `keystore/` | Assinatura acadêmica usada pelo build release. |

## Como montar o app

1. Abra o módulo no Android Studio com JDK 21 e instale o SDK 36 e uma imagem de emulador.
2. Configure a origem HTTPS da AWS com certificado público. O Gradle lê `-PapiUrl`, `API_URL` ou `api.url` no `local.properties`, nessa ordem.
3. Inicie a configuração `app` ou execute `gradlew.bat :app:installDebug`. O endpoint entra no APK durante o build.
4. Confira o catálogo e abra a Lia pelo avatar nas telas principais. Experimente perguntas sobre compra, locação e visitas e abra os atalhos das respostas.
5. Em uma instalação que já usa o modo demonstração, toque cinco vezes em **Versão 1.0** no Perfil e desative o modo para consultar a API. A troca limpa sessão e cache.
6. Valide com `gradlew.bat :app:lintDebug`. Use a assinatura do repositório somente na demonstração acadêmica.

A Lia envia `pergunta` e `usar_ia` para `POST /chat` na mesma origem usada em `/api/v1`. O consentimento à Groq vale para uma pergunta. O Android não envia o token da conta ao chat e mantém a conversa em memória enquanto a tela está aberta.

[Manual e navegação](../Dev%20Mobile/docs/manual-usuario.md) · [Roteiro de testes](../Dev%20Mobile/docs/roteiro-testes.md) · [Integração das rotas](../Dev%20BackEnd/imoveisapi/docs/migracao-mobile.md) · [Execução detalhada](rodar-aplicacao.md)
