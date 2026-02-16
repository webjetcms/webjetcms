package sk.iway.iwcm.components.dictionary;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.ReadObjectQuery;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.dictionary.model.DictionaryBean;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;

/**
 *  DictionaryDB.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 10.7.2005 19:06:00
 *@modified     $Date: 2005/10/25 06:48:06 $
 */
public class DictionaryDB
{
	private DictionaryDB() {

	}

	/**
	 * Vrati zoznam vsetkych slovicok
	 * @return
	 */
	public static List<DictionaryBean> getAll()
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		ReadAllQuery dbQuery = new ReadAllQuery(DictionaryBean.class);
		Query query = em.createQuery(dbQuery);
		List<DictionaryBean> result = JpaDB.getResultList(query);
		//System.out.println(result.get(0).getName());
		return result;

	}


	/**
	 * Ziskanie slovicok podla podmienok
	 * @param groups - zoznam skupin oddelenych ciarkou, alebo null
	 * @param orderBy - nazov JAVA PROPERTY (nie stlpca v DB), podla ktoreho sa robi order
	 * @return
	 */
	public static List<DictionaryBean> getItems(String groups, String orderBy) {
		return getItems(groups, null, orderBy);
	}

	/**
	 * Ziskanie slovicok podla podmienok
	 * @param groups - zoznam skupin oddelenych ciarkou, alebo null
	 * @param domain - domenove meno pre filtrovanie len slovicok pre tuto domenu
	 * @param orderBy - nazov JAVA PROPERTY (nie stlpca v DB), podla ktoreho sa robi order
	 * @return
	 */
	public static List<DictionaryBean> getItems(String groups, String domain, String orderBy)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		ExpressionBuilder builder = new ExpressionBuilder();
		ReadAllQuery dbQuery = new ReadAllQuery(DictionaryBean.class, builder);

		Expression expr = null;

		if (Tools.isNotEmpty(groups))
		{
			groups = Tools.replace(groups, "+", ",");
			groups = DB.removeSlashes(groups);
			expr = JpaDB.and(expr, builder.get("dictionaryGroup").in(groups.split(",")));

		}

		if (Tools.isNotEmpty(domain))
		{
			expr = JpaDB.and(expr, builder.get("domain").equalsIgnoreCase(domain));
		}

		if (expr != null) dbQuery.setSelectionCriteria(expr);

		if (orderBy != null)
		{
			StringTokenizer st = new StringTokenizer(orderBy, ",");
			while (st.hasMoreTokens())
			{
				dbQuery.addOrdering(builder.get(st.nextToken().trim()).ascending());
			}
		}

		Query query = em.createQuery(dbQuery);
		List<DictionaryBean> items = JpaDB.getResultList(query);

		return(items);
	}

	/**
	 * Vrati zoznam skupin slovicok (ako DictionaryBean)
	 * @return
	 */
	public static List<DictionaryBean> getGroups()
	{
		List<DictionaryBean> items = getItems(null, "dictionaryGroup");
		Map<String, String> groupsFound = new Hashtable<>();
		List<DictionaryBean> groups = new ArrayList<>();
		for(DictionaryBean d : items)
		{
			if (groupsFound.get(d.getDictionaryGroup())==null)
			{
				groupsFound.put(d.getDictionaryGroup(), "true");
				groups.add(d);
			}
		}

		return(groups);
	}

	/**
	 * Vrati zaznam z tabulky dictionary
	 * @param dictionaryId
	 * @return
	 */
	public static DictionaryBean getDictionary(int dictionaryId)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		return (em.find(DictionaryBean.class, dictionaryId));
	}

	/**
	 * Vrati list zaznamov z tabulky dictionary na zaklade mena
	 * @param dictionaryName
	 * @return
	 */
	public static List<DictionaryBean> getDictionariesByName(String dictionaryName, String dictionaryGroup)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		ExpressionBuilder builder = new ExpressionBuilder();
		ReadAllQuery raq = new ReadAllQuery(DictionaryBean.class);

		Expression exp = builder.get("name").equalsIgnoreCase(dictionaryName);
		exp = JpaDB.and(exp, builder.get("dictionaryGroup").equalsIgnoreCase(dictionaryGroup));

		raq.setSelectionCriteria(exp);
		Query q = em.createQuery(raq);
		return  JpaDB.getResultList(q);
	}

	/**
	 * Vrati DictionaryBean podla zadaneho nazvu a skupiny
	 * @param name
	 * @param dictionaryGroup
	 * @return
	 */
	public static DictionaryBean getDictionary(String name, String dictionaryGroup)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		ExpressionBuilder builder = new ExpressionBuilder();
		ReadObjectQuery dbQuery = new ReadObjectQuery(DictionaryBean.class, builder);

		Expression expr = builder.get("name").equal(name);
		expr = expr.and(builder.get("dictionaryGroup").equal(dictionaryGroup));

		dbQuery.setSelectionCriteria(expr);

		Query query = em.createQuery(dbQuery);

		DictionaryBean db = null;
		try
		{
			db = (DictionaryBean)query.getSingleResult();
		}
		catch (NoResultException e)
		{
			return null;
		}

		return db;
	}

	/** ziska tooltip **/
	private static DictionaryBean getDictionary(String name, String language, String domain) {
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		ExpressionBuilder builder = new ExpressionBuilder();
		ReadObjectQuery dbQuery = new ReadObjectQuery(DictionaryBean.class, builder);

		Expression expr = builder.get("name").equal(name);
		expr = expr.and(builder.get("language").equal(language));
		expr = expr.and(builder.get("domain").equal(domain));

		Logger.debug(DictionaryDB.class, "_______________________"+expr.toString());

		dbQuery.setSelectionCriteria(expr);

		Query query = em.createQuery(dbQuery);

		DictionaryBean db = null;
		try
		{
			db = (DictionaryBean)query.getSingleResult();
		}
		catch (NoResultException e)
		{
			return null;
		}

		return db;
	}



	/**
	 * vymaze zaznam
	 * @param dictionaryId
	 * @return
	 */
	public static boolean deleteDictionary(int dictionaryId)
	{
		boolean deleteOk = true;
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		DictionaryBean db = em.getReference(DictionaryBean.class, dictionaryId);

		try
		{
			em.getTransaction().begin();
			em.remove(db);
			em.getTransaction().commit();
		}
		catch (Exception e)
		{
			deleteOk = false;
			em.getTransaction().rollback();
			sk.iway.iwcm.Logger.error(e);
		}

		return deleteOk;
	}

	/**
	 * ulozi zaznam
	 * @param name
	 * @param nameOrig
	 * @param dictionaryGroup
	 * @param value
	 * @return
	 */
	public static boolean saveDictionary(String name, String nameOrig, String dictionaryGroup, String value)
	{
		boolean saveOk = true;

		DictionaryBean db = getDictionary(name, dictionaryGroup);

	   JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();

	   try
		{
	   	em.getTransaction().begin();
		   if (db == null)
		   {
		      db = new DictionaryBean();
		      db.setName(name);
		      db.setNameOrig(nameOrig);
		      db.setDictionaryGroup(dictionaryGroup);
		      em.persist(db);
		   }
		   db.setNameOrig(nameOrig);
		   db.setValue(value);

		   em.getTransaction().commit();
		}
		catch (Exception e)
		{
			saveOk = false;
			em.getTransaction().rollback();
			sk.iway.iwcm.Logger.error(e);
		}

  		return saveOk;
	}


	public static boolean saveTooltip(String name, String language, String domain, String value) {
		boolean saveOk = true;

		DictionaryBean db = getDictionary(name, language, domain);
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();

		   try
			{
		   	em.getTransaction().begin();
			   if (db == null)
			   {
				Logger.debug(DictionaryDB.class, "_______________________________null");
			      db = new DictionaryBean();
			      db.setName(name);
			      db.setDictionaryGroup("tooltip");
			      db.setDomain(domain);
			      db.setLanguage(language);
			      db.setValue(value);
			      em.persist(db);
			   }else{
				Logger.debug(DictionaryDB.class, "____________________________NOT___null");
			   }
			   db.setNameOrig("tooltip");
			   db.setName(name);
			   db.setDomain(domain);
			   db.setLanguage(language);
			   db.setValue(value);

			   em.getTransaction().commit();
			}
			catch (Exception e)
			{
				saveOk = false;
				em.getTransaction().rollback();
				sk.iway.iwcm.Logger.error(e);
			}

	  		return saveOk;
	}

	public static boolean saveTooltip(DictionaryBean dictionary) {
		if (Tools.isEmpty(dictionary.getDictionaryGroup())) dictionary.setDictionaryGroup("tooltip");
		if (Tools.isEmpty(dictionary.getNameOrig())) dictionary.setNameOrig(dictionary.getName());

		boolean saveOK = dictionary.save();
		return saveOK;
	}
}
