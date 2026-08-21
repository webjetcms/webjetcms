package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Constants;

/**
 * Tests currency normalization, validation, fallback selection, and conversion in {@link BasketTools}.
 */
class BasketToolsTest {

    private String originalSystemCurrency;
    private String originalDisplayCurrency;
    private String originalSupportedCurrencies;

    @BeforeEach
    void setUp() {
        originalSystemCurrency = Constants.getString(BasketTools.BASKET_PRODUCT_CURRENCY);
        originalDisplayCurrency = Constants.getString(BasketTools.BASKET_DISPLAY_CURRENCY);
        originalSupportedCurrencies = Constants.getString(BasketTools.SUPPORTED_CURRENCIES);
    }

    @AfterEach
    void tearDown() {
        Constants.setString(BasketTools.BASKET_PRODUCT_CURRENCY, originalSystemCurrency);
        Constants.setString(BasketTools.BASKET_DISPLAY_CURRENCY, originalDisplayCurrency);
        Constants.setString(BasketTools.SUPPORTED_CURRENCIES, originalSupportedCurrencies);
    }

    /**
     * Verifies that configured currencies are normalized, deduplicated, and filtered to valid codes.
     */
    @Test
    void filtersAndNormalizesConfiguredCurrencyCodes() {
        Constants.setString(
            BasketTools.SUPPORTED_CURRENCIES,
            "eur, Kč,usd<script>,123, gbp,EUR"
        );

        assertArrayEquals(new String[] { "eur", "czk", "gbp" }, BasketTools.getSupportedCurrencies());
        assertEquals("czk", BasketTools.getNormalizedSupportedCurrency(" CZK "));
        assertEquals("czk", BasketTools.getNormalizedSupportedCurrency("Kč"));
        assertTrue(BasketTools.isCurrencySupported("GBP"));
        assertFalse(BasketTools.isCurrencySupported("usd<script>"));
        assertNull(BasketTools.getNormalizedSupportedCurrency("123"));
    }

    /**
     * Verifies that invalid primary currencies fall back safely or fail when no valid option exists.
     */
    @Test
    void usesSafeFallbackForInvalidSystemCurrency() {
        Constants.setString(BasketTools.BASKET_PRODUCT_CURRENCY, "<img src=x onerror=alert(1)>");
        Constants.setString(BasketTools.BASKET_DISPLAY_CURRENCY, "Kč");
        Constants.setString(BasketTools.SUPPORTED_CURRENCIES, "usd,czk");

        assertEquals("czk", BasketTools.getSystemCurrency());

        Constants.setString(BasketTools.BASKET_DISPLAY_CURRENCY, "invalid");
        assertEquals("usd", BasketTools.getSystemCurrency());

        Constants.setString(BasketTools.SUPPORTED_CURRENCIES, "invalid,123");
        assertThrows(IllegalStateException.class, BasketTools::getSystemCurrency);
    }

    /**
     * Verifies that currency conversion accepts normalized aliases and rejects unsafe codes.
     */
    @Test
    void rejectsInvalidCurrencyCodesDuringConversion() {
        assertEquals(
            BigDecimal.TEN,
            BasketTools.convertCurrency(BigDecimal.TEN, "Kč", "czk")
        );
        assertThrows(
            IllegalStateException.class,
            () -> BasketTools.convertCurrency(BigDecimal.ONE, "eur<script>", "usd")
        );
    }
}
