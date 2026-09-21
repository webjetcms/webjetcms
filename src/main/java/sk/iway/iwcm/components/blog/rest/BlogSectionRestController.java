package sk.iway.iwcm.components.blog.rest;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.blog.jpa.BlogSectionBean;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;

/** Creates Blog sections through the standard editor with inline validation. */
@RestController
@RequestMapping("/admin/rest/blog/sections")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_blog|cmp_blog_admin')")
@Datatable
public class BlogSectionRestController extends DatatableRestControllerV2<BlogSectionBean, Long> {
    private final EditorFacade editorFacade;

    public BlogSectionRestController(EditorFacade editorFacade) {
        super(null);
        this.editorFacade = editorFacade;
    }

    @Override
    public Page<BlogSectionBean> getAllItems(Pageable pageable) {
        return new DatatablePageImpl<>(List.of());
    }

    @Override
    public boolean checkItemPerms(BlogSectionBean entity, Long id) {
        if (!Long.valueOf(-1).equals(id) || entity == null || entity.getParentGroupId() == null
                || !BloggerService.isUserBloggerOrBloggerAdmin(getUser())) return false;
        return BlogService.getFolderTree(getUser(), DocDB.getDomain(getRequest())).isSelectable(entity.getParentGroupId());
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, BlogSectionBean> target,
            Identity user, Errors errors, Long id, BlogSectionBean entity) {
        // Check the scope before looking up names in the requested parent folder.
        if (!"create".equals(target.getAction()) || !checkItemPerms(entity, -1L)) throwError("components.blog.basic_perm_error");
        String name = GroupsDB.sanitizeGroupName(entity.getGroupName(), true);
        if (GroupsDB.getInstance().getGroup(name, entity.getParentGroupId()) != null) {
            errors.rejectValue("errorField.groupName", null, getProp().getText("components.blog.topics.already_exists"));
        }
    }

    @Override
    public BlogSectionBean insertItem(BlogSectionBean entity) {
        GroupDetails group = BloggerService.addNewBloggerGroup(editorFacade, getUser(), entity.getParentGroupId(), entity.getGroupName());
        if (group == null) throwError("components.blog.add_new_group.failed");
        entity.setId((long) group.getGroupId());
        entity.setGroupName(group.getGroupName());
        addNotify(new NotifyBean(getProp().getText("components.blog.add_folder.title"),
                getProp().getText("components.blog.add_new_group.success"), NotifyBean.NotifyType.SUCCESS, 60000));
        return entity;
    }
}
