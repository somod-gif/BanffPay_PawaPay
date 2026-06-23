package com.banffpay.pawapay.service;

import com.banffpay.pawapay.client.PawapayClient;
import com.banffpay.pawapay.dto.*;
import com.banffpay.pawapay.model.Transaction;
import com.banffpay.pawapay.model.TransactionStatus;
import com.banffpay.pawapay.model.TransactionType;
import com.banffpay.pawapay.service.CountryRoutingService.CountryRoutingResult;
import com.banffpay.pawapay.util.TransactionStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payout service with automated country routing.
 * <p>
 * <b>Key improvement:</b> Users no longer submit provider/network codes.
 * The {@link CountryRoutingService} automatically resolves:
 * Country → Mobile Money Network → PawaPay
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PawapayClient pawapayClient;
    private final TransactionStore store;
    private final CountryRoutingService countryRoutingService;

    /**
     * Initiates a payout transaction with automatic country routing.
     */
    public PayoutResponseDTO initiatePayout(PayoutRequestDTO request) {
        // Parse amount
        BigDecimal amount = new BigDecimal(request.getAmount());

        // Route country to mobile network (no provider selection by client)
        CountryRoutingResult routing = countryRoutingService.route(
                request.getCountry(),
                request.getPhoneNumber()
        );

        // Generate transaction IDs
        String payoutId = UUID.randomUUID().toString();
        String transactionId = UUID.randomUUID().toString();
        String customerMessage = "Payout " + request.getMerchantTransactionId();

        log.info("Initiating payout: country={} network={} amount={} {}",
                routing.country().getIso2(), routing.network().getNetworkCode(),
                amount, routing.currency());

        // Call PawaPay API with strongly typed DTO
        PawapayPayoutResponse pawapayResponse = pawapayClient.initiatePayout(
                payoutId,
                request.getPhoneNumber(),
                routing.network().getNetworkCode(),
                amount.toBigInteger().toString(),
                routing.currency(),
                request.getMerchantTransactionId(),
                customerMessage
        );

        // Parse response status
        String status = pawapayResponse.getStatus() != null
                ? pawapayResponse.getStatus()
                : "PROCESSING";

        String pawapayId = pawapayResponse.getPayoutId() != null
                ? pawapayResponse.getPayoutId()
                : payoutId;

        // Save transaction
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .merchantTransactionId(request.getMerchantTransactionId())
                .customerName(request.getCustomerName())
                .pawapayId(pawapayId)
                .type(TransactionType.PAYOUT)
                .status(TransactionStatus.fromValue(status))
                .amount(amount)
                .currency(routing.currency())
                .phoneNumber(request.getPhoneNumber())
                .country(routing.country().getIso2())
                .provider(routing.network().getNetworkCode())
                .createdAt(LocalDateTime.now())
                .build();

        store.save(transaction);

        log.info("Payout initiated: transactionId={} pawapayId={} status={} network={}",
                transactionId, pawapayId, status, routing.network().getNetworkCode());

        return mapToPayoutResponse(transaction, routing);
    }

    /**
     * Gets the current status of a payout transaction.
     */
    public PayoutResponseDTO getPayoutStatus(String transactionId) {
        Transaction transaction = store.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        // Try to sync with PawaPay
        try {
            PawapayPayoutResponse response = pawapayClient.checkPayoutStatus(transaction.getPawapayId());
            if (response.getStatus() != null) {
                String newStatus = response.getStatus();
                if (!newStatus.equals(transaction.getStatus().toString())) {
                    log.info("Payout status updated: transactionId={} oldStatus={} newStatus={}",
                            transactionId, transaction.getStatus(), newStatus);
                    transaction.setStatus(TransactionStatus.fromValue(newStatus));
                    store.save(transaction);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to sync payout status from PawaPay, using local data: {}", e.getMessage());
        }

        return mapToPayoutResponse(transaction, null);
    }

    private PayoutResponseDTO mapToPayoutResponse(Transaction transaction,
                                                   CountryRoutingResult routing) {
        return PayoutResponseDTO.builder()
                .transactionId(transaction.getTransactionId())
                .merchantTransactionId(transaction.getMerchantTransactionId())
                .customerName(transaction.getCustomerName())
                .pawapayId(transaction.getPawapayId())
                .type(transaction.getType())
                .status(transaction.getStatus() != null ? transaction.getStatus().getValue() : null)
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .phoneNumber(transaction.getPhoneNumber())
                .country(transaction.getCountry())
                .network(transaction.getProvider())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}