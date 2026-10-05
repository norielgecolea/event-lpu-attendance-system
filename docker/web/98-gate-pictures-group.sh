#!/bin/sh
# Gate photo files are often mode 640. Give the nginx worker that group so it
# can read the mounted directory without making the files world-readable.
set -e

DIR=/var/gate-pictures
if [ ! -d "$DIR" ]; then
  exit 0
fi

gid=$(stat -c '%g' "$DIR" 2>/dev/null || stat -f '%g' "$DIR" 2>/dev/null || true)
if [ -z "$gid" ] || [ "$gid" = "0" ]; then
  exit 0
fi

if ! getent group "$gid" >/dev/null 2>&1; then
  groupadd -g "$gid" gatepics
fi
group=$(getent group "$gid" | cut -d: -f1)
usermod -aG "$group" nginx
