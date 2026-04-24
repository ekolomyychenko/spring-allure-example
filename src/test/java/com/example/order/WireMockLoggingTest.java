package com.example.order;

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

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;

@Epic("Allure instrumentation")
@Feature("WireMock — stubs and matched requests")
class WireMockLoggingTest extends BaseIntegrationTest {

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
    @DisplayName("WireMock stub mapping is logged as a step with query params")
    void shouldLogRegisteredStub() {
        stubFor(get(urlPathEqualTo("/api/prices"))
                .withQueryParam("product", equalTo("monitor"))
                .willReturn(okJson("{\"price\": 349.99}")));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"productName": "monitor", "quantity": 1}
                        """)
        .when()
                .post()
        .then()
                .statusCode(201);
    }

    @Test
    @DisplayName("WireMock logs matched request and response as step")
    void shouldLogMatchedRequestResponse() {
        stubFor(get(urlPathEqualTo("/api/prices"))
                .willReturn(okJson("{\"price\": 25.00}")));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"productName": "mouse", "quantity": 1}
                        """)
        .when()
                .post()
        .then()
                .statusCode(201);
    }

    @Test
    @DisplayName("WireMock logs 500 stub as matched error response")
    void shouldLogServerErrorStub() {
        stubFor(get(urlPathEqualTo("/api/prices")).willReturn(serverError()));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"productName": "error-product", "quantity": 1}
                        """)
        .when()
                .post()
        .then()
                .statusCode(500);
    }
}
