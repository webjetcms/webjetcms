package sk.iway.iwcm.system.fulltext.indexed;

import static sk.iway.iwcm.system.fulltext.lucene.LuceneUtils.dateToLucene;
import static sk.iway.iwcm.system.fulltext.lucene.LuceneUtils.nvl;

import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.Version;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.AdminTools;
import sk.iway.iwcm.common.SearchTools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.fulltext.FulltextSearch;
import sk.iway.iwcm.system.fulltext.lucene.AnalyzerFactory;
import sk.iway.iwcm.system.fulltext.lucene.IndexSearcherBuilder;
import sk.iway.iwcm.system.fulltext.lucene.IndexingMapper;
import sk.iway.iwcm.system.fulltext.lucene.LuceneUtils;

/**
 * Forums.java
 *
 *@Title webjet7
 *@Company Interway s.r.o. (www.interway.sk)
 *@Copyright Interway s.r.o. (c) 2001-2011
 *@author $Author: jeeff thaber $
 *@version $Revision: 1.3 $
 *@created Date: 1.6.2011 13:04:21
 *@modified $Date: 2004/08/16 06:26:11 $
 */
public class Forums extends Indexed
{
	private static final String JOIN_CLAUSE = " join documents d on (d.doc_id = df.doc_id) where df.confirmed=1 AND df.deleted=0 ";

	@Override
	public void close()
	{
		//
	}

	@Override
	public String language()
	{
		return defaultLangauge();
	}

	private static String defaultLangauge()
	{
		return Tools.isNotEmpty(Constants.getString("defaultLanguage"))?Constants.getString("defaultLanguage"):"sk";
	}

	@Override
	public IndexingMapper mapper(IndexWriter writer, Writer log)
	{
		return new IndexingMapper(writer, log)
		{
			int count = 0;
			@Override
			public Void map(ResultSet rs) throws SQLException
			{
				try
				{
					writer.addDocument(toLuceneDocument(rs));
					//proccessed();

					//if (count % 100 == 0)
					{
						FulltextSearch.log(Forums.class, "Indexed " + count + " forums.", log);
					}

					count++;
					proccessed();
				}
				catch (Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
				}
				return null;
			}
		};
	}

	@Override
	public String name()
	{
		return "forums";
	}

	@Override
	public int numberOfDocuments()
	{
		return new SimpleQuery().forInt("SELECT count(*) FROM document_forum df " + JOIN_CLAUSE);
	}

	private static Document toLuceneDocument(ResultSet rs)
	{
		Document document = new Document();

		DocDB docDB = DocDB.getInstance();

		try
		{
			document.add(new Field("forum_id", nvl(rs.getString("forum_id")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("doc_id", nvl(rs.getString("doc_id")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("parent_id", nvl(rs.getString("parent_id")), Field.Store.YES, Field.Index.ANALYZED));
			String title = rs.getString("subject");
			document.add(new Field("subject", nvl(title), Field.Store.YES, Field.Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));

			String question = SearchTools.htmlToPlain(rs.getString("question"));
			document.add(new Field("question", nvl(question), Field.Store.YES, Field.Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
			document.add(new Field("author_name", nvl(rs.getString("author_name")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("author_email", nvl(rs.getString("author_email")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("flag", nvl(rs.getString("flag")), Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("user_id", nvl(rs.getString("user_id")), Field.Store.YES, Field.Index.ANALYZED));
			//document.add(new NumericField("user_id", Field.Store.YES, true).setIntValue(rs.getInt("user_id")));
			//Logger.debug(Forums.class,"user_id stored value: "+ new NumericField("user_id", Field.Store.YES, true).setIntValue(rs.getInt("user_id")).stringValue());
			document.add(new Field("question_date", dateToLucene(rs.getDate("question_date")), Field.Store.YES, Field.Index.ANALYZED));//FIXME: prerobit na numeric field a serach na datumy na numericrangequery

			String data = rs.getString("title");
			if (Tools.isNotEmpty(data)) data = data+" "+question;
			else data = question;

			document.add(new Field("data", LuceneUtils.nonNull(data), Field.Store.YES, Field.Index.ANALYZED,TermVector.WITH_POSITIONS_OFFSETS));
			document.add(Documents.typeField("forums"));

			DocDetails doc = docDB.getBasicDocDetails(rs.getInt("doc_id"), false);
			if (doc != null)
			{
				String url = DocDB.getInstance().getDocLink(doc.getDocId());

				int parentId = rs.getInt("parent_id");
				if (parentId > 0) url = Tools.addParameterToUrlNoAmp(url, "pId", String.valueOf(parentId));
				url = Tools.addParameterToUrlNoAmp(url, "hfid", String.valueOf(rs.getInt("forum_id")));
				url = url+"#post"+rs.getInt("forum_id");

				Logger.debug(Forums.class, "Setting URL: "+url);
				document.add(Documents.urlField(url));
				if (Tools.isEmpty(title))
				{
					title = DocDB.getInstance().getDoc(rs.getInt("doc_id")).getTitle();
				}

				document.add(new Field("file_name", nvl(GroupsDB.getInstance().getGroupNamePath(doc.getGroupId())), Field.Store.NO, Field.Index.ANALYZED));

				Documents.addMultiValueField(doc.getPasswordProtected(), "password_protected", document);

				document.add(new NumericField("group_id", Field.Store.NO, true).setIntValue(doc.getGroupId()));
			}

			document.add(AdminTools.titleField(title));
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return document;
	}

	@Override
	public String sql()
	{
		return "SELECT df.*,d.title FROM document_forum df "+JOIN_CLAUSE;
	}

	public static void updateSingleQuestion(int forumId)
	{
		if (Constants.getBoolean("luceneIncrementalForumIndexing"))
		{
			Logger.debug(Forums.class, "updateSingleQuestion, forumId="+forumId);

			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("SELECT df.*,d.title FROM document_forum df "+JOIN_CLAUSE+" and df.forum_id = ? ");
				ps.setInt(1, forumId);
				rs = ps.executeQuery();

				IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_31, AnalyzerFactory.getAnalyzer(Version.LUCENE_31,defaultLangauge()));
				config.setOpenMode(OpenMode.APPEND);
				IndexWriter writer = new IndexWriter(FulltextSearch.getIndexDirectory(defaultLangauge()), config);

				if (rs.next())
				{
					writer.updateDocument(new Term("forum_id", Integer.toString(forumId)), toLuceneDocument(rs));
				}
				else
				{
					writer.deleteDocuments(new Term("forum_id", Integer.toString(forumId)));
				}

				writer.commit();
				writer.close();
				IndexSearcherBuilder.refresh();

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
}
