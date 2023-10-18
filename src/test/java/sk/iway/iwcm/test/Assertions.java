package sk.iway.iwcm.test;

import java.util.Collection;

/**
 *  Assertions.java
 *
 *		A set of assertions not contained in the original JUnit framework.
 *		Webjet specific assertions 
 *
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 4.12.2009 14:52:59
 *@modified     $Date: 2009/12/11 15:46:32 $
 */
public final class Assertions
{
	
	public static <T1,T2> boolean collectionsEqual(Collection<T1> first, Collection<T2> second)
	{
		return collectionsEqual(first, second, normalComparator(first, second));
	}
	
	public static <T1,T2> boolean collectionsEqual(Collection<T1> first, Collection<T2> second, Equal<T1, T2> comparator)
	{
		return isSubset(first, second, comparator) && isSubset(second, first, reversedComparator(comparator));
	}
	
	public static <T1, T2> boolean isSubset(Collection<T1> subset, Collection<T2> superset, Equal<T1, T2> comparator)
	{
		//each element in the subset ought to be present in the superset
		for (T1 elementOfSubset : subset)
		{
			boolean isContained = false;
			for(T2 elementOfSuperset : superset)
			{
				if (comparator.areEqual(elementOfSubset, elementOfSuperset))
				{
					isContained = true;
					break;
				}
			}
			
			if (!isContained)
			{
				System.out.println("Neobsahuje: "+elementOfSubset);
				return false;
			}
		}
		
		return true;
	}
	
	
	/**
	 * Basic comparator - using equals
	 */
	private static <T1, T2> Equal<T1, T2> normalComparator(Collection<T1> first, Collection<T2> second)
	{
		return new Equal<T1, T2>(){
			@Override
			public boolean areEqual(T1 object1, T2 object2){return object1.equals(object2);}
		};
	}
	
	
	/**
	 * We need this one due to the reversed order of type parameters
	 */
	private static <T1, T2> Equal<T2, T1> reversedComparator(final Equal<T1, T2> comparator)
	{
		return new Equal<T2, T1>()
		{
			@Override
			public boolean areEqual(T2 object1, T1 object2){return comparator.areEqual(object2, object1);}
		};
	}
}
