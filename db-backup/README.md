# Cinebh PostgreSQL Demo Data

This folder contains a sanitized PostgreSQL data dump and an explicit import script:

```text
dump2.sql
init.sh
```

PostgreSQL uses `.sql` or PostgreSQL custom-format dump files, not SQL Server `.bak` files.

The dump contains public demo catalog data only. It intentionally excludes users, password hashes, verification
codes, OAuth identifiers, bookings, and payments.

Do not mount `init.sh` into PostgreSQL `/docker-entrypoint-initdb.d`: this is a data-only dump and Flyway creates the
tables. Run the script as a one-shot init service alongside the backend, or run it manually from an environment that
has `psql`. The script waits up to three minutes for the Flyway schema by default:

```sh
sh ./db-backup/init.sh "postgresql://USER:PASSWORD@HOST:5432/cinebh"
```

The standard `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD` variables can be used instead of the URL:

```sh
sh ./db-backup/init.sh
```

The SQL uses `ON CONFLICT DO NOTHING`, so rerunning the import does not duplicate existing rows.
