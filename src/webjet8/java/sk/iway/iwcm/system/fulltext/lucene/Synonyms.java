package sk.iway.iwcm.system.fulltext.lucene;

import static sk.iway.iwcm.system.fulltext.cdb.CdbUtils.encode;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import io.github.duckasteroid.cdb.Cdb;

import org.apache.commons.pool.impl.GenericObjectPool;

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
@SuppressWarnings("rawtypes")
public class Synonyms
{
	private static final Map<String, GenericObjectPool> pools = new HashMap<>();

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
	@SuppressWarnings("unchecked")
	public static char[] get(String language, char[] form,int offset,int length)
	{
		synchronized (pools)
		{
			if (!pools.containsKey(language))
			{
				GenericObjectPool pool = new GenericObjectPool(new CdbFactory(language,CdbFactory.Type.Synonyms));
				pools.put(language, pool);
			}
			try
			{
				GenericObjectPool pool = pools.get(language);
				Cdb cdb = (Cdb)pool.borrowObject();
				ByteBuffer bytes = cdb.find(ByteBuffer.wrap(encode(form, offset, length)));

				pool.returnObject(cdb);
				if (bytes != null && bytes.hasArray())
				{
					return CdbUtils.decode(bytes.array());
				}
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		return null;
	}
}
