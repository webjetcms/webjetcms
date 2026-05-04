package sk.iway.iwcm.doc;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.beanutils.BeanUtils;

import sk.iway.iwcm.Tools;

/**
 *  AtrComparator.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 23.11.2005 12:03:44
 *@modified     $Date: 2007/09/07 13:39:29 $
 */
public class AtrComparator implements Comparator<AtrDocBean>, Serializable
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 5317055441014901662L;
	private String atrName = null;

	public AtrComparator(String atrName)
	{
		this.atrName = atrName;
	}
	@Override
	public int compare(AtrDocBean d1, AtrDocBean d2)
	{
		String s1 = getAtrValue(d1);
		String s2 = getAtrValue(d2);

		try
		{
			double i1 = Double.parseDouble(s1.replace(',', '.'));
			double i2 = Double.parseDouble(s2.replace(',', '.'));

			return Double.compare(i1, i2);
		}
		catch (Exception e)
		{
			//nie su to cisla
		}

		return(s1.compareTo(s2));
	}

	public String getAtrValue(AtrDocBean atrDocBean)
	{
		if (atrName.startsWith("doc."))
		{
			//skusme ako property
		   try
			{
				String value = BeanUtils.getProperty(atrDocBean, atrName.substring(4));
				if (value != null) return value;
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}

	   for (AtrBean atrBean : atrDocBean.getAtrList())
	   {
	   	if (atrName.equals(atrBean.getAtrName()) && Tools.isNotEmpty(atrBean.getValueHtml()))
			{
	   		//Logger.println(AtrComparator.class, "compare: " + atrDocBean.getTitle()+" value="+atrBean.getValueHtml());
				return(atrBean.getValueHtml().toLowerCase());
			}
	   }

	   //nenasli sme, skusme ako klasicke property
	   try
		{
			String value = BeanUtils.getProperty(atrDocBean, atrName);
			if (value != null) return value;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

	   return("");
	}
}
