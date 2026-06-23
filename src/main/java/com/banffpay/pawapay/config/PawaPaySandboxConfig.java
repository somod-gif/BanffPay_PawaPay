package com.banffpay.pawapay.config;

import com.banffpay.pawapay.model.SupportedCountry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * PawaPay Sandbox Configuration.
 * <p>
 * <b>Important:</b> The PawaPay sandbox account only supports specific countries.
 * This configuration defines which countries are ACTUALLY available in the sandbox.
 * <p>
 * Countries not in this list will receive a clear 400 error instead of a generic 500.
 * </p>
 * <p>
 * <b>To enable more countries:</b> Contact PawaPay to enable them on your sandbox account.
 * </p>
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "pawapay.sandbox")
public class PawaPaySandboxConfig {

    /**
     * Countries enabled on the PawaPay sandbox account.
     * Based on testing: TZ, KE, RW, CM, BJ, ZM work. NG, ZA, UG have issues.
     */
    private Set<String> enabledCountries = new HashSet<>(Set.of(
            "TZ",  // Tanzania
            "KE",  // Kenya
            "RW",  // Rwanda
            "CM",  // Cameroon
            "BJ",  // Benin
            "ZM"   // Zambia
    ));

    /**
     * Networks that are known to have issues on sandbox.
     */
    private Set<String> blockedNetworks = new HashSet<>(Set.of(
            // Add networks here if they consistently fail
    ));

    public Set<String> getEnabledCountries() {
        return enabledCountries;
    }

    public void setEnabledCountries(Set<String> enabledCountries) {
        this.enabledCountries = enabledCountries;
    }

    public Set<String> getBlockedNetworks() {
        return blockedNetworks;
    }

    public void setBlockedNetworks(Set<String> blockedNetworks) {
        this.blockedNetworks = blockedNetworks;
    }

    /**
     * Checks if a country is enabled on the sandbox.
     */
    public boolean isCountryEnabled(String iso2Code) {
        boolean enabled = enabledCountries.contains(iso2Code.toUpperCase());
        if (!enabled) {
            log.warn("Country {} is not enabled on PawaPay sandbox. Enabled countries: {}", iso2Code, enabledCountries);
        }
        return enabled;
    }

    /**
     * Checks if a network is blocked on the sandbox.
     */
    public boolean isNetworkBlocked(String networkCode) {
        return blockedNetworks.contains(networkCode.toUpperCase());
    }

    /**
     * Returns a user-friendly message for unsupported countries.
     */
    public String getUnsupportedCountryMessage(String iso2Code) {
        return String.format(
                "Country %s is not supported by current PawaPay account. Supported countries: %s",
                iso2Code, enabledCountries
        );
    }

    /**
     * Returns a user-friendly message for unsupported networks.
     */
    public String getUnsupportedNetworkMessage(String networkCode, String countryCode) {
        return String.format(
                "Mobile money provider %s is not configured for country %s",
                networkCode, countryCode
        );
    }
}