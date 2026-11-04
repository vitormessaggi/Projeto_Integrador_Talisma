-- Leads e visitas por corretor. Substitua 1 pelo ID autorizado.
SELECT l.id, l.assunto, l.status, l.criado_em, v.data_preferida, v.periodo, v.status AS visita_status
FROM lead_contato l LEFT JOIN visita v ON v.lead_id = l.id
WHERE l.corretor_id = 1 ORDER BY l.criado_em DESC, l.id DESC LIMIT 10;

-- Favoritos publicados do cliente.
SELECT i.id, i.titulo
FROM favorito f JOIN imovel i ON i.id = f.imovel_id
WHERE f.usuario_id = 1 AND i.status = 'PUBLICADO' ORDER BY i.id;

-- Indicadores agregados, sem dados pessoais.
SELECT status, COUNT(*) AS total FROM imovel GROUP BY status;
SELECT status, COUNT(*) AS total FROM lead_contato GROUP BY status;
SELECT status, COUNT(*) AS total FROM visita GROUP BY status;
