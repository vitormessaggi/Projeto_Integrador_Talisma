package br.com.talisma.imoveis.dados;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import br.com.talisma.imoveis.BuildConfig;

/**
 * TODAS as rotas provisórias da API (6.9, D25) — único ponto a editar quando o back-end publicar
 * as rotas definitivas. A base vem de {@code BuildConfig.API_URL} (local.properties → {@code api.url}).
 */
public final class ApiRotas {

    public static final String BASE = BuildConfig.API_URL;

    // Imóveis
    public static final String DESTAQUES = "/api/imoveis/destaques";
    public static final String OPCOES_FILTRO = "/api/imoveis/filtros";

    // Autenticação
    public static final String CADASTRO = "/api/auth/cadastro";
    public static final String VERIFICAR_EMAIL = "/api/auth/verificar-email";
    public static final String REENVIAR_CODIGO = "/api/auth/reenviar-codigo";
    public static final String LOGIN = "/api/auth/login";
    public static final String ESQUECI_SENHA = "/api/auth/esqueci-senha";
    public static final String REDEFINIR_SENHA = "/api/auth/redefinir-senha";

    // Usuário logado
    public static final String PERFIL = "/api/usuarios/me";
    public static final String ALTERAR_SENHA = "/api/usuarios/me/senha";
    public static final String FAVORITOS = "/api/usuarios/me/favoritos";

    // Visitas
    public static final String VISITAS = "/api/visitas";
    public static final String MINHAS_VISITAS = "/api/visitas/minhas";

    // Corretor
    public static final String IMOVEIS_CORRETOR = "/api/corretor/imoveis";

    private ApiRotas() {
    }

    /** {@code GET /api/imoveis?finalidade&tipo&cidade&precoMin&precoMax&quartosMin&page&size=10} */
    public static String imoveis(Filtro filtro, int pagina) {
        StringBuilder sb = new StringBuilder("/api/imoveis?page=").append(pagina)
                .append("&size=").append(Pagina.TAMANHO_PADRAO);
        if (filtro != null) {
            parametro(sb, "finalidade", filtro.finalidade);
            parametro(sb, "tipo", filtro.tipo);
            parametro(sb, "cidade", filtro.cidade);
            parametro(sb, "precoMin", filtro.precoMin != null ? numero(filtro.precoMin) : null);
            parametro(sb, "precoMax", filtro.precoMax != null ? numero(filtro.precoMax) : null);
            parametro(sb, "quartosMin", filtro.quartosMin != null ? String.valueOf(filtro.quartosMin) : null);
        }
        return sb.toString();
    }

    /** {@code GET /api/imoveis/{id}} */
    public static String imovel(long id) {
        return "/api/imoveis/" + id;
    }

    /** {@code PUT|DELETE /api/usuarios/me/favoritos/{imovelId}} */
    public static String favorito(long imovelId) {
        return FAVORITOS + "/" + imovelId;
    }

    /** {@code PATCH /api/visitas/{id}/cancelar} */
    public static String cancelarVisita(long id) {
        return VISITAS + "/" + id + "/cancelar";
    }

    /** {@code POST /api/imoveis/{id}/fotos} (multipart, campo {@code arquivo}) */
    public static String fotosImovel(long imovelId) {
        return "/api/imoveis/" + imovelId + "/fotos";
    }

    private static void parametro(StringBuilder sb, String nome, String valor) {
        if (valor == null || valor.isEmpty()) {
            return;
        }
        try {
            sb.append('&').append(nome).append('=').append(URLEncoder.encode(valor, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Valor monetário sem notação científica e sem ".0" desnecessário. */
    private static String numero(double valor) {
        return valor == Math.rint(valor) ? String.valueOf((long) valor) : String.valueOf(valor);
    }
}
