package sk.iway.iwcm.system.cluster;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.persistence.jpa.JpaEntityManager;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.tags.CombineTag;


/**
 *  ClusterRefresher.java
 *  objekt pravidelne kontrolujuci databazu clustra pre obnovu dat
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.5 $
 *@created      Date: 17.11.2008 16:50:16
 *@modified     $Date: 2010/01/11 08:15:01 $
 */
public class ClusterRefresher extends TimerTask
{
	private Timer timer;
	public static final String THREAD_NAME = "ClusterRefresherThread";

	//pouzivane v rezime auto na identifikaciu poslednej hodnoty z DB tabulky ktoru mame precitanu
	private static int lastExecutedAutoId = -1;

	/**
	 * Konstruktor, ak je nastavene clusterNames tak inicializuje Timer task
	 */
	public ClusterRefresher()
	{
		String clusterNames = Constants.getString("clusterNames");
		if (Tools.isEmpty(clusterNames)) return;
		String clusterMyNodeName = Constants.getString("clusterMyNodeName");

		if ("auto".equals(Constants.getString("clusterNames")))
		{
			//inicializuj aktualne poslednu hodnotu z DB tabulky pre auto aktualizaciu nodov
			setLastExecutedAutoId(new SimpleQuery().forInt("SELECT MAX(cluster_refresh_id) FROM cluster_refresher WHERE (node_name=? OR node_name=?) ", "auto", clusterMyNodeName));
		}

		timer = new Timer(true);
		timer.schedule(this, 5000, Constants.getInt("clusterRefreshTimeout"));
	}

	/**
	 * Kontrola aktualizacii objektov
	 */
	@Override
	public void run()
	{
		try
		{
			String clusterMyNodeName = Constants.getString("clusterMyNodeName");
			if (Tools.isEmpty(clusterMyNodeName)) return;

			if ("auto".equals(Constants.getString("clusterNames")))
			{
				readFromAutoMode(clusterMyNodeName);
			}
			else
			{
				readFromDB(clusterMyNodeName);
			}

			try
			{
				JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
				if(em != null)
				{
					em.getEntityManagerFactory().getCache().evictAll();
				}
			}
			catch (Exception ex)
			{
				Adminlog.add(Adminlog.TYPE_CRON, "ClusterRefresher.run ERROR JPA " + ex.getMessage()+"\n\n"+Logger.getStackTrace(ex), -1, -1);
				sk.iway.iwcm.Logger.error(ex);
			}

			//zmaz stare zaznamy nebeziacich nodov
			cleanOldStatusAllNodes();
		}
		catch (Exception ex)
		{
			Adminlog.add(Adminlog.TYPE_CRON, "ClusterRefresher.run ERROR " + ex.getMessage()+"\n\n"+Logger.getStackTrace(ex), -1, -1);
			Logger.error(ClusterRefresher.class, ex);
		}
	}

	/**
	 * Precitanie zaznamov z DB pre moj node (ak mame presne zadane mena nodov)
	 * @param clusterMyNodeName
	 */
	private void readFromDB(String clusterMyNodeName)
	{
		try
		{
			List<String> updateClassNames = new SimpleQuery().forListString("SELECT class_name FROM cluster_refresher WHERE node_name=? ORDER BY cluster_refresh_id ASC", clusterMyNodeName);

			Thread.currentThread().setName(THREAD_NAME);
			Set<String> allreadyExecuted = new HashSet<>();
			for (String className : updateClassNames)
			{
				if (allreadyExecuted.contains(className)) continue;

				refreshObject(clusterMyNodeName, className);
				allreadyExecuted.add(className);
			}
		}
		catch (IllegalStateException ex)
		{
			Adminlog.add(Adminlog.TYPE_CRON, "ClusterRefresher.run ERROR 1 " + ex.getMessage()+"\n\n"+Logger.getStackTrace(ex), -1, -1);
			Logger.error(ClusterRefresher.class, ex);
		}
		catch (Exception ex)
		{
			Adminlog.add(Adminlog.TYPE_CRON, "ClusterRefresher.run ERROR 2 " + ex.getMessage()+"\n\n"+Logger.getStackTrace(ex), -1, -1);
			Logger.error(ClusterRefresher.class, ex);
		}
	}

	/**
	 * Precitanie zaznamov z DB ak mame rezim auto, cize kazdy nod si sam pamata co ma aktualizovat (cita len nove zaznamy z DB)
	 * @param clusterMyNodeName
	 */
	private void readFromAutoMode(String clusterMyNodeName)
	{
		try
		{
			//we need myNodeName to read monitoring refresher data
			int actualMax = new SimpleQuery().forInt("SELECT MAX(cluster_refresh_id) FROM cluster_refresher WHERE (node_name=? OR node_name=?) AND cluster_refresh_id>?", "auto", clusterMyNodeName, getLastExecutedAutoId());
			//getLastExecutedAutoId()>0 - ak je to prvy beh, tak nechceme robit nic, su tam zapisane data z tohto nodu
			if (actualMax > 0 && getLastExecutedAutoId()>=0)
			{
				List<String> updateClassNames = new SimpleQuery().forListString("SELECT class_name FROM cluster_refresher WHERE (node_name=? OR node_name=?)  AND cluster_refresh_id>? AND cluster_refresh_id<=? ORDER BY cluster_refresh_id ASC", "auto", clusterMyNodeName, getLastExecutedAutoId(), actualMax);

				Thread.currentThread().setName(THREAD_NAME);
				Set<String> allreadyExecuted = new HashSet<>();
				for (String className : updateClassNames)
				{
					if (allreadyExecuted.contains(className)) continue;

					refreshObject(clusterMyNodeName, className);
					allreadyExecuted.add(className);
				}

				Logger.debug(ClusterRefresher.class, "readFromAutoMode, actualMax="+actualMax+" lastExecutedAutoId="+getLastExecutedAutoId());
			}

			if (actualMax > 0) setLastExecutedAutoId(actualMax);
		}
		catch (Exception ex)
		{
			Adminlog.add(Adminlog.TYPE_CRON, "ClusterRefresher.run ERROR " + ex.getMessage()+"\n\n"+Logger.getStackTrace(ex), -1, -1);
			Logger.error(ClusterRefresher.class, ex);
		}
	}

	/**
	 * Vykona refresh DB objektu volanim getInstance(true)
	 * @param nodeName
	 * @param className
	 */
	private void refreshObject(String nodeName, String className)
	{
		try
		{
			Logger.debug(ClusterRefresher.class, "invoking: " + className);

			long now = Tools.getNow();

			if (className.startsWith("sk.iway.iwcm.doc.DocDB-"))
			{
				//je to ciastkovy update DocDB, musime zavolat
				int docId = Tools.getIntValue(className.substring(className.indexOf('-')+1), -1);
				if (docId > 0)
				{
					DocDB.getInstance().updateInternalCaches(docId);
				}
			}
         	else if (className.startsWith("sk.iway.iwcm.system.ConfDB-"))
			{
				//je to ciastkovy update ConfDB
				String name = className.substring(className.indexOf('-')+1);
				if (Tools.isNotEmpty(name))
				{
                    ConfDB.refreshVariable(name);
				}
			}
         	else if (className.startsWith("sk.iway.iwcm.Cache-"))
			{
				//je to ciastkovy update Cache
				String name = className.substring(className.indexOf('-')+1);
				if (Tools.isNotEmpty(name))
				{
					if ("delAll".equals(name)) {
						Cache.getInstance().clearAll();
						DB.resetHtmlAllowedFields();
					}
					else Cache.getInstance().removeObject(name, false);
				}
			}
			else if (className.startsWith("sk.iway.iwcm.Cache:startsWithName-"))
			{
				//je to ciastkovy update Cache
				String name = className.substring(className.indexOf('-')+1);
				Cache.getInstance().removeObjectStartsWithName(name, false);
			}
			else if (className.startsWith("sk.iway.iwcm.tags.CombineTag-"))
			{
				//aktualizacia timestampu
				long timestamp = Tools.getLongValue(className.substring(className.indexOf('-')+1), Tools.getNow());
				//zmen version tag
				CombineTag.setVersion(timestamp);
			}
			else if (className.startsWith("sk.iway.iwcm.stat.SessionHolder-")) {
				//If sessionId is set, we want do invalid only this sessionId
				String sessionId = className.substring(className.indexOf('-') + 1);
				sessionId = sessionId.substring(0, sessionId.length() - 2); //Remove postfix -0
				SessionHolder.getInstance().invalidateSession(sessionId);
			}
			else
			{
				String classNameFixed = className;
				String methodName = "getInstance";
				Class<?>[] parameterTypes = new Class[] {boolean.class};
				Object[] arguments = new Object[] {true};

				if (className.contains("-"))
				{
					//je to ciastkovy update objektu, na to mame speci metodu g
					classNameFixed = className.substring(0, className.indexOf('-'));
					long id = Tools.getLongValue(className.substring(className.indexOf('-')+1), 1);
					methodName = "refresh";
					parameterTypes = new Class[] {long.class};
					arguments = new Object[] {id};
				}

				Class<?> c = Class.forName(classNameFixed);
				Method m = c.getMethod(methodName, parameterTypes);

				now = Tools.getNow();
				m.invoke(c, arguments);

				Cache.getInstance().removeObjectStartsWithName(classNameFixed, false);
			}

			cleanClassStatus(nodeName, className, now);
		}
		catch (Exception ex)
		{
			Adminlog.add(Adminlog.TYPE_CRON, "Error invoking "+className+" error: " + ex.getMessage()  +"\n\n"+Logger.getStackTrace(ex), -1, -1);
			Logger.error(ClusterRefresher.class, ex);
		}
	}

	/**
	 * Po spravnom refreshe databazy vymaze zaznam z DB
	 * @param nodeName
	 * @param className
	 * @param now - datum vykonania refreshu
	 */
	private void cleanClassStatus(String nodeName, String className, long now)
	{
		new SimpleQuery().execute("DELETE FROM cluster_refresher WHERE node_name=? AND class_name=? AND refresh_time<=?",
				nodeName, className, new Timestamp(now));
	}

	/**
	 * Zmaze stare zaznamy (starsie ako 5 hodin) ktore sa kopia kvoli nebeziacim nodom (napr. pasive, alebo aktualne vypnutym) aby sa nenafukovala DB
	 */
	private void cleanOldStatusAllNodes()
	{
		Calendar cal = Calendar.getInstance();
		if ("auto".equals(Constants.getString("clusterNames")))
		{
			//v auto mode ponechavame udalosti len za poslednych 30 minut (nema zmysel viac)
			cal.add(Calendar.MINUTE, -30);
		}
		else
		{
			cal.add(Calendar.MINUTE, -5*60);
		}

		new SimpleQuery().execute("DELETE FROM cluster_refresher WHERE refresh_time<=?", new Timestamp(cal.getTimeInMillis()));

		//zmas aj stare konf. premenne statXXX
		if ("auto".equals(Constants.getString("clusterNames"))) {
			new SimpleQuery().execute("DELETE FROM "+ConfDB.CONF_TABLE_NAME+" WHERE name like ? AND date_changed<=?", "statDistinctUsers-%", new Timestamp(cal.getTimeInMillis()));
			new SimpleQuery().execute("DELETE FROM "+ConfDB.CONF_TABLE_NAME+" WHERE name like ? AND date_changed<=?", "statSessions-%", new Timestamp(cal.getTimeInMillis()));
		}
	}

	public void cancelTask()
	{
		Logger.println(ClusterRefresher.class, "destroying cluster refresher");
		if (timer != null) timer.cancel();
		this.cancel();
	}

	private static int getLastExecutedAutoId() {
		return lastExecutedAutoId;
	}

	private static void setLastExecutedAutoId(int lastExecutedAutoId) {
		ClusterRefresher.lastExecutedAutoId = lastExecutedAutoId;
	}


}
