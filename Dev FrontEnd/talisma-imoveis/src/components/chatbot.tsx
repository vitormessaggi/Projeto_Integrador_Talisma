"use client";

import Image from "next/image";
import Link from "next/link";
import { RotateCcw, Send, X } from "lucide-react";
import { useEffect, useRef, useState, type FormEvent } from "react";
type RespostaChat = { resposta: string; links?: { titulo: string; href: string }[] };
type Mensagem = RespostaChat & { autor: "visitante" | "lia" };
const sugestoes = ["Imóveis para alugar", "Imóveis à venda", "Como agendar uma visita?"];
const avatar = "/images/lia.webp";

export function Chatbot() {
  const [aberto, setAberto] = useState(false);
  const [mensagens, setMensagens] = useState<Mensagem[]>([]);
  const [texto, setTexto] = useState("");
  const [aguardando, setAguardando] = useState(false);
  const [usarIA, setUsarIA] = useState(false);
  const [erro, setErro] = useState("");
  const entrada = useRef<HTMLInputElement>(null);
  const botao = useRef<HTMLButtonElement>(null);
  const conversa = useRef<HTMLDivElement>(null);
  const pedido = useRef<AbortController | null>(null);

  useEffect(() => {
    if (aberto) entrada.current?.focus();
  }, [aberto]);

  useEffect(() => {
    if (conversa.current) conversa.current.scrollTop = conversa.current.scrollHeight;
  }, [mensagens, aguardando, aberto]);

  useEffect(() => () => pedido.current?.abort(), []);

  function fechar() {
    setAberto(false);
    botao.current?.focus();
  }

  function limparConversa() {
    pedido.current?.abort();
    pedido.current = null;
    setMensagens([]);
    setTexto("");
    setErro("");
    setAguardando(false);
    entrada.current?.focus();
  }

  async function enviar(valor: string) {
    const mensagem = valor.trim();
    if (!mensagem || pedido.current) return;

    const controller = new AbortController();
    pedido.current = controller;
    setErro("");
    setTexto("");
    setAguardando(true);
    setMensagens(atual => [...atual, { autor: "visitante", resposta: mensagem }]);

    try {
      const resposta = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ mensagem, usarIA }),
        signal: controller.signal,
      });
      if (!resposta.ok) throw new Error("Falha no chat");
      const dados: RespostaChat = await resposta.json();
      if (pedido.current === controller) {
        setMensagens(atual => [...atual, { ...dados, autor: "lia" }]);
      }
    } catch {
      if (pedido.current === controller && !controller.signal.aborted) {
        setMensagens(atual => atual.slice(0, -1));
        setTexto(mensagem);
        setErro("Não consegui responder agora. Tente enviar novamente.");
      }
    } finally {
      if (pedido.current === controller) {
        pedido.current = null;
        setAguardando(false);
      }
    }
  }

  function enviarFormulario(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    void enviar(texto);
  }

  return (
    <aside className="fixed bottom-24 right-4 z-40 flex flex-col items-end gap-3 md:bottom-5 md:right-5" aria-label="Assistente da Talismã">
      {aberto && (
        <section id="chat-lia" role="dialog" aria-labelledby="chat-lia-titulo"
          onKeyDown={evento => { if (evento.key === "Escape") fechar(); }}
          className="flex h-[31rem] max-h-[calc(100dvh-11rem)] w-[min(23rem,calc(100vw-2rem))] flex-col overflow-hidden rounded-3xl border border-border bg-white shadow-xl md:max-h-[calc(100dvh-7rem)]">
          <header className="flex shrink-0 items-center gap-3 bg-[#650014] px-4 py-3 text-white">
            <Image src={avatar} alt="" width={42} height={42} className="rounded-full ring-1 ring-white/40" />
            <div className="flex-1"><h2 id="chat-lia-titulo" className="font-semibold">Lia</h2><p className="text-xs text-rose-100">Assistente virtual da Talismã</p></div>
            <div className="flex shrink-0 items-center">
              <button type="button" onClick={limparConversa} aria-label="Limpar conversa" title="Limpar conversa" className="rounded-full p-2 hover:bg-white/15 focus-visible:outline-2 focus-visible:outline-white"><RotateCcw size={18} aria-hidden="true" /></button>
              <button type="button" onClick={fechar} aria-label="Fechar conversa" className="rounded-full p-2 hover:bg-white/15"><X size={20} /></button>
            </div>
          </header>
          <div ref={conversa} role="log" aria-label="Mensagens da conversa" className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4 text-sm">
            <p className="mr-4 rounded-2xl rounded-tl-sm bg-[#f7eff0] px-3 py-2.5">Olá! Sou a Lia. Posso ajudar você a encontrar um imóvel, entender como comprar ou alugar e agendar uma visita.</p>
            {!mensagens.length && <div className="flex flex-wrap gap-2">
              {sugestoes.map(sugestao => <button key={sugestao} type="button" onClick={() => void enviar(sugestao)} className="rounded-full border border-border px-3 py-1.5 text-xs text-primary hover:bg-muted">{sugestao}</button>)}
            </div>}
            {mensagens.map((mensagem, indice) => (
              <div key={indice} className={mensagem.autor === "lia" ? "mr-4 rounded-2xl rounded-tl-sm bg-[#f7eff0] px-3 py-2.5" : "ml-8 rounded-2xl rounded-tr-sm bg-primary px-3 py-2.5 text-white"}>
                <p className="break-words"><span className="sr-only">{mensagem.autor === "lia" ? "Lia: " : "Você: "}</span>{mensagem.resposta}</p>
                {!!mensagem.links?.length && <ul className="mt-2 space-y-1">
                  {mensagem.links.map(link => <li key={link.href}><Link href={link.href} onClick={fechar} className="font-medium text-primary underline underline-offset-2">{link.titulo}</Link></li>)}
                </ul>}
              </div>
            ))}
            {aguardando && <p className="text-xs text-muted-foreground" role="status">Lia está respondendo…</p>}
          </div>
          <form onSubmit={enviarFormulario} className="shrink-0 border-t border-border p-3">
            {erro && <p role="alert" className="mb-2 text-xs text-primary">{erro}</p>}
            <div className="flex gap-2">
              <input ref={entrada} aria-label="Sua mensagem para Lia" maxLength={2000} value={texto} onChange={evento => setTexto(evento.target.value)}
                placeholder="Pergunte sobre imóveis…" className="min-w-0 flex-1 rounded-xl border border-border px-3 text-base focus:border-primary focus:outline-2 focus:outline-primary" />
              <button type="submit" disabled={aguardando || !texto.trim()} aria-label="Enviar mensagem" className="rounded-xl bg-primary p-3 text-white disabled:opacity-50"><Send size={18} /></button>
            </div>
            <label className="mt-2 flex items-start gap-2 text-xs text-muted-foreground">
              <input type="checkbox" checked={usarIA} onChange={evento => setUsarIA(evento.target.checked)} />
              Usar Groq. Algumas perguntas serão enviadas ao serviço de IA. Evite dados pessoais.
            </label>
          </form>
        </section>
      )}
      <button ref={botao} type="button" onClick={() => aberto ? fechar() : setAberto(true)}
        aria-label={aberto ? "Fechar chat da Lia" : "Conversar com Lia"} aria-expanded={aberto} aria-controls={aberto ? "chat-lia" : undefined}
        className="flex items-center gap-2 rounded-full bg-[#650014] py-1 pl-1 pr-4 text-sm font-semibold text-white shadow-lg ring-1 ring-white/70 hover:bg-primary">
        <Image src={avatar} alt="" width={46} height={46} className="rounded-full" />
        Lia
      </button>
    </aside>
  );
}
