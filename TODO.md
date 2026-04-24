# Allure Instrumentation Layer — TODO

## 5. Kafka producer — добавить логирование send

**Проблема:** `OrderService.createOrder()` вызывает `kafkaTemplate.send("order-events", key, json)` (строка 42 в `OrderService.java`), но это событие нигде не логируется в Allure. Видно только `KafkaConsumer.poll()` на стороне потребителя. В отчёте нет шага "сообщение отправлено" — непонятно, что именно отправил сервис.

**Куда:** `src/test/java/com/example/order/allure/kafka/`

**Два варианта реализации (выбрать один):**

### Вариант A: ByteBuddy на KafkaProducer (рекомендуется — аналогично poll)
- [ ] **Файл `AllureKafkaInstrumentation.java`** — добавить второй `.type()` блок для `org.apache.kafka.clients.producer.KafkaProducer`, перехватить метод `send(ProducerRecord)` (и перегрузку `send(ProducerRecord, Callback)`)
- [ ] **Новый файл `AllureKafkaSendAdvice.java`** — `@Advice.OnMethodEnter`, читать из `ProducerRecord`: `topic()`, `key()`, `value()`, `partition()`, `headers()`. Формат шага: `Kafka send → topic [key]`. Attachment: полное содержимое записи
- [ ] **Тест:** в `OrderMockMvcMockitoTest.shouldSendKafkaEventOnOrderCreation` — после POST убедиться, что в Allure-отчёте виден шаг `Kafka send → order-events` ДО шага `Kafka poll`

### Вариант B: AOP-аспект на KafkaTemplate (проще, Spring-зависимый)
- [ ] **Новый файл `AllureKafkaTemplateAspect.java`** в `kafka/` — `@Around("execution(* org.springframework.kafka.core.KafkaTemplate.send(..))")`, логировать аргументы (topic, key, data)
- [ ] **Зарегистрировать** как бин в `AllureTestConfig.java`
- [ ] Минус: не перехватит прямые вызовы `KafkaProducer.send()`, только через `KafkaTemplate`

---

## 6. DB aspect — заменить isCalledFromTest() на проверку через Allure lifecycle

**Проблема:** `AllureRepositoryAspect.isCalledFromTest()` (строки 67-79) хардкодит пакеты `com.example.order.service.` и `com.example.order.controller.` — если вызов идёт из них, считает что это НЕ тестовый вызов. Но если добавить `com.example.order.scheduler.`, `com.example.order.listener.`, `com.example.order.handler.` и т.д., они не попадут в фильтр → вызовы из них будут ошибочно логироваться как тестовые. Текущая логика: "если в стеке есть service/controller — не тест, если есть *Test — тест". Это инвертировано и хрупко.

**Куда:** `src/test/java/com/example/order/allure/db/AllureRepositoryAspect.java`

**Что сделать:**
- [ ] **Заменить метод `isCalledFromTest()`** — вместо стектрейс-анализа использовать `Allure.getLifecycle().getCurrentTestCase()`. Если Allure lifecycle имеет активный test case, значит мы в тестовом контексте. Это надёжно, не зависит от пакетов, и работает с любой структурой проекта
- [ ] **Удалить хардкод пакетов** `com.example.order.service.` и `com.example.order.controller.`
- [ ] **Нюанс:** `getCurrentTestCase()` возвращает `Optional<String>` (UUID тест-кейса). Проверить `isPresent()`. Но есть тонкость — repository вызывается и из production code внутри теста (controller → service → repo), и оттуда тоже будет `isPresent() == true`. Если мы хотим логировать ВСЕ DB-вызовы во время теста (включая через production code), это ОК. Если только прямые тестовые — нужно оставить стектрейс-проверку, но сделать её generic
- [ ] **Решение:** логировать все DB-вызовы при активном Allure test case — это полезнее, видна полная картина взаимодействия с БД

---

## 7. DB aspect — учитывать наследование Entity

**Проблема:** Метод `describeEntity()` (строки 96-113 в `AllureRepositoryAspect.java`) вызывает `c.getDeclaredFields()`, который возвращает только поля **текущего** класса, без суперклассов. Сейчас `Order` не наследует от BaseEntity — проблема не проявляется. Но если появится `BaseEntity` с полями `id`, `createdAt`, `updatedAt`, они пропадут из Allure-аттачментов. В Response будет `Order{productName=laptop, quantity=2, price=1999.98, status=PRICED}` без `id`.

**Куда:** `src/test/java/com/example/order/allure/db/AllureRepositoryAspect.java`, метод `describeEntity()` и lambda в `fieldCache.computeIfAbsent()`

**Что сделать:**
- [ ] **Изменить lambda в `fieldCache.computeIfAbsent()`** — вместо `c.getDeclaredFields()` обойти всю цепочку `getSuperclass()` до `Object.class`, собрать все поля в `ArrayList<Field>`, вызвать `setAccessible(true)` на каждом, преобразовать в `Field[]`
- [ ] **Кеширование сохранить** — `fieldCache` по-прежнему хранит объединённый массив, вычисляется один раз на класс
- [ ] **Пример кода:**
  ```java
  fieldCache.computeIfAbsent(clazz, c -> {
      List<Field> all = new ArrayList<>();
      Class<?> current = c;
      while (current != null && current != Object.class) {
          for (Field f : current.getDeclaredFields()) {
              f.setAccessible(true);
              all.add(f);
          }
          current = current.getSuperclass();
      }
      return all.toArray(new Field[0]);
  });
  ```
- [ ] **Порядок:** суперкласс-поля (`id`, `createdAt`) будут в конце списка — это ОК, но можно инвертировать (собирать от суперкласса вниз) для более логичного вывода `Order{id=1, createdAt=..., productName=laptop, ...}`

---

## 9. WireMock — поддержка instance (не только static) полей

**Проблема:** `AllureWireMockTestListener.findServers()` (строка 110) вызывает `field.get(null)` — это работает только для `static` полей. В текущем проекте `wireMockServer` в `OrderWireMockRestAssuredTest` объявлен как `static` — работает. Но если кто-то объявит WireMockServer как instance-поле (например через `@RegisterExtension WireMockExtension` в JUnit 5, или просто `private WireMockServer wm = new WireMockServer(0)`), `field.get(null)` бросит `IllegalArgumentException` → сервер не обнаружится → запросы и стабы не попадут в отчёт. Ошибка будет проглочена в catch (сейчас хотя бы залогируется через `AllureInstrumentationLogger`).

**Куда:** `src/test/java/com/example/order/allure/wiremock/AllureWireMockTestListener.java`, метод `findServers()` и вызывающие его `registerListeners()` / `prependStubSteps()`

**Что сделать:**
- [ ] **Изменить сигнатуру `findServers()`** — принимать не `Class<?>`, а `TestContext`, чтобы иметь доступ к `testContext.getTestInstance()`
- [ ] **В `findServers()`** — проверять `java.lang.reflect.Modifier.isStatic(field.getModifiers())`:
  - Если `static` → `field.get(null)` (как сейчас)
  - Если instance → `field.get(testContext.getTestInstance())` — получить экземпляр теста
- [ ] **Обновить вызовы** `registerListeners()` и `prependStubSteps()` — передавать `testContext` вместо `testContext.getTestClass()`
- [ ] **Тест:** можно не добавлять (текущий тест с static полем продолжит работать, а instance-поле потребовало бы нового тестового класса)

---

## ~~1. AssertJ — расширить список перехватываемых методов~~ DONE

**Файл:** `src/test/java/com/example/order/allure/assertion/AllureAssertInstrumentation.java`

- [x] Переделано на blacklist-подход: перехватываются **все** public non-static методы `AbstractAssert+`
- [x] Blacklist конфигурационных методов: `as`, `describedAs`, `withFailMessage`, `overridingErrorMessage`, `usingComparator`, `usingElementComparator`, `usingRecursiveComparison`, `usingDefaultComparator`, `withRepresentation`, `withThreadDumpOnError`, `withAssertionInfo`, `inHexadecimal`, `inBinary`, `extracting`, `filteredOn`, `asInstanceOf`, `asString`, `asList`, internal-методы (`getActual`, `actual`, `info`, `myself`, `objects`, `failWithMessage`, `failWithActualExpectedAndMessage`, `newAbstractIterableAssert`), `equals`/`hashCode`/`toString`
- [x] `isNotNull` в blacklist — вызывается внутренне из многих assertion-методов, создаёт шум
- [x] Теперь автоматически покрываются: коллекции (`containsExactly`, `containsOnly`, `doesNotContain`...), строки (`startsWith`, `endsWith`, `matches`, `isBlank`...), типы (`isInstanceOf`, `isSameAs`, `isIn`...), числа (`isBetween`, `isCloseTo`, `isPositive`...), iterable (`allMatch`, `anyMatch`, `noneMatch`...), exceptions (`hasMessage`, `hasCause`...) и все будущие assertion-методы
- [x] Тест `shouldCreateOrderAndPersistToDb` расширен: добавлены `startsWith`, `endsWith`, `containsIgnoringCase`, `isInstanceOf`, `isIn`, `isPositive`, `isBetween`, `isCloseTo`, `allMatch`, `anyMatch`, `noneMatch`

---

## ~~2. Spring AssertionErrors — добавить недостающие методы~~ DONE

**Файл:** `src/test/java/com/example/order/allure/assertion/AllureAssertInstrumentation.java:44-49`

Сейчас перехватываются только `assertEquals(3 args)` и `assertTrue(2 args)`.

- [x] Добавить `assertNotNull(String, Object)` — `AllureSpringAssertNotNullAdvice.java`
- [x] Добавить `assertNull(String, Object)` — `AllureSpringAssertNullAdvice.java`
- [x] Добавить `assertFalse(String, boolean)` — `AllureSpringAssertFalseAdvice.java`
- [x] Добавить `assertNotEquals(String, Object, Object)` — `AllureSpringAssertNotEqualsAdvice.java`
- [x] Добавить `fail(String)` — `AllureSpringFailAdvice.java` (логируется как FAILED step)
- [x] Создать соответствующие Advice-классы для новых методов
- [x] Зарегистрировать все новые Advice в `AllureAssertInstrumentation.java`
- [x] Добавить тест `shouldCreateOrderAndVerifyWithSpringAssertions` в `OrderMockMvcMockitoTest`

---

## ~~3. Hamcrest — добавить 2-arg assertThat~~ DONE

**Файл:** `src/test/java/com/example/order/allure/assertion/AllureAssertInstrumentation.java:53-57`

Перехватывается только `assertThat(String, Object, Matcher)` (3 аргумента). Двухаргументный `assertThat(Object, Matcher)` — самый частый вариант — не логируется.

- [x] Добавить перехват `assertThat(Object, Matcher)` с `takesArguments(2)` — зарегистрирован в `AllureAssertInstrumentation.java`
- [x] Создать `AllureHamcrestSimpleAdvice.java` для 2-arg варианта
- [x] Добавить тест `shouldCreateOrderAndVerifyWithHamcrestAssertions` в `OrderMockMvcMockitoTest` — покрывает оба варианта (2-arg и 3-arg)

---

## ~~4. Hamcrest reason парсинг — не только jsonPath~~ DONE

**Файл:** `src/test/java/com/example/order/allure/assertion/AllureHamcrestAdvice.java:16-22`

Парсинг `reason` ищет только `"$."` (jsonPath). Для обычных `assertThat("should be positive", val, greaterThan(0))` reason теряется.

- [x] Всегда включать reason в вывод шага (если не null/empty) — обычный reason выводится как `reason: actual matcher`
- [x] jsonPath-парсинг оставить как дополнительное форматирование — `"$."` по-прежнему извлекается как компактный label
- [x] Тест уже покрыт в `shouldCreateOrderAndVerifyWithHamcrestAssertions` (3-arg вызовы с reason)

---

## ~~8. Mockito handler — различать stubbing, вызов и verify~~ DONE

**Файл:** `src/test/java/com/example/order/allure/mock/AllureMockitoHandler.java:20-27`

Все mock-взаимодействия выглядят одинаково в отчёте: stubbing (`when`), реальный вызов, verification (`verify`).

- [x] Определять тип взаимодействия: verify — через `ThreadSafeMockingProgress` (peek verificationMode поле без consume), stub vs call — по наличию production-кода в стектрейсе
- [x] Использовать разные prefixes: `Mock stub:`, `Mock call:`, `Mock verify:`
- [x] Фаза определяется ДО `delegate.handle()`, чтобы Mockito internal state не был consumed
- [x] Добавить тест `shouldDistinguishMockitoPhases` в `OrderMockMvcMockitoTest` — покрывает все три фазы

---

## ~~10. Общее — обработка ошибок~~ DONE

Во всех Advice-классах исключения полностью подавляются (`suppress = Throwable.class` + `catch (Throwable ignored)`). Это правильно для стабильности тестов, но затрудняет отладку инструментации.

- [x] Создать `AllureInstrumentationLogger` — shared `java.util.logging.Logger` (уровень FINE) для всех компонентов
- [x] Заменить все 15 `catch (Throwable ignored)` / `catch (Exception ignored)` на логирование через `AllureInstrumentationLogger.warn(component, t)`
- [x] Компоненты: AssertJ, Hamcrest, SpringAssert*, SpringFail, KafkaPoll, KafkaInstrumentation, AssertInstrumentation, MockitoVerifyDetection, WireMockDiscovery
