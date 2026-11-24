import { perfilSeAutenticado } from "@/lib/api";

export async function GET(_request: Request, { params }: { params: Promise<{ cep: string }> }) {
  const { cep } = await params;
  if (!/^[0-9]{8}$/.test(cep)) return new Response(null, { status: 400 });

  try {
    const perfil = await perfilSeAutenticado();
    if (!perfil) return new Response(null, { status: 401 });
    if (perfil.perfil !== "CORRETOR" && perfil.perfil !== "ADMIN") return new Response(null, { status: 403 });

    const resposta = await fetch(`https://viacep.com.br/ws/${cep}/json/`, {
      next: { revalidate: 86400 },
      redirect: "error",
      signal: AbortSignal.timeout(5000),
    });
    if (!resposta.ok) return new Response(null, { status: 502 });
    const dados = await resposta.json();
    if (dados?.erro === true || dados?.erro === "true") return new Response(null, { status: 404 });
    if (typeof dados?.cep !== "string" || dados.cep.replace("-", "") !== cep || typeof dados.localidade !== "string"
      || !dados.localidade.trim() || typeof dados.uf !== "string" || !/^[A-Z]{2}$/.test(dados.uf)) {
      return new Response(null, { status: 502 });
    }

    return Response.json({
      logradouro: typeof dados.logradouro === "string" ? dados.logradouro.trim() : "",
      bairro: typeof dados.bairro === "string" ? dados.bairro.trim() : "",
      cidade: dados.localidade.trim(),
      uf: dados.uf,
    }, { headers: { "Cache-Control": "private, no-store" } });
  } catch {
    return new Response(null, { status: 502 });
  }
}
