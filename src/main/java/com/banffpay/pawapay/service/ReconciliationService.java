package com.banffpay.pawapay.service;

import com.banffpay.pawapay.client.PawapayClient;
import com.banffpay.pawapay.dto.PawapayDepositResponse;
import com.banffpay.pawapay.dto.PawapayPayoutResponse;
import com.banffpay.pawapay.model.Transaction;
import com.banffpay.pawapay.model.TransactionStatus;
import com.banffpay.pawapay.model.TransactionType;
import com.banffpay.pawapay.util.TransactionStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled reconciliation service that periodically checks pending transactions
 * by querying PawaPay for live status updates.
 * <p>
 * Runs every 5 minutes via {@link Scheduled}.
 * Keeps it simple: find pending transactions → query PawaPay → update status.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final TransactionStore transactionStore;
    private final PawapayClient pawapayClient;

    @Scheduled(fixedRate = 300000, initialDelay = 60000) // Every 5 minutes, initial 1 min delay
    public void reconcilePendingTransactions() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        try {
            List<Transaction> pendingTransactions = transactionStore.findAll().stream()
                    .filter(tx -> tx.getStatus() != null && tx.getStatus().isPending())
                    .toList();

            if (pendingTransactions.isEmpty()) {
                log.info("Reconciliation: No pending transactions found.");
                return;
            }

            log.info("Reconciliation: Found {} pending transaction(s) to reconcile.", pendingTransactions.size());

            for (Transaction tx : pendingTransactions) {
                long minutesSinceCreation = ChronoUnit.MINUTES.between(tx.getCreatedAt(), LocalDateTime.now());

                log.info("Reconciliation pending: transactionId={} pawapayId={} type={} status={} ageMinutes={}",
                        tx.getTransactionId(), tx.getPawapayId(), tx.getType(),
                        tx.getStatus(), minutesSinceCreation);

                // Query PawaPay for live status
                try {
                    if (tx.getType() == TransactionType.DEPOSIT) {
                        PawapayDepositResponse response = pawapayClient.checkDepositStatus(tx.getPawapayId());
                        if (response.getStatus() != null) {
                            TransactionStatus newStatus = TransactionStatus.fromValue(response.getStatus());
                            if (newStatus != null && newStatus.isTerminal()) {
                                tx.setStatus(newStatus);
                                transactionStore.save(tx);
                                log.info("Reconciliation updated: transactionId={} newStatus={}",
                                        tx.getTransactionId(), newStatus);
                            }
                        }
                    } else {
                        PawapayPayoutResponse response = pawapayClient.checkPayoutStatus(tx.getPawapayId());
                        if (response.getStatus() != null) {
                            TransactionStatus newStatus = TransactionStatus.fromValue(response.getStatus());
                            if (newStatus != null && newStatus.isTerminal()) {
                                tx.setStatus(newStatus);
                                transactionStore.save(tx);
                                log.info("Reconciliation updated: transactionId={} newStatus={}",
                                        tx.getTransactionId(), newStatus);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Reconciliation error: Failed to check status for {}: {}",
                            tx.getPawapayId(), e.getMessage());
                }

                // Escalate if pending > 60 minutes
                if (minutesSinceCreation > 60) {
                    log.error("Reconciliation escalation: transactionId={} status={} ageMinutes={} pawapayId={} amount={} {}",
                            tx.getTransactionId(), tx.getStatus(), minutesSinceCreation,
                            tx.getPawapayId(), tx.getAmount(), tx.getCurrency());
                }
            }

            log.info("Reconciliation complete: Processed {} pending transaction(s).", pendingTransactions.size());

        } catch (Exception e) {
            log.error("Reconciliation failed: {}", e.getMessage());
        } finally {
            MDC.remove("correlationId");
        }
    }
}