# Arquitetura do Sistema

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          MONITOR SYSTEM                                  │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                           MONITOR-API (Port 8080)                         │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │   Site      │  │  Dashboard  │  │   Ingest    │  │   Event     │   │
│  │ Controller  │  │ Controller  │  │ Controller  │  │ Controller  │   │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘   │
│         │                │                 │                 │           │
│         ▼                ▼                 ▼                 ▼           │
│  ┌──────────────────────────────────────────────────────────────┐       │
│  │                    Service Layer                              │       │
│  │  - IngestService      - DashboardService                      │       │
│  │  - EventPublisher     - WeeklyReportService                   │       │
│  │  - CheckSchedulerService  - EmailSenderService                │       │
│  └──────────────────────────────────────────────────────────────┘       │
│         │                                │                               │
│         ▼                                ▼                               │
│  ┌──────────────┐              ┌──────────────────┐                     │
│  │ Repositories │              │  Quartz Jobs     │                     │
│  │ (JPA/Hibernate)│            │  - CheckScheduler│                     │
│  └──────┬───────┘              │  - WeeklyReport  │                     │
│         │                      └────────┬─────────┘                     │
│         │                               │                               │
│         ▼                               ▼                               │
│  ┌─────────────────────────────────────────────┐                        │
│  │         PostgreSQL Database                  │                        │
│  │  sites, site_pages, runs, page_results,     │                        │
│  │  failures, request_errors, rules             │                        │
│  └─────────────────────────────────────────────┘                        │
│                               │                                          │
│                               │ Publish RUN_CHECK                        │
│                               ▼                                          │
│                      ┌──────────────────┐                                │
│                      │   RabbitMQ       │                                │
│                      │ monitor.run-check│                                │
│                      └────────┬─────────┘                                │
└──────────────────────────────┼──────────────────────────────────────────┘
                                │
                                │ Consume
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                       MONITOR-RUNNER (Port 8081)                          │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌──────────────────────────────────────────┐                            │
│  │      RunCheckConsumer (RabbitMQ)         │                            │
│  └─────────────────┬────────────────────────┘                            │
│                    │                                                      │
│                    ▼                                                      │
│  ┌──────────────────────────────────────────┐                            │
│  │      MonitorApiClient (REST)             │                            │
│  │  GET /api/sites/{id}/config              │                            │
│  └─────────────────┬────────────────────────┘                            │
│                    │                                                      │
│                    ▼                                                      │
│  ┌──────────────────────────────────────────┐                            │
│  │      PlaywrightExecutor                  │                            │
│  │  ┌────────────────────────────────────┐  │                            │
│  │  │  For each page:                    │  │                            │
│  │  │  - NetworkCollector (requests)     │  │                            │
│  │  │  - ConsoleCollector (errors)       │  │                            │
│  │  │  - PerfCollector (TTFB/DOM/Load)   │  │                            │
│  │  └────────────────────────────────────┘  │                            │
│  └─────────────────┬────────────────────────┘                            │
│                    │                                                      │
│                    │ POST /api/ingest/runs                                │
│                    ▼                                                      │
│            Back to MONITOR-API                                            │
│                                                                           │
│  ┌──────────────────────────────────────────┐                            │
│  │     Playwright Chromium Headless         │                            │
│  │     - Intercepts network traffic         │                            │
│  │     - Captures console messages          │                            │
│  │     - Collects navigation timing         │                            │
│  └──────────────────────────────────────────┘                            │
│                                                                           │
└──────────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────┐
│                        EXTERNAL CLIENTS                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐               │
│  │   Frontend   │   │   SSE Client │   │  Email Client│               │
│  │  Dashboard   │   │  (EventSource)│   │  (Weekly)    │               │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘               │
│         │                  │                   │                        │
│         │ HTTP             │ SSE               │ SMTP                   │
│         ▼                  ▼                   ▼                        │
│    GET /api/dashboard  GET /api/events  [Email Reports]                │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘


═══════════════════════════════════════════════════════════════════════════
                            DATA FLOW
═══════════════════════════════════════════════════════════════════════════

1. SCHEDULING (Every minute)
   ┌──────────────────────┐
   │ Quartz Scheduler     │
   │ CheckSchedulerJob    │
   └──────────┬───────────┘
              │ Checks sites due
              ▼
   ┌──────────────────────┐
   │ CheckSchedulerService│
   │ findSitesDueForCheck │
   └──────────┬───────────┘
              │ For each due site
              ▼
   ┌──────────────────────┐
   │ Publish to RabbitMQ  │
   │ monitor.run-check    │
   └──────────────────────┘


2. EXECUTION (Playwright)
   ┌──────────────────────┐
   │ RabbitMQ Message     │
   │ {siteId, baseUrl}    │
   └──────────┬───────────┘
              │
              ▼
   ┌──────────────────────┐
   │ RunCheckConsumer     │
   │ receives message     │
   └──────────┬───────────┘
              │ Fetch config
              ▼
   ┌──────────────────────┐
   │ GET /sites/{id}/config│
   └──────────┬───────────┘
              │ Returns pages
              ▼
   ┌──────────────────────┐
   │ PlaywrightExecutor   │
   │ for each page        │
   └──────────┬───────────┘
              │
              ▼
   ┌──────────────────────────────────────┐
   │  Navigate + Collect                   │
   │  - Network: requests, responses, 404s │
   │  - Console: errors, warnings          │
   │  - Performance: TTFB, DOM, Load       │
   └──────────┬───────────────────────────┘
              │
              ▼
   ┌──────────────────────┐
   │ Build IngestRequest  │
   │ - PageResults        │
   │ - Failures           │
   │ - RequestErrors      │
   └──────────┬───────────┘
              │
              ▼
   ┌──────────────────────┐
   │ POST /ingest/runs    │
   └──────────────────────┘


3. PERSISTENCE & EVENTS
   ┌──────────────────────┐
   │ IngestController     │
   │ receives payload     │
   └──────────┬───────────┘
              │
              ▼
   ┌──────────────────────┐
   │ IngestService        │
   │ - Save Run           │
   │ - Save PageResults   │
   │ - Save Failures      │
   │ - Save RequestErrors │
   └──────────┬───────────┘
              │
              ▼
   ┌──────────────────────┐
   │ PostgreSQL           │
   │ Transaction commit   │
   └──────────┬───────────┘
              │
              ▼
   ┌──────────────────────┐
   │ EventPublisher       │
   │ publishRunCompleted  │
   └──────────┬───────────┘
              │
              ▼
   ┌──────────────────────┐
   │ SSE Clients          │
   │ receive event        │
   └──────────────────────┘


4. WEEKLY REPORT (Sunday 20:00)
   ┌──────────────────────┐
   │ Quartz Scheduler     │
   │ WeeklyReportJob      │
   └──────────┬───────────┘
              │
              ▼
   ┌──────────────────────────────────┐
   │ WeeklyReportService              │
   │ - Calculate uptime               │
   │ - Get failures by severity       │
   │ - Calculate p95 performance      │
   │ - Find top issues/slow pages/404s│
   │ - Compare with previous week     │
   └──────────┬───────────────────────┘
              │
              ▼
   ┌──────────────────────┐
   │ Thymeleaf Template   │
   │ weekly-report.html   │
   └──────────┬───────────┘
              │
              ▼
   ┌──────────────────────┐
   │ EmailSenderService   │
   │ (SMTP or Fake)       │
   └──────────┬───────────┘
              │
              ▼
   ┌──────────────────────┐
   │ Recipients inbox     │
   └──────────────────────┘


═══════════════════════════════════════════════════════════════════════════
                         DATABASE SCHEMA
═══════════════════════════════════════════════════════════════════════════

sites (1) ──┬──< site_pages (N)
            │
            ├──< rules (N)
            │
            └──< runs (N) ──┬──< page_results (N)
                            │
                            ├──< failures (N)
                            │
                            └──< request_errors (N)

Indexes:
- sites: enabled, created_at
- site_pages: site_id, enabled
- runs: site_id, started_at DESC, status
- page_results: run_id, page_id
- failures: run_id, severity, type
- request_errors: run_id, created_at, status
