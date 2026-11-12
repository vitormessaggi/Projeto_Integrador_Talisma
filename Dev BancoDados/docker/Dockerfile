FROM mysql:8.4@sha256:a81d043930e51745522bd5c430961cf8286f0a23f0a9a2a6e0108f7ea71b0902
COPY my.cnf /etc/mysql/conf.d/talisma.cnf
COPY 01-test-database.sh /docker-entrypoint-initdb.d/01-test-database.sh
