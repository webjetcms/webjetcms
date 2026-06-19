package sk.iway.iwcm.components.multistep_form.rest;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.Html2Text;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.form_settings.rest.FormSettingsService;
import sk.iway.iwcm.components.forms.FormsEntity;
import sk.iway.iwcm.components.forms.FormsRepository;
import sk.iway.iwcm.components.forms.RegExpEntity;
import sk.iway.iwcm.components.forms.RegExpRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.components.multistep_form.mvc.MultistepFormApp;
import sk.iway.iwcm.components.multistep_form.support.FormProcessorInterface;
import sk.iway.iwcm.components.multistep_form.support.SaveFormException;
import sk.iway.iwcm.components.upload.XhrFileUploadService;
import sk.iway.iwcm.components.upload.XhrFileUploadServlet;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.form.FormDB;
import sk.iway.iwcm.form.FormFileRestriction;
import sk.iway.iwcm.form.FormMailAction;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.captcha.Captcha;
import sk.iway.iwcm.system.datatable.RowReorderDto;
import sk.iway.iwcm.system.datatable.RowReorderDto.RowReorderValue;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.system.stripes.CSRF;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.utils.Pair;

@Service
public class MultistepFormsService {
    /**
     * Service handling validation, session persistence and saving of multistep forms.
     * <p>
     * Responsibilities:
     * - Generate stable session keys per form and user request
     * - Validate step inputs (required, regex, XSS, CAPTCHA, CSRF, file rules)
     * - Orchestrate custom processors/interceptors for steps and final save
     * - Persist intermediate step data into HTTP session and finalize DB save
     */

    public static final String SESSION_PREFIX = "MultistepForm_";
    public static final String MULTIUPLOAD_PREFIX = "multiupload";

    private static final String ALL_FILES_SIZE_SESSION_KEY_SUFFIX = "_allFilesSizeInKB";

    private static final String ITEM_KEY_LABEL_PREFIX = "components.formsimple.label.";
    private static final String ITEM_KEY_HIDE_FIELDS_PREFIX = "components.formsimple.hide.";
    private static final String ITEM_KEY_INPUT_PREFIX = "components.formsimple.input.";

    public static final String VISIBILITY_TAB = "visibilityConditions";
    public static final String REQUIREMENT_TAB = "requirementConditions";

    private final SaveFormService saveFormService;
    private final FormsRepository formsRepository;
    private final FormItemsRepository formItemsRepository;
    private final FormStepsRepository formStepsRepository;
    private final FormSettingsRepository formSettingsRepository;

    @Autowired
    public MultistepFormsService(SaveFormService saveFormService, FormsRepository formsRepository, FormItemsRepository formItemsRepository, FormStepsRepository formStepsRepository, FormSettingsRepository formSettingsRepository) {
        this.saveFormService = saveFormService;
        this.formsRepository = formsRepository;
        this.formItemsRepository = formItemsRepository;
        this.formStepsRepository = formStepsRepository;
        this.formSettingsRepository = formSettingsRepository;
    }

    /* ********** PUBLIC STATIC - small support methods ********** */

    /**
     * Build the session key used to store per-form, per-request step data.
     *
     * @param formName logical form name (will be sanitized elsewhere)
     * @param request  HTTP request providing the CSRF token header
     * @return stable session key in format {@code MultistepForm_<formName>_<domainId>_<csrf>}
     */
    public static final String getSessionKey(String formName, HttpServletRequest request) {
        return getNewSessionKey(formName, request.getHeader("X-CSRF-Token"));
    }

    /**
     * Build a new session key for the given form and CSRF token.
     *
     * @param formName logical form name
     * @param csrf     CSRF token value
     * @return session key string
     */
    public static final String getNewSessionKey(String formName, String csrf) {
        StringBuilder sessionKey = new StringBuilder(SESSION_PREFIX);
        sessionKey.append(formName).append("_");
        sessionKey.append(CloudToolsForCore.getDomainId());
        sessionKey.append("_").append(csrf);
        return sessionKey.toString();
    }

    /**
     * Extract and sanitize the {@code formName} from a params map.
     *
     * @param params map potentially containing key {@code formName}
     * @return sanitized form name (safe for identifiers)
     */
    public static final String getFormName(Map<String, String> params) {
        String formName = Tools.getStringValue(params.get("formName"), "");
        return formName;
    }

    /**
     * Extract and sanitize the {@code formName} from the request.
     *
     * @param request HTTP request containing parameter {@code formName}
     * @return sanitized form name (safe for identifiers)
     */
    public static final String getFormName(HttpServletRequest request) {
        String formName = Tools.getStringValue(request.getParameter("formName"), "");
        return formName;
    }

    /**
     * Provide available regular expression validations as label/value pairs for UI.
     *
     * @param regExpRepository repository of regex definitions
     * @param request          request used to resolve localized titles
     * @return list of label/value options
     */
    public static final List<LabelValue> getRegExOptions(RegExpRepository regExpRepository, HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);
        List<LabelValue> options = new ArrayList<>();

        for(RegExpEntity regEx : regExpRepository.findAll())
            options.add( new LabelValue(prop.getText(regEx.getTitle()), regEx.getId() + ""));

        return options;
    }

    /**
     * Provide supported form field types as label/value pairs.
     *
     * @param request request used to resolve localized labels
     * @return list of field type options
     */
    public static final Pair<List<LabelValue>, List<LabelValue>> getFieldTypes(HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);

        List<LabelValue> visibleOptions = new ArrayList<>();
        List<LabelValue> technicalOptions = new ArrayList<>();
        Map<String, String> formsimpleFields = prop.getTextStartingWith(ITEM_KEY_LABEL_PREFIX);

        for(Entry<String, String> entry : formsimpleFields.entrySet()) {
            // Get html code of this field type
            String htmlCode = prop.getText(ITEM_KEY_INPUT_PREFIX + entry.getKey().substring(ITEM_KEY_LABEL_PREFIX.length()));
            // Skip prohibited submit buttons
            if(htmlCode != null && (htmlCode.toLowerCase().contains("type=\"submit\"") || htmlCode.toLowerCase().contains("type=\'submit\'")) )
                continue;

            String value = entry.getKey().substring(ITEM_KEY_LABEL_PREFIX.length());
            visibleOptions.add(new LabelValue(entry.getValue(), value));

            String inputType = "text";
            if(htmlCode != null && htmlCode.contains("${iterable}")) inputType = "iterable";

            technicalOptions.add(new LabelValue(inputType, value));
        }

        //sort by label
        visibleOptions.sort((o1, o2) -> o1.getLabel().compareToIgnoreCase(o2.getLabel()));

        return new Pair<>(visibleOptions, technicalOptions);
    }

    public static final boolean isFieldtypeIterable(String fieldType, HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);
        String htmlCode = prop.getText(ITEM_KEY_INPUT_PREFIX + fieldType);
        return (htmlCode != null && htmlCode.contains("${iterable}"));
    }

    /**
     * Provide visibility configuration per field type (which technical fields are hidden).
     * Adds a special option {@code allwaysHidden}.
     *
     * @param request request used to resolve localized labels
     * @return list of visibility label/value pairs
     */
    public static final List<LabelValue> getFiledTypeVisibility(HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);

        List<LabelValue> options = new ArrayList<>();
        Map<String, String> formsimpleFields = prop.getTextStartingWith(ITEM_KEY_HIDE_FIELDS_PREFIX);

        for(Entry<String, String> entry : formsimpleFields.entrySet()) {
            options.add(new LabelValue(entry.getValue(), entry.getKey().substring(ITEM_KEY_HIDE_FIELDS_PREFIX.length())));
        }

        options.add(new LabelValue("id,formName,itemFormId", "allwaysHidden"));
        return options;
    }

    /**
     * Provide tab visibility configuration per field type.
     * <p>
     * Depending on the field type, returns list of tabs that should be shown in admin:
     * visibility conditions, requirement conditions, both, or none.
     *
     * @param request request used to resolve localized labels and field metadata
     * @return list mapping field type to a comma-separated list of visible tabs
     */
    public static final List<LabelValue> getFieldTabVisibility(HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);

        List<LabelValue> options = new ArrayList<>();
        Map<String, String> formsimpleFields = prop.getTextStartingWith(ITEM_KEY_LABEL_PREFIX);

        List<LabelValue> fieldVisibility = getFiledTypeVisibility(request);
        Map<String, List<String>> fieldVisibilityMap = fieldVisibility.stream().collect(Collectors.toMap(LabelValue::getValue, fv -> List.of( Tools.getTokens(fv.getLabel(), ",") )));

        for(Entry<String, String> entry : formsimpleFields.entrySet()) {
            String type = entry.getKey().substring(ITEM_KEY_LABEL_PREFIX.length());

            if(getRowViewItemTypes().contains(type) || "captcha".equals(type) || "verify_code".equals(type)) {
                options.add(new LabelValue(VISIBILITY_TAB + "," + REQUIREMENT_TAB, type));
            } else if(fieldVisibilityMap.getOrDefault(type, List.of()).contains("required")) {
                options.add(new LabelValue(REQUIREMENT_TAB, type));
            } else {
                options.add(new LabelValue("", type));
            }
        }

        return options;
    }

    /**
     * Get the next step following the provided step within a form sequence.
     *
     * @param formName      logical form name
     * @param currentStepId identifier of current step
     * @param repo          repository used to fetch ordered steps
     * @return next step entity or {@code null} if current step is the last
     * @throws IllegalStateException when the provided step does not belong to the form/domain
     */
    public static final FormStepEntity getNextStep(String formName, FormStepEntity currentStep, FormStepsRepository repo) {
        // Return null indicating its last step and there are no more steps
        if(currentStep.isLastStep()) return null;

        return repo.getStepByPosition(formName, currentStep.getCurrentPosition() + 1, CloudToolsForCore.getDomainId())
            .orElseThrow(() -> new IllegalStateException("Given currentStepId: " + currentStep.getId() + " for form " + formName + " does NOT exist") );
    }

    /**
     * Return technical field types used only for row/column layout in the form editor.
     *
     * @return list of non-input layout item field types
     */
    public static final List<String> getRowViewItemTypes() {
        return List.of("novy-riadok", "prazdny-stlpec", "new-row", "empty-column");
    }

    /**
     * Convert serialized form data into a key/value map.
     * <p>
     * Input format is expected as {@code itemFormId~value} pairs separated by {@code |}.
     * Multi-upload synthetic suffix {@code -fileNames} is normalized away.
     *
     * @param form persisted form entity with serialized data
     * @return ordered map of field identifiers and their values
     */
    public static final Map<String, String> getFormDataAsMap(FormsEntity form) {
        Map<String, String> formData = new LinkedHashMap<>();
        for(String fieldData : Tools.getTokens(form.getData(), "|")) {
            String[] fieldDataArr = Tools.getTokens(fieldData, "~");
            String fieldId = fieldDataArr[0];
            if(fieldId.endsWith("-fileNames")) fieldId = fieldId.replace("-fileNames", "");
            String fieldValue = fieldDataArr.length == 2 ? fieldDataArr[1] : "";

            formData.put(fieldId, fieldValue);
        }
        return formData;
    }

    /**
     * Resolve human-readable field name for validation and UI messages.
     *
     * @param stepItem form item definition
     * @param prop     localization provider
     * @return localized field label or plain text extracted from custom HTML label
     */
    public static final String getFieldName (FormItemEntity stepItem, Prop prop) {
        String fieldName = "";
        if(Tools.isEmpty(stepItem.getLabel())) fieldName = prop.getText(ITEM_KEY_LABEL_PREFIX + stepItem.getFieldType());
        else fieldName = new Html2Text( StringEscapeUtils.unescapeHtml4(stepItem.getLabel()) ).getText();
        return fieldName;
    }

    /**
     * Extract chart statistics parameters from request.
     *
     * @param request HTTP request containing optional {@code formName} and {@code itemFormId}
     * @return pair of {@code (formName, itemFormId)} when both are present, otherwise {@code null}
     */
    public static final Pair<String, String> getChartStatInfo(HttpServletRequest request) {
        String formName = Tools.getStringValue(request.getParameter("formName"), "");
        String itemFormId = Tools.getStringValue(request.getParameter("itemFormId"), "");

        if(Tools.isNotEmpty(formName) && Tools.isNotEmpty(itemFormId)) return new Pair<>(formName, itemFormId);
        else return null;
    }

    /* ********** PUBLIC - support methods ********** */

    public final int getFormId(String formName) {
        if(Tools.isEmpty(formName)) return -1;
        return formsRepository.getFormId(formName, CloudToolsForCore.getDomainId()).orElse(-1L).intValue();
    }

    /**
     * Validate whether provided form and step represent an allowed step for current session.
     *
     * @param formName      logical form name
     * @param currentStepId step id to validate
     * @param request       request with session context
     * @return {@code true} when the step exists and is permitted in current session
     */
    public final boolean validateFormInfo(String formName, Long currentStepId, HttpServletRequest request) {
        return getValidStepEntity(formName, currentStepId, request) != null;
    }

    /**
     * Build available source fields for condition builder up to the current step.
     *
     * @param formName logical form name
     * @param stepId   current step id
     * @param prop     localization provider
     * @return sorted condition field options grouped by step labels
     */
    public final List<LabelValue> getAvailableConditionFields(String formName, Integer stepId, Prop prop) {
        int currentPosition = new SimpleQuery().forInt("SELECT current_position FROM form_steps WHERE id = ? AND domain_id = ?", stepId, CloudToolsForCore.getDomainId());

        Map<Long, String> stepNames = new HashMap<>();
        Map<Long, Integer> stepPositions = new HashMap<>();
        List<Integer> stepsIds = new ArrayList<>();
        for(FormStepEntity fse : formStepsRepository.getStepsUpToPosition(formName, currentPosition, CloudToolsForCore.getDomainId())) {
            stepNames.put(fse.getId(), prop.getText("components.form_items.step_title") + " " + fse.getCurrentPosition());
            stepPositions.put(fse.getId(), fse.getCurrentPosition());
            stepsIds.add(fse.getId().intValue());
        }

        return sortFields(formItemsRepository.findAllByFormNameAndStepIdInAndDomainId(formName, stepsIds, CloudToolsForCore.getDomainId()), stepPositions, stepNames, prop);
    }

    /**
     * Build condition field options from arbitrary field collection.
     *
     * @param fields   fields to transform into options
     * @param formName logical form name used to resolve step labels/positions
     * @param prop     localization provider
     * @return sorted options in format {@code (Step X) Field Name}
     */
    public final List<LabelValue> prepareConditionFieldOptions(List<FormItemEntity> fields, String formName, Prop prop) {
        Map<Long, String> stepNames = new HashMap<>();
        Map<Long, Integer> stepPositions = new HashMap<>();

        for(FormStepEntity fse : formStepsRepository.findAllByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId())) {
            stepNames.put(fse.getId(), prop.getText("components.form_items.step_title") + " " + fse.getCurrentPosition());
            stepPositions.put(fse.getId(), fse.getCurrentPosition());
        }

        return sortFields(fields, stepPositions, stepNames, prop);
    }

    /**
     * Sort fields by step and field order and convert them into label/value options.
     *
     * @param fields        source field list
     * @param stepPositions map of step id to step position
     * @param stepNames     map of step id to localized step title
     * @param prop          localization provider
     * @return deduplicated options ordered by step position and sort priority
     */
    private final List<LabelValue> sortFields(List<FormItemEntity> fields, Map<Long, Integer> stepPositions, Map<Long, String> stepNames, Prop prop) {
        List<FormItemEntity> sortedFields = fields
            .stream()
            .sorted(Comparator
                .comparingInt((FormItemEntity fie) -> stepPositions.getOrDefault(fie.getStepId().longValue(), 0))
                .thenComparingInt(fie -> fie.getSortPriority() != null ? fie.getSortPriority() : 0))
            .toList();

        List<LabelValue> options = new ArrayList<>();
        String previousItemFormId = null;
        boolean previousItemFormIdSet = false;

        for (FormItemEntity fie : sortedFields) {
            if (previousItemFormIdSet && fie.getItemFormId().equals(previousItemFormId)) continue;

            StringBuilder itemName = new StringBuilder("");
            itemName.append("(").append(stepNames.get(fie.getStepId().longValue())).append(") ");
            itemName.append( MultistepFormsService.getFieldName(fie, prop) );
            options.add(new LabelValue(itemName.toString(), fie.getItemFormId()));

            previousItemFormId = fie.getItemFormId();
            previousItemFormIdSet = true;
        }

        return options;
    }

    /**
     * Build human-readable options for steps of a form suitable for selection inputs.
     *
     * @param formName logical form name
     * @return list of label/value pairs ordered by step sorting
     */
    public final List<LabelValue> getFormStepsOptions(String formName, Prop prop) {
        List<LabelValue> options = new ArrayList<>();

        if(Tools.isEmpty(formName)) return options;

        int counter = 1;
        for(FormStepEntity step : formStepsRepository.findAllByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId())) {
            StringBuilder label = new StringBuilder();
            label.append(prop.getText("components.form_items.step_title")).append(" ").append(counter);

            options.add(new LabelValue(label.toString(), step.getId() + ""));
            counter++;
        }

        return options;
    }

    /**
     * Generate a valid and unique {@code itemFormId} for a form item within a form.
     * <p>
     * For {@code radio} fields, derives id from label/placeholder and required flag,
     * allowing multiple radio buttons to share the same logical id. For other fields,
     * generates a unique suffix (numeric) to avoid collisions among the same field type.
     *
     * @param entity form item to generate id for
     * @return unique itemFormId string safe for storage and queries
     */
    public final String getValidItemFormId(FormItemEntity entity) {

        // type radio use placeholder or label as itemFormId, so we can join them - here same id is wanted outcome
        String fieldType = entity.getFieldType();
        if("radio".equals(fieldType)) {
            String postfix = "-" + (Tools.isTrue(entity.getRequired()) ? "true" : "false");
            String baseFormId = Tools.getStringValue(fieldType, "");
            baseFormId = StringEscapeUtils.unescapeHtml4(baseFormId);

            String itemFormId = "";
            String label = entity.getLabel();
            if (Tools.isNotEmpty(label)) {
                label = ResponseUtils.filter(label);
                baseFormId = DB.prepareString(label, 200);
            }

            itemFormId = baseFormId + postfix;

            return DocTools.removeChars(itemFormId, true);
        }

        String itemFormId = fieldType;
        itemFormId = DocTools.removeChars(itemFormId, true);

        // Generate unique itemFormId with numeric postfix.
        // Collect existing itemFormIds for this form/field type.
        List<String> existingIds = formItemsRepository.getItemFormIds(entity.getFormName(), entity.getFieldType(), CloudToolsForCore.getDomainId());

        String prefix = itemFormId + "-";
        int biggestPostfix = 0;

        // Determine highest numeric postfix used so far for the given prefix.
        for (String existing : existingIds) {
            if (existing != null && existing.startsWith(prefix)) {
                int lastDash = existing.lastIndexOf('-');
                if (lastDash > -1 && lastDash + 1 < existing.length()) {
                    String numberPart = existing.substring(lastDash + 1);
                    int num = Tools.getIntValue(numberPart, -1);
                    if (num > biggestPostfix) biggestPostfix = num;
                }
            }
        }

        // Next available postfix.
        int nextPostfix = biggestPostfix + 1;
        String candidate = prefix + nextPostfix;

        // Safety loop if duplicates somehow appear in the loaded list.
        while (existingIds.contains(candidate)) {
            nextPostfix++;
            candidate = prefix + nextPostfix;
        }

        return candidate;
    }

    /* ********** PUBLIC - main logic methods to work with form ********** */

    /**
     * Update or create the pattern entity (form definition) for the form by
     * concatenating all unique {@code itemFormId}s in validation order.
     * Multi-upload fields append the {@code -fileNames} postfix.
     *
     * @param formName logical form name
     */
    public final void updateFormPattern(String formName) {
        //First get from pattern entity
        FormsEntity patternEntity = formsRepository.findFirstByFormNameAndDomainIdAndCreateDateIsNullOrderByIdAsc(formName, CloudToolsForCore.getDomainId());
        if(patternEntity == null) {
            patternEntity = new FormsEntity();
            patternEntity.setFormName(formName);
            patternEntity.setDomainId(CloudToolsForCore.getDomainId());
            patternEntity.setCreateDate(null);
            patternEntity.setDocId(-1);
        }

        //Get form items in order
        StringBuilder patternData = new StringBuilder();
        for(FormItemEntity stepItem : getFormItemsForValidation(formName)) {
            if(stepItem.getFieldType().startsWith("captcha")) continue; // captcha is not saved in DB

            if(patternData.isEmpty()) patternData.append(stepItem.getItemFormId());
            else patternData.append("|~").append(stepItem.getItemFormId());

            if(stepItem.getFieldType().startsWith(MULTIUPLOAD_PREFIX)) patternData.append("-fileNames");
        }

        if (patternData.isEmpty() == false) patternEntity.setData(patternData.toString());
        formsRepository.save(patternEntity);
    }

    /**
     * Validate and persist a single step of a multistep form into session, and optionally
     * finalize the form submission. Performs CSRF/CAPTCHA/file validations and invokes
     * custom processors. When the last step is completed, triggers final save into DB.
     *
     * @param formName logical form name
     * @param stepId   current step identifier
     * @param request  HTTP request containing JSON body of field values
     * @param response JSON response to be enriched with errors, next step or forward
     * @throws SaveFormException when validation or processing fails in a controlled way
     * @throws IOException       when reading request body fails
     */
    public final void saveFormStep(String formName, Long stepId, HttpServletRequest request, JSONObject response) throws SaveFormException, IOException {
        FormStepEntity validStepEntity = getValidStepEntity(formName, stepId, request);
        if(validStepEntity == null) throw new IllegalStateException("Provided formName: " + formName + " AND stepId: " + stepId + " are INVALID for current domain id: " + CloudToolsForCore.getDomainId());

        String body = request.getReader().lines().collect(Collectors.joining());
        if (Tools.isEmpty(body)) throw new IllegalStateException("Empty request body.");
        // !!! characters | and ~ are PROHIBITTED in form data - they are used as separators in form
        body = body.replace("|", "").replace("~", "");
        JSONObject received = new JSONObject(body);

        Map<String, String> errors = new HashMap<>();

        /* Prepare support values */
        FormSettingsEntity formSettings = formSettingsRepository.findByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId());
        Integer iLastDocId = getiLastDocId(formName, request, formSettings);
        boolean spamProtectionEnabled = getSpamProtectionEnabled(iLastDocId);

        /* check like CRSF and Cookies */
        beforeStepSaveCheck(spamProtectionEnabled, request);

        /* validate required / captcha / XSS (for names and values) */
        validateFields(formName, stepId, received, spamProtectionEnabled, request, errors);

        /* Separate validate file fields */
        validateFileFields(formName, formSettings, received, errors, request);

        // GET form processor that can have custom validation / interceptor / form save
        FormProcessorInterface formProcessor = getFormProcessor(request, formName, stepId, formSettings);

        // STEP VALIDATION
        customStepValidation(formProcessor, formName, validStepEntity, received, request, errors);

        if(errors.isEmpty()) {
            // RUN STEP INTERCEPTOR
            customStepInterceptor(formProcessor, formName, validStepEntity, received, request, errors);

            //Save step of form - its LOCAL save into session, NOT db save
            saveStepData(formName, stepId, received, request);

            // Validation success
            FormStepEntity nextStep = getNextStep(formName, validStepEntity, formStepsRepository);

            String forwardOk = null;
            if(nextStep == null) {
                // Set ok forward
                if (Tools.isNotEmpty(formSettings.getForward())) forwardOk = formSettings.getForward();

                // Because we are going save form for real now, add count to attempts of form submission
                formSettingsRepository.incrementResponseAttempts(formName, CloudToolsForCore.getDomainId());

                // HERE we can run custom saving of form
                boolean continueWithSaving = customFormSave(formProcessor, formName, request, formSettings, iLastDocId);

                if(continueWithSaving) {
                    // REAL form save into DB (will join all the steps and save it)
                    saveFormService.saveFormAnswers(formName, formSettings, iLastDocId, request);
                }
            }

            if(Tools.isNotEmpty(forwardOk)) response.put("forward", forwardOk);
            response.put("form-name", formName);
            response.put("step-id", nextStep == null ? -1L : nextStep.getId()); // -1L means that form ends, there is no more steps

        } else {
            // Add error to response to show to user
            response.put("fieldErrors", errors);

            // increment field error count
            formItemsRepository.incrementErrorCountByItemFormIds(formName, CloudToolsForCore.getDomainId(), new ArrayList<>(errors.keySet()));
        }
    }

    /* ********** PRIVATE - main logic methods ********** */

    /**
     * Instantiate optional custom form processor configured in form settings.
     *
     * @param request      HTTP request used for localized error messages
     * @param formName     logical form name
     * @param stepId       current step id
     * @param formSettings form settings containing processor class name
     * @return processor instance or {@code null} when no valid processor is configured
     * @throws SaveFormException when processor class cannot be instantiated
     */
    private FormProcessorInterface getFormProcessor(HttpServletRequest request, String formName, Long stepId, FormSettingsEntity formSettings) throws SaveFormException {
        String className = formSettings.getAfterSendInterceptor();
        if (Tools.isEmpty(className)) return null;

        try {
            // Load class dynamically
            Class<?> clazz = Class.forName(className);

            // The interface we want to check
            if (FormProcessorInterface.class.isAssignableFrom(clazz)) {
                // return instance
                return (FormProcessorInterface) clazz.getDeclaredConstructor().newInstance();
            } else {
                Logger.error(MultistepFormsService.class, "Provided form processor " + className + " , do NOT implement required FormProcessorInterface. For formName: " + formName + " stepId:" + stepId);
            }
        } catch (Exception e) {
            Logger.error(MultistepFormsService.class, "Failed to get instance of " + className + ". Cause: " + e.getLocalizedMessage());
            throw new SaveFormException(Prop.getInstance(request).getText("datatable.error.unknown"), false, null);
        }

        return null;
    }

    /**
     * Execute custom step validation hook.
     *
     * @param formProcessor optional processor implementation
     * @param formName      logical form name
     * @param stepEntity    current step entity
     * @param received      submitted step payload
     * @param request       current HTTP request
     * @param errors        mutable map for field-level validation errors
     * @throws SaveFormException when custom validation fails
     */
    private void customStepValidation(FormProcessorInterface formProcessor, String formName, FormStepEntity stepEntity, JSONObject received, HttpServletRequest request, Map<String, String> errors) throws SaveFormException {
        if(formProcessor != null) {
            try {
                formProcessor.validateStep(formName, stepEntity, received, request, errors);
            } catch (SaveFormException sfe) {
                throw sfe;
            } catch (Exception e) {
                Logger.error(MultistepFormsService.class, "FormName: " + formName + " stepId:" + stepEntity.getId() + " failed step validation. Cause: " + e.getLocalizedMessage());
                throw new SaveFormException(Prop.getInstance(request).getText("datatable.error.unknown"), false, null);
            }
        }
    }

    /**
     * Execute custom step interceptor hook after successful validation.
     *
     * @param formProcessor optional processor implementation
     * @param formName      logical form name
     * @param stepEntity    current step entity
     * @param received      submitted step payload
     * @param request       current HTTP request
     * @param errors        mutable map for interceptor-reported errors
     * @throws SaveFormException when custom interceptor fails
     */
    private void customStepInterceptor(FormProcessorInterface formProcessor, String formName, FormStepEntity stepEntity, JSONObject received, HttpServletRequest request, Map<String, String> errors) throws SaveFormException {
        if(formProcessor != null) {
            try {
                formProcessor.runStepInterceptor(formName, stepEntity, received, request, errors);
            } catch (SaveFormException sfe) {
                throw sfe;
            } catch (Exception e) {
                Logger.error(MultistepFormsService.class, "FormName: " + formName + " stepId:" + stepEntity.getId() + " failed run step interceptor. Cause: " + e.getLocalizedMessage());
                throw new SaveFormException(Prop.getInstance(request).getText("datatable.error.unknown"), false, null);
            }
        }
    }

    /**
     * @return
     *  TRUE - continue with WebJET basic save
     *  FALSE - skip basic save
     */
    private boolean customFormSave(FormProcessorInterface formProcessor, String formName, HttpServletRequest request, FormSettingsEntity formSettings, Integer iLastDocId) throws SaveFormException {
        if(formProcessor != null) {
            try {
                return formProcessor.handleFormSave(formName, formSettings, iLastDocId, request);
            } catch (SaveFormException sfe) {
                throw sfe;
            } catch (Exception e) {
                Logger.error(MultistepFormsService.class, "FormName: " + formName + " failed run custom save. Cause: " + e.getLocalizedMessage());
                throw new SaveFormException(Prop.getInstance(request).getText("datatable.error.unknown"), false, null);
            }
        }
        return true; // CONTINUE with basic saving
    }

    /**
     * Store validated step values into HTTP session using stable field keys.
     *
     * @param formName logical form name
     * @param stepId   current step id
     * @param received submitted step payload
     * @param request  HTTP request with session storage
     */
    private void saveStepData(String formName, Long stepId, JSONObject received, HttpServletRequest request) {
        String sessionKey = getSessionKey(formName, request);
        String prefix = sessionKey + "_";

        for(FormItemEntity stepItem : getStepItemsForValidation(stepId)) {
            String[] values = asArray(stepItem.getItemFormId(), received);
            String stringValue = Tools.join(values, ",");
            if("captcha".equals(stepItem.getFieldType())) {
                // Skip captcha fields
            } else {
                request.getSession().setAttribute(prefix + stepItem.getItemFormId(), stringValue);
            }
        }
    }

    /**
     * Validate non-file step fields including required flags, captcha, XSS and regex rules.
     *
     * @param formName              logical form name
     * @param stepId                current step id
     * @param received              submitted step payload
     * @param spamProtectionEnabled whether captcha/csrf protections are active
     * @param request               HTTP request with localization/session context
     * @param errors                mutable map collecting field validation errors
     * @throws SaveFormException when anti-spam XSS checks fail
     */
    private void validateFields(String formName, Long stepId, JSONObject received, boolean spamProtectionEnabled, HttpServletRequest request, Map<String, String> errors) throws SaveFormException {
        Prop prop = Prop.getInstance(request);
        List<RegExpEntity> allRegExps = FormDB.getInstance().getAllRegularExpressionAsEntity();
        List<FormItemEntity> stepItems = getStepItemsForValidation(stepId);

        FormConditionsHandler formConditionsHandler = new FormConditionsHandler(formName, request);

        for(FormItemEntity stepItem : stepItems) {
            // multiupload fields are validated in method validateFileFields()
            if(stepItem.getFieldType().startsWith(MULTIUPLOAD_PREFIX)) continue;

            // Skip validation for fields hidden by visibility conditions
            Boolean isHiddenByCondition = formConditionsHandler.isFieldHiddenByCondition(stepItem, received);
            if (Tools.isTrue(isHiddenByCondition)) continue;

            String itemFormId = stepItem.getItemFormId();
            String fieldName = getFieldName(stepItem, prop);

            // CAPTCHA check
            if("captcha".equals(stepItem.getFieldType())) {
                String value = received.optString("wjcaptcha", "");
                if(Tools.isEmpty(value)) {
                    //Can NOT be empty
                    errors.put(stepItem.getItemFormId(), fieldName + " - " + prop.getText("checkform.title.required"));
                } else if (spamProtectionEnabled == true && checkCaptcha(request, value) == false) {
                    errors.put(stepItem.getItemFormId(), fieldName + " - " + prop.getText("send_mail_error.captcha"));
                }
                continue;
            }

            // XSS check of name
            if (DocTools.testXss(fieldName) || fieldName.indexOf('"') != -1 || fieldName.indexOf('\'') != -1) {
                throw new SaveFormException(prop.getText("send_mail_error.probablySpamBot"), "probablySpamBot", false, null);
            }

            // XSS check of values
            for(String value : asArray(stepItem.getItemFormId(), received)) {
                if(DocTools.testXss(value)) {
                    throw new SaveFormException(prop.getText("send_mail_error.probablySpamBot"), "probablySpamBot", false, null);
                }
            }

            // Check if field is required (static flag or dynamic requirement conditions) - IF requiredByFields is set (is not null) use it as higher priority indicator
            Boolean requiredByFields = formConditionsHandler.isFieldRequiredByCondition(stepItem, received);
            boolean isRequired = requiredByFields == null ? Tools.isTrue(stepItem.getRequired()) : Tools.isTrue(requiredByFields);
            if(isRequired) {
                String[] values = asArray(itemFormId, received);

                if(values == null || values.length < 1) {
                    errors.put(itemFormId, fieldName + " - " + prop.getText("checkform.title.required"));
                    // continue; // no need to check regex if field is empty
                } else if("wysiwyg".equalsIgnoreCase(stepItem.getFieldType())) {
                    if(values.length == 1) {
                        String value = values[0].trim();
                        if (Tools.isEmpty(value) || "<br>".equals(value) || "<p></p>".equals(value) || Tools.isEmpty(Html2Text.html2text(value))) errors.put(itemFormId, fieldName + " - " + prop.getText("checkform.title.required"));
                    }
                }
            }

            for(RegExpEntity regExp : allRegExps) {
                boolean needTovalidate = false;
                for(int i : Tools.getTokensInt(stepItem.getRegexValidation(), "+")) {
                    if(regExp.getId().intValue() == i) {
                        needTovalidate = true;
                        break;
                    }
                }

                if(needTovalidate == false) continue;

                //need validation
				String regex = Tools.replace(regExp.getRegExp(), "\\\\", "\\");
                String value = received.optString(itemFormId, "");

                if(value.matches(regex) == false) {
                    if (DocTools.testXss(value)) value = "";
                    if (DocTools.testXss(itemFormId)) itemFormId = "";

                    value = ResponseUtils.filter(value);

                    String className = regExp.getType();
                    String regExpErr = "";
                    if(className.indexOf("email") != -1)
                        regExpErr = Tools.replace(prop.getText("converter.email.invalidEmail"), "{1}", value);
                    else if(className.indexOf("minLen") != -1)
                        regExpErr = Tools.replace(Tools.replace(prop.getText("validation.minlength.valueTooShort"), "{2}", className.replaceFirst("minLen", "")), "{0}", fieldName);
                    else if(className.indexOf("number") != -1)
                        regExpErr = Tools.replace(Tools.replace(prop.getText("converter.number.invalidNumber"), "{1}", value), "{0}", fieldName);
                    else
                        regExpErr = Tools.replace(Tools.replace(prop.getText("validation.expression.valueFailedExpression"), "{1}", value), "{0}", fieldName);

                    if(errors.containsKey(itemFormId)) {
                        errors.put(itemFormId, errors.get(itemFormId) + "\n" + regExpErr);
                    } else errors.put(itemFormId, regExpErr);
                }
            }
        }
    }

    /**
     * Validate multi-upload file fields including restrictions and duplicate file names.
     *
     * @param formName     logical form name
     * @param formSettings already-loaded form settings (may be null)
     * @param received     submitted step payload
     * @param errors       mutable map collecting file validation errors
     * @param request      HTTP request with localization context
     */
    private void validateFileFields(String formName, FormSettingsEntity formSettings, JSONObject received, Map<String, String> errors, HttpServletRequest request) throws SaveFormException {
        Prop prop = Prop.getInstance(request);

        String sessionKey = getSessionKey(formName, request);
        String fileSizeMapKey = sessionKey + ALL_FILES_SIZE_SESSION_KEY_SUFFIX;

        // Load existing file size map from session (fileKey -> sizeInKB)
        Map<String, Long> fileSizeMap = new HashMap<>();
        Object mapObj = request.getSession().getAttribute(fileSizeMapKey);
        if (mapObj instanceof Map<?, ?> existingMap) {
            for (Map.Entry<?, ?> entry : existingMap.entrySet()) {
                if (entry.getKey() instanceof String k && entry.getValue() instanceof Long v) {
                    fileSizeMap.put(k, v);
                }
            }
        }

        String[] uploadedFilesParamNameList = asArray("Multiupload.formElementName", received);
        if (uploadedFilesParamNameList == null || uploadedFilesParamNameList.length == 0) {
            return;
        }

        // Build restriction from already-loaded formSettings to avoid redundant DB queries
        FormFileRestriction restriction = FormSettingsService.getFileRestriction(formName, formSettings);

        XhrFileUploadService uploadService = XhrFileUploadServlet.getService();

        for (String uploadedFilesParamName : uploadedFilesParamNameList) {
            // Skip if there is no value for this parameter
            if (Tools.isEmpty(received.optString(uploadedFilesParamName, ""))) continue;

            Map<String, Integer> sameImageCount = new HashMap<>();
            StringBuilder fileNames = new StringBuilder(); // collected names (currently unused, kept for compatibility)

            String[] uploadedValues = asArray(uploadedFilesParamName, received);
            for (String uploadedValue : uploadedValues) {
                if (Tools.isEmpty(uploadedValue)) continue;

                for (String fileKey : Tools.getTokens(uploadedValue, ";")) {
                    if (Tools.isEmpty(fileKey)) continue;

                    // Validate restriction
                    if (restriction != null) {
                        String filePath = uploadService.getTempFilePath(fileKey);
                        Logger.debug(FormMailAction.class, "Multiupload, fileKey=" + fileKey + " path=" + filePath);
                        if (filePath != null) {
                            IwcmFile file = new IwcmFile(filePath);
                            String errMsg = restriction.isSentFileValid(file, prop);
                            if (file.exists() && errMsg != null) {
                                errors.merge(
                                    uploadedFilesParamName,
                                    errMsg,
                                    (oldVal, newVal) -> oldVal + "\n" + newVal
                                );
                            } else {
                                // Always put/overwrite size for this fileKey (handles re-submits)
                                long fileSizeInKB = file.length() / 1024;
                                fileSizeMap.put(fileKey, fileSizeInKB);
                            }
                        }
                    }

                    // Collect file name
                    String tempFileName = uploadService.getTempFileName(fileKey);
                    if (Tools.isNotEmpty(tempFileName)) {
                        if (fileNames.isEmpty() == false) fileNames.append(",");
                        fileNames.append(tempFileName);

                        String originalFileName = uploadService.getOriginalFileName(fileKey);
                        if (Tools.isNotEmpty(originalFileName))
                            sameImageCount.merge(originalFileName, 1, (oldValue, newValue) -> oldValue + newValue);
                    }
                }
            }

            // Check duplicates
            sameImageCount.forEach((fileName, count) -> {
                if (count > 1) {
                    errors.merge(
                        uploadedFilesParamName,
                        prop.getText("multistep_form.duplicate_file", fileName),
                        (oldVal, newVal) -> oldVal + "\n" + newVal
                    );
                }
            });
        }

        // Drop deleted/expired temp files from the session map (e.g., user removed an upload)
        fileSizeMap.entrySet().removeIf(e -> uploadService.getTempFilePath(e.getKey()) == null);

        // Save file size map back to session
        request.getSession().setAttribute(fileSizeMapKey, fileSizeMap);

        // Check combined size of all files across all steps
        if (restriction != null && restriction.getMaxCombinedSizeInKilobytes() > 0) {
            long totalSizeInKB = fileSizeMap.values().stream().mapToLong(Long::longValue).sum();
            if (totalSizeInKB > restriction.getMaxCombinedSizeInKilobytes()) {
                throw new SaveFormException(prop.getText("components.forms.combined_files_to_big_err", FileTools.formatFileSizeFromKb(restriction.getMaxCombinedSizeInKilobytes())), "bad_file", false, null);
            }
        }
    }

    /* ********** PRIVATE - support methods ********** */

    /**
     * Resolve document id used for current form submission context.
     *
     * @param formName     logical form name
     * @param request      current HTTP request
     * @param formSettings form settings which may override document id
     * @return resolved document id or {@code null} when unavailable
     */
    private final Integer getiLastDocId(String formName, HttpServletRequest request, FormSettingsEntity formSettings) {
        Integer iLastDocId = (Integer) request.getSession().getAttribute( getSessionKey(formName, request) + MultistepFormApp.DOC_ID );

        //niekedy je formular napr v pravom menu, potom sa neparsuje docid podla requestu, ale
        //sa mu musi presne povedat, kde sa ten formular nachadza
        if (formSettings.getUseFormDocId() != null && formSettings.getUseFormDocId() > 0)
            iLastDocId = formSettings.getUseFormDocId();

        return iLastDocId;
    }

    /**
     * Determine whether spam protection should be enabled for current submission context.
     *
     * @param iLastDocId resolved document id, may be {@code null}
     * @return {@code true} when spam protection is enabled
     */
    private final boolean getSpamProtectionEnabled(Integer iLastDocId) {
        boolean spamProtectionEnabled = true;

        if (iLastDocId != null) {
            DocDetails doc = DocDB.getInstance().getDoc(iLastDocId, -1, false);
            if (doc != null) {
                TemplateDetails temp = TemplatesDB.getInstance().getTemplate(doc.getTempId());
                if (temp != null) spamProtectionEnabled = temp.isDisableSpamProtection() == false;
            }
        }

        //conf value overrides everything
        if (Constants.getBoolean("spamProtection") == false) spamProtectionEnabled = false;

        return spamProtectionEnabled;
    }

    /**
     * Validate that step belongs to form/domain and session has permission to continue.
     *
     * @param formName      logical form name
     * @param currentStepId candidate step id
     * @param request       current HTTP request
     * @return valid step entity or {@code null} when validation fails
     */
    private final FormStepEntity getValidStepEntity(String formName, Long currentStepId, HttpServletRequest request) {
        if(Tools.isEmpty(formName)) return null;
        if(currentStepId < 1L) return null;
        FormStepEntity validStep = formStepsRepository.getValidStep(formName, currentStepId, CloudToolsForCore.getDomainId()).orElse(null);
        if(validStep == null) return null;
        else {
            Object permitted = request.getSession().getAttribute(getSessionKey(formName, request) + MultistepFormApp.PERMITTED);
            if (permitted == null) return null;
            if (permitted instanceof Boolean b)
                return Tools.isTrue(b) ? validStep : null;
            else return null;
        }
    }

    /**
     * Read JSON field value as non-empty string array.
     * <p>
     * Supports scalar string or JSON array input while filtering empty values.
     *
     * @param name     JSON key to read
     * @param received JSON payload
     * @return normalized array of submitted values, never {@code null}
     */
    private String[] asArray(String name, JSONObject received) {
        if(received.has(name) == false) return new String[0];

        Object raw = received.opt(name);
        if(raw == null) return new String[0];

        if (raw instanceof JSONArray arr) {
            List<String> notNull = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++)
                if(Tools.isNotEmpty(arr.getString(i))) notNull.add(arr.getString(i));
            return notNull.toArray(new String[0]);
        }

        if(Tools.isEmpty(raw.toString())) return new String[0];
        return new String[] {raw.toString()};
    }

    /**
     * Fetch minimal field definitions of a single step required by validation pipeline.
     *
     * @param stepId step identifier
     * @return list of simplified form item entities for the step
     */
    private List<FormItemEntity> getStepItemsForValidation(Long stepId) {
        String sql = "SELECT id, item_form_id, label, field_type, regex_validation, required FROM form_items WHERE step_id = ? AND domain_id = ?";

        List<FormItemEntity> values = new ArrayList<>();
        new ComplexQuery().setSql(sql).setParams(stepId, CloudToolsForCore.getDomainId()).list(new Mapper<FormItemEntity>() {
			@Override
			public FormItemEntity map(ResultSet rs) throws SQLException {
                values.add( resultSetToEntity(rs) );
                return null;
			}
		});

        return values;
    }

    /**
     * Fetch distinct minimal form item definitions required for validation for the form.
     *
     * @param formName logical form name
     * @return list of simplified {@code FormItemEntity} containing id, label, type, regex, required
     */
    public static List<FormItemEntity> getFormItemsForValidation(String formName) {
        String sql = "SELECT f.id, item_form_id, label, field_type, regex_validation, required FROM form_items f, form_steps s WHERE f.form_name = ? AND f.domain_id = ? AND f.step_id=s.id ORDER BY s.sort_priority ASC, f.sort_priority ASC";

        List<FormItemEntity> values = new ArrayList<>();
        new ComplexQuery().setSql(sql).setParams(formName, CloudToolsForCore.getDomainId()).list(new Mapper<FormItemEntity>() {
			@Override
			public FormItemEntity map(ResultSet rs) throws SQLException {
                FormItemEntity stepItem = resultSetToEntity(rs);

                // Radio's act like separe items but if they have same itemFormId AND are one after another they are GROUP (so as only one note in DB)
                String previous = values.size() > 0 ? values.get( values.size() - 1).getItemFormId() : "";
                if("radio".equals(stepItem.getFieldType()) &&  previous.equals(stepItem.getItemFormId())) return null;

                values.add(stepItem);

                return null;
			}
		});

        return values;
    }

    /**
     * Map SQL result row into simplified form item entity.
     *
     * @param rs SQL result set positioned on current row
     * @return mapped form item entity
     * @throws SQLException when reading row data fails
     */
    private static FormItemEntity resultSetToEntity(ResultSet rs) throws SQLException{
        FormItemEntity fe = new FormItemEntity();
        fe.setId( rs.getLong("id") );
        fe.setItemFormId( rs.getString("item_form_id") );
        fe.setLabel( rs.getString("label") );
        fe.setFieldType( rs.getString("field_type") );
        fe.setRegexValidation( rs.getString("regex_validation") );
        fe.setRequired( rs.getBoolean("required") );
        return fe;
    }

    /**
     * Perform pre-save anti-spam checks based on cookies and CSRF token.
     *
     * @param spamProtectionEnabled flag whether CSRF/captcha checks are enforced
     * @param request               current HTTP request
     * @throws SaveFormException when anti-spam checks fail
     */
    private void beforeStepSaveCheck(boolean spamProtectionEnabled, HttpServletRequest request) throws SaveFormException {
        Prop prop = Prop.getInstance(request);

        //test na cookies (spameri zvycajne nemaju nastavene)
		if (request.getCookies() == null || request.getCookies().length == 0) {
            throw new SaveFormException(prop.getText("send_mail_error.probablySpamBot"), "probablySpamBot", false, null);
        }

        // Check CRSF
		if (checkCsrf(request) == false) {
            throw new SaveFormException(prop.getText("send_mail_error.probablySpamBotCsrf"), "probablySpamBotCsrf", false, null);
        }
    }

    /**
     * Validate captcha response according to configured captcha provider.
     *
     * @param request         current HTTP request
     * @param captchaResponse submitted captcha token/value
     * @return {@code true} when captcha is valid or captcha validation is disabled
     */
    private boolean checkCaptcha(HttpServletRequest request, String captchaResponse) {
		String captchaType = Constants.getString("captchaType");
		if (Tools.isEmpty(captchaType)) captchaType = "none";

		if ("internal".equals(captchaType) == false) {
			//nie su nastavene premenne, takze nemozeme validovat
			if(!Constants.getBoolean("reCaptchaEnabled") || Tools.isEmpty(Constants.getString("reCaptchaSiteKey")) || Tools.isEmpty(Constants.getString("reCaptchaSecret")))
				captchaType = "none";
		}

		if ("none".equals(captchaType) == false)
			return Captcha.validateResponse(request, captchaResponse, null);

		return true;
	}

    /**
     * Validate CSRF token for AJAX form submission when CSRF protection is configured.
     *
     * @param request current HTTP request
     * @return {@code true} when token is valid or CSRF check is not required
     */
    private boolean checkCsrf(HttpServletRequest request) {
		String spamProtectionJavascript = Constants.getString("spamProtectionJavascript");
	    if (spamProtectionJavascript.contains("formmailCsrf") == false) {
			//csrf token sa nepouziva
			return true;
		}
		//return if is correct
		return CSRF.verifyTokenAjax(request.getSession(), request.getHeader("X-CSRF-Token"));
	}

    /**
     * Refresh step positions after row reorder operation payload.
     *
     * @param rowReorderDto datatable row reorder payload
     */
    public void updateStepsPositions(RowReorderDto rowReorderDto) {
        if(rowReorderDto == null) return;

        List<RowReorderValue> values = rowReorderDto.getValues();
        if(values == null || values.size() == 0) return;

        // Get form name by step id
        String formName = formStepsRepository.getFormNameByStepId(values.get(0).getId(), CloudToolsForCore.getDomainId()).orElse(null);
        if(Tools.isEmpty(formName)) return;

        // call update
        updateStepsPositions(formName);
    }

    /**
     * Recompute current and max positions for all steps in a form.
     *
     * @param formName logical form name
     */
    public void updateStepsPositions(String formName) {
        if(Tools.isEmpty(formName)) return;

        List<FormStepEntity> steps = formStepsRepository.findAllByFormNameAndDomainIdOrderBySortPriorityAsc(formName, CloudToolsForCore.getDomainId());
        List<FormStepEntity> stepsToUpdate = new ArrayList<>();

        int currentPosition = 1;
        int maxPosition = steps.size();

        for(FormStepEntity stepEntity : steps) {
            if(stepEntity.getCurrentPosition() != currentPosition || stepEntity.getMaxPosition() != maxPosition) {
                stepEntity.setCurrentPosition(currentPosition);
                stepEntity.setMaxPosition(maxPosition);
                stepsToUpdate.add(stepEntity);
            }
            currentPosition++;
        }

        if(stepsToUpdate.size() > 0) formStepsRepository.saveAll(stepsToUpdate);
    }
}
