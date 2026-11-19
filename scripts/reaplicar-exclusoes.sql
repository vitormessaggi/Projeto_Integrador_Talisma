-- Executar em banco RESTAURADO, antes de disponibilizar tráfego.
-- Preencher com IDs do ledger atual exportado por administrador antes da restauração.
-- A tabela temporária vazia faz este script não excluir nenhum registro por padrão.
CREATE TEMPORARY TABLE exclusoes_confirmadas (id BIGINT PRIMARY KEY);
-- INSERT INTO exclusoes_confirmadas VALUES (ID_CONFIRMADO);
START TRANSACTION;
-- Nunca excluir contas internas por este procedimento.
DELETE e FROM exclusoes_confirmadas e JOIN usuario u ON u.id=e.id WHERE u.perfil<>'CLIENTE';
DELETE n FROM notificacao n LEFT JOIN lead_contato l ON l.id=n.lead_id
 WHERE n.usuario_id IN (SELECT id FROM exclusoes_confirmadas) OR l.cliente_id IN (SELECT id FROM exclusoes_confirmadas);
DELETE v FROM visita v JOIN lead_contato l ON l.id=v.lead_id WHERE l.cliente_id IN (SELECT id FROM exclusoes_confirmadas);
DELETE FROM lead_contato WHERE cliente_id IN (SELECT id FROM exclusoes_confirmadas);
DELETE FROM favorito WHERE usuario_id IN (SELECT id FROM exclusoes_confirmadas);
DELETE FROM documento_cliente WHERE usuario_id IN (SELECT id FROM exclusoes_confirmadas);
DELETE FROM aceite_legal WHERE usuario_id IN (SELECT id FROM exclusoes_confirmadas);
DELETE FROM codigo_verificacao WHERE usuario_id IN (SELECT id FROM exclusoes_confirmadas);
DELETE FROM sessao WHERE usuario_id IN (SELECT id FROM exclusoes_confirmadas);
DELETE FROM usuario WHERE id IN (SELECT id FROM exclusoes_confirmadas) AND perfil='CLIENTE';
COMMIT;
DROP TEMPORARY TABLE exclusoes_confirmadas;
