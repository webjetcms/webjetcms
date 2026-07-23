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
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
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
                if (type.getId() == id) return type;
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
            getInstance(-1).deleteOldDocAndGroups();
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
                        getInstance(-1).deleteOldDocAndGroups();
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

    public void deleteOldDocAndGroups() {
        // Remove all OLD groups from trash
        removeOldTrashGroups();

        // OLD groups from trash are gone - now remove DOC that are old enought in trash
        removeOldTrashDocs();

        // Remove empty folders
        removeEmptyTrashGroups();
    }

    private void removeEmptyTrashGroups() {
        List<GroupDetails> trashGroups = GroupsDB.getInstance().getTrashGroupsAllDomains();
        for (GroupDetails trashGroup : trashGroups) {
            // Get all subfolders recursively (excluding the trash root itself)
            List<GroupDetails> allGroupsInTrash = GroupsDB.getInstance().getGroupsTree(trashGroup.getGroupId(), false, true);
            // Process bottom-up (reverse) so leaves are deleted before their parents
            for (int i = allGroupsInTrash.size() - 1; i >= 0; i--) {
                GroupDetails group = allGroupsInTrash.get(i);
                List<DocDetails> docs = DocDB.getInstance().getDocByGroup(group.getGroupId(), DocDB.ORDER_ID, true, -1, -1, true, false);
                if (docs.isEmpty()) {
                    // Re-check children after earlier deletions in this loop
                    List<GroupDetails> remainingChildren = GroupsDB.getInstance().getGroupsTree(group.getGroupId(), false, true);
                    if (remainingChildren.isEmpty()) {
                        // Remove group + sub-groups
                        // the param withHistory TRUE allso secures removeing of binded groups_scheduler
                        GroupsDB.deleteGroup(group.getGroupId(), false, true, true, true);
                    }
                }
            }
        }
    }

    private void removeOldTrashDocs() {
        // OLD groups from trash are gone - now remove DOC that are old enough in trash
        Date before = getCalendarBeforeDate(GdprDataDeletingType.OLD_DOC_AND_GROUPS.getAfterConstantInt()).getTime();

        List<GroupDetails> trashGroups = GroupsDB.getInstance().getTrashGroupsAllDomains();
        for (GroupDetails trashGroup : trashGroups) {
            // Get the trash group itself + all subfolders recursively
            List<GroupDetails> allGroupsInTrash = GroupsDB.getInstance().getGroupsTree(trashGroup.getGroupId(), true, true);
            for (GroupDetails group : allGroupsInTrash) {
                // Get all docs in this group (including unavailable), without data column
                List<DocDetails> docs = DocDB.getInstance().getDocByGroup(group.getGroupId(), DocDB.ORDER_ID, true, -1, -1, true, false);
                for (DocDetails doc : docs) {
                    if (doc.getDateCreated() > 0 && doc.getDateCreated() < before.getTime()) {
                        // Remove doc permanently
                        // the param withHistory TRUE allso secures removeing of binded documents_history
                        DocDB.deleteDoc(doc.getDocId(), null, true, true);
                    }
                }
            }
        }
    }

    private void removeOldTrashGroups() {
        List<GroupDetails> topLevelGroupsInTrash = GroupsDB.getInstance().getTopLevelGroupsInTrash();
        List<Integer> oldEnoght = getOldGroupIds();

        List<Integer> oldGroupsToRemove = new ArrayList<>();
        for (GroupDetails groupInTrash : topLevelGroupsInTrash) {
            if(oldEnoght.contains(groupInTrash.getGroupId())) {
                oldGroupsToRemove.add(groupInTrash.getGroupId());
            }
        }

        // Remove groups that are old enought (it will allso remove all the subgroups and DOC's that are in this groups) - later
        for(int groupId : oldGroupsToRemove) {
            // Remove group + sub-groups and all docs inside
            // the param withHistory TRUE allso secures removeing of binded documents_history and groups_scheduler
            GroupsDB.deleteGroup(groupId, false, true, true, true);
        }
    }

    private List<Integer> getOldGroupIds() {
        Date before = getCalendarBeforeDate(GdprDataDeletingType.OLD_DOC_AND_GROUPS.getAfterConstantInt()).getTime();

        List<Integer> groupsOlderThanDate = new ArrayList<>();

        new ComplexQuery()
            .setSql("SELECT group_id, MIN(schedule_id) as top_id FROM groups_scheduler WHERE awaiting_approve IS NULL AND disapproved_by IS NULL AND save_date <= ? GROUP BY group_id")
            .setParams(before)
            .list(new Mapper<Object>() {
                @Override
                public Object map(java.sql.ResultSet rs) throws java.sql.SQLException {
                    groupsOlderThanDate.add(rs.getInt("group_id"));
                    return null;
                }
            });

        return groupsOlderThanDate;
    }
}
