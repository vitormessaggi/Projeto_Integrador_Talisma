import Form from "next/form";
import Link from "next/link";
import { PertoDeMim } from "@/components/recursos-imovel";
import { chamada, ErroApi, type Imovel, type Pagina, type Schemas } from "@/lib/api";
import { caixa, campo, CartaoImovel, Paginacao, primario, rotulo, Vazio } from "@/components/vitrine";

const filtros = ["finalidade", "tipo", "cidade", "quartosMin", "precoMin", "precoMax", "page", "latitude", "longitude", "ordenacao"];

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
  const precoMin = Number(params.get("precoMin") ?? 0);
  const precoMax = Number(params.get("precoMax") ?? Infinity);
  return <section className={`${caixa} py-10 md:py-16`}>
    <p className="eyebrow">ENCONTRE SEU PRÓXIMO LAR</p><h1 className="page-title">{finalidade === "VENDA" ? "Imóveis à venda" : finalidade === "LOCACAO" ? "Imóveis para locação" : "Encontre seu imóvel"}</h1>
    <div className="my-6 flex flex-wrap items-center gap-3">
      {[["VENDA", "À venda"], ["LOCACAO", "Locação"]].map(([valor, nome]) => { const p = new URLSearchParams(params); p.set("finalidade", valor); p.delete("page"); return <Link key={valor} className={`filter-chip ${finalidade === valor ? "active" : ""}`} aria-current={finalidade === valor ? "page" : undefined} href={"/imoveis?"+p}>{nome}</Link>; })}
      <PertoDeMim/>
    </div>
    <details open className="mb-8 rounded-2xl border border-border bg-white p-5 sm:p-6"><summary className="cursor-pointer text-sm font-semibold text-primary">Refinar busca</summary>
    <Form key={params.toString()} className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3" action="/imoveis">
      {["latitude", "longitude", "ordenacao"].map(nome => params.has(nome) ? <input key={nome} type="hidden" name={nome} value={params.get(nome)!}/> : null)}
      <label className={rotulo}>Finalidade<select className={campo} name="finalidade" defaultValue={finalidade}><option value="">Todas</option><option value="VENDA">Comprar</option><option value="LOCACAO">Alugar</option></select></label>
      <label className={rotulo}>Tipo de imóvel<select className={campo} name="tipo" defaultValue={params.get("tipo") ?? ""}><option value="">Todos</option>{opcoes.tipos?.map(tipo => <option key={tipo} value={tipo}>{tipo}</option>)}</select></label>
      <label className={rotulo}>Cidade<select className={campo} name="cidade" defaultValue={params.get("cidade") ?? ""}><option value="">Todas</option>{opcoes.cidades?.map(cidade => <option key={cidade} value={cidade}>{cidade}</option>)}</select></label>
      {[["quartosMin", "Quartos (mínimo)", "1"], ["precoMin", "Preço mínimo (R$)", "0.01"], ["precoMax", "Preço máximo (R$)", "0.01"]].map(([nome, label, step]) =>
        <label className={rotulo} key={nome}>{label}<input className={campo} name={nome} type="number" min="0" step={step} defaultValue={params.get(nome) ?? ""} /></label>)}
      <div className="flex flex-wrap items-center gap-4 lg:col-span-3"><button type="submit" className={primario}>Aplicar filtros</button><Link href="/imoveis" className="text-primary underline">Limpar filtros</Link></div>
    </Form></details>
    {resultado instanceof ErroApi ? <p role="alert" className="text-destructive">{resultado.message}</p> : <>
      <p className="mb-5 text-sm text-muted-foreground">{resultado.totalElementos} {resultado.totalElementos === 1 ? "imóvel encontrado" : "imóveis encontrados"}</p>
      {resultado.itens.length ? <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">{resultado.itens.map(imovel => {
        const oferta = imovel.ofertas.find(item =>
          (!finalidade || item.finalidade === finalidade) && item.preco !== undefined &&
          item.preco >= precoMin && item.preco <= precoMax,
        );
        return <CartaoImovel key={imovel.id} imovel={imovel} ofertaSelecionada={oferta} />;
      })}</div>
        : <Vazio>Nenhum imóvel corresponde aos filtros. Experimente outra cidade ou faixa de preço.</Vazio>}
      <Paginacao atual={resultado.pagina} total={resultado.totalPaginas} href={page => {
        const proxima = new URLSearchParams(params);
        proxima.set("page", String(page));
        return `/imoveis?${proxima}`;
      }} />
    </>}
  </section>;
}
