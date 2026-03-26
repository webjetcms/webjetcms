package sk.iway.iwcm.system.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Attribute konverter pre JPA beany ktory umozni pouzit HTML kod
 * pouzitie: k fieldu ktory ma mat povoleny HTML kod zadajte anotaciu
 * @jakarta.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
 *
 * @Column(name = "text")
 * @Column(name = "description")
 */
@Converter(autoApply = false)
public class AllowHtmlAttributeConverter implements AttributeConverter<String, String>
{
   @Override
   public String convertToDatabaseColumn(String data)
   {
      return data;
   }

   @Override
   public String convertToEntityAttribute(String databaseValue)
   {
      //Logger.debug(AllowHtmlAttributeConverter.class, "value="+databaseValue);
      return databaseValue;
   }
}
