# Talismã Imóveis

Projeto integrador de Análise e Desenvolvimento de Sistemas do SENAI Armando de Arruda Pereira. O sistema reúne um catálogo de compra e locação, painéis de gestão e um aplicativo Android. O site e o app compartilham a API REST.

A Lia, assistente virtual da imobiliária, está documentada no [guia do chatbot](Dev%20Chatbot/README.md).

**Para começar**, siga o [passo a passo para rodar a aplicação](docs/rodar-aplicacao.md). Ele cobre a execução local, a AWS Academy, o Android e os testes com dados fictícios.

## Documentação por área

| Guia | Conteúdo |
|---|---|
| [Backend](docs/backend.md) | Spring Boot, módulos da API, banco, migrations e armazenamento de fotos. |
| [Frontend](docs/frontend.md) | Next.js, estrutura do site, sessão e configuração da API. |
| [Mobile](docs/mobile.md) | Projeto Android, SDK, emulador e integração com a API. |
| [AWS Academy](docs/aws.md) | EC2, Aurora MySQL, S3, rede, publicação e restauração. |

## Referências do projeto

- [Contrato OpenAPI](Dev%20BackEnd/imoveisapi/docs/openapi.json)
- [Schema e migrations](Dev%20BancoDados/README.md)
- [Manual Android](Dev%20Mobile/docs/manual-usuario.md)
- [Análise técnica e regras de negócio](docs/analise-tecnica.md)
- [Resultados de validação](docs/validacao.md)

Os textos legais canônicos ficam em `Dev BackEnd/imoveisapi/src/main/resources/legal/`. Ao alterá-los, atualize a versão e rode `python scripts/sincronizar-legais.py`. Confira as cópias web e Android com `python scripts/sincronizar-legais.py --check`.
