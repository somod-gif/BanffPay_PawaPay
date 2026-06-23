package com.banffpay.pawapay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Strongly typed DTO for deposit (collection) requests.
 * <p>
 * <b>Key Design:</b> Users submit country, phone, and amount — NOT a provider/network code.
 * The backend automatically routes to the appropriate mobile money network
 * via {@link com.banffpay.pawapay.service.CountryRoutingService}.
 * </p>
 */
@Data
@Schema(description = "Deposit request from client")
public class DepositRequestDTO {

    @NotBlank(message = "merchantTransactionId is required")
    @Schema(description = "Client's unique transaction reference", example = "DEP-001")
    private String merchantTransactionId;

    @NotBlank(message = "customerName is required")
    @Schema(description = "Customer name", example = "Jane Doe")
    private String customerName;

    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^[0-9]{7,15}$", message = "phoneNumber must be 7-15 digits (MSISDN format)")
    @Schema(description = "Customer phone number in MSISDN format", example = "260763456789")
    private String phoneNumber;

    @NotBlank(message = "country is required")
    @Pattern(regexp = "^[A-Za-z]{2,3}$", message = "country must be ISO2 or ISO3 code (e.g., ZM, KEN)")
    @Schema(description = "Country code (ISO2 or ISO3)", example = "ZM")
    private String country;

    @NotBlank(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    @Schema(description = "Transaction amount", example = "100.00")
    private String amount;
}