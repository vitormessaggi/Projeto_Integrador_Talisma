import { expect, test } from "@playwright/test";

test("titular solicita acesso, baixa sua cópia e exclui a conta", async ({ page, request, context }) => {
  test.skip(!process.env.E2E_CONTA_EMAIL || !process.env.E2E_CONTA_SENHA || !process.env.API_URL, "Requer conta fictícia descartável e verificada na API real de testes. O teste exclui essa conta.");
  const email = process.env.E2E_CONTA_EMAIL!;
  const senha = process.env.E2E_CONTA_SENHA!;
  const api = process.env.API_URL!;
  const login = await request.post(`${api}/api/v1/auth/login`, { data: { email, senha } });
  expect(login.ok()).toBe(true);
  const tokens = await login.json();
  const origem = test.info().project.use.baseURL!;
  await context.addCookies([
    { name: "talisma_acesso", value: tokens.accessToken, url: origem, httpOnly: true, sameSite: "Lax" },
    { name: "talisma_renovacao", value: tokens.refreshToken, url: origem, httpOnly: true, sameSite: "Lax" },
  ]);
  await page.goto("/conta/privacidade");
  await page.getByRole("button", { name: "Recusar opcionais", exact: true }).click();
  await page.getByLabel("Senha atual", { exact: true }).filter({ visible: true }).fill(senha);
  await page.getByRole("button", { name: "Solicitar acesso", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("Protocolo:");
  await page.getByLabel("Senha atual", { exact: true }).filter({ visible: true }).fill(senha);
  const arquivo = page.waitForEvent("download");
  await page.getByRole("button", { name: "Baixar meus dados", exact: true }).click();
  const download = await arquivo;
  expect(download.suggestedFilename()).toBe("dados-talisma.json");
  const stream = await download.createReadStream();
  let texto = ""; for await (const parte of stream!) texto += parte.toString();
  const copia = JSON.parse(texto);
  expect(copia.perfil.email).toBe(email);
  expect(texto).not.toContain("senha_hash");
  await page.getByLabel("Senha atual", { exact: true }).filter({ visible: true }).fill(senha);
  await page.getByRole("checkbox", { name: "Confirmo que desejo solicitar a exclusão." }).check();
  page.on("dialog", dialog => void dialog.accept());
  await page.getByRole("button", { name: "Confirmar exclusão dos meus dados", exact: true }).click();
  await expect(page).toHaveURL(/\/$/);
  expect((await context.cookies()).filter(c => c.name === "talisma_acesso" || c.name === "talisma_renovacao")).toEqual([]);
  const revogada = await request.get(`${api}/api/v1/usuarios/me`, { headers: { Authorization: `Bearer ${tokens.accessToken}` } });
  expect(revogada.status()).toBe(401);
});
