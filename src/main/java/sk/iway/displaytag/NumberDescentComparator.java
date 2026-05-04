package sk.iway.displaytag;

/**
 *  NumberDescentComparator.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Date: 10.4.2006 11:19:45
 *@modified     $Date: 2006/05/05 08:54:28 $
 */
public class NumberDescentComparator extends NumberComparator
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Object object1, Object object2)
	{
		int compare = super.compare(object1, object2);
		return(-compare);
	}
}
