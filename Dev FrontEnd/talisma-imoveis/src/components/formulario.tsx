"use client";

import { useActionState, useId, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { campo, rotulo } from "./vitrine";
import type { Estado } from "@/lib/acoes";

export type Campo = { nome: string; rotulo: string; tipo?: string; inicial?: string | number | boolean | null;
  obrigatorio?: boolean; opcoes?: { valor: string; texto: string }[]; min?: number | string; step?: string; autocomplete?: string };

function Entrada({ item, id, valor, alterar }: { item: Campo; id: string; valor: string | boolean; alterar: (valor: string | boolean) => void }) {
  if (item.tipo === "checkbox") return <input id={id} name={item.nome} type="checkbox" checked={Boolean(valor)} onChange={e => alterar(e.target.checked)} />;
  const comum = { id, name: item.nome, required: item.obrigatorio ?? true, value: String(valor), onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => alterar(e.target.value) };
  if (item.opcoes) return <select className={campo} {...comum}><option value="">Selecione</option>{item.opcoes.map(opcao => <option value={opcao.valor} key={opcao.valor}>{opcao.texto}</option>)}</select>;
  if (item.tipo === "textarea") return <textarea className={campo} {...comum} rows={4} />;
  if (item.tipo === "file") return <input className={campo} id={id} name={item.nome} type="file" accept="image/jpeg,image/png" multiple required />;
  return <Input className="min-h-11 bg-white" {...comum} type={item.tipo ?? "text"} autoComplete={item.autocomplete} min={item.min} step={item.step} />;
}

export function Formulario({ campos, acao, botao, colunas = false }: { campos: Campo[];
  acao: (estado: Estado, form: FormData) => Promise<Estado>; botao: string; colunas?: boolean }) {
  const id = useId();
  const [estado, enviar, pendente] = useActionState(acao, {});
  const [valores, setValores] = useState<Record<string, string | boolean>>({});
  return (
    <form action={enviar} className="grid gap-4">
      <div className={colunas ? "grid gap-4 sm:grid-cols-2" : "grid gap-4"}>
        {campos.map(item => <div key={item.nome} className={item.tipo === "checkbox" ? "flex items-center gap-3" : rotulo}>
          <label htmlFor={id + item.nome}>{item.rotulo}</label>
          <Entrada item={item} id={id + item.nome} valor={valores[item.nome] ?? (item.tipo === "checkbox" ? Boolean(item.inicial) : String(item.inicial ?? ""))}
            alterar={valor => setValores(anterior => ({ ...anterior, [item.nome]: valor }))} />
        </div>)}
      </div>
      {estado.erro && <p className="rounded-md bg-accent p-3 text-sm text-destructive" role="alert">{estado.erro}</p>}
      {estado.sucesso && <p className="text-sm text-emerald-800" role="status">{estado.sucesso}</p>}
      <Button className="min-h-11 justify-self-start" disabled={pendente}>{pendente ? "Enviando…" : botao}</Button>
    </form>
  );
}
