package sk.iway.iwcm.system.elfinder;

import java.util.ArrayList;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;


@RestController
@RequestMapping("/admin/rest/elfinder/file-properties/")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_elfinder')")
@Datatable
public class FilePropertiesRestController extends DatatableRestControllerV2<FilePropertiesDTO, Long> {

    private HttpServletResponse response;
    
    @Autowired
    public FilePropertiesRestController(HttpServletResponse response) {
        super(null);
        this.response = response;
    }

    @Override
    public Page<FilePropertiesDTO> getAllItems(Pageable pageable) {
        return new DatatablePageImpl<>( new ArrayList<>() );
    }

    @Override
    public FilePropertiesDTO getOneItem(long id) {
        return FilePropertiesService.getOneItem(getRequest(), getUser());
    }

    @Override
    public void beforeSave(FilePropertiesDTO entity) {
        if(getUser().isFolderWritable(entity.getDirPath() + "/") == false)   
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    /* IT'S ALWAYS INSERT - because editor is always opened as create */
    @Override
    public FilePropertiesDTO insertItem(FilePropertiesDTO entity) {
        return FilePropertiesService.saveFile(entity, getRequest(), getUser());
    }

    @Override
    public void afterSave(FilePropertiesDTO entity, FilePropertiesDTO saved) {
        //refreshni zoznam v PathFilter
        PathFilter.reloadProtectedDirs();
    }

    @GetMapping("/fulltext-index")
    public void indexFolder(@RequestParam(required = true) String dir, @RequestParam(required = true) String file) {
        try {
            FilePropertiesService.indexFile(dir, file, getRequest(), response, getUser());
        } catch (Exception e) {
            Logger.error(e);
        }
    }
}
