package sk.iway.iwcm.search;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ModuleInfo;
import sk.iway.iwcm.system.Modules;
import sk.iway.iwcm.users.UsersDB;

/**
 * vyhladavanie vo WJ moduloch modla ich nazvu
 */
public class AplicationSearch implements Searchable
{
	public List<SearchResult> search(String text, HttpServletRequest request)
	{
		List<SearchResult> result = new ArrayList<>();

		String lng = PageLng.getUserLng(request);
		Prop prop = Prop.getInstance(lng);
		Identity user = UsersDB.getCurrentUser(request);

		List<ModuleInfo> modules = Modules.getInstance().getUserMenuItems(user);

		for(ModuleInfo module:modules)
		{
			String propText = prop.getText(module.getNameKey());

			String cmpModuleName = DB.internationalToEnglish(propText.toLowerCase());
			String cmpText = DB.internationalToEnglish(text.toLowerCase());

			if(Tools.isNotEmpty(propText) && cmpModuleName.contains(cmpText))
			{
				SearchResult sr = new SearchResult();
				sr.setLabel(propText);
				sr.setText("");
				sr.setLink(module.getLeftMenuLink());
				sr.setType(this.getClass().getCanonicalName());
				result.add(sr);
			}
		}

		return result;
	}
	@Override
	public boolean canUse(HttpServletRequest request)
	{
		// TODO: Doriesit prava ci sa to ma userovi vobec zobrazovat
		return true;
	}
}
