package sk.iway.iwcm.system.cluster;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;

/**
 *  ClusterDB.java - objekt pre podporu clustra
 *  riesi primarne refreshovanie instancii objektov na jednotlivych nodoch
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.5 $
 *@created      Date: 17.11.2008 16:24:56
 *@modified     $Date: 2010/01/28 15:40:03 $
 */
public class ClusterDB
{
	protected ClusterDB() {
		//utility class
	}

	/**
	 * Poznaci do databazy informaciu o refreshi objektu (=ziskanie getInstance())
	 * @param clas
	 */
	public static void addRefresh(Class<?> clas)
	{
		addRefresh(clas, (Long)null);
	}

	public static void addRefresh(Class<?> clas, Long id)
	{
		String className = clas.getName();
		addRefresh(className, id);
	}

	/**
	 * Poznaci do databazy informaciu o refreshi objektu (=ziskanie getInstance())
	 * @param className
	 */
	public static void addRefresh(String className)
	{
		addRefresh(className, (Long)null);
	}

	/**
	 * Poznaci do databazy informaciu o refreshi objektu/zaznamu v DB
	 * @param className - trieda
	 * @param id - id zaznamu alebo NULL pre refresh celej triedy
	 */
	public static void addRefresh(String className, Long id)
	{
		//kontrola thread name na refresher thread
		Logger.debug(ClusterDB.class, "thread: " + Thread.currentThread().getName());
		if (ClusterRefresher.THREAD_NAME.equals(Thread.currentThread().getName())) return;

		String clusterMyNodeName = Constants.getString("clusterMyNodeName");
		String clusterNames = Constants.getString("clusterNames");
		if (Tools.isEmpty(clusterNames) || Tools.isEmpty(clusterMyNodeName)) return;

		try
		{
			Timestamp t = new Timestamp(Tools.getNow());

			SimpleQuery query = new SimpleQuery();

			String idAppend = "";
			if (id != null) idAppend = "-"+id;

			for (String nodeName : getClusterNodeNames())
			{
				if (nodeName.equalsIgnoreCase(clusterMyNodeName)) continue;

				query.execute("INSERT INTO cluster_refresher (node_name, class_name, refresh_time) VALUES (?, ?, ?)",
						nodeName, className+idAppend, t
				);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	public static void addRefreshClusterMonitoring(String nodeName, Class<?> clas) {
		//Before refresh delete all data's

		//delete cluster monitoring data
		new SimpleQuery().execute("DELETE FROM cluster_monitoring WHERE node = ?", nodeName);

		//ask for refresh
		String className = clas.getName();
		SimpleQuery query = new SimpleQuery();
		query.execute("INSERT INTO cluster_refresher (node_name, class_name, refresh_time) VALUES (?, ?, ?)",
					nodeName, className, new Timestamp(Tools.getNow())
		);
	}

	/**
	 * Vymaze info o refreshoch pre aktualny objekt po inicializacii systemu
	 */
	public static void cleanup()
	{
		try
		{
			String clusterMyNodeName = Constants.getString("clusterMyNodeName");
			if (Tools.isEmpty(clusterMyNodeName)) return;

			new SimpleQuery().execute("DELETE FROM cluster_refresher WHERE node_name=?", clusterMyNodeName);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	public static boolean isServerRunningInClusterMode()
	{
		if(Tools.isEmpty(Constants.getString("clusterMyNodeName")))
			return false;
		String clusterNames = Constants.getString("clusterNames");
		if (Tools.isEmpty(clusterNames))
			return false;

		if ("auto".equalsIgnoreCase(clusterNames)) return true;

		return getClusterNodeNames().size() > 1;
	}

	public static List<String> getClusterNodeNames()
	{
		String clusterNames = Constants.getString("clusterNames");

		String[] nodesAsArray = Tools.getTokens(clusterNames, ",");
		return new ArrayList<>(Arrays.asList(nodesAsArray));
	}

	/**
	 * Vrati zoznam nodov expandnutych pre rezim auto (napr. pre vyber nodu v monitoringu)
	 * expand sa robi distinct selectom z databazy za posledny mesiac
	 * @return
	 */
	public static List<String> getClusterNodeNamesExpandedAuto()
	{
		return getClusterNodeNamesExpandedAuto(0);
	}

	public static List<String> getClusterNodeNamesExpandedAuto(long dateFrom)
	{
		String clusterNames = Constants.getString("clusterNames");

		if ("auto".equalsIgnoreCase(clusterNames))
		{
			if (dateFrom < 1)
			{
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.MONTH, -1);
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				dateFrom = cal.getTimeInMillis();
			}

			//Logger.debug(ClusterDB.class, "Getting cluster node names from date "+Tools.formatDateTimeSeconds(dateFrom));

			Cache c = Cache.getInstance();
			String CACHE_KEY = "ClusterDB.autoNames-"+dateFrom;
			@SuppressWarnings("unchecked")
			List<String> names = (List<String>)c.getObject(CACHE_KEY);
			if (names != null) return names;

			names = new SimpleQuery().forListString("SELECT DISTINCT node_name FROM monitoring WHERE date_insert>?", new Timestamp(dateFrom));

			List<String> currentValues = new SimpleQuery().forListString("SELECT DISTINCT cluster_node FROM crontab");
			for (String node : currentValues) {
				if ("all".equals(node) || Tools.isEmpty(node)) continue;
				if (names.contains(node)==false) names.add(node);
			}

			Collections.sort(names);

			c.setObjectSeconds(CACHE_KEY, names, 5*60, true);
			return names;
		}

		String[] nodesAsArray = Tools.getTokens(clusterNames, ",");
		return new ArrayList<>(Arrays.asList(nodesAsArray));
	}
}
