package com.banffpay.pawapay.util;

import com.banffpay.pawapay.model.WebhookEvent;
import com.banffpay.pawapay.model.WebhookProcessingStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for webhook events.
 * Used for audit logging and idempotency checks in development/demo mode.
 * In production, this would be replaced with a database (JPA repository).
 */
@Component
public class WebhookEventStore {

    private final ConcurrentHashMap<String, WebhookEvent> eventsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> correlationIdIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<WebhookEvent>> eventsByPawapayId = new ConcurrentHashMap<>();

    /**
     * Saves a webhook event.
     *
     * @param event the webhook event to save
     * @return the saved event
     * @throws IllegalStateException if a duplicate correlationId is detected
     */
    public WebhookEvent save(WebhookEvent event) {
        // Idempotency check: ensure correlationId is unique
        String existingEventId = correlationIdIndex.putIfAbsent(event.getCorrelationId(), event.getId());
        if (existingEventId != null) {
            WebhookEvent existing = eventsById.get(existingEventId);
            throw new IllegalStateException(
                    "Duplicate webhook event detected: correlationId=" + event.getCorrelationId() +
                            ". This webhook has already been processed."
            );
        }

        eventsById.put(event.getId(), event);

        // Index by pawapayId for quick lookup
        eventsByPawapayId.computeIfAbsent(event.getPawapayId(), k -> new ArrayList<>()).add(event);

        return event;
    }

    /**
     * Finds a webhook event by its ID.
     */
    public Optional<WebhookEvent> findById(String id) {
        return Optional.ofNullable(eventsById.get(id));
    }

    /**
     * Finds a webhook event by correlation ID.
     * Used for idempotency checks.
     */
    public Optional<WebhookEvent> findByCorrelationId(String correlationId) {
        String eventId = correlationIdIndex.get(correlationId);
        if (eventId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(eventsById.get(eventId));
    }

    /**
     * Checks if a correlation ID has already been processed.
     */
    public boolean existsByCorrelationId(String correlationId) {
        return correlationIdIndex.containsKey(correlationId);
    }

    /**
     * Finds all webhook events for a given PawaPay transaction ID.
     */
    public List<WebhookEvent> findByPawapayId(String pawapayId) {
        return eventsByPawapayId.getOrDefault(pawapayId, new ArrayList<>());
    }

    /**
     * Updates the processing status of a webhook event.
     */
    public WebhookEvent updateStatus(String eventId, WebhookProcessingStatus status, String errorMessage) {
        WebhookEvent event = eventsById.get(eventId);
        if (event != null) {
            event.setProcessingStatus(status);
            event.setErrorMessage(errorMessage);
            event.setProcessedAt(java.time.LocalDateTime.now());
        }
        return event;
    }

    /**
     * Increments the retry count for a webhook event.
     */
    public WebhookEvent incrementRetryCount(String eventId) {
        WebhookEvent event = eventsById.get(eventId);
        if (event != null) {
            event.setRetryCount(event.getRetryCount() + 1);
        }
        return event;
    }

    /**
     * Returns all webhook events (for debugging/admin purposes).
     */
    public List<WebhookEvent> findAll() {
        return new ArrayList<>(eventsById.values());
    }

    /**
     * Clears all stored events (for testing purposes only).
     */
    public void clearAll() {
        eventsById.clear();
        correlationIdIndex.clear();
        eventsByPawapayId.clear();
    }
}