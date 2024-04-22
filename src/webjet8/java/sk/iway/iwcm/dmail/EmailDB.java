package sk.iway.iwcm.dmail;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.RowSetDynaClass;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;


/**
 *  EmailDB.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: bhric $
 *@version      $Revision: 1.19 $
 *@created      Date: 5.10.2005 16:24:20
 *@modified     $Date: 2009/10/07 12:08:54 $
 */
public class EmailDB
{
	protected EmailDB() {
		//utility class
	}

	public static List<DynaBean> getEmail(HttpSession session)
	{
		String sql = "SELECT c.*, u.first_name, u.last_name FROM  emails_campain c LEFT JOIN users u ON c.created_by_user_id = u.user_id ORDER BY create_date DESC";
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			if(db_conn != null)
			{
				ps = db_conn.prepareStatement(sql);
				rs = ps.executeQuery();

				RowSetDynaClass rsdc = new RowSetDynaClass(rs);
				rs.close();
				ps.close();
				rs = null;
				ps = null;

				return(rsdc.getRows());
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
				if (db_conn != null)
					db_conn.close();
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return(new ArrayList<>());
	}

	public static List<DynaBean> getEmailFromEmails(HttpSession session)
	{
		String sql = " SELECT count(e.email_id) AS countt, max(e.sent_date) AS esent_date, e.create_date AS ecreate_date, c.*"+
						 " FROM emails_campain c  RIGHT JOIN emails e ON e.create_date=c.create_date"+
						 " WHERE e.sent_date IS NOT NULL"+
		             " GROUP BY e.create_date" +
		             " ORDER BY e.create_date DESC";

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			if(db_conn != null)
			{
				ps = db_conn.prepareStatement(sql);
				rs = ps.executeQuery();

				RowSetDynaClass rsdc = new RowSetDynaClass(rs);

				rs.close();
				ps.close();

				rs = null;
				ps = null;

				return(rsdc.getRows());
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
				if (db_conn != null)
					db_conn.close();
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return(new ArrayList<>());
	}

	/**
	 * Zaznamena statistiku kliknutia z emailu
	 * @param emailId
	 */
	public static void addStatClick(int emailId, String url, String params, HttpServletRequest request, HttpServletResponse response)
	{
		if (Tools.isNotEmpty(params))
		{
			try
			{
				//webjetDmsp= je vzdy na konci parametrov
				int i = params.indexOf(Constants.getString("dmailStatParam"));
				if (i > 0)
				{
					url = url + "?" + params.substring(0, i);
					if (url.endsWith("&"))
						url = url.substring(0, url.length()-1);
				}
			}
			catch (RuntimeException e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("INSERT INTO emails_stat_click (email_id, link, click_date, session_id, browser_id) VALUES (?, ?, ?, ?, ?)");
			ps.setInt(1, emailId);
			ps.setString(2, DB.prepareString(url, 255));
			ps.setTimestamp(3, new Timestamp(Tools.getNow()));
			ps.setLong(4, StatDB.getSessionId(request));
			ps.setLong(5, StatDB.getBrowserId(request, response));

			ps.execute();

			ps.close();
			db_conn.close();

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
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
	}

	/**
	 * Ziska stav kampane, vrati hodnoty:
	 *  - unknown (neznama kampan)
	 *  - disabled (kampan je zastavena)
	 *  - enabled (kampan je spustena)
	 *  - all_sent (vsetky emaily odoslane)
	 * @param campaignId
	 * @return
	 */
	public static String getStaus(int campaignId)
	{
		String status = "unknown";

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT DISTINCT(disabled) as disabled FROM emails WHERE campain_id = ?");
			ps.setInt(1, campaignId);
			rs = ps.executeQuery();
			if (rs.next())
			{
				if (rs.getBoolean("disabled"))
					status="disabled";
				else
					status = "enabled";
			}
			rs.close();
			ps.close();

			//zisti ci nahodou uz nie je vsetko odoslane
			if ("enabled".equals(status))
			{
				ps = db_conn.prepareStatement("SELECT count(email_id) as to_send FROM emails WHERE campain_id = ? AND sent_date IS NULL");
				ps.setInt(1, campaignId);
				rs = ps.executeQuery();
				if (rs.next())
				{
					if (rs.getInt("to_send") == 0)
						status="all_sent";
				}
				rs.close();
				ps.close();
			}

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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return(status);
	}

	/**
	 * Znova odosle emaily kampane (moze sa zmenit obsah stranky a znova odoslat)
	 * @param campaignId
	 */
	public static void resendEmail(int campaignId)
	{
		deleteUnsubscribedEmailsFromCampaign(campaignId);

		try
		{
			new SimpleQuery().execute("UPDATE emails SET sent_date = ?, retry = ?, disabled = ? WHERE campain_id = ?", null, 0, false, campaignId);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Aktivovanie / deaktivovanie kampane
	 * @param disabled
	 * @param campainId
	 */
	public static void activateDisableEmails(boolean disabled, int campainId)
	{
		deleteUnsubscribedEmailsFromCampaign(campainId);

		try
		{
			new SimpleQuery().execute("UPDATE emails SET disabled = ? WHERE campain_id = ?", disabled, campainId);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	public static boolean createEmail(String recipientEmail, String recipientName, String senderName, String senderEmail, String subject, String userUrl, String attachments, int createdByUserId, Timestamp createDate, int campaignId, String replyTo, String ccEmail, String bccEmail, int recipientUserId, boolean disabled)
	{
		boolean saveOK = false;

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			String sql = "INSERT INTO emails (recipient_email, recipient_name, sender_name, sender_email, subject, url, attachments, created_by_user_id, create_date, disabled, campain_id, reply_to, cc_email, bcc_email, recipient_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			ps = db_conn.prepareStatement(sql);
			ps.setString(1, recipientEmail);
			ps.setString(2, recipientName);
			ps.setString(3, senderName);
			ps.setString(4, senderEmail);
			ps.setString(5, subject);
			ps.setString(6, userUrl);
			DB.setClob(ps, 7, attachments);
			ps.setInt(8, createdByUserId);
			ps.setTimestamp(9, createDate);
			ps.setBoolean(10, disabled);
			ps.setInt(11, campaignId);
			ps.setString(12, replyTo);
			ps.setString(13, ccEmail);
			ps.setString(14, bccEmail);
			ps.setInt(15, recipientUserId);
			ps.execute();

			saveOK = true;
		}
		catch(Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		finally
		{
			try
			{
				if(ps != null)
					ps.close();
				if(db_conn != null)
					db_conn.close();
			}
			catch(Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		return saveOK;
	}

	/**
	 * Naplni bean z result setu
	 * @param e
	 * @param rs
	 * @throws SQLException
	 */
	public static void fillEmailCampainBean(EmailCampainBean e, ResultSet rs) throws SQLException
	{
		e.setEmailsCampainId(rs.getInt("emails_campain_id"));
		e.setSenderName(DB.getDbString(rs, "sender_name"));
		e.setSenderEmail(DB.getDbString(rs, "sender_email"));
		e.setSubject(DB.getDbString(rs, "subject"));
		e.setUrl(DB.getDbString(rs, "url"));
		e.setCreatedByUserId(rs.getInt("created_by_user_id"));
		e.setCreateDate(rs.getDate("create_date"));
		e.setCountOfRecipients(rs.getInt("count_of_recipients"));
		e.setCountOfSentEmails(rs.getInt("count_of_sent_mails"));
		e.setLastSentDate(rs.getDate("last_sent_date"));
		e.setUserGroups(DB.getDbString(rs, "user_groups"));

		e.setSendAt(rs.getDate("send_at"));
		e.setAttachments(DB.getDbString(rs, "attachments"));
		e.setReplyTo(DB.getDbString(rs, "reply_to"));
		e.setCcEmail(DB.getDbString(rs, "cc_email"));
		e.setBccEmail(DB.getDbString(rs, "bcc_email"));
	}

	/**
	 * Ziska zadanu kampan
	 * @param campainId
	 * @return
	 */
	public static EmailCampainBean getCampaign(int campainId)
	{
		EmailCampainBean e = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM emails_campain WHERE emails_campain_id = ?");
			ps.setInt(1, campainId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				e = new EmailCampainBean();
				fillEmailCampainBean(e, rs);
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return e;
	}

	/**
	 * Ziska zoznam kampani (obsah tabulky emails_campain)
	 * @return
	 */
	public static List<EmailCampainBean> getCampaigns()
	{
		List<EmailCampainBean> campains = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM emails_campain ORDER BY emails_campain_id DESC");
			rs = ps.executeQuery();
			while (rs.next())
			{
				EmailCampainBean e = new EmailCampainBean();
				fillEmailCampainBean(e, rs);
				campains.add(e);
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return campains;
	}

	/**
	 * Prida do mailingu novy zaznam, vrati Hashtabulku so zoznamom uz pridanych zaznamov
	 * @param allreadyHasEmails - hashtabulka uz pridanych zaznamov, ak je prazdna, nacita sa z DB
	 * @param campain
	 * @param emailDetails
	 * @return
	 */
	public static boolean addToMailing(Map<String, Integer> allreadyHasEmails, EmailCampainBean campain, EMailDetails emailDetails)
	{
		if (emailDetails == null || Tools.isEmpty(emailDetails.getEmail()))
			return false;

		if (allreadyHasEmails != null && allreadyHasEmails.get(emailDetails.getEmail()) != null)
			return false;

		boolean saveOK = false;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			if (allreadyHasEmails == null || allreadyHasEmails.size() < 1)
			{
				if (allreadyHasEmails == null) allreadyHasEmails = new Hashtable<>();
				ps = db_conn.prepareStatement("SELECT recipient_email, recipient_user_id FROM emails WHERE campain_id=?");
				ps.setInt(1, campain.getEmailsCampainId());
				rs = ps.executeQuery();
				while (rs.next())
				{
					allreadyHasEmails.put(rs.getString("recipient_email"), rs.getInt("recipient_user_id"));
				}
				rs.close();
				ps.close();
			}


			if (allreadyHasEmails!=null && allreadyHasEmails.get(emailDetails.getEmail())!=null)
			{
				return false;
			}

			String userURL = campain.getUrl() + "&userid=" + emailDetails.getUserId();
			saveOK = createEmail(emailDetails.getEmail(), emailDetails.getFull_name(), campain.getSenderName(), campain.getSenderEmail(), campain.getSubject(), userURL, campain.getAttachments(), campain.getCreatedByUserId(), new Timestamp(campain.getCreateDate().getTime()), campain.getEmailsCampainId(), campain.getReplyTo(), campain.getCcEmail(), campain.getBccEmail(), emailDetails.getUserId(), true);
			if (saveOK)
			{
				allreadyHasEmails.put(emailDetails.getEmail(), emailDetails.getUserId());
				//aktualizuj pocet prijemcov v mail kampani
				ps = db_conn.prepareStatement("UPDATE emails_campain SET count_of_recipients=? WHERE emails_campain_id=?");
				ps.setInt(1, allreadyHasEmails.size());
				ps.setInt(2, campain.getEmailsCampainId());
				ps.execute();
				ps.close();
			}

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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return saveOK;
	}

	public static boolean preGenerate(int eid)
	{
		boolean result = false;

		String sql;
		int realnyPocetPrijemcov = 0;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("DELETE FROM emails WHERE campain_id=?");
			ps.setInt(1, eid);
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("SELECT * FROM emails_campain WHERE emails_campain_id=?");
			ps.setInt(1, eid);
			rs = ps.executeQuery();
			List<UserDetails> usersList = new ArrayList<>();
			EmailCampainBean e = new EmailCampainBean();
			if (rs.next())
			{
					fillEmailCampainBean(e, rs);
					String[] users = e.getUserGroups().split(",");
					int[] usersNum = new int[users.length];
					for(int i = 0; i < users.length; i++){
						usersNum[i] = Tools.getIntValue(users[i], -1);
					}
					usersList = EmailDB.getUsersByGroup(usersNum);
			}
			rs.close();
			ps.close();
			if(!usersList.isEmpty()){
				//EMailAction.saveEmails(usersList, e.getUrl(), e.getSenderName(), e.getSenderEmail(), e.getReplyTo(), e.getCcEmail(), e.getBccEmail(), e.getSubject(), e.getAttachments(), e.getCreatedByUserId(), new Timestamp(e.getCreateDate().getTime()), null, null, null);
				String userUrl;
				String recipientEmail;
				String recipientName;
				Map<String, Integer> uzMamEmailsTable = new Hashtable<>();
				for (UserDetails ud : usersList)
				{
					//if (ud.isAuthorized()==false || ud.isAllowDateLogin()==false) continue;

					try
					{
						userUrl = e.getUrl() + "&userid=" + ud.getUserId();
						recipientEmail = ud.getEmail();
						recipientName = ud.getFullName();

						if (recipientEmail == null || recipientEmail.indexOf('@')==-1 || uzMamEmailsTable.get(recipientEmail)!=null)
						{
							continue;
						}
						uzMamEmailsTable.put(recipientEmail, 1);

						//if (out!=null) out.println("<hr>");

						Logger.println(EmailDB.class,"fill: " + recipientName + " email=" + recipientEmail);


						sql = "INSERT INTO emails (recipient_email, recipient_name, sender_name, sender_email, subject, url, attachments, created_by_user_id, create_date, disabled, campain_id, reply_to, cc_email, bcc_email, recipient_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
						ps = db_conn.prepareStatement(sql);
						ps.setString(1, recipientEmail);
						ps.setString(2, recipientName);
						ps.setString(3, e.getSenderName());
						ps.setString(4, e.getSenderEmail());
						ps.setString(5, e.getSubject());
						ps.setString(6, userUrl);
						DB.setClob(ps, 7, e.getAttachments());
						ps.setInt(8, e.getCreatedByUserId());
						ps.setTimestamp(9, new Timestamp(e.getCreateDate().getTime()));
						ps.setBoolean(10, true);
						ps.setInt(11, e.getEmailsCampainId());
						ps.setString(12, e.getReplyTo());
						ps.setString(13, e.getCcEmail());
						ps.setString(14, e.getBccEmail());
						ps.setInt(15, ud.getUserId());
						ps.execute();
						ps.close();

						realnyPocetPrijemcov++;

						//ps.close();

					}
					catch (Exception ex)
					{
						sk.iway.iwcm.Logger.error(ex);
					}
				}
			}

			realnyPocetPrijemcov -= deleteUnsubscribedEmailsFromCampaign(eid);

			sql = "UPDATE emails_campain SET count_of_recipients=? WHERE emails_campain_id=?";
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, realnyPocetPrijemcov);
			ps.setInt(2, e.getEmailsCampainId());
			ps.execute();
			ps.close();

			db_conn.close();

			rs = null;
			ps = null;
			db_conn = null;

			result = true;
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return (result);
	}

	public static void fillUnsubscribedEmailBean(EmailUnsubscribedBean e, ResultSet rs) throws SQLException
	{
		e.setEmailsUnsubscribedId(rs.getInt("emails_unsubscribed_id"));
		e.setEmail(rs.getString("email"));
		e.setCreateDate(DB.getDate(rs, "create_date"));
	}

	public static List<EmailUnsubscribedBean> getUnsubscribedEmail(String searchString)
	{
		List<EmailUnsubscribedBean> list = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM emails_unsubscribed");
			if (searchString != null)
				sql.append(" WHERE email LIKE ?");

			ps = db_conn.prepareStatement(sql.toString());

			if (searchString != null)
				ps.setString(1, "%" + searchString + "%");

			rs = ps.executeQuery();
			EmailUnsubscribedBean e;
			while (rs.next())
			{
				e = new EmailUnsubscribedBean();
				fillUnsubscribedEmailBean(e, rs);
				list.add(e);
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return (list);
	}

	public static boolean deleteUnsubscribedEmail(int id)
	{
		try
		{
			new SimpleQuery().execute("DELETE FROM emails_unsubscribed WHERE emails_unsubscribed_id = ?", id);
			Adminlog.add(Adminlog.TYPE_DATA_DELETING, "Unsubscribed email deleted, id="+id, id, -1);
			return true;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			return false;
		}
	}

	public static boolean deleteUnsubscribedEmail(String email)
	{
		try
		{
			new SimpleQuery().execute("DELETE FROM emails_unsubscribed WHERE email = ?", email);
			Adminlog.add(Adminlog.TYPE_DATA_DELETING, "Unsubscribed email deleted, email="+email, -1, -1);
			return true;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			return false;
		}
	}

	public static boolean addUnsubscribedEmail(String email)
	{
		if (!EmailDB.deleteUnsubscribedEmail(email))//najprv skusi email vymazat, ak uz existuje
			return false;

		try
		{
			new SimpleQuery().execute("INSERT INTO emails_unsubscribed (email, create_date) VALUES (?,?)", email, new Date());
			return true;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			return false;
		}
	}

	public static Map<String, Integer> getHashtableFromUnsubscribedEmails()
	{
		Map<String, Integer> hashTable = new Hashtable<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM emails_unsubscribed");
			rs = ps.executeQuery();
			while (rs.next())
			{
				hashTable.put(DB.getDbString(rs, "email").toLowerCase(), 1);
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return (hashTable);
	}


	/**
	 * Prida do mailingu prienik dvoch zoznamov
	 * @param allreadyHasEmails - hashtabulka uz pridanych zaznamov, ak je prazdna, nacita sa z DB
	 * @param campain
	 * @param emailDetails
	 * @return
	 */
	public static int addConjToMailing(Map<String, Integer> allreadyHasEmails,Map<String, Integer> table, EmailCampainBean campain)
	{
		int pocetPridanych = 0;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			if (allreadyHasEmails == null || allreadyHasEmails.size() < 1)
			{
				if (allreadyHasEmails == null)
					allreadyHasEmails = new Hashtable<>();

				ps = db_conn.prepareStatement("SELECT recipient_email, recipient_user_id FROM emails WHERE campain_id=?");
				ps.setInt(1, campain.getEmailsCampainId());
				rs = ps.executeQuery();
				while (rs.next())
				{
					allreadyHasEmails.put(rs.getString("recipient_email"), rs.getInt("recipient_user_id"));
				}

				rs.close();
				ps.close();

				if (allreadyHasEmails != null && allreadyHasEmails.size() > 0)
				{
					for (Map.Entry<String, Integer> entry : allreadyHasEmails.entrySet())
					{
						String email = entry.getKey();

                  if (table.get(email) == null)// ak nie je v prvom, alebo nie je v druom vybere, tak ho odstranime
                  {
                  	deleteEmail(campain.getEmailsCampainId(), entry.getValue(), allreadyHasEmails.size()-1);
                  	allreadyHasEmails.remove(email);
                  	pocetPridanych++;
                  }
					}
				}
			}

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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return pocetPridanych;
	}

	/**
	 * Prida do mailingu rozdiel dvoch zoznamov
	 * @param allreadyHasEmails - hashtabulka uz pridanych zaznamov, ak je prazdna, nacita sa z DB
	 * @param campain
	 * @param emailDetails
	 * @return
	 */
	public static int addNegToMailing(Map<String, Integer> allreadyHasEmails, Map<String, Integer> table,EmailCampainBean campain)
	{
		int pocetPridanych = 0;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			if (allreadyHasEmails == null || allreadyHasEmails.size() < 1)
			{
				if (allreadyHasEmails==null) allreadyHasEmails = new Hashtable<>();
				ps = db_conn.prepareStatement("SELECT recipient_email, recipient_user_id FROM emails WHERE campain_id=?");
				ps.setInt(1, campain.getEmailsCampainId());
				rs = ps.executeQuery();
				while (rs.next())
				{
					allreadyHasEmails.put(rs.getString("recipient_email"), rs.getInt("recipient_user_id"));
				}
				rs.close();
				ps.close();

				if (allreadyHasEmails != null && allreadyHasEmails.size()>0 )
				{
					for (Map.Entry<String, Integer> entry : allreadyHasEmails.entrySet())
					{
						String email = entry.getKey();

						if (table.get(email) != null)//ak je email v prvom vybere tak ho odstranime
						{
							deleteEmail(campain.getEmailsCampainId(), entry.getValue(), allreadyHasEmails.size()-1);
							allreadyHasEmails.remove(email);
							pocetPridanych++;
						}
					}
				}
			}

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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return pocetPridanych;
	}

	private static void deleteEmail(int campaignId, int userId, int newSize)
	{
		try
		{
			new SimpleQuery().execute("DELETE from emails WHERE campain_id = ? AND recipient_user_id = ? ", campaignId, userId);
			new SimpleQuery().execute("UPDATE emails_campain SET count_of_recipients = ? WHERE emails_campain_id= ? ", newSize, campaignId);//aktualizuj pocet prijemcov v mail kampani
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Vymaze email kampan vratane prijemcov
	 */
	public static void deleteEmailCampaign(int emailCampaignId)
	{
		try
		{
			new SimpleQuery().execute("DELETE from emails_campain WHERE emails_campain_id = ? ", emailCampaignId);
			new SimpleQuery().execute("DELETE from emails WHERE campain_id = ? ", emailCampaignId);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Vymaze prijemcov kampane
	 */
	public static void deleteEmailCampaignRecipients(int emailCampaignId)
	{
		try
		{
			new SimpleQuery().execute("DELETE FROM emails WHERE campain_id = ? ", emailCampaignId);
			new SimpleQuery().execute("UPDATE emails_campain SET count_of_recipients = 0 WHERE emails_campain_id = ? ", emailCampaignId);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Vrati email pre zadane email_id z tabulky emails, pouziva sa na kontrolu emailu pre odhlasenie
	 * @param emailId
	 * @return
	 */
	public static String getEmail(int emailId)
	{
		String email = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT recipient_email FROM emails WHERE email_id=?");
			ps.setInt(1, emailId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				email = DB.getDbString(rs, "recipient_email");
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return email;
	}

	/**
	 * Vymaze odhlasene emaily z danej kampane, je potrebne vykonat pred znovaspustenim kampane
	 * @param campaignId
	 * @return pocet zmazanych emailov
	 */
	public static int deleteUnsubscribedEmailsFromCampaign(int campaignId)
	{
		int pocetZmazanych = 0;
		List<EmailUnsubscribedBean> emails = getUnsubscribedEmail(null);

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("DELETE FROM emails WHERE campain_id= ? and recipient_email = ?");

			for (EmailUnsubscribedBean eb : emails)
			{
				ps.setInt(1, campaignId);
				ps.setString(2, eb.getEmail());
				int count = ps.executeUpdate();
				pocetZmazanych += count;
			}

			ps.close();
			db_conn.close();
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
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return pocetZmazanych;
	}

	/**
	 * Vrati zoznam prijemcov e-mail kampane ako pouzivatelov systemu reprezentovanych objektom {@link UserDetails}. Pre tych, co nie su registrovani v systeme sa vrati iba e-mail adresa
	 * v poli ForumRank je vratena hodnota email_id
	 *
	 * @param campaignId	identifikator e-mailovej kampane
	 *
	 * @return ak dana kampan neexistuje, vrati sa null
	 */
	public static List<UserDetails> getUsersInCampaign(int campaignId)
	{
		if (campaignId < 0)
			return null;

		String sql = "SELECT u.user_id, u.title, u.first_name, u.last_name, u.user_groups, e.recipient_email, e.email_id, e.recipient_name " +
						"FROM users u RIGHT JOIN emails e ON u.user_id = e.recipient_user_id WHERE e.campain_id = ? ORDER BY email_id DESC";

      List<UserDetails> campaignUsers = new ComplexQuery().setSql(sql).setParams(campaignId).list(new Mapper<UserDetails>()
      {
      	UserGroupsDB userGroupsDB = UserGroupsDB.getInstance();

         @Override
			public UserDetails map(ResultSet rs) throws SQLException
         {
         	UserDetails user = new UserDetails();

         	user.setUserId(rs.getInt("user_id"));
         	if (user.getUserId()>0)
         	{
	         	user.setTitle(DB.getDbString(rs, "title"));
					user.setFirstName(DB.getDbString(rs, "first_name"));
					user.setLastName(DB.getDbString(rs, "last_name"));
         	}
         	else
         	{
         		user.splitFullName(DB.getDbString(rs, "recipient_name"));
         	}
				user.setUserGroupsIds(DB.getDbString(rs, "user_groups"));
				user.setUserGroupsNames(userGroupsDB.convertIdsToNames(user.getUserGroupsIds()));

				user.setEmail(DB.getDbString(rs, "recipient_email"));

				user.setForumRank(rs.getInt("email_id"));

         	return user;
         }
      });

		return campaignUsers;
	}

	/**
	 * Aktualizuje e-mailovu kampan. Najprv nastavi jej zakladne parametre (prilohy sa neaktualizuju, tie sa iba prekopiruju, nedaju zmenit), potom sa vymazu vsetky e-maily prijemcov a
	 * nakoniec sa nove e-maily ulozia do tabulky emails. A uplne na konci sa aktualizuje pocet prijemcov e-mail kampane
	 *
	 * @param campaignId				identifikator kampane
	 * @param users					novy zoznam prijemcov
	 * @param url						url adresa
	 * @param senderName				meno odosielatela
	 * @param senderEmail			e-mail odosielatela
	 * @param replyTo					e-mail na odpoved
	 * @param ccEmail					kopia
	 * @param bccEmail				tajna kopia
	 * @param subject					predmet e-mailu
	 * @param userId					identifikator aktualne prihlaseneho pouzivatela
	 * @param userGroupsString		retazec skupin, v ktorych sa pouzivatel nachadza
	 * @param attachments			prilohy
	 *
	 * @return	Ak e-mailova kampan neexistuje, vrati false, inak vrati true
	 */
   public static boolean updateEmails(int campaignId, List<UserDetails> users, String url, String senderName, String senderEmail, String replyTo, String ccEmail, String bccEmail, String subject, int userId, String userGroupsString, String attachments)
   {
		if (campaignId < 0)
			return false;

		List<Object> params = new ArrayList<>();

		if (!Tools.isEmail(replyTo))
			replyTo = null;
		if (!Tools.isEmail(ccEmail))
			ccEmail = null;
		if (!Tools.isEmail(bccEmail))
			bccEmail = null;

		/**
		 * Aktualizacia emailovej kampane
		 */
		params.clear();
		params.add(senderName);
		params.add(senderEmail);
		if(users != null) params.add(users.size());
		else params.add(0);
		params.add(subject);
		params.add(url);
		params.add(userGroupsString);
		params.add(replyTo);
		params.add(ccEmail);
		params.add(bccEmail);
		params.add(campaignId);

		new SimpleQuery().execute("UPDATE emails_campain SET sender_name = ?, sender_email = ?, count_of_recipients = ?, subject = ?, url = ?, user_groups = ?, reply_to = ?, cc_email = ?, bcc_email = ? WHERE emails_campain_id = ?", params.toArray());

		//zoznam odhlasenych emailov
		Map<String, Integer> uzMamEmailsTable = EmailDB.getHashtableFromUnsubscribedEmails();
		int realnyPocetPrijemcov = 0;

		//ziskaj zoznam aktualnych prijemcov kampane ako list aj hashtabulku
		List<UserDetails> actualRecipients = EmailDB.getUsersInCampaign(campaignId);
		Map<String, UserDetails> actualRecipientsTable = new Hashtable<>();
		for (UserDetails ud : actualRecipients)
		{
			actualRecipientsTable.put(ud.getEmail().toLowerCase(), ud);
			Logger.debug(EmailDB.class, "Existing emails in campaign " + campaignId + ": " + ud.getEmail());

			//aktualizuj hodnoty aktualnym prijemcom
			String userUrl = url + "&userid=" + ud.getUserId();
			params.clear();
			params.add(senderName);
			params.add(senderEmail);
			params.add(subject);
			params.add(userUrl);
			params.add(userId);

			params.add(replyTo);
			params.add(ccEmail);
			params.add(bccEmail);
			params.add(campaignId);
			params.add(ud.getForumRank());

			new SimpleQuery().execute("UPDATE emails SET sender_name=?, sender_email=?, subject=?, url=?, created_by_user_id=?, reply_to=?, cc_email=?, bcc_email=?, campain_id=? WHERE email_id=?", params.toArray());

			uzMamEmailsTable.put(ud.getEmail().toLowerCase(), 1);

			realnyPocetPrijemcov++;
		}

		String recipientEmail;
		String recipientName;

		if (users != null)
		{

			for (UserDetails ud : users)
			{
				recipientEmail = ud.getEmail();
				recipientName = ud.getFullName();

				//odstranme zo zoznamu povodnych emailov aby sme vedeli co sa vymazalo
				actualRecipientsTable.remove(ud.getEmail());

				if (recipientEmail == null || recipientEmail.indexOf('@')==-1 || uzMamEmailsTable.get(recipientEmail) != null)
					continue;

				uzMamEmailsTable.put(recipientEmail, 1);

				String userUrl = url + "&userid=" + ud.getUserId();

				params.clear();
				params.add(recipientEmail);
				params.add(recipientName);
				params.add(senderName);
				params.add(senderEmail);
				params.add(subject);
				params.add(userUrl);
				params.add(attachments);
				params.add(userId);
				params.add(new Timestamp(Tools.getNow()));
				params.add(false);
				params.add(campaignId);
				params.add(replyTo);
				params.add(ccEmail);
				params.add(bccEmail);
				params.add(ud.getUserId());

				Logger.debug(EmailDB.class, "inserting email "+recipientEmail);

				new SimpleQuery().execute("INSERT INTO emails (recipient_email, recipient_name, sender_name, sender_email, subject, url, attachments, created_by_user_id, create_date, disabled, campain_id, reply_to, cc_email, bcc_email, recipient_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", params.toArray());

				realnyPocetPrijemcov++;
			}
		}

		//ty co nam zostali v actualRecipientsTable boli zmazany
		for (UserDetails ud : actualRecipientsTable.values())
		{
			new SimpleQuery().execute("DELETE FROM emails WHERE email_id=? AND campain_id=?", ud.getForumRank(), campaignId);
			realnyPocetPrijemcov--;
		}

		//	aktualizuj realny pocet prijemcov
		new SimpleQuery().execute("UPDATE emails_campain SET count_of_recipients = ? WHERE emails_campain_id = ?", realnyPocetPrijemcov, campaignId);

		return(true);
	}

   /**
 	 * Vymaze zaznam prijemcu e-mailovej kampane z tabulky emails a zmensi pocet prijemcov o 1
 	 *
 	 * @param deleteEmailId	identifikator prijemcu
 	 * @param campaignId		identifikator e-mail kampane
 	 * @param count			pocet prijemcov kampane
 	 *
 	 * @return true ak vymazanie z databazy prebehlo v poriadku, inak false
 	 */
	public static boolean deleteRecipient(int deleteEmailId, int campaignId, int count)
	{
		try
		{
			new SimpleQuery().execute("DELETE FROM emails WHERE email_id = ?", deleteEmailId);
			new SimpleQuery().execute("UPDATE emails_campain SET count_of_recipients = ? WHERE emails_campain_id = ? ", (count - 1), campaignId);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * Naplnenie tabulky emailov
	 *
	 * @param userGroupId - id skupiny pousivatelov, ktorym sa mail posiela
	 * @param url - URL adresa webu (http://www.interway.sk/showdoc.do?docid=10) - aby zbehol loopback connect
	 * @param senderName - meno odosielatela
	 * @param senderEmail - email odosielatela
	 * @param subject - predmet
	 * @param attachments - zoznam priloh oddelenych ; (linka na subor na disku)
	 * @param userId - id pouzivatela, ktory kampan vytvara
	 * @param createDate - datum vytvorenia (podla toho je potom potrebne zavolat runEmails)
	 * @param prop - I18N prop (alebo null ak sa nic nevypisuje)
	 *
	 * @return
	 */
	public static boolean saveEmails(int userGroupId, String url, String senderName, String senderEmail, String subject, String attachments, int userId, Timestamp createDate, Prop prop, PrintWriter out)
	{
		List<UserDetails> users = new ArrayList<>();
		for (UserDetails ud : UsersDB.getUsersByGroup(userGroupId))
		{
			if (ud.isAuthorized() == false || ud.isAllowDateLogin() == false)
				continue;
			users.add(ud);
		}

		return(saveEmails(users, url, senderName, senderEmail, null, null, null, subject, attachments, userId, createDate, null));
	}

	public static boolean saveEmails(List<UserDetails> users, String url, String senderName, String senderEmail, String replyTo, String ccEmail, String bccEmail, String subject, String attachments, int userId, Timestamp createDate, String userGroupsString)
	{
		boolean sendOK = false;

		if (Tools.isEmail(replyTo) == false)
			replyTo = null;
		if (Tools.isEmail(ccEmail) == false)
			ccEmail = null;
		if (Tools.isEmail(bccEmail) == false)
			bccEmail = null;

		Connection dbConn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			dbConn = DBPool.getConnection();

			String sql = "INSERT INTO emails_campain (sender_name, sender_email,count_of_recipients, subject, url, created_by_user_id, create_date, user_groups, send_at, attachments, reply_to, cc_email, bcc_email) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			ps = dbConn.prepareStatement(sql);
			ps.setString(1, senderName);
			ps.setString(2, senderEmail);
			if (users != null)
				ps.setInt(3, users.size());
			else ps.setInt(3, 0);
			ps.setString(4, subject);
			ps.setString(5, url);
			ps.setInt(6, userId);
			ps.setTimestamp(7, createDate);
			ps.setString(8, userGroupsString);
			ps.setTimestamp(9, null);
			DB.setClob(ps, 10, attachments);
			ps.setString(11, replyTo);
			ps.setString(12, ccEmail);
			ps.setString(13, bccEmail);
			ps.execute();
			ps.close();

			int campaignId = -1;

			ps = dbConn.prepareStatement("SELECT max(emails_campain_id) FROM emails_campain WHERE created_by_user_id=?");
			ps.setInt(1, userId);
			rs = ps.executeQuery();
			if (rs.next())
			{
				campaignId = rs.getInt(1);
			}
			rs.close();
			ps.close();

			String userUrl;
			String recipientEmail;
			String recipientName;

			int realnyPocetPrijemcov = 0;
			if (users != null)
			{
				Map<String, Integer> uzMamEmailsTable = EmailDB.getHashtableFromUnsubscribedEmails();
				for (UserDetails ud : users)
				{

					try
					{
						userUrl = url + "&userid=" + ud.getUserId();
						recipientEmail = ud.getEmail().toLowerCase();
						recipientName = ud.getFullName();

						if (recipientEmail == null || recipientEmail.indexOf('@')==-1 || uzMamEmailsTable.get(recipientEmail)!=null)
						{
							continue;
						}
						uzMamEmailsTable.put(recipientEmail.toLowerCase(), 1);

						Logger.println(EmailDB.class,"fill: " + recipientName + " email=" + recipientEmail);

						sql = "INSERT INTO emails (recipient_email, recipient_name, sender_name, sender_email, subject, url, attachments, created_by_user_id, create_date, disabled, campain_id, reply_to, cc_email, bcc_email, recipient_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
						ps = dbConn.prepareStatement(sql);
						ps.setString(1, recipientEmail);
						ps.setString(2, recipientName);
						ps.setString(3, senderName);
						ps.setString(4, senderEmail);
						ps.setString(5, subject);
						ps.setString(6, userUrl);
						DB.setClob(ps, 7, attachments);
						ps.setInt(8, userId);
						ps.setTimestamp(9, createDate);
						ps.setBoolean(10, true);
						ps.setInt(11, campaignId);
						ps.setString(12, replyTo);
						ps.setString(13, ccEmail);
						ps.setString(14, bccEmail);
						ps.setInt(15, ud.getUserId());
						ps.execute();
						ps.close();

						realnyPocetPrijemcov++;

					}
					catch (Exception ex)
					{
						sk.iway.iwcm.Logger.error(ex);
					}
				}

			}
			sendOK = true;

			//	aktualizuj realny pocet prijemcov
			sql = "UPDATE emails_campain SET count_of_recipients=? WHERE emails_campain_id = ?";
			ps = dbConn.prepareStatement(sql);
			ps.setInt(1, realnyPocetPrijemcov);
			ps.setInt(2, campaignId);
			ps.execute();
			ps.close();

			rs = null;
			ps = null;
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
				if (dbConn != null)
					dbConn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return(sendOK);
	}

	/**
	 * Aktivovanie odosielania emailov so zadanym datumom
	 * @param date
	 */
	public static void activateEmails(long date)
	{
		try
		{
		    //spustime v okoli sekundy kvoli zaokruhlovaniu v DB
			new SimpleQuery().execute("UPDATE emails SET disabled = ? WHERE create_date > ? AND create_date < ?", false, new Timestamp(date-1000), new Timestamp(date+1000));
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	public static List<UserDetails> getUsersByGroup(int[] userGroupIds)
	{
		DebugTimer dt = new DebugTimer("getUsersByGroup");

		List<UserDetails> users = new ArrayList<>();
		Map<String, UserDetails> usersTable = new Hashtable<>();
		UserGroupsDB userGroupsDB = UserGroupsDB.getInstance();

		Connection dbConn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			dt.diff(" nacitavam users");

			dbConn = DBPool.getConnection();
			ps = dbConn.prepareStatement("SELECT user_id, title, first_name, last_name, user_groups, email, authorized, allow_login_start, allow_login_end FROM users"); //ORDER BY last_name, first_name
			rs = ps.executeQuery();

			dt.diff(" mam rs");

			UserDetails usr;
			while (rs.next())
			{
				usr = new UserDetails();
				usr.setUserId(rs.getInt("user_id"));
				usr.setTitle(DB.getDbString(rs, "title"));
				usr.setFirstName(DB.getDbString(rs, "first_name"));
				usr.setLastName(DB.getDbString(rs, "last_name"));
				usr.setUserGroupsIds(DB.getDbString(rs, "user_groups"));
				usr.setUserGroupsNames(userGroupsDB.convertIdsToNames(usr.getUserGroupsIds()));

				usr.setEmail(DB.getDbString(rs, "email"));

				usr.setAuthorized(rs.getBoolean("authorized"));

				usr.setAllowLoginStart(DB.getDbDate(rs, "allow_login_start"));
				usr.setAllowLoginEnd(DB.getDbDate(rs, "allow_login_end"));

				usr.setAllowDateLogin(LogonTools.checkAllowLoginDates(rs));

				if (usr.isAuthorized() && usr.isAllowDateLogin() && Tools.isNotEmpty(usr.getEmail()) && usr.getEmail().length()>4 && usersTable.get(usr.getEmail())==null)
				{
					if (userGroupIds == null || userGroupIds.length==0)
					{
						users.add(usr);
						usersTable.put(usr.getEmail(), usr);
					}
					else
					{
						for (int i = 0; i < userGroupIds.length; i++)
						{
							if (usr.isInUserGroup(userGroupIds[i]))
							{
								users.add(usr);
								usersTable.put(usr.getEmail(), usr);
								break;
							}
						}
					}
				}
			}
			rs.close();
			ps.close();
			dbConn.close();

			rs = null;
			ps = null;
			dbConn = null;

			dt.diff("rs nacitane");
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
				if (dbConn != null)
					dbConn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return (users);
	}

	/**
	 * Zisti z DB z tabulky emails, ze ci je email aktivovany, ci sa bude posielat, ak je disabled 1, email sa uz poslal resp. sa nebude posielat
	 *
	 * @param campaignId			id emailovej kampane
	 * @param recipientEmail	email adresata
	 *
	 * @return	vrati true, ak je e-mail deaktivovany, inak vrati false. Ak je cislo e-mail kampane < 1 alebo je prazdny/null email adresata, potom vrati true.
	 */
	public static boolean isEmailDisabled(int campaignId, String recipientEmail)
	{
		if (Tools.isEmpty(recipientEmail) || campaignId < 1)
			return true;

		boolean isEmailDisabled = true;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT disabled FROM emails WHERE campain_id = ? AND recipient_email = ?");
			int psCounter = 1;
			ps.setInt(psCounter++, campaignId);
			ps.setString(psCounter++, recipientEmail);
			rs = ps.executeQuery();
			while (rs.next())
			{
				isEmailDisabled = rs.getBoolean("disabled");
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return isEmailDisabled;
	}

}