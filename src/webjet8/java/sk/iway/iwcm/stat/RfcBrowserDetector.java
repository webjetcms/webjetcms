package sk.iway.iwcm.stat;

import static sk.iway.iwcm.Tools.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/**
 *  RfcBrowserDetector.java
 *  
 *  Detects browsers and operating systems
 *  in a User-Agent string. Unlike legacy {@link BrowserDetector},
 *  algorithms in this class are based on HTTP RFC description
 *  of User-Agent string.
 *  
 *  For further description, see
 *  http://www.texsoft.it/index.php?c=software&m=sw.php.useragent&l=it
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 11.5.2011 16:03:14
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class RfcBrowserDetector
{
	/*
	private static final Pattern productPattern = Pattern.compile(
				"[ ]*([^/ ]*)([/ ]([^/ ]*))?([ ]*\\[[a-z][a-z]\\])?"+
				"[ ]*(\\((([^()]|(\\([^()]*\\)))*)\\))?[ ]*",
				Pattern.CASE_INSENSITIVE
	);
	*/
	
	final String userAgent;
	List<Product> products;
	private BrowserExtractor browserExtractor;
	private OsExtractor osExtractor;
	
	public RfcBrowserDetector(String userAgent)
	{
		this.userAgent = userAgent;
		this.products = parse();
		this.browserExtractor = new BrowserExtractor(this);
		this.osExtractor = new OsExtractor(this);
	}
	
	private List<Product> parse()
	{
		List<Product> products = new ArrayList<Product>();
		Logger.debug(RfcBrowserDetector.class, "Testing "+userAgent);
		/*
		Matcher matcher = productPattern.matcher(userAgent);
		int failsafe = 0;
		while (matcher.find())
		{
			Product product = new Product();
			product.name = matcher.group(1);
			product.version = stripSubVersions(matcher.group(3));
			product.comment = matcher.group(6);
			products.add(product);
			Logger.debug(RfcBrowserDetector.class, "Adding product: "+product.name+" ver="+product.version+" comment="+product.comment);
		}	
		*/
		
		//Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_7; sk-sk) AppleWebKit/533.21.1 (KHTML, like Gecko) Version/5.0.5 Safari/533.21.1
		StringTokenizer st = new StringTokenizer(userAgent, " ");
		Product product = null;
		boolean somVZatvorkach = false;
		while (st.hasMoreTokens())
		{
			String token = st.nextToken().trim();
			if (token.startsWith("(")) somVZatvorkach = true;
			
			if (product != null && somVZatvorkach)
			{
				if (token.startsWith("(")) token = token.substring(1);
				if (token.endsWith(")"))
				{
					somVZatvorkach = false;
					token = token.substring(0, token.length()-1);
				}
				
				//je to komentar
				if (Tools.isNotEmpty(product.comment)) product.comment += " " + token;
				else product.comment = token;								
			}			
			else
			{
				//lebo Opera 8.5 je bez lomitka
				if (product != null && Character.isDigit(token.charAt(0)))
				{
					double verzia = Tools.getDoubleValue(token, 0);
					if (verzia > 0)
					{
						product.version = stripSubVersions(token);
						continue;
					}
				}
				
				somVZatvorkach = false;
				product = new Product();
				String nameVersion[] = Tools.getTokens(token, "/", true);
				if (nameVersion.length>=2)
				{
					product.name = nameVersion[0];
					product.version = stripSubVersions(nameVersion[1]);
				}
				else
				{
					product.name = token;
				}
				products.add(product);
			}
		}
		for (Product p : products)
		{
			Logger.debug(RfcBrowserDetector.class, "Adding product: "+p.name+" ver="+p.version+" comment="+p.comment);
		}
		return products;
	}
	
	String stripSubVersions(String version)
	{
		if (isEmpty(version))
			return null;
		String[] versionParts = version.split("[.,]");
		if (versionParts.length == 0)
			return version;
		return versionParts[0]; //.replaceAll(".*([0-9].*)", "$1");
	}

	public String getBrowserName()
	{
		return browserExtractor.getBrowserName();
	}
	
	public Product getBrowser()
	{
		return browserExtractor.getBrowser();
	}
	
	public String getOsName()
	{
		return new OsVersionTranslator(osExtractor.getOs()).getOsName();
	}
	
	public String getOsVersion()
	{
		return new OsVersionTranslator(osExtractor.getOs()).getVersion();
	}

	Product firstProduct()
	{
		return products.get(0);
	}

	boolean nothingFound()
	{
		return products.size() == 0;
	}

	/*
	boolean hasOnlyOneProduct()
	{
		return products.size() == 1 && isNotEmpty(firstProduct().name);
	}
	*/
}