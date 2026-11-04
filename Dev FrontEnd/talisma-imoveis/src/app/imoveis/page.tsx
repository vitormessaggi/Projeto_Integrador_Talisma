import Link from "next/link";
import { chamada, ErroApi, type Imovel, type Pagina, type Schemas } from "@/lib/api";
import { caixa, campo, CartaoImovel, Paginacao, primario, rotulo, Vazio } from "@/components/vitrine";

const filtros = ["finalidade", "tipo", "cidade", "quartosMin", "precoMin", "precoMax", "page"];

export default async function Imoveis({ searchParams }: { searchParams: Promise<Record<string, string | string[] | undefined>> }) {
  const busca = await searchParams;
  const params = new URLSearchParams();
  for (const nome of filtros) if (typeof busca[nome] === "string" && busca[nome]) params.set(nome, busca[nome]);
  const [opcoes, resultado] = await Promise.all([
    chamada<Schemas["Filtros"]>("/imoveis/filtros"),
    chamada<Pagina<Imovel>>(`/imoveis?${params}`).catch((erro: unknown) => {
      if (erro instanceof ErroApi && erro.status === 400) return erro;
      throw erro;
    }),
  ]);
  const finalidade = params.get("finalidade") ?? "";
  return <section className={`${caixa} py-10 md:py-16`}>
    <h1 className="text-3xl font-bold text-primary md:text-4xl">{finalidade === "VENDA" ? "Imóveis à venda" : finalidade === "LOCACAO" ? "Imóveis para locação" : "Encontre seu imóvel"}</h1>
    <form className="my-7 grid gap-4 rounded-xl bg-muted p-4 sm:grid-cols-2 lg:grid-cols-3" action="/imoveis">
      <label className={rotulo}>Finalidade<select className={campo} name="finalidade" defaultValue={finalidade}><option value="">Todas</option><option value="VENDA">Comprar</option><option value="LOCACAO">Alugar</option></select></label>
      <label className={rotulo}>Tipo de imóvel<select className={campo} name="tipo" defaultValue={params.get("tipo") ?? ""}><option value="">Todos</option>{opcoes.tipos?.map(tipo => <option key={tipo}>{tipo}</option>)}</select></label>
      <label className={rotulo}>Cidade<select className={campo} name="cidade" defaultValue={params.get("cidade") ?? ""}><option value="">Todas</option>{opcoes.cidades?.map(cidade => <option key={cidade}>{cidade}</option>)}</select></label>
      {[["quartosMin", "Quartos (mínimo)", "1"], ["precoMin", "Preço mínimo (R$)", "0.01"], ["precoMax", "Preço máximo (R$)", "0.01"]].map(([nome, label, step]) =>
        <label className={rotulo} key={nome}>{label}<input className={campo} name={nome} type="number" min="0" step={step} defaultValue={params.get(nome) ?? ""} /></label>)}
      <div className="flex flex-wrap items-center gap-4 lg:col-span-3"><button className={primario}>Aplicar filtros</button><Link href="/imoveis" className="text-primary underline">Limpar filtros</Link></div>
    </form>
    {resultado instanceof ErroApi ? <p role="alert" className="text-destructive">{resultado.message}</p> : <>
      <p className="mb-5 text-sm text-muted-foreground">{resultado.totalElementos} {resultado.totalElementos === 1 ? "imóvel encontrado" : "imóveis encontrados"}</p>
      {resultado.itens.length ? <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">{resultado.itens.map(imovel => <CartaoImovel key={imovel.id} imovel={imovel} finalidade={finalidade} />)}</div>
        : <Vazio>Nenhum imóvel corresponde aos filtros. Experimente outra cidade ou faixa de preço.</Vazio>}
      <Paginacao atual={resultado.pagina} total={resultado.totalPaginas} href={page => {
        const proxima = new URLSearchParams(params);
        proxima.set("page", String(page));
        return `/imoveis?${proxima}`;
      }} />
    </>}
  </section>;
}
