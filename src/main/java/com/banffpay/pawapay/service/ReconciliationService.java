package com.banffpay.pawapay.service;

import com.banffpay.pawapay.model.Transaction;
import com.banffpay.pawapay.model.TransactionStatus;
import com.banffpay.pawapay.store.TransactionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Scheduled reconciliation service that periodically checks pending transactions
 * by syncing their status with PawaPay.
 * <p>
 * Runs every 5 minutes via {@link Scheduled}.
 * In production, this would query PawaPay's API to get live status updates.
 * For demonstration purposes, it logs the pending transactions that need attention.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final TransactionStore transactionStore;

    /**
     * Scheduled job that runs every 5 minutes to reconcile pending transactions.
     * <p>
     * This job identifies transactions that have been in a pending state
     * (ACCEPTED or PROCESSING) for more than 15 minutes and logs them
     * for manual intervention or automated status check with PawaPay.
     * </p>
     * In production, this method would:
     * <ol>
     *   <li>Query all pending transactions</li>
     *   <li>For each, call PawaPay's GET /deposits/{id} or GET /payouts/{id}</li>
     *   <li>Update status if PawaPay reports a terminal state</li>
     *   <li>Alert operations team if transaction has been pending > 1 hour</li>
     * </ol>
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000) // Every 5 minutes, initial 1 min delay
    public void reconcilePendingTransactions() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        try {
            log.info("[RECONCILIATION_START] Checking pending transactions at {}",
                    LocalDateTime.now());

            List<Transaction> pendingTransactions = transactionStore.findAll().stream()
                    .filter(tx -> tx.getStatus() != null && tx.getStatus().isPending())
                    .collect(Collectors.toList());

            if (pendingTransactions.isEmpty()) {
                log.info("[RECONCILIATION] No pending transactions found.");
                return;
            }

            log.info("[RECONCILIATION] Found {} pending transaction(s) to reconcile.",
                    pendingTransactions.size());

            LocalDateTime now = LocalDateTime.now();

            for (Transaction tx : pendingTransactions) {
                long minutesSinceCreation = ChronoUnit.MINUTES.between(tx.getCreatedAt(), now);

                // Log each pending transaction with age
                log.warn("[RECONCILIATION_PENDING] transactionId={} pawapayId={} type={} status={} " +
                                "createdAt={} ageMinutes={}",
                        tx.getTransactionId(), tx.getPawapayId(), tx.getType(),
                        tx.getStatus(), tx.getCreatedAt(), minutesSinceCreation);

                // If transaction has been pending for more than 60 minutes, escalate
                if (minutesSinceCreation > 60) {
                    log.error("[RECONCILIATION_ESCALATION] Transaction {} has been {} for {} minutes! " +
                                    "Requires immediate manual review. pawapayId={} amount={} {}",
                            tx.getTransactionId(), tx.getStatus(), minutesSinceCreation,
                            tx.getPawapayId(), tx.getAmount(), tx.getCurrency());
                }

                // In production, call PawaPay API to check live status:
                // try {
                //     JsonNode response = tx.getType() == TransactionType.DEPOSIT
                //             ? pawapayClient.checkDepositStatus(tx.getPawapayId())
                //             : pawapayClient.checkPayoutStatus(tx.getPawapayId());
                //     if (response.has("status")) {
                //         TransactionStatus newStatus = TransactionStatus.fromValue(response.get("status").asText());
                //         if (newStatus != null && newStatus.isTerminal()) {
                //             tx.setStatus(newStatus);
                //             transactionStore.save(tx);
                //             log.info("[RECONCILIATION_UPDATED] transactionId={} newStatus={}",
                //                     tx.getTransactionId(), newStatus);
                //         }
                //     }
                // } catch (Exception e) {
                //     log.warn("[RECONCILIATION_ERROR] Failed to check status for {}: {}",
                //             tx.getPawapayId(), e.getMessage());
                // }
            }

            log.info("[RECONCILIATION_COMPLETE] Processed {} pending transaction(s).", pendingTransactions.size());

        } catch (Exception e) {
            log.error("[RECONCILIATION_FAILED] Error during reconciliation: {}", e.getMessage(), e);
        } finally {
            MDC.remove("correlationId");
        }
    }
}