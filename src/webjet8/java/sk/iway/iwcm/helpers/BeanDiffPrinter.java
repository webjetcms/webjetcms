package sk.iway.iwcm.helpers;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import sk.iway.iwcm.Logger;

/**
 *  BeanDiffPrinter.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 31.3.2010 14:05:32
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class BeanDiffPrinter
{
	BeanDiff diff;

	public BeanDiffPrinter(BeanDiff diff)
	{
		this.diff = diff;
	}

	public String toHtml()
	{
		return toString().replaceAll("\n", "<br />");
	}

	@Override
	public String toString()
	{
		try
		{
			Map<String, PropertyDiff> changes = diff.diff();
			if (changes.size() == 0)
				return " Žiadne zmeny";

			StringBuilder output = new StringBuilder();
			for (Entry<String, PropertyDiff> change : changes.entrySet())
			{
				output.append('\n').
					append(change.getKey()).
					append(": ").
					append(StringUtils.abbreviate(change.getValue().valueBefore.toString(), 100)).
					append(" => ").
					append(StringUtils.abbreviate(change.getValue().valueAfter.toString(), 100));
			}

			return output.toString();
		} catch (Exception ex) {
			Logger.error(BeanDiffPrinter.class, ex);
		}
		return " Chyba pri ziskani zoznamu zmien";
	}
}
