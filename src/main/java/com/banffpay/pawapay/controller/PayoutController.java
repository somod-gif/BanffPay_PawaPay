package com.banffpay.pawapay.controller;

import com.banffpay.pawapay.dto.PayoutRequest;
import com.banffpay.pawapay.dto.TransactionResponse;
import com.banffpay.pawapay.service.PayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payouts")
@RequiredArgsConstructor
@Tag(name = "Payouts", description = "Mobile money payout endpoints")
public class PayoutController {

    private final PayoutService payoutService;

    @PostMapping
    @Operation(summary = "Initiate a payout", description = "Creates a new mobile money payout request and submits it to PawaPay for processing")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Success – Payout initiated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TransactionResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "transactionId": "7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02",
                      "merchantTransactionId": "INV-673476476",
                      "customerName": "Jane Doe",
                      "pawapayId": "c6601bd2-1568-4140-bf2d-eb77d2b2b222",
                      "type": "PAYOUT",
                      "status": "ACCEPTED",
                      "amount": 15,
                      "currency": "ZMW",
                      "createdAt": "2026-06-10T10:19:43.697Z"
                    }"""))),
        @ApiResponse(responseCode = "202", description = "Accepted – Payout is being processed"),
        @ApiResponse(responseCode = "400", description = "Bad Request – Validation error or invalid request",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"success":false,"error":"Invalid provider 'WRONG_PROVIDER' for currency ZMW. Expected: MTN_MOMO_ZMB","timestamp":"2026-06-10T10:00:00"}"""))),
        @ApiResponse(responseCode = "401", description = "Not Authenticated – Missing or invalid API key"),
        @ApiResponse(responseCode = "403", description = "Not Allowed – Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Not Found – Resource not found"),
        @ApiResponse(responseCode = "409", description = "Duplicate/Conflict – Duplicate transaction detected"),
        @ApiResponse(responseCode = "422", description = "Validation Rule Failed – Business validation failed"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error – Backend crashed"),
        @ApiResponse(responseCode = "502", description = "Bad Gateway – External service (PawaPay) failed"),
        @ApiResponse(responseCode = "503", description = "Service Down – Service temporarily unavailable"),
        @ApiResponse(responseCode = "504", description = "Gateway Timeout – External service timed out")
    })
    public ResponseEntity<TransactionResponse> initiatePayout(@Valid @RequestBody PayoutRequest request) {
        TransactionResponse response = payoutService.initiatePayout(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get payout status", description = "Retrieves the current status of a payout transaction")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Success – Payout status retrieved",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TransactionResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "transactionId": "7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02",
                      "merchantTransactionId": "INV-673476476",
                      "customerName": "Jane Doe",
                      "pawapayId": "c6601bd2-1568-4140-bf2d-eb77d2b2b222",
                      "type": "PAYOUT",
                      "status": "ACCEPTED",
                      "amount": 15,
                      "currency": "ZMW",
                      "createdAt": "2026-06-10T10:19:43.697Z"
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Bad Request – Invalid transaction ID"),
        @ApiResponse(responseCode = "401", description = "Not Authenticated"),
        @ApiResponse(responseCode = "403", description = "Not Allowed"),
        @ApiResponse(responseCode = "404", description = "Not Found – Transaction not found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error"),
        @ApiResponse(responseCode = "502", description = "Bad Gateway – PawaPay status check failed"),
        @ApiResponse(responseCode = "503", description = "Service Down"),
        @ApiResponse(responseCode = "504", description = "Gateway Timeout")
    })
    public ResponseEntity<TransactionResponse> getPayoutStatus(@PathVariable String transactionId) {
        TransactionResponse response = payoutService.getPayoutStatus(transactionId);
        return ResponseEntity.ok(response);
    }
}