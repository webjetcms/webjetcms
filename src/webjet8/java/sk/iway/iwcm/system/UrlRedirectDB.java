package sk.iway.iwcm.system;

import static sk.iway.iwcm.Tools.firstNonNull;
import static sk.iway.iwcm.Tools.isNotEmpty;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.servlet.ServletContext;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.*;
import sk.iway.iwcm.admin.layout.MenuService;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.system.jpa.JpaTools;

/**
 *  UrlRedirectDB.java - spravuje presmerovania stranok vo WebJETe
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: jeeff $
 *@version      $Revision: 1.10 $
 *@created      Date: 21.11.2006 15:43:35
 *@modified     $Date: 2010/01/20 10:13:50 $
 */
public class UrlRedirectDB
{

	public static final int NEW = 2;
	public static final int OLD = 1;
	public static final int OLD_EXACT = 3;
	private static final String CACHED_REDIRECTS = "UrlRedirectDB.CACHED_REDIRECTS";
	private static String regExpCacheKey = "regularExpressionsRedirects";
	private static String regExpPrefix = "regexp:";

	private static final String TIMESTAMP_OF_LAST_RUN = "UrlRedirectDB.TIMESTAMP_OF_LAST_RUN";
	private static final String TIMESTAMP_OF_NEXT_RUN = "UrlRedirectDB.TIMESTAMP_OF_NEXT_RUN";
	private static final int TIMESTAMP_VALID_IN_MINUTES = 1440; // 24 hod

	private static final Map<String, String> adminRedirects = new HashMap<>();
	static {
		adminRedirects.put("/admin/editor.do", "/admin/v9/webpages/web-pages-list/");
		adminRedirects.put("/admin/editor", "/admin/v9/webpages/web-pages-list/");
		adminRedirects.put("/admin/webpages/", "/admin/v9/webpages/web-pages-list/");
		adminRedirects.put("/admin/listgroups.do", "/admin/v9/webpages/web-pages-list/");
		adminRedirects.put("/admin/photogallery.do", "/admin/v9/apps/gallery/");
		adminRedirects.put("/components/gallery/admin_gallery_popup.jsp", "/admin/v9/apps/image-editor/");
	}

	protected UrlRedirectDB() {
		//utility class
	}

	public static UrlRedirectBean getById(Long id)
	{
		return JpaTools.getEclipseLinkEntityManager().find(UrlRedirectBean.class, id);
	}

	/**
	 * Prida redirect do databazy, z URL odstrani parametre ze znakom ? (ak tam nejake nebodaj su)
	 * @param oldUrl
	 * @param newUrl
	 * @param redirectCode
	 */
	public static void addRedirect(String oldUrl, String newUrl, String domainName, int redirectCode)
	{
		if(Constants.getBoolean("editorDisableAutomaticRedirect")) return;

		if (Tools.isEmpty(oldUrl) || Tools.isEmpty(newUrl)) return;

		if (oldUrl.equals(newUrl)) return;
		//odstran parametre
		int i = oldUrl.indexOf('?');
		if (i > 0) oldUrl = oldUrl.substring(0, i);
		i = newUrl.indexOf('?');
		if (i > 0) newUrl = newUrl.substring(0, i);

		UrlRedirectBean urlRedirect = new UrlRedirectBean();
		urlRedirect.setOldUrl(oldUrl);
		urlRedirect.setNewUrl(newUrl);
		if (domainName == null) domainName = "";
		urlRedirect.setDomainName(domainName);
		urlRedirect.setRedirectCode(redirectCode);
		urlRedirect.setInsertDate(new Date());
		save(urlRedirect);
	}

	/**
	 * Ziska najnovsi redirect zadaneho URL, alebo null ak redirect neexistuje
	 * @param oldUrl
	 * @return
	 */
	public static String getRedirect(String oldUrl, String domainName)
	{
		UrlRedirectBean redirect = getRedirectBean(oldUrl, domainName);
		if(redirect != null) return redirect.getNewUrl();
		return null;
	}

	/**
	 * Get's newest redirect for oldUrl and domainName or returns NULL if doesn't exist
	 * @param oldUrl
	 * @param domainName
	 * @return
	 */
	public static UrlRedirectBean getRedirectBean(String oldUrl, String domainName)
	{
		//redirect's of old admin pages, always have precedense
		String redirectURL = adminRedirects.get(oldUrl);
		if (redirectURL!=null) {
			return new UrlRedirectBean(oldUrl, redirectURL, 302, "");
		}

		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb!=null && rb.isUserAdmin() && Tools.isNotEmpty(oldUrl)) {
			//redirect of old admin pages, always have precedense
			String v9link = MenuService.replaceV9MenuLink(oldUrl);
			if (v9link!=null && v9link.equals(oldUrl)==false) return new UrlRedirectBean(oldUrl, v9link, 302, domainName);
		}

		UrlRedirectBean redirectBean;
		if (Constants.getBoolean("multiDomainEnabled")==true && Tools.isNotEmpty(domainName))
		{
			redirectBean = getRedirectImpl(oldUrl, domainName);
			if (redirectBean != null) return redirectBean;
			//stare WebJETy negenerovali / na konci adresara
			if (oldUrl.indexOf('^')==-1) redirectBean = getRedirectImpl(oldUrl + "/", domainName);
			if (redirectBean != null) return redirectBean;
		}
		else
		{
			//failsafe pre standardny WebJET - hladanie bez ohladu na domenu
			redirectBean = getRedirectImpl(oldUrl, "");
			if (redirectBean != null) return redirectBean;
		}

		//stare WebJETy negenerovali / na konci adresara
		if (oldUrl.indexOf('^')==-1) redirectBean = getRedirectImpl(oldUrl + "/", "");
		if (redirectBean != null) return redirectBean;

		if (oldUrl.endsWith("/")==false && oldUrl.endsWith(".html")==false)
		{
			//skus najst stranku s / na konci (/produkty vs /produkty/)
			DocDB docDB = DocDB.getInstance();
			String newUrl = oldUrl+"/";
			if (docDB.getDocIdFromURLImpl(newUrl, domainName)>0)
			{
				return new UrlRedirectBean(oldUrl, newUrl, 302, domainName);
			}
		}

		return null;
	}

	public static int deleteOldRedirects() {

		EntityManager em = JpaTools.getEclipseLinkEntityManager();

		// choose all unique redirect urls with more than one record
		TypedQuery<UrlRedirectBean> query = em.createQuery("select redirect from UrlRedirectBean redirect where redirect.urlRedirectId in (select min(r.urlRedirectId) from UrlRedirectBean r group by r.oldUrl, r.domainName having count(r.urlRedirectId) > 1)", UrlRedirectBean.class);
		em.getTransaction().begin();
		List<UrlRedirectBean> allRedirects = query.getResultList();
		em.getTransaction().commit();

		int affected = 0;

		for(UrlRedirectBean redirect : allRedirects) {
			List<Long> idsToRemove = new ArrayList<>();

			try {
				// search actual and older redirects for iterated redirect
				List<UrlRedirectBean> redirects = search(redirect.oldUrl, OLD_EXACT, redirect.domainName);
				if(redirects.size() > 1) {

					// all found redirects except first are old
					redirects.remove(0);

					// map old redirects ids to list
					idsToRemove = redirects.stream().map(i -> i.getId()).collect(Collectors.toList());

					// delete old redirects if exists
					if(idsToRemove != null && idsToRemove.size() > 0){
						affected++;

						em = JpaTools.getEclipseLinkEntityManager();
						TypedQuery<UrlRedirectBean> q = em.createQuery("delete from UrlRedirectBean r where r.urlRedirectId in :ids", UrlRedirectBean.class);
						q.setParameter("ids", idsToRemove);

						em.getTransaction().begin();
						q.executeUpdate();
						em.getTransaction().commit();
					}
				}
			} catch(Exception e) {
				Adminlog.add(Adminlog.TYPE_REDIRECT_UPDATE, "CRON: deleteOldRedirects urlRedirectId[" + redirect.getId() + "], idsToRemove[" + Tools.join(idsToRemove) + "]", redirect.getId().intValue(), -1);
			}

		}

		// clear old publish dates
		em = JpaTools.getEclipseLinkEntityManager();
		TypedQuery<UrlRedirectBean> q = em.createQuery("update UrlRedirectBean r set r.publishDate = null where r.publishDate <= :date", UrlRedirectBean.class);
		q.setParameter("date", new Date());

		em.getTransaction().begin();
		q.executeUpdate();
		em.getTransaction().commit();

		// reload cache
		try {
			if (Constants.getBoolean("cacheUrlRedirects") && affected > 0)
			{
				reloadCache();
			}
		} catch(Exception e) {
			Logger.error(UrlRedirectDB.class, "reloadCache failed", e);
			Adminlog.add(Adminlog.TYPE_REDIRECT_UPDATE, "CRON: deleteOldRedirects - reloadCache failed.", -1, -1);
		}

		// return count of affected urls
		return affected;
	}

	private static UrlRedirectBean getRedirectImpl(String oldUrl, String domainName)
	{
		try
		{
			if (Constants.getBoolean("cacheUrlRedirects"))
			{
				Cache c = Cache.getInstance();
				Long lastRun = c.getObject(TIMESTAMP_OF_LAST_RUN, Long.class);
				Long nextRun = c.getObject(TIMESTAMP_OF_NEXT_RUN, Long.class);
				if (lastRun == null || nextRun == null || new Date().getTime() >= nextRun)  {
					reloadCache();
				}

				domainName = firstNonNull(domainName, "");
				oldUrl = normalizeUrl(oldUrl);
				Map<String, Map<String, UrlRedirectBean>> redirects = getCachedRedirects();
				if (redirects == null) return null;

				UrlRedirectBean toReturn = redirects.get(domainName).get(oldUrl);

				// cache reload is not that often, so do check of publishDate and validTo here
				if(toReturn != null && (toReturn.getValidTo() != null || toReturn.getPublishDate() != null)) {
					Date now = new Date();
					boolean noMoreValid = toReturn.getValidTo() != null && toReturn.getValidTo().before(now);
					boolean notPublishedYet = toReturn.getPublishDate() != null && toReturn.getPublishDate().after(now);

					if(noMoreValid || notPublishedYet) {
						// remove from cache
						removeRedirectFromCache(toReturn);
						toReturn = null;
					}
				}
				return toReturn;
			}

			String params = null;

			int paramsIndex = oldUrl.indexOf('?');
			if (paramsIndex > 0)
			{
				if (!oldUrl.endsWith("?"))
					params = oldUrl.substring(paramsIndex+1);
				oldUrl = oldUrl.substring(0, paramsIndex);
			}

			//aby sme dokazali spravit exact match z 404.jsp
			oldUrl = Tools.replace(oldUrl, "^", "?");

			List<UrlRedirectBean> redirects = search(oldUrl, OLD_EXACT, domainName);

			if (redirects!=null && redirects.size() > 0)
			{
				UrlRedirectBean urlRedirect = redirects.get(0);
				StringBuilder newUrl = new StringBuilder(urlRedirect.getNewUrl());

				if (params != null) newUrl.append('?').append(params);

				UrlRedirectBean redirect = new UrlRedirectBean(urlRedirect.getOldUrl(), newUrl.toString(), urlRedirect.getRedirectCode(), urlRedirect.getDomainName());
				return redirect;
			}

			String regExpURL = regExpRedirect(oldUrl, domainName);
			if (regExpURL != null) {
				if (params != null) regExpURL += '?'+ params;
				return new UrlRedirectBean(oldUrl, regExpURL, 302, domainName);
			}
		}
		catch (Exception e){
			sk.iway.iwcm.Logger.error(e);
		}
		return(null);
	}

	private static synchronized Map<String, Map<String, UrlRedirectBean>> reloadCache() {
		Cache c = Cache.getInstance();
		c.setObject(TIMESTAMP_OF_LAST_RUN, new Date().getTime(), TIMESTAMP_VALID_IN_MINUTES);
		Date date = getDateOfNextChange();
		c.setObject(TIMESTAMP_OF_NEXT_RUN, date.getTime(), TIMESTAMP_VALID_IN_MINUTES);

		Map<String, Map<String, UrlRedirectBean>> redirects = new HashMap<>();

		JpaEntityManager manager = JpaTools.getEclipseLinkEntityManager();
		try {
			java.sql.Time now = new java.sql.Time(new Date().getTime());
			org.eclipse.persistence.expressions.Expression conditions = new ExpressionBuilder().get("publishDate").lessThanEqual(now);
			conditions = conditions.or( new ExpressionBuilder().get("publishDate").isNull() );

			// check that valid_to is in the future or is null
			conditions = conditions.and( new ExpressionBuilder().get("validTo").greaterThanEqual(now).or( new ExpressionBuilder().get("validTo").isNull() ) );

			ReadAllQuery query = new ReadAllQuery(UrlRedirectBean.class, conditions);
			if(Constants.DB_TYPE == Constants.DB_ORACLE || Constants.DB_TYPE == Constants.DB_PGSQL) {
				query.addOrdering(new ExpressionBuilder().get("publishDate").descending().nullsLast());
				query.addOrdering(new ExpressionBuilder().get("insertDate").descending().nullsLast());
			} else {
				query.addOrdering(new ExpressionBuilder().get("publishDate").descending());
				query.addOrdering(new ExpressionBuilder().get("insertDate").descending());
			}

			List<UrlRedirectBean> list = JpaDB.getResultList(manager.createQuery(query));

			for (UrlRedirectBean b : list) {
				String domain = b.getDomainName();
				if (!redirects.containsKey(domain)) redirects.put(domain, new HashMap<>());
				String oldUrl = normalizeUrl(b.getOldUrl());
				if (redirects.get(domain).containsKey(oldUrl)==false) {
					UrlRedirectBean redirect = new UrlRedirectBean(oldUrl, b.getNewUrl(), b.getRedirectCode(), b.getDomainName());
					redirects.get(domain).put(oldUrl, redirect);
				}

			}

		} catch (Exception e){

		}finally{
			if (manager != null) manager.close();
		}

		ServletContext context = Constants.getServletContext();
		context.setAttribute(CACHED_REDIRECTS, redirects);

		return redirects;
	}

	/**
	 * Zisti ci ma zmysel vykonat reload odkazov.
	 * @return
	 */
	private static boolean isReloadNecessary() {
		boolean result = false;

		JpaEntityManager manager = JpaTools.getEclipseLinkEntityManager();
		try {
			java.sql.Time now = new java.sql.Time(new Date().getTime());
			org.eclipse.persistence.expressions.Expression conditions = new ExpressionBuilder().get("publishDate").greaterThanEqual( now );
			conditions = conditions.or( new ExpressionBuilder().get("validTo").lessThanEqual( now ) );

			ReadAllQuery query = new ReadAllQuery(UrlRedirectBean.class, conditions);
			if(Constants.DB_TYPE == Constants.DB_ORACLE || Constants.DB_TYPE == Constants.DB_PGSQL) {
				query.addOrdering(new ExpressionBuilder().get("publishDate").descending().nullsLast());
				query.addOrdering(new ExpressionBuilder().get("insertDate").descending().nullsLast());
			} else {
				query.addOrdering(new ExpressionBuilder().get("publishDate").descending());
				query.addOrdering(new ExpressionBuilder().get("insertDate").descending());
			}

			List<UrlRedirectBean> list = JpaDB.getResultList(manager.createQuery(query));
			manager.close();

			if(list.size() > 0) result = true;
		} catch (NoResultException e){
			return result;
		} finally{
			manager.close();
		}

		return result;
	}

	protected static String normalizeUrl(String oldUrl)
	{
		//doesn't end in an extension
		if (!oldUrl.matches("^.*[.][a-zA-Z0-9]+$"))
			oldUrl = oldUrl + "/";

		return oldUrl.replace("//", "/");
	}


	/**
	 * Vrati List podla zadaneho url (old/new)
	 * @param url
	 * @param kategoria
	 * @return
	 */
	public static List<UrlRedirectBean> search(String url, int kategoria)
	{
		return UrlRedirectDB.search(url, kategoria, null);
	}

	public static List<UrlRedirectBean> search(String url, int kategoria, boolean adminSearch)
	{
		return UrlRedirectDB.search(url, kategoria, null, adminSearch);
	}

	/**
	 * Vrati List podla zadaneho url (old/new)
	 * @param url
	 * @param kategoria
	 * @return
	 */
	public static List<UrlRedirectBean> search(String url, int kategoria, String domain) {
		return search(url, kategoria, domain, false);
	}

	public static List<UrlRedirectBean> search(String url, int kategoria, String domain, boolean adminSearch)
	{
		try
		{
			org.eclipse.persistence.expressions.Expression conditions = new ExpressionBuilder().get("urlRedirectId").greaterThan(0);
			if (url != null && kategoria > -1)
			{
				if (kategoria == OLD)
					conditions = conditions.and(new ExpressionBuilder().get("oldUrl").like('%'+url+'%'));
				else if (kategoria == NEW)
					conditions = conditions.and(new ExpressionBuilder().get("newUrl").like('%'+url+'%'));
				else if (kategoria == OLD_EXACT)
					conditions = conditions.and(new ExpressionBuilder().get("oldUrl").equal(url));
			}
			if(!adminSearch) {
				// publishDate must be in the past or null
				conditions = conditions.and(new ExpressionBuilder().get("publishDate").lessThanEqual(new Date()).or(new ExpressionBuilder().get("publishDate").isNull()));
				// validTo must be in the future or null
				conditions = conditions.and(new ExpressionBuilder().get("validTo").greaterThanEqual(new Date()).or(new ExpressionBuilder().get("validTo").isNull()));
			}

			if (isNotEmpty(domain))
			{
				if (kategoria == OLD_EXACT) conditions = conditions.and(new ExpressionBuilder().get("domainName").equal(domain));
				else conditions = conditions.and(new ExpressionBuilder().get("domainName").like('%'+domain+'%'));
			}

			ReadAllQuery query = new ReadAllQuery(UrlRedirectBean.class, conditions);
            if(Constants.DB_TYPE == Constants.DB_ORACLE || Constants.DB_TYPE == Constants.DB_PGSQL) {
                query.addOrdering(new ExpressionBuilder().get("publishDate").descending().nullsLast());
                query.addOrdering(new ExpressionBuilder().get("insertDate").descending().nullsLast());
            } else {
                query.addOrdering(new ExpressionBuilder().get("publishDate").descending());
                query.addOrdering(new ExpressionBuilder().get("insertDate").descending());
            }

			JpaEntityManager manager = JpaTools.getEclipseLinkEntityManager();
			List<UrlRedirectBean> list = JpaDB.getResultList(manager.createQuery(query));
			manager.close();
			return list;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return (null);
	}

	/**
	 * Vrati rozne domeny, ktore uz existuju v tabulke
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<String> getDistinctDomains()
	{
		return new SimpleQuery().forList("SELECT DISTINCT(domain_name) FROM url_redirect ORDER BY domain_name ASC");
	}

	public static void delete(Long id)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		UrlRedirectBean urlRedirect = em.getReference(UrlRedirectBean.class, id);
		removeRedirectFromCache(id);

		if (InitServlet.isTypeCloud() && CloudToolsForCore.getDomainName().equals(urlRedirect.getDomainName())==false) return;

		em.getTransaction().begin();
		em.remove(urlRedirect);
		em.getTransaction().commit();

		if(Tools.isNotEmpty(urlRedirect.getOldUrl()) && urlRedirect.getOldUrl().startsWith(regExpPrefix))
			Cache.getInstance().removeObject(regExpCacheKey);
	}

	private static void removeRedirectFromCache(Long id)
	{
		if (!Constants.getBoolean("cacheUrlRedirects"))
			return;

		UrlRedirectBean redirect = getById(id);
		removeRedirectFromCache(redirect);
	}


	public static void save(UrlRedirectBean urlRedirect)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		if(urlRedirect.getUrlRedirectId()==null || urlRedirect.getUrlRedirectId()<1)
		{
			urlRedirect.setInsertDate(new Date());
		}
		else
		{
			em.detach(urlRedirect);
			UrlRedirectBean oldRedirect = getById(urlRedirect.getUrlRedirectId());
			removeRedirectFromCache(oldRedirect);
		}

		try{
			em.getTransaction().begin();
			if(urlRedirect.getUrlRedirectId()!=null && urlRedirect.getUrlRedirectId() > 0)
				urlRedirect = em.merge(urlRedirect);
			else
				urlRedirect.setUrlRedirectId(0L);
			em.persist(urlRedirect);
			em.getTransaction().commit();
		}catch (Exception e) {
			em.getTransaction().rollback();
		} finally{
			em.close();
		}
		addToCache(urlRedirect);

		if(Tools.isNotEmpty(urlRedirect.getOldUrl()) && urlRedirect.getOldUrl().startsWith(regExpPrefix))
			Cache.getInstance().removeObject(regExpCacheKey);
	}

	private static void removeRedirectFromCache(UrlRedirectBean oldRedirect)
	{
		if (!Constants.getBoolean("cacheUrlRedirects"))
			return;
		String domain = firstNonNull(oldRedirect.getDomainName(), "");

		Map<String, Map<String,UrlRedirectBean>> cachedRedirects = getCachedRedirects();

		if (cachedRedirects != null) {
			Map<String, UrlRedirectBean> cacheForThisDomain = cachedRedirects.get(domain);
			cacheForThisDomain.remove(normalizeUrl(oldRedirect.getOldUrl()));
		}
	}

	private static void addToCache(UrlRedirectBean redirect)
	{
		if (!Constants.getBoolean("cacheUrlRedirects"))
			return;
		Cache.getInstance().setObject(TIMESTAMP_OF_NEXT_RUN, getDateOfNextChange().getTime(), TIMESTAMP_VALID_IN_MINUTES);
		String domain = firstNonNull(redirect.getDomainName(), "");
		if (!getCachedRedirects().containsKey(domain))
			getCachedRedirects().put(domain, new HashMap<>());

		getCachedRedirects().get(domain).put(normalizeUrl(redirect.getOldUrl()), redirect);
	}


	/**
	 * Metoda, ktora zmeni vsetky domeny s nazvom oldDomain na newDomain
	 *
	 * @param oldDomain	stary nazov domeny, ktoru chceme zmenit
	 * @param newDomain	novy nazov domeny, ktorou sa nahradia vsetky presmerovania so starou domenou oldDomain
	 */
	public static void changeDomain(String oldDomain, String newDomain)
	{
		if(oldDomain == null || newDomain == null)
			return;

		new SimpleQuery().execute("UPDATE url_redirect SET domain_name = ? WHERE domain_name = ?", newDomain, oldDomain);
		changeDomainInCache(oldDomain, newDomain);
	}


	private static void changeDomainInCache(String oldDomain, String newDomain)
	{
		if (!Constants.getBoolean("cacheUrlRedirects"))
			return;

		Map<String, UrlRedirectBean> oldRedirects = getCachedRedirects().get(oldDomain);

		//swap domain name in lists elements
		for (UrlRedirectBean b : oldRedirects.values()) {
			b.setDomainName(newDomain);
		}

		getCachedRedirects().remove(oldDomain);
		if (getCachedRedirects().get(newDomain) == null)
			getCachedRedirects().put(newDomain, new HashMap<>());
		getCachedRedirects().get(newDomain).putAll(oldRedirects);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Map<String, UrlRedirectBean>> getCachedRedirects()
	{
		ServletContext context = Constants.getServletContext();
		// domain => [old url => new url]
		Map<String, Map<String, UrlRedirectBean>> redirects = (Map<String, Map<String, UrlRedirectBean>>) context.getAttribute(CACHED_REDIRECTS);

		//context.setAttribute(CACHED_FUTURE_REDIRECTS, findFutureRedirects());
		if (redirects == null)
		{
			synchronized (UrlRedirectDB.class)
			{
				//double check
				redirects = (Map<String, Map<String, UrlRedirectBean>>) context.getAttribute(CACHED_REDIRECTS);
				if (redirects == null)
				{
					redirects = reloadCache();
				}
			}
		}
		return redirects;
	}

	@SuppressWarnings("unchecked")
	private static String regExpRedirect(String oldUrl, String domainName)
	{
		String redirectURL = null;

		try
		{
			//cache-uje sa dvojica redirect bean a jeho regexp pattern (aby sa nemusel stale vztvarat)
			List<Entry<UrlRedirectBean, Pattern>> regExps = (List<Entry<UrlRedirectBean, Pattern>>) Cache.getInstance().getObject(regExpCacheKey);
			if(regExps == null)
			{
				regExps = loadRegExps();
				Cache.getInstance().setObject(regExpCacheKey, regExps, Constants.getInt("redirectRegExpCacheMinutes"));
			}

			if(regExps != null)
			{
				for(Entry<UrlRedirectBean, Pattern> redirect : regExps)
				{
					//ak je zadane domainName, ale nenechadza sa v redirect objekte, alebo sa nerovna, tak preskocime
					if(Tools.isNotEmpty(domainName) && (Tools.isEmpty(redirect.getKey().getDomainName()) || !redirect.getKey().getDomainName().equals(domainName)))
						continue;

					Matcher m = redirect.getValue().matcher(oldUrl);
					if(m.matches())
					{
						redirectURL = redirect.getKey().getNewUrl();

						// nahrada skupin v regExp, napr.: ^\/thisiswhere\/oldfiles\/(.+) -> /thisiswhere/myfilesmovedto/$1
						for(int i=1; i<=m.groupCount(); i++)
							redirectURL = Tools.replace(redirectURL, "$"+i, m.group(i));
						break;
					}
				}
			}
		}
		catch(Exception e){sk.iway.iwcm.Logger.error(e);}

		return redirectURL;
	}

	private static List<Entry<UrlRedirectBean, Pattern>> loadRegExps()
	{
		List<Entry<UrlRedirectBean, Pattern>> regExps = new ArrayList<>();

		JpaEntityManager manager = JpaTools.getEclipseLinkEntityManager();
		try
		{
			Expression condition = new ExpressionBuilder(UrlRedirectBean.class).get("oldUrl").like(regExpPrefix+"%");
			ReadAllQuery query = new ReadAllQuery(UrlRedirectBean.class, condition);
			manager.getTransaction().begin();
			query.addDescendingOrdering("urlRedirectId");
			Query q = manager.createQuery(query);
			List<UrlRedirectBean> resultList = JpaDB.getResultList(q);

			if(resultList!=null)
			{
				for(UrlRedirectBean redirect : resultList)
				{
					if(Tools.isNotEmpty(redirect.getOldUrl()) && redirect.getOldUrl().length()>regExpPrefix.length())
					{
					    try
                        {
                            String regExpString = redirect.getOldUrl().substring(regExpPrefix.length());
                            Entry<UrlRedirectBean, Pattern> element = new SimpleEntry<>(redirect, Pattern.compile(regExpString));
                            regExps.add(element);
                        }
                        catch (Exception ex) { sk.iway.iwcm.Logger.error(ex); }
					}
				}
			}
		}catch (NoResultException e){
		}finally{
			manager.close();
		}

		return regExps;
	}

	public static Date getDateOfNextChange() {
		String sql = "";

		if(Constants.DB_TYPE == Constants.DB_ORACLE) {
			sql = "SELECT * FROM (SELECT publish_date FROM url_redirect WHERE publish_date > ? ORDER BY publish_date ASC) where ROWNUM <= 1";
		} else if (Constants.DB_TYPE == Constants.DB_MSSQL) {
			sql = "SELECT TOP 1 publish_date FROM url_redirect WHERE CAST(publish_date AS DATETIME) > CAST(? AS DATETIME) ORDER BY publish_date ASC";
		} else {
			sql = "SELECT publish_date FROM url_redirect WHERE publish_date > ? ORDER BY publish_date ASC LIMIT 1";
		}

		// musel som upravit sk.iway.iwcm.database.SimpleQuery.forDate vid koment tam
		Date date = new SimpleQuery().forDate(sql, new Date());
		if (date == null) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, 2050);
			date = cal.getTime();
		}
		return date;
	}

	public static void updateDomainName(String newDomainName, String oldDomainName) {
		new SimpleQuery().execute("UPDATE url_redirect SET domain_name=? WHERE domain_name=?");
		if (Constants.getBoolean("cacheUrlRedirects") && isReloadNecessary()) {
			// alebo pozret do cache a rovno ju editnut kedze len menim domenu
			reloadCache();
		}
	}
}
