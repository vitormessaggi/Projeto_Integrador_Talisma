"use client";
import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { privacidade } from "@/lib/privacidade";

export function ControlesPrivacidade({ aceiteAtual }: { aceiteAtual: boolean }) {
  const [senha, setSenha] = useState(""), [mensagem, setMensagem] = useState(""), [ocupado, setOcupado] = useState(false), [aceite, setAceite] = useState(false), [confirmar, setConfirmar] = useState(false);
  const router = useRouter();
  async function executar(acao: "ACESSO" | "EXCLUSAO" | "EXPORTAR" | "ACEITAR") {
    setOcupado(true); setMensagem("");
    try {
      const r = await privacidade(acao, senha);
      setSenha("");
      if (r.erro) { setMensagem(r.erro); return; }
      if (r.dados) {
        const url = URL.createObjectURL(new Blob([r.dados], { type: "application/json" }));
        const a = document.createElement("a"); a.href = url; a.download = "dados-talisma.json"; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
      }
      if (r.excluida) {
        try { for (const chave of Object.keys(localStorage)) if (chave.startsWith("talisma:favoritos:")) localStorage.removeItem(chave); } catch {}
        window.alert(`Conta excluída. Guarde seu protocolo: ${r.protocolo}`); window.location.replace("/"); return;
      }
      setMensagem(r.protocolo ? `Solicitação registrada. Protocolo: ${r.protocolo}` : r.sucesso ?? "Cópia disponibilizada."); router.refresh();
    } finally { setOcupado(false); }
  }
  return <div className="grid max-w-2xl gap-5">
    {!aceiteAtual && <div className="rounded-xl border p-5"><p>Leia os <Link href="/termos" className="underline">Termos de Uso</Link> e a <Link href="/privacidade" className="underline">Política de Privacidade</Link> atualizados.</p><label className="my-3 flex gap-3"><input type="checkbox" checked={aceite} onChange={e => setAceite(e.target.checked)}/>Aceito os termos e confirmo a leitura da política.</label><button className="action-secondary" disabled={!aceite || ocupado} onClick={() => void executar("ACEITAR")}>Registrar aceite</button></div>}
    <p>Solicite acesso aos seus dados ou baixe uma cópia. Para confirmar sua identidade, informe sua senha atual. Não são exportados dados de terceiros, senhas ou tokens.</p>
    <label className="grid gap-2">Senha atual<input className="rounded border p-3" type="password" autoComplete="current-password" value={senha} onChange={e => setSenha(e.target.value)}/></label>
    <div className="flex flex-wrap gap-3"><button className="action-secondary" disabled={ocupado || !senha} onClick={() => void executar("ACESSO")}>Solicitar acesso</button><button className="action-secondary" disabled={ocupado || !senha} onClick={() => void executar("EXPORTAR")}>Baixar meus dados</button></div>
    <div className="rounded-xl border border-red-300 p-5"><h2 className="font-semibold">Exclusão dos dados</h2><p className="my-3 text-sm">No laboratório, a confirmação exclui a conta de cliente, seus documentos legados, favoritos, contatos e visitas e encerra as sessões. Contas internas seguem para análise administrativa. Backups expiram em até sete dias.</p><label className="flex gap-3"><input type="checkbox" checked={confirmar} onChange={e => setConfirmar(e.target.checked)}/>Confirmo que desejo solicitar a exclusão.</label><button className="action-secondary mt-4" disabled={ocupado || !senha || !confirmar} onClick={() => void executar("EXCLUSAO")}>Confirmar exclusão dos meus dados</button></div>
    {mensagem && <p role="status">{mensagem}</p>}
  </div>;
}
