package sk.iway.iwcm.system.monitoring;

/**
 *  SqlExecutionTimeMonitor.java
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 20.8.2009 15:09:26
 *@modified     $Date: 2009/08/21 10:55:38 $
 */
class SqlExecutionTimeMonitor extends ExecutionTimeMonitor
{
	
	@Override
	protected String generateEntryKeyFrom(String name)
	{
		return name.trim();
	}
}
