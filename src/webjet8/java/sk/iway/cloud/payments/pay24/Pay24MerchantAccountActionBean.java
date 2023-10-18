package sk.iway.cloud.payments.pay24;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.stripes.WebJETActionBean;

/**
 *  Pay24MerchantAccountActionBean.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2017
 *@author       $Author: jeeff prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.5.2017 8:28:57
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class Pay24MerchantAccountActionBean extends WebJETActionBean
{
//	@ValidateNestedProperties({
//	      @Validate(field="eshopId", required=true),
//	      @Validate(field="key", required=true),
//	      @Validate(field="domainId", required=true)
//	    })
	private Pay24MerchantAccountBean account = new Pay24MerchantAccountBean();
	public Pay24MerchantAccountActionBean()
	{
		account = getAccount(false);
	}
	
	@DefaultHandler
	public Resolution defaultHandler()
	{
		return new ForwardResolution(RESOLUTION_CONTINUE);
	}
	
	@Override
	public void setContext(net.sourceforge.stripes.action.ActionBeanContext context) {
		this.context = context;
		
		Pay24MerchantAccountBean byDomain = new JpaDB<Pay24MerchantAccountBean>(Pay24MerchantAccountBean.class).findFirst("domainId", CloudToolsForCore.getDomainId());
		if(byDomain != null) this.account = byDomain;
	};
	
	public Resolution save()
	{
		Identity user = getCurrentUser();
		if (user == null || user.isDisabledItem("anyPremiumService"))
		{
			return new ForwardResolution(RESOLUTION_NOT_LOGGED);			
		}
		
		//set correct domain_id (if somebody tries to use fake one)
		account.setDomainId(CloudToolsForCore.getDomainId());
		boolean saveok=account.save();
		getRequest().setAttribute("pay24Saveok", saveok);
		return new ForwardResolution(RESOLUTION_CONTINUE);
	}
	
	public Pay24MerchantAccountBean getAccount()
	{
		return account;
	}
	
	/** Inicializacia platobnej brany. 
	 * 
	 * @param testGateway - ak je true, vytvori testovaciu branu, ak je false, pokusi sa vytvorit ostru/live platobnu branu.
	 */
	public Pay24MerchantAccountBean getAccount(boolean isTestGateway)
	{
		if(!isTestGateway && !Tools.isAnyEmpty(Constants.getString("24payEshopId"), Constants.getString("24payKey"), Constants.getString("24payMid")))
		{	//ostra platba webjet (nie cloud)
			account.setEshopId(Constants.getString("24payEshopId"));
			account.setMid(Constants.getString("24payMid"));
			account.setKey(Constants.getString("24payKey")); 
		}
		else
		{	//testovacie data
			account.setEshopId("11111111");
			account.setMid("demoOMED");
			account.setKey("1234567812345678123456781234567812345678123456781234567812345678");
		}

		if(!isTestGateway && InitServlet.isTypeCloud() )
		{	//ostra platba webjet cloud
			Pay24MerchantAccountBean byDomain = new JpaDB<Pay24MerchantAccountBean>(Pay24MerchantAccountBean.class).findFirst("domainId", CloudToolsForCore.getDomainId());
			if(byDomain != null) 
			{
				account.setEshopId(byDomain.getEshopId());
				account.setMid(byDomain.getMid());
				account.setKey(byDomain.getKey()); 
			}
		}
		account.setDomainId(CloudToolsForCore.getDomainId());
		return account;
	}

	public void setAccount(Pay24MerchantAccountBean account)
	{
		this.account = account;
	}
}
