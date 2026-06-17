package com.banffpay.pawapay.dto;

import com.banffpay.pawapay.model.TransactionStatus;
import com.banffpay.pawapay.model.TransactionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Strongly typed DTO for PawaPay webhook callback requests.
 * Uses enums instead of raw strings for type safety.
 */
@Data
@Schema(description = "PawaPay webhook callback request")
public class WebhookRequest {

    @NotBlank(message = "pawapayId is required")
    @Schema(description = "PawaPay transaction ID (depositId or payoutId)",
            example = "f4401bd2-1568-4140-bf2d-eb77d2b2b639")
    private String pawapayId;

    @NotNull(message = "type is required")
    @Pattern(regexp = "DEPOSIT|PAYOUT", message = "type must be either DEPOSIT or PAYOUT")
    @Schema(description = "Transaction type",
            example = "DEPOSIT",
            allowableValues = {"DEPOSIT", "PAYOUT"})
    private String type;

    @NotNull(message = "status is required")
    @Pattern(regexp = "ACCEPTED|PROCESSING|COMPLETED|FAILED|REJECTED|CANCELLED",
            message = "status must be one of: ACCEPTED, PROCESSING, COMPLETED, FAILED, REJECTED, CANCELLED")
    @Schema(description = "Transaction status",
            example = "COMPLETED",
            allowableValues = {"ACCEPTED", "PROCESSING", "COMPLETED", "FAILED", "REJECTED", "CANCELLED"})
    private String status;

    @Schema(description = "Optional correlation ID for idempotency (if not provided, will be generated)",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String correlationId;

    @Schema(description = "Raw payload hash for signature verification (optional)",
            example = "sha256:abc123...")
    private String signature;

    /**
     * Converts the string type to TransactionType enum.
     */
    public TransactionType getTransactionType() {
        if (type == null) {
            return null;
        }
        return TransactionType.fromValue(type);
    }

    /**
     * Converts the string status to TransactionStatus enum.
     */
    public TransactionStatus getTransactionStatus() {
        if (status == null) {
            return null;
        }
        return TransactionStatus.fromValue(status);
    }
}