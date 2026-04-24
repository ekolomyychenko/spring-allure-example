package com.example.order.allure.wiremock;

import com.example.order.allure.AllureInstrumentationLogger;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discovers {@code WireMockServer} instances in test classes via reflection and:
 * <ul>
 *   <li>registers a {@link AllureWireMockListener} for request/response logging</li>
 *   <li>logs registered stubs as Allure steps (prepended to the beginning of the test)</li>
 * </ul>
 * No test code changes required. Registered via {@code META-INF/spring.factories}.
 */
public class AllureWireMockTestListener implements TestExecutionListener {

    private static final Set<Integer> REGISTERED = ConcurrentHashMap.newKeySet();

    @Override
    public void beforeTestMethod(TestContext testContext) {
        AllureWireMockListener.clear();
        registerListeners(testContext.getTestClass());
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        AllureWireMockListener.flushToAllure();
        prependStubSteps(testContext.getTestClass());
    }

    private void registerListeners(Class<?> clazz) {
        for (WireMockServer server : findServers(clazz)) {
            if (REGISTERED.add(System.identityHashCode(server))) {
                server.addMockServiceRequestListener(
                        AllureWireMockListener::onRequestReceived);
            }
        }
    }

    private void prependStubSteps(Class<?> clazz) {
        List<StepResult> stubSteps = new ArrayList<>();
        for (WireMockServer server : findServers(clazz)) {
            for (StubMapping stub : server.getStubMappings()) {
                stubSteps.add(buildStubStep(stub));
            }
        }
        if (!stubSteps.isEmpty()) {
            Allure.getLifecycle().updateTestCase(testResult ->
                    testResult.getSteps().addAll(0, stubSteps));
        }
    }

    private StepResult buildStubStep(StubMapping stub) {
        String method = "";
        String urlPattern = "";
        if (stub.getRequest() != null) {
            if (stub.getRequest().getMethod() != null) {
                method = stub.getRequest().getMethod().getName();
            }
            urlPattern = firstNonNull(
                    stub.getRequest().getUrlPathPattern(),
                    stub.getRequest().getUrlPath(),
                    stub.getRequest().getUrl(),
                    stub.getRequest().getUrlPattern());
        }

        int status = stub.getResponse() != null ? stub.getResponse().getStatus() : 200;
        String stepName = "WireMock stub: " + method + " " + urlPattern + " → " + status;

        StepResult stepResult = new StepResult()
                .setName(stepName)
                .setStatus(Status.PASSED);

        stepResult.getAttachments().add(
                new io.qameta.allure.model.Attachment()
                        .setName("Stub mapping")
                        .setType("text/plain")
                        .setSource(writeAttachment(formatStubDetails(stub, method, urlPattern, status))));

        return stepResult;
    }

    private String writeAttachment(String content) {
        String source = UUID.randomUUID().toString();
        Allure.getLifecycle().writeAttachment(source, new java.io.ByteArrayInputStream(
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return source;
    }

    private List<WireMockServer> findServers(Class<?> clazz) {
        List<WireMockServer> servers = new ArrayList<>();
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            for (Field field : c.getDeclaredFields()) {
                if (WireMockServer.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        WireMockServer server = (WireMockServer) field.get(null);
                        if (server != null && server.isRunning()) {
                            servers.add(server);
                        }
                    } catch (Exception e) {
                        AllureInstrumentationLogger.warn("WireMockDiscovery", e);
                    }
                }
            }
            c = c.getSuperclass();
        }
        return servers;
    }

    private String formatStubDetails(StubMapping stub, String method, String url, int status) {
        StringBuilder sb = new StringBuilder();
        sb.append("Request:\n");
        sb.append("  Method: ").append(method).append("\n");
        sb.append("  URL: ").append(url).append("\n");
        if (stub.getRequest() != null && stub.getRequest().getQueryParameters() != null) {
            stub.getRequest().getQueryParameters().forEach((key, pattern) -> {
                String value;
                try {
                    value = pattern.getExpected();
                } catch (Exception e) {
                    value = key;
                }
                sb.append("  Query: ").append(key).append(" = ").append(value).append("\n");
            });
        }
        sb.append("\nResponse:\n");
        sb.append("  Status: ").append(status).append("\n");
        if (stub.getResponse() != null) {
            if (stub.getResponse().getBody() != null) {
                sb.append("  Body: ").append(stub.getResponse().getBody()).append("\n");
            }
            if (stub.getResponse().getFixedDelayMilliseconds() != null) {
                sb.append("  Delay: ").append(stub.getResponse().getFixedDelayMilliseconds()).append("ms\n");
            }
        }
        return sb.toString();
    }

    private static String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null) return v;
        }
        return "";
    }
}
