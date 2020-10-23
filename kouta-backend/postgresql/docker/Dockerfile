# Based on <https://hub.docker.com/_/postgres/>

FROM postgres:10.6

COPY ./configure-database.sh /docker-entrypoint-initdb.d/
COPY ./configure-postgres.sh /docker-entrypoint-initdb.d/
