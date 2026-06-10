package com.banffpay.pawapay.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "pawapay")
public class PawapayProperties {

    private String baseUrl = "https://api.pawapay.io";
    private String apiKey;
    private DepositConfig deposit = new DepositConfig();
    private PayoutConfig payout = new PayoutConfig();
    private int timeout = 30;
    private int maxRetries = 3;

    @Data
    public static class DepositConfig {
        private String endpoint = "/v2/deposits";
        private boolean idempotencyHeader = true;
    }

    @Data
    public static class PayoutConfig {
        private String endpoint = "/v2/payouts";
        private boolean idempotencyHeader = true;
    }
}