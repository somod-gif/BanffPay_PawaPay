package com.banffpay.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Strongly typed DTO for PawaPay v2 payout API requests.
 * Replaces the previous Map/JsonNode approach.
 */
@Value
@Builder
public class PawapayPayoutRequest {

    @JsonProperty("payoutId")
    String payoutId;

    @JsonProperty("recipient")
    Recipient recipient;

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
    public static class Recipient {
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