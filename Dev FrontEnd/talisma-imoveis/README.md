# Site Talismã Imóveis

Consulte o [guia do frontend](../../docs/frontend.md) para a estrutura do módulo e o [passo a passo de execução](../../docs/rodar-aplicacao.md#36-abrir-o-site-e-o-mailpit-com-a-api-da-aws) para rodar o site.

Para testar um frontend já em execução, defina `E2E_BASE_URL` no terminal do Playwright. O [guia de execução](../../docs/rodar-aplicacao.md#verificações-automatizadas) descreve também o teste com API simulada.

## Lia, assistente virtual

Abra o botão **Lia** no canto inferior direito. O Next.js encaminha `POST /api/chat` para `POST /chat` na origem HTTPS de `API_URL`. Configure `CHATBOT_URL` apenas se a Lia usar outra origem HTTPS. O histórico fica em memória no navegador e a chave da Groq fica na [API Python da Lia](../../Dev%20Chatbot/README.md).

Com o frontend em execução, teste a rota com `node scripts/testar-chat.mjs http://127.0.0.1:3000`.
