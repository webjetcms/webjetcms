package sk.iway.iwcm.i18n;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.tags.support_logic.ResponseUtils;

/**
 *  PropDB.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: Marián Halaš $
 *@version      $Revision: 1.3 $
 *@created      Date: 9.2.2012 16:47:09
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class PropDB
{
	protected PropDB() {
		//utility class
	}

	/**
	 * Metoda spravi insert/update do customizovanych properties, ak v danom jazyku uz je v db dany kluc spravi sa update
	 * ak tam taky kluc nie je, spravi sa insert
	 *
	 * @param iwprop
	 * @param lng
	 * @param addPrefix - prida zadany prefix na zaciatok kluca
	 * @param filterPrefix - ulozi iba kluce zacinajuce na tento prefix
	 * @param onlyNewKeys - ak je true, a ak uz taky kluc existuje, nevykona sa update
	 * @deprecated - use version with Identity user parameter
	 */
	@Deprecated
	public static void save(IwayProperties iwprop, String lng, String addPrefix, String filterPrefix, boolean onlyNewKeys) {
		save(null, iwprop, lng, addPrefix, filterPrefix, onlyNewKeys);
	}

	/**
	 * Metoda spravi insert/update do customizovanych properties, ak v danom jazyku uz je v db dany kluc spravi sa update
	 * ak tam taky kluc nie je, spravi sa insert
	 *
	 * @param user - aktualne prihlaseny pouzivatel
	 * @param iwprop
	 * @param lng
	 * @param addPrefix - prida zadany prefix na zaciatok kluca
	 * @param filterPrefix - ulozi iba kluce zacinajuce na tento prefix
	 * @param onlyNewKeys - ak je true, a ak uz taky kluc existuje, nevykona sa update
	 */
	public static void save(Identity user, IwayProperties iwprop, String lng, String addPrefix, String filterPrefix, boolean onlyNewKeys)
	{
		int insertCounter = 0;
		int updateCounter = 0;
		if(iwprop == null) throw new IllegalArgumentException("IwayProp can't be null!");
		if(Tools.isEmpty(lng)) throw new IllegalArgumentException("Language can't be empty!");

		for(Entry<String, String> property : iwprop.entrySet())
		{
			if(Tools.isEmpty(property.getKey())) continue;

			String key = property.getKey();

			//pripoji zadany prefix pred nazov kluca
			if(Tools.isNotEmpty(addPrefix))
				key = addPrefix + "." + key;

			key = Tools.replace(key, "\"", "");

			//ulozi iba kluce, kotre zacinaju na filterPrefix
			if(Tools.isNotEmpty(filterPrefix) && !key.startsWith(filterPrefix))
				continue;

			String value = escapeUnsafeValue(user, lng, key, property.getValue());
			Logger.debug(PropDB.class, "Importing prop, key="+key+" value="+value);

			if((new SimpleQuery().forInt("Select count(*) from "+ConfDB.PROPERTIES_TABLE_NAME+" where lng = ? and prop_key = ?", lng, key)) > 0)
			{
				// ak onlyNewKeys je true -> nerobime update uz existujucich klucov, iba vkladame nove
				if(onlyNewKeys == false)
				{
					new SimpleQuery().execute("UPDATE "+ConfDB.PROPERTIES_TABLE_NAME+" SET prop_value=? WHERE prop_key=? AND lng = ?",value,key,lng);
					updateCounter++;
				}
			}
			else
			{
				new SimpleQuery().execute("INSERT INTO "+ConfDB.PROPERTIES_TABLE_NAME+" (prop_key,lng,prop_value) VALUES (?,?,?)",key,lng,value);
				insertCounter++;
			}
		}
		Logger.debug(PropDB.class, "IwayProperties saved, inserted: " + insertCounter + " , updated: " + updateCounter);
	}

	/**
	 * Odfiltruje zoznam textovych klucov podla nastaveni konstanty propertiesEnabledKeys a prav usera (na neobmedzene zobrazenie textov)
	 * @param user
	 * @param propList
	 * @return
	 */
	public static List<String> filterByPerms(Identity user, List<String> propList)
	{
		//odfiltruj zoznam podla prav
		String propertiesEnabledKeys = Constants.getStringExecuteMacro("propertiesEnabledKeys");

		if (Tools.isEmpty(propertiesEnabledKeys) || user.isEnabledItem("prop.show_all_texts")) return propList;

		List<String> filtered = new ArrayList<>();
		String[] enabledKeys = Tools.getTokens(propertiesEnabledKeys, ",");
		for (String c : propList)
		{
			if (isKeyVisibleToUser(user, enabledKeys, c))
			{
				filtered.add(c);
			}
		}

		return filtered;
	}

	public static Map<String, String> filterByPerms(Identity user, Map<String, String> propList)
	{
		//odfiltruj zoznam podla prav
		String propertiesEnabledKeys = Constants.getStringExecuteMacro("propertiesEnabledKeys");

		if (Tools.isEmpty(propertiesEnabledKeys) || user.isEnabledItem("prop.show_all_texts")) return propList;

		Map<String, String> filtered = new Hashtable<>();
		String[] enabledKeys = Tools.getTokens(propertiesEnabledKeys, ",");
		for (Entry<String, String> e : propList.entrySet())
		{
			if (isKeyVisibleToUser(user, enabledKeys, e.getKey()))
			{
				filtered.put(e.getKey(), e.getValue());
			}
		}

		return filtered;
	}

	public static boolean isKeyVisibleToUser(Identity user, String[] enabledKeys, String key)
	{
		if (Tools.isEmpty(key) || user.isEnabledItem("prop.show_all_texts")) return true;

		for (String testKey : enabledKeys)
		{
			if (key.startsWith(testKey))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Overi, ci pouzivatel moze upravit dany kluc
	 * @param user
	 * @param key
	 * @return
	 */
	public static boolean canEdit(Identity user, String key)
	{
		String propertiesEnabledKeys = Constants.getStringExecuteMacro("propertiesEnabledKeys");
		String[] enabledKeys = Tools.getTokens(propertiesEnabledKeys, ",");
		boolean canEdit = isKeyVisibleToUser(user, enabledKeys, key);
		return canEdit;
	}

	/**
	 * Riesi problem s HTML kodom v prekladovych textoch. Ak pouzivatel nema pravo na zobrazenie vsetkych prekladovych textov
	 * vykona escape specialnych znakov na entitu. Povolene su len vybrane HTML tagy a vybrane atributy
	 *
	 * priklad (povoleny): Toto je XSS pokus <a href="pokus">xss</strong>
	 * priklad (escapnuty): Toto je XSS pokus");alert(1)
	 * @param user
	 * @param lng
	 * @param key
	 * @param newValue
	 * @return
	 */
	public static String escapeUnsafeValue(Identity user, String lng, String key, String newValue) {
		//ma plne prava, vratime to co zadal
		if (user != null && user.isEnabledItem("prop.show_all_texts")) return newValue;
		if (Tools.isEmpty(newValue)) return newValue;

		String propAllowedTags = Constants.getString("propAllowedTags");
		if ("*".equals(propAllowedTags)) return newValue;

		String escapedValue = ResponseUtils.filter(newValue);
		//odporucane v penteste
		escapedValue = Tools.replace(escapedValue, "(", "&#x28;");
		escapedValue = Tools.replace(escapedValue, ")", "&#x29;");
		String escapedValueOriginal = escapedValue;

		if (Tools.isNotEmpty(propAllowedTags) && propAllowedTags.equals("-")==false) {
			//povol niektore znacky ako <p, <div, <a, <sub, <sup, <br />
			String[] allowedTags = Tools.getTokens(Constants.getString("propAllowedTags"), ",");
			for (String tag : allowedTags) {
				escapedValue = Tools.replace(escapedValue, "&lt;"+tag, "<"+tag);
				escapedValue = Tools.replace(escapedValue, "&lt;/"+tag, "</"+tag);
			}

			if (escapedValueOriginal.equals(escapedValue)==false) {
				//obsahuje povolenu znacku, povol koncovy zobak a koncove uvodzovky
				escapedValue = Tools.replace(escapedValue, "&gt;", ">");

				escapedValue = Tools.replace(escapedValue, "&quot; ", "\" ");
				escapedValue = Tools.replace(escapedValue, "&quot;>", "\">");
				escapedValue = Tools.replace(escapedValue, "&#39; ", "' ");
				escapedValue = Tools.replace(escapedValue, "&#39;>", "'>");

				//povol aj HTML atributy
				String[] allowedAttributes = Tools.getTokens(Constants.getString("propAllowedAttrs"), ",");
				for (String attribute : allowedAttributes) {
					escapedValue = Tools.replace(escapedValue, attribute+"=&quot;", attribute+"=\"");
					escapedValue = Tools.replace(escapedValue, attribute+"=&#39;", attribute+"='");
				}
			}
		}

		//pri dvojitom ulozeni vznikaju taketo haluze
		escapedValue = Tools.replace(escapedValue, "&amp;amp;", "&amp;");
		escapedValue = Tools.replace(escapedValue, "&amp;quot;", "&quot;");
		escapedValue = Tools.replace(escapedValue, "&amp;lt;", "&lt;");
		escapedValue = Tools.replace(escapedValue, "&amp;gt;", "&gt;");
		escapedValue = Tools.replace(escapedValue, "&amp;#", "&#");

		return escapedValue;
	}
}
