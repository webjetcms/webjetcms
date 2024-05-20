
package sk.iway.iwcm.forum;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.lucene.document.Document;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.forum.jpa.DocForumEntity;
import sk.iway.iwcm.components.forum.jpa.ForumGroupEntity;
import sk.iway.iwcm.components.forum.rest.DocForumService;
import sk.iway.iwcm.components.forum.rest.ForumGroupService;
import sk.iway.iwcm.components.rating.jpa.RatingEntity;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.fulltext.LuceneQuery;
import sk.iway.iwcm.system.fulltext.indexed.Forums;
import sk.iway.iwcm.system.fulltext.lucene.LuceneUtils;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 * Diskusne forum
 *
 * @Title WebJET
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2002
 * @author unascribed
 * @version 1.0
 * @created Sobota, 2002, december 21
 * @modified $Date: 2003/11/11 15:25:21 $
 */
public class ForumDB
{
	protected ForumDB() {
		//utility class
	}

	public static List<DocForumEntity> getForumFieldsForDoc(HttpServletRequest request, int doc_id)
	{
		return (getForumFieldsForDoc(request, doc_id, true, 0, true));
	}

	public static List<DocForumEntity> getForumFieldsForDoc(HttpServletRequest request, int doc_id, boolean onlyConfirmed)
	{
		return (getForumFieldsForDoc(request, doc_id, onlyConfirmed, 0));
	}

	/**
	 * vrati zoznam vsetkych otazok pre dane docId
	 *
	 * @param request
	 * @param doc_id
	 * @param onlyConfirmed
	 *            - ak true, zo zoznamu vyhodi nepotvrdene
	 * @return
	 */
	public static List<DocForumEntity> getForumFieldsForDoc(HttpServletRequest request, int doc_id, boolean onlyConfirmed, int parentId)
	{
		return (getForumFieldsForDoc(request, doc_id, onlyConfirmed, parentId, true, false));
	}

	public static List<DocForumEntity> getForumFieldsForDoc(HttpServletRequest request, int doc_id, boolean onlyConfirmed, int parentId, boolean sortAscending)
	{
		return getForumFieldsForDoc(request, doc_id, onlyConfirmed, parentId, sortAscending, false);
	}

	public static List<DocForumEntity> getForumFieldsForDoc(HttpServletRequest request, int doc_id, boolean onlyConfirmed, int parentId, boolean sortAscending, boolean showDeleted) {
		return DocForumService.getForumFieldsForDoc(request, doc_id, onlyConfirmed, parentId, sortAscending, showDeleted, false);
	}

	/**
	 * vrati ForumDetails pre zadane forumId
	 *
	 * @param forumId
	 * @return
	 */
	public static DocForumEntity getForumBean(HttpServletRequest request, int forumId)
	{
		return (getForumBean(request, forumId, true));
	}

	public static DocForumEntity getForumBean(HttpServletRequest request, int forumId, boolean sortAscending) {
		//HttpServletRequest request - NOT USED, probably historicly for back compatibility

		return DocForumService.getForumBean(forumId, sortAscending);
	}

	/**
	 * Vrati posledne zadany prispevok zadaneho fora
	 *
	 * @param doc_id
	 *            - id web stranky / fora
	 * @return
	 */
	public static DocForumEntity getLastMessage(int doc_id)
	{
		return DocForumService.getLastMessage(doc_id);
	}

	/**
	 * Rekurzivna metoda. Vymaze zadany prispevok a jeho odpovede z DB.
	 *
	 * @param forumId
	 *            - id prispevku
	 * @return
	 */
	public static boolean deleteMessage(int forumId, int docId, Identity user) {
		return DocForumService.docForumRecursiveAction(DocForumService.ActionType.DELETE, forumId, docId, user);
	}

	/**
	 * vrati ForumGroupBean pre zadane docId - data z tab. forum, ak neexistuje
	 * definicia, vrati defaultnu
	 *
	 * @param docId
	 *            - stranka, ku ktorej je pridane forum
	 * @return
	 */
	public static ForumGroupEntity getForum(int docId)
	{
		return getForum(docId, false);
	}

	/**
	 * vrati ForumGroupBean pre zadane docId - data z tab. forum
	 *
	 * @param docId - id stranky
	 * @param returnNull - ak je nastavene na false a zaznam neexistuje, vrati default zaznam
	 * @return
	 */
	public static ForumGroupEntity getForum(int docId, boolean returnNull) {
		return ForumGroupService.getForum(docId, returnNull);
	}

	/**
	 * Potvrdi prispevok diskusie
	 *
	 * @param forumId
	 * @return
	 */
	public static boolean forumApprove(int forumId, int docId, Identity user)
	{
		return DocForumService.docForumRecursiveAction(DocForumService.ActionType.APPROVE, forumId, docId, user);
	}

	/**
	 * Vrati zoznam schvalenych podtem pre dane forum
	 *
	 * @param docId
	 * @return
	 */
	public static List<DocForumEntity> getForumTopics(int docId)
	{
		return (getForumTopics(docId, true));
	}

	public static List<DocForumEntity> getForumTopics(int docId, boolean onlyConfirmed)
	{
		return (getForumTopics(docId, true, false));
	}

	public static List<DocForumEntity> getForumTopics(int docId, boolean onlyConfirmed, boolean showDeleted)
	{
		return getForumTopics(docId, onlyConfirmed, showDeleted, null);
	}

	public static List<DocForumEntity> getForumTopics(int docId, boolean onlyConfirmed, boolean showDeleted, String flagSearch)
	{
		return getForumTopics(docId, onlyConfirmed, showDeleted, flagSearch, ForumSortBy.LastPost);
	}

	public static List<DocForumEntity> getForumTopics(int docId, boolean onlyConfirmed, boolean showDeleted, String flagSearch, ForumSortBy sortBy)
	{
		return DocForumService.getForumTopics(docId, onlyConfirmed, showDeleted, flagSearch, sortBy);
	}

	/**
	 * Vrati forumId podtemy, cize spravy, ktora ma parentId=-1
	 *
	 * @param forumId
	 */
	public static int getForumMessageParent(int forumId, int docId)
	{
		List<DocForumEntity> msgList;

		try
		{
			if (forumId > 0 && docId > 0)
			{
				msgList = DocForumService.getForumFieldsForDoc(null, docId, true, -1, true, false, true);
				// Logger.println(this,msgList.size());
				forumId = findParentRecursive(msgList, forumId);

			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return forumId;
	}

	/**
	 * Rekurzivne vyhlada forumId podtemy
	 *
	 * @param msgList
	 * @param parentId
	 * @return
	 */
	private static int findParentRecursive(List<DocForumEntity> msgList, int parentId) {
		return DocForumService.findParentRecursive(msgList, parentId);
	}

	/**
	 * Zvysi statistiku videni diskusie
	 *
	 * @param forumId
	 */
	public static void updateForumStatViews(int forumId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			if (forumId > 0)
			{
				db_conn = DBPool.getConnection();
				String sql = "UPDATE document_forum SET stat_views=stat_views+1 WHERE forum_id=? AND parent_id=-1" + CloudToolsForCore.getDomainIdSqlWhere(true);
				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, forumId);
				ps.execute();
				ps.close();
				db_conn.close();
				ps = null;
				db_conn = null;
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}
	}

	/**
	 * Ziska stat. udaje o fore, do ForumBean-u sa setne iba last post a replies
	 * (celk pocet sprav)
	 *
	 * @param docId
	 * @return
	 */
	public static DocForumEntity getForumStat(int docId) {
		return DocForumService.getForumStat(docId);
	}

	/**
	 * Uzatvori/otvori podtemu diskusie
	 *
	 * @param forumId
	 * @param close
	 *            - ak je TRUE, diskusia sa zatvori, ak FALSE diskusia sa otvori
	 * @return
	 */
	public static boolean closeForumTopic(int forumId, boolean close)
	{
		boolean ret = false;
		Connection db_conn = null;
		PreparedStatement ps = null;

		try
		{
			if (forumId > 0)
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("UPDATE document_forum SET active=? WHERE forum_id=?" + CloudToolsForCore.getDomainIdSqlWhere(true) + "");
				ps.setBoolean(1, !close);
				ps.setInt(2, forumId);

				ps.execute();
				ps.close();
				db_conn.close();
				ps = null;
				db_conn = null;
				ret = true;
			}

		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}

		Adminlog.add(Adminlog.TYPE_FORUM_CLOSE, (close ? "Zatvorila " : "Otvorila") + " sa diskusia", forumId, -1);

		return (ret);
	}

	/**
	 * Vyhladavanie vo fore
	 *
	 * @param docId
	 *            - id stranky s forom
	 * @param searchStr
	 *            - retazec co sa hlada
	 * @param userId
	 *            - ak je zadane hladaju sa prispevky od tohto pouzivatela
	 *            (docId je vtedy ignorovane)
	 * @return
	 */
	public static List<ForumSearchBean> searchForum(int docId, String searchStr, int userId)
	{
		List<ForumSearchBean> results = new ArrayList<>();
		if (Constants.getBoolean("luceneForumsSearch") && Tools.isNotEmpty(searchStr))
		{
			LuceneQuery query = new LuceneQuery(new Forums());
			// List<Query> numericQueries = new LinkedList<Query>();
			StringBuilder queryString = new StringBuilder();
			boolean hasCriteria = false;

			if (Tools.isNotEmpty(searchStr))
			{
				queryString.append("( subject:(\"");
				queryString.append(searchStr);
				queryString.append("\") OR question:(\"");
				queryString.append(searchStr);
				queryString.append("\"))");
				hasCriteria = true;
			}

			if (docId > 0)
			{
				if (hasCriteria) queryString.append(" AND ");
				queryString.append(" doc_id:");
				queryString.append(docId);
				hasCriteria = true;
			}

			if (userId > 0)
			{
				if (hasCriteria) queryString.append(" AND ");
				queryString.append(" user_id:[" + userId + " TO " + userId + "]");
				hasCriteria = true;
			}
			// FIX-ME: problem, ked zadam user_id 1 tak mi selectne aj prispevky
			// s user_id -1 -> problem v indexovani? v indexe je ale spravna
			// hodnota..
			results.addAll(CollectionUtils.collect(query.documents(queryString.toString()), new Transformer<Document, ForumSearchBean>()
			{

				@Override
				public ForumSearchBean transform(Document doc)
				{
					ForumSearchBean fsb = new ForumSearchBean();
					fsb.setForumId(Tools.getIntValue(doc.getFieldable("forum_id").stringValue(), -1));
					fsb.setForumName(doc.getFieldable("title").stringValue());
					fsb.setDocId(Tools.getIntValue(doc.getFieldable("doc_id").stringValue(), -1));
					fsb.setParentId(Tools.getIntValue(doc.getFieldable("parent_id").stringValue(), -1));
					fsb.setSubject(doc.getFieldable("subject").stringValue());
					fsb.setQuestion(doc.getFieldable("question").stringValue());
					fsb.setAutorFullName(doc.getFieldable("author_name").stringValue());
					fsb.setAutorEmail(doc.getFieldable("author_email").stringValue());
					fsb.setFlag(doc.getFieldable("flag").stringValue());
					fsb.setUserId(Tools.getIntValue(doc.getFieldable("user_id").stringValue(), -1));
					fsb.setQuestionDate(Tools.formatDateTimeSeconds(LuceneUtils.luceneDateToDate(doc.getFieldable("question_date").stringValue())));
					return fsb;
				}
			}));
			Logger.debug(ForumDB.class, "Results: " + results.size());
		}
		else
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			ForumSearchBean fsb = null;
			try
			{

				db_conn = DBPool.getConnection();
				StringBuilder sql;
				if (docId > 0 && Tools.isNotEmpty(searchStr) && userId == -1)
				{
					sql = new StringBuilder("SELECT d.title, f.* ").append(
							"FROM document_forum f, documents d ").append(
							"WHERE f.doc_id=d.doc_id AND f.confirmed="+DB.getBooleanSql(true)+" AND f.deleted="+DB.getBooleanSql(false)+" " + CloudToolsForCore.getDomainIdSqlWhere(true, "f"));
					if (docId > 0) sql.append("AND f.doc_id=? AND ");
					sql.append("(f.subject LIKE ? OR f.question LIKE ?) ").append(
							"ORDER BY stat_last_post DESC");

					ps = db_conn.prepareStatement(sql.toString());
					int i = 1;

					if (docId > 0) ps.setInt(i++, docId);
					ps.setString(i++, "%" + searchStr + "%");
					ps.setString(i++, "%" + searchStr + "%");
					rs = ps.executeQuery();
					while (rs.next())
					{
						fsb = toForumSearchBean(rs);
						results.add(fsb);
					}
					rs.close();
					ps.close();
					rs = null;
					ps = null;
				}
				else if (docId < 1 && Tools.isNotEmpty(searchStr) && userId == -1)
				{
					sql = new StringBuilder("SELECT d.title, f.* ").append(
							"FROM document_forum f, documents d ").append(
							"WHERE f.doc_id=d.doc_id AND f.confirmed="+DB.getBooleanSql(true)+" "+ CloudToolsForCore.getDomainIdSqlWhere(true, "f")
									+ " AND f.deleted="+DB.getBooleanSql(false)+" AND (f.subject LIKE ? OR f.question LIKE ?) ").append(
							"ORDER BY stat_last_post DESC");

					ps = db_conn.prepareStatement(sql.toString());
					ps.setString(1, "%" + searchStr + "%");
					ps.setString(2, "%" + searchStr + "%");

					rs = ps.executeQuery();
					// Logger.println(this,"SQL: "+sql+"\n RS: "+rs.next());
					while (rs.next())
					{
						// Logger.println(this,"Iterujem... forumID= "+rs.getInt("forum_id"));
						fsb = toForumSearchBean(rs);
						results.add(fsb);
					}
					rs.close();
					ps.close();
					rs = null;
					ps = null;
				}
				else if (userId > 0)
				{
					sql = new StringBuilder("SELECT d.title, f.* ").append(
							"FROM document_forum f, documents d ").append(
							"WHERE f.doc_id=d.doc_id AND f.confirmed="+DB.getBooleanSql(true)+" " + CloudToolsForCore.getDomainIdSqlWhere(true, "f") + " AND f.deleted="+DB.getBooleanSql(false)+" AND f.user_id=? ").append(
							"ORDER BY stat_last_post DESC");

					ps = db_conn.prepareStatement(sql.toString());
					ps.setInt(1, userId);

					rs = ps.executeQuery();
					// Logger.println(this,"SQL: "+sql+"\n RS: "+rs.next());
					while (rs.next())
					{
						fsb = toForumSearchBean(rs);
						results.add(fsb);
					}
					rs.close();
					ps.close();
					rs = null;
					ps = null;
					// aktualizuj pocet prispevkov pouzivatela (mohla nastat
					// chyba v pocitani forum_rank a tu je dobre miesto to
					// opravit)

					ps = db_conn.prepareStatement("UPDATE users SET forum_rank=? WHERE user_id=?");
					ps.setInt(1, results.size());
					ps.setInt(2, userId);
					ps.execute();
					ps.close();
					ps = null;
				}
				db_conn.close();
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
					if (rs != null) rs.close();
					if (ps != null) ps.close();
					if (db_conn != null) db_conn.close();
				}
				catch (Exception ex2)
				{}
			}
		}
		return (results);
	}

	/**
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private static ForumSearchBean toForumSearchBean(ResultSet rs) throws SQLException
	{
		ForumSearchBean fsb = new ForumSearchBean();
		fsb.setForumId(rs.getInt("forum_id"));
		fsb.setForumName(DB.getDbString(rs, "title"));
		fsb.setDocId(rs.getInt("doc_id"));
		fsb.setParentId(rs.getInt("parent_id"));
		fsb.setSubject(DB.getDbString(rs, "subject"));
		fsb.setQuestion(DB.getDbString(rs, "question"));
		fsb.setAutorFullName(DB.getDbString(rs, "author_name"));
		fsb.setAutorEmail(DB.getDbString(rs, "author_email"));
		fsb.setFlag(DB.getDbString(rs, "flag"));
		fsb.setUserId(rs.getInt("user_id"));
		fsb.setQuestionDate(DB.getDbDateTime(rs, "question_date"));
		return fsb;
	}

	/**
	 *
	 * @param lastPost
	 * @param lastLogon
	 * @return
	 */
	public static boolean compareDates(String lastPost, String lastLogon)
	{
		boolean ret = false;
		long lPost = 0;
		long lLogon = 0;

		try
		{
			StringTokenizer st = new StringTokenizer(lastPost);
			if (st.hasMoreTokens() && st.countTokens() == 2) lPost = DB.getTimestamp(st.nextToken(), st.nextToken());

			st = new StringTokenizer(lastLogon);
			if (st.hasMoreTokens() && st.countTokens() == 2) lLogon = DB.getTimestamp(st.nextToken(), st.nextToken());
			if (lPost > lLogon) ret = true;
			/*
			 * Calendar cal = Calendar.getInstance();
			 * cal.setTimeInMillis(lPost); Logger.println(this,"Post: "
			 * +cal.getTime()); cal.setTimeInMillis(lLogon);
			 * Logger.println(this,"Logon: " +cal.getTime());
			 */
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return ret;
	}

	public static void updateForumStatInfo(int docId, int forumId) {
		DocForumService.updateForumStatInfo(docId, forumId);
	}

	public static String getParentIds(int topicId, int docId)
	{
		StringBuilder parentIds = new StringBuilder("-1");
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		List<DocForumEntity> ids = new ArrayList<>();
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT forum_id, parent_id FROM document_forum WHERE doc_id=? AND deleted = 0" + CloudToolsForCore.getDomainIdSqlWhere(true) + "");
			ps.setInt(1, docId);
			rs = ps.executeQuery();

			while (rs.next())
			{
				DocForumEntity fb = new DocForumEntity();
				fb.setId(rs.getLong("forum_id"));
				fb.setParentId(rs.getInt("parent_id"));
				ids.add(fb);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;

			// rekurzivne ziskaj zoznam
			List<DocForumEntity> sorted = new ArrayList<>();
			getParentIds(ids, sorted, topicId);

			// prehod to na String
			for (DocForumEntity fb : sorted)
				parentIds.append(',').append(fb.getForumId());
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}
		return (parentIds).toString();
	}

	private static void getParentIds(List<DocForumEntity> all, List<DocForumEntity> ret, int parentId) {
		for (DocForumEntity fb : all) {
			if (fb.getParentId() == parentId) {
				ret.add(fb);
				getParentIds(all, ret, fb.getForumId());
			}
		}
	}

	/**
	 * Vrati datum predposledneho loginu usera uvedeneho v tab. stat_userlogon
	 *
	 * @param userId
	 * @return
	 */
	public static String getUserLastLogon(int userId)
	{
		String ret = null;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql;
		try
		{
			if (userId > 0)
			{
				sql = "SELECT logon_time FROM stat_userlogon WHERE user_id=? ORDER BY logon_time DESC";
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, userId);
				rs = ps.executeQuery();

				// preskocime prvy zaznam - sucasna session
				rs.next();

				if (rs.next())
				{
					ret = DB.getDbDateTime(rs, "logon_time");
				}
				// Logger.println(this,"--->USER: "+userId+
				// "  LAST LOGON: "+ret);
				rs.close();
				ps.close();
				db_conn.close();
				rs = null;
				ps = null;
				db_conn = null;
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}
		// usr.setLastLogon(DB.getDbDateTime(rs, "last_logon"));

		return ret;
	}

	/**
	 * vrati pocet tem vo fore
	 *
	 * @param docId
	 * @return
	 */
	public static int getForumTopicsCount(int docId)
	{
		int ret = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Connection db_conn = null;
		try
		{
			db_conn = DBPool.getConnection();
			if (docId > 0)
			{

				ps = db_conn.prepareStatement("SELECT COUNT(forum_id) AS pocet FROM document_forum WHERE doc_id=? AND parent_id=-1 AND confirmed=? AND deleted=?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true) + "");
				ps.setInt(1, docId);
				ps.setBoolean(2, true);
				ps.setBoolean(3, false);
				rs = ps.executeQuery();
				while (rs.next())
				{
					ret = rs.getInt("pocet");
				}
				rs.close();
				ps.close();
				rs = null;
				ps = null;
			}
			db_conn.close();
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
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}
		return ret;
	}

	/**
	 * vrati pocet vsetkych prispevkov vo fore
	 *
	 * @param docId
	 * @return
	 */
	public static int getForumPostsCount(int docId)
	{
		PreparedStatement ps = null;
		int ret = 0;
		ResultSet rs = null;
		Connection db_conn = null;
		try
		{
			db_conn = DBPool.getConnection();
			if (docId > 0)
			{

				ps = db_conn.prepareStatement("SELECT COUNT(forum_id) AS pocet FROM document_forum WHERE doc_id=? AND confirmed=? AND deleted=?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true) + "");
				ps.setInt(1, docId);
				ps.setBoolean(2, true);
				ps.setBoolean(3, false);
				rs = ps.executeQuery();
				while (rs.next())
				{
					ret = rs.getInt("pocet");
				}
				rs.close();
				ps.close();
				rs = null;
				ps = null;
			}
			db_conn.close();
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
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}
		return ret;
	}

	/**
	 * vrati datum a autora posledneho prispevku
	 *
	 * @param docId
	 * @return
	 */
	public static String[] getForumLastPostDate(int docId)
	{
		String[] ret = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Connection db_conn = null;
		try
		{
			db_conn = DBPool.getConnection();
			if (docId > 0)
			{
				String sql = "SELECT question_date, author_name, user_id FROM document_forum WHERE doc_id=? AND confirmed=? AND deleted=?" + CloudToolsForCore.getDomainIdSqlWhere(true)
						+ " ORDER BY forum_id DESC";
				if (Constants.DB_TYPE == Constants.DB_MYSQL || Constants.DB_TYPE==Constants.DB_PGSQL)
				{
					sql += " LIMIT 1";
				}

				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, docId);
				ps.setBoolean(2, true);
				ps.setBoolean(3, false);
				rs = ps.executeQuery();
				if (rs.next())
				{
					ret = new String[]
					{ Tools.formatDateTime(rs.getTimestamp("question_date").getTime()), rs.getString("author_name"), rs.getString("user_id") };
				}
				rs.close();
				ps.close();
				rs = null;
				ps = null;
			}
			db_conn.close();
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
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}

		return ret;
	}

	/**
	 * Vrati nastavene limity pre nahratie suboru do fora so zadanym docId
	 *
	 * @param docId
	 * @param request
	 * @return
	 */
	public static LabelValueDetails getUploadLimits(int docId, HttpServletRequest request)
	{
		Identity user = (Identity) request.getSession().getAttribute(Constants.USER_KEY);
		if (user == null) return null;

		LabelValueDetails uploadLimitsSession = null;
		uploadLimitsSession = (LabelValueDetails) request.getSession().getAttribute("ForumDB.uploadLimits");
		request.getSession().removeAttribute("ForumDB.uploadLimits");

		// ziskaj stranku
		DocDB docDB = DocDB.getInstance();
		DocDetails doc = docDB.getDoc(docId);
		if (doc == null) return uploadLimitsSession;

		int start = doc.getData().indexOf("!INCLUDE(");
		int end = doc.getData().indexOf(")!");
		if (start == -1 || end <= start) return uploadLimitsSession;

		start = doc.getData().indexOf(",", start);
		if (start == -1 || end <= start) return uploadLimitsSession;

		PageParams pageParams = new PageParams(doc.getData().substring(start + 1, end));

		String allowedFileTypes = pageParams.getValue("allowedFileTypes", null);
		int allowedFileSizeKB = pageParams.getIntValue("allowedFileSizeKB", 0);
		String fileUploadDir = pageParams.getValue("fileUploadDir", null);

		if (Tools.isNotEmpty(user.getUserGroupsIds()))
		{
			StringTokenizer st = new StringTokenizer(user.getUserGroupsIds(), ",");
			while (st.hasMoreTokens())
			{
				int userGroupId = Tools.getIntValue(st.nextToken(), 0);
				if (userGroupId > 0)
				{
					String fileTypes = pageParams.getValue("allowedFileTypes-" + userGroupId, null);
					if (fileTypes != null) allowedFileTypes = fileTypes;

					int fileSize = pageParams.getIntValue("allowedFileSizeKB-" + userGroupId, 0);
					if (fileSize > 0) allowedFileSizeKB = fileSize;

					String uploadDir = pageParams.getValue("fileUploadDir-" + userGroupId, null);
					if (uploadDir != null) fileUploadDir = uploadDir;
				}
			}
		}
		if (allowedFileTypes == null) return uploadLimitsSession;

		LabelValueDetails lvd = new LabelValueDetails();
		lvd.setValue(allowedFileTypes.replace('+', ',').toLowerCase());
		lvd.setInt1(allowedFileSizeKB);
		lvd.setValue2(fileUploadDir);

		return lvd;
	}

	/**
	 * Zobrazi zoznam clankov zoradenych podla poctu prispevkov v diskusii. Ak
	 * existuju clanky s rovnakym poctom prispevkov, zoradi ich podla poctu
	 * hlasujucich citatelov.
	 *
	 * @param docsLength
	 *            - pocet zobrazenych clankov
	 * @param period
	 *            - pocet dni dozadu za ktore sa statistika berie
	 * @param minUsers
	 *            - minimalny pocet prispevkov v diskusii
	 * @param groupIds
	 *            - id adresara v ktorom sa sledovane clanky nachadzaju
	 * @param includeSubGroups
	 *            - ak je true, beru sa aj podadresare
	 * @return
	 */
	public static List<RatingEntity> getTopForums(int docsLength, int period, int minUsers, String groupIds, boolean includeSubGroups)
	{
		List<RatingEntity> topPages = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		RatingEntity rBean;

		try
		{
			DebugTimer dt = new DebugTimer("ForumDB.getTopForums");

			// priprav si hashtabulku povolenych adresarov
			Map<Integer, Integer> availableGroups = new Hashtable<>();
			boolean doGroupsCheck = false;
			GroupsDB groupsDB = GroupsDB.getInstance();
			if (Tools.isNotEmpty(groupIds))
			{
				StringTokenizer st = new StringTokenizer(groupIds, ",+; ");
				while (st.hasMoreTokens())
				{
					int groupId = Tools.getIntValue(st.nextToken(), 0);
					if (groupId > 0)
					{
						doGroupsCheck = true;

						availableGroups.put(groupId, 1);
						if (includeSubGroups)
						{
							// najdi child grupy
							List<GroupDetails> searchGroupsArray = groupsDB.getGroupsTree(groupId, false, false);
							for (GroupDetails group : searchGroupsArray)
							{
								if (group != null && group.isInternal() == false)
								{
									availableGroups.put(group.getGroupId(), 1);
								}
							}
						}
					}
				}
			}

			dt.diff("availableGroups=" + availableGroups.size());

			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_MONTH, -period);

			String sql = "SELECT doc_id, COUNT(doc_id) AS clicks, MAX(question_date) AS question_date ";
			sql += "FROM document_forum WHERE question_date >= ? AND confirmed=? AND deleted=?" + CloudToolsForCore.getDomainIdSqlWhere(true) + " GROUP BY doc_id";

			Logger.debug(ForumDB.class, "sql:" + sql);
			Logger.debug(ForumDB.class, "fromDate:" + Tools.formatDate(cal.getTime()));

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			ps.setTimestamp(1, new Timestamp(cal.getTimeInMillis()));
			ps.setBoolean(2, true);
			ps.setBoolean(3, false);
			rs = ps.executeQuery();

			DocDetails testDoc;
			DocDB docDB = DocDB.getInstance();
			List<RatingEntity> list = new ArrayList<>();
			while (rs.next())
			{
				rBean = new RatingEntity();
				rBean.setDocId(rs.getInt("doc_id"));

				testDoc = docDB.getBasicDocDetails(rBean.getDocId(), false);
				if (testDoc == null || testDoc.isAvailable() == false) continue;

				// kontrola na groupids
				if (doGroupsCheck)
				{
					if (availableGroups.get(testDoc.getGroupId()) == null) continue;
				}

				rBean.setDocTitle(testDoc.getTitle());
				rBean.setRatingStat(rs.getInt("clicks"));
				rBean.setTotalUsers(rBean.getRatingStat());

				Date d = rs.getTimestamp("question_date");
				if(d != null) rBean.setInsertDate(d);

				if (minUsers > 0 && rBean.getTotalUsers() < minUsers) continue;

				list.add(rBean);
			}
			rs.close();
			ps.close();

			rs = null;
			ps = null;

			dt.diff("sorting");

			// usortuj to podla poctu prispevkov a nazvu

			Collections.sort(list, new Comparator<RatingEntity>() {

				@Override
				public int compare(RatingEntity r1, RatingEntity r2)
				{
					if (r1.getTotalUsers() == r2.getTotalUsers())
					{
						// return r1.getDocTitle().compareTo(r2.getDocTitle());
						// vratim usporiadane podla novsieho ratingu
						if (r2.getInsertDateLong() > r1.getInsertDateLong()) return 1;
						else if (r2.getInsertDateLong() < r1.getInsertDateLong()) return -1;
						return 0;
					}

					return r2.getTotalUsers() - r1.getTotalUsers();
				}

			});

			// sprav z toho skrateny zoznam
			if (docsLength < 1) topPages = list;
			else
			{
				for (int i = 0; (i < docsLength && i < list.size()); i++)
				{
					topPages.add(list.get(i));
				}
			}

			dt.diff("done, size: " + topPages.size());
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (db_conn != null) db_conn.close();
				if (rs != null) rs.close();
				if (ps != null) ps.close();
			}
			catch (Exception ex2)
			{}
		}

		return (topPages);
	}

	/**
	 * Rekurzivna metoda. Obnovi zadany prispevok a jeho odpovede z DB.
	 *
	 * @param forumId
	 *            - id prispevku
	 * @return
	 */
	public static boolean recoverMessage(int forumId, int docId, Identity user) {
		return DocForumService.docForumRecursiveAction(DocForumService.ActionType.RECOVER, forumId, docId, user);
	}

	/**
	 * Update stlpcu forum_count, pre konkretne doc_id
	 *
	 * @param doc_id
	 * @param forum_count
	 */
	public static void setForumCountForDocIds(int doc_id, int forum_count)
	{
		new SimpleQuery().execute("UPDATE documents SET forum_count = ? WHERE doc_id = ?", forum_count, doc_id);
	}

	/**
	 * Test forum, if it's active, by given docId.
	 * @param docId - ID of the page where the forum is located
	 * @return - return true if isActive, else false
	 */
	public static boolean isActive(int docId) {
		return ForumGroupService.isActive(docId);
	}

	/**
	 * Vrati mi hash tabulku s userID a forumRank pre userov, ktori patria do
	 * skupiny userGroup
	 *
	 * @param userGroup
	 * @return
	 */
	public static Hashtable<String, String> getForumRanks(int userGroup)
	{
		Hashtable<String, String> ranks = new Hashtable<String, String>();

		try
		{
			for (UserDetails user : UsersDB.getUsersByGroup(userGroup))
			{
				ranks.put(Integer.toString(user.getUserId()), Integer.toString(user.getForumRank()));
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return ranks;
	}

	//Pri týchto metódch sa nenašli žiadne referencie v projekte ani žiaden výskyt v súboroch v časti ./src/main/webapp/
	//Odstránené - sivan - #55649
		//public static List<ForumBean> getForumFieldsForDocAndUser(HttpServletRequest request, int docId, int userId)
		//public static List<ForumBean> getForumFieldsForDocAndUsers(HttpServletRequest request, int docId, List<UserDetails> users)
		//private static void recursiveSort(List<ForumBean> unsorted, List<ForumBean> sorted, int level, int parent)
		//public static List<ForumBean> getForumTopicsByUserId(int docId, boolean onlyConfirmed, boolean showDeleted, String flagSearch, ForumSortBy sortBy)
		//public static ForumBean getForumStatNormal(int docId)
		//public static boolean isTopicHot(int docId, int topicId, int hours, int postCount)
		//public static int getParentTopId(HttpServletRequest request, int topicId, int docId)
		//public static int getParentId(HttpServletRequest request, int topicId, int docId)
		//public static List<Integer> getChildIds(int topicId, int docId)
		//public static int saveForumQuestion(int docId, int parentId, String subject, String question, String authorName, String email, String remoteIp, boolean sendNotification, int userId)
		//public static void recountForumCountForDocIds()
		//public static int getCountMessagesByUserId(int docId, int userId)
		//public static int getLastUserForumMessageFromForum(int docId, int userId)
		//public static List<ForumBean> getLastMessagesFromForum(int numberOf, int docId)
		//public static List<ForumBean> getLastMessages(int docsLength, String groupIds, boolean includeSubGroups)
		//public static List<ForumBean> getChildren(HttpServletRequest request, int topicId, int docId)
		//public static List<Integer> getChildrenIds(HttpServletRequest request, int topicId, int docId)
		//public static Map<String, String> getForumRanks(int userGroup)

}