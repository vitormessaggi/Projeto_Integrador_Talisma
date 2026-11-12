import { cookies } from "next/headers";
import { acessoCookie, apiUrl } from "@/lib/api";

export async function GET(_request: Request, { params }: { params: Promise<{ id:string; foto:string }> }) {
  const {id,foto}=await params;
  if(!/^\d+$/.test(id)||!/^\d+$/.test(foto))return new Response(null,{status:404});
  const token=(await cookies()).get(acessoCookie)?.value;
  if(!token)return new Response(null,{status:401});
  const resposta=await fetch(`${apiUrl}/api/v1/gestao/imoveis/${id}/fotos/${foto}`,{headers:{Authorization:`Bearer ${token}`},cache:"no-store",signal:AbortSignal.timeout(10000)}).catch(()=>null);
  if(!resposta?.ok)return new Response(null,{status:resposta?.status??503});
  return new Response(resposta.body,{headers:{"Content-Type":resposta.headers.get("content-type")??"image/jpeg","Cache-Control":"private, no-store","X-Content-Type-Options":"nosniff"}});
}
