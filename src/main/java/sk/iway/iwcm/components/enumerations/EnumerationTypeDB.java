package sk.iway.iwcm.components.enumerations;

import java.util.List;

import javax.persistence.Query;

import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeBean;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.utils.Pair;

/**
 * EnumerationTypeDB.java
 *
 * Class EnumerationTypeDB is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2018
 * @author       $Author: mhruby $
 * @version      $Revision: 1.0 $
 * created      9.4.2018 12:00
 * modified     6.4.2018 12:06
 */

public class EnumerationTypeDB extends JpaDB<EnumerationTypeBean> {

    private static EnumerationTypeDB INSTANCE = new EnumerationTypeDB();

    public static EnumerationTypeDB getInstance() {
        return INSTANCE;
    }

    public EnumerationTypeDB() {
        super(EnumerationTypeBean.class);
    }


    public static EnumerationTypeBean getEnumerationById(int id) {
        return new JpaDB<>(EnumerationTypeBean.class).getById(id);
    }

    public static boolean saveEnumerationType(EnumerationTypeBean enumerationTypeBean) {
        Cache.getInstance().removeObjectStartsWithName("enumeration.");
        return new JpaDB<>(EnumerationTypeBean.class).save(enumerationTypeBean);
    }

    /**
     * Skryje zadany typ ciselnika.
     * @param id
     * @return
     */
    public static boolean softDelete(int id) {
        Cache.getInstance().removeObjectStartsWithName("enumeration.");
        EnumerationTypeBean enumerationTypeBean = getEnumerationById(id);
        if (enumerationTypeBean != null) {
            enumerationTypeBean.setHidden(true);
            return saveEnumerationType(enumerationTypeBean);
        }
        return false;
    }

    public static boolean deleteEnumeration(int id) {
        return JpaTools.delete(EnumerationTypeBean.class, id);
    }

    public static List<EnumerationTypeBean> getAllEnumerationType() {
        return JpaTools.getAll(EnumerationTypeBean.class);
    }

    public static EnumerationTypeBean getEnumerationByName(String name) {
        @SuppressWarnings("unchecked")
        EnumerationTypeBean ret = new JpaDB<>(EnumerationTypeBean.class).findFirstByProperties(Pair.of("typeName", name));
        return ret;
    }

    /**
     * Return string of all types in format {a, b, c}.
     * @return
     */
    public static String getStringTypes() {
        String string = "";
        string += "{ ";
        for (EnumerationTypeBean item : EnumerationTypeDB.getAllEnumerationType()) {
            string += "\"" + item.getTypeName() + "\": " + item.getEnumerationTypeId() + ",";
        }
        string += " }";
        return string;
    }

    public static void deleteAll() {
        for(EnumerationTypeBean item : getAllEnumerationType()) {
            deleteEnumeration(item.getEnumerationTypeId());
        }
    }

    public static List<EnumerationTypeBean> getAllSorted(boolean ascending) {
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        try {
            ExpressionBuilder builder = new ExpressionBuilder();
            ReadAllQuery dbQuery = new ReadAllQuery(EnumerationTypeBean.class, builder);
            if (ascending)
                dbQuery.addAscendingOrdering("typeName");
            else
                dbQuery.addDescendingOrdering("typeName");
            Query query = em.createQuery(dbQuery);
            List<EnumerationTypeBean> enumerationDataBeanList = JpaDB.getResultList(query);
            return enumerationDataBeanList;
        }catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        } finally {
            em.close();
        }
        return null;
    }

    public static int getEnumerationIdFromString(String enumerationIdString) {
        return Tools.getIntValue(enumerationIdString.substring(enumerationIdString.indexOf("_") + 1), -1);
    }
}
