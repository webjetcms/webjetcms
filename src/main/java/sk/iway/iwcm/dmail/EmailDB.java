package sk.iway.iwcm.dmail;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.EmailToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UserDetails;
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
			ps = db_conn.prepareStatement("SELECT DISTINCT(disabled) as disabled FROM emails WHERE campain_id = ? AND domain_id=?");
			ps.setInt(1, campaignId);
			ps.setInt(2, CloudToolsForCore.getDomainId());
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
				ps = db_conn.prepareStatement("SELECT count(email_id) as to_send FROM emails WHERE campain_id = ? AND domain_id=? AND sent_date IS NULL");
				ps.setInt(1, campaignId);
				ps.setInt(2, CloudToolsForCore.getDomainId());
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
			new SimpleQuery().execute("UPDATE emails SET sent_date = ?, retry = ?, disabled = ? WHERE campain_id = ? AND domain_id=?", null, 0, false, campaignId, CloudToolsForCore.getDomainId());
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
			new SimpleQuery().execute("UPDATE emails SET disabled = ? WHERE campain_id = ? AND domain_id=?", disabled, campainId, CloudToolsForCore.getDomainId());
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
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
			sql.append("SELECT * FROM emails_unsubscribed WHERE domain_id=?");
			if (searchString != null)
				sql.append(" AND email LIKE ?");

			ps = db_conn.prepareStatement(sql.toString());

			ps.setInt(1, CloudToolsForCore.getDomainId());
			if (searchString != null)
				ps.setString(2, "%" + searchString + "%");

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
		return EmailToolsForCore.deleteUnsubscribedEmail(id);
	}

	public static boolean deleteUnsubscribedEmail(String email)
	{
		return EmailToolsForCore.deleteUnsubscribedEmail(email);
	}

	public static boolean addUnsubscribedEmail(String email)
	{
		if (!EmailDB.deleteUnsubscribedEmail(email))//najprv skusi email vymazat, ak uz existuje
			return false;

		return EmailToolsForCore.addUnsubscribedEmail(email);
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
			ps = db_conn.prepareStatement("SELECT * FROM emails_unsubscribed WHERE domain_id=?");
			ps.setInt(1, CloudToolsForCore.getDomainId());
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
			ps = db_conn.prepareStatement("SELECT recipient_email FROM emails WHERE email_id=? AND domain_id=?");
			ps.setInt(1, emailId);
			ps.setInt(2, CloudToolsForCore.getDomainId());
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
			ps = db_conn.prepareStatement("DELETE FROM emails WHERE campain_id= ? and recipient_email = ? AND domain_id=?");

			for (EmailUnsubscribedBean eb : emails)
			{
				ps.setInt(1, campaignId);
				ps.setString(2, eb.getEmail());
				ps.setInt(3, CloudToolsForCore.getDomainId());
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
		ArrayList<UserDetails> users = new ArrayList<>();
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
				Set<String> uzMamEmailsTable = DmailUtil.getUnsubscribedEmails();
				for (UserDetails ud : users)
				{

					try
					{
						userUrl = url + "&userid=" + ud.getUserId();
						recipientEmail = ud.getEmail().toLowerCase();
						recipientName = ud.getFullName();

						if (recipientEmail == null || recipientEmail.indexOf('@')==-1 || uzMamEmailsTable.contains(recipientEmail))
						{
							continue;
						}
						uzMamEmailsTable.add(recipientEmail.toLowerCase());

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

	/**
	 * You can use this main method in cron to periodically send emails.
	 * It expect one argument in pageParams format. Params are:
	 * - groupId - userGroupId
	 * - url - url to be sent in email
	 * - senderName - name of sender
	 * - senderEmail - email of sender
	 * - subject - subject of email
	 * - instanceId - unique ID of instance to verify last sending, it's verified against Audit DMAIL_AUTOSENDER
	 * @param args
	 */
	public static void main(String[] args)
	{
		Logger.println(EmailDB.class, "EMailAction main:");
		if (args != null && args.length > 0)
		{
			for (int i = 0; i < args.length; i++)
				Logger.println(EmailDB.class,"   args[" + i + "]=" + args[i]);

			PageParams pageParams = new PageParams(args[0]);

			long createDate = Tools.getNow();
			int userGroup = pageParams.getIntValue("groupId", -1);
			StringBuilder url = new StringBuilder(pageParams.getValue("url", null));
			String senderName = pageParams.getValue("senderName", null);
			String senderEmail = pageParams.getValue("senderEmail", null);
			String subject = pageParams.getValue("subject", null);
			int instanceId = pageParams.getIntValue("instanceId", 0);

			//otestuj, kedy sme boli naposledy spusteny
			long lastDate = Adminlog.getLastDate(Adminlog.TYPE_DMAIL_AUTOSENDER, instanceId);

			if (url.indexOf("?") == -1)
				url.append('?');
			else
				url.append('&');

			url.append("lastDmailDate=").append(lastDate);

			EmailDB.saveEmails(userGroup, url.toString(), senderName, senderEmail, subject, null, -1, new Timestamp(createDate), null, null);
			EmailDB.activateEmails(createDate);

			Adminlog.add(Adminlog.TYPE_DMAIL_AUTOSENDER, instanceId, "sending email " + url, userGroup, instanceId);
		}
	}
}