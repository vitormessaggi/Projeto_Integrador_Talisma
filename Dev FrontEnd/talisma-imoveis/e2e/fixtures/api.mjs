// Fixture HTTP determinística apenas para testar a UI. Não autentica nem persiste dados.
import { createServer } from "node:http";
const imovel = { id: 1, titulo: "Casa fictícia para teste de privacidade", bairro: "Centro", cidade: "Cidade Teste", uf: "SP", tipo: "CASA", quartos: 2, banheiros: 1, vagas: 1, areaTotal: 80, fotos: [], ofertas: [{ id: 1, finalidade: "VENDA", preco: 250000 }] };
createServer(async (req, res) => {
  res.setHeader("Content-Type", "application/json");
  if (req.url === "/health") return res.end('{}');
  if (req.url === "/api/v1/imoveis/1") return res.end(JSON.stringify({ imovel, descricao: "Imóvel fictício de teste.", latitude: -23.55, longitude: -46.63 }));
  if (req.url?.startsWith("/api/v1/imoveis/filtros")) return res.end(JSON.stringify({ tipos: ["CASA"], cidades: ["Cidade Teste"] }));
  if (req.url?.startsWith("/api/v1/imoveis?")) return res.end(JSON.stringify({ itens: [], pagina: 0, tamanho: 10, totalElementos: 0, totalPaginas: 0 }));
  if (req.headers.authorization === "Bearer e2e-corretor") {
    if (req.url === "/api/v1/usuarios/me") return res.end(JSON.stringify({ id: 2, nome: "Corretor fictício", perfil: "CORRETOR" }));
    if (req.method === "GET" && req.url?.startsWith("/api/v1/gestao/proprietarios")) {
      return res.end(JSON.stringify({ itens: [{ id: 1, nome: "Proprietário fictício", telefone: "1132334455" }], pagina: 0, totalPaginas: 1 }));
    }
    if (req.method === "POST" && ["/api/v1/gestao/imoveis", "/api/v1/gestao/proprietarios"].includes(req.url)) {
      try {
        let body = "";
        for await (const parte of req) body += parte;
        const { cep, telefone } = JSON.parse(body);
        const valido = req.url.endsWith("/imoveis") ? /^[0-9]{8}$/.test(cep) : /^[0-9]{10,11}$/.test(telefone);
        res.statusCode = valido ? 201 : 400;
        return res.end(JSON.stringify(valido ? { id: 2 } : { detail: "Formato enviado à API incorreto." }));
      } catch {
        res.statusCode = 400;
        return res.end("{}");
      }
    }
  }
  res.statusCode = 401; res.end(JSON.stringify({ detail: "Credenciais inválidas ou sessão expirada." }));
}).listen(18089, "127.0.0.1");
