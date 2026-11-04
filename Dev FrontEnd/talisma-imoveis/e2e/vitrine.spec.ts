import { expect, test } from "@playwright/test";

for (const viewport of [{ width: 320, height: 800 }, { width: 768, height: 1024 }, { width: 1600, height: 1000 }]) {
  test(`busca funciona a ${viewport.width}px com navegação por teclado`, async ({ page }, teste) => {
    await page.setViewportSize(viewport);
    await page.goto("/");
    await expect(page.getByRole("heading", { name: "O imóvel que você procura está aqui!" })).toBeVisible();
    await page.keyboard.press("Tab");
    await expect(page.getByRole("link", { name: "Pular para o conteúdo" })).toBeFocused();
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(viewport.width);
    await page.screenshot({ path: teste.outputPath("home.png"), fullPage: true });
    await page.getByText("Alugar", { exact: true }).click();
    await page.getByRole("button", { name: "Buscar" }).click();
    await expect(page).toHaveURL(/finalidade=LOCACAO/);
    await expect(page.getByRole("heading", { name: "Imóveis para locação" })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(viewport.width);
  });
}

test("protege a área de conta e mostra credenciais inválidas", async ({ page }) => {
  await page.goto("/conta");
  await expect(page).toHaveURL(/\/entrar/);
  await page.getByLabel("E-mail").fill("conta-inexistente@example.test");
  await page.getByLabel("Senha", { exact: true }).fill("SenhaDeTeste123");
  await page.getByRole("button", { name: "Entrar", exact: true }).click();
  await expect(page.getByRole("alert").filter({ hasText: /credenciais inválidas/i })).toBeVisible();
});
