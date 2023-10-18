package sk.iway.iwcm.ebanking.epayments;

/**
 *  NotValidSignatureException.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.2 $
 *@created      Date: 26.8.2009 16:00:00
 *@modified     $Date: 2009/08/27 17:10:01 $
 */
public class InvalidSignatureException extends Exception
{
	private static final long serialVersionUID = 1L;

	public InvalidSignatureException()
	{
		super();
	}

	public InvalidSignatureException(String message)
	{
		super(message);
	}
}
