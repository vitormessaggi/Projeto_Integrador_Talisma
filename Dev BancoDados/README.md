# Banco Talismã

Alvo: MySQL 8.4, InnoDB, utf8mb4_0900_ai_ci.

Provisionar uma base vazia `talisma` e outra `talisma_test`, com usuários distintos. Nunca executar testes na base de desenvolvimento.

## Ordem das migrations
1. DDL/V1__catalogo_identidade.sql
2. DML/V2__perfis.sql
3. DDL/V3__atendimento_seguranca.sql

O backend empacota estes arquivos diretamente e executa via Flyway. Não aplicar manualmente as mesmas migrations em uma base gerenciada pelo Flyway. Não editar migrations já aplicadas; criar uma nova versão.

DQL contém consultas de consulta manual, não migrations. Não há senha padrão nem usuário administrativo nos seeds. O provisionamento do primeiro administrador pertence ao backend.

## Relações
Usuário pertence a perfil e pode possuir documento, favoritos e sessões. Proprietário e imóvel são gerenciados pelo corretor. Imóvel possui ofertas distintas por finalidade e fotos ordenadas. Lead pertence ao cliente, pode referenciar imóvel/oferta e ser atribuído a corretor. Visita possui um único lead. Notificações persistem independentemente do envio SMTP. Histórico registra ator e ação sobre o imóvel.

## Backup e restauração
Executar mysqldump com --single-transaction --routines --triggers, usando senha solicitada interativamente ou arquivo de credenciais protegido fora do Git. Incluir separadamente o diretório de fotos. Restaurar em uma base nova e validar contagens e relacionamentos antes de substituir qualquer base em uso.

## Estado de validação
Scripts ainda não executados contra MySQL. Seeds demonstrativos e testes de integridade ainda pendentes.
