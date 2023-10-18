package sk.iway.iwcm.system.spring.webjet_component.dialect;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.insertScript.InsertScriptBean;
import sk.iway.iwcm.components.insertScript.InsertScriptDB;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;

import javax.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.List;

/**
 * Trieda CombineAttributeTagProcessor pre iwcm tag s nazvom script
 * Tag <div data-iwcm-script=""/> sluzi na vlozenie skriptov
 */
public class ScriptAttributeTagProcessor extends AbstractIwcmAttributeTagProcessor {
    private static final String ATTR_NAME = "script";
    private static final int PRECEDENCE = 10002;

    public ScriptAttributeTagProcessor(final String dialectPrefix) {
        super(dialectPrefix, ATTR_NAME, PRECEDENCE);
    }

    /**
     * Metoda pre nahradenie tagu obsahom
     */
    @Override
    protected void processTag(ITemplateContext context, IProcessableElementTag tag, String attributeValue, IElementTagStructureHandler structureHandler, HttpServletRequest request) {
        StringBuilder outPut = new StringBuilder();

        int docId = Tools.getDocId(request);
        DocDetails docDetails = DocDB.getInstance().getBasicDocDetails(docId, false);
        int groupId = docDetails != null ? docDetails.getGroupId() : -1;
        if(Tools.isEmpty(attributeValue) || groupId < 0) {
            Logger.debug(ScriptAttributeTagProcessor.class, "Attribute {} value is null or cannot find group for page id {}", attributeValue, docId);
            structureHandler.removeElement();
            return;
        }

        Date now = new Date(Tools.getNow());
        List<InsertScriptBean> listInsertScript = InsertScriptDB.getInstance().filter(null, attributeValue, null, docId, groupId, now, now);

        if (Tools.isEmpty(listInsertScript)) {
            Logger.debug(ScriptAttributeTagProcessor.class, "listInsertScript for postition {} is empty of parent groups list for group id {} is empty", attributeValue, groupId);
            structureHandler.removeElement();
            return;
        }

        for (InsertScriptBean isb : listInsertScript) {
            Logger.debug(ScriptAttributeTagProcessor.class,"listInsertScript.size(): {}", listInsertScript.size());
            if (!Tools.canSetCookie(isb.getCookieClass(), request.getCookies())) {
                //Logger.debug(this,"continue: "+listInsertScript.get(i).getCookieClass());
                continue;
            }
            if (outPut.length()>0) outPut.append("\n");
            outPut.append(isb.getScriptBody());
        }

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

        structureHandler.removeAttribute("data-iwcm-remove");

        if (replace) {
            structureHandler.replaceWith(outPut.toString(), false);
        } else {
            structureHandler.setBody(outPut.toString(), false);
        }
    }
}