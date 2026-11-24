export type CampoFormatado = "telefone" | "cep" | "codigo";

export const formatos = {
  telefone: { limite: 11, pattern: String.raw`\([1-9][0-9]\) [0-9]{4,5}-[0-9]{4}`, instrucao: "Informe o telefone com DDD, como (11) 91234-5678." },
  cep: { limite: 8, pattern: "[0-9]{5}-[0-9]{3}", instrucao: "Informe os oito dígitos do CEP." },
  codigo: { limite: 6, pattern: "[0-9]{6}", instrucao: "Informe os seis dígitos do código." },
} satisfies Record<CampoFormatado, { limite: number; pattern: string; instrucao: string }>;

export function digitosDoCampo(nome: CampoFormatado, valor: string) {
  const digitos = valor.replace(/\D/g, "");
  if (nome === "telefone" && digitos.startsWith("55") && (digitos.length === 12 || digitos.length === 13)) {
    return digitos.slice(2);
  }
  return digitos;
}

export function formatarCampo(nome: CampoFormatado, valor: string) {
  const digitos = digitosDoCampo(nome, valor).slice(0, formatos[nome].limite);
  if (nome === "codigo") return digitos;
  if (nome === "cep") return digitos.replace(/^([0-9]{5})([0-9])/, "$1-$2");
  if (!digitos) return "";
  if (digitos.length <= 2) return "(" + digitos;
  const ddd = digitos.slice(0, 2);
  const numero = digitos.slice(2);
  const corte = numero.length > 8 ? 5 : 4;
  return `(${ddd}) ${numero.slice(0, corte)}${numero.length > corte ? "-" + numero.slice(corte) : ""}`;
}

export function normalizarCampo(nome: CampoFormatado, valor: string) {
  const entrada = valor.trim();
  const digitos = digitosDoCampo(nome, entrada);
  const formato = formatos[nome];
  if (new RegExp(`^(?:${formato.pattern})$`).test(entrada)) return digitos;
  if (nome === "telefone" && /^(?:\+55[ ()-]*)?[0-9() -]+$/.test(entrada) && /^[1-9][0-9]{9,10}$/.test(digitos)) return digitos;
  if (nome === "cep" && /^[0-9]{8}$/.test(entrada)) return entrada;
  return null;
}
