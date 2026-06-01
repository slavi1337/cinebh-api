#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DUMP_FILE="$SCRIPT_DIR/dump2.sql"
WAIT_SECONDS="${DB_INIT_WAIT_SECONDS:-2}"
MAX_ATTEMPTS="${DB_INIT_MAX_ATTEMPTS:-90}"

if [ "$#" -gt 1 ]; then
    echo "Usage: $0 [postgresql://USER:PASSWORD@HOST:PORT/DATABASE]" >&2
    exit 1
fi

if [ "$#" -eq 1 ]; then
    set -- psql "$1"
else
    set -- psql
fi

echo "Waiting for the Flyway schema..."
attempt=1
until "$@" --tuples-only --no-align --command "SELECT to_regclass('public.movies') IS NOT NULL" 2>/dev/null | grep -q t; do
    if [ "$attempt" -ge "$MAX_ATTEMPTS" ]; then
        echo "Flyway schema was not available after $MAX_ATTEMPTS attempts" >&2
        exit 1
    fi

    sleep "$WAIT_SECONDS"
    attempt=$((attempt + 1))
done

echo "Importing data"
"$@" --set ON_ERROR_STOP=on --file "$DUMP_FILE"
echo "Import completed"
