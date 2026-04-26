# Techstars Job Parser

Spring Boot service that scrapes jobs from `https://jobs.techstars.com/jobs`, stores them in PostgreSQL, and exposes a REST API for jobs, filters, and scrape history.

## What Is Implemented

- Maven-based Spring Boot 4 application.
- PostgreSQL 18 storage with Flyway migrations.
- Jsoup-based Techstars scraper.
- Job synchronization: create, update, skip duplicates, mark missing jobs inactive.
- REST API with pagination and filters.
- Manual and optional scheduled scraping.
- Swagger/OpenAPI.
- SQL dump with sample scraped data: `techstars_jobs_dump.sql`.

## Main API

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/jobs` | Paginated jobs with filters |
| `GET` | `/api/jobs/{id}` | Job details |
| `GET` | `/api/companies` | Company filter values |
| `GET` | `/api/tags` | Tag filter values |
| `POST` | `/api/scrape-runs` | Run scraper manually |
| `GET` | `/api/scrape-runs` | Scrape history |
| `GET` | `/swagger-ui.html` | Swagger UI |

## Useful Docs

- [INSTALL.md](INSTALL.md) - step-by-step launch and verification.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) - architecture overview.
- [docs/CODE_WALKTHROUGH.md](docs/CODE_WALKTHROUGH.md) - code structure guide.
- [docs/FLOWS_AND_API.md](docs/FLOWS_AND_API.md) - runtime flows and API examples.
