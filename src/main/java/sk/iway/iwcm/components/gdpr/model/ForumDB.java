package sk.iway.iwcm.components.gdpr.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;

import org.eclipse.persistence.jpa.JpaEntityManager;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;

public class ForumDB extends JpaDB<ForumBean> implements GdprDB
{
    private final Class<ForumBean> clazz;
    private final List<String> properties;
    private static ForumDB instance = new ForumDB();

    public ForumDB()
    {
        super(ForumBean.class);
        clazz = ForumBean.class;

        properties = new ArrayList<>();
        properties.add("subject");
        properties.add("question");
    }

    public static ForumDB getInstance()
    {
        return instance;
    }

    @SuppressWarnings("unused")
    public List<GdprResult> search(List<GdprRegExpBean> regexps, HttpServletRequest request) {
        JpaEntityManager jpaEntityManager = JpaTools.getEclipseLinkEntityManager(dbName);

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT o FROM " + clazz.getSimpleName() + " o WHERE ");
        sb.append(" o.domainId = :domainId AND (");

        List<String> or = new ArrayList<>();
        for (String property : properties) {
            or.add("o." + property + " REGEXP :regexp{index}");
        }


        int i = 0;
        List<String> or2 = new ArrayList<>();
        for (GdprRegExpBean regexp : regexps) {
            String str = Tools.join(or, " OR ");
            or2.add(Tools.replace(str, "{index}", "" + (++i)));
        }
        sb.append(Tools.join(or2, " OR ")+")");


        Query query = jpaEntityManager.createQuery(sb.toString());

        query.setParameter("domainId",CloudToolsForCore.getDomainId());

        i = 0;
        for (GdprRegExpBean regexp : regexps) {
            query.setParameter("regexp" + (++i), regexp.getRegexpValue());
        }

        List<ForumBean> rows = new ArrayList<>();
        try
        {
            rows = JpaDB.getResultList(query);
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }

        List<GdprResult> results = new ArrayList<>();

        for (ForumBean row : rows) {
            results.add(new GdprResult(row, regexps, request));
        }

        return results;
    }
}