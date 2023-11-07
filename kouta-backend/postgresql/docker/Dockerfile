# Based on <https://hub.docker.com/_/postgres/>

FROM postgres:12.12

RUN apt-get update \
      && apt-get install -y postgresql-12-cron \
      && rm -rf /var/lib/apt/lists/*

COPY ./1-configure-kouta-database.sh /docker-entrypoint-initdb.d/
COPY ./2-configure-postgres.sh /docker-entrypoint-initdb.d/
COPY ./3-configure-pg-cron.sh /docker-entrypoint-initdb.d/