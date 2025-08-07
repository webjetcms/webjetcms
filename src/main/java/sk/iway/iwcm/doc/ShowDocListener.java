package sk.iway.iwcm.doc;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;

/**
 * Pocuva eventy z WebJETu potrebne na nastavenie (urcenych) atributov do requestu.
 */
@Component
public class ShowDocListener {

    private NavbarService navbarService = new NavbarService();

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.doc.ShowDocBean'")
    public void setRequestData(final WebjetEvent<ShowDocBean> event) {
        WebjetEventType eventType = event.getEventType();

        DocDetails sourceDoc = event.getSource().getDoc();
        if(eventType == WebjetEventType.ON_END && sourceDoc != null) {
            HttpServletRequest sourceRequest = event.getSource().getRequest();
            DocDB docDB = DocDB.getInstance();

            //tempFieldADocId
            if (sourceDoc.getTempFieldADocId() > 0) {
                DocDetails dd_menu = docDB.getDoc(sourceDoc.getTempFieldADocId());
                if (dd_menu != null) {
                    sourceRequest.setAttribute("template_object_a", dd_menu.getData());
                }
            } else if (sourceDoc.getTempFieldADocId() == -2) {
                sourceRequest.setAttribute("template_object_a", "");
            }

            //tempFieldBDocId
            if (sourceDoc.getTempFieldBDocId() > 0) {
                DocDetails dd_menu = docDB.getDoc(sourceDoc.getTempFieldBDocId());
                if (dd_menu != null) {
                    sourceRequest.setAttribute("template_object_b", dd_menu.getData());
                }
            } else if (sourceDoc.getTempFieldBDocId() == -2) {
                sourceRequest.setAttribute("template_object_b", "");
            }

            //tempFieldCDocId
            if (sourceDoc.getTempFieldCDocId() > 0) {
                DocDetails dd_menu = docDB.getDoc(sourceDoc.getTempFieldCDocId());
                if (dd_menu != null) {
                    sourceRequest.setAttribute("template_object_c", dd_menu.getData());
                }
            } else if (sourceDoc.getTempFieldCDocId() == -2) {
                sourceRequest.setAttribute("template_object_c", "");
            }

            //tempFieldDDocId
            if (sourceDoc.getTempFieldDDocId() > 0) {
                DocDetails dd_menu = docDB.getDoc(sourceDoc.getTempFieldDDocId());
                if (dd_menu != null) {
                    sourceRequest.setAttribute("template_object_d", dd_menu.getData());
                }
            } else if (sourceDoc.getTempFieldDDocId() == -2) {
                sourceRequest.setAttribute("template_object_d", "");
            }

            //Set navbar
            sourceRequest.setAttribute("navbar", navbarService.getNavbar(sourceDoc, sourceRequest));
        }
    }
}
