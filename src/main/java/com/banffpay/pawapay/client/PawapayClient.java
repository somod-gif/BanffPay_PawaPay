package com.banffpay.pawapay.client;

import com.banffpay.pawapay.dto.PawapayDepositRequest;
import com.banffpay.pawapay.dto.PawapayDepositResponse;
import com.banffpay.pawapay.dto.PawapayPayoutRequest;
import com.banffpay.pawapay.dto.PawapayPayoutResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for communicating with the PawaPay v2 API.
 * <p>
 * <b>Refactored:</b> Uses strongly typed DTOs instead of {@code JsonNode} / {@code Map<String, Object>}.
 * All request/response payloads are now type-safe and validated at compile time.
 * </p>
 */
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
    public PawapayDepositResponse initiateDeposit(String depositId, String phoneNumber, String networkCode,
                                                   String amount, String currency, String clientReferenceId,
                                                   String customerMessage) {
        String url = properties.getBaseUrl() + properties.getDeposit().getEndpoint();

        PawapayDepositRequest request = PawapayDepositRequest.builder()
                .depositId(depositId)
                .payer(PawapayDepositRequest.Payer.builder()
                        .type("MMO")
                        .accountDetails(PawapayDepositRequest.AccountDetails.builder()
                                .phoneNumber(normalizePhone(phoneNumber))
                                .provider(networkCode)
                                .build())
                        .build())
                .amount(amount)
                .currency(currency)
                .clientReferenceId(clientReferenceId)
                .customerMessage(sanitizeMessage(customerMessage))
                .build();

        return doPost(url, request, PawapayDepositResponse.class, depositId);
    }

    /**
     * Initiate a payout with PawaPay v2 API.
     * POST {baseUrl}/v2/payouts
     */
    public PawapayPayoutResponse initiatePayout(String payoutId, String phoneNumber, String networkCode,
                                                 String amount, String currency, String clientReferenceId,
                                                 String customerMessage) {
        String url = properties.getBaseUrl() + properties.getPayout().getEndpoint();

        PawapayPayoutRequest request = PawapayPayoutRequest.builder()
                .payoutId(payoutId)
                .recipient(PawapayPayoutRequest.Recipient.builder()
                        .type("MMO")
                        .accountDetails(PawapayPayoutRequest.AccountDetails.builder()
                                .phoneNumber(normalizePhone(phoneNumber))
                                .provider(networkCode)
                                .build())
                        .build())
                .amount(amount)
                .currency(currency)
                .clientReferenceId(clientReferenceId)
                .customerMessage(sanitizeMessage(customerMessage))
                .build();

        return doPost(url, request, PawapayPayoutResponse.class, payoutId);
    }

    /**
     * Check deposit status from PawaPay v2.
     * GET {baseUrl}/v2/deposits/{depositId}
     */
    public PawapayDepositResponse checkDepositStatus(String depositId) {
        String url = properties.getBaseUrl() + properties.getDeposit().getEndpoint() + "/" + depositId;
        return doGet(url, PawapayDepositResponse.class, depositId);
    }

    /**
     * Check payout status from PawaPay v2.
     * GET {baseUrl}/v2/payouts/{payoutId}
     */
    public PawapayPayoutResponse checkPayoutStatus(String payoutId) {
        String url = properties.getBaseUrl() + properties.getPayout().getEndpoint() + "/" + payoutId;
        return doGet(url, PawapayPayoutResponse.class, payoutId);
    }

    private <T> T doPost(String url, Object requestBody, Class<T> responseType, String idempotencyKey) {
        try {
            HttpHeaders headers = createHeaders(idempotencyKey);
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            log.info("PawaPay POST {} | idempotencyKey={}", url, idempotencyKey);
            log.debug("PawaPay request body: {}", jsonBody);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            log.info("PawaPay response status: {} for idempotencyKey={}",
                    response.getStatusCodeValue(), idempotencyKey);

            return objectMapper.readValue(response.getBody(), responseType);
        } catch (Exception e) {
            log.error("PawaPay POST failed for idempotencyKey={}: {}", idempotencyKey, e.getMessage());
            throw new RuntimeException("PawaPay API error: " + e.getMessage(), e);
        }
    }

    private <T> T doGet(String url, Class<T> responseType, String idempotencyKey) {
        try {
            HttpHeaders headers = createHeaders(idempotencyKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.info("PawaPay GET {} | idempotencyKey={}", url, idempotencyKey);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            log.info("PawaPay GET response status: {} for idempotencyKey={}",
                    response.getStatusCodeValue(), idempotencyKey);

            return objectMapper.readValue(response.getBody(), responseType);
        } catch (Exception e) {
            log.error("PawaPay GET failed for idempotencyKey={}: {}", idempotencyKey, e.getMessage());
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