package sk.iway.iwcm.common;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BasketTools {

	public static final String COUNTRY_KEY_PREFIX = "stat.countries.tld.";

	private BasketTools() {}

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
}