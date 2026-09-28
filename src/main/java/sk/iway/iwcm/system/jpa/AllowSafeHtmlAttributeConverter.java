package sk.iway.iwcm.system.jpa;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.HtmlSanitizer;
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
    * Sanitizes HTML while retaining basic formatting, safe styles, links, images,
    * accessibility attributes and custom data attributes on allowed elements.
    * Attribute names remain subject to the sanitizer's own name validation.
    *
    * @param unsafeHtml HTML to sanitize, may be {@code null}
    * @return sanitized HTML, or an empty string for {@code null}
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
         .allowAttributes("class").onElements("a", "img", "div", "span", "p", "h1", "h2", "h3", "h4", "h5", "h6", "i", "b", "strong", "em", "li", "ul", "ol", "blockquote", "table", "tr", "td", "th", "thead", "tbody", "tfoot", "br")
         .allowAttributes("role", "id", "title", "lang").globally()
         .allowAttributes("dir").matching(true, "ltr", "rtl", "auto").globally()
         .allowAttributes("tabindex").matching(false, "-1", "0").globally()
         .allowAttributes(collectDynamicAttributes(unsafeHtml)).globally()
         .toFactory();
      String safeHTML = policy.sanitize(unsafeHtml);
      safeHTML = Tools.replace(safeHTML, Constants.NON_BREAKING_SPACE, "&nbsp;");
      //allow + as normal char, e.g. in webpage title
      safeHTML = Tools.replace(safeHTML, "&#43;", "+");
      return safeHTML;
   }

   /**
    * Collects concrete {@code aria-*} and {@code data-*} attribute names because
    * the sanitizer does not support wildcards in attribute names. Only names are
    * collected; the original HTML is then processed by the full sanitization policy.
    *
    * @param html HTML to inspect, may be {@code null}
    * @return attribute names with a non-empty suffix after either prefix
    */
   private static String[] collectDynamicAttributes(String html) {
      Set<String> attributeNames = new HashSet<>();
      HtmlSanitizer.sanitize(html, new HtmlSanitizer.Policy() {
         @Override
         public void openTag(String elementName, List<String> attrs) {
            for (int i = 0; i < attrs.size(); i += 2) {
               String name = attrs.get(i);
               if (name.length() > 5 && (name.startsWith("aria-") || name.startsWith("data-"))) {
                  attributeNames.add(name);
               }
            }
         }

         @Override
         public void openDocument() {}

         @Override
         public void closeDocument() {}

         @Override
         public void closeTag(String elementName) {}

         @Override
         public void text(String text) {}
      });
      return attributeNames.toArray(new String[0]);
   }
}
