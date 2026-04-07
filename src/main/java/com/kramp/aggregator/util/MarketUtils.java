package com.kramp.aggregator.util;

import java.util.Map;

/**
 * BCP 47 market → ISO 4217 currency lookup.
 */
public final class MarketUtils {

    private static final Map<String, String> MARKET_TO_CURRENCY = Map.ofEntries(
            Map.entry("nl-NL", "EUR"),
            Map.entry("de-DE", "EUR"),
            Map.entry("fr-FR", "EUR"),
            Map.entry("be-BE", "EUR"),
            Map.entry("at-AT", "EUR"),
            Map.entry("fi-FI", "EUR"),
            Map.entry("pl-PL", "PLN"),
            Map.entry("en-GB", "GBP"),
            Map.entry("sv-SE", "SEK"),
            Map.entry("da-DK", "DKK"),
            Map.entry("cs-CZ", "CZK"),
            Map.entry("hu-HU", "HUF"),
            Map.entry("ro-RO", "RON"),
            Map.entry("no-NO", "NOK")
    );

    private MarketUtils() {}

    public static String getCurrency(String market) {
        return MARKET_TO_CURRENCY.getOrDefault(market, "EUR");
    }
}
