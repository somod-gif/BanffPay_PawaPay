package com.banffpay.pawapay.store;

import com.banffpay.pawapay.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TransactionStore {

    private final ConcurrentHashMap<String, Transaction> storeByTransactionId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> pawapayIdToTransactionId = new ConcurrentHashMap<>();

    public Transaction save(Transaction transaction) {
        storeByTransactionId.put(transaction.getTransactionId(), transaction);
        if (transaction.getPawapayId() != null) {
            pawapayIdToTransactionId.put(transaction.getPawapayId(), transaction.getTransactionId());
        }
        return transaction;
    }

    public Optional<Transaction> findById(String transactionId) {
        return Optional.ofNullable(storeByTransactionId.get(transactionId));
    }

    public Optional<Transaction> findByPawapayId(String pawapayId) {
        String transactionId = pawapayIdToTransactionId.get(pawapayId);
        if (transactionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storeByTransactionId.get(transactionId));
    }

    /**
     * Returns all transactions in the store.
     */
    public List<Transaction> findAll() {
        return new ArrayList<>(storeByTransactionId.values());
    }

    /**
     * Clears all transactions (for testing purposes only).
     */
    public void clearAll() {
        storeByTransactionId.clear();
        pawapayIdToTransactionId.clear();
    }
}
