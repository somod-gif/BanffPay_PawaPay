package com.banffpay.pawapay.service;

import com.banffpay.pawapay.client.PawapayClient;
import com.banffpay.pawapay.config.PawaPaySandboxConfig;
import com.banffpay.pawapay.dto.*;
import com.banffpay.pawapay.model.MobileNetwork;
import com.banffpay.pawapay.model.SupportedCountry;
import com.banffpay.pawapay.model.Transaction;
import com.banffpay.pawapay.model.TransactionStatus;
import com.banffpay.pawapay.model.TransactionType;
import com.banffpay.pawapay.util.TransactionStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Deposit service with comprehensive validation and automatic country routing.
 * <p>
 * <b>Validation flow:</b>
 * 1. Validate country code
 * 2. Validate currency matches country
 * 3. Validate network is supported for country
 * 4. Validate phone number format (country-specific)
 * 5. Validate amount is within limits
 * 6. Call PawaPay API
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepositService {

    private final PawapayClient pawapayClient;
    private final TransactionStore transactionStore;
    private final CountryValidationService validationService;
    private final PawaPaySandboxConfig sandboxConfig;

    /**
     * Initiates a deposit transaction with comprehensive validation.
     */
    public DepositResponseDTO initiateDeposit(DepositRequestDTO request) {
        String correlationId = UUID.randomUUID().toString();

        try {
            // Step 1: Validate country
            log.info("[{}] Validating deposit request for country: {}", correlationId, request.getCountry());
            SupportedCountry country = validationService.validateCountry(request.getCountry());

            // Step 1b: Check sandbox support
            if (!sandboxConfig.isCountryEnabled(country.getIso2())) {
                log.warn("[{}] Country not enabled on sandbox: {}", correlationId, country.getIso2());
                throw new IllegalArgumentException(
                        sandboxConfig.getUnsupportedCountryMessage(country.getIso2())
                );
            }

            // Step 2: Parse and validate amount
            BigDecimal amount = new BigDecimal(request.getAmount());
            validationService.validateAmount(amount, country.getIso2());
            log.info("[{}] Amount validated: {} {}", correlationId, amount, country.getCurrency());

            // Step 3: Validate phone number format
            validationService.validatePhoneNumber(request.getPhoneNumber(), country.getIso2());
            log.info("[{}] Phone number validated: {}", correlationId, maskPhone(request.getPhoneNumber()));

            // Step 4: Get default network for country (automatic routing)
            MobileNetwork network = country.getDefaultNetwork();

            // Step 4b: Check if network is blocked on sandbox
            if (sandboxConfig.isNetworkBlocked(network.getNetworkCode())) {
                log.warn("[{}] Network blocked on sandbox: {}", correlationId, network.getNetworkCode());
                throw new IllegalArgumentException(
                        sandboxConfig.getUnsupportedNetworkMessage(network.getNetworkCode(), country.getIso2())
                );
            }

            log.info("[{}] Routing to default network: {} for country {}", correlationId,
                    network.getNetworkCode(), country.getIso2());

            // Step 5: Generate transaction IDs
            String depositId = UUID.randomUUID().toString();
            String transactionId = UUID.randomUUID().toString();
            String customerMessage = "Deposit " + request.getMerchantTransactionId();

            log.info("[{}] Initiating deposit: country={} network={} amount={} {} phone={}",
                    correlationId, country.getIso2(), network.getNetworkCode(),
                    amount, country.getCurrency(), maskPhone(request.getPhoneNumber()));

            // Step 6: Call PawaPay API
            PawapayDepositResponse pawapayResponse;
            try {
                log.info("[{}] Calling PawaPay API: country={} network={} amount={} {}",
                        correlationId, country.getIso2(), network.getNetworkCode(), amount, country.getCurrency());
                pawapayResponse = pawapayClient.initiateDeposit(
                        depositId,
                        request.getPhoneNumber(),
                        network.getNetworkCode(),
                        amount.toBigInteger().toString(),
                        country.getCurrency(),
                        request.getMerchantTransactionId(),
                        customerMessage
                );
                log.info("[{}] PawaPay API responded with status: {}", correlationId, pawapayResponse.getStatus());
            } catch (IllegalStateException e) {
                // Re-throw business errors (provider rejection, etc.)
                log.error("[{}] Provider error: {}", correlationId, e.getMessage());
                throw e;
            } catch (Exception e) {
                // API connection failure
                log.error("[{}] PawaPay API call failed: {}", correlationId, e.getMessage(), e);
                throw new IllegalStateException("Failed to connect to payment provider: " + e.getMessage());
            }

            // Step 7: Parse response
            String status = pawapayResponse.getStatus() != null
                    ? pawapayResponse.getStatus()
                    : "PROCESSING";

            String pawapayId = pawapayResponse.getDepositId() != null
                    ? pawapayResponse.getDepositId()
                    : depositId;

            // Step 8: Check for PawaPay rejection
            if ("REJECTED".equalsIgnoreCase(status)) {
                String failureReason = pawapayResponse.getFailureReason() != null
                        ? pawapayResponse.getFailureReason().getFailureMessage()
                        : "Unknown reason";
                log.warn("[{}] Deposit rejected by PawaPay: country={} network={} reason={}",
                        correlationId, country.getIso2(), network.getNetworkCode(), failureReason);
                throw new IllegalStateException(
                        "Deposit rejected by payment provider: " + failureReason
                );
            }

            // Step 9: Save transaction
            Transaction transaction = Transaction.builder()
                    .id(UUID.randomUUID().toString())
                    .transactionId(transactionId)
                    .merchantTransactionId(request.getMerchantTransactionId())
                    .customerName(request.getCustomerName())
                    .pawapayId(pawapayId)
                    .type(TransactionType.DEPOSIT)
                    .status(TransactionStatus.fromValue(status))
                    .amount(amount)
                    .currency(country.getCurrency())
                    .phoneNumber(request.getPhoneNumber())
                    .country(country.getIso2())
                    .provider(network.getNetworkCode())
                    .createdAt(LocalDateTime.now())
                    .build();

            transactionStore.save(transaction);

            log.info("[{}] Deposit initiated successfully: transactionId={} pawapayId={} status={} network={}",
                    correlationId, transactionId, pawapayId, status, network.getNetworkCode());

            return mapToDepositResponse(transaction, country, network);

        } catch (IllegalArgumentException e) {
            log.warn("[{}] Validation failed: {}", correlationId, e.getMessage());
            throw e; // Re-throw validation errors as-is (400 Bad Request)
        } catch (IllegalStateException e) {
            log.error("[{}] Business error: {}", correlationId, e.getMessage());
            throw e; // Re-throw business errors (409 Conflict or 500)
        } catch (Exception e) {
            log.error("[{}] Unexpected error: {}", correlationId, e.getMessage(), e);
            throw new RuntimeException("Internal server error: " + e.getMessage());
        }
    }

    /**
     * Gets the current status of a deposit transaction.
     */
    public DepositResponseDTO getDepositStatus(String transactionId) {
        Transaction transaction = transactionStore.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        // Try to sync with PawaPay
        try {
            PawapayDepositResponse response = pawapayClient.checkDepositStatus(transaction.getPawapayId());
            if (response.getStatus() != null) {
                String newStatus = response.getStatus();
                if (!newStatus.equals(transaction.getStatus().getValue())) {
                    log.info("Deposit status updated: transactionId={} oldStatus={} newStatus={}",
                            transactionId, transaction.getStatus(), newStatus);
                    transaction.setStatus(TransactionStatus.fromValue(newStatus));
                    transactionStore.save(transaction);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to sync deposit status from PawaPay, using local data: {}", e.getMessage());
        }

        return mapToDepositResponse(transaction,
                SupportedCountry.findByCountryCode(transaction.getCountry()),
                MobileNetwork.findByNetworkCode(transaction.getProvider()));
    }

    private DepositResponseDTO mapToDepositResponse(Transaction transaction,
                                                     SupportedCountry country,
                                                     MobileNetwork network) {
        return DepositResponseDTO.builder()
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

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return phone;
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}