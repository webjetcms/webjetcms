package sk.iway.iwcm.system.monitoring;

/**
 *  MemoryMeasurement.java
 *  
 *		Used for measurements of consumed memory upon execution of a piece of code.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 10.9.2010 15:33:58
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class MemoryMeasurement
{
	
	long usedMemoryAtLastCheck;
	
	public MemoryMeasurement()
	{
		usedMemoryAtLastCheck = memoryNow();
	}

	protected long memoryNow()
	{
		Runtime rt = Runtime.getRuntime();
		return rt.totalMemory() - rt.freeMemory();
	}

	/**
	 * Returns the memory difference since the object creation or 
	 * since diff function has been called for the last time.
	 *  
	 * @return long Memory difference
	 */
	public long diff()
	{
		long now = memoryNow();
		long memoryDiff = now - usedMemoryAtLastCheck; 
		usedMemoryAtLastCheck = now;
		return memoryDiff;
	}
}