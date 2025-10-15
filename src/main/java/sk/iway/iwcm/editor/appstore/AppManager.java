package sk.iway.iwcm.editor.appstore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.ModuleComparator;
import sk.iway.iwcm.system.ModuleInfo;
import sk.iway.iwcm.system.Modules;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.users.UsersDB;

/**
 * AppDB.java
 *
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2014
 * @author $Author: jeeff jeeff $
 * @version $Revision: 1.3 $
 * @created Date: 17.3.2014 15:03:01
 * @modified $Date: 2004/08/16 06:26:11 $
 */
public class AppManager
{
	private static Cache c = Cache.getInstance();

	private AppManager() {
		//private konstruktor, kedze vsetky metody su staticke
	}

	/**
	 * vyfiltruje len aplikacie povolene pre daneho pouzivatela
	 * @param appsList
	 * @param request
	 * @return
	 */
	private static List<AppBean> filterUserAppList(List<AppBean> appsList, HttpServletRequest request)
	{
		List<AppBean> ret = new ArrayList<>();
		if (appsList != null)
		{
			ret.addAll(appsList);
			Identity user = UsersDB.getCurrentUser(request);
			Logger.debug(AppManager.class, "filterUserAppList, getCurrentUser="+user);
			if (user == null)
				return ret;

			for (Iterator<AppBean> iterator = ret.iterator(); iterator.hasNext();)
			{
				AppBean app = iterator.next();
				//odstranim zakazane moduly pre pouzivatela
				if(user.isDisabledItem(app.getItemKey()))
				{
					Logger.debug(AppManager.class, "filterUserAppList, odstranujem: app.getItemKey()="+app.getItemKey());
					iterator.remove();
				}
			}

			// sort alphabetically by app name
			Prop prop = Prop.getInstance(Constants.getServletContext(), request);
			Collections.sort(ret, new ModuleComparator(prop));

			return ret;
		}

		return ret;
	}

	/**
	 * Vrati zoznam dostupnych aplikacii pre admin_appstore.jsp
	 *
	 * @param request
	 * @return
	 */
	@SuppressWarnings("java:S1075")
	public static List<AppBean> getAppsList(HttpServletRequest request)
	{

		String lng = Prop.getLngForJavascript(request);
		String CACHE_KEY = "cloud.AppManager.appsList." + lng;
		@SuppressWarnings("unchecked")
		List<AppBean> appsList = (List<AppBean>) c.getObject(CACHE_KEY);
		if (appsList != null)
			return filterUserAppList(appsList, request);

		appsList = new ArrayList<>();

		String dirPath = sk.iway.iwcm.Tools.getRealPath("/components/");
		List<ModuleInfo> modules = Modules.getInstance().getAvailableModules();
		ModuleInfo mi;
		AppBean app;
		if (dirPath != null)
		{
			int size = modules.size();
			int i;

			// prescanuj adresar /components na podadresare, ktory existuje
			// vypis
			String imgPath;
			String editorPath;

			for (i = 0; i < size; i++)
			{
				mi = modules.get(i);
				if (mi.isApp() == false) continue;

				if ("cmp_htmlbox_cloud".equals(mi.getItemKey()))
				{
					// Logger.debug(AppManager.class, "Som HTMLBOX");
				}

				// jeeff: basket zatial nebudeme ponukat ako samostatny modul
				// if (mi.getPath().indexOf("/basket")!=-1) continue;

				// otestuj ci je tam subor editor.jsp
				editorPath = mi.getPath() + "/editor_component.jsp";
				if (FileTools.isFile(editorPath))
				{
					imgPath = mi.getPath() + "/editoricon.png";
					if (FileTools.isFile(imgPath)==false) imgPath = mi.getPath() + "/editoricon.gif";
					if (FileTools.isFile(imgPath)==false)
					{
						continue;
					}

					app = new AppBean(lng);
					app.setItemKey(mi.getItemKey());
					app.setNameKey(mi.getNameKey());
					app.setComponentClickAction(mi.getPath().substring(mi.getPath().lastIndexOf("/") + 1));
					app.setImagePath(imgPath);

					app.setDomainName(mi.getDomainName());
					appsList.add(app);
				}
				if (mi.getComponents() != null && mi.getComponents().size() > 0)
				{
					for (LabelValueDetails lvd : mi.getComponents())
					{
						app = new AppBean(lng);
						app.setItemKey(mi.isUserItem() ? mi.getItemKey() : "");
						app.setNameKey(lvd.getLabel());
						if("cloud".equals(Constants.getInstallName())) app.setComponentClickAction(lvd.getValue().substring(lvd.getValue().lastIndexOf("cloud")+6,lvd.getValue().lastIndexOf('/')));
						else app.setComponentClickAction(lvd.getValue());
						app.setImagePath(lvd.getValue2());

						appsList.add(app);
					}
				}

			}// for
		}

		String dirPathSpec = sk.iway.iwcm.Tools
				.getRealPath("/components/"+Constants.getInstallName()+"/");
		IwcmFile file = new IwcmFile(dirPathSpec);
		IwcmFile[] names = file.listFiles();
		for (IwcmFile f : names)
		{
			if (f.isDirectory() && f.getName().startsWith("app-"))
			{
				app = new AppBean(lng);
				String nameKey = "components."+Constants.getInstallName()+"." + f.getName() + ".title";

				String imgPath = "/components/"+Constants.getInstallName()+"/" + f.getName() + "/editoricon.png";
				if (FileTools.isFile(imgPath)==false) imgPath = "/components/"+Constants.getInstallName()+"/" + f.getName() + "/editoricon.gif";

				//String componentClick = "/components/"+Constants.INSTALL_NAME+"/" + f.getName() + "/editor_component.jsp";
				app.setNameKey(nameKey);
				app.setComponentClickAction(f.getName());
				app.setImagePath(imgPath);
				app.setCustom(true);

				appsList.add(app);
			}
		}

        scanAnnotations(appsList, lng);

		c.setObjectSeconds(CACHE_KEY, appsList, 120 * 60, true);

		return filterUserAppList(appsList, request);
	}

	private static boolean isVariantSame(String variant1, String variant2) {
		if (variant1 == null) variant1 = "";
		if (variant2 == null) variant2 = "";
		variant1 = variant1.trim();
		variant2 = variant2.trim();
		return variant1.equals(variant2);
	}

	public static List<String> getPackageNames() {
		List<String> packageNames = new ArrayList<>();
		packageNames.add("sk.iway.iwcm");
		packageNames.add("sk.iway."+Constants.getInstallName());
		if (Tools.isNotEmpty(Constants.getLogInstallName())) packageNames.add("sk.iway."+Constants.getLogInstallName());
		if (Tools.isNotEmpty(Constants.getString("springAddPackages"))) packageNames.addAll(Arrays.asList(Tools.getTokens(Constants.getString("springAddPackages"), ",", true)));
		return packageNames;
	}

    private static void scanAnnotations(List<AppBean> apps, String lng) {

        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(WebjetAppStore.class));

		Set<String> duplicityCheck = new HashSet<>();

		for (String packageName : getPackageNames()) {
			for (BeanDefinition beanDef : provider.findCandidateComponents(packageName)) {
				try {
					String fqdn = beanDef.getBeanClassName();
					if (fqdn == null || duplicityCheck.contains(fqdn)) continue;
					duplicityCheck.add(fqdn);

					Class<?> cl = Class.forName(fqdn);

					WebjetAppStore appStore = cl.getAnnotation(WebjetAppStore.class);

					String itemKey = appStore.itemKey();
					if (Tools.isNotEmpty(itemKey)) {
						//remove app from apps if there is already one with the same itemKey
						for (AppBean app : apps) {
							if (app.getItemKey().equals(itemKey) && isVariantSame(appStore.variant(), app.getVariant())) {
								Logger.debug("Removing app with itemKey="+itemKey);
								apps.remove(app);
								break;
							}
						}
					}

					AppBean app = new AppBean(lng);
					app.setComponentClickAction(cl.getCanonicalName());
					app.setNameKey(appStore.nameKey());
					app.setDescKey(appStore.descKey());
					app.setItemKey(itemKey);
					app.setImagePath(appStore.imagePath());
					app.setDomainName(appStore.domainName());
					app.setGalleryImages(appStore.galleryImages());
					app.setComponentPath(appStore.componentPath());
					app.setVariant(appStore.variant());

					if (fqdn.startsWith("sk.iway.iwcm")) {
						if (appStore.custom().length>1) app.setCustom(appStore.custom()[0]);
						apps.add(app);
					}
					else {
						apps.add(0, app);
						if (appStore.custom().length>1) app.setCustom(appStore.custom()[0]);
						else app.setCustom(true);
					}

					Logger.debug(AppManager.class, "Adding app from annotation, key="+app.getNameKey()+" fqdn="+fqdn);

				} catch (Exception e) {
					Logger.error(AppManager.class, "Got exception: " + e.getMessage());
				}
			}
		}
    }

	/**
	 * Returns map of class names and jsp paths for replacing in appstore editor componentseg.:
	 * "sk.iway.iwcm.components.gallery.GalleryApp" -> "/components/gallery/gallery.jsp"
	 * @param request
	 * @return
	 */
	public static Map<String, String> getClassToJspReplaces(HttpServletRequest request) {
		Map<String, String> replaces = new Hashtable<>();
		List<AppBean> apps = getAppsList(request);
		for (AppBean app : apps) {
			String path = app.getComponentPath();
			if (Tools.isEmpty(path)) continue;

			//path could be coma separated, we need all of them
			String[] paths = Tools.getTokens(path, ",", true);
			for (String p : paths) {
				replaces.put(p, app.getComponentClickAction());
			}
		}
		return replaces;
	}

}
