# Based on <https://hub.docker.com/_/postgres/>

FROM postgres:11.9

COPY ./configure-postgres.sh /docker-entrypoint-initdb.d/
COPY ./configure-kouta-database.sh /docker-entrypoint-initdb.d/