import assert from "node:assert/strict";

const endpoint = new URL("/api/chat", process.argv[2] ?? "http://127.0.0.1:3000");

async function enviar(mensagem, status = 200) {
  const response = await fetch(endpoint, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ mensagem }),
    signal: AbortSignal.timeout(20_000),
  });
  assert.equal(response.status, status);
  return response.json();
}

const fora = await enviar("azul girafa 123");
assert.match(fora.resposta, /Posso ajudar com imóveis da Talismã/);
assert.equal(fora.links.length, 0);

const visita = await enviar("Como agendar uma visita?");
assert.match(visita.resposta, /solicitar visita/);
assert.ok(visita.links.some(link => link.href === "/imoveis"));

const aluguel = await enviar("Apartamentos para alugar");
assert.ok(aluguel.links.some(link => link.href.includes("finalidade=LOCACAO") && link.href.includes("tipo=APARTAMENTO")));
await enviar("   ", 400);
await enviar("a".repeat(2001), 400);

console.log("Chat: assunto fora do escopo, visita, filtros do catálogo e validação de entrada OK.");
