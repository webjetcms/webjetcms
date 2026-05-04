package sk.iway.iwcm.system.cron;

import java.util.List;

/**
 *  TaskSource.java
 *
 *		Interface providing {@link CronFacade} with tasks to execute
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 9.7.2010 15:51:43
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public interface TaskSource
{
	List<CronTask> getTasks();
}
