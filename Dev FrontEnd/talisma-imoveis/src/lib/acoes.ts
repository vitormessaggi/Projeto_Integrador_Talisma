"use server";

import { versaoLegal } from "./legal";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { revalidatePath } from "next/cache";
import { validadeRenovacao, acessoCookie, chamada, ErroApi, renovacaoCookie, retornoSeguro, type Schemas } from "./api";
import { formatos, normalizarCampo } from "./campos-formatados";

export type Estado = { erro?: string; sucesso?: string };
type Token = Required<Schemas["Tokens"]>;
export type Operacao = { caminho: string; metodo: "POST" | "PUT" | "PATCH" | "DELETE"; retorno?: string;
  fixos?: Record<string, string | number | boolean | null>; campos?: string[]; numeros?: string[]; imovel?: boolean; fotos?: boolean };

const cookieOpts = { httpOnly: true, sameSite: "lax" as const, secure: process.env.COOKIE_SECURE === "true", path: "/" };
const rotasAuth = { entrar: "login", cadastro: "cadastro", verificar: "verificar-email", recuperar: "esqueci-senha", redefinir: "redefinir-senha", reenviar: "reenviar-codigo", sessao: "renovar" } as const;
export type Modo = keyof typeof rotasAuth;

async function guardar(tokens: Token) {
  const jar = await cookies();
  jar.set(acessoCookie, tokens.accessToken, { ...cookieOpts, maxAge: Math.max(1, tokens.expiresIn) });
  jar.set(renovacaoCookie, tokens.refreshToken, { ...cookieOpts, maxAge: validadeRenovacao(tokens.accessToken) });
}

async function renovar() {
  const refreshToken = (await cookies()).get(renovacaoCookie)?.value;
  if (!refreshToken) throw new ErroApi(401, "Entre novamente para continuar.");
  const tokens = await chamada<Token>("/auth/renovar", { method: "POST", body: JSON.stringify({ refreshToken }) });
  await guardar(tokens);
  return tokens;
}

function camposDoFormulario(form: FormData, nomes: string[], numeros: string[] = []) {
  return Object.fromEntries(nomes.map(nome => {
    const valor = form.get(nome);
    if (typeof valor !== "string" || !valor.trim()) return [nome, null];
    if (nome === "telefone" || nome === "cep" || nome === "codigo") {
      const normalizado = normalizarCampo(nome, valor);
      if (normalizado === null) throw new ErroApi(400, formatos[nome].instrucao);
      return [nome, normalizado];
    }
    if (numeros.includes(nome)) {
      const numero = Number(valor);
      if (!Number.isFinite(numero)) throw new ErroApi(400, "Confira os valores numéricos do formulário.");
      return [nome, numero];
    }
    return [nome, valor];
  }));
}

function mensagem(erro: unknown): Estado {
  return { erro: erro instanceof ErroApi ? erro.message : "Não foi possível concluir a operação." };
}

export async function autenticar(modo: Modo, retorno: string, _estado: Estado, form: FormData): Promise<Estado> {
  const destino = retornoSeguro(retorno);
  let proximo: string | undefined;
  try {
    if (modo === "sessao") {
      await renovar();
      proximo = destino;
    } else {
      if (modo === "cadastro" && form.get("senha") !== form.get("confirmarSenha")) return { erro: "As senhas precisam ser iguais." };
      const corpo = camposDoFormulario(form, ["nome", "email", "senha", "codigo", "novaSenha"]);
      if (modo === "cadastro") {
        if (!form.has("aceiteTermos")) return { erro: "Confirme o aceite dos termos após ler os documentos." };
        Object.assign(corpo, { aceiteTermos: true, versaoTermos: versaoLegal, versaoPrivacidade: versaoLegal, origem: "WEB" });
      }
      const tokens = await chamada<Token | undefined>(`/auth/${rotasAuth[modo]}`, { method: "POST", body: JSON.stringify(corpo) });
      const retornoQuery = `?retorno=${encodeURIComponent(destino)}`;
      if (modo === "entrar" && tokens) {
        await guardar(tokens);
        proximo = destino === "/conta" && tokens.usuario.perfil !== "CLIENTE" ? "/painel" : destino;
      } else if (modo === "cadastro") proximo = "/verificar-email" + retornoQuery;
      else if (modo === "recuperar") proximo = "/redefinir-senha" + retornoQuery;
      else if (modo === "verificar" || modo === "redefinir") proximo = "/entrar" + retornoQuery;
    }
  } catch (erro) {
    if (modo === "sessao" && erro instanceof ErroApi && erro.status === 401) {
      const jar = await cookies();
      jar.delete(acessoCookie);
      jar.delete(renovacaoCookie);
      proximo = `/entrar?retorno=${encodeURIComponent(destino)}`;
    } else {
      return mensagem(erro);
    }
  }
  if (proximo) redirect(proximo);
  return { sucesso: "Se a conta estiver apta, um código será enviado por e-mail." };
}

const mutacoes = /^\/(?:usuarios\/me(?:\/senha|\/favoritos\/\d+)?|leads(?:\/\d+\/(?:status|corretor))?|visitas(?:\/\d+\/status)?|admin\/(?:privacidade\/solicitacoes\/[0-9a-f-]{36}|corretores|equipe|usuarios\/\d+\/desbloqueio)|gestao\/(?:proprietarios(?:\/\d+)?|imoveis(?:\/\d+(?:\/status|\/fotos(?:\/\d+)?)?)?))$/;

export async function executar(operacao: Operacao, _estado: Estado, form: FormData): Promise<Estado> {
  if (!mutacoes.test(operacao.caminho)) return { erro: "Operação inválida." };
  let corpo: Record<string, string | number | boolean | null>;
  try {
    corpo = { ...camposDoFormulario(form, operacao.campos ?? [], operacao.numeros), ...operacao.fixos };
  } catch (erro) {
    return mensagem(erro);
  }
  let body: BodyInit | undefined;
  if (operacao.fotos) {
    const arquivos = form.getAll("arquivos").filter((item): item is File => typeof item !== "string" && item.size > 0);
    if (!arquivos.length || arquivos.length > 10 || arquivos.some(arquivo => arquivo.size > 5 * 1024 * 1024)) {
      return { erro: "Envie de uma a dez imagens com até 5 MB cada." };
    }
    const multipart = new FormData();
    arquivos.forEach(arquivo => multipart.append("arquivos", arquivo));
    body = multipart;
  } else if (operacao.metodo !== "DELETE") {
    const { valorVenda, valorLocacao, ...dados } = corpo;
    const ofertas = [["VENDA", valorVenda], ["LOCACAO", valorLocacao]]
      .filter(([, preco]) => typeof preco === "number" && preco > 0).map(([finalidade, preco]) => ({ finalidade, preco }));
    if (operacao.imovel && !ofertas.length) return { erro: "Informe um valor de venda ou de locação maior que zero." };
    body = JSON.stringify(operacao.imovel ? {
      ...dados,
      mobiliado: form.has("mobiliado"),
      destaque: form.has("destaque"),
      ofertas,
    } : corpo);
  }
  const jar = await cookies();
  let acesso = jar.get(acessoCookie)?.value;
  try {
    if (!acesso) acesso = (await renovar()).accessToken;
    try {
      await chamada(operacao.caminho, { method: operacao.metodo, body }, acesso);
    } catch (erro) {
      if (!(erro instanceof ErroApi) || erro.status !== 401) throw erro;
      await chamada(operacao.caminho, { method: operacao.metodo, body }, (await renovar()).accessToken);
    }
  } catch (erro) {
    return mensagem(erro);
  }
  const destino = retornoSeguro(operacao.retorno);
  revalidatePath(destino.split("?")[0]);
  revalidatePath("/");
  if (operacao.caminho.endsWith("/senha")) {
    jar.delete(acessoCookie);
    jar.delete(renovacaoCookie);
    redirect("/entrar");
  }
  return { sucesso: "Alteração salva." };
}

export async function sair() {
  const jar = await cookies();
  const refreshToken = jar.get(renovacaoCookie)?.value;
  if (refreshToken) {
    await chamada("/auth/logout", { method: "POST", body: JSON.stringify({ refreshToken }) });
  }
  jar.delete(acessoCookie);
  jar.delete(renovacaoCookie);
  redirect("/");
}
