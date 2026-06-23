package com.banffpay.pawapay.service;

import com.banffpay.pawapay.model.MobileNetwork;
import com.banffpay.pawapay.model.SupportedCountry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for routing a country to its default mobile money network.
 * <p>
 * <b>Key Design:</b> Users submit only country, phone number, and amount.
 * The backend automatically determines the appropriate mobile money network
 * using a strategy based on {@link SupportedCountry}'s default network mapping.
 * </p>
 *
 * <p>This avoids the need for:
 * <ul>
 *   <li>{@code if/else} chains checking country codes</li>
 *   <li>Switch-case statements that need updating for each new country</li>
 *   <li>Clients needing to know provider/network codes</li>
 * </ul>
 * </p>
 *
 * <p><b>Routing Strategy:</b> Uses the {@link SupportedCountry#getDefaultNetwork()} mapping.
 * Each country has a default mobile money network defined in the enum itself,
 * making it trivially extensible — add a new country to {@link SupportedCountry}
 * with its default network, and routing works automatically.
 * </p>
 *
 * <p><b>Example:</b>
 * <pre>
 *   CountryRoutingResult result = routingService.route("KENYA", "254712345678");
 *   // result.country = SupportedCountry.KENYA
 *   // result.network = MobileNetwork.MPESA_KEN
 *   // result.currency = "KES"
 * </pre>
 * </p>
 *
 * @author BanffPay Team
 */
@Slf4j
@Service
public class CountryRoutingService {

    /**
     * Routes a country request to the appropriate mobile network.
     *
     * @param countryCode ISO2 (e.g., "ZM") or ISO3 (e.g., "ZMB") or enum name (e.g., "KENYA")
     * @param phoneNumber the customer's phone number (used for future ML-based routing)
     * @return CountryRoutingResult containing the resolved country, network, and currency
     * @throws IllegalArgumentException if the country is not supported
     */
    public CountryRoutingResult route(String countryCode, String phoneNumber) {
        // Step 1: Resolve the country from the provided code
        SupportedCountry country = SupportedCountry.findByCountryCode(countryCode);

        // Step 2: Get the default mobile network for this country
        MobileNetwork network = country.getDefaultNetwork();

        log.info("Country routed: country={} ({}) network={} currency={} phone={}",
                country.getCountryName(), country.getIso2(), network.getNetworkCode(),
                country.getCurrency(), maskPhone(phoneNumber));

        return new CountryRoutingResult(
                country,
                network,
                country.getCurrency(),
                country.getProvider()
        );
    }

    /**
     * Routes a country request with an optional explicit network preference.
     * Falls back to the default network if the preference is not provided.
     *
     * @param countryCode  ISO2 or ISO3 country code
     * @param networkCode  optional explicit network code (e.g., "AIRTEL_UGA")
     * @param phoneNumber  the customer's phone number
     * @return CountryRoutingResult
     * @throws IllegalArgumentException if country is not supported or network is invalid for the country
     */
    public CountryRoutingResult routeWithNetwork(String countryCode, String networkCode, String phoneNumber) {
        SupportedCountry country = SupportedCountry.findByCountryCode(countryCode);

        MobileNetwork network;
        if (networkCode != null && !networkCode.isBlank()) {
            // Explicit network requested — validate it belongs to the country
            network = MobileNetwork.findByNetworkCode(networkCode);
            if (!country.isValidNetwork(networkCode)) {
                throw new IllegalArgumentException(
                        "Network '" + networkCode + "' is not supported in " + country.getCountryName()
                        + ". Available networks: " + String.join(", ", country.getNetworkCodes())
                );
            }
        } else {
            network = country.getDefaultNetwork();
        }

        log.info("Country routed (explicit network): country={} network={} currency={}",
                country.getIso2(), network.getNetworkCode(), country.getCurrency());

        return new CountryRoutingResult(
                country,
                network,
                country.getCurrency(),
                country.getProvider()
        );
    }

    /**
     * Masks the middle digits of a phone number for logging.
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return phone;
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }

    /**
     * Result of country routing containing the resolved country, network, currency, and provider.
     */
    public record CountryRoutingResult(
            SupportedCountry country,
            MobileNetwork network,
            String currency,
            com.banffpay.pawapay.model.Provider provider
    ) {}
}