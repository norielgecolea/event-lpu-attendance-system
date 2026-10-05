FROM nginx:latest

RUN apt-get update \
  && apt-get install -y --no-install-recommends openssl \
  && rm -rf /var/lib/apt/lists/*

COPY 98-gate-pictures-group.sh /docker-entrypoint.d/98-gate-pictures-group.sh
COPY 99-ensure-certs.sh /docker-entrypoint.d/99-ensure-certs.sh
RUN chmod +x /docker-entrypoint.d/98-gate-pictures-group.sh /docker-entrypoint.d/99-ensure-certs.sh
