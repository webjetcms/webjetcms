package sk.iway.iwcm.system.spring.webjet_component.dialect;

import jakarta.servlet.http.HttpServletRequest;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.ninja.Ninja;

/**
 * Trieda WriteAttributeTagProcessor pre iwcm tag s nazvom write
 * Tag <div data-iwcm-write=""/> sluzi na vykonanie !INCLUDE v zadanom mene atributu (napr. doc_data)
 */
public class WriteAttributeTagProcessor extends AbstractIwcmAttributeTagProcessor {

    private static final String ATTR_NAME = "write";
    private static final int PRECEDENCE = 10003;

    public WriteAttributeTagProcessor(final String dialectPrefix) {
        super(dialectPrefix, ATTR_NAME, PRECEDENCE);
    }

    /**
     * Metoda pre nahradenie tagu obsahom
     */
    @Override
    protected void processTag(ITemplateContext context, IProcessableElementTag tag, String attributeValue, IElementTagStructureHandler structureHandler, HttpServletRequest request) {
        Ninja ninja = new Ninja(request);
        String htmlCode = ninja.write(attributeValue);

        //ak ma tag atribut data-iwcm-remove="tag" tak robime replace celeho tagu, inak robime len setBody
        IAttribute removeAttr = tag.getAttribute("data-iwcm-remove");
        boolean replace = true;
        if (removeAttr != null) {
            if ("false".equals(removeAttr.getValue())) replace=false;
            else if ("true".equals(removeAttr.getValue())) {
                //uz je defaultne nastavene
                //replace=true;
            }
            else if ("tag".equals(removeAttr.getValue())==false) replace=false;
        } else {
            //pre doc_ atributy remove defaultne nerobime
            if (attributeValue.startsWith("doc_")) replace = false;
        }

        IAttribute jqueryAttr = tag.getAttribute("data-iwcm-jquery");
        if (jqueryAttr != null) {
            if ("false".equals(jqueryAttr.getValue())) {
                //nechceme vkladat jquery automaticky, poznacme ze uz bolo vlozene
                Tools.insertJQuery(request);
            }
            if ("true".equals(jqueryAttr.getValue())) {
                htmlCode = Tools.insertJQuery(request) + "\n" + htmlCode;
            }
            if ("force".equals(jqueryAttr.getValue())) {
                request.removeAttribute("jQueryInserted");
                request.removeAttribute("commonPageFunctionsInserted");
                htmlCode = Tools.insertJQuery(request) + "\n" + htmlCode;
            }
        }

        structureHandler.removeAttribute("data-iwcm-write");
        structureHandler.removeAttribute("data-iwcm-remove");
        structureHandler.removeAttribute("data-iwcm-jquery");

        if (replace) {
            structureHandler.replaceWith(htmlCode, false);
        } else {
            structureHandler.setBody(htmlCode, false);
        }
    }
}