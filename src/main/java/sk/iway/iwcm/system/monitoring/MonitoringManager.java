package sk.iway.iwcm.system.monitoring;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.dbpool.ConfigurableDataSource;

/**
 * MonitoringManager.java - trieda sluziaca na pracu v module Monitoring servera, metody na pracu s databazou
 *	@Title        webjet4
 *	@Company      Interway s.r.o. (www.interway.sk)
 *	@Copyright    Interway s.r.o. (c) 2001-2008
 *	@author       $Author: jeeff $
 *	@version      $Revision: 1.4 $
 *	@created      Date: 11.06.2009 10:52:51
 *	@modified     $Date: 2009/11/20 12:40:57 $
 */

public class MonitoringManager
{
	/**
	 * Metoda volana z crontabu kazdych 30s, zapisuje do tabulky monitoring jednotlive hodnoty stavu servera.
	 * Musi byt vsak povolena v konstante serverMonitoringEnable, ktora je prednastavena na false.
	 *
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
			if (Constants.getBoolean("serverMonitoringEnable"))
			{
				if (MonitoringManager.saveSaveCurrentServerParameter())
				{
					//Logger.debug(MonitoringManager.class,"MonitoringManager.saveSaveCurrentServerParameter() - saving successful");
				}
				else
					Logger.debug(MonitoringManager.class,"MonitoringManager.saveSaveCurrentServerParameter() - db error");
			}

			if(ClusterDB.isServerRunningInClusterMode() && "none".equals(Constants.getString("statMode"))==false)
			{
				countUsersOnAllNodes();
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	private static void countUsersOnAllNodes()
	{
		//if the public node doesn't perms to write to _conf_ table, then don't count users
		if (Constants.getBoolean("monitoringEnableCountUsersOnAllNodes")==false) {
			return;
		}

		String myClusterName = Constants.getString("clusterMyNodeName");
		ConfDB.setName("statSessions-"+myClusterName, Integer.toString(SessionHolder.getTotalSessionsPerNode()));
		ConfDB.setName("statDistinctUsers-"+myClusterName, Integer.toString(SessionHolder.getDistinctUsersCountPerNode()));
		int totalDistinctUsers=0;
		int totalSessions=0;
		for(String clusterName : ClusterDB.getClusterNodeNamesExpandedAuto())
		{
			ConfDetails cd = ConfDB.getVariable("statDistinctUsers-"+clusterName);
			if(cd!=null)
				totalDistinctUsers = totalDistinctUsers + Tools.getIntValue(cd.getValue(), 0);
			cd = ConfDB.getVariable("statSessions-"+clusterName);
			if(cd!=null)
				totalSessions = totalSessions + Tools.getIntValue(cd.getValue(), 0);
		}
		Constants.setInt("statSessionsAllNodes", totalSessions);
		Constants.setInt("statDistinctUsersAllNodes", totalDistinctUsers);
	}

	/**
	 * Funkcia, ktora zapise do databazy jednotlive aktualne hodnoty servera zo stranky admin/mem.jsp
	 * @return					vrati true, ak zapis prebehol uspesne, inak false
	 */
	public static boolean saveSaveCurrentServerParameter()
	{
		int numActive = 0;
		int numIdle = 0;

		try
		{
			ConfigurableDataSource ds = (ConfigurableDataSource)DBPool.getInstance().getDataSource("iwcm");
			numActive = ds.getNumActive();
			numIdle = ds.getNumIdle();
		}
		catch (Exception ex)
		{
		    sk.iway.iwcm.Logger.error(ex);
		}

		try{

			Runtime rt = Runtime.getRuntime();

			String sql = "INSERT INTO monitoring (date_insert, node_name, db_active, db_idle, mem_free, mem_total, cache, sessions, cpu_usage, process_usage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			long free = rt.freeMemory();
			long total = rt.totalMemory();
			CpuInfo cpu = new CpuInfo();	//ak nie je monitoring cpu aktivny, nevytvorim ani instanciu triedy a do databazy zapisujem 0.0
			int cpuUsage = cpu.getCpuUsage();
			int cpuUsageProcess = cpu.getCpuUsageProcess();
			Logger.debug(null, "Vysledna cpuUsage: "+ cpuUsage);
			new SimpleQuery().execute(sql, new Timestamp(Tools.getNow()), Constants.getString("clusterMyNodeName"),
				numActive, numIdle, free, total, Cache.getInstance().getSize(), SessionHolder.getTotalSessionsPerNode(), cpuUsage, cpuUsageProcess);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
 	 * Vyfiltruje a vrati maximalne max_rows zaznamov monitorovanych hodnot z tabulky monitoring vyfiltrovane podla zadanych datumov a nazvu clustra.
 	 *
 	 * @param filterDateFrom 	od ktoreho datumu sa maju vyselektovat rezervacie
 	 * @param filterDateTo 		do ktoreho datumu sa maju vyselektovat rezervacie
 	 * @param filterNodeName	nazov clustra, pre ktory boli zaznamenane hodnoty
 	 *
 	 * @return ArrayList 		naplneny Beanmi so zaznamenanymi monitorovacimi informaciami, ktore splnaju podmienky udane vstupnymi parametrami
 	 */
	public static List<MonitoringBean> getMonitoringStats(Date filterDateFrom, Date filterDateTo, String filterNodeName)
	{
		List<MonitoringBean> monitoringStats = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		StringBuilder sql = new StringBuilder("SELECT * FROM monitoring WHERE monitoring_id > 0 ");
		if (filterDateFrom != null)
			sql.append("AND date_insert >= ? ");
		if (filterDateTo != null)
			sql.append("AND date_insert <= ? ");
		if (Tools.isNotEmpty(filterNodeName))
			sql.append("AND node_name = ?");

		sql.append("ORDER BY date_insert DESC");

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql.toString());

			int psIndex = 1;

			if (filterDateFrom != null)
				ps.setTimestamp(psIndex++, new Timestamp(filterDateFrom.getTime()));
			if (filterDateTo != null)
				ps.setTimestamp(psIndex++, new Timestamp(filterDateTo.getTime()));
			if (Tools.isNotEmpty(filterNodeName))
				ps.setString(psIndex++, filterNodeName);

			rs = ps.executeQuery();

			while (rs.next())
			{
				MonitoringBean monitoringStat = new MonitoringBean();

				monitoringStat.setMonitoringId(rs.getInt("monitoring_id"));
				monitoringStat.setDateInsert(rs.getTimestamp("date_insert"));
				monitoringStat.setNodeName(rs.getString("node_name"));

				monitoringStat.setDbActive(rs.getInt("db_active"));
				monitoringStat.setDbIdle(rs.getInt("db_idle"));
				monitoringStat.setMemFree(rs.getBigDecimal("mem_free").longValue());
				monitoringStat.setMemTotal(rs.getBigDecimal("mem_total").longValue());

				monitoringStat.setCache(rs.getInt("cache"));
				monitoringStat.setSessions(rs.getInt("sessions"));

				monitoringStat.setCpuUsage(rs.getDouble("cpu_usage"));
				monitoringStat.setProcessUsage(rs.getDouble("process_usage"));
				monitoringStats.add(monitoringStat);

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
			sk.iway.iwcm.Logger.error(ex);
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
			}
		}

		return monitoringStats;
	}

	/**
 	 * Vymaze zaznam monitorovanych hodnot z tabulky monitoring
 	 *
 	 * @param monitoringId - identifikacne cislo ulozeneho zaznamu, ktory chceme vymazat
 	 *
 	 * @return true ak vymazanie z databazy prebehlo v poriadku, inak false
 	 */
	public static boolean deleteMonitoringStat(int monitoringId)
	{
		try{
			new SimpleQuery().execute("DELETE FROM monitoring WHERE monitoring_id = ?", monitoringId);
			return true;
		}catch (Exception e) {
			return false;
		}
	}

	/**
 	 * Vyfiltruje a vrati rozne nazvy uzlov clustera z tabulky monitoring
 	 *
 	 * @return List naplneny roznymi nazvami uzlov clustera, ktore su zapisane v tabulke monitoring
 	 */
	public static List<String> getDistinctNodeNames()
	{
		return new SimpleQuery().forListString("SELECT DISTINCT node_name FROM monitoring");
	}


}