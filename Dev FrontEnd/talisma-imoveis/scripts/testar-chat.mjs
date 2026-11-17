import assert from "node:assert/strict";

const endpoint = new URL("/api/chat", process.argv[2] ?? "http://localhost:3000");

try {
  const response = await fetch(endpoint, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ mensagem: "Responda apenas com a palavra PONG." }),
    signal: AbortSignal.timeout(20_000),
  });

  assert.ok(response.ok, `A rota retornou HTTP ${response.status}.`);
  const dados = await response.json();
  assert.equal(typeof dados.resposta, "string");
  assert.match(dados.resposta, /\bPONG\b/i, "O modelo não retornou o texto esperado.");

  console.log("A rota de chat recebeu uma resposta do modelo.");
} catch (erro) {
  console.error(erro.message);
  process.exitCode = 1;
}
