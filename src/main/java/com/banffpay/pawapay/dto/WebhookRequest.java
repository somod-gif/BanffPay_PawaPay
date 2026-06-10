package com.banffpay.pawapay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "PawaPay webhook callback request")
public class WebhookRequest {

    @NotBlank(message = "pawapayId is required")
    @Schema(description = "PawaPay transaction ID (depositId or payoutId)", example = "f4401bd2-1568-4140-bf2d-eb77d2b2b639")
    private String pawapayId;

    @NotBlank(message = "type is required")
    @Schema(description = "Transaction type", example = "DEPOSIT", allowableValues = {"DEPOSIT", "PAYOUT"})
    private String type;

    @NotBlank(message = "status is required")
    @Schema(description = "Transaction status", example = "COMPLETED", allowableValues = {"ACCEPTED", "PROCESSING", "COMPLETED", "FAILED", "REJECTED", "CANCELLED"})
    private String status;
}