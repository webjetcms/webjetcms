package sk.iway.iwcm.components.news;

import java.util.Collection;
import java.util.Locale;

import net.sourceforge.stripes.validation.ScopedLocalizableError;
import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;

@SuppressWarnings("rawtypes")
public class EnumeratedTypeConverter implements TypeConverter<Enum> {

    /**
     * Does nothing at present due to the fact that enumerated types don't support localization
     * all that well. 
     */
    @Override
	public void setLocale(Locale locale) {
        // Do nothing
    }

	@SuppressWarnings("unchecked")
	@Override
	public Enum convert(String input, Class<? extends Enum> targetType, Collection<ValidationError> errors) {

        try 
        {
            return Enum.valueOf(targetType, input.toUpperCase());
        }
        catch (IllegalArgumentException iae) 
        {
            errors.add(new ScopedLocalizableError("converter.enum", "notAnEnumeratedValue"));
            return null;
        }
    }
}