# Site Talismã Imóveis

Consulte o [guia do frontend](../../docs/frontend.md) para conhecer a estrutura do módulo e o [passo a passo de execução](../../docs/rodar-aplicacao.md#24-iniciar-o-frontend) para rodar o site.

Para testar um frontend já em execução, defina `E2E_BASE_URL` no terminal do Playwright. O [guia de execução](../../docs/rodar-aplicacao.md#verificações-automatizadas) descreve também o teste com API simulada.

## Lia, assistente virtual

Inicie a [API Python da Lia](../../Dev%20Chatbot/README.md) e abra o botão **Lia** no canto inferior direito. O Next.js encaminha `POST /api/chat` ao FastAPI em `CHATBOT_URL`, com padrão `http://127.0.0.1:8000`. O histórico fica em memória no navegador. A chave da Groq pertence à API Python.

Com o frontend em execução, teste a rota com `node scripts/testar-chat.mjs http://127.0.0.1:3000`.
