package com.banffpay.pawapay.client;

import com.banffpay.pawapay.dto.PawapayDepositResponseDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class PawapayClient {

    private final PawapayProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PawapayClient(PawapayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    /**
     * Initiate a deposit with PawaPay v2 API.
     * POST {baseUrl}/v2/deposits
     */
    public JsonNode initiateDeposit(String depositId, String phoneNumber, String provider,
                                     String amount, String currency, String clientReferenceId,
                                     String customerMessage) {
        String url = properties.getBaseUrl() + properties.getDeposit().getEndpoint();

        Map<String, Object> body = Map.of(
                "depositId", depositId,
                "payer", Map.of(
                        "type", "MMO",
                        "accountDetails", Map.of(
                                "phoneNumber", normalizePhone(phoneNumber),
                                "provider", provider
                        )
                ),
                "amount", amount,
                "currency", currency,
                "clientReferenceId", clientReferenceId,
                "customerMessage", sanitizeMessage(customerMessage)
        );

        return doPost(url, body, depositId);
    }

    /**
     * Initiate a payout with PawaPay v2 API.
     * POST {baseUrl}/v2/payouts
     */
    public JsonNode initiatePayout(String payoutId, String phoneNumber, String provider,
                                    String amount, String currency, String clientReferenceId,
                                    String customerMessage) {
        String url = properties.getBaseUrl() + properties.getPayout().getEndpoint();

        Map<String, Object> body = Map.of(
                "payoutId", payoutId,
                "recipient", Map.of(
                        "type", "MMO",
                        "accountDetails", Map.of(
                                "phoneNumber", normalizePhone(phoneNumber),
                                "provider", provider
                        )
                ),
                "amount", amount,
                "currency", currency,
                "clientReferenceId", clientReferenceId,
                "customerMessage", sanitizeMessage(customerMessage)
        );

        return doPost(url, body, payoutId);
    }

    /**
     * Check deposit status from PawaPay v2.
     * GET {baseUrl}/v2/deposits/{depositId}
     */
    public PawapayDepositResponseDto checkDepositStatus(String depositId) {
        String url = properties.getBaseUrl() + properties.getDeposit().getEndpoint() + "/" + depositId;
        return doGet(url, depositId);
    }

    /**
     * Check payout status from PawaPay v2.
     * GET {baseUrl}/v2/payouts/{payoutId}
     */
    public JsonNode checkPayoutStatus(String payoutId) {
        String url = properties.getBaseUrl() + properties.getPayout().getEndpoint() + "/" + payoutId;
        return doGet(url, payoutId);
    }

    private <T,R> T doPost(String url, R body, String idempotencyKey) {
        try {
            HttpHeaders headers = createHeaders(idempotencyKey);
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            log.info("PawaPay POST {} | idempotencyKey={}", url, idempotencyKey);
            log.debug("PawaPay request body: {}", jsonBody);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            log.info("PawaPay response: {} {}", response.getStatusCodeValue(), response.getBody());
            return objectMapper.readValue(response.getBody(), new TypeReference<T>() {});
        } catch (Exception e) {
            log.error("PawaPay POST failed: {}", e.getMessage(), e);
            throw new RuntimeException("PawaPay API error: " + e.getMessage(), e);
        }
    }

    private <T> T doGet(String url, String idempotencyKey) {
        try {
            HttpHeaders headers = createHeaders(idempotencyKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.info("PawaPay GET {} | idempotencyKey={}", url, idempotencyKey);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            log.info("PawaPay response: {} {}", response.getStatusCodeValue(), response.getBody());
            return objectMapper.readValue(response.getBody(), new TypeReference<T>() {});
        } catch (Exception e) {
            log.error("PawaPay GET failed: {}", e.getMessage(), e);
            throw new RuntimeException("PawaPay API error: " + e.getMessage(), e);
        }
    }

    private HttpHeaders createHeaders(String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + properties.getApiKey());
        headers.set("X-Idempotency-Key", idempotencyKey);
        return headers;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("[^0-9]", "");
    }

    /**
     * Sanitize message: only alphanumeric + spaces, min 4 max 22 chars (PawaPay requirement).
     */
    private String sanitizeMessage(String message) {
        if (message == null) return "Payment";
        String sanitized = message.replaceAll("[^a-zA-Z0-9 ]", "");
        if (sanitized.length() < 4) sanitized = "Payment";
        return sanitized.length() > 22 ? sanitized.substring(0, 22) : sanitized;
    }
}