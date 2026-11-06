# Roteiro de demonstração — app Talismã Imóveis (~10 min)

Apresentação do app (EAP 2.6.3). Detalhes de cada tela no [manual do usuário](manual-usuario.md).

## Antes de começar

- APK release instalado no emulador `Talisma_API36` ou num aparelho (ver [README do app](../talisma-app/README.md#gerar-o-release)).
- Modo demonstração **ligado** (padrão). Para começar do zero: Configurações → Apps → Talismã Imóveis → Limpar dados.
- Tenha à mão: `cliente@teste.com` / `1234`, `corretor@teste.com` / `1234`, `bloqueada@teste.com`, código `1234`.
- Opcional: uma foto já na galeria do aparelho para a parte do corretor.

## Roteiro

| Tempo | Etapa | O que fazer e falar |
|---|---|---|
| 0:00 | Abertura | Contexto: app Android nativo (Java) da Talismã, parte da EAP 2; mesma stack do professor; funciona sem a API pelo modo demonstração. |
| 0:45 | Home | Primeira abertura: explicar o pedido de localização (só para "Perto de mim") e tocar **Continuar**. Mostrar Comprar/Alugar, tipo, cidade e os destaques. |
| 1:45 | Busca | **Alugar + Apartamento + São Paulo → Buscar.** Na lista, abrir o chip de preço e mostrar o erro de mínimo > máximo; rolar para carregar mais; **Perto de mim** com a distância. Girar a tela: filtro e posição se mantêm. |
| 3:15 | Detalhe | Abrir um imóvel: fotos, galeria em tela cheia, Ver mais, mapa com endereço, **compartilhar** (seletor do sistema). Favoritar no coração. |
| 4:15 | Favoritos e offline | Aba Favoritos: remover e **Desfazer**. Perfil → 5 toques em "Versão 1.0" → **Simular sem conexão**: voltar à lista e ao detalhe já vistos com o aviso offline. Desligar a simulação. |
| 5:30 | Conta | Perfil → Entrar com `cliente@teste.com` / `1234`; os favoritos de deslogado continuam. Citar o bloqueio após 3 tentativas (mostrar `bloqueada@teste.com` se houver tempo). |
| 6:45 | Visita | No Detalhe, **Comprar agora / Alugar agora** → data, período, telefone → enviar. Perfil → **Meus contatos**: "Pendente" (vira "Confirmada" em 2 min) e cancelar. |
| 8:00 | Corretor | Sair → Entrar → **Acesse a área interna** com `corretor@teste.com`. Escolher imóvel, adicionar foto pela câmera ou galeria, **Enviar**; abrir o Detalhe e mostrar a foto nova. |
| 9:15 | Fechamento | Modo demonstração × API real (`local.properties` → `api.url`), release assinado com o keystore de demonstração, testes em API 36, API 24 e Pixel 9 Pro, e o que falta (endpoints reais, aparelho físico). |

## Plano B

- Emulador lento: usar os prints do [manual](manual-usuario.md).
- Visita não confirmou em 2 min: mostrar o print `prints/t06/meus_contatos_confirmada_retrato.png`.
- Galeria vazia no emulador: usar a câmera do emulador para a foto do corretor.
