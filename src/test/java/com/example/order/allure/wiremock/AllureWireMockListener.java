package com.example.order.allure.wiremock;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import io.qameta.allure.Allure;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Collects every request that hits WireMock and flushes them as Allure steps
 * on the test thread (via {@link AllureWireMockTestListener#afterTestMethod}).
 * <p>
 * WireMock fires the listener callback on its Jetty thread, where Allure has
 * no active test case. So we buffer the data and replay it on the right thread.
 */
public final class AllureWireMockListener {

    private static final Queue<CapturedExchange> EXCHANGES = new ConcurrentLinkedQueue<>();

    public static void onRequestReceived(Request request, Response response) {
        EXCHANGES.add(new CapturedExchange(
                request.getMethod().getName(),
                request.getUrl(),
                response.getStatus(),
                formatRequest(request),
                formatResponse(response)
        ));
    }

    /** Called from {@link AllureWireMockTestListener#afterTestMethod} on the test thread. */
    static void flushToAllure() {
        CapturedExchange exchange;
        while ((exchange = EXCHANGES.poll()) != null) {
            String stepName = "WireMock request: " + exchange.method + " " + exchange.url
                    + " → " + exchange.status;
            String req = exchange.requestDetails;
            String resp = exchange.responseDetails;
            Allure.step(stepName, () -> {
                Allure.addAttachment("WireMock Request", "text/plain", req);
                Allure.addAttachment("WireMock Response", "text/plain", resp);
            });
        }
    }

    /** Clears any leftover data between tests. */
    static void clear() {
        EXCHANGES.clear();
    }

    private static String formatRequest(Request request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getMethod()).append(" ").append(request.getUrl()).append("\n");

        request.getHeaders().all().forEach(h ->
                sb.append(h.key()).append(": ").append(h.firstValue()).append("\n"));

        String body = request.getBodyAsString();
        if (body != null && !body.isEmpty()) {
            sb.append("\n").append(body);
        }
        return sb.toString();
    }

    private static String formatResponse(Response response) {
        StringBuilder sb = new StringBuilder();
        sb.append(response.getStatus()).append("\n");

        response.getHeaders().all().forEach(h ->
                sb.append(h.key()).append(": ").append(h.firstValue()).append("\n"));

        String body = response.getBodyAsString();
        if (body != null && !body.isEmpty()) {
            sb.append("\n").append(body);
        }
        return sb.toString();
    }

    private record CapturedExchange(String method, String url, int status,
                                    String requestDetails, String responseDetails) {
    }

    private AllureWireMockListener() {
    }
}
