package sk.iway.iwcm.system.elfinder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@Datatable
@RequestMapping("/admin/rest/elfinder/unused-files")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuFbrowser')")
public class UnusedFilesRestController extends DatatableRestControllerV2<UnusedFileDTO, Long> {

    private static final String DELETE_OPERATION_ATTRIBUTE = UnusedFilesRestController.class.getName() + ".deleteOperation";
    private static final String DELETE_OPERATION_ERROR_ATTRIBUTE = DELETE_OPERATION_ATTRIBUTE + ".error";

    private final UnusedFilesService unusedFilesService;

    @Autowired
    public UnusedFilesRestController(UnusedFilesService unusedFilesService) {
        super(null);
        this.unusedFilesService = unusedFilesService;
    }

    @Override
    public Page<UnusedFileDTO> getAllItems(Pageable pageable) {
        String taskId = getRequest().getParameter("taskId");
        if (Tools.isEmpty(taskId)) {
            return new DatatablePageImpl<>(Collections.emptyList());
        }

        List<UnusedFileDTO> files = unusedFilesService.getResults(taskId, getUser());
        return new DatatablePageImpl<>(files);
    }

    @Override
    public boolean processAction(UnusedFileDTO entity, String action) {
        if ("start_analyze".equals(action)) {
            JSONObject customData = new JSONObject(getRequest().getParameter("customData"));
            unusedFilesService.startScan(
                customData.optString("taskId"),
                customData.optString("dir"),
                customData.optBoolean("includeSubfolders", false),
                getUser(),
                SetCharacterEncodingFilter.getCurrentRequestBean()
            );
            return true;
        }
        return false;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, UnusedFileDTO> target,
            Identity user, Errors errors, Long id, UnusedFileDTO entity) {
        if (target.isDelete() == false || request.getAttribute(DELETE_OPERATION_ATTRIBUTE) != null ||
            request.getAttribute(DELETE_OPERATION_ERROR_ATTRIBUTE) != null) {
            return;
        }

        try {
            prepareDeleteOperation(request, target.getData().values(), user);
        } catch (ResponseStatusException ex) {
            request.setAttribute(DELETE_OPERATION_ERROR_ATTRIBUTE, Boolean.TRUE);
            String message = ex.getReason();
            if (Tools.isEmpty(message)) {
                message = Prop.getInstance(request).getText("elfinder.folder_prop.unused_files.delete_invalid");
            }
            ((BindingResult) errors).addError(new ObjectError("global", message));
        }
    }

    @Override
    public boolean beforeDelete(UnusedFileDTO entity) {
        if (getRequest().getAttribute(DELETE_OPERATION_ATTRIBUTE) == null) {
            prepareDeleteOperation(getRequest(), List.of(entity), getUser());
        }
        return true;
    }

    @Override
    public boolean deleteItem(UnusedFileDTO entity, long id) {
        return entity != null && unusedFilesService.deleteFile(entity.getFullPath(), getUser());
    }

    @GetMapping("/status")
    public UnusedFilesTaskDTO status(@RequestParam(required = true) String taskId) {
        return unusedFilesService.getStatus(taskId, getUser());
    }

    private void prepareDeleteOperation(HttpServletRequest request, Collection<UnusedFileDTO> files, Identity user) {
        if (request.getAttribute(DELETE_OPERATION_ATTRIBUTE) != null) {
            return;
        }

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            throw new IllegalStateException("Request attributes are not available");
        }

        UnusedFilesService.DeleteOperation operation = unusedFilesService.acquireDeleteOperation(files, user);
        try {
            request.setAttribute(DELETE_OPERATION_ATTRIBUTE, operation);
            requestAttributes.registerDestructionCallback(
                DELETE_OPERATION_ATTRIBUTE,
                operation::close,
                RequestAttributes.SCOPE_REQUEST
            );
        } catch (RuntimeException ex) {
            request.removeAttribute(DELETE_OPERATION_ATTRIBUTE);
            operation.close();
            throw ex;
        }
    }
}
