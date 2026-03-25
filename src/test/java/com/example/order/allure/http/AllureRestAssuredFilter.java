package com.example.order.allure.http;

import io.qameta.allure.Allure;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

public class AllureRestAssuredFilter implements Filter {

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {

        Response response = ctx.next(requestSpec, responseSpec);

        String stepName = requestSpec.getMethod() + " " + requestSpec.getURI() + " → " + response.getStatusCode();

        Allure.step(stepName, step -> {
            Allure.addAttachment("Request", "text/plain", formatRequest(requestSpec));
            Allure.addAttachment("Response", "text/plain", formatResponse(response));
        });

        return response;
    }

    private String formatRequest(FilterableRequestSpecification req) {
        StringBuilder sb = new StringBuilder();
        sb.append(req.getMethod()).append(" ").append(req.getURI()).append("\n");

        req.getHeaders().forEach(h ->
                sb.append(h.getName()).append(": ").append(h.getValue()).append("\n"));

        Object body = req.getBody();
        if (body != null) {
            sb.append("\n").append(body);
        }
        return sb.toString();
    }

    private String formatResponse(Response resp) {
        StringBuilder sb = new StringBuilder();
        sb.append(resp.getStatusCode()).append(" ").append(resp.getStatusLine()).append("\n");

        resp.getHeaders().forEach(h ->
                sb.append(h.getName()).append(": ").append(h.getValue()).append("\n"));

        String body = resp.getBody().asString();
        if (body != null && !body.isEmpty()) {
            sb.append("\n").append(body);
        }
        return sb.toString();
    }
}
