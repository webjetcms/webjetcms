package sk.iway.iwcm.system.fulltext.lucene;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.search.IndexSearcher;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.system.fulltext.FulltextSearch;

/**
 * IndexSearcherBuilder.java
 *
 *@Title webjet7
 *@Company Interway s.r.o. (www.interway.sk)
 *@Copyright Interway s.r.o. (c) 2001-2011
 *@author $Author: jeeff thaber $
 *@version $Revision: 1.3 $
 *@created Date: 14.4.2011 9:47:26
 *@modified $Date: 2004/08/16 06:26:11 $
 */
public class IndexSearcherBuilder
{
	private static Map<String, IndexSearcher> searchers = new HashMap<>();
	private static Map<String, Integer> uses = new HashMap<>();
	private static boolean shouldRefresh = false;

	protected IndexSearcherBuilder() {
		//utility class
	}

	/**
	 * Vytvori IndexSearcher
	 * @param indexed
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static IndexSearcher build(String langauge)
	{
		synchronized (searchers)
		{
			String key = langauge;
			boolean close = shouldRefresh; //toto sposobovalo problemy && (!uses.containsKey(key) || uses.get(key) == 0);

			if (uses.containsKey(key)) Logger.debug(IndexSearcherBuilder.class, "close="+close+" shouldRefresh="+shouldRefresh+" uses.get="+uses.get(key));

			if (close)
			{
				try
				{
					//musime pozatvarat vsetko, kedze shouldRefresh nenastavuje jazyk
					for (Entry<String, IndexSearcher> searcher : searchers.entrySet() )
					{
						try
						{
							//aby ked nam to padlo na jednom sme pokracovali dalej
							Logger.debug(IndexSearcherBuilder.class, "Closing index: "+searcher.getKey());
							searcher.getValue().close();
							searchers.remove(searcher.getKey());
							uses.remove(searcher.getKey());
						}
						catch (Exception e)
						{
							sk.iway.iwcm.Logger.error(e);
						}
					}
					shouldRefresh = false;
				}
				catch (Exception e)
				{
					//dost pruser, najbezpecnejsie je znova inicializovat mapy
					searchers = new HashMap<String, IndexSearcher>();
					uses = new HashMap<String, Integer>();

					sk.iway.iwcm.Logger.error(e);
				}
			}


			if (!searchers.containsKey(key))
			{
				try
				{
					searchers.put(key, new IndexSearcher(FulltextSearch.getIndexDirectory(langauge)));
				}
				catch (IOException e)
				{
					sk.iway.iwcm.Logger.error(e);
				}
			}
			if (uses.containsKey(key))
			{
				uses.put(key, uses.get(key) + 1);
			}
			else
			{
				uses.put(key,1);
			}
			return searchers.get(key);
		}
	}

	public static void refresh()
	{
		synchronized (searchers)
		{
			shouldRefresh = true;
		}
	}
	/**
	 * Zatvori IndexSearcher
	 * @param indexed
	 */
	public static void close(String language)
	{
		synchronized (searchers)
		{
			String key = language;
			if (uses.containsKey(key))
			{
				uses.put(key, uses.get(key) - 1);
			}
			else
			{
				uses.put(key, - 1);
			}
		}
	}
}