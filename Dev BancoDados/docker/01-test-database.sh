#!/bin/bash
set -eu
# Names and passwords are validated before interpolation into SQL.
[[ "$TEST_DB_USER" =~ ^[a-zA-Z0-9_]+$ ]] || { echo 'Invalid test username' >&2; exit 1; }
[[ "$TEST_DB_PASSWORD" =~ ^[a-zA-Z0-9_-]{16,}$ ]] || { echo 'Test password requires 16+ letters, digits, _ or -' >&2; exit 1; }
[[ "$TEST_DB_USER" != "$MYSQL_USER" ]] || { echo 'Use separate database accounts' >&2; exit 1; }
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot <<SQL
CREATE DATABASE IF NOT EXISTS talisma_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS '${TEST_DB_USER}'@'%' IDENTIFIED BY '${TEST_DB_PASSWORD}';
GRANT ALL PRIVILEGES ON talisma_test.* TO '${TEST_DB_USER}'@'%';
SQL
