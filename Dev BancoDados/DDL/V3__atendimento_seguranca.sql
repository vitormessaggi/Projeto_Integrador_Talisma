CREATE TABLE lead (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 cliente_id BIGINT NOT NULL,
 corretor_id BIGINT,
 imovel_id BIGINT,
 oferta_id BIGINT,
 assunto VARCHAR(160) NOT NULL,
 mensagem VARCHAR(2000) NOT NULL,
 telefone VARCHAR(20) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'NOVO',
 criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CHECK(status IN ('NOVO','EM_ATENDIMENTO','ENCERRADO')),
 CHECK(oferta_id IS NULL OR imovel_id IS NOT NULL),
 FOREIGN KEY(cliente_id) REFERENCES usuario(id),
 FOREIGN KEY(corretor_id) REFERENCES usuario(id),
 FOREIGN KEY(imovel_id) REFERENCES imovel(id),
 FOREIGN KEY(oferta_id,imovel_id) REFERENCES oferta(id,imovel_id),
 INDEX idx_lead_cliente(cliente_id,criado_em),
 INDEX idx_lead_corretor(corretor_id,status)
) ENGINE=InnoDB;
CREATE TABLE visita (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 lead_id BIGINT NOT NULL UNIQUE,
 data_preferida DATE NOT NULL,
 periodo VARCHAR(10) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
 versao BIGINT NOT NULL DEFAULT 0,
 CHECK(periodo IN ('MANHA','TARDE','NOITE')),
 CHECK(status IN ('PENDENTE','CONFIRMADA','CONCLUIDA','CANCELADA')),
 FOREIGN KEY(lead_id) REFERENCES lead(id)
) ENGINE=InnoDB;
CREATE TABLE sessao (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 usuario_id BIGINT NOT NULL,
 refresh_hash CHAR(64) NOT NULL UNIQUE,
 expira_em DATETIME(6) NOT NULL,
 revogada_em DATETIME(6),
 criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY(usuario_id) REFERENCES usuario(id),
 INDEX idx_sessao_usuario(usuario_id)
) ENGINE=InnoDB;
CREATE TABLE codigo_verificacao (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 usuario_id BIGINT NOT NULL,
 finalidade VARCHAR(20) NOT NULL,
 codigo_hash VARCHAR(100) NOT NULL,
 tentativas INT NOT NULL DEFAULT 0,
 expira_em DATETIME(6) NOT NULL,
 consumido_em DATETIME(6),
 criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CHECK(finalidade IN ('CADASTRO','RECUPERACAO')),
 FOREIGN KEY(usuario_id) REFERENCES usuario(id),
 INDEX idx_codigo_usuario(usuario_id,finalidade,criado_em)
) ENGINE=InnoDB;
CREATE TABLE notificacao (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 usuario_id BIGINT NOT NULL,
 lead_id BIGINT,
 assunto VARCHAR(200) NOT NULL,
 corpo TEXT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
 tentativas INT NOT NULL DEFAULT 0,
 proxima_tentativa DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 enviada_em DATETIME(6),
 criada_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CHECK(status IN ('PENDENTE','ENVIADA','FALHOU')),
 FOREIGN KEY(usuario_id) REFERENCES usuario(id),
 FOREIGN KEY(lead_id) REFERENCES lead(id),
 INDEX idx_envio(status,proxima_tentativa)
) ENGINE=InnoDB;
CREATE TABLE historico_imovel (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 imovel_id BIGINT NOT NULL,
 autor_id BIGINT NOT NULL,
 acao VARCHAR(40) NOT NULL,
 motivo VARCHAR(1000),
 criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY(imovel_id) REFERENCES imovel(id),
 FOREIGN KEY(autor_id) REFERENCES usuario(id),
 INDEX idx_historico(imovel_id,criado_em)
) ENGINE=InnoDB;
