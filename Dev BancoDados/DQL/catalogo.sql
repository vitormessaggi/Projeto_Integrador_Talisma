-- Ofertas publicadas: finalidade e faixa aplicadas à mesma oferta.
SELECT i.id, i.titulo, i.cidade, o.finalidade, o.preco
FROM imovel i JOIN oferta o ON o.imovel_id = i.id
WHERE i.status = 'PUBLICADO'
  AND o.finalidade = 'VENDA'
  AND o.preco BETWEEN 100000.00 AND 900000.00
ORDER BY o.preco, i.id LIMIT 10 OFFSET 0;
-- Fila de aprovação.
SELECT id, titulo, corretor_id FROM imovel
WHERE status = 'PENDENTE' ORDER BY criado_em, id;
