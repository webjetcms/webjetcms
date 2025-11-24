package sk.iway.iwcm.components.gallery;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;

import sk.iway.iwcm.JsonTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class ImageEditorListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='image-editor'")
    protected void setInitalData(final WebjetEvent<ThymeleafEvent> event) {
        ModelMap model = event.getSource().getModel();

        try {
            List<OptionDto> perexList = DocDB.getInstance().getPerexGroupOptions();

            String data = JsonTools.objectToJSON(perexList);
            model.addAttribute("perexGroupsOptions", data);

        } catch (Exception e) {
            Logger.error(ImageEditorListener.class, "Error setting initial data", e);
            model.addAttribute("perexGroupsOptions", "[]");
        }
    }
}
