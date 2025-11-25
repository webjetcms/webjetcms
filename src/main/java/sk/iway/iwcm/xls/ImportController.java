package sk.iway.iwcm.xls;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.editor.rest.WebPagesListener;
import sk.iway.iwcm.system.spring.SpringUrlMapping;
import sk.iway.iwcm.users.UsersDB;

/**
 * Universal controller for importing data from XLS files.
 * See example in /admin/spec/import_xls.jsp file, your import class is send as URL parameter type.
 * Your excel file must have first row with column names, and class must extend ExcelImportJXL class.
 * See example in ImportStructureExcel.
 */
@Controller
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class ImportController {

    @PostMapping("/admin/import/excel/")
    @ResponseBody
    public void importFromExcel(@RequestParam("file") MultipartFile file, HttpServletRequest request, HttpServletResponse response) {
        try {
            Identity user = UsersDB.getCurrentUser(request);
            if (user == null || user.isAdmin()==false) {
                SpringUrlMapping.redirectToLogon(response);
                return;
            }

            ImportService.importFromExcel(file, request, response);
        } catch(Exception ex) {
            Logger.error(WebPagesListener.class, ex);
        }
    }
}
