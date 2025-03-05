package sk.iway.iwcm.components.gdpr;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.persistence.jpa.JpaEntityManager;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

public class GdprDataDeleting {

    private SimpleQuery sq;
    private static GdprDataDeleting INSTANCE = null; //NOSONAR
    private int userId;
    private static String cronSignature = "";
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); //NOSONAR

    public static void main(String[] args)
    {
        Logger.debug(GdprDataDeleting.class, "Spúšťam GdprDataDeleting cron pre mazanie starych userov, mailov, objednavok, formularov");
        getInstance(-1).deleteSendedEmails();
        getInstance(-1).deleteOldFormData();
        getInstance(-1).deleteOldBasketOrders();
        getInstance(-1).deleteUnusedUsers();
    }

    public GdprDataDeleting(int userId) {
        sq = new SimpleQuery();
    }

    public static GdprDataDeleting getInstance(HttpServletRequest request)
    {
        if(request != null)
        {
            Identity user = UsersDB.getCurrentUser(request);
            if(user != null)
                getInstance(user.getUserId());
        }
        return getInstance(-1);
    }

    public static GdprDataDeleting getInstance(int userId)
    {
        if(INSTANCE == null)
            INSTANCE = new GdprDataDeleting(userId);
        INSTANCE.setUserId(userId);
        cronSignature = "";
        if(userId == -1)
            cronSignature = "Cron Job";
        return INSTANCE;
    }

    public static List<UserDetails> getUnusedUsers()
    {
        List<UserDetails> users = new ArrayList<>();
        String date = getFormatedDate(Constants.getInt("gdprDeleteUserAfterDays"));

        if(Constants.DB_TYPE == Constants.DB_MSSQL)
        {
            users.addAll(UsersDB.getUsersByWhereSql(" AND ( (last_logon<convert (datetime,'"+date+"')) OR ( last_logon IS NULL AND reg_date<convert (datetime,'"+date+"') ) ) "));
        }
        else if(Constants.DB_TYPE == Constants.DB_ORACLE || Constants.DB_TYPE == Constants.DB_PGSQL) {
            users.addAll(UsersDB.getUsersByWhereSql(" AND ( (last_logon < to_date('"+date+"','YYYY-MM-DD')) OR ( last_logon IS NULL AND reg_date<to_date('"+date+"','YYYY-MM-DD') ) ) "));
        }
        else // na MYSQL a ORACLE to zbieha ako tent isty SQl dotaz
        {
            users.addAll(UsersDB.getUsersByWhereSql(" AND ( (last_logon < date '"+date+"') OR ( last_logon IS NULL AND reg_date<'"+date+"' ) ) "));
        }

        return users;
    }

    public void deleteUnusedUsers()
    {
        Adminlog.add(Adminlog.TYPE_GDPR_USERS_DELETE,getUserId(),"GDPR "+cronSignature+" Hromadne mazem "+getUnusedUsers().size()+" pouzivatelov z databazy, s datumom posledneho prihlasenia starsieho ako "+Constants.getInt("gdprDeleteUserAfterDays")+" dni",-1,-1);
        for(UserDetails ud:getUnusedUsers())
        {
            UsersDB.deleteUser(ud.getUserId(),"GDPR ");
        }
    }

    public void deleteOldFormData()
    {
        Adminlog.add(Adminlog.TYPE_GDPR_FORMS_DELETE,getUserId(),"GDPR "+cronSignature+" Hromadne mazem "+getOldFormDataCount()+" zaznamov z formularov, starsich ako "+Constants.getInt("gdprDeleteFormDataAfterDays")+" dni",-1,-1);
        String date = getFormatedDate(Constants.getInt("gdprDeleteFormDataAfterDays"));
        if(Constants.DB_TYPE == Constants.DB_MSSQL)
        {
            sq.execute("DELETE FROM forms where create_date < convert (datetime,'"+date+"')");
        }
        else // na MYSQL a ORACLE to zbieha ako tent isty SQl dotaz
        {
            sq.execute("DELETE FROM forms where create_date < '"+date+"'");
        }
    }

    public int getOldFormDataCount()
    {
        String date = getFormatedDate(Constants.getInt("gdprDeleteFormDataAfterDays"));
        if(Constants.DB_TYPE == Constants.DB_MSSQL)
        {
            return sq.forInt("SELECT count(*) FROM forms where create_date < convert (datetime,'"+date+"')");
        }
        else // na MYSQL a ORACLE to zbieha ako tent isty SQl dotaz
        {
            return sq.forInt("SELECT count(*) FROM forms where create_date < '"+date+"'");
        }
        //return sq.forInt("SELECT count(*) FROM forms where create_date < ?", getCalendarBeforeDate(Constants.getInt("gdprDeleteFormDataAfterDays")).getTime());
    }

    public long getOldBasketOrdersCount()
    {
        long count;
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        em.getTransaction().begin();
        Query query = em.createQuery("SELECT COUNT(b) FROM BasketInvoiceBean b WHERE b.createDate < :createDate ",Long.class);
        query.setParameter("createDate",getCalendarBeforeDate(Constants.getInt("gdprDeleteUserBasketOrdersAfterYears")*365).getTime());
        count = (Long)query.getSingleResult();
        em.getTransaction().commit();
        return count;
    }

    public void deleteOldBasketOrders()
    {
        Adminlog.add(Adminlog.TYPE_GDPR_BASKET_INVOICES_DELETE,getUserId(),"GDPR "+cronSignature+" Hromadne mazem "+getOldBasketOrdersCount()+" zaznamov z objednavok / kosiku, starsich ako "+Constants.getInt("gdprDeleteUserBasketOrdersAfterYears")+" rokov",-1,-1);
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        em.getTransaction().begin();
        Query query = em.createQuery("DELETE FROM BasketInvoiceBean b WHERE b.createDate < :createDate");
        query.setParameter("createDate",getCalendarBeforeDate(Constants.getInt("gdprDeleteUserBasketOrdersAfterYears")*365).getTime());
        query.executeUpdate();
        em.getTransaction().commit();
    }

    public int getSendedEmailsCount()
    {
        String date = getFormatedDate(Constants.getInt("gdprDeleteEmailsAfterDays"));

        if(Constants.DB_TYPE == Constants.DB_MSSQL)
        {
            return sq.forInt("SELECT count(*) FROM emails WHERE sent_date < convert (datetime,'"+date+"')");
        }
        else if(Constants.DB_TYPE == Constants.DB_ORACLE || Constants.DB_TYPE == Constants.DB_PGSQL)
        {
            return sq.forInt("SELECT count(*) FROM emails WHERE sent_date < to_date('"+date+"','YYYY-MM-DD')");
        }
        else
        {
            return sq.forInt("SELECT count(*) FROM emails WHERE sent_date < '"+date+"'");
        }


    }

    public void deleteSendedEmails()
    {
        Adminlog.add(Adminlog.TYPE_GDPR_EMAILS_DELETE,getUserId(),"GDPR "+cronSignature+" Hromadne mazem "+getSendedEmailsCount()+" zaznamov z tabulky emails, starsich ako 0 dni",-1,-1);
        String date = getFormatedDate(Constants.getInt("gdprDeleteEmailsAfterDays"));

        if(Constants.DB_TYPE == Constants.DB_MSSQL)
        {
            sq.execute("DELETE FROM emails WHERE sent_date < convert (datetime,'"+date+"')");
        }
        else if(Constants.DB_TYPE == Constants.DB_ORACLE || Constants.DB_TYPE == Constants.DB_PGSQL)
        {
            sq.execute("DELETE FROM emails WHERE sent_date < to_date('"+date+"','YYYY-MM-DD')");
        }
        else // na MYSQL a ORACLE to zbieha ako tent isty SQl dotaz
        {
            sq.execute("DELETE FROM emails where sent_date < ?", date);
        }
    }

    private static String getFormatedDate(int daysBefore)
    {
        Calendar cal = getCalendarBeforeDate(daysBefore);
        return sdf.format(cal.getTime());// cal.get(Calendar.YEAR)+"-"+(cal.get(Calendar.MONTH)+1)+"-"+cal.get(Calendar.DAY_OF_MONTH);
    }

    private static Calendar getCalendarBeforeDate(int daysBefore)
    {
        Calendar cal =  Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, - daysBefore);
        return cal;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
