package sk.iway.iwcm.system.monitoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.monitoring.jpa.ExecutionEntry;
import sk.iway.iwcm.i18n.Prop;

/**
 *  ExecutionTimeMonitor.java
 *
 *		Serves as a facade and as a superclass for monitoring issues
 *
 *	For the big picture:	@see ComponentExecutionTimeMonitor
 *
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.2 $
 *@created      Date: 19.8.2009 14:43:51
 *@modified     $Date: 2009/08/21 10:55:38 $
 */
public abstract class ExecutionTimeMonitor
{
	private static DocumentExecutionTimeMonitor documentsMonitor = new DocumentExecutionTimeMonitor();

	private static ComponentExecutionTimeMonitor componentsMonitor = new ComponentExecutionTimeMonitor();

	private static SqlExecutionTimeMonitor sqlMonitor = new SqlExecutionTimeMonitor();

	protected Map<String, ExecutionEntry> executionDurations = new ConcurrentHashMap<String, ExecutionEntry>();


	public static void recordSqlExecution(String sql, long timeTaken)
	{
		sqlMonitor.addExecutionRecord(sql, timeTaken, timeTaken);
	}

	public static void recordDocumentExecution(String uri, long timeTaken, long memoryDifference)
	{
		documentsMonitor.addExecutionRecord(uri, timeTaken, memoryDifference);
	}

	public static void recordComponentExecution(String component, long timeTaken, long memoryDifference)
	{
		componentsMonitor.addExecutionRecord(component, timeTaken, memoryDifference);
	}

	public static void recordComponentExecutionFromCache(String component, long timeTaken)
	{
		componentsMonitor.addCacheRecord(component, timeTaken, -1);
	}

	public static void resetDocumentMeasurements()
	{
		documentsMonitor.reset();
	}

	public static void resetComponentMeasurements()
	{
		componentsMonitor.reset();
	}

	public static void resetSqlMeasurements()
	{
		sqlMonitor.reset();
	}

	public static List<ExecutionEntry> statsForComponents()
	{
		return componentsMonitor.generateStats();
	}

	public static List<ExecutionEntry> statsForDocuments()
	{
		return documentsMonitor.generateStats();
	}

	public static List<ExecutionEntry> statsForSqls()
	{
		return sqlMonitor.generateStats();
	}

	public void reset()
	{
		executionDurations = new ConcurrentHashMap<String, ExecutionEntry>();
	}

	public void addExecutionRecord(String name, long timeTaken, long memoryDifference)
	{
		if (!Constants.getBoolean("serverMonitoringEnablePerformance") || name==null)
			return;

		synchronized (executionDurations)
		{
			ExecutionEntry record = getRecord(name);
			record.setNumberOfHits(record.getNumberOfHits() + 1);
			record.setTotalTimeOfExecutions(record.getTotalTimeOfExecutions() + timeTaken);

			if (record.getMaximumExecutionTime() < timeTaken)
				record.setMaximumExecutionTime(timeTaken);

			if (record.getMinimumExecutionTime() > timeTaken)
				record.setMinimumExecutionTime(timeTaken);

			if (memoryDifference > 0) {

				record.setTotalMemoryConsumed(record.getTotalMemoryConsumed() + memoryDifference);

				record.setValidMemoryMeasurements(record.getValidMemoryMeasurements() + 1);

				if (memoryDifference > record.getMemoryConsumptionPeek())
					record.setMemoryConsumptionPeek(memoryDifference);
			}
		}
	}

	protected ExecutionEntry getRecord(String name)
	{
		ExecutionEntry record;
		name = generateEntryKeyFrom(name);

		synchronized (executionDurations)
		{
			if (executionDurations.containsKey(name))
				record = executionDurations.get(name);
			else
				record = new ExecutionEntry(name);

			executionDurations.put(name, record);
		}
		return record;
	}

	protected abstract String generateEntryKeyFrom(String name);

	public List<ExecutionEntry> generateStats()
	{
		if (!Constants.getBoolean("serverMonitoringEnablePerformance"))
			throw new IllegalStateException(Prop.getInstance().getText("components.monitoring.not_enabled"));

		List<ExecutionEntry> stats = new ArrayList<ExecutionEntry>(executionDurations.values());
		Collections.sort(stats);
		return stats;
	}
}