# Lia · API RAG da Talismã

A Lia segue a estrutura do exercício [API na nuvem](https://tarry-minute-1d3.notion.site/9-API-na-nuvem-41bb70973a9f8005b9e8e1435e0e864b?pvs=143). O botão do site chama `POST /api/chat` no Next.js, que encaminha a mensagem ao FastAPI pelo servidor.

| Arquivo | Função |
|---|---|
| `app.py` | FastAPI, rotas `/chat` e `/health`, roteador, busca no Chroma e conexão Groq. |
| `banco_vetorial.py` | Leitura do PDF, divisão em trechos e indexação persistente no Chroma. |
| `Talisma_Base_Conhecimento.pdf` | Informações institucionais, compra, locação, visitas e contato. |
| `requirements.txt` | Dependências Python. |
| `banco_chroma/` | Índice gerado pela ingestão e ignorado pelo Git. |

O caminho **RAG** recupera trechos do PDF e usa LangChain/Groq para responder sobre a empresa e seus processos. A busca de anúncios faz uma consulta à API imobiliária, pois preços e ofertas dependem do catálogo atualizado. Perguntas fora do escopo recebem uma resposta genérica. No site, o visitante escolhe se autoriza o uso da Groq. Com a opção desligada, o chatbot responde com orientações prontas e consultas ao catálogo.

## Executar localmente

Abra um terminal em `Dev Chatbot` e use Python 3.12. Em uma instalação nova no Windows:

```powershell
py -3.12 -m venv .venv
.\.venv\Scripts\python.exe -m pip install torch --index-url https://download.pytorch.org/whl/cpu
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

Se o Python 3.12 estiver instalado por `uv`, crie o ambiente com `uv venv --python 3.12 .venv` e use o executável da venv para os próximos comandos. Com `uv`, instale as dependências usando `uv pip install --python .venv/Scripts/python.exe` no lugar de `python -m pip install`.

Defina `GROQ_API_KEY` em um arquivo privado, fora do Git. O serviço lê `.env` por padrão. Para manter o arquivo fora do projeto, informe seu caminho em `TALISMA_CHAT_ENV`. A variável opcional `GROQ_MODEL` escolhe o modelo. `API_URL` é o endereço da API Java e tem como padrão `http://127.0.0.1:8080`.

```powershell
.\.venv\Scripts\python.exe banco_vetorial.py
.\.venv\Scripts\python.exe -m uvicorn app:app --host 127.0.0.1 --port 8000
```

Execute a indexação ao alterar o PDF. O primeiro uso baixa `sentence-transformers/all-mpnet-base-v2` para o cache da máquina. A API utiliza PyTorch em CPU.

Em outro terminal, teste pelo contrato do exercício:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
$corpo = @{ pergunta = 'Como agendar uma visita?' } | ConvertTo-Json
Invoke-RestMethod http://127.0.0.1:8000/chat -Method Post -ContentType 'application/json' -Body $corpo
```

A documentação OpenAPI fica em [127.0.0.1:8000/docs](http://127.0.0.1:8000/docs). Quando a resposta usa a Groq com contexto recuperado, a API retorna `modo: rag`.

Deixe a API Java e o frontend em execução conforme o [guia geral](../docs/rodar-aplicacao.md). O Next.js usa `CHATBOT_URL=http://127.0.0.1:8000` por padrão. No site, abra **Lia** e marque **Usar Groq** para experimentar a geração RAG.

## Ajuste para EC2

No Ubuntu, use `python3 -m venv .venv` e `.venv/bin/python` nos mesmos passos. Gere o índice na EC2, passe o ambiente de forma privada e inicie o Uvicorn. Use a rede interna entre o Next.js e o FastAPI ou um proxy HTTPS com acesso adequado, conforme a topologia. Aponte `API_URL` do chatbot à API Java e `CHATBOT_URL` do frontend ao FastAPI. A chave fica somente no serviço Python.
