#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${APP_IMAGE_TAG:?Defina a revisão a implantar}"
[[ "$APP_IMAGE_TAG" =~ ^[a-zA-Z0-9._-]+$ ]] || { echo 'Tag inválida'; exit 1; }
test -r /opt/talisma/runtime.env
docker compose -f infra/aws/compose.yaml build api
# As migrations são executadas separadamente pelo operador com credenciais temporárias.
docker compose -f infra/aws/compose.yaml up -d
for tentativa in $(seq 1 30); do
  if curl --fail --silent http://127.0.0.1:8080/actuator/health >/dev/null; then
    printf 'API saudável. Versão %s\n' "$APP_IMAGE_TAG"
    exit 0
  fi
  sleep 2
done
echo 'API não ficou saudável. Consulte logs sem divulgar credenciais; mantenha a imagem anterior para rollback.'
exit 1
