CREATE TABLE aceite_legal (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 usuario_id BIGINT NOT NULL,
 versao_termos VARCHAR(40) NOT NULL,
 versao_privacidade VARCHAR(40) NOT NULL,
 hash_termos CHAR(64) NOT NULL,
 hash_privacidade CHAR(64) NOT NULL,
 origem VARCHAR(10) NOT NULL,
 criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CHECK(origem IN ('WEB','ANDROID')),
 FOREIGN KEY(usuario_id) REFERENCES usuario(id),
 INDEX idx_aceite_usuario(usuario_id,criado_em)
) ENGINE=InnoDB;

CREATE TABLE solicitacao_privacidade (
 protocolo CHAR(36) PRIMARY KEY,
 usuario_id BIGINT,
 tipo VARCHAR(10) NOT NULL,
 status VARCHAR(12) NOT NULL DEFAULT 'PENDENTE',
 resposta VARCHAR(2000),
 criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 concluido_em DATETIME(6),
 CHECK(tipo IN ('ACESSO','EXCLUSAO')),
 CHECK(status IN ('PENDENTE','CONCLUIDA','INDEFERIDA')),
 FOREIGN KEY(usuario_id) REFERENCES usuario(id) ON DELETE SET NULL,
 INDEX idx_privacidade_usuario(usuario_id,status)
) ENGINE=InnoDB;

CREATE TABLE exclusao_arquivo (
 arquivo VARCHAR(255) PRIMARY KEY,
 tentativas INT NOT NULL DEFAULT 0,
 criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;
