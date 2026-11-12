import Link from "next/link";
import { executar, sair, type Operacao } from "@/lib/acoes";
import { Formulario, type Campo } from "./formulario";
import { caixa } from "./vitrine";

export function Painel({ titulo, links, children }: { titulo: string; links: [string, string][]; children: React.ReactNode }) {
  return <div className={`${caixa} management-shell grid gap-8 py-9 md:grid-cols-[14rem_minmax(0,1fr)] md:py-12`}>
    <aside className="management-sidebar h-fit rounded-2xl border border-border bg-white p-5 md:sticky md:top-6">
      <p className="eyebrow mb-5 hidden md:block">ESPAÇO TALISMÃ</p>
      <details className="md:hidden">
        <summary className="cursor-pointer font-semibold text-primary">Menu da área interna</summary>
        <Links links={links} />
      </details>
      <div className="hidden md:block"><Links links={links} /></div>
      <form action={sair} className="mt-4 border-t pt-4"><button className="min-h-10 text-sm font-semibold text-primary underline">Sair da conta</button></form>
    </aside>
    <section className="min-w-0"><p className="eyebrow mb-2">MINHA ÁREA</p><h1 className="mb-8 text-3xl font-semibold tracking-tight">{titulo}</h1>{children}</section>
  </div>;
}

function Links({ links }: { links: [string, string][] }) {
  return <nav aria-label="Menu da conta" className="grid gap-2">{links.map(([href, nome]) => <Link key={href} className="rounded-md px-2 py-2 text-sm font-semibold hover:bg-accent" href={href}>{nome}</Link>)}</nav>;
}

export function Mutacao({ operacao, campos = [], botao, colunas }: { operacao: Operacao; campos?: Campo[]; botao: string; colunas?: boolean }) {
  return <Formulario acao={executar.bind(null, operacao)} campos={campos} botao={botao} colunas={colunas} />;
}

export function Bloco({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return <section className="rounded-2xl border border-border bg-white p-5 shadow-sm sm:p-7"><h2 className="mb-5 border-b border-border pb-4 text-lg font-semibold">{titulo}</h2>{children}</section>;
}

export const nomesStatus: Record<string, string> = {
  RASCUNHO: "Rascunho", PENDENTE: "Pendente", PUBLICADO: "Publicado", REJEITADO: "Rejeitado", ARQUIVADO: "Arquivado",
  NOVO: "Novo", EM_ATENDIMENTO: "Em atendimento", ENCERRADO: "Encerrado",
  CONFIRMADA: "Confirmada", CONCLUIDA: "Concluída", CANCELADA: "Cancelada",
};
export function Status({ valor }: { valor?: string }) {
  return <span data-status={valor} className="status-badge">{nomesStatus[valor ?? ""] ?? valor}</span>;
}
