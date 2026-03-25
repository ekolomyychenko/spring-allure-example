# Order Service

Order management microservice on Spring Boot 3.

This project shows how to wire up detailed Allure reporting into integration tests **without touching test or application code**. All interceptors kick in automatically via SPI plugins, Spring AOP, ByteBuddy instrumentation and Spring TestExecutionListener.

## Stack

- Java 21, Spring Boot 3.5
- PostgreSQL — order storage
- Kafka — fires events on order creation
- REST client (RestTemplate) — outbound integration with a pricing service

## API

### POST /api/orders
Create an order. Calls the pricing service for a price, persists the order to DB and pushes an event to the `order-events` Kafka topic.

```json
{"productName": "laptop", "quantity": 2}
```

### GET /api/orders/{id}
Get an order by ID.

## Tests

Integration tests live in `src/test/java/com/example/order/`:
- `OrderMockMvcMockitoTest` — MockMvc + Mockito (`@MockBean`) for the pricing client
- `OrderWireMockRestAssuredTest` — RestAssured + WireMock for the pricing client

**What's used:**
- **Testcontainers** — spins up PostgreSQL and Kafka in Docker
- **MockMvc** — HTTP calls to endpoints (`OrderMockMvcMockitoTest`)
- **RestAssured** — HTTP calls to endpoints (`OrderWireMockRestAssuredTest`)
- **WireMock** — stubs the external pricing service
- **Spring Data JPA repositories** — DB state verification after calls
- **Mockito (`@MockBean`)** — mocks `PricingClient` (`OrderMockMvcMockitoTest`)
- **KafkaConsumer** — verifies messages published to the topic

**Prerequisites:**
- Java 21+
- Maven 3.8+
- Docker (Testcontainers needs it for PostgreSQL and Kafka)

Clone and enter the project:
```bash
git clone <repo-url>
cd spring-allure-example
```

Run tests (Docker must be running):
```bash
mvn clean test
```

Generate and open Allure report in browser:
```bash
mvn allure:serve
```

## Allure reporting without changing tests

All reporting code lives in `src/test/java/com/example/order/allure/`. Tests and production code are untouched.

### Report example

![Allure Report](docs/Снимок%20экрана%202026-03-25%20в%2008.38.45.png)

![Allure Report — step details](docs/Снимок%20экрана%202026-03-25%20в%2008.41.23.png)

### What gets logged

| What                              | How it's intercepted                             | Example step in report                               |
|-----------------------------------|--------------------------------------------------|------------------------------------------------------|
| HTTP requests/responses (MockMvc) | `MockMvcBuilderCustomizer` + `ResultHandler`     | `POST /api/orders → 201`                             |
| HTTP requests/responses (RestAssured) | `Filter` implementation                      | `POST /api/orders → 201`                             |
| WireMock stubs                    | Spring `TestExecutionListener` + reflection      | `WireMock stub: GET /api/prices → 200`               |
| WireMock requests/responses       | Spring `TestExecutionListener` + `RequestListener` | `WireMock request: GET /api/prices?product=laptop → 200` |
| Mockito mock calls                | Custom `MockMaker` (SPI plugin)                  | `Mock: pricingClient.getPrice("laptop") → 999.99`    |
| DB queries (repositories)         | Spring AOP aspect                                | `DB: OrderRepository.findAll`                         |
| Kafka consumer (poll)             | ByteBuddy instrumentation of `KafkaConsumer`     | `Kafka poll → 1 record(s)`                            |
| AssertJ assertions                | ByteBuddy instrumentation of `AbstractAssert`    | `Assert: laptop isEqualTo laptop`                     |
| MockMvc assertions (andExpect)    | ByteBuddy instrumentation of `AssertionErrors`   | `Assert: Status expected 201 = 201`                   |
| Hamcrest assertions               | ByteBuddy instrumentation of `MatcherAssert`     | `Assert: $.productName laptop is "laptop"`            |
| App configuration                 | Spring `TestExecutionListener`                   | `Configuration` step with properties                  |
| App logs                          | Spring `TestExecutionListener` + Logback appender | `Application Logs` attachment                         |

### How it works

- **MockMvc** — `AllureHttpResultHandler` hooks into `MockMvcBuilderCustomizer.alwaysDo()` and catches every request
- **RestAssured** — `AllureRestAssuredFilter` registers globally in `AllureTestConfig` and intercepts every RestAssured request/response
- **WireMock** — `AllureWireMockTestListener` registers via `META-INF/spring.factories` as a `TestExecutionListener`, discovers `WireMockServer` fields via reflection, adds a `RequestListener` for request/response logging, and reads registered stubs from the server after each test
- **Mockito** — `AllureMockitoMockMaker` replaces the default `MockMaker` via the SPI file `mockito-extensions/org.mockito.plugins.MockMaker`. Wraps the `MockHandler` to see all mock invocations
- **DB** — `AllureRepositoryAspect` attaches to `Repository+` via Spring AOP. Walks the call stack to only log calls coming from tests, not from service code
- **Kafka** — the ByteBuddy agent is already loaded by Mockito, so `AllureKafkaInstrumentation` piggybacks on it and patches `KafkaConsumer.poll()` right in the bytecode
- **Assertions** — `AllureAssertInstrumentation` patches AssertJ (`AbstractAssert`), Spring (`AssertionErrors`) and Hamcrest (`MatcherAssert`) the same way
- **Logs & config** — `AllureLogsListener` registers via `META-INF/spring.factories` as a `TestExecutionListener`, attaches a Logback appender for the duration of each test

## Structure

```
src/main/java/com/example/order/
├── OrderServiceApplication.java
├── config/AppConfig.java
├── controller/OrderController.java
├── service/OrderService.java
├── client/PricingClient.java
├── repository/OrderRepository.java
├── dto/CreateOrderRequest.java
├── dto/OrderResponse.java
└── model/Order.java, OrderStatus.java

src/test/java/com/example/order/
├── BaseIntegrationTest.java
├── OrderMockMvcMockitoTest.java
├── OrderWireMockRestAssuredTest.java
└── allure/
    ├── AllureTestConfig.java            — entry point, registers all components
    ├── AllureLogsListener.java          — app logs + configuration
    ├── http/
    │   ├── AllureHttpResultHandler.java — intercepts MockMvc requests
    │   └── AllureRestAssuredFilter.java — intercepts RestAssured requests
    ├── mock/
    │   ├── AllureMockitoMockMaker.java  — SPI plugin for Mockito
    │   └── AllureMockitoHandler.java    — logs mock invocations
    ├── db/
    │   └── AllureRepositoryAspect.java  — AOP intercept for repositories
    ├── kafka/
    │   ├── AllureKafkaInstrumentation.java — ByteBuddy instrumentation
    │   └── AllureKafkaPollAdvice.java     — advice for KafkaConsumer.poll()
    ├── wiremock/
    │   ├── AllureWireMockTestListener.java — discovers servers, logs stubs + requests
    │   └── AllureWireMockListener.java     — buffers request/response pairs
    └── assertion/
        ├── AllureAssertInstrumentation.java   — ByteBuddy instrumentation
        ├── AllureAssertJAdvice.java            — AssertJ
        ├── AllureSpringAssertAdvice.java       — Spring assertEquals
        ├── AllureSpringAssertTrueAdvice.java   — Spring assertTrue
        └── AllureHamcrestAdvice.java           — Hamcrest
```
