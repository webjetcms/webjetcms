package sk.iway.iwcm.system.fulltext.indexed;

import static sk.iway.iwcm.system.fulltext.lucene.LuceneUtils.nvl;

import java.io.Writer;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexWriter;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.common.AdminTools;
import sk.iway.iwcm.system.fulltext.lucene.IndexingMapper;
import sk.iway.iwcm.system.fulltext.lucene.LuceneUtils;


/**
 * Trieda
 * Indexed.java
 *
 *@Title webjet7
 *@Company Interway s.r.o. (www.interway.sk)
 *@Copyright Interway s.r.o. (c) 2001-2011
 *@author $Author: jeeff thaber $
 *@version $Revision: 1.3 $
 *@created Date: 6.4.2011 18:04:40
 *@modified $Date: 2004/08/16 06:26:11 $
 */
public abstract class Indexed
{
	/**
	 * vracia SQL vracajúce dokumenty. ktoré chcem indexovat
	 * @return
	 */
	public abstract String sql();
	/**
	 * vracia IndexingMapper, ktorý spracuje dodaný ResultSet a zapíše ho do dodaného writer-a
	 * @param writer
	 * @return
	 */
	public abstract IndexingMapper mapper(IndexWriter writer, Writer log);
	/**
	 * Vracia názov slúžiaci na identifikáciu dokumentu, používa sa pri vytváraní adresárov
	 * @return
	 */
	public abstract String name();
	/**
	 * Vracia názov poľa dokumentu, v ktorom sa bude vyhľadávať ak výraz neobsahuje pole v ktorom sa má hľadať
	 * @return
	 */
	public static String defaultField(){
		return "data";
	}

	public static String titleField(){
		return "title";
	}
	/**
	 * Počet všetkých dokumentov
	 * @return
	 */
	public abstract int numberOfDocuments();

	/**
	 * Jazyk indexovaných dokumentov
	 * @return
	 */
	public abstract String language();

	protected Callback callback;

	public static interface Callback{
		public void call();
	}

	/**
	 * @param callback The callback to set.
	 */
	public void setCallback(Callback callback)
	{
		this.callback = callback;
	}


	int numberOfIndexedDocuments = 0;

	public void proccessed()
	{
		callback.call();
		numberOfIndexedDocuments++;
		if (numberOfIndexedDocuments % 1000 == 0){
			Logger.println(this.getClass(), String.format("Indexed %d documents.",numberOfIndexedDocuments));
		}


	}

	public abstract void close();

	/**
	 * @deprecated - use AdminTools.defaultLanguage
	 * @return
	 */
	@Deprecated
	public static String defaultLanguage(){
		return AdminTools.defaultLanguage();
	}

	protected static Field urlField(String url){
		return new Field("url",nvl(url),Field.Store.YES,
					Field.Index.NO);
	}

	protected static Field titleField(String title){
		return new Field("title",LuceneUtils.nonNull(title),Field.Store.YES,
					Field.Index.ANALYZED,TermVector.WITH_POSITIONS_OFFSETS);
	}


}

