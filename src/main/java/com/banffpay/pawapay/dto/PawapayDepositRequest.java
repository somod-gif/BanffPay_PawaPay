package com.banffpay.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Strongly typed DTO for PawaPay v2 deposit API requests.
 * Replaces the previous Map/JsonNode approach.
 */
@Value
@Builder
public class PawapayDepositRequest {

    @JsonProperty("depositId")
    String depositId;

    @JsonProperty("payer")
    Payer payer;

    @JsonProperty("amount")
    String amount;

    @JsonProperty("currency")
    String currency;

    @JsonProperty("clientReferenceId")
    String clientReferenceId;

    @JsonProperty("customerMessage")
    String customerMessage;

    @Value
    @Builder
    public static class Payer {
        @JsonProperty("type")
        String type;

        @JsonProperty("accountDetails")
        AccountDetails accountDetails;
    }

    @Value
    @Builder
    public static class AccountDetails {
        @JsonProperty("phoneNumber")
        String phoneNumber;

        @JsonProperty("provider")
        String provider;
    }
}