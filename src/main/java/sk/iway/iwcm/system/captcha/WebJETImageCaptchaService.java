package sk.iway.iwcm.system.captcha;

import com.octo.captcha.service.captchastore.FastHashMapCaptchaStore;
import com.octo.captcha.service.image.DefaultManageableImageCaptchaService;

/**
 *  ImageCaptchaService.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Date: 2.2.2010 13:36:38
 *@modified     $Date: 2010/02/09 08:56:19 $
 */
public class WebJETImageCaptchaService extends DefaultManageableImageCaptchaService
{
	public WebJETImageCaptchaService()
	{
		super(new FastHashMapCaptchaStore(),
		    		new WebJETImageCaptchaEngine(),
		    		180,
		    		100000,
		    		75000);
	}
	public boolean testResponseForID(String ID, String response)
	{
		try
		{
			if (super.store.getCaptcha(ID) != null) return super.store.getCaptcha(ID).validateResponse(response.toUpperCase());
		}
		catch (Exception e)
		{
			
		}
		return false;		
	}
}
