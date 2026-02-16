package sk.iway.iwcm.xls;

import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.persistence.jpa.JpaEntityManager;

import jxl.Cell;
import jxl.Sheet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ActiveRecord;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.jpa.JpaTools;

@SuppressWarnings({"rawtypes"})
public class DefaultEntityImporter extends ExcelImportJXL
{

	private Field[] fields;
	private Class<ActiveRecord> entityClass = null;

	@SuppressWarnings({"unchecked"})
	public DefaultEntityImporter(InputStream in, HttpServletRequest request, PrintWriter out, Class entityClass)
	{
		super(in, request, out);
		this.entityClass = entityClass;
		fields = this.entityClass.getDeclaredFields();

		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		em.getTransaction().begin();
		Query q = em.createQuery("DELETE FROM "+entityClass.getSimpleName());
		q.executeUpdate();
		em.getTransaction().commit();
	}

	@Override
	protected void saveRow(Connection db_conn, Cell[] row, Sheet sheet, Prop prop) throws Exception
	{
		print("Setting object '"+entityClass.getSimpleName()+"' : ");
		ActiveRecord bean = entityClass.getDeclaredConstructor().newInstance();
		for (Field f : fields)
		{
			if (f.isAnnotationPresent(XLSColumn.class))
			{
				XLSColumn annotation = f.getAnnotation(XLSColumn.class);

				String name = annotation.name();
				Class<?> fieldType = annotation.type().getType();
				if (Tools.isEmpty(name)) name = f.getName();
				Object value = null;
				switch (annotation.type()) {
					case INT:
						Integer iVal = getIntValue(row, formatHeaderName(name));
						value = iVal;

						break;
					case DOUBLE:
						Double dVal = getDouble(row, formatHeaderName(name));
						value = dVal;
						break;
					case BOOLEAN:
						Boolean bVal = getBooleanValue(row, formatHeaderName(name));
						value = bVal;
						break;
					case DATE:
						Date dateVal = null;
						if (Tools.isNotEmpty(annotation.datePattern()))
						{
							dateVal = Tools.getDateFromString(getValue(row, formatHeaderName(name)), annotation.datePattern());
							value = dateVal;
						}
						else
						{
							dateVal = getDateValue(row, formatHeaderName(name));
							value = dateVal;
						}
						break;
					case CUSTOM:
						if (annotation.columnResolver().getSimpleName().equals("ColumnResolver")) throw new Exception("Column resolver not defined for field '"+f.getName()+"'."); //NOSONAR
						fieldType = annotation.columnType();
						ColumnResolver resolver = (ColumnResolver)annotation.columnResolver().getDeclaredConstructor().newInstance();
						value = resolver.resolveColumn(row, name);
						break;
					default:
						// default read String
						String sVal = getValue(row, formatHeaderName(name));
						value = sVal;
						break;

				}
				print(name+" = "+value+", ");
				set(f, fieldType, bean, value);
			}
		}

		if (!bean.save()) printlnError("Error saving object.");
		else println("Saving success.");
	}

	/**
	 * setne atribut do objektu, zavolanim defaultneho settera
	 * @param field
	 * @param type
	 * @param target
	 * @param value
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	private static void set(Field field, Class<?> type, Object target, Object value) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{

		String setterName = "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
		Method setter = null;
		try
		{
			setter = target.getClass().getDeclaredMethod(setterName, type);
		}
		catch (Exception e)
		{

			if (OBJECT_TO_PRIMITIVE.containsKey(type))
			{
				try
				{
					setter = target.getClass().getDeclaredMethod(setterName, OBJECT_TO_PRIMITIVE.get(type));
				}
				catch (Exception e2)
				{
					Logger.debug(DefaultEntityImporter.class, "Getter not found");
				}
			}
		}
		if (setter!=null) setter.invoke(target, value);
	}

	private static final Map<Class,Class> OBJECT_TO_PRIMITIVE;
	static {
		OBJECT_TO_PRIMITIVE = new HashMap<Class, Class>(); //NOSONAR
		OBJECT_TO_PRIMITIVE.put(Integer.class, Integer.TYPE);
		OBJECT_TO_PRIMITIVE.put(Double.class, Double.TYPE);
		OBJECT_TO_PRIMITIVE.put(Long.class, Long.TYPE);
		OBJECT_TO_PRIMITIVE.put(Boolean.class, Boolean.TYPE);
	}


}
