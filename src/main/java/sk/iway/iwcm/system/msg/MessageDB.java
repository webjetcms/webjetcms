package sk.iway.iwcm.system.msg;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.UpdateAllQuery;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  MessageDB.java - treida drziaca zoznam odkazov
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: thaber $
 *@version      $Revision: 1.9 $
 *@created      Date: 24.2.2006 23:47:06
 *@modified     $Date: 2010/02/24 16:31:26 $
 */
public class MessageDB
{
	private static final String CONTEXT_NAME = "sk.iway.iwcm.system.msg.MessageDB";

	public synchronized static MessageDB getInstance(boolean forceRefresh)
	{
		//try to get it from server space
		if (forceRefresh == false)
		{
			if (Constants.getServletContext()!=null && Constants.getServletContext().getAttribute(CONTEXT_NAME) != null)
			{
				//Logger.println(this,"DocDB: getting from server space");
				MessageDB messageDB = (MessageDB) Constants.getServletContext().getAttribute(CONTEXT_NAME);

				return (messageDB);
			}
		}
		return (new MessageDB());
	}

	private MessageDB()
	{
		//remove
		Constants.getServletContext().removeAttribute(CONTEXT_NAME);
		//save us to server space
		Constants.getServletContext().setAttribute(CONTEXT_NAME, this);

		ClusterDB.addRefresh(MessageDB.class);
	}

	public boolean saveMessage(HttpSession session, AdminMessageBean msg)
	{
		boolean saveOK = false;
		try
		{
			EntityManager em = JpaTools.getEntityManager();
			em.getTransaction().begin();
			em.persist(msg);

		   msg.setCreateDate(new Date(Tools.getNow()));

		   Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
		   if (user != null)
		   {
		   	msg.setCreateByUserId(Integer.valueOf(user.getUserId()));
		   }
		   em.getTransaction().commit();
			saveOK = true;

			//	refreshni DB
			getInstance(true);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return(saveOK);
	}

	public boolean saveMessage(AdminMessageBean msg)
	{
		boolean saveOK = false;
		try
		{
			EntityManager em = JpaTools.getEntityManager();
			em.getTransaction().begin();
			em.persist(msg);

			msg.setCreateDate(new Date(Tools.getNow()));

			em.getTransaction().commit();
			saveOK = true;

			//	refreshni DB
			getInstance(true);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return(saveOK);
	}

	public List<AdminMessageBean> getUnreadedMessages(HttpSession session)
	{
		Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
		if (user == null) return(new ArrayList<AdminMessageBean>());
		return(getUnreadedMessages(user.getUserId()));
	}

	public List<AdminMessageBean> getUnreadedMessages(int userId)
	{
		return this.getUnreadedMessages(userId, true);
	}

	public List<AdminMessageBean> getUnreadedMessages(int userId, boolean setSeen) {
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		ExpressionBuilder builder = new ExpressionBuilder();
		ReadAllQuery dbQuery = new ReadAllQuery(AdminMessageBean.class, builder);
		Expression expr = builder.get("recipientUserId").equal(userId);
		expr = expr.and(builder.get("isReaded").equal(null));
		dbQuery.setSelectionCriteria(expr);
		dbQuery.addOrdering(builder.get("createDate").ascending());

		em.getTransaction().begin();
		Query query = em.createQuery(dbQuery);
		List<AdminMessageBean> messages = JpaDB.getResultList(query);

		if (setSeen){
				boolean mamZmenu = false;
			for (int i = 0; i < messages.size(); i++) {
				mamZmenu = true;
				messages.get(i).setReaded(Boolean.TRUE);
			}
			if (mamZmenu)
				em.getTransaction().commit();
			else
				em.getTransaction().rollback();
		}

		return messages;
	}

	public AdminMessageBean getMessage(int messageId)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		return em.find(AdminMessageBean.class, messageId);
	}

	public List<AdminMessageBean> getAllMessages()
	{
		return(getAllMessages(-1));
	}

	public List<AdminMessageBean> getAllMessages(int userId)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		ExpressionBuilder builder = new ExpressionBuilder();
		ReadAllQuery dbQuery = new ReadAllQuery(AdminMessageBean.class, builder);
		if(userId>0)
		{
			Expression expr = builder.get("recipientUserId").equal(userId);
			dbQuery.setSelectionCriteria(expr);
		}
		dbQuery.addOrdering(builder.get("createDate").descending());
		dbQuery.setMaxRows(40);

		Query query = em.createQuery(dbQuery);
		List<AdminMessageBean> messages = JpaDB.getResultList(query);

		return(messages);
	}

	/** vrati vsetky spravy pre dannych dvoch uzivatelov od tejto spravy
	 * @param session
	 * @param messageId
	 * @param equal ak je true tak buden hladat iba od messageId spravy
	 * @return
	 */
	public List<AdminMessageBean> getMessages(HttpSession session, int messageId, boolean equal)
	{
		Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
		AdminMessageBean am = getMessage(messageId);
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		ExpressionBuilder builder = new ExpressionBuilder();

		ReadAllQuery dbQuery = new ReadAllQuery(AdminMessageBean.class, builder);
		Expression ex = builder.get("createByUserId").equal(am.getCreateByUserId());
		ex = ex.and(builder.get("recipientUserId").equal(am.getRecipientUserId()));
		Expression ex2 = builder.get("createByUserId").equal(am.getRecipientUserId());
		ex2 = ex2.and(builder.get("recipientUserId").equal(am.getCreateByUserId()));
		Expression expr = ex.or(ex2);
		if(!equal)
			expr = expr.and(builder.get("createDate").greaterThan(am.getCreateDate()));
		else
			expr = expr.and(builder.get("createDate").greaterThanEqual(am.getCreateDate()));

		dbQuery.setSelectionCriteria(expr);
		dbQuery.setMaxRows(40);

		em.getTransaction().begin();
		Query query = em.createQuery(dbQuery);
		List<AdminMessageBean> messages = JpaDB.getResultList(query);

		for(int i=0;i<messages.size();i++)
		{
			if(messages.get(i).getRecipientUserId().intValue()==user.getUserId())
				messages.get(i).setReaded(Boolean.TRUE);
		}
		em.getTransaction().commit();
		return messages;
	}

	public static List<AdminMessageBean> getMessagesWithUser(HttpSession session, int toUserId,int limit)
	{
		Identity user = (Identity)session.getAttribute(Constants.USER_KEY);

		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		ExpressionBuilder builder = new ExpressionBuilder();

		ReadAllQuery dbQuery = new ReadAllQuery(AdminMessageBean.class, builder);
		Expression ex = builder.get("createByUserId").equal(user.getUserId());
		ex = ex.and(builder.get("recipientUserId").equal(toUserId));
		Expression ex2 = builder.get("createByUserId").equal(toUserId);
		ex2 = ex2.and(builder.get("recipientUserId").equal(user.getUserId()));
		Expression expr = ex.or(ex2);

		dbQuery.setSelectionCriteria(expr);
		if(limit > 0)
		{
			dbQuery.setMaxRows(limit);
		}
		dbQuery.addOrdering(builder.get("createDate").ascending());
		Query query = em.createQuery(dbQuery);

		return JpaDB.getResultList(query);
	}

	public static void setMessagesRead(int createByUserId, HttpSession session,long timeTo)
	{
		UserDetails user = UsersDB.getCurrentUser(session);
		new SimpleQuery().execute("UPDATE admin_message SET is_readed = "+DB.getBooleanSql(true)+" WHERE recipient_user_id = ? AND create_by_user_id = ?", user.getUserId(), createByUserId);
	}

	public static void setMessageRead(HttpSession session,int messageId)
	{
		UserDetails user = UsersDB.getCurrentUser(session);
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		ExpressionBuilder builder = new ExpressionBuilder();

		UpdateAllQuery dbQuery = new UpdateAllQuery(AdminMessageBean.class, builder);
		Expression ex = builder.get("recipientUserId").equal(user.getUserId());
		ex = ex.and(builder.get("admin_message_id").equal(messageId));

		dbQuery.setSelectionCriteria(ex);
		dbQuery.addUpdate("isReaded", true);

		em.getTransaction().begin();
		Query query = em.createQuery(dbQuery);
		query.executeUpdate();
		em.getTransaction().commit();
	}

	public static Map<Integer,Integer> getCountOfUnreadMessages(int user_id) {
		Map<Integer,Integer> map = new HashMap<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			db_conn = DBPool.getConnection();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT create_by_user_id, COUNT(*) as POCET FROM admin_message ");
			sql.append("WHERE recipient_user_id=? AND (is_readed = "+DB.getBooleanSql(false)+" OR is_readed IS NULL) ");
			sql.append("GROUP BY create_by_user_id");
			ps = db_conn.prepareStatement(sql.toString());
			ps.setInt(1, user_id);
			rs = ps.executeQuery();
			while (rs.next()) {
				map.put(rs.getInt("create_by_user_id"),rs.getInt("pocet"));
			}
			rs.close();
			ps.close();

			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			} catch (Exception ex2) {

			}
		}
		return map;
	}

	public static List<Integer> getAllContactsWhichUserSentMessage(int userId, List<Integer> alreadyHave) {
		List<Integer> ret = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			db_conn = DBPool.getConnection();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT DISTINCT create_by_user_id, recipient_user_id FROM admin_message WHERE create_by_user_id = ?");
			if (alreadyHave != null && !alreadyHave.isEmpty())
				sql.append(" AND recipient_user_id NOT IN (" + StringUtils.join(alreadyHave,",") + ")");
			ps = db_conn.prepareStatement(sql.toString());
			ps.setInt(1, userId);
			rs = ps.executeQuery();
			while (rs.next()) {
				ret.add(rs.getInt("recipient_user_id"));
			}
			rs.close();
			ps.close();

			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			} catch (Exception ex2) {

			}
		}
		return ret;
	}

	public static List<Integer> getAllContactsWhichUserReceivedMessage(int userId, List<Integer> alreadyHave) {
		List<Integer> ret = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			db_conn = DBPool.getConnection();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT DISTINCT recipient_user_id, create_by_user_id FROM admin_message WHERE recipient_user_id = ?");
			if (alreadyHave != null && !alreadyHave.isEmpty())
				sql.append(" AND create_by_user_id NOT IN (" + StringUtils.join(alreadyHave,",") + ")");
			ps = db_conn.prepareStatement(sql.toString());
			ps.setInt(1, userId);
			rs = ps.executeQuery();
			while (rs.next()) {
				ret.add(rs.getInt("create_by_user_id"));
			}
			rs.close();
			ps.close();

			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			} catch (Exception ex2) {

			}
		}
		return ret;
	}

}