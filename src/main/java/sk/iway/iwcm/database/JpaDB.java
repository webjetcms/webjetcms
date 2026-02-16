package sk.iway.iwcm.database;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.utils.Pair;

/**
 *  JpaDB.java
 *
 *  Basic read, update, insert and delete operations for a JPA bean
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 28.2.2011 11:52:59
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class JpaDB<T extends ActiveRecordBase>
{
	private final Class<T> clazz;
	protected String dbName = "iwcm";

	public JpaDB(Class<T> clazz)
	{
		this.clazz = clazz;
		if (clazz.isAnnotationPresent(DataSource.class))
		{
			dbName = clazz.getAnnotation(DataSource.class).name();
		}
	}

	public JpaDB(Class<T> clazz, String dbName)
	{
		this.clazz = clazz;
		this.dbName = dbName;
	}

	public static <T extends ActiveRecordBase> JpaDB<T> of(Class<T> clazz)
	{
		return new JpaDB<T>(clazz);
	}

	public T getById(int id) {
		return getById((long)id);
	}

	public T getById(Long id)
	{
		//musime vyrobit novy EntityManager, pretoze ten threadovy em.find zatvori a potom nemusime mat otvoreny em dalej v kode (napr. v helpdesku)
		EntityManager threadEm = JpaTools.getEclipseLinkEntityManager(dbName);
		EntityManager em = threadEm.getEntityManagerFactory().createEntityManager();

		T obj;
		Long idLong = null;
		//pre ActiveRecordRepository musime posielat ID ako long
        Integer idInt = Integer.valueOf(id.intValue());
		if (clazz.getSuperclass().isAssignableFrom(ActiveRecordRepository.class)) idLong = Long.valueOf(id);

		if((InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")) && Constants.getString("jpaFilterByDomainIdBeanList").indexOf(clazz.getName()) != -1)
		{
			Map<String,Object>  hashMap = new HashMap<String,Object>();
			hashMap.put("domainId", CloudToolsForCore.getDomainId());
			if (idLong == null) obj = em.find(clazz, idInt, hashMap);
			else obj = em.find(clazz, idLong, hashMap);
		}
		else
		{
			if (idLong == null) obj = em.find(clazz, idInt);
			else obj = em.find(clazz, idLong);
		}

		//pre istotu
		if (em != null && em.isOpen()) em.close();

		return obj;
	}

	public List<T> getAll()
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(dbName);
		ReadAllQuery dbQuery = new ReadAllQuery(clazz);

        if((InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")) && Constants.getString("jpaFilterByDomainIdBeanList").indexOf(clazz.getName()) != -1)
			return find("domainId", CloudToolsForCore.getDomainId());

		Query query = em.createQuery(dbQuery);
		List<T> records = JpaDB.getResultList(query);

		return records;
	}

	public long getCount()
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(dbName);
        if((InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")) && Constants.getString("jpaFilterByDomainIdBeanList").indexOf(clazz.getName()) != -1)
        {
            Query query = em.createQuery("select count(e) from " + clazz.getSimpleName() + " e WHERE e.domainId = :domainId").setParameter("domainId",CloudToolsForCore.getDomainId());
            return Long.parseLong(query.getSingleResult().toString());
        }
		return Long.parseLong(em.createQuery("select count(e) from " + clazz.getSimpleName() + " e").getSingleResult().toString());
	}

	@SuppressWarnings("rawtypes")
	public List getValues(String property)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(dbName);
		try
		{
            if((InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")) && Constants.getString("jpaFilterByDomainIdBeanList").indexOf(clazz.getName()) != -1)
            {
                Query query = em.createQuery("select distinct e." + property + " FROM " + clazz.getSimpleName() + " e WHERE e.domainId = :domainId ORDER BY e." + property).setParameter("domainId",CloudToolsForCore.getDomainId());
                return JpaDB.getResultList(query);
            }
			return em.createQuery("select distinct e." + property + " FROM " + clazz.getSimpleName() + " e ORDER BY e." + property).getResultList();
		}
		finally
		{
			em.close();
		}

	}

	@SuppressWarnings("unchecked")
	public boolean save(T...entities)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(dbName);
		em.getTransaction().begin();
        try
		{
			for (T entity : entities)
			{
                if((InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")) && Constants.getString("jpaFilterByDomainIdBeanList").indexOf(clazz.getName()) != -1)
                {
                    try
                    {
                        Method method = clazz.getMethod("getDomainId");
                        int domainIdSave = (Integer) method.invoke(entity);
                        if(CloudToolsForCore.getDomainId() != domainIdSave && domainIdSave != 0)
                        {
                            Logger.debug(this,"Pokus o ulozenie zaznamu mimo domainId ("+domainIdSave+"). Spravna domainId = "+CloudToolsForCore.getDomainId());
                            return false;
                        }
                    }catch(Exception exc)
                    {
                        sk.iway.iwcm.Logger.error(exc);
                    }
                }

					 if (entity instanceof ActiveRecord)
					 {
						 ActiveRecord entityAR = (ActiveRecord)entity;
						 if(entityAR.getId() < 1)
						 {
							entityAR.setId(0);
							 em.persist(entity);
						 }
						 else
						 {
							 entity = em.merge(entity);
						 }
					 }
					 else if (entity instanceof ActiveRecordRepository)
					 {
						ActiveRecordRepository entityR = (ActiveRecordRepository)entity;
						 if(entityR.getId() == null || entityR.getId().longValue()< 1)
						 {
							entityR.setId(0L);
							em.persist(entity);
						 }
						 else
						 {
							entity = em.merge(entity);
						 }
					 }
			}

			em.getTransaction().commit();

			return true;
		}
		catch (Exception e)
		{
			try
			{
				em.getTransaction().rollback();
			}
			catch (Exception e2)
			{

			}
			sk.iway.iwcm.Logger.error(e);
		}
		finally
		{
			JpaTools.getEclipseLinkEntityManager(dbName).close();
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public boolean delete(T...entities)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(dbName);
		em.getTransaction().begin();
		try
		{
			for (T entity : entities)
			{
				entity = em.merge(entity);
				em.remove(entity);
			}

			em.getTransaction().commit();
			return true;
		}catch (Exception e) {
			em.getTransaction().rollback();
			sk.iway.iwcm.Logger.error(e);
		}finally{
			JpaTools.getEclipseLinkEntityManager(dbName).close();
		}
		return false;
	}

	public void deleteByIds(int...ids)
	{
		JpaTools.batchDelete(clazz, ids);
	}

	public List<T> find(String property, Object value)
	{
		return JpaTools.findByMatchingProperty(clazz, property, value);
	}

	public T findFirst(String property, Object value)
	{
        if((InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")) && Constants.getString("jpaFilterByDomainIdBeanList").indexOf(clazz.getName()) != -1)
        {
			  @SuppressWarnings("unchecked")
            List<T> listProp = findByProperties(new Pair<String, Object>(property, value), new Pair<String, Object>("domainId", CloudToolsForCore.getDomainId()));
            if(listProp != null && listProp.size() > 0)
                return listProp.get(0);
            return null;
        }
		return JpaTools.findFirstByMatchingProperty(clazz, property, value);
	}

	@SuppressWarnings("unchecked")
	public List<T> findByProperties(Pair<String, ? extends Object>... properties)
	{
		return JpaTools.findByProperties(clazz, properties);
	}

	@SuppressWarnings("unchecked")
	public T findFirstByProperties(Pair<String, ? extends Object>... properties)
	{
		return JpaTools.findFirstByProperties(clazz, properties);
	}

	/**
	 * Vrati zoznam objektov, ktore splnaju zadane podmienky.
	 * Priklad:
	 * <code>
	 * class ExampleDB extends JpaDB&lt;ExampleBean&gt;
	 * {
	 *   public List&lt;ExampleBean&gt; getExamples(String nameFilter) {
	 *     return findBy(filterSubstring("name"), nameFilter);
	 *   }
	 * }
	 * </code>
	 *
	 * @ see filterSubstring
	 * @ see filterIn
	 *
	 * @param conditions
	 * @return
	 */
	public List<T> findBy(JpaTools.Condition... conditions)
	{
		return findBy((Integer) null, conditions);
	}

	public List<T> findBy(Integer maxRows, JpaTools.Condition... conditions)
	{
		return JpaTools.findBy(clazz, maxRows, conditions);
	}

	// conditions:

	protected static JpaTools.Condition filterNull(final String property)
	{
		return new JpaTools.Condition() {

			@Override
			public Expression applyTo(Expression object)
			{
				return object.getField(property).isNull();
			}

		};
	}

	protected static JpaTools.Condition filterNotNull(final String property)
	{
		return new JpaTools.Condition() {

			@Override
			public Expression applyTo(Expression object)
			{
				return object.getField(property).isNull().not();
			}

		};
	}

	/**
	 * Podmienka je splnena, ak filter nie je zadany (null alebo prazdny) alebo ak sa hodnota vlastnosti rovna danej hodnote.
	 *
	 * @param property
	 * @param value
	 * @return
	 */
	protected static JpaTools.Condition filterEquals(final String property, final String value)
	{
		return new JpaTools.Condition() {

			@Override
			public Expression applyTo(Expression object)
			{
				return Tools.isEmpty(value) ? null : object.getField(property).equal(value);
			}

		};
	}

	protected static JpaTools.Condition filterEquals(final String property, final int value)
	{
		return new JpaTools.Condition() {

			@Override
			public Expression applyTo(Expression object)
			{
				return object.getField(property).equal(value);
			}

		};
	}

	protected static JpaTools.Condition filterEquals(final String property, final boolean value)
	{
		return new JpaTools.Condition() {

			@Override
			public Expression applyTo(Expression object)
			{
				return object.getField(property).equal(value);
			}

		};
	}

	/**
	 * Podmienka je splnena, ak filter nie je zadany (null alebo prazdny) alebo ak hodnota vlastnosti obsahuje tento podretazec.
	 *
	 * @param property   nazov vlastnosti
	 * @param substring  hladany podretazec
	 * @return
	 */
	protected static JpaTools.Condition filterSubstring(final String property, final String substring)
	{
		return new JpaTools.Condition() {

			@Override
			public Expression applyTo(Expression object)
			{
				return Tools.isEmpty(substring) ? null : object.getField(property).containsSubstring(substring);
			}

		};
	}

    /**
     * Podmienka je splnena, ak filter nie je zadany (null alebo prazdny) alebo ak hodnota vlastnosti obsahuje tento podretazec, nezalezi na velkosti pismen.
     *
     * @param property   nazov vlastnosti
     * @param substring  hladany podretazec
     * @return
     */
    protected static JpaTools.Condition filterSubstringIgnoringCase(final String property, final String substring)
    {
        return new JpaTools.Condition() {

            @Override
            public Expression applyTo(Expression object)
            {
                return Tools.isEmpty(substring) ? null : object.getField(property).containsSubstringIgnoringCase(substring);
            }

        };
    }

	/**
	 * Podmienka je splnena, ak filter nie je zadany (null alebo prazdny zoznam) alebo ak hodnota vlastnosti je v tomto zozname.
	 *
	 * @param property
	 * @param values
	 * @return
	 */
	public static JpaTools.Condition filterIn(final String property, final Collection<String> values)
	{
		return new JpaTools.Condition() {

			@Override
			public Expression applyTo(Expression object)
			{
				return Tools.isEmpty(values) ? null : object.getField(property).in(values);
			}

		};
	}

	/**
	 * Podmienka je splnena, ak filter nie je zadany (null alebo prazdny zoznam) alebo ak hodnota vlastnosti nie je v tomto zozname.
	 *
	 * @param property
	 * @param values
	 * @return
	 */
	protected static JpaTools.Condition filterNotIn(final String property, final Collection<String> values)
	{
		return new JpaTools.Condition() {

			@Override
			public Expression applyTo(Expression object)
			{
				return Tools.isEmpty(values) ? null : object.getField(property).not().in(values);
			}

		};
	}

	/**
	 * Podmienka je splnena, ak hodnota vlastnosti je v danom intervale (vratane oboch krajnych hodnot).
	 *
	 * @param property
	 * @param minValue
	 * @param maxValue
	 * @return
	 */
	protected static JpaTools.Condition filterBetween (final String property, final int minValue, final int maxValue)
	{
		return new JpaTools.Condition() {

			@Override
			public Expression applyTo(Expression object)
			{

				    return object.getField(property).between(minValue, maxValue);
			}

		};
	}

	protected static JpaTools.Condition filterBetween(final String property, final BigDecimal minValue, final BigDecimal maxValue)
	{
		return new JpaTools.Condition() {

			@Override
			public Expression applyTo(Expression object)
			{
				return and(
					(null == minValue) ? null : object.getField(property).greaterThanEqual(minValue),
					(null == maxValue) ? null : object.getField(property).lessThanEqual(maxValue)
				);
			}

		};
	}

	protected static JpaTools.Condition filterBetween(final String property, final Date minValue, final Date maxValue)
	{
		return new JpaTools.Condition() {

			@Override
			public Expression applyTo(Expression object)
			{
				return and(
					(null == minValue) ? null : object.getField(property).greaterThanEqual(minValue),
					(null == maxValue) ? null : object.getField(property).lessThanEqual(maxValue)
				);
			}

		};
	}

	/**
	 * Spojenie dvoch Expression cez "AND", zabezpecene proti hodnotam "null".
	 * Pomocka pre metody "filter..."
	 *
	 * @param e1
	 * @param e2
	 * @return
	 */
	public static Expression and(Expression e1, Expression e2)
	{
		return (null == e1) ? e2 : (null == e2) ? e1 : e1.and(e2);
	}

	/**
	 * Spojenie dvoch Expression cez "OR", zabezpecene proti hodnotam "null".
	 * Pomocka pre metody "filter..."
	 *
	 * @param e1
	 * @param e2
	 * @return
	 */
	public static Expression or(Expression e1, Expression e2)
	{
		return (null == e1) ? e2 : (null == e2) ? e1 : e1.or(e2);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <T> List<T> getResultList(Query q) {
		List list = q.getResultList();
		return list;
  }
}