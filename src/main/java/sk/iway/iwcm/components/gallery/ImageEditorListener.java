package sk.iway.iwcm.components.gallery;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;

import sk.iway.iwcm.JsonTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.perex_groups.PerexGroupsEntity;
import sk.iway.iwcm.components.perex_groups.PerexGroupsRepository;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class ImageEditorListener {

    @Autowired
    private PerexGroupsRepository perexGroupsRepository;

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='image-editor'")
    protected void setInitalData(final WebjetEvent<ThymeleafEvent> event) {
        ModelMap model = event.getSource().getModel();

        List<OptionDto> perexGroupsOptions = new java.util.ArrayList<>();
        for(PerexGroupsEntity perex : perexGroupsRepository.findAllByDomainIdOrderByPerexGroupNameAsc(CloudToolsForCore.getDomainId())) {
            perexGroupsOptions.add(new OptionDto(perex.getPerexGroupName(), String.valueOf(perex.getId()), null));
        }

        try {
            model.addAttribute("perexGroupsOptions", JsonTools.objectToJSON(perexGroupsOptions));
        } catch (Exception e) {
            Logger.error(ImageEditorListener.class, "Error setting initial data", e);
            model.addAttribute("perexGroupsOptions", null);
        }
    }
}
