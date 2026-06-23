package com.banffpay.pawapay.controller;

import com.banffpay.pawapay.config.CorrelationIdFilter;
import com.banffpay.pawapay.dto.ApiResponse;
import com.banffpay.pawapay.dto.WebhookDTO;
import com.banffpay.pawapay.service.WebhookResult;
import com.banffpay.pawapay.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "PawaPay webhook callback endpoints")
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/pawapay")
    @Operation(
            summary = "Receive PawaPay webhook callback",
            description = "Processes webhook callbacks from PawaPay. Supports idempotency via X-Correlation-ID header."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook processed successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "Completed", value = """
                                    {"success":true,"message":"Deposit completed successfully","data":{"correlationId":"550e8400-e29b-41d4-a716-446655440000","duplicate":false,"unmatched":false},"timestamp":"2026-06-22T20:00:00"}"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Accepted but transaction not yet found (pending reconciliation)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "Unmatched", value = """
                                    {"success":false,"message":"Transaction not found for pawapayId: ...","data":{"correlationId":"...","duplicate":false,"unmatched":true},"timestamp":"2026-06-22T20:00:00"}"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<WebhookResultData>> processWebhook(
            @Valid @RequestBody WebhookDTO request,
            HttpServletRequest httpRequest) {

        String correlationId = resolveCorrelationId(httpRequest);

        log.info("Webhook received. transactionId={} type={} status={} correlationId={}",
                request.getPawapayId(), request.getType(), request.getStatus(), correlationId);

        WebhookResult result = webhookService.processWebhook(request, correlationId);

        WebhookResultData data = new WebhookResultData(
                result.getCorrelationId(),
                result.isDuplicate(),
                result.isUnmatched()
        );

        if (result.isUnmatched()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.<WebhookResultData>builder()
                            .success(false)
                            .message(result.getMessage())
                            .data(data)
                            .correlationId(correlationId)
                            .build());
        }

        return ResponseEntity.ok(
                ApiResponse.<WebhookResultData>builder()
                        .success(result.isSuccess())
                        .message(result.getMessage())
                        .data(data)
                        .correlationId(correlationId)
                        .build()
        );
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId.trim();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Data record for webhook result responses.
     */
    public record WebhookResultData(
            String correlationId,
            boolean duplicate,
            boolean unmatched
    ) {}
}