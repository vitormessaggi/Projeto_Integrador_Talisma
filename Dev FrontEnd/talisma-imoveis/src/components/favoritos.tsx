"use client";
import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import { Heart } from "lucide-react";
import { estadoFavoritos, alterarFavorito } from "@/lib/favoritos";
import type { Imovel } from "@/lib/api";

type Local = {itens: Imovel[]; pendentes: Record<string, boolean>};
type Contexto = {itens:Imovel[]; pronto:boolean; ocupado:boolean; erro:string; interno:boolean; alternar:(imovel:Imovel)=>Promise<void>};
const Context = createContext<Contexto>({itens:[],pronto:false,ocupado:false,erro:"",interno:false,alternar:async()=>{}});
function ler(chave:string):Local { try {const v=JSON.parse(localStorage.getItem(chave) ?? "null"); return v && Array.isArray(v.itens) && v.pendentes ? v : {itens:[],pendentes:{}};} catch {return {itens:[],pendentes:{}};} }
function gravar(chave:string, dados:Local) {localStorage.setItem(chave,JSON.stringify(dados));}
export function FavoritosProvider({children}:{children:React.ReactNode}) {
  const [itens,setItens]=useState<Imovel[]>([]), [pronto,setPronto]=useState(false), [ocupado,setOcupado]=useState(false), [erro,setErro]=useState(""), [interno,setInterno]=useState(false);
  const sessao=useRef<{chave:string;usuario:number|null}>({chave:"",usuario:null});
  const trava=useRef(false); const pathname=usePathname();
  const sincronizar=useCallback(async()=>{
    if(trava.current)return; trava.current=true; setOcupado(true);
    try {
      const remoto=await estadoFavoritos();
      if(remoto.erro)throw Error(remoto.erro);
      const visitante="talisma:favoritos:"+remoto.ambiente+":visitante";
      const chave=remoto.usuario ? "talisma:favoritos:"+remoto.ambiente+":"+remoto.usuario : visitante;
      sessao.current={chave,usuario:remoto.usuario}; setInterno(!!remoto.interno);
      let local=ler(chave);
      if(remoto.usuario) {
        const anonimo=ler(visitante);
        for(const item of anonimo.itens) {if(!(String(item.id) in local.pendentes))local.pendentes[item.id]=true;if(!local.itens.some(i=>i.id===item.id))local.itens.push(item);}
        gravar(chave,local); gravar(visitante,{itens:[],pendentes:{}});
        for(const [id,salvo] of Object.entries(local.pendentes)) {
          const resposta=await alterarFavorito(Number(id),salvo,remoto.usuario);
          if(resposta.erro)throw Error(resposta.erro);
          delete local.pendentes[id]; gravar(chave,local);
        }
        const atualizado=await estadoFavoritos();
        if(atualizado.erro || atualizado.usuario!==remoto.usuario)throw Error(atualizado.erro ?? "A conta mudou. Atualize a página.");
        local={itens:atualizado.itens,pendentes:{}};gravar(chave,local);
      }
      setItens(local.itens);setErro("");setPronto(true);
    }catch(e){setErro(e instanceof Error ? e.message:"Favoritos indisponíveis.");}
    finally{trava.current=false;setOcupado(false);}
  },[]);
  useEffect(()=>{queueMicrotask(()=>void sincronizar()); const atualizar=()=>{void sincronizar();}; window.addEventListener("focus",atualizar);window.addEventListener("online",atualizar);window.addEventListener("storage",atualizar);return()=>{window.removeEventListener("focus",atualizar);window.removeEventListener("online",atualizar);window.removeEventListener("storage",atualizar);};},[sincronizar,pathname]);
  async function alternar(imovel:Imovel) {
    if(trava.current || !pronto || interno)return;
    const {chave,usuario}=sessao.current; const local=ler(chave);const salvo=!local.itens.some(i=>i.id===imovel.id);
    local.itens=salvo?[...local.itens,imovel]:local.itens.filter(i=>i.id!==imovel.id);
    if(usuario)local.pendentes[imovel.id]=salvo;
    try {gravar(chave,local);setItens(local.itens);setErro("");if(usuario)await sincronizar();}
    catch {setErro("O navegador não permitiu salvar seus favoritos.");}
  }
  return <Context.Provider value={{itens,pronto,ocupado,erro,interno,alternar}}>{children}</Context.Provider>;
}
export function useFavoritos(){return useContext(Context);}
export function BotaoFavorito({imovel}:{imovel:Imovel}) {
  const f=useFavoritos(); const salvo=f.itens.some(i=>i.id===imovel.id);
  if(f.interno)return null;
  return <button type="button" disabled={!f.pronto||f.ocupado} onClick={()=>void f.alternar(imovel)} aria-label={salvo?"Remover dos favoritos":"Salvar em favoritos"} aria-pressed={salvo} title={f.erro || undefined} className="favorite-button"><Heart size={21} fill={salvo?"currentColor":"none"} aria-hidden /></button>;
}
