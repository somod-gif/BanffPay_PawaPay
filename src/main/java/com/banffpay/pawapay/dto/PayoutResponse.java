package com.banffpay.pawapay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "PawaPay payout response")
public class PayoutResponse {

    @Schema(description = "Payout identifier", example = "b5501bd2-1568-4140-bf2d-eb77d2b2b111")
    private String payoutId;

    @Schema(description = "Transaction status", example = "ACCEPTED")
    private String status;

    @Schema(description = "Creation timestamp", example = "2026-06-09T12:00:00Z")
    private String created;
}