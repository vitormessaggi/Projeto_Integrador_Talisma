import type { Pagina, Schemas } from "@/lib/api";
import { Bloco, Mutacao, Status } from "./painel-comum";
import { Paginacao, Vazio } from "./vitrine";

export function Leads({ dados, destino, papel, corretores = [] }: { dados: Pagina<Schemas["Lead"]>; destino: string;
  papel: string; corretores?: Schemas["ContaInterna"][] }) {
  return <Bloco titulo="Contatos">
    {dados.itens.length ? <ul className="grid gap-4">{dados.itens.map(lead => <li key={lead.id} className="grid gap-2 rounded-lg border p-4">
      <div className="flex flex-wrap items-center justify-between gap-3"><h3 className="font-semibold">{lead.assunto}</h3><Status valor={lead.status} /></div>
      <p className="whitespace-pre-line break-words text-sm">{lead.mensagem}</p>
      <p className="text-sm text-muted-foreground">Telefone {lead.telefone} {lead.imovelId ? `· Imóvel #${lead.imovelId}` : "· Contato geral"}</p>
      {papel !== "CLIENTE" && lead.status !== "ENCERRADO" && <details><summary className="cursor-pointer text-sm text-primary underline">Atualizar atendimento</summary>
        <div className="mt-3"><Mutacao operacao={{ caminho: `/leads/${lead.id}/status`, metodo: "PATCH", campos: ["status"], retorno: destino }} botao="Atualizar"
          campos={[{ nome: "status", rotulo: "Status", inicial: lead.status, opcoes: [{ valor: "NOVO", texto: "Novo" }, { valor: "EM_ATENDIMENTO", texto: "Em atendimento" }, { valor: "ENCERRADO", texto: "Encerrado" }] }]} /></div></details>}
      {papel === "ADMIN" && !lead.imovelId && corretores.length > 0 && <details><summary className="cursor-pointer text-sm text-primary underline">Atribuir corretor</summary>
        <div className="mt-3"><Mutacao operacao={{ caminho: `/leads/${lead.id}/corretor`, metodo: "PATCH", campos: ["corretorId"], numeros: ["corretorId"], retorno: destino }} botao="Atribuir"
          campos={[{ nome: "corretorId", rotulo: "Corretor", inicial: lead.corretorId, opcoes: corretores.map(c => ({ valor: String(c.id), texto: c.nome ?? "" })) }]} /></div></details>}
    </li>)}</ul> : <Vazio>Nenhum contato registrado.</Vazio>}
    <Paginacao atual={dados.pagina} total={dados.totalPaginas} href={p => `${destino.split("?")[0]}?aba=contatos&page=${p}`} />
  </Bloco>;
}

export function Visitas({ dados, destino, papel }: { dados: Pagina<Schemas["Visita"]>; destino: string; papel: string }) {
  const interno = papel !== "CLIENTE";
  return <Bloco titulo="Visitas">
    {dados.itens.length ? <ul className="grid gap-4">{dados.itens.map(visita => <li className="grid gap-3 rounded-lg border p-4" key={visita.id}>
      <div className="flex flex-wrap items-center justify-between gap-3"><h3 className="font-semibold">Visita ao imóvel #{visita.imovelId}</h3><Status valor={visita.status} /></div>
      <p className="text-sm">{visita.dataPreferida?.split("-").reverse().join("/")} · {visita.periodo === "MANHA" ? "Manhã" : visita.periodo === "TARDE" ? "Tarde" : "Noite"}</p>
      <div className="flex flex-wrap gap-3">
        {interno && visita.status === "PENDENTE" && <AcaoVisita id={visita.id} status="CONFIRMADA" destino={destino} botao="Confirmar visita" />}
        {interno && visita.status === "CONFIRMADA" && <AcaoVisita id={visita.id} status="CONCLUIDA" destino={destino} botao="Concluir visita" />}
        {(visita.status === "PENDENTE" || visita.status === "CONFIRMADA") && <AcaoVisita id={visita.id} status="CANCELADA" destino={destino} botao="Cancelar visita" />}
      </div>
    </li>)}</ul> : <Vazio>Nenhuma visita registrada.</Vazio>}
    <Paginacao atual={dados.pagina} total={dados.totalPaginas} href={p => `${destino.split("?")[0]}?aba=visitas&page=${p}`} />
  </Bloco>;
}

function AcaoVisita({ id, status, destino, botao }: { id?: number; status: string; destino: string; botao: string }) {
  return <Mutacao operacao={{ caminho: `/visitas/${id}/status`, metodo: "PATCH", retorno: destino, fixos: { status } }} botao={botao} />;
}
