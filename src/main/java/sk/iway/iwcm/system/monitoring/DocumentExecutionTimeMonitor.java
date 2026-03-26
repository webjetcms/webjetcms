package sk.iway.iwcm.system.monitoring;


/**
 *  DocumentExecutionTimeMonitor.java
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 19.8.2009 14:40:40
 *@modified     $Date: 2009/08/19 13:58:09 $
 */
class DocumentExecutionTimeMonitor extends ExecutionTimeMonitor
{

	@Override
	protected String generateEntryKeyFrom(String name)
	{
		return name;
	}	
}