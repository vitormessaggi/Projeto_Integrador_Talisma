-- Executar apenas na base talisma com um corretor de demonstração já cadastrado.
-- Defina @email_corretor na mesma conexão antes de executar.
START TRANSACTION;

SET @corretor := (SELECT id FROM usuario WHERE email = @email_corretor AND perfil = 'CORRETOR');

INSERT INTO proprietario(corretor_id, nome, telefone)
SELECT @corretor, 'Proprietário de demonstração', '11000000000'
WHERE @corretor IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM proprietario WHERE corretor_id = @corretor AND nome = 'Proprietário de demonstração'
);

SET @proprietario := (SELECT MIN(id) FROM proprietario
    WHERE corretor_id = @corretor AND nome = 'Proprietário de demonstração');

INSERT INTO imovel(proprietario_id, corretor_id, titulo, descricao, tipo, status, cidade, bairro, uf,
    cep, logradouro, numero, quartos, banheiros, vagas, area_total, destaque)
SELECT @proprietario, @corretor, '[DEMO] Casa com quintal', 'Imóvel fictício para demonstração de venda e locação.',
    'CASA', 'RASCUNHO', 'São Paulo', 'Parque São Rafael', 'SP', '00000000', 'Rua de Demonstração', '10', 2, 1, 1, 95, TRUE
WHERE @proprietario IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM imovel WHERE corretor_id = @corretor AND titulo = '[DEMO] Casa com quintal'
);

SET @imovel := (SELECT MIN(id) FROM imovel WHERE corretor_id = @corretor AND titulo = '[DEMO] Casa com quintal');

INSERT INTO oferta(imovel_id, finalidade, preco)
SELECT @imovel, tipos.finalidade, tipos.preco
FROM (SELECT 'VENDA' AS finalidade, 320000.00 AS preco UNION ALL SELECT 'LOCACAO', 1800.00) tipos
WHERE @imovel IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM oferta WHERE imovel_id = @imovel AND finalidade = tipos.finalidade
);

COMMIT;
SELECT @imovel AS imovel_de_demonstracao;
