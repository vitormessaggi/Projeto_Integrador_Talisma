import Image from "next/image";
import Link from "next/link";
import { BedDouble, Bath, CarFront, Maximize, MapPin, House, ArrowUpRight } from "lucide-react";
import type { Imovel } from "@/lib/api";
import { BotaoFavorito } from "./favoritos";

export const dinheiro = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
export const caixa = "mx-auto max-w-7xl px-5 sm:px-10";
export const campo = "field";
export const primario = "action-primary";
export const rotulo = "grid content-start gap-1.5 text-sm font-semibold";

export function CartaoImovel({ imovel, finalidade }: { imovel: Imovel; finalidade?: string }) {
  const oferta = imovel.ofertas.find((item) => item.finalidade === finalidade) ?? imovel.ofertas[0];
  return (
    <article className="property-card">
      <div className="relative">
        <Link href={`/imoveis/${imovel.id}${oferta?.finalidade ? `?finalidade=${oferta.finalidade}` : ""}`} aria-label={`Ver ${imovel.titulo}`} className="property-photo">
          {imovel.fotos[0] ? <Image src={imovel.fotos[0]} alt={imovel.titulo} fill unoptimized sizes="(max-width: 640px) 90vw, 33vw" className="object-cover" /> : <House size={42} aria-hidden />}
        </Link>
        <span className="property-label">{oferta?.finalidade === "LOCACAO" ? "Para alugar" : "À venda"}</span>
        <BotaoFavorito imovel={imovel}/>
      </div>
      <Link className="block p-5" href={`/imoveis/${imovel.id}${oferta?.finalidade ? `?finalidade=${oferta.finalidade}` : ""}`}>
        <p className="mb-2 flex items-center gap-1.5 text-xs text-muted-foreground"><MapPin size={14} aria-hidden />{imovel.bairro}, {imovel.cidade}</p>
        <h2 className="line-clamp-2 min-h-12 text-base font-semibold leading-6">{imovel.titulo}</h2>
        <div className="my-4 flex flex-wrap gap-x-4 gap-y-2 text-xs text-muted-foreground">
          <span className="flex items-center gap-1"><BedDouble size={15} aria-hidden/>{imovel.quartos} quartos</span>
          <span className="flex items-center gap-1"><Bath size={15} aria-hidden/>{imovel.banheiros}</span>
          <span className="flex items-center gap-1"><CarFront size={15} aria-hidden/>{imovel.vagas}</span>
          <span className="flex items-center gap-1"><Maximize size={15} aria-hidden/>{imovel.areaTotal} m²</span>
        </div>
        <div className="flex items-end justify-between border-t border-border pt-4">
          <div>{oferta?.preco !== undefined && <p className="text-xl font-semibold tracking-tight text-primary">{dinheiro.format(oferta.preco)}{oferta.finalidade === "LOCACAO" && <span className="text-xs font-normal text-muted-foreground"> /mês</span>}</p>}
          {imovel.distanciaKm != null && <p className="mt-1 text-xs text-muted-foreground">A {imovel.distanciaKm.toFixed(1)} km de você</p>}</div>
          <ArrowUpRight size={20} className="text-primary" aria-hidden/>
        </div>
      </Link>
    </article>
  );
}

export function Paginacao({ atual, total, href }: { atual: number; total: number; href: (page: number) => string }) {
  if (total <= 1 && atual === 0) return null;
  return (
    <nav aria-label="Paginação" className="mt-9 flex flex-wrap items-center gap-5">
      {atual > 0 && <Link className="text-primary underline" href={href(atual - 1)}>Anterior</Link>}
      <span className="text-sm">Página {atual + 1}{total > 0 ? ` de ${total}` : ""}</span>
      {atual + 1 < total && <Link className="text-primary underline" href={href(atual + 1)}>Próxima</Link>}
    </nav>
  );
}

export function Vazio({ children }: { children: React.ReactNode }) {
  return <p className="rounded-2xl border border-dashed border-border bg-muted/50 p-8 text-muted-foreground">{children}</p>;
}
