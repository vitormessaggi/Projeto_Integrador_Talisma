import { TelaAcesso } from "@/components/tela-acesso";
export default async function Pagina({ searchParams }: { searchParams: Promise<{ retorno?: string }> }) {
  return <TelaAcesso modo="verificar" retorno={(await searchParams).retorno} />;
}
