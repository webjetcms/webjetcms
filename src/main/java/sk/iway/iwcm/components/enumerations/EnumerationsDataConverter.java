package sk.iway.iwcm.components.enumerations;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;

import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.enumerations.dto.EnumerationDataDto;
import sk.iway.iwcm.components.enumerations.mapper.EnumerationMapper;

/**
 * Automaticky konvertuje EnumerationDataBean na EnumerationDataDto na int a spat JPA
 *
 * @author mpijak
 *
 */
public class EnumerationsDataConverter implements Converter, TypeConverter<EnumerationDataDto>
{
    private static final long serialVersionUID = 1L;

    @Override
    public EnumerationDataDto convertDataValueToObjectValue(Object value, Session arg1)
    {
        if (value instanceof Number) {
            Integer id = ((Number) value).intValue();
            EnumerationDataDto enumerationDataDto = EnumerationMapper.INSTANCE.toEnumerationDataDto(EnumerationDataDB.getEnumerationDataById(id.intValue()));
            if (enumerationDataDto != null) {
                return enumerationDataDto;
            }
        }
        return null;
    }

    @Override
    public Integer convertObjectValueToDataValue(Object object, Session arg1)
    {
        if (object instanceof EnumerationDataDto)
        {
            EnumerationDataDto enumerationDataDto = (EnumerationDataDto) object;
            return Integer.valueOf(enumerationDataDto.getId());
        }
        return null;
    }

    @Override
    public EnumerationDataDto convert(String value, Class<? extends EnumerationDataDto> paramClass, Collection<ValidationError> paramCollection)
    {
        int id = Tools.getIntValue(value, 0);
        if (id > 0)
        {
            EnumerationDataDto enumerationDataDto = EnumerationMapper.INSTANCE.toEnumerationDataDto(EnumerationDataDB.getEnumerationDataById(id));
            if (enumerationDataDto != null) {
                return enumerationDataDto;
            }
        }
        return null;
    }


    @Override
    public void setLocale(Locale paramLocale)
    {
        //must implement
    }

    @Override
    public void initialize(DatabaseMapping arg0, Session arg1)
    {
        //must implement
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }
}
