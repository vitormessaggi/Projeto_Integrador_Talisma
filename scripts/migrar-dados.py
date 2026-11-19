"""Backup/restauração conferidos. Requer mysql/mysqldump; envio S3 requer AWS CLI.
Não altera o banco de origem. Senhas são solicitadas sem eco, nunca passadas na linha de comando.
"""
import argparse
import getpass
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

def executar(comando, senha=None, **kwargs):
    env = os.environ.copy()
    if senha is not None:
        env["MYSQL_PWD"] = senha
    return subprocess.run(comando, env=env, check=True, **kwargs)

def hash_arquivo(path):
    with path.open("rb") as entrada:
        return hashlib.file_digest(entrada, "sha256").hexdigest()

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("acao", choices=["exportar", "restaurar", "s3"])
    p.add_argument("--pasta", type=Path, required=True)
    p.add_argument("--host", default="127.0.0.1")
    p.add_argument("--port", type=int, default=3306)
    p.add_argument("--usuario")
    p.add_argument("--banco", default="talisma")
    p.add_argument("--fotos", type=Path)
    p.add_argument("--ca", type=Path, help="CA do RDS; obrigatório para restaurar")
    p.add_argument("--bucket")
    p.add_argument("--mysql", default="mysql")
    p.add_argument("--mysqldump", default="mysqldump")
    a = p.parse_args()
    if not re.fullmatch(r"[a-zA-Z0-9_]+", a.banco): p.error("Nome de banco inválido")
    pasta = a.pasta.resolve()
    manifest = pasta / "manifesto.json"
    if a.acao == "s3":
        if not a.bucket or not re.fullmatch(r"[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]", a.bucket): p.error("Informe --bucket válido")
        dados = json.loads(manifest.read_text(encoding="utf-8"))
        for foto in dados["fotos"]:
            local = pasta / "fotos" / foto["arquivo"]
            if hash_arquivo(local) != foto["sha256"]: raise SystemExit("Backup de foto inconsistente")
            chave = foto["arquivo"]
            uri = f"s3://{a.bucket}/{chave}"
            # Repetição segura: não sobrescreve um objeto com conteúdo diferente.
            consulta = subprocess.run(["aws", "s3api", "head-object", "--bucket", a.bucket, "--key", chave, "--region", "us-east-1"], capture_output=True, text=True)
            if consulta.returncode == 0:
                remoto = json.loads(consulta.stdout)
                if remoto.get("Metadata", {}).get("sha256") != foto["sha256"]: raise SystemExit("Objeto já existe sem hash correspondente: " + chave)
            else:
                if "404" not in consulta.stderr and "Not Found" not in consulta.stderr:
                    raise SystemExit("Não foi possível verificar objeto S3. Verifique permissões e sessão do laboratório.")
                executar(["aws", "s3", "cp", str(local), uri, "--region", "us-east-1", "--metadata", "sha256=" + foto["sha256"], "--only-show-errors"])
            # Baixa novamente e compara bytes; não confia em ETag de multipart.
            temporario = pasta / "verificacao-s3.tmp"
            try:
                executar(["aws", "s3", "cp", uri, str(temporario), "--region", "us-east-1", "--only-show-errors"])
                if hash_arquivo(temporario) != foto["sha256"]: raise SystemExit("Conteúdo S3 diverge do backup")
            finally:
                temporario.unlink(missing_ok=True)
        print("Fotos copiadas e verificadas no S3.")
        return
    if not a.usuario: p.error("Informe --usuario")
    if a.acao == "restaurar" and not a.ca: p.error("Restauração exige --ca e validação TLS do RDS")
    senha = getpass.getpass("Senha MySQL (não será gravada): ")
    opcoes = ["--protocol=TCP", "--host=" + a.host, "--port=" + str(a.port), "--user=" + a.usuario, "--default-character-set=utf8mb4"]
    if a.ca: opcoes += ["--ssl-mode=VERIFY_IDENTITY", "--ssl-ca=" + str(a.ca.resolve())]
    def sql(consulta):
        return executar([a.mysql, *opcoes, "--batch", "--raw", "--skip-column-names", a.banco, "-e", consulta], senha, capture_output=True, text=True).stdout.strip()
    def contagens(tabelas):
        if any(not re.fullmatch(r"[a-zA-Z0-9_]+", t) for t in tabelas):
            raise SystemExit("Nome de tabela incompatível no banco ou manifesto")
        return {t: int(sql(f"SELECT COUNT(*) FROM `{t}`")) for t in tabelas}
    if a.acao == "exportar":
        if not a.fotos: p.error("Informe --fotos")
        if pasta.exists(): raise SystemExit("Use uma pasta nova para não sobrescrever backups")
        tabelas = sql("SHOW TABLES").splitlines()
        if "flyway_schema_history" not in tabelas: raise SystemExit("Histórico Flyway ausente; reconciliar esquema antes da migração")
        nomes = sql("SELECT arquivo FROM foto ORDER BY id").splitlines()
        fotos = []
        for nome in nomes:
            if not re.fullmatch(r"[0-9a-f-]{36}\.(jpeg|png)", nome): raise SystemExit("Nome de foto incompatível: " + nome)
            origem = a.fotos.resolve() / nome
            if not origem.is_file() or origem.is_symlink(): raise SystemExit("Foto ausente ou link: " + nome)
            fotos.append({"arquivo": nome, "sha256": hash_arquivo(origem)})
        antes = contagens(tabelas)
        (pasta / "fotos").mkdir(parents=True)
        dump = pasta / "banco.sql"
        executar([a.mysqldump, *opcoes, "--single-transaction", "--routines", "--triggers", "--no-tablespaces", "--set-gtid-purged=OFF", "--skip-add-drop-table", "--result-file=" + str(dump), a.banco], senha)
        for foto in fotos:
            shutil.copy2(a.fotos.resolve() / foto["arquivo"], pasta / "fotos" / foto["arquivo"])
            if hash_arquivo(pasta / "fotos" / foto["arquivo"]) != foto["sha256"]: raise SystemExit("Foto mudou durante backup")
        if contagens(tabelas) != antes: raise SystemExit("O banco mudou durante a cópia. Interrompa gravações e repita em pasta nova.")
        dados = {"banco": a.banco, "contagens": antes, "fotos": fotos, "dumpSha256": hash_arquivo(dump)}
        manifest.write_text(json.dumps(dados, ensure_ascii=False, indent=2), encoding="utf-8")
        print("Backup concluído. Preserve a pasta e restrinja acesso a ela.")
    else:
        if sql("SHOW TABLES"): raise SystemExit("O banco de destino deve estar vazio; não haverá sobrescrita")
        dados = json.loads(manifest.read_text(encoding="utf-8"))
        dump = pasta / "banco.sql"
        if hash_arquivo(dump) != dados["dumpSha256"]: raise SystemExit("Dump inconsistente")
        with dump.open("rb") as entrada: executar([a.mysql, *opcoes, a.banco], senha, stdin=entrada)
        atual = contagens(dados["contagens"])
        if atual != dados["contagens"]: raise SystemExit("Contagens divergentes: não efetuar corte")
        print("Restauração conferida. Agora aplique somente as migrations novas pela API.")

if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError:
        raise SystemExit("Comando falhou. Confira conexão, permissões e TLS. Não publique credenciais ou dumps nos logs.")
