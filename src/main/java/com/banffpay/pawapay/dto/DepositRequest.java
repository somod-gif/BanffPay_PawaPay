package com.banffpay.pawapay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Deposit request from client")
public class DepositRequest {

    @NotBlank(message = "merchantTransactionId is required")
    @Schema(description = "Client's unique transaction reference", example = "INV-260763456789")
    private String merchantTransactionId;

    @NotBlank(message = "customerName is required")
    @Schema(description = "Customer name", example = "Eniola")
    private String customerName;

    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^[0-9]{7,15}$", message = "phoneNumber must be 7-15 digits (MSISDN format)")
    @Schema(description = "Customer phone number in MSISDN format", example = "260763456789")
    private String phoneNumber;

    @NotBlank(message = "country is required")
    @Pattern(regexp = "^[A-Za-z]{2,3}$", message = "country must be 2-3 letter code")
    @Schema(description = "Country code (ISO2 or ISO3, case-insensitive)", example = "ZMB",
            allowableValues = {"ZM", "ZMB", "UG", "UGA", "RW", "RWA", "CM", "CMR", "TZ", "TZA", "BJ", "BEN"})
    private String country;

    @NotBlank(message = "currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be 3-letter ISO code")
    @Schema(description = "Currency code", example = "ZMW", allowableValues = {"ZMW", "UGX"})
    private String currency;

    @NotBlank(message = "amount is required")
    @Schema(description = "Deposit amount", example = "20")
    private String amount;

    @NotBlank(message = "provider is required")
    @Schema(description = "Mobile money provider", example = "MTN_MOMO_ZMB", allowableValues = {"MTN_MOMO_ZMB", "MTN_MOMO_UGA"})
    private String provider;
}