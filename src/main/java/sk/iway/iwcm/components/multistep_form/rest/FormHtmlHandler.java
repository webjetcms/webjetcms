package sk.iway.iwcm.components.multistep_form.rest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.CryptoFactory;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.common.SearchTools;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.FormsEntity;
import sk.iway.iwcm.components.forms.FormsService;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.form.FormMailAction;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.utils.Pair;

/**
 * Renders multi‑step form HTML for both on‑page and email contexts.
 * <p>
 * Responsibilities:
 * - Builds step and item markup using translation keys and form settings
 * - Transforms inputs to read‑only representations for email/PDF rendering
 * - Collects and inlines CSS for email/PDF variants
 * - Optionally encrypts rendered email HTML using a provided public key
 */
@Service
public class FormHtmlHandler {

    private static final String FORM_START_KEY = "components.mustistep.form.start";
    private static final String FORM_END_KEY = "components.mustistep.form.end";

    private final FormStepsRepository formStepsRepository;
    private final FormItemsRepository formItemsRepository;
    private final FormSettingsRepository formSettingsRepository;

    // Set by class
    private Prop prop;
    private String requiredLabelAdd;
    private boolean rowView;
    private String recipients;
    private Set<String> firstTimeHeadingSet;

    //
    private boolean isEmailRender;
    private boolean addTechInfo;
    private boolean formForceTextPlain;
    private String publicKey;
    private String formAddClasses;
    private String formCss;
    private Map<String, String> formData;

    private String emailTextBefore;
    private String emailTextAfter;

    private DocDB docDB;

    private Pair<String, String> cssDataPair;
    private String formHtmlBeforeCss;

    @Autowired
    public FormHtmlHandler(FormStepsRepository formStepsRepository, FormItemsRepository formItemsRepository, FormSettingsRepository formSettingsRepository) {
        this.formStepsRepository = formStepsRepository;
        this.formItemsRepository = formItemsRepository;
        this.formSettingsRepository = formSettingsRepository;
    }

    /**
     * Returns the last rendered form HTML without appended CSS.
     * Useful for plain email etc.
     *
     * @return HTML content without CSS, or empty string if not available
     */
    public final String getFormHtmlBeforeCss() {
        if(formHtmlBeforeCss == null) return "";
        return new String(formHtmlBeforeCss);
    }

    /**
     * Returns CSS collected during the last render.
     *
     * @return pair of (inline <style> CSS, <link> tags); empty values if not available
     */
    public final Pair<String, String> getCssDataPair() {
        if(cssDataPair == null) return new Pair<String,String>("", "");
        return new Pair<String,String>(cssDataPair.first, cssDataPair.second);
    }

    private final void setSupportValues(String formName, HttpServletRequest request, boolean isEmailRender) {
        this.prop = Prop.getInstance(request);
        this.requiredLabelAdd = prop.getText("components.formsimple.requiredLabelAdd");
        this.firstTimeHeadingSet = new HashSet<String>();

        FormSettingsEntity formSettings = formSettingsRepository.findByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId());
        this.rowView = Tools.isTrue(formSettings.getRowView());
        this.recipients = formSettings.getRecipients();
        this.addTechInfo = Tools.isTrue(formSettings.getAddTechInfo());
        this.formForceTextPlain = Tools.isTrue(formSettings.getForceTextPlain());
        this.publicKey = formSettings.getEncryptKey();
        this.formCss = formSettings.getFormCss();
        this.emailTextBefore = formSettings.getEmailTextBefore();
        this.emailTextAfter = formSettings.getEmailTextAfter();

        this.formAddClasses = formSettings.getFormAddClasses();
        if(Tools.isEmpty(this.formAddClasses)) this.formAddClasses = "";

        this.isEmailRender = isEmailRender;
        this.docDB = DocDB.getInstance();
    }

    /**
     * Builds HTML for a single form step including its items and wrappers.
     * Intended for on‑page rendering (not email).
     *
     * @param formName name/identifier of the form
     * @param stepId ID of the step to render
     * @param request current HTTP request
     * @return HTML markup of the requested step
     */
    public final String getFormStepHtml(String formName, Long stepId, HttpServletRequest request) {
        //Frisrt set needed values
        setSupportValues(formName, request, false);

        StringBuilder stepHtml = new StringBuilder();
        FormStepEntity formStep = formStepsRepository.getById(stepId);

        // Form start
        stepHtml.append( getFormStart(formName, stepId, request) );

        // Form fields for selected step (aka step items)
        stepHtml.append( getStepHtml(formName, request, formStep) );

        // From end
        stepHtml.append( getFormEnd(formName, stepId, request, formStep) );

        return stepHtml.toString();
    }

    /**
     * Creates the opening HTML of the form, including optional CSS links and action URL.
     *
     * @param formName form identifier
     * @param stepId current step ID (used for action URL), -1 for email render
     * @param request HTTP request
     * @return HTML builder with form start content
     */
    private StringBuilder getFormStart(String formName, Long stepId, HttpServletRequest request) {
        StringBuilder formStartHtml = new StringBuilder("");

        //<link rel="stylesheet" type="text/css" href="/templates/aceintegration/jet/assets/multistep-form/css/base.css" />
        for(String css : Tools.getTokens(this.formCss, "\n", true))
            formStartHtml.append("<link rel='stylesheet' type='text/css' href='").append(css).append("' />");

        formStartHtml.append(FormsService.replaceFields(prop.getText(FORM_START_KEY), formName, recipients, null, requiredLabelAdd, isEmailRender, false, firstTimeHeadingSet, prop, request));

        String newPath = "/rest/multistep-form/save-form?form-name=" + formName + "&step-id=" + stepId;
        Tools.replace(formStartHtml, "${formActionSrc}", newPath);
        Tools.replace(formStartHtml, "${formAddClasses}", this.formAddClasses);
        return formStartHtml;
    }

    /**
     * Creates the HTML wrapper for a step and injects the step items.
     *
     * @param formName form identifier
     * @param stepId step ID to render
     * @param request HTTP request
     * @return HTML builder with step content
     */
    private StringBuilder getStepHtml(String formName, HttpServletRequest request, FormStepEntity formStep) {
        StringBuilder formStepHtml = new StringBuilder();

        // Form step wrapper start

        StringBuilder stepWrapperStart = new StringBuilder();
        stepWrapperStart.append(prop.getText("components.mustistep.step.start"));

        if (Tools.isNotEmpty(formStep.getStepName()))
            stepWrapperStart.append(Tools.replace(prop.getText("components.mustistep.step.primaryHeader"), "${step-primaryHeader}", formStep.getStepName()));

        if (Tools.isNotEmpty(formStep.getStepSubName()))
            stepWrapperStart.append(Tools.replace(prop.getText("components.mustistep.step.secondaryHeader"), "${step-secondaryHeader}", formStep.getStepSubName()));

        formStepHtml.append(stepWrapperStart);

        if (rowView) formStepHtml.append(prop.getText("components.mustistep.rowView.start"));

        // Into formStep we must insert step items
        formStepHtml.append( getStepItems(formName, formStep.getId(), request) );

        if (rowView) formStepHtml.append( prop.getText("components.mustistep.rowView.end"));

        // Form step wrapper end
        formStepHtml.append( prop.getText("components.mustistep.step.end") );

        return formStepHtml;
    }

    /**
     * Renders all items belonging to a step. For email context, inputs are converted
     * to read‑only text equivalents.
     *
     * @param formName form identifier
     * @param stepId step ID
     * @param request HTTP request
     * @return HTML builder with step items
     */
    private StringBuilder getStepItems(String formName, Long stepId, HttpServletRequest request) {
        StringBuilder stepItemsHtml = new StringBuilder();
        for(FormItemEntity stepItem : formItemsRepository.getAllStepItems(stepId, CloudToolsForCore.getDomainId())) {

            JSONObject item = new JSONObject(stepItem);
            String fieldType = item.getString("fieldType");

            item.put("labelOriginal", item.getString("label"));
            if (Tools.isEmpty(item.getString("label")))
                item.put("label", prop.getText("components.formsimple.label." + fieldType));

            String itemHtml = FormsService.replaceFields(prop.getText("components.formsimple.input." + fieldType), formName, recipients, item, requiredLabelAdd, isEmailRender, rowView, firstTimeHeadingSet, prop, request);

            if (isEmailRender == false) {
                if(itemHtml.contains("!INCLUDE"))
                    itemHtml = EditorToolsForCore.renderIncludes(itemHtml, false, request);

                stepItemsHtml.append(itemHtml);
            } else {
                // !! its for show or for email... remaster item html
                itemHtml = editFieldHtmlToEmailRender(itemHtml, stepItem);
                stepItemsHtml.append(itemHtml);
            }
        }

        return stepItemsHtml;
    }

    /**
     * Creates the closing HTML of the form and decides the submit button text
     * based on whether the step is the last one.
     *
     * @param formName form identifier
     * @param stepId current step ID
     * @param request HTTP request
     * @return HTML builder with form end content
     */
    private StringBuilder getFormEnd(String formName, Long stepId, HttpServletRequest request, FormStepEntity formStep) {
        Pair<String, String> buttonsLabels = getButtonsLabels(formName, stepId);

        StringBuilder formEndHtml =  getFormEnd(formName, buttonsLabels.getSecond(), request);

        if(isEmailRender == false) {
            if(formStep.getStepBonusHtml() == null) formEndHtml.append("");
            formEndHtml.append(formStep.getStepBonusHtml());
        }

        return formEndHtml;
    }

    /**
     * Creates the closing HTML of the form with a specific submit button text.
     *
     * @param formName form identifier
     * @param submitButtonString text for the submit button
     * @param request HTTP request
     * @return HTML builder with form end content
     */
    private StringBuilder getFormEnd(String formName, String submitButtonString, HttpServletRequest request) {
        StringBuilder formEndHtml = new StringBuilder();

        formEndHtml.append( FormsService.replaceFields(prop.getText(FORM_END_KEY), formName, recipients, null, requiredLabelAdd, isEmailRender, false, firstTimeHeadingSet, prop, request) );

        Tools.replace(formEndHtml, "${submitButtonText}", submitButtonString);

        if(isEmailRender == false) Tools.replace(formEndHtml, "{tech-info}", "");

        return formEndHtml;
    }

    /**
     * Renders full multi‑step form as email‑ready HTML into the provided {@link FormsEntity}.
     * Applies optional encryption and collects CSS for inlining.
     *
     * @param form target form entity to populate with rendered HTML
     * @param request current HTTP request
     * @param docId ID of the document used to resolve template/group CSS
     */
    public final void setFormHtml(FormsEntity form, HttpServletRequest request, Integer docId) {
        StringBuilder formHtml = new StringBuilder("");

        formHtml.append(ResponseUtils.filter(emailTextBefore).replaceAll("\\n", "<br/>")).append("<br/>");

        //
        setSupportValues(form.getFormName(), request, true);

        // Form start
        formHtml.append( getFormStart(form.getFormName(), -1L, request) );

        // prepare data
        this.formData = MultistepFormsService.getFormDataAsMap(form);

        for(FormStepEntity formSteps : formStepsRepository.findAllByFormNameAndDomainIdOrderBySortPriorityAsc(form.getFormName(), CloudToolsForCore.getDomainId()))
            formHtml.append( getStepHtml(form.getFormName(), request, formSteps) );

        // End form
        formHtml.append( getFormEnd(form.getFormName(), "", request) );

        formHtml.append("<br/>").append(ResponseUtils.filter(emailTextAfter).replaceAll("\\n", "<br/>")).append("<br/>");

        String techInfo = "";
        if(this.addTechInfo) {
            techInfo = prop.getText("components.formsimple.techinfo");
            techInfo = ShowDoc.updateCodes(null, techInfo, form.getDocId(), request, Constants.getServletContext());
        }
        formHtml = Tools.replace(formHtml, "{tech-info}", techInfo);

        //Remove submit buttons
        formHtml = Tools.replaceRegex(formHtml, "<button.*type=\"submit\".*?><\\/button>", "", false);

        String formHtmlAsText;
        if (this.formForceTextPlain || Constants.getBoolean("formMailSendPlainText")) {
            // First remove all SCRIPT's from HTML
            if(formHtml != null) formHtml = Tools.replaceRegex(formHtml, "<\\s*script\\b[^>]*>.*?<\\s*/\\s*script\\s*>", "", true);

            if(formHtml == null) formHtmlAsText = "";
            else formHtmlAsText = SearchTools.htmlToPlain(formHtml.toString());
        }
        else
            formHtmlAsText = SearchTools.removeCommands(formHtml.toString()); // odstranim z HTML riadiace bloky napr.: !INCLUDE(...)!, !PARAMETER(...)! - MULTISTEP form should not have this problem but just in case I leave it here

        //Get CSS data
        Pair<String, String> cssPair = getCssDataLink(docId);
        this.cssDataPair = cssPair;

        // Set final value without CSS and without crypto to separe variable .... we need tthis value other logic like PDF version etc
        this.formHtmlBeforeCss = new String(formHtmlAsText);

        //
        CryptoFactory cryptoFactory = new CryptoFactory();
        if(Tools.isNotEmpty(this.publicKey))
            form.setHtml( cryptoFactory.encrypt(appendStyle(formHtmlAsText, cssPair.second, null), publicKey) );
        else
            form.setHtml( appendStyle(formHtmlAsText, cssPair.second, null) );
    }

    /**
     * Method can be called ONLY if method:setFormHtml was already called
     * @return
     */
    /**
     * Returns HTML wrapped with inline CSS suitable for PDF generation
     * based on the last {@link #setFormHtml(FormsEntity, HttpServletRequest, Integer)} call.
     *
     * @return HTML with inline CSS, or empty string if not available
     */
    public final String getFormPdfVersion() {
        //Check if form html and css styles were allready set
        if(Tools.isNotEmpty(formHtmlBeforeCss) && cssDataPair != null) {
            return appendStyle(formHtmlBeforeCss, cssDataPair.first, null);
        }
        return "";
    }

    /**
     * Wraps provided HTML with minimal email document structure and appends inline CSS.
     * Uses instance flag forcing plain text when configured.
     *
     * @param htmlData HTML body content
     * @param styleHtml inline CSS wrapped in <style> tag
     * @param emailEncoding optional charset for meta header
     * @return full HTML document or plain text when plain‑text mode is enabled
     */
    private String appendStyle(String htmlData, String styleHtml, String emailEncoding) {
        boolean forceTextPlain = this.formForceTextPlain || Constants.getBoolean("formMailSendPlainText");
        return appendStyle(htmlData, styleHtml, emailEncoding, forceTextPlain);
    }

    /**
     * Static helper to wrap HTML with minimal email document structure and append CSS.
     *
     * @param htmlData HTML body content
     * @param styleHtml inline CSS wrapped in <style> tag
     * @param emailEncoding optional charset for meta header
     * @param forceTextPlain when true returns original htmlData without wrapping
     * @return full HTML document or plain text when {@code forceTextPlain} is true
     */
    public static String appendStyle(String htmlData, String styleHtml, String emailEncoding, boolean forceTextPlain) {
		if (forceTextPlain == true) return htmlData;

		if (styleHtml == null) styleHtml = "";

		StringBuilder metaEncoding = new StringBuilder("");
		if (Tools.isNotEmpty(emailEncoding)) metaEncoding.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=").append(emailEncoding).append("\">");

		StringBuilder htmlDataWithStyle = new StringBuilder("");
        htmlDataWithStyle.append("<html><head>").append(metaEncoding).append(styleHtml);
        htmlDataWithStyle.append("</head><body id='WebJETEditorBody' class=\"WebJETMailBody\"><div class=\"WebJETMailWrapper\">\n\n");
        htmlDataWithStyle.append(htmlData).append("\n\n</div></body></html>");
        return htmlDataWithStyle.toString();
	}


    /**
     * Resolves CSS for the current render using instance settings.
     *
     * @param docId document ID used to resolve template/group CSS
     * @return pair of (inline <style> CSS, <link> tags)
     */
    private Pair<String, String> getCssDataLink(Integer docId) {
        return getCssDataLink(docId, this.formForceTextPlain, this.docDB, this.formCss);
    }

    /**
     * Resolves CSS for email/PDF rendering given a document context.
     *
     * @param docId document ID
     * @param forceTextPlain when true returns empty CSS
     * @param docDB document service for resolving templates
     * @return pair of (inline <style> CSS, <link> tags) or empty pair when plain text
     */
    public static Pair<String, String> getCssDataLink(Integer docId, boolean forceTextPlain, DocDB docDB) {
        return getCssDataLink(docId, forceTextPlain, docDB, null);
    }

    /**
     * Resolves CSS for email/PDF rendering including template, editor and form‑specific CSS.
     *
     * @param docId document ID
     * @param forceTextPlain when true returns empty CSS
     * @param docDB document service for resolving templates
     * @param formSpecificCssStr newline‑separated list of additional CSS paths from form settings
     * @return pair of (inline <style> CSS, <link> tags); returns null if document cannot be resolved
     */
    public static Pair<String, String> getCssDataLink(Integer docId, boolean forceTextPlain, DocDB docDB, String formSpecificCssStr) {
        if(Constants.getBoolean("formMailSendPlainText") == true || forceTextPlain) return new Pair<>("", "");

        String cssData = null;
		String cssLink = null;

        DocDetails doc = docDB.getDoc(docId, -1, false);
        if(doc == null) return null;

        GroupsDB groupsDB = GroupsDB.getInstance();
		TemplatesDB tempDB = TemplatesDB.getInstance();

        TemplateDetails temp = tempDB.getTemplate(doc.getTempId());
        GroupDetails group = groupsDB.getGroup(doc.getGroupId());

		try
		{
			cssData = "<style type='text/css'>";
			cssLink = "";

			if (temp != null)
			{
				String domainAlias = MultiDomainFilter.getDomainAlias(group.getDomainName());

                //nacitaj css styl ako v editore stranok
				StringBuilder cssStyle = new StringBuilder("");

                // base
				String baseCssPaths[] = Tools.getTokens(temp.getBaseCssPath(), "\n");
				if (group != null && Constants.getBoolean("multiDomainEnabled") == true && Tools.isNotEmpty(group.getDomainName())) {
                    for(String baseCssPath : baseCssPaths) {
                        //ak je cssko v /templates adresari uz domain alias nepridavame
                        if (baseCssPath.contains(domainAlias)==false && baseCssPath.contains("/templates/")==false && baseCssPath.contains("/files/")==false)
                            baseCssPath = Tools.replace(baseCssPath, "/css/", "/css/" + MultiDomainFilter.getDomainAlias(group.getDomainName()) + "/"); //NOSONAR
                    }
				}
                for(String baseCssPath : baseCssPaths) {
                    baseCssPath = FormMailAction.checkEmailCssVersion(baseCssPath);
                    cssStyle.append(FileTools.readFileContent(baseCssPath)).append('\n');
                    cssLink += "<link rel='stylesheet' href='" + baseCssPath + "' type='text/css'/>\n";
                }

                // temp
				String tempCssLinks[] =  Tools.getTokens(temp.getCss(), "\n");
				if (group != null && Constants.getBoolean("multiDomainEnabled") == true && Tools.isNotEmpty(group.getDomainName())) {
                    for(String tempCssLink : tempCssLinks) {
                        //ak je cssko v /templates adresari uz domain alias nepridavame
                        if (tempCssLink.contains(domainAlias)==false && tempCssLink.contains("/templates/")==false && tempCssLink.contains("/files/")==false)
                            tempCssLink = Tools.replace(tempCssLink, "/css/", "/css/" + domainAlias + "/");
                    }
				}
                for(String tempCssLink : tempCssLinks) {
                    tempCssLink = FormMailAction.checkEmailCssVersion(tempCssLink);
                    cssStyle.append(FileTools.readFileContent(tempCssLink)).append('\n');
                    cssLink += "<link rel='stylesheet' href='" + tempCssLink + "' type='text/css'/>\n";
                }

                // editor
				String editorEditorCsses[] = Tools.getTokens(Constants.getString("editorEditorCss"), "\n");
                for(String editorEditorCss : editorEditorCsses) {
                    editorEditorCss = FormMailAction.checkEmailCssVersion(editorEditorCss);
                    cssStyle.append(FileTools.readFileContent(editorEditorCss)).append('\n');
                    cssLink += "<link rel='stylesheet' href='" + editorEditorCss + "' type='text/css'/>\n";
                }

                // Form specific csss (from form settings)
                String formSpecificCsses[] = Tools.getTokens(formSpecificCssStr, "\n");
                for(String formSpecificCss : formSpecificCsses) {
                    formSpecificCss = FormMailAction.checkEmailCssVersion(formSpecificCss);
                    cssStyle.append(FileTools.readFileContent(formSpecificCss)).append('\n');
                    cssLink += "<link rel='stylesheet' href='" + formSpecificCss + "' type='text/css'/>\n";
                }

				cssData += cssStyle.toString();
			} else {
				//nacitaj css styl
				InputStream is = Constants.getServletContext().getResourceAsStream("/css/email.css");
				if (is == null) {
					is = Constants.getServletContext().getResourceAsStream(Constants.getString("editorPageCss"));
					cssLink += "<link rel='stylesheet' href='" + Constants.getString("editorPageCss") + "' type='text/css'/>\n";
				} else
					cssLink += "<link rel='stylesheet' href='/css/email.css' type='text/css'/>\n";

				if (is != null) {
					BufferedReader br = new BufferedReader(new InputStreamReader(is, Constants.FILE_ENCODING));
					String line;
					StringBuilder startBuf = new StringBuilder(cssData);
					while ((line = br.readLine()) != null)
						startBuf.append(line).append('\n');

					cssData = startBuf.toString();
				}
			}
		} catch (Exception ex) {
			Logger.error(FormMailAction.class, ex);
		}

		cssData += "</style>";

        if(Tools.isEmpty(cssLink)) cssLink = "";

        return new Pair<>(cssData, cssLink);
    }

    private String editFieldHtmlToEmailRender(String itemHtml, FormItemEntity stepItem) {
        if("captcha".equals(stepItem.getFieldType())) return "";

        if (itemHtml.contains("!INCLUDE"))
            itemHtml = Tools.replaceRegex(itemHtml, "!INCLUDE\\(.*?\\)!", "<span class=\"form-control emailInput-text\">" + getFieldValue(stepItem.getItemFormId()) + "</span>", true);

        //
        boolean radioCheckboxAsText = Constants.getBoolean("formMailRenderRadioCheckboxText");

        // Loop all inputs
        String inputRegex = "<input.*?>";
        Pattern inputPattern = Pattern.compile(inputRegex);
        Matcher inputMatcher = inputPattern.matcher(itemHtml);

        while (inputMatcher.find()) {
            String originalValue = inputMatcher.group();

            if(itemHtml.contains("type=\"checkbox\"")) {
                boolean isSelected = isCheckboxOrRadioSelected(originalValue, stepItem.getItemFormId());
                if (radioCheckboxAsText) {
                    String replacement = isSelected
                        ? "<span class='inputcheckbox emailinput-cb input-checked'>[X]</span>"
                        : "<span class='inputcheckbox emailinput-cb input-unchecked'>[&nbsp;]</span>";
                    itemHtml = Tools.replace(itemHtml, originalValue, replacement);
                } else {
                    String replacement = isSelected
                        ? "<input class='inputcheckbox emailinput-cb input-checked' type='checkbox' checked disabled>"
                        : "<input class='inputcheckbox emailinput-cb input-unchecked' type='checkbox' disabled>";
                    itemHtml = Tools.replace(itemHtml, originalValue, replacement);
                }
            } else if (itemHtml.contains("type=\"radio\"")) {
                boolean isSelected = isCheckboxOrRadioSelected(originalValue, stepItem.getItemFormId());
                if (radioCheckboxAsText) {
                    String replacement = isSelected
                        ? "<span class='inputradio emailinput-radio input-checked'>[X]</span>"
                        : "<span class='inputradio emailinput-radio input-unchecked'>[&nbsp;]</span>";
                    itemHtml = Tools.replace(itemHtml, originalValue, replacement);
                } else {
                    String replacement = isSelected
                        ? "<input class='inputradio emailinput-radio input-checked' type='radio' checked disabled>"
                        : "<input class='inputradio emailinput-radio input-unchecked' type='radio' disabled>";
                    itemHtml = Tools.replace(itemHtml, originalValue, replacement);
                }
            } else {
                String fieldValue = getFieldValue(stepItem.getItemFormId());
                fieldValue = filterHtml(itemHtml, fieldValue);
                itemHtml = Tools.replace(itemHtml, originalValue, "<span class=\"form-control emailInput-text\">" + fieldValue + "</span>");
            }
        }

        // Loop all textareas
        String textareaRegex = "<textarea.*?</textarea>";
        Pattern textareaPattern = Pattern.compile(textareaRegex);
        Matcher textareaMatcher = textareaPattern.matcher(itemHtml);

        while (textareaMatcher.find()) {
            String code = textareaMatcher.group();
            String fieldValue = getFieldValue(stepItem.getItemFormId());
            fieldValue = filterHtml(code, fieldValue);
            if (isFilterHtml(code)) {
                fieldValue = fieldValue.replaceAll("\\n", "<br/>");
            }
            itemHtml = Tools.replace(itemHtml, textareaMatcher.group(), "<span class=\"form-control emailInput-textarea\" style=\"height: auto;\">" + fieldValue + "</span>");
        }

        // Loop selects
        String selectRegex = "(?s)<select.*?</select>";
        Pattern selectPattern = Pattern.compile(selectRegex);
        Matcher selectMatcher = selectPattern.matcher(itemHtml);

        while (selectMatcher.find()) {
            String code = selectMatcher.group();
            String fieldValue = getFieldValue(stepItem.getItemFormId());
            fieldValue = filterHtml(code, fieldValue);
            itemHtml = Tools.replace(itemHtml, selectMatcher.group(), "<span class=\"form-control emailInput-select\">" + fieldValue + "</span>");
        }

        // Remove help blocks
        itemHtml = Tools.replaceRegex(itemHtml, "<div class=\"help-block.*?<\\/div>", "", false);

        return itemHtml;
    }

    private boolean isFilterHtml(String code) {
        if(code.contains("-wysiwyg")) return false;
        return true;
    }

    private String filterHtml(String code, String fieldValue) {
        if (isFilterHtml(code)) {
            return ResponseUtils.filter(fieldValue);
        }
        //for wysiwyg fields (quill) filter at least unsafe HTML code
        return AllowSafeHtmlAttributeConverter.sanitize(fieldValue);
    }

    private boolean isCheckboxOrRadioSelected(String itemHtml, String itemFormId) {
        String values = this.formData.get(itemFormId);
        if(Tools.isEmpty(values)) return false;
        for(String value : Tools.getTokens(values, ",")) {
            if(itemHtml.contains("value=\"" + value + "\"") == true) return true;
        }
        return false;
    }

    private String getFieldValue(String itemFormId) {
        String value = this.formData.get(itemFormId);
        if(value == null) value = "";
        return value;
    }

    private Pair<String, String> getButtonsLabels(String formName, Long currentStepId) {
        int stepIndex = 0;
        FormStepEntity actualStep = null;

        List<FormStepEntity> formSteps = formStepsRepository.findAllByFormNameAndDomainIdOrderBySortPriorityAsc(formName, CloudToolsForCore.getDomainId());
        for(FormStepEntity formStep : formSteps) {
            if(formStep.getId().equals(currentStepId)) {
                actualStep = formStep;
                break;
            }
            stepIndex++;
        }

        if(actualStep == null) throw new IllegalStateException("getButtonsLabels() - form step was not found by provided params");

        String backBtnLabel;
        if(Tools.isNotEmpty(actualStep.getBackStepBtnLabel())) backBtnLabel = actualStep.getBackStepBtnLabel();
        else backBtnLabel = prop.getText("components.mustistep.form.back_step");

        String nextBtnLabel;
        if(Tools.isNotEmpty(actualStep.getNextStepBtnLabel())) nextBtnLabel = actualStep.getNextStepBtnLabel();
        else {
            if(stepIndex == (formSteps.size() - 1)) // last step
                nextBtnLabel = prop.getText("components.mustistep.form.save_form");
            else // not last step
                nextBtnLabel = prop.getText("components.mustistep.form.next_step");
        }

        return new Pair<String,String>(backBtnLabel, nextBtnLabel);
    }
}