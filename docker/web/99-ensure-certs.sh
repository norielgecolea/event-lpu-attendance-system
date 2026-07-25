#!/bin/sh
# Auto-create a self-signed cert when none are mounted (certs are gitignored).
set -e

CERT_DIR="${NGINX_CERT_DIR:-/etc/nginx/certs}"
CERT_FILE="${CERT_DIR}/fullchain.pem"
KEY_FILE="${CERT_DIR}/privkey.pem"
CN="${SERVER_NAME:-localhost}"

if [ -f "$CERT_FILE" ] && [ -f "$KEY_FILE" ]; then
  echo "TLS certs found in ${CERT_DIR}"
  exit 0
fi

echo "No TLS certs in ${CERT_DIR}; generating self-signed certificate for CN=${CN}"
mkdir -p "$CERT_DIR"
openssl req -x509 -nodes -newkey rsa:2048 -days 3650 \
  -keyout "$KEY_FILE" \
  -out "$CERT_FILE" \
  -subj "/CN=${CN}" \
  -addext "subjectAltName=DNS:${CN},DNS:localhost,IP:127.0.0.1"

chmod 644 "$CERT_FILE"
chmod 600 "$KEY_FILE"
echo "Self-signed TLS certificate ready"
