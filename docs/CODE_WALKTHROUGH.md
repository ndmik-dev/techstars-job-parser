# Code Walkthrough

This document explains what each important class does and how the pieces fit together.

## Entry Point

### `TechstarsJobParserApplication`

Path:

```text
src/main/java/io/ndmik/tsparser/TechstarsJobParserApplication.java
```

Responsibilities:

- starts the Spring Boot application;
- enables configuration properties scanning;
- enables Spring scheduling.

Important annotations:

- `@SpringBootApplication`
- `@ConfigurationPropertiesScan`
- `@EnableScheduling`

## Configuration

### `TechstarsScraperProperties`

Path:

```text
src/main/java/io/ndmik/tsparser/config/TechstarsScraperProperties.java
```

Binds these properties:

```yaml
techstars:
  scraper:
    jobs-url: https://jobs.techstars.com/jobs
    timeout: 10s
    user-agent: ...
    scheduling-enabled: false
    cron: "0 0 */6 * * *"
```

Used by:

- `TechstarsJobsClient`
- `ScheduledScrapeRunner`

### `OpenApiConfig`

Path:

```text
src/main/java/io/ndmik/tsparser/config/OpenApiConfig.java
```

Defines OpenAPI metadata:

- title;
- version;
- description.

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

## Domain Model

### `Company`

Represents a company from Techstars.

Important fields:

- `id`
- `name`
- `sourceUrl`
- `createdAt`
- `updatedAt`

The company name is unique in the database.

### `Job`

Represents a job listing.

Important fields:

- `externalId` - stable unique identifier for deduplication;
- `title`
- `location`
- `description`
- `sourceUrl`
- `remoteType`
- `seniority`
- `salaryText`
- `postedAtText`
- `active`
- `company`
- `tags`
- `firstSeenAt`
- `lastSeenAt`

Important methods:

- `updateDetails(...)`
  - updates job data from a scraped record.
- `replaceTags(Set<Tag>)`
  - replaces the full tag set for a job.
- `deactivate()`
  - marks the job inactive.

### `Tag`

Represents a job tag.

Important fields:

- `id`
- `name`

The tag name is unique.

### `ScrapeRun`

Represents one scraper execution.

Important fields:

- `startedAt`
- `finishedAt`
- `status`
- `retrievedCount`
- `createdCount`
- `updatedCount`
- `deactivatedCount`
- `errorMessage`

Important methods:

- `started()`
  - creates a new run with status `RUNNING`.
- `complete(...)`
  - stores final counters and status `COMPLETED`.
- `fail(...)`
  - stores error message and status `FAILED`.

### `ScrapeRunStatus`

Possible statuses:

- `RUNNING`
- `COMPLETED`
- `FAILED`

## DTOs

DTOs isolate the API and service layer from JPA internals.

### Public Response DTOs

- `JobResponse`
  - returned by job endpoints.
- `CompanyResponse`
  - returned by `GET /api/companies`.
- `TagResponse`
  - returned by `GET /api/tags`.
- `ScrapeRunResponse`
  - returned by scrape run endpoints.
- `PageResponse<T>`
  - wraps Spring `Page<T>` into a stable response shape.

### Internal DTOs

- `ScrapedJob`
  - normalized result produced by the parser.
- `JobFilter`
  - filter parameters for job search.
- `JobSyncResult`
  - summary returned after synchronizing scraped jobs.

## Repositories

### `JobRepository`

Extends:

```java
JpaRepository<Job, Long>
JpaSpecificationExecutor<Job>
```

Important methods:

- `findByExternalId(String externalId)`
  - finds existing job during sync.
- `findWithCompanyAndTagsById(Long id)`
  - loads details for single job response.
- `findByActiveTrueAndExternalIdNotIn(Collection<String> externalIds)`
  - finds jobs to deactivate after a scrape.

Uses `@EntityGraph` for some methods so company and tags can be loaded with the job.

### `CompanyRepository`

Important methods:

- `findByNameIgnoreCase(String name)`

Used by sync logic to reuse existing companies.

### `TagRepository`

Important methods:

- `findByNameIgnoreCase(String name)`

Used by sync logic to reuse existing tags.

### `ScrapeRunRepository`

Important methods:

- `findTopByOrderByStartedAtDesc()`

Stores scrape statistics.

## Scraping Code

### `TechstarsJobsClient`

This class performs the HTTP fetch.

Main method:

```java
public List<ScrapedJob> fetchJobs()
```

Flow:

1. Connects to `properties.jobsUrl()` with Jsoup.
2. Sets User-Agent from config.
3. Sets timeout from config.
4. Downloads the HTML document.
5. Passes the document to `TechstarsJobsParser`.
6. Converts `IOException` into `JobScrapingException`.

### `TechstarsJobsParser`

This class converts HTML into normalized `ScrapedJob` records.

Responsibilities:

- find job cards in the Techstars/Getro HTML;
- extract title, company, location, tags, salary, seniority, posted date, and URLs;
- normalize whitespace;
- build absolute URLs;
- derive a stable external ID from URL when possible;
- use SHA-256 fallback when URL parsing cannot produce a stable ID.

The parser is intentionally isolated in one class because external HTML is the most fragile part of the project.

### `JobScrapingException`

Runtime exception used when fetching/parsing jobs fails.

## Synchronization Code

### `ScrapeService`

Coordinates a single scrape execution.

Main method:

```java
public JobSyncResult scrapeAndSync()
```

Flow:

1. Uses `AtomicBoolean` to check whether another scrape is running.
2. Throws `ScrapeAlreadyRunningException` if a run is already active.
3. Fetches scraped jobs through `TechstarsJobsClient`.
4. Passes scraped jobs to `JobSyncService`.
5. Releases the running flag in `finally`.

### `JobSyncService`

The most important business service in the project.

Main method:

```java
public JobSyncResult sync(Collection<ScrapedJob> scrapedJobs)
```

High-level algorithm:

1. Create a `ScrapeRun` with status `RUNNING`.
2. Filter invalid scraped records.
3. Deduplicate records by `externalId`.
4. For every valid scraped job:
   - resolve or create company;
   - resolve or create tags;
   - find existing job by `externalId`;
   - create a new job or update existing one;
   - replace tag set.
5. Deactivate active jobs not present in the latest non-empty scrape result.
6. Mark the scrape run as `COMPLETED`.
7. If an exception happens, mark the scrape run as `FAILED`.

Why it uses `externalId`:

- URLs can contain stable job identifiers.
- Stable identifiers prevent duplicate database rows across repeated scrapes.

Why empty scrape results do not deactivate all jobs:

- An empty result may mean the site returned unexpected HTML or a temporary issue occurred.
- Deactivating every job in that case would be destructive.

### `ScheduledScrapeRunner`

Runs scheduled scraping when enabled.

Annotation:

```java
@ConditionalOnProperty(
    prefix = "techstars.scraper",
    name = "scheduling-enabled",
    havingValue = "true"
)
```

This means the bean exists only when scheduling is explicitly enabled.

Main method:

```java
@Scheduled(cron = "${techstars.scraper.cron}")
public void runScheduledScrape()
```

It logs:

- completed runs;
- skipped runs caused by overlap;
- unexpected failures.

## Query Code

### `JobQueryService`

Provides read operations for jobs.

Main methods:

- `findJobs(JobFilter filter, Pageable pageable)`
- `findJob(Long id)`

Returns DTOs instead of entities.

### `JobSpecifications`

Builds dynamic JPA specifications for job filters.

Supported filters:

- `active`
- `q`
- `location`
- `company`
- `tag`
- `remoteType`
- `seniority`

The `q` filter searches:

- job title;
- job description;
- company name.

### `ReferenceDataService`

Returns:

- all companies sorted by name;
- all tags sorted by name.

Used by:

- `CompanyController`
- `TagController`

## Controllers

### `JobController`

Base path:

```text
/api/jobs
```

Endpoints:

```text
GET /api/jobs
GET /api/jobs/{id}
```

`GET /api/jobs` supports:

- `page`
- `size`
- `sort`
- `q`
- `location`
- `company`
- `tag`
- `remoteType`
- `seniority`
- `active`

Default:

- `active=true`
- `size=20`
- `sort=lastSeenAt`

### `ScrapeRunController`

Base path:

```text
/api/scrape-runs
```

Endpoints:

```text
POST /api/scrape-runs
GET /api/scrape-runs
```

`POST /api/scrape-runs` runs the scraper synchronously and returns the created scrape run.

### `CompanyController`

Base path:

```text
/api/companies
```

Returns all known companies sorted by name.

### `TagController`

Base path:

```text
/api/tags
```

Returns all known tags sorted by name.

### `ApiExceptionHandler`

Maps exceptions to API errors:

- `JobNotFoundException` -> `404`
- `ScrapeAlreadyRunningException` -> `409`

## Tests

### Parser Tests

`TechstarsJobsParserTest`

Covers HTML parsing and normalization behavior.

### Sync Tests

`JobSyncServiceTest`

Covers:

- creating jobs, companies, tags, and scrape runs;
- updating existing jobs;
- deactivating missing jobs;
- skipping invalid and duplicate scraped jobs;
- protecting existing data when scrape result is empty.

### API Tests

- `JobControllerTest`
- `ReferenceDataControllerTest`
- `ScrapeRunControllerTest`
- `OpenApiSmokeTest`

These tests use `MockMvc`.

### Scheduler Tests

`ScheduledScrapeRunnerTest`

Covers:

- scheduler calls scrape service;
- overlapping scrape exception is swallowed and logged.

## Adding New Features

### Add a New Job Filter

1. Add a field to `JobFilter`.
2. Add request parameter in `JobController`.
3. Add specification method in `JobSpecifications`.
4. Add or update controller tests.

### Add a New Scraped Field

1. Add a field to `ScrapedJob`.
2. Extract it in `TechstarsJobsParser`.
3. Add a database column through a new Flyway migration.
4. Add a field to `Job`.
5. Update `JobSyncService.applyScrapedData`.
6. Add the field to `JobResponse` if it should be public.
7. Update tests.

### Change Database Schema

Do not edit existing applied migrations after a release. Add a new file:

```text
src/main/resources/db/migration/V2__description.sql
```

Then update entities and tests to match.
