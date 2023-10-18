package sk.iway.iwcm.components.forum.rest;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.forum.jpa.DocForumRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@Service
@RequestScope
public class DocForumService {
    private DocForumRepository forumRepository;

    @Autowired
    public DocForumService(DocForumRepository forumRepository, HttpServletRequest request) {
        this.forumRepository = forumRepository;
    }

    /**
     * Perform delete of DocForumEntity. By constant "forumReallyDeleteMessages" check if perform soft or hard delete.
     * In case of soft delete set param deleted as 1.
     * @param id
     * @return return true after delete
     */
    public boolean deleteDocForum(Long id) {
        if(Constants.getBoolean("forumReallyDeleteMessages")) {
            //Hard delete
            forumRepository.hardDelete(id, CloudToolsForCore.getDomainId());
        } else {
            //Soft delete
            forumRepository.softDelete(id, CloudToolsForCore.getDomainId());
        }

        return true;
    }

    /**
     * Recover soft deleted entity by changing param delete from 1 to 0.
     * @param id
     */
    public void undeleteEntity(Long id) {
        forumRepository.undeleteEntity(id, CloudToolsForCore.getDomainId());
    }

    /**
     * Return icon options for status icon select.
     * @param prop
     * @return
     */
    public List<LabelValue> getStatusIconOptions(Prop prop) {
		List<LabelValue> icons = new ArrayList<>();

        icons.add(new LabelValue("<i class=\"fa-regular fa-circle-check\" style=\"color: #00be9f;\"></i> " + prop.getText("apps.forum.icon.confirmed"), "confirmed:true"));
        icons.add(new LabelValue("<i class=\"fa-regular fa-circle-xmark\" style=\"color: #ff4b58;\"></i> " + prop.getText("apps.forum.icon.non_confirmed"), "confirmed:false"));
        icons.add(new LabelValue("<i class=\"fa-solid fa-lock\" style=\"color: #000000;\"></i> " + prop.getText("apps.forum.icon.non_active"), "active:false"));
        icons.add(new LabelValue("<i class=\"fa-regular fa-trash-can-undo\" style=\"color: #fabd00;\"></i> " + prop.getText("apps.forum.icon.deleted"), "deleted:true"));

		return icons;
	}
}
