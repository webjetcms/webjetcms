package sk.iway.iwcm.components.domainRedirects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Query;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.ReadObjectQuery;
import org.eclipse.persistence.queries.ReadQuery;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.jpa.JpaTools;



/**
 *  RedirectDB.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: Marián Halaš $
 *@version      $Revision: 1.3 $
 *@created      Date: 9.11.2010 15:49:02
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class DomainRedirectDB
{
	private static final String ALIAS_CACHE_KEY = "sk.iway.iwcm.components.domainRedirects.DomainRedirectDB.serverNameAlias";

	private static DomainRedirectDB instance;
	private static final Object classLock = DomainRedirectDB.class;
	private Map<String,DomainRedirectBean> redirTable = null;

	public static DomainRedirectDB getInstance()
	{
		return getInstance(false);
	}

	/**
	 * Ziskanie instancie
	 * @param forceRefresh - ak je nastavene na true, znova sa aktualizuju data z databazy
	 * @return
	 */
	public static DomainRedirectDB getInstance(boolean forceRefresh)
	{
		synchronized (classLock)
		{
			if (instance == null || forceRefresh)
			{
				instance = new DomainRedirectDB();
			}
			return instance;
		}
	}

	private void refreshTable()
	{
		redirTable = getRedirectTable();
	}

	public boolean containsDomain(String domain)
	{
		return redirTable.containsKey(domain);
	}

	public DomainRedirectBean getRedirect(String domain)
	{
		return redirTable.get(domain);
	}

	/**
	 * Private konstruktor
	 */
	private DomainRedirectDB()
	{
		Logger.debug(DomainRedirectDB.class, "DomainRedirectDB.constructor");
		refreshTable();

		ClusterDB.addRefresh(DomainRedirectDB.class);
	}

	public static List<DomainRedirectBean> getAllRedirects() {

		try {
			JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
			ReadAllQuery q = new ReadAllQuery(DomainRedirectBean.class);
			return JpaDB.getResultList(em.createQuery(q));
		} catch (Exception ex) {
			Logger.error(DomainRedirectDB.class, ex);
			return new ArrayList<>();
		}
	}


	public static DomainRedirectBean getRedirect(Integer id) {
		return JpaTools.getEclipseLinkEntityManager().find(DomainRedirectBean.class, id);
	}


	public static DomainRedirectBean update(DomainRedirectBean redir)
	{
		DomainRedirectBean modified =  null;
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		em.getTransaction().begin();
		modified = em.merge(redir);
		em.getTransaction().commit();
		em.close();
		getInstance(true);
		return modified;

	}


	public static void insert(DomainRedirectBean redir)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		em.getTransaction().begin();
		em.persist(redir);
		em.getTransaction().commit();
		em.close();
		getInstance(true);
	}

	public static void delete(Integer id)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		em.getTransaction().begin();
		DomainRedirectBean redir = em.find(DomainRedirectBean.class, id);
		if(redir == null)
		{
		   Logger.debug(DomainRedirectDB.class, "object for deletion, ID: "+ id + "doesn't exist!");
			//throw new IllegalArgumentException("Redirect object for deletion, ID:" + id + "doesn't exist");
		}
		else
		{
			em.remove(redir);
			em.getTransaction().commit();
			em.close();
		}
		getInstance(true);
	}

	public static boolean delete(DomainRedirectBean redir)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		em.getTransaction().begin();
		if(redir == null)
		{
			 Logger.debug(DomainRedirectDB.class, "object for deletion doesn't exist!");
			return false;
		}
		em.remove(redir);
		em.getTransaction().commit();
		em.close();
		getInstance(true);
		return true;

	}


	public static DomainRedirectBean getRedirectBySourceDomain(String sourceDomain) {
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();

		ExpressionBuilder builder = new ExpressionBuilder();
		Expression expr = builder.get("redirectFrom").equal(sourceDomain);
		ReadQuery query = new ReadObjectQuery(DomainRedirectBean.class, expr);

		em.getTransaction().begin();
		Query q = em.createQuery(query);

		return (DomainRedirectBean)q.getSingleResult();


	}

	public static List<DomainRedirectBean> getRedirectByDestDomain(String toDomain)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();

		ExpressionBuilder builder = new ExpressionBuilder();

		String domainVariants[] = new String[3];
		domainVariants[0] = toDomain;
		domainVariants[1] = "http://"+toDomain;
		domainVariants[2] = "https://"+toDomain;

		Expression expr = builder.get("redirectTo").in(domainVariants);
		expr = expr.or(builder.get("redirectTo").like(toDomain+"/%"));
		expr = expr.or(builder.get("redirectTo").like("%//"+toDomain+"/%"));

		ReadQuery query = new ReadAllQuery(DomainRedirectBean.class, expr);

		em.getTransaction().begin();
		Query q = em.createQuery(query);

		return JpaDB.getResultList(q);
	}

	/**
	 * @return Map of redirects where key is redirect_from and value is ID of redirect in DB
	 * if there are no entries in DB, returns empty map
	 */
	public static Map<String, DomainRedirectBean> getRedirectTable() {
		Map<String,DomainRedirectBean> map = new HashMap<String, DomainRedirectBean>();
		for (DomainRedirectBean tmp : getAllRedirects())
		{
			if(tmp.getActive())
			{
				if("http".equals(tmp.getProtocol()))
					map.put(addProtocol(tmp.getRedirectFrom(), false), tmp);
				else if("https".equals(tmp.getProtocol()))
					map.put(addProtocol(tmp.getRedirectFrom(), true), tmp);
				else
				{
					map.put(addProtocol(tmp.getRedirectFrom(), true), tmp);
					map.put(addProtocol(tmp.getRedirectFrom(), false), tmp);
				}
			}
		}
		return map;
	}

	/**
	 * @param domain
	 * @param path
	 * @param params
	 * @return redirected url or null when redirect not needed
	 */
	public static String translate(String domain, String path, String params, boolean isSecure)
	{
		try
		{
			DomainRedirectDB drdb = DomainRedirectDB.getInstance();

	    	if(drdb.containsDomain(addProtocol(domain, isSecure))==false) return null;

	    	DomainRedirectBean redirect = drdb.getRedirect(addProtocol(domain, isSecure));

	    	if ("alias".equals(redirect.getProtocol())) return null;

	    	if((isSecure && "http".equals(redirect.getProtocol())) || (!isSecure && "https".equals(redirect.getProtocol())))
	    		return null;

			//ak sa jedna o dmail, tak nepresmerovavam na https
			if("http".equals(redirect.getProtocol()) && Tools.isNotEmpty(params) && params.contains("&isDmail=true"))
				return null;

			Logger.debug(DomainRedirectDB.class, "redirect needed from " + domain + " to redirectId: " + redirect);

			StringBuilder result = new StringBuilder();

			if (redirect.getRedirectTo().startsWith("http")==false) {
				if ("http".equals(redirect.getProtocol())) {
					result.append("https://");
				} else {
					result.append("http://");
				}
			}
			result.append(redirect.getRedirectTo());

			if(!(path ==  null || path.equals("")))
			{
	 			if(redirect.isRedirectPath()){
	 				if(domain.charAt(domain.length() -1) != '/' && path.charAt(0) != '/')
	 					result.append('/');
	 				result.append(path);
	 			}
			}
			if(!(params ==  null || params.equals("")))
			{
	 			if(redirect.getRedirectParams()){
	 				if(params.charAt(0) != '?')
	 					result.append('?');
	 				result.append(params);
	 			}
			}
			Logger.debug(DomainRedirectDB.class, "result: " + result);

			return result.toString();
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return null;
	}

	/**
	 * vrati String "https://"+domain alebo "http://"+domain podla parametra isSecure
	 *
	 * @param domain
	 * @param isSecure
	 * @return
	 */
	private static String addProtocol(String domain, boolean isSecure)
	{
		if(Tools.isNotEmpty(domain))
		{
			if(isSecure)
				return "https://"+domain;
			else
				return "http://"+domain;
		}
		else
			return domain;
	}

	/**
	 * Vrati realnu domenu z domenoveho aliasu
	 * @param aliasedDomainName - aliasovana domena typu alias.domena.sk
	 * @return - skutocna domena nastavena adresaru vo web strankach, napr. www.realna-domena.sk
	 */
	public static String getDomainFromAlias(String aliasedDomainName) {
		try
		{
			//domenovy alias
			//chova sa to tak, ze navonok (v prehliadaci) kame domenu napr. neweb.iway.sk ale interne to WebJET vidi ako www.neweb.sk
			if (Constants.getBoolean("multiDomainEnabled"))
			{
				Cache c = Cache.getInstance();

				@SuppressWarnings("unchecked")
				HashMap<String, String> aliasesTable = (HashMap<String, String>) c.getObject(ALIAS_CACHE_KEY);

				if (aliasesTable == null)
				{
					aliasesTable = new HashMap<>();
					//aliasy su evidovane v presmerovani domen s protokolom alias
					List<DomainRedirectBean> redirects = DomainRedirectDB.getAllRedirects();
					for (DomainRedirectBean r : redirects)
					{
						if ("alias".equals(r.getProtocol()) && r.getActive() == true)
						{
							aliasesTable.put(r.getRedirectFrom(), r.getRedirectTo());
						}
					}

					c.setObject(ALIAS_CACHE_KEY, aliasesTable, 10);
				}

				if (aliasesTable.size() > 0)
				{
					String alias = aliasesTable.get(aliasedDomainName);
					if (Tools.isNotEmpty(alias)) return alias;
				}
			}
		} catch (Exception ex)
		{
			Logger.error(Tools.class, ex);
		}
		return null;
	}
}
