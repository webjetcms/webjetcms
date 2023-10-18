package sk.iway.iwcm.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.stat.StatDB;

public class EmailToolsForCore {
    /**
     * sem sa uklada hash o aktualnom userovi, kontroluje to ShowDocAction
     * (ak sa v url nachadza id pouzivatela pre jeho prihlasenie)
     */
    public static String ACTUAL_USER_HASH = null;

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
        if (!deleteUnsubscribedEmail(email))//najprv skusi email vymazat, ak uz existuje
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

    public static void addStatOpen(int emailId)
    {
        if(emailId < 0){
            Logger.debug(EmailToolsForCore.class, "email id lesser than 0, can't add stat open!");
            return;
        }

        Date seenDate = new SimpleQuery().forDate("SELECT seen_date FROM emails WHERE email_id = ?", emailId);
        if(seenDate == null){
            Logger.debug(EmailToolsForCore.class, "Adding seend email stat");
            new SimpleQuery().execute("UPDATE emails SET seen_date = ? where email_id = ?", new Timestamp(System.currentTimeMillis()),emailId);
        }

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
}
