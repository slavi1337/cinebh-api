# Cinebh API Deployment Guide

This document describes the Cinebh backend deployment model, required services, environment variables, database setup,
and reverse proxy requirements.

## Deployment Overview

The backend is a Spring Boot API packaged as a Docker image. The deployed system normally runs together with:

- PostgreSQL
- Redis
- S3-compatible object storage or AWS S3
- frontend container
- Nginx/reverse proxy with HTTPS
- Jenkins build and deploy jobs

The backend exposes HTTPS on port `8443` inside the container.

## Docker Image

The backend `Dockerfile` uses two stages.

Build stage:

- `maven:3.9-eclipse-temurin-21-alpine`
- downloads dependencies
- packages the app with `./mvnw package -DskipTests -B`

Runtime stage:

- `eclipse-temurin:21-jre-alpine`
- runs as a non-root `cinebh` user
- copies the packaged jar to `/app/app.jar`
- copies the local keystore to `/app/cinebh-keystore.p12`
- exposes port `8443`

Manual build example:

```bash
docker build -t cinebh-api:latest .
```

## Runtime Services

The included `docker-compose.yml` starts:

- `db` - PostgreSQL 17 with healthcheck
- `redis` - Redis 7 with healthcheck
- `minio` - local S3-compatible storage
- `minio-init` - creates the `cinebh` bucket
- `backend` - Spring Boot API
- `frontend` - built from `../cinebh-web`

The backend depends on healthy PostgreSQL and Redis services before startup.

Start the stack from the backend repository root:

```bash
docker compose up --build
```

The frontend repository must be available next to the backend repository:

```text
parent-folder/
  cinebh-api/
  cinebh-web/
```

## Required Environment Variables

Production and Jenkins should provide secrets through environment variables or Jenkins credentials.

Core backend:

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<host>:5432/cinebh
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>
JWT_SECRET=<long-random-secret>
```

Google OAuth:

```text
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
```

Mail:

```text
SMTP2GO_PASSWORD=<smtp-password>
```

TLS keystore:

```text
KEYSTORE_PASSWORD=<keystore-password>
```

Storage:

```text
APP_STORAGE_ACCESS_KEY=<storage-access-key>
APP_STORAGE_SECRET_KEY=<storage-secret-key>
APP_STORAGE_PUBLIC_BASE_URL=<public-storage-base-url>
```

Cookie/CORS/frontend:

```text
APP_FRONTEND_BASE_URL=https://<frontend-public-domain>
APP_SECURITY_COOKIE_DOMAIN=<cookie-domain>
APP_SECURITY_COOKIE_SECURE=true
APP_SECURITY_COOKIE_SAME_SITE=None
APP_SECURITY_CORS_ALLOWED_ORIGINS=https://<frontend-public-domain>
```

For same-origin deployment, the cookie domain should match the public application domain. For subdomain sharing, use a
shared parent domain only when that is intentional and supported by the deployed domains.

## Local HTTPS Development

Local development uses HTTPS and custom local hostnames so cookies and Google OAuth can be tested realistically.

Add host mappings:

```text
127.0.0.1 cinebh.com
127.0.0.1 api.cinebh.com
```

The local backend runs on:

```text
https://api.cinebh.com:8443
```

The local frontend runs on:

```text
https://cinebh.com:5173
```

The `local` profile expects a PKCS12 keystore:

```text
src/main/resources/cinebh-keystore.p12
```

Example keystore generation command:

```bash
keytool -genkeypair \
  -alias cinebh \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore src/main/resources/cinebh-keystore.p12 \
  -validity 3650 \
  -storepass <keystore-password> \
  -dname "CN=api.cinebh.com, OU=Development, O=Cinebh, L=Banja Luka, ST=RS, C=BA" \
  -ext "SAN=dns:api.cinebh.com,dns:cinebh.com,ip:127.0.0.1"
```

Set the matching password through `KEYSTORE_PASSWORD`.

## Google OAuth

The application uses Spring Security's standard OAuth routes:

```text
/oauth2/authorization/google
/login/oauth2/code/google
```

The OAuth callback is intentionally not under `/api/v1`.

Local Google Console configuration:

Authorized JavaScript origin:

```text
https://cinebh.com:5173
```

Authorized redirect URI:

```text
https://api.cinebh.com:8443/login/oauth2/code/google
```

Production Google Console configuration should use the public deployment domain. For a same-origin deployment:

```text
https://<frontend-public-domain>/login/oauth2/code/google
```

If Google returns `redirect_uri_mismatch`, copy the exact `redirect_uri` value from the Google error details and add it
to the Google Console for the correct OAuth client.

## Reverse Proxy and TLS

The public reverse proxy must terminate HTTPS and forward these backend paths to the backend service:

```text
/api/**
/swagger-ui/**
/v3/api-docs/**
/oauth2/**
/login/**
```

Forward these headers to preserve the public URL for OAuth redirect generation:

```nginx
proxy_set_header Host $host;
proxy_set_header X-Forwarded-Proto https;
proxy_set_header X-Forwarded-Host $host;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
```

If OAuth redirect URIs are generated with `http` instead of `https`, configure the proxy/Spring forwarded-header
strategy, for example:

```text
SERVER_FORWARD_HEADERS_STRATEGY=framework
```

## Database and Flyway

Flyway owns schema creation and migration. Migration scripts are stored in:

```text
src/main/resources/db/migration
```

Do not put ordinary seed/demo data into Flyway migrations unless the team explicitly agrees that it should run in every
environment.

For production deployment:

1. create or provision the target PostgreSQL database
2. set the datasource environment variables
3. start the backend
4. let Flyway apply schema migrations
5. import demo/catalog data only if the target environment needs it

## Demo Data Import

The repository includes sanitized demo data in:

```text
db-backup/dump2.sql
db-backup/init.sh
```

This dump is for public catalog/demo data only. It should not contain users, password hashes, OAuth identifiers,
verification codes, bookings, or payments.

Run after Flyway has created the schema:

```bash
sh ./db-backup/init.sh "postgresql://USER:PASSWORD@HOST:5432/cinebh"
```

Or provide standard PostgreSQL environment variables:

```bash
PGHOST=<host> PGPORT=5432 PGDATABASE=cinebh PGUSER=<user> PGPASSWORD=<password> sh ./db-backup/init.sh
```

The script waits for the Flyway schema and uses `ON CONFLICT DO NOTHING`, so it is safe to rerun for the same data set.

## Jenkins Pipeline

The deployment currently follows this model:

1. Jenkins has separate build jobs for the frontend and backend GitHub repositories.
2. Each job targets the branch selected for deployment.
3. Jenkins stores credentials/secrets that should not be committed to Git.
4. Backend and frontend Docker images are built from their repository Dockerfiles.
5. A deploy pipeline starts both applications in the same Docker network on the EC2 instance.
6. Nginx exposes the frontend and backend to the web and terminates HTTPS traffic.

Recommended Jenkins credential categories:

- database credentials
- JWT secret
- Google OAuth client ID/secret
- SMTP password
- keystore password or certificate material
- storage credentials
- server/SSH or registry credentials

## Deployment Verification

After deployment, verify:

- backend container is healthy
- PostgreSQL and Redis are reachable
- Flyway migrations completed successfully
- `/api/v1/currently-showing/filters` returns city, venue and genre options
- `/api/v1/upcoming-movies/filters` returns city, venue and genre options
- `/api/v1/movies/{movieId}/details` returns movie details, media, credits, filter metadata and recommendations
- `/api/v1/movies/{movieId}/projections` returns projection times for selected schedule filters
- venue filter options include `cityId`
- Swagger is reachable if enabled
- signup/login creates HttpOnly cookies
- Google login redirects back to `/login/oauth2/code/google`
- `/api/v1/auth/me` returns the authenticated user after login

## Troubleshooting

### Backend starts locally but browser cannot call it

Check:

- local hosts file entries
- backend certificate trust
- `VITE_API_BASE_URL`
- CORS allowed origins
- cookie domain and `SameSite` settings

### Google OAuth redirects to the wrong URI

Check:

- proxy `Host` and `X-Forwarded-Proto` headers
- Google Console redirect URI
- `APP_FRONTEND_BASE_URL`
- whether `/oauth2/**` and `/login/**` are routed to the backend

### `/me` returns 401 after login

Check:

- JWT secret consistency across instances
- cookie domain and secure settings
- access/refresh token cookies are present
- user exists in the deployed database

### No movie data appears after deployment

Check:

- the target database contains catalog/projection data
- Flyway migrations completed
- demo data import was run if needed
- currently showing and upcoming projections match the date filters used by the frontend
