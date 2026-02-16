package sk.iway.iwcm.doc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.persistence.jpa.JpaEntityManager;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.utils.Pair;

import javax.persistence.EntityManager;

/**
 * TemplatesGroupDB.java
 *
 * Class TemplatesGroupDB is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2018
 * author       $Author: mhruby $
 * version      $Revision: 1.0 $
 * created      12.6.2018 17:21
 * modified     12.6.2018 17:12
 */

public class TemplatesGroupDB extends JpaDB<TemplatesGroupBean> {

    private static TemplatesGroupDB instance = new TemplatesGroupDB();

    public static TemplatesGroupDB getInstance() {
        return instance;
    }

    public TemplatesGroupDB() {
        super(TemplatesGroupBean.class);
    }

    public static List<TemplatesGroupBean> getAllTemplatesGroups() {
        return JpaTools.getAll(TemplatesGroupBean.class);
    }

    public static List<TemplatesGroupBean> getAllTemplatesGroupsWithCount() {
        Map<Long,Integer> map = new HashMap<>();
        for (TemplateDetails templateDetails : TemplatesDB.getInstance().getTemplates()) {
            Integer count = map.get(templateDetails.getTemplatesGroupId());
            if (count == null)
                count = 1;
            else
                count++;
            map.put(templateDetails.getTemplatesGroupId(),count);
        }
        List<TemplatesGroupBean> templatesGroupList = getAllTemplatesGroups();
        for (TemplatesGroupBean templatesGroupBean: templatesGroupList) {
            templatesGroupBean.setCount(Tools.getIntValue(map.get(templatesGroupBean.getId()),0));
        }
        return templatesGroupList;
    }

    public static boolean  delete(Long id) {
        boolean result=false;
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(TemplatesGroupBean.class);
        try
        {
            em.getTransaction().begin();
            TemplatesGroupBean toBeDeleted = em.getReference(TemplatesGroupBean.class, id);
            if(!canDeleteDomain(toBeDeleted.getClass(), toBeDeleted))
            {
                return result;
            }
            em.remove(toBeDeleted);
            em.getTransaction().commit();
            result = true;
        }
        catch (Exception e)
        {
            sk.iway.iwcm.Logger.error(e);
        }
        finally
        {
            em.close();
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
    private static boolean canDeleteDomain(Class clazz, Object o) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        if((InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")) && Constants.getString("jpaFilterByDomainIdBeanList").indexOf(clazz.getName()) != -1)
        {
            @SuppressWarnings("unchecked")
            Method method = clazz.getMethod("getDomainId");
            int domainIdDelete = (Integer) method.invoke(o);
            if(CloudToolsForCore.getDomainId() != domainIdDelete)
            {
                Logger.debug(JpaTools.class, "Pokus o zmazanie zaznamu mimo domainId (" + domainIdDelete + "). Spravna domainId = " + CloudToolsForCore.getDomainId());
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static TemplatesGroupBean getTemplatesGroupByName(String name) {
        return new JpaDB<>(TemplatesGroupBean.class).findFirstByProperties(Pair.of("name", name));
    }

    public TemplatesGroupBean getById(Long id) {
        //musime vyrobit novy EntityManager, pretoze ten threadovy em.find zatvori a potom nemusime mat otvoreny em dalej v kode (napr. v helpdesku)
        EntityManager threadEm = JpaTools.getEclipseLinkEntityManager(dbName);
        EntityManager em = threadEm.getEntityManagerFactory().createEntityManager();

        TemplatesGroupBean templatesGroupBean;
        if((InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")) && Constants.getString("jpaFilterByDomainIdBeanList").contains(TemplatesGroupBean.class.getName()))
        {
            Map<String,Object>  hashMap = new HashMap<>();
            hashMap.put("domainId", CloudToolsForCore.getDomainId());
            templatesGroupBean = em.find(TemplatesGroupBean.class, id,hashMap);
        }
        else
        {
            templatesGroupBean = em.find(TemplatesGroupBean.class, id);
        }

        if (em.isOpen()) em.close();

        return templatesGroupBean;
    }

    /**
     * Vrati ID template grupy pre zadane docId
     * @param docId
     * @return
     */
    public static long getFromDocId(int docId) {
        DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
        if (doc != null) {
            TemplateDetails temp = TemplatesDB.getInstance().getTemplate(doc.getTempId());
            if (temp != null && temp.getTemplatesGroupId()!=null) {
                return temp.getTemplatesGroupId().longValue();
            }
        }
        return -1;
    }
}