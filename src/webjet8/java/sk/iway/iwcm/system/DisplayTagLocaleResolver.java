package sk.iway.iwcm.system;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.displaytag.localization.I18nResourceProvider;
import org.displaytag.localization.LocaleResolver;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.tags.support_logic.CustomResponseUtils;

/**
 *  DislpayTagLocaleResolver.java - resolver pre ResourceBundle
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.9 $
 *@created      Date: 10.4.2005 18:11:45
 *@modified     $Date: 2009/05/04 09:26:17 $
 */
public class DisplayTagLocaleResolver implements I18nResourceProvider, LocaleResolver
{
	/**
    * prefix/suffix for missing entries.
    */
   public static final String UNDEFINED_KEY = "???"; //$NON-NLS-1$

   /**
    * @see LocaleResolver#resolveLocale(HttpServletRequest)
    */
   @Override
   public Locale resolveLocale(HttpServletRequest request)
   {
       Locale userLocale = request.getLocale();

       String lng = getLng(null, request);
       if (Tools.isNotEmpty(lng))
       {
      	 if ("cz".equals(lng)) lng = "cs";
      	 userLocale = new Locale(lng);
       }

       Logger.debug(this,"resolveLocale, lng="+lng+" locale="+userLocale);

       return userLocale;
   }

   /**
    * @see I18nResourceProvider#getResource(String, String, Tag, PageContext)
    */
   @Override
   public String getResource(String resourceKey, String defaultValue, Tag tag, PageContext pageContext)
   {
      // if titleKey isn't defined either, use property
      String key = (resourceKey != null) ? resourceKey : defaultValue;

      if (key.indexOf("basic.msg.empty_list")!=-1)
      {
      	System.out.println("------------------ MAM TO -----------------");
      }

      HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

      String lng = getLng(pageContext, request);

		boolean needRefresh = false;
		if (request.getSession().getAttribute("userlngr") != null)
		{
			//ak to mame v session, tak chceme pri kazdom zobrazeni stranky
			// spravit refresh
			needRefresh = true;
		}
		if (request.getParameter("userlngr") != null)
		{
			needRefresh = true;
		}
		if (pageContext.getAttribute("userlngr") != null)
		{
			//pri zobrazeni tejto stranky sme uz jazyk refreshli, nema zmysel to
			// robit znova
			needRefresh = false;
		}
		//Logger.debug(this,"userlngr="+request.getParameter("userlngr"));
		Prop prop = null;
		if (needRefresh)
		{
			pageContext.setAttribute("userlngr", "refreshed");
			prop = Prop.getInstance(Constants.getServletContext(), lng, true);
			if ("session".equals(request.getParameter("userlngr")))
			{
				//uloz si to do session, teraz sa bude refreshovat pri kazdom
				// zobrazeni stranky
				request.getSession().setAttribute("userlngr", "session");
			}
		}
		else
		{
			prop = Prop.getInstance(Constants.getServletContext(), lng, false);
		}

		Logger.debug(this,"resolver ("+lng+"): "+key);

		String text = prop.getText(key);
		//text = prop.getText(key);
		if (text != null)
		{
			return(text);
		}

       return key;
   }

   private String getLng(PageContext pageContext, HttpServletRequest request)
   {
   	String lng = "";
		if (pageContext != null && pageContext.getAttribute("lng") != null)
		{
			lng = (String) pageContext.getAttribute("lng");
		}
		else if (request.getAttribute("PageLng")!=null)
		{
			lng = (String)request.getAttribute("PageLng");
		}
		else
		{
			lng = CustomResponseUtils.filter(request.getParameter("language"));
			if (lng != null) request.getSession().setAttribute(Prop.SESSION_I18N_PROP_LNG, lng);
			if (lng == null)
			{
				lng = (String) request.getSession().getAttribute(Prop.SESSION_I18N_PROP_LNG);
			}
			if (lng == null)
			{
				lng = sk.iway.iwcm.Constants.getString("defaultLanguage");
			}
		}

		return lng;
   }

}
