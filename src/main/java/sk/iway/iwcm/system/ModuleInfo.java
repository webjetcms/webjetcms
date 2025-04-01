package sk.iway.iwcm.system;

import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Tools;

/**
 *  ModuleInfo.java - info o module
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.15 $
 *@created      $Date: 2008/10/28 09:39:56 $
 *@modified     $Date: 2008/10/28 09:39:56 $
 */
public class ModuleInfo
{
	/**
	 * Kluc pre nazov modulu, napr. calendar.title
	 */
	private String nameKey;
	/**
	 * Kluc pre CSS styl a <iwcm:menu name="">
	 */
	private String itemKey;
	/**
	 * Cesta k suboru / adresaru, ktory sa kontroluje na pritomnost modulu
	 */
	private String path;
	/**
	 * Informacia o dostupnosti modulu
	 */
	private boolean available = false;
	/**
	 * Ak je modul nahrany z databazy (_modules_) je to true (doplni sa automaticky do menu)
	 */
	private boolean fromDatabase = false;
	/**
	 * Vyzadovany kluc v Constants, ak nie je null, tak kontroluje, ci
	 * dany kluc je nastaveny a ma dlzku > 1. Inak bude disabled
	 */
	private String requireConstantsKey = null;

	/**
	 * Ak je true, polozka sa zobrazuje v pravach pouzivatela
	 */
	private boolean userItem = true;
	/**
	 * identifikacia parent modulu pre submenus
	 */
	private String rootModule;

	//identifikacia skupiny v ktorej sa menu polozka zobrazi (moduly, ovladaci panel...)
	private String group;

	//ikona pre zobrazenie v menu
	private String menuIcon;

	private String leftMenuNameKey = null;
	private int menuOrder = 1000;
	private boolean showInLeftMenu = false;
	private String leftMenuLink = null;

	private boolean defaultDisabled = false;

	private boolean hideSubmenu;

	private String domainName = null;

	/**
	 * V akych verziach WJ je modul dostupny
	 */
	private String wjVersions = "";

	private List<ModuleInfo> submenus = null;

	private List<LabelValueDetails> components = null;

	/**
	 * Ak je nastavene true bude sa v editacii pouzivatela zobrazovat vola pre nastavenie skupin na dany modul
	 */
	private boolean showSubCategories = false;

	private boolean custom = false;

	/**
	 * @return Returns the available.
	 */
	public boolean isAvailable()
	{
		//Logger.debug(ModuleInfo.class, "testing " + itemKey +" wjversions=" +wjVersions+" vs "+Constants.getString("wjVersion")+" available="+available);
		if (wjVersions.indexOf(Constants.getString("wjVersion"))==-1)
		{
			available = false;
		}
		return available;
	}

	public ModuleInfo()
	{
		//empty constructor
	}

	/**
	 * Konstruktor
	 * @param nameKey - kluc do text.properties pre nazov modulu
	 * @param itemKey - kluc pre prava
	 * @param path - cesta k suboru, ktory sa kontroluje na existenciu
	 * @param requireConstantsKey - kontrola ci existuje Constants.getString(...)
	 * @param userItem - ak true, je mozne to volit v nastaveni pouzivatela
	 * @param showInLeftMenu - ak je true, zobrazuje sa v lavom menu pouzivatela
	 * @param leftMenuLink - linka, ktora sa pouziva pre lave menu
	 * @param wjWersions - na ake verzie WJ sa to pouziva B=basic, P=Pro, E=Enterprise
	 * @param menuOrder - poradie, v akom sa menu zobrazi
	 */
	public ModuleInfo(String nameKey, String itemKey, String path, String requireConstantsKey, boolean userItem, boolean showInLeftMenu, String leftMenuLink, String wjWersions, int menuOrder)
	{
		this.nameKey = nameKey;
		this.itemKey = itemKey;
		this.path = path;
		this.available = false;
		this.requireConstantsKey = requireConstantsKey;
		this.userItem = userItem;
		this.showInLeftMenu = showInLeftMenu;
		this.leftMenuLink = leftMenuLink;
		this.wjVersions = wjWersions;
		this.menuOrder = menuOrder;
	}

	public void addSubmenu(ModuleInfo subModule)
	{
		if (submenus == null) submenus = new ArrayList<>();
		subModule.setRootModule(this.itemKey);
		submenus.add(subModule);
	}

	public void addSubmenu(String nameKey, String path)
	{
		ModuleInfo sub = new ModuleInfo();
		sub.setNameKey(nameKey);
		sub.setPath(path);
		sub.setWjVersions(this.wjVersions);
		sub.setUserItem(false);

		addSubmenu(sub);
	}

	public void addComponent(LabelValueDetails component)
	{
		if (components == null) components = new ArrayList<>();
		components.add(component);
	}

	/**
	 * @param available The available to set.
	 */
	public void setAvailable(boolean available)
	{
		this.available = available;
	}
	/**
	 * @return Returns the fileCheck.
	 */
	public String getPath()
	{
		return path;
	}
	/**
	 * @param path
	 */
	public void setPath(String path)
	{
		this.path = path;
	}
	/**
	 * @return Returns the itemKey.
	 */
	public String getItemKey()
	{
		return itemKey;
	}
	/**
	 * @param itemKey The itemKey to set.
	 */
	public void setItemKey(String itemKey)
	{
		this.itemKey = itemKey;
	}
	/**
	 * @return Returns the nameKey.
	 */
	public String getNameKey()
	{
		return nameKey;
	}
	/**
	 * @param nameKey The nameKey to set.
	 */
	public void setNameKey(String nameKey)
	{
		this.nameKey = nameKey;
	}
	/**
	 * @return Returns the fromDatabase.
	 */
	public boolean isFromDatabase()
	{
		return fromDatabase;
	}
	/**
	 * @param fromDatabase The fromDatabase to set.
	 */
	public void setFromDatabase(boolean fromDatabase)
	{
		this.fromDatabase = fromDatabase;
	}
	/**
	 * @return Returns the requireConstantsKey.
	 */
	public String getRequireConstantsKey()
	{
		return requireConstantsKey;
	}
	/**
	 * @param requireConstantsKey The requireConstantsKey to set.
	 */
	public void setRequireConstantsKey(String requireConstantsKey)
	{
		this.requireConstantsKey = requireConstantsKey;
	}
	/**
	 * @return Returns the userItem.
	 */
	public boolean isUserItem()
	{
		return userItem;
	}
	/**
	 * @param userItem The userItem to set.
	 */
	public void setUserItem(boolean userItem)
	{
		this.userItem = userItem;
	}
	public String getWjVersions()
	{
		return wjVersions;
	}
	public void setWjVersions(String wjVersions)
	{
		this.wjVersions = wjVersions;
	}
	public String getLeftMenuNameKey()
	{
		if (leftMenuNameKey == null)
		{
			return(nameKey);
		}
		return leftMenuNameKey;
	}
	public void setLeftMenuNameKey(String leftMenuNameKey)
	{
		this.leftMenuNameKey = leftMenuNameKey;
	}
	public int getMenuOrder()
	{
		return menuOrder;
	}
	public ModuleInfo setMenuOrder(int menuOrder)
	{
		this.menuOrder = menuOrder;
		return this;
	}
	public boolean isShowInLeftMenu()
	{
		return showInLeftMenu;
	}
	public void setShowInLeftMenu(boolean showInLeftMenu)
	{
		this.showInLeftMenu = showInLeftMenu;
	}
	public String getLeftMenuLink()
	{
		if (leftMenuLink == null)
		{
			String link = path;
			if (link.contains("/admin/v9/dist/views/")) {
				link = Tools.replace(link, "/admin/v9/dist/views/", "/admin/v9/");
				link = Tools.replace(link, ".html", "/");
				link = Tools.replace(link, "/index/", "/");
			}
			return(link);
		}
		return leftMenuLink;
	}
	public void setLeftMenuLink(String leftMenuLink)
	{
		this.leftMenuLink = leftMenuLink;
	}
	/**
	 * @return Returns the defaultDisabled.
	 */
	public boolean isDefaultDisabled()
	{
		return defaultDisabled;
	}
	/**
	 * @param defaultDisabled The defaultDisabled to set.
	 */
	public void setDefaultDisabled(boolean defaultDisabled)
	{
		this.defaultDisabled = defaultDisabled;
	}

	public List<ModuleInfo> getSubmenus()
	{
		return submenus;
	}

	public List<LabelValueDetails> getComponents()
	{
		return components;
	}

	/**
	 * Vrati zoznam submenu podla prav daneho pouzivatela
	 * @param user
	 * @return
	 */
	public List<ModuleInfo> getSubmenus(Identity user)
	{
		return Modules.getSubmenus(this, user);
	}

	public void setRootModule(String rootModule)
	{
		this.rootModule = rootModule;
	}

	public String getRootModule()
	{
		return rootModule;
	}

	public boolean isShowSubCategories()
	{
		return showSubCategories;
	}

	public void setShowSubCategories(boolean showCategories)
	{
		this.showSubCategories = showCategories;
	}

	@Override
	public String toString() {
		return "ModuleInfo [nameKey=" + nameKey + ", itemKey=" + itemKey
				+ ", path=" + path + ", available=" + available
				+ ", fromDatabase=" + fromDatabase + ", requireConstantsKey="
				+ requireConstantsKey + ", userItem=" + userItem
				+ ", menuOrder=" + menuOrder + ", leftMenuLink=" + leftMenuLink
				+ "]";
	}

	public String getGroup()
	{
		return group;
	}

	public ModuleInfo setGroup(String group)
	{
		this.group = group;
		return this;
	}

	public String getMenuIcon()
	{
		return menuIcon;
	}

	public ModuleInfo setMenuIcon(String menuIcon)
	{
		this.menuIcon = menuIcon;
		return this;
	}

	public boolean getHideSubmenu()
	{
		return hideSubmenu;
	}

	public void setHideSubmenu(boolean hideSubmenu)
	{
		this.hideSubmenu = hideSubmenu;
	}

	public String getDomainName()
	{
		return domainName;
	}

	public void setDomainName(String domainName)
	{
		this.domainName = domainName;
	}

	public boolean isCustom() {
		return custom;
	}

	public void setCustom(boolean custom) {
		this.custom = custom;
	}
}
