package com.banffpay.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic standardized API response wrapper for all BanffPay endpoints.
 * <p>
 * Provides consistent response structure across the entire API:
 * <ul>
 *   <li>success — boolean indicating if the operation succeeded</li>
 *   <li>message — human-readable status/error message</li>
 *   <li>data — the actual response payload (generic type T)</li>
 *   <li>correlationId — distributed tracing ID</li>
 *   <li>timestamp — when the response was generated</li>
 * </ul>
 * </p>
 *
 * @param <T> the type of the data payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Indicates if the operation was successful", example = "true")
    private boolean success;

    @Schema(description = "Human-readable message", example = "Deposit initiated successfully")
    private String message;

    @Schema(description = "Response payload")
    private T data;

    @Schema(description = "Correlation ID for distributed tracing", example = "550e8400-e29b-41d4-a716-446655440000")
    private String correlationId;

    @Schema(description = "Response timestamp", example = "2026-06-22T20:00:00")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Creates a success response with data and message.
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a success response with data and default message.
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Operation completed successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an error response with a message.
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an error response with message and correlationId.
     */
    public static <T> ApiResponse<T> error(String message, String correlationId) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}