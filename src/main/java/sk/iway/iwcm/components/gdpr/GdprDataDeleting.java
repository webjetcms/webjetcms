package sk.iway.iwcm.components.gdpr;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.persistence.jpa.JpaEntityManager;

import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.OldDocGroupsRemovingService;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

public class GdprDataDeleting {

    private SimpleQuery sq;
    private static GdprDataDeleting INSTANCE = null; //NOSONAR
    private int userId;
    private static String cronSignature = "";
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); //NOSONAR

    public enum GdprDataDeletingType {
        SENDED_EMAILS(1L, "sendedEmails", "gdprDeleteEmailsAfterDays"),
        OLD_FORM_DATA(2L, "oldFormData", "gdprDeleteFormDataAfterDays"),
        OLD_BASKET_ORDERS(3L, "oldBasketOrders", "gdprDeleteUserBasketOrdersAfterYears"),
        UNUSED_USERS(4L, "unusedUsers", "gdprDeleteUserAfterDays"),
        OLD_DOC_AND_GROUPS(5L, "oldDocAndGroups", "gdprDeleteDocAndGroupsAfterDays");

        private Long id;
        private String value;
        private String afterConstant;

        GdprDataDeletingType(Long id, String value, String afterConstant) {
            this.id = id;
            this.value = value;
            this.afterConstant = afterConstant;
        }

        public Long getId() { return id; }
        public String getValue() { return value; }
        public String getAfterConstant() { return afterConstant; }
        public int getAfterConstantInt() { return Constants.getInt(afterConstant); }

        public static GdprDataDeletingType getById(Long id) {
            if(id == null) return null;

            for (GdprDataDeletingType type : values()) {
                if (type.getId().equals(id)) return type;
            }
            return null;
        }

        public static GdprDataDeletingType getByValue(String value) {
            if(Tools.isEmpty(value)) return null;

            for (GdprDataDeletingType type : values()) {
                if (type.getValue().equals(value)) return type;
            }
            return null;
        }
    }

    public static void main(String[] args)
    {
        if(args == null || args.length == 0 || Tools.isEmpty(args[0])) {
            Logger.debug(GdprDataDeleting.class, "Spúšťam GdprDataDeleting cron pre mazanie starych userov, mailov, objednavok, formularov");
            getInstance(-1).deleteSendedEmails();
            getInstance(-1).deleteOldFormData();
            getInstance(-1).deleteOldBasketOrders();
            getInstance(-1).deleteUnusedUsers();
            OldDocGroupsRemovingService.deleteOldDocAndGroups();
        } else {
            String deletingTypesStr = args[0];
            String[] deletingTypes = Tools.getTokens(deletingTypesStr, ",");

            for (String deletingType : deletingTypes) {
                deletingType = deletingType.trim();
                GdprDataDeletingType type = GdprDataDeletingType.getByValue(deletingType);

                if(type == null) {
                    Logger.error(GdprDataDeleting.class, "Uknown GdprDataDeletingType value: " + String.valueOf(deletingType));
                    return;
                }

                switch (type) {
                    case SENDED_EMAILS:
                        Logger.debug(GdprDataDeleting.class, "Spúšťam GdprDataDeleting cron pre mazanie SENDED_EMAILS");
                        getInstance(-1).deleteSendedEmails();
                        break;
                    case OLD_FORM_DATA:
                        Logger.debug(GdprDataDeleting.class, "Spúšťam GdprDataDeleting cron pre mazanie OLD_FORM_DATA");
                        getInstance(-1).deleteOldFormData();
                        break;
                    case OLD_BASKET_ORDERS:
                        Logger.debug(GdprDataDeleting.class, "Spúšťam GdprDataDeleting cron pre mazanie OLD_BASKET_ORDERS");
                        getInstance(-1).deleteOldBasketOrders();
                        break;
                    case UNUSED_USERS:
                        Logger.debug(GdprDataDeleting.class, "Spúšťam GdprDataDeleting cron pre mazanie UNUSED_USERS");
                        getInstance(-1).deleteUnusedUsers();
                        break;
                    case OLD_DOC_AND_GROUPS:
                        Logger.debug(GdprDataDeleting.class, "Spúšťam GdprDataDeleting cron pre mazanie OLD_DOC_AND_GROUPS");
                        OldDocGroupsRemovingService.deleteOldDocAndGroups();
                        break;
                }
            }
        }
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
        String date = getFormatedDate(GdprDataDeletingType.UNUSED_USERS.getAfterConstantInt());

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
        Adminlog.add(Adminlog.TYPE_GDPR_USERS_DELETE,getUserId(),"GDPR "+cronSignature+" Hromadne mazem "+getUnusedUsers().size()+" pouzivatelov z databazy, s datumom posledneho prihlasenia starsieho ako "+GdprDataDeletingType.UNUSED_USERS.getAfterConstantInt()+" dni",-1,-1);
        for(UserDetails ud:getUnusedUsers())
        {
            UsersDB.deleteUser(ud.getUserId(),"GDPR ");
        }
    }

    public void deleteOldFormData()
    {
        Adminlog.add(Adminlog.TYPE_GDPR_FORMS_DELETE,getUserId(),"GDPR "+cronSignature+" Hromadne mazem "+getOldFormDataCount()+" zaznamov z formularov, starsich ako "+GdprDataDeletingType.OLD_FORM_DATA.getAfterConstantInt()+" dni",-1,-1);
        Date before = getCalendarBeforeDate(GdprDataDeletingType.OLD_FORM_DATA.getAfterConstantInt()).getTime();
        sq.execute("DELETE FROM forms where create_date < ?", before);
    }

    public int getOldFormDataCount()
    {
        Date before = getCalendarBeforeDate(GdprDataDeletingType.OLD_FORM_DATA.getAfterConstantInt()).getTime();
        return sq.forInt("SELECT count(*) FROM forms where create_date < ?", before);
    }

    public long getOldBasketOrdersCount()
    {
        long count;
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        em.getTransaction().begin();
        Query query = em.createQuery("SELECT COUNT(b) FROM BasketInvoiceEntity b WHERE b.createDate < :createDate ",Long.class);
        query.setParameter("createDate",getCalendarBeforeDate(GdprDataDeletingType.OLD_BASKET_ORDERS.getAfterConstantInt()*365).getTime());
        count = (Long)query.getSingleResult();
        em.getTransaction().commit();
        return count;
    }

    public void deleteOldBasketOrders()
    {
        Adminlog.add(Adminlog.TYPE_GDPR_BASKET_INVOICES_DELETE,getUserId(),"GDPR "+cronSignature+" Hromadne mazem "+getOldBasketOrdersCount()+" zaznamov z objednavok / kosiku, starsich ako "+GdprDataDeletingType.OLD_BASKET_ORDERS.getAfterConstantInt()+" rokov",-1,-1);
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        em.getTransaction().begin();
        Query query = em.createQuery("DELETE FROM BasketInvoiceEntity b WHERE b.createDate < :createDate");
        query.setParameter("createDate",getCalendarBeforeDate(GdprDataDeletingType.OLD_BASKET_ORDERS.getAfterConstantInt()*365).getTime());
        query.executeUpdate();
        em.getTransaction().commit();
    }

    public int getSendedEmailsCount()
    {
        Date before = getCalendarBeforeDate(GdprDataDeletingType.SENDED_EMAILS.getAfterConstantInt()).getTime();

        return sq.forInt("SELECT count(*) FROM emails WHERE sent_date < ?", before);
    }

    public void deleteSendedEmails()
    {
        Adminlog.add(Adminlog.TYPE_GDPR_EMAILS_DELETE,getUserId(),"GDPR "+cronSignature+" Hromadne mazem "+getSendedEmailsCount()+" zaznamov z tabulky emails, starsich ako "+GdprDataDeletingType.SENDED_EMAILS.getAfterConstantInt()+" dni",-1,-1);
        Date before = getCalendarBeforeDate(GdprDataDeletingType.SENDED_EMAILS.getAfterConstantInt()).getTime();

        sq.execute("DELETE FROM emails where sent_date < ?", before);
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
