package sk.iway.iwcm.dmail;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DebugTimer;

/**
 *  DomainThrottlePerformanceTester.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2013
 *@author       $Author: jeeff mhalas $
 *@version      $Revision: 1.3 $
 *@created      Date: 26.7.2013 11:20:01
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class DomainThrottlePerformanceTester
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		SortedMap<Long, String> map = Collections.synchronizedSortedMap(new TreeMap<Long,String>());
		Random rnd = new Random();
		SecureRandom random = new SecureRandom();
		DebugTimer dt = new DebugTimer("treemap test");
		for(int i = 0; i < 1000;i++)
		{
			map.put(nextLong(rnd,10000000), new BigInteger(130, random).toString(32));
		}
		dt.diff("after 10^3 rec");
		
		map.clear();
		for(int i = 0; i < 1000000;i++)
		{
			map.put(nextLong(rnd,10000000), new BigInteger(130, random).toString(32));
		}
		dt.diff("after 10^6 rec");
		
		dt.diff("before add");
		map.put(123456789l, "domain");
		dt.diff("after add");
		
		dt.diff("before count");
		int count =0;
		for(String domain : map.values())
		{
			Tools.isNotEmpty(domain);
			count++;
		}
		dt.diff("after count: "+count);
		
		
	}
	
	static long nextLong(Random rng, long n) {
	   // error checking and 2^x checking removed for simplicity.
	   long bits, val;
	   do {
	      bits = (rng.nextLong() << 1) >>> 1;
	      val = bits % n;
	   } while (bits-val+(n-1) < 0L);
	   return val;
	}
}
