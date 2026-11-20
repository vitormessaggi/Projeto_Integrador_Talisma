# Lia · API RAG da Talismã

A Lia segue a estrutura do exercício [API na nuvem](https://tarry-minute-1d3.notion.site/9-API-na-nuvem-41bb70973a9f8005b9e8e1435e0e864b?pvs=143). O site encaminha `POST /api/chat` ao FastAPI pelo Next.js. O Android envia `POST /chat` diretamente à origem HTTPS compartilhada com a API Java.

| Arquivo | Função |
|---|---|
| `app.py` | FastAPI, rotas `/chat` e `/health`, roteador, busca no Chroma e conexão Groq. |
| `banco_vetorial.py` | Leitura do PDF, divisão em trechos e indexação persistente no Chroma. |
| `Talisma_Base_Conhecimento.pdf` | Informações institucionais, compra, locação, visitas e contato. |
| `requirements.txt` | Dependências Python. |
| `Dockerfile` | Instala dependências, baixa o modelo CPU e indexa o PDF para publicação na EC2. |
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

Defina `GROQ_API_KEY` em um arquivo privado, fora do Git. O serviço lê `.env` por padrão. Para manter o arquivo fora do projeto, informe seu caminho em `TALISMA_CHAT_ENV`. A variável opcional `GROQ_MODEL` escolhe o modelo. Em diagnóstico local do módulo Python, defina `API_URL` com a origem HTTPS da API Java na AWS para habilitar os resultados do catálogo.

```powershell
.\.venv\Scripts\python.exe banco_vetorial.py
.\.venv\Scripts\python.exe -m uvicorn app:app --host 127.0.0.1 --port 8000
```

Execute a indexação ao alterar o PDF. O primeiro uso baixa `sentence-transformers/all-mpnet-base-v2` para o cache da máquina. A API utiliza PyTorch em CPU.

Em outro terminal, teste pelo contrato do exercício:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
$corpo = @{ pergunta = 'Como agendar uma visita?'; usar_ia = $false } | ConvertTo-Json
Invoke-RestMethod http://127.0.0.1:8000/chat -Method Post -ContentType 'application/json' -Body $corpo
```

A documentação OpenAPI fica em [127.0.0.1:8000/docs](http://127.0.0.1:8000/docs). Quando a resposta usa a Groq com contexto recuperado, a API retorna `modo: rag`.

## Publicar na EC2

O Compose publica a Lia atrás do Nginx e configura o acesso interno à API Java. Para geração RAG, adicione `GROQ_API_KEY` ao arquivo privado `/opt/talisma/chatbot.env`. Ative a opção Groq ao enviar uma pergunta de orientação no site ou no Android. [Guia de publicação e execução](../docs/rodar-aplicacao.md)
