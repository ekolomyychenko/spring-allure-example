package com.example.order;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;

@Epic("Allure instrumentation")
@Feature("RestAssured — HTTP request/response logging")
class RestAssuredLoggingTest extends BaseIntegrationTest {

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
    @DisplayName("RestAssured logs POST request body and 201 response")
    void shouldLogRestAssuredPost201() {
        stubFor(get(urlPathEqualTo("/api/prices")).willReturn(okJson("{\"price\": 349.99}")));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"productName": "monitor", "quantity": 3}
                        """)
        .when()
                .post()
        .then()
                .statusCode(201);
    }

    @Test
    @DisplayName("RestAssured logs GET request and 200 response")
    void shouldLogRestAssuredGet200() {
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
                .statusCode(200);
    }

    @Test
    @DisplayName("RestAssured logs GET request and 404 response")
    void shouldLogRestAssuredGet404() {
        given()
        .when()
                .get("/{id}", 99999)
        .then()
                .statusCode(404);
    }
}
