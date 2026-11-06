# Recursos reaproveitados para o app mobile

Materiais de design e conteúdo vindos dos projetos Talismã de semestres anteriores, já convertidos para o formato do Android.
Quando o projeto `Dev Mobile/talisma-app` for criado, cada pasta é copiada para o destino indicado.

| Pasta | Conteúdo | Destino no app | Origem |
|---|---|---|---|
| `fontes/` | Poppins Regular, Medium, SemiBold e Bold (+ licença OFL) | `app/src/main/res/font/` | Site 1º sem. (`assets/fonts/Poppins`) |
| `drawable/logo_talisma_vermelho.xml` | Logo completo (pino + "Talismã Imóveis"), fundo claro | `res/drawable/` | `Logo-Red.svg` (site 1º sem. = `Dev FrontEnd`) |
| `drawable/logo_talisma_branco.xml` | Logo completo para fundo borgonha | `res/drawable/` | `Logo-White.svg` |
| `drawable/ic_marcador_casa.xml` | Pino com casa (símbolo dos protótipos de Login/Cadastro) | `res/drawable/` | `marker.svg` |
| `drawable/ic_telefone.xml`, `ic_email.xml`, `ic_horario.xml` | Ícones de contato (Material Symbols) | `res/drawable/` | site 1º sem. |
| `imagens/imoveis/` | 60 fotos (15 imóveis × 4), JPEG até 1280 px, ~100 KB cada | `app/src/main/assets/fake/fotos/` | App Expo 3º sem. (imóveis 01–06) e site 1º sem. (07–15) |
| `dados/imoveis_exemplo.json` | 15 imóveis no formato do modelo `Imovel` (seção 6.8 do plano): título, finalidade, tipo, preço, bairro, cidade, UF, endereço, quartos, banheiros, vagas, área, descrição, lat/long, destaque, corretorId e fotos | `app/src/main/assets/fake/imoveis.json` | idem |
| `dados/usuarios.json` | Contas de teste do modo simulado (D27): cliente, corretor e conta bloqueada | `app/src/main/assets/fake/usuarios.json` | criado para o plano |
| `textos/termos_e_condicoes.md` | Termos e Condições adaptados para o app | `strings.xml` da `TermosActivity` | `termos.html` |
| `textos/empresa.md` | Endereço, contato, horário, texto "Sobre" | `strings.xml` | `contato.html`, `Sobre.tsx` |

## Observações

- **Cores:** o app Expo usava `#C70039`; o app segue a especificação v2.0 e os protótipos atuais (borgonha `#8D001A`, variações `#6B0014`, `#6C0015`, `#AF001F`, `#49000E`, `#FF234E`).
- **Logos:** convertidos de SVG para Vector Drawable mantendo os paths originais; conferir no preview do Android Studio ao copiar.
- **Fotos:** são imagens ilustrativas usadas nos trabalhos anteriores, servem só para os dados simulados e para a demonstração. Trocar por fotos reais da imobiliária antes de qualquer uso público.
- **Dados simulados:** preços, áreas e descrições vêm dos projetos anteriores. As coordenadas são **aproximadas** (centro do bairro/cidade) e servem só para testar mapa e "Perto de mim"; o imóvel 14 está sem coordenadas de propósito, para testar o caso sem lat/long. Área (imóveis 01–06) e vagas (07–15) não existiam na origem e foram completados com valores plausíveis; endereço é só bairro/cidade/UF; todos os imóveis pertencem ao corretor de teste (id 2); destaques: 1, 2, 5, 6, 9 e 12.
- **Não trazidos:** vídeo institucional (sem uso nos protótipos), banner `image-banner.svg` (13 MB, sem uso no mobile), `familia.png`, script SQL do banco de 1º semestre (o modelo de dados é do back-end).
- Script de conversão usado: imagens redimensionadas com Pillow; SVG → Vector Drawable copiando `pathData` e cores.
