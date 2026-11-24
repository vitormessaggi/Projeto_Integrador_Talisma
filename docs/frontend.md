# Frontend

O site em `Dev FrontEnd/talisma-imoveis` reúne o catálogo público, a conta do cliente e os painéis do corretor e do administrador.

## Tecnologias

Next.js 16.4.5 com App Router e Server Actions, React 19, TypeScript, Tailwind CSS 4 e componentes shadcn/ui. A execução usa Node.js 24 e npm. Playwright cobre os fluxos de navegador.

## Estrutura

| Caminho | Responsabilidade |
|---|---|
| `src/app/` | Rotas do catálogo, conta, privacidade e painel. |
| `src/app/api/v1/` | Rotas Next.js que encaminham fotos públicas e privadas à API. |
| `src/components/` | Navegação, formulários, vitrine e componentes de gestão. |
| `src/lib/api.ts` e `src/lib/acoes.ts` | Cliente HTTP executado no servidor, ações autenticadas e cookies de sessão. |
| `src/lib/contrato.ts` | Tipos gerados a partir do OpenAPI do backend. |
| `public/` e `e2e/` | Arquivos estáticos, testes Playwright e API simulada para os testes. |

As páginas do catálogo consultam a API pelo servidor Next.js. A sessão web usa cookies HttpOnly e as operações autenticadas são encaminhadas à API. O site acessa as fotos pelas suas próprias rotas.

## Como montar o frontend

1. Inicie a API e abra a pasta `Dev FrontEnd/talisma-imoveis`.
2. Use Node.js 24 e execute `npm ci`.
3. Defina `API_URL` com a origem HTTPS da EC2 conforme o guia de execução. Execute `npm run dev` e abra [o site](http://127.0.0.1:3000/). A rota interna `/api/chat` encaminha perguntas à Lia em `POST /chat` na mesma origem. Se o FastAPI estiver publicado separadamente, configure `CHATBOT_URL` com a outra origem HTTPS.

No cadastro e na edição de imóveis, a rota `/api/cep/{cep}` do Next.js consulta o ViaCEP para corretores e administradores autenticados. O formulário sugere logradouro, bairro, cidade e UF. Quem cadastra o imóvel confere o endereço, informa o número e completa manualmente os campos quando a consulta falha ou retorna um endereço parcial. A API imobiliária recebe o CEP e o telefone com apenas dígitos. [ViaCEP](https://viacep.com.br/)
4. Se mudar o contrato, execute `npm run gerar:api`. Valide com `npm run typecheck`, `npm run lint` e `npm run build`.

Use `COOKIE_SECURE=true` quando o próprio site for servido por HTTPS. A página de contato lê `TALISMA_TELEFONE`, `TALISMA_EMAIL`, `TALISMA_ENDERECO` e `TALISMA_HORARIO` da configuração do servidor.

[Execução detalhada](rodar-aplicacao.md)
