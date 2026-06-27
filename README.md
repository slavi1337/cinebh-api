# Cinebh - Backend API

Cinebh is a modern, web-based ticketing application built for a cinema theater chain with multiple subsidiaries. This
repository contains the Spring Boot REST API, providing backend support for homepage content, currently showing
listings, movie schedules, bookings, and future payment integration.

For deployment, Jenkins, Docker Compose, EC2, TLS, OAuth and database import notes, see
[DEPLOYMENT.md](DEPLOYMENT.md).

---

## Tech Stack

- **Framework:** Spring Boot 4.0.4
- **Language:** Java 21 (LTS)
- **Build Tool:** Maven
- **Database:** PostgreSQL 17
- **Migrations:** Flyway
- **ORM / Querying:** Spring Data JPA + Querydsl
- **Security:** Spring Security & JWT
- **Documentation:** Swagger UI / OpenAPI 3
- **Storage:** S3-compatible storage abstraction
- **Payments:** Stripe Java SDK

---

## Project Architecture

The project follows a layered architecture to ensure separation of concerns and maintainability:

- `com.cinebh.api.controllers` - REST controllers for handling HTTP requests
- `com.cinebh.api.services` - business logic layer
- `com.cinebh.api.services.storage` - Storage abstraction (S3 compatible)
- `com.cinebh.api.repositories` - data access layer using Spring Data JPA
- `com.cinebh.api.repositories.custom` - custom Querydsl repositories
- `com.cinebh.api.entities` - JPA entities mapped to the database schema
- `com.cinebh.api.dto` - request and response DTOs
- `com.cinebh.api.config` - configuration classes for security, OpenAPI, storage, etc.
- `com.cinebh.api.exceptions` - global exception handling and custom exceptions
- `com.cinebh.api.security` - JWT and authentication
- `com.cinebh.api.utils` - Utility classes
- `src/main/resources/db/migration` - Flyway migration scripts

---

## Implemented Features

The backend currently provides public endpoints for:

- Homepage:
    - Hero movie section
    - Currently showing movies
    - Upcoming movies
    - Venues listing
- Movie Details:
    - detailed movie metadata, synopsis, ratings and media
    - cast, directors and writers
    - available movie-specific cities, cinemas and projection dates
    - projection times filtered by date, city and cinema
    - see also recommendations
- Authentication:
    - email/password signup and login
    - email verification
    - Google OAuth login
    - JWT authentication through HttpOnly cookies
    - refresh and logout endpoints
- Currently Showing listing with:
    - search by title
    - filters by city, cinema, genre and projection time
    - date-based schedule view
    - load more pagination
    - filter metadata endpoints
    - venue filtering based on selected city
    - venue filter options include `cityId` for frontend city/cinema synchronization
- Upcoming Movies listing with:
    - search by title
    - filters by city, cinema, genre and date range
    - load more pagination
    - filter metadata endpoints
    - venue filtering based on selected city
    - venue filter options include `cityId` for frontend city/cinema synchronization

---

## Configuration & Profiles

The application uses **Spring Profiles** to manage environment-specific settings:

- **`local` (Default):** Targeted for local development. Connects to a local PostgreSQL instance.
- **`prod`:** Optimized for deployment. Uses environment variables for sensitive credentials.

You can activate a profile by changing the active profile in `application.yml` under the section spring_profiles_active.
See the example below:

`active: local` - for local development with HTTPS on port `8443`

`active: prod` - for production

---

## Database & Migrations

Database schema is managed by **Flyway**. Upon application startup, Flyway automatically applies migration scripts
located in `src/main/resources/db/migration`.

- **Initial Schema (V1):** Implements a highly normalized relational model with UUIDs, composite unique indexes for
  concurrency control, and partial indexes to support a full audit trail for bookings.

## Getting Started

### Prerequisites

**Before running the backend, install and configure the following:**

- JDK 21
- Maven Wrapper is included in the project, so standalone Maven is optional
- PostgreSQL 17
- MinIO *(+ Docker)* or AWS S3-compatible storage if file uploads are used

**Recommended tools:**

- IntelliJ IDEA
- Postman for API testing
- pgAdmin for database inspection

---

## Repository Setup

Clone the repository and enter the project directory.

```bash
git clone https://github.com/slavi1337/cinebh-api.git
cd cinebh-api
```

## Configuration

The application requires environment-specific configuration in `application-local.yml`, `application-prod.yml`.

### Minimum Local Configuration

Typical local values include:

- PostgreSQL connection URL
- PostgreSQL username and password
- Redis host and port
- Google OAuth client ID and secret
- JWT secret
- keystore password
- SMTP password for verification emails
- Storage endpoint, region, bucket, access and secret key

### Example Local Configuration

Adjust the values to match your local machine.

Check `application-local.yml` for local config and edit configuration in `application.yml`.

**Note:** keep actual secrets out of version control.

Required local environment variables:

```text
JWT_SECRET=<long-random-secret>
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
KEYSTORE_PASSWORD=<local-keystore-password>
SMTP2GO_PASSWORD=<smtp-password>
```

Depending on your shell/IDE, Spring placeholders can also be provided as `keystore.password` and `smtp2go.password`.

## Local HTTPS Setup

Local development uses HTTPS and custom local hostnames so auth cookies and Google OAuth can be tested realistically.

Add these entries to your hosts file:

```text
127.0.0.1 cinebh.com
127.0.0.1 api.cinebh.com
```

The backend runs on:

```text
https://api.cinebh.com:8443
```

The frontend runs on:

```text
https://cinebh.com:5173
```

The local profile expects a PKCS12 keystore at:

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

Set `KEYSTORE_PASSWORD` to the same password.

## Google OAuth Setup

Spring Security uses standard OAuth routes:

```text
/oauth2/authorization/google
/login/oauth2/code/google
```

The OAuth callback is intentionally not under `/api/v1`.

For local Google Console configuration:

Authorized JavaScript origin:

```text
https://cinebh.com:5173
```

Authorized redirect URI:

```text
https://api.cinebh.com:8443/login/oauth2/code/google
```

For deployment, the redirect URI must match the public domain that Google sees, for example:

```text
https://cinebhapp.praksa.abhapp.com/login/oauth2/code/google
```

## Database Setup

Create the database manually before starting the application.

```sql
CREATE DATABASE cinebh;
```

Ensure PostgreSQL is running and that your configured user has permission to access the database.

## Running Database Migrations

Flyway migrations are executed automatically on application startup.

Migration scripts are stored in:

```text
src/main/resources/db/migration
```

If Flyway fails, inspect:

- migration ordering
- already applied migrations
- enum/type conflicts in PostgreSQL
- schema mismatches between entities and migration scripts

## Running the Application

Start the backend locally with:

```bash
./mvnw spring-boot:run
```

To run with a specific profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

To build the project:

```bash
./mvnw clean install
```

To run the packaged JAR:

```bash
java -jar target/api-0.0.1-SNAPSHOT.jar
```

## Swagger / OpenAPI

Once the application is running locally, API documentation is available at:

```text
https://api.cinebh.com:8443/swagger-ui.html
```

## Storage Setup (MinIO / S3)

If the project uses image/file uploads, object storage must be configured.

### MinIO Local Setup

1. Start MinIO locally
2. Create the required bucket, for example `cinebh`
3. Configure:
    - `storage.endpoint`
    - `storage.region`
    - `storage.bucket`
    - `storage.access-key`
    - `storage.secret-key`
    - `storage.path-style-access-enabled`
    - `storage.presign-endpoint`
    - `storage.presigned-url-ttl`

Profile images are served through short-lived S3 presigned URLs. The presign endpoint must be reachable from the
browser. For local MinIO this is normally `http://localhost:9000`; in production it must be the public S3/MinIO
endpoint exposed by the reverse proxy. The URL host must not be rewritten after signing because that invalidates the
signature.

## Docker Compose Local Stack

The repository includes a `docker-compose.yml` that can run the backend together with:

- PostgreSQL 17
- Redis 7
- MinIO
- frontend from `../cinebh-web`

Expected local folder layout:

```text
parent-folder/
  cinebh-api/
  cinebh-web/
```

Run from the backend repository:

```bash
docker compose up --build
```

The backend waits for PostgreSQL and Redis healthchecks before starting.

## Demo Data Import

Sanitized public demo/catalog data is stored under:

```text
db-backup/
```

The dump intentionally excludes users, password hashes, OAuth identifiers, verification codes, bookings and payments.

Flyway should create the schema first. Import demo data afterwards:

```bash
sh ./db-backup/init.sh "postgresql://USER:PASSWORD@HOST:5432/cinebh"
```

See `db-backup/README.md` for details.

## Environment Differences

### Local

- usually uses local PostgreSQL
- usually uses MinIO for storage
- usually uses test Stripe keys
- easier debugging and direct DB access

### Production

- should use environment variables or secret managers
- should use production Stripe keys
- should use production-grade database and storage services
- logging, monitoring, and deployment configuration should be handled by DevOps

## Useful API Areas for QA / DevOps

### Movie Details

Supports:

- movie details by movie ID
- movie media, cast, directors, writers and ratings
- movie-specific city and cinema filter metadata
- projection dates
- projection times filtered by selected date, city and venue
- see also recommendations

Main endpoints:

```text
GET /api/v1/movies/{movieId}/details
GET /api/v1/movies/{movieId}/projections
```

### Currently Showing

Supports:

- movie search
- city filter
- cinema filter
- genre filter
- projection time filter
- date-based schedule
- load more pagination

### Upcoming Movies

Supports:

- movie search
- city filter
- cinema filter
- genre filter
- date range filter
- load more pagination

DevOps/QA should ensure the target environment has enough seeded data to test:

- multiple cities
- multiple venues
- multiple genres
- movies with cast, directors, writers and media assets
- future projections
- currently showing schedules

## Local Testing Tips

To properly test listing pages and filters:

- seed enough currently showing movies and projections
- seed enough upcoming movies and future projections
- seed movies with details data, media, credits and see also candidates
- make sure multiple cinemas exist across different cities
- verify that filter metadata endpoints return meaningful values

## Deployment Notes for DevOps

Detailed deployment notes are maintained in [DEPLOYMENT.md](DEPLOYMENT.md).

For deployment environments, ensure the following are configured:

- active Spring profile
- datasource URL, username, password
- Redis connection
- JWT secret
- Google OAuth client ID and secret
- Stripe keys
- storage endpoint / region / bucket / credentials
- frontend base URL
- cookie domain, secure flag and SameSite policy
- CORS allowed origins
- reverse proxy routes for `/api/**`, `/oauth2/**`, `/login/**`, `/swagger-ui/**`, and `/v3/api-docs/**`
- forwarded `Host` and `X-Forwarded-Proto` headers for OAuth redirects
- database is reachable before app startup
- migration strategy is defined and safe for the target environment

## Recommended README / Docs Maintenance

Whenever new infrastructure-relevant functionality is added, update documentation with:

- new environment variables
- new external services
- new setup requirements
- new migration requirements
- new operational notes

## Quick Start Summary

1. Install JDK 21 and PostgreSQL 17
2. Create the `cinebh` database
3. Add local host mappings for `cinebh.com` and `api.cinebh.com`
4. Configure local environment variables
5. Start Redis and MinIO if needed
6. Run the backend with `./mvnw spring-boot:run`
7. Let Flyway run migrations automatically
8. Open Swagger and test endpoints
