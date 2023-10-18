package sk.iway.iwcm.users;

/**
 *  UnknownHashAlgorithm.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 4.1.2011 13:40:22
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class UnknownHashAlgorithm extends RuntimeException
{
	private static final long serialVersionUID = -8210465100657764582L;
	
	@Override
	public String getMessage()
	{
		return "Hash algorithm SHA-512 is not implemented on this virtual machine";
	}
}