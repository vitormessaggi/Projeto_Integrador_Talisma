package br.com.talisma.api.imoveisapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.talisma.api.imoveisapi.catalogo.CatalogoService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("bancoDeTesteConfigurado")
@Transactional
class ImoveisapiApplicationTests {
    static boolean bancoDeTesteConfigurado() {
        String url = System.getenv("TEST_DB_URL");
        return Boolean.getBoolean("talisma.testcontainers")
                || (url != null && url.matches("jdbc:mysql://[^/]+/talisma_test(?:\\?.*)?"));
    }

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registry) {
        registry.add("APP_JWT_KEY", () -> java.util.Base64.getEncoder().encodeToString(new byte[32]));
        if (Boolean.getBoolean("talisma.testcontainers")) {
            registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
            registry.add("spring.datasource.url", () -> "jdbc:tc:mysql:8.4:///talisma_test?connectionTimeZone=UTC");
            registry.add("spring.datasource.username", () -> "teste");
            registry.add("spring.datasource.password", () -> "senha-descartavel");
            return;
        }
        registry.add("spring.datasource.url", () -> System.getenv("TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("TEST_DB_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("TEST_DB_PASSWORD"));
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired CatalogoService catalogo;

    @Test
    void filtraPrecoDaMesmaOfertaEProtegeRascunhos() {
        jdbc.update("""
                INSERT INTO usuario(nome,email,senha_hash,perfil)
                VALUES ('Corretor de teste','corretor-catalogo@example.test','fixture','CORRETOR')
                """);
        long corretor = jdbc.queryForObject("SELECT id FROM usuario WHERE email='corretor-catalogo@example.test'", Long.class);
        jdbc.update("INSERT INTO proprietario(corretor_id,nome,telefone) VALUES (?, 'Proprietário fictício', '11900000000')", corretor);
        long proprietario = jdbc.queryForObject("SELECT MAX(id) FROM proprietario WHERE corretor_id=?", Long.class, corretor);
        long publicado = criarImovel(proprietario, corretor, "PUBLICADO");
        long rascunho = criarImovel(proprietario, corretor, "RASCUNHO");

        assertThat(catalogo.buscar("Cidade Teste", null, null, null, "VENDA",
                null, new BigDecimal("3000"), 0, 10).itens()).isEmpty();
        var pagina = catalogo.buscar("Cidade Teste", null, 2, null, "LOCACAO",
                new BigDecimal("1000"), new BigDecimal("3000"), 0, 10);
        assertThat(pagina.itens()).extracting(CatalogoService.ImovelDto::id).containsExactly(publicado);
        assertThat(catalogo.detalhar(publicado).imovel().ofertas()).hasSize(2);
        assertThatThrownBy(() -> catalogo.detalhar(rascunho)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void ordenaPorDistanciaAntesDePaginarERespeitaOfertas() {
        jdbc.update("INSERT INTO usuario(nome,email,senha_hash,perfil) VALUES ('Corretor','perto@example.test','fixture','CORRETOR')");
        long corretor = jdbc.queryForObject("SELECT id FROM usuario WHERE email='perto@example.test'", Long.class);
        jdbc.update("INSERT INTO proprietario(corretor_id,nome,telefone) VALUES (?, 'Dono', '11900000000')", corretor);
        long dono = jdbc.queryForObject("SELECT MAX(id) FROM proprietario WHERE corretor_id=?", Long.class, corretor);
        long longe = criarImovel(dono, corretor, "PUBLICADO");
        jdbc.update("UPDATE imovel SET latitude=-24,longitude=-47 WHERE id=?", longe);
        for (int i = 0; i < 11; i++) {
            long id = criarImovel(dono, corretor, "PUBLICADO");
            jdbc.update("UPDATE imovel SET latitude=-23.55,longitude=-46.63 WHERE id=?", id);
        }
        criarImovel(dono, corretor, "PUBLICADO"); // no coordinates: excluded only from proximity
        long rascunho = criarImovel(dono, corretor, "RASCUNHO");
        jdbc.update("UPDATE imovel SET latitude=-23.55,longitude=-46.63 WHERE id=?", rascunho);
        var primeira = catalogo.buscar("Cidade Teste", null, null, null, "LOCACAO", null, new BigDecimal("3000"), 0, 10, -23.55, -46.63, "distancia");
        var segunda = catalogo.buscar("Cidade Teste", null, null, null, "LOCACAO", null, new BigDecimal("3000"), 1, 10, -23.55, -46.63, "distancia");
        assertThat(primeira.totalElementos()).isEqualTo(12);
        assertThat(primeira.totalPaginas()).isEqualTo(2);
        assertThat(primeira.itens()).hasSize(10);
        assertThat(primeira.itens()).extracting(CatalogoService.ImovelDto::id).isSorted().doesNotContain(longe, rascunho);
        assertThat(segunda.itens()).hasSize(2);
        assertThat(segunda.itens().getLast().id()).isEqualTo(longe);
        assertThat(segunda.itens().getLast().distanciaKm()).isGreaterThan(0);
        assertThat(catalogo.buscar("Cidade Teste", null, null, null, "VENDA", null, new BigDecimal("3000"), 0, 10, -23.55, -46.63, "distancia").itens()).isEmpty();
        assertThatThrownBy(() -> catalogo.buscar(null,null,null,null,null,null,null,0,10,91.0,0.0,"distancia")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> catalogo.buscar(null,null,null,null,null,null,null,0,10,-23.55,null,"distancia")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> catalogo.buscar(null,null,null,null,null,null,null,0,10,null,null,"aleatoria")).isInstanceOf(ResponseStatusException.class);
    }

    private long criarImovel(long proprietario, long corretor, String status) {
        jdbc.update("""
                INSERT INTO imovel(proprietario_id,corretor_id,titulo,descricao,tipo,status,cidade,bairro,uf,
                    cep,logradouro,numero,quartos,area_total)
                VALUES (?,?,'Casa fictícia','Descrição','CASA',?,'Cidade Teste','Bairro Teste','SP',
                    '01001000','Rua Fictícia','1',2,80)
                """, proprietario, corretor, status);
        long imovel = jdbc.queryForObject("SELECT MAX(id) FROM imovel WHERE corretor_id=?", Long.class, corretor);
        jdbc.update("INSERT INTO oferta(imovel_id,finalidade,preco) VALUES (?, 'VENDA', 250000), (?, 'LOCACAO', 2100)", imovel, imovel);
        return imovel;
    }

    @Autowired org.springframework.web.context.WebApplicationContext web;
    @Autowired org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder senhas;
    @Autowired tools.jackson.databind.ObjectMapper json;

    @Test
    void executaFluxoDeVendaLocacaoVisitaEIsolamento() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web)
                .addFilters(web.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
        var post = org.springframework.http.HttpMethod.POST;
        var get = org.springframework.http.HttpMethod.GET;
        var patch = org.springframework.http.HttpMethod.PATCH;
        var put = org.springframework.http.HttpMethod.PUT;

        chamar(mvc, post, "/auth/cadastro", null, """
                {"nome":"Cliente QA","email":"cliente-qa@example.test","senha":"ApenasTeste123",
                "aceiteTermos":true,"versaoTermos":"2026-11-25","versaoPrivacidade":"2026-11-25","origem":"WEB"}
                """, 201);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM codigo_verificacao c JOIN usuario u ON u.id=c.usuario_id
                WHERE u.email='cliente-qa@example.test' AND c.finalidade='CADASTRO'
                """, Integer.class)).isEqualTo(1);
        jdbc.update("""
                UPDATE codigo_verificacao c JOIN usuario u ON u.id=c.usuario_id
                SET c.codigo_hash=? WHERE u.email='cliente-qa@example.test'
                """, senhas.encode("123456"));
        chamar(mvc, post, "/auth/verificar-email", null, """
                {"email":"cliente-qa@example.test","codigo":"123456"}
                """, 204);
        var cliente = login(mvc, "cliente-qa@example.test");
        String tokenCliente = cliente.path("accessToken").asString();
        String tokenAdmin = conta(mvc, "admin-qa@example.test", "ADMIN");
        String tokenCorretor = conta(mvc, "corretor-qa@example.test", "CORRETOR");
        String tokenOutro = conta(mvc, "outro-qa@example.test", "CORRETOR");

        var dono = chamar(mvc, post, "/gestao/proprietarios", tokenCorretor,
                "{\"nome\":\"Dono fictício\",\"telefone\":\"11900000000\"}", 201).path("id").asLong();
        String dados = """
                {"proprietarioId":%d,"titulo":"Casa QA","descricao":"Casa de teste","tipo":"CASA","cidade":"São Paulo",
                "bairro":"Centro","uf":"SP","cep":"01001000","logradouro":"Rua QA","numero":"1","quartos":2,
                "banheiros":1,"vagas":1,"areaTotal":80,"mobiliado":false,"destaque":true,
                "ofertas":[{"finalidade":"VENDA","preco":250000},{"finalidade":"LOCACAO","preco":2100}]}
                """.formatted(dono);
        long id = chamar(mvc, post, "/gestao/imoveis", tokenCorretor, dados, 201).path("imovel").path("id").asLong();
        chamar(mvc, get, "/gestao/imoveis/" + id, tokenOutro, null, 404);
        var invalida = new org.springframework.mock.web.MockMultipartFile("arquivos", "invalida.png",
                "image/png", new byte[]{1, 2, 3});
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/v1/gestao/imoveis/" + id + "/fotos").file(invalida)
                        .header("Authorization", "Bearer " + tokenCorretor))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        chamar(mvc, get, "/imoveis/" + id, null, null, 404);
        chamar(mvc, patch, "/gestao/imoveis/" + id + "/status", tokenCorretor, "{\"status\":\"PENDENTE\"}", 204);
        chamar(mvc, patch, "/gestao/imoveis/" + id + "/status", tokenCorretor, "{\"status\":\"PUBLICADO\"}", 409);
        chamar(mvc, patch, "/gestao/imoveis/" + id + "/status", tokenAdmin, "{\"status\":\"PUBLICADO\"}", 204);
        assertThat(chamar(mvc, get, "/imoveis/" + id, null, null, 200).path("imovel").path("ofertas").size()).isEqualTo(2);
        chamar(mvc, put, "/usuarios/me/favoritos/" + id, tokenCliente, null, 204);
        chamar(mvc, put, "/usuarios/me/favoritos/" + id, tokenCliente, null, 204);
        assertThat(chamar(mvc, get, "/usuarios/me/favoritos", tokenCliente, null, 200).path("itens").size()).isEqualTo(1);

        var data = java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).plusDays(2);
        var visita = chamar(mvc, post, "/visitas", tokenCliente, """
                {"imovelId":%d,"data":"%s","periodo":"TARDE","telefone":"11900000000"}
                """.formatted(id, data), 201);
        long visitaId = visita.path("id").asLong();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notificacao WHERE lead_id=?", Integer.class,
                visita.path("leadId").asLong())).isEqualTo(1);
        chamar(mvc, patch, "/visitas/" + visitaId + "/status", tokenOutro, "{\"status\":\"CONFIRMADA\"}", 404);
        chamar(mvc, patch, "/visitas/" + visitaId + "/status", tokenCorretor, "{\"status\":\"CONFIRMADA\"}", 204);
        chamar(mvc, patch, "/visitas/" + visitaId + "/status", tokenCliente, "{\"status\":\"CANCELADA\"}", 204);
        chamar(mvc, put, "/gestao/imoveis/" + id, tokenCorretor, dados, 200);
        chamar(mvc, get, "/imoveis/" + id, null, null, 404);

        String refresh = cliente.path("refreshToken").asString();
        var renovado = chamar(mvc, post, "/auth/renovar", null, "{\"refreshToken\":\"" + refresh + "\"}", 200);
        chamar(mvc, post, "/auth/renovar", null, "{\"refreshToken\":\"" + refresh + "\"}", 401);
        chamar(mvc, post, "/auth/logout", null, "{\"refreshToken\":\"" + renovado.path("refreshToken").asString() + "\"}", 204);
        chamar(mvc, get, "/usuarios/me", tokenCliente, null, 401);
    }

    @Test
    void bloqueiaCorretorDepoisDeTresFalhasEApenasAdminDesbloqueia() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web)
                .addFilters(web.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
        var post = org.springframework.http.HttpMethod.POST;
        var patch = org.springframework.http.HttpMethod.PATCH;
        String admin = conta(mvc, "admin-bloqueio@example.test", "ADMIN");
        var corretor = chamar(mvc, post, "/admin/corretores", admin, """
                {"nome":"Corretor fictício","email":"bloqueio@example.test","senha":"ApenasTeste123"}
                """, 201);
        String token = login(mvc, "bloqueio@example.test").path("accessToken").asString();
        String desbloqueio = "/admin/usuarios/" + corretor.path("id").asLong() + "/desbloqueio";
        chamar(mvc, patch, desbloqueio, token, null, 403);
        for (int i = 0; i < 3; i++) {
            chamar(mvc, post, "/auth/login", null, """
                    {"email":"bloqueio@example.test","senha":"SenhaIncorreta"}
                    """, 401);
        }
        chamar(mvc, post, "/auth/login", null, """
                {"email":"bloqueio@example.test","senha":"ApenasTeste123"}
                """, 423);
        chamar(mvc, patch, desbloqueio, admin, null, 204);
        assertThat(login(mvc, "bloqueio@example.test").path("accessToken").asString()).isNotBlank();
    }

    @Test
    void cadastroExigeAceiteVersionadoSemColetarDocumentos() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web)
                .addFilters(web.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
        String cadastro = """
                {"nome":"Cliente fictício","email":"aceite@example.test","senha":"ApenasTeste123",
                "aceiteTermos":true,"versaoTermos":"2026-11-25","versaoPrivacidade":"2026-11-25","origem":"WEB"}
                """;
        chamar(mvc,org.springframework.http.HttpMethod.POST,"/auth/cadastro",null,cadastro.replace("true","false"),400);
        chamar(mvc,org.springframework.http.HttpMethod.POST,"/auth/cadastro",null,cadastro.replace("2026-11-25","antiga"),409);
        chamar(mvc,org.springframework.http.HttpMethod.POST,"/auth/cadastro",null,cadastro,201);
        long id=jdbc.queryForObject("SELECT id FROM usuario WHERE email='aceite@example.test'",Long.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM documento_cliente WHERE usuario_id=?",Long.class,id)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM aceite_legal WHERE usuario_id=? AND LENGTH(hash_termos)=64 AND origem='WEB'",Long.class,id)).isEqualTo(1);
    }

    @Test
    void exportaSomenteDadosPropriosEExcluiDependenciasRevogandoSessao() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web)
                .addFilters(web.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
        var post=org.springframework.http.HttpMethod.POST;
        String cliente=conta(mvc,"privacidade@example.test","CLIENTE");
        String outro=conta(mvc,"outro-privacidade@example.test","CLIENTE");
        String admin=conta(mvc,"admin-privacidade@example.test","ADMIN");
        long id=jdbc.queryForObject("SELECT id FROM usuario WHERE email='privacidade@example.test'",Long.class);
        chamar(mvc,post,"/usuarios/me/privacidade/solicitacoes",cliente,"{\"tipo\":\"ACESSO\",\"senha\":\"incorreta\"}",400);
        var primeiro=chamar(mvc,post,"/usuarios/me/privacidade/solicitacoes",cliente,"{\"tipo\":\"ACESSO\",\"senha\":\"ApenasTeste123\"}",201);
        var repetido=chamar(mvc,post,"/usuarios/me/privacidade/solicitacoes",cliente,"{\"tipo\":\"ACESSO\",\"senha\":\"ApenasTeste123\"}",201);
        assertThat(primeiro.path("protocolo").asString()).isEqualTo(repetido.path("protocolo").asString());
        chamar(mvc,post,"/leads",cliente,"{\"assunto\":\"Privado\",\"mensagem\":\"Minha mensagem\",\"telefone\":\"11900000000\"}",201);
        jdbc.update("INSERT INTO documento_cliente(usuario_id,cpf,rg,orgao_emissor,uf) VALUES (?,'12345678909','ficticio','SSP','SP')",id);
        var copia=chamar(mvc,post,"/usuarios/me/privacidade/exportacao",cliente,"{\"senha\":\"ApenasTeste123\"}",200);
        assertThat(copia.path("perfil").path("email").asString()).isEqualTo("privacidade@example.test");
        assertThat(copia.path("documentosLegados").size()).isEqualTo(1);
        assertThat(copia.toString()).doesNotContain("senha_hash","refresh_hash","outro-privacidade@example.test");
        var privado=chamar(mvc,org.springframework.http.HttpMethod.GET,"/usuarios/me/privacidade",outro,null,200);
        assertThat(privado.path("solicitacoes").size()).isZero();
        chamar(mvc,org.springframework.http.HttpMethod.GET,"/admin/privacidade/solicitacoes",cliente,null,403);
        var exclusao=chamar(mvc,post,"/usuarios/me/privacidade/solicitacoes",cliente,"{\"tipo\":\"EXCLUSAO\",\"senha\":\"ApenasTeste123\"}",201);
        assertThat(exclusao.path("status").asString()).isEqualTo("CONCLUIDA");
        for(String tabela:java.util.List.of("sessao","documento_cliente","favorito","aceite_legal","codigo_verificacao"))
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM "+tabela+" WHERE usuario_id=?",Long.class,id)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lead_contato WHERE cliente_id=?",Long.class,id)).isZero();
        chamar(mvc,org.springframework.http.HttpMethod.GET,"/usuarios/me",cliente,null,401);
        chamar(mvc,org.springframework.http.HttpMethod.GET,"/usuarios/me",outro,null,200);
        var interno=chamar(mvc,post,"/usuarios/me/privacidade/solicitacoes",admin,"{\"tipo\":\"EXCLUSAO\",\"senha\":\"ApenasTeste123\"}",201);
        assertThat(interno.path("status").asString()).isEqualTo("PENDENTE");
    }

    private String conta(org.springframework.test.web.servlet.MockMvc mvc, String email, String perfil) throws Exception {
        jdbc.update("INSERT INTO usuario(nome,email,senha_hash,perfil,email_verificado) VALUES (?,?,?,?,TRUE)",
                "Usuário de teste", email, senhas.encode("ApenasTeste123"), perfil);
        return login(mvc, email).path("accessToken").asString();
    }

    private tools.jackson.databind.JsonNode login(org.springframework.test.web.servlet.MockMvc mvc, String email) throws Exception {
        return chamar(mvc, org.springframework.http.HttpMethod.POST, "/auth/login", null,
                "{\"email\":\"" + email + "\",\"senha\":\"ApenasTeste123\"}", 200);
    }

    private tools.jackson.databind.JsonNode chamar(org.springframework.test.web.servlet.MockMvc mvc, org.springframework.http.HttpMethod metodo,
            String path, String token, String corpo, int status) throws Exception {
        var pedido = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(metodo, "/api/v1" + path);
        if (token != null) {
            pedido.header("Authorization", "Bearer " + token);
        }
        if (corpo != null) {
            pedido.contentType("application/json").content(corpo);
        }
        var resposta = mvc.perform(pedido).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(status))
                .andReturn().getResponse();
        return resposta.getContentAsByteArray().length == 0 ? json.createObjectNode() : json.readTree(resposta.getContentAsByteArray());
    }
}
