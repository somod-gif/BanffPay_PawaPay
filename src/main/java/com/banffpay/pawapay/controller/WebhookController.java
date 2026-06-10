package com.banffpay.pawapay.controller;

import com.banffpay.pawapay.dto.WebhookRequest;
import com.banffpay.pawapay.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "PawaPay webhook endpoints")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/pawapay")
    @Operation(summary = "Receive PawaPay webhook", description = "Processes webhook callbacks from PawaPay")
    public ResponseEntity<Map<String, String>> processWebhook(@Valid @RequestBody WebhookRequest request) {
        String message = webhookService.processWebhook(request);
        return ResponseEntity.ok(Map.of("message", message));
    }
}