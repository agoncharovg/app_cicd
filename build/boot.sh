#!/bin/bash

case $1 in
    app)
    python manage.py migrate
    echo "Booting app"
    python manage.py collectstatic
    exec gunicorn -b :"${BIND_PORT}" --limit-request-line 0 --workers 4 --timeout 240 src.wsgi
    ;;
esac