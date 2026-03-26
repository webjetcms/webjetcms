package sk.iway.iwcm.system.stripes;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.sourceforge.stripes.config.Configuration;
import sk.iway.iwcm.Constants;

/**
 *  LocalizationBundleFactory.java - factory pre pracu s IwayResourceBundle
 *
 *@Title        webjet5
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2007
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 26.3.2007 21:40:50
 *@modified     $Date: 2007/09/07 13:39:29 $
 */
public class LocalizationBundleFactory implements net.sourceforge.stripes.localization.LocalizationBundleFactory
{
	@Override
	public void init(Configuration configuration) throws Exception
	{
		//Logger.debug(LocalizationBundleFactory.class, "init, conf="+configuration);
	}
	@Override
	public ResourceBundle getErrorMessageBundle(Locale locale) throws MissingResourceException
	{
		return(getFormFieldBundle(locale));
	}
	@Override
	public ResourceBundle getFormFieldBundle(Locale locale) throws MissingResourceException
	{
		//Logger.debug(LocalizationBundleFactory.class, "getFormFieldBundle, locale:"+locale);

		if (locale == null)
		{
			return new IwayResourceBundle(Constants.getString("defaultLanguage"));
		}
		else
		{
			return new IwayResourceBundle(locale.getLanguage());
		}
	}
}
