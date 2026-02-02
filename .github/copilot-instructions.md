# Site Monitoring System - Copilot Instructions

## Project Overview
Monorepo com sistema de monitoramento sintético de sites usando Spring Boot 3, Playwright Java, PostgreSQL, RabbitMQ.

## Architecture
- **monitor-api**: REST API, ingest, SSE events, Quartz scheduler, dashboard endpoints
- **monitor-runner**: Playwright executor, RabbitMQ consumer, performance metrics collector

## Tech Stack
- Java 21
- Spring Boot 3.2+
- PostgreSQL + Flyway
- RabbitMQ
- Playwright Java
- Quartz Scheduler
- Thymeleaf (email templates)

## Code Standards
- Use `record` for all DTOs
- Use `enum` for status, severity, types
- Follow clean architecture principles
- Package structure: controller > service > repository > entity
- Use constructor injection (no @Autowired on fields)
