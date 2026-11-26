import Link from "next/link";
import Image from "next/image";
import { redirect } from "next/navigation";
import { privada, type Pagina, type Perfil, type Schemas } from "@/lib/api";
import { Bloco, Mutacao, Painel, Status, nomesStatus } from "@/components/painel-comum";
import { Leads, Visitas } from "@/components/atendimentos";
import { TreinamentoCorretor } from "@/components/treinamento-corretor";
import type { Campo } from "@/components/formulario";
import { EditorImovel } from "@/components/editor-imovel";
import { Paginacao, Vazio } from "@/components/vitrine";

const abas = ["imoveis", "proprietarios", "contatos", "visitas", "usuarios", "protecao-de-dados", "novo-imovel", "editar-imovel"];

export default async function Gestao({ searchParams }: { searchParams: Promise<{ aba?: string; page?: string; id?: string; status?: string }> }) {
  const busca = await searchParams;
  const aba = busca.aba && abas.includes(busca.aba) ? busca.aba : "imoveis";
  const pagina = busca.page && /^\d{1,6}$/.test(busca.page) ? Number(busca.page) : 0;
  const id = busca.id && /^\d+$/.test(busca.id) ? busca.id : "";
  const status = busca.status === "PENDENTE" ? "PENDENTE" : "";
  const destino = `/painel?aba=${aba}&page=${pagina}${id ? `&id=${id}` : ""}${status ? `&status=${status}` : ""}`;
  const perfil = await privada<Perfil>("/usuarios/me", destino);
  if (perfil.perfil !== "ADMIN" && perfil.perfil !== "CORRETOR") redirect("/conta");
  const admin = perfil.perfil === "ADMIN";
  const links: [string, string][] = [["/conta/privacidade", "Privacidade"], ["/painel", "Imóveis"], ["/painel?aba=novo-imovel", "Cadastrar imóvel"],
    ["/painel?aba=proprietarios", "Proprietários"], ["/painel?aba=contatos", "Contatos"], ["/painel?aba=visitas", "Visitas"]];
  if (perfil.perfil === "CORRETOR") links.push(["/painel?aba=protecao-de-dados", "LGPD e segurança"]);
  if (admin) links.push(["/painel/privacidade", "Solicitações de privacidade"], ["/painel?status=PENDENTE", "Aprovações"], ["/painel?aba=usuarios", "Equipe da imobiliária"]);
  let conteudo: React.ReactNode;
  const filtroPagina = `?page=${pagina}`;
  switch (aba) {
    case "protecao-de-dados":
      if (perfil.perfil !== "CORRETOR") redirect("/painel");
      conteudo = <TreinamentoCorretor />;
      break;
    case "novo-imovel":
    case "editar-imovel": {
      const [proprietarios, existente] = await Promise.all([
        privada<Pagina<Schemas["Proprietario"]>>("/gestao/proprietarios?size=100", destino),
        id && aba === "editar-imovel" ? privada<Schemas["DetalheGestao"]>(`/gestao/imoveis/${id}`, destino) : undefined,
      ]);
      conteudo = <div className="grid gap-5">
        {existente?.imovel && <Bloco titulo={existente.imovel.titulo ?? "Imóvel"}>
          <Status valor={existente.imovel.status} />
          <div className="mt-4 flex flex-wrap gap-4">
            {["RASCUNHO", "REJEITADO"].includes(existente.imovel.status ?? "") && <AcaoImovel id={id} status="PENDENTE" destino={destino} />}
            {admin && existente.imovel.status === "PENDENTE" && <><AcaoImovel id={id} status="PUBLICADO" destino={destino} /><AcaoImovel id={id} status="REJEITADO" destino={destino} /></>}
            {existente.imovel.status !== "ARQUIVADO" && <AcaoImovel id={id} status="ARQUIVADO" destino={destino} />}
          </div>
        </Bloco>}
        {existente?.imovel?.status !== "ARQUIVADO" && <EditorImovel proprietarios={proprietarios.itens} existente={existente} destino={destino} />}
        {existente?.imovel && existente.imovel.status !== "ARQUIVADO" && <Bloco titulo="Fotos do imóvel">
          <p className="mb-4 text-sm">Envie até dez fotos em JPEG ou PNG, com até 5 MB por arquivo.</p>
          <Mutacao operacao={{ caminho: `/gestao/imoveis/${id}/fotos`, metodo: "POST", retorno: destino, fotos: true }}
            campos={[{ nome: "arquivos", rotulo: "Selecione as fotos", tipo: "file" }]} botao="Enviar fotos" />
          <ul className="mt-5 grid gap-3 sm:grid-cols-2">{existente.fotos?.map((foto, index) => <li key={foto} className="flex flex-wrap items-center gap-4 rounded-xl border border-border p-3">
            <Image src={`/api/v1/gestao/imoveis/${id}/fotos/${foto}`} alt={`Foto ${index+1} do imóvel`} width={100} height={80} unoptimized className="h-20 w-24 rounded-lg object-cover"/><span className="text-xs">Foto {index + 1}</span><Mutacao operacao={{ caminho: `/gestao/imoveis/${id}/fotos/${foto}`, metodo: "DELETE", retorno: destino }} botao="Remover foto" />
          </li>)}</ul>
        </Bloco>}
      </div>;
      break;
    }
    case "proprietarios": {
      const dados = await privada<Pagina<Schemas["Proprietario"]>>(`/gestao/proprietarios${filtroPagina}`, destino);
      conteudo = <div className="grid gap-5">
        <Bloco titulo="Novo proprietário"><EditorProprietario destino={destino} /></Bloco>
        <Bloco titulo="Proprietários cadastrados">{dados.itens.length ? <ul className="grid gap-3">{dados.itens.map(dono => <li key={dono.id} className="rounded-lg border p-4">
          <details><summary className="cursor-pointer font-semibold">{dono.nome} · {dono.telefone}</summary>
            <div className="mt-4"><EditorProprietario existente={dono} destino={destino} /></div></details>
        </li>)}</ul> : <Vazio>Nenhum proprietário cadastrado.</Vazio>}
        <Paginacao atual={dados.pagina} total={dados.totalPaginas} href={page => `/painel?aba=proprietarios&page=${page}`} /></Bloco>
      </div>;
      break;
    }
    case "contatos": {
      const [dados, internos] = await Promise.all([privada<Pagina<Schemas["Lead"]>>(`/leads${filtroPagina}`, destino),
        admin ? privada<Pagina<Schemas["ContaInterna"]>>("/admin/usuarios?size=100", destino) : undefined]);
      conteudo = <Leads dados={dados} destino={destino} papel={perfil.perfil} corretores={internos?.itens.filter(conta => conta.perfil === "CORRETOR" && !conta.bloqueado)} />;
      break;
    }
    case "visitas": conteudo = <Visitas dados={await privada<Pagina<Schemas["Visita"]>>(`/visitas${filtroPagina}`, destino)} destino={destino} papel={perfil.perfil} />; break;
    case "usuarios": {
      if (!admin) {
        conteudo = <Vazio>Acesso restrito à administração.</Vazio>;
        break;
      }
      const dados = await privada<Pagina<Schemas["ContaInterna"]>>(`/admin/usuarios${filtroPagina}`, destino);
      conteudo = <div className="grid gap-5">
        <Bloco titulo="Cadastrar corretor"><CadastroEquipe perfil="CORRETOR" destino={destino} /></Bloco>
        <Bloco titulo="Cadastrar funcionário"><CadastroEquipe perfil="FUNCIONARIO" destino={destino} /></Bloco>
        <Bloco titulo="Contas internas"><ul className="grid gap-4">{dados.itens.map(conta => <li key={conta.id} className="grid gap-2 rounded-lg border p-4">
          <span className="font-semibold">{conta.nome}</span><span className="break-words text-sm">{conta.email} · {conta.perfil === "FUNCIONARIO" ? "Funcionário" : conta.perfil} · {conta.bloqueado ? "Bloqueada" : "Ativa"}</span>
          {conta.bloqueado && <Mutacao operacao={{ caminho: `/admin/usuarios/${conta.id}/desbloqueio`, metodo: "PATCH", retorno: destino }} botao="Desbloquear" />}
        </li>)}</ul><Paginacao atual={dados.pagina} total={dados.totalPaginas} href={page => `/painel?aba=usuarios&page=${page}`} /></Bloco>
      </div>;
      break;
    }
    default: {
      const [dados, pendentes, contatos, visitas] = await Promise.all([
        privada<Pagina<Schemas["Resumo"]>>(`/gestao/imoveis${filtroPagina}${status ? "&status=PENDENTE" : ""}`, destino),
        admin && !status ? privada<Pagina<Schemas["Resumo"]>>("/gestao/imoveis?status=PENDENTE&size=1", destino) : null,
        admin && !status ? privada<Pagina<Schemas["Lead"]>>("/leads?size=1", destino) : null,
        admin && !status ? privada<Pagina<Schemas["Visita"]>>("/visitas?size=1", destino) : null,
      ]);
      conteudo = <div className="grid gap-5">
        {pendentes && contatos && visitas && <dl className="grid gap-3 sm:grid-cols-3">
          {[[ "Aguardando aprovação", pendentes.totalElementos ], ["Contatos", contatos.totalElementos], ["Visitas", visitas.totalElementos]].map(([rotulo, total]) =>
            <div key={rotulo} className="rounded-lg bg-muted p-4"><dt className="text-sm">{rotulo}</dt><dd className="mt-2 text-2xl font-semibold text-primary">{total}</dd></div>)}
        </dl>}
        <Bloco titulo={status ? "Imóveis aguardando aprovação" : "Imóveis cadastrados"}>
        <p className="mb-4 text-sm text-muted-foreground">{dados.totalElementos} imóveis nesta lista</p>
        {dados.itens.length ? <ul className="grid gap-3">{dados.itens.map(imovel => <li key={imovel.id} className="flex flex-wrap items-center justify-between gap-3 rounded-lg border p-4">
          <Link className="font-semibold text-primary underline" href={`/painel?aba=editar-imovel&id=${imovel.id}`}>{imovel.titulo}</Link>
          <Status valor={imovel.status} />
        </li>)}</ul> : <Vazio>Nenhum imóvel nesta lista.</Vazio>}
        <Paginacao atual={dados.pagina} total={dados.totalPaginas} href={page => `/painel?page=${page}${status ? "&status=PENDENTE" : ""}`} />
        </Bloco>
      </div>;
    }
  }
  return <Painel titulo={admin ? "Administração" : "Painel do corretor"} links={links}>{conteudo}</Painel>;
}

const estados = "AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO"
  .split(" ").map(valor => ({ valor, texto: valor }));

function CadastroEquipe({ perfil, destino }: { perfil: "CORRETOR" | "FUNCIONARIO"; destino: string }) {
  const campos: Campo[] = [
    { nome: "nome", rotulo: "Nome completo", autocomplete: "name" },
    { nome: "email", rotulo: "E-mail", tipo: "email", autocomplete: "off" },
    { nome: "telefone", rotulo: "Telefone com DDD", tipo: "tel" },
    { nome: "cpf", rotulo: "CPF" },
    { nome: "rg", rotulo: "RG" },
    { nome: "orgaoEmissor", rotulo: "Órgão emissor do RG" },
    { nome: "ufRg", rotulo: "UF do RG", opcoes: estados },
    { nome: "cargo", rotulo: "Cargo" },
    { nome: "senha", rotulo: "Senha inicial", tipo: "password", autocomplete: "new-password" },
  ];
  if (perfil === "CORRETOR") campos.splice(8, 0, { nome: "creci", rotulo: "CRECI" });
  return <Mutacao operacao={{ caminho: "/admin/equipe", metodo: "POST", retorno: destino, fixos: { perfil },
    campos: campos.map(campo => campo.nome) }} campos={campos} botao="Criar conta" colunas />;
}

function EditorProprietario({ existente, destino }: { existente?: Schemas["Proprietario"]; destino: string }) {
  return <Mutacao operacao={{ caminho: existente ? `/gestao/proprietarios/${existente.id}` : "/gestao/proprietarios",
    metodo: existente ? "PUT" : "POST", retorno: destino, campos: ["nome", "telefone"] }}
    campos={[{ nome: "nome", rotulo: "Nome", inicial: existente?.nome }, { nome: "telefone", rotulo: "Telefone", tipo: "tel", inicial: existente?.telefone }]}
    botao={existente ? "Salvar proprietário" : "Cadastrar proprietário"} colunas />;
}

function AcaoImovel({ id, status, destino }: { id: string; status: string; destino: string }) {
  const botoes: Record<string, string> = { PENDENTE: "Enviar para revisão", PUBLICADO: "Aprovar e publicar", REJEITADO: "Rejeitar", ARQUIVADO: "Arquivar" };
  return <Mutacao operacao={{ caminho: `/gestao/imoveis/${id}/status`, metodo: "PATCH", retorno: destino, fixos: { status } }}
    botao={botoes[status] ?? nomesStatus[status]} />;
}
