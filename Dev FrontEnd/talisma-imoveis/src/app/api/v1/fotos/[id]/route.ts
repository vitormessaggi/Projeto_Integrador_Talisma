import { apiUrl } from "@/lib/api";
export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!/^\d+$/.test(id)) return new Response(null, { status: 404 });
  const resposta = await fetch(`${apiUrl}/api/v1/fotos/${id}`, { cache: "no-store", signal: AbortSignal.timeout(10000) }).catch(() => null);
  if (!resposta?.ok) return new Response(null, { status: resposta?.status ?? 503 });
  return new Response(resposta.body, { headers: {
    "Content-Type": resposta.headers.get("content-type") ?? "application/octet-stream",
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
  } });
}
