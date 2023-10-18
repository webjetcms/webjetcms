package sk.iway.iwcm.components.templates;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.admin.layout.LayoutService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.TemplateDetailEditorFields;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/templates/temps-list")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuTemplates')")
public class TemplatesController extends DatatableRestControllerV2<TemplateDetails, Long> {

    private final TemplateDetailsService templateDetailsService;

    @Autowired
    public TemplatesController(TemplateDetailsService templateDetailsService) {
        super(null);
        this.templateDetailsService = templateDetailsService;
    }

    @Override
    public Page<TemplateDetails> getAllItems(Pageable pageable) {
        DatatablePageImpl<TemplateDetails> page = new DatatablePageImpl<>(templateDetailsService.getAllTemplateDetails(getUser()));
        LayoutService ls = new LayoutService(getRequest());
        WebpagesService ws = new WebpagesService(-1, getUser(), getProp(), getRequest());
        List<DocDetails> headerFooterDocs = ws.addEmptyDoc(ws.getHeaderList(false), -1);
        List<DocDetails> menuDocs = ws.addEmptyDoc(ws.getMenuList(false), -1);

        List<DocDetails> headerFooterMenuDocs = ws.getHeaderFooterMenuList(false);

        for (TemplateDetails temp : page.getContent()) {
            checkDocInList(headerFooterDocs, temp.getHeaderDocId());
            checkDocInList(headerFooterDocs, temp.getFooterDocId());

            checkDocInList(menuDocs, temp.getMenuDocId());
            checkDocInList(menuDocs, temp.getRightMenuDocId());

            checkDocInList(headerFooterMenuDocs, temp.getObjectADocId());
            checkDocInList(headerFooterMenuDocs, temp.getObjectBDocId());
            checkDocInList(headerFooterMenuDocs, temp.getObjectCDocId());
            checkDocInList(headerFooterMenuDocs, temp.getObjectDDocId());

            //Init editor fields
            if(temp.getEditorFields() == null) temp.setEditorFields(new TemplateDetailEditorFields());
        }

        page.addOption("editorFields.mergeToTempId", "", "-1", null);
        page.addOptions("editorFields.mergeToTempId", page.getContent(), "tempName", "tempId", false);

        page.addOptions("lng", ls.getLanguages(false, true), "label", "value", false);
        page.addOptions("templatesGroupId", templateDetailsService.getTemplatesGroupBeans(), "name", "templatesGroupId", false);
        page.addOptions("headerDocId,footerDocId", headerFooterDocs, "title", "docId", false);
        page.addOptions("menuDocId,rightMenuDocId", menuDocs, "title", "docId", false);
        page.addOptions("objectADocId,objectBDocId,objectCDocId,objectDDocId", ws.addEmptyDoc(headerFooterMenuDocs, -1), "title", "docId", false);
        return page;
    }

    /**
     * Overi ci v zozname je dane docId, ak nie, prida (aby sa nestalo, ze nemame vo vyberovom poli danu stranku)
     * @param list
     * @param docId
     */
    private void checkDocInList(List<DocDetails> list, int docId)
    {
        if (docId<1) return;

        boolean docFound = false;
        for (DocDetails doc : list)
        {
            if (doc.getDocId()==docId)
            {
                docFound = true;
                break;
            }
        }

        if (docFound == false)
        {
            DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
            if (doc != null)
            {
                list.add(0, doc);
            }
        }
    }

    @Override
    public TemplateDetails insertItem(TemplateDetails templateDetails) {
        if (null == templateDetails) {
            throw new IllegalArgumentException("TemplateDetailsDto cannot be null.");
        }
        //reload options in editor
        setForceReload(true);

        return templateDetailsService.insertTemplateDetail(templateDetails);
    }

    //
    @Override
    public TemplateDetails editItem(TemplateDetails templateDetails, long id) {

        //Check if we want merge templates
        TemplateDetailEditorFields ef = templateDetails.getEditorFields();
        if(id > 0 && ef != null && ef.getMergeToTempId() > 0 && ef.isMergeTemplates()) {
            //Check loop error
            if(id == ef.getMergeToTempId()) throw new IllegalArgumentException(getProp().getText("template.temps-list.loop_error"));

            templateDetailsService.mergeTemplate(id, ef.getMergeToTempId());
            //Refresh
            setForceReload(true);
            return null;
        }

        return templateDetailsService.editTemplateDetail(templateDetails, id);
    }

    @Override
    public boolean deleteItem(TemplateDetails entity, long id) {
        boolean deleted = templateDetailsService.deleteTemplateDetails(id);
        return deleted;
    }

    @GetMapping("/autocomplete")
    public List<String> getAutocomplete(@RequestParam String term, @RequestParam(required = false, name="DTE_Field_templatesGroupId") Integer templatesGroupId, @RequestParam(required = false, name="DTE_Field_templateInstallName") String installName) {
        List<String> forwards = templateDetailsService.getTemplateForwards(installName, templatesGroupId, term);
        return forwards;
    }
}
