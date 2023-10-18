package sk.iway.iwcm.components.news;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;

import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;

public class NewsTemplateConverter implements Converter, TypeConverter<NewsTemplateBean>
{
	private static final long serialVersionUID = 1L;

	@Override
	public NewsTemplateBean convertDataValueToObjectValue(Object value, Session arg1) 
	{
		if (value instanceof String)
		{			
			return new NewsTemplateBean((String) value);
		}
		return null;
	}

	@Override
	public String convertObjectValueToDataValue(Object object, Session arg1) 
	{
		if (object instanceof NewsTemplateBean)
		{
			NewsTemplateBean template = (NewsTemplateBean)object;
			if (template!=null)
			{
				return template.getKey();
			}
		}
		return null;
	}

	@Override
	public NewsTemplateBean convert(String value, Class<? extends NewsTemplateBean> paramClass, Collection<ValidationError> paramCollection) 
	{
		if (value != null)
		{
			return new NewsTemplateBean((String) value);
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
