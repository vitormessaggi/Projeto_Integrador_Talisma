import { expect, test } from "@playwright/test";

test("recusa persiste, permite rever escolha e documentos estão acessíveis", async ({ page }) => {
  const externas: string[] = [];
  page.on("request", r => { if (/openstreetmap|google-analytics|googletagmanager/.test(r.url())) externas.push(r.url()); });
  await page.goto("/privacidade");
  await expect(page.getByRole("heading", { name: "Política de privacidade", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Recusar opcionais", exact: true }).click();
  await page.reload();
  await expect(page.getByRole("button", { name: "Recusar opcionais", exact: true })).toHaveCount(0);
  expect(externas).toEqual([]);
  await page.getByRole("button", { name: "Preferências de cookies", exact: true }).click();
  await page.getByRole("button", { name: "Aceitar opcionais", exact: true }).click();
  expect(await page.evaluate(() => JSON.parse(localStorage.getItem("talisma:consentimento")!).opcionais)).toBe(true);
  await page.getByRole("button", { name: "Preferências de cookies", exact: true }).click();
  await page.getByRole("button", { name: "Recusar opcionais", exact: true }).click();
  expect(await page.evaluate(() => JSON.parse(localStorage.getItem("talisma:consentimento")!).opcionais)).toBe(false);
});

test("cadastro minimizado exige aceite desmarcado", async ({ page }) => {
  await page.goto("/cadastro");
  await page.getByRole("button", { name: "Recusar opcionais", exact: true }).click();
  await expect(page.getByLabel("CPF", { exact: true })).toHaveCount(0);
  await expect(page.getByLabel("RG", { exact: true })).toHaveCount(0);
  const aceite = page.getByRole("checkbox", { name: /Aceito os Termos/ });
  await expect(aceite).not.toBeChecked();
  await expect(aceite).toHaveAttribute("required", "");
  await expect(page.getByRole("link", { name: "Leia os Termos de Uso" })).toHaveAttribute("href", "/termos");
});

test("mapa só faz requisição após autorização e é removido na revogação", async ({ page }, teste) => {
  test.skip(Boolean(process.env.API_URL) && !process.env.E2E_IMOVEL_ID, "Na API real informe E2E_IMOVEL_ID publicado e com coordenadas.");
  const requests: string[] = [];
  await page.route("https://www.openstreetmap.org/**", async route => { requests.push(route.request().url()); await route.fulfill({ body: "<html>Mapa simulado pelo teste</html>", contentType: "text/html" }); });
  await page.goto(`/imoveis/${process.env.E2E_IMOVEL_ID ?? "1"}`);
  await expect(page.getByTitle("Localização do imóvel", { exact: true })).toHaveCount(0);
  expect(requests).toHaveLength(0);
  await page.getByRole("button", { name: "Recusar opcionais", exact: true }).click();
  await page.reload();
  await expect(page.getByTitle("Localização do imóvel", { exact: true })).toHaveCount(0);
  expect(requests).toHaveLength(0);
  await page.getByRole("button", { name: "Escolher preferências", exact: true }).click();
  await page.getByRole("button", { name: "Aceitar opcionais", exact: true }).click();
  await expect(page.getByTitle("Localização do imóvel", { exact: true })).toBeVisible();
  await expect.poll(() => requests.length).toBe(1);
  await page.getByRole("button", { name: "Preferências de cookies", exact: true }).click();
  await page.getByRole("button", { name: "Recusar opcionais", exact: true }).click();
  await expect(page.getByTitle("Localização do imóvel", { exact: true })).toHaveCount(0);
  await page.screenshot({ path: teste.outputPath("privacidade-mapa.png"), fullPage: true });
});
