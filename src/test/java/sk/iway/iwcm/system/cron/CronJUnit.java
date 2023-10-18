package sk.iway.iwcm.system.cron;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 *  CronJUnit.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 12.7.2010 11:38:54
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class CronJUnit implements TaskSource
{

	static int FIVE_ONCE_SCHEDULER_RUNS = 0;
	static int SCHEDULER_RUN_COUNT = 0;
	static List<String> FILLED_BY_PARAMS = null;
	@Override
	public List<CronTask> getTasks()
	{
		CronTask testTask = new CronTask();
		testTask.setTask("sk.iway.iwcm.system.cron.CronJUnit");
		testTask.setParams("one|two|three");
		testTask.setMinutes("*");
		testTask.setSeconds("*");
		return Arrays.asList(testTask);
	}

	public static void main(String[] args)
	{
		SCHEDULER_RUN_COUNT++;
		FIVE_ONCE_SCHEDULER_RUNS = 5;
		FILLED_BY_PARAMS = Arrays.asList(args);
	}

	@Test
	public void scheduler()
	{
		assertEquals(0, FIVE_ONCE_SCHEDULER_RUNS);
		CronFacade.getInstance().setTaskSource(new CronJUnit());
		CronFacade.getInstance().start();
		safeSleep(1000);
		CronFacade.getInstance().stop();
		assertEquals(5, FIVE_ONCE_SCHEDULER_RUNS);
		assertEquals(3, FILLED_BY_PARAMS.size());
		assertTrue(FILLED_BY_PARAMS.get(0).equals("one") && FILLED_BY_PARAMS.get(2).equals("three"));
		System.out.println("SCHEDULER_RUN_COUNT="+SCHEDULER_RUN_COUNT);
		assertEquals(1, SCHEDULER_RUN_COUNT);
		CronFacade.getInstance().start();
		safeSleep(1000);
		CronFacade.getInstance().stop();
		assertEquals(2, SCHEDULER_RUN_COUNT);
	}

	private void safeSleep(long time)
	{
		try
		{
			Thread.sleep(time);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
}
