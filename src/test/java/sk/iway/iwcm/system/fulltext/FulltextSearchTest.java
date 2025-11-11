package sk.iway.iwcm.system.fulltext;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.util.Version;
import org.junit.jupiter.api.Test;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.common.SearchTools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.system.fulltext.indexed.Documents;
import sk.iway.iwcm.system.fulltext.lucene.CustomAnalyzer;
import sk.iway.iwcm.system.fulltext.lucene.IndexSearcherBuilder;
import sk.iway.iwcm.system.fulltext.lucene.Lemmas;
import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.List;

/**
 * Test.java
 *
 *@Title webjet7
 *@Company Interway s.r.o. (www.interway.sk)
 *@Copyright Interway s.r.o. (c) 2001-2011
 *@author $Author: jeeff thaber $
 *@version $Revision: 1.3 $
 *@created Date: 5.5.2011 14:40:15
 *@modified $Date: 2004/08/16 06:26:11 $
 */
class FulltextSearchTest extends BaseWebjetTest
{
	public void shouldCreateSQL()
	{
		System.out.println(new Documents("sk").sql());
		assertNotEquals(0, new SimpleQuery().forList(new Documents("sk").sql()).size());
	}

	public void shouldTestIndexSpped()
	{
		for (int i = 0; i < 100; i++)
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					IndexSearcherBuilder.build("sk");
				}
			}).start();
		}
	}

	public void shouldSearch()
	{
		LuceneQuery query = new LuceneQuery(new Documents("sk"));
		List<Document> documents = query.documents("title:Fico");
		assertTrue(documents.size() > 0);
		for (Document document : documents)
		{
			System.out.println(document.getFieldable("title").stringValue());
		}
	}

	public void shouldTestLemmas()
	{
		try
		{
			// text to tokenize
			final String text = "Nahravam";
			CustomAnalyzer analyzer = new CustomAnalyzer(Version.LUCENE_31, "sk");
			TokenStream stream = analyzer.tokenStream("comments", new StringReader(text));
			// get the TermAttribute from the TokenStream
			CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

			//TermAttribute termAtt = stream.addAttribute(TermAttribute.class);
			stream.reset();
			// print all tokens until stream is exhausted
			while (stream.incrementToken())
			{
				System.out.println(termAtt.buffer());
			}
			stream.end();
			stream.close();
			analyzer.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	@Test
	void shouldIndex()
	{
		try
		{
			DBPool.getInstance();
			DBPool.jpaInitialize();

			DocDB.getInstance();
			TemplatesDB.getInstance();
			Logger.setWJLogLevel(Logger.DEBUG);
			FulltextSearch.index(new Documents("sk"),new PrintWriter(System.out));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
		}
	}

	public void shouldDohtmlToPlain()
	{
		String plain = SearchTools.htmlToPlain("<html><body> !INCLUDE(/components/text,1,2)! \n Lorem impsum c1 "
					+ "!INCLUDE(/components/text,3,4)! \n Lorem impsum c2  "
					+ "!INCLUDE(/components/text,5,8)! \n Lorem impsum c3  </body></html>");
		assertFalse(plain.contains("INCLUDE"));
		assertTrue(plain.contains("Lorem impsum c1"));
		assertTrue(plain.contains("Lorem impsum c2"));
		assertTrue(plain.contains("Lorem impsum c3"));
	}


	public void test() throws MalformedURLException{
		java.net.URL helpdesk = new java.net.URL("http://intra.iway.sk/helpdesk/?bugID=9191");
		System.out.println(helpdesk.getPath()+"?"+helpdesk.getQuery());
	}

	public void testSearch(){
		List<Document> list = new LuceneQuery("sk").documents("data:x");
		for (Document doc:list){
			System.out.println(doc.getFieldable("url").stringValue());
		}
	}

	public void testLemmas(){
		String a = "banky";
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		for (int i = 0;i<5000;i++){
			Lemmas.get("sk", a.toCharArray(), 0, a.length());
		}
		stopWatch.stop();
		System.out.println(stopWatch.getTime());
	}
}
