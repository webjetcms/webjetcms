package sk.iway.iwcm;

/**
 *  ObjectNotSerializableException.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2013
 *@author       $Author: jeeff Marián Halaš $
 *@version      $Revision: 1.3 $
 *@created      Date: 14.1.2013 14:41:06
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ObjectNotSerializableException extends Exception
{

	public ObjectNotSerializableException(String string)
	{
		super(string);
	}

	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 327762376315955824L;
}
