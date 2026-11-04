import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { chamada, ErroApi, perfilSeAutenticado, type Imovel, type Schemas } from "@/lib/api";
import { executar } from "@/lib/acoes";
import { Formulario } from "@/components/formulario";
import { caixa, dinheiro, primario, Vazio } from "@/components/vitrine";

export default async function Detalhe({ params }: { params: Promise<{ id: string }> }) {
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
  const opcoes = imovel.ofertas.map(oferta => ({ valor: String(oferta.id), texto: oferta.finalidade === "VENDA" ? "Compra" : "Locação" }));
  return <section className={`${caixa} py-10 md:py-16`}>
    <Link href="/imoveis" className="text-sm text-primary underline">Voltar para imóveis</Link>
    <h1 className="mt-4 text-3xl font-bold text-primary md:text-5xl">{imovel.titulo}</h1>
    <p className="mt-3">{imovel.bairro}, {imovel.cidade} · {imovel.uf}</p>
    <div className="mt-8 grid gap-8 lg:grid-cols-[3fr_2fr]">
      <div>
        {imovel.fotos.length ? <div className="grid gap-4 sm:grid-cols-2">
          {imovel.fotos.map((foto, index) => <a key={foto} href={foto} className={`relative block aspect-[4/3] overflow-hidden rounded-lg ${index === 0 ? "sm:col-span-2" : ""}`}>
            <Image src={foto} alt={`Foto ${index + 1} de ${imovel.titulo}`} fill unoptimized sizes="(max-width: 640px) 95vw, 55vw" className="object-cover" />
          </a>)}
        </div> : <Vazio>Este imóvel ainda não tem fotos disponíveis.</Vazio>}
        <p className="mt-6 flex flex-wrap gap-4 font-semibold"><span>{imovel.quartos} quartos</span><span>{imovel.banheiros} banheiros</span><span>{imovel.vagas} vagas</span><span>{imovel.areaTotal} m²</span></p>
        <h2 className="mb-3 mt-7 text-2xl font-semibold">Sobre o imóvel</h2><p className="whitespace-pre-line break-words leading-relaxed">{detalhe.descricao}</p>
      </div>
      <aside className="grid content-start gap-5 rounded-xl bg-card p-5 sm:p-7">
        <h2 className="text-xl font-semibold">Ofertas</h2>
        <ul className="grid gap-2">{imovel.ofertas.map(oferta => <li key={oferta.id}><span>{oferta.finalidade === "VENDA" ? "Venda" : "Locação"} </span><strong>{dinheiro.format(oferta.preco ?? 0)}{oferta.finalidade === "LOCACAO" ? "/mês" : ""}</strong></li>)}</ul>
        {perfil?.perfil === "CLIENTE" ? <>
          <Formulario acao={executar.bind(null, { caminho: `/usuarios/me/favoritos/${id}`, metodo: "PUT", retorno: caminho })} campos={[]} botao="Salvar em favoritos" />
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
