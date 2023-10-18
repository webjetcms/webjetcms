package sk.iway.iwcm.components.blog;

/**
 *  BlogGroupDoesntExistException.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 18.4.2011 11:43:54
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class BlogGroupDoesntExistException extends Exception
{
	private static final long serialVersionUID = 1L;
	
	public BlogGroupDoesntExistException()
	{
		super();
	}
	
	public BlogGroupDoesntExistException(String message){
		super(message);
	}
}