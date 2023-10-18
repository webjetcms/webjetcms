package sk.iway.iwcm.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.SearchAction;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.i18n.PropDB;
import sk.iway.iwcm.users.UsersDB;

/**
 * vyhladavanie v textovych klucoch podla kluca, alebo hodnoty
 */
public class PropertiesSearch implements Searchable
{
	public List<SearchResult> search(String text, HttpServletRequest request)
	{
		String[] searchallPropLanguages = Tools.getTokens(Constants.getString("searchallPropLanguages"), ",");

		Identity user = UsersDB.getCurrentUser(request);

		List<SearchResult> result = new ArrayList<>();

		for (String lng : searchallPropLanguages)
		{
			result.addAll(search(text, lng, user));
		}

		return result;
	}

	public List<SearchResult> search(String text, String lng, Identity user)
	{
		List<SearchResult> result = new ArrayList<>();

		Prop prop = Prop.getInstance(lng);
		Map<String, String> properties = PropDB.filterByPerms(user, prop.getRes(lng));

		if(properties!=null)
		{
			for (Map.Entry<String, String> property : properties.entrySet())
			{
				String cmpPropKey = DB.internationalToEnglish(property.getKey().toLowerCase());
				String cmpPropValue = DB.internationalToEnglish(property.getValue().toLowerCase());
				String cmpText = DB.internationalToEnglish(text.toLowerCase());

				if(cmpPropKey.contains(cmpText) || SearchAction.containsIgnoreHtml(cmpPropValue, cmpText))
				{
					SearchResult sr = new SearchResult();
					sr.setLabel(property.getKey());
					sr.setText(property.getValue());
					sr.setLink("/admin/v9/settings/translation-keys/#dt-open-editor=true&dt-filter-key=^" + property.getKey()+"$");
					sr.setType(this.getClass().getCanonicalName());
					result.add(sr);
				}
			}
		}

		return result;
	}

	@Override
	public boolean canUse(HttpServletRequest request)
	{
		Identity user = UsersDB.getCurrentUser(request);
		if (user == null || user.isDisabledItem("edit_text")) return false;
		return true;
	}

}
