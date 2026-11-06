# Manual do usuário — app Talismã Imóveis (Android)

Versão 1.0 · EAP 2.6.3. O app mostra os imóveis da Talismã para venda e locação, guarda favoritos, agenda
visitas com um corretor e permite ao corretor enviar fotos dos imóveis.

> Enquanto a API não estiver pronta, o app roda no **modo demonstração**: os dados vêm do próprio app.
> Contas de teste: `cliente@teste.com` / `1234`, `corretor@teste.com` / `1234` e `bloqueada@teste.com`
> (conta bloqueada). Código de verificação de e-mail: `1234`.

## Sumário

1. [Navegação](#1-navegação)
2. [Home](#2-home)
3. [Imóveis e filtros](#3-imóveis-e-filtros)
4. [Detalhe do imóvel e galeria](#4-detalhe-do-imóvel-e-galeria)
5. [Favoritos](#5-favoritos)
6. [Perfil](#6-perfil)
7. [Entrar, cadastrar e recuperar senha](#7-entrar-cadastrar-e-recuperar-senha)
8. [Solicitar visita e Meus contatos](#8-solicitar-visita-e-meus-contatos)
9. [Dados pessoais, Alterar senha, Fale conosco e Termos](#9-dados-pessoais-alterar-senha-fale-conosco-e-termos)
10. [Área do corretor: fotos dos imóveis](#10-área-do-corretor-fotos-dos-imóveis)
11. [Sem conexão](#11-sem-conexão)
12. [Modo demonstração](#12-modo-demonstração)

## 1. Navegação

A barra inferior tem quatro abas: **Início**, **Imóveis**, **Favoritos** e **Perfil**. Voltar na Home fecha o
app; nas outras abas, volta à Home. Todas as telas funcionam em retrato e paisagem.

## 2. Home

Na primeira abertura o app explica por que usa a localização (só para "Perto de mim"); você pode negar e
continuar usando normalmente. Escolha **Comprar** ou **Alugar**, o tipo de imóvel e a cidade e toque em
**Buscar**. Abaixo aparecem os destaques; o coração favorita o imóvel e **Ver todos** abre a lista completa.

| Home | Pedido de localização | Paisagem |
|---|---|---|
| ![Home](prints/t04/home_retrato.png) | ![Localização](prints/t04/home_localizacao_dialogo_retrato.png) | ![Home paisagem](prints/t04/home_paisagem.png) |

## 3. Imóveis e filtros

A lista carrega 10 imóveis por vez ao rolar. Os chips filtram por **preço**, **tipo**, **quartos** e
**cidade** (com as buscas recentes); **Perto de mim** ordena pela distância. Preço mínimo maior que o máximo
mostra um aviso. **Limpar filtros** desfaz os filtros.

| Lista | Filtro de preço | Filtro de tipo | Perto de mim | Nenhum resultado |
|---|---|---|---|---|
| ![Imóveis](prints/t04/imoveis_retrato.png) | ![Preço](prints/t04/imoveis_sheet_preco_erro_retrato.png) | ![Tipo](prints/t04/imoveis_sheet_tipo_retrato.png) | ![Perto](prints/t04/imoveis_perto_de_mim_retrato.png) | ![Vazio](prints/t04/imoveis_filtro_vazio_retrato.png) |

## 4. Detalhe do imóvel e galeria

Mostra fotos, preço, local, quartos, banheiros, vagas, área, descrição (**Ver mais**), endereço e mapa.
O ícone de compartilhar abre o seletor do sistema; o coração favorita. Tocar numa foto abre a **galeria** em
tela cheia (deslize para trocar). O botão **Comprar agora** / **Alugar agora** leva à solicitação de visita.

| Detalhe | Mapa | Galeria | Paisagem |
|---|---|---|---|
| ![Detalhe](prints/t04/detalhe_retrato.png) | ![Mapa](prints/t08/detalhe_mapa_retrato.png) | ![Galeria](prints/t04/galeria_retrato.png) | ![Detalhe paisagem](prints/t04/detalhe_paisagem.png) |

## 5. Favoritos

Os favoritos ficam salvos no aparelho, mesmo sem login e sem internet; ao entrar na conta eles são levados
junto. Para remover, toque no coração; o aviso **Desfazer** restaura.

| Favoritos | Desfazer | Vazio |
|---|---|---|
| ![Favoritos](prints/t04/favoritos_retrato.png) | ![Desfazer](prints/t04/favoritos_desfazer_retrato.png) | ![Vazio](prints/t04/favoritos_vazio_retrato.png) |

## 6. Perfil

Deslogado: convite para **Entrar** ou **Cadastrar**, Fale com a imobiliária e Termos. Logado: Dados pessoais,
Meus favoritos, Meus contatos com corretores, Alterar senha, Fale com a imobiliária, Termos e **Sair** (pede
confirmação).

| Deslogado | Logado | Sair |
|---|---|---|
| ![Perfil deslogado](prints/t06/perfil_deslogado_retrato.png) | ![Perfil logado](prints/t06/perfil_logado_retrato.png) | ![Sair](prints/t06/perfil_sair_dialogo_retrato.png) |

## 7. Entrar, cadastrar e recuperar senha

- **Entrar:** e-mail e senha. Três senhas erradas bloqueiam a conta; a conta bloqueada mostra um aviso para
  falar com a imobiliária.
- **Cadastrar:** nome, e-mail, telefone, CPF e senha, aceitando os Termos; depois digite o código enviado por
  e-mail (no modo demonstração, `1234`).
- **Esqueci minha senha:** informe o e-mail, digite o código e crie a nova senha.

| Entrar | Senha errada | Conta bloqueada | Cadastro | Verificar e-mail |
|---|---|---|---|---|
| ![Login](prints/t06/login_retrato.png) | ![Senha errada](prints/t06/login_senha_errada_retrato.png) | ![Bloqueada](prints/t06/login_bloqueada_retrato.png) | ![Cadastro](prints/t06/cadastro_retrato.png) | ![Código](prints/t06/verificar_email_retrato.png) |

| Esqueci senha | Nova senha | Senha alterada |
|---|---|---|
| ![Esqueci](prints/t06/esqueci_senha_retrato.png) | ![Nova senha](prints/t06/nova_senha_retrato.png) | ![Senha alterada](prints/t06/login_senha_alterada_retrato.png) |

## 8. Solicitar visita e Meus contatos

No Detalhe, toque em **Comprar agora**/**Alugar agora** (pede login). Escolha a data, o período e confirme o
telefone. A solicitação aparece em **Meus contatos** como **Pendente** e passa a **Confirmada** quando o
corretor confirma (no modo demonstração, após 2 minutos). Dá para **cancelar** a visita (pede confirmação).

| Solicitar visita | Data | Enviada | Pendente | Confirmada | Cancelada |
|---|---|---|---|---|---|
| ![Solicitar](prints/t06/solicitar_visita_retrato.png) | ![Data](prints/t06/solicitar_visita_datepicker_retrato.png) | ![Enviada](prints/t06/solicitar_visita_enviada_retrato.png) | ![Pendente](prints/t06/meus_contatos_pendente_retrato.png) | ![Confirmada](prints/t06/meus_contatos_confirmada_retrato.png) | ![Cancelada](prints/t06/meus_contatos_cancelada_retrato.png) |

## 9. Dados pessoais, Alterar senha, Fale conosco e Termos

| Dados pessoais | Alterar senha | Fale com a imobiliária | Termos e Condições |
|---|---|---|---|
| ![Dados](prints/t06/dados_pessoais_retrato.png) | ![Alterar senha](prints/t06/alterar_senha_sucesso_retrato.png) | ![Fale conosco](prints/t06/fale_conosco_retrato.png) | ![Termos](prints/t06/termos_retrato.png) |

## 10. Área do corretor: fotos dos imóveis

Na tela Entrar, toque em **Acesse a área interna** e entre com uma conta de corretor (`corretor@teste.com`);
contas de cliente são recusadas. Escolha o imóvel, adicione fotos pela **câmera** ou pela **galeria**, remova
as que não quiser e toque em **Enviar N fotos**. Se o envio falhar, toque na foto para reenviar. As fotos enviadas
aparecem no Detalhe do imóvel.

| Login do corretor | Selecionar imóvel | Fotos novas | Enviando | Falhou (reenviar) | No Detalhe |
|---|---|---|---|---|---|
| ![Corretor](prints/t08/login_corretor_retrato.png) | ![Selecionar](prints/t08/selecionar_imovel_retrato.png) | ![Novas](prints/t08/fotos_novas_retrato.png) | ![Enviando](prints/t08/fotos_enviando_retrato.png) | ![Falhou](prints/t08/fotos_envio_falhou_retrato.png) | ![Detalhe](prints/t08/detalhe_fotos_enviadas_retrato.png) |

## 11. Sem conexão

Sem internet, o app mostra o que você já viu com o aviso "Você está offline — mostrando dados salvos". Se
nada foi visto antes, aparece "Sem conexão" com o botão **Tentar novamente**.

| Lista offline | Detalhe offline | Nada salvo |
|---|---|---|
| ![Lista offline](prints/t04/imoveis_offline_retrato.png) | ![Detalhe offline](prints/t04/detalhe_offline_retrato.png) | ![Sem conexão](prints/t04/imoveis_estado_offline_retrato.png) |

## 12. Modo demonstração

No Perfil, toque **5 vezes em "Versão 1.0"** para abrir o diálogo **Modo demonstração**. Ligado, o app usa os
dados internos; desligado, usa a API real. **Simular sem conexão** serve para testar o comportamento offline.
Ao alternar, a sessão e os dados salvos são limpos.

![Modo demonstração](prints/t06/modo_demonstracao_retrato.png)
