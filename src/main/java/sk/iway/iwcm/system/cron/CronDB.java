package sk.iway.iwcm.system.cron;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 *  CronDB.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 9.7.2010 17:44:00
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class CronDB
{
	private static final String SQL_INSERT = "INSERT INTO crontab(task_name, second, minute, hour, dayofmonth,"
			+ " month, dayofweek, year, task, extrainfo, businessDays, cluster_node, audit_task, run_at_startup, enable_task) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String SQL_UPDATE = "UPDATE crontab SET task_name=?, second=?, minute=?, hour=?, dayofmonth=?,"
			+ " month=?, dayofweek=?, year=?, task=?, extrainfo=?, businessDays=?, cluster_node = ?, audit_task = ?, run_at_startup=?, enable_task=? WHERE id=?";

	private static final String SQL_MAX_VALUE = "SELECT MAX(id) FROM crontab";

	public static final Mapper<CronTask> mapper = new Mapper<CronTask>()
	{
		@Override
		public CronTask map(ResultSet rs) throws SQLException
		{
			CronTask task = new CronTask();
			task.setId(rs.getLong("id"));
			task.setTaskName(DB.getDbString(rs, "task_name"));
			task.setSeconds(DB.getDbString(rs, "second"));
			task.setMinutes(DB.getDbString(rs, "minute"));
			task.setHours(DB.getDbString(rs, "hour"));
			task.setDaysOfMonth(DB.getDbString(rs, "dayofmonth"));
			task.setMonths(DB.getDbString(rs, "month"));
			task.setDaysOfWeek(DB.getDbString(rs, "dayofweek"));
			task.setYears(DB.getDbString(rs, "year"));
			task.setTask(DB.getDbString(rs, "task").trim());
			task.setParams(DB.getDbString(rs, "extrainfo"));
			//task.setBusinessDays(rs.getBoolean("businessDays")); - uz sa nepouziva
			task.setBusinessDays(false);
			task.setClusterNode(DB.getDbString(rs, "cluster_node"));
			task.setAudit(rs.getBoolean("audit_task"));
			task.setRunAtStartup(rs.getBoolean("run_at_startup"));
			task.setEnableTask(rs.getBoolean("enable_task"));
			return task;
		}
	};

	public static CronTask getById(Long id)
	{
		try
		{
			return new ComplexQuery().setSql("SELECT * FROM crontab WHERE id = ?").setParams(id).singleResult(mapper);
		}
		catch (Exception ex)
		{

		}
		return null;
	}

	public static List<CronTask> getAll()
	{
		return new ComplexQuery().setSql("SELECT * FROM crontab").list(mapper);
	}

	public static List<CronTask> getCronTasks(String filterCrontabTask)
	{
		if (Tools.isNotEmpty(filterCrontabTask))
			return new ComplexQuery().setSql("SELECT * FROM crontab WHERE task LIKE ?").setParams("%" + filterCrontabTask + "%").list(mapper);
		else
			return CronDB.getAll();
	}

	public static List<CronTask> getCronTasksRunAtStartup()
	{
		return new ComplexQuery().setSql("SELECT * FROM crontab WHERE run_at_startup = ? AND enable_task = ?").setParams(Boolean.TRUE, Boolean.TRUE).list(mapper);
	}

	public static void delete(Long id)
	{
		CronTask task = getById(id);
		if (task != null)
		{
			Adminlog.add(Adminlog.TYPE_CRON, Adminlog.getChangelogDelete(task.getId(), task), id.intValue(), -1);
		}
		else
		{
			Adminlog.add(Adminlog.TYPE_CRON, "DELETE: \nid:"+id, id.intValue(), -1);
		}

		new SimpleQuery().execute("DELETE FROM crontab WHERE id = ?", id);
	}

	public static CronTask save(CronTask task)
	{
		CronTask old = getById(task.getId());

		if (task.getId() < 0)
			new SimpleQuery().execute(SQL_INSERT, task.getTaskName(), task.getSeconds(), task.getMinutes(), task.getHours(), task.getDaysOfMonth(), task.getMonths(),
					task.getDaysOfWeek(), task.getYears(), task.getTask(), task.getParams(), task.isBusinessDays(), task.getClusterNode(), task.getAudit(), task.isRunAtStartup(), task.isEnableTask());
		else
			new SimpleQuery().execute(SQL_UPDATE, task.getTaskName(), task.getSeconds(), task.getMinutes(), task.getHours(), task.getDaysOfMonth(), task.getMonths(),
					task.getDaysOfWeek(), task.getYears(), task.getTask(), task.getParams(), task.isBusinessDays(), task.getClusterNode(), task.getAudit(), task.isRunAtStartup(), task.isEnableTask(), task.getId());

		Long id = task.getId();
		if (id == null || id.longValue()<1) {
			id = Long.valueOf((new SimpleQuery()).forLong(SQL_MAX_VALUE));
		}

		CronTask saved = getById(id);

		Adminlog.add(Adminlog.TYPE_CRON, Adminlog.getChangelog(saved.getId(), saved, old), saved.getId().intValue(), -1);

		return saved;
	}

}