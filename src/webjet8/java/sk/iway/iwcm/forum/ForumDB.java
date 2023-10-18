
package sk.iway.iwcm.forum;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.lucene.document.Document;

import sk.iway.Password;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.calendar.CalendarDB;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.rating.RatingBean;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
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

	public static List<ForumBean> getForumFieldsForDoc(HttpServletRequest request, int doc_id)
	{
		return (getForumFieldsForDoc(request, doc_id, true, 0, true));
	}

	public static List<ForumBean> getForumFieldsForDoc(HttpServletRequest request, int doc_id, boolean onlyConfirmed)
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
	public static List<ForumBean> getForumFieldsForDoc(HttpServletRequest request, int doc_id, boolean onlyConfirmed, int parentId)
	{
		return (getForumFieldsForDoc(request, doc_id, onlyConfirmed, parentId, true, false));
	}

	public static List<ForumBean> getForumFieldsForDoc(HttpServletRequest request, int doc_id, boolean onlyConfirmed, int parentId, boolean sortAscending)
	{
		return getForumFieldsForDoc(request, doc_id, onlyConfirmed, parentId, sortAscending, false);
	}

	public static List<ForumBean> getForumFieldsForDoc(HttpServletRequest request, int doc_id, boolean onlyConfirmed, int parentId, boolean sortAscending, boolean showDeleted)
	{
		List<ForumBean> unsorted = new ArrayList<>();
		List<ForumBean> sorted = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			if (doc_id > 0)
			{
				ForumGroupBean fgb = getForum(doc_id);

				db_conn = DBPool.getConnection();
				String sql = "";

				// najskor rekurzivne ziskame zoznam parent_id ktore sa pre moje
				// zadane parent v DB nachadzaju aby sme nemuseli vsetko
				// nasledne citat
				// v CityU to robilo problem s OOM
				StringBuilder parentIds = new StringBuilder(String.valueOf(parentId));
				HashSet<Integer> parentIdUzMam = new HashSet<>();
				HashSet<Integer> childIdUzMam = new HashSet<>();
				parentIdUzMam.add(Integer.valueOf(parentId));
				sql = "SELECT forum_id, parent_id FROM document_forum WHERE doc_id=? AND parent_id>0" + CloudToolsForCore.getDomainIdSqlWhere(true);
				if (showDeleted == false) sql += " AND deleted = 0";
				sql += " ORDER BY forum_id ASC";

				Logger.debug(ForumDB.class, "parentIds.append(',').append(dbParentId), sql1=" + sql);

				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, doc_id);
				rs = ps.executeQuery();
				while (rs.next())
				{
					int dbForumId = rs.getInt("forum_id");
					int dbParentId = rs.getInt("parent_id");

					if (parentId == 0) parentIds.append(',').append(dbParentId); // aby
																					// nam
																					// fungovali
																					// odpovede
																					// v
																					// beznom
																					// fore

					if (parentIdUzMam.contains(Integer.valueOf(dbParentId)))
					{
						childIdUzMam.add(Integer.valueOf(dbForumId));
					}
					else if (childIdUzMam.contains(Integer.valueOf(dbParentId)))
					{
						childIdUzMam.add(Integer.valueOf(dbForumId));

						parentIdUzMam.add(Integer.valueOf(dbParentId));
						parentIds.append(',').append(dbParentId);
					}
				}
				rs.close();
				ps.close();

				if (showDeleted) sql = "SELECT * FROM document_forum WHERE doc_id=? AND parent_id IN (" + parentIds.toString() + ") " + CloudToolsForCore.getDomainIdSqlWhere(true)
						+ "ORDER BY question_date";
				else sql = "SELECT * FROM document_forum WHERE doc_id=? AND parent_id IN (" + parentIds.toString() + ") " + CloudToolsForCore.getDomainIdSqlWhere(true)
						+ "AND deleted = 0 ORDER BY question_date";

				if (sortAscending)
				{
					sql += " ASC";
				}
				else
				{
					sql += " DESC";
				}

				Logger.debug(ForumDB.class, "getForumFieldsForDod(" + doc_id + ", " + onlyConfirmed + "," + parentId + " sql=" + sql);

				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, doc_id);
				rs = ps.executeQuery();
				ForumBean fb;
				SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.getString("dateFormat"));
				SimpleDateFormat timeFormat = new SimpleDateFormat(Constants.getString("timeFormat"));
				while (rs.next())
				{
					fb = new ForumBean();
					fb.setForumId(rs.getInt("forum_id"));
					fb.setDocId(rs.getInt("doc_id"));
					fb.setParentId(rs.getInt("parent_id"));
					fb.setSubject(DB.getDbString(rs, "subject"));
					fb.setQuestion(DB.getDbString(rs, "question"));
					fb.setQuestionDate(rs.getTimestamp("question_date"));
					fb.setQuestionDateDisplayDate(dateFormat.format(rs.getTimestamp("question_date")));
					fb.setQuestionDateDisplayTime(timeFormat.format(rs.getTimestamp("question_date")));
					fb.setAutorFullName(DB.getDbString(rs, "author_name"));
					fb.setAutorEmail(DB.getDbString(rs, "author_email"));
					fb.setConfirmed(rs.getBoolean("confirmed"));
					fb.setSendNotif(rs.getBoolean("send_answer_notif"));
					fb.setActive(rs.getBoolean("active"));
					fb.setUserId(rs.getInt("user_id"));
					fb.setFlag(DB.getDbString(rs, "flag"));

					fb.setDeleted(rs.getBoolean("deleted"));

					// ak ma forum nastavene potvrdzovanie prispevkov,
					// kontrolujem ci je prispevok potvrdeny
					if (onlyConfirmed && fgb.isMessageConfirmation())
					{
						if (fb.isConfirmed()) unsorted.add(fb);
					}
					else if (onlyConfirmed) {
						if (fb.isConfirmed()) unsorted.add(fb);
					}
					else
					{
						unsorted.add(fb);
					}
				}
				rs.close();
				ps.close();
				db_conn.close();
				rs = null;
				ps = null;
				db_conn = null;
				fb = getForumBean(request, parentId);
				if (fb != null && sortAscending)
				{
					// pridame na zaciatok
					sorted.add(fb);
				}
				recursiveSort(unsorted, sorted, 0, parentId);
				if (fb != null && sortAscending == false)
				{
					// pridame na koniec (kedze je to akoze prva otazka)
					sorted.add(fb);
				}
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

		return (sorted);
	}

	public static List<ForumBean> getForumFieldsForDocAndUser(HttpServletRequest request, int docId, int userId)
	{
		List<ForumBean> result = new ArrayList<>();
		String sql = "SELECT * FROM document_forum WHERE doc_id=? AND user_id=? " + CloudToolsForCore.getDomainIdSqlWhere(true) + "ORDER BY question_date DESC";

		Connection connection = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ForumBean fb = null;

		SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.getString("dateFormat"));
		SimpleDateFormat timeFormat = new SimpleDateFormat(Constants.getString("timeFormat"));

		try
		{
			try
			{
				connection = DBPool.getConnection();

				try
				{
					ps = connection.prepareStatement(sql);
					ps.setInt(1, docId);
					ps.setInt(2, userId);

					try
					{
						rs = ps.executeQuery();

						while (rs.next())
						{
							fb = new ForumBean();
							fb.setForumId(rs.getInt("forum_id"));
							fb.setDocId(rs.getInt("doc_id"));
							fb.setParentId(rs.getInt("parent_id"));
							fb.setSubject(DB.getDbString(rs, "subject"));
							fb.setQuestion(DB.getDbString(rs, "question"));
							fb.setQuestionDate(rs.getTimestamp("question_date"));
							fb.setQuestionDateDisplayDate(dateFormat.format(rs.getTimestamp("question_date")));
							fb.setQuestionDateDisplayTime(timeFormat.format(rs.getTimestamp("question_date")));
							fb.setAutorFullName(DB.getDbString(rs, "author_name"));
							fb.setAutorEmail(DB.getDbString(rs, "author_email"));
							fb.setConfirmed(rs.getBoolean("confirmed"));
							fb.setHashCode(DB.getDbString(rs, "hash_code"));
							fb.setSendNotif(rs.getBoolean("send_answer_notif"));
							fb.setActive(rs.getBoolean("active"));
							fb.setUserId(rs.getInt("user_id"));
							fb.setFlag(DB.getDbString(rs, "flag"));
							fb.setDeleted(rs.getBoolean("deleted"));

							result.add(fb);
						}

						return result;
					}
					finally
					{
						if (rs != null)
						{
							rs.close();
						}
					}
				}
				finally
				{
					if (ps!=null) ps.close();
				}
			}
			finally
			{
				if (connection != null) connection.close();
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return result;
	}

	public static List<ForumBean> getForumFieldsForDocAndUsers(HttpServletRequest request, int docId, List<UserDetails> users)
	{
		List<ForumBean> result = new ArrayList<>();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM document_forum WHERE doc_id=? AND ");
		for (int i = 0; i < users.size(); i++)
		{
			sql.append("user_id=? ");

			if (i < users.size() - 1)
			{
				sql.append("OR ");
			}
		}
		sql.append(CloudToolsForCore.getDomainIdSqlWhere(true));
		sql.append("ORDER BY question_date DESC");

		Connection connection = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ForumBean fb = null;

		SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.getString("dateFormat"));
		SimpleDateFormat timeFormat = new SimpleDateFormat(Constants.getString("timeFormat"));

		try
		{
			try
			{
				connection = DBPool.getConnection();

				try
				{
					ps = connection.prepareStatement(sql.toString());
					ps.setInt(1, docId);

					for (int i = 0; i < users.size(); i++)
					{
						ps.setInt(i + 2, users.get(i).getUserId());
					}

					try
					{
						rs = ps.executeQuery();

						while (rs.next())
						{
							fb = new ForumBean();
							fb.setForumId(rs.getInt("forum_id"));
							fb.setDocId(rs.getInt("doc_id"));
							fb.setParentId(rs.getInt("parent_id"));
							fb.setSubject(DB.getDbString(rs, "subject"));
							fb.setQuestion(DB.getDbString(rs, "question"));
							fb.setQuestionDate(rs.getTimestamp("question_date"));
							fb.setQuestionDateDisplayDate(dateFormat.format(rs.getTimestamp("question_date")));
							fb.setQuestionDateDisplayTime(timeFormat.format(rs.getTimestamp("question_date")));
							fb.setAutorFullName(DB.getDbString(rs, "author_name"));
							fb.setAutorEmail(DB.getDbString(rs, "author_email"));
							fb.setConfirmed(rs.getBoolean("confirmed"));
							fb.setHashCode(DB.getDbString(rs, "hash_code"));
							fb.setSendNotif(rs.getBoolean("send_answer_notif"));
							fb.setActive(rs.getBoolean("active"));
							fb.setUserId(rs.getInt("user_id"));
							fb.setFlag(DB.getDbString(rs, "flag"));
							fb.setDeleted(rs.getBoolean("deleted"));

							result.add(fb);
						}

						return result;
					}
					finally
					{
						if (rs != null)
						{
							rs.close();
						}
					}
				}
				finally
				{
					if (ps!=null) ps.close();
				}
			}
			finally
			{
				if (connection!=null) connection.close();
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return result;
	}

	/**
	 * vrati ForumDetails pre zadane forumId
	 *
	 * @param forumId
	 * @return
	 */
	public static ForumBean getForumBean(HttpServletRequest request, int forumId)
	{
		return (getForumBean(request, forumId, true));
	}

	public static ForumBean getForumBean(HttpServletRequest request, int forumId, boolean sortAscending)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ForumBean fb = null;
		try
		{
			db_conn = DBPool.getConnection();
			String sql = "SELECT * FROM document_forum WHERE forum_id=?" + CloudToolsForCore.getDomainIdSqlWhere(true) + " ORDER BY question_date";
			if (sortAscending)
			{
				sql += " ASC";
			}
			else
			{
				sql += " DESC";
			}
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, forumId);
			rs = ps.executeQuery();
			SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.getString("dateFormat"));
			SimpleDateFormat timeFormat = new SimpleDateFormat(Constants.getString("timeFormat"));
			if (rs.next())
			{
				fb = new ForumBean();
				fb.setForumId(rs.getInt("forum_id"));
				fb.setDocId(rs.getInt("doc_id"));
				fb.setParentId(rs.getInt("parent_id"));
				fb.setSubject(DB.getDbString(rs, "subject"));
				fb.setQuestion(DB.getDbString(rs, "question"));
				fb.setQuestionDate(rs.getTimestamp("question_date"));
				fb.setQuestionDateDisplayDate(dateFormat.format(rs.getTimestamp("question_date")));
				fb.setQuestionDateDisplayTime(timeFormat.format(rs.getTimestamp("question_date")));
				fb.setAutorFullName(DB.getDbString(rs, "author_name"));
				fb.setAutorEmail(DB.getDbString(rs, "author_email"));
				fb.setConfirmed(rs.getBoolean("confirmed"));
				fb.setHashCode(DB.getDbString(rs, "hash_code"));
				fb.setSendNotif(rs.getBoolean("send_answer_notif"));
				fb.setActive(rs.getBoolean("active"));
				fb.setUserId(rs.getInt("user_id"));
				fb.setFlag(DB.getDbString(rs, "flag"));
				fb.setDeleted(rs.getBoolean("deleted"));
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
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}
		return (fb);
	}

	/**
	 * Description of the Method
	 *
	 * @param unsorted
	 *            Description of the Parameter
	 * @param sorted
	 *            Description of the Parameter
	 * @param level
	 *            Description of the Parameter
	 * @param parent
	 *            Description of the Parameter
	 */
	private static void recursiveSort(List<ForumBean> unsorted, List<ForumBean> sorted, int level, int parent)
	{
		for (ForumBean fb : unsorted)
		{
			if (fb.getParentId() == parent)
			{
				fb.setPrefix("<div style=\"margin-left:" + (20 * level) + "px\">");
				fb.setLevel(level);
				sorted.add(fb);
				recursiveSort(unsorted, sorted, level + 1, fb.getForumId());
			}
		}
	}

	/**
	 * Vrati posledne zadany prispevok zadaneho fora
	 *
	 * @param doc_id
	 *            - id web stranky / fora
	 * @return
	 */
	public static ForumBean getLastMessage(int doc_id)
	{
		ForumBean fb = null;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM document_forum WHERE doc_id=? " + CloudToolsForCore.getDomainIdSqlWhere(true) + "ORDER BY question_date DESC");
			ps.setInt(1, doc_id);
			rs = ps.executeQuery();

			SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.getString("dateFormat"));
			SimpleDateFormat timeFormat = new SimpleDateFormat(Constants.getString("timeFormat"));

			if (rs.next())
			{
				fb = new ForumBean();
				fb.setForumId(rs.getInt("forum_id"));
				fb.setDocId(rs.getInt("doc_id"));
				fb.setParentId(rs.getInt("parent_id"));
				fb.setSubject(DB.getDbString(rs, "subject"));
				fb.setQuestion(DB.getDbString(rs, "question"));
				fb.setQuestionDate(rs.getTimestamp("question_date"));
				fb.setQuestionDateDisplayDate(dateFormat.format(rs.getTimestamp("question_date")));
				fb.setQuestionDateDisplayTime(timeFormat.format(rs.getTimestamp("question_date")));
				fb.setAutorFullName(DB.getDbString(rs, "author_name"));
				fb.setAutorEmail(DB.getDbString(rs, "author_email"));
				fb.setConfirmed(rs.getBoolean("confirmed"));
				fb.setSendNotif(rs.getBoolean("send_answer_notif"));
				fb.setActive(rs.getBoolean("active"));
				fb.setUserId(rs.getInt("user_id"));
				fb.setFlag(DB.getDbString(rs, "flag"));

				fb.setDeleted(rs.getBoolean("deleted"));
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
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}
		return (fb);
	}

	/**
	 * Rekurzivna metoda. Vymaze zadany prispevok a jeho odpovede z DB.
	 *
	 * @param forumId
	 *            - id prispevku
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static boolean deleteMessage(int forumId, int docId, Identity user)
	{
		List<ForumBean> msgList;
		String fIds = "";
		String sql;
		int parentId = -1;
		ForumBean fb;
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			if (user != null)
			{
				fb = getForumBean(null, forumId);
				if (fb != null) parentId = fb.getParentId();

				msgList = getForumFieldsForDoc(null, docId, true, forumId);
				fIds = getChildForumIdsRecursive(msgList, forumId);
				if (Tools.isNotEmpty(fIds)) fIds += forumId;
				else fIds = Integer.toString(forumId);
				// Logger.println(this,"GROUPS: "+fIds);
				if (fb!=null) Adminlog.add(Adminlog.TYPE_FORM_DELETE, "Zmazany diskusny prispevok: " + fb.getSubject(), forumId, docId);

				boolean isAdmin = user.isAdmin();
				if (isAdmin == false)
				{
					ForumGroupBean forumGroupBean = ForumDB.getForum(docId);
					isAdmin = forumGroupBean.isAdmin(user);
				}

				if (isAdmin) sql = "SELECT user_id FROM document_forum WHERE forum_id IN (" + fIds + ")" + CloudToolsForCore.getDomainIdSqlWhere(true);
				else sql = "SELECT user_id FROM document_forum WHERE forum_id IN (" + fIds + ")" + CloudToolsForCore.getDomainIdSqlWhere(true) + " AND user_id=" + user.getUserId();

				List<Number> usersInForums = null;
				try
				{
					usersInForums = new SimpleQuery().forList(sql);
				}
				catch (Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
					usersInForums = new ArrayList<>();
					if (isAdmin == false) usersInForums.add(Integer.valueOf(user.getUserId()));
				}

				if (Constants.getBoolean("forumReallyDeleteMessages"))
				{
					if (isAdmin) sql = "DELETE FROM document_forum WHERE forum_id IN (" + fIds + ")" + CloudToolsForCore.getDomainIdSqlWhere(true);
					else sql = "DELETE FROM document_forum WHERE forum_id IN (" + fIds + ")" + CloudToolsForCore.getDomainIdSqlWhere(true) + " AND user_id=?";
				}
				else
				{
					if (isAdmin)
					{
						// sql =
						// "DELETE FROM document_forum WHERE forum_id IN ("+fIds+")";
						sql = "UPDATE document_forum SET deleted = 1 WHERE forum_id IN (" + fIds + ")" + CloudToolsForCore.getDomainIdSqlWhere(true) + " AND deleted < 1";
					}
					else
					{
						// sql =
						// "DELETE FROM document_forum WHERE forum_id IN ("+fIds+") AND user_id=?";
						sql = "UPDATE document_forum SET deleted = 1 WHERE forum_id IN (" + fIds + ")" + CloudToolsForCore.getDomainIdSqlWhere(true) + " AND deleted < 1 AND user_id=?";
					}
				}

				Logger.debug(ForumDB.class, "deleteMessage sql=" + sql);

				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement(sql);
				if (!isAdmin) ps.setInt(1, user.getUserId());
				int updated = ps.executeUpdate();
				ps.close();
				ps = null;

				if (updated > 0)
				{
					if (usersInForums != null && usersInForums.isEmpty()==false)
					{
						// znizim userovi forum rank

						ps = db_conn.prepareStatement("UPDATE users SET forum_rank=forum_rank-1 WHERE user_id=? AND forum_rank > 0");
						for (Number n : usersInForums)
						{
							Logger.debug(ForumDB.class, "Znizujem rank for user: " + n.intValue());
							ps.setInt(1, n.intValue());
							ps.execute();
						}
						ps.close();
						ps = null;
					}

					// znizim forum_count stranky
					ps = db_conn.prepareStatement("UPDATE documents SET forum_count=forum_count-? WHERE doc_id=? AND forum_count > 0");
					ps.setInt(1, updated);
					ps.setInt(2, docId);
					ps.execute();
					ps.close();
					ps = null;
				}

				db_conn.close();
				db_conn = null;
				// Logger.println(this,"SQL: "+sql);

				updateForumStatInfo(docId, parentId);

				// uprav full text index
				int[] fidsInt = Tools.getTokensInt(fIds, ",");
				for (int fid : fidsInt)
				{
					Forums.updateSingleQuestion(fid);
				}
			}
			return (true);

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
		return (false);
	}

	/**
	 * vrati ForumGroupBean pre zadane docId - data z tab. forum, ak neexistuje
	 * definicia, vrati defaultnu
	 *
	 * @param docId
	 *            - stranka, ku ktorej je pridane forum
	 * @return
	 */
	public static ForumGroupBean getForum(int docId)
	{
		return getForum(docId, false);
	}

	/**
	 * vrati ForumGroupBean pre zadane docId - data z tab. forum
	 *
	 * @param docId
	 *            - id stranky
	 * @param returnNull
	 *            - ak je nastavene na false a zaznam neexistuje, vrati default
	 *            zaznam
	 * @return
	 */
	public static ForumGroupBean getForum(int docId, boolean returnNull)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ForumGroupBean fgb = null;
		if (!returnNull)
		{
			fgb = new ForumGroupBean();
			fgb.setMessageConfirmation(false);
			fgb.setActive(true);
		}
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM forum WHERE doc_id=?" + CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setInt(1, docId);
			rs = ps.executeQuery();
			SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.getString("dateFormat"));
			SimpleDateFormat timeFormat = new SimpleDateFormat(Constants.getString("timeFormat"));
			if (rs.next())
			{
				fgb = new ForumGroupBean();
				fgb.setId(rs.getInt("id"));
				fgb.setDocId(rs.getInt("doc_id"));
				fgb.setActive(rs.getBoolean("active"));
				if (rs.getTimestamp("date_from") != null)
				{
					fgb.setDateFrom(dateFormat.format(rs.getTimestamp("date_from")));
					fgb.setDateFromTime(timeFormat.format(rs.getTimestamp("date_from")));
				}
				if (rs.getTimestamp("date_to") != null)
				{
					fgb.setDateTo(dateFormat.format(rs.getTimestamp("date_to")));
					fgb.setDateToTime(timeFormat.format(rs.getTimestamp("date_to")));
				}
				fgb.setHoursAfterLastMessage(rs.getInt("hours_after_last_message"));
				fgb.setMessageConfirmation(rs.getBoolean("message_confirmation"));
				fgb.setApproveEmail(DB.getDbString(rs, "approve_email"));
				fgb.setNotifEmail(DB.getDbString(rs, "notif_email"));
				fgb.setMessageBoard(rs.getBoolean("message_board"));
				fgb.setAdvertisementType(rs.getBoolean("advertisement_type"));
				fgb.setAdminGroups(rs.getString("admin_groups"));
				fgb.setAddmessageGroups(rs.getString("addmessage_groups"));
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
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}
		return (fgb);
	}

	/**
	 * Ulozi forum do DB - tab. forum
	 *
	 * @param fgb
	 *            - parametre diskusie
	 * @return
	 */
	public static boolean saveForum(ForumGroupBean fgb)
	{
		boolean ret = false;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean dateFrom = false;
		boolean dateTo = false;
		int i = 1;
		StringBuilder sql = null;

		try
		{
			if (fgb != null)
			{
				if (Tools.isNotEmpty(fgb.getDateFrom())) dateFrom = true;
				if (Tools.isNotEmpty(fgb.getDateTo())) dateTo = true;
				// if (fgb.getHoursAfterLastMessage() > 0) hours = true;

				db_conn = DBPool.getConnection();
				if (fgb.getId() < 1)
				{
					sql = new StringBuilder("INSERT INTO forum (doc_id, active, domain_id");
					if (dateFrom) sql.append(", date_from");
					if (dateTo) sql.append(", date_to");
					sql.append(", hours_after_last_message");
					sql.append(", message_confirmation, approve_email, notif_email, message_board, advertisement_type,admin_groups, addmessage_groups");

					sql.append(") VALUES (?, ?, ?");

					if (dateFrom) sql.append(", ?");
					if (dateTo) sql.append(", ?");
					sql.append(", ?, ?, ?, ?, ?, ?, ?, ?)");
				}
				else
				{
					sql = new StringBuilder("UPDATE forum SET ").append("doc_id=?, active=?, domain_id=?");

					if (dateFrom) sql.append(", date_from=?");
					if (dateTo) sql.append(", date_to=?");
					sql.append(", hours_after_last_message=?, message_confirmation=?, approve_email=?, notif_email=?, message_board=?, advertisement_type=?, admin_groups=?, addmessage_groups=?");
					sql.append(" WHERE id=?" + CloudToolsForCore.getDomainIdSqlWhere(true));
				}
				// Logger.println(this,"SQL: "+sql);
				ps = db_conn.prepareStatement(sql.toString());
				ps.setInt(i++, fgb.getDocId());
				ps.setBoolean(i++, fgb.isActive());
				ps.setInt(i++, CloudToolsForCore.getDomainId());
				if (dateFrom)
				{
					ps.setTimestamp(i++, new Timestamp(DB.getTimestamp(fgb.getDateFrom(), fgb.getDateFromTime())));
				}
				if (dateTo)
				{
					ps.setTimestamp(i++, new Timestamp(DB.getTimestamp(fgb.getDateTo(), fgb.getDateToTime())));
				}
				ps.setInt(i++, fgb.getHoursAfterLastMessage());

				ps.setBoolean(i++, fgb.isMessageConfirmation());
				ps.setString(i++, fgb.getApproveEmail());
				ps.setString(i++, fgb.getNotifEmail());
				ps.setBoolean(i++, fgb.isMessageBoard());
				ps.setBoolean(i++, fgb.isAdvertisementType());
				ps.setString(i++, fgb.getAdminGroups());
				ps.setString(i++, fgb.getAddmessageGroups());

				if (fgb.getId() > 0)
				{
					ps.setInt(i++, fgb.getId());
				}
				// Logger.println(this,"i= "+i);
				ps.execute();
				ps.close();
				ps = null;
				if (fgb.getId() < 1)
				{
					ps = db_conn.prepareStatement("SELECT max(id) AS id FROM forum WHERE doc_id = ?" + CloudToolsForCore.getDomainIdSqlWhere(true));
					ps.setInt(1, fgb.getDocId());
					rs = ps.executeQuery();
					if (rs.next())
					{
						fgb.setId(rs.getInt("id"));
					}
					rs.close();
					ps.close();
					rs = null;
					ps = null;
				}

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
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}

		return (ret);
	}

	/**
	 * Otestuje zadane forum, ci je aktivne
	 *
	 * @param docId
	 *            - ID stranky, kde je umiestnene forum
	 * @return
	 */
	public static boolean isActive(int docId)
	{
		boolean ret = true;
		ForumGroupBean fgb;
		ForumBean fBean;
		long date;
		Calendar cal = Calendar.getInstance();

		try
		{
			if (docId > 0)
			{
				fgb = getForum(docId, true);
				if (fgb != null)
				{
					if (!fgb.isActive()) ret = false;
					if (Tools.isNotEmpty(fgb.getDateFrom()))
					{
						date = DB.getTimestamp(fgb.getDateFrom(), fgb.getDateFromTime());
						if (Tools.getNow() <= date) ret = false;
					}
					if (Tools.isNotEmpty(fgb.getDateTo()))
					{
						date = DB.getTimestamp(fgb.getDateTo(), fgb.getDateToTime());
						if (Tools.getNow() >= date) ret = false;
					}
					if (fgb.getHoursAfterLastMessage() > 0)
					{
						fBean = getLastMessage(docId);
						if (fBean != null)
						{
							cal.clear();
							cal.setTime(fBean.getQuestionDate());
							cal.add(Calendar.HOUR_OF_DAY, fgb.getHoursAfterLastMessage());
							if (Tools.getNow() < cal.getTimeInMillis())
							{
								ret = false;
							}
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return (ret);
	}

	/**
	 * Potvrdi prispevok diskusie
	 *
	 * @param forumId
	 * @return
	 */
	public static boolean forumApprove(int forumId)
	{
		boolean ret = false;
		Connection db_conn = null;
		PreparedStatement ps = null;

		try
		{
			if (forumId > 0)
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("UPDATE document_forum SET confirmed=? WHERE forum_id=?" + CloudToolsForCore.getDomainIdSqlWhere(true));
				ps.setBoolean(1, true);
				ps.setInt(2, forumId);

				ps.execute();
				ps.close();
				ps = null;
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
		return (ret);
	}

	/**
	 * Vrati zoznam schvalenych podtem pre dane forum
	 *
	 * @param docId
	 * @return
	 */
	public static List<ForumBean> getForumTopics(int docId)
	{
		return (getForumTopics(docId, true));
	}

	public static List<ForumBean> getForumTopics(int docId, boolean onlyConfirmed)
	{
		return (getForumTopics(docId, true, false));
	}

	public static List<ForumBean> getForumTopics(int docId, boolean onlyConfirmed, boolean showDeleted)
	{
		return getForumTopics(docId, onlyConfirmed, showDeleted, null);
	}

	public static List<ForumBean> getForumTopics(int docId, boolean onlyConfirmed, boolean showDeleted, String flagSearch)
	{
		return getForumTopics(docId, onlyConfirmed, showDeleted, flagSearch, ForumSortBy.LastPost);
	}

	public static List<ForumBean> getForumTopics(int docId, boolean onlyConfirmed, boolean showDeleted, String flagSearch, ForumSortBy sortBy)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ForumBean fb = null;
		List<ForumBean> topics = new ArrayList<>();
		try
		{
			if (docId > 0)
			{
				db_conn = DBPool.getConnection();
				String sql = "SELECT * FROM document_forum WHERE doc_id=? AND parent_id=-1" + CloudToolsForCore.getDomainIdSqlWhere(true);
				if (showDeleted == false) sql += " AND deleted = 0";
				if (Tools.isNotEmpty(flagSearch)) sql += " AND flag LIKE ?";
				sql += " ORDER BY flag DESC, " + sortBy.getColumnName() + " DESC";

				Logger.debug(ForumDB.class, "getForumTopics: docId=" + docId + " flagSearch=" + flagSearch + " sql=" + sql);

				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, docId);
				if (Tools.isNotEmpty(flagSearch)) ps.setString(2, flagSearch + "%");
				rs = ps.executeQuery();
				SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.getString("dateFormat"));
				SimpleDateFormat timeFormat = new SimpleDateFormat(Constants.getString("timeFormat"));
				while (rs.next())
				{
					fb = new ForumBean();
					fb.setForumId(rs.getInt("forum_id"));
					fb.setDocId(rs.getInt("doc_id"));
					fb.setParentId(rs.getInt("parent_id"));
					fb.setSubject(DB.getDbString(rs, "subject"));
					fb.setQuestion(DB.getDbString(rs, "question"));
					fb.setQuestionDate(rs.getTimestamp("question_date"));
					fb.setQuestionDateDisplayDate(dateFormat.format(rs.getTimestamp("question_date")));
					fb.setQuestionDateDisplayTime(timeFormat.format(rs.getTimestamp("question_date")));
					fb.setAutorFullName(DB.getDbString(rs, "author_name"));
					fb.setAutorEmail(DB.getDbString(rs, "author_email"));
					fb.setConfirmed(rs.getBoolean("confirmed"));
					fb.setHashCode(DB.getDbString(rs, "hash_code"));
					fb.setSendNotif(rs.getBoolean("send_answer_notif"));
					fb.setStatReplies(rs.getInt("stat_replies"));
					fb.setStatViews(rs.getInt("stat_views"));
					fb.setLastPost(DB.getDbDateTime(rs, "stat_last_post"));
					fb.setFlag(DB.getDbString(rs, "flag"));
					fb.setUserId(rs.getInt("user_id"));
					fb.setActive(rs.getBoolean("active"));
					fb.setDeleted(rs.getBoolean("deleted"));
					if (onlyConfirmed)
					{
						if (fb.isConfirmed()) topics.add(fb);
					}
					else
					{
						topics.add(fb);
					}
				}
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
		return (topics);
	}

	public static List<ForumBean> getForumTopicsByUserId(int docId, boolean onlyConfirmed, boolean showDeleted, String flagSearch, ForumSortBy sortBy)
	{
		return getForumTopics(docId, onlyConfirmed, showDeleted, flagSearch, sortBy);
	}

	/**
	 * Vrati forumId podtemy, cize spravy, ktora ma parentId=-1
	 *
	 * @param forumId
	 */
	public static int getForumMessageParent(int forumId, int docId)
	{
		List<ForumBean> msgList;

		try
		{
			if (forumId > 0 && docId > 0)
			{
				msgList = getForumFieldsForDoc(null, docId, true, -1);
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
	private static int findParentRecursive(List<ForumBean> msgList, int parentId)
	{
		int forumId = -1;
		try
		{
			// Logger.println(this,"Rekurzia");
			for (ForumBean fb : msgList)
			{
				if (fb.getForumId() == parentId)
				{
					forumId = findParentRecursive(msgList, fb.getParentId());
					if (forumId < 0) forumId = fb.getForumId();
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return forumId;
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
	public static ForumBean getForumStat(int docId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ForumBean fb = null;

		try
		{
			if (docId > 0)
			{
				String lastPost = "";
				int totalMessages = 0;
				db_conn = DBPool.getConnection();
				String sql = "SELECT stat_last_post, (stat_replies+1) AS messages " +
						"FROM document_forum " +
						"WHERE doc_id=? AND parent_id=-1 AND deleted=? " + CloudToolsForCore.getDomainIdSqlWhere(true) + "" +
						"ORDER BY stat_last_post DESC";

				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, docId);
				ps.setBoolean(2, false);
				rs = ps.executeQuery();
				fb = new ForumBean();
				while (rs.next())
				{
					totalMessages += rs.getInt("messages");
					if (Tools.isEmpty(lastPost)) lastPost = DB.getDbDateTime(rs, "stat_last_post");
				}
				fb.setLastPost(lastPost);
				fb.setStatReplies(totalMessages);

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
		return (fb);
	}

	public static ForumBean getForumStatNormal(int docId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ForumBean fb = null;

		try
		{
			if (docId > 0)
			{
				String lastPost = "";
				int totalMessages = 0;
				db_conn = DBPool.getConnection();
				String sql = "select max(question_date) as last_post, count(forum_id) as messages FROM document_forum WHERE doc_id=? AND deleted=?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true);

				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, docId);
				ps.setBoolean(2, false);
				rs = ps.executeQuery();
				fb = new ForumBean();
				if (rs.next())
				{
					totalMessages += rs.getInt("messages");
					lastPost = DB.getDbDateTime(rs, "last_post");
				}
				fb.setLastPost(lastPost);
				fb.setStatReplies(totalMessages);

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
		return (fb);
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
							"WHERE f.doc_id=d.doc_id AND f.confirmed=1 AND f.deleted=0 " + CloudToolsForCore.getDomainIdSqlWhere(true, "f"));
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
							"WHERE f.doc_id=d.doc_id AND f.confirmed=1" + CloudToolsForCore.getDomainIdSqlWhere(true, "f")
									+ " AND f.deleted=0 AND (f.subject LIKE ? OR f.question LIKE ?) ").append(
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
							"WHERE f.doc_id=d.doc_id AND f.confirmed=1" + CloudToolsForCore.getDomainIdSqlWhere(true, "f") + " AND f.deleted=0 AND f.user_id=? ").append(
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

	/**
	 * Rekurzivna metoda. Vrati string so vsetkymi child forumId.
	 *
	 * @param msgList
	 * @param forumId
	 * @return
	 */
	private static String getChildForumIdsRecursive(List<ForumBean> msgList, int forumId)
	{
		StringBuilder idStr = new StringBuilder();
		try
		{
			if (msgList != null && forumId > 0)
			{
				// Logger.println(this,"Rekurzia");
				for (ForumBean fb : msgList)
				{
					if (fb.getParentId() == forumId)
					{
						if (Tools.isNotEmpty(idStr)) idStr.append(String.valueOf(fb.getForumId())).append(',');
						else idStr = new StringBuilder(String.valueOf(fb.getForumId())).append(',');
						idStr.append(getChildForumIdsRecursive(msgList, fb.getForumId()));
						// Logger.println(this,fb.getForumId()+"   idStr: "+idStr);
					}
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return idStr.toString();
	}

	public static void updateForumStatInfo(int docId, int forumId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql;
		String IDs;
		String[] childForumIds;
		int countStatReplies = 0;
		long lastPost = 0;
		List<ForumBean> msgList = getForumFieldsForDoc(null, docId, true, forumId);
		int parentId = findParentRecursive(msgList, forumId);
		// int parentId = getForumMessageParent(forumId, 181);

		try
		{
			// IDs = getChildForumIdsRecursive(getForumFieldsForDoc(null, 181,
			// false, -1), parentId);
			IDs = getChildForumIdsRecursive(msgList, parentId);
			if (Tools.isNotEmpty(IDs) && IDs.endsWith(",")) IDs = IDs.substring(0, IDs.length() - 1);

			childForumIds = Tools.getTokens(IDs, ",");
			if (childForumIds != null)
			{
				countStatReplies = childForumIds.length;

			}

			// Logger.println(this,"****\nparentId: " +parentId+ "\ntemp: "
			// +IDs+ "\narray size: " +countStatReplies);

			if (countStatReplies == 0)
			{
				db_conn = DBPool.getConnection();
				sql = "UPDATE document_forum SET stat_replies=?, stat_last_post=question_date " +
						" WHERE forum_id=? AND parent_id=-1" + CloudToolsForCore.getDomainIdSqlWhere(true);

				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, countStatReplies);
				ps.setInt(2, parentId);
				ps.execute();
				ps.close();
				ps = null;
				db_conn.close();
				db_conn = null;
			}
			else if (Tools.isNotEmpty(IDs))
			{
				db_conn = DBPool.getConnection();

				sql = "SELECT ";
				if (Constants.DB_TYPE == Constants.DB_MSSQL) sql += "TOP 1 ";
				sql += "forum_id, question_date FROM document_forum " +
						" WHERE doc_id=?" + CloudToolsForCore.getDomainIdSqlWhere(true) + " AND forum_id IN (" + IDs + ")" +
						" ORDER BY question_date DESC";

				if (Constants.DB_TYPE == Constants.DB_MYSQL) sql += " LIMIT 1";

				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, docId);
				rs = ps.executeQuery();
				if (rs.next())
				{
					lastPost = rs.getTimestamp("question_date").getTime();
				}
				rs.close();
				ps.close();
				rs = null;
				ps = null;
				if (lastPost > 0 && countStatReplies > 0)
				{
					sql = "UPDATE document_forum SET stat_replies=?, stat_last_post=? " +
							" WHERE forum_id=? AND parent_id=-1" + CloudToolsForCore.getDomainIdSqlWhere(true);

					ps = db_conn.prepareStatement(sql);
					ps.setInt(1, countStatReplies);
					ps.setTimestamp(2, new Timestamp(lastPost));
					ps.setInt(3, parentId);
					ps.execute();
					ps.close();
					ps = null;
				}
				db_conn.close();
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
	}

	/**
	 *
	 * @param docId
	 * @param forumId
	 * @param hours
	 * @param postCount
	 * @return
	 */
	public static boolean isTopicHot(int docId, int topicId, int hours, int postCount)
	{
		//
		boolean ret = false;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql;
		long hoursInMilisec = 0;
		int posts = 0;

		try
		{
			if (docId > 0 && topicId > 0 && hours > 0 && postCount > 0)
			{
				/*
				 * msgList = getForumFieldsForDoc(null, docId, true, -1); IDs =
				 * getChildForumIdsRecursive(msgList, forumId); if
				 * (Tools.isNotEmpty(IDs) && IDs.endsWith(",")) IDs =
				 * IDs.substring(0, IDs.length()-1);
				 */

				String parentIds = getParentIds(topicId, docId);

				db_conn = DBPool.getConnection();

				hoursInMilisec = Tools.getNow() - 60L * 60L * 1000L * hours;
				if (Tools.isNotEmpty(parentIds) && hoursInMilisec > 0)
				{
					sql = "SELECT COUNT(forum_id) AS posts FROM document_forum WHERE forum_id IN (" + parentIds + ")" + CloudToolsForCore.getDomainIdSqlWhere(true)
							+ " AND question_date > ?";
					// Logger.println(ForumDB.class, "sql: " + sql);

					ps = db_conn.prepareStatement(sql);
					ps.setTimestamp(1, new Timestamp(hoursInMilisec));
					rs = ps.executeQuery();
					if (rs.next())
					{
						posts = rs.getInt("posts");
					}
					// Logger.println(this,"POSTS: "+posts+" new DATE: "+(new
					// Timestamp(hoursInMilisec)));
					rs.close();
					ps.close();
					rs = null;
					ps = null;

					if (posts >= postCount) ret = true;
				}

				db_conn.close();
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
		return (ret);
	}

	public static int getParentTopId(HttpServletRequest request, int topicId, int docId) {
		int result = topicId;
		int parentId = topicId;
		int counter = 0;
		int max = 20;

		while (parentId != 0) {
			counter++;
			if (counter > max) {
				break;
			}

			parentId = getParentId(request, parentId, docId);
			if (parentId != 0) {
				result = parentId;
			}
		}

		return result;
	}

	public static int getParentId(HttpServletRequest request, int topicId, int docId) {
		int result = 0;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT forum_id, parent_id FROM document_forum WHERE doc_id=? AND forum_id=?" + CloudToolsForCore.getDomainIdSqlWhere(true) + " LIMIT 1");
			ps.setInt(1, docId);
			ps.setInt(2, topicId);
			rs = ps.executeQuery();

			if (rs.next())
			{
				result = rs.getInt("parent_id");
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
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}

		return result;
	}

	public static List<ForumBean> getChildren(HttpServletRequest request, int topicId, int docId) {
		List<ForumBean> unsorted = new ArrayList<>();
		List<ForumBean> sorted = new ArrayList<>();

		unsorted.add(getForumBean(request, topicId));
		List<Integer> childIds = getChildrenIds(request, topicId, docId);
		for(Integer childId : childIds) {
			unsorted.add(getForumBean(request, childId));
		}

		recursiveSort(unsorted, sorted, 0, 0);

		return sorted;
	}

	public static List<Integer> getChildrenIds(HttpServletRequest request, int topicId, int docId) {
		List<Integer> result = new LinkedList<>();

		List<Integer> childIds = getChildIds(topicId, docId);
		while(childIds.isEmpty()==false)
		{
			for(Integer childId : childIds)
			{
				result.add(childId);

				childIds = getChildIds(childId, docId);
			}
		}

		return result;
	}

	public static List<Integer> getChildIds(int topicId, int docId) {
		List<Integer> result = new LinkedList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT forum_id, parent_id FROM document_forum WHERE doc_id=? AND parent_id=? AND deleted = 0" + CloudToolsForCore.getDomainIdSqlWhere(true) + " ORDER BY question_date");
			ps.setInt(1, docId);
			ps.setInt(2, topicId);
			rs = ps.executeQuery();

			while (rs.next())
			{
				result.add(rs.getInt("forum_id"));
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
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}

		return result;
	}

	public static String getParentIds(int topicId, int docId)
	{
		StringBuilder parentIds = new StringBuilder("-1");
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		List<ForumBean> ids = new ArrayList<>();
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT forum_id, parent_id FROM document_forum WHERE doc_id=? AND deleted = 0" + CloudToolsForCore.getDomainIdSqlWhere(true) + "");
			ps.setInt(1, docId);
			rs = ps.executeQuery();

			while (rs.next())
			{
				ForumBean fb = new ForumBean();
				fb.setForumId(rs.getInt("forum_id"));
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
			List<ForumBean> sorted = new ArrayList<>();
			getParentIds(ids, sorted, topicId);

			// prehod to na String
			for (ForumBean fb : sorted)
			{
				parentIds.append(',').append(fb.getForumId());
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
		return (parentIds).toString();
	}

	private static void getParentIds(List<ForumBean> all, List<ForumBean> ret, int parentId)
	{
		// Logger.println(ForumDB.class,
		// "all.size="+all.size()+" ret.size="+ret.size()+" parentId="+parentId);
		for (ForumBean fb : all)
		{
			if (fb.getParentId() == parentId)
			{
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
	 * Vrati mi hash tabulku s userID a forumRank pre userov, ktori patria do
	 * skupiny userGroup
	 *
	 * @param userGroup
	 * @return
	 */
	public static Map<String, String> getForumRanks(int userGroup)
	{
		Map<String, String> ranks = new Hashtable<>();

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
				if (Constants.DB_TYPE == Constants.DB_MYSQL)
				{
					sql += " LIMIT 0, 1";
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
	public static List<RatingBean> getTopForums(int docsLength, int period, int minUsers, String groupIds, boolean includeSubGroups)
	{
		List<RatingBean> topPages = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		RatingBean rBean;

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
			List<RatingBean> list = new ArrayList<>();
			while (rs.next())
			{
				rBean = new RatingBean();
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
				Timestamp t = rs.getTimestamp("question_date");
				if (t != null) rBean.setInsertDateLong(t.getTime());

				if (minUsers > 0 && rBean.getTotalUsers() < minUsers) continue;

				list.add(rBean);
			}
			rs.close();
			ps.close();

			rs = null;
			ps = null;

			dt.diff("sorting");

			// usortuj to podla poctu prispevkov a nazvu

			Collections.sort(list, new Comparator<RatingBean>() {

				@Override
				public int compare(RatingBean r1, RatingBean r2)
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
	@SuppressWarnings("unchecked")
	public static boolean recoverMessage(int forumId, int docId, Identity user)
	{
		PreparedStatement ps = null;
		List<ForumBean> msgList;
		String fIds = "";
		String sql;
		int parentId = -1;
		ForumBean fb;
		Connection db_conn = null;
		try
		{
			if (user != null)
			{
				fb = getForumBean(null, forumId);
				if (fb != null) parentId = fb.getParentId();

				msgList = getForumFieldsForDoc(null, docId, true, forumId, true, true);
				fIds = getChildForumIdsRecursive(msgList, forumId);
				if (Tools.isNotEmpty(fIds)) fIds += forumId;
				else fIds = Integer.toString(forumId);
				// Logger.println(this,"GROUPS: "+fIds);

				if (user.isAdmin()) sql = "SELECT user_id FROM document_forum WHERE forum_id IN (" + fIds + ")" + CloudToolsForCore.getDomainIdSqlWhere(true) + " AND deleted > 0";
				else sql = "SELECT user_id FROM document_forum WHERE forum_id IN (" + fIds + ")" + CloudToolsForCore.getDomainIdSqlWhere(true) + " AND deleted > 0 AND user_id="
						+ user.getUserId();

				List<Number> usersInForums = null;
				try
				{
					usersInForums = new SimpleQuery().forList(sql);
				}
				catch (Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
					usersInForums = new ArrayList<>();
					if (user.isAdmin() == false) usersInForums.add(Integer.valueOf(user.getUserId()));
				}

				if (user.isAdmin())
				{
					// sql =
					// "DELETE FROM document_forum WHERE forum_id IN ("+fIds+")";
					sql = "UPDATE document_forum SET deleted = 0 WHERE forum_id IN (" + fIds + ")" + CloudToolsForCore.getDomainIdSqlWhere(true) + " AND deleted > 0";
				}
				else
				{
					// sql =
					// "DELETE FROM document_forum WHERE forum_id IN ("+fIds+") AND user_id=?";
					sql = "UPDATE document_forum SET deleted = 0 WHERE forum_id IN (" + fIds + ")" + CloudToolsForCore.getDomainIdSqlWhere(true) + " AND deleted > 0 AND user_id=?";
				}

				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement(sql);
				if (!user.isAdmin()) ps.setInt(1, user.getUserId());
				int updated = ps.executeUpdate();
				// boolean updated = ps.execute();
				ps.close();
				ps = null;
				// if (updated)
				if (updated > 0)
				{
					if (usersInForums != null && usersInForums.isEmpty()==false)
					{
						// zvysim userovi forum rank

						ps = db_conn.prepareStatement("UPDATE users SET forum_rank=forum_rank+1 WHERE user_id=?");
						for (Number n : usersInForums)
						{
							Logger.debug(ForumDB.class, "Zvysujem forum rank pre usera " + n.intValue());
							ps.setInt(1, n.intValue());
							ps.execute();
						}
						ps.close();
						ps = null;
					}

					// zvysim forum_count stranky
					ps = db_conn.prepareStatement("UPDATE documents SET forum_count=forum_count+? WHERE doc_id=?");
					ps.setInt(1, updated);
					ps.setInt(2, docId);
					ps.execute();
					ps.close();
					ps = null;
				}

				db_conn.close();
				db_conn = null;
				// Logger.println(this,"SQL: "+sql);

				updateForumStatInfo(docId, parentId);
				Adminlog.add(Adminlog.TYPE_FORUM_UNDELETE, "Obnoveny prispevok: " + (fb == null ? " ktory neexistuje" : fb.getSubject()), forumId, parentId);
			}
			return (true);

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
		return (false);
	}

	/**
	 * vrati posledne prispevky
	 *
	 * @param docsLength
	 *            - pocet zobrazenych prispevky
	 * @param groupIds
	 *            - id adresara v ktorom sa prispevky nachadzaju
	 * @param includeSubGroups
	 *            - ak je true, beru sa aj podadresare
	 * @return
	 */
	public static List<ForumBean> getLastMessages(int docsLength, String groupIds, boolean includeSubGroups)
	{
		ForumBean fb = null;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<ForumBean> result = new ArrayList<>();
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

		try
		{
			db_conn = DBPool.getConnection();
			StringBuilder sql;
			if (Constants.DB_TYPE == Constants.DB_MSSQL) sql = new StringBuilder("SELECT TOP ").append((docsLength + 10));
			else sql = new StringBuilder("SELECT");
			sql.append(" * FROM document_forum WHERE confirmed=? AND deleted=?" + CloudToolsForCore.getDomainIdSqlWhere(true) + " ORDER BY question_date DESC");
			if (Constants.DB_TYPE == Constants.DB_MYSQL) sql.append(" LIMIT 0,").append((docsLength + 10));
			ps = db_conn.prepareStatement(sql.toString());
			ps.setBoolean(1, true);
			ps.setBoolean(2, false);
			rs = ps.executeQuery();

			SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.getString("dateFormat"));
			SimpleDateFormat timeFormat = new SimpleDateFormat(Constants.getString("timeFormat"));

			DocDetails testDoc;
			DocDB docDB = DocDB.getInstance();

			int poc = 0;
			while (rs.next())
			{
				fb = new ForumBean();
				fb.setForumId(rs.getInt("forum_id"));
				fb.setDocId(rs.getInt("doc_id"));
				fb.setParentId(rs.getInt("parent_id"));
				fb.setSubject(DB.getDbString(rs, "subject"));
				fb.setQuestion(DB.getDbString(rs, "question"));
				fb.setQuestionDate(rs.getTimestamp("question_date"));
				fb.setQuestionDateDisplayDate(dateFormat.format(rs.getTimestamp("question_date")));
				fb.setQuestionDateDisplayTime(timeFormat.format(rs.getTimestamp("question_date")));
				fb.setAutorFullName(DB.getDbString(rs, "author_name"));
				fb.setAutorEmail(DB.getDbString(rs, "author_email"));
				fb.setConfirmed(rs.getBoolean("confirmed"));
				fb.setSendNotif(rs.getBoolean("send_answer_notif"));
				fb.setActive(rs.getBoolean("active"));
				fb.setUserId(rs.getInt("user_id"));
				fb.setFlag(DB.getDbString(rs, "flag"));

				fb.setDeleted(rs.getBoolean("deleted"));

				testDoc = docDB.getBasicDocDetails(fb.getDocId(), false);
				if (testDoc == null || testDoc.isAvailable() == false) continue;

				// kontrola na groupids
				if (doGroupsCheck)
				{
					if (availableGroups.get(testDoc.getGroupId()) == null) continue;
				}

				result.add(fb);

				if (++poc == docsLength) break;
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
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}

		return result;
	}

	/**
	 * Vrati poslednych numberOf prispevkov do fora, ktore je pod web strankou s
	 * identifikatorom docId
	 *
	 * @param numberOf
	 *            - pocet zobrazenych prispevky
	 * @param docId
	 *            - id web stranky (documentu), v ktorom sa forum nachadza
	 *
	 * @author kmarton
	 *
	 * @return arraylist s prispevkami o velkosti numberOf alebo null ak sa
	 *         nepodari kontakt s DB
	 */
	public static List<ForumBean> getLastMessagesFromForum(int numberOf, int docId)
	{
		ForumBean fb = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		List<ForumBean> result = new ArrayList<>();

		try
		{
			db_conn = DBPool.getConnection();
			String sql = "";

			if (Constants.DB_TYPE == Constants.DB_MSSQL) sql = "SELECT TOP " + (numberOf + 10);
			else sql = "SELECT";

			sql += " * FROM document_forum WHERE confirmed = ? AND deleted = ? AND doc_id = ?" + CloudToolsForCore.getDomainIdSqlWhere(true) + " ORDER BY question_date DESC";

			if (Constants.DB_TYPE == Constants.DB_MYSQL) sql += " LIMIT 0," + (numberOf + 10);

			ps = db_conn.prepareStatement(sql);
			ps.setBoolean(1, true);
			ps.setBoolean(2, false);
			ps.setInt(3, docId);
			rs = ps.executeQuery();

			SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.getString("dateFormat"));
			SimpleDateFormat timeFormat = new SimpleDateFormat(Constants.getString("timeFormat"));

			DocDetails testDoc;
			DocDB docDB = DocDB.getInstance();

			int poc = 0;
			while (rs.next())
			{
				fb = new ForumBean();
				fb.setForumId(rs.getInt("forum_id"));
				fb.setDocId(rs.getInt("doc_id"));
				fb.setParentId(rs.getInt("parent_id"));
				fb.setSubject(DB.getDbString(rs, "subject"));
				fb.setQuestion(DB.getDbString(rs, "question"));
				fb.setQuestionDate(rs.getTimestamp("question_date"));
				fb.setQuestionDateDisplayDate(dateFormat.format(rs.getTimestamp("question_date")));
				fb.setQuestionDateDisplayTime(timeFormat.format(rs.getTimestamp("question_date")));
				fb.setAutorFullName(DB.getDbString(rs, "author_name"));
				fb.setAutorEmail(DB.getDbString(rs, "author_email"));
				fb.setConfirmed(rs.getBoolean("confirmed"));
				fb.setSendNotif(rs.getBoolean("send_answer_notif"));
				fb.setActive(rs.getBoolean("active"));
				fb.setUserId(rs.getInt("user_id"));
				fb.setFlag(DB.getDbString(rs, "flag"));

				fb.setDeleted(rs.getBoolean("deleted"));

				testDoc = docDB.getBasicDocDetails(fb.getDocId(), false);
				if (testDoc == null || testDoc.isAvailable() == false) continue;

				result.add(fb);

				if (++poc == numberOf) break;
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
			return null;
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
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return result;
	}

	/**
	 * Vrati identifikator posledneho prispevku od pouzivatela z daneho fora
	 *
	 * @author kmarton
	 * @return
	 */
	public static int getLastUserForumMessageFromForum(int docId, int userId)
	{
		return new SimpleQuery().forInt("SELECT MAX(forum_id) AS forum_id FROM document_forum WHERE doc_id = ? AND user_id = ?" + CloudToolsForCore.getDomainIdSqlWhere(true), docId,
				userId);
	}

	/**
	 * vrati pocet prispekov od konkretneho uzivatela pre zadane forum
	 *
	 * @param docId
	 * @param userId
	 * @return
	 */
	public static int getCountMessagesByUserId(int docId, int userId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int result = 0;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT Count(doc_id) AS pocet FROM document_forum WHERE doc_id = ? AND user_id = ? AND confirmed=? AND deleted=?"
					+ CloudToolsForCore.getDomainIdSqlWhere(true) + "");
			ps.setInt(1, docId);
			ps.setInt(2, userId);
			ps.setBoolean(3, true);
			ps.setBoolean(4, false);
			rs = ps.executeQuery();
			if (rs.next())
			{
				result = rs.getInt("pocet");
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
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}

		return result;
	}

	/**
	 * prepocita a nastavi forum_count pre jednotlive dokumenty v documents
	 */
	public static void recountForumCountForDocIds()
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			HashMap<Integer, Integer> resultMap = new HashMap<>();
			db_conn = DBPool.getConnection();

			// ziskame pocty pre vsetky doc_id ktore maju nejake prispevky
			ps = db_conn.prepareStatement("SELECT doc_id, count(*) as pocet FROM document_forum" + CloudToolsForCore.getDomainIdSqlWhere(true) + " GROUP BY doc_id ORDER BY doc_id");
			rs = ps.executeQuery();
			while (rs.next())
			{
				int docId = rs.getInt("doc_id");
				int pocet = rs.getInt("pocet");
				resultMap.put(docId, pocet);
			}
			rs.close();
			ps.close();
			rs = null;
			ps = null;
			// vyresetujeme vsetky pocty na 0
			ps = db_conn.prepareStatement("UPDATE documents SET forum_count = ?");
			ps.setInt(1, 0);
			ps.executeUpdate();
			ps.close();
			ps = null;
			// nastavime spocitane hodnoty
			for (Entry<Integer, Integer> entry : resultMap.entrySet())
			{
				Integer pocet = entry.getValue();
				ps = db_conn.prepareStatement("UPDATE documents SET forum_count = ? WHERE doc_id = ?");
				ps.setInt(1, pocet);
				ps.setInt(2, entry.getKey());
				ps.executeUpdate();
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

	public static int saveForumQuestion(int docId, int parentId, String subject, String question, String authorName, String email, String remoteIp, boolean sendNotification,
			int userId)
	{
		int pk = -1;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			String sql = "INSERT INTO document_forum (doc_id, parent_id, subject, question, question_date, "
					+ "author_name, author_email, ip, confirmed, hash_code, send_answer_notif, "
					+ "user_id, stat_views, stat_replies, stat_last_post, active, flag, domain_id) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			db_conn = DBPool.getConnection();
			int i = 1;
			ps = db_conn.prepareStatement(sql);
			ps.setInt(i++, docId);
			ps.setInt(i++, parentId);
			ps.setString(i++, subject);
			DB.setClob(ps, i++, question);
			ps.setTimestamp(i++, new Timestamp(Tools.getNow()));

			ps.setString(i++, authorName);
			ps.setString(i++, email);
			ps.setString(i++, remoteIp);
			ps.setBoolean(i++, true);

			ps.setString(i++, Password.generatePassword(15));
			ps.setBoolean(i++, sendNotification);
			ps.setInt(i++, userId);
			ps.setInt(i++, 0);
			ps.setInt(i++, 0);
			ps.setTimestamp(i++, new Timestamp(Tools.getNow()));
			ps.setBoolean(i++, true);
			ps.setString(i++, "");
			ps.setInt(i++, CloudToolsForCore.getDomainId());

			ps.execute();
			ps.close();
			ps = null;
			ps = db_conn.prepareStatement("select max(forum_id) as id from document_forum where user_id = ? AND doc_id = ? AND parent_id = ?"
					+ CloudToolsForCore.getDomainIdSqlWhere(true) + "");
			ps.setInt(1, userId);
			ps.setInt(2, docId);
			ps.setInt(3, parentId);
			rs = ps.executeQuery();

			while (rs.next())
			{
				pk = rs.getInt("id");
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
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}
		return pk;

	}

	/**
	 * Update stlpcu forum_count, pre konkretne doc_id
	 *
	 * @param doc_id
	 * @param forum_count
	 */
	public static void setForumCountForDocIds(int doc_id, int forum_count)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();

			ps = db_conn.prepareStatement("UPDATE documents SET forum_count = ? WHERE doc_id = ?");
			ps.setInt(1, forum_count);
			ps.setInt(2, doc_id);
			ps.executeUpdate();
			ps.close();
			ps = null;

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
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2)
			{}
		}
	}

	public static boolean saveForumComment(int docId, String subjectParam, String question, String authorName, String email, int userId, HttpServletRequest request)
	{
	    boolean requireApprove = false;

		String subject = subjectParam;

		HttpSession session = request.getSession();
        int i;
        String hashCode;

        Identity sender = (Identity) session.getAttribute(Constants.USER_KEY);

        ForumGroupBean fgb = ForumDB.getForum(docId, true);
        if (fgb == null)
        {
            //najdi parent adresar a skus podla neho
            DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
            if (doc != null)
            {
                GroupDetails group = GroupsDB.getInstance().getGroup(doc.getGroupId());
                if (group != null)
                {
                    GroupDetails parentGroup = GroupsDB.getInstance().getGroup(group.getParentGroupId());
                    if (parentGroup!=null)
                    {
                        fgb = ForumDB.getForum(parentGroup.getDefaultDocId(), true);
                    }
                }
            }
        }

        if (fgb == null) {
            //ak je null fgb - podla docid najdi adresar (groupId), pre groupId najdi default stranku a potom nacitaj fgb pre tuto stranku
            DocDetails docDetails = DocDB.getInstance().getDoc(docId);
            if (docDetails != null) {
                int defaultDocId = docDetails.getGroup().getDefaultDocId();
                DocDetails defaultDocDetails = DocDB.getInstance().getDoc(defaultDocId);
                fgb = ForumDB.getForum(defaultDocDetails.getDocId(), true);
            }
        }

        Connection db_conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {

            db_conn = DBPool.getConnection(request);
            String sql;
            int forumId = 0;

            hashCode = Password.generatePassword(15);

            sql = "INSERT INTO document_forum (doc_id, parent_id, subject, question, question_date, "+
                    "author_name, author_email, ip, confirmed, hash_code, send_answer_notif, user_id, flag, domain_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            long insertDate = Tools.getNow();
            i = 1;
            ps = db_conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(i++, docId);
            ps.setInt(i++, 0);
            ps.setString(i++, subject);
            DB.setClob(ps, i++, question);
            ps.setTimestamp(i++, new Timestamp(insertDate));
            if(sender != null && Tools.isEmpty(authorName)) {
                ps.setString(i++, sender.getLogin());
                ps.setString(i++, sender.getEmail());
            } else {
                ps.setString(i++, authorName);
                ps.setString(i++, email);
            }
            ps.setString(i++, Tools.getRemoteIP(request));

            //ak je zapnute potvrdzovanie, nastavim novym prispevkom FALSE
            if (fgb!=null &&  fgb.isMessageConfirmation())
                ps.setBoolean(i++, false);
            else
                ps.setBoolean(i++, true);

            ps.setString(i++, hashCode);
            ps.setBoolean(i++, false); // isSendNotif?
            ps.setInt(i++, userId);
            ps.setString(i++, ""); // forumForm.getFlag
            ps.setInt(i++, CloudToolsForCore.getDomainId());
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if(rs.next())
            {
                forumId = rs.getInt(1);
            }
            rs.close();
            ps.close();
            ps = null;
            int fId = ForumDB.getForumMessageParent(9, docId);
            //podteme zvysim statistiku a nastavim datum last_post na aktualny
            if (fId > 0)
            {
                sql = "UPDATE document_forum SET stat_replies=stat_replies+1, stat_last_post=? " +
                        "WHERE forum_id=? AND parent_id=-1"+CloudToolsForCore.getDomainIdSqlWhere(true);

                ps = db_conn.prepareStatement(sql);
                ps.setTimestamp(1, new Timestamp(insertDate));
                ps.setInt(2, fId);
                ps.execute();
                ps.close();
                ps = null;
            }
            Adminlog.add(Adminlog.TYPE_FORUM_CREATE, "Vytvoreny prispevok: "+subject, -1, docId);

			  db_conn.close();
			  db_conn = null;


            if (fgb == null)
            {
                String forumDefaultApproveEmail = Constants.getString("forumDefaultApproveEmail");
                String forumDefaultNotifyEmail = Constants.getString("forumDefaultNotifyEmail");

                if (Tools.isNotEmpty(forumDefaultApproveEmail) || Tools.isNotEmpty(forumDefaultNotifyEmail))
                {
                    fgb = new ForumGroupBean();
                    fgb.setMessageConfirmation(false);
                    if (Tools.isNotEmpty(forumDefaultApproveEmail))
                    {
                        fgb.setApproveEmail(forumDefaultApproveEmail);
                        fgb.setMessageConfirmation(true);
                    }
                    if (Tools.isNotEmpty(forumDefaultNotifyEmail))
                    {
                        fgb.setNotifEmail(forumDefaultNotifyEmail);
                    }
                }
            }

			  ForumDB.sendConfirmationNotificationEmail(fgb, forumId, -1, subject, question, docId, authorName, email, hashCode, request);


            String fromName = Constants.getString("forumNotifySenderName");
            if(Tools.isEmpty(fromName)) fromName = authorName;
            String fromEmail = Constants.getString("forumNotifySenderEmail");
            if (Tools.isEmpty(fromEmail))	fromEmail = email;

            if (Tools.isNotEmpty(fromName))
            {
                //uloz meno a email do cookies
                Cookie forumName = new Cookie("forumname", Tools.URLEncode(fromName));
                forumName.setPath("/");
                forumName.setMaxAge(60 * 24 * 3600);
                forumName.setHttpOnly(true);
                Cookie forumEmail = new Cookie("forumemail", Tools.URLEncode(fromEmail));
                forumEmail.setPath("/");
                forumEmail.setMaxAge(60 * 24 * 3600);
                forumEmail.setHttpOnly(true);

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

        return requireApprove;
    }

    public static void sendConfirmationNotificationEmail(ForumGroupBean fgb, int forumId, int forumParentId, String forumSubject, String forumQuestion, int docId, String authorName, String authorEmail, String hashCode, HttpServletRequest request)
	 {
	 	HttpSession session = request.getSession();
		 Prop prop = Prop.getInstance(request);

		 //skusim ziskat notifyEmail (email autora prispevku v docman) na ktory sa ma poslat notifikacia
		 PageParams pageParams = (session.getAttribute("forumOpenPageParams") != null ? (PageParams)session.getAttribute("forumOpenPageParams") : null);
		 if (pageParams == null) pageParams = new PageParams();
		 UserDetails notifyUserDetail = null;
		 if (pageParams.getValue("notifyEmail", null)!=null) notifyUserDetail = UsersDB.getUserByEmail(pageParams.getValue("notifyEmail", null));

		 if(fgb == null)
		 {
			 fgb = new ForumGroupBean();
			 fgb.setMessageConfirmation(false);
		 }
		 if(notifyUserDetail != null)
		 {
			 fgb.setNotifEmail(notifyUserDetail.getEmail());
		 }
		 //zistim ci ukladam v docman
		 boolean isDocman = pageParams.getBooleanValue("docman", false);
		 DocDB docDB = DocDB.getInstance();

		 DocDetails doc = docDB.getDoc(fgb.getDocId());
		 String docTitle = "";
		 String docData = "";
		 String docUrl = "/showdoc.do?docid="+docId;
		 if (doc != null)
		 {
			 docTitle = doc.getTitle();
			 docData = doc.getData();
			 if (Tools.isNotEmpty(doc.getVirtualPath())) docUrl = doc.getVirtualPath();
		 }
		 String fromName = Constants.getString("forumNotifySenderName");
		 if(Tools.isEmpty(fromName)) fromName = authorName;
		 String fromEmail = Constants.getString("forumNotifySenderEmail");
		 if (Tools.isEmpty(fromEmail))	fromEmail = authorEmail;
		 if (Tools.isEmpty(fromEmail)) fromEmail = "info@"+Tools.getServerName(request);
		 String subject = (!isDocman ? prop.getText("components.forum.email_subject") : prop.getText("components.forum.docman_email_subject", docTitle));
		 String messageHlavicka = prop.getText("components.forum.hlavicka");
		 String messagePaticka = prop.getText("components.forum.paticka");

		 String messageLink = "";
		 if(docId > CalendarDB.FORUM_OFFSET)
		 {
			 String calendarUrl = prop.getText("components.calendar.url",String.valueOf(docId-CalendarDB.FORUM_OFFSET));
			 messageLink = Tools.getBaseHref(request) + calendarUrl;
		 }else
			 messageLink = Tools.getBaseHref(request) + Tools.addParameterToUrl(docUrl, "forum", "open");

		 //moznost do request atributu zadat kluc, ktory sa pouzije na text spravy. Napr. na nastenke chceme mat iny text ako v diskusii na stranke
		 String messageKey = null;
		 if (request.getAttribute("forumNotifyMessageKey")!=null)
		 {
			 String messageKeyVerify = (String)request.getAttribute("forumNotifyMessageKey");
			 String messageText = prop.getText(messageKeyVerify);
			 if (messageKeyVerify.equals(messageText)==false)
			 {
				 //kluc existuje, pouzijeme ho aj s parametrami
				 messageKey = messageKeyVerify;
			 }
		 }

		 boolean approveMailSent = false;
		 if(fgb != null && fgb.isMessageConfirmation() && Tools.isNotEmpty(fgb.getApproveEmail()))
		 {
			 //pre linku na vymazanie sa ku hasCode prida DEL_CONST a potom v ForumApproveAction sa testuje, ci je na konci DEL_CONST
			 //ak ano, vykona sa delete, inak sa vykona approve
			 final int DEL_CONST = 33;

			 String toEmail = fgb.getApproveEmail();

			 String approveLink = Tools.getBaseHref(request) + "/admin/forumApprove.do?forumId="+forumId + "&forumCode=" + hashCode;
			 String delLink = Tools.getBaseHref(request) + "/admin/forumApprove.do?forumId="+forumId + "&forumCode=" + (hashCode+DEL_CONST);

			 String message = messageHlavicka +
			 "<p>"+(!isDocman ? prop.getText("components.forum.email_subject") : prop.getText("components.forum.docman_email_subject", docTitle))+".<br/>"+
			 "DocID: " + docId+
			 "</p>" +
			 "<table border='0' cellpadding='0' cellspacing='0'>"+
			 "<tr><td style='vertical-align: top;'>"+ prop.getText("components.forum.message_name")+": </td><td>"+docTitle+"</td></tr>"+
			 "<tr><td>"+ prop.getText("components.forum.author")+": </td><td>"+authorName+"</td></tr>"+

			 "<tr><td>"+ prop.getText("components.forum.message_text")+": </td><td>"+forumQuestion+"</td></tr>"+
			 "</table>"+
			 "<p>"+prop.getText("components.forum.message_approve_link") + ": <a href='"+approveLink+"'>"+approveLink+"</a><br/>" +
			 prop.getText("components.forum.message_delete_link") + ": <a href='"+delLink+"'>"+delLink+"</a></p>" +
			 messagePaticka;
			 SendMail.send(fromName, fromEmail, toEmail, subject, message);
			 approveMailSent = true;
		 }

		 boolean sendNotif = true;
		 //ak je pri fore uvedeny notif_email, odosle sa kazdy prispevok na danu adresu, ak sa vsak na tu adresu
		 //uz odoslal potvrdzovaci mail, notifikacia sa neodosle
		 if( fgb != null && Tools.isNotEmpty(fgb.getNotifEmail()) && !approveMailSent)
		 {
			 if (fgb.getNotifEmail().equals(fgb.getApproveEmail()))
			 {
				 sendNotif = false;
			 }

			 if (sendNotif)
			 {
				 String toEmail = fgb.getNotifEmail();

				 String message;
				 if (messageKey!=null)
				 {
					 message = prop.getText(messageKey, messageLink, docTitle, docData, authorName, forumQuestion);
				 }
				 else
				 {
					 message = messageHlavicka+
					 "<p>"+(!isDocman ? prop.getText("components.forum.email_subject") : prop.getText("components.forum.docman_email_subject", docTitle))+".<br/>"+
					 //docId poslem len adminovi
					 ((!isDocman || (notifyUserDetail != null && notifyUserDetail.isAdmin())) ? "DocID: " + docId : "")+
					 "</p>" +
					 "<table border='0' cellpadding='0' cellspacing='0'>"+
					 "<tr><td style='vertical-align: top;'>"+ prop.getText("components.forum.message_name")+": </td><td>"+docTitle+"</td></tr>"+
					 "<tr><td>"+ prop.getText("components.forum.author")+": </td><td>"+authorName+"</td></tr>"+

					 "<tr><td>"+ prop.getText("components.forum.message_text")+": </td><td>"+forumQuestion+"</td></tr>"+
					 "</table>"+
					 (!isDocman ? ("<p>"+prop.getText("components.forum.open_forum") + ": <br/><a href='"+messageLink+"'>"+messageLink+"</a></p>") : "") +
					 messagePaticka;
				 }

				 SendMail.send(fromName, fromEmail, toEmail, subject, message);
			 }
		 }


		 //zistim, ci je novy prispevok odpoved na existujuci, ak ano, ceknem ci ma aktivnu notifikaciu
		 if(forumParentId > 0)
		 {
			 ForumBean fb =  ForumDB.getForumBean(request, forumParentId);
			 if (Tools.isEmpty(authorEmail)) authorEmail = fb.getAutorEmail();
			 if (fb != null && fb.isSendNotif() && Tools.isNotEmpty(fb.getAutorEmail()) && Tools.isEmail(authorEmail))
			 {
				 fromEmail = authorEmail;
				 String toEmail = fb.getAutorEmail();
				 subject = (!isDocman ? prop.getText("components.forum.email_answer_subject") : prop.getText("components.forum.docman_email_answer_subject", docDB.getBasicDocDetails(docId, true).getTitle()));

				 String message;
				 if (messageKey!=null)
				 {
					 message = prop.getText(messageKey, messageLink, docTitle, docData, authorName, forumQuestion);
				 }
				 else
				 {

					 message = messageHlavicka+
					 "<p>"+(!isDocman ? prop.getText("components.forum.email_answer_subject") : prop.getText("components.forum.docman_email_answer_subject", docDB.getBasicDocDetails(docId, true).getTitle()))+ ".</p>" +
					 "<table border='0' cellpadding='0' cellspacing='0'>"+
					 "<tr><td style='vertical-align: top;'>"+ prop.getText("components.forum.answer_message_name")+": </td><td>"+fb.getSubject()+"</td></tr>"+
					 "<tr><td>"+ prop.getText("components.forum.email_your_question")+": </td><td>"+fb.getQuestion()+"</td></tr>"+
					 "<tr><td>"+ prop.getText("components.forum.email_answer")+": </td><td>"+forumSubject+"</td></tr>"+
					 "<tr><td>"+ prop.getText("components.forum.answer_author")+": </td><td>"+authorName+"</td></tr>"+
					 "<tr><td>"+ prop.getText("components.forum.answer_text")+": </td><td>"+forumQuestion+"</td></tr>"+
					 "</table>"+
					 (!isDocman ? ("<p>"+prop.getText("components.forum.open_forum") + ": <br/><a href='"+messageLink+"'>"+messageLink+"</a></p>") : "") +
					 messagePaticka;
				 }
				 SendMail.send(fromName, fromEmail, toEmail, subject, message);
			 }
		 }
	 }
}