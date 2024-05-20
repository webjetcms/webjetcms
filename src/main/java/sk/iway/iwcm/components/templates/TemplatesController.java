package sk.iway.iwcm.components.templates;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.LayoutService;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.template_groups.TemplateGroupsService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.TemplateDetailEditorFields;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerAvailableGroups;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/templates/temps-list")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuTemplates')")
public class TemplatesController extends DatatableRestControllerAvailableGroups<TemplateDetails, Long> {

    private final TemplateDetailsService templateDetailsService;
    private final TemplateGroupsService templateGroupsService;

    @Autowired
    public TemplatesController(TemplateDetailsService templateDetailsService, TemplateGroupsService templateGroupsService) {
        super(null, "tempId", "availableGroups");
        this.templateDetailsService = templateDetailsService;
        this.templateGroupsService = templateGroupsService;
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
        page.addOptions("templatesGroupId", templateGroupsService.getTemplateGroups(getUser()), "name", "templatesGroupId", false);
        page.addOptions("headerDocId,footerDocId", headerFooterDocs, "title", "docId", false);
        page.addOptions("menuDocId,rightMenuDocId", menuDocs, "title", "docId", false);
        page.addOptions("objectADocId,objectBDocId,objectCDocId,objectDDocId", ws.addEmptyDoc(headerFooterMenuDocs, -1), "title", "docId", false);

        List<LabelValueDetails> inlineEditingModes = new ArrayList<>();
        inlineEditingModes.add(new LabelValueDetails(getProp().getText("template.inline_editing_mode.byTemplateGroup"), ""));
        for (LabelValueDetails lvd : templateGroupsService.getInlineEditors(getProp())) {
            String mode = lvd.getValue();
            if (Tools.isEmpty(mode)) mode="default"; //we must distinguish between byTemplateGroup and default
            inlineEditingModes.add(new LabelValueDetails(lvd.getLabel(), mode));
        }
        page.addOptions("inlineEditingMode", inlineEditingModes, "label", "value", false);
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

    @Override
    public void beforeSave(TemplateDetails entity) {
        //in multiweb set at least root group as available group for template
        if (InitServlet.isTypeCloud() && CloudToolsForCore.isControllerDomain()==false && "cloud".equals(Constants.getInstallName())==false) {
      	    if (Tools.isEmpty(entity.getAvailableGroups())) entity.setAvailableGroups(CloudToolsForCore.getRootGroupIds());
        }
    }

    @Override
    public TemplateDetails getOneItem(long id) {
        TemplateDetails old = TemplatesDB.getInstance().getTemplate((int)id);

        List<TemplateDetails> list = new ArrayList<>();
        list.add(old);
        list = templateDetailsService.filterByCurrentDomainAndUser(getUser(), list);
        if (list.isEmpty()) return null;

        return old;
    }

    @GetMapping("/autocomplete")
    public List<String> getAutocomplete(@RequestParam String term, @RequestParam(required = false, name="DTE_Field_templatesGroupId") Integer templatesGroupId, @RequestParam(required = false, name="DTE_Field_templateInstallName") String installName) {
        List<String> forwards = templateDetailsService.getTemplateForwards(installName, templatesGroupId, term);
        return forwards;
    }
}
