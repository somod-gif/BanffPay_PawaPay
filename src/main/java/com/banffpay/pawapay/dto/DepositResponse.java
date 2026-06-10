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
@Schema(description = "PawaPay deposit response")
public class DepositResponse {

    @Schema(description = "Deposit identifier", example = "f4401bd2-1568-4140-bf2d-eb77d2b2b639")
    private String depositId;

    @Schema(description = "Transaction status", example = "ACCEPTED")
    private String status;

    @Schema(description = "Creation timestamp", example = "2026-06-09T12:00:00Z")
    private String created;
}