#!/usr/bin/env bash

set -euo pipefail

DB_APP_DB=kouta
DB_APP_USER=oph

psql "${DB_APP_DB}" -c "CREATE EXTENSION pg_cron" \
                    -c "GRANT USAGE ON SCHEMA \"cron\" TO \"${DB_APP_USER}\""