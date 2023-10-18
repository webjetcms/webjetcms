package sk.iway.iwcm.system.monitoring;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 *  MemoryMeasurementJUnit.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 10.9.2010 15:38:26
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class MemoryMeasurementJUnit
{
	@Test
	public void test()
	{
		class TestMemoryMeasurement extends MemoryMeasurement{
			@Override
			protected long memoryNow(){return System.nanoTime();}
		}

		MemoryMeasurement measurement = new TestMemoryMeasurement();
		long memoryUsed = measurement.diff();
		assertTrue(memoryUsed > 0);
	}
}