# Allure Instrumentation Layer — TODO

## 1. AssertJ — расширить список перехватываемых методов

**Файл:** `src/test/java/com/example/order/allure/assertion/AllureAssertInstrumentation.java:26-39`

Сейчас захардкожены 14 методов. Нужно либо перейти на blacklist-подход (перехватывать все методы `AbstractAssert+`, кроме конфигурационных вроде `as`, `describedAs`, `usingComparator`), либо существенно расширить whitelist.

Пропущенные методы:

### Коллекции и Map
- [ ] `containsExactly`
- [ ] `containsExactlyInAnyOrder`
- [ ] `containsOnly`
- [ ] `containsAll`
- [ ] `doesNotContain`
- [ ] `containsKey`, `containsValue`, `containsEntry`
- [ ] `containsSubsequence`
- [ ] `containsAnyOf`

### Строки
- [ ] `startsWith`, `endsWith`
- [ ] `matches`
- [ ] `isBlank`, `isNotBlank`
- [ ] `containsIgnoringCase`, `isEqualToIgnoringCase`
- [ ] `hasToString`

### Типы и identity
- [ ] `isInstanceOf`, `isNotInstanceOf`, `isExactlyInstanceOf`
- [ ] `isSameAs`, `isNotSameAs`
- [ ] `isIn`, `isNotIn`
- [ ] `hasSameClassAs`

### Числа
- [ ] `isBetween`, `isStrictlyBetween`
- [ ] `isCloseTo`, `isNotCloseTo`
- [ ] `isZero`, `isPositive`, `isNegative`, `isNotZero`, `isOne`

### Iterable-специфичные
- [ ] `hasSizeBetween`, `hasSizeGreaterThan`, `hasSizeLessThan`
- [ ] `allMatch`, `anyMatch`, `noneMatch`
- [ ] `allSatisfy`, `anySatisfy`, `noneSatisfy`
- [ ] `filteredOn`, `extracting`
- [ ] `first`, `last`, `element`, `singleElement`

### Exception assertions (AbstractThrowableAssert)
- [ ] `hasMessage`, `hasMessageContaining`, `hasMessageStartingWith`
- [ ] `hasCause`, `hasNoCause`, `hasRootCause`
- [ ] `hasStackTraceContaining`

### Альтернативный подход (предпочтительный)
- [ ] Переделать на blacklist: перехватывать **все** методы `AbstractAssert+`, исключая конфигурационные (`as`, `describedAs`, `withFailMessage`, `overridingErrorMessage`, `usingComparator`, `usingElementComparator`, `withRepresentation`, `withThreadDumpOnError`, `satisfies` без аргументов)

---

## 2. Spring AssertionErrors — добавить недостающие методы

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

## 3. Hamcrest — добавить 2-arg assertThat

**Файл:** `src/test/java/com/example/order/allure/assertion/AllureAssertInstrumentation.java:53-57`

Перехватывается только `assertThat(String, Object, Matcher)` (3 аргумента). Двухаргументный `assertThat(Object, Matcher)` — самый частый вариант — не логируется.

- [x] Добавить перехват `assertThat(Object, Matcher)` с `takesArguments(2)` — зарегистрирован в `AllureAssertInstrumentation.java`
- [x] Создать `AllureHamcrestSimpleAdvice.java` для 2-arg варианта
- [x] Добавить тест `shouldCreateOrderAndVerifyWithHamcrestAssertions` в `OrderMockMvcMockitoTest` — покрывает оба варианта (2-arg и 3-arg)

---

## 4. Hamcrest reason парсинг — не только jsonPath

**Файл:** `src/test/java/com/example/order/allure/assertion/AllureHamcrestAdvice.java:16-22`

Парсинг `reason` ищет только `"$."` (jsonPath). Для обычных `assertThat("should be positive", val, greaterThan(0))` reason теряется.

- [x] Всегда включать reason в вывод шага (если не null/empty) — обычный reason выводится как `reason: actual matcher`
- [x] jsonPath-парсинг оставить как дополнительное форматирование — `"$."` по-прежнему извлекается как компактный label
- [x] Тест уже покрыт в `shouldCreateOrderAndVerifyWithHamcrestAssertions` (3-arg вызовы с reason)

---

## 5. Kafka producer — добавить логирование send

**Файл:** `src/test/java/com/example/order/allure/kafka/AllureKafkaInstrumentation.java`

Логируется только `KafkaConsumer.poll()`. Публикация через `KafkaTemplate.send()` / `KafkaProducer.send()` — нет.

- [ ] Добавить ByteBuddy инструментацию `org.apache.kafka.clients.producer.KafkaProducer.send(ProducerRecord)`
- [ ] Создать `AllureKafkaSendAdvice` — логировать topic, key, value, partition
- [ ] Либо: AOP-аспект на `KafkaTemplate.send()` (проще, но Spring-зависимый)

---

## 6. DB aspect — заменить isCalledFromTest() на проверку через Allure lifecycle

**Файл:** `src/test/java/com/example/order/allure/db/AllureRepositoryAspect.java:67-79`

Хардкод пакетов `com.example.order.service.` и `com.example.order.controller.` хрупкий — новые пакеты (scheduler, listener, handler и т.д.) не учтены.

- [ ] Заменить `isCalledFromTest()` на `Allure.getLifecycle().getCurrentTestCase().isPresent()` (или аналог)
- [ ] Удалить хардкод пакетов

---

## 7. DB aspect — учитывать наследование Entity

**Файл:** `src/test/java/com/example/order/allure/db/AllureRepositoryAspect.java:96-103`

`c.getDeclaredFields()` не поднимается по суперклассам. Если entity наследует от `BaseEntity` с полями `id`, `createdAt` — они не попадут в описание.

- [ ] Обходить всю иерархию классов (`getSuperclass()` в цикле до `Object.class`)
- [ ] Кешировать результат (объединённый массив полей) в `fieldCache`

---

## 8. Mockito handler — различать stubbing, вызов и verify

**Файл:** `src/test/java/com/example/order/allure/mock/AllureMockitoHandler.java:20-27`

Все mock-взаимодействия выглядят одинаково в отчёте: stubbing (`when`), реальный вызов, verification (`verify`).

- [x] Определять тип взаимодействия: verify — через `ThreadSafeMockingProgress` (peek verificationMode поле без consume), stub vs call — по наличию production-кода в стектрейсе
- [x] Использовать разные prefixes: `Mock stub:`, `Mock call:`, `Mock verify:`
- [x] Фаза определяется ДО `delegate.handle()`, чтобы Mockito internal state не был consumed
- [x] Добавить тест `shouldDistinguishMockitoPhases` в `OrderMockMvcMockitoTest` — покрывает все три фазы

---

## 9. WireMock — поддержка instance (не только static) полей

**Файл:** `src/test/java/com/example/order/allure/wiremock/AllureWireMockTestListener.java:109`

`field.get(null)` работает только со static полями. Instance-поля (например через `@RegisterExtension`) не обнаруживаются.

- [ ] Для non-static полей использовать `field.get(testContext.getTestInstance())`
- [ ] Проверять `Modifier.isStatic(field.getModifiers())` и выбирать соответствующий вариант

---

## 10. Общее — обработка ошибок

Во всех Advice-классах исключения полностью подавляются (`suppress = Throwable.class` + `catch (Throwable ignored)`). Это правильно для стабильности тестов, но затрудняет отладку инструментации.

- [ ] Добавить `java.util.logging.Logger` (или SLF4J) с уровнем FINE/DEBUG для подавленных ошибок
- [ ] Хотя бы в development-режиме видеть, если инструментация ломается
