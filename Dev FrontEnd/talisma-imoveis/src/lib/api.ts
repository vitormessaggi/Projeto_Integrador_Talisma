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
export function origemApi(valor = process.env.API_URL): string {
  if (!valor || !URL.canParse(valor)) return "";
  const url = new URL(valor);
  const mock = process.env.TALISMA_TEST_API === "1" && url.origin === "http://127.0.0.1:18089";
  return (url.protocol === "https:" || mock) && !url.username && !url.password
    && url.pathname === "/" && !url.search && !url.hash ? url.origin : "";
}

export const apiUrl = origemApi();

export function validadeRenovacao(token: string): number {
  try {
    const { session_exp } = JSON.parse(Buffer.from(token.split(".")[1], "base64url").toString("utf8"));
    return typeof session_exp === "number"
      ? Math.max(0, Math.min(86400, session_exp - Math.floor(Date.now() / 1000)))
      : 86400;
  } catch {
    return 86400;
  }
}

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
    const timeout = AbortSignal.timeout(10000);
    const signal = init.signal ? AbortSignal.any([init.signal, timeout]) : timeout;
    resposta = await fetch(`${apiUrl}/api/v1${path}`, { ...init, headers, cache: "no-store", signal });
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
