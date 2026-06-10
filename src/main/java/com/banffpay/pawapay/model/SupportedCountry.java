package com.banffpay.pawapay.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum SupportedCountry {

    ZAMBIA("Zambia", "ZM", "ZMB", "ZMW", "MTN_MOMO_ZMB"),
    UGANDA("Uganda", "UG", "UGA", "UGX", "MTN_MOMO_UGA"),
    RWANDA("Rwanda", "RW", "RWA", "RWF", "MTN_MOMO_RWA"),
    CAMEROON("Cameroon", "CM", "CMR", "XAF", "MTN_MOMO_CMR"),
    TANZANIA("Tanzania", "TZ", "TZA", "TZS", "AIRTEL_TZA"),
    BENIN("Benin", "BJ", "BEN", "XOF", "MTN_MOMO_BEN");

    private final String countryName;
    private final String iso2;
    private final String iso3;
    private final String currency;
    private final String provider;

    SupportedCountry(String countryName, String iso2, String iso3, String currency, String provider) {
        this.countryName = countryName;
        this.iso2 = iso2;
        this.iso3 = iso3;
        this.currency = currency;
        this.provider = provider;
    }

    /**
     * Find a SupportedCountry by its country code (ISO2 or ISO3, case-insensitive).
     *
     * @param code the country code (e.g. "ZM", "ZMB", "ug", "uga")
     * @return the matching SupportedCountry
     * @throws IllegalArgumentException if no country matches
     */
    public static SupportedCountry findByCountryCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Country code must not be null or blank");
        }
        String normalized = code.trim().toUpperCase();
        for (SupportedCountry country : values()) {
            if (country.iso2.equals(normalized) || country.iso3.equals(normalized)) {
                return country;
            }
        }
        throw new IllegalArgumentException(buildErrorMessage(normalized));
    }

    /**
     * Build a descriptive error message listing all supported country codes.
     */
    private static String buildErrorMessage(String invalidCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("Unsupported country: ").append(invalidCode).append(".\n\n");
        sb.append("Supported country codes:\n");
        for (SupportedCountry country : values()) {
            sb.append("  ").append(country.iso2).append(", ").append(country.iso3)
              .append("  ->  ").append(country.countryName)
              .append("  (").append(country.currency).append(" / ").append(country.provider).append(")\n");
        }
        return sb.toString();
    }

    /**
     * Returns all supported ISO2 and ISO3 codes as a flat list.
     */
    public static List<String> getAllCountryCodes() {
        return Arrays.stream(values())
                .flatMap(c -> java.util.stream.Stream.of(c.iso2, c.iso3))
                .collect(Collectors.toList());
    }
}