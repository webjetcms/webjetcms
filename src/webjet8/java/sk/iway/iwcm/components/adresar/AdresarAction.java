package sk.iway.iwcm.components.adresar;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import sk.iway.iwcm.system.stripes.WebJETActionBean;
import sk.iway.iwcm.users.UserDetails;

/**
 *  AdresarAction.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: bhric $
 *@version      $Revision: 1.1 $
 *@created      Date: 1.10.2009 16:28:53
 *@modified     $Date: 2009/10/08 08:06:38 $
 */
public class AdresarAction extends WebJETActionBean
{
	private String filter;
	private String vyraz;
	private int oddelenieId;

	public String getFilter()
	{
		return filter;
	}
	public void setFilter(String filter)
	{
		this.filter = filter;
	}
	public String getVyraz()
	{
		return vyraz;
	}
	public void setVyraz(String vyraz)
	{
		this.vyraz = vyraz;
	}
	public int getOddelenieId() {
		return oddelenieId;
	}

	public void setOddelenieId(int oddelenieId) {
		this.oddelenieId = oddelenieId;
	}

	@DefaultHandler
   @HandlesEvent("hladaj")
   public Resolution hladaj()
   {
		List<UserDetails> users = null;
		try
		{	if ("all".equals(filter)) {
				users = AdresarDB.listUsers(filter,vyraz, getRequest());
			} else {
		        List<Integer> oddelenieIds = new ArrayList<>();
		        oddelenieIds.add(this.getOddelenieId());
				users = AdresarDB.listUsers(oddelenieIds, vyraz, getRequest());
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		getRequest().setAttribute("users", users);
		return (new ForwardResolution("/components/maybeError.jsp"));
   }
}
