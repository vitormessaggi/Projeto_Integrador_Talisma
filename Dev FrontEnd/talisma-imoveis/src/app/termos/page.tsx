import { caixa } from "@/components/vitrine";
export default function Termos() {
  return <article className={`${caixa} py-14`}><h1 className="text-4xl font-bold text-primary">Termos e condições</h1>
    <div className="mt-7 grid max-w-3xl gap-5 leading-relaxed">
      <h2 className="text-xl font-semibold">Uso do catálogo</h2>
      <p>O site reúne anúncios e permite enviar pedidos de atendimento e visita. Preços e disponibilidade devem ser confirmados com a imobiliária.</p>
      <h2 className="text-xl font-semibold">Cadastro e atendimento</h2>
      <p>Use dados corretos, mantenha sua senha em sigilo e avise a imobiliária se perder acesso à conta. Os dados do cadastro são usados para identificar a conta e encaminhar os pedidos de atendimento.</p>
      <h2 className="text-xl font-semibold">Negociação</h2>
      <p>Solicitar contato ou visita não reserva um imóvel e não formaliza a compra ou a locação.</p>
    </div></article>;
}
