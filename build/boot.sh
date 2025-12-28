#!/bin/bash
set -e

case "$1" in
  app)
    python manage.py migrate --noinput
    python manage.py collectstatic --noinput
    exec gunicorn -b :"${BIND_PORT}" --workers 4 --timeout 240 src.wsgi
    ;;
  *)
    exec "$@"
    ;;
esac