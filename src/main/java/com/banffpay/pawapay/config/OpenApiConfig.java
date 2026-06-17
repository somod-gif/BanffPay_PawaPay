package com.banffpay.pawapay.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BanffPay PawaPay Integration API")
                        .version("2.0.0")
                        .description("""
                                Mobile money payment gateway via PawaPay API v2.
                                Supports deposits (collection) and payouts (disbursement) across 9 African countries.
                                
                                ## Supported Countries (9)
                                | Country | ISO2 | ISO3 | Currency | Providers |
                                |---------|------|------|----------|-----------|
                                | Uganda | UG | UGA | UGX | MTN_MOMO_UGA, AIRTEL_UGA |
                                | Zambia | ZM | ZMB | ZMW | MTN_MOMO_ZMB, AIRTEL_ZMB |
                                | Rwanda | RW | RWA | RWF | MTN_MOMO_RWA, AIRTEL_RWA |
                                | Tanzania | TZ | TZA | TZS | AIRTEL_TZA, VODACOM_TZA, TIGO_TZA, HALOTEL_TZA |
                                | Kenya | KE | KEN | KES | MPESA_KE, AIRTEL_KE, TKASH_KE |
                                | Nigeria | NG | NGA | NGN | MTN_MOMO_NG, AIRTEL_NG, GLO_NG, 9MOBILE_NG |
                                | South Africa | ZA | ZAF | ZAR | VODACOM_ZA, MTN_ZA, TELKOM_ZA |
                                | Cameroon | CM | CMR | XAF | MTN_MOMO_CMR, ORANGE_CMR |
                                | Benin | BJ | BEN | XOF | MTN_MOMO_BEN, MOOV_BEN |
                                
                                ## Webhook Features
                                - **Idempotency:** Send `X-Correlation-ID` header to prevent duplicate processing
                                - **6 Scenarios:** Deposit/Payout × Completed/Failed/Pending
                                - **Scheduled Reconciliation:** Every 5 minutes via @Scheduled
                                - **Audit Trail:** All webhooks logged to WebhookEventStore (in-memory) + audit log file
                                - **Correlation IDs:** Distributed tracing across all requests""")
                        .contact(new Contact()
                                .name("BanffPay")
                                .email("support@banffpay.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://tasty-porcupine-cabdriver.ngrok-free.dev").description("Ngrok Tunnel (Webhooks)")
                ));
    }
}
