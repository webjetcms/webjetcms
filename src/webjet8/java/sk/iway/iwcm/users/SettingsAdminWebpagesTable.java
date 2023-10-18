package sk.iway.iwcm.users;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;

/**
 *  SettingsAdminWebpagesTable.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author       $Author: jeeff suchy $
 *@version      $Revision: 1.3 $
 *@created      Date: Sep 29, 2015 3:56:40 PM
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class SettingsAdminWebpagesTable
{
	protected static int userId;
	protected static UserDetails user;
	protected static Map<String, SettingsAdminBean> userAdminSettings;

	protected SettingsAdminWebpagesTable() {
		//utility class
	}

	public static List<String> getAllowedProperties()
	{
		return new ArrayList<>(
					Arrays.asList(
								"showPublishing",
								"showPerex",
								"showRights",
								"showTemplate",
								"showUrl",
								"showOptionals",
								"showFixedTableHeader"
								)
					);
	}

	protected static void setUserData(HttpSession session)
	{
		userId =  ((Identity)session.getAttribute(Constants.USER_KEY)).getUserId();
		user = UsersDB.getUser(userId);
		userAdminSettings = user.getAdminSettings();
	}

	public static void saveProperty(HttpSession session, String property, String propertyValue)
	{
		setUserData(session);

		propertyValue = Boolean.toString(Integer.parseInt(propertyValue) != 0);

		if(getAllowedProperties().contains(property))
		{
			userAdminSettings.put(property, new SettingsAdminBean(userId, property, propertyValue));
			SettingsAdminDB.setSettings(userId, userAdminSettings);
		}
	}

	public static boolean loadProperty(HttpSession session, String propertyName)
	{
		setUserData(session);
		SettingsAdminBean prop = userAdminSettings.get(propertyName);
		if(prop != null)
		{
			return Tools.getBooleanValue(prop.getValue(), false);
		}
		else
		{
			return false;
		}

	}
}
