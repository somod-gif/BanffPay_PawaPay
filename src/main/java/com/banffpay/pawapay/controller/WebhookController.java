package com.banffpay.pawapay.controller;

import com.banffpay.pawapay.config.CorrelationIdFilter;
import com.banffpay.pawapay.dto.WebhookRequest;
import com.banffpay.pawapay.service.WebhookResult;
import com.banffpay.pawapay.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "PawaPay webhook callback endpoints for deposits and payouts")
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/pawapay")
    @Operation(
            summary = "Receive PawaPay webhook callback",
            description = "Processes webhook callbacks from PawaPay for deposits and payouts across all supported countries. " +
                    "Handles 6 scenarios: Deposit/Payout x Completed/Failed/Pending. " +
                    "Supports idempotency via X-Correlation-ID header. Duplicate webhooks with the " +
                    "same correlationId return 200 OK with duplicate: true flag. " +
                    "Unmatched webhooks (unknown pawapayId) return 202 Accepted for reconciliation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deposit Completed",
                    content = @Content(schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(name = "DepositCompleted",
                                    value = "{\"success\":true,\"message\":\"Deposit completed successfully. Amount: 20 ZMW\",\"correlationId\":\"550e8400-e29b-41d4-a716-446655440000\",\"duplicate\":false,\"unmatched\":false,\"timestamp\":\"2026-06-16T22:00:00\"}"))),
            @ApiResponse(responseCode = "200", description = "Deposit Failed",
                    content = @Content(schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(name = "DepositFailed",
                                    value = "{\"success\":true,\"message\":\"Deposit failed. Status: FAILED\",\"correlationId\":\"550e8400-e29b-41d4-a716-446655440001\",\"duplicate\":false,\"unmatched\":false,\"timestamp\":\"2026-06-16T22:01:00\"}"))),
            @ApiResponse(responseCode = "200", description = "Deposit Pending",
                    content = @Content(schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(name = "DepositPending",
                                    value = "{\"success\":true,\"message\":\"Deposit is PROCESSING. Will be reconciled automatically.\",\"correlationId\":\"550e8400-e29b-41d4-a716-446655440002\",\"duplicate\":false,\"unmatched\":false,\"timestamp\":\"2026-06-16T22:02:00\"}"))),
            @ApiResponse(responseCode = "200", description = "Payout Completed",
                    content = @Content(schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(name = "PayoutCompleted",
                                    value = "{\"success\":true,\"message\":\"Payout completed successfully. Amount: 50 ZMW\",\"correlationId\":\"550e8400-e29b-41d4-a716-446655440003\",\"duplicate\":false,\"unmatched\":false,\"timestamp\":\"2026-06-16T22:03:00\"}"))),
            @ApiResponse(responseCode = "200", description = "Payout Failed",
                    content = @Content(schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(name = "PayoutFailed",
                                    value = "{\"success\":true,\"message\":\"Payout failed. Status: FAILED\",\"correlationId\":\"550e8400-e29b-41d4-a716-446655440004\",\"duplicate\":false,\"unmatched\":false,\"timestamp\":\"2026-06-16T22:04:00\"}"))),
            @ApiResponse(responseCode = "200", description = "Payout Pending",
                    content = @Content(schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(name = "PayoutPending",
                                    value = "{\"success\":true,\"message\":\"Payout is PROCESSING. Will be reconciled automatically.\",\"correlationId\":\"550e8400-e29b-41d4-a716-446655440005\",\"duplicate\":false,\"unmatched\":false,\"timestamp\":\"2026-06-16T22:05:00\"}"))),
            @ApiResponse(responseCode = "200", description = "Duplicate webhook (idempotent response)",
                    content = @Content(schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(name = "Duplicate",
                                    value = "{\"success\":true,\"message\":\"Webhook already processed with correlationId: 550e8400-e29b-41d4-a716-446655440000\",\"correlationId\":\"550e8400-e29b-41d4-a716-446655440000\",\"duplicate\":true,\"unmatched\":false,\"timestamp\":\"2026-06-16T22:06:00\"}"))),
            @ApiResponse(responseCode = "202", description = "Accepted but transaction not yet found (pending reconciliation)",
                    content = @Content(schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(name = "Unmatched",
                                    value = "{\"success\":false,\"message\":\"Transaction not found for pawapayId: 00000000-0000-0000-0000-000000000000\",\"correlationId\":\"550e8400-e29b-41d4-a716-446655440006\",\"duplicate\":false,\"unmatched\":true,\"timestamp\":\"2026-06-16T22:07:00\"}"))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> processWebhook(
            @Valid @RequestBody WebhookRequest request,
            HttpServletRequest httpRequest) {
            log.info("[WEBHOOK_RECEIVED] pawapayId={} type={} status={} correlationId={}",
                    request.getPawapayId(), request.getType(), request.getStatus(), httpRequest.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));

        // Resolve correlation ID from request header or generate new one
        String correlationId = resolveCorrelationId(httpRequest);

        WebhookResult result = webhookService.processWebhook(request, correlationId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        response.put("correlationId", result.getCorrelationId());
        response.put("duplicate", result.isDuplicate());
        response.put("unmatched", result.isUnmatched());
        response.put("timestamp", LocalDateTime.now());

        // Return 200 for success, 202 for unmatched (accepted but async processing needed)
        HttpStatus status = result.isUnmatched() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Resolves the correlation ID from the request header or generates a new one.
     */
    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId.trim();
        }
        return java.util.UUID.randomUUID().toString();
    }
}