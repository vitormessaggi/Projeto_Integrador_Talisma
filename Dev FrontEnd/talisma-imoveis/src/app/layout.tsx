import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import localFont from "next/font/local";
import { CircleUserRound } from "lucide-react";
import logo from "@/assets/images/Logo-Red.svg";
import logoBranco from "@/assets/images/Logo-White.svg";
import "@fontsource/roboto/latin-400.css";
import "./globals.css";

const poppins = localFont({
  src: [
    { path: "../assets/fonts/Poppins/Poppins-Regular.ttf", weight: "400" },
    { path: "../assets/fonts/Poppins/Poppins-SemiBold.ttf", weight: "600" },
    { path: "../assets/fonts/Poppins/Poppins-Bold.ttf", weight: "700" },
  ],
  variable: "--font-poppins",
  display: "swap",
});

export const metadata: Metadata = {
  title: { default: "Talismã Imóveis", template: "%s | Talismã Imóveis" },
  description: "Encontre imóveis para compra ou locação e fale com um corretor.",
};

const links = [["/", "Início"], ["/imoveis", "Imóveis"], ["/sobre", "Sobre nós"], ["/contato", "Contato"]];

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-BR" className={poppins.variable}>
      <body>
        <a href="#conteudo" className="sr-only focus:not-sr-only focus:absolute focus:z-20 focus:bg-white focus:p-3">Pular para o conteúdo</a>
        <header className="mx-auto flex max-w-[1600px] flex-wrap items-center justify-between gap-x-5 gap-y-4 px-5 py-5 sm:px-10 md:py-8">
          <Link href="/" aria-label="Talismã Imóveis — início"><Image src={logo} alt="Talismã Imóveis" className="h-auto w-32 md:w-44" priority /></Link>
          <Link href="/conta" className="order-2 rounded-full bg-primary p-2 text-white md:order-3" aria-label="Minha conta">
            <CircleUserRound size={23} aria-hidden />
          </Link>
          <nav className="order-3 flex w-full flex-wrap gap-x-5 gap-y-2 text-sm text-primary md:order-2 md:ml-auto md:w-auto md:gap-9" aria-label="Menu principal">
            {links.map(([href, label]) => <Link className="py-2 hover:underline" key={href} href={href}>{label}</Link>)}
          </nav>
        </header>
        <main id="conteudo" className="min-h-[50vh]">{children}</main>
        <footer className="bg-primary text-primary-foreground">
          <div className="mx-auto grid max-w-7xl gap-9 px-6 py-12 sm:grid-cols-3 sm:px-10">
            <Link href="/"><Image src={logoBranco} alt="Talismã Imóveis" className="h-auto w-40" /></Link>
            <div><h2 className="font-semibold">Contato</h2><Link className="mt-3 inline-block underline" href="/contato">Envie uma mensagem</Link></div>
            <div className="grid content-start gap-3"><h2 className="font-semibold">Informações</h2><Link href="/imoveis">Imóveis</Link><Link href="/sobre">Sobre nós</Link><Link href="/termos">Termos e condições</Link></div>
            <p className="text-sm sm:col-span-3">Talismã Imóveis © {new Date().getFullYear()}</p>
          </div>
        </footer>
      </body>
    </html>
  );
}
