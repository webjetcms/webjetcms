package sk.iway.iwcm.dmail;

/**
 *  Hromadne zasielanie emailov
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.8 $
 *@created      $Date: 2004/01/10 17:16:21 $
 *@modified     $Date: 2004/01/10 17:16:21 $
 */
public class EMailAction
{
	/**
	 * You can use this main method in cron to periodically send emails.
	 * @param args
	 * @deprecated - Use EmailDB.main(args) instead.
	 */
	@Deprecated
	public static void main(String[] args)
	{
		EmailDB.main(args);
	}
}