package sk.iway.iwcm.system.adminlog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.audit.AuditNotifyEntity;

/**
 *	AdminlogNotifyManager.java - vykonava pracu s databazou, posiela e-mail
 *	@Title        webjet4
 *	@Company      Interway s.r.o. (www.interway.sk)
 *	@Copyright    Interway s.r.o. (c) 2001-2008
 *	@author       $Author: jeeff $
 *	@version      $Revision: 1.4 $
 *	@created      Date: 28.01.2009 12:05:51
 *	@modified     $Date: 2009/02/09 10:18:13 $
 */
public class AdminlogNotifyManager
{
	protected AdminlogNotifyManager() {
		//utility class
	}

	/**
 	 * Posle notifikacne spravy na vsetky e-maily, ktore su priradene k danej akcii v tabulke adminlog_notify
 	 * @param logType typ adminlogu, ktory sa pridal do DB a chceme na nho poslat notifikacie
 	 * @param requestBean dolezite informacie z requestu, ktore zahrnieme v sprave (IP adresa, domena, id pouzivatela...)
 	 * @param timestamp datum a cas, kedy bola akcia vykonana
 	 * @param description opis akcie, ktora bola zaznamenana
 	 * @return true, ak sa poslu nejake notifikacie, false ak nebol poslany ziadny email
 	 */
	public static boolean sendNotification(int logType, RequestBean requestBean, Timestamp timestamp, String description, boolean writeToAuditLog)
	{
		List<AuditNotifyEntity> notifyBeans = AdminlogNotifyManager.getNotifyEmails(logType);

		if (notifyBeans.size() == 0)
			return(false);

		Prop prop = Prop.getInstance();

		String subject = (prop.getText(("components.adminlog.subject")) + prop.getText(("components.adminlog."+logType)));
		StringBuilder text = new StringBuilder(prop.getText(("components.adminlog.action"))).append(' ').append(prop.getText(("components.adminlog."+logType))).append('\n');
		text.append(prop.getText(("components.adminlog.datetime"))).append(' ').append(Tools.formatDateTime(timestamp.getTime())).append('\n');
		text.append(prop.getText(("components.adminlog.userid"))).append(' ').append(requestBean.getUserId()).append('\n');
		text.append(prop.getText(("components.adminlog.description"))).append(' ').append(description).append('\n');
		if (requestBean.getRemoteIP() != null)
			text.append(prop.getText(("components.adminlog.IPaddress"))).append(' ').append(requestBean.getRemoteIP()).append('\n');
		if (requestBean.getRemoteHost() != null)
			text.append(prop.getText(("components.adminlog.domain"))).append(' ').append(requestBean.getRemoteHost()).append('\n');

		String descIntToLower = DB.internationalToEnglish(description.toLowerCase());
		for(AuditNotifyEntity anb : notifyBeans)
		{
			//ak sa ma poslat email len ak obsahuje nejaky text
			if(Tools.isNotEmpty(anb.getText()))
				if(!descIntToLower.contains(DB.internationalToEnglish(anb.getText().toLowerCase()))) continue;

			SendMail.sendCapturingException(prop.getText("components.adminlog.senderEmail.name"), anb.getEmail(), anb.getEmail(), null, null, null, subject, text.toString(), null, null, true, writeToAuditLog);
		}

		return(true);
	}

	/**
 	 * Vyselektuje vsetky e-maily, na ktore je potrebne poslat notifikaciu o zaznamenanom adminlogu
 	 * @param logType typ adminlogu, ktory sa pridal do DB a chceme na nho poslat notifikacie
 	 * @return List naplneny e-mailami, na ktore sa posle notifikacna sprava
 	 */
	private static synchronized List<AuditNotifyEntity> getNotifyEmails(int logType)
	{

		String CACHE_KEY = "AdminlogNotifyEmails.type"+logType;
		Cache c = Cache.getInstance();
		@SuppressWarnings("unchecked")
		List<AuditNotifyEntity> notifyBeans = (List<AuditNotifyEntity>)c.getObject(CACHE_KEY);

		if (notifyBeans != null) return notifyBeans;

		notifyBeans = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM adminlog_notify WHERE adminlog_type = ?");

			ps.setInt(1, logType);

			rs = ps.executeQuery();
			AuditNotifyEntity anb = null;
			while (rs.next())
			{
				anb = new AuditNotifyEntity();
				anb.setId(rs.getLong("adminlog_notify_id"));
				anb.setAdminlogType(rs.getInt("adminlog_type"));
				anb.setEmail(rs.getString("email"));
				anb.setText(rs.getString("text"));
				notifyBeans.add(anb);
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

		c.setObjectSeconds(CACHE_KEY, notifyBeans, 60*60);

		return notifyBeans;
	}
}
