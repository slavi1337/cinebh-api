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

```
com.cinebh.api.controllers          # REST controllers
com.cinebh.api.services             # Business logic layer
com.cinebh.api.repositories         # JPA repositories
com.cinebh.api.repositories.custom  # Querydsl custom repositories
com.cinebh.api.entities             # JPA entities
com.cinebh.api.dto                  # Request/response DTOs
com.cinebh.api.config               # Configuration (Swagger, Security, Storage)
com.cinebh.api.exceptions           # Global error handling
com.cinebh.api.security             # JWT & authentication
com.cinebh.api.services.storage     # Storage abstraction (S3-compatible)
com.cinebh.api.utils                # Utility classes
```

---

## Implemented Features

The backend currently provides public endpoints for:

- Homepage hero movies
- Currently showing movies
- Upcoming movies
- Venues listing
- Currently showing page with:
    - search
    - filters
    - pagination
- Filter metadata for movie listings
- Venue filtering based on selected city

---

## Configuration & Profiles

The application uses **Spring Profiles** to manage environment-specific settings:

- **`local` (Default):** Targetted for local development. Connects to a local PostgreSQL instance.
- **`prod`:** Optimized for deployment. Uses environment variables for sensitive credentials.

## Database & Migrations

Database schema is managed by **Flyway**. Upon application startup, Flyway automatically applies migration scripts
located in `src/main/resources/db/migration`.

- **Initial Schema (V1):** Implements a highly normalized relational model with UUIDs, composite unique indexes for
  concurrency control, and partial indexes to support a full audit trail for bookings.

## Getting Started

### Prerequisites

- JDK 21
- PostgreSQL 17
- *(Optional)* Docker + MinIO

---

## Local Setup

1. **Clone the repository.**
2. **Create the database:** Execute `CREATE DATABASE cinebh;` in your PostgreSQL instance.
3. **Configure Local Environment:** Open `src/main/resources/application-local.yml` and update the `username` and
   `password` fields with your local PostgreSQL credentials.
4. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```
5. **Access Swagger UI at:** http://localhost:8080/swagger-ui.html

---

### Storage (S3 / MinIO)

If using local S3-compatible storage:

- Ensure the service is running
- Ensure the configured bucket exists

---
