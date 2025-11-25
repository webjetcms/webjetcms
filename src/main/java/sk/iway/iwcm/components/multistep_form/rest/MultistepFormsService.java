package sk.iway.iwcm.components.multistep_form.rest;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.Html2Text;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.components.forms.FormsEntity;
import sk.iway.iwcm.components.forms.FormsRepository;
import sk.iway.iwcm.components.forms.FormsService;
import sk.iway.iwcm.components.forms.RegExpEntity;
import sk.iway.iwcm.components.forms.RegExpRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.components.upload.XhrFileUploadService;
import sk.iway.iwcm.components.upload.XhrFileUploadServlet;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.form.FormAttributeDB;
import sk.iway.iwcm.form.FormDB;
import sk.iway.iwcm.form.FormFileRestriction;
import sk.iway.iwcm.form.FormMailAction;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.captcha.Captcha;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.users.UsersDB;

@Service
public class MultistepFormsService {

    private static final String ITEM_KEY_LABEL_PREFIX = "components.formsimple.label.";
    private static final String ITEM_KEY_HIDE_FIELDS_PREFIX = "components.formsimple.hide.";

    private final FormsRepository formsRepository;
    private final FormItemsRepository formItemsRepository;
    private final FormStepsRepository formStepsRepository;

    @Autowired
    public MultistepFormsService(FormsRepository formsRepository, FormItemsRepository formItemsRepository, FormStepsRepository formStepsRepository) {
        this.formsRepository = formsRepository;
        this.formItemsRepository = formItemsRepository;
        this.formStepsRepository = formStepsRepository;
    }

    /* ********** PUBLIC STATIC - small support methods ********** */

    public static final String getFormName(Map<String, String> params) {
        String formName = Tools.getStringValue(params.get("formName"), "");
        return DocTools.removeChars(formName, true);
    }

    public static final String getFormName(HttpServletRequest request) {
        String formName = Tools.getStringValue(request.getParameter("formName"), "");
        return DocTools.removeChars(formName, true);
    }

    public static final List<LabelValue> getRegExOptions(RegExpRepository regExpRepository, HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);
        List<LabelValue> options = new ArrayList<>();

        for(RegExpEntity regEx : regExpRepository.findAll())
            options.add( new LabelValue(prop.getText(regEx.getTitle()), regEx.getId() + ""));

        return options;
    }

    public static final List<LabelValue> getFieldTypes(HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);

        List<LabelValue> options = new ArrayList<>();
        Map<String, String> formsimpleFields = prop.getTextStartingWith(ITEM_KEY_LABEL_PREFIX);

        for(Entry<String, String> entry : formsimpleFields.entrySet())
            options.add(new LabelValue(entry.getValue(), entry.getKey().substring(ITEM_KEY_LABEL_PREFIX.length())));

        return options;
    }

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

    /* ********** PUBLIC - supprot methods ********** */

    public final List<LabelValue> getFormStepsOptions(String formName) {
        List<LabelValue> options = new ArrayList<>();

        if(Tools.isEmpty(formName)) return options;

        int counter = 1;
        for(FormStepEntity step : formStepsRepository.findAllByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId())) {
            options.add(new LabelValue(step.getStepName() + " (" + counter++ + ")", step.getId() + ""));
        }

        return options;
    }

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

        //Get form steps in order
        StringBuilder patternData = new StringBuilder();
        for(FormItemEntity stepItem : getFormItemsForValidation(formName)) {
            if(stepItem.getFieldType().startsWith("captcha")) continue; // captcha is not saved in DB
            if(stepItem.getFieldType().startsWith("multiupload")) continue; // skip multiupload fields in pattern, value from them is stored in separe column
            if(patternData.isEmpty()) patternData.append(stepItem.getItemFormId());
            else patternData.append("|~" + stepItem.getItemFormId());
        }

        patternEntity.setData(patternData.toString());
        formsRepository.save(patternEntity);
    }

    public final void saveFormStep(String formName, Long stepId, HttpServletRequest request, JSONObject response) throws IOException {
        if(validateFormInfo(formName, stepId, formStepsRepository) == false) throw new IllegalStateException("Provided formName: " + formName + " AND stepId: " + stepId + " are INVALID for current domain id: " + CloudToolsForCore.getDomainId());

        String body = request.getReader().lines().collect(Collectors.joining());
        if (Tools.isEmpty(body)) throw new IllegalStateException("Empty request body.");
        // !!! characters | and ~ are PROHIITTED in form data - they are used as separators in form
        body = body.replace("|", "").replace("~", "");
        JSONObject received = new JSONObject(body);

        Map<String, String> errors = validateFields(stepId, received, request, formItemsRepository);
        validateFileFields(formName, received, errors, request);

        if(errors == null || errors.size() < 1) {
            //Save step of form
            saveStepData(formName, stepId, received, request);

            // Validation success
            FormStepEntity nextStep = getNextStep(formName, stepId, formStepsRepository);

            if(nextStep == null) {
                //This was last step perform permanent save of form data
                saveFormAnswers(formName, formsRepository, formItemsRepository, formStepsRepository, request);
            }

            response.put("form-name", formName);
            response.put("step-id", nextStep == null ? -1L : nextStep.getId()); // -1L means that form ends, there is no more steps

        } else response.put("fieldErrors", errors);
    }

    public final String getFormStepHtml(String formName, Long stepId, HttpServletRequest request) {
        StringBuilder stepHtml = new StringBuilder();
        boolean isEmailRender = false;
        boolean rowView = false;

        if(validateFormInfo(formName, stepId, formStepsRepository) == false) throw new IllegalStateException("Provided formName: " + formName + " AND stepId: " + stepId + " are INVALID for current domain id: " + CloudToolsForCore.getDomainId());

        Prop prop = Prop.getInstance(request);

        String requiredLabelAdd = prop.getText("components.formsimple.requiredLabelAdd");
        Map<String, String> attributes = new FormAttributeDB().load(DocTools.removeChars(formName, true));

        String recipients = "";
        if (attributes!=null && Tools.isNotEmpty(attributes.get("recipients"))) recipients = attributes.get("recipients");

        Set<String> firstTimeHeadingSet = new HashSet<String>();

        stepHtml.append(FormsService.replaceFields(prop.getText("components.mustistep.form.start"), formName, recipients, null, requiredLabelAdd, isEmailRender, false, firstTimeHeadingSet, prop));
        if (rowView) stepHtml.append("<div class=\"row\">");

        //
        String newPath = "/rest/multistep-form/save-form?form-name=" + formName + "&step-id=" + stepId;
        Tools.replace(stepHtml, "${formActionSrc}", newPath);

        for(FormItemEntity stepItem : formItemsRepository.getAllStepItems(stepId, CloudToolsForCore.getDomainId())) {
            JSONObject item = new JSONObject(stepItem);
            String fieldType = item.getString("fieldType");

            item.put("labelOriginal", item.getString("label"));
            if (Tools.isEmpty(item.getString("label"))) {
                String label = prop.getText("components.formsimple.label." + fieldType);
                item.put("label", label);
            }

            String html = FormsService.replaceFields(prop.getText("components.formsimple.input." + fieldType), formName, recipients, item, requiredLabelAdd, isEmailRender, rowView, firstTimeHeadingSet, prop);
            if (html.contains("!INCLUDE"))
                html = EditorToolsForCore.renderIncludes(html, false, request);

            stepHtml.append(html);
        }

        if (rowView) stepHtml.append("</div>");
        stepHtml.append( FormsService.replaceFields(prop.getText("components.mustistep.form.end"), formName, recipients, null, requiredLabelAdd, isEmailRender, false, firstTimeHeadingSet, prop) );

        String submitButtonString = "";
        if(isLastStep(formName, stepId))
            submitButtonString = prop.getText("components.mustistep.form.save_form");
        else submitButtonString = prop.getText("components.mustistep.form.next_step");

        Tools.replace(stepHtml, "${submitButtonText}", submitButtonString);

        //
        return stepHtml.toString();
    }

    /* ********** PRIVATE - main logic methods ********** */

    private void saveStepData(String formName, Long stepId, JSONObject received, HttpServletRequest request) {
        String prefix = UsersDB.getCurrentUser(request).getUserId() + "_" + formName + "_";

        for(FormItemEntity stepItem : getStepItemsForValidation(stepId)) {
            String[] values = asArray(stepItem.getItemFormId(), received);
            String stringValue = Tools.join(values, ",");
            if("captcha".equals(stepItem.getFieldType())) continue;
            else if(stepItem.getFieldType().startsWith("multiupload")) {
                // All multiupload files are stored in session under ONE special key
                String files = (String) request.getSession().getAttribute(prefix + "multiupload");
                if(files == null) files = "";
                request.getSession().setAttribute(prefix + "multiupload", files + "," + stringValue);
            } else request.getSession().setAttribute(prefix + stepItem.getItemFormId(), stringValue);
        }
    }

    private String saveFormAnswers(String formName, FormsRepository formsRepository, FormItemsRepository formItemsRepository, FormStepsRepository formStepsRepository, HttpServletRequest request) {
        FormsEntity form = new FormsEntity();
        form.setFormName(formName);
        form.setDomainId(CloudToolsForCore.getDomainId());
        form.setCreateDate(new Date());
        form.setDocId(0);
        form.setUserDetails(UsersDB.getCurrentUser(request));

        //
        String prefix = form.getUserDetails().getUserId() + "_" + formName + "_";
        StringBuilder data = new StringBuilder();

        // Enumerate session attributes (Enumeration is not directly iterable by for-each)
        java.util.Enumeration<String> attrNames = request.getSession().getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String sessionAttribute = attrNames.nextElement();
            if(sessionAttribute.startsWith(prefix) == false) continue;

            //Special case put in in separe column
            if(sessionAttribute.endsWith("multiupload")) {
                form.setFiles( (String) request.getSession().getAttribute(sessionAttribute) );
                continue;
            }

            data.append( sessionAttribute.substring(prefix.length()) ).append("~");
            data.append(request.getSession().getAttribute(sessionAttribute)).append("|");
        }

        //Remove last "|"
        data.deleteCharAt(data.length() - 1);
        form.setData( data.toString() );

        formsRepository.save(form);

        return null;
    }

    private Map<String, String> validateFields(Long stepId, JSONObject received, HttpServletRequest request, FormItemsRepository repo) {
        Prop prop = Prop.getInstance(request);
        Map<String, String> errors = new HashMap<>();
        List<RegExpEntity> allRegExps = FormDB.getInstance().getAllRegularExpressionAsEntity();

        for(FormItemEntity stepItem : getStepItemsForValidation(stepId)) {
            String itemFormId = stepItem.getItemFormId();

            String fieldName = "";
            if(Tools.isEmpty(stepItem.getLabel())) fieldName = prop.getText("components.formsimple.label." + stepItem.getFieldType());
            else fieldName = new Html2Text( StringEscapeUtils.unescapeHtml4(stepItem.getLabel()) ).getText();
            if (DocTools.testXss(fieldName)) fieldName = "";

            // CAPTCHA is special case and need to be handled separately
            if("captcha".equals(stepItem.getFieldType())) {
                String value = received.getString("wjcaptcha");
                boolean isValid =  Captcha.isReponseCorrect(request, value);
                if(isValid == false)
                    errors.put(itemFormId, fieldName + " - " + prop.getText("checkform.title.required"));

                continue; // captcha cant have regex validation
            }

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
				String regex = Tools.replace(regExp.getRegExp(), "\\\\", "\\").toLowerCase();
                String value = received.optString(itemFormId, "").toLowerCase();

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

        return errors;
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

            //nastav hodnotu input pola so zoznamom suborov, aby sa nam to pekne vyrenderovalo v maile a HTML verzii
			//((IwcmRequest)request).setParameter(uploadedFilesParamName+"-fileNames", fileNames.toString());
        }
    }

    /* ********** PRIVATE - support methods ********** */

    private boolean isLastStep(String formName, Long currentStepId) {
        return getNextStep(formName, currentStepId, formStepsRepository) == null;
    }

    private boolean validateFormInfo(String formName, Long currentStepId, FormStepsRepository repo) {
        if(Tools.isEmpty(formName)) return false;
        if(currentStepId < 1L) return false;
        return repo.validationStepCount(formName, currentStepId, CloudToolsForCore.getDomainId()) == 1; //must be EXACTLY ONE
    }

    private FormStepEntity getNextStep(String formName, Long currentStepId, FormStepsRepository repo) {
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

    private List<FormItemEntity> getFormItemsForValidation(String formName) {
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

    private FormItemEntity resultSetToEntity(ResultSet rs) throws SQLException{
        FormItemEntity fe = new FormItemEntity();
        fe.setItemFormId( rs.getString("item_form_id") );
        fe.setLabel( rs.getString("label") );
        fe.setFieldType( rs.getString("field_type") );
        fe.setRegexValidation( rs.getString("regex_validation") );
        fe.setRequired( rs.getBoolean("required") );
        return fe;
    }
}