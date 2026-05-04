package sk.iway.iwcm.system.monitoring;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.monitoring.jpa.ExecutionEntry;

/**
 *  ComponentExecutionTimeMonitor.java
 *
 *		NOTE! This class requires configuration variable 'serverMonitoringEnablePerformance' set to 'true' in order
 *			to work. The class won't perform any operation at all if this condition is not met.
 *
 *		Holds statistic sheets of component(.jsp) execution times.
 *
 *		Tracks:
 *			number of cache hits
 *			number of executions 
 *			total time of cache retrievals
 *			total time of all executions
 *			maximum execution time
 *			minimum execution time
 *		for each component. The same component with different parameters (e.g. news.jsp) has several
 *		track records, one for each distinct parameter set.
 *
 *
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 17.8.2009 13:57:34
 *@modified     $Date: 2009/08/19 13:58:10 $
 */
class ComponentExecutionTimeMonitor extends ExecutionTimeMonitor {
	
	public void addCacheRecord(String component, long timeTaken, long memoryDifference) {
		if (!Constants.getBoolean("serverMonitoringEnablePerformance"))
			return;
		
		synchronized (executionDurations) {
			ExecutionEntry record = getRecord(component);
			record.setNumberOfCacheHits(record.getNumberOfCacheHits() + 1);
			record.setTotalTimeOfCacheExecutions(record.getTotalTimeOfCacheExecutions() + timeTaken);
		}
	}
	
	@Override
	protected String generateEntryKeyFrom(String name) {
		name = name.replace("writeTag_!INCLUDE(", "");
		return name;
	}
	
	
}