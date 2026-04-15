# CineBH - Backend API

CineBH is a modern, web-based ticketing application built for a cinema theater chain with multiple subsidiaries. This
repository contains the Spring Boot REST API, providing backend support for homepage content, currently showing
listings, movie schedules, bookings, and future payment integration.

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
- Currently Showing listing with:
    - search by title
    - filters by city, cinema, genre and projection time
    - date-based schedule view
    - load more pagination
    - filter metadata endpoints
    - venue filtering based on selected city
- Upcoming Movies listing with:
    - search by title
    - filters by city, cinema, genre and date range
    - load more pagination
    - filter metadata endpoints
    - venue filtering based on selected city

---

## Configuration & Profiles

The application uses **Spring Profiles** to manage environment-specific settings:

- **`local` (Default):** Targetted for local development. Connects to a local PostgreSQL instance.
- **`prod`:** Optimized for deployment. Uses environment variables for sensitive credentials.

You can activate a profile by changing the active profile in `application.yml` under the section spring_profiles_active.
See the example below:

`active: local` - for local development

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
- Storage endpoint, region, bucket, access and secret key

### Example Local Configuration

Adjust the values to match your local machine.

Check `application-local.yml` for local config and edit configuration in `application.yml`.

**Note:** keep actual secrets out of version control.

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
http://localhost:8080/swagger-ui/index.html
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
- future projections
- currently showing schedules

## Local Testing Tips

To properly test listing pages and filters:

- seed enough currently showing movies and projections
- seed enough upcoming movies and future projections
- make sure multiple cinemas exist across different cities
- verify that filter metadata endpoints return meaningful values

## Deployment Notes for DevOps

For deployment environments, ensure the following are configured:

- active Spring profile
- datasource URL, username, password
- JWT secret
- Stripe keys
- storage endpoint / region / bucket / credentials
- any reverse proxy / CORS rules if required
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
3. Configure local application properties / environment variables
4. Start MinIO if file uploads are needed
5. Run the backend with `./mvnw spring-boot:run`
6. Let Flyway run migrations automatically
7. Open Swagger and test endpoints
