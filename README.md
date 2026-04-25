# Techstars Job Parser

Spring Boot application that scrapes job listings from `https://jobs.techstars.com/jobs`, stores them in PostgreSQL, and exposes REST endpoints for querying jobs, filters, and scrape run history.

## Features

- Scrapes Techstars job cards with Jsoup.
- Stores companies, jobs, tags, and scrape run statistics in SQL via Spring Data JPA.
- Uses Flyway for database schema creation.
- Synchronizes repeated scrapes:
  - creates new jobs;
  - updates existing jobs;
  - skips invalid or duplicate scraped records;
  - marks missing jobs as inactive.
- Provides paginated and filterable job API.
- Provides reference endpoints for companies and tags.
- Provides manual and scheduled scraping.
- Exposes Swagger/OpenAPI documentation.

## Main Endpoints

- `GET /api/jobs` - paginated jobs list.
- `GET /api/jobs/{id}` - job details.
- `GET /api/companies` - companies sorted by name.
- `GET /api/tags` - tags sorted by name.
- `POST /api/scrape-runs` - run scraping manually.
- `GET /api/scrape-runs` - scrape run history.
- `GET /v3/api-docs` - OpenAPI JSON.
- `GET /swagger-ui.html` - Swagger UI.

## Example Requests

```bash
curl "http://localhost:8080/api/jobs?page=0&size=20"
curl "http://localhost:8080/api/jobs?q=java&location=remote&tag=spring"
curl "http://localhost:8080/api/jobs?active=false"
curl "http://localhost:8080/api/companies"
curl "http://localhost:8080/api/tags"
curl -X POST "http://localhost:8080/api/scrape-runs"
curl "http://localhost:8080/api/scrape-runs"
```

## Tech Stack

- Java 25
- Spring Boot 4
- Maven
- Spring Web MVC
- Spring Data JPA
- Flyway
- PostgreSQL 18
- Jsoup
- Lombok
- Springdoc OpenAPI
- H2 for tests

See [INSTALL.md](INSTALL.md) for local setup and run instructions.
