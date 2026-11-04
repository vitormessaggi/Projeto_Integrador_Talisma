import { TelaAcesso } from "@/components/tela-acesso";
export default async function Pagina({ searchParams }: { searchParams: Promise<{ retorno?: string }> }) {
  return <TelaAcesso modo="recuperar" retorno={(await searchParams).retorno} />;
}
