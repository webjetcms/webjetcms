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

import javax.servlet.http.HttpServletRequest;

public class BasketTools {

	public static final String COUNTRY_KEY_PREFIX = "stat.countries.tld.";
	public static final String BASKET_PRODUCT_CURRENCY = "basketProductCurrency";
	public static final String SUPPORTED_CURRENCIES = "supportedCurrencies";

	private BasketTools() {}

	public static BigDecimal convertToBasketDisplayCurrency(BigDecimal ammount, HttpServletRequest request) {
		return convertCurrency(ammount, Constants.getString(BASKET_PRODUCT_CURRENCY), EshopService.getDisplayCurrency(request));
	}

    public static BigDecimal convertCurrency(BigDecimal ammount, String fromCurrency, String toCurrency) {
		if(BigDecimal.ZERO.equals(ammount)) return ammount;

		if(Tools.isEmpty(fromCurrency) || Tools.isEmpty(toCurrency)) throw new IllegalStateException("Currencies not valid.");

		fromCurrency = fromCurrency.toLowerCase();
		toCurrency = toCurrency.toLowerCase();

		if(fromCurrency.equals(toCurrency)) return ammount;

		try {
			String constantName = "kurz_" + fromCurrency + "_" + toCurrency;
			BigDecimal rate;

			// We found basic rate
			if (Tools.isNotEmpty(Constants.getString(constantName))) {
				rate = new BigDecimal( Constants.getString(constantName) );
				return rate.multiply( ammount );
			}

			// unsuccessful, try reverse convert
			constantName = "kurz_" + toCurrency + "_" + fromCurrency;

			// because it's revert rate, we need to do
			// 1/rate
			if (Tools.isNotEmpty(Constants.getString(constantName))) {
				rate = new BigDecimal( Constants.getString(constantName) );
				return (BigDecimal.ONE.divide(rate, 3, RoundingMode.HALF_EVEN)).multiply( ammount );
			}

		} catch (NumberFormatException e) {
			sk.iway.iwcm.Logger.error(e);
			throw new IllegalStateException("Malformed constant format for currencies " + fromCurrency + " and " + toCurrency);
		}

		return ammount;
	}

	public static String getCountryName(String countryCode, Prop prop) {
		if(Tools.isEmpty(countryCode)) return "";
		if(prop == null) prop = Prop.getInstance();
		if(countryCode.startsWith(".")) countryCode = countryCode.substring(1);
		return prop.getText(COUNTRY_KEY_PREFIX + countryCode.toLowerCase());
	}

	public static String[] getSupportedCurrencies() {
		return Constants.getString(SUPPORTED_CURRENCIES).split(",");
	}

  	public static List<LabelValue> getSupportedCurrenciesOptions() {
        List<String> supportedCurrencies = Arrays.asList( getSupportedCurrencies() );
        List<LabelValue> groupsList = new ArrayList<>();
        for (String curr: supportedCurrencies) groupsList.add( new LabelValue(curr, curr) );
        return groupsList;
    }

	public static boolean isCurrencySupported(String currency) {
        if(Tools.isEmpty(currency) == true) return false;
        List<String> supportedCurrencies = Arrays.asList( getSupportedCurrencies() );
        return supportedCurrencies.contains(currency);
    }
}