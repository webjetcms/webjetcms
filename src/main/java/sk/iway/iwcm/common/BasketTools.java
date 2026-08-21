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

public class BasketTools {

	public static final String COUNTRY_KEY_PREFIX = "stat.countries.tld";
	public static final String BASKET_PRODUCT_CURRENCY = "basketProductCurrency";
	public static final String BASKET_DISPLAY_CURRENCY = "basketDisplayCurrency";
	public static final String SUPPORTED_CURRENCIES = "supportedCurrencies";
	private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("^[a-zA-Z]{3}$");

	private BasketTools() {}

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
	 * Convert amount from one currency to another using defined constants kurs_FROM_TO, e.g. kurz_eur_usd=1.123
	 * @param amount
	 * @param fromCurrency
	 * @param toCurrency
	 * @return
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

	public static String getNormalizedSupportedCurrency(String currency) {
		String normalizedCurrency = normalizeCurrencyCode(currency);
		if(normalizedCurrency == null) return null;

		List<String> supportedCurrencies = Arrays.asList(getSupportedCurrencies());
		return supportedCurrencies.contains(normalizedCurrency) ? normalizedCurrency : null;
	}

	private static String normalizeCurrencyCode(String currency) {
		if(currency == null) return null;

		String trimmedCurrency = currency.trim();
		if("Kč".equalsIgnoreCase(trimmedCurrency)) return "czk";

		String normalizedCurrency = trimmedCurrency.toLowerCase(Locale.ROOT);
		return CURRENCY_CODE_PATTERN.matcher(normalizedCurrency).matches() ? normalizedCurrency : null;
	}
}
