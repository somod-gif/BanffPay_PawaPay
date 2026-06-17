package com.banffpay.pawapay.service;

import com.banffpay.pawapay.client.PawapayClient;
import com.banffpay.pawapay.dto.PayoutRequest;
import com.banffpay.pawapay.dto.TransactionResponse;
import com.banffpay.pawapay.model.SupportedCountry;
import com.banffpay.pawapay.model.Transaction;
import com.banffpay.pawapay.model.TransactionStatus;
import com.banffpay.pawapay.model.TransactionType;
import com.banffpay.pawapay.store.TransactionStore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Enhanced Payout Service with multi-country support.
 *
 * <p>Refactored to use {@link CountryValidationService} for all validation logic,
 * eliminating the duplicate validation that previously existed inline.
 * This ensures consistency between deposits and payouts.</p>
 *
 * <p>Key design decisions:
 * <ul>
 *   <li><b>Centralized validation:</b> Delegates all country/currency/provider validation to
 *       {@link CountryValidationService}. No inline validation logic.</li>
 *   <li><b>Backend-controlled currency:</b> The backend always uses the currency derived from
 *       {@link SupportedCountry}. The client's currency input is validated for consistency.</li>
 *   <li><b>Multi-provider support:</b> Providers are validated against the country's allowed
 *       provider list (defined in {@link SupportedCountry}).</li>
 *   <li><b>Extensibility:</b> Adding a new country requires only updating {@link SupportedCountry}.
 *       No changes to this service needed.</li>
 *   <li><b>Backward compatibility:</b> All existing API contracts are preserved.</li>
 * </ul>
 * </p>
 *
 * @author BanffPay Team
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PawapayClient pawapayClient;
    private final TransactionStore store;
    private final CountryValidationService countryValidationService;

    /**
     * Initiates a payout transaction with multi-country support.
     *
     * <p>Processing flow:
     * <ol>
     *   <li>Parse and validate all request parameters via {@link CountryValidationService}</li>
     *   <li>Call PawaPay API with normalized data (backend-controlled currency, validated provider)</li>
     *   <li>Save transaction with normalized country (ISO2), provider, and backend currency</li>
     * </ol>
     * </p>
     *
     * @param request the payout request from the client
     * @return TransactionResponse with normalized transaction details
     * @throws IllegalArgumentException if any validation fails
     */
    public TransactionResponse initiatePayout(PayoutRequest request) {
        // Extract request parameters
        String countryCode = request.getCountry();
        String clientCurrency = request.getCurrency();
        String provider = request.getProvider();
        String phoneNumber = request.getPhoneNumber();
        String amountStr = request.getAmount();
        String merchantTransactionId = request.getMerchantTransactionId();
        String customerName = request.getCustomerName();

        // Parse amount
        BigDecimal amount = new BigDecimal(amountStr);

        // Step 1-6: Centralized validation via CountryValidationService (no duplicate inline logic)
        CountryValidationService.ValidationResult result = countryValidationService.validateAll(
                countryCode, clientCurrency, provider, amount, phoneNumber
        );

        SupportedCountry supported = result.country();
        String validatedProvider = result.validatedProvider();
        String backendCurrency = result.backendCurrency();

        // Generate transaction IDs
        String payoutId = UUID.randomUUID().toString();
        String transactionId = UUID.randomUUID().toString();
        String customerMessage = "Payout " + merchantTransactionId;

        log.info("Initiating payout for country: {} ({}) with provider: {}, amount: {} {}",
                supported.getCountryName(), supported.getIso2(), validatedProvider, amount, backendCurrency);

        // Call PawaPay API with normalized data
        JsonNode pawapayResponse = pawapayClient.initiatePayout(
                payoutId,
                phoneNumber,
                validatedProvider,
                amount.toBigInteger().toString(),
                backendCurrency,  // Backend-controlled currency (not client's)
                merchantTransactionId,
                customerMessage
        );

        // Parse PawaPay response
        String pawapayStatus = pawapayResponse.has("status")
                ? pawapayResponse.get("status").asText()
                : "PROCESSING";
        String pawapayId = pawapayResponse.has("payoutId")
                ? pawapayResponse.get("payoutId").asText()
                : payoutId;

        TransactionStatus status = TransactionStatus.fromValue(pawapayStatus);

        // Save transaction with normalized data
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .merchantTransactionId(merchantTransactionId)
                .customerName(customerName)
                .pawapayId(pawapayId)
                .type(TransactionType.PAYOUT)
                .status(status)
                .amount(amount)
                .currency(backendCurrency)                // Backend-controlled
                .phoneNumber(phoneNumber)
                .country(supported.getIso2())              // Normalized to ISO2
                .provider(validatedProvider)
                .createdAt(LocalDateTime.now())
                .build();

        store.save(transaction);

        log.info("Payout initiated successfully: transactionId={} pawapayId={} status={} country={} currency={} provider={}",
                transactionId, pawapayId, status, supported.getIso2(), backendCurrency, validatedProvider);

        return mapToResponse(transaction);
    }

    /**
     * Gets the current status of a payout transaction.
     *
     * <p>Attempts to sync with PawaPay live status. If the PawaPay call fails,
     * returns the last known local status (graceful degradation).</p>
     *
     * @param transactionId the internal transaction ID
     * @return TransactionResponse with updated status
     * @throws RuntimeException if the transaction is not found
     */
    public TransactionResponse getPayoutStatus(String transactionId) {
        Transaction transaction = store.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        if (transaction.getType() != TransactionType.PAYOUT) {
            log.warn("Transaction {} is not a payout transaction (type: {})", transactionId, transaction.getType());
        }

        // Try to get live status from PawaPay (graceful degradation on failure)
        try {
            JsonNode response = pawapayClient.checkPayoutStatus(transaction.getPawapayId());
            if (response.has("status")) {
                String newStatusStr = response.get("status").asText();
                TransactionStatus newStatus = TransactionStatus.fromValue(newStatusStr);
                if (!newStatus.equals(transaction.getStatus())) {
                    log.info("Payout status updated: transactionId={} oldStatus={} newStatus={}",
                            transactionId, transaction.getStatus(), newStatus);
                    transaction.setStatus(newStatus);
                    store.save(transaction);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to sync payout status from PawaPay, using local data: {}", e.getMessage());
        }

        return mapToResponse(transaction);
    }

    /**
     * Maps a Transaction entity to a TransactionResponse DTO.
     * Maintains backward compatibility with existing API response contracts.
     *
     * @param transaction the transaction entity
     * @return the response DTO
     */
    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .merchantTransactionId(transaction.getMerchantTransactionId())
                .customerName(transaction.getCustomerName())
                .pawapayId(transaction.getPawapayId())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .phoneNumber(transaction.getPhoneNumber())
                .country(transaction.getCountry())
                .provider(transaction.getProvider())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}