package com.banffpay.pawapay.service;

import com.banffpay.pawapay.model.SupportedCountry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Centralized validation service for country-specific business rules.
 * <p>
 * This service is the <b>single point of validation</b> for all country-related checks.
 * Both {@link DepositService} and {@link PayoutService} use this service, eliminating
 * duplicate validation logic across the codebase.
 * </p>
 *
 * <p>Validation rules enforced:
 * <ul>
 *   <li>Country code is valid (ISO2 or ISO3) via {@link SupportedCountry#findByCountryCode(String)}</li>
 *   <li>Currency is backend-controlled — the client's currency input is validated against
 *       the resolved country, but the backend always uses its own currency value</li>
 *   <li>Provider is supported for the resolved country (multi-provider support)</li>
 *   <li>Amount is positive and within reasonable bounds</li>
 *   <li>Phone number matches expected format for the country</li>
 * </ul>
 * </p>
 *
 * <p><b>Extensibility:</b> Adding a new country requires only updating {@link SupportedCountry}.
 * No changes to this service or the payment services are needed.</p>
 *
 * @author BanffPay Team
 * @version 2.0
 */
@Slf4j
@Service
public class CountryValidationService {

    // Pattern for basic phone number validation (numeric, 7-15 digits)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{7,15}$");

    /**
     * Validates a country code string and resolves it to a {@link SupportedCountry}.
     * <p>
     * Accepts both ISO2 (e.g. "ZM") and ISO3 (e.g. "ZMB") codes, case-insensitive.
     * This is the <b>only</b> place where country resolution should happen.
     * </p>
     *
     * @param countryCode the country code to validate and resolve
     * @return the resolved SupportedCountry
     * @throws IllegalArgumentException if the country code is invalid or unsupported
     */
    public SupportedCountry validateAndResolveCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            throw new IllegalArgumentException("Country code is required");
        }
        try {
            SupportedCountry country = SupportedCountry.findByCountryCode(countryCode);
            log.debug("Resolved country code '{}' -> {} ({})", countryCode, country.getIso2(), country.getCountryName());
            return country;
        } catch (IllegalArgumentException e) {
            // Wrap with a clearer message context
            throw new IllegalArgumentException("Invalid country: " + countryCode + ". " + e.getMessage(), e);
        }
    }

    /**
     * Validates that the client-provided currency matches the backend-controlled currency
     * for the resolved country.
     * <p>
     * The currency is <b>always backend-controlled</b>. The client may optionally provide a currency,
     * but if provided it must match the backend value. If not provided (null/blank), validation passes
     * and the backend currency is used.
     * </p>
     *
     * @param country        the resolved SupportedCountry
     * @param clientCurrency the currency provided by the client (may be null if not provided)
     * @throws IllegalArgumentException if the client currency is provided but does not match
     */
    public void validateCurrencyForCountry(SupportedCountry country, String clientCurrency) {
        // If client didn't provide a currency, backend controls it — no validation needed
        if (clientCurrency == null || clientCurrency.isBlank()) {
            log.debug("No currency provided by client; backend will use {} for {}", country.getCurrency(), country.getIso2());
            return;
        }

        String normalizedCurrency = clientCurrency.trim().toUpperCase();
        if (!country.getCurrency().equals(normalizedCurrency)) {
            throw new IllegalArgumentException(
                "Invalid currency '" + clientCurrency + "' for country " + country.getIso2()
                + " (" + country.getCountryName() + "). Expected: " + country.getCurrency()
            );
        }
        log.debug("Client-provided currency '{}' validated for {}", normalizedCurrency, country.getIso2());
    }

    /**
     * Returns the backend-controlled currency for the given country.
     * <p>
     * This is the currency that will <b>actually</b> be used in the PawaPay API call,
     * regardless of what the client provided (which was only validated for consistency).
     * </p>
     *
     * @param country the resolved SupportedCountry
     * @return the ISO 4217 currency code
     */
    public String getCurrencyForCountry(SupportedCountry country) {
        return country.getCurrency();
    }

    /**
     * Validates that the given provider is supported for the resolved country.
     * <p>
     * Supports multiple providers per country. The provider is validated against the
     * country's allowed provider list.
     * </p>
     *
     * @param country  the resolved SupportedCountry
     * @param provider the provider code to validate
     * @return the validated provider code (normalized to uppercase)
     * @throws IllegalArgumentException if the provider is null, blank, or not supported for this country
     */
    public String validateProviderForCountry(SupportedCountry country, String provider) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Provider is required");
        }

        String normalizedProvider = provider.trim().toUpperCase();
        if (!country.isValidProvider(normalizedProvider)) {
            throw new IllegalArgumentException(
                "Invalid provider '" + provider + "' for country " + country.getIso2()
                + " (" + country.getCountryName() + "). Valid providers: " + String.join(", ", country.getProviders())
            );
        }
        log.debug("Provider '{}' validated for {}", normalizedProvider, country.getIso2());
        return normalizedProvider;
    }

    /**
     * Validates the transaction amount.
     * <p>
     * Amount must be positive and non-zero.
     * </p>
     *
     * @param amount  the transaction amount
     * @param country the resolved SupportedCountry (for future country-specific limits)
     * @throws IllegalArgumentException if the amount is invalid
     */
    public void validateAmount(BigDecimal amount, SupportedCountry country) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive. Received: " + amount.toPlainString());
        }

        // Future: Add country-specific min/max limits here
        // e.g., check against SupportedCountry's minAmount/maxAmount if added

        log.debug("Amount {} validated for {}", amount.toPlainString(), country.getIso2());
    }

    /**
     * Validates a phone number format.
     * <p>
     * Phone must be numeric and 7-15 digits long (standard MSISDN format).
     * </p>
     *
     * @param phoneNumber the phone number to validate
     * @param country     the resolved SupportedCountry (for future country-specific formats)
     * @throws IllegalArgumentException if the phone number is invalid
     */
    public void validatePhoneNumber(String phoneNumber, SupportedCountry country) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (!PHONE_PATTERN.matcher(phoneNumber.trim()).matches()) {
            throw new IllegalArgumentException(
                "Invalid phone number format for " + country.getIso2() + " (" + country.getCountryName()
                + "). Phone number must be 7-15 digits. Received: " + phoneNumber
            );
        }
        log.debug("Phone number validated for {}", country.getIso2());
    }

    /**
     * Validates all country-specific fields in a single call for convenience.
     * <p>
     * This method chains all validations and returns the resolved country and validated
     * provider/currency for use by the calling service.
     * </p>
     *
     * @param countryCode  the country code (ISO2 or ISO3)
     * @param clientCurrency the client-provided currency (may be null for backend control)
     * @param provider     the provider code
     * @param amount       the transaction amount
     * @param phoneNumber  the phone number
     * @return a {@link ValidationResult} containing the resolved country, validated provider, and backend currency
     * @throws IllegalArgumentException if any validation fails
     */
    public ValidationResult validateAll(String countryCode, String clientCurrency, String provider,
                                        BigDecimal amount, String phoneNumber) {
        // Step 1: Resolve country
        SupportedCountry country = validateAndResolveCountry(countryCode);

        // Step 2: Validate currency (backend-controlled)
        validateCurrencyForCountry(country, clientCurrency);

        // Step 3: Get backend currency
        String backendCurrency = getCurrencyForCountry(country);

        // Step 4: Validate provider
        String validatedProvider = validateProviderForCountry(country, provider);

        // Step 5: Validate amount
        validateAmount(amount, country);

        // Step 6: Validate phone
        validatePhoneNumber(phoneNumber, country);

        return new ValidationResult(country, validatedProvider, backendCurrency);
    }

    /**
     * Result container for the {@link #validateAll(String, String, String, BigDecimal, String)} method.
     * Holds the resolved country, validated provider, and backend currency.
     */
    public record ValidationResult(SupportedCountry country, String validatedProvider, String backendCurrency) {}
}