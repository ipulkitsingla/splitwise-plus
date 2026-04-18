package com.splitwiseplusplus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Currency conversion service with caching.
 * Uses exchangerate-api.com; falls back to 1:1 rate on error.
 */
@Service
@Slf4j
public class CurrencyService {

    @Value("${app.currency.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Cacheable(value = "exchangeRates", key = "#from + '-' + #to")
    public BigDecimal getExchangeRate(String from, String to) {
        if (from.equalsIgnoreCase(to)) return BigDecimal.ONE;

        try {
            String url = apiUrl + "/" + from.toUpperCase();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("rates")) {
                @SuppressWarnings("unchecked")
                Map<String, Number> rates = (Map<String, Number>) response.get("rates");
                Number rate = rates.get(to.toUpperCase());
                if (rate != null) {
                    return BigDecimal.valueOf(rate.doubleValue()).setScale(6, RoundingMode.HALF_UP);
                }
            }
        } catch (Exception e) {
            log.warn("Currency conversion failed ({} -> {}): {}. Using 1:1 rate.", from, to, e.getMessage());
        }

        return BigDecimal.ONE; // Safe fallback
    }
}
