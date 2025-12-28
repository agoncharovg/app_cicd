FROM python:3.11-slim-bookworm

RUN apt-get update && apt-get --yes install libsasl2-dev python-dev-is-python3 libldap2-dev libssl-dev \
    xmlsec1 libxmlsec1-dev libxmlsec1-openssl libpq-dev \

COPY build/ /app/
RUN pip install --upgrade pip && pip install -r requirements.txt

COPY manage.py /app/
WORKDIR /app

COPY src/ /app/src/

RUN mkdir -p /app/static
RUN chmod +x /app/boot.sh

EXPOSE 5101
ENTRYPOINT ["./boot.sh"]

