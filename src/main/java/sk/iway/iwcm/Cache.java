package sk.iway.iwcm;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.users.UserDetails;


/**
 *  Cache.java - cache pre casto pouzivane objekty, ktorych generovanie je pomale
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.16 $
 *@created      $Date: 2009/06/02 06:15:56 $
 *@modified     $Date: 2009/06/02 06:15:56 $
 */
public class Cache
{
	/**
	 * A list of objects which will be notified about any change in the cached objects
	 */
	private static final List<CacheListener> listeners = new Vector<>();

	/**
	 * Subscribes to the new events in Cache
	 *
	 * @param theListener
	 */
	public static void subscribe(CacheListener theListener)
	{
		listeners.add(theListener);
	}

	/**
	 * No longer willing to hear about new events in Cache
	 * @param theListener
	 */
	public static void unsubscribe(CacheListener theListener)
	{
		listeners.remove(theListener);
	}

	/**
	 * Nazov, pod ktorym sa tento objekt nachadza v ServletContexte
	 */
	public static final String CONTEXT_NAME = "sk.iway.iwcm.Cache";

	private Hashtable<String, CacheBean> objectCache; //NOSONAR

	//timestamp poslednej kontroly nepotrebnych objektov
	private long lastRemoveTime = 0;
	//pocet ms v ktorych sa bude vykonavat kontrola nepotrebnych objektov
	private long REMOVE_CHECK = 300000; //NOSONAR

	//pocet ms pre ktore sa pouzije smart refresh
	private long SMART_REFRESH_TIME = 30000; //NOSONAR

	/**
	 * Vrati instanciu Cache
	 * @return
	 */
	public static Cache getInstance()
	{
		return(getInstance(false));
	}

	/**
	 * Vrati instanciu chache
	 * @param forceRefresh - ak je true, vytvori sa nanovo (zabudne stare objekty)
	 * @return
	 */
	public static synchronized Cache getInstance(boolean forceRefresh)
	{
		//try to get it from server space
		if (forceRefresh == false)
		{
			Object o = Constants.getServletContext().getAttribute(CONTEXT_NAME);
			if (o instanceof Cache)
			{
				Cache cache = (Cache)o;
				return (cache);
			}
		}
		return (new Cache());
	}

	/**
	 * Privatny konstruktor, Cache musi byt ziskana cez getInstance()
	 */
	private Cache()
	{
		Logger.println(this,"Cache: constructor ["+Constants.getInstallName()+"]");
		objectCache = new Hashtable<>();

		REMOVE_CHECK = Constants.getInt("cacheRemoveCheckSeconds") * 1000L;
		SMART_REFRESH_TIME = Constants.getInt("cacheSmartRefreshSeconds") * 1000L;

		Constants.getServletContext().setAttribute(CONTEXT_NAME, this);
	}

	private void removeCheck()
	{
		long currentTime = System.currentTimeMillis();
		if (lastRemoveTime+REMOVE_CHECK > currentTime) return;

		lastRemoveTime = System.currentTimeMillis();

		List<String> removeNames = new ArrayList<>();
		try
		{
			Enumeration<CacheBean> e = objectCache.elements();
			while (e.hasMoreElements())
			{
				CacheBean cb = e.nextElement();
				if (cb.getExpiryTime() < currentTime)
				{
					removeNames.add(cb.getName());
				}
			}
		}
		catch (Exception ex)
		{

		}

		Iterator<String> iter = removeNames.iterator();
		while (iter.hasNext())
		{
			removeObject(iter.next());
		}

	}

	/**
	 * Vymaze celu cache
	 *
	 */
	public void clearAll()
	{
		try
		{
			Enumeration<String> e = objectCache.keys();
			while (e.hasMoreElements())
			{
				removeObject(e.nextElement());
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		objectCache = new Hashtable<>();
	}

	/**
	 * Vrati pocet poloziek v cache
	 * @return
	 */
	public int getSize()
	{
		return(objectCache.size());
	}

	/**
	 * Ziska objekt z cache, vrati null, ak sa v cache nenachadza (alebo cas exspiroval)
	 * @param name - symbolicke meno objektu v cache
	 * @return
	 */
	public Object getObject(String name)
	{
		removeCheck();

		//Logger.println(this,"Cache.getObject("+name+")");

		Object object = objectCache.get(name);
		Object returnObject = null;
		if (object != null)
		{
			CacheBean cb = (CacheBean)object;
			long currentTime = Tools.getNow();
			//Logger.debug(this, "Cache.getObject("+name+") - object found now="  + currentTime + " exp=" + cb.getExpiryTime() + " diff="+(cb.getExpiryTime() - currentTime));
			if (cb.getExpiryTime() > currentTime)
			{
				if (cb.isAllowSmartRefresh() && cb.isSmartRefreshed()==false &&
					 (cb.getExpiryTime()-(SMART_REFRESH_TIME)) < System.currentTimeMillis())
				{
					//30 sekund pred vyprsanim vratime null
					cb.setSmartRefreshed(true);

					//Logger.debug(Cache.class, "SmartRefresh: "+cb.getName());

					return null;
				}
				//Logger.debug(this, "mam v cache: " + name);
				returnObject = cb.getObject();
			}
			else
			{
				//Logger.debug(this, "Removing expired object "+name);
				//vyhod objekt z cache
				removeObject(name);
			}
		}

		return(returnObject);
	}

	/**
	 * vrati objekt uz pretypovany (nemam rad ked musim po vrateni objekt pretypovavat :) )
	 * @param name
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <R extends Object> R getObject(String name, Class<R> type)
	{
		Object result = getObject(name);
		if (result!=null && type.isAssignableFrom(result.getClass()))
		{
			return (R) result;
		}
		return null;
	}

	/**
	 * Vrati zoznam vsetkych klucov z cache
	 * @return
	 */
	public Enumeration<String> getAllKeys()
	{
		return objectCache.keys();
	}

	/**
	 * Vrati zoznam vsetkych objektov v cache
	 * @return
	 */
	public Enumeration<CacheBean> getAllElements()
	{
		return objectCache.elements();
	}

	/**
	 * Vlozi objekt do cache
	 * @param name - symbolicke meno objektu v cache
	 * @param object - objekt, ktory sa ma vlozit
	 * @param cacheInMinutes - pocet minut, pocas ktorych sa objekt bude v cache nachadzat
	 */
	public void setObject(String name, Object object, int cacheInMinutes)
	{
		setObjectSeconds(name, object, cacheInMinutes*60);
	}

	/**
	 * Vlozi objekt do cache, pouzije SMART Refresh
	 * @param name - symbolicke meno objektu v cache
	 * @param object - objekt, ktory sa ma vlozit
	 * @param cacheInSeconds - pocet sekund, pocas ktorych sa objekt uchova
	 */
	public void setObjectSeconds(String name, Object object, int cacheInSeconds)
	{
		setObjectSeconds(name, object, cacheInSeconds, true);
	}

	/**
	 * Vlozi objekt do cache
	 * @param name - symbolicke meno objektu v cache
	 * @param object - objekt, ktory sa ma vlozit
	 * @param cacheInSeconds - pocet sekund, pocas ktorych sa objekt uchova
	 * @param allowSmartRefresh - ak je nastavene na true, tak tesne pred vyprsanim objektu
	 * 			zo session je vratene null, aby dany thread mohol objekt znova naplnit (SmartCahce)
	 */
	public void setObjectSeconds(String name, Object object, int cacheInSeconds, boolean allowSmartRefresh)
	{
		long expiryTime = Tools.getNow() + (1000l * cacheInSeconds);
		setObjectByExpiry(name, object, expiryTime, allowSmartRefresh);
	}

	/**
	 * Vlozi objekt do cache s nastavenym casom exspiracie
	 * @param name
	 * @param object
	 * @param expiryTime
	 * @param allowSmartRefresh
	 */
	public void setObjectByExpiry(String name, Object object, long expiryTime, boolean allowSmartRefresh)
	{
		CacheBean cb = new CacheBean();
		cb.setName(name);
		cb.setObject(object);
		cb.setExpiryTime(expiryTime);
		cb.setAllowSmartRefresh(allowSmartRefresh);

		//Logger.debug(this,"Cache.setObject("+name+") " + Tools.formatDateTimeSeconds(Tools.getNow()) + " exp=" + Tools.formatDateTimeSeconds(expiryTime));

		//vloz ho do HashTabulky
		removeObject(name);

		objectCache.put(name, cb);

		synchronized (listeners)
		{
			for (CacheListener theListener : listeners)
			{
				try{
					theListener.objectAdded(cb);
				}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
			}
		}
	}

	/**
	 * Nastavi uz existujucemu objektu v cache novy cas exspiracie
	 * @param name
	 * @param expiryTime
	 */
	public void setObjectExpiryTime(String name, long expiryTime) {
		CacheBean cb = objectCache.get(name);
		if (cb != null) {
			cb.setExpiryTime(expiryTime);
		}
	}

	public void removeObject(String name)
	{
		removeObject(name, false);
	}

	/**
	 * Odstrani zadany objekt z cahce. Ak je refreshCluster true odstani sa aj z ostatnych nodov clustra
	 * @param name
	 * @param refreshCluster
	 */
	public void removeObject(String name, boolean refreshCluster)
	{
		//najskor refresh, lebo na tomto node nemusi existovat
		if (refreshCluster)
		{
			ClusterDB.addRefresh(DB.prepareString("sk.iway.iwcm.Cache-" + name, 250));
			Adminlog.add(Adminlog.TYPE_DATA_DELETING, "Deleting cache, key= " + name, -1, -1);
		}

		Object o = objectCache.get(name);
		if (o == null)
			return;

		CacheBean theBean = (CacheBean)o;

		synchronized (listeners)
		{
			for (CacheListener theListener : listeners)
			{
				try{
					theListener.objectRemoved(theBean);
				}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
			}
		}

		objectCache.remove(name);
	}

	/**
	 * Vymaze z cache objekty zacinajuce na dane meno
	 * @param name
	 */
	public void removeObjectStartsWithName(String name)
	{
		removeObjectStartsWithName(name, false);
	}

	/**
	 * Vymaze z cache objekty zacinajuce na dane meno. Ak je refreshCluster true odstani sa aj z ostatnych nodov clustra
	 * @param name
	 * @param refreshCluster
	 */
	public void removeObjectStartsWithName(String name, boolean refreshCluster)
	{
		List<String> removeNames = new ArrayList<>();
		try
		{
			Enumeration<CacheBean> e = objectCache.elements();
			while (e.hasMoreElements())
			{
				CacheBean cb = e.nextElement();
				if (cb.getName().startsWith(name))
				{
					removeNames.add(cb.getName());
				}
			}
		}
		catch (Exception ex)
		{

		}

		if (refreshCluster) {
			ClusterDB.addRefresh(DB.prepareString("sk.iway.iwcm.Cache:startsWithName-" + name, 250));
			Adminlog.add(Adminlog.TYPE_DATA_DELETING, "Deleting cache:startsWithName, key= " + name, -1, -1);
		}

		removeObject(name, false);

		Iterator<String> iter = removeNames.iterator();
		while (iter.hasNext())
		{
			removeObject(iter.next(), false);
		}
	}

	/**
	 * Stiahne url a ulozi ho do cache na cacheInMinutes minut. Podporuje iba GET
	 * @param url - url adresa stranky (sluzi aj ako kluc do cache)
	 * @param cacheInMinutes - pocet minut, pocas ktorych sa bude drzat v cache
	 * @return
	 */
	public String downloadUrl(String url, int cacheInMinutes)
	{
		//meno v cache
		String name = "downloadUrl."+url;

		Object o = getObject(name);
		if (cacheInMinutes>0 && o!=null && o instanceof String)
		{
			//parada, mame to v cache
			String data = (String)o;
			return(data);
		}

		//stiahni to a uloz do cache
		String data = Tools.downloadUrl(url);
		if (cacheInMinutes>0 && data != null && data.length()>0)
		{
			setObject(name, data, cacheInMinutes);
		}

		return(data);
	}

	public void onDocChange(DocDetails doc)
	{
		String cacheOnDocCahngeMode = Constants.getString("cacheOnDocCahngeMode");
		if (Tools.isEmpty(cacheOnDocCahngeMode) || cacheOnDocCahngeMode.length()<2 || "none".equalsIgnoreCase(cacheOnDocCahngeMode)) return;

		if ("all".equalsIgnoreCase(cacheOnDocCahngeMode))
		{
			try
			{
				Logger.debug(Cache.class, "onDocChange, removing ALL ");
				clearAll();
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
				Cache.getInstance(true);
			}
		}
		else if ("include".equalsIgnoreCase(cacheOnDocCahngeMode) || "groupid".equalsIgnoreCase(cacheOnDocCahngeMode))
		{
			try
			{
				List<String> objNamesToRemove = new ArrayList<>();

				//priprav zoznam
				Enumeration<String> e = objectCache.keys();
				while (e.hasMoreElements())
				{
					String objName = e.nextElement();

					if ("groupid".equalsIgnoreCase(cacheOnDocCahngeMode))
					{
						objName = objName.toLowerCase();
						if (objName.indexOf("groupid="+doc.getGroupId()+",")!=-1 ||
							 objName.indexOf("groupid="+doc.getGroupId()+" ")!=-1 ||
							 objName.indexOf("groupid='"+doc.getGroupId()+"'")!=-1 ||
							 objName.indexOf("groupid=\""+doc.getGroupId()+"\"")!=-1
						)
						{
							objNamesToRemove.add(objName);
						}
					}
					else
					{
						if (objName.indexOf("!INCLUDE")!=-1 || objName.indexOf("writeTag_")!=-1) objNamesToRemove.add(objName);
					}
				}

				//vymaz data
				for (String objName : objNamesToRemove)
				{
					CacheBean c = objectCache.get(objName);
					if (c == null) continue;

					if (c.isAllowSmartRefresh())
					{
						Logger.debug(Cache.class, "onDocChange, smart removing "+objName);
						//nastav cas pre refreshnutie na aktualny+SMART refresh-sekunda aby sa najblizsie objekt refreshol
						c.setExpiryTime(System.currentTimeMillis()+SMART_REFRESH_TIME-1000);
					}
					else
					{
						Logger.debug(Cache.class, "onDocChange, removing "+objName);
						removeObject(objName);
					}
				}
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
				Cache.getInstance(true);
			}
		}
	}

	/**
	 * Ziska timestamp exspiracie objektu z cache, vrati null, ak sa objekt v cache nenachadza (alebo cas exspiroval)
	 * @param name - symbolicke meno objektu v cache
	 * @return
	 */
	public Long getObjectExpiryTime(String name)
	{
		removeCheck();

		//Logger.println(this,"Cache.getObject("+name+")");

		Object object = objectCache.get(name);
		Long returnObject = null;
		if (object != null)
		{
			CacheBean cb = (CacheBean)object;
			//Logger.println(this,"Cache.setObject("+name+") - object found "  + Tools.getNow() + " exp=" + cb.getExpiryTime());
			if (cb.getExpiryTime() > System.currentTimeMillis())
			{
				//Logger.println(this,"mam v cache: " + name);
				returnObject = cb.getExpiryTime();
			}
			else
			{
				//vyhod objekt z cache
				removeObject(name);
			}
		}

		return(returnObject);
	}

	/**
	 * Vrati prefix klucov pre zadaneho usera
	 * @param user
	 * @return
	 */
	private String getUserPrefix(UserDetails user)
	{
		String loginName = "notLoggedUser";
		if (user != null) loginName = user.getLogin();

		return "usr."+loginName+".";
	}

	/**
	 * Vlozi objekt do cache pre daneho pouzivatela
	 * @param user - prihlaseny pouzivatel
	 * @param name - symbolicke meno objektu v cache
	 * @param object - objekt, ktory sa ma vlozit
	 * @param cacheInMinutes - pocet minut, pocas ktorych sa objekt bude v cache nachadzat
	 */
	public void setUserObject(UserDetails user, String name, Object object, int cacheInMinutes)
	{
		setObjectSeconds(getUserPrefix(user) + name, object, cacheInMinutes*60);
	}

	/**
	 * vrati objekt uz pretypovany (nemam rad ked musim po vrateni objekt pretypovavat :) )
	 * @param user - prihlaseny pouzivatel
	 * @param name
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <R extends Object> R getUserObject(UserDetails user, String name, Class<R> type)
	{
		Object result = getUserObject(user, name);
		if (result!=null && type.isAssignableFrom(result.getClass()))
		{
			return (R) result;
		}
		return null;
	}

	/**
	 * Ziska objekt z cache, vrati null, ak sa v cache nenachadza (alebo cas exspiroval)
	 * @param user - prihlaseny pouzivatel
	 * @param name - symbolicke meno objektu v cache
	 * @return
	 */
	public Object getUserObject(UserDetails user, String name)
	{
		return getObject(getUserPrefix(user)+name);
	}

	/**
	 * Odstrani z cache vsetky objekty zadaneho pouzivatela
	 * @param user
	 */
	public void removeUserAllUserObjects(UserDetails user)
	{
		if (user != null)
		{
			removeObjectStartsWithName(getUserPrefix(user), true);
		}
	}
}
