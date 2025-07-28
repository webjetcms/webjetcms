package sk.iway.iwcm.components.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.perex_groups.PerexGroupsEntity;
import sk.iway.iwcm.components.perex_groups.PerexGroupsRepository;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerAvailableGroups;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/export-dat")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_export')")
public class ExportDatRestController extends DatatableRestControllerAvailableGroups<ExportDatBean, Long> {

    private final ExportDatRepository exportDatBeanRepository;
    private final PerexGroupsRepository perexGroupsRepository;

    @Autowired
    public ExportDatRestController(ExportDatRepository exportDatBeanRepository, PerexGroupsRepository perexGroupsRepository) {
        super(exportDatBeanRepository, "id", "groupIds");
        this.exportDatBeanRepository = exportDatBeanRepository;
        this.perexGroupsRepository = perexGroupsRepository;
    }

    @Override
    public Page<ExportDatBean> getAllItems(Pageable pageable) {

        DatatablePageImpl<ExportDatBean> page;

        page = new DatatablePageImpl<>(filterByPerms(exportDatBeanRepository.findAll()));

        processFromEntity(page, ProcessItemAction.GETALL);

        List<PerexGroupsEntity> perexList = perexGroupsRepository.findAllByDomainIdOrderByPerexGroupNameAsc(CloudToolsForCore.getDomainId());

        page.addOptions("editorFields.perexGroupsIds", perexList, "perexGroupName", "id", false);
        page.addOptions("format", getFormatOptions(), null, null, false);

        return page;
    }

    @Override
    public ExportDatBean processFromEntity(ExportDatBean entity, ProcessItemAction action) {

        if (entity == null) entity = new ExportDatBean();

        if(entity.getEditorFields() == null) {
            ExportDatEditorFields edbef = new ExportDatEditorFields();
            edbef.fromExportDatBean(entity);
            entity.setEditorFields(edbef);
        }

        return entity;
    }

    @Override
    public ExportDatBean processToEntity(ExportDatBean entity, ProcessItemAction action) {
        if(entity != null) {
            //
            ExportDatEditorFields edbef = new ExportDatEditorFields();
            edbef.toExportDatBean(entity);
        }
        return entity;
    }

    /**
     * Returns list of available export JSP files (export formats)
     * @return
     */
    private List<String> getFormatOptions() {
        List<String> formats = new ArrayList<>();
        IwcmFile dir = new IwcmFile(Tools.getRealPath("/components/export/"));
        List<IwcmFile> files = new ArrayList<>();
        Collections.addAll(files, dir.listFiles());
        IwcmFile customDir = new IwcmFile(Tools.getRealPath("/components/"+Constants.getInstallName()+"/export/"));
        if (customDir.exists() && customDir.isDirectory()) {
            Collections.addAll(files, customDir.listFiles());
        }
        for (IwcmFile file : FileTools.sortFilesByName(files)) {
            if (file.isFile()==false || "jsp".equals(FileTools.getFileExtension(file.getName()))==false) continue;
            String name = file.getName();
            int i = name.lastIndexOf(".");
            if (i > 0) name = name.substring(0, i);
            if (formats.contains(name)) continue;
            formats.add(name);
        }
        return formats;
    }
}
