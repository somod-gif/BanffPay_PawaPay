package com.banffpay.pawapay.service;

import com.banffpay.pawapay.model.MobileNetwork;
import com.banffpay.pawapay.model.SupportedCountry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Resolves country-related information for deposit and payout operations.
 * <p>
 * Single responsibility: country code → SupportedCountry mapping.
 * Reusable across deposit, payout, and webhook services.
 * </p>
 */
@Slf4j
@Service
public class CountryResolver {

    /**
     * Resolves a country code (ISO2/ISO3) to SupportedCountry.
     */
    public SupportedCountry resolve(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            throw new IllegalArgumentException("Country code is required");
        }
        try {
            SupportedCountry country = SupportedCountry.findByCountryCode(countryCode);
            log.debug("Resolved country: {} -> {}", countryCode, country.getIso2());
            return country;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid country code: {}", countryCode);
            throw new IllegalArgumentException("Unsupported country: '" + countryCode + "'");
        }
    }

    /**
     * Resolves the default mobile network for a country.
     */
    public MobileNetwork resolveDefaultNetwork(SupportedCountry country) {
        MobileNetwork network = country.getDefaultNetwork();
        log.debug("Default network for {}: {}", country.getIso2(), network.getNetworkCode());
        return network;
    }

    /**
     * Resolves a specific network for a country (if provided).
     */
    public MobileNetwork resolveNetwork(SupportedCountry country, String networkCode) {
        if (networkCode == null || networkCode.isBlank()) {
            return resolveDefaultNetwork(country);
        }
        MobileNetwork network = MobileNetwork.findByNetworkCode(networkCode);
        if (!country.isValidNetwork(networkCode)) {
            throw new IllegalArgumentException(
                    "Network '" + networkCode + "' not supported in " + country.getCountryName()
            );
        }
        log.debug("Resolved network for {}: {}", country.getIso2(), network.getNetworkCode());
        return network;
    }
}