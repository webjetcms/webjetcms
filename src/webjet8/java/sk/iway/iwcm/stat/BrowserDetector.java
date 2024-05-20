package sk.iway.iwcm.stat;

import sk.iway.iwcm.*;
import sk.iway.iwcm.components.seo.SeoManager;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.util.ResponseUtils;

import java.io.Serializable;
import java.util.StringTokenizer;

import ua_parser.Parser;
import ua_parser.Client;

/**
 * Parse User-Agent string to detect browser name, version and platform.
 */
public class BrowserDetector implements Serializable {
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -4467029894704819932L;

	/**
	 * The user agent string.
	 */
	private String userAgentString = "";

	/**
	 * The browser name specified in the user agent string.
	 */
	private String browserName = "";

	/**
	 * The browser version specified in the user agent string. If we can't parse
	 * the version just assume an old browser.
	 */
	private String browserVersion = "0.0";

	/**
	 * The browser platform specified in the user agent string.
	 */
	private String browserPlatform = "unknown";

	private String browserSubplatform = "";

	private boolean phone = false;
	private boolean tablet = false;
	private boolean amp;

	private String browserDeviceType = null;

	public static final String MSIE = "MSIE";
	public static final String OPERA = "Opera";
	public static final String MOZILLA = "Mozilla";
	public static final String FIREFOX = "Firefox";
	public static final String SAFARI = "Safari";
	public static final String CHROME = "Chrome";

	public static final String WINDOWS = "Windows";
	public static final String UNIX = "Unix";
	public static final String MACINTOSH = "macOS";

	// boty maskujuce sa ako Mozilla
	protected static final String[] BOTS = { "Googlebot", "Yahoo!", "Charlotte", "Wauuu", "VoilaBot" };

	private int browserUaId = 0;
	private int platformId = 0;
	private int subplatformId = 0;
	private String country = null;

	private static Parser uaParser = null;

	static {
		String configPath = Constants.getString("uaParserYamlPath");
		if (Tools.isNotEmpty(configPath)) {
			try {
				IwcmFile yamlFile = new IwcmFile(Tools.getRealPath(configPath));
				if (yamlFile.exists() && yamlFile.isFile() && yamlFile.canRead()) {
					Logger.debug(BrowserDetector.class, "loading parser from " + yamlFile.getAbsolutePath());
					IwcmInputStream is = new IwcmInputStream(yamlFile);
					uaParser = new Parser(is, Parser.getDefaultLoaderOptions());
					is.close();
				}
			} catch (Exception ie) {
				Logger.error(BrowserDetector.class, "Failed to initialize parser from "+configPath, ie);
			}
		}
		if (uaParser == null) uaParser = new Parser();
	}

	/**
	 * Ziska instanciu detektora prehliadaca a ulozi do session na neskorsie
	 * pouzitie
	 *
	 * @param request
	 * @return
	 */
	public static BrowserDetector getInstance(HttpServletRequest request) {
		if (request.getParameter("forceBrowserDetector") != null)
			request.getSession().removeAttribute(StatDB.BROWSER_DETECTOR);

		BrowserDetector browser = (BrowserDetector) request.getSession().getAttribute(StatDB.BROWSER_DETECTOR);
		if (browser != null) {
			return browser;
		}

		// double check cez Cache objekt (kvoli botom)
		String KEY = null;
		Cache c = null;
		if (request.getParameter("forceBrowserDetector") == null) {
			c = Cache.getInstance();
			KEY = "browserDetector-" + Tools.getRemoteIP(request) + "-" + request.getHeader("User-Agent");
			browser = (BrowserDetector) c.getObject(KEY);
			if (browser != null) {
				return browser;
			}
		}

		browser = new BrowserDetector(request);
		request.getSession().setAttribute(StatDB.BROWSER_DETECTOR, browser);

		if (c != null) {
			c.setObjectSeconds(KEY, browser, 5 * 60);
		}

		return browser;
	}

	public static boolean isStatIpAllowedFast(HttpServletRequest request) {
		if (request == null)
			return true;

		BrowserDetector browser = (BrowserDetector) request.getSession().getAttribute(StatDB.BROWSER_DETECTOR);
		if (browser != null) {
			return browser.isStatIpAllowed();
		}
		return isStatIpAllowedImpl(request);
	}

	private static boolean isStatIpAllowedImpl(HttpServletRequest request) {
		try {
			String statNoLogIP = Constants.getString("statNoLogIP");
			if (statNoLogIP != null) {
				StringTokenizer st = new StringTokenizer(statNoLogIP, ",");
				String ip;
				String myIP = Tools.getRemoteIP(request);
				while (st.hasMoreTokens()) {
					ip = st.nextToken();
					if (myIP.startsWith(ip)) {
						return false;
					}
				}
			}
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
		}
		return true;
	}

	public BrowserDetector(String userAgentString) {
		this.userAgentString = userAgentString;
		parse(null);
	}

	/**
	 * Konstruktor objektu, nemal by sa pouzivat, treba pouzivat
	 * getInstance(request)
	 *
	 * @param request
	 */
	public BrowserDetector(HttpServletRequest request) {
		this.userAgentString = request.getHeader("User-Agent");

		Logger.debug(BrowserDetector.class, "BD: get Instance, ip=" + Tools.getRemoteIP(request) + " ua=" + userAgentString);

		parse(request);

		statIpAllowed = isStatIpAllowedImpl(request);

		statUserAgentAllowed = true;
		String statDisableUserAgent = Constants.getString("statDisableUserAgent");
		if (Tools.isNotEmpty(statDisableUserAgent)) {
			StringTokenizer st = new StringTokenizer(statDisableUserAgent.toLowerCase(), ",;+");
			// potrebujeme full UA
			String userAgent = request.getHeader("User-Agent");
			if (Tools.isNotEmpty(userAgent)) {
				if (userAgent.indexOf('(') == -1) {
					// je velmi nepravdepodobne, ze normalny UA tam nema nieco v ()
					statUserAgentAllowed = false;
				} else {
					userAgent = userAgent.toLowerCase();
					while (st.hasMoreTokens()) {
						String token = st.nextToken().trim();
						if (userAgent.indexOf(token) != -1) {
							statUserAgentAllowed = false;
							break;
						}
					}
				}
			} else {
				// ak je UA prazdne, povazujeme to za robota
				statUserAgentAllowed = false;
			}

			/**
			 * ulozi vyhladavaci stroj do tabulky kvoli statistike
			 */
			if (!statUserAgentAllowed) {
				SeoManager.addSearchEngineVisit(this.browserName + " " + this.browserVersion);
				if ("unknown".equals(getBrowserPlatform()))
					browserPlatform = "Search Bot";
			}
		}

		// nastav idecka
		browserUaId = StatDB.getStatKeyId(getBrowserName() + " " + getBrowserVersionShort());
		platformId = StatDB.getStatKeyId(getBrowserPlatform());
		subplatformId = StatDB.getStatKeyId(getBrowserSubplatform());

	}

	/**
	 * Helper method to initialize this class.
	 */
	public void parse(HttpServletRequest request) {
		try {
			if (userAgentString == null)
				return;

			Client uaClient = uaParser.parse(userAgentString);

			browserName = uaClient.userAgent.family;
			if (uaClient.userAgent.major!=null) browserVersion = uaClient.userAgent.major+"."+uaClient.userAgent.minor;
			browserPlatform = uaClient.os.family;
			browserSubplatform = uaClient.os.major;

			String forceBrowserDetector = forceBrowserTypeIfRequested(request);
			detectBrowserDeviceType();
			// nastav globalnu premennu
			if (Tools.isNotEmpty(forceBrowserDetector))
				browserDeviceType = forceBrowserDetector;

			if ("phone".equals(forceBrowserDetector)) {
				phone = true;
			} else if ("amp".equals(forceBrowserDetector)) {
				amp = true;
			} else if ("tablet".equals(forceBrowserDetector)) {
				tablet = true;
			} else if ("smartphoneOrTablet".equals(forceBrowserDetector)) {
				tablet = true;
			} else if ("normal".equals(forceBrowserDetector) || "pc".equals(forceBrowserDetector)) {
				phone = false;
				tablet = false;
				browserDeviceType = null;
			}
			detectCountry(request);

		} catch (Exception e) {
			Logger.error(BrowserDetector.class, "BD PARSE: ua=" + userAgentString);
			sk.iway.iwcm.Logger.error(e);
		}
	}

	private String forceBrowserTypeIfRequested(HttpServletRequest request) {
		if (request == null) return null;

		String forceBrowserDetector = null;

		if (request.getSession()!=null) {
			forceBrowserDetector = (String) request.getSession().getAttribute("BrowserDetector.forceBrowserDetector");
		}
		if (Tools.isNotEmpty(request.getParameter("forceBrowserDetector"))) {
			forceBrowserDetector = request.getParameter("forceBrowserDetector").toLowerCase();
			if ("pc".equals(forceBrowserDetector)) {
				forceBrowserDetector = null;
				request.getSession().removeAttribute("BrowserDetector.forceBrowserDetector");
			} else {
				request.getSession().setAttribute("BrowserDetector.forceBrowserDetector", forceBrowserDetector);
			}
		}
		return forceBrowserDetector;
	}

	private void detectBrowserDeviceType() {
		// null lebo s nim narabame tak v Template device type (historicka zalezitost)
		browserDeviceType = null;
		String ua = getUserAgentString().toLowerCase();
		if (ua.contains("ipad") || ua.contains("tablet") || ua.contains("kindle")) {
			tablet = true;
			browserDeviceType = "tablet";
		} else if (ua.contains("iphone")) {
			phone = true;
			browserDeviceType = "phone";
		} else if (ua.contains("android")) {
			// https://stackoverflow.com/questions/5341637/how-do-detect-android-tablets-in-general-useragent
			if (ua.contains("mobile")) {
				phone = true;
				browserDeviceType = "phone";
			} else {
				tablet = true;
				browserDeviceType = "tablet";
			}
		}
	}

	private void detectCountry(HttpServletRequest request) {
		if (request == null) {
			country = "unknown";
			return;
		}
		country = Tools.getRemoteHost(request);
		if (country == null || country.length() < 2) {
			country = "unknown";
		}

		int index = country.lastIndexOf('.');
		if (index > 0) {
			country = country.substring(index + 1);
		} else {
			country = "unknown";
		}

		// ak host je cislo je to IP adresa, nemame meno...
		try {
			Integer.parseInt(country);
			country = "unknown";
		} catch (Exception ex) {

		}

		if ("unknown".equals(country)) {
			// skusme vydedukovat podla accept language
			try {
				// sk,en-us;q=0.7,en;q=0.3
				String acceptLanguage = request.getHeader("accept-language");
				if (Tools.isNotEmpty(acceptLanguage)) {
					String[] alArray = acceptLanguage.split(",");
					if (alArray.length > 0) {
						String lang = alArray[0];
						try {
							// en-us;q=0.7
							if (lang.indexOf(';') != -1)
								lang = lang.split(";")[0];
							// en-us
							if (lang.indexOf('-') != -1)
								lang = lang.split("-")[1];
							// sk_SK
							if (lang.indexOf('_') != -1)
								lang = lang.split("_")[1];
						} catch (Exception e) {
						}

						lang = lang.toLowerCase();
						String domain = StatDB.getLanguageDomainTable().get(lang);
						if (domain != null)
							lang = domain;

						country = lang;
					}
				} else {
					Logger.debug(StatDB.class, "Unknown IP & lang: ua=" + request.getHeader("User-Agent"));
				}
			} catch (Exception e) {
				Logger.error(StatDB.class, "Unknown IP & lang: ua=" + request.getHeader("User-Agent")
						+ " acceptLanguage=" + request.getHeader("accept-language"));
				sk.iway.iwcm.Logger.error(e);
			}
		}

		if ("gb".equals(country))
			country = "co.uk";
		if (country.indexOf('_') > 0) //NOSONAR
			country = country.substring(0, country.indexOf('_'));
		if (country.indexOf('-') > 0) //NOSONAR
			country = country.substring(0, country.indexOf('-'));

		country = DB.internationalToEnglish(DB.prepareString(country, 4).toLowerCase());
	}

	/**
	 * Ak je nastavene true zaznamenava sa statistika pre aktualnu IP adresu
	 */
	private boolean statIpAllowed = true;

	public boolean isStatIpAllowed() {
		return statIpAllowed;
	}

	/**
	 * Ak je nastavene na true zaznamenava sa statistika pre aktualneho user agenta
	 */
	private boolean statUserAgentAllowed = true;

	public boolean isStatUserAgentAllowed() {
		return statUserAgentAllowed;
	}

	public String getBrowserName() {
		if (Tools.isEmpty(browserName) || "Other".equals(browserName)) return "Unknown";
		if ("IE".equals(browserName)) return "MSIE";
		return ResponseUtils.filter(browserName);
	}

	public String getBrowserPlatform() {
		if (Tools.isEmpty(browserPlatform) || "Other".equals(browserPlatform)) return "unknown";
		return ResponseUtils.filter(browserPlatform);
	}

	public String getBrowserVersion() {
		return browserVersion;
	}

	public String getBrowserVersionShort() {
		if (browserVersion == null) return "";
		int index = browserVersion.indexOf('.');
		if (index != -1) {
			return browserVersion.substring(0, index);
		}
		return browserVersion;
	}

	public String getUserAgentString() {
		return ResponseUtils.filter(userAgentString);
	}

	public String getBrowserSubplatform() {
		return ResponseUtils.filter(browserSubplatform);
	}

	public boolean isAmp() {
		return amp;
	}

	public boolean isTablet() {
		return tablet;
	}

	public boolean isPhone() {
		return phone;
	}

	public boolean isDesktop() {
		if (isTablet() || isPhone())
			return false;
		return true;
	}

	/**
	 * Vrati true ak sa jedna o iPhone, iPad alebo Android zariadenie
	 *
	 * @return
	 */
	public boolean isSmartphoneOrTablet() {
		return tablet || phone;
	}

	/**
	 * Vrati true ak sa jedna o iPhone, iPad alebo Android zariadenie
	 *
	 * @param request
	 * @return
	 */
	public static boolean isSmartphoneOrTablet(HttpServletRequest request) {
		BrowserDetector bd = BrowserDetector.getInstance(request);
		return bd.isSmartphoneOrTablet();
	}

	public String getBrowserDeviceType() {
		return ResponseUtils.filter(browserDeviceType);
	}

	public int getBrowserUaId() {
		return browserUaId;
	}

	public int getPlatformId() {
		return platformId;
	}

	public int getSubplatformId() {
		return subplatformId;
	}

	/**
	 * Max 4 pismenkovy kod krajiny
	 *
	 * @return
	 */
	public String getCountry() {
		return ResponseUtils.filter(country);
	}
}
