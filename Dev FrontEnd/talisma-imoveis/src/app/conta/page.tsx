
import { redirect } from "next/navigation";
import { privada, type Pagina, type Perfil, type Schemas } from "@/lib/api";
import { Bloco, Mutacao, Painel } from "@/components/painel-comum";
import { Leads, Visitas } from "@/components/atendimentos";


const links: [string, string][] = [["/conta/privacidade", "Privacidade"], ["/conta", "Meu perfil"], ["/favoritos", "Favoritos"], ["/conta?aba=contatos", "Contatos"], ["/conta?aba=visitas", "Visitas"]];

export default async function Conta({ searchParams }: { searchParams: Promise<{ aba?: string; page?: string }> }) {
  const { aba, page } = await searchParams;
  const numero = page && /^\d{1,6}$/.test(page) ? Number(page) : 0;
  const retorno = aba && ["favoritos", "contatos", "visitas"].includes(aba) ? `/conta?aba=${aba}&page=${numero}` : "/conta";
  const perfil = await privada<Perfil>("/usuarios/me", retorno);
  const funcionario = perfil.perfil === "FUNCIONARIO";
  if (perfil.perfil !== "CLIENTE" && !funcionario) redirect("/painel");
  if (funcionario && aba) redirect("/conta");
  let conteudo: React.ReactNode;
  switch (aba) {
    case "favoritos": {
      redirect("/favoritos");
    }
    case "contatos": conteudo = <Leads dados={await privada<Pagina<Schemas["Lead"]>>(`/leads?page=${numero}`, retorno)} papel={perfil.perfil} destino={retorno} />; break;
    case "visitas": conteudo = <Visitas dados={await privada<Pagina<Schemas["Visita"]>>(`/visitas?page=${numero}`, retorno)} papel={perfil.perfil} destino={retorno} />; break;
    default: conteudo = <div className="grid gap-5">
      <Bloco titulo="Dados pessoais">
        <p className="mb-4 text-sm">{perfil.email}{perfil.cpfMascarado ? ` · CPF ${perfil.cpfMascarado}` : ""}{perfil.rgMascarado ? ` · RG ${perfil.rgMascarado}` : ""}</p>
        <Mutacao operacao={{ caminho: "/usuarios/me", metodo: "PUT", campos: ["nome", "telefone"] }} botao="Salvar perfil" campos={[
          { nome: "nome", rotulo: "Nome", inicial: perfil.nome, autocomplete: "name" },
          { nome: "telefone", rotulo: "Telefone", tipo: "tel", inicial: perfil.telefone, obrigatorio: false, autocomplete: "tel" },
        ]} colunas />
      </Bloco>
      <Bloco titulo="Alterar senha"><Mutacao operacao={{ caminho: "/usuarios/me/senha", metodo: "PUT", campos: ["senhaAtual", "novaSenha"] }} botao="Alterar senha"
        campos={[{ nome: "senhaAtual", rotulo: "Senha atual", tipo: "password", autocomplete: "current-password" },
          { nome: "novaSenha", rotulo: "Nova senha", tipo: "password", autocomplete: "new-password" }]} colunas /></Bloco>
    </div>;
  }
  return <Painel titulo={`Olá, ${perfil.nome}`} links={funcionario ? links.slice(0, 2) : links}>{conteudo}</Painel>;
}
