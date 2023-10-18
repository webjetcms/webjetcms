package sk.iway.iwcm.system.spring.webjet_component.dialect;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import javax.servlet.http.HttpServletRequest;

/**
 * Abstraktna trieda pre Thymeleaf TagProcessor - spracovanie iwcm tagov
 * spracovava len otvaraci tag, ignoruje jeho body
 */
public abstract class AbstractIwcmAttributeTagProcessor extends AbstractAttributeTagProcessor {

    /**
     * POZOR: tu nesmu byt ziadne private atributy, ktore su zavisle na requeste
     * tato trieda vznikne len raz a je multithreadova, takze hodnoty by sa
     * prepisovali
     * preto je cela implementacia spravena cez staticke metody
     */

    protected AbstractIwcmAttributeTagProcessor(String dialectPrefix, String attrName, int precedence) {
        super(
                TemplateMode.HTML, // This processor will apply only to HTML mode
                dialectPrefix,     // Prefix to be applied to name for matching
                null,              // No tag name: match any tag name
                false,             // No prefix to be applied to tag name
                attrName,         // Name of the attribute that will be matched
                true,              // Apply dialect prefix to attribute name
                precedence,        // Precedence (inside dialect's precedence)
                true);             // Remove the matched attribute afterwards
    }

    protected abstract void processTag(ITemplateContext context, IProcessableElementTag tag, String attributeValue, IElementTagStructureHandler structureHandler, HttpServletRequest request);

    @Override
    protected void doProcess(ITemplateContext iTemplateContext, IProcessableElementTag iProcessableElementTag, AttributeName attributeName, String attributeValue, IElementTagStructureHandler iElementTagStructureHandler) {
        HttpServletRequest request = (HttpServletRequest) iTemplateContext.getVariable("request");
        processTag(iTemplateContext, iProcessableElementTag, attributeValue, iElementTagStructureHandler, request);
    }

}
