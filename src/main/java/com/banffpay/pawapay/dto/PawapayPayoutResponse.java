package com.banffpay.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Strongly typed DTO for PawaPay v2 payout API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PawapayPayoutResponse {

    @JsonProperty("payoutId")
    private String payoutId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("failureReason")
    private FailureReason failureReason;

    @JsonProperty("created")
    private String created;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FailureReason {
        @JsonProperty("failureCode")
        private String failureCode;

        @JsonProperty("failureMessage")
        private String failureMessage;
    }
}