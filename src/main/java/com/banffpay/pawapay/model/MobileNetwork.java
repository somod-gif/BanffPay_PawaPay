package com.banffpay.pawapay.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Mobile money networks supported by the BanffPay platform via PawaPay.
 * <p>
 * <b>Critical Design Decision:</b> These are MOBILE MONEY NETWORKS, NOT providers.
 * PawaPay is the payment provider. Networks like MTN_MOMO_UGA, AIRTEL_UGA, MPESA_KEN
 * are the mobile money networks that PawaPay connects to.
 * </p>
 *
 * <p>Each network is mapped to its relevant country codes for routing resolution.</p>
 *
 * @author BanffPay Team
 */
@Getter
public enum MobileNetwork {

    // ======================== UGANDA ========================
    MTN_MOMO_UGA("MTN MoMo Uganda", "UG", "UGA", "UGX"),
    AIRTEL_UGA("Airtel Uganda", "UG", "UGA", "UGX"),

    // ======================== TANZANIA ========================
    AIRTEL_TZA("Airtel Tanzania", "TZ", "TZA", "TZS"),
    VODACOM_TZA("Vodacom Tanzania", "TZ", "TZA", "TZS"),
    TIGO_TZA("Tigo Tanzania", "TZ", "TZA", "TZS"),
    HALOTEL_TZA("Halotel Tanzania", "TZ", "TZA", "TZS"),

    // ======================== KENYA ========================
    MPESA_KEN("M-Pesa Kenya", "KE", "KEN", "KES"),
    AIRTEL_KEN("Airtel Kenya", "KE", "KEN", "KES"),
    TKASH_KEN("T-Kash Kenya", "KE", "KEN", "KES"),

    // ======================== RWANDA ========================
    MTN_MOMO_RWA("MTN MoMo Rwanda", "RW", "RWA", "RWF"),
    AIRTEL_RWA("Airtel Rwanda", "RW", "RWA", "RWF"),

    // ======================== CAMEROON ========================
    MTN_MOMO_CMR("MTN MoMo Cameroon", "CM", "CMR", "XAF"),
    ORANGE_CMR("Orange Cameroon", "CM", "CMR", "XAF"),

    // ======================== NIGERIA ========================
    MTN_MOMO_NG("MTN MoMo Nigeria", "NG", "NGA", "NGN"),
    AIRTEL_NG("Airtel Nigeria", "NG", "NGA", "NGN"),
    GLO_NG("Glo Nigeria", "NG", "NGA", "NGN"),
    NINE_MOBILE_NG("9Mobile Nigeria", "NG", "NGA", "NGN"),

    // ======================== BENIN ========================
    MTN_MOMO_BEN("MTN MoMo Benin", "BJ", "BEN", "XOF"),
    MOOV_BEN("Moov Benin", "BJ", "BEN", "XOF"),

    // ======================== ZAMBIA ========================
    MTN_MOMO_ZMB("MTN MoMo Zambia", "ZM", "ZMB", "ZMW"),
    AIRTEL_ZMB("Airtel Zambia", "ZM", "ZMB", "ZMW"),

    // ======================== SOUTH AFRICA ========================
    VODACOM_ZA("Vodacom South Africa", "ZA", "ZAF", "ZAR"),
    MTN_ZA("MTN South Africa", "ZA", "ZAF", "ZAR"),
    TELKOM_ZA("Telkom South Africa", "ZA", "ZAF", "ZAR");

    private final String displayName;
    private final String iso2;
    private final String iso3;
    private final String currency;

    MobileNetwork(String displayName, String iso2, String iso3, String currency) {
        this.displayName = displayName;
        this.iso2 = iso2;
        this.iso3 = iso3;
        this.currency = currency;
    }

    /**
     * Returns the network code (e.g., "MTN_MOMO_UGA") which is the enum name.
     */
    public String getNetworkCode() {
        return this.name();
    }

    /**
     * Finds a MobileNetwork by its network code (enum name), case-insensitive.
     *
     * @param networkCode the network code (e.g., "MTN_MOMO_UGA")
     * @return the matching MobileNetwork
     * @throws IllegalArgumentException if the code is null, blank, or no network matches
     */
    public static MobileNetwork findByNetworkCode(String networkCode) {
        if (networkCode == null || networkCode.isBlank()) {
            throw new IllegalArgumentException("Network code must not be null or blank");
        }
        String normalized = networkCode.trim().toUpperCase();
        for (MobileNetwork network : values()) {
            if (network.name().equals(normalized)) {
                return network;
            }
        }
        throw new IllegalArgumentException("Unsupported mobile network: '" + networkCode
                + "'. Supported networks: " + Arrays.toString(values()));
    }

    /**
     * Finds all networks for a given country ISO2 or ISO3 code.
     *
     * @param countryCode ISO2 (e.g., "UG") or ISO3 (e.g., "UGA") code
     * @return list of MobileNetworks for that country
     */
    public static List<MobileNetwork> findByCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return Collections.emptyList();
        }
        String normalized = countryCode.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(n -> n.iso2.equals(normalized) || n.iso3.equals(normalized))
                .toList();
    }
}