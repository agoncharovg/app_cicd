FROM python:3.11-slim-bookworm

RUN apt-get update && apt-get --yes install libsasl2-dev python-dev-is-python3 libldap2-dev libssl-dev \
    xmlsec1 libxmlsec1-dev libxmlsec1-openssl libpq-dev
COPY build/requirements.txt requirements.txt
RUN pip install --upgrade pip && pip install -r requirements.txt

COPY manage.py build/boot.sh /src/app/
WORKDIR /src/app

COPY src/ /src/app/src/

RUN mkdir -p /src/app/static
RUN chmod +x /src/app/boot.sh

EXPOSE 5101
ENTRYPOINT ["./boot.sh"]