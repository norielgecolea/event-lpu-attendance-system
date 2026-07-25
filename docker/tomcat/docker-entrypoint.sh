#!/bin/bash
set -euo pipefail

WAR_SRC="/opt/wars/event-attendance-system.war"
WEBAPPS="${CATALINA_HOME:-/usr/local/tomcat}/webapps"
WAR_DST="${WEBAPPS}/event-attendance-system.war"

if [[ ! -f "$WAR_SRC" ]]; then
  echo "ERROR: baked WAR missing at ${WAR_SRC}" >&2
  exit 1
fi

mkdir -p "$WEBAPPS"

# Prefer an existing mounted/git-deployed WAR; never delete WAR files.
# Only copy the image WAR when the webapps volume has none yet.
if [[ -f "$WAR_DST" ]]; then
  echo "Keeping existing WAR at ${WAR_DST}"
else
  echo "No WAR in webapps; copying baked WAR to ${WAR_DST}"
  cp -f "$WAR_SRC" "$WAR_DST"
fi

exec "$@"
