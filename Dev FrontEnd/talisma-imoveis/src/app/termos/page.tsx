import { termos, versaoLegal } from "@/lib/legal";
import { caixa } from "@/components/vitrine";
export default function Documento() { return <article className={`${caixa} py-12`}><h1 className="text-3xl font-bold text-primary">Termos de uso</h1><p className="my-4 text-sm">Versão {versaoLegal} · Demonstração acadêmica</p><div className="max-w-3xl whitespace-pre-wrap leading-relaxed">{termos}</div></article>; }
