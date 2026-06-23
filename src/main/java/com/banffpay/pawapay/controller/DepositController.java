package com.banffpay.pawapay.controller;

import com.banffpay.pawapay.dto.ApiResponse;
import com.banffpay.pawapay.dto.DepositRequestDTO;
import com.banffpay.pawapay.dto.DepositResponseDTO;
import com.banffpay.pawapay.service.DepositService;
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
@RequestMapping("/api/deposits")
@RequiredArgsConstructor
@Tag(name = "Deposits", description = "Mobile money deposit collection endpoints")
public class DepositController {

    private final DepositService depositService;

    @PostMapping
    @Operation(summary = "Initiate a deposit",
            description = "Creates a new mobile money deposit. Country is routed to the appropriate mobile network automatically.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Deposit initiated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(name = "ZambiaDeposit", value = """
                    {
                      "success": true,
                      "message": "Deposit initiated successfully",
                      "data": {
                        "transactionId": "7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02",
                        "merchantTransactionId": "DEP-001",
                        "customerName": "Jane Doe",
                        "pawapayId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
                        "type": "DEPOSIT",
                        "status": "ACCEPTED",
                        "amount": 100.00,
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
    public ResponseEntity<ApiResponse<DepositResponseDTO>> initiateDeposit(
            @Valid @RequestBody DepositRequestDTO request) {
        DepositResponseDTO response = depositService.initiateDeposit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Deposit initiated successfully", response));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get deposit status")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deposit status retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<DepositResponseDTO>> getDepositStatus(
            @PathVariable String transactionId) {
        DepositResponseDTO response = depositService.getDepositStatus(transactionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}