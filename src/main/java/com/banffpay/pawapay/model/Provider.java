package com.banffpay.pawapay.model;

/**
 * Payment provider for the BanffPay platform.
 * <p>
 * Currently supports only PawaPay as the payment provider.
 * In the future, this enum can be extended to support additional providers
 * (e.g., Flutterwave, Stripe, Paystack) without architectural changes.
 * </p>
 *
 * <p><b>Design Decision:</b> PawaPay is the PAYMENT PROVIDER.
 * Mobile money networks (MTN_MOMO_UGA, AIRTEL_UGA, MPESA_KEN, etc.)
 * are NOT providers — they are mobile money networks that PawaPay connects to.
 * </p>
 *
 * @author BanffPay Team
 */
public enum Provider {

    PAWAPAY("PawaPay", "Africa's leading mobile money aggregator");

    private final String displayName;
    private final String description;

    Provider(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns the default provider for the BanffPay platform.
     */
    public static Provider getDefaultProvider() {
        return PAWAPAY;
    }
}