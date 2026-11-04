import Link from "next/link";
export default function NotFound() {
  return <div className="mx-auto max-w-6xl px-6 py-16"><h1 className="text-2xl font-semibold">Página indisponível</h1><Link href="/imoveis" className="mt-5 inline-block text-primary underline">Explorar imóveis</Link></div>;
}
