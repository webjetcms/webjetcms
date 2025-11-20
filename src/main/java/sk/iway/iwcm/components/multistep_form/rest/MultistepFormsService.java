package sk.iway.iwcm.components.multistep_form.rest;

import java.io.IOException;
import java.util.ArrayList;
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

import sk.iway.Html2Text;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.components.forms.FormsService;
import sk.iway.iwcm.components.forms.RegExpEntity;
import sk.iway.iwcm.components.forms.RegExpRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.components.upload.XhrFileUploadService;
import sk.iway.iwcm.components.upload.XhrFileUploadServlet;
import sk.iway.iwcm.form.FormAttributeDB;
import sk.iway.iwcm.form.FormDB;
import sk.iway.iwcm.form.FormFileRestriction;
import sk.iway.iwcm.form.FormMailAction;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.captcha.Captcha;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.tags.support.ResponseUtils;

public class MultistepFormsService {

    private static final String ITEM_KEY_LABEL_PREFIX = "components.formsimple.label.";
    private static final String ITEM_KEY_HIDE_FIELDS_PREFIX = "components.formsimple.hide.";

    public static String getFormName(Map<String, String> params) {
        String formName = Tools.getStringValue(params.get("formName"), "");
        return DocTools.removeChars(formName, true);
    }

    public static String getFormName(HttpServletRequest request) {
        String formName = Tools.getStringValue(request.getParameter("formName"), "");
        return DocTools.removeChars(formName, true);
    }

    public static List<LabelValue> getFormStepsOptions(String formName, FormStepsRepository formStepsRepository) {
        List<LabelValue> options = new ArrayList<>();

        if(Tools.isEmpty(formName)) return options;

        int counter = 1;
        for(FormStepEntity step : formStepsRepository.findAllByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId())) {
            options.add(new LabelValue(step.getStepName() + " (" + counter++ + ")", step.getId() + ""));
        }

        return options;
    }

    public static List<LabelValue> getRegExOptions(RegExpRepository regExpRepository, HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);
        List<LabelValue> options = new ArrayList<>();

        for(RegExpEntity regEx : regExpRepository.findAll())
            options.add( new LabelValue(prop.getText(regEx.getTitle()), regEx.getId() + ""));

        return options;
    }

    public static List<LabelValue> getFieldTypes(HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);

        List<LabelValue> options = new ArrayList<>();
        Map<String, String> formsimpleFields = prop.getTextStartingWith(ITEM_KEY_LABEL_PREFIX);

        for(Entry<String, String> entry : formsimpleFields.entrySet())
            options.add(new LabelValue(entry.getValue(), entry.getKey().substring(ITEM_KEY_LABEL_PREFIX.length())));

        return options;
    }

    public static List<LabelValue> getFiledTypeVisibility(HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);

        List<LabelValue> options = new ArrayList<>();
        Map<String, String> formsimpleFields = prop.getTextStartingWith(ITEM_KEY_HIDE_FIELDS_PREFIX);

        for(Entry<String, String> entry : formsimpleFields.entrySet()) {
            options.add(new LabelValue(entry.getValue(), entry.getKey().substring(ITEM_KEY_HIDE_FIELDS_PREFIX.length())));
        }


        options.add(new LabelValue("id,formName", "allwaysHidden"));
        return options;
    }

    private static boolean validateFormInfo(String formName, Long currentStepId, FormStepsRepository repo) {
        if(Tools.isEmpty(formName)) return false;
        if(currentStepId < 1L) return false;
        return repo.validationStepCount(formName, currentStepId, CloudToolsForCore.getDomainId()) == 1; //must be EXACTLY ONE
    }

    private static  FormStepEntity getNextStep(String formName, Long currentStepId, FormStepsRepository repo) {
        List<FormStepEntity> steps = repo.findAllByFormNameAndDomainIdOrderBySortPriorityAsc(formName, CloudToolsForCore.getDomainId());

        for(int i = 0; i < steps.size(); i++) {
            if(steps.get(i).getId() == currentStepId) {
                //This step is current - set next one if is there
                if((i + 1) < steps.size()) {
                    return steps.get(i + 1);
                }
                else return null; // there is no next step
            }
        }

        throw new IllegalStateException("Give currentStepId: " + currentStepId + " for form " + formName + " does NOT exist");
    }

    private static String[] asArray(String name, JSONObject received) {
        if(received.has(name) == false) return new String[0];

        Object raw = received.opt(name);
        if(raw == null) return new String[0];

        if (raw instanceof JSONArray arr) {
            String[] result = new String[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                result[i] = arr.getString(i);
            }
            return result;
        }

        return new String[] {raw.toString()};
    }

    private static void validateFileFields(String formName, JSONObject received, Map<String, String> errors, HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);
		//skontroluj multiupload subory
		String[] uploadedFilesParamNameList = asArray("Multiupload.formElementName", received);
		if (uploadedFilesParamNameList != null && uploadedFilesParamNameList.length>0)
		{
			//mame to tu znova keby padlo mulipart parsovanie
			FormFileRestriction restriction = null;
			if (FormDB.isThereFileRestrictionFor(formName)) restriction = FormDB.getFileRestrictionFor(formName);

			//over subory uploadnute cez HTML5 upload
			for (String uploadedFilesParamName : uploadedFilesParamNameList)
			{
				if (Tools.isNotEmpty(received.optString(uploadedFilesParamName, "")))
				{

                    Map<String, Integer> sameImageCount = new HashMap<>();

					StringBuilder fileNames = new StringBuilder();

					for (String keys : asArray(uploadedFilesParamName, received))
					{
						for (String fileKey : Tools.getTokens(keys, ";"))
						{
							if (restriction!=null)
							{
								String filePath = XhrFileUploadServlet.getService().getTempFilePath(fileKey);
								Logger.debug(FormMailAction.class, "Multiupload, fileKey=" + fileKey + " path=" + filePath);
								if (filePath != null) {
									IwcmFile file = new IwcmFile(filePath);
									if (file.exists()) {
                                        if (restriction.isSentFileValid(file) == false)
                                            errors.merge(uploadedFilesParamName, prop.getText("multistep_form.bad_file", XhrFileUploadService.getOriginalFileName(file)), (oldVal, newVal) -> oldVal + "\n" + newVal);
									}
								}
							}
							String fileName = XhrFileUploadServlet.getService().getTempFileName(fileKey);
							if (Tools.isNotEmpty(fileName)) {
								if (fileNames.length()>0) fileNames.append(", ");
								fileNames.append(fileName);

                                sameImageCount.merge(XhrFileUploadServlet.getService().getOriginalFileName(fileKey), 1, (oldValue, newValue) -> oldValue + newValue);
							}
						}
					}

                    sameImageCount.forEach((fileName, count) -> {
                        if (count > 1) {
                            errors.merge(uploadedFilesParamName, prop.getText("multistep_form.duplicate_file", fileName), (oldVal, newVal) -> oldVal + "\n" + newVal);
                        }
                    });

                    // TODO treba ?
					//nastav hodnotu input pola so zoznamom suborov, aby sa nam to pekne vyrenderovalo v maile a HTML verzii
					//((IwcmRequest)request).setParameter(uploadedFilesParamName+"-fileNames", fileNames.toString());
				}
			}
		}

        String pes = "";
    }

    private static Map<String, String> validateFields(Long stepId, JSONObject received, HttpServletRequest request, FormItemsRepository repo) {
        Prop prop = Prop.getInstance(request);
        Map<String, String> errors = new HashMap<>();
        List<RegExpEntity> allRegExps = FormDB.getInstance().getAllRegularExpressionAsEntity();

        for(FormItemEntity stepItem : repo.findAllByStepIdAndDomainIdOrderBySortPriorityAsc(stepId, CloudToolsForCore.getDomainId())) {
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
                String value = received.getString(itemFormId);
                if(Tools.isEmpty(value)) {
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
                String value = received.getString(itemFormId).toLowerCase();

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

    public static void validateStepAndSetNextStep(String formName, Long stepId, HttpServletRequest request, JSONObject response, FormItemsRepository formItemsRepository, FormStepsRepository formStepsRepository) throws IOException {
        if(validateFormInfo(formName, stepId, formStepsRepository) == false) throw new IllegalStateException("Provided formName: " + formName + " AND stepId: " + stepId + " are INVALID for current domain id: " + CloudToolsForCore.getDomainId());

        String body = request.getReader().lines().collect(Collectors.joining());
        if (Tools.isEmpty(body)) throw new IllegalStateException("Empty request body.");
        JSONObject received = new JSONObject(body);

        Map<String, String> errors = validateFields(stepId, received, request, formItemsRepository);
        validateFileFields(formName, received, errors, request);

        if(errors == null || errors.size() < 1) {
            // Validation success
            FormStepEntity nextStep = getNextStep(formName, stepId, formStepsRepository);
            response.put("form-name", formName);
            response.put("step-id", nextStep == null ? -1L : nextStep.getId()); // -1L means that form ends, there is no more steps

        } else response.put("fieldErrors", errors);
    }

    public static String getFormStepHtml(String formName, Long stepId, HttpServletRequest request, FormStepsRepository formStepsRepository, FormItemsRepository formItemsRepository) {
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

        stepHtml.append(FormsService.replaceFields(prop.getText("components.formsimple.form.start"), formName, recipients, null, requiredLabelAdd, isEmailRender, false, firstTimeHeadingSet, prop));
        if (rowView) stepHtml.append("<div class=\"row\">");

        //
        String oldPath = "action=\"/formmail.do?savedb=kontaktny_formular\"";
        int start = stepHtml.indexOf(oldPath);
        int end = start + oldPath.length();

        String newPath = "action=\"/rest/multistep-form/save-form?form-name=" + formName + "&step-id=" + stepId + "\"";
        stepHtml.replace(start, end, newPath);

        for(FormItemEntity stepItem : formItemsRepository.findAllByStepIdAndDomainIdOrderBySortPriorityAsc(stepId, CloudToolsForCore.getDomainId())) {
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
        stepHtml.append( FormsService.replaceFields(prop.getText("components.formsimple.form.end"), formName, recipients, null, requiredLabelAdd, isEmailRender, false, firstTimeHeadingSet, prop) );

        //
        return stepHtml.toString();
    }
}