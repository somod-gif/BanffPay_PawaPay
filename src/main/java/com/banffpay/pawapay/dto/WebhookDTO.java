package com.banffpay.pawapay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Strongly typed DTO for PawaPay webhook callback requests.
 * <p>
 * Uses strongly typed enums for type safety instead of raw strings.
 * </p>
 */
@Data
@Schema(description = "PawaPay webhook callback request")
public class WebhookDTO {

    @NotBlank(message = "pawapayId is required")
    @Schema(description = "PawaPay transaction ID (depositId or payoutId)",
            example = "f4401bd2-1568-4140-bf2d-eb77d2b2b639")
    private String pawapayId;

    @NotBlank(message = "type is required")
    @Pattern(regexp = "DEPOSIT|PAYOUT", message = "type must be either DEPOSIT or PAYOUT")
    @Schema(description = "Transaction type", example = "DEPOSIT",
            allowableValues = {"DEPOSIT", "PAYOUT"})
    private String type;

    @NotBlank(message = "status is required")
    @Pattern(regexp = "ACCEPTED|PROCESSING|COMPLETED|FAILED|REJECTED|CANCELLED",
            message = "status must be one of: ACCEPTED, PROCESSING, COMPLETED, FAILED, REJECTED, CANCELLED")
    @Schema(description = "Transaction status", example = "COMPLETED",
            allowableValues = {"ACCEPTED", "PROCESSING", "COMPLETED", "FAILED", "REJECTED", "CANCELLED"})
    private String status;

    @Schema(description = "Optional correlation ID for idempotency",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String correlationId;
}