package sk.iway.iwcm.system.fulltext.indexed;

import static sk.iway.iwcm.system.fulltext.lucene.LuceneUtils.dateToLucene;
import static sk.iway.iwcm.system.fulltext.lucene.LuceneUtils.nvl;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import sk.iway.Html2Text;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.SearchTools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.system.fulltext.FulltextSearch;
import sk.iway.iwcm.system.fulltext.lucene.AnalyzerFactory;
import sk.iway.iwcm.system.fulltext.lucene.IndexSearcherBuilder;
import sk.iway.iwcm.system.fulltext.lucene.IndexingMapper;
import sk.iway.iwcm.system.fulltext.lucene.Lemmas;
import sk.iway.iwcm.system.fulltext.lucene.LuceneUtils;

/**
 * Documents.java
 *
 *@Title webjet7
 *@Company Interway s.r.o. (www.interway.sk)
 *@Copyright Interway s.r.o. (c) 2001-2011
 *@author $Author: jeeff thaber $
 *@version $Revision: 1.3 $
 *@created Date: 6.4.2011 18:04:19
 *@modified $Date: 2004/08/16 06:26:11 $
 */
public class Documents extends Indexed
{
	private static final String SELECTING_SQL = "SELECT * FROM documents WHERE available="+DB.getBooleanSql(true)+" and searchable="+DB.getBooleanSql(true)+" AND (external_link IS NULL OR external_link NOT LIKE '/files/protected%') ";
	private static final String TOTAL_SQL = "SELECT count(doc_id) FROM documents WHERE available="+DB.getBooleanSql(true)+" and searchable="+DB.getBooleanSql(true)+" AND (external_link IS NULL OR external_link NOT LIKE '/files/protected%') ";
	private String language;
	private Collection<String> paths;

	/**
	 *
	 */
	public Documents(final String language)
	{
		this.language = language;
		final GroupsDB gdb = GroupsDB.getInstance();
		final String defaultLanguage = Constants.getString("defaultLanguage");
		Collection<String> newPaths = CollectionUtils.collect(GroupsDB.getRootGroups(), new Transformer<GroupDetails, String>()
		{
			@Override
			public String transform(GroupDetails gd)
			{
				if ("system".equals(DB.internationalToEnglish(gd.getGroupName().toLowerCase())))
					return null;

				TemplateDetails template = TemplatesDB.getInstance().getTemplate(gd.getTempId());
				String tempLng = defaultLanguage;
				if (template != null) tempLng = template.getLng();
				if (Tools.isEmpty(tempLng)) tempLng = "sk";

				if (language.equals(tempLng))
					return gdb.getGroupNamePath(gd.getGroupId());

				return null;
			}
		});
		this.paths = CollectionUtils.select(newPaths, new Predicate<String>()
		{
			@Override
			public boolean evaluate(String path)
			{
				if (path != null)
					return true;
				return false;
			}
		});
	}

	public static String parseHeadings(String html) {

		StringBuilder headings = new StringBuilder();
		if (html != null && html.length()<30000000) {
			org.jsoup.nodes.Document jsoup = Jsoup.parse(html);
			Elements htags = jsoup.select("h1, h2, h3, h4, h5, h6");
			for (String heading : htags.eachText()) {
				if (headings.length()>0) headings.append(" ");
				headings.append(heading);
			}
		}
		return headings.toString();
	}

	/**
     * Vrati Document na zaklade dodaneho ResultSet
     *
     * @param rs
     * @throws SQLException
     */
    public static Document toLuceneDocument(ResultSet rs, String language) throws SQLException
    {
        Document document = new Document();
        try
        {
            int docId = rs.getInt("doc_id");
            Logger.debug(Documents.class, "toLuceneDocument, docid="+docId);

				int groupId = rs.getInt("group_id");
				int tempId = rs.getInt("temp_id");

				String docLng = getDocumentLanguage(groupId, tempId);
				if (language.equals(docLng)==false)
				{
					Logger.debug(Documents.class, docId+" nesedi jazyk doc="+docLng+" indexed language="+language);
					return null;
				}

            DocDB docDB = DocDB.getInstance();

            document.add(new Field("doc_id", nvl(rs.getString("doc_id")), Field.Store.YES, Field.Index.ANALYZED));
            Field title = new Field("title", nvl(rs.getString("title"))+" "+Lemmas.get(language, nvl(rs.getString("title"))), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
            title.setBoost(10.0f);
            document.add(title);

            //doplnit to co sa pridava do DataASC - fulltextIncludeKeywords a fulltextIncludePerex a fulltextIncludeAttributes
            String dataDB = rs.getString("data");
            if (dataDB != null) Logger.debug(Documents.class, "toLuceneDocument, dataDB size = "+dataDB.length());
            else Logger.debug(Documents.class, "toLuceneDocument, dataDB is null");

				//vykonaj !DOC_TITLE! znacky v texte
				dataDB = Tools.replace(dataDB, "!DOC_TITLE!", nvl(rs.getString("title")));

            String plainText;
            if (dataDB.length()<2000000) plainText = SearchTools.htmlToPlain(dataDB);
            else if (dataDB.length()<30000000) plainText = Html2Text.html2text(dataDB);
            else plainText = dataDB;

            DocDetails doc = docDB.getDoc(docId);
            EditorForm ef = new EditorForm(doc);

            String data = EditorDB.getDataAsc(plainText, ef, true);

            String htmlData = SearchTools.htmlToPlain(rs.getString("html_data"));

			Field headings = new Field("headings", Lemmas.get(language, parseHeadings(dataDB)), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
			headings.setBoost(5.0f);

			document.add(headings);
            document.add(new Field("data", nvl(data), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
            document.add(new Field("external_link", nvl(rs.getString("external_link")), Field.Store.NO, Field.Index.ANALYZED));
            document.add(new Field("date_created", nvl(dateToLucene(rs.getTimestamp("date_created"))), Field.Store.YES, Field.Index.ANALYZED));

            Timestamp publishStart = rs.getTimestamp("publish_start");
            if (publishStart == null) publishStart = rs.getTimestamp("date_created");

            document.add(new Field("publish_start", nvl(dateToLucene(publishStart)), Field.Store.YES,	Field.Index.ANALYZED));
            document.add(new Field("publish_end", nvl(dateToLucene(rs.getTimestamp("publish_end"))), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new NumericField("author_id", Field.Store.YES, true).setIntValue(rs.getInt("author_id")));
            document.add(new NumericField("group_id", Field.Store.NO, true).setIntValue(rs.getInt("group_id")));
            document.add(new NumericField("temp_id", Field.Store.NO, true).setIntValue(rs.getInt("temp_id")));
            document.add(new Field("file_name", nvl(GroupsDB.getInstance().getGroupNamePath(rs.getInt("group_id"))), Field.Store.NO, Field.Index.ANALYZED));
            document.add(new NumericField("sort_priority", Field.Store.YES, true).setIntValue(rs.getInt("sort_priority"))); //store.yes potrebujeme kvoli sortovaniu
            addMultiValueField(rs.getString("password_protected"), "password_protected", document);
            document.add(new Field("html_head", nvl(rs.getString("html_head")), Field.Store.NO, Field.Index.ANALYZED));
            document.add(new Field("html_data", nvl(htmlData), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("perex_place", nvl(rs.getString("perex_place")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("perex_image", nvl(rs.getString("perex_image")), Field.Store.YES, Field.Index.ANALYZED));
            addStringValueField(rs.getString("perex_group"), "perex_group", document);
            document.add(new Field("event_date", nvl(dateToLucene(rs.getTimestamp("event_date"))), Field.Store.YES, Field.Index.ANALYZED));

			//this are in BasicDocDetails
            document.add(new Field("field_a", nvl(rs.getString("field_a")), Field.Store.NO, Field.Index.ANALYZED));
            document.add(new Field("field_b", nvl(rs.getString("field_b")), Field.Store.NO, Field.Index.ANALYZED));
            document.add(new Field("field_c", nvl(rs.getString("field_c")), Field.Store.NO, Field.Index.ANALYZED));

            document.add(new Field("field_d", nvl(rs.getString("field_d")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("field_e", nvl(rs.getString("field_e")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("field_f", nvl(rs.getString("field_f")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("field_g", nvl(rs.getString("field_g")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("field_h", nvl(rs.getString("field_h")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("field_i", nvl(rs.getString("field_i")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("field_j", nvl(rs.getString("field_j")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("field_k", nvl(rs.getString("field_k")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("field_l", nvl(rs.getString("field_l")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("field_m", nvl(rs.getString("field_m")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("field_n", nvl(rs.getString("field_n")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("field_o", nvl(rs.getString("field_o")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("field_p", nvl(rs.getString("field_p")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("field_q", nvl(rs.getString("field_q")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("field_r", nvl(rs.getString("field_r")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("field_s", nvl(rs.getString("field_s")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("field_t", nvl(rs.getString("field_t")), Field.Store.YES, Field.Index.ANALYZED));

			/* 	uncommend this when you will do some refactor which will require Lucene reindexing
			    also uncomment multiDomainEnabled/root_group_l1 condition in LuceneSearchAction
			document.add(new NumericField("root_group_l1", Field.Store.YES, true).setIntValue(rs.getInt("root_group_l1")));
			document.add(new NumericField("root_group_l2", Field.Store.YES, true).setIntValue(rs.getInt("root_group_l2")));
			document.add(new NumericField("root_group_l3", Field.Store.YES, true).setIntValue(rs.getInt("root_group_l3")));
			*/

            document.add(typeField("documents"));
            String url = DocDB.getInstance().getDocLink(rs.getInt("doc_id"));
            document.add(urlField(url));

            Logger.debug(Documents.class, "toLuceneDocument DONE, docid="+rs.getString("doc_id"));
        }
        catch (Exception e)
        {
            sk.iway.iwcm.Logger.error(e);
        }
        return document;
    }

	public static void addMultiValueField(String value, String name, Document document)
    {
        if (Tools.isNotEmpty(value))
        {
            for (String part : value.split(","))
            {
                if (Tools.isNotEmpty(part))
                {
                    document.add(new NumericField(name, Field.Store.YES, true).setIntValue(Integer.parseInt(part)));
                }
            }
        }
        else
        {
            //document.add(new Field(name, LuceneUtils.EMPTY, Field.Store.YES, Field.Index.ANALYZED));
            //jeeff: nemozeme kombinovat numeric a non numeric fieldy, zle to potom hlada
            document.add(new NumericField(name, Field.Store.YES, true).setIntValue(0));
        }
    }

	private static void addStringValueField(String value, String name, Document document)
    {
        if (Tools.isNotEmpty(value))
        {
            for (String part : value.split(","))
            {
                if (Tools.isNotEmpty(part))
                {
                    document.add(new Field(name, part, Field.Store.YES, Field.Index.ANALYZED));
                }
            }
        }
        else
        {
            document.add(new Field(name, LuceneUtils.EMPTY, Field.Store.YES, Field.Index.ANALYZED));
        }
    }

	public static Field typeField(String indexed){
        return new Field("type",indexed,Field.Store.YES,
                    Field.Index.ANALYZED);
    }

	public static Field urlField(String url){
        return new Field("url",nvl(url),Field.Store.YES,
                    Field.Index.NO);
    }

	@Override
	public String language()
	{
		return this.language;
	}

	private ExecutorService executor = Executors.newFixedThreadPool(5);// indexWriter umoznuje pristup max 5 vlakien naraz, opravene je to az v 4.0
	private BlockingQueue<Document> queue = new LinkedBlockingQueue<>(50);
	private WriterThread save = new WriterThread();

	static final class DocumentsIndexingMapper extends IndexingMapper {

		private Documents documents;

		/**
		 * @param writer
		 */
		public DocumentsIndexingMapper(IndexWriter writer, Documents documents, Writer log)
		{
			super(writer,log);
			this.documents = documents;
			documents.save.setIndexed(documents);
			documents.save.setQueue(documents.queue);
			documents.save.setWriter(writer);
			documents.save.setLog(log);
		}

		@Override
		public Void map(ResultSet rs) throws SQLException
		{
			try
			{
				final Document document = toLuceneDocument(rs, documents.language);
				if (document != null) documents.queue.put(document);
				documents.executor.execute(documents.save);
			}
			catch (SQLException|InterruptedException e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
			return null;
		}
	}

	static final class WriterThread implements Runnable {

		private IndexWriter writer;
		private BlockingQueue<Document> queue;
		private Indexed indexed;
		private Writer log;



		/**
		 * @param indexed The indexed to set.
		 */
		public void setIndexed(Indexed indexed)
		{
			this.indexed = indexed;
		}


		/**
		 * @param log The log to set.
		 */
		public void setLog(Writer log)
		{
			this.log = log;
		}

		/**
		 * @param writer The writer to set.
		 */
		public void setWriter(IndexWriter writer)
		{
			this.writer = writer;
		}

		/**
		 * @param queue The queue to set.
		 */
		public void setQueue(BlockingQueue<Document> queue)
		{
			this.queue = queue;
		}

		int count = 0;
		@Override
		public void run()
		{
			try
			{
				Thread.currentThread().setName("Documents");
				Document document = queue.poll(100, TimeUnit.MILLISECONDS);
				if (document != null)
				{
					writer.addDocument(document);
					count++;
					if (count % 100 == 0)
					{
						FulltextSearch.log(Documents.class, "Indexed " + count + " documents, last doc_id="+document.get("doc_id")+" title="+document.get("title"), log);
					}
				}
			}
			catch (IOException|InterruptedException e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
			finally{
				indexed.proccessed();
			}
		}
	}


	@Override
	public IndexingMapper mapper(IndexWriter writer,Writer log)
	{
		return new DocumentsIndexingMapper(writer, this,log);
	}

	private String pathsLikeString()
	{
		StringBuilder pathsLike = new StringBuilder();
		if (paths.size() > 0)
		{
			String prefix = " AND ( ";
			pathsLike.append(prefix);
			for (String p : paths)
			{
				// not the first
				if (pathsLike.length() != prefix.length())
				{
					pathsLike.append(" OR ");
				}
				pathsLike.append(" file_name like '");
				pathsLike.append(p);
				pathsLike.append("%'");
			}
			pathsLike.append(" ) ");
		}
		return pathsLike.toString();
	}

	/* THA verzia
	private static Document toLuceneDocument(ResultSet rs) throws SQLException
	{
		Document document = new Document();
		try
		{
			DocDB docDB = DocDB.getInstance();
			document.add(new Field("doc_id", nvl(rs.getString("doc_id")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new NumericField("temp_id", Field.Store.NO, true).setIntValue(rs.getInt("temp_id")));
			document.add(new NumericField("sort_priority", Field.Store.NO, true).setIntValue(rs.getInt("sort_priority")));
			document.add(new NumericField("group_id", Field.Store.YES, true).setIntValue(rs.getInt("group_id")));
			document.add(new Field("html_head", nvl(rs.getString("html_head")), Field.Store.NO, Field.Index.ANALYZED));

			//doplnit to co sa pridava do DataASC - fulltextIncludeKeywords a fulltextIncludePerex a fulltextIncludeAttributes

			StringBuilder data = new StringBuilder(rs.getString("data"));

			String perexGroup = rs.getString("perex_group");
			if (Constants.getBoolean("fulltextIncludeKeywords") && Tools.isNotEmpty(perexGroup))
			{
				String perexGroupIds[] = Tools.getTokens(perexGroup, ",");
				if (perexGroupIds != null)
				{
					String keywords = null;
					for (String keyword : perexGroupIds)
					{
						keyword = docDB.convertPerexGroupIdToName(Tools.getIntValue(keyword, -1));
						if (Tools.isEmpty(keyword) || keyword.startsWith("#")) continue;
						if (keywords == null) keywords = keyword;
						else keywords += ", "+keyword;
					}
					if (Tools.isNotEmpty(keywords))
					{
						data.append(" ").append(DB.internationalToEnglish(keywords).trim().toLowerCase());
					}
				}
			}

			String htmlData = SearchAction.htmlToPlain(rs.getString("html_data"));
			if (Constants.getBoolean("fulltextIncludePerex"))
			{
				data.append(" ").append(htmlData);
			}

			//ak treba dopln atributy
			if (Constants.getBoolean("fulltextIncludeAttributes"))
			{
				try
				{
					List<AtrBean> attrs = AtrDB.getAtributes(rs.getInt("doc_id"), null, null);	//ziskam vsetky atributy pre danu stranku
					if (attrs != null && attrs.size() > 0)
					{
						String attributes = null;
						for (AtrBean attr : attrs)
						{
							if (attr == null) continue;
							if(Tools.isNotEmpty(attr.getAtrName()) && Tools.isNotEmpty(attr.getValueString()))
							{
								if (attributes == null) attributes = attr.getAtrName() + "=" + attr.getValueString();
								else attributes += ", "+attr.getAtrName() + "=" + attr.getValueString();
							}
						}

						if (Tools.isNotEmpty(attributes))
						{
							data.append(" ").append(DB.internationalToEnglish(attributes).trim().toLowerCase());
						}
					}
				} catch (Exception ex) {}
			}

			document.add(new Field("data", nvl(data.toString()), Field.Store.YES, Field.Index.ANALYZED,
						TermVector.WITH_POSITIONS_OFFSETS));
			document.add(new Field("html_data", nvl(htmlData), Field.Store.YES,
						Field.Index.ANALYZED));
			document.add(new Field("field_a", nvl(rs.getString("field_a")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("field_b", nvl(rs.getString("field_b")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("field_c", nvl(rs.getString("field_c")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("field_d", nvl(rs.getString("field_d")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("field_e", nvl(rs.getString("field_e")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("field_f", nvl(rs.getString("field_f")), Field.Store.NO, Field.Index.ANALYZED));
			document.add(new Field("field_g", nvl(rs.getString("field_g")), Field.Store.NO, Field.Index.ANALYZED));
			document.add(new Field("field_h", nvl(rs.getString("field_h")), Field.Store.NO, Field.Index.ANALYZED));
			document.add(new Field("field_i", nvl(rs.getString("field_i")), Field.Store.NO, Field.Index.ANALYZED));
			document.add(new Field("field_j", nvl(rs.getString("field_j")), Field.Store.NO, Field.Index.ANALYZED));
			document.add(new Field("field_k", nvl(rs.getString("field_k")), Field.Store.NO, Field.Index.ANALYZED));
			document.add(new Field("field_l", nvl(rs.getString("field_l")), Field.Store.NO, Field.Index.ANALYZED));
			document.add(new Field("publish_start", nvl(dateToLucene(rs.getDate("publish_start"))), Field.Store.YES,	Field.Index.ANALYZED));
			document.add(new Field("publish_end", nvl(dateToLucene(rs.getDate("publish_end"))), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("perex_place", nvl(rs.getString("perex_place")), Field.Store.YES, Field.Index.ANALYZED));
			Field title = new Field("title", nvl(rs.getString("title")), Field.Store.YES, Field.Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS);
			title.setBoost(2.0f);
			document.add(title);
			document.add(new Field("date_created", nvl(dateToLucene(rs.getDate("date_created"))), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("file_name", nvl(GroupsDB.getInstance().getGroupNamePath(rs.getInt("group_id"))), Field.Store.NO, Field.Index.ANALYZED));
			document.add(new Field("external_link", nvl(rs.getString("external_link")), Field.Store.YES, Field.Index.ANALYZED));
			addMultiValueField(rs.getString("password_protected"), "password_protected", document);
			addStringValueField(rs.getString("perex_group"), "perex_group", document);
			document.add(typeField("documents"));
			String url = DocDB.getInstance().getDocLink(rs.getInt("doc_id"));
			document.add(urlField(url));
		}
		catch (Exception e){
			sk.iway.iwcm.Logger.error(e);
		}
		return document;
	}
	*/


	static int  count = 0;
	@Override
	public String sql()
	{
		return SELECTING_SQL + pathsLikeString();
	}

	@Override
	public String name()
	{
		return "documents";
	}


	@Override
	public int numberOfDocuments()
	{
		return new SimpleQuery().forInt(TOTAL_SQL + pathsLikeString());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see sk.iway.iwcm.system.fulltext.Indexed#close()
	 */
	@Override
	public void close()
	{
		executor.shutdownNow();
	}

	/**
	 * Obnovi v indexe dokument
	 *
	 * @param docId
	 */
	public static void updateSingleDocument(int docId)
	{
		if (Constants.getBoolean("luceneIncrementalDocumentIndexing"))
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			IndexWriter writer = null;
			try
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("SELECT * FROM documents WHERE doc_id = ? ");
				ps.setInt(1, docId);
				rs = ps.executeQuery();
				if (rs.next())
				{
					int groupId = rs.getInt("group_id");
					int tempId = rs.getInt("temp_id");
					String lng = getDocumentLanguage(groupId, tempId);

					IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_31, AnalyzerFactory.getAnalyzer(Version.LUCENE_31, lng));
					config.setOpenMode(OpenMode.APPEND);
					writer = new IndexWriter(FulltextSearch.getIndexDirectory(lng), config);
					if(rs.getBoolean("available") == false || rs.getBoolean("searchable") == false || isProtectedFile(rs.getString("external_link")))
					{
						writer.deleteDocuments(new Term("doc_id", Integer.toString(docId)));
					}
					else
					{
						Document luceneDoc = toLuceneDocument(rs, lng);
						if (luceneDoc == null) writer.deleteDocuments(new Term("doc_id", Integer.toString(docId)));
						else writer.updateDocument(new Term("doc_id", Integer.toString(docId)), luceneDoc);
					}
					writer.commit();
					writer.close();
					writer = null;
					IndexSearcherBuilder.refresh();
				}
				rs.close();
				ps.close();
				db_conn.close();
				rs = null;
				ps = null;
				db_conn = null;
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
			finally
			{
				try
				{
					if (rs != null)
						rs.close();
					if (ps != null)
						ps.close();
					if (db_conn != null)
						db_conn.close();
				}
				catch (Exception ex2)
				{
				}
				try
				{
					if (writer != null) writer.close();
				}
				catch (Exception e)
				{
				}
			}
		}


	}

	/**
	 * Zmaze dokument z indexu
	 *
	 * @param docId
	 */
	public static void deleteSingleDocument(int docId)
	{
		if (Constants.getBoolean("luceneIncrementalDocumentIndexing"))
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("SELECT * FROM documents WHERE doc_id = ? ");
				ps.setInt(1, docId);
				rs = ps.executeQuery();
				while (rs.next())
				{
					int tempId = GroupsDB.getInstance().getGroup(rs.getInt("group_id")).getTempId();
					String lng = TemplatesDB.getInstance().getTemplate(tempId).getLng();
					IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_31, AnalyzerFactory.getAnalyzer(Version.LUCENE_31, lng));
					config.setOpenMode(OpenMode.APPEND);
					IndexWriter writer = new IndexWriter(FulltextSearch.getIndexDirectory(lng), config);
					writer.deleteDocuments(new Term("doc_id", Integer.toString(docId)));
					writer.commit();
					writer.close();
					IndexSearcherBuilder.refresh();
				}
				rs.close();
				ps.close();
				db_conn.close();
				rs = null;
				ps = null;
				db_conn = null;
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
			finally
			{
				try
				{
					if (rs != null)
						rs.close();
					if (ps != null)
						ps.close();
					if (db_conn != null)
						db_conn.close();
				}
				catch (Exception ex2)
				{
				}
			}
		}
	}

	/**
	 * Vrati jazyk dokumentu pre zadane id priecinku a sablony
	 * @param groupId
	 * @param tempId
	 * @return
	 */
	private static String getDocumentLanguage(int groupId, int tempId)
	{
		GroupDetails group = GroupsDB.getInstance().getGroup(groupId);
		TemplateDetails temp = TemplatesDB.getInstance().getTemplate(tempId);
		if (group != null && Tools.isNotEmpty(group.getLng()))
		{
			return group.getLng();
		}
		if (temp != null && Tools.isNotEmpty(temp.getLng()))
		{
			return temp.getLng();
		}
		String defaultLanguage = Constants.getString("defaultLanguage");
		if (Tools.isEmpty(defaultLanguage)) defaultLanguage = "sk";

		return defaultLanguage;
	}

	private static boolean isProtectedFile(String externalLink) {
		if (Tools.isNotEmpty(externalLink) && externalLink.startsWith("/files/protected")) return true;
		return false;
	}
}
