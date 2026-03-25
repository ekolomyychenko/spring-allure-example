# Order Service

Микросервис управления заказами на Spring Boot 3.

Проект является примером подключения детального Allure-логирования к интеграционным тестам **без изменения кода тестов и приложения**. Все перехватчики работают автоматически через SPI-плагины, Spring AOP, ByteBuddy-инструментирование и Spring TestExecutionListener.

## Стек

- Java 17, Spring Boot 3.2
- PostgreSQL — хранение заказов
- Kafka — отправка событий при создании заказа
- REST-клиент (RestTemplate) — исходящая интеграция с сервисом цен

## API

### POST /api/orders
Создать заказ. Получает цену из внешнего сервиса, сохраняет в БД, отправляет событие в Kafka-топик `order-events`.

```json
{"productName": "laptop", "quantity": 2}
```

### GET /api/orders/{id}
Получить заказ по ID.

## Тесты

Интеграционные тесты в `src/test/java/com/example/order/OrderIntegrationTest.java`.

**Что используется:**
- **Testcontainers** — поднимает PostgreSQL и Kafka в Docker
- **MockMvc** — HTTP-вызовы эндпоинтов
- **Spring Data JPA репозитории** — проверка состояния БД после вызовов
- **Mockito (`@MockBean`)** — мок `PricingClient` (исходящая REST-интеграция)
- **KafkaConsumer** — проверка отправки сообщений в топик

**Требования:**
- Java 17+
- Maven 3.8+
- Docker (Testcontainers поднимает PostgreSQL и Kafka)

Клонировать и перейти в проект:
```bash
git clone <repo-url>
cd spring-allure-example
```

Запуск тестов (Docker должен быть запущен):
```bash
mvn clean test
```

Генерация и открытие Allure-отчёта в браузере:
```bash
mvn allure:serve
```

## Allure-логирование без изменения тестов

Весь код логирования находится в `src/test/java/com/example/order/allure/` и подключается автоматически — тесты и приложение не модифицируются.

### Пример отчёта

![Allure Report](docs/Снимок%20экрана%202026-03-25%20в%2008.38.45.png)

![Allure Report — детализация степов](docs/Снимок%20экрана%202026-03-25%20в%2008.41.23.png)

### Что логируется

| Что                               | Как перехватывается                              | Пример степа в отчёте                              |
|------------------------------------|--------------------------------------------------|-----------------------------------------------------|
| HTTP-запросы/ответы (MockMvc)      | `MockMvcBuilderCustomizer` + `ResultHandler`     | `POST /api/orders → 201`                            |
| Вызовы Mockito-моков               | Кастомный `MockMaker` (SPI-плагин)               | `Mock: pricingClient.getPrice("laptop") → 999.99`   |
| Запросы к БД (репозитории)         | Spring AOP аспект                                | `DB: OrderRepository.findAll`                        |
| Kafka consumer (poll)              | ByteBuddy-инструментирование `KafkaConsumer`     | `Kafka poll → 1 record(s)`                           |
| AssertJ-ассерты                    | ByteBuddy-инструментирование `AbstractAssert`    | `Assert: laptop isEqualTo laptop`                    |
| MockMvc-ассерты (andExpect)        | ByteBuddy-инструментирование `AssertionErrors`   | `Assert: Status expected 201 = 201`                  |
| Hamcrest-ассерты                   | ByteBuddy-инструментирование `MatcherAssert`     | `Assert: $.productName laptop is "laptop"`           |
| Конфигурация приложения            | Spring `TestExecutionListener`                   | Степ `Configuration` с пропертями                    |
| Логи приложения                    | Spring `TestExecutionListener` + Logback appender | Аттач `Application Logs`                             |

### Как это работает

- **MockMvc** — `AllureHttpResultHandler` регистрируется через `MockMvcBuilderCustomizer.alwaysDo()`, срабатывает на каждый запрос автоматически
- **Mockito** — `AllureMockitoMockMaker` подменяет стандартный `MockMaker` через SPI-файл `mockito-extensions/org.mockito.plugins.MockMaker`, оборачивает `MockHandler` каждого мока для логирования вызовов
- **БД** — `AllureRepositoryAspect` перехватывает вызовы `Repository+` через Spring AOP, логирует только вызовы из тестового кода (не из сервисов приложения)
- **Kafka** — `AllureKafkaInstrumentation` через ByteBuddy агент (уже загружен Mockito) инструментирует `KafkaConsumer.poll()`, вставляя Allure-степы в байткод
- **Ассерты** — `AllureAssertInstrumentation` аналогично инструментирует AssertJ, Spring `AssertionErrors` и Hamcrest `MatcherAssert`
- **Логи и конфиг** — `AllureLogsListener` регистрируется как Spring `TestExecutionListener` через `META-INF/spring.factories`, подключает Logback-appender на время каждого теста

## Структура

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
├── OrderIntegrationTest.java
└── allure/
    ├── AllureTestConfig.java            — точка входа, регистрация всех компонентов
    ├── AllureLogsListener.java          — логи приложения + конфигурация
    ├── http/
    │   └── AllureHttpResultHandler.java — перехват MockMvc запросов
    ├── mock/
    │   ├── AllureMockitoMockMaker.java  — SPI-плагин для Mockito
    │   └── AllureMockitoHandler.java    — логирование вызовов моков
    ├── db/
    │   └── AllureRepositoryAspect.java  — AOP-перехват репозиториев
    ├── kafka/
    │   ├── AllureKafkaInstrumentation.java — ByteBuddy-инструментирование
    │   └── AllureKafkaPollAdvice.java     — advice для KafkaConsumer.poll()
    └── assertion/
        ├── AllureAssertInstrumentation.java   — ByteBuddy-инструментирование
        ├── AllureAssertJAdvice.java            — AssertJ
        ├── AllureSpringAssertAdvice.java       — Spring assertEquals
        ├── AllureSpringAssertTrueAdvice.java   — Spring assertTrue
        └── AllureHamcrestAdvice.java           — Hamcrest
```
