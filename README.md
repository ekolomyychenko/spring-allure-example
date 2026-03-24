# Order Service

Микросервис управления заказами на Spring Boot 3.

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

## Конфигурация

`src/main/resources/application.yml` — основные настройки (datasource, kafka, pricing-service URL).

## Тесты

Интеграционные тесты в `src/test/java/com/example/order/OrderIntegrationTest.java`.

**Что используется:**
- **Testcontainers** — поднимает PostgreSQL и Kafka в Docker
- **MockMvc** — HTTP-вызовы эндпоинтов
- **Spring Data JPA репозитории** — проверка состояния БД после вызовов
- **Mockito (`@MockBean`)** — мок `PricingClient` (исходящая REST-интеграция)
- **KafkaConsumer** — проверка отправки сообщений в топик

**Allure-отчётность:**
- `@Epic`, `@Feature`, `@Story` — группировка тестов
- `@Severity` — уровни критичности
- `@DisplayName` — человекочитаемые названия

**Требования:** Docker должен быть запущен.

```bash
# Запуск тестов
mvn clean test

# Генерация и открытие Allure-отчёта
mvn allure:serve
```

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
└── OrderIntegrationTest.java
```
