package sk.iway.iwcm.system.captcha;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;

/**
 *  CaptchaServiceSingleton.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Date: 2.2.2010 10:58:11
 *@modified     $Date: 2010/02/09 08:56:19 $
 */
public class CaptchaServiceSingleton
{
	private static WebJETImageCaptchaService instance;
	private static String lastUsedMode = null;
	private static String lastBackgroundMode = null;
	private static int lastWaveSize = -9999;

   public static WebJETImageCaptchaService getInstance()
   {
	   if (instance==null || isConfigChange())
	   {
	   	Logger.debug(CaptchaServiceSingleton.class, "Creating Captcha instance");
		   instance = new WebJETImageCaptchaService();
		   
	   }
	   
	   return instance;
   }
   
   private static boolean isConfigChange()
   {
   	boolean isChange = false;
   	
   	if (lastUsedMode == null || lastUsedMode.equals(Constants.getString("captchaMode"))==false) isChange = true;
   	if (lastBackgroundMode == null || lastBackgroundMode.equals(Constants.getString("captchaBackgroundMode"))==false) isChange = true;
   	if (lastWaveSize == -9999 || lastWaveSize != Constants.getInt("captchaWaveSize")) isChange = true;
   	
   	if (isChange)
   	{
   		lastUsedMode = Constants.getString("captchaMode");
   		lastBackgroundMode = Constants.getString("captchaBackgroundMode");
		   lastWaveSize = Constants.getInt("captchaWaveSize");
   	}
   	
   	return isChange;
   }
   
   public static void flush()
   {
	   instance = null;
   }
}
