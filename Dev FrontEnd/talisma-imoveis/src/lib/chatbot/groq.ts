import "server-only";
import { StringOutputParser } from "@langchain/core/output_parsers";
import { ChatPromptTemplate } from "@langchain/core/prompts";
import { ChatGroq } from "@langchain/groq";

const prompt = ChatPromptTemplate.fromMessages([["human", "{mensagem}"]]);

export async function gerarResposta(mensagem: string, signal: AbortSignal) {
  const apiKey = process.env.GROQ_API_KEY?.trim();
  if (!apiKey) return null;

  const modelo = new ChatGroq({
    apiKey,
    model: process.env.GROQ_MODEL?.trim() || "openai/gpt-oss-120b",
    temperature: 0.2,
    maxTokens: 1024,
    maxRetries: 0,
  });

  const fluxo = prompt.pipe(modelo).pipe(new StringOutputParser());
  const resposta = (await fluxo.invoke({ mensagem }, { signal })).trim();
  if (!resposta) throw new Error("A Groq retornou uma resposta vazia.");

  return resposta;
}
