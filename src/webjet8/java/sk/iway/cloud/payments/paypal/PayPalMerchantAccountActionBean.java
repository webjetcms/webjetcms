package sk.iway.cloud.payments.paypal;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.stripes.WebJETActionBean;

/**
 *  PayPalMerchantAccountActionBean.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2014
 *@author       $Author: jeeff mhalas $
 *@version      $Revision: 1.3 $
 *@created      Date: 25.9.2014 10:28:40
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class PayPalMerchantAccountActionBean extends WebJETActionBean
{
	@ValidateNestedProperties({
      @Validate(field="user", required=true),
      @Validate(field="pwd", required=true),
      @Validate(field="signature", required=true)
    })
	private PayPalMerchantAccountBean account = new PayPalMerchantAccountBean();
	
	@Override
	public void setContext(net.sourceforge.stripes.action.ActionBeanContext context) {
		this.context = context;
		
		//TO DO: check for xss attack
		
		//find if this domain has already defined paypal info, if yes load it
		PayPalMerchantAccountBean byDomain = new JpaDB<PayPalMerchantAccountBean>(PayPalMerchantAccountBean.class).findFirst("domainId", CloudToolsForCore.getDomainId());
		if(byDomain != null) this.account = byDomain;
	};
	
	@DefaultHandler
	public Resolution defaultHandler()
	{
		return new ForwardResolution(RESOLUTION_CONTINUE);
	}
	
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
		getRequest().setAttribute("paypalSaveok", saveok);
		return new ForwardResolution(RESOLUTION_CONTINUE);
	}

	public PayPalMerchantAccountBean getAccount()
	{
		return account;
	}

	public void setAccount(PayPalMerchantAccountBean account)
	{
		this.account = account;
	}
	
	
}
