"use client";
import Link from "next/link";
import { Heart } from "lucide-react";
import { useFavoritos } from "@/components/favoritos";
import { CartaoImovel, caixa } from "@/components/vitrine";

export default function Favoritos() {
  const favoritos=useFavoritos();
  return <section className={`${caixa} py-12 md:py-16`}>
    <p className="eyebrow">SEU PRÓXIMO LAR</p><h1 className="page-title">Meus favoritos</h1>
    <p className="mt-3 max-w-xl text-muted-foreground">Guarde os imóveis que combinam com você. Ao entrar, seus favoritos ficam disponíveis também no aplicativo.</p>
    {favoritos.erro && <p role="alert" className="my-6 rounded-xl bg-accent p-4 text-primary">{favoritos.erro}</p>}
    {!favoritos.pronto && !favoritos.erro ? <p role="status" className="py-12">Carregando favoritos…</p> : favoritos.itens.length ?
      <div className="mt-9 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">{favoritos.itens.map(imovel=><CartaoImovel key={imovel.id} imovel={imovel}/>)}</div> :
      <div className="empty-state"><Heart size={40} aria-hidden/><h2 className="text-xl font-semibold">Seu próximo lar pode estar aqui</h2><p>Toque no coração de um imóvel para salvá-lo.</p><Link className="action-primary" href="/imoveis">Explorar imóveis</Link></div>}
  </section>;
}
