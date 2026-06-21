package com.banffpay.pawapay.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Yusuf Olosan
 * @role software engineer
 * @createdOn 20 Sat Jun, 2026
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PawapayDepositResponseDto {
    private String depositId;
    private PawapayStatus status;
    private FailureReason failureReason;
    private String created;

    public boolean isSuccessful() {
        return this.status == PawapayStatus.COMPLETED;
    }

    public boolean isPending() {
        return this.status == PawapayStatus.ACCEPTED || this.status == PawapayStatus.PROCESSING;
    }

    public boolean isFailed() {
        return this.status == PawapayStatus.PROVIDER_TEMPORARILY_UNAVAILABLE ||
               this.status == PawapayStatus.INVALID_AMOUNT ||
               this.status == PawapayStatus.INVALID_PHONE_NUMBER ||
               this.status == PawapayStatus.INVALID_CURRENCY ||
               this.status == PawapayStatus.AMOUNT_OUT_OF_BOUNDS;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FailureReason {
        private String failureCode;
        private String failureMessage;
    }

    public String message(){
        if(this.isFailed()){
            return String.format("%s: %s",this.failureReason.getFailureCode(), this.failureReason.getFailureMessage());
        }
        return null;
    }
}
