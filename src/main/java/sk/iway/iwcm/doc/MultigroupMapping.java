package sk.iway.iwcm.doc;

/**
 *  MultigroupMapping.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: rusho $
 *@version      $Revision: 1.3 $
 *@created      Date: 24.8.2010 10:53:00
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class MultigroupMapping
{
	private int docId;
	private int masterId;
	private boolean redirect;
	
	public int getDocId()
	{
		return docId;
	}
	public void setDocId(int docId)
	{
		this.docId = docId;
	}
	public int getMasterId()
	{
		return masterId;
	}
	public void setMasterId(int masterId)
	{
		this.masterId = masterId;
	}
	public boolean isRedirect()
	{
		return redirect;
	}
	public void setRedirect(boolean redirect)
	{
		this.redirect = redirect;
	}
	
	
}
