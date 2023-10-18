package sk.iway.iwcm.system.spring.webjet_component.dialect;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.processor.element.AbstractAttributeModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Abstraktna trieda pre Thymeleaf MODELProcessor - spracovanie iwcm tagov
 * spracovava aj BODY elementu, nielen atributy
 */
public abstract class AbstractIwcmAttributeModelProcessor extends AbstractAttributeModelProcessor {

    /**
     * POZOR: tu nesmu byt ziadne private atributy, ktore su zavisle na requeste
     * tato trieda vznikne len raz a je multithreadova, takze hodnoty by sa
     * prepisovali
     * preto je cela implementacia spravena cez staticke metody
     */

    protected AbstractIwcmAttributeModelProcessor(String dialectPrefix, String attrName, int precedence) {
        super(
                TemplateMode.HTML,  // This processor will apply only to HTML mode
                dialectPrefix,      // Prefix to be applied to name for matching
                null,               // No tag name: match any tag name
                false,              // No prefix to be applied to tag name
                attrName,           // Name of the attribute that will be matched
                true,               // Apply dialect prefix to attribute name
                precedence,         // Precedence (inside dialect's precedence)
                true);              // Remove the matched attribute afterwards
    }

    protected abstract void processTag(ITemplateContext context, IModel model, IProcessableElementTag baseTag, HttpServletRequest request);

    @Override
    protected void doProcess(ITemplateContext context, IModel model, AttributeName attributeName, String attributeValue, IElementModelStructureHandler structureHandler) {

        HttpServletRequest request = (HttpServletRequest) context.getVariable("request");
        IProcessableElementTag baseTag = null;
        for (int i = 0; i < model.size(); i++) {
            final ITemplateEvent event = model.get(i);
            if (event instanceof IProcessableElementTag) {
                IProcessableElementTag tag = (IProcessableElementTag) event;
                // nas base tag je vzdy prvy element tag
                baseTag = tag;
                break;
            }
        }
        if (baseTag != null) {
            processTag(context, model, baseTag, request);
        }

    }

    protected static void addCssTag(IModel model, IModelFactory modelFactory, String href) {
        // ak tam uz nieco je, tak pridaj odsadenie
        if (model.size() > 0)
            model.add(modelFactory.createText("        "));

        Map<String, String> attributes = new HashMap<>();
        attributes.put("href", href);
        attributes.put("rel", "stylesheet");
        attributes.put("type", "text/css");

        IOpenElementTag link = modelFactory.createOpenElementTag("link", attributes, AttributeValueQuotes.DOUBLE, false);
        model.add(link);
        model.add(modelFactory.createCloseElementTag("link"));
        model.add(modelFactory.createText("\n"));
    }

    protected static void addScriptTag(IModel model, IModelFactory modelFactory, String src) {
        // ak tam uz nieco je, tak pridaj odsadenie
        if (model.size() > 0)
            model.add(modelFactory.createText("        "));

        Map<String, String> attributes = new HashMap<>();
        attributes.put("src", src);
        attributes.put("type", "text/javascript");

        IOpenElementTag script = modelFactory.createOpenElementTag("script", attributes, AttributeValueQuotes.DOUBLE, false);
        model.add(script);
        model.add(modelFactory.createCloseElementTag("script"));
        model.add(modelFactory.createText("\n"));
    }
}
