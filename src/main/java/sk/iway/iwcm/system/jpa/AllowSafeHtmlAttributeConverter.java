package sk.iway.iwcm.system.jpa;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import sk.iway.iwcm.DB;

/**
 * Attribute konverter pre JPA beany ktory umozni pouzit BEZPECNY HTML kod
 * podla odporucani OWASP: https://owasp.org/www-project-java-html-sanitizer/
 * ponechane je len zakladne HTML formatovanie
 *
 * pouzitie: k fieldu ktory ma mat povoleny HTML kod zadajte anotaciu
 * @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
 */
@Converter(autoApply = false)
public class AllowSafeHtmlAttributeConverter implements AttributeConverter<String, String>
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
         //odfiltruj nebezpecny kod na zaklade OWASP sanitizera
         //https://owasp.org/www-project-java-html-sanitizer/
         PolicyFactory policy = new HtmlPolicyBuilder()
            .allowCommonInlineFormattingElements()
            .allowCommonBlockElements()
            .allowStyling()
            .allowElements("a")
            .allowUrlProtocols("http", "https", "data")
            .allowAttributes("href").onElements("a")
            .allowAttributes("src").onElements("img")
            .toFactory();
         String safeHTML = policy.sanitize(databaseValue);
         return safeHTML;
      }

      return databaseValue;
   }
}
