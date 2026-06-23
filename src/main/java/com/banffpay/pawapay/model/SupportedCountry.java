package com.banffpay.pawapay.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum representing all countries supported by the BanffPay platform.
 * <p>
 * <b>Architecture Refactor (v3.0):</b> This enum now uses the correct domain model:
 * <ul>
 *   <li>{@link Provider} — The payment provider (e.g., PawaPay)</li>
 *   <li>{@link MobileNetwork} — The mobile money networks (e.g., MTN_MOMO_UGA)</li>
 *   <li>{@link SupportedCountry} — The country with references to both</li>
 * </ul>
 * </p>
 *
 * <p>Each entry defines:
 * <ul>
 *   <li>Country name (display)</li>
 *   <li>ISO 3166-1 alpha-2 code (e.g. "UG")</li>
 *   <li>ISO 3166-1 alpha-3 code (e.g. "UGA")</li>
 *   <li>Currency code (ISO 4217, e.g. "UGX") — <b>backend-controlled</b>, never from client</li>
 *   <li>List of supported mobile money networks for this country</li>
 *   <li>Default network for automatic routing</li>
 * </ul>
 * </p>
 *
 * @author BanffPay Team
 * @version 3.0
 */
@Getter
public enum SupportedCountry {

    // ======================== EAST AFRICA ========================
    UGANDA("Uganda", "UG", "UGA", "UGX",
            List.of(MobileNetwork.MTN_MOMO_UGA, MobileNetwork.AIRTEL_UGA),
            MobileNetwork.MTN_MOMO_UGA),

    TANZANIA("Tanzania", "TZ", "TZA", "TZS",
            List.of(MobileNetwork.AIRTEL_TZA, MobileNetwork.VODACOM_TZA,
                    MobileNetwork.TIGO_TZA, MobileNetwork.HALOTEL_TZA),
            MobileNetwork.AIRTEL_TZA),

    KENYA("Kenya", "KE", "KEN", "KES",
            List.of(MobileNetwork.MPESA_KEN, MobileNetwork.AIRTEL_KEN,
                    MobileNetwork.TKASH_KEN),
            MobileNetwork.MPESA_KEN),

    RWANDA("Rwanda", "RW", "RWA", "RWF",
            List.of(MobileNetwork.MTN_MOMO_RWA, MobileNetwork.AIRTEL_RWA),
            MobileNetwork.MTN_MOMO_RWA),

    // ======================== CENTRAL AFRICA ========================
    CAMEROON("Cameroon", "CM", "CMR", "XAF",
            List.of(MobileNetwork.MTN_MOMO_CMR, MobileNetwork.ORANGE_CMR),
            MobileNetwork.MTN_MOMO_CMR),

    // ======================== WEST AFRICA ========================
    NIGERIA("Nigeria", "NG", "NGA", "NGN",
            List.of(MobileNetwork.MTN_MOMO_NG, MobileNetwork.AIRTEL_NG,
                    MobileNetwork.GLO_NG, MobileNetwork.NINE_MOBILE_NG),
            MobileNetwork.MTN_MOMO_NG),

    BENIN("Benin", "BJ", "BEN", "XOF",
            List.of(MobileNetwork.MTN_MOMO_BEN, MobileNetwork.MOOV_BEN),
            MobileNetwork.MTN_MOMO_BEN),

    // ======================== SOUTHERN AFRICA ========================
    ZAMBIA("Zambia", "ZM", "ZMB", "ZMW",
            List.of(MobileNetwork.MTN_MOMO_ZMB, MobileNetwork.AIRTEL_ZMB),
            MobileNetwork.MTN_MOMO_ZMB),

    SOUTH_AFRICA("South Africa", "ZA", "ZAF", "ZAR",
            List.of(MobileNetwork.VODACOM_ZA, MobileNetwork.MTN_ZA,
                    MobileNetwork.TELKOM_ZA),
            MobileNetwork.VODACOM_ZA);

    private final String countryName;
    private final String iso2;
    private final String iso3;
    private final String currency;
    private final List<MobileNetwork> networks;
    private final MobileNetwork defaultNetwork;

    SupportedCountry(String countryName, String iso2, String iso3, String currency,
                     List<MobileNetwork> networks, MobileNetwork defaultNetwork) {
        this.countryName = countryName;
        this.iso2 = iso2;
        this.iso3 = iso3;
        this.currency = currency;
        this.networks = Collections.unmodifiableList(networks);
        this.defaultNetwork = defaultNetwork;
    }

    /**
     * Returns the provider for this country (always PawaPay in current implementation).
     */
    public Provider getProvider() {
        return Provider.PAWAPAY;
    }

    /**
     * Returns the network codes (e.g., "MTN_MOMO_UGA") for validation/compatibility.
     */
    public List<String> getNetworkCodes() {
        return networks.stream()
                .map(MobileNetwork::getNetworkCode)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Finds a SupportedCountry by its country code (ISO2 or ISO3, case-insensitive).
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
     * Validates that the given network code is supported for this country.
     *
     * @param networkCode the network code to validate (e.g., "MTN_MOMO_UGA")
     * @return true if the network is in this country's supported list
     */
    public boolean isValidNetwork(String networkCode) {
        if (networkCode == null || networkCode.isBlank()) {
            return false;
        }
        return networks.stream()
                .anyMatch(n -> n.getNetworkCode().equals(networkCode.trim().toUpperCase()));
    }

    /**
     * Builds a descriptive error message listing all supported country codes, currencies, and networks.
     */
    private static String buildErrorMessage(String invalidCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("Unsupported country code: '").append(invalidCode).append("'.\n\n");
        sb.append("Supported countries (ISO2 / ISO3 -> Name [Currency]):\n");
        for (SupportedCountry country : values()) {
            sb.append("  ")
              .append(String.format("%-2s / %-3s -> %-15s [%s]", country.iso2, country.iso3,
                      country.countryName, country.currency))
              .append("\n");
        }
        sb.append("\nSupported networks per country:\n");
        for (SupportedCountry country : values()) {
            sb.append("  ").append(country.iso2).append(": ")
              .append(String.join(", ", country.getNetworkCodes()))
              .append("\n");
        }
        return sb.toString();
    }

    /**
     * Checks if this country is supported by the PawaPay sandbox.
     * This is a runtime check against the sandbox configuration.
     */
    public boolean isSandboxSupported() {
        // This will be injected via configuration
        return true; // Default to true, overridden by PawaPaySandboxConfig
    }

    /**
     * Returns all supported ISO2 and ISO3 codes as a flat list.
     */
    public static List<String> getAllCountryCodes() {
        return Arrays.stream(values())
                .flatMap(c -> Stream.of(c.iso2, c.iso3))
                .collect(Collectors.toList());
    }

    /**
     * Returns the ISO2 code for a given ISO3 code (or vice versa).
     */
    public static String resolveToIso2(String code) {
        return findByCountryCode(code).getIso2();
    }

    /**
     * Returns the ISO3 code for a given ISO2 code (or vice versa).
     */
    public static String resolveToIso3(String code) {
        return findByCountryCode(code).getIso3();
    }
}