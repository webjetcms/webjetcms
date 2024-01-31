package sk.iway.iwcm.system.fulltext;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.AdminTools;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.fulltext.indexed.Documents;
import sk.iway.iwcm.system.fulltext.indexed.Forums;
import sk.iway.iwcm.system.fulltext.indexed.Indexed;
import sk.iway.iwcm.system.fulltext.lucene.AnalyzerFactory;
import sk.iway.iwcm.system.fulltext.lucene.LuceneUtils;

/**
 * FulltextSearch.java
 *
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2011
 * @author $Author: jeeff thaber $
 * @version $Revision: 1.3 $
 * @created Date: 6.4.2011 17:56:22
 * @modified $Date: 2004/08/16 06:26:11 $
 */
public class FulltextSearch
{
	private static Map<String, SpellChecker> documentsSpellingDictionary = new Hashtable<>();

	public static void log(Class<?> c, String msg, Writer log)
	{
		if (log != null)
		{
			try
			{
				log.write(Tools.formatDateTimeSeconds(Tools.getNow())+" " + msg+"<br/>");
				log.flush();
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		Logger.debug(c, msg);
	}

	interface Callback
	{
		void callback();
	}

	public static void index()
	{
		index(null, null);
	}

	@SuppressWarnings({"deprecation","unchecked"})
	public static void index(Indexed indexed, Writer log)
	{
		List<Indexed> indexeds = new ArrayList<>();
		if (indexed != null)
		{
			indexeds.add(indexed);
		}
		else
		{
			indexeds.add(new Documents(AdminTools.defaultLanguage()));
			//indexeds.add(new Tickets()); JEEFF: zatial vypnute, nie je otestovane
			indexeds.add(new Forums());
		}
		IndexWriter writer = null;

		try
		{
			for (Indexed i : indexeds)
			{

				IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_31, AnalyzerFactory.getAnalyzer(Version.LUCENE_31,i.language()));


				if (indexed == null)
				{
					log(FulltextSearch.class, "Creating index", log);
				   /*reindexing the whole index*/
					config.setOpenMode(OpenMode.CREATE);
				}
				else
				{
					log(FulltextSearch.class, "Appending index", log);
					/*partial reindex*/
					config.setOpenMode(OpenMode.CREATE_OR_APPEND);
				}
				config.setRAMBufferSizeMB(64.0);
				writer = new IndexWriter(getIndexDirectory(i.language()), config);


				if (indexed != null)
				{
					log(FulltextSearch.class, "Deleting index data, type="+indexed.name(), log);
					writer.deleteDocuments(new Term("type",indexed.name()));
					writer.commit();
				}
				ComplexQuery query = new ComplexQuery().setSql(i.sql());
				query.setStreamingResultSet(true);
				int count = i.numberOfDocuments();

				final CountDownLatch latch = new CountDownLatch(count);

				i.setCallback(new Indexed.Callback()
				{
					@Override
					public void call()
					{
						Logger.debug(FulltextSearch.class, "count down call");
						latch.countDown();
					}
				});

				log(FulltextSearch.class, "Indexing " + count + " documents.", log);
				query.list(i.mapper(writer, log));
				latch.await();
				writer.commit();
				log(FulltextSearch.class, "Optimizing index.", log);
				writer.optimize();
				log(FulltextSearch.class, "Closing index.", log);
				writer.close();
				writer = null;
				i.close();
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			log(FulltextSearch.class, "ERROR: "+e.getMessage(), log);
		}

		if (writer != null)
		{
			try
			{
				writer.close();
			}
			catch (IOException e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public static synchronized Directory getIndexDirectory(String language) throws IOException
	{
		SimpleFSDirectory directory = (SimpleFSDirectory) Cache.getInstance().getObject("FulltextSearch.getIndexDirectory." + language);
		if (directory == null)
		{
			File indexDirectory;
			indexDirectory = new File(LuceneUtils.LUCENE_INDEX,language);
			if (!indexDirectory.exists())
			{
				indexDirectory.mkdirs();
			}
			directory = new SimpleFSDirectory(indexDirectory);
			Cache.getInstance().setObject("FulltextSearch.getIndexDirectory." + language, directory, 5);
		}
		return directory;
	}

	public static synchronized void updateSpellCheck(String language)
	{
		if (Constants.getBoolean("luceneUpdateSpellCheck") && !documentsSpellingDictionary.containsKey(language))
		{
			try
			{
				Directory indexDirectory = FulltextSearch.getIndexDirectory(language);
				SpellChecker spellChecker = new SpellChecker(indexDirectory);
				documentsSpellingDictionary.put(language, spellChecker);
				IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_31, AnalyzerFactory.getAnalyzer(Version.LUCENE_31, language));
				spellChecker.indexDictionary(new LuceneDictionary(IndexReader.open(indexDirectory), "data"), config, false);
			}
			catch (IOException e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
	}

	/**
	 * Vrati najblizsie podobne slovo
	 *
	 * @param textToFind
	 * @param language
	 * @return
	 */
	public static synchronized String[] suggestSimilar(String textToFind, String language)
	{
		String[] result = null;
		try
		{
			updateSpellCheck(language);

			SpellChecker sp = documentsSpellingDictionary.get(language);
			if (sp != null)
			{
				String[] suggestions = sp.suggestSimilar(textToFind, 1);
				return suggestions;
			}
		}
		catch (IOException e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return result;
	}

	/**
	 * Vrati mnozinu stopslov pre jazyk
	 *
	 * @param language
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static synchronized Set<String> stopwords(String language)
	{
		Set<String> fromCache = (Set<String>) Cache.getInstance().getObject("FulltextSearch.stopwords." + language);
		if (fromCache != null)
		{
			return fromCache;
		}
		List<String> stopwords = new SimpleQuery().forList("select word from stopword where language = ? ", language);
		Set<String> result = new HashSet<>(stopwords);
		Cache.getInstance().setObject("FulltextSearch.stopwords." + language, result, 5);
		return result;
	}
}
