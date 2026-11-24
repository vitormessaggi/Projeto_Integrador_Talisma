"use client";

import { useActionState, useEffect, useId, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { campo, rotulo } from "./vitrine";
import type { Estado } from "@/lib/acoes";
import { digitosDoCampo, formatarCampo, formatos, type CampoFormatado } from "@/lib/campos-formatados";

export type Campo = { nome: string; rotulo: string; tipo?: string; inicial?: string | number | boolean | null;
  obrigatorio?: boolean; opcoes?: { valor: string; texto: string }[]; min?: number | string; max?: number | string;
  step?: string; autocomplete?: string };

const limites: Record<string, number> = {
  nome: 160, email: 254, titulo: 180, descricao: 10000, logradouro: 200,
  bairro: 120, cidade: 120, numero: 20, mensagem: 2000,
};
const camposEndereco = ["logradouro", "bairro", "cidade", "uf"] as const;

function mascaraDe(item: Campo): CampoFormatado | undefined {
  if (item.tipo === "tel") return "telefone";
  if (item.nome === "cep" || item.nome === "codigo") return item.nome;
}

function Entrada({ item, id, valor, alterar }: { item: Campo; id: string; valor: string | boolean; alterar: (valor: string | boolean) => void }) {
  if (item.tipo === "checkbox") return <input id={id} name={item.nome} type="checkbox" required={item.obrigatorio === true} checked={Boolean(valor)} onChange={e => alterar(e.target.checked)} />;
  const comum = { id, name: item.nome, required: item.obrigatorio ?? true, value: String(valor), onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => alterar(e.target.value) };
  if (item.opcoes) return <select className={campo} {...comum}><option value="">Selecione</option>{item.opcoes.map(opcao => <option value={opcao.valor} key={opcao.valor}>{opcao.texto}</option>)}</select>;
  if (item.tipo === "textarea") return <textarea className={campo} {...comum} rows={4} maxLength={limites[item.nome]} />;
  if (item.tipo === "file") return <input className={campo} id={id} name={item.nome} type="file" accept="image/jpeg,image/png" multiple required />;
  const mascara = mascaraDe(item);
  if (mascara) {
    const formato = formatos[mascara];
    return <Input className="min-h-11 bg-white" {...comum} type={item.tipo ?? "text"} inputMode={mascara === "telefone" ? "tel" : "numeric"}
      autoComplete={item.autocomplete ?? (mascara === "cep" ? "postal-code" : mascara === "telefone" ? "tel" : "one-time-code")}
      pattern={formato.pattern} title={formato.instrucao}
      placeholder={mascara === "telefone" ? "(11) 91234-5678" : mascara === "cep" ? "00000-000" : undefined}
      value={formatarCampo(mascara, String(valor))}
      onInvalid={e => {
        if (e.currentTarget.validity.patternMismatch) e.currentTarget.setCustomValidity(formato.instrucao);
      }}
      onChange={e => {
        const input = e.currentTarget;
        input.setCustomValidity("");
        const formatado = formatarCampo(mascara, input.value);
        const fim = input.selectionStart === input.value.length;
        const antesDoCursor = input.value.slice(0, input.selectionStart ?? 0).replace(/\D/g, "").length;
        alterar(formatado);
        if (!fim && formatado !== input.value) {
          let posicao = 0;
          while (posicao < formatado.length && formatado.slice(0, posicao).replace(/\D/g, "").length < antesDoCursor) posicao++;
          requestAnimationFrame(() => {
            if (document.activeElement === input && input.value === formatado) input.setSelectionRange(posicao, posicao);
          });
        }
      }} />;
  }
  return <Input className="min-h-11 bg-white" {...comum} type={item.tipo ?? "text"} autoComplete={item.autocomplete}
    min={item.min} max={item.max} step={item.step} maxLength={item.tipo === "password" ? 72 : limites[item.nome]}
    minLength={item.autocomplete === "new-password" ? 8 : undefined} />;
}

export function Formulario({ campos, acao, botao, colunas = false, preencherEndereco = false }: { campos: Campo[];
  acao: (estado: Estado, form: FormData) => Promise<Estado>; botao: string; colunas?: boolean; preencherEndereco?: boolean }) {
  const id = useId();
  const [estado, enviar, pendente] = useActionState(acao, {});
  const [valores, setValores] = useState<Record<string, string | boolean>>({});
  const [retornoCep, setRetornoCep] = useState<{ cep: string; mensagem: string } | null>(null);
  const revisoes = useRef<Record<string, number>>({});
  const cep = preencherEndereco ? digitosDoCampo("cep", String(valores.cep ?? "")) : "";

  useEffect(() => {
    if (!/^[0-9]{8}$/.test(cep)) return;
    const consulta = new AbortController();
    const revisaoInicial = { ...revisoes.current };
    const timer = window.setTimeout(async () => {
      let mensagem: string;
      try {
        const resposta = await fetch(`/api/cep/${cep}`, { signal: consulta.signal });
        if (resposta.status === 404) mensagem = "CEP não encontrado. Confira o CEP ou preencha o endereço manualmente.";
        else if (resposta.status === 401) mensagem = "Sessão expirada. Entre novamente para consultar o CEP.";
        else if (!resposta.ok) mensagem = "Consulta ao CEP indisponível. Preencha o endereço manualmente.";
        else {
          const endereco = await resposta.json() as Record<typeof camposEndereco[number], string>;
          if (consulta.signal.aborted) return;
          const preenchidos = camposEndereco.filter(nome => revisoes.current[nome] === revisaoInicial[nome]);
          setValores(atuais => {
            if (digitosDoCampo("cep", String(atuais.cep ?? "")) !== cep) return atuais;
            return { ...atuais, ...Object.fromEntries(preenchidos.map(nome => [nome, endereco[nome]])) };
          });
          mensagem = "Confira o endereço e complete os campos em branco.";
        }
      } catch {
        mensagem = "Consulta ao CEP indisponível. Preencha o endereço manualmente.";
      }
      if (!consulta.signal.aborted) setRetornoCep({ cep, mensagem });
    }, 250);
    return () => {
      window.clearTimeout(timer);
      consulta.abort();
    };
  }, [cep]);

  const consultandoCep = cep.length === 8 && retornoCep?.cep !== cep;
  const mensagemCep = consultandoCep ? "Consultando CEP…" : retornoCep?.mensagem;
  return (
    <form action={enviar} className="grid gap-4">
      <div className={colunas ? "grid gap-4 sm:grid-cols-2" : "grid gap-4"}>
        {campos.map(item => <div key={item.nome} className={item.tipo === "checkbox" ? "flex items-center gap-3" : rotulo}>
          <label htmlFor={id + item.nome}>{item.rotulo}</label>
          <Entrada item={item} id={id + item.nome} valor={valores[item.nome] ?? (item.tipo === "checkbox" ? Boolean(item.inicial) : String(item.inicial ?? ""))}
            alterar={valor => {
              if (preencherEndereco) {
                revisoes.current[item.nome] = (revisoes.current[item.nome] ?? 0) + 1;
                if (item.nome === "cep" && valor !== valores.cep) setRetornoCep(null);
              }
              setValores(anterior => ({ ...anterior, [item.nome]: valor }));
            }} />
          {preencherEndereco && item.nome === "cep" && mensagemCep &&
            <p className="text-sm font-normal text-muted-foreground" role="status">{mensagemCep}</p>}
        </div>)}
      </div>
      {estado.erro && <p className="rounded-md bg-accent p-3 text-sm text-destructive" role="alert">{estado.erro}</p>}
      {estado.sucesso && <p className="text-sm text-emerald-800" role="status">{estado.sucesso}</p>}
      <Button className="min-h-11 justify-self-start" disabled={pendente || consultandoCep}>{pendente ? "Enviando…" : botao}</Button>
    </form>
  );
}
