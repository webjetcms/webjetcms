package sk.iway.iwcm.system.cron;

import java.lang.reflect.Method;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.system.cluster.ClusterRefresher;


/**
 *  RunnableWrapper.java
 *
 *  Wraps an execution of public static void main() in a Runnable interface.
 *  Necessary for transforming main() method into a cron task.
 *  @see CronFacade
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 9.7.2010 16:03:51
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
class RunnableWrapper implements Runnable
{
	private final Class<?> clazz;
	private final String[] args;
	private final boolean audit;
	private final Long id;

	public RunnableWrapper(Class<?> clazz, CronTask task)
	{
		this.clazz = clazz;
		this.args = task.receiveArgs();
		this.audit = task.getAudit();
		this.id = task.getId();
	}

	@Override
	public void run()
	{
		try
		{
			int autoModeRandomDelay = ClusterRefresher.getAutoModeRandomDelay();
			if (autoModeRandomDelay > 0)
			{
				Thread.sleep(autoModeRandomDelay);
			}

			Method main = clazz.getMethod("main", String[].class);
			Object[] arguments = new Object[]{args};

			StringBuilder argsString = new StringBuilder("");
			if (args != null && args.length>0)
			{
				for (String arg : args)
				{
					if (argsString.isEmpty() == false) argsString.append(' ');
					argsString.append(arg);
				}
			}

			if(audit) {
				int auditId = -1;
				if (id != null) auditId = id.intValue();
				Adminlog.add(Adminlog.TYPE_CRON, String.format("Cron task executed: %s [%s], id: %d", clazz.getName(), argsString, id), auditId, -1);
			}
			main.invoke(null, arguments);
		}
		catch (InterruptedException ie)
		{
			Thread.currentThread().interrupt();
			sk.iway.iwcm.Logger.error(RunnableWrapper.class, "Cron task interrupted: " + clazz.getName(), ie);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.println(RunnableWrapper.class, "---------FAILED TO LAUNCH A CRONTAB TASK-----------");
			sk.iway.iwcm.Logger.println(RunnableWrapper.class, clazz.getName());
			sk.iway.iwcm.Logger.error(RunnableWrapper.class, "Error while executing cron task: " + clazz.getName(), e);
		}
	}
}