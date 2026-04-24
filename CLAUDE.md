# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Order management microservice (Spring Boot 3.5, Java 21) that demonstrates transparent Allure reporting wired into integration tests **without modifying test or production code**. All interceptors use SPI plugins, Spring AOP, ByteBuddy instrumentation, and Spring TestExecutionListeners.

## Build & Test Commands

```bash
# Run all tests (requires Docker for Testcontainers: PostgreSQL + Kafka)
mvn clean test

# Run a single test class
mvn test -Dtest=OrderMockMvcMockitoTest

# Run a single test method
mvn test -Dtest=OrderMockMvcMockitoTest#shouldCreateOrder

# Generate and open Allure report in browser
mvn allure:serve
```

## Architecture

**Application** (`src/main/java/com/example/order/`): REST API for orders. `POST /api/orders` calls an external pricing service via `PricingClient` (RestTemplate), persists to PostgreSQL via Spring Data JPA, and publishes to the `order-events` Kafka topic. `GET /api/orders/{id}` retrieves an order. Database migrations are managed by Liquibase (`src/main/resources/db/changelog/`).

**Tests** (`src/test/java/com/example/order/`): Two integration test classes share `BaseIntegrationTest` which bootstraps Testcontainers (PostgreSQL, Kafka):
- `OrderMockMvcMockitoTest` — MockMvc + `@MockBean` for the pricing client
- `OrderWireMockRestAssuredTest` — RestAssured + WireMock for the pricing client

**Allure instrumentation layer** (`src/test/java/com/example/order/allure/`): This is the core of the project — a set of interceptors that automatically log test interactions to Allure:
- `http/` — MockMvc (`ResultHandler` via `MockMvcBuilderCustomizer`) and RestAssured (`Filter`) request/response logging
- `mock/` — Custom `MockMaker` SPI plugin that wraps Mockito's `MockHandler` to log mock invocations
- `db/` — Spring AOP aspect on `Repository+` that logs DB calls (only from test call stack)
- `kafka/` — ByteBuddy patches `KafkaConsumer.poll()` (piggybacks on Mockito's ByteBuddy agent)
- `wiremock/` — `TestExecutionListener` discovers `WireMockServer` fields via reflection, logs stubs and request/response pairs
- `assertion/` — ByteBuddy patches AssertJ, Spring `AssertionErrors`, and Hamcrest assertion classes
- `AllureLogsListener` — `TestExecutionListener` that attaches Logback app logs and Spring config to reports
- `AllureTestConfig` — entry point that wires all components together

Key SPI/extension registration files:
- `src/test/resources/META-INF/spring.factories` — registers TestExecutionListeners
- `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` — registers custom MockMaker
