package sk.iway.iwcm.components.news.templates.rest;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.news.templates.jpa.NewsTemplatesEntity;
import sk.iway.iwcm.components.news.templates.jpa.NewsTemplatesRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@RestController
@RequestMapping("/admin/rest/templates/news-templates")
@PreAuthorize("@WebjetSecurityService.hasPermission('components.news.edit_templates')")
@Datatable
public class NewsTemplatesRestController extends DatatableRestControllerV2<NewsTemplatesEntity, Long> {

    private final NewsTemplatesRepository repo;

    @Autowired
    public NewsTemplatesRestController(NewsTemplatesRepository repo) {
        super(repo, NewsTemplatesEntity.class);
        this.repo = repo;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, NewsTemplatesEntity> target, Identity user, Errors errors, Long id, NewsTemplatesEntity entity) {
        if("remove".equals(target.getAction()) == false && Tools.isNotEmpty(entity.getName())) {
            Optional<NewsTemplatesEntity> duplicityCheck = repo.findFirstByNameAndDomainId(entity.getName(), CloudToolsForCore.getDomainId());
            if(duplicityCheck.isPresent() && !duplicityCheck.get().getId().equals(entity.getId())) {
                errors.rejectValue("name", "", getProp().getText("components.news.templates.name.duplicity_err"));
            }
        }

        super.validateEditor(request, target, user, errors, id, entity);
    }

    @Override
    public void getOptions(DatatablePageImpl<NewsTemplatesEntity> page) {
        List<LabelValue> engines = List.of(
            new LabelValue(getProp().getText("components.news.templates.engine.velocity"), "velocity")
        );
        page.addOptions("engine", engines, "label", "value", false);
    }
}