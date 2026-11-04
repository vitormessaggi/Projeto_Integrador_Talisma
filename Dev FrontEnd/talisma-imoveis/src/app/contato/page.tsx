import Image from "next/image";
import Link from "next/link";
import { Clock3, Mail, MapPin, Phone } from "lucide-react";
import mapa from "@/assets/figma/contato-mapa.png";
import { perfilSeAutenticado } from "@/lib/api";
import { executar } from "@/lib/acoes";
import { Formulario } from "@/components/formulario";
import { caixa, primario } from "@/components/vitrine";

export default async function Contato() {
  const perfil = await perfilSeAutenticado();
  const informacoes = [
    { rotulo: "Telefone", valor: process.env.TALISMA_TELEFONE, Icone: Phone },
    { rotulo: "E-mail", valor: process.env.TALISMA_EMAIL, Icone: Mail },
    { rotulo: "Endereço", valor: process.env.TALISMA_ENDERECO, Icone: MapPin },
    { rotulo: "Horário", valor: process.env.TALISMA_HORARIO, Icone: Clock3 },
  ].filter(item => item.valor);
  return <section className={`${caixa} py-12 md:py-20`}>
    <h1 className="mb-8 text-4xl font-bold">Fale conosco</h1>
    <div className="grid items-start gap-8 md:grid-cols-2">
      <div>
        <h2 className="text-2xl font-semibold text-primary">Informações de contato</h2>
        <p className="mt-4 max-w-lg leading-relaxed">Envie uma mensagem à Talismã Imóveis. Nossa equipe vai encaminhar seu pedido para atendimento.</p>
        {informacoes.length > 0 && <dl className="mt-6 grid gap-4">{informacoes.map(({ rotulo, valor, Icone }) =>
          <div key={rotulo} className="flex gap-3"><Icone size={24} className="text-primary" aria-hidden />
            <div className="min-w-0"><dt className="font-semibold">{rotulo}</dt><dd className="break-words">{valor}</dd></div></div>)}</dl>}
      </div>
      <div className="rounded-2xl bg-card p-6 shadow-md sm:p-9">
        <h2 className="mb-5 text-xl font-semibold text-primary">Envie sua mensagem</h2>
        {perfil?.perfil === "CLIENTE" ? <>
          <p className="mb-4 break-words text-sm">{perfil.nome} · {perfil.email}</p>
          <Formulario botao="Enviar mensagem" acao={executar.bind(null, { caminho: "/leads", metodo: "POST", retorno: "/contato", campos: ["assunto", "telefone", "mensagem"] })}
            campos={[{ nome: "assunto", rotulo: "Assunto", opcoes: ["Compra", "Locação", "Dúvida sobre atendimento", "Outros"].map(texto => ({ valor: texto, texto })) },
              { nome: "telefone", rotulo: "Telefone", tipo: "tel", inicial: perfil.telefone }, { nome: "mensagem", rotulo: "Mensagem", tipo: "textarea" }]} />
        </> : <Link href={perfil ? "/painel" : "/entrar?retorno=%2Fcontato"} className={primario}>{perfil ? "Acessar painel" : "Entre para enviar uma mensagem"}</Link>}
      </div>
    </div>
    <Image src={mapa} alt="Mapa de São Paulo" sizes="(max-width: 1280px) 95vw, 1200px" className="mt-10 h-auto w-full rounded-lg" />
  </section>;
}
