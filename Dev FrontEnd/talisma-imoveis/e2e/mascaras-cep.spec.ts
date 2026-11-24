import { expect, test } from "@playwright/test";

test.skip(Boolean(process.env.API_URL || process.env.E2E_BASE_URL), "Teste executado com a API simulada.");

test("formata fixo e celular com DDD e envia só os dígitos", async ({ page, context, baseURL }) => {
  await context.addCookies([{ name: "talisma_acesso", value: "e2e-corretor", url: baseURL! }]);
  await page.goto("/painel?aba=proprietarios");
  const form = page.locator("form").filter({ has: page.getByRole("button", { name: "Cadastrar proprietário", exact: true }) });
  await expect(page.locator("details").filter({ hasText: "Proprietário fictício" }).getByLabel("Telefone")).toHaveValue("(11) 3233-4455");
  await form.getByLabel("Nome", { exact: true }).fill("Proprietário de teste");
  const telefone = form.getByLabel("Telefone", { exact: true });
  await telefone.fill("1234");
  expect(await telefone.evaluate(el => (el as HTMLInputElement).checkValidity())).toBe(false);
  await telefone.fill("+55 (11) 91234-5678");
  await expect(telefone).toHaveValue("(11) 91234-5678");
  await form.getByRole("button", { name: "Cadastrar proprietário", exact: true }).click();
  await expect(form.getByText("Alteração salva.")).toBeVisible();
});

test("preenche o endereço pelo CEP e mantém os campos editáveis", async ({ page, context, baseURL }) => {
  await context.addCookies([{ name: "talisma_acesso", value: "e2e-corretor", url: baseURL! }]);
  await page.route("**/api/cep/01001000", async route => {
    await route.fulfill({ json: { logradouro: "Praça da Sé", bairro: "Sé", cidade: "São Paulo", uf: "SP" } });
  });
  await page.route("**/api/cep/99999999", route => route.fulfill({ status: 404 }));
  let liberarConsulta!: () => void;
  const aguardarEdicao = new Promise<void>(resolver => { liberarConsulta = resolver; });
  await page.route("**/api/cep/01002000", async route => {
    await aguardarEdicao;
    await route.fulfill({ json: { logradouro: "", bairro: "", cidade: "São Paulo", uf: "SP" } });
  });
  await page.goto("/painel?aba=novo-imovel");
  const cep = page.getByLabel("CEP", { exact: true });
  await cep.fill("99999999");
  await expect(page.getByText(/CEP não encontrado/)).toBeVisible();
  await cep.fill("01001000");
  await expect(cep).toHaveValue("01001-000");
  await expect(page.getByLabel("Logradouro", { exact: true })).toHaveValue("Praça da Sé");
  await expect(page.getByLabel("Bairro", { exact: true })).toHaveValue("Sé");
  await expect(page.getByLabel("Cidade", { exact: true })).toHaveValue("São Paulo");
  await expect(page.getByLabel("UF", { exact: true })).toHaveValue("SP");
  await expect(page.getByLabel("Número", { exact: true })).toHaveValue("");
  const novaConsulta = page.waitForRequest("**/api/cep/01002000");
  await cep.fill("01002000");
  await novaConsulta;
  await expect(page.getByRole("button", { name: "Cadastrar imóvel", exact: true })).toBeDisabled();
  const logradouro = page.getByLabel("Logradouro", { exact: true });
  await logradouro.fill("Entrada lateral");
  liberarConsulta();
  await expect(page.getByLabel("Bairro", { exact: true })).toHaveValue("");
  await expect(logradouro).toHaveValue("Entrada lateral");
  await page.getByLabel("Bairro", { exact: true }).fill("Sé");
  await page.getByLabel("Proprietário", { exact: true }).selectOption("1");
  await page.getByLabel("Tipo de imóvel", { exact: true }).selectOption("CASA");
  await page.getByLabel("Título do anúncio").fill("Casa de teste");
  await page.getByLabel("Descrição").fill("Imóvel fictício.");
  await page.getByLabel("Número", { exact: true }).fill("S/N");
  await page.getByLabel("Área total (m²)", { exact: true }).fill("75.50");
  await page.getByLabel("Valor de venda (R$)", { exact: true }).fill("250000");
  await page.getByRole("button", { name: "Cadastrar imóvel", exact: true }).click();
  await expect(page.getByText("Alteração salva.")).toBeVisible();
});
