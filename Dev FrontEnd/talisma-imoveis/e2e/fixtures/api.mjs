// Fixture HTTP determinística apenas para testar a UI. Não autentica nem persiste dados.
import { createServer } from "node:http";
const imovel = { id: 1, titulo: "Casa fictícia para teste de privacidade", bairro: "Centro", cidade: "Cidade Teste", uf: "SP", tipo: "CASA", quartos: 2, banheiros: 1, vagas: 1, areaTotal: 80, fotos: [], ofertas: [{ id: 1, finalidade: "VENDA", preco: 250000 }] };
createServer((req, res) => {
  res.setHeader("Content-Type", "application/json");
  if (req.url === "/health") return res.end('{}');
  if (req.url === "/api/v1/imoveis/1") return res.end(JSON.stringify({ imovel, descricao: "Imóvel fictício de teste.", latitude: -23.55, longitude: -46.63 }));
  if (req.url?.startsWith("/api/v1/imoveis/filtros")) return res.end(JSON.stringify({ tipos: ["CASA"], cidades: ["Cidade Teste"] }));
  if (req.url?.startsWith("/api/v1/imoveis?")) return res.end(JSON.stringify({ itens: [], pagina: 0, tamanho: 10, totalElementos: 0, totalPaginas: 0 }));
  res.statusCode = 401; res.end(JSON.stringify({ detail: "Credenciais inválidas ou sessão expirada." }));
}).listen(18089, "127.0.0.1");
