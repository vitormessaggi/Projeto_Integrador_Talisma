# Guia de adaptação do mobile

Atualize a base para `/api/v1`. O contrato oficial é [OpenAPI](openapi.json). Os exemplos abaixo usam caminhos relativos à base.

| Operação anterior | Contrato novo |
| --- | --- |
| `GET /api/imoveis` | `GET /imoveis?page=0&size=10&finalidade=LOCACAO` |
| `GET /api/imoveis/destaques` | `GET /imoveis?destaque=true` |
| `GET /api/imoveis/{id}` e `/filtros` | Mesmos recursos na nova base. O detalhe separa resumo e descrição |
| `POST /api/auth/cadastro` | `POST /auth/cadastro` com nome, e-mail, senha e aceite versionado obrigatório |
| Login, código, recuperação | `/auth/login`, `/auth/verificar-email`, `/auth/reenviar-codigo`, `/auth/esqueci-senha`, `/auth/redefinir-senha` |
| Perfil, senha e favoritos | `/usuarios/me`, `/usuarios/me/senha`, `/usuarios/me/favoritos` |
| Solicitação de visita | `POST /visitas` ou `POST /leads` para contato sem visita |
| Minhas visitas | `GET /visitas` com paginação |
| Cancelar visita | `PATCH /visitas/{id}/status` com `{"status":"CANCELADA"}` |
| Lista de imóveis do corretor | `GET /gestao/imoveis` |
| Foto do corretor | `POST /gestao/imoveis/{id}/fotos` com campo multipart repetido `arquivos` |

Autenticação usa `Authorization: Bearer <accessToken>`. Login devolve `accessToken`, `refreshToken`, `expiresIn` e `usuario`. A verificação devolve HTTP 204 e é seguida do login. Códigos têm seis dígitos. Em 401, use `POST /auth/renovar` com `{"refreshToken":"..."}`, armazene o par novo e repita a chamada uma vez. Envie o refresh atual a `POST /auth/logout` ao sair.

As páginas usam `itens`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`. Os imóveis contêm ofertas separadas:

```json
{
  "id": 42,
  "titulo": "Casa de demonstração",
  "ofertas": [
    {"id": 101, "finalidade": "VENDA", "preco": 320000.00},
    {"id": 102, "finalidade": "LOCACAO", "preco": 1800.00}
  ],
  "fotos": ["/api/v1/fotos/7"]
}
```

Para enviar visita: `{"imovelId":42,"ofertaId":102,"data":"2027-02-09","periodo":"TARDE","telefone":"11900000000"}`. Não exigir CPF/RG para solicitar visitas. Documentos legados são acessíveis somente no fluxo autenticado de exportação. Erros usam HTTP e Problem Details. Exiba uma mensagem compatível com o status caso não haja `detail`.

## Privacidade (2026-11-18)

`GET /legal` é público e devolve termos e privacidade com versão, hash e texto. Cadastro deve incluir `aceiteTermos: true`, `versaoTermos: "2026-11-18"`, `versaoPrivacidade: "2026-11-18"` e `origem: "ANDROID"`. O checkbox nasce desmarcado e os textos devem ser acessíveis antes do envio. Versão antiga retorna 409; nunca simular um aceite de conta existente.

- `GET /usuarios/me/privacidade`: aceite atual e solicitações próprias.
- `POST /usuarios/me/aceites`: confirma aceite atual com as mesmas versões/origem e aceiteTermos.
- `POST /usuarios/me/privacidade/solicitacoes`: `{ "tipo": "ACESSO" | "EXCLUSAO", "senha": "senha atual" }`. Cliente confirmado é excluído do ambiente ativo; conta interna permanece pendente de análise.
- `POST /usuarios/me/privacidade/exportacao`: `{ "senha": "senha atual" }`, devolve JSON próprio com `Cache-Control: no-store`. Não cachear essa resposta nem imprimir conteúdo em log.
- Após exclusão concluída, limpar sessão, favoritos e filas locais; não reenviar dados da conta excluída. Outros aparelhos offline requerem limpeza quando forem usados novamente.

A URL remota deve usar HTTPS confiável. Compile com `-PapiUrl=https://IP_PUBLICO` e JDK 21 para executar o Gradle. Java da API é 25. Consulte o runbook AWS na raiz para IP, certificado e Mailpit privado.
