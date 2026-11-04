import "server-only";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import type { components } from "./contrato";

export type Schemas = components["schemas"];
export type Imovel = Required<Schemas["ImovelDto"]>;
export type Perfil = Required<Schemas["Perfil"]>;
export type Pagina<T> = { itens: T[]; pagina: number; tamanho: number; totalElementos: number; totalPaginas: number };

export const acessoCookie = "talisma_acesso";
export const renovacaoCookie = "talisma_renovacao";
export const apiUrl = process.env.API_URL ?? "http://127.0.0.1:8080";

export class ErroApi extends Error {
  constructor(public status: number, mensagem: string) {
    super(mensagem);
  }
}

export async function chamada<T>(path: string, init: RequestInit = {}, token?: string): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && typeof init.body === "string") headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  let resposta: Response;
  try {
    resposta = await fetch(`${apiUrl}/api/v1${path}`, { ...init, headers, cache: "no-store", signal: AbortSignal.timeout(10000) });
  } catch {
    throw new ErroApi(503, "O serviço está temporariamente indisponível. Tente novamente.");
  }
  if (!resposta.ok) {
    const problema = await resposta.json().catch(() => null);
    const mensagens: Record<number, string> = {
      400: "Verifique os dados informados.",
      401: "Credenciais inválidas ou sessão expirada.",
      403: "Você não tem acesso a esta operação.",
      404: "Não foi possível encontrar o registro.",
      409: "A operação não é possível no estado atual.",
      423: "Conta bloqueada. Contate a imobiliária.",
    };
    throw new ErroApi(resposta.status, problema?.detail ?? mensagens[resposta.status] ?? "Não foi possível concluir a operação.");
  }
  const conteudo = await resposta.text();
  return conteudo ? JSON.parse(conteudo) as T : undefined as T;
}

export function retornoSeguro(retorno?: string | null) {
  return retorno && /^\/(?!\/)[a-zA-Z0-9/?&=._%+-]*$/.test(retorno) ? retorno : "/conta";
}

export async function privada<T>(path: string, retorno = "/conta"): Promise<T> {
  const jar = await cookies();
  const acesso = jar.get(acessoCookie)?.value;
  const renovar = jar.has(renovacaoCookie);
  const destino = renovar ? "/sessao" : "/entrar";
  if (!acesso) redirect(`${destino}?retorno=${encodeURIComponent(retorno)}`);
  try {
    return await chamada<T>(path, {}, acesso);
  } catch (erro) {
    if (erro instanceof ErroApi && erro.status === 401) redirect(`${destino}?retorno=${encodeURIComponent(retorno)}`);
    throw erro;
  }
}

export async function perfilSeAutenticado() {
  const acesso = (await cookies()).get(acessoCookie)?.value;
  if (!acesso) return null;
  try {
    return await chamada<Perfil>("/usuarios/me", {}, acesso);
  } catch (erro) {
    if (erro instanceof ErroApi && erro.status === 401) return null;
    throw erro;
  }
}
