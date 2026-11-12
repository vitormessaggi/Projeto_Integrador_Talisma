"use server";
import { cookies } from "next/headers";
import { createHash } from "node:crypto";
import { acessoCookie, renovacaoCookie, chamada, apiUrl, ErroApi, type Imovel, type Pagina, type Perfil, type Schemas } from "./api";

async function autenticada<T>(caminho: string, init: RequestInit = {}): Promise<T> {
  const jar = await cookies();
  let token = jar.get(acessoCookie)?.value;
  try {
    if (!token) throw new ErroApi(401, "Sessão expirada");
    return await chamada<T>(caminho, init, token);
  } catch (error) {
    if (!(error instanceof ErroApi) || error.status !== 401) throw error;
    const refreshToken = jar.get(renovacaoCookie)?.value;
    if (!refreshToken) throw error;
    const tokens = await chamada<Required<Schemas["Tokens"]>>("/auth/renovar", {method:"POST", body:JSON.stringify({refreshToken})});
    const options = {httpOnly:true, sameSite:"lax" as const, secure:process.env.COOKIE_SECURE === "true", path:"/"};
    jar.set(acessoCookie, tokens.accessToken, {...options, maxAge:tokens.expiresIn});
    jar.set(renovacaoCookie, tokens.refreshToken, {...options, maxAge:7*86400});
    token = tokens.accessToken;
    return chamada<T>(caminho, init, token);
  }
}
export async function estadoFavoritos() {
  const ambiente = createHash("sha256").update(apiUrl).digest("hex").slice(0,16);
  try {
    const jar = await cookies();
    if (!jar.has(acessoCookie) && !jar.has(renovacaoCookie)) return {ambiente, usuario:null, itens:[] as Imovel[]};
    const perfil = await autenticada<Perfil>("/usuarios/me");
    if (perfil.perfil !== "CLIENTE") return {ambiente, usuario:null, interno:true, itens:[] as Imovel[]};
    const itens: Imovel[] = [];
    let pagina = 0;
    while (true) {
      const dados = await autenticada<Pagina<Imovel>>(`/usuarios/me/favoritos?page=${pagina}&size=100`);
      itens.push(...dados.itens);
      if (++pagina >= dados.totalPaginas) break;
    }
    return {ambiente, usuario:perfil.id, itens};
  } catch (error) {
    return {ambiente, usuario:null, itens:[] as Imovel[], erro: error instanceof ErroApi ? error.message : "Não foi possível carregar os favoritos."};
  }
}
export async function alterarFavorito(id: number, salvo: boolean, usuarioEsperado: number) {
  if (!Number.isSafeInteger(id) || id <= 0 || typeof salvo !== "boolean") return {erro:"Imóvel inválido."};
  try {
    const perfil = await autenticada<Perfil>("/usuarios/me");
    if (perfil.id !== usuarioEsperado || perfil.perfil !== "CLIENTE") return {erro:"A conta mudou. Atualize a página."};
    await autenticada(`/usuarios/me/favoritos/${id}`, {method:salvo ? "PUT" : "DELETE"});
    return {sucesso:true};
  } catch (error) { return {erro:error instanceof ErroApi ? error.message : "Não foi possível salvar o favorito."}; }
}
