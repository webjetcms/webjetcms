package sk.iway.iwcm.system.fulltext.lucene;

import java.nio.ByteBuffer;
import java.util.Arrays;

import io.github.duckasteroid.cdb.Cdb;

import org.apache.lucene.analysis.cz.CzechStemmer;
import org.apache.lucene.analysis.de.GermanMinimalStemmer;
import org.apache.lucene.analysis.en.EnglishMinimalStemmer;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.fulltext.cdb.CdbCacheListener;
import sk.iway.iwcm.system.fulltext.cdb.CdbFactory;
import sk.iway.iwcm.system.fulltext.cdb.CdbUtils;

/**
 * Lemmas.java
 *
 *@Title webjet7
 *@Company Interway s.r.o. (www.interway.sk)
 *@Copyright Interway s.r.o. (c) 2001-2011
 *@author $Author: jeeff thaber $
 *@version $Revision: 1.3 $
 *@created Date: 4.5.2011 15:40:45
 *@modified $Date: 2004/08/16 06:26:11 $
 */
public class Lemmas
{
	protected Lemmas() {
		//utility class
	}

	/**
	 * Vytvori lemmas z celej vety:
	 * Žiadosti o výplatu -> ziadost o vyplat
	 * @param language
	 * @param text
	 * @return
	 */
	public static String get(String language, String text) {
		String[] words = Tools.getTokens(text, " \t\n");
		StringBuilder response = new StringBuilder();
		for (String word : words) {
			if (response.length()>1) response.append(" ");
			char[] lemmas = get(language, word.toCharArray(), 0, word.length());
			if (lemmas != null && lemmas.length>1) response.append(lemmas);
		}

		return response.toString();
	}

	/**
	 * Get a lemma from the supplied form
	 * @param language
	 * @param form
	 * @param offset
	 * @param length
	 * @return
	 */
	public static char[] get(String language, char[] form,int offset,int length)
	{
		if (Constants.getBoolean("luceneIndexingSkAlgorithmicStemming") && "sk".equals(language)){
			return SlovakStemmer.stem(new String(form,offset,length)).toCharArray();
		}

		if ("cz".equals(language)) {
			//pre CZ nemame Lemmas, mame len stemmer, ale lepsie ako nic
			CzechStemmer stemmer = new CzechStemmer();
			int baseLength = stemmer.stem(form, length);
			if (baseLength<1) return form;
			char[] stemmed = Arrays.copyOf(form, baseLength);
			return stemmed;
		}
		else if ("en".equals(language)) {
			//pre CZ nemame Lemmas, mame len stemmer, ale lepsie ako nic
			EnglishMinimalStemmer stemmer = new EnglishMinimalStemmer();
			int baseLength = stemmer.stem(form, length);
			if (baseLength<1) return form;
			char[] stemmed = Arrays.copyOf(form, baseLength);
			return stemmed;
		}
		else if ("de".equals(language)) {
			//pre CZ nemame Lemmas, mame len stemmer, ale lepsie ako nic
			GermanMinimalStemmer stemmer = new GermanMinimalStemmer();
			int baseLength = stemmer.stem(form, length);
			if (baseLength<1) return form;
			char[] stemmed = Arrays.copyOf(form, baseLength);
			return stemmed;
		}

		try {
			Cache c = Cache.getInstance();
			CdbCacheListener.init();
			String CACHE_KEY = "Lucene.Lemmas."+language+"."+Thread.currentThread().getId();
			Cdb cdb = (Cdb)c.getObject(CACHE_KEY);
			if (cdb == null) {
				cdb = (Cdb)new CdbFactory(language,CdbFactory.Type.LEMMAS).makeObject();
				c.setObjectSeconds(CACHE_KEY, cdb, 5*60, false);
			}

			ByteBuffer bytes = cdb.find( ByteBuffer.wrap(CdbUtils.encode(form, offset, length)) );

			if (bytes != null && bytes.hasArray())
			{
				return CdbUtils.decode(bytes.array());
			}
		} catch (Exception e) {
			Logger.error(Lemmas.class, e);
		}
		return SlovakStemmer.stem(new String(form,offset,length)).toCharArray();
	}
}
