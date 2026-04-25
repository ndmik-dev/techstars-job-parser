# Installation and Run Guide

## Requirements

- Java 25
- Maven 3.9+
- Docker Desktop or Docker Engine

## Database

The project uses PostgreSQL 18 from `compose.yaml`.

Container settings:

- image: `postgres:18-alpine`
- database: `techstars_jobs`
- user: `techstars_user`
- password: `techstars_password`
- host port: `5433`
- container port: `5432`
- volume: `postgres_18_data`

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Check the mapped port:

```bash
docker compose ps
```

The application expects this JDBC URL:

```text
jdbc:postgresql://localhost:5433/techstars_jobs
```

## Run Tests

Tests use H2 and do not require the PostgreSQL container.

```bash
mvn test
```

## Run Application

Start PostgreSQL first:

```bash
docker compose up -d postgres
```

Run the Spring Boot app:

```bash
mvn spring-boot:run
```

The app starts on:

```text
http://localhost:8080
```

Flyway creates the schema automatically on startup.

## Trigger Scraping

Manual scrape:

```bash
curl -X POST "http://localhost:8080/api/scrape-runs"
```

Scrape history:

```bash
curl "http://localhost:8080/api/scrape-runs"
```

## Query Jobs

```bash
curl "http://localhost:8080/api/jobs?page=0&size=20"
curl "http://localhost:8080/api/jobs?q=java"
curl "http://localhost:8080/api/jobs?location=remote"
curl "http://localhost:8080/api/jobs?company=techstars"
curl "http://localhost:8080/api/jobs?tag=spring"
curl "http://localhost:8080/api/jobs?active=false"
```

Reference data:

```bash
curl "http://localhost:8080/api/companies"
curl "http://localhost:8080/api/tags"
```

## Swagger

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## Scheduled Scraping

Scheduled scraping is disabled by default:

```yaml
techstars:
  scraper:
    scheduling-enabled: false
    cron: "0 0 */6 * * *"
```

To enable it, set:

```yaml
techstars:
  scraper:
    scheduling-enabled: true
```

Default cron runs every 6 hours.

## Database Dump

After running at least one scrape, create a dump with:

```bash
docker compose exec postgres pg_dump -U techstars_user techstars_jobs > techstars_jobs_dump.sql
```

Restore example:

```bash
docker compose exec -T postgres psql -U techstars_user techstars_jobs < techstars_jobs_dump.sql
```

## Stop Services

Stop the database container:

```bash
docker compose down
```

Stop and remove the PostgreSQL volume:

```bash
docker compose down -v
```
