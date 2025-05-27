package sk.iway.iwcm.system.elfinder;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@Datatable
@RequestMapping("/admin/rest/elfinder/file-usage")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuFbrowser')")
public class FileUsageRestController extends DatatableRestControllerV2<FileUsageDTO, Long> {

    @Autowired
    public FileUsageRestController() {
        super(null);
    }

    @Override
    public Page<FileUsageDTO> getAllItems(Pageable pageable) {
        String filePath = getRequest().getParameter("filePath");
        List<FileUsageDTO> entities = new ArrayList<>();

        if(Tools.isEmpty(filePath) == true) {
            return new DatatablePageImpl<>( new ArrayList<>() );
        } else {
            for(Column column : FileTools.getFileUsage(filePath, getUser())) {
                entities.add( new FileUsageDTO(column.getColumn1(), column.getColumn2(), column.getIntColumn1()) );
            }
        }

        return new DatatablePageImpl<>( entities );
    }
}
