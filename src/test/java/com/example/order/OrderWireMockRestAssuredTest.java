package com.example.order;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@Epic("Order Service")
@Feature("WireMock + RestAssured + JPA")
class OrderWireMockRestAssuredTest extends BaseIntegrationTest {

    static WireMockServer wireMockServer = new WireMockServer(0);

    @LocalServerPort
    private int port;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeAll
    static void startWireMock() {
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void configurePricingService(DynamicPropertyRegistry registry) {
        registry.add("pricing-service.url", () -> "http://localhost:" + wireMockServer.port());
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/orders";
        wireMockServer.resetAll();
    }

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
    }

    @Test
    @Story("Create Order")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /api/orders — создание заказа через WireMock + RestAssured")
    void shouldCreateOrderWhenPricingServiceResponds() {
        stubFor(get(urlPathEqualTo("/api/prices"))
                .withQueryParam("product", equalTo("monitor"))
                .willReturn(okJson("{\"price\": 349.99}")));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"productName": "monitor", "quantity": 3}
                        """)
        .when()
                .post()
        .then()
                .statusCode(201)
                .body("productName", is("monitor"))
                .body("quantity", is(3))
                .body("price", is(1049.97F))
                .body("status", is("PRICED"));

        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getProductName()).isEqualTo("monitor");
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.PRICED);

        verify(1, getRequestedFor(urlPathEqualTo("/api/prices"))
                .withQueryParam("product", equalTo("monitor")));
    }

    @Test
    @Story("Get Order")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("GET /api/orders/{id} — получение заказа через RestAssured")
    void shouldReturnOrderById() {
        Order order = new Order();
        order.setProductName("keyboard");
        order.setQuantity(2);
        order.setPrice(new BigDecimal("150.00"));
        order.setStatus(OrderStatus.PRICED);
        Order saved = orderRepository.save(order);

        given()
        .when()
                .get("/{id}", saved.getId())
        .then()
                .statusCode(200)
                .body("id", is(saved.getId().intValue()))
                .body("productName", is("keyboard"))
                .body("quantity", is(2))
                .body("price", is(150.00F));
    }

    @Test
    @Story("Get Order")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /api/orders/{id} — 404 для несуществующего заказа через RestAssured")
    void shouldReturn404ForNonExistentOrder() {
        given()
        .when()
                .get("/{id}", 99999)
        .then()
                .statusCode(404);
    }

    @Test
    @Story("Create Order")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/orders — 400 при невалидном запросе через RestAssured")
    void shouldReturn400ForInvalidRequest() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"productName": "", "quantity": 0}
                        """)
        .when()
                .post()
        .then()
                .statusCode(400);
    }

    @Test
    @Story("External Service Integration")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /api/orders — обработка таймаута от Pricing Service")
    void shouldHandleTimeoutFromPricingService() {
        stubFor(get(urlPathEqualTo("/api/prices"))
                .willReturn(aResponse()
                        .withFixedDelay(5000)
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"price\": 100.00}")));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"productName": "slow-product", "quantity": 1}
                        """)
        .when()
                .post()
        .then()
                .statusCode(anyOf(is(500), is(201)));
    }

    @Test
    @Story("External Service Integration")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /api/orders — обработка 500 от Pricing Service")
    void shouldHandle500FromPricingService() {
        stubFor(get(urlPathEqualTo("/api/prices"))
                .willReturn(serverError()));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"productName": "error-product", "quantity": 1}
                        """)
        .when()
                .post()
        .then()
                .statusCode(500);

        assertThat(orderRepository.findAll()).isEmpty();
    }
}
