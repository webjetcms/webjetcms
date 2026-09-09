package sk.iway.iwcm.components.forms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.CryptoFactory;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.components.enumerations.EnumerationDataDB;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.components.multistep_form.rest.MultistepFormsService;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.form.FormMailAction;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.system.spring.SpringUrlMapping;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.utils.Pair;

/**
 * Provides form administration, submission searching, access control, export, and rendering operations.
 *
 * The service supports repository implementations for both form definitions and submitted records while
 * consistently restricting data to the current domain and the pages editable by the current user.
 *
 * @param <R> repository type used to access form entities
 * @param <E> form entity type handled by the repository
 */
public class FormsService<R extends FormsRepositoryInterface<E>, E extends FormsEntityBasic> {

    /**
     * Identifies the supported form layouts and their persisted values.
     */
    public enum FORM_TYPE {
        SIMPLE("simple"),
        MULTISTEP("multistep"),
        BASIC("basic"),
        UNKNOWN("unknown");

        private final String value;
        private static final String PREFIX = "components.form.form_type.";

        FORM_TYPE(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static List<String> getAllValues() {
            return Arrays.stream(FORM_TYPE.values()).map(FORM_TYPE::value).collect(Collectors.toList());
        }

        public static List<LabelValue> getSelectOptions(Prop prop) {
            List<String> values = getAllValues();
            List<LabelValue> options = new ArrayList<>();
            for (String v : values) options.add(new LabelValue(prop.getText(PREFIX + v), v));
            return options;
        }
    }

    private final R formsRepository;
    private final FormSettingsRepository formSettingsRepository;
    private final FormStepsRepository formStepsRepository;
    private final FormItemsRepository formItemsRepository;

    /**
     * Resolves the selected form name when the request displays form submission details.
     *
     * @param request  request containing the detail flag and form name
     * @return selected form name, or {@code null} when detail mode is disabled
     */
    public String getFormName(HttpServletRequest request) {
        if(Tools.getBooleanValue(request.getParameter("detail"), false))
            return Tools.getStringValue(request.getParameter("formName"), null);
        return null;
    }

    public boolean isExport(HttpServletRequest request) { return "true".equals(request.getParameter("export")); }

    public FormsService(R formsRepository, FormSettingsRepository formSettingsRepository, FormStepsRepository formStepsRepository, FormItemsRepository formItemsRepository) {
        this.formsRepository = formsRepository;
        this.formSettingsRepository = formSettingsRepository;
        this.formStepsRepository = formStepsRepository;
        this.formItemsRepository = formItemsRepository;
    }

    /**
     * Returns either form definitions or submissions of the form selected by the request.
     *
     * Submission exports also update the last-export date of the returned records.
     *
     * @param page  page supplied by the DataTable request pipeline
     * @param pageable  requested pagination and sorting
     * @param request  request that selects detail and export modes
     * @param user  user whose editable pages determine form access
     * @return page of form definitions or submissions, or {@code null} when access is denied
     */
    public Page<E> getAllItems(Page<E> page, Pageable pageable, HttpServletRequest request, Identity user) {
        String formName = getFormName(request);

        if(formName != null) {
            if (request.getParameter("size")==null) page = findInDataByColumns(formName, user, new HashMap<>(), null);
            else page = findInDataByColumns(formName, user, new HashMap<>(), pageable);

            if(page == null) return null;

            if (isExport(request)) setExportDate(page.getContent());
        } else page = new DatatablePageImpl<>(getFormsList(user));

        return page;
    }

    /**
     * Searches submissions of the form selected by the request using DataTable filter parameters.
     *
     * Request parameters are merged into the supplied parameter map before the repository query is built.
     *
     * @param params  mutable map that receives request parameters used for filtering
     * @param pageable  requested pagination and sorting
     * @param search  search entity supplied by the DataTable pipeline
     * @param request  request that selects the form and export mode
     * @param user  user whose editable pages determine form access
     * @return matching submissions, or {@code null} when no form is selected or access is denied
     */
    public Page<E> findByColumns(Map<String, String> params, Pageable pageable, E search, HttpServletRequest request, Identity user) {
        String formName = getFormName(request);
        if(formName != null) {
            java.util.Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String parameterName = parameterNames.nextElement();
                Object value = request.getParameter(parameterName);
                if(value != null) params.put(parameterName, String.valueOf(value));
            }

            Page<E> data = findInDataByColumns(formName, user, params, pageable);
            if (isExport(request)) setExportDate(data.getContent());
            return data;
        }

        return null;
    }

    /**
     * Enriches a form definition with its submission count and latest submission metadata.
     *
     * @param entity  form definition to enrich
     * @param domainId  domain containing the form and its submissions
     */
    public void prepareForm(E entity, int domainId) {
        entity.setCount(formsRepository.countAllByFormNameAndDomainId(entity.getFormName(), domainId) - 1);
        E lastOne = formsRepository.findTopByFormNameAndDomainIdAndCreateDateNotNullOrderByCreateDateDesc(entity.getFormName(), domainId);
        if (lastOne != null) {
            entity.setCreateDate(lastOne.getCreateDate());
            entity.setDocId(lastOne.getDocId());
        }
    }

    /**
     * Returns distinct form definitions that the user is allowed to manage.
     *
     * @param user  user whose editable pages and groups determine form access
     * @return accessible form definitions in the current domain
     */
    public List<E> getFormsList(UserDetails user) {
        Integer domainId = CloudToolsForCore.getDomainId();
        List<E> formsEntities = formsRepository.findAllByCreateDateIsNullAndDomainId(domainId);
        for (E entity : formsEntities) prepareForm(entity, domainId);
        formsEntities = filterDistinct(formsEntities);
        return filterFormsByUser(user, formsEntities);
    }

    /**
     * Removes duplicate form definitions that share the same name.
     *
     * Duplicate management records can exist when forms were populated incorrectly by application code.
     *
     * @param allForms  form definitions to filter
     * @return form definitions containing only the first occurrence of each name
     */
    private List<E> filterDistinct(List<E> allForms) {
        List<E> ret = new ArrayList<>();
        Set<String> distinct = new HashSet<>();
        for (E f : allForms) {
            String key = f.getFormName();
            if (distinct.contains(key)==false) {
                ret.add(f);
                distinct.add(key);
            }
        }

        return ret;
    }

    /**
	 * Filters forms by the user's permission to edit their associated page or directory.
	 *
	 * @param user  user whose editable pages and groups are evaluated
	 * @param allForms  forms to filter
	 * @return forms accessible to the user
	 */
	private List<E> filterFormsByUser(UserDetails user, List<E> allForms) {
		List<E> ret = new ArrayList<>(allForms.size());

		GroupsDB groupsDB = GroupsDB.getInstance();

		int[] userEditableGroups = groupsDB.expandGroupIdsToChilds(Tools.getTokensInt(user.getEditableGroups(), ","), true);
		int[] userEditablePages = Tools.getTokensInt(user.getEditablePages(), ",");
		if ((userEditableGroups == null || userEditableGroups.length<1) && (userEditablePages==null || userEditablePages.length<1)) return allForms;

		DocDB docDB = DocDB.getInstance();
		for (E form : allForms)
		{
			boolean pridaj = isFormAccessible(form, userEditableGroups, userEditablePages, docDB);
			if (pridaj) ret.add(form);
		}

		return ret;
    }

    /**
     * Checks whether the user can manage a form through its associated page or directory.
     *
     * @param formName  name of the form to check in the current domain
     * @param user  user whose editable pages and groups are evaluated
     * @return {@code true} when the form is accessible to the user
     */
    public boolean isFormAccessible(String formName, UserDetails user) {
        Integer domainId = CloudToolsForCore.getDomainId();
        GroupsDB groupsDB = GroupsDB.getInstance();

		int[] userEditableGroups = groupsDB.expandGroupIdsToChilds(Tools.getTokensInt(user.getEditableGroups(), ","), true);
		int[] userEditablePages = Tools.getTokensInt(user.getEditablePages(), ",");
		if ((userEditableGroups == null || userEditableGroups.length<1) && (userEditablePages==null || userEditablePages.length<1)) return true;

		DocDB docDB = DocDB.getInstance();

        E lastOne = formsRepository.findTopByFormNameAndDomainIdAndCreateDateNotNullOrderByCreateDateDesc(formName, domainId);
		if (lastOne == null) return false;

        return isFormAccessible(lastOne, userEditableGroups, userEditablePages, docDB);
    }

    /**
     * Checks whether a form belongs to a page or directory in the user's editable scope.
     *
     * @param form  form whose associated document is checked
     * @param userEditableGroups  identifiers of editable directories, including expanded child directories
     * @param userEditablePages  identifiers of individually editable pages
     * @param docDB  document cache used to resolve the form's page
     * @return {@code true} when the form is in an editable page or directory
     */
    private boolean isFormAccessible(E form, int[] userEditableGroups, int[] userEditablePages, DocDB docDB) {
        if (userEditableGroups!=null && userEditableGroups.length>0)
        {
            DocDetails doc = docDB.getBasicDocDetails(form.getDocId(), false);
            if (doc != null)
            {
                for (int groupId : userEditableGroups)
                {
                    if (doc.getGroupId()==groupId)
                    {
                        return true;
                    }
                }
            }
        }
        if (userEditablePages!=null && userEditablePages.length>0)
        {
            for (int docId : userEditablePages)
            {
                if (form.getDocId()==docId)
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Resolves submission columns and display labels for a form.
     *
     * Multistep form fields are labeled from their item definitions and associated steps. The result also
     * includes the submission count, form type, and double opt-in state when available.
     *
     * @param formName  name of the form in the current domain
     * @param user  user whose access to the form is verified
     * @param prop  localization provider used to build field and step labels
     * @return form column metadata, or {@code null} when access is denied
     */
    public FormColumns getColumnNames(String formName, UserDetails user, Prop prop) {

        if (isFormAccessible(formName, user)==false) return null;

        FormColumns formColumns = new FormColumns();
        E entity = formsRepository.findFirstByFormNameAndDomainIdAndCreateDateIsNullOrderByIdAsc(formName, CloudToolsForCore.getDomainId());

        Map<String, String> itemNames = new HashMap<>();
        if(formItemsRepository != null) {
            formColumns.setFormType(entity.getFormType());
            if(entity instanceof FormsEntity && FORM_TYPE.MULTISTEP.value.equals(entity.getFormType())) {

                int index = 1;
                Map<Long, String> stepNames = new HashMap<>();
                for(FormStepEntity fse : formStepsRepository.findAllByFormNameAndDomainIdOrderBySortPriorityAsc(formName, CloudToolsForCore.getDomainId())) {
                    stepNames.put(fse.getId(), prop.getText("components.form_items.step_title") + " " + index);
                    index++;
                }

                for(FormItemEntity fie : formItemsRepository.findAllByFormNameAndDomainId(entity.getFormName(), entity.getDomainId())) {
                    StringBuilder itemName = new StringBuilder(MultistepFormsService.getFieldName(fie, prop));
                    if(stepNames != null && stepNames.size() > 1) itemName.append(" (").append(stepNames.get(fie.getStepId())).append(")");
                    itemNames.put(fie.getItemFormId(), itemName.toString());
                }
            }
        }

        String[] formsColumns = entity.getData().split("~");
        List<LabelValue> columns = new ArrayList<>();
        for (String column : formsColumns) {
            if (!column.contains("|")) {
                column += "|";
            }

            String[] columnNames = column.split("\\|", -1);
            if (Tools.isEmpty(columnNames[1])) {
                columnNames[1] = Tools.replace(columnNames[0], "-", " ");
                columnNames[1] = Tools.replace(columnNames[1], "_", " ");
                columnNames[1] = Tools.replace(columnNames[1], "e mail", "e-mail");
            }

            if(itemNames != null && itemNames.size() > 0) {
                String key = columnNames[0];
                if(key.endsWith("-fileNames")) key = key.substring(0, key.length()-10);
                columnNames[1] = itemNames.get(key);
            }

            columns.add(new LabelValue(columnNames[1], columnNames[0]));
        }

        formColumns.setColumns(columns);
        formColumns.setCount(formsRepository.countAllByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId()));

        //
        if(formSettingsRepository != null) {
            formColumns.setDoubleOptIn(
                Tools.isTrue( formSettingsRepository.isDoubleOptIn(formName, CloudToolsForCore.getDomainId()) )
            );
        }

        return formColumns;
    }

    /**
     * Returns submitted records for a form and parses their serialized data columns.
     *
     * @param formName  name of the form in the current domain
     * @param user  user whose access to the form is verified
     * @param pageable  requested pagination and sorting
     * @return page of submitted records, or {@code null} when access is denied
     */
    Page<E> getFormsData(String formName, UserDetails user, Pageable pageable) {

        if (isFormAccessible(formName, user)==false) return null;

        Page<E> formsEntities = formsRepository.findAllByFormNameAndDomainIdAndCreateDateNotNull(formName, CloudToolsForCore.getDomainId(), pageable);
        parseDataColumnInFormsEntities(formsEntities);
        return formsEntities;
    }

    /**
     * Searches submitted form records using server-side DataTable filters.
     *
     * @param formName  name of the form in the current domain
     * @param user  user whose access to the form is verified
     * @param params  filter parameters from the DataTable request
     * @param pageable  requested pagination and sorting
     * @return matching submitted records, or {@code null} when access is denied
     */
    public Page<E> findInDataByColumns(String formName, UserDetails user, Map<String, String> params, Pageable pageable) {

        if (isFormAccessible(formName, user)==false) return null;

        Integer domainId = CloudToolsForCore.getDomainId();

        Specification<E> spec = null;
        spec = getSearchConditions(formName, domainId, params);

        Page<E> filteredForms;
        if (spec != null && formsRepository instanceof JpaSpecificationExecutor) filteredForms = formsRepository.findAll(spec, pageable);
        else filteredForms = formsRepository.findAllByFormNameAndDomainIdAndCreateDateNotNull(formName, domainId, pageable);

        parseDataColumnInFormsEntities(filteredForms);

        return filteredForms;
    }

    /**
     * Builds database predicates for form identity, domain, submission state, and supported column filters.
     *
     * @param formName  name of the form whose submissions are searched
     * @param domainId  domain containing the form submissions
     * @param params  DataTable search parameters to convert into predicates
     * @return specification representing the supported search conditions
     */
    protected Specification<E> getSearchConditions(String formName, Integer domainId, Map<String, String> params) {
		return (Specification<E>) (root, query, builder) -> {
			final List<Predicate> predicates = new ArrayList<>();

            predicates.add(builder.equal(root.get("formName"), formName));
            predicates.add(builder.equal(root.get("domainId"), domainId));
            predicates.add(builder.isNotNull(root.get("createDate")));

            for (Map.Entry<String, String> paramsEntry : params.entrySet()) {
                String key = paramsEntry.getKey();
                if ("fromLastExport".equals(key)) {
                    String value = paramsEntry.getValue();
                    if (Tools.isNotEmpty(value) && "false".equals(value)==false) {
                        predicates.add(builder.isNull(root.get(value)));
                    }
                } else if ("id".equals(key)) {
                        String value = DatatableRestControllerV2.getCleanValue(paramsEntry.getValue());
                        int[] ids = Tools.getTokensInt(value, ",");
                        List<Integer> idsList = Arrays.stream(ids).boxed().collect(Collectors.toList());

                        //ak nic neoznacil, nic mu neexportneme
                        if (idsList.isEmpty()) idsList.add(Integer.valueOf(-1));

                        predicates.add(root.get("id").in(idsList));
                } else if (checkSearchParam(key)) {
                    key = DatatableRestControllerV2.getCleanKey(paramsEntry.getKey());
                    if ("createDate".equals(key) || "lastExportDate".equals(key) || "doubleOptinConfirmationDate".equals(key)) {
                        String dateRange = paramsEntry.getValue();
                        Pair<Date, Date> datePair = parseDate(dateRange);
                        if (datePair != null) {
                            if (datePair.first != null) predicates.add(builder.greaterThanOrEqualTo(root.get(key), datePair.first));
                            if (datePair.second != null) predicates.add(builder.lessThanOrEqualTo(root.get(key), datePair.second));
                        }

                    } else if ("note".equals(key) || "files".equals(key)) {
                        String value = paramsEntry.getValue();
                        if (value.startsWith("^") && value.endsWith("$")) predicates.add(builder.equal(root.get(key), value.substring(1, value.length()-1)));
                        else {
                            if (value.startsWith("^")) value = value.substring(1)+"%";
                            else if (value.endsWith("$")) value = "%"+value.substring(0, value.length()-1);
                            else value = "%"+value+"%";
                            if (Constants.DB_TYPE==Constants.DB_ORACLE) {
                                predicates.add(builder.like(builder.lower(root.get(key)), value.toLowerCase()));
                            } else if (Constants.DB_TYPE==Constants.DB_PGSQL) {
                                predicates.add(builder.like(builder.lower(builder.function("unaccent", String.class, root.get(key))), DB.internationalToEnglish(value).toLowerCase()));
                            } else {
                                predicates.add(builder.like(root.get(key), value));
                            }
                        }
                    } else {
                        String value = DatatableRestControllerV2.getCleanValue(paramsEntry.getValue());
                        if (key.startsWith("col_")) key = key.substring(4);
                        String searchParam = "%" + key + "~" + value + "%";
                        if (Constants.DB_TYPE==Constants.DB_ORACLE) {
                            predicates.add(builder.like(builder.lower(root.get("data")), searchParam.toLowerCase()));
                        } else if (Constants.DB_TYPE==Constants.DB_PGSQL) {
                            predicates.add(builder.like(builder.lower(builder.function("unaccent", String.class, root.get("data"))), DB.internationalToEnglish(searchParam).toLowerCase()));
                        } else {
                            predicates.add(builder.like(root.get("data"), searchParam));
                        }
                    }
                }
            }

            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    /**
     * Parses a {@code daterange:from-to} value into optional lower and upper date bounds.
     *
     * @param dateRange  serialized date range; either bound may be omitted
     * @return parsed date bounds with {@code null} for an omitted bound, or {@code null} for an empty value
     */
    protected Pair<Date, Date> parseDate(String dateRange) {
        Pair<Date, Date> dateRangePair = null;
        String[] dates = null;
        if (Tools.isNotEmpty(dateRange)) {
            dates = Tools.getTokens(dateRange.substring(dateRange.indexOf(":") + 1), "-");

            if (dates.length == 1) {
                if (dateRange.contains("-")) {
                    dateRangePair = new Pair<>(null, new Date(Tools.getLongValue(dates[0], new Date().getTime())));
                } else {
                    dateRangePair = new Pair<>(new Date(Tools.getLongValue(dates[0], 0)), null);
                }
            } else {
                dateRangePair = new Pair<>(new Date(Tools.getLongValue(dates[0], 0)), new Date(Tools.getLongValue(dates[1], new Date().getTime())));
            }

        }
        return dateRangePair;
    }

    /**
     * Parses serialized submission data into named, decrypted values for DataTable rendering.
     *
     * Values originating from WYSIWYG fields are selectively unescaped, and stored HTML is removed from
     * each returned entity after it has been used to determine the field rendering mode.
     *
     * @param formsEntities  submitted form records to transform in place
     */
    private void parseDataColumnInFormsEntities(Page<E> formsEntities) {
        for (E entity : formsEntities) {
            String[] columns = (entity.getData().split("\\|", -1));
            Map<String, String> columnNamesAndValues = new HashMap<>();

            boolean containsWysiwyg = false;
            //didnt find better way to check if HTML is enabled in the column
            String WYSIWYG_HTML = ResponseUtils.filter("<span class='form-control emailInput-textarea formsimple-wysiwyg' style='height: auto;'>");
            if (entity.getHtml()!=null && entity.getHtml().contains(WYSIWYG_HTML)) {
                containsWysiwyg = true;
            }

            for (String c : columns) {
                String[] nameAndValueArray = c.split("~");
                if (nameAndValueArray.length == 1) {
                    columnNamesAndValues.put(nameAndValueArray[0], "");
                } else {
                    columnNamesAndValues.put(nameAndValueArray[0], CryptoFactory.decrypt(nameAndValueArray[1]));
                }

                //allow HTML for cleditor - unescape entities
                if (containsWysiwyg) {
                    String value = columnNamesAndValues.get(nameAndValueArray[0]);
                    if (Tools.isNotEmpty(value) && entity.getHtml().contains(WYSIWYG_HTML+value)) {
                        //unescape entities
                        value = value.replace("&lt;", "<");
                        value = value.replace("&gt;", ">");
                        value = value.replace("&amp;", "&");
                        value = value.replace("&quot;", "\"");
                        value = value.replace("&#39;", "'");
                        columnNamesAndValues.put(nameAndValueArray[0], value);
                    }
                }
                //unescape double escape
                String value = columnNamesAndValues.get(nameAndValueArray[0]);
                if (Tools.isNotEmpty(value)) {
                    value = value.replace("&amp;", "&");
                    columnNamesAndValues.put(nameAndValueArray[0], value);
                }
            }

            entity.setColumnNamesAndValues(columnNamesAndValues);
            entity.setHtml("");
        }
    }

    boolean checkSearchParam(String param) {
        return param.startsWith("search");
    }

    /**
     * Finds a form record by identifier in the current domain.
     *
     * @param id  database identifier of the form record
     * @return matching form record, or {@code null} when no record exists in the current domain
     */
    public E getById(long id) {
        return formsRepository.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElse(null);
    }

    /**
     * Updates the note of an existing form record.
     *
     * @param note  note to store
     * @param id  database identifier of the form record
     */
    public void updateNote(String note, long id) {
        E form = getById(id);
        if (form != null) {
            form.setNote(note);
            formsRepository.save(form);
        }
    }

    /**
     * Sets the last-export date of the supplied form records using batched repository updates.
     *
     * @param forms  form records marked as exported
     */
    public void setExportDate(List<E> forms) {
        int counter = 0;
        List<Long> ids = new ArrayList<>();
        Date now = new Date(Tools.getNow());
        for (E form : forms) {
            ids.add(form.getId());
            form.setLastExportDate(now);
            if (counter++ % 100 == 0) {

                formsRepository.updateLastExportDate(now, ids);
                ids = new ArrayList<>();
            }
        }

        if (ids.isEmpty()==false) {
            formsRepository.updateLastExportDate(now, ids);
        }
    }

    /**
     * Deletes one submission or all records and structure belonging to a form definition.
     *
     * Deleting a management record, identified by a missing creation date, removes all submissions,
     * steps, and items with the same form name. Form settings are intentionally preserved.
     *
     * @param entity  entity carrying the form name to delete
     * @param id  database identifier of the record initiating the deletion
     * @param formStepsRepository  repository used to remove multistep form steps
     * @param formItemsRepository  repository used to remove multistep form items
     * @param formSettingsRepository  settings repository retained for deletion workflow compatibility
     * @return {@code true} when deletion succeeds; {@code false} for another domain or on failure
     */
    public boolean deleteItem(E entity, long id, FormStepsRepository formStepsRepository, FormItemsRepository formItemsRepository, FormSettingsRepository formSettingsRepository) {
        try {
            String formName = entity.getFormName();
            E entityDb = getById(id);

            int domainId = CloudToolsForCore.getDomainId();
            if (domainId != entityDb.getDomainId()) return false;

            if (entityDb.getCreateDate() == null) {
                //zmaz vsetky podla mena formu, ak su uz len 2 zaznamy (cize riadiaci + jeden form) zmaz tiez vsetko
                formsRepository.deleteByFormNameAndDomainId(formName, domainId);
                // Ak ma, zmaz aj steps/items (multistep forms)
                formStepsRepository.deleteAllByFormNameAndDomainId(formName, domainId);
                formItemsRepository.deleteAllByFormNameAndDomainId(formName, domainId);
                // DO NOT DELETE maybe form is still in webpage and we just deleted form records
                // formSettingsRepository.deleteByFormNameAndDomainId(formName, domainId);
            } else {
                // remove form filled record
                formsRepository.deleteById(id);
            }

            return true;
        } catch (Exception e) {
            Logger.error(getClass(), e);
        }
        return false;
    }

    /**
     * Authorizes and streams a stored form attachment to the HTTP response.
     *
     * Access requires an authenticated administrator with form permissions and, when the owning form can
     * be resolved, edit access to that form. Invalid sessions or permissions return a login redirect.
     *
     * @param name  stored attachment name, optionally prefixed by its form record ID
     * @param request  current HTTP request used for authentication and access checks
     * @param response  response receiving attachment headers and file content
     * @return login redirect for an unauthorized request, otherwise {@code null}
     * @throws IOException if the response stream cannot be opened or written
     */
    public String downloadAttachment(String name, HttpServletRequest request, HttpServletResponse response) throws IOException {
        //Something wrong
        if(request == null || response == null) return null;

        //Redirect to /admin/logon.jsp
        HttpSession session = request.getSession();
        if (session == null) return SpringUrlMapping.redirectToLogon();

        //Test actual user or redirect
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || !user.isAdmin()) return SpringUrlMapping.redirectToLogon();

        if (user.isEnabledItem("cmp_form")==false) return SpringUrlMapping.redirectToLogon();

        //ak je zle poslana linka typu /WEB-INF/formfiles/23882_pdf.pdf;23882_pdf.pdf oprav
        if (name.contains(";")) name = name.substring(name.lastIndexOf(";")+1);

        //ochrana voci hackerom ;-)
        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) return null;

        //get form name and check perms
        String formname = null;
        //get ID of row
        int i = name.indexOf("_");
        if (i > 0) {
            int id = Tools.getIntValue(name.substring(0, i), 0);
            if (id > 0) {
                formname = (new SimpleQuery()).forString("SELECT form_name FROM forms WHERE id=?", Integer.valueOf(id));
            }
        }

        //if we have formname check access, otherwise for safety reason (e.g. forms_archive different ID) allow access
        if (Tools.isNotEmpty(formname) && isFormAccessible(formname, user)==false) return SpringUrlMapping.redirectToLogon();

        //fix na lomitko v Tomcate
        String filePath = Tools.getRealPath(FormMailAction.FORM_FILE_DIR);
        if (filePath.endsWith(Character.toString(File.separatorChar))) filePath = filePath + name;
        else filePath = filePath + File.separatorChar + name;

        if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")) {
           //kedze sme zmenili umiestnenie suborov na externy adresar, ak subor bol vytvoreny pred zmenou tak je v centralnom adresari
           IwcmFile f = new IwcmFile(filePath);
           if (f.exists() == false) {
              String globalPath = Constants.getServletContext().getRealPath(FormMailAction.FORM_FILE_DIR);
              if (globalPath.endsWith(Character.toString(File.separatorChar))) globalPath = globalPath + name;
              else globalPath = globalPath + File.separatorChar + globalPath;

              f = new IwcmFile(globalPath);
              if (f.exists()) filePath = globalPath;
           }
        }

        ServletOutputStream out = response.getOutputStream();
        //citaj subor a posielaj na vystup
        byte buff[] = new byte[8000];
        IwcmInputStream fis = new IwcmInputStream(filePath);
        int len;

        String mimeType = "application/octet-stream";
        try {
	    	mimeType = Constants.getServletContext().getMimeType(filePath.toLowerCase());
        } catch (Exception ex) {
	    	sk.iway.iwcm.Logger.error(ex);
        }

	    if (Tools.isEmpty(mimeType)) mimeType = "application/octet-stream";

        response.setContentType(mimeType);
        name = name.substring(name.indexOf('_') + 1);
        response.setHeader("Content-Disposition", Tools.sanitizeHttpHeaderParam("attachment; filename=\"" + name + "\""));

        while ((len = fis.read(buff)) != -1) out.write(buff, 0, len);

        fis.close();
        out.flush();
        out.close();
        return null;
    }

    /**
     * Creates the placeholder map shared by form field and tooltip templates.
     *
     * @param item form item metadata
     * @param id rendered field identifier
     * @param label label rendered by the target template
     * @param labelSanitized label without HTML markup
     * @param value field value
     * @param placeholder field placeholder
     * @param classes CSS classes added to the field
     * @param tooltip rendered tooltip HTML
     * @return mutable placeholder map
     */
    public static Map<String, String> getFieldReplacementMap(
        JSONObject item,
        String id,
        String label,
        String labelSanitized,
        String value,
        String placeholder,
        String classes,
        String tooltip
    ) {
        String safeId = Tools.getStringValue(id, "");
        String safeValue = Tools.getStringValue(value, "");
        String itemId = item == null ? "" : item.optString("id", "");
        String stepId = item == null ? "" : item.optString("stepId", "");
        String tooltipId = ("info-tooltip-" + safeId + "-" + itemId).replaceAll("[^A-Za-z0-9_-]", "-");

        Map<String, String> fields = new HashMap<>();
        fields.put("tooltipId", tooltipId);
        fields.put("id", safeId);
        fields.put("itemId", itemId);
        fields.put("stepId", stepId);
        fields.put("label", Tools.getStringValue(label, ""));
        fields.put("labelSanitized", Tools.getStringValue(labelSanitized, ""));
        fields.put("value", safeValue);
        fields.put("valueSanitized", DocTools.removeChars(safeValue, true));
        fields.put("placeholder", ResponseUtils.filter(Jsoup.parse(Tools.getStringValue(placeholder, "")).text()));
        fields.put("classes", Tools.getStringValue(classes, ""));
        fields.put("tooltip", Tools.getStringValue(tooltip, ""));
        return fields;
    }

    /**
     * Replaces form placeholders in one pass without interpreting placeholders contained in replacement values.
     *
     * @param html template containing placeholders in the {@code ${name}} format
     * @param fields placeholder values
     * @return template with known placeholders replaced
     */
    public static String replaceFields(String html, Map<String, String> fields) {
        StringSubstitutor substitutor = new StringSubstitutor(fields);
        substitutor.setDisableSubstitutionInValues(true);
        substitutor.setValueDelimiterMatcher(null);
        substitutor.setEscapeChar('\0');
        return substitutor.replace(Tools.getStringValue(html, ""));
    }

    /**
     * Renders a form field template by replacing form, item, label, value, validation, and iterable placeholders.
     *
     * Selected item metadata is filtered before being inserted into HTML, optional first-use headings are applied,
     * and user-specific expressions are resolved in the completed fragment.
     *
     * @param html  HTML template containing supported placeholders
     * @param formName  form name inserted into form-related placeholders
     * @param recipients  recipient value inserted into the template
     * @param item  form item metadata, or {@code null} for a template without item placeholders
     * @param requiredLabelAdd  marker appended to required labels and placeholders
     * @param isEmailRender  whether the fragment is rendered for email rather than an interactive form
     * @param rowView  whether non-closing fragments are wrapped in a row column
     * @param firstTimeHeadingSet  mutable set used to prevent repeated first-use headings
     * @param prop  localization provider for tooltip, iterable, and heading templates
     * @param request  request used to resolve user-specific expressions
     * @return rendered HTML fragment
     */
    public static final String replaceFields(String html, String formName, String recipients, JSONObject item, String requiredLabelAdd, boolean isEmailRender, boolean rowView, Set<String> firstTimeHeadingSet, Prop prop, HttpServletRequest request)
    {
        html = Tools.replace(Tools.getStringValue(html, ""), "${formname}", Tools.getStringValue(formName, ""));
        html = Tools.replace(html, "${savedb}", Tools.getStringValue(formName, ""));
        html = Tools.replace(html, "${recipients}", Tools.getStringValue(recipients, ""));

        if (item != null) {
            try {
                String fieldType = item.optString("fieldType", "unknown");
                String value = item.has("value") ? Tools.getStringValue(item.optString("value", ""), "") : "";

                boolean required = false;
                try {
                    required = "true".equals(item.getString("required"));
                } catch (Exception ex) {
                    required = item.optBoolean("required", false);
                }

                String label = StringEscapeUtils.unescapeHtml4(Tools.getStringValue(item.optString("label", ""), ""));

                String placeholder = "";
                if (item.has("placeholder")) {
                    placeholder = Tools.getStringValue(item.optString("placeholder", ""), "");
                    if (Tools.isNotEmpty(placeholder)) {
                        placeholder = ResponseUtils.filter(placeholder);

                        //ak je zadany placeholder a nebol zadany label, tak label schovat
                        if (Tools.isEmpty(Tools.getStringValue(item.optString("labelOriginal", ""), ""))) {
                            if (isEmailRender == false) html = Tools.replace(html, "<label ", "<label class=\"d-none\" ");

                            //pretoze z label sa generuje potom ID/name elementu a potrebujeme polia rozlisovat
                            label = placeholder;
                            if (required && Tools.isNotEmpty(requiredLabelAdd)) {
                                placeholder += requiredLabelAdd;
                            }
                        }
                    }
                }

                String tooltipLabel = "";
                if (item.has("tooltip")) {
                    tooltipLabel = StringEscapeUtils.unescapeHtml4(Tools.getStringValue(item.optString("tooltip", ""), ""));
                    if (Tools.isNotEmpty(tooltipLabel)) tooltipLabel = ResponseUtils.filter(tooltipLabel);
                }

                String labelSanitized = ResponseUtils.filter(Jsoup.parse(label).text());

                // Prefer explicit itemFormId; fallback keeps backward compatibility.
                String id = Tools.getStringValue(item.optString("itemFormId", ""), "");
                if (Tools.isEmpty(id)) id = DocTools.removeChars(label, true);

                String classes = "";
                if (required) {
                    classes = "required ";
                    if (Tools.isNotEmpty(requiredLabelAdd)) {
                        //ak label konci na : pridaj required text pred dvojbodku
                        if (label.endsWith(":")) label = label.substring(0, label.lastIndexOf(":")) + requiredLabelAdd + ":";
                        else label += requiredLabelAdd;
                    }
                }

                String tooltip = "";
                Map<String, String> replacementFields = getFieldReplacementMap(item, id, label, labelSanitized, value, placeholder, classes, tooltip);
                if (isEmailRender == false && Tools.isNotEmpty(tooltipLabel)) {
                    Map<String, String> tooltipFields = new HashMap<>(replacementFields);
                    tooltipFields.put("label", tooltipLabel);
                    tooltip = " " + replaceFields(prop.getText("components.formsimple.tooltipCode"), tooltipFields);
                    replacementFields.put("tooltip", tooltip);
                }

                //skus zobrazit nadpis nad pole ak je definovany cez components.formsimple.firstTime.xxx
                String firstTimeHeadingKey = "components.formsimple.firstTimeHeading." + fieldType;
                String firstTimeHeading = prop.getText(firstTimeHeadingKey, false);
                if (firstTimeHeadingSet != null && Tools.isNotEmpty(firstTimeHeading) && firstTimeHeading.equals(firstTimeHeadingKey) == false && firstTimeHeadingSet.contains(label) == false) {
                    firstTimeHeadingSet.add(label);
                    html = firstTimeHeading + html;
                }

                //iterable - pre skupinu poli
                int iterableSize = 0;
                if (html.contains("${iterable}") && Tools.isNotEmpty(value)) {
                    StringBuilder iterable = new StringBuilder();
                    String iterableKey = "components.formsimple.iterable." + fieldType;
                    String iterableCode = prop.getText(iterableKey);
                    if (Tools.isNotEmpty(iterableCode) && iterableCode.equals(iterableKey) == false) {
                        String[] values = parseIterableValues(value);

                        int counter = 0;
                        iterableSize = values.length;
                        for (String token : values) {
                            String valueLabel = token;
                            String code = iterableCode;

                            int separator = token.indexOf(":");
                            if (code.contains("${value-label}") && separator >= 0) {
                                valueLabel = token.substring(0, separator);
                                token = token.substring(separator + 1);
                            }

                            code = Tools.replace(code, "${value}", token);
                            code = Tools.replace(code, "${value-label}", valueLabel);
                            code = Tools.replace(code, "${counter}", String.valueOf(counter));

                            iterable.append(code).append("\n");
                            counter++;
                        }
                    }
                    html = Tools.replace(html, "${iterable}", iterable.toString());
                }

                html = replaceFields(html, replacementFields);

                StringBuilder csError = new StringBuilder();
                csError.append("<div class=\"help-block cs-error cs-error-").append(id);
                if (iterableSize > 0) {
                    for (int counter = 0; counter < iterableSize; counter++) {
                        csError.append(" cs-error-").append(id).append("-").append(counter);
                    }
                }
                csError.append("\"></div>");
                html = Tools.replace(html, "${cs-error}", csError.toString());
            } catch (Exception ex) {
                sk.iway.iwcm.Logger.error(ex);
            }
       }

       if (rowView && html.startsWith("</div") == false) {
          //ak to nie je ukoncovaci tag, obal to do div.col
          html = "<div class=\"col\">" + html + "</div>";
       }

       return DocTools.updateUserCodes(UsersDB.getCurrentUser(request), new StringBuilder(html)).toString();
    }

    /**
     * Resolves configured options, including options backed by an enumeration.
     *
     * @param value  serialized options
     * @return resolved option tokens, or an empty array for an empty or invalid enumeration configuration
     */
    public static String[] parseIterableValues(String value) {
        String normalized = Tools.getStringValue(value, "").trim();
        if (Tools.isEmpty(normalized)) return new String[0];

        String[] enumerationValues = resolveEnumerationIterableValues(normalized);
        if (enumerationValues != null) return enumerationValues;

        String delimiter = " ";
        if (normalized.contains("|")) delimiter = "|";
        else if (normalized.contains(",")) delimiter = ",";

        return Tools.getTokens(normalized, delimiter, true);
    }

    /**
     * Resolves an {@code enumeration-options} configuration into label and value pairs.
     *
     * @param value  serialized iterable configuration
     * @return resolved {@code label:value} entries, an empty array for an invalid enumeration configuration,
     *         or {@code null} when the value is not an enumeration configuration
     */
    private static String[] resolveEnumerationIterableValues(String value) {
        String normalized = value;
        if (normalized.startsWith("{") && normalized.endsWith("}")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }

        if (normalized.startsWith("enumeration-options") == false) return null;

        String paramsText = normalized.substring("enumeration-options".length());
        if (paramsText.startsWith("|")) paramsText = paramsText.substring(1);

        String[] enumerationParams = Tools.getTokens(paramsText, "|", true);
        if (enumerationParams == null || enumerationParams.length != 3) return new String[0];

        int enumId = Tools.getIntValue(enumerationParams[0], -1);
        List<EnumerationDataBean> enumObjects;
        if (enumId > 0) enumObjects = EnumerationDataDB.getEnumerationDataByType(enumId);
        else enumObjects = EnumerationDataDB.getEnumerationDataByType(enumerationParams[0]);

        if (enumObjects == null || enumObjects.isEmpty()) return new String[0];

        List<String> enumValues = new ArrayList<>();
        for (EnumerationDataBean enumObject : enumObjects) {
            try {
                BeanWrapperImpl bw = new BeanWrapperImpl(enumObject);

                String enumLabel = String.valueOf(bw.getPropertyValue(enumerationParams[1]));
                String enumValue = String.valueOf(bw.getPropertyValue(enumerationParams[2]));

                enumValues.add(enumLabel + ":" + enumValue);
            } catch (Exception ex) {
                Logger.error(null, ex);
            }
        }

        return enumValues.toArray(new String[0]);
    }

    public boolean isFormNameUnique(String formName) {
        Integer count = formsRepository.countAllByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId());
        return (count != null && count > 0) ? false : true;
    }
}
