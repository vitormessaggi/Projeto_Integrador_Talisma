export const runtime = "nodejs";

const limiteCorpo = 16 * 1024;
const limiteMensagem = 2000;
const timeoutMs = 25_000;

function json(dados: object, status = 200) {
  return Response.json(dados, { status, headers: { "Cache-Control": "no-store" } });
}

async function lerMensagem(request: Request): Promise<{ mensagem: string; usarIA: boolean } | Response> {
  const leitor = request.body?.getReader();
  if (!leitor) return json({ erro: "Informe uma mensagem." }, 400);

  const decoder = new TextDecoder("utf-8", { fatal: true });
  let tamanho = 0;
  let corpo = "";

  try {
    for (;;) {
      const { done, value } = await leitor.read();
      if (done) break;

      tamanho += value.byteLength;
      if (tamanho > limiteCorpo) {
        void leitor.cancel().catch(() => {});
        return json({ erro: "O corpo da requisição é muito grande." }, 413);
      }

      corpo += decoder.decode(value, { stream: true });
    }

    const dados: unknown = JSON.parse(corpo + decoder.decode());
    if (typeof dados === "object" && dados !== null && "mensagem" in dados && typeof dados.mensagem === "string") {
      const mensagem = dados.mensagem.trim();
      if (mensagem && mensagem.length <= limiteMensagem) {
        return { mensagem, usarIA: "usarIA" in dados && dados.usarIA === true };
      }
    }

    return json({ erro: "Informe uma mensagem de 1 a 2.000 caracteres." }, 400);
  } catch {
    return json({ erro: "Envie um corpo JSON válido." }, 400);
  } finally {
    leitor.releaseLock();
  }
}

export async function POST(request: Request) {
  const origem = request.headers.get("origin");
  const host = request.headers.get("host") ?? new URL(request.url).host;
  if (origem !== null && (!URL.canParse(origem) || new URL(origem).host !== host)) {
    return json({ erro: "Origem da requisição não permitida." }, 403);
  }

  const tipo = request.headers.get("content-type")?.split(";", 1)[0].trim().toLowerCase();
  if (tipo !== "application/json") {
    return json({ erro: "Use o tipo de conteúdo application/json." }, 415);
  }

  const mensagem = await lerMensagem(request);
  if (mensagem instanceof Response) return mensagem;

  const timeout = AbortSignal.timeout(timeoutMs);
  try {
    const origemChat = process.env.CHATBOT_URL ?? "http://127.0.0.1:8000";
    const resposta = await fetch(new URL("/chat", origemChat), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ pergunta: mensagem.mensagem, usar_ia: mensagem.usarIA }),
      cache: "no-store",
      signal: AbortSignal.any([request.signal, timeout]),
    });
    if (!resposta.ok) return json({ erro: "O serviço de chat está indisponível." }, 502);
    return json(await resposta.json());
  } catch {
    if (timeout.aborted) return json({ erro: "O tempo de resposta do chat foi excedido." }, 504);

    return json({ erro: "O chat não conseguiu responder. Tente novamente em instantes." }, 502);
  }
}
