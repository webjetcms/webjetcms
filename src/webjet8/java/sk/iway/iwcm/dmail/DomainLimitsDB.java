package sk.iway.iwcm.dmail;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import sk.iway.iwcm.database.JpaDB;

/**
 *  DomainLimitsDB.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2013
 *@author       $Author: jeeff mhalas $
 *@version      $Revision: 1.3 $
 *@created      Date: 24.7.2013 15:25:23
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class DomainLimitsDB extends JpaDB<DomainLimitBean>
{

	private static DomainLimitsDB instance = null;
	private static final Object classLock = DomainLimitsDB.class;

	private static ConcurrentMap<String,DomainLimitBean> domainLimits = new ConcurrentHashMap<>();

	public static DomainLimitsDB getInstance()
	{
		return getInstance(false);
	}

	public static DomainLimitsDB getInstance(boolean forceRefresh)
	{
		//jeeff: aby sme zbytocne nemali synchronized blok
		if (forceRefresh==false && instance != null) return instance;

		synchronized (classLock)
		{
			if (instance == null || forceRefresh)
			{
				instance = new DomainLimitsDB();
				Collection<DomainLimitBean> allLimits = instance.getAll();
				if(allLimits != null) {
					//domainLimits = Lambda.index(allLimits, Lambda.on(DomainLimitBean.class).getDomain());
					domainLimits = new ConcurrentHashMap<>();
					allLimits.forEach(domainLimit -> domainLimits.put(domainLimit.getDomain(), domainLimit));
				}

			}
			return instance;
		}
	}
	public static void refresh()
	{
		instance=null;
	}

	private DomainLimitsDB()
	{
		super(DomainLimitBean.class);
	}

	/**
	 * Skontroluje ci existuje pre danu domenu nejaky limit
	 * @param domain domena
	 * @return
	 */
	public boolean existsDomainLimit(String domain)
	{
		return domainLimits.containsKey(domain);
	}

}
