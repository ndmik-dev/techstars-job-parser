# Install and Verify

## Prerequisites

- Java 25
- Maven 3.9+
- Docker Desktop or Docker Engine

## 1. Start PostgreSQL

```bash
docker compose up -d postgres
```

Verify that PostgreSQL is mapped to host port `5433`:

```bash
docker compose ps
```

Expected port mapping:

```text
0.0.0.0:5433->5432/tcp
```

Database config used by the app:

```text
jdbc:postgresql://localhost:5433/techstars_jobs
user: techstars_user
password: techstars_password
```

## 2. Run Tests

Tests use H2, so they do not require PostgreSQL.

```bash
mvn test
```

Expected result:

```text
BUILD SUCCESS
```

## 3. Start the Application

```bash
mvn spring-boot:run
```

Expected startup result:

```text
Tomcat started on port 8080
Started TechstarsJobParserApplication
```

Flyway creates the database schema automatically.

## 4. Verify Basic API

In another terminal:

```bash
curl "http://localhost:8080/api/jobs"
```

Expected result before scraping:

```json
{"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0,"first":true,"last":true}
```

Verify Swagger:

```bash
curl -I "http://localhost:8080/swagger-ui.html"
```

Expected result:

```text
HTTP/1.1 200
```

## 5. Run Scraper

```bash
curl -X POST "http://localhost:8080/api/scrape-runs"
```

Expected result:

HTTP `201` with `"status":"COMPLETED"` and positive `retrievedCount`.

Exact counts can change if Techstars changes the page.

## 6. Verify Scraped Data

```bash
curl "http://localhost:8080/api/jobs?size=3"
```

Expected result:

- HTTP `200`
- non-empty `content`
- `totalElements` greater than `0`

Check filters:

```bash
curl "http://localhost:8080/api/jobs?q=software"
curl "http://localhost:8080/api/jobs?location=United"
curl "http://localhost:8080/api/jobs?tag=Software"
curl "http://localhost:8080/api/companies"
curl "http://localhost:8080/api/tags"
```

Check scrape history:

```bash
curl "http://localhost:8080/api/scrape-runs"
```

Expected result:

- at least one run
- latest run has `status: COMPLETED`

## 7. Create Fresh Dump

After a successful scrape:

```bash
docker compose exec -T postgres pg_dump -U techstars_user techstars_jobs > techstars_jobs_dump.sql
```

Restore dump:

```bash
docker compose exec -T postgres psql -U techstars_user techstars_jobs < techstars_jobs_dump.sql
```

## 8. Stop Services

Stop only containers:

```bash
docker compose down
```

Stop containers and remove database volume:

```bash
docker compose down -v
```

## Optional: Scheduled Scraping

Scheduled scraping is disabled by default:

```yaml
techstars.scraper.scheduling-enabled: false
```

Enable it in `application.yaml`:

```yaml
techstars:
  scraper:
    scheduling-enabled: true
    cron: "0 0 */6 * * *"
```
