package com.banffpay.pawapay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Deposit request from client")
public class DepositRequest {

    @NotBlank(message = "merchantTransactionId is required")
    private String merchantTransactionId;

    @NotBlank(message = "customerName is required")
    private String customerName;

    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^[0-9]{7,15}$", message = "phoneNumber must be 7-15 digits")
    private String phoneNumber;

    @NotBlank(message = "country is required")
    @Pattern(regexp = "^[A-Za-z]{2,3}$", message = "country must be ISO2 or ISO3 code")
    private String country;

    @NotBlank(message = "amount is required")
    private String amount;

    @NotBlank(message = "currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be 3-letter ISO code")
    @Schema(description = "Currency code (backend-validated against country)", example = "ZMW", allowableValues = {"ZMW", "UGX", "KES", "NGN", "ZAR", "TZS", "RWF"})
    private String currency;

    @NotBlank(message = "provider is required")
    @Schema(description = "Mobile money provider", example = "MTN_MOMO_ZMB")
    private String provider;
}
