package sk.iway.iwcm.system.fulltext.lucene;

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
	//private static final Map<String, GenericObjectPool> pools = new HashMap<String, GenericObjectPool>();

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
		/*synchronized (pools)
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
				byte[] bytes = cdb.find(encode(form, offset, length));
				pool.returnObject(cdb);
				if (bytes != null)
				{
					return CdbUtils.decode(bytes);
				}
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}*/
		return null;
	}
}
