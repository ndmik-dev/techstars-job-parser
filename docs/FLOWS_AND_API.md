# Runtime Flows and API Guide

This document explains the main runtime flows and how to use the API.

## Local Runtime

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run the app:

```bash
mvn spring-boot:run
```

Base URL:

```text
http://localhost:8080
```

PostgreSQL:

```text
localhost:5433
```

## End-to-End Scrape Flow

Trigger:

```bash
curl -X POST "http://localhost:8080/api/scrape-runs"
```

Expected response:

```json
{
  "id": 1,
  "startedAt": "2026-04-25T13:11:55.741850Z",
  "finishedAt": "2026-04-25T13:11:56.665171Z",
  "status": "COMPLETED",
  "retrievedCount": 20,
  "createdCount": 20,
  "updatedCount": 0,
  "deactivatedCount": 0,
  "errorMessage": null
}
```

What happens internally:

1. `ScrapeRunController.startScrape()` receives the request.
2. It calls `ScrapeService.scrapeAndSync()`.
3. `ScrapeService` ensures no other scrape is running.
4. `TechstarsJobsClient` downloads the Techstars jobs page.
5. `TechstarsJobsParser` converts HTML into `ScrapedJob` records.
6. `JobSyncService` saves new data and updates old data.
7. `ScrapeRun` is completed with counters.
8. The controller returns the final scrape run response.

## Repeated Scrape Behavior

When a scrape runs again:

- same `externalId` means the job is updated, not duplicated;
- company and tags are reused where possible;
- tags are replaced with the current scraped tag set;
- jobs missing from the latest non-empty scrape are marked `active=false`;
- empty scrape result does not deactivate existing jobs.

## Job List API

Endpoint:

```text
GET /api/jobs
```

Basic request:

```bash
curl "http://localhost:8080/api/jobs?page=0&size=20"
```

Response shape:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

Supported query parameters:

| Parameter | Meaning |
| --- | --- |
| `page` | zero-based page index |
| `size` | page size |
| `sort` | Spring sort expression, for example `lastSeenAt,desc` |
| `q` | search in title, description, and company name |
| `location` | location contains text |
| `company` | company name contains text |
| `tag` | tag name contains text |
| `remoteType` | remote type contains text |
| `seniority` | seniority contains text |
| `active` | `true` by default; use `false` for inactive jobs |

Examples:

```bash
curl "http://localhost:8080/api/jobs?q=java"
curl "http://localhost:8080/api/jobs?location=remote"
curl "http://localhost:8080/api/jobs?company=memgraph"
curl "http://localhost:8080/api/jobs?tag=software"
curl "http://localhost:8080/api/jobs?active=false"
curl "http://localhost:8080/api/jobs?page=0&size=10&sort=lastSeenAt,desc"
```

## Job Details API

Endpoint:

```text
GET /api/jobs/{id}
```

Example:

```bash
curl "http://localhost:8080/api/jobs/1"
```

If a job does not exist, the API returns `404`.

## Reference Data APIs

Companies:

```bash
curl "http://localhost:8080/api/companies"
```

Tags:

```bash
curl "http://localhost:8080/api/tags"
```

These endpoints are useful for building UI filters.

## Scrape Run History API

Endpoint:

```text
GET /api/scrape-runs
```

Example:

```bash
curl "http://localhost:8080/api/scrape-runs?page=0&size=10"
```

Response contains page metadata and scrape run rows.

## Concurrent Scrape Protection

If one scrape is already running and another request tries to start a scrape:

```text
HTTP 409 Conflict
```

This is controlled by `ScrapeService` with an `AtomicBoolean`.

## Scheduled Scraping

Scheduling is configured but disabled by default.

Default configuration:

```yaml
techstars:
  scraper:
    scheduling-enabled: false
    cron: "0 0 */6 * * *"
```

Enable:

```yaml
techstars:
  scraper:
    scheduling-enabled: true
```

The scheduled runner calls the same `ScrapeService` as the manual endpoint, so concurrency protection is shared.

## Swagger/OpenAPI

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## Database Dump

A verified dump artifact is stored at:

```text
techstars_jobs_dump.sql
```

Create a new dump:

```bash
docker compose exec -T postgres pg_dump -U techstars_user techstars_jobs > techstars_jobs_dump.sql
```

Restore:

```bash
docker compose exec -T postgres psql -U techstars_user techstars_jobs < techstars_jobs_dump.sql
```

## Verification Checklist

Run tests:

```bash
mvn test
```

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Start app:

```bash
mvn spring-boot:run
```

Check API:

```bash
curl "http://localhost:8080/api/jobs"
curl "http://localhost:8080/v3/api-docs"
```

Trigger scrape:

```bash
curl -X POST "http://localhost:8080/api/scrape-runs"
```

Confirm jobs:

```bash
curl "http://localhost:8080/api/jobs?size=3"
```

