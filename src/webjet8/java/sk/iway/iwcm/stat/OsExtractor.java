package sk.iway.iwcm.stat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.iway.iwcm.Logger;


/**
 *  OsExtractor.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 12.5.2011 14:05:00
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
class OsExtractor
{
	private final RfcBrowserDetector detector;
	private static final Pattern versionPattern = Pattern.compile("\\b(.*?)\\b([0-9._]+)\\b");

	public OsExtractor(RfcBrowserDetector detector)
	{
		this.detector = detector;
	}

	public Product getOs()
	{
		if (detector.nothingFound())
			return new Product("Unknown");

		return extractOsFromComment(detector.firstProduct().comment);
	}

	private Product extractOsFromComment(String comment)
	{
		try
		{
			List<String> osList = new ArrayList<String>();
			for (String element : comment.split(";"))
			{
				String elementInLower = element.toLowerCase();
				if (elementInLower.contains("win") ||
				elementInLower.contains("linux") ||
				elementInLower.contains("macintosh") ||
				elementInLower.contains("mac os x") ||
				elementInLower.contains("freebsd") ||
				elementInLower.contains("netbsd") ||
				elementInLower.contains("openbsd") ||
				elementInLower.contains("sunos") ||
				elementInLower.contains("amiga") ||
				elementInLower.contains("beos") ||
				elementInLower.contains("irix") ||
				elementInLower.contains("os/2"))
				{
					osList.add(element);
				}
			}

			if (osList.size() > 1)
				return getRelevantElement(osList);
			if (osList.size() == 1)
				return toProduct(osList.get(0));
		}
		catch (Exception e)
		{
			Logger.printf(OsExtractor.class, "Can't parse OS out of user agent: %s", detector.userAgent);
		}
		return new Product("Unknown");
	}

	private Product toProduct(String element)
	{
		Matcher matcher = versionPattern.matcher(element);
		if (!matcher.find())
			return new Product(element);
		return new Product(matcher.group(1).trim(), matcher.group(2).trim());
	}

	/**
	 * For win exclude "windows", if present "Win 9x 4.90", return it
	 */
	private Product getRelevantElement(List<String> osList)
	{
		for (Iterator<String> iterator = osList.iterator(); iterator.hasNext();)
		{
			String product = iterator.next();
			if (product.toLowerCase().contains("macintosh"))
				iterator.remove();
			if (product.equalsIgnoreCase("windows"))
				iterator.remove();

			if (osList.size() == 1)
				break;
		}

		return toProduct(osList.get(0));
	}
}