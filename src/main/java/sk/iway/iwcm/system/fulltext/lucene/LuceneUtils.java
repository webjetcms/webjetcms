package sk.iway.iwcm.system.fulltext.lucene;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;


/**
 *  LuceneUtils.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: jeeff thaber $
 *@version      $Revision: 1.3 $
 *@created      Date: 18.5.2011 16:09:13
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class LuceneUtils
{
	public static final String EMPTY = "EMPTY"; // poison
	public static final String DATE_MIN = "19700101"; // najmensi mozny datum
	public static final String DATE_MAX = "20990101"; // najvacsi mozny datum
	public static final String LUCENE_INDEX = Tools.getRealPath(Constants.getString("luceneIndexDir"));

	private LuceneUtils(){}

	public static String nvl(String value)
	{
		if (StringUtils.isBlank(value) || "null".equals(value) )
		{
			return LuceneUtils.EMPTY;
		}
		return value;
	}

	public static String nonNull(String value){
		if (value == null)
		{
			return "";
		}
		return value;
	}

	public static java.util.Date luceneDateToDate(String date)
	{
		if (EMPTY.equals(date))
			return null;
		try
		{
			return DateTools.stringToDate(date);
		}
		catch (ParseException e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return null;
	}

	public static String timestampToLucene(long timestamp)
	{
		return DateTools.timeToString(timestamp, DateTools.Resolution.MINUTE);
	}

	public static String dateToLucene(java.sql.Date date)
	{
		if (date != null)
		{
			return timestampToLucene(date.getTime());
		}
		return null;
	}

	public static String dateToLucene(Timestamp date)
	{
		if (date != null)
		{
			return timestampToLucene(date.getTime());
		}
		return null;
	}

	/**
	 * Kontrola prazdnosti fieldu vratane prazdnosti hodnoty
	 * @param luceneDocument
	 * @param fieldName
	 * @return
	 */
	public static boolean isEmpty(Document luceneDocument, String fieldName)
	{
		Fieldable f = luceneDocument.getFieldable(fieldName);
		if (isEmpty(f)) return true;

		String value = luceneDocument.get(fieldName);
		if (LuceneUtils.EMPTY.equals(value)) return true;

		return Tools.isEmpty(value);
	}

	/**
	 * Pozor, toto netestuje hodnotu, ale len prazdny field
	 * @param f
	 * @return
	 */
	private static boolean isEmpty(Fieldable f)
	{
		if (f == null) return true;
		if (LuceneUtils.EMPTY.equals(f.stringValue())) return true;

		return false;
	}

	/**
	 * Pokusi sa sparsovat dany string podla poskytnuteho formatu, ak sa nieco pokazi tak vrati aktualny cas
	 * @param dateString
	 * @param df
	 * @return
	 */
	public static long getTimestamp(String dateString, DateFormat df)
	{
		long time = System.currentTimeMillis();
		try{
			time = df.parse(dateString).getTime();
		}
		catch(ParseException e){
			Logger.debug(LuceneUtils.class, "Failed to parse date string: " + dateString + " expected format: " + df);
		}
		return time;
	}
}
