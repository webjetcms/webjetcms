package sk.iway.iwcm.system.stripes;

import java.util.Enumeration;
import java.util.ResourceBundle;

import sk.iway.iwcm.i18n.Prop;

/**
 *  IwayResourceBundle.java
 *
 *@Title        webjet5
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2007
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 27.3.2007 10:35:42
 *@modified     $Date: 2007/09/07 13:39:29 $
 */
public class IwayResourceBundle extends ResourceBundle
{
	String lng;
	Prop prop;

	public IwayResourceBundle(String lng)
	{
		super();
		this.lng = lng;
		//Logger.debug(IwayResourceBundle.class, "constructor");
		prop = Prop.getInstance(lng);
	}

	@Override
	public Enumeration<String> getKeys()
	{

		return null;
	}

	@Override
	protected Object handleGetObject(String key)
	{
		String value = prop.getText(key);
		if (value.equals(key)) value=null;
		//Logger.debug(IwayResourceBundle.class, "handleGetObject("+key+")="+value);
		return value;
	}
}
