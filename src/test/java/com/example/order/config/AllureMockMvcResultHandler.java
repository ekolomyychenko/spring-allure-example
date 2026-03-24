package com.example.order.config;

import io.qameta.allure.Allure;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;

import java.io.UnsupportedEncodingException;

public class AllureMockMvcResultHandler implements ResultHandler {

    @Override
    public void handle(MvcResult result) throws Exception {
        MockHttpServletRequest req = result.getRequest();
        MockHttpServletResponse resp = result.getResponse();

        String query = req.getQueryString();
        String fullUri = query != null ? req.getRequestURI() + "?" + query : req.getRequestURI();
        String stepName = req.getMethod() + " " + fullUri + " → " + resp.getStatus();

        Allure.step(stepName, step -> {
            Allure.addAttachment("Request", "text/plain", formatRequest(req));
            Allure.addAttachment("Response", "text/plain", formatResponse(resp));
        });
    }

    private String formatRequest(MockHttpServletRequest req) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append(req.getMethod()).append(" ").append(req.getRequestURI());
        if (req.getQueryString() != null) {
            sb.append("?").append(req.getQueryString());
        }
        sb.append("\n");

        var headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            sb.append(name).append(": ").append(req.getHeader(name)).append("\n");
        }

        String body = req.getContentAsString();
        if (body != null && !body.isEmpty()) {
            sb.append("\n").append(body);
        }
        return sb.toString();
    }

    private String formatResponse(MockHttpServletResponse resp) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(resp.getStatus()).append("\n");

        for (String name : resp.getHeaderNames()) {
            sb.append(name).append(": ").append(resp.getHeader(name)).append("\n");
        }

        String body = resp.getContentAsString();
        if (!body.isEmpty()) {
            sb.append("\n").append(body);
        }
        return sb.toString();
    }
}
