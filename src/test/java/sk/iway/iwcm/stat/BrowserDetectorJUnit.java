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
class BrowserDetectorJUnit
{
	private String safari = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_7; sk-sk) AppleWebKit/533.21.1 (KHTML, like Gecko) Version/5.0.5 Safari/533.21.1";
	private String firefox = "Mozilla/5.0 (Macintosh; Intel Mac OS X 11.6; rv:2.0.1) Gecko/20100101 Firefox/4.0.1";
	private String safariOnMac = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_7; sk-sk) AppleWebKit/533.21.1 (KHTML, like Gecko) Version/5.0.5 Safari/533.21.1";
	private String crazyBrowser = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Trident/4.0; GTB6.6; InfoPath.1; Crazy Browser 3.0.3)";
	private String googleBot = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
	private String chromeOnLinux = "Mozilla/5.0 (X11; U; Linux x86_64; en-US) AppleWebKit/534.16 (KHTML, like Gecko) Chrome/10.0.642.2 Safari/534.16";

	private String fbIphone =  	"Mozilla/5.0 (iPhone; CPU iPhone OS 17_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/21C66 Instagram 314.0.6.16.114 (iPhone13,2; iOS 17_2_1; sk_SK; sk; scale=3.00; 1170x2532; 555508374)";
	private String fbIphone2 = 	"Mozilla/5.0 (iPhone; CPU iPhone OS 17_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/21C66 Instagram 314.0.6.16.114 (iPhone11,6; iOS 17_2_1; sk_SK; sk; scale=3.00; 1242x2688; 555508374) NW/3";
	private String fbIpad = 	"Mozilla/5.0 (iPad; CPU OS 16_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/20D67 [FBAN/FBIOS;FBAV/446.0.0.32.330;FBBV/554572428;FBDV/iPad6,11;FBMD/iPad;FBSN/iPadOS;FBSV/16.3.1;FBSS/2;FBID/tablet;FBLC/sk_SK;FBOP/5;FBRV/558404046]";
	private String fbAndroid = 	"Mozilla/5.0 (Linux; Android 10; M2006C3MNG Build/QP1A.190711.020; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/120.0.6099.210 Mobile Safari/537.36 [FB_IAB/FB4A;FBAV/441.0.0.23.113;]";
	private String fbAndroid2 = "Mozilla/5.0 (Linux; Android 12; 2203129G Build/SKQ1.211006.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/120.0.6099.211 Mobile Safari/537.36 [FB_IAB/FB4A;FBAV/441.0.0.23.113;]";

	private String ahrefs = 	"Mozilla/5.0 (compatible; AhrefsBot/7.0; +http://ahrefs.com/robot/)";
	private String claude = 	"Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko; compatible; ClaudeBot/1.0; +claudebot@anthropic.com)";
	private String perplexity = "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko; compatible; PerplexityBot/1.0; +https://perplexity.ai/perplexitybot)";
	private String openAi = 	"Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko); compatible; ChatGPTBot/1.0; +https://openai.com/bot";
	private String amazon = 	"Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko; compatible; Amzn-SearchBot/0.1) Chrome/119.0.6045.214 Safari/537.36";
	private String uptime = 	"Mozilla/5.0+(compatible; UptimeRobot/2.0; http://www.uptimerobot.com/)";
	private String amazon2 = 	"Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko; compatible; Amazonbot/0.1; +https://developer.amazon.com/support/amazonbot) Chrome/119.0.6045.214 Safari/537.36";
	private String monspark = 	"Mozilla/5.0+(compatible; MonSpark/1.0; http://www.monspark.com/)";
	private String bing =		"Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko; compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm) Chrome/116.0.1938.76 Safari/537.36";

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
				"Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1A543a Safari/419.3", "Mobile Safari",
				"Microsoft-WebDAV-MiniRedir/6.1.7600", "Unknown",
				"Mozilla/5.0 (compatible; Yahoo! Slurp/3.0; http://help.yahoo.com/help/us/ysearch/slurp)", "Yahoo! Slurp",
				"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; en) Opera 8.50", "Opera",
				"Mozilla/5.0 (Windows; U; MSIE 9.0; Windows NT 9.0; en-US)", "MSIE",
				"Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/532.9 (KHTML, like Gecko) Chrome/5.0.307.1 Safari/532.9", "Chrome",
				"Mozilla/5.0 (compatible; Yahoo! Slurp/3.0; http://help.yahoo.com/help/us/ysearch/slurp)", "Yahoo! Slurp",
				"Mozilla/5.0 (Windows NT 5.1) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.65 Safari/534.24", "Chrome",
				fbIphone, "Instagram",
				fbIphone2, "Instagram",
				fbIpad, "Facebook",
				fbAndroid, "Facebook",
				fbAndroid2, "Facebook",
				ahrefs, "AhrefsBot",
				claude, "ClaudeBot",
				perplexity, "PerplexityBot",
				openAi, "ChatGPTBot",
				amazon, "Amzn-SearchBot",
				uptime, "UptimeRobot",
				amazon2, "Amazonbot",
				monspark, "MonSparkBot",
				bing, "bingbot"
	);

	private Map<String, String> expectedVersions = MapUtils.asMap(
				safari, "5.0",
				firefox, "4.0",
				safariOnMac, "5.0",
				chromeOnLinux, "10.0",
				crazyBrowser, "3.0",
				googleBot, "2.1",
				"msnbot/2.1", "2.1",
				"", "0.0",
				"Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1A543a Safari/419.3", "3.0",
				"Microsoft-WebDAV-MiniRedir/6.1.7600", "0.0",
				"Lynx/2.8.6rel.5 libwww-FM/2.14", "2.8",
				"Wget/1.9 (Red Hat modified)", "1.9",
				"msnbot/1.1 (+http://search.msn.com/msnbot.htm)", "1.1",
				"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; en) Opera 8.50", "8.50",
				"Mozilla/5.0 (Windows; U; MSIE 9.0; Windows NT 9.0; en-US)", "9.0",
				"Mozilla/5.0 (compatible; Yahoo! Slurp/3.0; http://help.yahoo.com/help/us/ysearch/slurp)", "3.0",
				"Mozilla/5.0 (Windows NT 5.1) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.65 Safari/534.24", "11.0",
				fbIphone, "314.0",
				fbIphone2, "314.0",
				fbIpad, "446.0",
				fbAndroid, "441.0",
				fbAndroid2, "441.0",
				perplexity, "1.0",
				openAi, "1.0",
				amazon, "0.1",
				uptime, "2.0",
				amazon2, "0.1",
				monspark, "1.0",
				bing, "2.0"
	);

	private Map<String, String> expectedOperatingSystems = MapUtils.asMap(
				safari, "Mac OS X",
				firefox, "Mac OS X",
				safariOnMac, "Mac OS X",
				crazyBrowser, "Windows",
				googleBot, "unknown",
				chromeOnLinux, "Linux",
				"Mozilla/5.0 (Windows; U; MSIE 9.0; Windows NT 9.0; en-US)", "Windows",
				"Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1A543a Safari/419.3", "iOS",
				"Mozilla/5.0 (compatible; Yahoo! Slurp/3.0; http://help.yahoo.com/help/us/ysearch/slurp)", "unknown",
				fbIphone, "iOS",
				fbIphone2, "iOS",
				fbIpad, "iOS",
				fbAndroid, "Android",
				fbAndroid2, "Android",
				perplexity, "unknown",
				openAi, "unknown",
				amazon, "unknown",
				uptime, "unknown",
				amazon2, "unknown",
				monspark, "unknown",
				bing, "unknown"
	);

	private Map<String, String> expectedOperatingSystemVersions = MapUtils.asMap(
				safari, "10",
				firefox, "11",
				safariOnMac, "10",
				crazyBrowser, "XP",
				perplexity, "unknown",
				openAi, "unknown",
				amazon, "unknown",
				uptime, "unknown",
				amazon2, "unknown",
				monspark, "unknown",
				bing, "unknown"
	);

	@Test
	void shouldDetectAllBrowsers()
	{
		for(String userAgent : expectedBrowsers.keySet())
		{
			System.out.println("shouldDetectAllBrowsers: "+userAgent);
			BrowserDetector detector = new BrowserDetector(userAgent);
			System.out.println(detector.getBrowserName()+"; "+detector.getBrowserVersion()+"; "+detector.getBrowserPlatform()+"; "+detector.getBrowserSubplatform());
			System.out.flush();
			assertThat(detector.getBrowserName(), is(expectedBrowsers.get(userAgent)));
		}
	}

	@Test
	void shouldDetectBrowserVersions()
	{
		for(String userAgent : expectedVersions.keySet())
		{
			System.out.println("shouldDetectBrowserVersions: "+userAgent);
			System.out.flush();

			BrowserDetector detector = new BrowserDetector(userAgent);
			System.out.println(userAgent);
			System.out.println("1: "+detector.getBrowserName()+"; "+detector.getBrowserVersion()+"; "+detector.getBrowserVersionShort());
			assertThat(detector.getBrowserVersion(), is(expectedVersions.get(userAgent)));
		}
	}

	@Test
	void shouldDetectOperatingSystems()
	{
		for(String userAgent : expectedOperatingSystems.keySet())
		{
			System.out.println("shouldDetectOperatingSystems: "+userAgent);
			System.out.flush();

			BrowserDetector detector = new BrowserDetector(userAgent);
			assertThat(detector.getBrowserPlatform(), is(expectedOperatingSystems.get(userAgent)));
		}
	}

	@Test
	void shouldDetectVersionsOfOperatingSystem()
	{
		for(String userAgent : expectedOperatingSystemVersions.keySet())
		{
			System.out.println("shouldDetectOperatingSystems: "+userAgent);
			System.out.flush();

			BrowserDetector detector = new BrowserDetector(userAgent);
			assertThat(detector.getBrowserSubplatform(), is(expectedOperatingSystemVersions.get(userAgent)));
		}
	}

	@Test
	void shouldBehaveTheSameWayInBrowserDetector()
	{
		TestRequest request = new TestRequest();
		String safariOrChrome = "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.65 Safari/534.24";
		request.addHeader("User-Agent", safariOrChrome);
		BrowserDetector browserDetector = new BrowserDetector(safariOrChrome);
		browserDetector.parse(request);
		assertThat(browserDetector.getBrowserName(), is("Chrome"));
	}
}