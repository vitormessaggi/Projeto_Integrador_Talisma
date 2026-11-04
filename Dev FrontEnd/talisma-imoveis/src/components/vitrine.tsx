import Image from "next/image";
import Link from "next/link";
import { BedDouble, MapPin, House } from "lucide-react";
import type { Imovel } from "@/lib/api";

export const dinheiro = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
export const caixa = "mx-auto max-w-7xl px-5 sm:px-10";
export const campo = "min-h-11 w-full rounded-md border border-input bg-white px-3 py-2 text-sm text-foreground";
export const primario = "inline-flex min-h-11 items-center justify-center rounded-md bg-primary px-5 py-2.5 text-sm font-semibold text-white hover:bg-[#6b0014]";
export const rotulo = "grid content-start gap-1.5 text-sm font-semibold";

export function CartaoImovel({ imovel, finalidade }: { imovel: Imovel; finalidade?: string }) {
  const oferta = imovel.ofertas.find((item) => item.finalidade === finalidade) ?? imovel.ofertas[0];
  return (
    <article className="overflow-hidden rounded-xl bg-card shadow-md">
      <Link className="block p-4 hover:underline" href={`/imoveis/${imovel.id}`}>
        <div className="relative grid aspect-[5/4] place-items-center overflow-hidden rounded-md bg-muted text-primary">
          {imovel.fotos[0] ? <Image src={imovel.fotos[0]} alt="" fill unoptimized sizes="(max-width: 640px) 90vw, 33vw" className="object-cover" /> : <House size={42} aria-hidden />}
        </div>
        <h2 className="mt-4 text-lg font-semibold">{imovel.titulo}</h2>
        <p className="mt-2 flex items-center gap-2 text-sm"><MapPin size={16} aria-hidden />{imovel.bairro}, {imovel.cidade} · {imovel.uf}</p>
        <p className="mt-2 flex flex-wrap items-center gap-3 text-sm">
          <span className="flex items-center gap-2"><BedDouble size={16} aria-hidden />{imovel.quartos} quartos</span>
          {oferta?.preco !== undefined && <strong>{dinheiro.format(oferta.preco)}{oferta.finalidade === "LOCACAO" ? "/mês" : ""}</strong>}
        </p>
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
  return <p className="rounded-lg bg-muted p-6 text-muted-foreground">{children}</p>;
}
