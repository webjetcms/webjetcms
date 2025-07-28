package sk.iway.iwcm.components.gallery;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;

import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.perex_groups.PerexGroupsEntity;
import sk.iway.iwcm.components.perex_groups.PerexGroupsRepository;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class ImageEditorListener {

    @Autowired
    private PerexGroupsRepository perexGroupsRepository;

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='image-editor'")
    protected void setInitalData(final WebjetEvent<ThymeleafEvent> event) {
        ModelMap model = event.getSource().getModel();

        Map<Long, String> perexGroupsMap = new HashMap<>();
        for(PerexGroupsEntity perex : perexGroupsRepository.findAllByDomainIdOrderByPerexGroupNameAsc(CloudToolsForCore.getDomainId())) {
            perexGroupsMap.put(perex.getId(), perex.getPerexGroupName());
        }

        model.addAttribute("perexGroupsOptions", perexGroupsMap);
    }
}
