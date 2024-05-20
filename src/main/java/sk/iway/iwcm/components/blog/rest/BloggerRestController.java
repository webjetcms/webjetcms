package sk.iway.iwcm.components.blog.rest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.UserTools;
import sk.iway.iwcm.components.blog.jpa.BloggerBean;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.users.UsersDB;

@RestController
@RequestMapping("/admin/rest/blog")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_blog_admin')")
@Datatable
public class BloggerRestController extends DatatableRestControllerV2<BloggerBean, Long> {

    private final UserDetailsRepository userDetailsRepository;
    private final EditorFacade editorFacade;

    @Autowired
    public BloggerRestController(UserDetailsRepository userDetailsRepository, EditorFacade editorFacade) {
        super(null);
        this.userDetailsRepository = userDetailsRepository;
        this.editorFacade = editorFacade;
    }

    @Override
    public Page<BloggerBean> getAllItems(Pageable pageable) {
        List<BloggerBean> bloggers = BlogService.getAllBloggers();
        return new DatatablePageImpl<>(bloggers);
    }

    @Override
    public BloggerBean getOneItem(long id) {
        if(id < 0) return new BloggerBean();
        return BlogService.getBloggerBean(id);
    }

    @Override
    public BloggerBean insertItem(BloggerBean entity) {
        boolean result = BlogService.saveBlogger(entity, userDetailsRepository, editorFacade, getRequest());
        if(Boolean.FALSE.equals(result)) throwError("datatable.error.unknown");

        //SAVE successful
        entity.setPassword(UserTools.PASS_UNCHANGED); //hide passwd
        return entity;
    }

    @Override
    public BloggerBean editItem(BloggerBean entity, long id) {
        boolean result = BlogService.editBlogger(entity, userDetailsRepository, getRequest());
        if(Boolean.FALSE.equals(result)) throwError("datatable.error.unknown");

        //EDIT successful
        entity.setPassword(UserTools.PASS_UNCHANGED); //hide passwd
        return entity;
    }

    @Override
    public boolean deleteItem(BloggerBean entity, long id) {
        //DELETE action is prohibited
        throwError("");
        return false;
    }

    @SuppressWarnings("all")
    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, BloggerBean> target, Identity user, Errors errors, Long id, BloggerBean entity) {
        if ("remove".equals(target.getAction())) return;

        Prop prop = Prop.getInstance(request);

        if ("create".equals(target.getAction())) {
            if( UsersDB.getUser(entity.getLogin()) != null )
                errors.rejectValue("errorField.login", null, prop.getText("useredit.login_allready_exists"));
        }

        //not empty aby pri prazdnej hlasilo v editore, ze to je povinne pole
        if (Tools.isEmpty(entity.getEmail()) || !Tools.isEmail(entity.getEmail()))
            errors.rejectValue("errorField.email", null, prop.getText("javax.validation.constraints.Email.message"));

        //validate selected blogger group
        if(entity.getEditableGroup() == null)
            errors.rejectValue("errorField.editableGroups", null, "error.required");
    }

    @Override
    public BloggerBean processFromEntity(BloggerBean entity, ProcessItemAction action) {
        //pri exporte nastav prazdne heslo
        if (isExporting() && entity!=null) entity.setPassword("");

        return entity;
    }
}