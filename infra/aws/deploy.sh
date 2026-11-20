#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${APP_IMAGE_TAG:?Defina a revisão a implantar}"
[[ "$APP_IMAGE_TAG" =~ ^[a-zA-Z0-9._-]+$ ]] || { echo 'Tag inválida'; exit 1; }
test -r /opt/talisma/runtime.env
docker compose -f infra/aws/compose.yaml build api chatbot
# As migrations são executadas separadamente pelo operador com credenciais temporárias.
docker compose -f infra/aws/compose.yaml up -d
for tentativa in $(seq 1 60); do
  if curl --fail --silent --max-time 2 http://127.0.0.1:8080/actuator/health >/dev/null &&
     curl --fail --silent --max-time 2 http://127.0.0.1:8000/health >/dev/null; then
    printf 'API e Lia saudáveis. Versão %s\n' "$APP_IMAGE_TAG"
    exit 0
  fi
  sleep 2
done
echo 'A API ou a Lia não ficou saudável. Consulte os logs sem divulgar credenciais.'
exit 1
