import Image from "next/image";
import Link from "next/link";
import { ArrowRight, Search, MapPin, ShieldCheck, MessagesSquare, KeyRound } from "lucide-react";
import { chamada, type Imovel, type Pagina, type Schemas } from "@/lib/api";
import { caixa, campo, CartaoImovel, primario, rotulo, Vazio } from "@/components/vitrine";

export const dynamic = "force-dynamic";

export default async function Home() {
  const [filtros, destaques] = await Promise.allSettled([
    chamada<Schemas["Filtros"]>("/imoveis/filtros"),
    chamada<Pagina<Imovel>>("/imoveis?destaque=true&size=6"),
  ]);
  const opcoes = filtros.status === "fulfilled" ? filtros.value : null;
  const lista = destaques.status === "fulfilled" ? destaques.value.itens : [];
  return <>
    <section className={`${caixa} pb-12 pt-3 md:pb-20 md:pt-6`}>
      <div className="relative overflow-hidden rounded-[2rem] bg-[#650014]">
        <div className="absolute inset-y-0 right-0 hidden w-[48%] md:block"><Image src="/images/lar.jpg" alt="Ambiente residencial ilustrativo" fill priority sizes="50vw" className="object-cover"/><div className="absolute inset-0 bg-gradient-to-r from-[#650014] via-[#650014]/10 to-transparent"/></div>
        <div className="relative px-6 pb-28 pt-12 sm:px-10 md:pb-36 md:pt-16 lg:px-14">
          <p className="mb-5 text-[10px] font-semibold uppercase tracking-[.23em] text-rose-200">NOVOS COMEÇOS, NOVOS ENDEREÇOS</p>
          <h1 className="max-w-xl text-balance text-4xl font-semibold leading-[1.12] tracking-[-.045em] text-white md:max-w-[55%] lg:text-6xl">Seu próximo capítulo começa em casa.</h1>
          <p className="mt-5 max-w-sm text-sm leading-7 text-rose-100/80">Encontre o imóvel que combina com a sua vida. A Talismã acompanha você em cada escolha.</p>
          <div className="mt-7 flex items-center gap-2 text-xs text-rose-100/80"><MapPin size={15}/>Encontre seu lugar em São Paulo e região</div>
        </div>
      </div>
      <form action="/imoveis" className="relative z-10 mx-4 -mt-16 rounded-2xl border border-border bg-white p-5 shadow-[0_16px_50px_#35000b0b] sm:mx-8 sm:p-7 md:mx-12">
        <fieldset className="mb-5 flex gap-2 border-b border-border pb-4"><legend className="sr-only">Finalidade</legend>
          {[["VENDA","Comprar"],["LOCACAO","Alugar"]].map(([valor,nome])=><label key={valor} className="cursor-pointer"><input className="peer sr-only" type="radio" name="finalidade" value={valor} defaultChecked={valor==="VENDA"}/><span className="block rounded-lg px-6 py-2.5 text-sm font-semibold text-muted-foreground peer-checked:bg-primary peer-checked:text-white peer-focus-visible:outline-2 peer-focus-visible:outline-primary">{nome}</span></label>)}
        </fieldset>
        <div className="grid items-end gap-4 md:grid-cols-[1fr_1.4fr_auto]">
          <label className={rotulo}>Tipo de imóvel<select name="tipo" className={campo}><option value="">Todos os tipos</option>{opcoes?.tipos?.map(tipo=><option key={tipo}>{tipo}</option>)}</select></label>
          <label className={rotulo}>Onde deseja morar?<select name="cidade" className={campo}><option value="">Todas as cidades</option>{opcoes?.cidades?.map(cidade=><option key={cidade}>{cidade}</option>)}</select></label>
          <button className={`${primario} md:px-8`}><Search size={18}/>Buscar imóveis</button>
        </div>
        {filtros.status === "rejected" && <p role="status" className="mt-4 text-sm text-muted-foreground">O catálogo está temporariamente indisponível. Tente novamente em instantes.</p>}
      </form>
    </section>
    <section className={`${caixa} pb-16 md:pb-24`} aria-labelledby="destaques">
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4"><div><p className="eyebrow">UMA SELEÇÃO PARA VOCÊ</p><h2 id="destaques" className="mt-3 text-3xl font-semibold tracking-tight">Imóveis em destaque</h2></div><Link href="/imoveis" className="flex items-center gap-2 text-xs font-semibold text-primary">Explorar todos<ArrowRight size={17}/></Link></div>
      {lista.length ? <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">{lista.map(imovel=><CartaoImovel key={imovel.id} imovel={imovel}/>)}</div> : <Vazio>{destaques.status === "rejected" ? "Não foi possível carregar os destaques. Tente novamente em instantes." : "Novas oportunidades em breve. Explore o catálogo para encontrar imóveis disponíveis."}</Vazio>}
    </section>
    <section className="border-y border-border bg-[#f5f1ef]"><div className={`${caixa} py-14 md:py-20`}><p className="eyebrow">MAIS DO QUE ENCONTRAR UM IMÓVEL</p><div className="mt-4 grid gap-10 lg:grid-cols-[1fr_1.6fr]"><div><h2 className="max-w-sm text-3xl font-semibold leading-tight tracking-tight">Um novo lar.<br/>Com toda a confiança.</h2><Link href="/sobre" className="mt-6 inline-flex items-center gap-2 text-xs font-semibold text-primary">Conheça a Talismã<ArrowRight size={16}/></Link></div><div className="grid gap-8 sm:grid-cols-3">{[[ShieldCheck,"Escolhas seguras","Informações claras para encontrar o imóvel certo para você."],[MessagesSquare,"Atendimento próximo","Converse com nossos corretores e tire suas dúvidas."],[KeyRound,"Do seu jeito","Compra ou locação, com acompanhamento em cada etapa."]].map(([Icon,titulo,texto])=>{const I=Icon as typeof ShieldCheck;return <div key={titulo as string}><I className="mb-4 text-primary" size={25}/><h3 className="text-sm font-semibold">{titulo as string}</h3><p className="mt-2 text-xs leading-6 text-muted-foreground">{texto as string}</p></div>;})}</div></div></div></section>
    <section className={`${caixa} py-14 md:py-20`}><div className="flex flex-col items-start justify-between gap-6 rounded-3xl border border-border bg-white p-8 sm:flex-row sm:items-center sm:p-12"><div><p className="eyebrow">VAMOS CONVERSAR?</p><h2 className="mt-3 text-2xl font-semibold tracking-tight">O próximo passo pode ser mais simples.</h2><p className="mt-3 text-sm text-muted-foreground">Conte o que você procura. Nossa equipe ajuda a encontrar.</p></div><Link href="/contato" className={`${primario} shrink-0`}>Fale com a Talismã<ArrowRight size={16}/></Link></div></section>
  </>;
}
