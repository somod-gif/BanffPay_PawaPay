package com.banffpay.pawapay.service;

import com.banffpay.pawapay.client.PawapayClient;
import com.banffpay.pawapay.dto.DepositRequest;
import com.banffpay.pawapay.dto.TransactionResponse;
import com.banffpay.pawapay.model.SupportedCountry;
import com.banffpay.pawapay.model.Transaction;
import com.banffpay.pawapay.store.TransactionStore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositService {

    private final PawapayClient pawapayClient;
    private final TransactionStore store;

    public TransactionResponse initiateDeposit(DepositRequest request) {
        String country = request.getCountry();
        String currency = request.getCurrency();
        String provider = request.getProvider();
        String phoneNumber = request.getPhoneNumber();
        String amountStr = request.getAmount();
        String merchantTransactionId = request.getMerchantTransactionId();
        String customerName = request.getCustomerName();

        BigDecimal amount = new BigDecimal(amountStr);

        // Resolve country and validate
        SupportedCountry supported = SupportedCountry.findByCountryCode(country);

        // Validate currency matches resolved country
        if (!supported.getCurrency().equals(currency)) {
            throw new RuntimeException("Invalid currency '" + currency + "' for country " + country
                    + ". Expected: " + supported.getCurrency() + " (" + supported.getCountryName() + ")");
        }

        // Validate provider matches resolved country
        if (!supported.getProvider().equals(provider)) {
            throw new RuntimeException("Invalid provider '" + provider + "' for currency " + currency
                    + ". Expected: " + supported.getProvider() + " (" + supported.getCountryName() + ")");
        }

        String depositId = UUID.randomUUID().toString();
        String transactionId = UUID.randomUUID().toString();
        String customerMessage = "Deposit " + merchantTransactionId;

        // Call PawaPay API
        JsonNode pawapayResponse = pawapayClient.initiateDeposit(
                depositId, phoneNumber, provider,
                amount.toBigInteger().toString(), currency,
                merchantTransactionId, customerMessage
        );

        String pawapayStatus = pawapayResponse.has("status") ? pawapayResponse.get("status").asText() : "PROCESSING";
        String pawapayId = pawapayResponse.has("depositId") ? pawapayResponse.get("depositId").asText() : depositId;

        // Save transaction locally
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .merchantTransactionId(merchantTransactionId)
                .customerName(customerName)
                .pawapayId(pawapayId)
                .type("DEPOSIT")
                .status(pawapayStatus)
                .amount(amount)
                .currency(currency)
                .phoneNumber(phoneNumber)
                .country(country)
                .provider(provider)
                .createdAt(LocalDateTime.now())
                .build();

        store.save(transaction);
        log.info("Deposit initiated: transactionId={} pawapayId={} status={} country={}",
                transactionId, pawapayId, pawapayStatus, supported.getCountryName());

        return mapToResponse(transaction);
    }

    public TransactionResponse getDepositStatus(String transactionId) {
        Transaction transaction = store.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        // Try to get live status from PawaPay
        try {
            JsonNode response = pawapayClient.checkDepositStatus(transaction.getPawapayId());
            if (response.has("status")) {
                String newStatus = response.get("status").asText();
                transaction.setStatus(newStatus);
                store.save(transaction);
            }
        } catch (Exception e) {
            log.warn("Failed to sync deposit status from PawaPay, using local: {}", e.getMessage());
        }

        return mapToResponse(transaction);
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .transactionId(t.getTransactionId())
                .merchantTransactionId(t.getMerchantTransactionId())
                .customerName(t.getCustomerName())
                .pawapayId(t.getPawapayId())
                .type(t.getType())
                .status(t.getStatus())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .createdAt(t.getCreatedAt())
                .build();
    }
}