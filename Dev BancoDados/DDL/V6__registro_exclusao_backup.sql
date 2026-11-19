-- Identificador técnico mínimo para reaplicar exclusões durante a janela de backups.
-- Não é documento, e-mail nem credencial. Não deve ser exposto no catálogo público.
ALTER TABLE solicitacao_privacidade ADD COLUMN usuario_excluido_id BIGINT NULL;
