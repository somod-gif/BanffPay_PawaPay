package com.banffpay.pawapay.controller;

import com.banffpay.pawapay.dto.ApiResponse;
import com.banffpay.pawapay.dto.PayoutRequestDTO;
import com.banffpay.pawapay.dto.PayoutResponseDTO;
import com.banffpay.pawapay.service.PayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payouts")
@RequiredArgsConstructor
@Tag(name = "Payouts", description = "Mobile money payout disbursement endpoints")
public class PayoutController {

    private final PayoutService payoutService;

    @PostMapping
    @Operation(summary = "Initiate a payout",
            description = "Creates a new mobile money payout. Country is routed to the appropriate mobile network automatically.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payout initiated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(name = "ZambiaPayout", value = """
                    {
                      "success": true,
                      "message": "Payout initiated successfully",
                      "data": {
                        "transactionId": "92ad6942-c1c1-48fb-b5a3-ee388db8443f",
                        "merchantTransactionId": "PAY-001",
                        "customerName": "Jane Doe",
                        "pawapayId": "f60bf205-8d39-444c-9836-a1458eb0d92c",
                        "type": "PAYOUT",
                        "status": "ACCEPTED",
                        "amount": 50.00,
                        "currency": "ZMW",
                        "phoneNumber": "260763456789",
                        "country": "ZM",
                        "network": "MTN_MOMO_ZMB",
                        "createdAt": "2026-06-22T20:00:00"
                      },
                      "timestamp": "2026-06-22T20:00:00"
                    }"""))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"success":false,"message":"Invalid country code: 'XX'","data":null,"timestamp":"2026-06-22T20:00:00"}"""))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate transaction"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<PayoutResponseDTO>> initiatePayout(
            @Valid @RequestBody PayoutRequestDTO request) {
        PayoutResponseDTO response = payoutService.initiatePayout(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payout initiated successfully", response));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get payout status")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payout status retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<PayoutResponseDTO>> getPayoutStatus(
            @PathVariable String transactionId) {
        PayoutResponseDTO response = payoutService.getPayoutStatus(transactionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}