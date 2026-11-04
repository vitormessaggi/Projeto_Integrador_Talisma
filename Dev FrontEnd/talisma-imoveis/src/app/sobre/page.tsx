import Image from "next/image";
import Link from "next/link";
import mapa from "@/assets/figma/home-mapa.png";
import { caixa, primario } from "@/components/vitrine";
export default function Sobre() {
  return <section className={`${caixa} grid items-center gap-8 py-14 md:grid-cols-2 md:py-24`}>
    <div><h1 className="text-4xl font-bold text-primary">Sobre a Talismã Imóveis</h1>
      <p className="my-6 leading-relaxed">Ajudamos quem busca comprar ou alugar um imóvel na região de São Paulo. Nossa equipe acompanha a busca, esclarece dúvidas sobre as ofertas e organiza visitas.</p>
      <Link className={primario} href="/contato">Fale com nossa equipe</Link></div>
    <Image src={mapa} alt="Mapa de São Paulo" className="mx-auto h-auto w-full max-w-md rounded-lg" />
  </section>;
}
