package sk.iway.iwcm.stat;

import static sk.iway.iwcm.Tools.firstNonNull;
import static sk.iway.iwcm.Tools.getDoubleValue;
import static sk.iway.iwcm.Tools.isNotEmpty;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.iway.iwcm.Logger;


/**
 *  BrowserExtractor.java
 *  
 *  Extracts browsers from User-Agent string
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 12.5.2011 13:58:20
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
class BrowserExtractor
{
	private static final List<String> correctlyBehavingBrowsers = Arrays.asList(
		"Firefox", "Netscape", "Safari", "Chrome", "Camino", "Mosaic", "Opera", "Galeon"
	);
			
	private static final List<String> browsersIgnoringRfc = Arrays.asList(
		"Crazy Browser", "Avant Browser"
	);
	
	private static final Pattern commentedProductPattern = Pattern.compile(
				";[ ]*([^/]+?)[/ ]+([0-9.ab]+)",
				Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
	);

	private final RfcBrowserDetector detector;

	public BrowserExtractor(RfcBrowserDetector browserDetector)
	{
		this.detector = browserDetector;
	}

	public String getBrowserName()
	{
		return getBrowser().name;
	}

	public Product getBrowser()
	{
		try
		{
			if (detector.nothingFound())
				return unknownBrowser();
			if (correctlyIdentifiesItself())
				return extractCorrectlyIdentifiedBrowser();
			if (cloaksAsMozilla())
				return extractFromComment(detector.firstProduct().comment);
			//browsers cloaked as MSIE should already be detected in previous steps
			if (isInternetExplorer())
				return extractInternetExplorerFrom(detector.firstProduct().comment);
			if (isObsoleteMozzila())
				return new Product("Netscape");
			if (isRealMozilla())
				return new Product("Mozilla");
			if (isNotEmpty(detector.firstProduct().name))
				return detector.firstProduct();
		}
		catch(Exception e)
		{
			Logger.printf(BrowserExtractor.class, "Unable to parser user agent: %s", detector.userAgent);
		}
		return unknownBrowser();
	}
	
	private Product unknownBrowser()
	{
		return new Product("Unknown");
	}
	
	private boolean correctlyIdentifiesItself()
	{
		return extractCorrectlyIdentifiedBrowser() != null;
	}
	
	private Product extractCorrectlyIdentifiedBrowser()
	{
		Product browser = null;
		for (String browserName : correctlyBehavingBrowsers)
			for (Product product : detector.products)
				if (browserName.equalsIgnoreCase(product.name))
					browser = product;
		if (browser != null && isVersionOutsideOfProduct())
			browser.version = extractVersionFromAnExternalProduct();
		
		return browser;
	}
	
	private boolean cloaksAsMozilla()
	{
		Product firstBrowser = detector.firstProduct();
		return "Mozilla".equals(firstBrowser.name) &&
			firstBrowser.comment != null &&
			firstBrowser.comment.toLowerCase().startsWith("compatible;"); 
	}
	
	/**
	 * General rule for comments containing 
	 * "compatible;" is as follows:
	 * 	(compatible; BROWSER VERSION; OS VERSION;...)
	 * 
	 * Microsoft is, naturally, an exception 
	 */
	private Product extractFromComment(String comment)
	{
		Product product = new Product();
		String illBehavingBrowser = findIllBehavedBrowsersIn(comment);
		if (illBehavingBrowser != null)
		{
			product.name = illBehavingBrowser;
			return product;
		}
		
		Matcher matcher = commentedProductPattern.matcher(comment.replace("compatible", ""));

		if (matcher.find()){
			product.name = firstNonNull(matcher.group(1), "Unknown");
			product.version = detector.stripSubVersions(matcher.group(2));
		}
		
		return product;
	}

	private String findIllBehavedBrowsersIn(String comment)
	{
		for (String browser : browsersIgnoringRfc)
			if (comment.contains(browser))
				return browser;
		return null;
	}
	
	private boolean isVersionOutsideOfProduct()
	{
		return extractVersionFromAnExternalProduct() != null;
	}
	
	/**
	 * Some browsers, namely and mainly Safari identifies itself as
	 * Safari/523.10. This "523.10" is not the version number, however.
	 * 
	 * Version number can be found in a separate product format string,
	 * such as "Version/5.0"
	 */
	private String extractVersionFromAnExternalProduct()
	{
		for (Product product : detector.products)
			if ("Version".equalsIgnoreCase(product.name))
				return product.version;
		return null;
	}
	
	private boolean isInternetExplorer()
	{
		return detector.firstProduct().comment != null &&
			(detector.firstProduct().comment.contains("MSIE") || detector.firstProduct().comment.contains("Trident/"));
	}

	private Product extractInternetExplorerFrom(String comment)
	{
		if (comment.indexOf("MSIE")!=-1)
		{
			Pattern msIePattern = Pattern.compile("MSIE[ /]+([0-9.ab]+)", Pattern.CASE_INSENSITIVE);
			Matcher matcher = msIePattern.matcher(comment);
			matcher.find();
			return new Product("MSIE", detector.stripSubVersions(matcher.group(1)));
		}
		else
		{
			//IE11+
			Pattern msIePattern = Pattern.compile("rv:([0-9.]+)", Pattern.CASE_INSENSITIVE);
			Matcher matcher = msIePattern.matcher(comment);
			matcher.find();
			return new Product("MSIE", detector.stripSubVersions(matcher.group(1)));
		}
	}

	private boolean isRealMozilla()
	{
		Product product = detector.firstProduct();
		Double version = getDoubleValue(product.version, Double.MAX_VALUE);
		return "Mozilla".equalsIgnoreCase(product.name) && version >= 5.0;
	}

	private boolean isObsoleteMozzila()
	{
		Product product = detector.firstProduct();
		Double version = getDoubleValue(product.version, Double.MAX_VALUE);
		return "Mozilla".equalsIgnoreCase(product.name) && version < 5.0;
	}
}