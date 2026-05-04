package sk.iway.iwcm.doc.templates;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.doc.TemplatesGroupBean;
import sk.iway.iwcm.doc.TemplatesGroupDB;

/**
 * TemplateFilesController.java
 *
 * Class TemplateFilesController is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2019
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      5.2.2019 17:29
 * modified     5.2.2019 17:21
 */

@RestController
@RequestMapping("/admin/rest/templates/")
public class TemplateFilesController {

    @PreAuthorize("@WebjetSecurityService.hasPermission('menutemplates')")
    @GetMapping(path = "groupId/{templateGroupId}/tempId/{templateId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> listFilesFor(@PathVariable String templateGroupId, @PathVariable String templateId) {
        TemplateDetails templateDetails = TemplatesDB.getInstance().getTemplate(Tools.getIntValue(templateId, -1));
        TemplatesGroupBean templatesGroupBean = TemplatesGroupDB.getInstance().getById(Tools.getLongValue(templateGroupId, -1));

        if (templatesGroupBean == null)
            return ResponseEntity.status(404).body(new ArrayList<>());

        Set<String> abc = new LinkedHashSet<>();

        String str = "/templates";
        if (Tools.isNotEmpty(Constants.getString("installName")))
            str += "/" +  Constants.getString("installName");
        if (Tools.isNotEmpty(templatesGroupBean.getDirectory()) && !"/".equals(templatesGroupBean.getDirectory()))
            str += "/" + templatesGroupBean.getDirectory();

        File dir = new File(Tools.getRealPath(str));
        List<File> test = FileTools.listFilesByType(dir, ".jsp");
        for (File file : test) {
            abc.add(file.getAbsolutePath().substring(dir.getPath().length()+1));
        }
        // pridam aktualne nastavenu hodnotu
        if (templateDetails != null)
            abc.add(templateDetails.getForward());
        return ResponseEntity.ok(new ArrayList<>(abc));
    }
}
