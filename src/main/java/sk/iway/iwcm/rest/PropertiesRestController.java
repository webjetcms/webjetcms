package sk.iway.iwcm.rest;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;

/**
 *  PropertiesController.java
 *  <br>
 *  <br>Return text properties in given language by prefix, or exact match
 *  <br>Possible languages: sk, cz, en, de, pl, hu, cho, ru, esp
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2017
 *@author       $Author: jeeff rzapach $
 *@version      $Revision: 1.3 $
 *@created      Date: 16.1.2017 12:04:56
 *@modified     $Date: 2004/08/16 06:26:11 $
 */

@RestController
public class PropertiesRestController extends sk.iway.iwcm.rest.RestController
{
	/**
	 * Returns list of pairs: text property key and text property value, in given language starting with given prefix.
	 * <br>
	 * <br>Example:
	 * <br>
	 * <br>URL - /rest/properties/sk/components.abtesting
	 * <br>Returned pairs:
	 * 	<br>components.abtesting.dialog_title = AB testovanie
	 * 	<br>components.abtesting.allowed = AB testovanie povolene
	 * 	<br>components.abtesting.ratio = Pomer
	 * 	<br>components.abtesting.variantName = Nazov varianty
	 * 	<br>components.abtesting.example = Priklad
	 *
	 * @param request
	 * @param lng property language
	 * @param prefix starting property key string for required property
	 * @return list of pairs: text property key and text property value
	 */
	@RequestMapping(path={"/rest/properties/{lng}/{prefix:.+}"}, method=RequestMethod.GET)
	public Map<String, String> getKeysWithPrefix(HttpServletRequest request, @PathVariable String lng, @PathVariable String prefix)
	{
		if (!isIpAddressAllowed(request)) {
			return null;
		}

		return getKeysWithPrefixWithoutIpValidation(request, lng, prefix);
	}

	public Map<String, String> getKeysWithPrefixWithoutIpValidation(HttpServletRequest request, String lng, String prefix)
	{
		Prop prop = Prop.getInstance(lng);

		if(isKeyAllowed(prefix) == false) return new HashMap<>();

		return prop.getTextStartingWith(prefix);
	}

	/**
	 * Verify if key prefix is allowed
	 * @param key
	 * @return
	 */
	private boolean isKeyAllowed(String key) {
		String propertiesRestControllerAllowedKeysPrefixes = Constants.getString("propertiesRestControllerAllowedKeysPrefixes");
		if (Tools.isEmpty(propertiesRestControllerAllowedKeysPrefixes)) return false;
		if ("*".equals(propertiesRestControllerAllowedKeysPrefixes)) return true;

		String[] allowedPrefixes = Constants.getArray("propertiesRestControllerAllowedKeysPrefixes");
		for (String prefix : allowedPrefixes) {
			if (key.startsWith(prefix)) return true;
		}

		return false;
	}

	/**
	 * Returns pair property key - property value by property language and exact property key.
	 * <br>If property contains variables: {0}, {1}, {2}, ..., it is possible to fill them with additional path variables.
	 * <br>
	 * <br>Examples:
	 * <br>
	 * <br>1)
	 * <br>Property key (in slovak) - converter.number.invalidNumber
	 * <br>Property value - Hodnota ({1}) v poli {0} musi byt cislo
	 * <br>URL - /rest/property/sk/converter.number.invalidNumber/4/test
	 * <br>Returned property value - Hodnota (test) v poli 4 musi byt cislo
	 * <br>
	 * <br>2)
	 * <br>Property key (in slovak) - calendar.invitation.saveok-A
	 * <br>Property value - Dakujeme za akceptovanie schodzky.
	 * <br>URL - /rest/property/sk/calendar.invitation.saveok-A
	 * <br>Returned property value - Dakujeme za akceptovanie schodzky.
	 *
	 * @param request
	 * @param key property key
	 * @param lng property language
	 * @return pair of text property key and text property value
	 */
	@RequestMapping(path={"/rest/property/{lng}/{key:.+}/**"}, method=RequestMethod.GET)
	public Entry<String, String> getKey(HttpServletRequest request,
				@PathVariable String key,
				@PathVariable String lng)
	{
		if(isIpAddressAllowed(request) == false || isKeyAllowed(key) == false)
			return null;

		Prop prop = Prop.getInstance(lng);
		String value = prop.getText(key);

		String calledUrl = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		String paramsString = calledUrl.substring( ("/rest/property/"+lng+"/"+key).length() );

		//doplnime parametre {i}, ak su zadane
		if(Tools.isNotEmpty(paramsString) && paramsString.length()>1)
		{
			paramsString = paramsString.substring(1);
			String[] params = paramsString.split("/");
			if(params!=null)
			{
				for(int i=0; i<params.length; i++)
				{
					value = Tools.replace(value, "!"+ (i), params[i]);
					value = Tools.replace(value, "{"+ (i) +"}", params[i]);
				}
			}
		}

		Entry<String, String> result = new SimpleEntry<String, String>(key, value);
		return result;
	}
}