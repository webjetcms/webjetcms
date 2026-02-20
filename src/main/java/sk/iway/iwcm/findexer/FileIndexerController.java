package sk.iway.iwcm.findexer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuFbrowser')")
public class FileIndexerController {

    @GetMapping("/admin/fbrowser/fulltext-index/index/")
    @ResponseBody
    public void indexFileOrFolder(@RequestParam(required = false) String file, @RequestParam(required = false) String dir, HttpServletRequest request, HttpServletResponse response) {
        try {
            FileIndexer.indexFileOrFolder(file, dir, request, response);
        } catch(Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }
}
