package sk.iway.iwcm.system.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import sk.iway.iwcm.DB;

/**
 * Konverter ktory automaticky escapne HTML kod ak sa v hodnote nachadzaju znaky &lt; alebo &gt;
 * Pre povolenie HTML znakov je potrebne pouzit anotaciu:
 * @jakarta.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
 * V pripade potreby vypnutia je mozne do konf. premennej xssHtmlAllowedFields pridat hodnotu jpaentity
 *
 * POZOR: ak vam to tu pada na tom, ze nevie skonvertovat BigDecimal na String a podobne
 * (cize aplikuje to akoby na nieco ine ako String) tak si skontrolujte typ pola v DB a typ pola
 * v Java triede, urcite to mate nespravne (v triede mate String namiesto Number, alebo v DB mate varchar namiesto number)
 */
@Converter(autoApply = true)
public class XssAttributeConverter implements AttributeConverter<String, String>
{
   @Override
   public String convertToDatabaseColumn(String data)
   {
      return data;
   }

   @Override
   public String convertToEntityAttribute(String databaseValue)
   {
      //V pripade potreby vypnutia je mozne do konf. premennej xssHtmlAllowedFields pridat hodnotu jpaentity
      if (databaseValue!=null && DB.isHtmlAllowed("jpaentity")==false)
      {
         return DB.filterHtml(databaseValue);
      }

      return databaseValue;
   }
}
