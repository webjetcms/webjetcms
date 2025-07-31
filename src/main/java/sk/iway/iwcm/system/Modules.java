package sk.iway.iwcm.system;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.IwayProperties;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;

/**
 *  Modules.java - konfiguracia a povolenie modulov (globalne pre cely server)
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.69 $
 *@created      $Date: 2010/02/09 08:26:37 $
 *@modified     $Date: 2010/02/09 08:26:37 $
 */
public class Modules
{
	public static final String CONTEXT_KEY = "sk.iway.iwcm.system.Modules";

	private static Map<String, String> modVersions;

	private List<ModuleInfo> modules; //NOSONAR

	static {
		modVersions = new Hashtable<>();
	}

	public static Modules getInstance()
	{
		return(getInstance(null, false));
	}

	public static Modules getInstance(String enableList, boolean forceRefresh)
	{
		if (forceRefresh == false)
		{
			if (Constants.getServletContext().getAttribute(CONTEXT_KEY) != null)
			{
				Modules modules = (Modules)Constants.getServletContext().getAttribute(CONTEXT_KEY);

				return (modules);
			}
		}
		return (new Modules(enableList));
	}

	/**
	 * Skratka pismen je:
	 * B = basic
	 * P = professional
	 * E = enterprise
	 * C = cestovka
	 * I = intranet
	 * D = dmail
	 * M = cloud (multi)
	 * O = open source
	 * @param enableList
	 */
	private Modules(String enableList)
	{
		modVersions.put("cmp__common", "BPECIDMO;c49");
		modVersions.put("cmp_webjet9", "BPECIDMO;c47");

		//apps
		modVersions.put("cmp_app-disqus", "BPECIDMO;d01");
		modVersions.put("cmp_app-facebook_comments", "BPECIDMO;d02");
		modVersions.put("cmp_app-facebook_like", "BPECIDMO;d03");
		modVersions.put("cmp_app-facebook_like_box", "BPECIDMO;d04");
		modVersions.put("cmp_app-docsembed", "BPECIDMO;d05");
		modVersions.put("cmp_app-htmlembed", "BPECIDMO;d06");
		modVersions.put("cmp_app-smartsupp", "BPECIDMO;d07");
		modVersions.put("cmp_app-social_icon", "BPECIDMO;d08");
		modVersions.put("cmp_app-weather", "BPECIDM;d09");
		modVersions.put("cmp_app-date", "BPECIDMO;d10");
		modVersions.put("cmp_app-vyhladavanie", "BPECIDMO;d11");
		modVersions.put("cmp_app-slit_slider", "BPECIDM;d12");
		modVersions.put("cmp_app-impress_slideshow", "BPECIDM;d13");
		modVersions.put("cmp_app-testimonials", "BPECIDM;d14");
		modVersions.put("cmp_app-cookiebar", "BPECIDMO;d15");

		//standard moduly
		modVersions.put("cmp_abtesting", "E;7c9"); //WebJET AB Testovanie
		modVersions.put("cmp_adminlog", "BPEICDMO;c5a"); //WebJET adminlog - 1990
		modVersions.put("cmp_adresar", "I;ec1"); //WebJET adresar - 1990
		modVersions.put("cmp_attributes", "E;1c9"); //WebJET Attributes - 9990
		modVersions.put("cmp_auction", "EI;1b2"); //WebJET Aukcie - 1990
		modVersions.put("cmp_banner", "EC;26d"); //WebJET Banner Manager - 5900
		modVersions.put("cmp_basket", "ECMI;943"); //WebJET Shopping Basket - 3900
		modVersions.put("cmp_bazar", "PEI;bf5"); //WebJET Bazzar - 3900
		modVersions.put("cmp_blog", "EIM;3cd"); //WebJET Blog - 4900
		modVersions.put("cmp_calendar", "PECIM;3a1"); //WebJET Event Manager - 6990
		modVersions.put("cmp_carousel_slider", "PECIM;15a"); //WebJET Event Manager - 6990
		modVersions.put("cmp_catalog", "PE;427"); //WebJET Catalog - 9900
		modVersions.put("cmp_cestovka", "C;d1c"); //WebJET Cestovna Agentura - 9900
		modVersions.put("cmp_clone_structure", "PEI;9bc");
		modVersions.put("cmp_content-block", "PEI;361");
		modVersions.put("cmp_counter", "PEIM;671"); //WebJET Counter - 990 Sk
		modVersions.put("cmp_crontab", "ECDIM;a01"); //WebJET Crontab - 4900
		modVersions.put("cmp_crypto", "PECDIM;2c9"); //WebJET Crypto - 499 eur
		modVersions.put("cmp_data_deleting", "BPECDIM;775"); //WebJET Data Deleting
		modVersions.put("cmp_date", "BPECDIMO;775"); //WebJET Date - 900
		modVersions.put("cmp_db_browser", "EI;f1e"); //WebJET DB Browser - 6900
		modVersions.put("cmp_dictionary", "EI;c4a"); //WebJET Dictionary - 1900
		modVersions.put("cmp_dmail", "EDI;617"); //WebJET Distribution List - 14990
		modVersions.put("cmp_docman", "I;26f"); //WebJET Document Management - 4900
		modVersions.put("cmp_elfinder", "E;c2e");
		modVersions.put("cmp_emoticon", "BPECIDMO;777");
		modVersions.put("cmp_enumerations", "PECID;3f8");
		modVersions.put("cmp_export", "PEIM;4f6");
		modVersions.put("cmp_fileatr", "EI;823");
		modVersions.put("cmp_file_archiv", "EI;dc1"); //archiv suborov
		modVersions.put("cmp_form", "PECIM;9ac"); //WebJET Interactive Form - 1990 Sk
		modVersions.put("cmp_formsimple", "PECIM;6e3"); //WebJET FormSimple
		modVersions.put("cmp_forum", "PECIM;a51"); //WebJET Forum & Message Board - 3990
		modVersions.put("cmp_gallery", "PECIDMO;b36"); //WebJET Photo Gallery - 4990
		modVersions.put("cmp_gdpr", "BPEIDM;7c2");
		modVersions.put("cmp_graph", "PEI;16d"); //WebJET Graph - 4990
        modVersions.put("cmp_grideditor", "BPECIDMO;27f"); //WebJET grideditor
		modVersions.put("cmp_htmlbox", "BPECIDMO;ccc"); //WebJET HTMLBox - v cene
		modVersions.put("cmp_helpdesk", "I;9b5");
		modVersions.put("cmp_intranet", "I;9b6");
		modVersions.put("cmp_inventory", "I;a86");
        modVersions.put("cmp_inzercia", "I;471");
		modVersions.put("cmp_kanban", "I;9ba");
		modVersions.put("cmp_chat", "ECI;5a0"); //WebJET Chat - 3990
		modVersions.put("cmp_imagefont", "PED;27c"); //WebJET Imagefont - 1900
		modVersions.put("cmp_import_web_pages", "PEDI;53c"); //WebJET Import Web Pages - 1900
		modVersions.put("cmp_insert_script", "PEIM;6a2");
		modVersions.put("cmp_inquiry", "PECIMO;d97"); //WebJET Inquiry - 2900
		modVersions.put("cmp_inquirysimple", "BPECIMO;2e9"); //WebJET InquirySimple
		modVersions.put("cmp_json_editor", "BPECIDMO;7a2");
		modVersions.put("cmp_kurzy", "PECI;e6c"); //WebJET Exchange Rates - 450
		modVersions.put("cmp_lms", "I;c85");
		modVersions.put("cmp_mail", "I;c81");
		modVersions.put("cmp_mcalendar", "I;c82");
		modVersions.put("cmp_map", "BPECIM;ac4"); //WebJET Map (Google) - 0
		modVersions.put("cmp_magzilla", "I;2fe"); //WebJET Help Desk - 4900
		modVersions.put("cmp_media", "PECIDM;a82"); //WebJET Media - 1990
		modVersions.put("cmp_menu", "BPECIDMO;f1c"); //WebJET News & Press Releases - 1990
		modVersions.put("cmp_messages", "BPECID;4e2"); //WebJET Messages
		modVersions.put("cmp_news", "BPECIDMO;f5c"); //WebJET News & Press Releases - 1990
		modVersions.put("cmp_organization", "I;ae3");
		modVersions.put("cmp_page_reactions", "EI;af2");
        modVersions.put("cmp_page_update_info", "EI;c52"); //-------WebJET Page Update Info - 1990
		modVersions.put("cmp_pocasie", "PECI;7c8"); //WebJET pocasie - 1990
		modVersions.put("cmp_popup", "BPECI;111"); //WebJET Popup - v cene
		modVersions.put("cmp_profesia", "PEI;d4d"); //WebJET Profesia - 1990
		modVersions.put("cmp_proxy", "ECI;52c"); //WebJET Proxy - 2990
		modVersions.put("cmp_projects_microsite", "I;46f");
		modVersions.put("cmp_qa", "PECIMO;2c8");  //WebJET Questions & Answers - 1990
		modVersions.put("cmp_quiz", "EI;9c6");
		modVersions.put("cmp_rating", "ECI;3dd"); //WebJET Rating - 900
		modVersions.put("cmp_redirects", "BPECIDMO;7c1"); //WebJET Redirects
		modVersions.put("cmp_related-pages", "ECI;4c1"); //WebJET Related Pages - 1900
		modVersions.put("cmp_reservation", "EIM;fa2"); //WebJET Reservations - 2900
		modVersions.put("cmp_response-header", "PEIM;74c");
		modVersions.put("cmp_restaurant_menu", "PEIM;6c8");
		modVersions.put("cmp_rss", "PECIMO;5f0"); //WebJET RSS Export
		modVersions.put("cmp_search", "PECIM;60c"); //WebJET Search - 1990 Sk
		modVersions.put("cmp_send_link", "PECI;842"); //WebJET Send Link - 1490 Sk
		modVersions.put("cmp_seo", "E;eb1"); //WebJET SEO - 2490 Sk
		modVersions.put("cmp_server_monitoring", "PECIM;aea"); //WebJET Send Link - 1490 Sk
		modVersions.put("cmp_sharepoint", "I;c84");
		modVersions.put("cmp_sharing_icons", "PECMO;15c"); //WebJET Sharing Icons
		modVersions.put("cmp_site_browser", "EI;92f"); //WebJET Site Browser - 900
		modVersions.put("cmp_sitemap", "BPECIM;701");	 //WebJET Sitemap	- 1990 Sk
		modVersions.put("cmp_slider", "PEIM;8a6");
		modVersions.put("cmp_sms", "EI;8cf"); //WebJET SMS Sender - 2900
		modVersions.put("cmp_stat", "PECIM;f49"); //WebJET Statistics - 9990 Sk
		modVersions.put("cmp_structuremirroring", "E;a42"); //WebJET structuremirroring
		modVersions.put("cmp_sync", "PEI;ce2"); //WebJET SyncDir - 2990
		modVersions.put("cmp_tips", "PECI;9d5"); //WebJET Tips - 900
		modVersions.put("cmp_tooltip", "E;371"); //WebJET Tooltip - 900
		modVersions.put("cmp_user", "PECDIO;ac6");
		modVersions.put("cmp_uschovna", "I;557");
		modVersions.put("cmp_video", "PEIMO;821"); //Video
		modVersions.put("cmp_wiki", "EI;1c4"); //Wiki
		modVersions.put("cmp_xml", "PEI;bf0");
		modVersions.put("cmp_captcha", "PECI;a3c"); //zoznam slov pre captchu

		//modVersions.put("menuUsers", "PECDIO;f20"); //WebJET Multi-User Access - 2990 Sk, WebJET Protected Section-4990
		modVersions.put("menuSync", "PEI;f36"); //WebJET Synchronization - 12900
		modVersions.put("imageEditor", "PECDIMO;f47"); //WebJET Image Editor - 4900

		modVersions.put("fileIndexer", "EI;c21"); //WebJET Extended Search - 14900

		modVersions.put("multiDomain", "EM;f17"); //WebJET Multi Domain - 25% ceny licencie

		//standardne moduly
		modVersions.put("menu.users", "PECDIOM;c01"); //standardny modul Pouzivatelia
		modVersions.put("menu.fbrowser", "BPECDIM;c02"); //standardny modul Subory
		modVersions.put("menu.templates", "BPECDIOM;c03"); //standardny modul Sablony


		//WebJET Active Directory Access - 24.900

		try
		{
			if (Tools.isNotEmpty(enableList))
			{
				String key;
				String modName;
				String modPerms;
				int failsafe = 0;
				while (enableList.length() > 2 && failsafe < 50)
				{
					key = enableList.substring(0, 3);
					enableList = enableList.substring(3);
					failsafe++;

					//preiteruj moduly a povol
					for (Map.Entry<String, String> entry : modVersions.entrySet())
					{
						modName = entry.getKey();
						modPerms = entry.getValue();

						//Logger.println(this,"--> testujem: " + key + " " + modName + " " + modPerms);

						if (modPerms!=null && modPerms.endsWith(";"+key))
						{
							//povol modul
							//Logger.println(this,"--> povolujem: " + modName);
							modVersions.put(modName, "BPEDCIDMO;"+key);
							break;
						}
					}
				}
			}
		}
		catch (Exception ex1)
		{
			Logger.error(Modules.class, ex1);
		}

		modules = new ArrayList<>();

		try
		{
			//natiahni komponenty
			IwcmFile[] components = (new IwcmFile(Tools.getRealPath("/components"))).listFiles();
			IwcmFile[] apps = (new IwcmFile(Tools.getRealPath("/apps"))).listFiles();

			List<IwcmFile> files = new ArrayList<>();
			files.addAll(Arrays.asList(components));
			if (apps != null && apps.length>0) files.addAll(Arrays.asList(apps));

			ModuleInfo m;
			String wjPerms;
			IwcmFile propFile;
			IwayProperties prop;
			String sTmp;
			//Logger.debug(Modules.class, "components.size="+size);
			for (IwcmFile file : files)
			{
				String baseDir = "/components/";
				String propertiesPrefix = "components.";
				if (file.getAbsolutePath().contains(File.separatorChar+"apps"+File.separatorChar)) {
					baseDir = "/apps/";
					propertiesPrefix = "apps.";
				}

				//Logger.debug(Modules.class, "components["+i+"]="+components[i].getAbsolutePath()+" isDir="+components[i].isDirectory());
				if (file.isDirectory())
				{
					for (int j=0; j<30; j++)
					{
		         		m = new ModuleInfo();
						m.setNameKey(propertiesPrefix+file.getName()+".title");
						m.setItemKey("cmp_"+file.getName());
						m.setPath(baseDir+file.getName());
						m.setUserItem(false);
						wjPerms = modVersions.get(m.getItemKey());
						if (wjPerms == null) wjPerms = "BPECIDMO";
						m.setWjVersions(wjPerms);

						String fileName = "modinfo.properties";
						if (j>0) fileName = "modinfo"+j+".properties";

						propFile = new IwcmFile(Tools.getRealPath(baseDir+file.getName()+"/"+fileName));
						if (propFile.exists())
						{
							//Logger.debug(Modules.class, "Loading: "+baseDir+file.getName()+"/"+fileName);

							//nahraj prop subor a nastav hodnoty
							prop = new IwayProperties("iso-8859-1");
							prop.load(propFile);

							//Logger.debug(Modules.class, "size: "+prop.entrySet().size());
							if (prop.entrySet().size() < 2)
							{
								//nepodari sa resetnut stream, skusme ako utf-8
								prop = new IwayProperties("utf-8");
								prop.load(propFile);
							}

							sTmp = prop.getProperty("userItem");
							if ("true".equals(sTmp))
							{
								m.setUserItem(true);
							}
							sTmp = prop.getProperty("leftMenuLink");
							if (Tools.isNotEmpty(sTmp))
							{
								m.setLeftMenuLink(sTmp.trim());
								m.setShowInLeftMenu(true);
							}
							sTmp = prop.getProperty("leftMenuNameKey");
							if (Tools.isNotEmpty(sTmp))
							{
								m.setLeftMenuNameKey(sTmp.trim());
							}
							sTmp = prop.getProperty("itemKey");
							if (Tools.isNotEmpty(sTmp))
							{
								m.setItemKey(sTmp.trim());
							}
							sTmp = prop.getProperty("menuOrder");
							if (Tools.isNotEmpty(sTmp))
							{
								m.setMenuOrder(Tools.getIntValue(sTmp, 0));
							}
							sTmp = prop.getProperty("defaultDisabled");
							if (sTmp!=null && "true".equals(sTmp.trim()))
							{
								m.setDefaultDisabled(true);
							}
							sTmp = prop.getProperty("requireConstantsKey");
							if (Tools.isNotEmpty(sTmp))
							{
								m.setRequireConstantsKey(sTmp.trim());
							}
							sTmp = prop.getProperty("showSubcategories");
							if (sTmp!=null && "true".equals(sTmp.trim()))
							{
								m.setShowSubCategories(true);
							}
							sTmp = prop.getProperty("hideSubmenu");
							if (sTmp!=null && "true".equals(sTmp.trim()))
							{
								m.setHideSubmenu(true);
							}

							sTmp = prop.getProperty("installName");
							//installName moze byt zapisane ako vubweb,vubintra
							if (Tools.isNotEmpty(sTmp) && (","+sTmp.trim()+",").indexOf(","+Constants.getInstallName()+",")==-1)
							{
								//tento modul je custom, preskakujem
								continue;
							}

							sTmp = prop.getProperty("isCustom");
							if (Tools.isEmpty(sTmp)) sTmp = prop.getProperty("custom");
							if ("true".equals(sTmp))
							{
								m.setCustom(true);
							}

							sTmp = prop.getProperty("group");
							if (Tools.isEmpty(sTmp)) sTmp = "components";
							m.setGroup(sTmp.trim());

							sTmp = prop.getProperty("icon");
							if (Tools.isEmpty(sTmp)) sTmp = "";
							m.setMenuIcon(sTmp.trim());

							sTmp = prop.getProperty("domainName");
							if (Tools.isEmpty(sTmp)) sTmp = "";
							m.setDomainName(sTmp.trim());

							//nacitaj submenus
							for (int s=1; s<25; s++)
							{
								String nameKey = prop.getProperty("leftSubmenu"+s+"NameKey");
								String link = prop.getProperty("leftSubmenu"+s+"Link");
								if (Tools.isNotEmpty(nameKey) && Tools.isNotEmpty(link))
								{
									ModuleInfo subModule = new ModuleInfo();

									subModule.setPath(baseDir+file.getName());
									subModule.setWjVersions(wjPerms);

									subModule.setNameKey(nameKey);
									subModule.setLeftMenuLink(link);

									sTmp = prop.getProperty("leftSubmenu"+s+"UserItem");
									if (sTmp!=null && "true".equals(sTmp.trim())) subModule.setUserItem(true);
									else subModule.setUserItem(false);

									sTmp = prop.getProperty("leftSubmenu"+s+"ItemKey");
									if (Tools.isNotEmpty(sTmp)) subModule.setItemKey(sTmp.trim());

									sTmp = prop.getProperty("leftSubmenu"+s+"DefaultDisabled");
									if ("true".equals(sTmp) || m.isDefaultDisabled()) subModule.setDefaultDisabled(true);

									sTmp = prop.getProperty("leftSubmenu"+s+"Icon");
									if (Tools.isNotEmpty(sTmp)) subModule.setMenuIcon(sTmp.trim());

									setAvailable(subModule);
									m.addSubmenu(subModule);
								}
							}

							//nacitaj subcomponents
							for (int s=1; s<15; s++)
							{
								String nameKey = prop.getProperty("component"+s+"NameKey");
								String link =    prop.getProperty("component"+s+"Link");
								String icon =    prop.getProperty("component"+s+"Icon");
								if (Tools.isNotEmpty(nameKey) && Tools.isNotEmpty(link))
								{
									if (Tools.isNotEmpty(icon)) icon = icon.trim();

									LabelValueDetails lvd = new LabelValueDetails();
									lvd.setLabel(nameKey.trim());
									lvd.setValue(link.trim());
									lvd.setValue2(icon);

									m.addComponent(lvd);
								}
							}
						}

						//Logger.println(this,"HASH: " + m.getItemKey().hashCode());
						if (j==0 || propFile.exists()) loadModule(m);
					}
				}
			}
		}
		catch (Exception e)
		{
			Logger.error(Modules.class, e);
		}

		ModuleInfo welcome = new ModuleInfo("welcome.left_menu", "welcome", "/admin/v9/dist/views/dashboard/overview.html", null, false, true, "/admin/", "BPECDIMO", 5).setGroup("welcome").setMenuIcon("home");
		loadModule(welcome);

		ModuleInfo webSites = new ModuleInfo("menu.web_sites", "menuWebpages", "/admin/v9/dist/views/webpages/web-pages-list.html", null, true, true, "/admin/webpages/", "BPECDIMO", 10);
		webSites.setGroup("website");
		webSites.setMenuIcon("docs");
		ModuleInfo sub;

		sub = new ModuleInfo();
		sub.setNameKey("user.miniEditMenu");
		sub.setPath("javascript:void()");
		sub.setWjVersions("BPECDIO");
		sub.setUserItem(true);
		sub.setItemKey("editorMiniEdit");
		sub.setDefaultDisabled(true);
		setAvailable(sub);
		webSites.addSubmenu(sub);

		sub = new ModuleInfo();
		sub.setNameKey("components.user.perms.addPage");
		sub.setPath("javascript:void()");
		sub.setWjVersions("BPECDIMO");
		sub.setUserItem(true);
		sub.setItemKey("addPage");
		sub.setDefaultDisabled(false);
		setAvailable(sub);
		webSites.addSubmenu(sub);

		sub = new ModuleInfo();
		sub.setNameKey("components.user.perms.pageSave");
		sub.setPath("javascript:void()");
		sub.setWjVersions("BPECDIMO");
		sub.setUserItem(true);
		sub.setItemKey("pageSave");
		sub.setDefaultDisabled(false);
		setAvailable(sub);
		webSites.addSubmenu(sub);

		sub = new ModuleInfo();
		sub.setNameKey("components.user.perms.pageSaveAs");
		sub.setPath("javascript:void()");
		sub.setWjVersions("BPECDIMO");
		sub.setUserItem(true);
		sub.setItemKey("pageSaveAs");
		sub.setDefaultDisabled(false);
		setAvailable(sub);
		webSites.addSubmenu(sub);

		sub = new ModuleInfo();
		sub.setNameKey("components.user.perms.deletePage");
		sub.setPath("javascript:void()");
		sub.setWjVersions("BPECDIMO");
		sub.setUserItem(true);
		sub.setItemKey("deletePage");
		sub.setDefaultDisabled(false);
		setAvailable(sub);
		webSites.addSubmenu(sub);

		sub = new ModuleInfo();
		sub.setNameKey("components.user.perms.editDir");
		sub.setPath("javascript:void()");
		sub.setWjVersions("BPECDIMO");
		sub.setUserItem(true);
		sub.setItemKey("editDir");
		sub.setDefaultDisabled(false);
		setAvailable(sub);
		webSites.addSubmenu(sub);

		sub = new ModuleInfo();
		sub.setNameKey("components.user.perms.addSubdir");
		sub.setPath("javascript:void()");
		sub.setWjVersions("BPECDIMO");
		sub.setUserItem(true);
		sub.setItemKey("addSubdir");
		sub.setDefaultDisabled(false);
		setAvailable(sub);
		webSites.addSubmenu(sub);

		sub = new ModuleInfo();
		sub.setNameKey("components.user.perms.deleteDir");
		sub.setPath("javascript:void()");
		sub.setWjVersions("BPECDIMO");
		sub.setUserItem(true);
		sub.setItemKey("deleteDir");
		sub.setDefaultDisabled(false);
		setAvailable(sub);
		webSites.addSubmenu(sub);

		sub = new ModuleInfo();
		sub.setNameKey("components.user.perms.editPerexGroups");
		sub.setPath("javascript:void()");
		sub.setWjVersions("BPECDIMO");
		sub.setUserItem(true);
		sub.setItemKey("editor_edit_perex");
		sub.setDefaultDisabled(false);
		setAvailable(sub);
		webSites.addSubmenu(sub);

		sub = new ModuleInfo();
		sub.setNameKey("components.user.perms.editMediaGroups");
		sub.setPath("javascript:void()");
		sub.setWjVersions("BPECIM");
		sub.setUserItem(true);
		sub.setItemKey("editor_edit_media_group");
		sub.setDefaultDisabled(false);
		setAvailable(sub);
		webSites.addSubmenu(sub);

      sub = new ModuleInfo();
      sub.setNameKey("groupedit.showHiddenFolders");
      sub.setPath("javascript:void()");
      sub.setWjVersions("PECDIMO");
      sub.setUserItem(true);
      sub.setItemKey("editor_show_hidden_folders");
      sub.setDefaultDisabled(true);
      setAvailable(sub);
      webSites.addSubmenu(sub);

      loadModule(webSites);

		sub = new ModuleInfo();
		sub.setNameKey("components.user.perms.unlimitedUpload");
		sub.setPath("javascript:void()");
		sub.setWjVersions("BPECDIMO");
		sub.setUserItem(true);
		sub.setItemKey("editor_unlimited_upload");
		sub.setDefaultDisabled(true);
		sub.setGroup("files");
		setAvailable(sub);
		loadModule(sub);

		//sablony
		ModuleInfo templates = new ModuleInfo("menu.templates", "menuTemplates", "/admin/v9/dist/views/templates/temps-list.html", null, true, true, "/admin/v9/templates/temps-list/", modVersions.get("menu.templates"), 20);
		templates.setGroup("templates");
		templates.setMenuIcon("grid");
		loadModule(templates);

		sub = new ModuleInfo();
        sub.setNameKey("components.news.title");
        sub.setPath("/admin/v9/dist/views/templates/news-temps.html");
        sub.setWjVersions(modVersions.get("menu.templates"));
        sub.setItemKey("menuNewsTemplates");
        sub.setUserItem(true);
		sub.setShowInLeftMenu(true);
        sub.setGroup("templates");
		sub.setMenuOrder(21);
		sub.setMenuIcon("ti ti-news");
		loadModule(sub);

		sub = new ModuleInfo();
        sub.setNameKey("menu.group_templates");
        sub.setPath("/admin/v9/dist/views/templates/temps-groups-list.html");
        sub.setWjVersions(modVersions.get("menu.templates"));
        sub.setItemKey("menuTemplatesGroup");
        sub.setUserItem(true);
		sub.setShowInLeftMenu(true);
        sub.setGroup("templates");
		sub.setMenuOrder(22);
		loadModule(sub);

		//pouzivatelia
		ModuleInfo users = new ModuleInfo("menu.users", "users.edit_admins|users.edit_public_users", "/components/user", null, false, true, "/admin/v9/users/user-list/", modVersions.get("menu.users"), 30);
		users.setGroup("users");
		users.setLeftMenuNameKey("menu.users");
		users.setMenuIcon("ti ti-users");
		loadModule(users);

		//toto je tu kvoli menu, aby sa vypisala moznost Zoznam pouzivatelov
		/*
		sub = new ModuleInfo().setGroup("users").setMenuOrder(10);
		sub.setNameKey("users.list");
		sub.setItemKey("users.list");
		sub.setPath("/admin/listusers.do");
		sub.setWjVersions(modVersions.get("menu.users"));
		sub.setUserItem(false);
		sub.setShowInLeftMenu(true);
		setAvailable(sub);
		*/

		//sub.addSubmenu("users.list", "/admin/listusers.do");
		//sub.addSubmenu("users.newUser", "javascript:openPopupDialogFromLeftMenu('/admin/edituser.do?userid=-1');");
		//sub.addSubmenu("users.importFromFile", "javascript:openPopupDialogFromLeftMenu('/admin/listusers.do?import=true');");
		//loadModule(sub);

		if ("cloud".equals(Constants.getInstallName())==false)
		{
			sub = new ModuleInfo().setMenuOrder(10);
			sub.setNameKey("users.edit_admins");
			sub.setItemKey("users.edit_admins");
			sub.setPath("javascript:void()");
			sub.setWjVersions(modVersions.get("menu.users"));
			sub.setDefaultDisabled(false);
			sub.setUserItem(true);
			sub.setShowInLeftMenu(false);

			setAvailable(sub);
			users.addSubmenu(sub);

			sub = new ModuleInfo().setMenuOrder(20);
			sub.setNameKey("users.edit_public_users");
			sub.setItemKey("users.edit_public_users");
			sub.setPath("javascript:void()");
			sub.setWjVersions(modVersions.get("menu.users"));
			sub.setDefaultDisabled(false);
			sub.setUserItem(true);
			sub.setShowInLeftMenu(false);

			setAvailable(sub);
			users.addSubmenu(sub);

			sub = new ModuleInfo().setGroup("users").setMenuOrder(30);
			sub.setNameKey("user.admin.editUserGroups");
			sub.setItemKey("user.admin.userGroups");
			sub.setPath("/admin/listusers.do?groups=true");
			sub.setWjVersions(modVersions.get("menu.users"));
			sub.setUserItem(true);
			sub.setShowInLeftMenu(true);
			sub.setGroup("users");

			setAvailable(sub);
			loadModule(sub);

			sub = new ModuleInfo().setGroup("users").setMenuOrder(31);
			sub.setNameKey("users.edit_perm_groups");
			sub.setItemKey("users.perm_groups");
			sub.setPath("/admin/v9/dist/views/users/permission-groups.html");
			sub.setWjVersions(modVersions.get("menu.users"));
			sub.setUserItem(true);
			sub.setShowInLeftMenu(true);
			sub.setGroup("users");

			setAvailable(sub);
			loadModule(sub);

			sub = new ModuleInfo().setMenuOrder(60);
			sub.setNameKey("users.welcomeShowLoggedAdmins");
			sub.setItemKey("welcomeShowLoggedAdmins");
			sub.setPath("javascript:void()");
			sub.setWjVersions(modVersions.get("menu.users"));
			sub.setDefaultDisabled(false);
			sub.setUserItem(true);
			sub.setShowInLeftMenu(false);
			sub.setGroup("welcome");

			setAvailable(sub);
			loadModule(sub);
		}

		ModuleInfo files = new ModuleInfo("menu.fbrowser", "menuFbrowser", "/admin/v9/files/index/", null, true, true, "/admin/elFinder/", modVersions.get("menu.fbrowser"), 45).setGroup("files").setMenuIcon("folder");

		sub = new ModuleInfo();
		sub.setNameKey("menu.fbrowser.deleteDirectory");
		sub.setPath("javascript:void()");
		sub.setWjVersions(modVersions.get("menu.fbrowser"));
		sub.setUserItem(true);
		sub.setItemKey("fbrowser_delete_directory");
		sub.setDefaultDisabled(true);
		setAvailable(sub);
		files.addSubmenu(sub);

		loadModule(files);



		// Konfiguracia
		sub = new ModuleInfo().setMenuOrder(7000);
		sub.setLeftMenuNameKey("webjet.left_conf.konfiguracia");
		sub.setNameKey("menu.config.rights.menu_config");
		sub.setPath("/admin/v9/dist/views/settings/configuration.html");
		sub.setWjVersions("BPECDIMO");
		sub.setUserItem(true);
		sub.setItemKey("menuConfig");
		sub.setDefaultDisabled(false);
		sub.setGroup("config");
		sub.setShowInLeftMenu(true);
		sub.setMenuIcon("wrench");
		loadModule(sub);

		sub = new ModuleInfo().setMenuOrder(7001);
		sub.setNameKey("admin.conf.showAllvariables");
		sub.setItemKey("conf.show_all_variables");
		sub.setPath("javascript:void()");
		sub.setWjVersions("BPECDIMO");
		sub.setDefaultDisabled(true);
		sub.setGroup("config");
		sub.setUserItem(true);
		sub.setShowInLeftMenu(false);
		loadModule(sub);

		// Aktualizacia WebJETu
		sub = new ModuleInfo().setMenuOrder(7300);
		sub.setLeftMenuNameKey("admin.conf_editor.update_system");
		sub.setNameKey("menu.config.rights.modUpdate");
		sub.setPath("/admin/v9/settings/update/");
		sub.setWjVersions("BPECDIM");
		sub.setUserItem(true);
		sub.setItemKey("modUpdate");
		sub.setDefaultDisabled(false);
		sub.setShowInLeftMenu(true);
		sub.setGroup("config");
		sub.setMenuIcon("refresh");
		loadModule(sub);

		// Editacia textov
		sub = new ModuleInfo().setMenuOrder(7020);
		sub.setLeftMenuNameKey("admin.conf_editor.edit_text");
		sub.setNameKey("menu.config.rights.edit_text");
		sub.setPath("/admin/v9/settings/translation-keys/");
		sub.setWjVersions("BPECDIM");
		sub.setUserItem(true);
		sub.setItemKey("edit_text");
		sub.setDefaultDisabled(false);
		sub.setGroup("config");
		sub.setShowInLeftMenu(true);
		sub.setMenuIcon("speech");
		setAvailable(sub);
		sub.addSubmenu("groupslist.search.title", "/admin/v9/dist/views/settings/translation-keys.html");
		//sub.addSubmenu("admin.conf_editor.pridat_zaznam", "javascript:openPopupDialogFromLeftMenu('/admin/prop_edit.jsp?key=&lng="+Constants.getString("defaultLanguage")+"&value=');");
		//sub.addSubmenu("admin.conf_editor.export-import", "/admin/prop_export-import.jsp");
		sub.addSubmenu("admin.conf_editor.missing", "/admin/prop_missing.jsp");
		loadModule(sub);

		sub = new ModuleInfo().setMenuOrder(7021);
		sub.setNameKey("admin.prop.showAllTexts");
		sub.setItemKey("prop.show_all_texts");
		sub.setPath("javascript:void()");
		sub.setWjVersions("BPECDIM");
		sub.setDefaultDisabled(true);
		sub.setGroup("config");
		sub.setUserItem(true);
		sub.setShowInLeftMenu(false);
		loadModule(sub);

		// Export do HTML
		sub = new ModuleInfo().setMenuOrder(7100);
		sub.setLeftMenuNameKey("admin.offline.dialogTitle");
		sub.setNameKey("menu.config.rights.export_offline");
		sub.setPath("javascript:openPopupDialogFromLeftMenu('/admin/offline.jsp');");
		sub.setWjVersions("BPECI");
		sub.setUserItem(true);
		sub.setItemKey("export_offline");
		sub.setDefaultDisabled(false);
		sub.setGroup("config");
		sub.setShowInLeftMenu(true);
		sub.setMenuIcon("disc");
		loadModule(sub);

		// Restartovat
		sub = new ModuleInfo().setMenuOrder(7310);
		sub.setLeftMenuNameKey("admin.conf_editor.restart");
		sub.setNameKey("menu.config.rights.modRestart");
		sub.setPath("javascript:confirmRestart()");
		sub.setWjVersions("BPECDIM");
		sub.setUserItem(true);
		sub.setItemKey("modRestart");
		sub.setDefaultDisabled(false);
		sub.setGroup("config");
		sub.setShowInLeftMenu(true);
		sub.setMenuIcon("refresh");
		loadModule(sub);

		sub = new ModuleInfo().setMenuOrder(7290);
		sub.setLeftMenuNameKey("components.user.perms.replaceAll");
		sub.setNameKey("components.user.perms.replaceAll");
		sub.setPath("/admin/replaceall.jsp");
		sub.setWjVersions("PEDI");
		sub.setUserItem(true);
		sub.setItemKey("replaceAll");
		sub.setDefaultDisabled(true);
		sub.setShowInLeftMenu(true);
		sub.setGroup("config");
		loadModule(sub);

		// Vytvorit ZIP archiv
		sub = new ModuleInfo().setMenuOrder(7120);
		sub.setLeftMenuNameKey("admin.make_zip_archive");
		sub.setNameKey("menu.config.rights.make_zip_archive");
		sub.setPath("javascript:openPopupDialogFromLeftMenu('/admin/archive.jsp');");
		sub.setWjVersions("BPECI");
		sub.setUserItem(true);
		sub.setItemKey("make_zip_archive");
		sub.setDefaultDisabled(false);
		sub.setGroup("config");
		sub.setShowInLeftMenu(true);
		sub.setMenuIcon("size-actual");
		loadModule(sub);

		sub = new ModuleInfo("image_editor.title", "imageEditor", "/admin/v9/dist/views/apps/image-editor.html", null, true, false, null, modVersions.get("imageEditor"), 50);
		sub.setGroup("website");
		loadModule(sub);

		loadModule(new ModuleInfo("fbrowse.fileIndexer", "fileIndexer", "/admin/fbrowser_dirprop.jsp", null, false, false, null, modVersions.get("fileIndexer"), 330));

		loadModule(new ModuleInfo("multiDomain", "multiDomain", "/admin/v9/dist/views/webpages/web-pages-list.html", "multiDomainEnabled", false, false, null, modVersions.get("multiDomain"), 9999));

		//natiahni moduly z databazy
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM "+ConfDB.MODULES_TABLE_NAME+" ORDER BY module_id");
			rs = ps.executeQuery();
			ModuleInfo mi;
			while (rs.next())
			{
				mi = new ModuleInfo(DB.getDbString(rs, "name_key"), DB.getDbString(rs, "item_key"), DB.getDbString(rs, "path"), null, true, true, null, "BPECDIO", rs.getInt("menu_order"));
				mi.setFromDatabase(true);
				//kontrola, ci uz taky modul neexistuje
				if (isModule(mi.getItemKey())==false)
				{
					loadModule(mi);
				}
			}
			rs.close();
			ps.close();
			rs = null;
			ps = null;
		}
		catch (Exception ex)
		{
			Logger.error(Modules.class, ex);
		}
		finally
		{
			try
			{
				if (db_conn != null)
					db_conn.close();
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				Logger.error(Modules.class, ex2);
			}
		}

		//usortuj ich
		//skonvertuj array list na pole
		int i;
		int size = modules.size();
		ModuleInfo[] modArray = new ModuleInfo[size];
		for (i=0; i<size; i++)
		{
			modArray[i] = modules.get(i);
		}

		Arrays.sort(modArray,
			new Comparator<ModuleInfo>()
			{
				@Override
				public int compare(ModuleInfo m1, ModuleInfo m2)
				{
					int order = m1.getMenuOrder() - m2.getMenuOrder();
					if (order == 0)
					{
					   order = m1.getNameKey().compareTo(m2.getNameKey());
					}
					return (order);
				}
			}
		);
		//skonvertuj na array
		modules = new ArrayList<>();
		modules.addAll(Arrays.asList(modArray));

		Constants.getServletContext().removeAttribute(CONTEXT_KEY);
		Constants.getServletContext().setAttribute(CONTEXT_KEY, this);
	}

	/**
	 * Skontroluje, ci je modul dostupny a prida ho do zoznamu
	 * @param mi
	 */
	private void loadModule(ModuleInfo mi)
	{
		//preiteruj a pripadne zmaz ak uz existuje
		for (ModuleInfo mi2 : modules)
		{
			if (mi2.getItemKey().equals(mi.getItemKey()))
			{
				//leftMenuLink je NULL ale getter vrati path, to je dodatocna kontrola
				if (mi.getPath().startsWith("/apps") && mi.getLeftMenuLink().equals(mi.getPath())) {
					//toto je autoload /apps adresara, modul uz je definovany z components, pouzijeme ten, tento novy ignorujme
					//deje sa, ked nove aplikacie vo WJ9 su v /apps adresari, ktory sa tiez inicializuje
					return;
				}

				//to co je z webjet9 adresara ma vyssiu prioritu, zachovame
				if (mi2.getPath().startsWith("/components/webjet9")) {
					return;
				}

				Logger.debug(Modules.class, "Removing module: " + mi2.getItemKey()+" "+mi2.getPath());
				modules.remove(mi2);
				break;
			}
		}

		setAvailable(mi);
		modules.add(mi);
	}

	/**
	 * Skontroluje, ci pozadovany subor je dostupny, pripadne skontroluje platnost konstanty
	 * @param mi
	 */
	private void setAvailable(ModuleInfo mi)
	{
		if (Tools.isEmpty(mi.getPath()) || Tools.isEmpty(mi.getItemKey())) return;

		//System.out.println("setAvailable, mi.path="+mi.getPath()+" isFile="+FileTools.isFile(mi.getPath()));
		//System.out.println("setAvailable, real="+ Tools.getRealPath(mi.getPath()));

		mi.setAvailable(false);
		if ((FileTools.exists(mi.getPath())) || mi.getLeftMenuLink().startsWith("javascript:") || mi.getPath().startsWith("javascript:") || mi.getPath().indexOf(".do")!=-1 || mi.getPath().endsWith("/"))
		{
			if (mi.getRequireConstantsKey()!=null)
			{
				if (Tools.isNotEmpty(Constants.getString(mi.getRequireConstantsKey())) && "false".equals(Constants.getString(mi.getRequireConstantsKey()))==false)
				{
					mi.setAvailable(true);
				}
			}
			else
			{
				mi.setAvailable(true);
			}
		}
		if (mi.isAvailable())
		{
			Logger.println(this,"Loading module: " + mi.getItemKey());
		}
		else
		{
			Logger.println(this,"Unloading module: "+mi.getItemKey()+" mi.path="+mi.getPath()+" const="+mi.getRequireConstantsKey());
		}
	}

	/**
	 * Vrati zoznam vsetkych modulov
	 * @return
	 */
	public List<ModuleInfo> getModules()
	{
		return(modules);
	}

	/**
	 * Vrati zoznam modulov, ktore su zakazane
	 * @return
	 */
	public List<ModuleInfo> getDisabledModules()
	{
		List<ModuleInfo> ret = new ArrayList<>();

		for (ModuleInfo mi : modules)
		{
			if (mi.isAvailable() == false)
			{
				ret.add(mi);
			}
		}

		return(ret);
	}

	/**
	 * Vrati zoznam dostupnych modulov
	 * @return
	 */
	public List<ModuleInfo> getAvailableModules()
	{
		List<ModuleInfo> ret = new ArrayList<>();

		for (ModuleInfo mi : modules)
		{
			if (mi.isAvailable())
			{
				ret.add(mi);
			}
		}

		return(ret);
	}

	/**
	 * Otestuje, ci zadany modul je dostupny (vseobecne pre cely server)
	 * @param itemKey
	 * @return
	 */
	public boolean isAvailable(String itemKey)
	{
		for (ModuleInfo mi : modules)
		{
			if (mi.getItemKey().equals(itemKey))
			{
				return(mi.isAvailable());
			}
			//skontroluj submenus
			if (mi.getSubmenus()!=null && mi.getSubmenus().size()>0)
			{
				for (ModuleInfo subMi : mi.getSubmenus())
				{
					if (subMi.getItemKey()!=null && subMi.getItemKey().equals(itemKey))
					{
						return(subMi.isAvailable());
					}
				}
			}
		}
		//vratim true, ak je to nazov modulu, ktory fyzicky nie je modul (neexistuje)
		//ako napr. editorMiniEdit
		if (itemKey.startsWith("cmp")) return(false);
		return(true);
	}

	/**
	 * Skontroluje, ci zadany modul existuje
	 * @param itemKey
	 * @return
	 */
	public boolean isModule(String itemKey)
	{
		for (ModuleInfo mi : modules)
		{
			if (mi.getItemKey().equals(itemKey))
			{
				return(true);
			}
			//skontroluj submenus
			if (mi.getSubmenus()!=null && mi.getSubmenus().size()>0)
			{
				for (ModuleInfo subMi : mi.getSubmenus())
				{
					if (subMi.getItemKey()!=null && subMi.getItemKey().equals(itemKey))
					{
						return true;
					}
				}
			}
		}
		return(false);
	}

	/**
	 * Vrati true ak pre zadany modul sa maju v nastaveni pouzivatela zobrazovat sub kategorie
	 * @param itemKey
	 * @return
	 */
	public boolean isShowSubCategories(String itemKey)
	{
		for (ModuleInfo mi : modules)
		{
			if (mi.getItemKey().equals(itemKey))
			{
				return(mi.isShowSubCategories());
			}
		}
		return(false);
	}

	/**
	 * Nastavi zakazane moduly pouzivatelovi
	 * @param user
	 */
	public void disableModules(Identity user)
	{
		for (ModuleInfo mi : modules)
		{
			if (mi.isAvailable() == false)
			{
				//Logger.debug(Modules.class, "disabling1: "+mi.getItemKey());
				user.addDisabledItem(mi.getItemKey());
			}
			//skontroluj submenus
			if (mi.getSubmenus()!=null && mi.getSubmenus().size()>0)
			{
				for (ModuleInfo subMi : mi.getSubmenus())
				{
					if (subMi.getItemKey()!=null && subMi.isAvailable() == false)
					{
						//Logger.debug(Modules.class, "disabling2: "+mi.getItemKey());
						user.addDisabledItem(subMi.getItemKey());
					}
				}
			}
		}
	}

	/**
	 * Vrati zoznam UNIKATNYCH modulov
	 * @param prop
	 * @return
	 */
	public List<ModuleInfo> getUserEditItems(Prop prop)
	{
		List<ModuleInfo> ret = new ArrayList<>();
		ModuleInfo miUser;
		Set<String> uniqueItems = new HashSet<>();
		for (ModuleInfo mi : modules)
		{
			if (mi.isAvailable())
			{
                if (mi.isUserItem() && mi.getItemKey().length()>2) {
                    miUser = new ModuleInfo(prop.getText(mi.getNameKey()), mi.getItemKey(), mi.getPath(), mi.getRequireConstantsKey(), true, mi.isShowInLeftMenu(), mi.getLeftMenuLink(), "BPECDIO", mi.getMenuOrder());
                    miUser.setLeftMenuNameKey(prop.getText(miUser.getLeftMenuNameKey()));
                    miUser.setRootModule(mi.getItemKey());
                    miUser.setGroup(mi.getGroup());
					if (uniqueItems.add(miUser.getItemKey())) {
                    	ret.add(miUser);
					}
                }

				//pridaj submenus
				if (mi.getSubmenus()!=null && mi.getSubmenus().size()>0)
				{
					for (ModuleInfo subMi : mi.getSubmenus())
					{
						if (subMi.isAvailable() && subMi.isUserItem() && subMi.getItemKey()!=null && subMi.getItemKey().length()>2)
						{
							//Logger.debug(Modules.class, "Pridavam submenu " + subMi.getLeftMenuNameKey());
							miUser = new ModuleInfo(prop.getText(subMi.getNameKey()), subMi.getItemKey(), subMi.getPath(), subMi.getRequireConstantsKey(), true, subMi.isShowInLeftMenu(), subMi.getLeftMenuLink(), "BPECDIO", subMi.getMenuOrder());
							miUser.setLeftMenuNameKey(prop.getText(mi.getLeftMenuNameKey()) + " - " + prop.getText(miUser.getLeftMenuNameKey()));
							miUser.setRootModule(mi.getItemKey());
							miUser.setGroup(mi.getGroup());
							if (uniqueItems.add(miUser.getItemKey())) {
								ret.add(miUser);
							}
						}
					}
				}
			}
		}
		return(ret);
	}

	/**
	 * Vrati zoradeny zoznam modulov pre useredit.jsp podla optGroups
	 * @param prop
	 * @return
	 */
	public List<ModuleInfo> getUserEditItemsSorted(Prop prop) {
		String[] optGroups = {"welcome", "website", "components", "templates", "files", "users", "config"};

		Map<String, HashMap<String, ModuleInfo>> orderedOptGroups = new LinkedHashMap<>();

		for (String str : optGroups) {
			orderedOptGroups.put(str, new HashMap<>());
		}

		for (ModuleInfo moduleInfo : Modules.getInstance().getUserEditItems(prop)) {
			if (orderedOptGroups.get(moduleInfo.getGroup()) == null) {
				continue;
			}
			orderedOptGroups.get(moduleInfo.getGroup()).put(moduleInfo.getItemKey(), moduleInfo);
		}

		List<ModuleInfo> ret = new ArrayList<>();

		for (Map.Entry<String, HashMap<String, ModuleInfo>> entry : orderedOptGroups.entrySet()){
			ret.addAll(entry.getValue().values());
		}
		return ret;
	}

	/**
	 * Ziska zoznam poloziek menu pre usera
	 * @param user
	 * @return
	 */
	public List<ModuleInfo> getUserMenuItems(Identity user)
	{
		List<ModuleInfo> ret = new ArrayList<>();
		for (ModuleInfo mi : modules)
		{
			//Logger.debug(this,"checking: " + mi.getItemKey() + "a=" + mi.isAvailable() + " m=" + mi.isShowInLeftMenu() + " u=" + user.isDisabledItem(mi.getItemKey()));
			if (mi.isAvailable() && (mi.isFromDatabase() || mi.isShowInLeftMenu()))
			{
				//musis overit aj submenu polozky
				if (user.isDisabledItem(mi.getItemKey()) && (mi.getSubmenus(user)==null || mi.getSubmenus(user).size()<1))
				{
					continue;
				}

				//Logger.debug(this,"Pridavam: " + mi.getItemKey());
				ret.add(mi);
			}
		}
		return(ret);
	}

	/**
	 * Vrati zoznam vsetkych modulov, pouziva licencny server pri generovani licencie
	 * @return
	 */
	public List<ModuleInfo> getItems()
	{
		List<ModuleInfo> ret = new ArrayList<>();

		Map<String, ModuleInfo> modulesTable = new Hashtable<>();
		for (ModuleInfo m : modules)
		{
			modulesTable.put(m.getItemKey().toLowerCase(), m);
		}

		//	preiteruj moduly a povol
		String modName;
		String modPerms;
		int index;
		for (Map.Entry<String, String> entry : modVersions.entrySet())
		{
			modName = entry.getKey();
			modPerms = entry.getValue();

			ModuleInfo mi = new ModuleInfo();
			mi.setWjVersions(modPerms);

			//Logger.println(this,"--> testujem: " + key + " " + modName + " " + modPerms);
			index = modPerms.indexOf(';');
			if (index > 0)
			{
				mi.setNameKey("webjet_"+modName);
				mi.setItemKey(modPerms.substring(index+1));

				mi.setWjVersions(modPerms.substring(0, index));

				mi.setUserItem(false);

				ModuleInfo m = modulesTable.get(modName.toLowerCase());
				if (m == null) m = modulesTable.get(Tools.replace(modName.toLowerCase(), "cmp_", "menu"));
				if (m != null)
				{
					mi.setUserItem(m.isUserItem());
					mi.setGroup(m.getGroup());
				}

				ret.add(mi);
			}
		}
		return(ret);
	}

	/**
	 * Vrati zoznam submenu modulu podla prav daneho pouzivatela
	 * @param mi
	 * @param user
	 * @return
	 */
	public static List<ModuleInfo> getSubmenus(ModuleInfo mi, Identity user)
	{
		List<ModuleInfo> userSubItems = new ArrayList<>();

		if (mi.getSubmenus()==null || mi.getSubmenus().size()<1) return userSubItems;

		for (ModuleInfo subMi : mi.getSubmenus())
		{
			if (Tools.isEmpty(subMi.getLeftMenuLink()) || "javascript:".equals(subMi.getLeftMenuLink()) || "javascript:void();".equals(subMi.getLeftMenuLink()) || "javascript:void()".equals(subMi.getLeftMenuLink())) continue;

			//pridam ak to nema itemKey, alebo ak ma pravo na dany itemKey
			String subItemKey = subMi.getItemKey();
			if (Tools.isEmpty(subItemKey)) subItemKey = mi.getItemKey();
			if (user.isDisabledItem(subItemKey)==false)
			{
				userSubItems.add(subMi);
			}
		}
		return userSubItems;
	}

	/**
	 * Odfiltruje zoznam modulov podla aktualnej domeny (nastavuje sa v modinfo.properties do atributu domainName)
	 * @param modules
	 * @param domain
	 * @return
	 */
	public static List<ModuleInfo> filterDomain(List<ModuleInfo> modules, String domain)
	{
		if (domain == null) return modules;

		domain = domain.toLowerCase();
		List<ModuleInfo> filtered = new ArrayList<>();
		for (ModuleInfo module : modules)
		{
			if (Tools.isNotEmpty(module.getDomainName()) && module.getDomainName().contains(domain)==false) continue;

			filtered.add(module);
		}
		return filtered;
	}
}