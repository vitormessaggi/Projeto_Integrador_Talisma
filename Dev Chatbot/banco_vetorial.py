"""Indexa a base institucional da Talismã no Chroma."""
import hashlib
import os
from functools import lru_cache
from pathlib import Path

from dotenv import load_dotenv
from langchain_chroma import Chroma
from langchain_community.document_loaders import PyPDFLoader
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

RAIZ = Path(__file__).resolve().parent
BASE = RAIZ / "Talisma_Base_Conhecimento.pdf"
load_dotenv(os.getenv("TALISMA_CHAT_ENV", str(RAIZ / ".env")))
MODELO = os.getenv("EMBEDDING_MODEL", "sentence-transformers/all-mpnet-base-v2")
DIRETORIO = os.getenv("CHROMA_DIR", str(RAIZ / "banco_chroma"))


@lru_cache(maxsize=1)
def abrir_banco() -> Chroma:
    revisao = hashlib.sha256(BASE.read_bytes() + MODELO.encode("utf-8")).hexdigest()[:16]
    embeddings = HuggingFaceEmbeddings(
        model_name=MODELO,
        model_kwargs={"device": "cpu"},
        encode_kwargs={"normalize_embeddings": True},
    )
    return Chroma(
        collection_name=f"talisma-{revisao}",
        persist_directory=DIRETORIO,
        embedding_function=embeddings,
    )


def indexar() -> None:
    paginas = PyPDFLoader(str(BASE)).load()
    trechos = RecursiveCharacterTextSplitter(chunk_size=520, chunk_overlap=70).split_documents(paginas)
    if not trechos:
        raise RuntimeError("A base de conhecimento não contém texto.")
    banco = abrir_banco()
    banco.add_documents(trechos, ids=[f"trecho-{i}" for i in range(len(trechos))])
    print(f"Banco Chroma pronto: {len(paginas)} páginas, {len(trechos)} trechos.")


if __name__ == "__main__":
    indexar()
