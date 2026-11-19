"""Atualiza cópias offline; --check falha se textos ou versões divergirem."""
from pathlib import Path
import argparse
import json
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "Dev BackEnd/imoveisapi/src/main/resources/legal"

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    textos = {nome: (SOURCE / f"{nome}.txt").read_text(encoding="utf-8") for nome in ("termos", "privacidade")}
    versoes = {re.search(r"Versão: (\S+)", texto).group(1) for texto in textos.values()}
    if len(versoes) != 1:
        raise SystemExit("As versões dos documentos devem ser iguais nesta implementação.")
    versao = versoes.pop()
    java = (ROOT / "Dev BackEnd/imoveisapi/src/main/java/br/com/talisma/api/imoveisapi/identidade/DocumentosLegais.java").read_text(encoding="utf-8")
    mobile = (ROOT / "Dev Mobile/talisma-app/app/src/main/java/br/com/talisma/imoveis/dados/ContaRepository.java").read_text(encoding="utf-8")
    tela = (ROOT / "Dev Mobile/talisma-app/app/src/main/java/br/com/talisma/imoveis/PrivacidadeActivity.java").read_text(encoding="utf-8")
    if f'VERSAO = "{versao}"' not in java or f'"versaoTermos", "{versao}"' not in mobile or f'"versaoTermos","{versao}"' not in tela:
        raise SystemExit("Atualize também as versões explícitas no backend e no Android.")
    ts = '/* Gerado dos documentos versionados do backend por scripts/sincronizar-legais.py. */\n'
    ts += f'export const versaoLegal = "{versao}";\n'
    ts += "".join(f"export const {nome} = {json.dumps(texto, ensure_ascii=False)};\n" for nome, texto in textos.items())
    arquivos = {ROOT / "Dev FrontEnd/talisma-imoveis/src/lib/legal.ts": ts}
    arquivos.update({ROOT / f"Dev Mobile/talisma-app/app/src/main/assets/legal/{nome}.txt": texto for nome, texto in textos.items()})
    for caminho, texto in arquivos.items():
        if args.check:
            if not caminho.exists() or caminho.read_text(encoding="utf-8") != texto:
                raise SystemExit(f"Documento desatualizado: {caminho.relative_to(ROOT)}")
        else:
            caminho.parent.mkdir(parents=True, exist_ok=True)
            caminho.write_text(texto, encoding="utf-8", newline="\n")
    print("Documentos legais sincronizados.")

if __name__ == "__main__":
    main()
