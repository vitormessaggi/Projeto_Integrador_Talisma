"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { House, Search, Heart, UserRound } from "lucide-react";
export function NavegacaoMobile() {
  const path=usePathname();
  if(path.startsWith("/painel"))return null;
  return <nav className="mobile-nav" aria-label="Navegação rápida">{[["/","Início",House],["/imoveis","Imóveis",Search],["/favoritos","Favoritos",Heart],["/conta","Perfil",UserRound]].map(([href,nome,Icon])=>{
    const Destino=Icon as typeof House;const ativo=href==="/"?path==="/":path.startsWith(href as string);
    return <Link key={href as string} href={href as string} aria-current={ativo?"page":undefined}><Destino size={21}/><span>{nome as string}</span></Link>;
  })}</nav>;
}
