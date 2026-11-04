import Link from "next/link";
import { autenticar, type Modo } from "@/lib/acoes";
import { retornoSeguro } from "@/lib/api";
import { Formulario, type Campo } from "./formulario";
import { caixa, primario } from "./vitrine";

const email: Campo = { nome: "email", rotulo: "E-mail", tipo: "email", autocomplete: "email" };
const senha: Campo = { nome: "senha", rotulo: "Senha", tipo: "password", autocomplete: "current-password" };
const codigo: Campo = { nome: "codigo", rotulo: "Código de seis dígitos", autocomplete: "one-time-code" };
const modos: Record<Exclude<Modo, "reenviar">, { titulo: string; botao: string; campos: Campo[] }> = {
  entrar: { titulo: "Acesse sua conta", botao: "Entrar", campos: [email, senha] },
  cadastro: { titulo: "Crie sua conta", botao: "Cadastrar", campos: [
    { nome: "nome", rotulo: "Nome completo", autocomplete: "name" }, email,
    { nome: "cpf", rotulo: "CPF" }, { nome: "rg", rotulo: "RG" }, { nome: "orgaoEmissor", rotulo: "Órgão emissor" },
    { nome: "uf", rotulo: "UF do RG", opcoes: "AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO".split(" ").map(valor => ({ valor, texto: valor })) },
    { ...senha, autocomplete: "new-password", rotulo: "Senha (mínimo de 8 caracteres)" },
    { ...senha, nome: "confirmarSenha", autocomplete: "new-password", rotulo: "Confirmar senha" },
  ] },
  verificar: { titulo: "Verifique seu e-mail", botao: "Confirmar código", campos: [email, codigo] },
  recuperar: { titulo: "Recupere sua senha", botao: "Enviar código", campos: [email] },
  redefinir: { titulo: "Defina sua nova senha", botao: "Alterar senha", campos: [email, codigo, { ...senha, nome: "novaSenha", rotulo: "Nova senha", autocomplete: "new-password" }] },
  sessao: { titulo: "Continue sua sessão", botao: "Continuar", campos: [] },
};

export function TelaAcesso({ modo, retorno }: { modo: keyof typeof modos; retorno?: string }) {
  const dados = modos[modo];
  const destino = retornoSeguro(retorno);
  const query = `?retorno=${encodeURIComponent(destino)}`;
  return <section className={`${caixa} py-12 md:py-20`}>
    <div className="mx-auto max-w-lg rounded-2xl bg-card p-5 shadow-md sm:p-9">
      <h1 className="mb-6 text-3xl font-bold text-primary">{dados.titulo}</h1>
      <Formulario acao={autenticar.bind(null, modo, destino)} campos={dados.campos} botao={dados.botao} colunas={modo === "cadastro"} />
      {modo === "entrar" && <nav aria-label="Ajuda com acesso" className="mt-6 flex flex-wrap gap-4 text-sm text-primary underline">
        <Link href={"/cadastro" + query}>Criar conta</Link><Link href={"/esqueci-senha" + query}>Esqueci a senha</Link><Link href={"/verificar-email" + query}>Verificar e-mail</Link>
      </nav>}
      {modo === "verificar" && <details className="mt-6"><summary className="mb-4 cursor-pointer text-primary underline">Preciso de outro código</summary>
        <Formulario acao={autenticar.bind(null, "reenviar", destino)} campos={[email]} botao="Reenviar código" /></details>}
      {modo === "sessao" && <Link href={"/entrar" + query} className={`${primario} mt-5 bg-secondary text-primary hover:text-white`}>Entrar novamente</Link>}
    </div>
  </section>;
}
