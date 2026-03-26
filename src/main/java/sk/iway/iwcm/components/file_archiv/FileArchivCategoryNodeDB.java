package sk.iway.iwcm.components.file_archiv;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.database.nestedsets.JpaNestedSetManager;
import sk.iway.iwcm.database.nestedsets.JpaNode;
import sk.iway.iwcm.database.nestedsets.NestedSetManager;
import sk.iway.iwcm.database.nestedsets.Node;
import sk.iway.iwcm.system.jpa.JpaTools;

/**
 *  FileArchivCategoryNodeDB.java
 *
 *	DAO class for manipulating with FileArchivCategoryNodeBean
 *
 *
 * Title        webjet7
 * Company      Interway s.r.o. (www.interway.sk)
 * Copyright    Interway s.r.o. (c) 2001-2010
 * @author       $Author: prau $
 * @version      $Revision: 1.3 $
 * created      Date: 03.01.2019 08:59:21
 * modified     $Date: 2004/08/16 06:26:11 $
 */
public class FileArchivCategoryNodeDB extends JpaDB<FileArchivCategoryNodeBean>
{
    private static int localId = -1;

	public FileArchivCategoryNodeDB()
	{
		super(FileArchivCategoryNodeBean.class);
	}


//	public List<FileArchivCategoryNodeBean> findByLft(int lft)
//	{
//		return JpaTools.findByMatchingProperty(FileArchivCategoryNodeBean.class, "lft", lft);
//	}

    public static FileArchivCategoryNodeBean findById(int id)
    {
        return JpaTools.findFirstByMatchingProperty(FileArchivCategoryNodeBean.class, "id", id);
    }

    public static Node<FileArchivCategoryNodeBean> getId(int id)
    {
        Node<FileArchivCategoryNodeBean> newNode = null;
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        NestedSetManager nsm = new JpaNestedSetManager(em);

        ReadAllQuery dbQuery = new ReadAllQuery(FileArchivCategoryNodeBean.class);
        ExpressionBuilder builder = new ExpressionBuilder();

        Expression where = builder.get("categoryType").equal(FileArchivNodeTypeEnum.CATEGORY);
        where = where.and(builder.get("id").equal(id));
        dbQuery.setSelectionCriteria(where);

        @SuppressWarnings("unchecked")
        List<FileArchivCategoryNodeBean> result = em.createQuery(dbQuery).getResultList();

        em.clear();
        nsm.clear();

        if(result != null && result.isEmpty()==false)
        {
            newNode = nsm.getNode(result.get(0));
        }
        return newNode;
    }

    /**
     * ziskania ID nodu aj moznostou podmienok pre level a ID kategorie 1. urovne
     * @param id - ID nodu
     * @param int1Val - vacsina ID kategorie 1. urovne
     * @param lvl - level
     * @return
     */
    public static Node<FileArchivCategoryNodeBean> getId(int id, int int1Val, int lvl)
    {
        Node<FileArchivCategoryNodeBean> newNode = null;
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        NestedSetManager nsm = new JpaNestedSetManager(em);

        ReadAllQuery dbQuery = new ReadAllQuery(FileArchivCategoryNodeBean.class);
        ExpressionBuilder builder = new ExpressionBuilder();

        Expression where = builder.get("categoryType").equal(FileArchivNodeTypeEnum.CATEGORY);
        where = where.and(builder.get("id").equal(id));
        where = where.and(builder.get("int1Val").equal(int1Val));
        where = where.and(builder.get("level").equal(lvl));
        dbQuery.setSelectionCriteria(where);

        @SuppressWarnings("unchecked")
        List<FileArchivCategoryNodeBean> result = em.createQuery(dbQuery).getResultList();

        em.clear();
        nsm.clear();

        if(result != null && result.isEmpty()==false)
        {
            newNode = nsm.getNode(result.get(0));
        }
        return newNode;
    }

    public static String getCategoryNameFromCategory(String categoryName)
    {
        localId = Tools.getIntValue(categoryName,-1);
        if(localId < 0)
        {
            return "";
        }
        Node<FileArchivCategoryNodeBean> node = getId(localId);
        if(node == null)
        {
            Logger.debug(FileArchivCategoryNodeDB.class,"getCategoryNameFromCategory("+categoryName+") nenasiel som kategoriu (localId)");
            return "";
        }
        return node.unwrap().getCategoryName();
    }

    public static JpaNestedSetManager getNsm()
    {
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        JpaNestedSetManager nsm = new JpaNestedSetManager(em);
        return nsm;
    }

    public static Node<FileArchivCategoryNodeBean> getRoot()
    {
        Node<FileArchivCategoryNodeBean> newNode;
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        NestedSetManager nsm = new JpaNestedSetManager(em);

        ReadAllQuery dbQuery = new ReadAllQuery(FileArchivCategoryNodeBean.class);
        ExpressionBuilder builder = new ExpressionBuilder();

        Expression where = builder.get("categoryType").equal(FileArchivNodeTypeEnum.ROOT);
        dbQuery.setSelectionCriteria(where);

        @SuppressWarnings("unchecked")
        List<FileArchivCategoryNodeBean> result = em.createQuery(dbQuery).getResultList();

        newNode =  nsm.getNode(result.get(0));
        em.clear();
        nsm.clear();
        return newNode;
    }

    public static void createRoot()
    {
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        JpaNestedSetManager nsm = new JpaNestedSetManager(em);
        em.getTransaction().begin();
            FileArchivCategoryNodeBean faCategory = new FileArchivCategoryNodeBean();
            faCategory.setCategoryName("ROOT");
            faCategory.createRoot();
            faCategory.setCategoryType(FileArchivNodeTypeEnum.ROOT);

            //vytvorime root node
            nsm.createRoot(faCategory);
        em.getTransaction().commit();
        em.clear();
        nsm.clear();

    }


    public static int getIdByCategory(String pCategory_1, String pCategory_2, String pCategory_3)
    {
        return getIdByCategory(pCategory_1,pCategory_2,pCategory_3,false);
    }

    public static int getIdByCategory(String pCategory_1, String pCategory_2, String pCategory_3, boolean doNotReturnCat2)
    {
        Node<FileArchivCategoryNodeBean> result = getByCategoryObj(pCategory_1, pCategory_2, pCategory_3, doNotReturnCat2);
        if(result != null)
        {
            return result.getId();
        }
        return -2;
    }
    public static Node<FileArchivCategoryNodeBean> getByCategoryObj(String pCategory_1, String pCategory_2, String pCategory_3)
    {
        return getByCategoryObj(pCategory_1, pCategory_2, pCategory_3,false);
    }

    public static Node<FileArchivCategoryNodeBean> getByCategoryObj(String pCategory_1, String pCategory_2, String pCategory_3, boolean doNotReturnCat2)
    {
        String objname = "MfsrCategoryDB-getByCategoryObj-"+pCategory_1+"-"+pCategory_2+"-"+pCategory_3+"-"+doNotReturnCat2;
        Cache c = Cache.getInstance();
        if(c.getObject(objname) != null)
        {
            @SuppressWarnings("unchecked")
            Node<FileArchivCategoryNodeBean> node = (Node<FileArchivCategoryNodeBean>) c.getObject(objname);
            return node;
        }

        String paramCategory_1_id = Tools.isNotEmpty(pCategory_1) ? pCategory_1.trim():"";
        String paramCategory_2_id = Tools.isNotEmpty(pCategory_2) ? pCategory_2.trim():"";
        String paramCategory_3_id = Tools.isNotEmpty(pCategory_3) ? pCategory_3.trim():"";

        if(Tools.isAnyEmpty(paramCategory_1_id,paramCategory_2_id))
        {
            Logger.error(FileArchivCategoryNodeDB.class, "getByCategoryObj: paramCategory_1_id alebo paramCategory_2_id je prazdne: paramCategory_1_id="+paramCategory_1_id+", paramCategory_2_id="+paramCategory_2_id);
            return null;
        }

        localId = Tools.getIntValue(paramCategory_2_id,-1);
        int id3 = Tools.getIntValue(paramCategory_3_id,-1);
        Node<FileArchivCategoryNodeBean> node = null;

        //ak je paramCategory_2_id alebo paramCategory_3_id cislo, moze to byt ID uzla a musime tento uzol najst a vratit ho
        int id1 = Tools.getIntValue(paramCategory_1_id, -1);
        if(id1 > -1 && localId > 0 && (Tools.isEmpty(paramCategory_3_id) || id3 > 0))
        {
            //kategoria 3
            if (id3 > 0)
            {
                node=getId(id3,id1,3);
            }
            else
            {
                //kategoria 2
                node=getId(localId,id1,2);
                if(node!=null)
                {
                    if(doNotReturnCat2)
                        return null;
                }
            }
        }

        //ak som nenansiel na zaklade nodeID, skusim vyhladat kategorie ako text
        if(node == null)
        {
            for (Node<FileArchivCategoryNodeBean> category_1 : getRoot().getChildren())
            {
                if(node != null) break;

                if(Tools.getIntValue(paramCategory_1_id,-1)==category_1.unwrap().getInt1Val())
                {
                    for (Node<FileArchivCategoryNodeBean> category_2 : category_1.getChildren())
                    {
                        if(node != null) break;

                        if(paramCategory_2_id.equals(category_2.unwrap().getCategoryName()))
                        {
                            if(Tools.isEmpty(paramCategory_3_id))
                            {
                                if(doNotReturnCat2)
                                    return null;

                                node = category_2;
                                break;
                            }
                            else
                            {
                                for (Node<FileArchivCategoryNodeBean> category_3 : category_2.getChildren())
                                {
                                    if(paramCategory_3_id.equals(category_3.unwrap().getCategoryName()))
                                    {
                                        node = category_3;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if(node != null)
            c.setObject(objname,node,240);

        return node;
    }

    public static boolean canShowCategory(int idCategory)
    {
        localId = idCategory;
        if(localId < 0)
        {
            Logger.debug(FileArchivCategoryNodeDB.class,"canShowCategory("+idCategory+") nenasiel som kategoriu (id)");
            return false;
        }
        Node<FileArchivCategoryNodeBean> node = getId(localId);
        if(node == null)
        {
            Logger.debug(FileArchivCategoryNodeDB.class,"canShowCategory("+idCategory+") nenasiel som kategoriu");
            return false;
        }
        return node.unwrap().isShow();
    }

    public static List<FileArchivCategoryNodeBean> getNodes(int level, int categoryId_1)
    {
        List<FileArchivCategoryNodeBean> listNodes = new ArrayList<>();

        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();

        ReadAllQuery dbQuery = new ReadAllQuery(FileArchivCategoryNodeBean.class);
        ExpressionBuilder builder = new ExpressionBuilder();

        Expression where = builder.get("categoryType").equal(FileArchivNodeTypeEnum.CATEGORY);
        where = where.and(builder.get("int1Val").equal(categoryId_1));
        where = where.and(builder.get("level").equal(level));
        dbQuery.setSelectionCriteria(where);

        @SuppressWarnings("unchecked")
        List<FileArchivCategoryNodeBean> result = em.createQuery(dbQuery).getResultList();
        if(!Tools.isEmpty(result))
        {
            listNodes.addAll(result);
        }
        em.clear();
        return listNodes;
    }

    public static boolean updateCategory(String newCategoryName, int nodeId)
    {
        FileArchivCategoryNodeBean categoryNodeBean = findById(nodeId);
        if(categoryNodeBean == null)
        {
            return false;
        }

        categoryNodeBean.setCategoryName(newCategoryName);
        return categoryNodeBean.save();
    }

    public static Node<FileArchivCategoryNodeBean> createSubNode(Node<FileArchivCategoryNodeBean> node, String categoryName, int category1id, int priority, boolean show)//leve1
    {
        FileArchivCategoryNodeBean faCategory = new FileArchivCategoryNodeBean();
        faCategory.setCategoryName(categoryName);
        faCategory.setPriority(priority);
        faCategory.setShow(show);
        faCategory.setCategoryType(FileArchivNodeTypeEnum.CATEGORY);
        if(category1id != -1)
            faCategory.setInt1Val(category1id);

        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        Node<FileArchivCategoryNodeBean> child = null;
        try
        {
            em.getTransaction().begin();
            //vytvorim novy list
            child = node.addChild(faCategory);
            em.getTransaction().commit();
        }catch (Exception e) {
            em.getTransaction().rollback();
            sk.iway.iwcm.Logger.error(e);
        }finally{
            em.clear();
        }
        return child;
    }

    public static List<Node<FileArchivCategoryNodeBean>> getCategories()
    {
        List<Node<FileArchivCategoryNodeBean>> fileArchivCategoryNodeBeans = FileArchivCategoryNodeDB.getRoot().getChildren();
        return fileArchivCategoryNodeBeans;
    }

    public static Node<FileArchivCategoryNodeBean> getCategory(int categoryId)
    {

        for(Node<FileArchivCategoryNodeBean> node : getCategories())
        {
            if(node.unwrap().getInt1Val() == categoryId)
            {
                return node;
            }
        }
        return null;
    }

    public static Node<FileArchivCategoryNodeBean> createNode(String categoryName, int category_1_id, int priority, boolean show)//leve1
    {
        FileArchivCategoryNodeBean faCategory = new FileArchivCategoryNodeBean();
        faCategory.setCategoryName(categoryName);
        faCategory.setPriority(priority);
        faCategory.setShow(show);
        faCategory.setCategoryType(FileArchivNodeTypeEnum.CATEGORY);
        if(category_1_id != -1)
            faCategory.setInt1Val(category_1_id);

        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        JpaNestedSetManager nsm = new JpaNestedSetManager(em);
        Node<FileArchivCategoryNodeBean> newNode = null;
        try
        {
            em.getTransaction().begin();
            //vytvorim novy node
            Node<FileArchivCategoryNodeBean> pn = new JpaNode<>(FileArchivCategoryNodeDB.getRoot().unwrap(),nsm);
            newNode = pn.addChild(faCategory);

            em.getTransaction().commit();
        }catch (Exception e) {
            em.getTransaction().rollback();
            sk.iway.iwcm.Logger.error(e);
        }finally{
            em.clear();
            nsm.clear();
        }

        return newNode;
    }
}