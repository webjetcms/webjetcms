package sk.iway.iwcm.system.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Constants;

/**
 *  ExecutionTimeMonitoringJUnit.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 10.9.2010 14:45:26
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ExecutionTimeMonitoringJUnit
{
	private static final String SOMETHING = "/something";

	@BeforeEach
	public void reset(){
		Constants.setBoolean("serverMonitoringEnablePerformance", true);
		ExecutionTimeMonitor.resetDocumentMeasurements();
		ExecutionTimeMonitor.resetComponentMeasurements();
	}

	@Test
	public void simpleExecution(){
		ExecutionTimeMonitor.recordDocumentExecution(SOMETHING, 20, 0);
		assertTrue(ExecutionTimeMonitor.statsForDocuments().size() == 1);
		assertTrue(ExecutionTimeMonitor.statsForDocuments().get(0).getTotalTimeOfExecutions() == 20);
	}

	@Test
	public void multipleExecutions(){
		ExecutionTimeMonitor.recordDocumentExecution(SOMETHING, 20, 0);
		ExecutionTimeMonitor.recordDocumentExecution(SOMETHING, 30, 0);
		assertEquals(ExecutionTimeMonitor.statsForDocuments().get(0).getTotalTimeOfExecutions(), 50);
		assertEquals(ExecutionTimeMonitor.statsForDocuments().get(0).getAverageExecutionTime(), 25);
		assertEquals(ExecutionTimeMonitor.statsForDocuments().get(0).getNumberOfExecutions(), 2);
		assertEquals(ExecutionTimeMonitor.statsForDocuments().get(0).getWhatWasExecuted(), SOMETHING);
	}

	@Test
	public void memoryExecutions(){
		ExecutionTimeMonitor.recordDocumentExecution(SOMETHING, 20, 1000);
		ExecutionTimeMonitor.recordDocumentExecution(SOMETHING, 30, 2000);
		assertEquals(ExecutionTimeMonitor.statsForDocuments().get(0).getTotalMemoryConsumed(), 3000);
		assertEquals(ExecutionTimeMonitor.statsForDocuments().get(0).getAverageMemoryConsumption(), 1500);
	}

	@Test
	public void invalidMeasurements(){
		ExecutionTimeMonitor.recordComponentExecution(SOMETHING, 20, 1000);
		ExecutionTimeMonitor.recordComponentExecution(SOMETHING, 20, -1000);
		ExecutionTimeMonitor.recordComponentExecution(SOMETHING, 20, 2000);
		assertEquals(ExecutionTimeMonitor.statsForComponents().get(0).getTotalMemoryConsumed(), 3000);
		assertEquals(ExecutionTimeMonitor.statsForComponents().get(0).getAverageMemoryConsumption(), 1500);
		assertEquals(ExecutionTimeMonitor.statsForComponents().get(0).getMemoryConsumptionPeek(), 2000);
	}
}