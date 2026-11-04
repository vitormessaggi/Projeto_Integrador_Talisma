import type { Schemas } from "@/lib/api";
import { Bloco, Mutacao } from "./painel-comum";
import type { Campo } from "./formulario";
import { Vazio } from "./vitrine";

export function EditorImovel({ proprietarios, existente, destino }: { proprietarios: Schemas["Proprietario"][];
  existente?: Schemas["DetalheGestao"]; destino: string }) {
  if (!proprietarios.length) return <Vazio>Cadastre um proprietário antes de cadastrar o imóvel.</Vazio>;
  const dados = existente?.dados;
  const id = existente?.imovel?.id;
  const numericos = ["proprietarioId", "quartos", "banheiros", "vagas", "areaTotal", "areaConstruida", "latitude", "longitude", "valorVenda", "valorLocacao"];
  const campos: Campo[] = [
    { nome: "proprietarioId", rotulo: "Proprietário", inicial: dados?.proprietarioId,
      opcoes: proprietarios.map(p => ({ valor: String(p.id), texto: p.nome ?? "" })) },
    { nome: "tipo", rotulo: "Tipo de imóvel", inicial: dados?.tipo,
      opcoes: [{ valor: "CASA", texto: "Casa" }, { valor: "APARTAMENTO", texto: "Apartamento" }, { valor: "TERRENO", texto: "Terreno" }] },
    { nome: "titulo", rotulo: "Título do anúncio", inicial: dados?.titulo },
    { nome: "descricao", rotulo: "Descrição", tipo: "textarea", inicial: dados?.descricao },
    { nome: "cep", rotulo: "CEP (somente números)", inicial: dados?.cep },
    { nome: "logradouro", rotulo: "Logradouro", inicial: dados?.logradouro },
    { nome: "numero", rotulo: "Número", inicial: dados?.numero },
    { nome: "bairro", rotulo: "Bairro", inicial: dados?.bairro },
    { nome: "cidade", rotulo: "Cidade", inicial: dados?.cidade },
    { nome: "uf", rotulo: "UF", inicial: dados?.uf,
      opcoes: "AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO".split(" ").map(valor => ({ valor, texto: valor })) },
    ...([["quartos", "Quartos"], ["banheiros", "Banheiros"], ["vagas", "Vagas"]] as const).map(([nome, rotulo]) => ({ nome, rotulo, tipo: "number", inicial: dados?.[nome] ?? 0, min: 0, step: "1" })),
    { nome: "areaTotal", rotulo: "Área total (m²)", inicial: dados?.areaTotal, tipo: "number", min: .01, step: "0.01" },
    { nome: "areaConstruida", rotulo: "Área construída (m²)", inicial: dados?.areaConstruida, tipo: "number", min: 0, step: "0.01", obrigatorio: false },
    { nome: "latitude", rotulo: "Latitude", inicial: dados?.latitude, tipo: "number", step: "any", obrigatorio: false },
    { nome: "longitude", rotulo: "Longitude", inicial: dados?.longitude, tipo: "number", step: "any", obrigatorio: false },
    { nome: "valorVenda", rotulo: "Valor de venda (R$)", inicial: dados?.ofertas?.find(o => o.finalidade === "VENDA")?.preco, tipo: "number", min: .01, step: "0.01", obrigatorio: false },
    { nome: "valorLocacao", rotulo: "Valor mensal de locação (R$)", inicial: dados?.ofertas?.find(o => o.finalidade === "LOCACAO")?.preco, tipo: "number", min: .01, step: "0.01", obrigatorio: false },
    { nome: "mobiliado", rotulo: "Mobiliado", inicial: dados?.mobiliado, tipo: "checkbox" },
    { nome: "destaque", rotulo: "Exibir nos destaques após aprovação", inicial: dados?.destaque, tipo: "checkbox" },
  ];
  return <Bloco titulo={id ? "Editar imóvel" : "Cadastro de imóvel"}>
    <p className="mb-4 text-sm text-muted-foreground">Informe o preço de venda, de locação ou ambos.</p>
    <Mutacao operacao={{ caminho: id ? `/gestao/imoveis/${id}` : "/gestao/imoveis", metodo: id ? "PUT" : "POST", retorno: destino,
      campos: campos.filter(campo => campo.tipo !== "checkbox").map(campo => campo.nome), numeros: numericos, imovel: true }}
      campos={campos} botao={id ? "Salvar alterações" : "Cadastrar imóvel"} colunas />
  </Bloco>;
}
