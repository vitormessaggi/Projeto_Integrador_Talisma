import { expect, test, type APIRequestContext } from "@playwright/test";
import { randomInt, randomUUID } from "node:crypto";

function cpfFicticio() {
  const digitos = String(randomInt(100_000_000, 999_999_999)).split("").map(Number);
  for (const tamanho of [9, 10]) {
    const soma = digitos.reduce((total, digito, indice) => total + digito * (tamanho + 1 - indice), 0);
    const verificador = 11 - soma % 11;
    digitos.push(verificador >= 10 ? 0 : verificador);
  }
  return digitos.join("");
}

async function codigo(request: APIRequestContext, email: string, assunto: string) {
  let recebido: string | undefined;
  await expect.poll(async () => {
    const mailpit = process.env.E2E_MAILPIT_URL;
    const lista = await request.get(`${mailpit}/api/v1/search?query=${encodeURIComponent("to:" + email)}`);
    if (!lista.ok()) return false;
    const mensagens = ((await lista.json()).messages ?? []) as { ID: string; Subject: string }[];
    const mensagem = mensagens.find(item => item.Subject === assunto);
    if (!mensagem) return false;
    const corpo = await request.get(`${mailpit}/api/v1/message/${mensagem.ID}`);
    recebido = (await corpo.json()).Text?.match(/\b\d{6}\b/)?.[0];
    return Boolean(recebido);
  }, { timeout: 12000 }).toBe(true);
  return recebido!;
}

test("verifica cadastro e recuperação de senha pelo Mailpit", async ({ page, request }) => {
  test.skip(!process.env.E2E_MAILPIT_URL, "Configure E2E_MAILPIT_URL para executar com uma caixa de testes local.");
  const email = `teste-${randomUUID()}@example.test`;
  await page.goto("/cadastro");
  await page.getByLabel("Nome completo").fill("Cliente fictício");
  await page.getByRole("textbox", { name: "E-mail", exact: true }).fill(email);
  await page.getByLabel("CPF", { exact: true }).fill(cpfFicticio());
  await page.getByLabel("RG", { exact: true }).fill("RG-TESTE");
  await page.getByLabel("Órgão emissor").fill("SSP");
  await page.getByLabel("UF do RG").selectOption("SP");
  await page.getByLabel("Senha (mínimo de 8 caracteres)", { exact: true }).fill("SenhaFicticia123");
  await page.getByLabel("Confirmar senha", { exact: true }).fill("SenhaFicticia123");
  await page.getByRole("button", { name: "Cadastrar", exact: true }).click();
  await expect(page).toHaveURL(/\/verificar-email/);
  await page.getByRole("textbox", { name: "E-mail", exact: true }).fill(email);
  await page.getByLabel("Código de seis dígitos").fill(await codigo(request, email, "Verifique seu e-mail"));
  await page.getByRole("button", { name: "Confirmar código" }).click();
  await expect(page).toHaveURL(/\/entrar/);
  await page.getByRole("link", { name: "Esqueci a senha" }).click();
  await page.getByRole("textbox", { name: "E-mail", exact: true }).fill(email);
  await page.getByRole("button", { name: "Enviar código" }).click();
  await expect(page).toHaveURL(/\/redefinir-senha/);
  await page.getByRole("textbox", { name: "E-mail", exact: true }).fill(email);
  await page.getByLabel("Código de seis dígitos").fill(await codigo(request, email, "Recupere sua senha"));
  await page.getByLabel("Nova senha").fill("NovaSenhaFicticia123");
  await page.getByRole("button", { name: "Alterar senha" }).click();
  await expect(page).toHaveURL(/\/entrar/);
  await page.getByRole("textbox", { name: "E-mail", exact: true }).fill(email);
  await page.getByLabel("Senha", { exact: true }).fill("NovaSenhaFicticia123");
  await page.getByRole("button", { name: "Entrar", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Olá, Cliente fictício" })).toBeVisible();
  const cookies = await page.context().cookies();
  expect(cookies.find(item => item.name === "talisma_acesso")?.httpOnly).toBe(true);
  await page.getByRole("button", { name: "Sair da conta" }).click();
  await expect(page).toHaveURL(/\/$/);
});
