# Kuku Invest (쿠쿠 증권) Copilot Instructions

This repository hosts the **Kuku Invest** project, a microservices-based trading platform designed with extreme reliability and high concurrency in mind.

## Project Overview
-   **Goal**: Build a trading platform that mimics real-world securities systems.
-   **Core Values**: Why-Driven Engineering, Resilience, Data Integrity, High Concurrency.
-   **Architecture**: Microservices (MSA), Hexagonal Architecture, Event-Driven.

## Technology Stack
-   **Language**: Java (JDK 21)
-   **Framework**: Spring Boot 3.x
-   **Database**: MySQL (Primary), Redis (Cache/Lock)
-   **Messaging**: Kafka
-   **Infrastructure**: Docker Compose, Kubernetes

## Code Review Guidelines (Best Practices)

When reviewing or generating code, strictly adhere to the following principles:

**IMPORTANT**: All code reviews, comments, and explanations must be provided in **Korean (한국어)**.

### 1. Resilience & Stability (Critical)
-   **Timeouts**: Ensure all external calls (HTTP, DB, Redis) have explicit timeouts.
-   **Circuit Breakers**: Use Resilience4j for external dependencies.
-   **Retries**: Implement retries with exponential backoff for transient failures.
-   **Fallback**: Always provide a fallback mechanism for failure scenarios.
-   **"Unknown" State**: Handle cases where the result of an operation is unknown (e.g., network timeout after request).

### 2. Concurrency Control
-   **Thread Safety**: Verify that shared resources are thread-safe.
-   **Locking**:
    -   Use **Optimistic Lock** (`@Version`) for low contention.
    -   Use **Pessimistic Lock** (`SELECT ... FOR UPDATE`) for high contention critical sections.
    -   Use **Redis Distributed Lock** for distributed resources.
-   **Race Conditions**: actively look for check-then-act patterns that might cause race conditions.

### 3. Data Integrity (Ledger)
-   **Double-Entry Bookkeeping**: Ensure all financial transactions follow the double-entry principle (Debit/Credit).
-   **Immutability**: Ledger entries should be immutable once written.
-   **Precision**: Use `BigDecimal` for all monetary values. Never use `Double` or `Float`.

### 4. Architecture & Design
-   **Hexagonal Architecture**:
    -   **Domain**: Pure Java code, no framework dependencies (no `@Service`, `@Entity` inside domain logic if possible, or strictly controlled).
    -   **Ports**: Interfaces defining inbound/outbound interactions.
    -   **Adapters**: Implementation of ports (Web, Persistence, Messaging).
-   **SOLID**: Strictly enforce SRP and OCP.
-   **DDD**: Use Ubiquitous Language. Respect Bounded Contexts.

### 5. Coding Standards
-   **Java Best Practices**: Use modern Java features (Records, Optional, Stream API) appropriately.
-   **Naming**: Be descriptive. Avoid abbreviations.
-   **Testing**:
    -   Prefer **TDD**.
    -   **Unit Tests**: Mock external dependencies.
    -   **Integration Tests**: Use `@SpringBootTest` or TestContainers for DB/Kafka interactions.

### 6. Performance
-   **Database**: Check for N+1 problems. Ensure proper indexing.
-   **Caching**: Use Look-aside or Write-back strategies appropriate for the data volatility.

## Git Workflow
-   **GitHub Flow**: `main` is production-ready. Use `feature/*` or `fix/*` branches.
-   **Commit Messages**: Use Conventional Commits (`feat`, `fix`, `docs`, `refactor`, etc.).
