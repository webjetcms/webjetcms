package sk.iway.iwcm.helpers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

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
	public String toString() {
		return toString(null);
	}

	public String toString(Prop prop)
	{
		try
		{
			Map<String, PropertyDiff> changes = diff.diff();
			if (changes.size() == 0)
				return " Žiadne zmeny";

			// Try prepare Map with column names and translated column titles (stored in DataTableColumn annotation)
			Map<String, String> translated = new HashMap<>();
			String className = diff.getNewClassName();
			if(prop != null && Tools.isNotEmpty(className)) {
				try {
					Class<?> entityClass = Class.forName(className);
					while (entityClass != null) {
						for (Field field : entityClass.getDeclaredFields()) {
							field.setAccessible(true);
							if (field.isAnnotationPresent(DataTableColumn.class)) {
								DataTableColumn dtcAnn = field.getAnnotation(DataTableColumn.class);
								if (dtcAnn == null) continue;
								translated.put(field.getName(), prop.getText( dtcAnn.title()) );
							}
						}

						entityClass = entityClass.getSuperclass();
					}
				} catch (Exception ex) {
					// If something went wrong, deleete whole map
					translated = null;
				}
			}

		StringBuilder output = new StringBuilder();
		for (Entry<String, PropertyDiff> change : changes.entrySet())
		{
			String key = "";
			if(translated != null && translated.size() > 0) key = translated.get( change.getKey() );
			if(Tools.isEmpty(key)) key = change.getKey();

			output.append('\n').
				append( key ).
				append(": ");

			if (diff.hasOriginal()) {
				output.append(StringUtils.abbreviate(change.getValue().valueBefore.toString(), 100)).
				append(" -> ").
				append(StringUtils.abbreviate(change.getValue().valueAfter.toString(), 100));
			} else {
				output.append(StringUtils.abbreviate(change.getValue().valueAfter.toString(), 100));
			}
		}

			return output.toString();
		} catch (Exception ex) {
			Logger.error(BeanDiffPrinter.class, ex);
		}
		return " Chyba pri ziskani zoznamu zmien";
	}
}
