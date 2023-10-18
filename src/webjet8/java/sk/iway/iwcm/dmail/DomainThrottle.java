package sk.iway.iwcm.dmail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.persistence.jpa.JpaEntityManager;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.jpa.JpaTools;


/**
 *  DomainThrottle.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2013
 *@author       $Author: jeeff mhalas $
 *@version      $Revision: 1.3 $
 *@created      Date: 23.7.2013 16:01:40
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class DomainThrottle
{

	private SortedMap<Long, String> map ;
	private Map<String,Long> lastEmails;
	private static DomainThrottle instance = null;
	private Map<String,DomainLimitBean> domainLimits;
	private long maxTimeLimit = 0;

	private DomainThrottle(){
		super();
		init();
	}

	private void init(){
		domainLimits = Collections.emptyMap();
		maxTimeLimit = 0;
		Collection<DomainLimitBean> allLimits = DomainLimitsDB.getInstance(true).getAll();
		try {
			if(allLimits != null) {
				//domainLimits = Lambda.index(allLimits, Lambda.on(DomainLimitBean.class).getDomain());
				allLimits.forEach(domainLimit -> domainLimits.put(domainLimit.getDomain(), domainLimit));
			}
			if(!domainLimits.isEmpty()) {
				//maxTimeLimit = Lambda.max(domainLimits.values(), Lambda.on(DomainLimitBean.class).getTimeUnit().toMillis(1));
				for (DomainLimitBean limit : domainLimits.values()) {
					if (maxTimeLimit < limit.getMinDelay()) maxTimeLimit = limit.getMinDelay();
				}
			}
		} catch (Exception ex) {
			Logger.error(DomainThrottle.class, ex);
		}
		map = new TreeMap<>();
		lastEmails = new HashMap<>();
		loadMap();
	}

	/**
	 * Zinicializuje mapu Cas->domena z tabulky emailov, berie len odoslane a cas len od nahdlhsieho nastaveneho limitu
	 */
	private void loadMap()
	{
		String exceptionMessage = null;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = "SELECT recipient_email, sent_date FROM emails WHERE sent_date IS NOT NULL AND sent_date >= ? ";
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			int parameterIndex = 1;
			ps.setTimestamp(parameterIndex++, DB.getDbTimestamp(System.currentTimeMillis()-maxTimeLimit));
			rs = ps.executeQuery();
			String domain = "";
			while (rs.next())
			{
				domain = getDomainFromEmail(rs.getString("recipient_email"));
				if(Tools.isNotEmpty(domain))
				{
					map.put(DB.getDbTimestamp(rs, "sent_date"), domain);
					if(domainLimits.containsKey(domain))
						lastEmails.put(domain, DB.getDbTimestamp(rs, "sent_date"));
				}
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			exceptionMessage = ex.getMessage();
			IllegalStateException exception = new IllegalStateException(exceptionMessage);
			exception.initCause(ex);
			throw exception;
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

	}

	/**
	 * dummy impl of getting domain from email address
	 * @param string
	 * @return
	 */
	public static String getDomainFromEmail(String address)
	{
		String domain = "";
		if(address.indexOf("@")!=-1)
		{
			domain = address.substring(address.indexOf("@")+1);
		}
		return domain;
	}

	public static DomainThrottle getInstance(){
		DomainThrottle throttle = instance;
		if(throttle == null)
		{
			synchronized(DomainThrottle.class)
			{
				throttle = instance;
				if(throttle == null)
					instance = throttle = new DomainThrottle();
			}
		}
		return throttle;
	}

	/**
	 * Zisti ci je mozne na zaklade nastaveny limitov poslat email na danu domenu
	 * najpr sa kontroluje ci ma domena nejaky limit, ak nie poslanie sa hned povoli
	 * ak ano, najprv sa skontroluje ci posledny email na danu domenu neodisiel uz davnejsie ako je
	 * minimalne delay pre danu domenu, ak ano tak sa poslanie povoli, ak nie
	 * tak sa spocita pocet emailov na danu domenu za cas od teraz po casovy limit a ak nie je limit prekroceny
	 * tak sa odoslanie povoli.
	 * @param domain domena
	 * @return
	 */
	public synchronized boolean canSend(String domain)
	{
		boolean canSend = true;

		if(domainLimits.get(domain)==null && domainLimits.get("*")!=null)
		{
			cloneGenericLimit(domain);
		}
		DomainLimitBean domainLimit=domainLimits.get(domain);
		if(domainLimit!=null)
		{
			long now = System.currentTimeMillis();
			//minimalny delay
			if(domainLimit.isDelayActive())
			{
				if(lastEmails.containsKey(domain))
				{
					long lastEmailTime = lastEmails.get(domain);
					if(lastEmailTime <= (now-domainLimit.getMinDelay())) canSend = true;
					else {
						canSend = false;
						Logger.debug(getClass(), "Domain: "+domain +" -> Min delay not reached yet");
					}
				}
			}
			//este skontrolujeme limit poctu mailov za casovu jednotku
			if(domainLimit.isActive() && canSend)
			{
				//Logger.debug(getClass(), "getting count of emails sent to "+ domain + " from: "+(now-domainLimit.getTimeUnit().toMillis(1)) + " to: "+now);
				int sentEmailsCount = countEmailsSentToDomain(now-domainLimit.getTimeUnit().toMillis(1), now, domain);
				if(sentEmailsCount < domainLimit.getLimit()) canSend = true;
				else{
					canSend = false;
					//Logger.debug(getClass(), "Domain: "+domain +" -> Limit over time crossed");
				}
			}
		}
		Logger.debug(getClass(), "Domain: "+domain +" Can send: "+canSend);
		return canSend;
	}

	/**
	 * Spravi kopiu generickeho limitu pre domenu ktora nema explicitne definovane limity
	 * tuto kopiu ale neulozi do DB, prepocita maxTimeLimit a oshapuje mapu emailov ak treba
	 * @param domain domena
	 */
	protected void cloneGenericLimit(String domain)
	{
		Logger.debug(getClass(), "No limit defined for domain " + domain + " , generic limit enabled, copying...");
		DomainLimitBean matrix = domainLimits.get("*");
		JpaEntityManager manager = JpaTools.getEclipseLinkEntityManager();
		manager.detach(matrix);
		matrix.setDomain(domain);
		matrix.setId(0);
		domainLimits.put(domain, matrix);
		Logger.debug(getClass(), "Limit for domain: " + domain + " added. Limit: " +matrix);
		Logger.debug(getClass(), "Recalculating maxTimeLimit...");
		//maxTimeLimit = Lambda.max(domainLimits.values(), Lambda.on(DomainLimitBean.class).getTimeUnit().toMillis(1));
		maxTimeLimit = 0;
		for (DomainLimitBean limit : domainLimits.values()) {
			if (maxTimeLimit < limit.getMinDelay()) maxTimeLimit = limit.getMinDelay();
		}
		Logger.debug(getClass(), "maxTimeLimit: " + maxTimeLimit);
		long currentTime = System.currentTimeMillis();
		if(!map.isEmpty())
		{
			long from = currentTime - maxTimeLimit;
			long to  = currentTime;
			if(from < this.map.firstKey()) from = this.map.firstKey();
			if(to > this.map.lastKey()) to = this.map.lastKey();
			if(from <= to ) map = new TreeMap<>(map.subMap(from, to));
		}
	}

	/**
	 * toto by malo zistit kolko mailov bolo na danu domenu poslanych v danom rozsahu casov
	 * snad to bude dostatocne rychle
	 * otazka concurent modification?
	 * @param from
	 * @param to
	 * @param domain
	 * @return
	 */
	private int countEmailsSentToDomain(long from, long to, String domain)
	{
		if(this.map.isEmpty()) return 0;
		if(from < this.map.firstKey()) from = this.map.firstKey();
		if(to > this.map.lastKey()) to = this.map.lastKey();
		if(from > to){
			Logger.debug(getClass(), "from key > to, returning 0");
			return 0;
		}
		SortedMap<Long,String> subMap = this.map.subMap(from, to);
		int count = 0;
		for(String d : subMap.values())
		{
			if(d.equals(domain)) count++;
		}
		return count;
	}

	/**
	 * This method will reload domain limits settings
	 * recalculate longest limit and shape domain records
	 * according to longest limit (keep records only from longest limit to present)
	 */
	public synchronized void refresh()
	{
		Collection<DomainLimitBean> allLimits = DomainLimitsDB.getInstance(true).getAll();
		try {
			if(allLimits != null) {
				//domainLimits = Lambda.index(allLimits, Lambda.on(DomainLimitBean.class).getDomain());
				domainLimits = Collections.emptyMap();
				allLimits.forEach(limit -> domainLimits.put(limit.getDomain(), limit));
			}
			if(!domainLimits.isEmpty()) {
				//maxTimeLimit = Lambda.max(domainLimits.values(), Lambda.on(DomainLimitBean.class).getTimeUnit().toMillis(1));
				maxTimeLimit = 0;
				for (DomainLimitBean limit : domainLimits.values()) {
					if (maxTimeLimit < limit.getMinDelay()) maxTimeLimit = limit.getMinDelay();
				}
			}
			long currentTime = System.currentTimeMillis();
			if(!map.isEmpty())
			{
				long from = currentTime - maxTimeLimit;
				long to  = currentTime;
				if(from < this.map.firstKey()) from = this.map.firstKey();
				if(to > this.map.lastKey()) to = this.map.lastKey();
				map = new TreeMap<>(map.subMap(from, to));
			}
		} catch (Exception e) {
			Logger.error(DomainThrottle.class, e);

			map = new TreeMap<>();
			lastEmails = new HashMap<>();
			loadMap();
		}

	}

	/**
	 * Prida odoslany email do tabuliek
	 * @param domain domena
	 * @param timeSent cas odoslania v milisekundach
	 */
	public synchronized void  addEmail(String domain, long timeSent)
	{
		lastEmails.put(domain, timeSent);
		map.put(timeSent, domain);
	}
}
