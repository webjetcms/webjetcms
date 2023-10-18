package sk.iway.iwcm.components.forms.archive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.forms.FormColumns;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableResponse;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/forms/archive-list")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_form')")
public class FormsArchiveController extends DatatableRestControllerV2<FormsArchiveEntity, Long> {

    private final FormsArchiveServiceImpl formsService;

    @Autowired
    public FormsArchiveController(FormsArchiveRepository formsRepository, FormsArchiveServiceImpl formsService) {
        super(formsRepository);
        this.formsService = formsService;
    }

    @Override
    public Page<FormsArchiveEntity> getAllItems(Pageable pageable) {
        Page<FormsArchiveEntity> page = new DatatablePageImpl<>(formsService.getFormsList(getUser()));
        return page;
    }

    @GetMapping(path = "/columns/{formName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public FormColumns getColumnNames(@PathVariable String formName) {
        return formsService.getColumnNames(formName, getUser());
    }

    @GetMapping(path = "/data/{formName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<FormsArchiveEntity> getSubList(@PathVariable String formName, @RequestParam Map<String, String> params, Pageable pageable) {
        Page<FormsArchiveEntity> data;
        if (getRequest().getParameter("size")==null) data = formsService.findInDataByColumns(formName, getUser(), params, null);
        else data = formsService.findInDataByColumns(formName, getUser(), params, pageable);

        if ("true".equals(getRequest().getParameter("export"))) {
            formsService.setExportDate(data.getContent());
        }

        return data;
    }

    @GetMapping(path = "/data/{formName}/search/findByColumns")
    public Page<FormsArchiveEntity> findInDataByColumns(@PathVariable String formName, @RequestParam Map<String, String> params, Pageable pageable, FormsArchiveEntity search) {

        Page<FormsArchiveEntity> data = formsService.findInDataByColumns(formName, getUser(), params, pageable);

        if ("true".equals(getRequest().getParameter("export"))) {
            formsService.setExportDate(data.getContent());
        }

        return data;
    }

    @PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
	@PostMapping(value = "/data/{formName}/editor", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DatatableResponse<FormsArchiveEntity>> handleEditorFormDetail(@PathVariable String formName, HttpServletRequest request, @RequestBody DatatableRequest<Long, FormsArchiveEntity> datatableRequest) {
        return super.handleEditor(request, datatableRequest);
    }

    @GetMapping(path="/html")
    public String getHtml(@RequestParam long id) {
        FormsArchiveEntity entity = formsService.getById(id);
        if (entity == null || Tools.isEmpty(entity.getFormName())) return null;

        if (formsService.isFormAccessible(entity.getFormName(), getUser())==false) return null;

        //html kod necitame v entite, musime ziskat takto
        String html = (new SimpleQuery()).forString("SELECT html FROM forms_archive WHERE id=?", id);
        return html;
    }

    @Override
    public FormsArchiveEntity editItem(FormsArchiveEntity entity, long id) {
        String note = entity.getNote();

        formsService.updateNote(note, id);

        return formsService.getById(id);
    }

    @Override
    public boolean deleteItem(FormsArchiveEntity entity, long id) {
        return formsService.deleteItem(entity, id);
    }

}
