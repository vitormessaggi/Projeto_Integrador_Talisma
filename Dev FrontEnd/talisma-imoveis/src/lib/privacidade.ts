"use server";
import { cookies } from "next/headers";
import { revalidatePath } from "next/cache";
import { acessoCookie, renovacaoCookie, chamada, ErroApi } from "./api";
import { versaoLegal } from "./legal";

export async function privacidade(acao: "ACESSO" | "EXCLUSAO" | "EXPORTAR" | "ACEITAR", senha: string) {
  try {
    const token = (await cookies()).get(acessoCookie)?.value;
    if (!token) return { erro: "Entre novamente para continuar." };
    if (acao === "ACEITAR") {
      await chamada("/usuarios/me/aceites", { method: "POST", body: JSON.stringify({ aceiteTermos: true, versaoTermos: versaoLegal, versaoPrivacidade: versaoLegal, origem: "WEB" }) }, token);
    } else if (acao === "EXPORTAR") {
      const dados = await chamada<Record<string, unknown>>("/usuarios/me/privacidade/exportacao", { method: "POST", body: JSON.stringify({ senha }) }, token);
      revalidatePath("/conta/privacidade");
      return { dados: JSON.stringify(dados, null, 2) };
    } else {
      const pedido = await chamada<{ protocolo: string; status: string }>("/usuarios/me/privacidade/solicitacoes", { method: "POST", body: JSON.stringify({ tipo: acao, senha }) }, token);
      if (acao === "EXCLUSAO" && pedido.status === "CONCLUIDA") {
        const jar = await cookies(); jar.delete(acessoCookie); jar.delete(renovacaoCookie);
        return { excluida: true, protocolo: pedido.protocolo };
      }
      revalidatePath("/conta/privacidade"); return { protocolo: pedido.protocolo };
    }
    revalidatePath("/conta/privacidade"); return { sucesso: "Aceite registrado." };
  } catch (e) { return { erro: e instanceof ErroApi ? e.message : "Não foi possível concluir a solicitação." }; }
}
