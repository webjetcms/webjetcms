package sk.iway.iwcm.stat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.test.TestRequest;
import sk.iway.iwcm.utils.MapUtils;

/**
 *  BrowserDetectorJUnit.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 11.5.2011 13:45:38
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class BrowserDetectorJUnit
{
	private String safari = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_7; sk-sk) AppleWebKit/533.21.1 (KHTML, like Gecko) Version/5.0.5 Safari/533.21.1";
	private String firefox = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:2.0.1) Gecko/20100101 Firefox/4.0.1";
	private String safariOnMac = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_7; sk-sk) AppleWebKit/533.21.1 (KHTML, like Gecko) Version/5.0.5 Safari/533.21.1";
	private String crazyBrowser = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Trident/4.0; GTB6.6; InfoPath.1; Crazy Browser 3.0.3)";
	private String googleBot = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
	private String chromeOnLinux = "Mozilla/5.0 (X11; U; Linux x86_64; en-US) AppleWebKit/534.16 (KHTML, like Gecko) Chrome/10.0.642.2 Safari/534.16";

	private Map<String, String> expectedBrowsers = MapUtils.asMap(
				safari, "Safari",
				firefox, "Firefox",
				safariOnMac, "Safari",
				crazyBrowser, "Crazy Browser",
				googleBot, "Googlebot",
				chromeOnLinux, "Chrome",
				"msnbot/1.1 (+http://search.msn.com/msnbot.htm)", "msnbot",
				"msnbot/2.1", "msnbot",
				"", "Unknown",
				"SeznamBot/2.0 (+http://fulltext.sblog.cz/robot/)", "SeznamBot",
				"Wget/1.9+cvs-stable (Red Hat modified)", "Wget",
				"Lynx/2.8.6rel.5 libwww-FM/2.14", "Lynx",
				"Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1A543a Safari/419.3", "Safari",
				"Microsoft-WebDAV-MiniRedir/6.1.7600", "Microsoft-WebDAV-MiniRedir",
				"Mozilla/5.0 (compatible; Yahoo! Slurp/3.0; http://help.yahoo.com/help/us/ysearch/slurp)", "Yahoo! Slurp",
				"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; en) Opera 8.50", "Opera",
				"Mozilla/5.0 (Windows; U; MSIE 9.0; Windows NT 9.0; en-US)", "MSIE",
				"Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/532.9 (KHTML, like Gecko) Chrome/5.0.307.1 Safari/532.9", "Chrome",
				"Mozilla/5.0 (compatible; Yahoo! Slurp/3.0; http://help.yahoo.com/help/us/ysearch/slurp)", "Yahoo! Slurp",
				"Mozilla/5.0 (Windows NT 5.1) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.65 Safari/534.24", "Chrome"
	);

	private Map<String, String> expectedVersions = MapUtils.asMap(
				safari, "5",
				firefox, "4",
				safariOnMac, "5",
				chromeOnLinux, "10",
				crazyBrowser, null,
				googleBot, "2",
				"msnbot/2.1", "2",
				"", null,
				"Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1A543a Safari/419.3", "3",
				"Microsoft-WebDAV-MiniRedir/6.1.7600", "6",
				"Lynx/2.8.6rel.5 libwww-FM/2.14", "2",
				"Wget/1.9 (Red Hat modified)", "1",
				"msnbot/1.1 (+http://search.msn.com/msnbot.htm)", "1",
				"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; en) Opera 8.50", "8",
				"Mozilla/5.0 (Windows; U; MSIE 9.0; Windows NT 9.0; en-US)", "9",
				"Mozilla/5.0 (compatible; Yahoo! Slurp/3.0; http://help.yahoo.com/help/us/ysearch/slurp)", "3",
				"Mozilla/5.0 (Windows NT 5.1) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.65 Safari/534.24", "11"
	);

	private Map<String, String> expectedOperatingSystems = MapUtils.asMap(
				safari, "macOS",
				firefox, "macOS",
				safariOnMac, "macOS",
				crazyBrowser, "Windows",
				googleBot, "unknown",
				chromeOnLinux, "Unix",
				"Mozilla/5.0 (Windows; U; MSIE 9.0; Windows NT 9.0; en-US)", "Windows",
				"Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1A543a Safari/419.3", "macOS",
				"Mozilla/5.0 (compatible; Yahoo! Slurp/3.0; http://help.yahoo.com/help/us/ysearch/slurp)", "unknown"
	);

	private Map<String, String> expectedOperatingSystemVersions = MapUtils.asMap(
				safari, "Snow Leopard",
				firefox, "Snow Leopard",
				safariOnMac, "Snow Leopard",
				crazyBrowser, "XP"
	);

	@Test
	public void shouldDetectAllBrowsers()
	{
		for(String userAgent : expectedBrowsers.keySet())
		{
			RfcBrowserDetector detector = new RfcBrowserDetector(userAgent);
			assertThat(detector.getBrowserName(), is(expectedBrowsers.get(userAgent)));
		}
	}

	@Test
	public void shouldDetectBrowserVersions()
	{
		for(String userAgent : expectedVersions.keySet())
		{
			RfcBrowserDetector detector = new RfcBrowserDetector(userAgent);
			System.out.println(userAgent);
			assertThat(detector.getBrowser().version, is(expectedVersions.get(userAgent)));
		}
	}

	@Test
	public void shouldDetectOperatingSystems()
	{
		for(String userAgent : expectedOperatingSystems.keySet())
		{
			RfcBrowserDetector detector = new RfcBrowserDetector(userAgent);
			assertThat(detector.getOsName(), is(expectedOperatingSystems.get(userAgent)));
		}
	}

	@Test
	public void shouldDetectVersionsOfOperatingSystem()
	{
		for(String userAgent : expectedOperatingSystemVersions.keySet())
		{
			RfcBrowserDetector detector = new RfcBrowserDetector(userAgent);
			assertThat(detector.getOsVersion(), is(expectedOperatingSystemVersions.get(userAgent)));
		}
	}

	@Test
	public void shouldBehaveTheSameWayInBrowserDetector()
	{
		TestRequest request = new TestRequest();
		String safariOrChrome = "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.65 Safari/534.24";
		request.addHeader("User-Agent", safariOrChrome);
		BrowserDetector browserDetector = new BrowserDetector(safariOrChrome);
		browserDetector.parse(request);
		assertThat(browserDetector.getBrowserName(), is("Chrome"));
	}
}