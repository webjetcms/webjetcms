package sk.iway.iwcm.components.forms;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.archive.FormsArchiveRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.components.multistep_form.rest.FormConditionsHandler;
import sk.iway.iwcm.components.multistep_form.rest.MultistepFormsService;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;

/** Deletes form data and removes shared definitions only when no remaining use requires them. */
@Service
public class FormsDeletionService {

    private static final Pattern FORM_INCLUDE = Pattern.compile(
        "!INCLUDE\\(\\s*(sk\\.iway\\.iwcm\\.components\\.multistep_form\\.mvc\\.MultistepFormApp|"
        + "/components/formsimple/form\\.jsp|sk\\.iway\\.iwcm\\.components\\.formsimple\\.FormSimpleApp)\\s*,(.*?)\\)!",
        Pattern.DOTALL);

    private final FormsRepository forms;
    private final FormsArchiveRepository archive;
    private final FormSettingsRepository settings;
    private final FormStepsRepository steps;
    private final FormItemsRepository items;
    private final FormItemsConditionsRepository conditions;
    private final DocDetailsRepository documents;

    public FormsDeletionService(FormsRepository forms, FormsArchiveRepository archive, FormSettingsRepository settings,
            FormStepsRepository steps, FormItemsRepository items, FormItemsConditionsRepository conditions,
            DocDetailsRepository documents) {
        this.forms = forms;
        this.archive = archive;
        this.settings = settings;
        this.steps = steps;
        this.items = items;
        this.conditions = conditions;
        this.documents = documents;
    }

    /**
     * Deletes a response or the data selected by a management record in the current domain.
     * Active management records survive while an archive or available page requires their definition.
     * Responses are deleted first; management records remain available for retry if cleanup fails.
     *
     * @param id stored record identifier; submitted names are deliberately ignored
     * @param archived whether the selected record belongs to the archive
     * @return deletion outcome and the reasons for retaining the definition
     */
    public DeletionResult delete(long id, boolean archived) {
        if (archived) return delete(id, archive, true);
        return delete(id, forms, false);
    }

    private <E extends FormsEntityBasic> DeletionResult delete(long id, FormsRepositoryInterface<E> repository, boolean archived) {
        int domainId = CloudToolsForCore.getDomainId();
        E stored = repository.findFirstByIdAndDomainId(id, domainId).orElse(null);
        if (stored == null || stored.getDomainId() != domainId)
            return new DeletionResult(false, false, false, false, false, List.of(), false);

        if (stored.getCreateDate() != null) {
            repository.deleteById(id);
            return new DeletionResult(true, false, false, false, false, List.of(), false);
        }

        String formName = stored.getFormName();
        List<Integer> pageIds = findUsingPages(formName, domainId);
        boolean activeData = archived && forms.countAllByFormNameAndDomainId(formName, domainId) > 0;
        boolean archivedData = !archived && archive.countAllByFormNameAndDomainId(formName, domainId) > 0;
        boolean preserve = activeData || archivedData || !pageIds.isEmpty();

        repository.deleteAllByFormNameAndDomainIdAndCreateDateNotNull(formName, domainId);
        boolean settingsDeleted = false;
        try {
            if (!preserve) {
                try {
                    conditions.deleteAllByFormNameAndDomainId(formName, domainId);
                    items.deleteAllByFormNameAndDomainId(formName, domainId);
                    steps.deleteAllByFormNameAndDomainId(formName, domainId);
                    FormConditionsHandler.deleteFormConditions(formName, domainId);
                    settings.deleteByFormNameAndDomainId(formName, domainId);
                    settingsDeleted = true;
                } finally {
                    MultistepFormsService.clearValidationFieldsCache(formName, domainId);
                }
            }
            if (archived || !preserve)
                repository.deleteAllByFormNameAndDomainIdAndCreateDateNull(formName, domainId);
        } catch (RuntimeException e) {
            Logger.error(getClass(), e);
            return new DeletionResult(true, true, settingsDeleted, activeData, archivedData, pageIds, true);
        }

        return new DeletionResult(true, true, settingsDeleted, activeData, archivedData, pageIds, false);
    }

    /** Finds available pages in the form's persistence domain, including pages the editor cannot edit. */
    List<Integer> findUsingPages(String formName, int domainId) {
        List<DocDetails> candidates = documents.findAll((root, query, builder) -> builder.and(
            builder.isTrue(root.get("available")),
            builder.or(builder.like(root.get("data"), "%MultistepFormApp%"),
                builder.like(root.get("data"), "%formsimple%"), builder.like(root.get("data"), "%savedb%"))));
        boolean partitioned = InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir");
        List<Integer> pageIds = new ArrayList<>();
        for (DocDetails page : candidates) {
            if (!page.isAvailable()) continue;
            if (partitioned && GroupsDB.getDomainId(GroupsDB.getInstance().getDomain(page.getGroupId())) != domainId) continue;
            if (usesForm(page.getData(), formName)) pageIds.add(page.getDocId());
        }
        return pageIds.stream().distinct().sorted().toList();
    }

    /** Matches complete component parameters or legacy savedb fields rather than name substrings. */
    static boolean usesForm(String html, String formName) {
        if (html == null || formName == null) return false;
        Matcher matcher = FORM_INCLUDE.matcher(html);
        while (matcher.find()) {
            String parameters = StringEscapeUtils.unescapeHtml4(matcher.group(2)).replace("\r", "").replace("\n", "");
            PageParams params = new PageParams(parameters);
            String defaultName = matcher.group(1).contains("formsimple") ? "form-simple" : "";
            if (formName.equals(params.getValue("formName", defaultName))) return true;
        }
        for (Element input : Jsoup.parse(html).select("input[name=savedb]")) {
            if (formName.equals(input.val())) return true;
        }
        return false;
    }

    /** Describes deleted responses, retained definitions, and cleanup failures that can be retried. */
    public record DeletionResult(boolean deleted, boolean definition, boolean settingsDeleted,
            boolean activeData, boolean archivedData, List<Integer> pageIds, boolean cleanupFailed) {

        /** Creates the localized notification for a management-record deletion. */
        public NotifyBean notification(Prop prop) {
            if (!deleted || !definition) return null;
            if (cleanupFailed) return new NotifyBean(prop.getText("components.forms.delete.data"),
                prop.getText("components.forms.delete.incomplete"), NotifyType.WARNING);
            if (settingsDeleted) return new NotifyBean(prop.getText("components.forms.delete.complete"), "", NotifyType.SUCCESS);

            List<String> reasons = new ArrayList<>();
            if (!pageIds.isEmpty()) reasons.add(prop.getTextWithParams("components.forms.delete.pages",
                pageIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", "))));
            if (archivedData) reasons.add(prop.getText("components.forms.delete.archive"));
            if (activeData) reasons.add(prop.getText("components.forms.delete.active"));
            return new NotifyBean(prop.getText("components.forms.delete.data"), String.join("<br>", reasons), NotifyType.INFO);
        }
    }
}
