import { FileDown, Fingerprint, ShieldAlert, ShieldCheck } from "lucide-react";
import Link from "next/link";

const cuidados = [
  ["Colete só o necessário", "Peça os dados indispensáveis para o atendimento. Evite receber documentos na fase de pesquisa ou agendamento se eles não forem necessários."],
  ["Controle o acesso", "Use sua própria conta e os canais da imobiliária. Confira destinatários e permissões antes de compartilhar arquivos. Ao sair da mesa, bloqueie a tela."],
  ["Proteja o anúncio", "Revise fotos e descrições antes de publicar. Retire documentos visíveis, nomes de moradores e outras informações pessoais desnecessárias."],
  ["Encaminhe pedidos de titulares", "Encaminhe solicitações de acesso, correção ou exclusão ao responsável por privacidade. Confirme a identidade pelo procedimento interno e registre o encaminhamento."],
];

const cenarios = [
  ["Um proprietário pede seu código de acesso para liberar uma visita. O que fazer?", "Não compartilhe o código. Confirme a solicitação pelo contato que já consta no cadastro e avise o responsável pelo atendimento."],
  ["Um suposto administrador envia por WhatsApp um link para mudar sua senha. Como verificar?", "Abra a aplicação pelo endereço que você já conhece e confirme a solicitação com a equipe por um canal independente da mensagem."],
  ["Você abriu um anexo suspeito. Qual é o primeiro passo?", "Interrompa a interação. Se houver suspeita de execução ou infecção, desconecte o dispositivo da rede e avise a equipe por outro aparelho. Preserve a mensagem e o horário."],
];

export function TreinamentoCorretor() {
  return <div className="grid gap-6">
    <section aria-labelledby="titulo-treinamento" className="overflow-hidden rounded-3xl bg-[#35000b] p-6 text-white sm:p-10">
      <ShieldCheck aria-hidden="true" className="mb-5 text-[#f4e7eb]" size={34} />
      <h2 id="titulo-treinamento" className="max-w-xl text-3xl font-semibold tracking-tight sm:text-4xl">LGPD e segurança no atendimento</h2>
      <p className="mt-4 max-w-xl text-sm leading-7 text-[#f4e7eb] sm:text-base">Cuide dos dados de clientes e proprietários, reconheça tentativas de phishing e saiba como agir quando suspeitar de um golpe.</p>
      <a href="/treinamentos/phishing-talisma.pdf" download className="mt-6 inline-flex min-h-11 items-center gap-2 rounded-lg bg-white px-4 py-2 text-sm font-semibold text-primary hover:bg-accent">
        <FileDown size={18} aria-hidden="true" /> Baixar treinamento de phishing (PDF)
      </a>
    </section>

    <section aria-labelledby="titulo-lgpd" className="rounded-2xl border border-border bg-white p-6 sm:p-8">
      <Fingerprint size={24} aria-hidden="true" className="mb-3 text-primary" />
      <h2 id="titulo-lgpd" className="text-xl font-semibold">Proteção de dados e LGPD</h2>
      <p className="mt-3 max-w-2xl text-sm leading-7 text-muted-foreground">Nome, telefone, e-mail e documentos podem identificar uma pessoa. Trate esses dados conforme a finalidade do atendimento, dentro dos acessos concedidos e dos prazos definidos pela imobiliária.</p>
      <ul className="mt-5 grid gap-3 sm:grid-cols-2">
        {cuidados.map(([titulo, texto]) => <li key={titulo} className="rounded-xl bg-muted p-4">
          <h3 className="font-semibold">{titulo}</h3><p className="mt-2 text-sm leading-6 text-muted-foreground">{texto}</p>
        </li>)}
      </ul>
      <Link href="/privacidade" className="mt-5 inline-block text-sm font-semibold text-primary underline">Ler a política de privacidade do Talismã</Link>
    </section>

    <section aria-labelledby="titulo-incidente" className="rounded-2xl border border-border bg-accent p-6 sm:p-8">
      <ShieldAlert size={24} aria-hidden="true" className="mb-3 text-primary" />
      <h2 id="titulo-incidente" className="text-xl font-semibold">Suspeitou de phishing?</h2>
      <ol className="mt-4 list-decimal space-y-2 pl-5 text-sm leading-6">
        <li>Pare a interação. Feche a página e evite abrir anexos, escanear QR codes ou informar códigos.</li>
        <li>Confirme a solicitação por um canal que você já usa com aquela pessoa ou empresa.</li>
        <li>Avise a administração ou o responsável por segurança ou privacidade da imobiliária.</li>
        <li>Preserve a mensagem e anote o horário, a ação realizada e quais dados podem ter sido expostos.</li>
      </ol>
    </section>

    <section aria-labelledby="titulo-pratica" className="rounded-2xl border border-border bg-white p-6 sm:p-8">
      <h2 id="titulo-pratica" className="text-xl font-semibold">Exercícios rápidos</h2>
      <p className="mt-2 text-sm text-muted-foreground">Pense no que faria e abra cada cenário para conferir.</p>
      <div className="mt-4 grid gap-3">
        {cenarios.map(([pergunta, resposta]) => <details key={pergunta} className="rounded-lg border border-border px-4 py-3">
          <summary className="cursor-pointer text-sm font-semibold">{pergunta}</summary>
          <p className="mt-3 text-sm leading-6 text-muted-foreground">{resposta}</p>
        </details>)}
      </div>
    </section>
  </div>;
}
