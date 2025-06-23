package sk.iway.iwcm.system.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;

/**
 * Attribute konverter pre JPA beany ktory umozni pouzit BEZPECNY HTML kod
 * podla odporucani OWASP: https://owasp.org/www-project-java-html-sanitizer/
 * ponechane je len zakladne HTML formatovanie
 *
 * pouzitie: k fieldu ktory ma mat povoleny HTML kod zadajte anotaciu
 * @jakarta.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
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
         String safeHTML = sanitize(databaseValue);
         return safeHTML;
      }

      return databaseValue;
   }

   /**
    * Sanitize HTML code using Owasp HTML sanitizer, allowed is:
    * - common inline formatting elements
    * - common block elements
    * - styling
    * - a element
    * - img element
    * - href attribute on a element
    * @param unsafeHtml
    * @return
    */
   public static String sanitize(String unsafeHtml) {
      //odfiltruj nebezpecny kod na zaklade OWASP sanitizera
      //https://owasp.org/www-project-java-html-sanitizer/
      PolicyFactory policy = new HtmlPolicyBuilder()
         .allowCommonInlineFormattingElements()
         .allowCommonBlockElements()
         .allowStyling()
         .allowElements("a")
         .allowElements("img")
         .allowUrlProtocols("http", "https", "data")
         .allowAttributes("href").onElements("a")
         .allowAttributes("src", "alt", "title").onElements("img")
         .allowAttributes("class").onElements("a", "img", "div", "span", "p", "h1", "h2", "h3", "h4", "h5", "h6", "i", "b", "strong", "em")
         .toFactory();
      String safeHTML = policy.sanitize(unsafeHtml);
      safeHTML = Tools.replace(safeHTML, Constants.NON_BREAKING_SPACE, "&nbsp;");
      return safeHTML;
   }
}
