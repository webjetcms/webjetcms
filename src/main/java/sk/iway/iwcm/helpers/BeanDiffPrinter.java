package sk.iway.iwcm.helpers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DataTableColumnsFactory;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.users.UserGroupsDB;

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
		//to distinguish between system and custom prop, we need to use different variable
		Prop systemProp = prop;
		if (systemProp == null) systemProp = Prop.getInstance();

		try
		{
			Map<String, PropertyDiff> changes = diff.diff();
			if (changes.size() == 0)
				return " " + systemProp.getText("beandiff.no_diff");

			// Try prepare Map with column names and translated column titles (stored in DataTableColumn annotation)
			Map<String, String> translated = new HashMap<>();
			String className = diff.getNewClassName();
			boolean isDatatable = false;
			if(prop != null && Tools.isNotEmpty(className)) {
				try {

					translated.put("publishAfterStart", prop.getText("editor.public"));
					translated.put("publishAfterEnd", prop.getText("editor.disableAfterEnd"));
					translated.put("perexGroupString", prop.getText("editor.perex.group"));
					translated.put("passwordProtected", prop.getText("groupedit.passwordProtected"));

					Class<?> entityClass = diff.getActual().getClass();
					while (entityClass != null) {
						for (Field field : entityClass.getDeclaredFields()) {
							if (field.isAnnotationPresent(DataTableColumn.class)) {
								DataTableColumn dtcAnn = field.getAnnotation(DataTableColumn.class);
								if (dtcAnn == null) continue;
								translated.put(field.getName(), prop.getText( dtcAnn.title()) );
								isDatatable = true;
							} else {
								String fieldName = field.getName();
								String key = "beandiff." + className.toLowerCase() + "." + fieldName;
								String translation = prop.getText(key);
								if (Tools.isNotEmpty(translation) && translation.equals(key)==false) {
									translated.put(fieldName, translation);
								} else {
									key = "editor." + fieldName;
									translation = prop.getText(key);
									if (Tools.isNotEmpty(translation) && translation.equals(key)==false) {
										translated.put(fieldName, translation);
									}
								}
							}
						}

						entityClass = entityClass.getSuperclass();
					}
				} catch (Exception ex) {
					// If something went wrong, delete whole map
					translated = null;
				}

				try {
					if (isDatatable && translated != null && translated.size() > 5) {
						Map<String, PropertyDiff> changesSorted = new LinkedHashMap<>();
						//try to sort fields according to datatable columns order
						DataTableColumnsFactory factory = new DataTableColumnsFactory(className);
						if (factory != null) {
							List<sk.iway.iwcm.system.datatable.json.DataTableColumn> columns = factory.getColumns(null);
        					List<sk.iway.iwcm.system.datatable.json.DataTableColumn> columnsSorted = DataTableColumnsFactory.sortColumns(columns);

							for (sk.iway.iwcm.system.datatable.json.DataTableColumn column : columnsSorted) {
								String fieldName = column.getName();
								PropertyDiff change = changes.get(fieldName);
								if (change != null) {
									changesSorted.put(fieldName, change);
									changes.remove(fieldName);
								}
							}

							//add remaining changes which are not in datatable columns
							for (Entry<String, PropertyDiff> change : changes.entrySet()) {
								changesSorted.put(change.getKey(), change.getValue());
							}
							changes = changesSorted;
						}
					}
				}catch (Exception ex) {
					// ignore
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
					output.append(translateValue(change.getValue().valueBefore.toString(), change.getKey(), prop)).
					append(" -> ").
					append(translateValue(change.getValue().valueAfter.toString(), change.getKey(), prop));
				} else {
					output.append(translateValue(change.getValue().valueAfter.toString(), change.getKey(), prop));
				}
			}

			return output.toString();
		} catch (Exception ex) {
			Logger.error(BeanDiffPrinter.class, ex);
		}
		return " " + systemProp.getText("beandiff.error_getting_changes");
	}

	/**
	 * Translate value to more user friendly form, e.g. translate template id to template name, group ids to group names, etc.
	 * Also abbreviate long values to max 100 chars.
	 * @param value
	 * @param key
	 * @param prop
	 * @return
	 */
	private String translateValue(String value, String key, Prop prop) {
		value = StringUtils.abbreviate(value, 100);
		if (prop != null) {
			if ("true".equals(value)) value = prop.getText("components.menu.generate_empty_span.true");
			else if ("false".equals(value)) value = prop.getText("components.menu.generate_empty_span.false");
		}
		if ("tempId".equals(key)) {
			//translate to template name
			int tempId = Tools.getIntValue(value, -1);
			TemplateDetails temp = TemplatesDB.getInstance().getTemplate(tempId);
			if (temp != null) {
				value = temp.getTempName() + " (" + value + ")";
			}
		}
		if ("passwordProtected".equals(key)) {
			int[] passGroupIds = Tools.getTokensInt(value, ",");
			UserGroupsDB ugdb = UserGroupsDB.getInstance();
			StringBuilder sb = new StringBuilder();
			for (int groupId : passGroupIds) {
				String groupName = ugdb.getUserGroupName(groupId);
				if (Tools.isNotEmpty(groupName)) {
					if (sb.isEmpty() == false) sb.append(", ");
					sb.append(groupName);
				}
			}
			if (sb.length() > 0) {
				value = sb.toString() + " (" + value + ")";
			}
		}
		if (key.toLowerCase().contains("docid")) {
			int docId = Tools.getIntValue(value, -1);
			if (docId > 0) {
				DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
				if (doc != null) {
					value = doc.getTitle() + " (" + value + ")";
				}
			}
		}
		return value;
	}
}