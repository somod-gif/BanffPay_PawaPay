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
                        .version("3.0.0")
                        .description("""
                                Mobile money payment gateway via PawaPay API v2.
                                Supports deposits (collection) and payouts (disbursement) across 9 African countries.
                                
                                ## Architecture
                                - **Provider:** PawaPay (single payment provider)
                                - **Networks:** MTN MoMo, Airtel Money, M-Pesa, etc. (mobile money networks)
                                - **Countries:** 9 supported with automatic network routing
                                
                                ## Country Routing
                                Users submit `{"country": "KENYA", "phoneNumber": "...", "amount": 100}`.
                                The backend automatically routes to the default mobile money network via CountryRoutingService.
                                
                                ## Supported Countries (9)
                                | Country | ISO2 | ISO3 | Currency | Default Network |
                                |---------|------|------|----------|----------------|
                                | Uganda | UG | UGA | UGX | MTN_MOMO_UGA |
                                | Zambia | ZM | ZMB | ZMW | MTN_MOMO_ZMB |
                                | Rwanda | RW | RWA | RWF | MTN_MOMO_RWA |
                                | Tanzania | TZ | TZA | TZS | AIRTEL_TZA |
                                | Kenya | KE | KEN | KES | MPESA_KEN |
                                | Nigeria | NG | NGA | NGN | MTN_MOMO_NG |
                                | South Africa | ZA | ZAF | ZAR | VODACOM_ZA |
                                | Cameroon | CM | CMR | XAF | MTN_MOMO_CMR |
                                | Benin | BJ | BEN | XOF | MTN_MOMO_BEN |
                                
                                ## API Standardization
                                - All responses wrapped in `ApiResponse<T>` generic envelope
                                - `201 CREATED` for successful resource creation
                                - `202 ACCEPTED` for unmatched webhooks
                                - `400 BAD REQUEST` for validation errors
                                - `409 CONFLICT` for duplicate/idempotency conflicts
                                - `500 INTERNAL SERVER ERROR` for unexpected failures
                                
                                ## Webhook Features
                                - **Idempotency:** Send `X-Correlation-ID` header to prevent duplicate processing
                                - **6 Scenarios:** Deposit/Payout × Completed/Failed/Pending
                                - **Scheduled Reconciliation:** Every 5 minutes
                                - **Structured Logging:** SLF4J with correlation IDs, no sensitive data in logs""")
                        .contact(new Contact()
                                .name("BanffPay")
                                .email("support@banffpay.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://tasty-porcupine-cabdriver.ngrok-free.dev").description("Ngrok Tunnel (Webhooks)")
                ));
    }
}