package com.banffpay.pawapay.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum representing all countries supported by the BanffPay PawaPay integration.
 *
 * <p>This is the <b>single source of truth</b> for country-to-currency-to-provider mappings.
 * Adding a new country requires only adding a new enum constant here — no service or
 * controller changes are needed.</p>
 *
 * <p>Each entry defines:
 * <ul>
 *   <li>Country name (display)</li>
 *   <li>ISO 3166-1 alpha-2 code (e.g. "UG")</li>
 *   <li>ISO 3166-1 alpha-3 code (e.g. "UGA")</li>
 *   <li>Currency code (ISO 4217, e.g. "UGX") — <b>backend-controlled</b>, never from client</li>
 *   <li>List of supported mobile money providers for this country</li>
 * </ul>
 * </p>
 *
 * @author BanffPay Team
 * @version 2.0
 */
@Getter
public enum SupportedCountry {

    // ======================== EAST AFRICA ========================
    UGANDA("Uganda", "UG", "UGA", "UGX", List.of("MTN_MOMO_UGA", "AIRTEL_UGA")),
    TANZANIA("Tanzania", "TZ", "TZA", "TZS", List.of("AIRTEL_TZA", "VODACOM_TZA", "TIGO_TZA", "HALOTEL_TZA")),
    KENYA("Kenya", "KE", "KEN", "KES", List.of("MPESA_KE", "AIRTEL_KE", "TKASH_KE")),
    RWANDA("Rwanda", "RW", "RWA", "RWF", List.of("MTN_MOMO_RWA", "AIRTEL_RWA")),

    // ======================== CENTRAL AFRICA ========================
    // Cameroon uses XAF (CFA Franc BEAC)
    CAMEROON("Cameroon", "CM", "CMR", "XAF", List.of("MTN_MOMO_CMR", "ORANGE_CMR")),

    // ======================== WEST AFRICA ========================
    NIGERIA("Nigeria", "NG", "NGA", "NGN", List.of("MTN_MOMO_NG", "AIRTEL_NG", "GLO_NG", "9MOBILE_NG")),
    // Benin and others using XOF (CFA Franc BCEAO)
    BENIN("Benin", "BJ", "BEN", "XOF", List.of("MTN_MOMO_BEN", "MOOV_BEN")),

    // ======================== SOUTHERN AFRICA ========================
    ZAMBIA("Zambia", "ZM", "ZMB", "ZMW", List.of("MTN_MOMO_ZMB", "AIRTEL_ZMB")),
    SOUTH_AFRICA("South Africa", "ZA", "ZAF", "ZAR", List.of("VODACOM_ZA", "MTN_ZA", "TELKOM_ZA"));

    private final String countryName;
    private final String iso2;
    private final String iso3;
    private final String currency;
    private final List<String> providers;

    /**
     * Constructs a SupportedCountry enum constant.
     *
     * @param countryName human-readable country name
     * @param iso2        ISO 3166-1 alpha-2 code (2 letters)
     * @param iso3        ISO 3166-1 alpha-3 code (3 letters)
     * @param currency    ISO 4217 currency code (3 letters)
     * @param providers   list of valid mobile money provider codes for this country
     */
    SupportedCountry(String countryName, String iso2, String iso3, String currency, List<String> providers) {
        this.countryName = countryName;
        this.iso2 = iso2;
        this.iso3 = iso3;
        this.currency = currency;
        this.providers = Collections.unmodifiableList(providers);
    }

    /**
     * Finds a SupportedCountry by its country code (ISO2 or ISO3, case-insensitive).
     * <p>
     * This is the <b>only</b> method that should be used to resolve a country from user input.
     * Never hardcode country resolution logic.
     * </p>
     *
     * @param code the country code (e.g. "ZM", "ZMB", "ug", "uga")
     * @return the matching SupportedCountry
     * @throws IllegalArgumentException if the code is null, blank, or no country matches
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
     * Checks whether the given provider code is valid for this country.
     *
     * @param provider the provider code to validate (e.g. "MTN_MOMO_UGA")
     * @return true if the provider is in this country's supported provider list
     */
    public boolean isValidProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return false;
        }
        return providers.contains(provider.trim().toUpperCase());
    }

    /**
     * Builds a descriptive error message listing all supported country codes, currencies, and providers.
     *
     * @param invalidCode the invalid code that was provided
     * @return formatted error message
     */
    private static String buildErrorMessage(String invalidCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("Unsupported country code: '").append(invalidCode).append("'.\n\n");
        sb.append("Supported countries (ISO2 / ISO3 -> Name [Currency]):\n");
        for (SupportedCountry country : values()) {
            sb.append("  ")
              .append(String.format("%-2s / %-3s -> %-15s [%s]", country.iso2, country.iso3, country.countryName, country.currency))
              .append("\n");
        }
        sb.append("\nValid providers per country:\n");
        for (SupportedCountry country : values()) {
            sb.append("  ").append(country.iso2).append(": ")
              .append(String.join(", ", country.providers))
              .append("\n");
        }
        return sb.toString();
    }

    /**
     * Returns all supported ISO2 and ISO3 codes as a flat list.
     *
     * @return list of all ISO2 and ISO3 codes
     */
    public static List<String> getAllCountryCodes() {
        return Arrays.stream(values())
                .flatMap(c -> Stream.of(c.iso2, c.iso3))
                .collect(Collectors.toList());
    }

    /**
     * Returns the ISO2 code for a given ISO3 code (or vice versa).
     *
     * @param code an ISO2 or ISO3 country code
     * @return the matching country's ISO2 code (normalized)
     */
    public static String resolveToIso2(String code) {
        return findByCountryCode(code).getIso2();
    }

    /**
     * Returns the ISO3 code for a given ISO2 code (or vice versa).
     *
     * @param code an ISO2 or ISO3 country code
     * @return the matching country's ISO3 code (normalized)
     */
    public static String resolveToIso3(String code) {
        return findByCountryCode(code).getIso3();
    }
}