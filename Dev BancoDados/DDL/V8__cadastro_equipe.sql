INSERT INTO perfil(codigo) VALUES ('FUNCIONARIO');

CREATE TABLE documento_equipe (
    usuario_id BIGINT PRIMARY KEY,
    cpf CHAR(11) NOT NULL UNIQUE,
    rg VARCHAR(30) NOT NULL,
    orgao_emissor VARCHAR(20) NOT NULL,
    uf_rg CHAR(2) NOT NULL,
    cargo VARCHAR(80) NOT NULL,
    creci VARCHAR(30),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB;
