package sk.iway.iwcm.common;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.basket.rest.EshopService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Provides shared helpers for basket currencies and localized country names.
 */
public class BasketTools {

	public static final String COUNTRY_KEY_PREFIX = "stat.countries.tld";
	public static final String BASKET_PRODUCT_CURRENCY = "basketProductCurrency";
	public static final String BASKET_DISPLAY_CURRENCY = "basketDisplayCurrency";
	public static final String SUPPORTED_CURRENCIES = "supportedCurrencies";
	private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("^[a-zA-Z]{3}$");

	private BasketTools() {}

	/**
	 * Resolves the basket system currency from the configured currency fallbacks.
	 *
	 * @return the normalized three-letter currency code
	 * @throws IllegalStateException if no valid basket currency is configured
	 */
	public static String getSystemCurrency() {
		String systemCurrency = normalizeCurrencyCode(Constants.getString(BASKET_PRODUCT_CURRENCY));
		if(systemCurrency != null) return systemCurrency;

		String displayCurrency = normalizeCurrencyCode(Constants.getString(BASKET_DISPLAY_CURRENCY));
		if(displayCurrency != null) return displayCurrency;

		String[] supportedCurrencies = getSupportedCurrencies();
		if(supportedCurrencies.length > 0) return supportedCurrencies[0];

		throw new IllegalStateException("No valid basket currency is configured.");
	}

	public static BigDecimal convertToBasketDisplayCurrency(BigDecimal amount, HttpServletRequest request) {
		return convertCurrency(amount, Constants.getString(BASKET_PRODUCT_CURRENCY), EshopService.getDisplayCurrency(request));
	}

	/**
	 * Converts an amount using a configured {@code kurz_FROM_TO} exchange rate.
	 *
	 * A reverse rate is used when no direct rate exists. If neither rate is configured,
	 * the original amount is returned.
	 *
	 * @param amount  amount to convert
	 * @param fromCurrency  source currency code
	 * @param toCurrency  target currency code
	 * @return the converted amount, or the original amount when no exchange rate is configured
	 * @throws IllegalStateException if either currency is invalid or a configured rate is malformed
	 */
    public static BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
		if(BigDecimal.ZERO.equals(amount)) return amount;

		fromCurrency = normalizeCurrencyCode(fromCurrency);
		toCurrency = normalizeCurrencyCode(toCurrency);
		if(fromCurrency == null || toCurrency == null) throw new IllegalStateException("Currencies not valid.");

		if(fromCurrency.equals(toCurrency)) return amount;

		try {
			String constantName = "kurz_" + fromCurrency + "_" + toCurrency;
			BigDecimal rate;

			// We found basic rate
			if (Tools.isNotEmpty(Constants.getString(constantName))) {
				rate = new BigDecimal( Constants.getString(constantName) );
				return rate.multiply( amount );
			}

			// unsuccessful, try reverse convert
			constantName = "kurz_" + toCurrency + "_" + fromCurrency;

			// because it's revert rate, we need to do
			// 1/rate
			if (Tools.isNotEmpty(Constants.getString(constantName))) {
				rate = new BigDecimal( Constants.getString(constantName) );
				return (BigDecimal.ONE.divide(rate, 3, RoundingMode.HALF_EVEN)).multiply( amount );
			}

		} catch (NumberFormatException e) {
			sk.iway.iwcm.Logger.error(e);
			throw new IllegalStateException("Malformed constant format for currencies " + fromCurrency + " and " + toCurrency);
		}

		return amount;
	}

	public static String getCountryName(String countryCode, Prop prop) {
		if(Tools.isEmpty(countryCode)) return "";
		if(prop == null) prop = Prop.getInstance();
		if(countryCode.startsWith(".") == false) countryCode = "." + countryCode;
		return prop.getText(COUNTRY_KEY_PREFIX + countryCode.toLowerCase());
	}

	/**
	 * Returns the distinct, normalized currency codes enabled for the basket.
	 *
	 * @return configured valid currency codes in their original order
	 */
	public static String[] getSupportedCurrencies() {
		return Arrays.stream(Constants.getString(SUPPORTED_CURRENCIES).split(","))
			.map(BasketTools::normalizeCurrencyCode)
			.filter(currency -> currency != null)
			.distinct()
			.toArray(String[]::new);
	}

  	public static List<LabelValue> getSupportedCurrenciesOptions() {
        List<String> supportedCurrencies = Arrays.asList( getSupportedCurrencies() );
        List<LabelValue> groupsList = new ArrayList<>();
        for (String curr: supportedCurrencies) groupsList.add( new LabelValue(curr, curr) );
        return groupsList;
    }

	public static boolean isCurrencySupported(String currency) {
		return getNormalizedSupportedCurrency(currency) != null;
	}

	/**
	 * Normalizes a currency code and verifies that it is enabled for the basket.
	 *
	 * @param currency  currency code to validate
	 * @return the normalized supported code, or {@code null} when the value is invalid or unsupported
	 */
	public static String getNormalizedSupportedCurrency(String currency) {
		String normalizedCurrency = normalizeCurrencyCode(currency);
		if(normalizedCurrency == null) return null;

		List<String> supportedCurrencies = Arrays.asList(getSupportedCurrencies());
		return supportedCurrencies.contains(normalizedCurrency) ? normalizedCurrency : null;
	}

	/**
	 * Converts a configured currency value to a lowercase ISO-style code.
	 *
	 * @param currency  currency value to normalize
	 * @return the normalized three-letter code, or {@code null} for an invalid value
	 */
	private static String normalizeCurrencyCode(String currency) {
		if(currency == null) return null;

		String trimmedCurrency = currency.trim();
		if("Kč".equalsIgnoreCase(trimmedCurrency)) return "czk";

		String normalizedCurrency = trimmedCurrency.toLowerCase(Locale.ROOT);
		return CURRENCY_CODE_PATTERN.matcher(normalizedCurrency).matches() ? normalizedCurrency : null;
	}
}
