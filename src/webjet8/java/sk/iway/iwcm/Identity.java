package sk.iway.iwcm;

import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;

import sk.iway.iwcm.system.Modules;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 * Trieda drzi info o prihlasenom pouzivatelovi
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.13 $
 *@created      $Date: 2004/03/09 14:07:31 $
 *@modified     $Date: 2004/03/09 14:07:31 $
 */

public class Identity extends UserDetails
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3752957232653314717L;

	private boolean valid = false;

	private boolean wasAdminSet = false;

	private Map<String, String> disabledItemsTable = null;

	public Identity()
	{
		super();
	}

	public Identity(UserDetails user)
	{
		try
		{
			PropertyUtils.copyProperties(this, user);
			//permissions treba precistit, aby sa korektne znova nasetovali
			if (disabledItemsTable != null) disabledItemsTable.clear();
			UsersDB.setDisabledItems(this);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	/**
	 *  Gets the loginName attribute of the Identity object
	 *
	 *@return    The loginName value
	 */
	public String getLoginName()
	{
		return login;
	}

	/**
	 *  Sets the loginName attribute of the Identity object
	 *
	 *@param  newLoginName  The new loginName value
	 */
	public void setLoginName(String newLoginName)
	{
		login = newLoginName;
	}

	/**
	 *  Sets the admin attribute of the Identity object
	 *
	 *@param  newAdmin  The new admin value
	 */
	@Override
	public void setAdmin(boolean newAdmin)
	{
		//admin atribut je mozne nastavit len raz
		//ochrana proti stupidnemu hacku...
		if (wasAdminSet && newAdmin == true)
		{
			return;
		}

		wasAdminSet = true;
		admin = newAdmin;
		if (admin == true)
		{
			authorized = true;
		}
	}

	/**
	 *  Sets the valid attribute of the Identity object
	 *
	 *@param  newValid  The new valid value
	 */
	public void setValid(boolean newValid)
	{
		valid = newValid;
	}

	/**
	 *  Gets the valid attribute of the Identity object
	 *
	 *@return    The valid value
	 */
	public boolean isValid()
	{
		return valid;
	}

	/**
	 *  Sets the authorized attribute of the Identity object
	 *
	 *@param  authorized  The new authorized value
	 */
	@Override
	public void setAuthorized(boolean authorized)
	{
		if (admin)
		{
			this.authorized = true;
		}
		else
		{
			this.authorized = authorized;
		}
	}

	/**
	 * Vrati true, ak pouzivatel nema zadane pravo
	 * @param name
	 * @return
	 */
	public boolean isDisabledItem(String name)
	{
		if (name.contains("&") || name.contains("|")) {
			return isEnabledItem(name)==false;
		}

		//allow welcome for all
		if ("welcome".equals(name)) return false;

		String fixedName = name;
		//fix starych nazvov
		if ("menuForms".equals(fixedName)) fixedName = "cmp_form";


		//Logger.println(this,"isDisabledItem("+name+")");
		if (disabledItemsTable == null)
		{
			return(false);
		}

		if (disabledItemsTable.get(fixedName)!=null)
		{
			return(true);
		}
		if (Modules.getInstance().isAvailable(fixedName)==false)
		{
			return(true);
		}
		return(false);
	}

	/**
	 * Vrati true, ak ma pouzivatel dane pravo, prava je mozne zadat aj vo formate:
	 * menuFbrowser|menuForms - pouzije sa ALEBO
	 * menuFbrowser&menuForms - pouzije sa A
	 * @param name
	 * @return
	 */
	public boolean isEnabledItem(String name)
	{
		String separator = "";
		if (name.contains("|")) separator = "|";
		else if (name.contains("&")) separator = "&";

		if (Tools.isEmpty(separator)) return !isDisabledItem(name);

		int counter = 0;
		String[] names = Tools.getTokens(name, separator);
		for (String n : names)
		{
			if (!isDisabledItem(n)) counter++;
		}

		if ("|".equals(separator))
		{
			return counter > 0;
		}
		else if ("&".equals(separator))
		{
			return counter == names.length;
		}
		return false;
	}

	public Map<String, String> getDisabledItemsTable()
	{
		//do NOT clone() this, callers may modify the content!
		return disabledItemsTable;
	}
	public void setDisabledItemsTable(Map<String, String> disabledItemsTable)
	{
		//do NOT clone() this, callers may modify the content!
		this.disabledItemsTable = disabledItemsTable;
	}
	public void addDisabledItem(String name)
	{
		if ("menuFbrowser".equals(name))
		{
			Logger.debug(Identity.class, "Som fbrowser");
		}

		if (disabledItemsTable == null)
		{
			disabledItemsTable = new Hashtable<>();
		}
		disabledItemsTable.put(name, name);
	}

	public void removeDisabledItem(String name)
	{
		if (disabledItemsTable != null)
		{
			disabledItemsTable.remove(name);
		}
	}

	/**
	 * JavaBean nevie v JSP vratit spravne FullName, pokial tam nie je toto
	 */
	@Override
	public String getFullName()
	{
		return(super.getFullName());
	}

	@Override
	public String getWritableFolders()
	{
		if (isDisabledItem("menuFbrowser") && Tools.isEmpty(writableFolders) && Constants.getBoolean("defaultDisableUpload")==false)
		{
			return "/images/*\n/files/*\n";
		}

		if (writableFolders==null) return null;
		return writableFolders.replace(' ', '\n');
	}
}
