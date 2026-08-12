# Cinebh API

Spring Boot backend for Cinebh, a cinema browsing and ticket booking application.

The matching React frontend is available in the
[Cinebh Web repository](https://github.com/slavi1337/cinebh-web).

## Features

### Movies and projections

- Homepage, movie details, currently showing and upcoming movie endpoints
- Search, pagination and filters by city, cinema, genre, date and projection time
- Movie credits, media gallery, ratings, related movies and grouped showtimes
- QueryDSL repositories for dynamic listing and filter queries

### Authentication and profiles

- Registration, email verification and email/password login
- Google OAuth login
- JWT access and refresh tokens stored in HttpOnly cookies
- Remember-me sessions, refresh, logout and password-change session invalidation
- Failed-login rate limiting by email and IP using Redis
- Profile editing, password change, account deactivation and avatar storage
- Purchased projection history and active reservations

### Booking and payments

- Projection seat map with regular, love and VIP pricing
- Five-minute seat-selection hold that is not reset by later seat changes
- Seat availability validation and database locking for concurrent requests
- Projection-specific WebSocket events after committed seat changes
- Additional five-minute payment window when Stripe Checkout is created
- Reservations valid until one hour before the projection
- Scheduled expiration of unpaid holds and reservations
- Stripe Checkout and signed webhook processing
- Payment confirmation email, ticket code and QR code generation
- Public ticket-code validation endpoint used by the QR confirmation page

## Tech Stack

- Java 21
- Spring Boot and Spring Security
- Spring Data JPA, Hibernate and QueryDSL
- PostgreSQL 17 and Flyway
- Redis
- Spring WebSocket
- Stripe Java SDK
- AWS SDK for S3-compatible storage
- Thymeleaf and SMTP email
- JUnit 5, Mockito and JaCoCo

## Local Setup

### Requirements

- JDK 21
- PostgreSQL 17
- Redis 7
- MinIO or another S3-compatible storage service
- Stripe test account
- SMTP credentials for verification and booking emails

Clone the repository:

```bash
git clone https://github.com/slavi1337/cinebh-api.git
cd cinebh-api
```

Create a PostgreSQL database named `cinebh` and start PostgreSQL, Redis and MinIO. The default local configuration uses:

```text
PostgreSQL: localhost:5432/cinebh
Redis:      localhost:6379
MinIO:      localhost:9000
```

The following local database setup matches `application-local.yml`:

```sql
CREATE USER cinebh_app WITH PASSWORD 'cinebh123';
CREATE DATABASE cinebh OWNER cinebh_app;
```

Create a MinIO bucket named `cinebh`. The local profile uses `minioadmin` for both the access key and secret key.

Set the required environment variables before starting the application:

```env
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
JWT_SECRET=<long-random-secret>
STRIPE_SECRET_KEY=<stripe-test-secret-key>
STRIPE_WEBHOOK_SECRET=<stripe-cli-webhook-secret>
SMTP2GO_PASSWORD=<smtp-password>
KEYSTORE_PASSWORD=<local-keystore-password>
```

The local profile serves HTTPS on `https://api.cinebh.com:8443`. Add these entries to the hosts file:

```text
127.0.0.1 cinebh.com
127.0.0.1 api.cinebh.com
```

Generate the local PKCS12 certificate before the first run. The command and Google OAuth callback setup are documented
in [DEPLOYMENT.md](DEPLOYMENT.md#local-https-development).

Run the API:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Swagger UI is available at `https://api.cinebh.com:8443/swagger-ui.html`.

The `local` profile is active by default. Set `SPRING_PROFILES_ACTIVE=prod` only when all production environment
variables are configured.

## Database

Flyway owns the schema and runs migrations from `src/main/resources/db/migration` on startup. Demo content is kept out
of Flyway.

After Flyway creates the schema, the tracked catalog dump can be imported manually:

```bash
sh ./db-backup/init.sh "postgresql://cinebh_app:cinebh123@localhost:5432/cinebh"
```

The dump contains demo catalog data such as movies, cinemas, halls, seats and projections. It does not contain users,
bookings, payments or authentication tokens. Projection dates are fixed, so they may need to be refreshed for a new
demo deployment.

## Stripe Webhooks

For local testing, forward Stripe events to the API:

```bash
stripe listen --forward-to https://api.cinebh.com:8443/api/v1/payments/stripe/webhook --skip-verify
```

Use the `whsec_...` value printed by the Stripe CLI as `STRIPE_WEBHOOK_SECRET`. A deployed environment must use the
secret belonging to its Dashboard webhook endpoint instead.

The webhook, not the browser redirect, confirms payment and issues the ticket.

## Tests

Run the complete test suite with:

```bash
./mvnw test
```

Create the application JAR with:

```bash
./mvnw clean package
```

JaCoCo reports are generated under `target/site/jacoco`.

## Project Structure

```text
src/main/java/com/cinebh/api/
  config/         application and integration configuration
  controllers/    REST controllers
  dto/            request and response contracts
  entities/       JPA entities and enums
  exceptions/     API errors and global exception handling
  mappers/        response mapping
  repositories/   JPA and QueryDSL data access
  security/       JWT and OAuth security
  services/       application and integration logic
  websocket/      projection seat events
```

## Production Configuration

The `prod` profile expects database, Redis, JWT, OAuth, Stripe, SMTP, cookie, frontend and S3 settings through environment
variables. Secrets must not be committed to the repository.

Public deployments should terminate TLS at a reverse proxy, forward `Host` and `X-Forwarded-Proto`, keep PostgreSQL and
Redis private, and expose the Stripe webhook over HTTPS. Google OAuth origins and callback URLs must exactly match the
public application domain.

See [DEPLOYMENT.md](DEPLOYMENT.md) for the full environment-variable, reverse-proxy, OAuth and local HTTPS reference.
Demo import details are available in [db-backup/README.md](db-backup/README.md).

## License

No open-source license is currently included in this repository.
