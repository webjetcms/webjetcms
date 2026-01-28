package sk.iway.iwcm.components.multistep_form.rest;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.Html2Text;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.FormsEntity;
import sk.iway.iwcm.components.forms.FormsRepository;
import sk.iway.iwcm.components.forms.RegExpEntity;
import sk.iway.iwcm.components.forms.RegExpRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.components.multistep_form.support.FormProcessorInterface;
import sk.iway.iwcm.components.multistep_form.support.SaveFormException;
import sk.iway.iwcm.components.upload.XhrFileUploadService;
import sk.iway.iwcm.components.upload.XhrFileUploadServlet;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
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
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.system.stripes.CSRF;
import sk.iway.iwcm.tags.support.ResponseUtils;

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
    public static final String MULTIUPLOAD_PREFIX = "multiupload_";

    private static final String ITEM_KEY_LABEL_PREFIX = "components.formsimple.label.";
    private static final String ITEM_KEY_HIDE_FIELDS_PREFIX = "components.formsimple.hide.";
    private static final String ITEM_KEY_INPUT_PREFIX = "components.formsimple.input.";

    private final SaveFormService saveFormService;
    private final FormsRepository formsRepository;
    private final FormItemsRepository formItemsRepository;
    private final FormStepsRepository formStepsRepository;
    private final FormSettingsRepository formSettingsRepository;

    private FormSettingsEntity formSettings = null;
    private Integer iLastDocId = null;
    private boolean spamProtectionEnabled = true;

    private DocDB docDB = DocDB.getInstance();

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
        return DocTools.removeChars(formName, true);
    }

    /**
     * Extract and sanitize the {@code formName} from the request.
     *
     * @param request HTTP request containing parameter {@code formName}
     * @return sanitized form name (safe for identifiers)
     */
    public static final String getFormName(HttpServletRequest request) {
        String formName = Tools.getStringValue(request.getParameter("formName"), "");
        return DocTools.removeChars(formName, true);
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
    public static final List<LabelValue> getFieldTypes(HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);

        List<LabelValue> options = new ArrayList<>();
        Map<String, String> formsimpleFields = prop.getTextStartingWith(ITEM_KEY_LABEL_PREFIX);

        for(Entry<String, String> entry : formsimpleFields.entrySet()) {
            // Get html code of this field type
            String htmlCode = prop.getText(ITEM_KEY_INPUT_PREFIX + entry.getKey().substring(ITEM_KEY_LABEL_PREFIX.length()));
            // Skip prohibited submit buttons
            if(htmlCode != null && (htmlCode.toLowerCase().contains("type=\"submit\"") || htmlCode.toLowerCase().contains("type=\'submit\'")) )
                continue;

            options.add(new LabelValue(entry.getValue(), entry.getKey().substring(ITEM_KEY_LABEL_PREFIX.length())));
        }

        return options;
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


        options.add(new LabelValue("id,formName", "allwaysHidden"));
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
    public static final FormStepEntity getNextStep(String formName, Long currentStepId, FormStepsRepository repo) {
        List<FormStepEntity> steps = repo.findAllByFormNameAndDomainIdOrderBySortPriorityAsc(formName, CloudToolsForCore.getDomainId());

        for(int i = 0; i < steps.size(); i++) {
            if(steps.get(i).getId() == currentStepId) {
                //This step is current - set next one if is there
                if((i + 1) < steps.size()) return steps.get(i + 1);
                else return null; // there is no next step
            }
        }

        throw new IllegalStateException("Give currentStepId: " + currentStepId + " for form " + formName + " does NOT exist");
    }

    public static final List<String> getRowViewItemTypes() {
        return List.of("novy-riadok", "prazdny-stlpec");
    }

    /**
     * Determine the position of a step within the ordered form sequence.
     *
     * @param formName logical form name
     * @param stepId   step identifier
     * @param repo     repository used to fetch ordered steps
     * @return index if found, otherwise -1
     */
    public static int getStepPositionIndex(String formName, Long stepId, FormStepsRepository repo) {
        int index = 1;
        boolean found = false;
        for(FormStepEntity formStep : repo.findAllByFormNameAndDomainIdOrderBySortPriorityAsc(formName, CloudToolsForCore.getDomainId())) {
            if(stepId.equals(formStep.getId())) {
                found = true;
                break;
            }
            index++;
        }

        if(found) return index;
        else return -1; //not found
    }

    /* ********** PUBLIC - supprot methods ********** */

    /**
     * Build human-readable options for steps of a form suitable for selection inputs.
     *
     * @param formName logical form name
     * @return list of label/value pairs ordered by step sorting
     */
    public final List<LabelValue> getFormStepsOptions(String formName) {
        List<LabelValue> options = new ArrayList<>();

        if(Tools.isEmpty(formName)) return options;

        int counter = 1;
        for(FormStepEntity step : formStepsRepository.findAllByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId())) {
            options.add(new LabelValue(step.getStepName() + " (" + counter++ + ")", step.getId() + ""));
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
            String baseFormId = Tools.getStringValue(entity.getLabel(), "");
            baseFormId = StringEscapeUtils.unescapeHtml4(baseFormId);

            String itemFormId = "";
            String placeholder = entity.getPlaceholder();
            if (Tools.isNotEmpty(placeholder)) {
                placeholder = ResponseUtils.filter(placeholder);
                if(Tools.isEmpty(baseFormId)) itemFormId = placeholder + postfix;
            } else itemFormId = baseFormId + postfix;

            return DocTools.removeChars(itemFormId, true);
        }

        String itemFormId = entity.getFieldType();
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
            else patternData.append("|~" + stepItem.getItemFormId());

            if(stepItem.getFieldType().startsWith(MULTIUPLOAD_PREFIX)) patternData.append("-fileNames");
        }

        patternEntity.setData(patternData.toString());
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
        if(validateFormInfo(formName, stepId) == false) throw new IllegalStateException("Provided formName: " + formName + " AND stepId: " + stepId + " are INVALID for current domain id: " + CloudToolsForCore.getDomainId());

        String body = request.getReader().lines().collect(Collectors.joining());
        if (Tools.isEmpty(body)) throw new IllegalStateException("Empty request body.");
        // !!! characters | and ~ are PROHIBITTED in form data - they are used as separators in form
        body = body.replace("|", "").replace("~", "");
        JSONObject received = new JSONObject(body);

        Map<String, String> errors = new HashMap<>();

        /*  */
        prepareBeforeSave(formName, request);

        /* check like CRSF and Cookies */
        beforeStepSaveCheck(request);

        /* validate required / captcha / XSS (for names and values) */
        validateFields(formName, stepId, received, request, errors);

        /* Separe validate file fields */
        validateFileFields(formName, received, errors, request);

        // GET form processor that can have custom validation / interceptor / form save
        FormProcessorInterface formProcessor = getFormProcessor(request, formName, stepId);

        // STEP VALIDATION
        customStepValidation(formProcessor, formName, stepId, received, request, errors);

        if(errors == null || errors.size() < 1) {
            // RUN STEP INTERCEPTOR
            customStepInterceptor(formProcessor, formName, stepId, received, request, errors);

            //Save step of form - its LOCAL save into session, NOT db save
            saveStepData(formName, stepId, received, request);

            // Validation success
            FormStepEntity nextStep = getNextStep(formName, stepId, formStepsRepository);

            String forwardOk = null;
            if(nextStep == null) {
                // Set ok forward
                if (Tools.isNotEmpty(formSettings.getForward())) forwardOk = formSettings.getForward();

                // HERE we can run custom saving of form
                boolean continueWithSaving = customFormSave(formProcessor, formName, request);

                if(continueWithSaving) {
                    // REAL form save into DB (will join all the steps and save it)
                    saveFormService.saveFormAnswers(formName, formSettings, iLastDocId, request);
                }
            }

            if(Tools.isNotEmpty(forwardOk)) response.put("forward", forwardOk);
            response.put("form-name", formName);
            response.put("step-id", nextStep == null ? -1L : nextStep.getId()); // -1L means that form ends, there is no more steps

        } else response.put("fieldErrors", errors);
    }

    /* ********** PRIVATE - main logic methods ********** */

    private FormProcessorInterface getFormProcessor(HttpServletRequest request, String formName, Long stepId) throws SaveFormException {
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

    private void customStepValidation(FormProcessorInterface formProcessor, String formName, Long stepId, JSONObject received, HttpServletRequest request, Map<String, String> errors) throws SaveFormException {
        if(formProcessor != null) {
            try {
                formProcessor.validateStep(formName, stepId, received, request, errors);
            } catch (SaveFormException sfe) {
                throw sfe;
            } catch (Exception e) {
                Logger.error(MultistepFormsService.class, "FormName: " + formName + " stepId:" + stepId + " failed step validation. Cause: " + e.getLocalizedMessage());
                throw new SaveFormException(Prop.getInstance(request).getText("datatable.error.unknown"), false, null);
            }
        }
    }

    private void customStepInterceptor(FormProcessorInterface formProcessor, String formName, Long stepId, JSONObject received, HttpServletRequest request, Map<String, String> errors) throws SaveFormException {
        if(formProcessor != null) {
            try {
                formProcessor.runStepInterceptor(formName, stepId, received, request, errors);
            } catch (SaveFormException sfe) {
                throw sfe;
            } catch (Exception e) {
                Logger.error(MultistepFormsService.class, "FormName: " + formName + " stepId:" + stepId + " failed run step interceptor. Cause: " + e.getLocalizedMessage());
                throw new SaveFormException(Prop.getInstance(request).getText("datatable.error.unknown"), false, null);
            }
        }
    }

    /**
     * @return
     *  TRUE - continue with WebJET basic save
     *  FALSE - skip basic save
     */
    private boolean customFormSave(FormProcessorInterface formProcessor, String formName, HttpServletRequest request) throws SaveFormException {
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

    private void saveStepData(String formName, Long stepId, JSONObject received, HttpServletRequest request) {
        String prefix = getSessionKey(formName, request) + "_";

        for(FormItemEntity stepItem : getStepItemsForValidation(stepId)) {
            String[] values = asArray(stepItem.getItemFormId(), received);
            String stringValue = Tools.join(values, ",");
            if("captcha".equals(stepItem.getFieldType())) continue;
            else request.getSession().setAttribute(prefix + stepItem.getItemFormId(), stringValue);
        }
    }

    private void validateFields(String formName, Long stepId, JSONObject received, HttpServletRequest request, Map<String, String> errors) throws SaveFormException {
        Prop prop = Prop.getInstance(request);
        List<RegExpEntity> allRegExps = FormDB.getInstance().getAllRegularExpressionAsEntity();

        for(FormItemEntity stepItem : getStepItemsForValidation(stepId)) {

            //
            if(stepItem.getFieldType().startsWith(MULTIUPLOAD_PREFIX)) continue;

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
            if (DocTools.testXss(fieldName) || fieldName.indexOf('"') != -1 || fieldName.indexOf('\'') != -1)
                throw new SaveFormException(prop.getText("send_mail_error.probablySpamBot"), false, null);

            // XSS check of values
            for(String value : asArray(stepItem.getItemFormId(), received))
                if(DocTools.testXss(value))
                    throw new SaveFormException(prop.getText("send_mail_error.probablySpamBot"), false, null);

            // Check if field is required
            if(Tools.isTrue(stepItem.getRequired())) {
                String[] values = asArray(itemFormId, received);
                if(values == null || values.length < 1) {
                    errors.put(itemFormId, fieldName + " - " + prop.getText("checkform.title.required"));
                    // continue; // no need to check regex if field is empty
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

    private void validateFileFields(String formName, JSONObject received, Map<String, String> errors, HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);

        String[] uploadedFilesParamNameList = asArray("Multiupload.formElementName", received);
        if (uploadedFilesParamNameList == null || uploadedFilesParamNameList.length == 0) return;

        FormFileRestriction restriction = null;
        if (FormDB.isThereFileRestrictionFor(formName)) restriction = FormDB.getFileRestrictionFor(formName);

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
                            if (file.exists() && restriction.isSentFileValid(file) == false) {
                                errors.merge(
                                    uploadedFilesParamName,
                                    prop.getText("multistep_form.bad_file", XhrFileUploadService.getOriginalFileName(file)),
                                    (oldVal, newVal) -> oldVal + "\n" + newVal
                                );
                            }
                        }
                    }

                    // Collect file name
                    String tempFileName = uploadService.getTempFileName(fileKey);
                    if (Tools.isNotEmpty(tempFileName)) {
                        if (fileNames.length() > 0) fileNames.append(",");
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
    }

    /* ********** PRIVATE - support methods ********** */

    private boolean validateFormInfo(String formName, Long currentStepId) {
        if(Tools.isEmpty(formName)) return false;
        if(currentStepId < 1L) return false;
        return formStepsRepository.validationStepCount(formName, currentStepId, CloudToolsForCore.getDomainId()) == 1; //must be EXACTLY ONE
    }

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

    private List<FormItemEntity> getStepItemsForValidation(Long stepId) {
        String sql = "SELECT DISTINCT(item_form_id), label, field_type, regex_validation, required FROM form_items WHERE step_id = ? AND domain_id = ?";

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
        String sql = "SELECT DISTINCT(item_form_id), label, field_type, regex_validation, required FROM form_items WHERE form_name = ? AND domain_id = ?";

        List<FormItemEntity> values = new ArrayList<>();
        new ComplexQuery().setSql(sql).setParams(formName, CloudToolsForCore.getDomainId()).list(new Mapper<FormItemEntity>() {
			@Override
			public FormItemEntity map(ResultSet rs) throws SQLException {
                values.add( resultSetToEntity(rs) );
                return null;
			}
		});

        return values;
    }

    private static FormItemEntity resultSetToEntity(ResultSet rs) throws SQLException{
        FormItemEntity fe = new FormItemEntity();
        fe.setItemFormId( rs.getString("item_form_id") );
        fe.setLabel( rs.getString("label") );
        fe.setFieldType( rs.getString("field_type") );
        fe.setRegexValidation( rs.getString("regex_validation") );
        fe.setRequired( rs.getBoolean("required") );
        return fe;
    }

    private void prepareBeforeSave (String formName, HttpServletRequest request) {
        formSettings = formSettingsRepository.findByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId());

        iLastDocId = (int) request.getSession().getAttribute( getSessionKey(formName, request) );

        //niekedy je formular napr v pravom menu, potom sa neparsuje docid podla requestu, ale
		//sa mu musi presne povedat, kde sa ten formular nachadza
		if (formSettings.getUseFormDocId() != null && formSettings.getUseFormDocId() > 0)
			iLastDocId = formSettings.getUseFormDocId();

		if (iLastDocId != null) {
            DocDetails doc = docDB.getDoc(iLastDocId, -1, false);
            if (doc != null) {
                TemplateDetails temp = TemplatesDB.getInstance().getTemplate(doc.getTempId());
                if (temp != null) spamProtectionEnabled = temp.isDisableSpamProtection() == false;
            }
        }

        //conf value overrides everything
		if (Constants.getBoolean("spamProtection") == false) spamProtectionEnabled = false;
    }

    private void beforeStepSaveCheck(HttpServletRequest request) throws SaveFormException {
        Prop prop = Prop.getInstance(request);

        //test na cookies (spameri zvycajne nemaju nastavene)
		if (request.getCookies() == null || request.getCookies().length == 0)
            throw new SaveFormException(prop.getText("send_mail_error.probablySpamBot"), false, null);

        // Check CRSF
		if (spamProtectionEnabled && checkCsrf(request) == false)
            throw new SaveFormException(prop.getText("send_mail_error.probablySpamBotCsrf"), false, null);
    }

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

    private String getFieldName (FormItemEntity stepItem, Prop prop) {
        String fieldName = "";
        if(Tools.isEmpty(stepItem.getLabel())) fieldName = prop.getText("components.formsimple.label." + stepItem.getFieldType());
        else fieldName = new Html2Text( StringEscapeUtils.unescapeHtml4(stepItem.getLabel()) ).getText();
        return fieldName;
    }

    private boolean checkCsrf(HttpServletRequest request) {
		String spamProtectionJavascript = Constants.getString("spamProtectionJavascript");
	    if (spamProtectionJavascript.contains("formmailCsrf") == false) {
			//csrf token sa nepouziva
			return true;
		}
		//return if is correct
		return CSRF.verifyTokenAjax(request.getSession(), request.getHeader("X-CSRF-Token"));
	}
}