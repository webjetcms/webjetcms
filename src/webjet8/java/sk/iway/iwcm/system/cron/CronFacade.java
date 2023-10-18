package sk.iway.iwcm.system.cron;

import it.sauronsoftware.cron4j.Scheduler;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.system.cluster.ClusterRefresher;


/**
 *  CronFacade.java
 *
 *  Facade for convenient use of cron4j library.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 9.7.2010 13:14:18
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class CronFacade
{
	private static final CronFacade instance = new CronFacade();
	private Scheduler schedulerCron;
	private TaskSource source;
	private volatile boolean running = false;

	public static CronFacade getInstance()
	{
		return instance;
	}

	/**
	 * Method exists only for cluster purposes({@link ClusterRefresher})
	 */
	public static CronFacade getInstance(boolean restart)
	{
		if (restart)
			instance.start();
		return instance;
	}

	public void setTaskSource(TaskSource source)
	{
		this.source = source;
	}

	/**
	 * Causes cron4j scheduler to start the scheduling thread, executing tasks loaded from {@link TaskSource}.
	 * If the scheduler is already running when this method is called, the actual scheduler is stopped and a new one is created.
	 * Running start() when the scheduler is running thus effectively triggers a restart.
	 *
	 *  @throws IllegalStateException if no task source is supplied
	 */
	public synchronized void start()
	{
		if (source == null)
			throw new IllegalStateException("Cron's task source not set. Please call setTaskSource() prior to calling start()");

		try
		{
			stop();

			schedulerCron = new Scheduler();

			for(CronTask task : source.getTasks())
			{
				if(task.isEnableTask() == false) continue;

				try
				{
					Class<?> clazz = task.receiveClazz();
					schedulerCron.schedule(task.receiveCronPattern(), new RunnableWrapper(clazz, task.receiveArgs(),task.getAudit()));
					Logger.debug(WebjetDatabaseTaskSource.class, String.format("Cron task started {%s}: %s %s, with pattern %s", task.getId(), clazz.getName(), task.getParams(), task.receiveCronPattern()));
				}
				catch (Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
					sk.iway.iwcm.Logger.println(CronFacade.class, "Cron ERROR " + e.getMessage());
				}
			}

			schedulerCron.start();

			running = true;
			sk.iway.iwcm.Logger.debug(CronFacade.class, "Cron started!");
			Adminlog.add(Adminlog.TYPE_CRON, "Cron started!", -1, -1);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			sk.iway.iwcm.Logger.println(CronFacade.class, "Cron start error: "+e.getMessage());
			Adminlog.add(Adminlog.TYPE_CRON, "Cron start error: "+e.getMessage(), -1, -1);
		}
	}

	/**
	 * Stop the scheduling thread of cron4j, causing the scheduling process to cease.
	 * However, it does NOT stop threads already scheduled by cron4j that started prior to stop() execution.
	 */
	public synchronized void stop()
	{
		if (schedulerCron != null && running)
		{
			sk.iway.iwcm.Logger.println(CronFacade.class, "Cron stop requested");
			Adminlog.add(Adminlog.TYPE_CRON, "Cron stop requested", -1, -1);
			schedulerCron.stop();
		}
		running = false;
	}

	/**
	 * Launch a cron task OUTSIDE of cron environment, in a separate thread
	 */
	public synchronized void runSimpleTaskOnce(CronTask task) throws ClassNotFoundException
	{
		new Thread(new RunnableWrapper(task.receiveClazz(), task.receiveArgs(), task.getAudit())).start();
	}
}