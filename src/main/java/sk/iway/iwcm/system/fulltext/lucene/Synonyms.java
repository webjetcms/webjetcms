package sk.iway.iwcm.system.fulltext.lucene;

import static sk.iway.iwcm.system.fulltext.cdb.CdbUtils.encode;

import java.nio.ByteBuffer;

import io.github.duckasteroid.cdb.Cdb;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.system.fulltext.cdb.CdbCacheListener;
import sk.iway.iwcm.system.fulltext.cdb.CdbFactory;
import sk.iway.iwcm.system.fulltext.cdb.CdbUtils;

/**
 *  Synonyms.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: jeeff thaber $
 *@version      $Revision: 1.3 $
 *@created      Date: 5.5.2011 14:12:42
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class Synonyms
{
	protected Synonyms() {
		//utility class
	}

	/**
	 * Get the base word from synonym order
	 * @param language
	 * @param form
	 * @param offset
	 * @param length
	 * @return
	 */
	public static char[] get(String language, char[] form,int offset,int length)
	{
		try
		{
			Cache c = Cache.getInstance();
			CdbCacheListener.init();
			String CACHE_KEY = "Lucene.Synonyms." + language+"."+Thread.currentThread().getId();
			Cdb cdb = (Cdb)c.getObject(CACHE_KEY);
			if (cdb == null)
			{
				cdb = (Cdb)new CdbFactory(language,CdbFactory.Type.SYNONYMS).makeObject();
				c.setObjectSeconds(CACHE_KEY, cdb, 5*60, false);
			}

			ByteBuffer bytes = cdb.find(ByteBuffer.wrap(encode(form, offset, length)));
			if (bytes != null && bytes.hasArray())
			{
				return CdbUtils.decode(bytes.array());
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return null;
	}
}
