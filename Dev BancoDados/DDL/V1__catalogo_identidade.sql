CREATE TABLE perfil (
 codigo VARCHAR(20) PRIMARY KEY
) ENGINE=InnoDB;
CREATE TABLE usuario (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 nome VARCHAR(160) NOT NULL,
 email VARCHAR(254) NOT NULL UNIQUE,
 senha_hash VARCHAR(100) NOT NULL,
 perfil VARCHAR(20) NOT NULL,
 email_verificado BOOLEAN NOT NULL DEFAULT FALSE,
 bloqueado BOOLEAN NOT NULL DEFAULT FALSE,
 tentativas INT NOT NULL DEFAULT 0,
 telefone VARCHAR(20),
 criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (perfil) REFERENCES perfil(codigo)
) ENGINE=InnoDB;
CREATE TABLE documento_cliente (
 usuario_id BIGINT PRIMARY KEY,
 cpf CHAR(11) NOT NULL UNIQUE,
 rg VARCHAR(30) NOT NULL,
 orgao_emissor VARCHAR(20) NOT NULL,
 uf CHAR(2) NOT NULL,
 FOREIGN KEY (usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB;
CREATE TABLE proprietario (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 corretor_id BIGINT NOT NULL,
 nome VARCHAR(160) NOT NULL,
 telefone VARCHAR(20) NOT NULL,
 FOREIGN KEY (corretor_id) REFERENCES usuario(id)
) ENGINE=InnoDB;
CREATE TABLE imovel (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 proprietario_id BIGINT NOT NULL,
 corretor_id BIGINT NOT NULL,
 titulo VARCHAR(180) NOT NULL,
 descricao TEXT NOT NULL,
 tipo VARCHAR(30) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO',
 cidade VARCHAR(120) NOT NULL,
 bairro VARCHAR(120) NOT NULL,
 uf CHAR(2) NOT NULL,
 cep CHAR(8) NOT NULL,
 logradouro VARCHAR(200) NOT NULL,
 numero VARCHAR(20) NOT NULL,
 quartos INT NOT NULL DEFAULT 0 CHECK (quartos >= 0),
 banheiros INT NOT NULL DEFAULT 0 CHECK (banheiros >= 0),
 vagas INT NOT NULL DEFAULT 0 CHECK (vagas >= 0),
 area_total DECIMAL(12,2) NOT NULL CHECK (area_total > 0),
 area_construida DECIMAL(12,2),
 mobiliado BOOLEAN NOT NULL DEFAULT FALSE,
 destaque BOOLEAN NOT NULL DEFAULT FALSE,
 latitude DECIMAL(10,7),
 longitude DECIMAL(10,7),
 versao BIGINT NOT NULL DEFAULT 0,
 criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CHECK (status IN ('RASCUNHO','PENDENTE','PUBLICADO','REJEITADO','ARQUIVADO')),
 FOREIGN KEY (proprietario_id) REFERENCES proprietario(id),
 FOREIGN KEY (corretor_id) REFERENCES usuario(id),
 INDEX idx_catalogo(status,cidade,tipo),
 INDEX idx_corretor(corretor_id,status)
) ENGINE=InnoDB;
CREATE TABLE oferta (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 imovel_id BIGINT NOT NULL,
 finalidade VARCHAR(10) NOT NULL,
 preco DECIMAL(15,2) NOT NULL CHECK(preco > 0),
 CHECK(finalidade IN ('VENDA','LOCACAO')),
 UNIQUE(imovel_id,finalidade),
 UNIQUE(id,imovel_id),
 FOREIGN KEY(imovel_id) REFERENCES imovel(id),
 INDEX idx_busca(finalidade,preco,imovel_id)
) ENGINE=InnoDB;
CREATE TABLE foto (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 imovel_id BIGINT NOT NULL,
 arquivo VARCHAR(255) NOT NULL UNIQUE,
 ordem INT NOT NULL DEFAULT 0,
 FOREIGN KEY(imovel_id) REFERENCES imovel(id)
) ENGINE=InnoDB;
CREATE TABLE favorito (
 usuario_id BIGINT NOT NULL,
 imovel_id BIGINT NOT NULL,
 PRIMARY KEY(usuario_id,imovel_id),
 FOREIGN KEY(usuario_id) REFERENCES usuario(id),
 FOREIGN KEY(imovel_id) REFERENCES imovel(id)
) ENGINE=InnoDB;
