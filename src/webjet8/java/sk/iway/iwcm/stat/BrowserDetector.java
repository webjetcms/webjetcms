package sk.iway.iwcm.stat;

import sk.iway.iwcm.*;
import sk.iway.iwcm.components.seo.SeoManager;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.StringTokenizer;

import static sk.iway.iwcm.Tools.isEmpty;

/**
 *  Description of the Class
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1.1.1 $
 *@created      Utorok, 2002, mďż˝j 28
 *@modified     $Date: 2003/01/28 11:30:13 $
 */
public class BrowserDetector implements Serializable
{
   /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -4467029894704819932L;

	/**
    *  The user agent string.
    */
   private String userAgentString = "";

   /**
    *  The browser name specified in the user agent string.
    */
   private String browserName = "";

   /**
    *  The browser version specified in the user agent string. If we can't parse
    *  the version just assume an old browser.
    */
   private float browserVersion = (float) 1.0;

   /**
    *  The browser platform specified in the user agent string.
    */
   private String browserPlatform = "unknown";

   private String browserSubplatform="";

   private boolean phone = false;
   private boolean tablet = false;
   private boolean amp;

   private String browserDeviceType = null;

   public final static String MSIE = "MSIE";
   public final static String OPERA = "Opera";
   public final static String MOZILLA = "Mozilla";
   public final static String FIREFOX = "Firefox";
   public final static String SAFARI = "Safari";
   public final static String CHROME = "Chrome";

   public final static String WINDOWS = "Windows";
   public final static String UNIX = "Unix";
   public final static String MACINTOSH = "macOS";

   //boty maskujuce sa ako Mozilla
   public final static String BOTS[] = {"Googlebot", "Yahoo!", "Charlotte", "Wauuu", "VoilaBot"};

   private int browserUaId = 0;
   private int platformId = 0;
   private int subplatformId = 0;
   private String country = null;

   /**
    * Ziska instanciu detektora prehliadaca a ulozi do session na neskorsie pouzitie
    * @param request
    * @return
    */
   public static BrowserDetector getInstance(HttpServletRequest request)
   {
   	if (request.getParameter("forceBrowserDetector")!=null) request.getSession().removeAttribute(StatDB.BROWSER_DETECTOR);

   	BrowserDetector browser = (BrowserDetector) request.getSession().getAttribute(StatDB.BROWSER_DETECTOR);
   	if (browser != null)
		{
			return browser;
		}
   	//toto synchronized robilo problemy na pluske... docasne zrusene
   	//synchronized (BrowserDetector.class)
		{
   		//double check	cez Cache objekt (kvoli botom)
			String KEY = null;
			Cache c = null;
			if (request.getParameter("forceBrowserDetector")==null)
			{
				c = Cache.getInstance();
				KEY = "browserDetector-" + Tools.getRemoteIP(request) + "-" + request.getHeader("User-Agent");
	   		browser = (BrowserDetector) c.getObject(KEY);
	      	if (browser != null)
	   		{
	   			return browser;
	   		}
			}

			browser = new BrowserDetector(request);
			request.getSession().setAttribute(StatDB.BROWSER_DETECTOR, browser);

			if (KEY != null && c != null)
			{
				c.setObjectSeconds(KEY, browser, 5*60);
			}

			return browser;
		}
   }

   public static boolean isStatIpAllowedFast(HttpServletRequest request)
   {
   	if (request == null) return true;

   	BrowserDetector browser = (BrowserDetector) request.getSession().getAttribute(StatDB.BROWSER_DETECTOR);
   	if (browser != null)
		{
			return browser.isStatIpAllowed();
		}
   	return isStatIpAllowedImpl(request);
   }

   private static boolean isStatIpAllowedImpl(HttpServletRequest request)
   {
   	try
		{
			String statNoLogIP = Constants.getString("statNoLogIP");
			if (statNoLogIP!=null)
			{
				StringTokenizer st = new StringTokenizer(statNoLogIP, ",");
				String ip;
				String myIP = Tools.getRemoteIP(request);
				while (st.hasMoreTokens())
				{
					ip = st.nextToken();
					if (myIP.startsWith(ip))
					{
						return false;
					}
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return true;
   }

   public BrowserDetector(String userAgentString)
	{
   	this.userAgentString = userAgentString;
	}

  /**
   * Konstruktor objektu, nemal by sa pouzivat, treba pouzivat getInstance(request)
   * @param request
   */
   public BrowserDetector(HttpServletRequest request)
   {
      this.userAgentString = request.getHeader("User-Agent");

		Logger.debug(BrowserDetector.class, "BD: get Instance, ip="+Tools.getRemoteIP(request)+" ua="+userAgentString);

      parse(request);

      statIpAllowed = isStatIpAllowedImpl(request);

		statUserAgentAllowed = true;
		String statDisableUserAgent = Constants.getString("statDisableUserAgent");
		if (Tools.isNotEmpty(statDisableUserAgent))
		{
			StringTokenizer st = new StringTokenizer(statDisableUserAgent.toLowerCase(), ",;+");
			String userAgent;
			//potrebujeme full UA
			String browserName = request.getHeader("User-Agent");
			if (Tools.isNotEmpty(browserName))
			{
				if (browserName.indexOf('(')==-1)
				{
					//je velmi nepravdepodobne, ze normalny UA tam nema nieco v ()
					statUserAgentAllowed = false;
				}
				else
				{
					browserName = browserName.toLowerCase();
					while (st.hasMoreTokens())
					{
						userAgent = st.nextToken().trim();
						if (browserName.indexOf(userAgent) != -1)
						{
							statUserAgentAllowed = false;
							break;
						}
					}
				}
			}
			else
			{
				//ak je UA prazdne, povazujeme to za robota
				statUserAgentAllowed = false;
			}

			/**
			 * 	ulozi vyhladavaci stroj do tabulky kvoli statistike
			 */
			if(!statUserAgentAllowed)
			{
				SeoManager.addSearchEngineVisit(this.browserName +" "+ this.browserVersion);
				if ("unknown".equals(getBrowserPlatform())) browserPlatform = "Search Bot";
			}
		}

		//nastav idecka
      browserUaId = StatDB.getStatKeyId(getBrowserName() + " " + getBrowserVersionShort());
      platformId = StatDB.getStatKeyId(getBrowserPlatform());
      subplatformId = StatDB.getStatKeyId(getBrowserSubplatform());


   }

   /**
    *  Helper method to initialize this class.
    */
   public void parse(HttpServletRequest request)
   {
		try
		{
			if (userAgentString == null)
				return;
			RfcBrowserDetector detector = new RfcBrowserDetector(userAgentString);
			Product browser = detector.getBrowser();
			browserName = browser.name;
			try
			{
				browserVersion = toFloat(browser.version);
			}
			catch (Exception e2)
			{
				browserVersion = 0.0f;
			}
			browserPlatform = detector.getOsName();
			browserSubplatform = detector.getOsVersion();
			if ("Mozilla".equals(browserName) && userAgentString.indexOf("http://") != -1)
			{
				// je to nejaky neznamy maskujuci sa bot
				browserName = "UnknownSearchBot";
			}
			String forceBrowserDetector = forceBrowserTypeIfRequested(request);
			detectBrowserDeviceType();
			// nastav globalnu premennu
			if (Tools.isNotEmpty(forceBrowserDetector)) browserDeviceType = forceBrowserDetector;

			if ("phone".equals(forceBrowserDetector))
			{
				phone = true;
			}
			else if ("amp".equals(forceBrowserDetector))
			{
				amp = true;
			}
			else if ("tablet".equals(forceBrowserDetector))
			{
				tablet = true;
			}
			else if ("smartphoneOrTablet".equals(forceBrowserDetector))
			{
				tablet = true;
			}
			else if ("normal".equals(forceBrowserDetector) || "pc".equals(forceBrowserDetector))
			{
				phone = false;
				tablet = false;
				browserDeviceType = null;
			}
			detectCountry(request);
		}
   	catch (Exception e)
   	{
   		Logger.error(BrowserDetector.class, "BD PARSE: ua="+userAgentString);
   		sk.iway.iwcm.Logger.error(e);
   	}
   }

   private String forceBrowserTypeIfRequested(HttpServletRequest request)
	{
		String forceBrowserDetector = (String)request.getSession().getAttribute("BrowserDetector.forceBrowserDetector");
      if (Tools.isNotEmpty(request.getParameter("forceBrowserDetector")))
      {
      	forceBrowserDetector = request.getParameter("forceBrowserDetector").toLowerCase();
      	if ("pc".equals(forceBrowserDetector))
      	{
      		forceBrowserDetector = null;
      		request.getSession().removeAttribute("BrowserDetector.forceBrowserDetector");
      	}
      	else
      	{
      		request.getSession().setAttribute("BrowserDetector.forceBrowserDetector", forceBrowserDetector);
      	}
      }
		return forceBrowserDetector;
	}

	private void detectBrowserDeviceType()
	{
		//null lebo s nim narabame tak v Template device type (historicka zalezitost)
		browserDeviceType = null;
		String ua = getUserAgentString().toLowerCase();
		if (ua.contains("ipad") || ua.contains("tablet") || ua.contains("kindle"))
		{
			tablet = true;
			browserDeviceType = "tablet";
		}
		else if (ua.contains("iphone"))
		{
			phone = true;
			browserDeviceType = "phone";
		}
		else if (ua.contains("android"))
		{
			//https://stackoverflow.com/questions/5341637/how-do-detect-android-tablets-in-general-useragent
			if (ua.contains("mobile"))
			{
				phone = true;
				browserDeviceType = "phone";
			}
			else
			{
				tablet = true;
				browserDeviceType = "tablet";
			}
		}
	}

	private void detectCountry(HttpServletRequest request)
	{
      country = Tools.getRemoteHost(request);
		if (country == null || country.length()<2)
		{
			country = "unknown";
		}

		int index = country.lastIndexOf('.');
		if (index > 0)
		{
			country = country.substring(index + 1);
		}
		else
		{
			country = "unknown";
		}

		//ak host je cislo je to IP adresa, nemame meno...
		try
		{
			Integer.parseInt(country);
			country = "unknown";
		}
		catch (Exception ex)
		{

		}

		if ("unknown".equals(country))
		{
			//skusme vydedukovat podla accept language
			try
			{
				//sk,en-us;q=0.7,en;q=0.3
				String acceptLanguage = request.getHeader("accept-language");
				if (Tools.isNotEmpty(acceptLanguage))
				{
					String alArray[] = acceptLanguage.split(",");
					if (alArray.length>0)
					{
						String lang = alArray[0];
						try
						{
							//en-us;q=0.7
							if (lang.indexOf(';')!=-1) lang = lang.split(";")[0];
							//en-us
							if (lang.indexOf('-')!=-1) lang = lang.split("-")[1];
							//sk_SK
							if (lang.indexOf('_')!=-1) lang = lang.split("_")[1];
						} catch (Exception e) {}

						lang = lang.toLowerCase();
						String domain = StatDB.getLanguageDomainTable().get(lang);
						if (domain != null) lang = domain;

						country = lang;
					}
				}
				else
				{
					Logger.debug(StatDB.class, "Unknown IP & lang: ua="+request.getHeader("User-Agent"));
				}
			}
			catch (Exception e)
			{
				Logger.error(StatDB.class, "Unknown IP & lang: ua="+request.getHeader("User-Agent")+" acceptLanguage="+request.getHeader("accept-language"));
				sk.iway.iwcm.Logger.error(e);
			}
		}

		if ("gb".equals(country)) country="co.uk";
		if (country.indexOf('_')>0) country = country.substring(0, country.indexOf('_'));
		if (country.indexOf('-')>0) country = country.substring(0, country.indexOf('-'));

		country = DB.internationalToEnglish(DB.prepareString(country, 4).toLowerCase());
	}

   /**
    * Ak je nastavene true zaznamenava sa statistika pre aktualnu IP adresu
    */
   private boolean statIpAllowed = true;
   public boolean isStatIpAllowed()
   {
   	return statIpAllowed;
   }

   /**
    * Ak je nastavene na true zaznamenava sa statistika pre aktualneho user agenta
    */
   private boolean statUserAgentAllowed = true;
   public boolean isStatUserAgentAllowed()
   {
   	return statUserAgentAllowed;
   }

   public String getBrowserName()
   {
      return browserName != null ? browserName : "";
   }

   public String getBrowserPlatform()
   {
      return browserPlatform;
   }

   public float getBrowserVersion()
   {
      return browserVersion;
   }

   public float getBrowserVersionShort()
   {
      int ver = (int) (browserVersion * 10f);
      return ((ver / 10f));
   }

   public String getUserAgentString()
   {
      return userAgentString;
   }

   private float toFloat(String s)
   {
   	if (isEmpty(s))
   		return 0.0f;
   	String sArr[] = s.split("\\.");
   	if (sArr.length==1) return Float.parseFloat(sArr[0]);
   	else if (sArr.length>1) return Float.parseFloat(sArr[0]+"."+sArr[1]);

      throw new NumberFormatException();
   }

   public String getBrowserSubplatform()
   {
      return browserSubplatform;
   }

   public boolean isAmp() {
	   return amp;
   }

	public boolean isTablet()
	{
		return tablet;
	}

	public boolean isPhone()
	{
		return phone;
	}

	/**
	 * Vrati true ak sa jedna o iPhone, iPad alebo Android zariadenie
	 * @return
	 */
	public boolean isSmartphoneOrTablet()
	{
		return tablet || phone;
	}

	/**
	 * Vrati true ak sa jedna o iPhone, iPad alebo Android zariadenie
	 * @param request
	 * @return
	 */
	public static boolean isSmartphoneOrTablet(HttpServletRequest request)
	{
		BrowserDetector bd = BrowserDetector.getInstance(request);
		if (bd == null) return false;
		return bd.isSmartphoneOrTablet();
	}

	public String getBrowserDeviceType()
	{
		return browserDeviceType;
	}

	public int getBrowserUaId()
	{
		return browserUaId;
	}

	public int getPlatformId()
	{
		return platformId;
	}

	public int getSubplatformId()
	{
		return subplatformId;
	}

	/**
	 * Max 4 pismenkovy kod krajiny
	 * @return
	 */
	public String getCountry()
	{
		return country;
	}
}
