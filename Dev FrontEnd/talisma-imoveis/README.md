# Site Talismã Imóveis

Next.js App Router, TypeScript, Tailwind e componentes shadcn/ui.

1. Inicie a API Java. O endereço padrão é http://127.0.0.1:8080.
2. Execute `npm ci` e `npm run dev` com Node 24.
3. Abra http://localhost:3000.

Defina `API_URL` no processo do Next.js se a API usar outra porta. O endereço é usado somente no servidor. Defina `COOKIE_SECURE=true` quando servir a aplicação via HTTPS. O navegador recebe a sessão em cookies HttpOnly. Operações autenticadas passam pelas Server Actions do Next.js, com verificação de origem.

Execute `npm run typecheck`, `npm run lint` e `npm run build`. Para testes em navegador, instale Chromium com `npx playwright install chromium` e execute `npm run e2e` com web e API em execução. Use `E2E_BASE_URL` para outra porta.

Para atualizar os tipos após uma mudança de contrato, execute `npm run gerar:api` no repositório integrado. O arquivo de entrada é o OpenAPI do backend.

No formulário de contato, configure as informações reais da imobiliária com TALISMA_TELEFONE, TALISMA_EMAIL, TALISMA_ENDERECO e TALISMA_HORARIO.

## Teste da integração com a Groq

Copie as entradas `GROQ_API_KEY` e `GROQ_MODEL` do `.env.example` da raiz para um arquivo `.env.local` nesta pasta. Preencha a chave da sua conta Groq e reinicie o Next.js. `GROQ_MODEL` é opcional e o padrão é `openai/gpt-oss-120b`.

Com o Next.js em execução, rode `node scripts/testar-chat.mjs` nesta pasta. O script envia uma chamada real à Groq por meio de `POST /api/chat`. Para usar outra porta, passe a URL como argumento, por exemplo `node scripts/testar-chat.mjs http://localhost:3011`. Esse teste usa apenas o Next.js e a Groq.

A rota aceita JSON no formato `{ "mensagem": "Olá!" }` e responde com `{ "resposta": "..." }` quando o modelo retorna texto.
