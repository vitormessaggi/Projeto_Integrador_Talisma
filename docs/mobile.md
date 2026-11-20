# Aplicativo Android

O aplicativo nativo está em `Dev Mobile/talisma-app`. Ele oferece catálogo, filtros, galeria, favoritos, conta, pedido de visita e gestão de fotos pelo corretor.

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
2. Selecione o modo de integração. Para o emulador local, a URL padrão é `http://10.0.2.2:8080`. Para a AWS, use HTTPS com certificado confiável no Android.
3. Inicie a configuração `app` ou execute `gradlew.bat :app:assembleDebug`. A propriedade `-PapiUrl` tem precedência sobre `api.url` em `local.properties`.
4. No perfil do app, toque cinco vezes em **Versão 1.0** para alternar entre demonstração e API. A troca limpa a sessão e o cache. No modo demonstração o servidor simulado usa dados locais.
5. Valide com `gradlew.bat :app:lintDebug` e teste o fluxo no emulador. Use a assinatura do repositório somente na demonstração acadêmica.

[Manual e navegação](../Dev%20Mobile/docs/manual-usuario.md) · [Roteiro de testes](../Dev%20Mobile/docs/roteiro-testes.md) · [Integração das rotas](../Dev%20BackEnd/imoveisapi/docs/migracao-mobile.md) · [Execução detalhada](rodar-aplicacao.md)
