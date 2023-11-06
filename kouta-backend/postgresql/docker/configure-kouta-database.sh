#!/usr/bin/env bash

set -euo pipefail

DB_APP_DB=kouta
DB_APP_USER=oph
DB_APP_PASSWORD=oph

echo "Creating database \"$DB_APP_DB\", creating role \"$DB_APP_USER\" with database owner privileges…"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-END
create role "${DB_APP_USER}" with password '${DB_APP_PASSWORD}' login;
create database "${DB_APP_DB}" encoding 'UTF-8' lc_collate 'C' lc_ctype 'C' TEMPLATE template0;
grant all privileges on database "${DB_APP_DB}" to "${DB_APP_USER}";
create extension pg_cron;
END

psql "${DB_APP_DB}" -c "ALTER SCHEMA \"public\" OWNER TO \"${DB_APP_USER}\"" \
                    -c "GRANT ALL ON SCHEMA \"public\" TO \"${DB_APP_USER}\""