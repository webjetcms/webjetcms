package sk.iway.iwcm.users;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.beanutils.BeanUtils;
import org.json.JSONObject;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.ModuleInfo;

/**
 *	SettingsAdminDB.java - vykonava pracu s databazou, konkretne s tabulkou user_settings_admin
 *
 *	@Title        webjet7
 *	@Company      Interway s.r.o. (www.interway.sk)
 *	@Copyright    Interway s.r.o. (c) 2001-2010
 *	@author       $Author: kmarton $
 *	@version      $Revision: 1.0 $
 * @created      Date: 02.07.2010 15:26:00
 * @modified     $Date: 2010/07/03 08:34:21 $
 */
public class SettingsAdminDB
{
	/**
	 * Vrati nastavenia pouzivatela z tabulky user_settings_admin v tvare nazov nastavenia ako key a SettingsAdminBean ako value
	 *
	 * @param userId	identifikator pouzivatela
	 *
	 * @return
	 */
	public static Map<String, SettingsAdminBean> getSettings(int userId)
	{
		Map<String, SettingsAdminBean> settings = new TreeMap<String, SettingsAdminBean>();

   	StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM user_settings_admin WHERE user_id = ? ");

      List<SettingsAdminBean> settingsList = new ComplexQuery().setSql(sql.toString()).setParams(userId).list(new Mapper<SettingsAdminBean>()
      {
      	@Override
         public SettingsAdminBean map(ResultSet rs) throws SQLException
         {
         	SettingsAdminBean setting = new SettingsAdminBean(rs.getInt("user_settings_admin_id"), rs.getInt("user_id"), DB.getDbString(rs, "skey"), DB.getDbString(rs, "value"));

         	return setting;
         }
      });

      for (SettingsAdminBean setting : settingsList)
		{
      	if (setting.getSkey() == null)
				continue;
			settings.put(setting.getSkey(), setting);
		}

		return settings;
	}

	/**
    * Funkcia, ktora vymaze vsetky admin nastavenia pouzivatela
    *
    * @param userId identifikator pouzivatela
    */
   public static void deleteAdminSettings(int userId)
   {
   	new SimpleQuery().execute("DELETE FROM user_settings_admin WHERE user_id = ?", userId);
   }

	/**
	 * Ulozi nastavenie pouzivatela do databazy
	 *
	 * @param userId		identifikator pouzivatela
	 * @param settings	admin nastavenia pouzivatela ulozene v mape
	 *
	 * @return
	 */
	public static boolean setSettings(int userId, Map<String, SettingsAdminBean> settings)
	{
		boolean saveOk = false;

		SettingsAdminDB.deleteAdminSettings(userId);

			for (Entry<String, SettingsAdminBean> entry : settings.entrySet())
			{
				SettingsAdminBean setting = entry.getValue();
				if (setting == null || "-".equals(entry.getKey()) || "__delete".equals(setting.getValue()))
					continue;
				setting.setSkey(entry.getKey());

				try
				{
					new SimpleQuery().execute("INSERT INTO user_settings_admin (user_id, skey, value) VALUES (?, ?, ?)", userId, DB.prepareString(setting.getSkey(), 255), DB.prepareString(setting.getValue(), 4000));
					saveOk = true;
				}
				catch (Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
					saveOk = false;
				}
			}

		return saveOk;
	}

	/**
	 * Prida polozku(fieldset) do sidebaru editora na poziciu position pre pouzivatela s identifikatorom userId
	 *
	 * @param fieldset	pole, ktore chceme pridat do sidebaru
	 * @param position	pozicia, na ktoru chceme pridat polozku
	 * @param userId		identifikator pouzivatela, pre ktoreho akciu vykonavame
	 *
	 * @return true, ak sa operacia podari, inak false
	 */
	public static boolean addToEditorSidebar(String fieldset, int position, int userId)
	{
		if (Tools.isEmpty(fieldset) || position < 0 || userId < 0)
			return false;

		Map<String, SettingsAdminBean> userAdminSettings = SettingsAdminDB.getSettings(userId);
		JSONObject sidebarJSON = new JSONObject();

		try
		{
			if (userAdminSettings.get("sidebar_items") != null)
				sidebarJSON = new JSONObject(userAdminSettings.get("sidebar_items").getValue());

			sidebarJSON.put(fieldset, position);
			userAdminSettings.put("sidebar_items", new SettingsAdminBean(userId, "sidebar_items", sidebarJSON.toString()));
		}
		catch(Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return SettingsAdminDB.setSettings(userId, userAdminSettings);	//dolezite ulozit vsetky zmeny v nastaveniach spat do DB
	}

	/**
	 * Odstrani polozku(fieldset) zo sidebaru editora pre pouzivatela s identifikatorom userId
	 *
	 * @param fieldset	pole, ktore chceme pridat do sidebaru
	 * @param userId		identifikator pouzivatela, pre ktoreho akciu vykonavame
	 *
	 * @return true, ak sa operacia podari, inak false
	 */
	public static boolean removeFromEditorSidebar(String fieldset, int userId)
	{
		if (Tools.isEmpty(fieldset) || userId < 0)
			return false;

		Map<String, SettingsAdminBean> userAdminSettings = SettingsAdminDB.getSettings(userId);
		JSONObject sidebarJSON;
		int removedPosition = 0;

		try
		{
			if (userAdminSettings.get("sidebar_items") != null)
			{
				sidebarJSON = new JSONObject(userAdminSettings.get("sidebar_items").getValue());
				removedPosition = sidebarJSON.getInt(fieldset);
				sidebarJSON.remove(fieldset);

				//uprava ostatnych pozicii podla odstranenej
				if (sidebarJSON.length() > 0)
				{
   				for(String key : JSONObject.getNames(sidebarJSON))
   				{
   					if(sidebarJSON.getInt(key) > removedPosition)
   						sidebarJSON.put(key, (sidebarJSON.getInt(key) -1));
   				}
				}

				userAdminSettings.put("sidebar_items", new SettingsAdminBean(userId, "sidebar_items", sidebarJSON.toString()));
			}
			else
				return false;

		}
		catch(Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return SettingsAdminDB.setSettings(userId, userAdminSettings);	//dolezite ulozit vsetky zmeny v nastaveniach spat do DB
	}

	/**
	 * Vrati vsetky polozky, ktore sa nachadzaju v sidebare pre pouzivatela s identifikatorom userId
	 *
	 * @param userId	identifikator pouzivatela
	 *
	 * @return Polozky v sidebare v notacii JSON, kde kluc je nazov polozky a hodnota je jej pozicia
	 */
	public static String getItemsEditorSidebar(int userId)
	{
		if (userId < 0)
			return null;

		Map<String, SettingsAdminBean> userAdminSettings = SettingsAdminDB.getSettings(userId);
		JSONObject sidebarJSON;

		try
		{
			if (userAdminSettings.get("sidebar_items") != null)
			{
				sidebarJSON = new JSONObject(userAdminSettings.get("sidebar_items").getValue());
				return sidebarJSON.toString();
			}
			else
				return null;

		}
		catch(Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			return null;
		}
	}

	/**
	 * Metoda vrati povolene kategorie pre jednotlive moduly
	 * @param userId userId
	 * @param moduleItems zoznamModulov pre useredit
	 * @return
	 */
	public static Map<String, SettingsAdminBean> getModuleCategoriesSetting(int userId, List<ModuleInfo> moduleItems)
	{
		Map<String, SettingsAdminBean> userAdminSettings = getSettings(userId);
		Map<String,SettingsAdminBean> categoriesSettings = new HashMap<String, SettingsAdminBean>();
		for(ModuleInfo mi : moduleItems)
		{
			if(userAdminSettings.get(mi.getNameKey())!= null)
			{
				categoriesSettings.put(mi.getNameKey(), userAdminSettings.get(mi.getNameKey()));
			}
		}

		return categoriesSettings;

	}

	/**
	 * Skontroluje ci ma user pravo pristupovat k danej kategorii v module
	 * @param group kategoria
	 * @param user user
	 * @param cmpName modul
	 * @return
	 */
	public static boolean canUserAccess(String group, Identity user, String cmpName)
	{
		Map<String,SettingsAdminBean> settings = SettingsAdminDB.getSettings(user.getUserId());
		if(settings.get(cmpName) == null){
			return true;
		}
		else{
			return getAllowedCategories(settings,cmpName ).contains(group);
		}
	}

	/**
	 * Ak existuje nastavenie pre dany cmpName tak z neho spravi zoznam kategorii
	 * @param settings
	 * @param cmpName
	 * @return zoznam otrimovanych kategorii alebo null ak nastavenie v settingAdmin neexistuje
	 */
	public static List<String> getAllowedCategories(Map<String, SettingsAdminBean> settings, String cmpName)
	{
		List<String> allowed = null;
		if(settings.get(cmpName) == null)
			return null;
		else
		{
			allowed = new ArrayList<String>();
			SettingsAdminBean setting = settings.get(cmpName);
			String[] categoriesArray = setting.getValue().trim().split(",");
			for(String cat : categoriesArray)
			{
				allowed.add(cat.trim());
			}
		}
		return allowed;
	}


	/**
	 * Meotda vyfiltruje list beanov podla povoloenych kategorii na zaklade propertyName. Ak je zoznam povolenych kategorii prazdny alebo null
	 * nefiltruje sa.
	 * @param <T> Cokolvek co sa da filtrovat podla nejakej skupiny
	 * @param beans List objektov na filtrovanie
	 * @param propertyName meno property so skupinou, kategoriou alebo cimkolvek podla coho sa ma filtrovat
	 * @param allowedCategories zoznam povolenych kategorii
	 * @return
	 */
	public static <T> List<T> filterBeansByUserAllowedCategories(List<T> beans,String propertyName, List<String> allowedCategories)
	{
		List<T> result = null;
		if(allowedCategories != null)
		{
			if(allowedCategories.size() > 0)
			{
				try
				{
					result = new ArrayList<T>(beans.size());
					for (T bean : beans)
					{
						if (allowedCategories.contains(BeanUtils.getProperty(bean, propertyName).trim()))
						{
							result.add(bean);
						}
					}
				}
				catch (NoSuchMethodException e)
				{
					Logger.debug(SettingsAdminDB.class, "Unable to filter beans: " + e.getMessage());
					result = beans;
				}
				catch (IllegalAccessException e)
				{
					Logger.debug(SettingsAdminDB.class, "Unable to filter beans: " + e.getMessage());
					result = beans;
				}
				catch (InvocationTargetException e)
				{
					Logger.debug(SettingsAdminDB.class, "Unable to filter beans: " + e.getMessage());
					result = beans;
				}

			}
		}
		else
		{
			result = beans;
		}
		return result;

	}

	/**
	 *
	 * @param <T> Cokolvek co sa da filtrovat podla nejakej skupiny
	 * @param beans List objektov na filtrovanie
	 * @param propertyName meno property so skupinou, kategoriou alebo cimkolvek podla coho sa ma filtrovat
	 * @param userId id usera
	 * @param cmpName meno prava pre komponentu
	 * @return vyfiltrovany list objektov, ak nie su nastavene ziadne obmedzenia na kategorie alebo su prazdne vrati nevyfiltrovany zoznam
	 */
	public static <T> List<T> filterBeansByUserAllowedCategories(List<T> beans,String propertyName, int userId, String cmpName)
	{
		List<String> allowedCategories = getAllowedCategories(getSettings(userId), cmpName);
		return filterBeansByUserAllowedCategories(beans, propertyName, allowedCategories);
	}

	/**
	 *
	 * @param <T> Cokolvek co sa da filtrovat podla nejakej skupiny
	 * @param beans List objektov na filtrovanie
	 * @param propertyName meno property so skupinou, kategoriou alebo cimkolvek podla coho sa ma filtrovat
	 * @param user user
	 * @param cmpName meno prava pre komponentu
	 * @return vyfiltrovany list objektov, ak nie su nastavene ziadne obmedzenia na kategorie alebo su prazdne vrati nevyfiltrovany zoznam
	 */
	public static <T> List<T> filterBeansByUserAllowedCategories(List<T> beans,String propertyName, UserDetails user, String cmpName)
	{
		List<String> allowedCategories = getAllowedCategories(user.getAdminSettings(), cmpName);
		return filterBeansByUserAllowedCategories(beans, propertyName, allowedCategories);
	}

	/**
	 * zmeni existujuce, alebo vytvori nove nastavenie. Ak userSettingsId>0 tak zmeni podla userSettingsId, inak podla userId a sKey
	 *
	 * @param setting
	 * @return
	 */
	public static boolean setSetting(SettingsAdminBean setting)
	{
		boolean saveOk = false;
		if(setting!=null && (setting.getSkey()!=null || setting.getUserSettingsId()>0))
		{
			//ak nemame zadane userSettingsId, tak ho zistime z DB
			SettingsAdminBean oldSetting = null;
			if(setting.getUserSettingsId()>0)
				oldSetting = setting;
			else
				oldSetting = getSetting(setting.getUserSettingsId(), setting.getUserId(), setting.getSkey());
			try
			{
				//ak setting v DB neexistuje
				if(oldSetting!=null && oldSetting.getUserSettingsId()>0)
					new SimpleQuery().execute("UPDATE user_settings_admin SET user_id=?, skey=?, value=? WHERE user_settings_admin_id=?", setting.getUserId(), DB.prepareString(setting.getSkey(), 255), DB.prepareString(setting.getValue(), 4000), oldSetting.getUserSettingsId());
				else
					new SimpleQuery().execute("INSERT INTO user_settings_admin (user_id, skey, value) VALUES (?, ?, ?)", setting.getUserId(), DB.prepareString(setting.getSkey(), 255), DB.prepareString(setting.getValue(), 4000));
				saveOk = true;
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
				saveOk = false;
			}
		}
		return saveOk;
	}

	/**
	 * vymaze nastavenie z DB. Ak userSettingsId>0 tak vymaze podla userSettingsId, inak podla userId a sKey
	 *
	 * @param setting
	 * @return
	 */
	public static boolean deleteSetting(SettingsAdminBean setting)
	{
		boolean delOk = false;
		if(setting!=null && (setting.getSkey()!=null || setting.getUserSettingsId()>0))
		{
			try
			{
				if(setting.getUserSettingsId()>0)
					new SimpleQuery().execute("DELETE FROM user_settings_admin WHERE user_settings_admin_id=?", setting.getUserSettingsId());
				else
					new SimpleQuery().execute("DELETE FROM user_settings_admin WHERE user_id=? AND skey=?", setting.getUserId(), DB.prepareString(setting.getSkey(), 255));
				delOk = true;
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
				delOk = false;
			}
		}
		return delOk;
	}

	/**
	 * vrati SettingsAdminBean z DB. Ak userSettingsId>0 tak najde podla userSettingsId, inak podla userId a sKey
	 *
	 * @param userSettingsId
	 * @param userId
	 * @param sKey
	 * @return
	 */
	public static SettingsAdminBean getSetting(int userSettingsId, int userId, String sKey)
	{
		List<SettingsAdminBean> resultList = null;

		try
		{
			StringBuilder sql = new StringBuilder();
			if(userSettingsId>0)
			{
				sql.append("SELECT * FROM user_settings_admin WHERE user_settings_admin_id = ? ");
				resultList = new ComplexQuery().setSql(sql.toString()).setParams(userSettingsId).list(new Mapper<SettingsAdminBean>()
			      {
			      	@Override
			         public SettingsAdminBean map(ResultSet rs) throws SQLException
			         {
			         	SettingsAdminBean setting = new SettingsAdminBean(rs.getInt("user_settings_admin_id"), rs.getInt("user_id"), DB.getDbString(rs, "skey"), DB.getDbString(rs, "value"));
			         	return setting;
			         }
			      });
			}
			else
			{
				sql.append("SELECT * FROM user_settings_admin WHERE user_id = ? AND skey=?");
				resultList = new ComplexQuery().setSql(sql.toString()).setParams(userId, sKey).list(new Mapper<SettingsAdminBean>()
			      {
			      	@Override
			         public SettingsAdminBean map(ResultSet rs) throws SQLException
			         {
			         	SettingsAdminBean setting = new SettingsAdminBean(rs.getInt("user_settings_admin_id"), rs.getInt("user_id"), DB.getDbString(rs, "skey"), DB.getDbString(rs, "value"));
			         	return setting;
			         }
			      });
			}

		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		if(resultList!=null && resultList.size()==1)
			return resultList.get(0);
		else
			return null;
	}

}