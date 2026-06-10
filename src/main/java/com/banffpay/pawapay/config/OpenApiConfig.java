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
                        .version("1.0.0")
                        .description("Mobile money payment gateway via PawaPay API v2. Supports deposits (collection) and payouts (disbursement) across multiple countries including Zambia (ZMW/MTN_MOMO_ZMB) and Uganda (UGX/MTN_MOMO_UGA).")
                        .contact(new Contact()
                                .name("BanffPay")
                                .email("support@banffpay.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://tasty-porcupine-cabdriver.ngrok-free.dev").description("Ngrok Tunnel (Webhooks)")
                ));
    }
}
