package com.banffpay.pawapay.service;

import com.banffpay.pawapay.model.SupportedCountry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Resolves currency information for deposit and payout operations.
 * <p>
 * Single responsibility: SupportedCountry → currency code mapping.
 * Reusable across deposit, payout, and webhook services.
 * </p>
 */
@Slf4j
@Service
public class CurrencyResolver {

    /**
     * Resolves the currency code for a given country.
     */
    public String resolveCurrencyCode(SupportedCountry country) {
        if (country == null) {
            throw new IllegalArgumentException("Country is required");
        }
        String currencyCode = country.getCurrency();
        log.debug("Resolved currency for {}: {}", country.getIso2(), currencyCode);
        return currencyCode;
    }

    /**
     * Resolves the currency code for a given country code (ISO2/ISO3).
     */
    public String resolveCurrencyCode(String countryCode) {
        SupportedCountry country = resolveCountry(countryCode);
        return resolveCurrencyCode(country);
    }

    /**
     * Validates that a given currency code is supported for a country.
     */
    public boolean isCurrencySupported(SupportedCountry country, String currencyCode) {
        if (country == null || currencyCode == null || currencyCode.isBlank()) {
            return false;
        }
        return country.getCurrency().equalsIgnoreCase(currencyCode.trim());
    }

    private SupportedCountry resolveCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            throw new IllegalArgumentException("Country code is required");
        }
        try {
            return SupportedCountry.findByCountryCode(countryCode);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid country code for currency resolution: {}", countryCode);
            throw new IllegalArgumentException("Unsupported country: '" + countryCode + "'");
        }
    }
}