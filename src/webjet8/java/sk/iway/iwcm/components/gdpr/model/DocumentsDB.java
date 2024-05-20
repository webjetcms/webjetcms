package sk.iway.iwcm.components.gdpr.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.persistence.jpa.JpaEntityManager;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.jpa.JpaTools;

public class DocumentsDB extends JpaDB<DocumentsBean> implements GdprDB
{
    private final Class<DocumentsBean> clazz;
    private final List<String> properties;
    private static DocumentsDB instance = new DocumentsDB();

    public DocumentsDB()
    {
        super(DocumentsBean.class);
        clazz = DocumentsBean.class;

        properties = new ArrayList<>();
        properties.add("title");
        properties.add("data");
        properties.add("dataAsc");
    }

    public static DocumentsDB getInstance()
    {
        return instance;
    }

    @SuppressWarnings("unused")
    public List<GdprResult> search(List<GdprRegExpBean> regexps, HttpServletRequest request) {
        JpaEntityManager jpaEntityManager = JpaTools.getEclipseLinkEntityManager(dbName);

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT o FROM " + clazz.getSimpleName() + " o WHERE ");


        List<String> or = new ArrayList<>();
        for (String property : properties) {

            if (Constants.DB_TYPE == Constants.DB_MSSQL || Constants.DB_TYPE == Constants.DB_PGSQL)
                or.add("o." + property + " LIKE :regexp{index}");
//            else if (Constants.DB_TYPE == Constants.DB_ORACLE)
//                or.add("o." + property + " LIKE :regexp{index}");
            else
                or.add("o." + property + " REGEXP :regexp{index}");
        }


        int i = 0;
        List<String> or2 = new ArrayList<>();
        for (GdprRegExpBean regexp : regexps) {
            String str = Tools.join(or, " OR ");
            or2.add(Tools.replace(str, "{index}", "" + (++i)));
        }
        sb.append(Tools.join(or2, " OR "));

        Query query = jpaEntityManager.createQuery(sb.toString());



        i = 0;
        for (GdprRegExpBean regexp : regexps) {
            query.setParameter("regexp" + (++i), regexp.getRegexpValue());
        }

        List<DocumentsBean> rows = new ArrayList<>();
        try
        {
            rows = JpaDB.getResultList(query);
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }

        List<GdprResult> results = new ArrayList<>();

        String domain = CloudToolsForCore.getDomainName();
        for (DocumentsBean row : rows) {
            GroupDetails gd = GroupsDB.getInstance().getGroup(row.getGroupId());
            if(gd == null || Tools.isEmpty(gd.getDomainName()) || gd.getDomainName().equalsIgnoreCase(domain))//if(gd != null && gd.getDomainName().equalsIgnoreCase(domain))
                results.add(new GdprResult(row, regexps, request));
        }

        return results;
    }
}