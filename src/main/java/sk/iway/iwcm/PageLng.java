package sk.iway.iwcm;

import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.controller.IwayStripesUtils;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.i18n.Prop;

/**
 *  Stranku na pracu s nastavenim jazyka pouzivatela
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Sobota, 2003, november 22
 *@modified     $Date: 2003/11/23 13:22:16 $
 */
public class PageLng
{

   protected PageLng() {
      //utility class
   }

   public static void setUserLng(HttpServletRequest request, HttpServletResponse response, String setLng)
   {
      try
      {
         String actualCookieValue = Tools.getCookieValue(request.getCookies(), "lng", null);
         String lng = setLng;

         if (request.getAttribute("is404") != null && Tools.isNotEmpty(actualCookieValue))
         {
            //som 404 stranka, nesmiem zmenit jazyk, pretoze potom na session nastavi iny jazyk ako je v cookie
            //napr. pri volani components/_common/ninja.min.css.map sa nastavi SK jazyk, pretoze to nie je /en/404.html
            //zachovame teda cookie hdonotu a nezmenime ho
            lng = actualCookieValue;
         }

         if (response != null)
         {
            if (actualCookieValue == null || actualCookieValue.equals(lng) == false)
            {
               Cookie c = new Cookie("lng", lng);
               c.setPath("/");
               c.setMaxAge(30 * 24 * 3600);
               c.setHttpOnly(true);
               Tools.addCookie(c, response, request);
            }
         }

         request.getSession().setAttribute("lng", lng);
         request.setAttribute("PageLng", lng);

         IwayStripesUtils.setLocale(request, lng);
         RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
         if (rb!=null)
         {
        	 rb.setLng(lng);
         }
      }
      catch (Exception ex)
      {
         sk.iway.iwcm.Logger.error(ex);
      }
   }

   public static String getUserLngIso(HttpServletRequest request)
   {
      String lng = getUserLng(request);

      return getUserLngIso(lng);
   }

   /**
    * Skonvertuje jazyk vo WebJETe na ISO kod jazyk-krajina, pre sk vrati sk-SK, pre cz vrati cs-CZ a podobne
    * krajina sa da konfigurovat v konf. premennej countryForLng
    * @param lng
    * @return
    */
   public static String getUserLngIso(final String lng)
   {
      String lngFixed = lng;
      if (Tools.isEmpty(lngFixed)) lngFixed = "sk";
      if ("cz".equals(lng)) lngFixed = "cs";

      String country = getCountryFromLng(lngFixed);
      String lngIso = lngFixed.toLowerCase()+"-"+country;

      return lngIso;
   }

   /**
    * Vrati krajinu pre zadany jazyk
    * @param lng
    * @return
    */
   public static String getCountryFromLng(final String lng)
   {
      String lngFixed = lng;
      if ("cz".equals(lng)) lngFixed = "cs";

      String CACHE_KEY = "PageLng.countryTable";
      Cache c = Cache.getInstance();
      @SuppressWarnings("unchecked")
      Map<String, String> countryTable = (Map<String, String>)c.getObject(CACHE_KEY);

      if (countryTable == null)
      {
         countryTable = Constants.getHashtable("countryForLng");
         c.setObject(CACHE_KEY, countryTable, 60);
      }

      String country = countryTable.get(lngFixed);

      if (Tools.isEmpty(country))
      {
         //failsafe ak by nebolo definovane nic v tabulke
         if ("cz".equals(lngFixed) || "cs".equals(lngFixed)) country = "CZ";
         else if ("en".equals(lngFixed)) country = "GB";
         else country = lngFixed.toUpperCase();
      }

      return country;
   }

   public static String getUserLng(HttpServletRequest request)
   {
      String lng = Constants.getString("defaultLanguage");

      try
      {
         if (request!=null)
         {
            if (request.getParameter("language") != null)
            {
               lng = request.getParameter("language");
            } else if (request.getParameter("__lng") != null)
            {
               lng = request.getParameter("__lng");
            } else if (request.getParameter("lng") != null && request.getParameter("key")==null)
            {
                //key tam je preto, aby nam to nemenilo jazyk v dialogu prop_edit.jsp
               lng = request.getParameter("lng");
            } else if (request.getAttribute("PageLng") != null)
            {
               lng = (String) request.getAttribute("PageLng");
            } else if (request.getSession().getAttribute(Prop.SESSION_I18N_PROP_LNG) != null && request.getSession().getAttribute(Constants.USER_KEY) != null && request.getRequestURI() != null && request.getRequestURI().indexOf("admin") != -1)
            {
               //najskor musi ist test pre admina, aby sa nemenil jazyk admin casti
               lng = (String) request.getSession().getAttribute(Prop.SESSION_I18N_PROP_LNG);
            } else if (request.getSession().getAttribute("lng") != null)
            {
               lng = (String) request.getSession().getAttribute("lng");
            } else if (request.getCookies() != null)
            {
               //preiteruj cez cookies
               Cookie[] cookies = request.getCookies();
               int i;
               int len = cookies.length;
               Cookie c;
               for (i = 0; i < len; i++)
               {
                  c = cookies[i];
                  if ("lng".equals(c.getName()))
                  {
                     lng = c.getValue();
                     break;
                  }
               }
            }
         }
      }
      catch (Exception ex)
      {

      }

      //System.out.println("lng1="+lng);

      //kontrola a ochrana pred xss v cookies
      if (lng == null || (lng.length()!=2 && lng.length()!=3))
      {
         lng = Constants.getString("defaultLanguage");
      }
      if (DocTools.testXss(lng)) lng = Constants.getString("defaultLanguage");

      if (Tools.isEmpty(lng) || "null".equals(lng))
      {
      	lng = "sk";
      }

      //toto nemozeme nastavit, potom boli dialogy pre pridanie komponenty v admin casti v jazyku sablony nie WebJETu
      //request.setAttribute("PageLng", lng);

      //System.out.println("lng2="+lng);

      return(lng);
   }
}
