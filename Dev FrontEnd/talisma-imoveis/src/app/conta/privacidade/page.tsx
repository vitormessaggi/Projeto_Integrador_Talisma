import { privada } from "@/lib/api";
import { ControlesPrivacidade } from "@/components/privacidade";
import { caixa } from "@/components/vitrine";
type Pedido = { protocolo: string; tipo: string; status: string; resposta?: string };
export default async function Privacidade() {
  const dados = await privada<{ aceiteAtual: boolean; solicitacoes: Pedido[] }>("/usuarios/me/privacidade", "/conta/privacidade");
  return <section className={`${caixa} py-12`}><h1 className="mb-6 text-3xl font-bold text-primary">Meus dados e privacidade</h1><ControlesPrivacidade aceiteAtual={dados.aceiteAtual}/><h2 className="mb-4 mt-10 text-xl font-semibold">Minhas solicitações</h2><ul className="grid gap-3">{dados.solicitacoes.map(p => <li className="rounded border p-4" key={p.protocolo}><p>{p.tipo} · {p.status}</p><p className="break-all text-sm">Protocolo: {p.protocolo}</p><p>{p.resposta}</p></li>)}</ul></section>;
}
