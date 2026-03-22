# CineBH - Backend API

CineBH is a modern, web-based ticketing application built for a cinema theater chain with multiple subsidiaries. This repository contains the Spring Boot REST API, providing a robust backend for movie discovery, seat management, secure bookings, and payment integration.

## Tech Stack
- **Framework:** Spring Boot 4.0.4
- **Language:** Java 21 (LTS)
- **Build Tool:** Maven
- **Database:** PostgreSQL 17
- **Migrations:** Flyway
- **Security:** Spring Security & JWT
- **Documentation:** Swagger UI / OpenAPI 3
- **Payments:** Stripe Java SDK

## Project Architecture
The project follows a standard layered architecture to ensure separation of concerns and maintainability:

- `com.cinebh.api.controllers` - REST Controllers for handling incoming HTTP requests.
- `com.cinebh.api.services` - Business logic layer.
- `com.cinebh.api.repositories` - Data access layer using Spring Data JPA.
- `com.cinebh.api.models` - JPA Entities representing the database schema.
- `com.cinebh.api.dto` - Data Transfer Objects for API requests and responses.
- `com.cinebh.api.config` - Configuration classes (Swagger, CORS, Security).
- `com.cinebh.api.exceptions` - Global error handling and custom exceptions.
- `com.cinebh.api.security` - JWT filters and authentication logic.

## Configuration & Profiles
The application uses **Spring Profiles** to manage environment-specific settings:

- **`dev` (Default):** Targetted for local development. Connects to a local PostgreSQL instance.
- **`prod`:** Optimized for deployment. Uses environment variables for sensitive credentials.

### Environment Variables
Sensitive data is managed via a `.env` file (locally) or server environment variables (production). See `.env.example` for the required keys:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `STRIPE_SECRET_KEY`
- `JWT_SECRET_KEY`

## Database & Migrations
Database schema is managed by **Flyway**. Upon application startup, Flyway automatically applies migration scripts located in `src/main/resources/db/migration`.

- **Initial Schema (V1):** Implements a highly normalized relational model with UUIDs, composite unique indexes for concurrency control, and partial indexes to support a full audit trail for bookings.

## Getting Started

### Prerequisites
- JDK 21 installed.
- PostgreSQL 17 server running.
- Maven 3.9+ (or use the included `./mvnw`).

### Local Setup
1. **Clone the repository.**
2. **Create the database:** Execute `CREATE DATABASE cinebh;` in your PostgreSQL instance.
3. **Configure Local Environment:** Open `src/main/resources/application-dev.yml` and update the `username` and `password` fields with your local PostgreSQL credentials.
4. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
5. **Access Swagger UI at:** http://localhost:8080/swagger-ui.html