package sk.iway.iwcm.users;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;

import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;
import sk.iway.iwcm.Tools;

/**
 * Automaticky konvertuje UserDetails (aj Identity) na int a spat JPA,
 * zaroven je to TypeConverter pre Stripes :P
 *
 * @author mbocko
 *
 */
public class UserDetailsConverter implements Converter, TypeConverter<UserDetails>
{
	private static final long serialVersionUID = 1L;

	@Override
	public UserDetails convertDataValueToObjectValue(Object value, Session arg1)
	{
		if (value instanceof Number)
		{
			int id = ((Number) value).intValue();
			if (id == -1 || id==0) return null;

			//v magma_calendar su neschvalene zaznamy zapisane v DB s -user_id hodnotou
			if (id < -1) {
				id = -id;
			}
			UserDetails user = UsersDB.getUserCached(id);
			if (user!=null)
			{
				return user;
			}
		}
		return null;
	}

	@Override
	public Integer convertObjectValueToDataValue(Object object, Session arg1)
	{
		if (object instanceof UserDetails)
		{
			UserDetails user = (UserDetails)object;
			return Integer.valueOf(user.getUserId());
		}
		return null;
	}

	@Override
	public UserDetails convert(String value, Class<? extends UserDetails> paramClass, Collection<ValidationError> paramCollection)
	{
		int id = Tools.getIntValue(value, 0);
		if (id>0)
		{
			UserDetails user = UsersDB.getUserCached(id);
			if (user!=null)
			{
				return user;
			}
		}
		return null;
	}


	@Override
	public void setLocale(Locale paramLocale)
	{

	}

	@Override
	public void initialize(DatabaseMapping arg0, Session arg1)
	{

	}

	@Override
	public boolean isMutable()
	{
		return false;
	}
}
