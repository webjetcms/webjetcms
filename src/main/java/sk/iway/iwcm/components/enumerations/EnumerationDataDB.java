package sk.iway.iwcm.components.enumerations;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeBean;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.utils.Pair;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.*;

/**
 * EnumerationDataDB.java
 *
 * Class EnumerationDataDB is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2018
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      9.4.2018 11:59
 * modified     5.4.2018 16:37
 */

public class EnumerationDataDB extends JpaDB<EnumerationDataBean> {

    private static EnumerationDataDB INSTANCE = new EnumerationDataDB();

    public static EnumerationDataDB getInstance() {
        return INSTANCE;
    }

    public EnumerationDataDB() {
        super(EnumerationDataBean.class);
    }

    public static EnumerationDataBean getEnumerationDataById(int id) {
        EnumerationDataBean enumerationDataBean = getFromCache(id);
        if (enumerationDataBean == null) {
            EnumerationDataBean temp = getEnumerationDataFromDB(id);
            if (temp != null) {
                saveToCache(temp);
                return temp;
            }
            return null;
        }
        return enumerationDataBean;
    }

    public static EnumerationDataBean getEnumerationDataFromDB(int id) {
        return new JpaDB<>(EnumerationDataBean.class).getById(id);
    }
    public static List<EnumerationDataBean> getEnumerationDataByType(String type) {
        return JpaTools.findByProperties(EnumerationDataBean.class, new Pair<>("type.typeName", type), new Pair<>("hidden", false));
    }

    public static boolean saveEnumerationData(EnumerationDataBean enumerationDataBean) {
        removeFromCache(enumerationDataBean.getEnumerationDataId());
        Cache.getInstance().removeObjectStartsWithName("enumeration.");
        if (enumerationDataBean.getSortPriority() == 0)
            enumerationDataBean.setSortPriority(EnumerationDataDB.getMaxPriority(enumerationDataBean.getType()) + 10);
        return new JpaDB<>(EnumerationDataBean.class).save(enumerationDataBean);
    }

    /**
     * Skryje zadany typ ciselnika.
     * @param id
     * @return
     */
    public static boolean softDelete(int id) {
        Cache.getInstance().removeObjectStartsWithName("enumeration.");
        EnumerationDataBean enumerationDataBean = getEnumerationDataById(id);
        if (enumerationDataBean != null) {
            enumerationDataBean.setHidden(true);
            return saveEnumerationData(enumerationDataBean);
        }
        return false;
    }

    public static boolean deleteEnumerationData(int id) {
        return JpaTools.delete(EnumerationDataBean.class, id);
    }

    public static List<EnumerationDataBean> getAllEnumerationData() {
        return JpaTools.getAll(EnumerationDataBean.class);
    }

    public static List<EnumerationDataBean> getAllEnumerationDataByType(int typeId) {
        return JpaTools.findByProperties(EnumerationDataBean.class, new Pair<>("type.id", typeId));
    }

    public static List<EnumerationDataBean> getEnumerationDataByType(int typeId) {
        return getEnumerationDataByType(typeId, false);
    }

    public static List<EnumerationDataBean> getEnumerationDataByType(int typeId, boolean hidden) {
        return JpaTools.findByProperties(EnumerationDataBean.class, new Pair<>("type.id", typeId), new Pair<>("hidden", hidden));
    }

    public static List<EnumerationDataBean> getEnumerationDataBy(String string1, int typeId ) {
        return JpaTools.findByProperties(EnumerationDataBean.class, new Pair<>("type.id", typeId), new Pair<>("hidden", false), new Pair<>("string1",string1));
    }

    public static List<EnumerationDataBean> getEnumerationDataBy(BigDecimal value, String decimalColumn, int typeId) {
        if ("decimal1".equalsIgnoreCase(decimalColumn) || "decimal2".equalsIgnoreCase(decimalColumn) || "decimal3".equalsIgnoreCase(decimalColumn) || "decimal4".equalsIgnoreCase(decimalColumn))
            return JpaTools.findByProperties(EnumerationDataBean.class, new Pair<>("type.id", typeId), new Pair<>("hidden", false), new Pair<>(decimalColumn.toLowerCase(), value));
        throw new InvalidParameterException("Unsuported decimalColumn value: " + decimalColumn);
    }

    public static List<EnumerationDataBean> getEnumerationDataBy(String string1, BigDecimal decimal1, int typeId ) {
        return JpaTools.findByProperties(EnumerationDataBean.class, new Pair<>("type.id", typeId), new Pair<>("hidden", false), new Pair<>("string1",string1), new Pair<>("decimal1", decimal1));
    }

    public static List<EnumerationDataBean> getEnumerationDataBy(BigDecimal decimal1, int typeId ) {
        return JpaTools.findByProperties(EnumerationDataBean.class, new Pair<>("type.id", typeId), new Pair<>("hidden", false), new Pair<>("decimal1", decimal1));
    }

    public static List<EnumerationDataBean> getEnumerationDataBy(String string1, BigDecimal decimal1, BigDecimal decimal2, int typeId ) {
        return JpaTools.findByProperties(EnumerationDataBean.class, new Pair<>("type.id", typeId), new Pair<>("hidden", false), new Pair<>("decimal1", decimal1), new Pair<>("decimal2", decimal2), new Pair<>("string1",string1));
    }

    public static List<EnumerationDataBean> getSortedEnumerationDataFromString(String enumerationIdString) {
        if (Tools.isNotEmpty(enumerationIdString)) {
            int enumerationId = EnumerationTypeDB.getEnumerationIdFromString(enumerationIdString);
            if (enumerationId > 0) {
                return getSortedEnumerationDataByType(EnumerationTypeDB.getEnumerationById(enumerationId));
            }
        }
        return Collections.emptyList();
    }

    public static List<EnumerationDataBean> getSortedEnumerationDataByType(EnumerationTypeBean typeBean) {
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        try {
            ExpressionBuilder builder = new ExpressionBuilder();
            ReadAllQuery dbQuery = new ReadAllQuery(EnumerationDataBean.class, builder);

            Expression expr = builder.get("type").equal(typeBean).and(builder.get("hidden").equal(false));
            dbQuery.setSelectionCriteria(expr);
            dbQuery.addAscendingOrdering("string1");
            Query query = em.createQuery(dbQuery);
            List<EnumerationDataBean> enumerationDataBeanList = JpaDB.getResultList(query);
            return enumerationDataBeanList;
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        } finally {
            em.close();
        }
        return null;
    }

    public static List<EnumerationDataBean> getEnumerationDataBy(String string1, String string9, String string10, int typeId ) {
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        try {
            ExpressionBuilder builder = new ExpressionBuilder();
            ReadAllQuery dbQuery = new ReadAllQuery(EnumerationDataBean.class, builder);


            Expression likeExpr = builder.get("string1").likeIgnoreCase("%" + string1 + "%").or(builder.get("string9").likeIgnoreCase("%" + string9 + "%")).or(builder.get("string10").likeIgnoreCase("%" + string10 + "%"));
            Expression expr = builder.get("typeId").equal(typeId).and(builder.get("hidden").equal(false)).and(likeExpr);

            dbQuery.setSelectionCriteria(expr);
            dbQuery.addAscendingOrdering("string1");
            Query query = em.createQuery(dbQuery);
            List<EnumerationDataBean> enumerationDataBeanList = JpaDB.getResultList(query);
            return enumerationDataBeanList;
        }catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        } finally {
            em.close();
        }
        return null;
    }

    public static void deleteAll(int enumerationTypeId) {
        for(EnumerationDataBean item : EnumerationDataDB.getEnumerationDataByType(enumerationTypeId)) {
            deleteEnumerationData(item.getEnumerationDataId());
        }
    }

    public static int getMaxPriority(EnumerationTypeBean type) {
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        try{
            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();

            //SELECT MAX(e.sortPriority) FROM enumeration_data e WHERE e.enumeration_type_id = 32;
            CriteriaQuery<Integer> criteria = criteriaBuilder.createQuery(Integer.class);
            Root<EnumerationDataBean> root = criteria.from(EnumerationDataBean.class);
            criteria.select(criteriaBuilder.max(root.get("sortPriority")));
            criteria.where(criteriaBuilder.equal(root.get("type"), type));
            Object result = em.createQuery(criteria).getSingleResult();
            if (result instanceof Integer)
                return (Integer) result;
        }catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }finally{
            em.close();
        }
        return -1;
    }

    public static String getString1(int enumerationDataId) {
        EnumerationDataBean enumerationDataBean = getFromCache(enumerationDataId);
        if (enumerationDataBean == null) {
            EnumerationDataBean temp = getEnumerationDataById(enumerationDataId);
            if (temp != null) {
                saveToCache(temp);
                return temp.getString1();
            }
            return "";
        }
        return enumerationDataBean.getString1();
    }

    private static void saveToCache(EnumerationDataBean temp) {
        if (temp != null) {
            Map<Integer, EnumerationDataBean> map = getCache();
            if (map != null)
                map.put(temp.getEnumerationDataId(), temp);
        }
    }

    private static Map<Integer, EnumerationDataBean> getCache() {
        Cache c = Cache.getInstance();
        @SuppressWarnings("unchecked")
        Map<Integer, EnumerationDataBean> map = (HashMap<Integer, EnumerationDataBean>) c.getObject("enumerationsBeans");
        if (map == null) {
            c.setObjectSeconds("enumerationsBeans", new HashMap<Integer, String>(), 28800, true); // 28800 = 8 hodin
            return getCache();
        } else {
            return map;
        }
    }

    private static EnumerationDataBean getFromCache(int enumerationDataId) {
        Map<Integer, EnumerationDataBean> map = getCache();
        return map.get(enumerationDataId);
    }

    private static void removeFromCache(int enumerationDataId) {
        Map<Integer, EnumerationDataBean> map = getCache();
        map.remove(enumerationDataId);
    }

    public List<EnumerationDataBean> getActiveDirectChildrens(EnumerationDataBean enumerationDataBean) {
        return JpaTools.findByProperties(EnumerationDataBean.class, new Pair<>("hidden", false), new Pair<>("type.id", enumerationDataBean.getType().getId()), new Pair<>("parentEnumerationData.enumerationDataId", enumerationDataBean.getId()));
    }

    public List<EnumerationDataBean> getAllActiveChildrens(EnumerationDataBean enumerationDataBean) {
        List<EnumerationDataBean> list = getActiveDirectChildrens(enumerationDataBean);
        List<EnumerationDataBean> list2 = new ArrayList<>();
        for (EnumerationDataBean obj: list) {
            list2.addAll(getActiveDirectChildrens(obj));
        }
        list.addAll(list2);
        return list;
    }
}