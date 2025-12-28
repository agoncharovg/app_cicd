FROM python:3.11-slim-bookworm

RUN apt-get update && apt-get install -y \
    libsasl2-dev \
    python-dev-is-python3 \
    libldap2-dev \
    libssl-dev \
    xmlsec1 \
    libxmlsec1-dev \
    libxmlsec1-openssl \
    libpq-dev

WORKDIR /app

COPY build/requirements.txt .
RUN pip install --upgrade pip && pip install -r requirements.txt

COPY manage.py .
COPY build/boot.sh .
COPY src/ ./src/

RUN chmod +x boot.sh
RUN mkdir -p static

EXPOSE 5101
ENTRYPOINT ["./boot.sh"]