package sk.iway.iwcm.components.insertScript; //NOSONAR

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.Query;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.system.jpa.JpaTools.Condition;

/**
 *  InsertScriptDB.java
 *
 *	DAO class for manipulating with InsertScriptBean
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 14.09.2016 14:46:14
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@SuppressWarnings("java:S127")
public class InsertScriptDB extends JpaDB<InsertScriptBean>
{
	private static InsertScriptDB instance;
	private static String cachePrefix = "InsertScript.";
	public InsertScriptDB()
	{
		super(InsertScriptBean.class);
	}

	public static InsertScriptDB getInstance()
	{
		if(instance == null)
			instance = new InsertScriptDB();

		return instance;
	}

	@SuppressWarnings("java:S1871")
	public List<InsertScriptBean> filter(String name, String position, String scriptBody, int docId, int groupId, Date validFrom, Date validTo)
	{
		//skontrolovat vypis a pridat ku conditions listu cez hash table

		List<Condition> conditions = new ArrayList<>();
		conditions.add(JpaDB.filterSubstring("name", name));
		conditions.add(JpaDB.filterSubstring("position", position));
		conditions.add(JpaDB.filterSubstring("script_body", scriptBody));
//		conditions.add(InsertScriptDB.filterBetween("valid_from", validFrom, null));
//		conditions.add(InsertScriptDB.filterBetween("valid_to", null, validTo));
		conditions.add(JpaDB.filterEquals("domain_id", CloudToolsForCore.getDomainId()));
		List<InsertScriptBean> insertScriptList = JpaTools.findBy(InsertScriptBean.class, conditions.toArray(new Condition[]{}));

		try {

			Date validFromBeanDate = null;
        	Date validToBeanDate = null;

			for(int i=0;i<insertScriptList.size();i++)
			{
				validFromBeanDate = insertScriptList.get(i).getValidFrom();
				validToBeanDate = insertScriptList.get(i).getValidTo();
				//ak mame vo filtri zadany len datum platnosti od a nemame datum do
				if(validFrom != null && validTo == null)
				{
					//mazeme tie zaznamy ktore: maju datum zaciatku pred datumom vo filtri a zaroven nemaju zadany konec platnosti alebo ho maju zadany ale je pred datumom zaciatku z filtra
					if(validFromBeanDate != null && validFromBeanDate.getTime() < validFrom.getTime() && (validToBeanDate == null || validToBeanDate.getTime() < validFrom.getTime()))
					{
						insertScriptList.remove(i);
						i--;
					}//mazeme tie ktorym konci platnost pred datumom z filtru
					else if(validFromBeanDate == null && validToBeanDate != null && validFrom.getTime() > validToBeanDate.getTime())
					{
						insertScriptList.remove(i);
						i--;
					}

				}
				else if(validTo != null && validFrom == null)
				{
					if(validFromBeanDate != null && validFromBeanDate.getTime() > validTo.getTime() || (validFromBeanDate == null && validToBeanDate != null && validToBeanDate.getTime() > validTo.getTime()))
					{
						insertScriptList.remove(i);
						i--;
					}
				}
				else if (validFrom != null && validTo != null)
				{
					if(validFromBeanDate != null && validFromBeanDate.getTime() > validTo.getTime())
					{
						insertScriptList.remove(i);
						i--;
					}
					else if(validToBeanDate != null && validToBeanDate.getTime() < validFrom.getTime())
					{
						insertScriptList.remove(i);
						i--;
					}
				}

			}

		} catch (Exception ex) {
			Logger.error(InsertScriptDB.class, ex);
		}

		List<InsertScriptBean> insertScriptListFilterdByDocsAndGroups = filterByDocAndGroupId(insertScriptList, docId, groupId);

		return insertScriptListFilterdByDocsAndGroups;
		//return intersection(insertScriptListFilterdByDocsAndGroups, insertScriptList);
	}

	private static List<InsertScriptBean> filterByDocAndGroupId(List<InsertScriptBean> insertScriptList, int docId, int groupId)
	{
		if(docId == -1 && groupId == -1) return insertScriptList;

		if(groupId == -1)
		{
			DocDetails dd = DocDB.getInstance().getBasicDocDetails(docId, false);
			if(dd != null) groupId = dd.getGroupId();
		}

		Set<InsertScriptBean> hs = new HashSet<>();
		List<GroupDetails> parentGroups = null;
		if (groupId > 0) parentGroups = GroupsDB.getParentGroupsCached(groupId);

		for(InsertScriptBean isb : insertScriptList)
		{
			//ak je prazdne groups aj docs pridaj (zobrazuje sa vsade)
			if ((isb.getDocIds()==null || isb.getDocIds().isEmpty()) && (isb.getGroupIds()==null || isb.getGroupIds().isEmpty()))
			{
				hs.add(isb);
			}
			else
			{
				if (isb.getGroupIds()!=null && isb.getGroupIds().isEmpty()==false && parentGroups!=null)
				{
					// pre adresare
					for(GroupDetails gd:parentGroups)
					{
						for(InsertScriptGroupBean isgb:isb.getGroupIds())
						{
							if(isgb.getGroupId() == gd.getGroupId()) hs.add(isb);
						}
					}
				}
				if (isb.getDocIds()!=null && isb.getDocIds().isEmpty()==false)
				{
					for(InsertScriptDocBean isdb:isb.getDocIds())
					{
						if(isdb.getDocId() == docId || docId == -1) hs.add(isb);
					}
				}
			}
		}

		insertScriptList.clear();
		insertScriptList.addAll(hs);
		return insertScriptList;
	}

	public List<InsertScriptBean> findByCreateDate(Date createDate)
	{
		return JpaTools.findByMatchingProperty(InsertScriptBean.class, "createDate", createDate);
	}

	public List<InsertScriptBean> findByDomainId(int domainId)
	{
		return JpaTools.findByMatchingProperty(InsertScriptBean.class, "domainId", domainId);
	}

	public InsertScriptBean findFirstByCreateDate(Date createDate)
	{
		return JpaTools.findFirstByMatchingProperty(InsertScriptBean.class, "createDate", createDate);
	}
	public List<InsertScriptBean> findByUser(int user)
	{
		return JpaTools.findByMatchingProperty(InsertScriptBean.class, "user", user);
	}

	public InsertScriptBean findFirstByUser(int user)
	{
		return JpaTools.findFirstByMatchingProperty(InsertScriptBean.class, "user", user);
	}
	public List<InsertScriptBean> findByScriptBody(String scriptBody)
	{
		return JpaTools.findByMatchingProperty(InsertScriptBean.class, "scriptBody", scriptBody);
	}

	public InsertScriptBean findFirstByScriptBody(String scriptBody)
	{
		return JpaTools.findFirstByMatchingProperty(InsertScriptBean.class, "scriptBody", scriptBody);
	}

	/** Cached
	 *
	 * @param position
	 * @param onlyActive - true(porovnava podla validFrom a validTo) / false (neporovnava)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<InsertScriptBean> findByPosition(String position, boolean onlyActive)
	{
		String cacheKey = getCacheKey(position, onlyActive);
		List<InsertScriptBean> isbList = null;
		//pre admina budeme vzdy mat cisty zoznam (bez cachovania)
		if (RequestBean.isAdminLogged()==false) isbList = (List<InsertScriptBean>) Cache.getInstance().getObject(cacheKey);
		if(isbList == null)
		{
			JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
			isbList = new LinkedList<>();
			try
			{
				ExpressionBuilder builder = new ExpressionBuilder();
				ReadAllQuery dbQuery = new ReadAllQuery(InsertScriptBean.class, builder);
				Expression expr = builder.get("position").equal(position);
				if(onlyActive)
				{
					expr = expr.and(builder.get("validFrom").lessThan(new Date()).or(builder.get("validFrom").equal(null)));
					expr = expr.and(builder.get("validTo").greaterThan(new Date()).or(builder.get("validTo").equal(null)));
				}
				dbQuery.setSelectionCriteria(expr);

				Expression expr1 = builder.get("changeDate");
				List<Expression> expressions = new ArrayList<>();
				expressions.add(expr1);

				dbQuery.addAscendingOrdering("id");

				Query query = em.createQuery(dbQuery);
				isbList = JpaDB.getResultList(query);
			}
			catch (Exception e) {
				//nevypiseme, asi neexistuje dana pozicia, len to potom plni logy, sk.iway.iwcm.Logger.error(e);
			}
			finally{
				em.close();
			}

			//musime setnut cache, aby sa nam to stale nedotazovalo do DB
			LinkedList<InsertScriptBean> isbListCopy = new LinkedList<>();
			if (isbList!=null) isbListCopy.addAll(isbList);
			Cache.getInstance().setObject(cacheKey, isbListCopy, getCacheInMinutes());
		}

		//musime takto, aby nam niekto z vonku nemenil cache objekty (co by nam menil)
		List<InsertScriptBean> retList = new LinkedList<>();
		if (isbList!=null) retList.addAll(isbList);

		return retList;
	}

	public List<InsertScriptBean> findByPositionSubString(String position)
	{
		List<Condition> conditions = new ArrayList<>();
		conditions.add(JpaDB.filterSubstring("position", position));
		return  JpaTools.findBy(InsertScriptBean.class, conditions.toArray(new Condition[]{}));
	}

	public static String getCacheKey(String position, boolean onlyActive)
	{
		return cachePrefix+"findByPosition("+position+","+onlyActive+")";
	}

	public static String getCachePrefix()
	{
		return cachePrefix;
	}

	public static void deleteCache()
	{
		Cache.getInstance().removeObjectStartsWithName(cachePrefix);
		ClusterDB.addRefresh("sk.iway.iwcm.Cache-" + cachePrefix);
	}

	public static int getCacheInMinutes()
	{
		return Constants.getInt("insertScriptCacheMinutes");
	}
	//return JpaTools.findByMatchingProperty(InsertScriptBean.class, "position", position);

	public InsertScriptBean findFirstByPosition(int position)
	{
		return JpaTools.findFirstByMatchingProperty(InsertScriptBean.class, "position", position);
	}
}