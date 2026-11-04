import Image from "next/image";
import Link from "next/link";
import { chamada, type Imovel, type Pagina, type Schemas } from "@/lib/api";
import { caixa, campo, CartaoImovel, primario, rotulo, Vazio } from "@/components/vitrine";
import intro from "@/assets/figma/home-intro.png";
import mapa from "@/assets/figma/home-mapa.png";
import familia from "@/assets/figma/home-familia.png";

export const dynamic = 'force-dynamic';

export default async function Home() {
  const [filtros, destaques] = await Promise.all([
    chamada<Schemas["Filtros"]>("/imoveis/filtros"),
    chamada<Pagina<Imovel>>("/imoveis?destaque=true&size=6"),
  ]);

  return (
    <>
      <section className="relative isolate bg-primary md:bg-transparent">
        <div className="absolute inset-x-0 top-0 -z-10 hidden h-[86%] bg-primary md:block" />
        <div className="mx-auto grid max-w-[1600px] items-center gap-5 px-5 pb-8 pt-5 md:grid-cols-[42%_58%] md:gap-0 md:pt-0">
          <Image src={intro} alt="" priority sizes="(max-width: 767px) 80vw, 42vw" className="mx-auto h-auto w-full max-w-72 md:max-w-none" />
          <div className="z-10 rounded-3xl bg-[#f2f2f2] px-5 py-7 shadow-xl sm:px-8 md:-ml-5 md:mr-6 md:-translate-y-7 lg:p-10">
            <h1 className="text-balance text-2xl font-semibold leading-tight text-black lg:text-4xl">O imóvel que você procura está aqui!</h1>
            <form action="/imoveis" className="mt-6 grid gap-4 lg:mt-12 lg:grid-cols-[1fr_1.6fr_auto] lg:items-end">
              <fieldset className="flex flex-wrap gap-2 border-b border-input pb-3 lg:col-span-3">
                <legend className="sr-only">Finalidade</legend>
                {[["VENDA", "Comprar"], ["LOCACAO", "Alugar"]].map(([valor, nome]) =>
                  <label key={valor} className="cursor-pointer">
                    <input className="peer sr-only" type="radio" name="finalidade" value={valor} defaultChecked={valor === "VENDA"} />
                    <span className="block rounded-lg px-4 py-2 text-sm font-semibold uppercase peer-checked:bg-[#6b0014] peer-checked:text-white peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-primary">{nome}</span>
                  </label>)}
              </fieldset>
              <label className={rotulo}>Tipo de imóvel<select name="tipo" className={campo}><option value="">Todos os tipos</option>{filtros.tipos?.map(tipo => <option key={tipo}>{tipo}</option>)}</select></label>
              <label className={rotulo}>Onde deseja morar?<select name="cidade" className={campo}><option value="">Todas as cidades</option>{filtros.cidades?.map(cidade => <option key={cidade}>{cidade}</option>)}</select></label>
              <button className={primario}>Buscar</button>
            </form>
          </div>
        </div>
      </section>
      <section className={`${caixa} py-12 md:py-20`} aria-labelledby="destaques">
        <h2 id="destaques" className="mb-8 text-center text-3xl font-bold text-primary">Imóveis em destaque</h2>
        {destaques.itens.length ? <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">{destaques.itens.map(imovel => <CartaoImovel key={imovel.id} imovel={imovel} />)}</div> :
          <Vazio>Ainda não há imóveis em destaque. <Link className="text-primary underline" href="/imoveis">Consulte o catálogo</Link>.</Vazio>}
      </section>
      <section className={`${caixa} grid items-center gap-9 py-10 md:grid-cols-2 md:py-20`}>
        <div className="max-w-xl">
          <p className="text-sm text-muted-foreground">Sobre a empresa</p>
          <h2 className="my-3 text-balance text-3xl font-bold">Realizando seu sonho de forma sólida, confiável e duradoura</h2>
          <p className="leading-relaxed">A Talismã Imóveis ajuda você a encontrar seu próximo imóvel com atendimento personalizado e transparência durante a compra ou locação.</p>
          <Link className="mt-5 inline-block font-semibold text-primary underline" href="/sobre">Conheça a imobiliária</Link>
        </div>
        <Image src={mapa} alt="Mapa de São Paulo, região de atendimento da Talismã Imóveis" sizes="(max-width: 767px) 90vw, 45vw" className="mx-auto h-auto w-full max-w-sm" />
      </section>
      <section className={`${caixa} flex flex-col items-center gap-8 py-12 md:flex-row md:py-20`}>
        <Image src={familia} alt="" sizes="220px" className="size-52 rounded-full border-4 border-primary object-cover" />
        <div><h2 className="text-2xl font-semibold">Encontre seu próximo lar</h2><p className="mt-3">Conte com nossa equipe para tirar dúvidas e agendar sua visita.</p><Link href="/contato" className="mt-4 inline-block text-primary underline">Fale conosco</Link></div>
      </section>
    </>
  );
}
