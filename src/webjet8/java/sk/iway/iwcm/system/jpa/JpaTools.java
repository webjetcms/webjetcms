package sk.iway.iwcm.system.jpa;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.internal.jpa.querydef.CompoundExpressionImpl;
import org.eclipse.persistence.internal.jpa.querydef.PathImpl;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.jpa.JpaHelper;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.ReportQuery;
import org.eclipse.persistence.queries.ReportQueryResult;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.DataSource;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.utils.Pair;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.Predicate;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 *  JpaTools.java - podporne nastroje pre JPA
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: rusho $
 *@version      $Revision: 1.3 $
 *@created      Date: 14.4.2010 15:33:13
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class JpaTools
{
	/**
	 * Vrati EntityManager pre zadany nazov DB spojenia (v povodnom JPA to je persistenceUnit)
	 *
	 * @param dbName - nazov DB spojenia
	 * @return EntityManager alebo null, ak pre DB spojenie neexistuje EntityManagerFactory
	 */
	public static EntityManager getEntityManager(String dbName)
	{
		return SetCharacterEncodingFilter.getEntityManager(dbName);
	}

	/**
	 * Vrati EntityManager pre defaultny nazov DB spojenia ("iwcm")
	 * @return an @EntityManager
	 */
	public static EntityManager getEntityManager()
	{
		return getEntityManager("iwcm");
	}


	/**
	 * Vrati JpaEntityManager pre zadany nazov DB spojenia.
	 *
	 * JpaEntityManager je EclipseLink-ove rozsirenie EntityManager-a, ktoreho funkcionalita je rozsirena oproti JPA specifikacii.
	 * Metoda skonvertuje EntityManager, vrateny metodou getEntityManager(dbName).
	 * @param dbName
	 * @return
	 */
	public static JpaEntityManager getEclipseLinkEntityManager(String dbName)
	{
		return JpaHelper.getEntityManager(getEntityManager(dbName));
	}

	/**
	 * Vrati JpaEntityManager pre defaultny nazov DB spojenia ("iwcm")
	 *
	 * JpaEntityManager je EclipseLink-ove rozsirenie EntityManager-a, ktoreho funkcionalita je rozsirena oproti JPA specifikacii.
	 * Metoda skonvertuje EntityManager, vrateny metodou getEntityManager().
	 * @return
	 */
	public static JpaEntityManager getEclipseLinkEntityManager()
	{
		return JpaHelper.getEntityManager(getEntityManager());
	}

	/**
	 * Vrati JpaEntityManager pre nazov DB spojenia ktory je nastaveny v danej classe cez anotaciu DataSource
	 *
	 * JpaEntityManager je EclipseLink-ove rozsirenie EntityManager-a, ktoreho funkcionalita je rozsirena oproti JPA specifikacii.
	 * Metoda skonvertuje EntityManager, vrateny metodou getEntityManager().
	 * @return
	 */
	public static JpaEntityManager getEclipseLinkEntityManager(Class<?> clazz)
	{
		String dbName = "iwcm";
		if (clazz.isAnnotationPresent(DataSource.class)) {

			Annotation annotation = clazz.getAnnotation(DataSource.class);
			DataSource dataSource = (DataSource) annotation;
			dbName = dataSource.name();
		}
		return JpaHelper.getEntityManager(getEntityManager(dbName));
	}

	public static List<String> getJpaClassNames(String rootUrl)
	{
		List<String> foundFiles = new ArrayList<String>();

		String basePackage = rootUrl;
		if (basePackage.startsWith("/WEB-INF/classes/")) basePackage = basePackage.substring(17);
		basePackage = basePackage.replace('/', '.');

		Logger.debug(JpaTools.class, "getJpaClassNamesByAnnotation.basePackage="+basePackage);

		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
		scanner.addIncludeFilter(new AnnotationTypeFilter(javax.persistence.Entity.class));
		scanner.addIncludeFilter(new AnnotationTypeFilter(javax.persistence.Converter.class));
		for (BeanDefinition bd : scanner.findCandidateComponents(basePackage))
		{
			Logger.debug(JpaTools.class, "found class: "+bd.getBeanClassName());
			foundFiles.add(bd.getBeanClassName());
		}

		//hladanie JpaInfo uz nepotrebujeme, kedze entity hladame podla anotacie a nie nazvu suboru

		return foundFiles;
	}

	/*
	public static List<String> getJpaClassNames(String rootUrl)
	{
		List<String> foundFiles = new ArrayList<String>();
		if (rootUrl.endsWith("/")== false)
			rootUrl = rootUrl + "/";

		File rootDir = new File(Tools.getRealPath(rootUrl));
		File files[] = rootDir.listFiles();
		if(files==null)
			return foundFiles;

		File myFile;
		int i;
		for (i=0; i<files.length; i++)
		{
			myFile = files[i];
			if (myFile.isDirectory())
			{
				foundFiles.addAll(getJpaClassNames(rootUrl + myFile.getName() + "/"));
			}
			else
			{
				//nacitanie suboru
				String filePath = rootUrl + myFile.getName();
				filePath = filePath.replace('\\', '/');
				if (filePath.startsWith("/WEB-INF/classes/") && (filePath.endsWith("Entity.class") || filePath.endsWith("Bean.class") || filePath.endsWith("/Media.class")) && filePath.endsWith("ActionBean.class")==false)
				{
					filePath = filePath.substring(17,filePath.length()-6).replace("/", ".");
					foundFiles.add(filePath);
					Logger.debug(JpaTools.class, "JPA: adding class: " + filePath);
				}
				else if (filePath.startsWith("/WEB-INF/classes/") && filePath.endsWith("JpaInfo.class"))
				{
					filePath = filePath.substring(17,filePath.length()-6).replace("/", ".");
					Class<? extends JpaInfo> jpaInfoClass = null;
					try
					{
						jpaInfoClass= ReflectUtil.findClass(filePath);
					} catch (ClassNotFoundException e)
					{

						sk.iway.iwcm.Logger.error(e);
					}//Class.forName(filePath)

					if (jpaInfoClass!=null && !jpaInfoClass.isInterface())
					{
						try
						{
							JpaInfo jpaInfo = jpaInfoClass.newInstance();
							List<String> foundClasses = jpaInfo.getJpaClasses();
							foundFiles.addAll(foundClasses);
							Logger.debug(JpaTools.class, "JPA: adding classes: " + foundClasses);
						}
						catch (InstantiationException | IllegalAccessException e)
						{
							sk.iway.iwcm.Logger.error(e);
						}
					}
					foundFiles.add(filePath);
					Logger.debug(JpaTools.class, "JPA: adding class: " + filePath);
				}
			}
		}

		return foundFiles;
	}*/


    public static <T> List<T> findByMatchingProperty(Class<T> clazz, String propertyName, Object propertyValue)
    {
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(clazz);
        try{
            ExpressionBuilder builder = new ExpressionBuilder();
            ReadAllQuery dbQuery = new ReadAllQuery(clazz, builder);
            Expression expr = builder.get(propertyName).equal(propertyValue);
            expr = applyDomainId(expr,builder,clazz);
            dbQuery.setSelectionCriteria(expr);

            Query query = em.createQuery(dbQuery);
            query.setHint("javax.persistence.cache.storeMode", "REFRESH");
            List<T> records = JpaDB.getResultList(query);
            return records;
        }catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }finally{
            em.close();
        }
        throw new IllegalStateException("Query did not complete regularly");
    }

	public static <T> T findFirstByMatchingProperty(Class<T> clazz, String propertyName, Object propertyValue)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(clazz);
		try{
			ExpressionBuilder builder = new ExpressionBuilder();
			ReadAllQuery dbQuery = new ReadAllQuery(clazz, builder);
			Expression expr = builder.get(propertyName).equal(propertyValue);
            expr = applyDomainId(expr,builder,clazz);
			dbQuery.setSelectionCriteria(expr);

			Query query = em.createQuery(dbQuery);
			query.setHint("javax.persistence.cache.storeMode", "REFRESH");
			@SuppressWarnings("unchecked")
			List<T> records = query.setMaxResults(1).getResultList();
			if (records == null || records.size() == 0)
				return null;
			return records.get(0);
		}catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}finally{
			em.close();
		}
		throw new IllegalStateException("Query did not complete normally");
	}

	@SuppressWarnings("all")
	@SafeVarargs
	public static <T> List<T> findByProperties(Class<T> clazz, Pair<String, ? extends Object>...properties)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(clazz);
		try{
			ExpressionBuilder builder = new ExpressionBuilder();
			ReadAllQuery dbQuery = new ReadAllQuery(clazz, builder);
			Expression expr = propertiesToExpression(builder, properties);
            expr = applyDomainId(expr,builder,clazz);
			dbQuery.setSelectionCriteria(expr);

			Query query = em.createQuery(dbQuery);
			List<T> records = JpaDB.getResultList(query);

			return records;
		}catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}finally{
			//em.close sa nesmie volat, pretoze to zavre threadLocal EM a dalej potom padne na inom citani, em zatvori SetCharacterEncodingFilter
			//em.close();
		}
		throw new IllegalStateException("Query did not complete normally");
	}

    public static <T> PaginatedBean<T> findPaginatedAndSortedByProperties(Class<T> clazz, Expression filter, int page, int pageSize, String sortField, JpaSortOrderEnum sortOrder)
    {
        PaginatedBean<T> paginatedBean = new PaginatedBean<>();
        ExpressionBuilder builder = new ExpressionBuilder();
        ReadAllQuery dbQuery = new ReadAllQuery(clazz, builder);

        if (sortField != null && sortOrder != null) {
            if (sortOrder.equals(JpaSortOrderEnum.ASC) && !sortField.equals("")) {
                dbQuery.addAscendingOrdering(sortField);
            }
            if (sortOrder.equals(JpaSortOrderEnum.DESC) && !sortField.equals("")) {
                dbQuery.addDescendingOrdering(sortField);
            }
        }

        filter = applyDomainId(filter,builder,clazz);
        dbQuery.setSelectionCriteria(filter);
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(clazz);

        try{
            Query query = em.createQuery(dbQuery);

            ReportQuery totalNumberQuery = new ReportQuery(clazz, filter);
//            totalNumberQuery.addCount();
			totalNumberQuery.addCount("COUNT", builder.distinct());
            ReportQueryResult res = (ReportQueryResult) em.createQuery(totalNumberQuery).getSingleResult();
            int total = (int) res.getByIndex(0);

            if (page < 1) page = 1;
//            if (pageSize < 1) pageSize = DEFAULT_COURSES_LIST_PAGE_SIZE;
            if (pageSize < 1) pageSize = 10;
            int offset = pageSize * (page-1);
            paginatedBean.setPage(page);
            paginatedBean.setPageSize(pageSize);
            paginatedBean.setTotal(total);

            query.setFirstResult(offset);
            query.setMaxResults(pageSize);
            List<T> records = JpaDB.getResultList(query);
            paginatedBean.setData(records);

            return paginatedBean;
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        } finally{
            em.close();
        }
        throw new IllegalStateException("Query did not complete normally");
    }

	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static <T> T findFirstByProperties(Class<T> clazz, Pair<String, ? extends Object>...properties)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(clazz);
		try{
			ExpressionBuilder builder = new ExpressionBuilder();
			ReadAllQuery dbQuery = new ReadAllQuery(clazz, builder);
			Expression expr = propertiesToExpression(builder, properties);
            expr = applyDomainId(expr,builder,clazz);
			dbQuery.setSelectionCriteria(expr);

			Query query = em.createQuery(dbQuery);
			List<T> records = query.setMaxResults(1).getResultList();
			if (records.size() == 0)
				return null;
			return records.get(0);
		}catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}finally{
			em.close();
		}
		throw new IllegalStateException("Query did not complete normally");
	}

	public static <T> List<T> findBy(Class<T> clazz, Condition... conditions)
	{
		return findBy(clazz, (Integer) null, conditions);
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> findBy(Class<T> clazz, Integer maxRows, Condition... conditions)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(clazz);
		try {
			ExpressionBuilder object = new ExpressionBuilder();
			ReadAllQuery query = new ReadAllQuery(clazz, object);
			Expression expr = null;
			for (Condition condition : conditions)
			{
				if (null == condition) continue;
				Expression cond = condition.applyTo(object);
				expr = (null == cond) ? expr : (null == expr) ? cond : expr.and(cond);
			}
            expr = applyDomainId(expr,object,clazz);
			query.setSelectionCriteria(expr);
			if (null != maxRows) query.setMaxRows(maxRows.intValue());
			return em.createQuery(query).getResultList();
		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		} finally {
			em.close();
		}
		throw new IllegalStateException("Query did not complete normally");
	}


	public static <T> void batchDelete(Class<T> clazz, int...ids)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(clazz);
		try
		{
			em.getTransaction().begin();
			for (int id : ids)
			{
				T toBeDeleted = em.getReference(clazz, covertPkToNumber(clazz, id));
				if(!canDeleteDomain(toBeDeleted.getClass(),toBeDeleted))
				{
					return;
				}
				em.remove(toBeDeleted);
			}

			em.getTransaction().commit();
		}
		catch(Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		finally
		{
			em.close();
		}
	}

	/**
	 * Vymaze podla ID jeden objekt
	 * @param clazz
	 * @param id
	 * @return true - ak vykonalo OK a false ak nastala vynimka
	 */
	public static <T> boolean  delete(Class <T> clazz,int id)
	{
		boolean result=false;
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(clazz);
		try
		{
			em.getTransaction().begin();
			T toBeDeleted = em.getReference(clazz, covertPkToNumber(clazz, id));
			if(!canDeleteDomain(toBeDeleted.getClass(),toBeDeleted))
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

    /** Nepovolime zmazat zaznam mimo aktualnej domeny
     *
     * @param clazz
     * @param o
     * @return
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static boolean canDeleteDomain(Class clazz, Object o) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        if((InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")) && Constants.getString("jpaFilterByDomainIdBeanList").indexOf(clazz.getName()) != -1)
        {
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

	public static <T> List<T> getAll(Class<T> clazz)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(clazz);
        ExpressionBuilder object = new ExpressionBuilder();
		ReadAllQuery dbQuery = new ReadAllQuery(clazz, object);
        Expression expr = applyDomainId(null, object, clazz);
        dbQuery.setSelectionCriteria(expr);
		Query query = em.createQuery(dbQuery);
		query.setHint("javax.persistence.cache.storeMode", "REFRESH");
		List<T> records = JpaDB.getResultList(query);

		return records;
	}

	/**
	 * Urci, ci dane meno datasource je urcene pre JPA, je to len iwcm, alebo nazov obsahujuci jpa (je to takto kvoli rychlosti startu WJ a optimalizacii pamate)
	 * @param name
	 * @return
	 */
	public static boolean isJPADatasource(String name)
	{
		if ("iwcm".equals(name) || name.indexOf("jpa")!=-1) return true;
		return false;
	}

	/**
	 * Podmienka na vyhladanie objektov pomocou metody "findBy".
	 */
	public interface Condition
	{
		/**
		 * Vytvori podmienku pre testovany objekt.
		 * Vysledna hodnota "null" znamena: tuto podmienku neberte do uvahy (cize automaticky ju splna kazdy objekt).
		 *
		 * @param object  vyraz vyjadrujuci testovany objekt
		 * @return        kriterium pre testovany objekt, alebo null
		 */
		public Expression applyTo(Expression object);
	}

	@SuppressWarnings("unchecked")
	public static Expression propertiesToExpression(ExpressionBuilder builder, Pair<String, ? extends Object>...properties) {
	    Expression  expr = null;
        for (Pair<String, ? extends Object> property : properties)
        {
            Expression nestedProperty = null;
            for (String subProperty : property.first.split("\\.")) {
                nestedProperty = (nestedProperty == null) ? builder.get(subProperty) : nestedProperty.get(subProperty);
            }
			if (nestedProperty != null) {
				Expression newCondition = nestedProperty.equal(property.second);
				expr = (expr == null) ? newCondition : expr.and(newCondition) ;
			}
        }
        return expr;
    }

	@SuppressWarnings("rawtypes")
    private static Expression applyDomainId(Expression exprParam,ExpressionBuilder builderParam,Class clazz)
    {
        Expression expr = exprParam;
        if((InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")) && Constants.getString("jpaFilterByDomainIdBeanList").indexOf(clazz.getName()) != -1)
        {

            ExpressionBuilder builder = builderParam;
            if(expr == null)
            {
                return builder.get("domainId").equal(CloudToolsForCore.getDomainId());
            }
            return  expr.and(builder.get("domainId").equal(CloudToolsForCore.getDomainId()));
        }
        return expr;
	 }

	 /**
	  * Ziska PK ako Long pre repository, alebo Integer pre standardne JPA
	  * @param <T>
	  * @param clazz
	  * @param id
	  * @return
	  */
	 public static <T> Number covertPkToNumber(Class<T> clazz, int id) {
		Number n = null;
		//musime cez String, pretoze volanie isInstance z nejakeho dovodu vracalo false
		if (clazz.getSuperclass().toString().contains("ActiveRecordRepository")) n = Long.valueOf(id);
		else n = Integer.valueOf(id);
		return n;
	 }

	 /**
	  * Removes predicate for field name from predicates list
	  * @param name
	  * @param predicates
	  */
	 public static void removePredicateWithName(String name, List<Predicate> predicates) {
		try {
			//remove predicates with name groupId
			List<Predicate> predicatesToRemove = new ArrayList<>();
			for (Predicate p : predicates) {
				if (p instanceof CompoundExpressionImpl) {
					CompoundExpressionImpl predicate = (CompoundExpressionImpl) p;
					if (predicate.getChildExpressions().size()>0 && predicate.getChildExpressions().get(0) instanceof PathImpl) {
						@SuppressWarnings("rawtypes")
						PathImpl path = (PathImpl) predicate.getChildExpressions().get(0);
						if (path.getCurrentNode().getName().equals(name)) {
							predicatesToRemove.add(predicate);
						}
					}
				}
			}
			predicates.removeAll(predicatesToRemove);
		} catch (Exception e) {
			Logger.error(e);
		}
	}
}
