
import Link from "next/link";
import { notFound } from "next/navigation";
import { chamada, ErroApi, perfilSeAutenticado, type Imovel, type Schemas } from "@/lib/api";
import { executar } from "@/lib/acoes";
import { Formulario } from "@/components/formulario";
import { caixa, dinheiro, primario } from "@/components/vitrine";
import { Galeria, Compartilhar, MapaImovel } from "@/components/recursos-imovel";
import { BotaoFavorito } from "@/components/favoritos";
import { BedDouble, Bath, CarFront, Maximize } from "lucide-react";

export default async function Detalhe({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{finalidade?: string}> }) {
  const { id } = await params;
  if (!/^\d+$/.test(id)) notFound();
  const caminho = `/imoveis/${id}`;
  const [detalhe, perfil] = await Promise.all([
    chamada<Required<Schemas["DetalheCatalogo"]>>(caminho).catch((erro: unknown) => {
      if (erro instanceof ErroApi && erro.status === 404) notFound();
      throw erro;
    }),
    perfilSeAutenticado(),
  ]);
  const imovel = detalhe.imovel as Imovel;
  const {finalidade} = await searchParams;
  const ofertas = [...imovel.ofertas].sort((a,b) => Number(b.finalidade === finalidade)-Number(a.finalidade === finalidade));
  const opcoes = ofertas.map(oferta => ({ valor: String(oferta.id), texto: oferta.finalidade === "VENDA" ? "Compra" : "Locação" }));
  return <section className={`${caixa} py-10 md:py-16`}>
    <Link href="/imoveis" className="text-sm text-primary underline">Voltar para imóveis</Link>
    <div className="mt-5 flex flex-wrap items-start justify-between gap-5"><h1 className="page-title max-w-3xl">{imovel.titulo}</h1><Compartilhar titulo={imovel.titulo}/></div>
    <p className="mt-3">{imovel.bairro}, {imovel.cidade} · {imovel.uf}</p>
    <div className="mt-8 grid gap-8 lg:grid-cols-[3fr_2fr]">
      <div>
        <Galeria fotos={imovel.fotos} titulo={imovel.titulo}/>
        <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">{[[BedDouble,imovel.quartos,"Quartos"],[Bath,imovel.banheiros,"Banheiros"],[CarFront,imovel.vagas,"Vagas"],[Maximize,`${imovel.areaTotal} m²`,"Área total"]].map(([Icon,valor,nome])=>{const I=Icon as typeof BedDouble;return <div key={nome as string} className="rounded-2xl border border-border bg-white p-4"><I size={23} className="mb-3 text-primary"/><p className="font-semibold">{valor as string|number}</p><p className="mt-1 text-xs text-muted-foreground">{nome as string}</p></div>;})}</div>
        <h2 className="mb-3 mt-7 text-2xl font-semibold">Sobre o imóvel</h2><p className="whitespace-pre-line break-words leading-relaxed">{detalhe.descricao}</p>
        <h2 className="mb-4 mt-8 text-2xl font-semibold">Localização</h2><p className="mb-4 text-sm text-muted-foreground">{imovel.bairro}, {imovel.cidade} · {imovel.uf}</p><MapaImovel latitude={detalhe.latitude} longitude={detalhe.longitude}/>
      </div>
      <aside className="relative grid h-fit content-start gap-5 rounded-3xl border border-border bg-white p-6 shadow-sm sm:p-8 lg:sticky lg:top-6">
        <p className="eyebrow pr-8">SEU NOVO ENDEREÇO</p><BotaoFavorito imovel={imovel}/>
        <h2 className="text-xl font-semibold">Encontre a sua melhor opção</h2>
        <ul className="grid gap-3">{ofertas.map(oferta => <li key={oferta.id} className="rounded-xl bg-muted p-4"><span className="text-xs text-muted-foreground">{oferta.finalidade === "VENDA" ? "Valor de venda" : "Valor da locação"} </span><strong className="mt-1 block text-2xl font-semibold tracking-tight text-primary">{dinheiro.format(oferta.preco ?? 0)}{oferta.finalidade === "LOCACAO" && <span className="text-xs font-normal"> /mês</span>}</strong></li>)}</ul>
        {perfil?.perfil === "CLIENTE" ? <>
          <details className="border-t pt-4"><summary className="mb-3 cursor-pointer font-semibold text-primary">Falar com o corretor</summary>
            <Formulario acao={executar.bind(null, { caminho: "/leads", metodo: "POST", retorno: caminho, fixos: { imovelId: Number(id), assunto: "Interesse no imóvel" }, campos: ["ofertaId", "telefone", "mensagem"], numeros: ["ofertaId"] })}
              campos={[{ nome: "ofertaId", rotulo: "Oferta", opcoes, obrigatorio: false }, { nome: "telefone", rotulo: "Telefone", tipo: "tel", inicial: perfil.telefone },
                { nome: "mensagem", rotulo: "Mensagem", tipo: "textarea" }]} botao="Enviar interesse" />
          </details>
          <details className="border-t pt-4"><summary className="mb-3 cursor-pointer font-semibold text-primary">Solicitar visita</summary>
            <Formulario acao={executar.bind(null, { caminho: "/visitas", metodo: "POST", retorno: caminho, fixos: { imovelId: Number(id) }, campos: ["ofertaId", "telefone", "data", "periodo", "mensagem"], numeros: ["ofertaId"] })}
              campos={[{ nome: "ofertaId", rotulo: "Oferta", opcoes, obrigatorio: false }, { nome: "telefone", rotulo: "Telefone", tipo: "tel", inicial: perfil.telefone },
                { nome: "data", rotulo: "Data preferida", tipo: "date" }, { nome: "periodo", rotulo: "Período", opcoes: [{ valor: "MANHA", texto: "Manhã" }, { valor: "TARDE", texto: "Tarde" }, { valor: "NOITE", texto: "Noite" }] },
                { nome: "mensagem", rotulo: "Mensagem", tipo: "textarea", obrigatorio: false }]} botao="Enviar pedido de visita" />
          </details>
        </> : !perfil && <Link className={primario} href={`/entrar?retorno=${encodeURIComponent(caminho)}`}>Entre para falar com o corretor</Link>}
      </aside>
    </div>
  </section>;
}
