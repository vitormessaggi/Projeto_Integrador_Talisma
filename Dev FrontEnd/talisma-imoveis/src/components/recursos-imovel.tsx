"use client";
import { useConsentimento } from "./consentimento";
import Image from "next/image";
import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { MapPin, Share2 } from "lucide-react";

export function Compartilhar({titulo}:{titulo:string}) {
  const [mensagem,setMensagem]=useState("");
  async function compartilhar(){try {if(navigator.share)await navigator.share({title:titulo,url:location.href});else {await navigator.clipboard.writeText(location.href);setMensagem("Link copiado!");}}catch(e){if(!(e instanceof DOMException && e.name==="AbortError"))setMensagem("Não foi possível compartilhar. Copie o endereço da página.");}}
  return <div><button className="action-secondary" onClick={()=>void compartilhar()}><Share2 size={18}/>Compartilhar</button><span role="status" className="ml-2 text-xs">{mensagem}</span></div>;
}
export function Galeria({fotos,titulo}:{fotos:string[];titulo:string}) {
  const [indice,setIndice]=useState(0);
  if(!fotos.length)return <div className="empty-state">Fotos em breve</div>;
  return <div><a href={fotos[indice]} target="_blank" rel="noreferrer" className="relative block aspect-[4/3] overflow-hidden rounded-3xl bg-muted"><Image src={fotos[indice]} alt={`${titulo} — foto ${indice+1}`} fill unoptimized sizes="(max-width: 767px) 95vw, 60vw" className="object-cover"/></a>
    <div className="mt-3 flex gap-3 overflow-x-auto pb-2" aria-label="Miniaturas">{fotos.map((foto,i)=><button key={foto} aria-label={`Ver foto ${i+1}`} aria-pressed={indice===i} onClick={()=>setIndice(i)} className={`relative h-20 w-24 shrink-0 overflow-hidden rounded-xl border-2 ${indice===i?"border-primary":"border-transparent"}`}><Image src={foto} alt="" fill unoptimized sizes="96px" className="object-cover"/></button>)}</div></div>;
}
export function PertoDeMim(){
  const router=useRouter(), params=useSearchParams();const [mensagem,setMensagem]=useState("");const [aguardando,setAguardando]=useState(false);const ativo=params.get("ordenacao")==="distancia";
  function buscar(){const p=new URLSearchParams(params);p.delete("page");if(ativo){["latitude","longitude","ordenacao"].forEach(k=>p.delete(k));router.push("/imoveis?"+p);return;}if(!navigator.geolocation){setMensagem("Localização indisponível neste navegador.");return;}setAguardando(true);navigator.geolocation.getCurrentPosition(pos=>{p.set("latitude",String(pos.coords.latitude));p.set("longitude",String(pos.coords.longitude));p.set("ordenacao","distancia");router.push("/imoveis?"+p);setAguardando(false);setMensagem("");},()=>{setAguardando(false);setMensagem("Não foi possível acessar sua localização. Você pode buscar pela cidade.");},{timeout:10000,maximumAge:60000});}
  return <div><button type="button" className={`filter-chip ${ativo?"active":""}`} onClick={buscar} disabled={aguardando} aria-pressed={ativo}><MapPin size={17}/>{aguardando?"Localizando…":"Perto de mim"}</button>{mensagem&&<p role="status" className="mt-2 text-sm text-primary">{mensagem}</p>}</div>;
}
export function MapaImovel({latitude,longitude}:{latitude?:number;longitude?:number}){
  const { opcionais, abrir } = useConsentimento();
  if(latitude==null||longitude==null)return <p className="text-sm text-muted-foreground">A localização no mapa ainda não foi informada.</p>;
  const bbox=[longitude-.008,latitude-.006,longitude+.008,latitude+.006].join(",");
  return <div className="overflow-hidden rounded-2xl border">{opcionais ? <iframe loading="lazy" title="Localização do imóvel" className="h-72 w-full border-0" referrerPolicy="no-referrer" src={`https://www.openstreetmap.org/export/embed.html?bbox=${encodeURIComponent(bbox)}&layer=mapnik&marker=${latitude}%2C${longitude}`}/> : <div className="p-5"><p>O mapa externo só carrega com sua autorização.</p><button className="action-secondary mt-3" onClick={abrir}>Escolher preferências</button></div>}<a className="block bg-white p-3 text-xs text-muted-foreground" target="_blank" rel="noreferrer" href={`https://www.openstreetmap.org/?mlat=${latitude}&mlon=${longitude}#map=16/${latitude}/${longitude}`}>© OpenStreetMap contributors · Abrir mapa</a></div>;
}
