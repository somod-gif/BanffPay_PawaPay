package com.banffpay.pawapay.service;

import com.banffpay.pawapay.model.MobileNetwork;
import com.banffpay.pawapay.model.SupportedCountry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Comprehensive validation service with country-specific rules.
 * <p>
 * Validates all inputs BEFORE calling PawaPay to avoid unnecessary API calls
 * and provide meaningful error messages to clients.
 * </p>
 */
@Slf4j
@Service
public class CountryValidationService {

    // Country-specific phone number patterns (must start with country code)
    private static final Map<String, Pattern> PHONE_PATTERNS = Map.of(
            "UG", Pattern.compile("^256[0-9]{9}$"),           // Uganda: 256XXXXXXXXX
            "KE", Pattern.compile("^254[0-9]{9}$"),           // Kenya: 254XXXXXXXXX
            "TZ", Pattern.compile("^255[0-9]{9}$"),           // Tanzania: 255XXXXXXXXX
            "RW", Pattern.compile("^250[0-9]{9}$"),           // Rwanda: 250XXXXXXXXX
            "CM", Pattern.compile("^237[0-9]{8,9}$"),         // Cameroon: 237XXXXXXXX
            "NG", Pattern.compile("^234[0-9]{10,11}$"),       // Nigeria: 234XXXXXXXXXX
            "BJ", Pattern.compile("^229[0-9]{8,9}$"),         // Benin: 229XXXXXXXX
            "ZM", Pattern.compile("^260[0-9]{9}$"),           // Zambia: 260XXXXXXXXX
            "ZA", Pattern.compile("^27[0-9]{9}$")             // South Africa: 27XXXXXXXXX
    );

    // Minimum and maximum amounts per country (in local currency)
    private static final Map<String, BigDecimal> MIN_AMOUNTS = Map.of(
            "UG", new BigDecimal("500"),    // UGX 500 min
            "KE", new BigDecimal("10"),     // KES 10 min
            "TZ", new BigDecimal("500"),    // TZS 500 min
            "RW", new BigDecimal("100"),    // RWF 100 min
            "CM", new BigDecimal("100"),    // XAF 100 min
            "NG", new BigDecimal("100"),    // NGN 100 min
            "BJ", new BigDecimal("100"),    // XOF 100 min
            "ZM", new BigDecimal("5"),      // ZMW 5 min
            "ZA", new BigDecimal("10")      // ZAR 10 min
    );

    private static final Map<String, BigDecimal> MAX_AMOUNTS = Map.of(
            "UG", new BigDecimal("10000000"),  // UGX 10M max
            "KE", new BigDecimal("150000"),    // KES 150K max
            "TZ", new BigDecimal("10000000"),  // TZS 10M max
            "RW", new BigDecimal("1000000"),   // RWF 1M max
            "CM", new BigDecimal("1000000"),   // XAF 1M max
            "NG", new BigDecimal("5000000"),   // NGN 5M max
            "BJ", new BigDecimal("1000000"),   // XOF 1M max
            "ZM", new BigDecimal("500000"),    // ZMW 500K max
            "ZA", new BigDecimal("100000")     // ZAR 100K max
    );

    /**
     * Validates country code and returns the SupportedCountry.
     */
    public SupportedCountry validateCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            throw new IllegalArgumentException("Country code is required");
        }
        try {
            return SupportedCountry.findByCountryCode(countryCode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported country: '" + countryCode + "'. " + e.getMessage());
        }
    }

    /**
     * Validates currency matches the country.
     */
    public void validateCurrency(SupportedCountry country, String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required for country " + country.getIso2());
        }
        String normalized = currency.trim().toUpperCase();
        if (!country.getCurrency().equals(normalized)) {
            throw new IllegalArgumentException(
                    "Invalid currency '" + currency + "' for country " + country.getIso2() +
                    ". Expected: " + country.getCurrency()
            );
        }
    }

    /**
     * Validates network is supported for the country.
     */
    public void validateNetwork(SupportedCountry country, String networkCode) {
        if (networkCode == null || networkCode.isBlank()) {
            throw new IllegalArgumentException("Network is required for country " + country.getIso2());
        }
        String normalized = networkCode.trim().toUpperCase();
        if (!country.isValidNetwork(normalized)) {
            throw new IllegalArgumentException(
                    "Unsupported network '" + networkCode + "' for country " + country.getIso2() +
                    ". Supported networks: " + String.join(", ", country.getNetworkCodes())
            );
        }
    }

    /**
     * Validates phone number format for the specific country.
     */
    public void validatePhoneNumber(String phoneNumber, String countryIso2) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        String normalized = phoneNumber.trim();
        Pattern pattern = PHONE_PATTERNS.get(countryIso2);

        if (pattern == null) {
            log.warn("No phone pattern defined for country: {}", countryIso2);
            return; // Allow if no pattern defined
        }

        if (!pattern.matcher(normalized).matches()) {
            String expectedFormat = getExpectedFormat(countryIso2);
            throw new IllegalArgumentException(
                    "Invalid phone number format for " + countryIso2 +
                    ". Expected: " + expectedFormat + ". Received: " + normalized
            );
        }
    }

    /**
     * Validates amount is within country limits.
     */
    public void validateAmount(BigDecimal amount, String countryIso2) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive. Received: " + amount);
        }

        BigDecimal min = MIN_AMOUNTS.get(countryIso2);
        BigDecimal max = MAX_AMOUNTS.get(countryIso2);

        if (min != null && amount.compareTo(min) < 0) {
            throw new IllegalArgumentException(
                    "Amount below minimum for " + countryIso2 +
                    ". Minimum: " + countryIso2 + " " + min + ". Received: " + amount
            );
        }

        if (max != null && amount.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                    "Amount above maximum for " + countryIso2 +
                    ". Maximum: " + countryIso2 + " " + max + ". Received: " + amount
            );
        }
    }

    private String getExpectedFormat(String countryIso2) {
        return switch (countryIso2) {
            case "UG" -> "256XXXXXXXXX (e.g., 256700000000)";
            case "KE" -> "254XXXXXXXXX (e.g., 254700000000)";
            case "TZ" -> "255XXXXXXXXX (e.g., 255700000000)";
            case "RW" -> "250XXXXXXXXX (e.g., 250700000000)";
            case "CM" -> "237XXXXXXXX (e.g., 237700000000)";
            case "NG" -> "234XXXXXXXXXX (e.g., 2348012345678)";
            case "BJ" -> "229XXXXXXXX (e.g., 22970000000)";
            case "ZM" -> "260XXXXXXXXX (e.g., 260700000000)";
            case "ZA" -> "27XXXXXXXXX (e.g., 27700000000)";
            default -> "Country code + digits";
        };
    }
}