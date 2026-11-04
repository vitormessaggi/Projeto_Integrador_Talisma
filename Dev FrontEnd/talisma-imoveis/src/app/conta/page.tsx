import Link from "next/link";
import { redirect } from "next/navigation";
import { privada, type Imovel, type Pagina, type Perfil, type Schemas } from "@/lib/api";
import { Bloco, Mutacao, Painel } from "@/components/painel-comum";
import { Leads, Visitas } from "@/components/atendimentos";
import { CartaoImovel, Paginacao, Vazio } from "@/components/vitrine";

const links: [string, string][] = [["/conta", "Meu perfil"], ["/conta?aba=favoritos", "Favoritos"], ["/conta?aba=contatos", "Contatos"], ["/conta?aba=visitas", "Visitas"]];

export default async function Conta({ searchParams }: { searchParams: Promise<{ aba?: string; page?: string }> }) {
  const { aba, page } = await searchParams;
  const numero = page && /^\d{1,6}$/.test(page) ? Number(page) : 0;
  const retorno = aba && ["favoritos", "contatos", "visitas"].includes(aba) ? `/conta?aba=${aba}&page=${numero}` : "/conta";
  const perfil = await privada<Perfil>("/usuarios/me", retorno);
  if (perfil.perfil !== "CLIENTE") redirect("/painel");
  let conteudo: React.ReactNode;
  switch (aba) {
    case "favoritos": {
      const dados = await privada<Pagina<Imovel>>(`/usuarios/me/favoritos?page=${numero}`, retorno);
      conteudo = <Bloco titulo="Favoritos">
        {dados.itens.length ? <div className="grid gap-5 sm:grid-cols-2">{dados.itens.map(imovel => <div key={imovel.id} className="grid gap-3">
          <CartaoImovel imovel={imovel} /><Mutacao operacao={{ caminho: `/usuarios/me/favoritos/${imovel.id}`, metodo: "DELETE", retorno }} botao="Remover favorito" />
        </div>)}</div> : <Vazio>Você ainda não salvou imóveis. <Link href="/imoveis" className="text-primary underline">Explore o catálogo</Link>.</Vazio>}
        <Paginacao atual={dados.pagina} total={dados.totalPaginas} href={p => `/conta?aba=favoritos&page=${p}`} />
      </Bloco>;
      break;
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
  return <Painel titulo={`Olá, ${perfil.nome}`} links={links}>{conteudo}</Painel>;
}
