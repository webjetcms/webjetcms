package sk.iway.iwcm.system.monitoring;

import java.util.Date;

import springfox.documentation.annotations.ApiIgnore;

/**
 * MonitoringBean.java - zaznam z tabulky monitoring pre uchovanie udajov z monitorovania
 *	@Title        webjet4
 *	@Company      Interway s.r.o. (www.interway.sk)
 *	@Copyright    Interway s.r.o. (c) 2001-2008
 *	@author       $Author: kmarton $
 *	@version      $Revision: 1.1 $
 *	@created      Date: 11.06.2009 10:52:51
 *	@modified     $Date: 2009/08/05 13:39:54 $
 */
//pada to na .nodeName v JS kode swaggeru, preto musime ignorovat
@ApiIgnore()
public class MonitoringBean
{
	private int 	monitoringId;		// identifikator
	private Date	dateInsert;			// cas a datum zaznamu
	private String nodeName;			// nazov clustera
	private int 	dbActive;			// pocet aktivnych spojeni s db
	private int 	dbIdle;				// pocet necinnych spojeni s db
	private long 	memFree;				// velkost volnej pamate
	private long 	memTotal;			// velkost celkovej pamate
	private int 	cache;				// pocet objektov v cache
	private int 	sessions;			// pocet sessions
	private double cpuUsage;			// vyuzitie procesora - cely system
	private double processUsage;			// vyuzitie procesora - jeden proces - webjet


	public long getUsedMem()
	{
		return (this.memTotal - this.memFree);
	}

	public int getMonitoringId()
	{
		return monitoringId;
	}
	public void setMonitoringId(int monitoringId)
	{
		this.monitoringId = monitoringId;
	}
	public Date getDateInsert()
	{
		return dateInsert == null ? null : (Date) dateInsert.clone();
	}
	public void setDateInsert(Date dateInsert)
	{
		this.dateInsert = dateInsert == null ? null : (Date) dateInsert.clone();
	}
	public String getNodeName()
	{
		return nodeName;
	}
	public void setNodeName(String nodeName)
	{
		this.nodeName = nodeName;
	}
	public int getDbActive()
	{
		return dbActive;
	}
	public void setDbActive(int dbActive)
	{
		this.dbActive = dbActive;
	}
	public int getDbIdle()
	{
		return dbIdle;
	}
	public void setDbIdle(int dbIdle)
	{
		this.dbIdle = dbIdle;
	}
	public long getMemFree()
	{
		return memFree;
	}
	public void setMemFree(long memFree)
	{
		this.memFree = memFree;
	}
	public long getMemTotal()
	{
		return memTotal;
	}
	public void setMemTotal(long memTotal)
	{
		this.memTotal = memTotal;
	}
	public int getCache()
	{
		return cache;
	}
	public void setCache(int cache)
	{
		this.cache = cache;
	}
	public int getSessions()
	{
		return sessions;
	}
	public void setSessions(int sessions)
	{
		this.sessions = sessions;
	}
	public double getCpuUsage()
	{
		return cpuUsage;
	}
	public void setCpuUsage(double cpuUsage)
	{
		this.cpuUsage = cpuUsage;
	}
	public double getProcessUsage()
	{
		return processUsage;
	}
	public void setProcessUsage(double processUsage)
	{
		this.processUsage = processUsage;
	}
}
