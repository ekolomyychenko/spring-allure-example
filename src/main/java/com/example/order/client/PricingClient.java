package com.example.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
public class PricingClient {

    private final RestTemplate restTemplate;
    private final String pricingServiceUrl;

    public PricingClient(RestTemplate restTemplate,
                         @Value("${pricing-service.url}") String pricingServiceUrl) {
        this.restTemplate = restTemplate;
        this.pricingServiceUrl = pricingServiceUrl;
    }

    public BigDecimal getPrice(String productName) {
        String url = pricingServiceUrl + "/api/prices?product=" + productName;
        PriceResponse response = restTemplate.getForObject(url, PriceResponse.class);
        if (response == null) {
            throw new RuntimeException("Pricing service returned null for product: " + productName);
        }
        return response.price();
    }

    public record PriceResponse(BigDecimal price) {}
}
