package sk.iway.iwcm.system.cron;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.test.BaseWebjetTest;

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
public class CronJUnitTest extends BaseWebjetTest implements TaskSource
{

	static int FIVE_ONCE_SCHEDULER_RUNS = 0;
	static int SCHEDULER_RUN_COUNT = 0;
	static List<String> FILLED_BY_PARAMS = null;
	@Override
	public List<CronTask> getTasks()
	{
		CronTask testTask = new CronTask();
		testTask.setTask("sk.iway.iwcm.system.cron.CronJUnitTest");
		testTask.setParams("one|two|three");
		testTask.setMinutes("*");
		testTask.setSeconds("*/5");
		return Arrays.asList(testTask);
	}

	public static void main(String[] args)
	{
		SCHEDULER_RUN_COUNT++;
		FIVE_ONCE_SCHEDULER_RUNS = 5;
		FILLED_BY_PARAMS = Arrays.asList(args);
		System.out.println("main called, time="+Tools.formatDateTimeSeconds(Tools.getNow())+" format="+Constants.getString("dateTimeFormat"));
	}

	@Test
	void scheduler()
	{
		assertEquals(0, FIVE_ONCE_SCHEDULER_RUNS);
		CronFacade.getInstance().setTaskSource(new CronJUnitTest());
		CronFacade.getInstance().start();
		safeSleep(5000);
		CronFacade.getInstance().stop();
		assertEquals(5, FIVE_ONCE_SCHEDULER_RUNS);
		assertEquals(3, FILLED_BY_PARAMS.size());
		assertTrue(FILLED_BY_PARAMS.get(0).equals("one") && FILLED_BY_PARAMS.get(2).equals("three"));
		System.out.println("SCHEDULER_RUN_COUNT="+SCHEDULER_RUN_COUNT);
		assertEquals(1, SCHEDULER_RUN_COUNT);
		CronFacade.getInstance().start();
		safeSleep(5000);
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
