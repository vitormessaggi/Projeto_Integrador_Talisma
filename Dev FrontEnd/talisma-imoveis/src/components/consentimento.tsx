"use client";
import Link from "next/link";
import { createContext, useContext, useEffect, useState } from "react";

const versao = "1";
const chave = "talisma:consentimento";
const Contexto = createContext({ opcionais: false, abrir: () => {} });
type Escolha = { versao: string; opcionais: boolean; registradoEm: string };
export function ConsentimentoProvider({ children }: { children: React.ReactNode }) {
  const [escolha, setEscolha] = useState<Escolha | null>(null);
  const [aberto, setAberto] = useState(false);
  useEffect(() => {
    function ler() {
      let salvo: Escolha | null = null;
      try { const v = JSON.parse(localStorage.getItem(chave) ?? "null"); if (v?.versao === versao && typeof v.opcionais === "boolean") salvo = v; } catch {}
      setEscolha(salvo); setAberto(salvo === null);
    }
    ler(); window.addEventListener("storage", ler);
    return () => window.removeEventListener("storage", ler);
  }, []);
  function escolher(opcionais: boolean) {
    const valor = { versao, opcionais, registradoEm: new Date().toISOString() };
    try { localStorage.setItem(chave, JSON.stringify(valor)); } catch { /* A escolha continua válida nesta página. */ }
    setEscolha(valor); setAberto(false);
  }
  return <Contexto.Provider value={{ opcionais: escolha?.opcionais === true, abrir: () => setAberto(true) }}>{children}
    {aberto && <section aria-label="Preferências de cookies e conteúdo externo" className="fixed bottom-20 left-3 right-3 z-50 mx-auto max-w-3xl rounded-2xl border bg-white p-5 text-black shadow-xl md:bottom-4">
      <h2 className="font-semibold">Sua privacidade</h2><p className="my-3 text-sm">Usamos armazenamento necessário para sessão, favoritos e sua escolha. Mapas externos só carregam se você aceitar os opcionais. Não usamos publicidade ou analytics. <Link className="underline" href="/privacidade">Saiba mais</Link>.</p>
      <div className="flex flex-wrap gap-3"><button className="action-secondary" onClick={() => escolher(true)}>Aceitar opcionais</button><button className="action-secondary" onClick={() => escolher(false)}>Recusar opcionais</button></div>
    </section>}
  </Contexto.Provider>;
}
export function useConsentimento() { return useContext(Contexto); }
export function PreferenciasCookies() { const { abrir } = useConsentimento(); return <button className="text-left underline" onClick={abrir}>Preferências de cookies</button>; }
