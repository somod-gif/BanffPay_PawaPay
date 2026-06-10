package com.banffpay.pawapay.service;

import com.banffpay.pawapay.dto.WebhookRequest;
import com.banffpay.pawapay.model.Transaction;
import com.banffpay.pawapay.store.TransactionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final TransactionStore store;

    private static final Set<String> VALID_STATUSES = Set.of(
            "ACCEPTED", "PROCESSING", "COMPLETED", "FAILED", "REJECTED", "CANCELLED"
    );

    public String processWebhook(WebhookRequest request) {
        if (!VALID_STATUSES.contains(request.getStatus())) {
            throw new RuntimeException("Invalid status: " + request.getStatus()
                    + ". Allowed: ACCEPTED, PROCESSING, COMPLETED, FAILED, REJECTED, CANCELLED");
        }

        Transaction transaction = store.findByPawapayId(request.getPawapayId())
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found for pawapayId: " + request.getPawapayId()));

        String oldStatus = transaction.getStatus();
        transaction.setStatus(request.getStatus());
        store.save(transaction);

        log.info("Webhook processed: pawapayId={} type={} {} -> {}",
                request.getPawapayId(), request.getType(), oldStatus, request.getStatus());

        return "Webhook processed successfully";
    }
}