package sk.iway.iwcm.rest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.i18n.PropDB;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.users.UsersDB;

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
		Identity user = UsersDB.getCurrentUser(request);
		if(user!=null && user.isAdmin() && user.isEnabledItem("menuTemplatesGroup")) {
			//potrebujeme to pre admin cast, konkretne skupiny sablon, pristup povol
		} else if (!isIpAddressAllowed(request)) {
			return null;
		}

		return getKeysWithPrefixWithoutIpValidation(request, lng, prefix);
	}

	public Map<String, String> getKeysWithPrefixWithoutIpValidation(HttpServletRequest request, String lng, String prefix)
	{
		Prop prop = Prop.getInstance(lng);
		Map<String, String> result = prop.getTextStartingWith(prefix);
		return result;
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
		if(!isIpAddressAllowed(request))
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


	/**
	 *
	 * Create/Update text property
	 *
	 * @param lng
	 * @param key
	 * @param value
	 * @return
	 */
	@RequestMapping(path={"/admin/rest/property/set"}, method=RequestMethod.POST)
	@PreAuthorize("@WebjetSecurityService.hasPermission('edit_text')")
	public Entry<String, String> setText(HttpServletRequest request, String lng, String key, String value){
		Connection db_conn = null;
		PreparedStatement ps = null;
		Identity user = UsersDB.getCurrentUser(request);
		try
		{
			if (PropDB.canEdit(user, key))
			{

				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("DELETE FROM " + ConfDB.PROPERTIES_TABLE_NAME + " WHERE prop_key = ? AND lng = ?");
				ps.setString(1,key);
				ps.setString(2,lng);
				ps.execute();
				ps.close();

				ps = db_conn.prepareStatement("INSERT INTO " + ConfDB.PROPERTIES_TABLE_NAME + " (prop_key, lng, prop_value) VALUES(?, ?, ?)");
				ps.setString(1,key);
				ps.setString(2,lng);
				ps.setString(3, PropDB.escapeUnsafeValue(user, lng, key, value));
				ps.execute();
				ps.close();
				db_conn.close();

				Prop.getInstance(true);

				Entry<String, String> result = new SimpleEntry<>(key, value);

				StringBuilder log = new StringBuilder().append("Zmeneny internacionalizovany text: ").append(lng).append(',').append(key).append(',').append(value);
				Adminlog.add(Adminlog.TYPE_PROP_UPDATE, log.toString(), -1, -1);

				return result;
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		Entry<String, String> result = new SimpleEntry<>("message", "vyskytol sa problem");
		return result;
	}
}