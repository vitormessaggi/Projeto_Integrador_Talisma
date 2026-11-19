import { redirect } from "next/navigation";
import { privada, type Perfil } from "@/lib/api";
import { Mutacao } from "@/components/painel-comum";
import { caixa } from "@/components/vitrine";
type Pedido = { protocolo: string; usuario_id?: number; tipo: string; status: string; resposta?: string };
export default async function FilaPrivacidade() {
  const perfil = await privada<Perfil>("/usuarios/me", "/painel/privacidade");
  if (perfil.perfil !== "ADMIN") redirect("/conta/privacidade");
  const dados = await privada<Pedido[]>("/admin/privacidade/solicitacoes", "/painel/privacidade");
  return <section className={`${caixa} py-12`}><h1 className="mb-6 text-3xl font-bold">Solicitações de privacidade</h1><p className="mb-5">Responder uma solicitação registra o resultado da análise; contas internas exigem tratamento manual de responsabilidades. Registre a providência realizada ou o fundamento específico, escopo e prazo de eventual retenção.</p><ul className="grid gap-5">{dados.map(p => <li className="rounded-xl border p-5" key={p.protocolo}><p className="break-all">{p.protocolo} · Usuário {p.usuario_id ?? "excluído"} · {p.tipo} · {p.status}</p><p>{p.resposta}</p>{p.status === "PENDENTE" && <Mutacao operacao={{ caminho: `/admin/privacidade/solicitacoes/${p.protocolo}`, metodo: "PATCH", retorno: "/painel/privacidade", campos: ["status", "resposta"] }} botao="Registrar resposta" campos={[{ nome: "status", rotulo: "Resultado", opcoes: [{ valor: "CONCLUIDA", texto: "Concluída" }, { valor: "INDEFERIDA", texto: "Indeferida com fundamento" }] }, { nome: "resposta", rotulo: "Providências e justificativa", tipo: "textarea" }]}/>}</li>)}</ul></section>;
}
