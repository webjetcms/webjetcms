package sk.iway.iwcm;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;

import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;
import sk.iway.iwcm.database.ActiveRecord;
import sk.iway.iwcm.database.JpaDB;

public class EntityTypeConverter<T extends ActiveRecord> implements TypeConverter<T>
{

	/**
	 * konvertuje id zo selectov na objekt z ciselnika
	 * db triedy ciselnikov su singletony tak prve skusa vytvarat instanciu invokovanim
	 * getInstance
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T convert(String value, Class<? extends T> clazz, Collection<ValidationError> errors)
	{
		int id = Tools.getIntValue(value, 0);
		if (id>0)
		{
			String dbClassName = Tools.replace(clazz.getName(), "Bean", "DB");
			try
			{
				try
				{
				Class<? extends JpaDB<T>> dbClass = (Class<? extends JpaDB<T>>) Class.forName(dbClassName);
				Method getInstanceMethod = null;

					getInstanceMethod = dbClass.getMethod("getInstance");
					JpaDB<T> db = (JpaDB<T>)getInstanceMethod.invoke(dbClass);
					return db.getById(id);
				}
				catch (NoSuchMethodException | ClassNotFoundException nfe)
				{
					/*do nothing*/
				}

				JpaDB<T> db = (JpaDB<T>)JpaDB.of(clazz);
				return db.getById(id);

			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		return null;
	}

	@Override
	public void setLocale(Locale arg0)
	{

	}
}