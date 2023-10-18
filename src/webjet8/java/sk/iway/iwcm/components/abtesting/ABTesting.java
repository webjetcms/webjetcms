package sk.iway.iwcm.components.abtesting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.users.UsersDB;

/**
 *  ABTesting.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2016
 *@author       $Author: jeeff rzapach $
 *@version      $Revision: 1.3 $
 *@created      Date: 25.1.2016 12:12:32
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ABTesting
{
	private ABTesting() {

	}

	public static int getVirtualPathDocId(String path, String domain, HttpServletRequest request, HttpServletResponse response)
	{
		DocDB docDB = DocDB.getInstance();

		boolean abTestAvailable = Constants.getBoolean("ABTesting");
		Identity user = UsersDB.getCurrentUser(request);

		//pre prihlaseneho usera test nepouzijeme, lebo sa neda spravit preview verzie
		if(abTestAvailable == false || (user != null && user.isAdmin()) )
			return docDB.getVirtualPathDocId(path, domain);

		String variant = Tools.getCookieValue(request.getCookies(), Constants.getString("ABTestingCookieName"), null);

		if(variant==null)
		{
			variant = generateVariant();
			Cookie cookie = new Cookie(Constants.getString("ABTestingCookieName"), variant);
			cookie.setMaxAge(Constants.getInt("ABTestingCookieDays") *24*60*60);
			cookie.setPath("/");
			//response.addCookie(cookie);
			Tools.addCookie(cookie, response, request);
		}

		if("a".equals(variant))
			return docDB.getVirtualPathDocId(path, domain);
		else
		{
			String varName = Constants.getString("ABTestingName") + variant;
			String newPath = getNewPath(path, varName);

			//ak taka stranka existuje
			int bPathDocId = docDB.getVirtualPathDocId(newPath, domain);
			String bPathUrl = docDB.getDocLink(bPathDocId);
			if (Tools.isEmpty(bPathUrl) || bPathUrl.contains(varName)==false)
			{
				//ak URL stranky co sa nasla neobsahuje variantu nemozeme pouzit, je to asi * url
				bPathDocId = -1;
			}

			if (bPathDocId > 0) {
				// [#36895] zistujeme, ci je stranka dostupna
				DocDetails basicDocDetails = docDB.getBasicDocDetails(bPathDocId, false);
				if (!basicDocDetails.isAvailable()) {
					bPathDocId = -1;
				}
			}

			if(bPathDocId>0)
				return docDB.getVirtualPathDocId(newPath, domain);
			else
				return docDB.getVirtualPathDocId(path, domain);
		}
	}

	public static String generateVariant()
	{
		String variantLetter = "a";
		try
		{
			String ratio = Constants.getString("ABTestingRatio");
			int sum = 0;

			String[] items = (ratio + "").replace(",", ".").split(":");
			double[] results = new double[items.length];
			for (int i = 0; i < items.length; i++)
			{
				results[i] = Double.parseDouble(items[i]);
				sum += results[i];
			}

			int picked = ThreadLocalRandom.current().nextInt(1, sum + 1);

			double check = 0;
			for(int i=0; i<results.length; i++)
			{
				check += results[i];
				if(picked<=check)
				{
					variantLetter = "" + ((char) ('a'+i));
					break;
				}
			}
		}
		catch(Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return variantLetter;
	}

	private static String getNewPath(String path, String varName)
	{
		String result = "";

		if(path.contains("."))
		{
			int lastDot = path.lastIndexOf('.');
			result = path.subSequence(0, lastDot) + "-"+varName+path.substring(lastDot);
		}
		else
			result = path+varName+".html";

		return result;
	}

	public static List<Integer> getAllVariantsDocIds(int doc_id)
	{
		return getAllVariantsDocIds(DocDB.getInstance().getBasicDocDetails(doc_id, false), GroupsDB.getInstance().getAllDomainsList(), DocDB.getInstance());
	}

	public static List<Integer> getAllVariantsDocIds(DocDetails doc, List<String> allDomains, DocDB docDB)
	{
		List<Integer> result = new ArrayList<Integer>();

		if(doc==null)
			return result;

		try
		{
			String path = doc.getVirtualPath();
			String newPath;
			int variantDocId;
			for(int i = 0; i< Constants.getString("ABTestingRatio").split(":").length; i++)
			{
				newPath = getNewPath(path, Constants.getString("ABTestingName") + ("" + ((char) ('a'+i))));
				variantDocId = docDB.getVirtualPathDocId(newPath, (String) null);

				if(variantDocId>0) {
					result.add(variantDocId);
				}
				else
				{
					for(String domain : allDomains)
					{
						variantDocId = docDB.getVirtualPathDocId(newPath, domain);
						if(variantDocId>0)
						{
							DocDetails variantDoc = docDB.getBasicDocDetails(variantDocId, false);
							String virtualPath = variantDoc != null ? variantDoc.getVirtualPath() : null;
							if (virtualPath != null && Tools.isNotEmpty(virtualPath) && (virtualPath.endsWith("*") || virtualPath.endsWith("*/"))) {
								continue;
							}

							result.add(variantDocId);
							break;
						}
					}
				}
			}
		}
		catch(Exception e){sk.iway.iwcm.Logger.error(e);}

		return result;
	}
}
