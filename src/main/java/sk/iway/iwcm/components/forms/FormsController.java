package sk.iway.iwcm.components.forms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.CryptoFactory;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.form.FormDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableResponse;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.json.LabelValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/forms-list")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_form')")
public class FormsController extends DatatableRestControllerV2<FormsEntity, Long> {

    private final FormsServiceImpl formsService;

    @Autowired
    public FormsController(FormsRepository formsRepository, FormsServiceImpl formsService) {
        super(formsRepository);
        this.formsService = formsService;
    }

    @Override
    public Page<FormsEntity> getAllItems(Pageable pageable) {
        Page<FormsEntity> page = new DatatablePageImpl<>(formsService.getFormsList(getUser()));
        return page;
    }

    @Override
    public FormsEntity getOneItem(long id) {
        FormsEntity entity = formsService.getById(id);
        if (entity == null) return null;
        if (formsService.isFormAccessible(entity.getFormName(), getUser())) return entity;
        return null;
    }

    @GetMapping(path = "/columns/{formName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public FormColumns getColumnNames(@PathVariable String formName) {
        return formsService.getColumnNames(formName, getUser());
    }

    @GetMapping(path = "/data/{formName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<FormsEntity> getSubList(@PathVariable String formName, @RequestParam Map<String, String> params, Pageable pageable) {
        Page<FormsEntity> data;
        if (getRequest().getParameter("size")==null) data = formsService.findInDataByColumns(formName, getUser(), params, null);
        else data = formsService.findInDataByColumns(formName, getUser(), params, pageable);

        if ("true".equals(getRequest().getParameter("export"))) {
            formsService.setExportDate(data.getContent());
        }

        return data;
    }

    @GetMapping(path = "/data/{formName}/search/findByColumns")
    public Page<FormsEntity> findInDataByColumns(@PathVariable String formName, @RequestParam Map<String, String> params, Pageable pageable, FormsEntity search) {

        Page<FormsEntity> data = formsService.findInDataByColumns(formName, getUser(), params, pageable);

        if ("true".equals(getRequest().getParameter("export"))) {
            formsService.setExportDate(data.getContent());
        }

        return data;
    }

    @PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
	@PostMapping(value = "/data/{formName}/editor", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DatatableResponse<FormsEntity>> handleEditorFormDetail(@PathVariable String formName, HttpServletRequest request, @RequestBody DatatableRequest<Long, FormsEntity> datatableRequest) {
        return super.handleEditor(request, datatableRequest);
    }

    @PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
	@PostMapping(value = "/data/{formName}/action/{action}")
    @Override
	public ResponseEntity<DatatableResponse<FormsEntity>> action(@PathVariable String action, @RequestParam(value = "ids[]") Long[] ids) {
        return super.action(action, ids);
    }

    /**
     * Get all regular expressions.
     * Available to all admins (it's used on variety of apps like webpages, news, etc)
     */
    @GetMapping(path="/regexps")
    @PreAuthorize(value = "@WebjetSecurityService.isAdmin()")
    public List<LabelValue> getAllRegularExpression() {
        Prop prop = getProp();
        List<LabelValue> regexps = new ArrayList<>();
        List<String[]> all = FormDB.getInstance().getAllRegularExpression();
        for (String[] regexp : all) {
            regexps.add(new LabelValue(prop.getText(regexp[0]), regexp[1]));
        }
        return regexps;
    }

    @GetMapping(path="/html")
    public String getHtml(@RequestParam long id) {
        FormsEntity entity = formsService.getById(id);
        if (entity == null || Tools.isEmpty(entity.getFormName())) return null;

        if (formsService.isFormAccessible(entity.getFormName(), getUser())==false) return null;

        //html kod necitame v entite, musime ziskat takto
        String html = (new SimpleQuery()).forString("SELECT html FROM forms WHERE id=?", id);
        html = CryptoFactory.decrypt(html);
        if (html.contains("<body")==false) {
            html = Tools.replace(html, "\n", "\n<br/>");
        }
        return html;
    }

    @Override
    public FormsEntity editItem(FormsEntity entity, long id) {
        String note = entity.getNote();

        formsService.updateNote(note, id);

        return formsService.getById(id);
    }

    @Override
    public boolean deleteItem(FormsEntity entity, long id) {
        return formsService.deleteItem(entity, id);
    }

    @Override
    public boolean processAction(FormsEntity entity, String action) {
        String formName = entity.getFormName();
        if ("archiveForm".equals(action)) {
            boolean success = FormDB.setFormName(formName, "Archiv-"+formName);
            Adminlog.add(Adminlog.TYPE_FORM_ARCHIVE, "Archivacia formularu: "+formName, -1, -1);
            return success;
        } else if ("archiveFormDetail".equals(action) && entity.getId()!=null) {
            String idsQuery = String.valueOf(entity.getId());
            int smallestId = entity.getId().intValue();
            boolean success = FormDB.setFormName(formName, "Archiv-"+formName, (" AND id IN (" + idsQuery + ")"), smallestId, false);
			Adminlog.add(Adminlog.TYPE_FORM_ARCHIVE, "Archivacia formularu: "+formName, -1, -1);
            return success;
        }
        return false;
    }

    @Override
    public boolean checkItemPerms(FormsEntity entity, Long id) {
        if (InitServlet.isTypeCloud()) {
            if (entity.getDomainId()!=CloudToolsForCore.getDomainId()) return false;
            FormsEntity old = getRepo().getById(entity.getId());
            if (old != null && old.getDomainId()!=CloudToolsForCore.getDomainId()) return false;
        }
        return true;
    }
}
