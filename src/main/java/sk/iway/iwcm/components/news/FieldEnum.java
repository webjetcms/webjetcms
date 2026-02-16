package sk.iway.iwcm.components.news;

import java.util.Date;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;

/**
 * Zoznam poli v tabulke documents
 * @author mbocko
 *
 */
public enum FieldEnum
{
	DOC_ID(Integer.class),
	DATA(String.class),
	DATA_ASC(String.class),
	DATE_CREATED(Date.class),
	PUBLISH_START(Date.class),
	PUBLISH_END(Date.class),
	AUTHOR_ID(Integer.class),
	SEARCHABLE(Boolean.class),
	GROUP_ID(Integer.class),
	AVAILABLE(Boolean.class),
	SHOW_IN_MENU(Boolean.class),
	PASSWORD_PROTECTED(Boolean.class),
	CACHEABLE(Boolean.class),
	EXTERNAL_LINK(String.class),
	VIRTUAL_PATH(String.class),
	TEMP_ID(Integer.class),
	TITLE(String.class),
	NAVBAR(String.class),
	FILE_NAME(String.class),
	SORT_PRIORITY(Integer.class),
	HEADER_DOC_ID(Integer.class),
	FOOTER_DOC_ID(Integer.class),
	MENU_DOC_ID(Integer.class),
	RIGHT_MENU_DOC_ID(Integer.class),
	HTML_HEAD(String.class),
	HTML_DATA(String.class),
	PEREX_PLACE(String.class),
	PEREX_IMAGE(String.class),
	PEREX_GROUP(String.class),
	EVENT_DATE(Date.class),
	SYNC_ID(Integer.class),
	SYNC_STATUS(Boolean.class),
	FIELD_A(String.class),
	FIELD_B(String.class),
	FIELD_C(String.class),
	FIELD_D(String.class),
	FIELD_E(String.class),
	FIELD_F(String.class),
	FIELD_G(String.class),
	FIELD_H(String.class),
	FIELD_I(String.class),
	FIELD_J(String.class),
	FIELD_K(String.class),
	FIELD_L(String.class),
	FIELD_M(String.class),
	FIELD_N(String.class),
	FIELD_O(String.class),
	FIELD_P(String.class),
	FIELD_Q(String.class),
	FIELD_R(String.class),
	FIELD_S(String.class),
	FIELD_T(String.class),
	DISABLE_AFTER_END(Boolean.class),
	FORUM_COUNT(Integer.class),
	VIEWS_TOTAL(Integer.class),
	REQUIRE_SSL(Boolean.class);
	
	private Class<?> fieldType;
	
	private FieldEnum (Class<?> fieldType)
	{
		this.fieldType = fieldType;
	}
	
	public Class<?> getFieldType()
	{
		return fieldType;
	}
	
	public String getFieldTypeString()
	{
		return fieldType.getSimpleName();
	}
	
	public String getDbName()
	{
		return "d."+this.name().toLowerCase();
	}

	private static String getFields(boolean data)
	{
		StringBuilder sb = new StringBuilder();
		boolean addcomma = false;
		for (FieldEnum field :	FieldEnum.values())
		{
			if (!data && field.name().equalsIgnoreCase("data"))
				continue;
			if (addcomma) 
				sb.append(",");
			sb.append(field.getDbName());
			addcomma = true;
		}
		return sb.toString();
	}

	public static String getFields()
	{
		return getFields(true);
	}

	public static String getFieldsNoData()
	{
		return getFields(false);
	}
	
	public String getTranslate()
	{
		return Prop.getInstance().getText("news.field_enum." + name().toLowerCase());
	}

	public static FieldEnum getField(String name)
	{
		for (FieldEnum field :	FieldEnum.values())
		{
			if ( Tools.replace(field.name(), "_", "").equalsIgnoreCase(name))
				return field;
		}
		return null;
	}

}
