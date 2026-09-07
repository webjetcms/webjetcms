package sk.iway.iwcm.components.multistep_form.rest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.CryptoFactory;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
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
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.utils.Pair;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Renders a multistep form for page, email, and PDF contexts.
 *
 * <p>The handler builds localized step and item markup, converts interactive controls
 * to read-only email representations, collects CSS, and optionally encrypts rendered
 * email HTML with the configured public key.</p>
 */
public class FormHtmlHandler {

    private static final String FORM_START_KEY = "components.mustistep.form.start";
    private static final String FORM_END_KEY = "components.mustistep.form.end";

    private final FormStepsRepository formStepsRepository;
    private final FormItemsRepository formItemsRepository;
    private final FormSettingsRepository formSettingsRepository;

    //
    private String formName;
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

    private Pair<String, String> cssDataPair;
    private String formHtmlBeforeCss;

    private int formCounter;

    /**
     * Creates a renderer for a specific form instance.
     *
     * @param formName  name of the form to render
     * @param formCounter  one-based form instance number used to create unique DOM identifiers
     * @param request  current HTTP request used to resolve language and form settings
     * @throws IllegalStateException if a required repository is unavailable or {@code formCounter} is invalid
     */
    public FormHtmlHandler(String formName, int formCounter, HttpServletRequest request) {
        this.formStepsRepository = Tools.getSpringBean("formStepsRepository", FormStepsRepository.class);
        if(this.formStepsRepository == null) throw new IllegalStateException("FormHtmlHandler was not able to obtain FormStepsRepository");

        this.formItemsRepository = Tools.getSpringBean("formItemsRepository", FormItemsRepository.class);
        if(this.formItemsRepository == null) throw new IllegalStateException("FormHtmlHandler was not able to obtain FormItemsRepository");

        this.formSettingsRepository = Tools.getSpringBean("formSettingsRepository", FormSettingsRepository.class);
        if(this.formSettingsRepository == null) throw new IllegalStateException("FormHtmlHandler was not able to obtain FormSettingsRepository");

        this.formCounter = formCounter;
        if(this.formCounter < 1) throw new IllegalStateException("Invalid formCounter for form rendering");

        this.formName = formName;

        this.prop = Prop.getInstance( PageLng.getUserLng(request) );
        this.requiredLabelAdd = prop.getText("components.formsimple.requiredLabelAdd");
        this.firstTimeHeadingSet = new HashSet<>();

        FormSettingsEntity formSettings = formSettingsRepository.findByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId());
        if (formSettings != null) {
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
        }
    }

    /**
     * Creates a renderer and resolves the form instance number from the current request state.
     *
     * @param formName  name of the form to render
     * @param request  current HTTP request used to resolve the form instance, language, and settings
     * @throws IllegalStateException if a required service or repository is unavailable or the resolved counter is invalid
     */
    public FormHtmlHandler(String formName, HttpServletRequest request) {
        this(formName, getFormCounter(formName, request), request);
    }

    private static int getFormCounter(String formName, HttpServletRequest request) {
        final MultistepFormsService multistepFormsService = Tools.getSpringBean("multistepFormsService", MultistepFormsService.class);
        if(multistepFormsService == null) throw new IllegalStateException("FormHtmlHandler was not able to obtain MultistepFormsService");
        return multistepFormsService.getFormCounter(formName, request);
    }

    /**
     * Returns the last rendered form content before CSS or encryption is applied.
     *
     * @return rendered HTML or plain text, depending on form settings, or an empty string if unavailable
     */
    public final String getFormHtmlBeforeCss() {
        if(formHtmlBeforeCss == null) return "";
        return formHtmlBeforeCss;
    }

    /**
     * Returns CSS collected during the last render.
     *
     * @return pair whose first value contains inline {@code <style>} CSS and whose second
     *         value contains {@code <link>} elements; both values are empty if unavailable
     */
    public final Pair<String, String> getCssDataPair() {
        if(cssDataPair == null) return new Pair<>("", "");
        return new Pair<>(cssDataPair.first, cssDataPair.second);
    }

    /**
     * Returns the prefix used to map logical form item identifiers to DOM identifiers.
     *
     * @return form-instance-specific DOM ID prefix
     */
    public final String getDomIdPrefix() {
        return "f" + this.formCounter + "-";
    }

    /**
     * Builds HTML for a single form step including its items and wrappers.
     * Intended for on‑page rendering (not email).
     *
     * @param stepId ID of the step to render
     * @param request current HTTP request
     * @return HTML markup of the requested step
     */
    public final String getFormStepHtml(Long stepId, HttpServletRequest request) {
        this.isEmailRender = false;

        StringBuilder stepHtml = new StringBuilder();
        FormStepEntity formStep = formStepsRepository.getReferenceById(stepId);

        // Form start
        stepHtml.append( getFormStart(stepId, request) );

        // Form fields for selected step (aka step items)
        stepHtml.append( getStepHtml(request, formStep) );

        // From end
        stepHtml.append( getFormEnd(stepId, request, formStep) );

        return stepHtml.toString();
    }

    /**
     * Creates the opening HTML of the form, including optional CSS links and action URL.
     *
     * @param stepId current step ID (used for action URL), -1 for email render
     * @param request HTTP request
     * @return HTML builder with form start content
     */
    private StringBuilder getFormStart(Long stepId, HttpServletRequest request) {
        StringBuilder formStartHtml = new StringBuilder("");

        //<link rel="stylesheet" type="text/css" href="/templates/aceintegration/jet/assets/multistep-form/css/base.css" />
        for(String css : Tools.getTokens(this.formCss, "\n", true))
            formStartHtml.append("<link rel='stylesheet' type='text/css' href='").append(css).append("' />");

        formStartHtml.append(FormsService.replaceFields(prop.getText(FORM_START_KEY), this.formName, recipients, null, requiredLabelAdd, isEmailRender, false, firstTimeHeadingSet, prop, request));

        String newPath = "/rest/multistep-form/save-form?form-name=" + this.formName + "&step-id=" + stepId;
        Tools.replace(formStartHtml, "${formActionSrc}", newPath);
        Tools.replace(formStartHtml, "${formAddClasses}", this.formAddClasses);
        return formStartHtml;
    }

    /**
     * Creates the HTML wrapper for a step and injects the step items.
     *
     * @param request  current HTTP request
     * @param formStep  step whose wrapper and items are rendered
     * @return HTML builder with step content
     */
    private StringBuilder getStepHtml(HttpServletRequest request, FormStepEntity formStep) {
        StringBuilder formStepHtml = new StringBuilder();

        // Form step wrapper start

        StringBuilder stepWrapperStart = new StringBuilder();
        stepWrapperStart.append(prop.getText("components.mustistep.step.start"));

        if (Tools.isNotEmpty(formStep.getHeader())) {
            // Swap header title
            stepWrapperStart.append(Tools.replace(prop.getText("components.mustistep.step.header"), "${step-header}", StringEscapeUtils.unescapeHtml4(formStep.getHeader()) ));

            // Swap user info
            stepWrapperStart = DocTools.updateUserCodes(UsersDB.getCurrentUser(request), stepWrapperStart);

            // Swap form items values
            stepWrapperStart = MultistepFormsService.updateFormValues(formName, request, stepWrapperStart);
        }

        formStepHtml.append(stepWrapperStart);

        if (rowView) formStepHtml.append(prop.getText("components.mustistep.rowView.start"));

        // Into formStep we must insert step items
        formStepHtml.append( getStepItems(formStep.getId(), request) );

        if (rowView) formStepHtml.append( prop.getText("components.mustistep.rowView.end"));

        // Form step wrapper end
        formStepHtml.append( prop.getText("components.mustistep.step.end") );

        return formStepHtml;
    }

    /**
     * Renders all items belonging to a step. For email context, inputs are converted
     * to read‑only text equivalents.
     *
     * @param stepId step ID
     * @param request HTTP request
     * @return HTML builder with step items
     */
    private StringBuilder getStepItems(Long stepId, HttpServletRequest request) {
        StringBuilder stepItemsHtml = new StringBuilder();

        FormConditionsHandler formConditionsHandler = new FormConditionsHandler(this.formName, request);
        // isFieldHiddenByCondition requires data as JSONObject
        JSONObject jsonObject = new JSONObject(this.formData);

        for(FormItemEntity stepItem : formItemsRepository.getAllStepItems(stepId, CloudToolsForCore.getDomainId())) {

            // DO NOT ADD item from form step if its hidden by condition - for email render
            if(isEmailRender == true && Tools.isTrue(formConditionsHandler.isFieldHiddenByCondition(stepItem, jsonObject))) continue;

            JSONObject item = new JSONObject(stepItem);
            // Keep the entity's logical ID unchanged and map only the rendered DOM ID.
            if(isEmailRender == false) item.put("itemFormId", getDomIdPrefix() + stepItem.getItemFormId());

            String fieldType = item.getString("fieldType");

            item.put("labelOriginal", stepItem.getLabel());
            if (Tools.isEmpty(stepItem.getLabel())) {
                item.put("label", prop.getText("components.formsimple.label." + fieldType));
            }

            String itemHtml = FormsService.replaceFields(prop.getText("components.formsimple.input." + fieldType), this.formName, recipients, item, requiredLabelAdd, isEmailRender, rowView, firstTimeHeadingSet, prop, request);

            if (isEmailRender == false) {
                if(itemHtml.contains("!INCLUDE"))
                    itemHtml = EditorToolsForCore.renderIncludes(itemHtml, false, request);

                stepItemsHtml.append(itemHtml);
            } else {
                if(MultistepFormsService.getRowViewItemTypes().contains(fieldType)) {
                    // Do not change HTML, this html can be unbalanced like "</div><div class="row">" and its not valid ... so editFieldHtmlToEmailRender would return "<div class="row"></div>" because of Jsoup.parseBodyFragment
                } else {
                    // !! its for show or for email... remaster item html
                    itemHtml = editFieldHtmlToEmailRender(itemHtml, stepItem, request);
                }

                stepItemsHtml.append(itemHtml);
            }
        }

        return stepItemsHtml;
    }

    /**
     * Creates the closing HTML of the form and decides the submit button text
     * based on whether the step is the last one.
     *
     * @param stepId  current step ID
     * @param request  current HTTP request
     * @param formStep  current step used to resolve navigation and optional trailing HTML
     * @return HTML builder with form end content
     */
    private StringBuilder getFormEnd(Long stepId, HttpServletRequest request, FormStepEntity formStep) {
        Pair<String, String> buttonsLabels = getButtonsLabels(stepId);

        StringBuilder formEndHtml = new StringBuilder();
        FormStepEntity previousStep = MultistepFormsService.getPreviousStep(formName, formStep, formStepsRepository);
        if(isEmailRender == false && previousStep != null) {
            formEndHtml.append("<button type=\"button\" class=\"btn btn-outline-secondary mt-3 me-2\" data-multistep-back-step=\"")
                .append(previousStep.getId())
                .append("\">")
                .append(StringEscapeUtils.escapeHtml4(buttonsLabels.getFirst()))
                .append("</button>");
        }
        formEndHtml.append(getFormEnd(buttonsLabels.getSecond(), request));

        if(isEmailRender == false) {
            if(formStep.getStepBonusHtml() == null) formEndHtml.append("");
            else formEndHtml.append(formStep.getStepBonusHtml());
        }

        return formEndHtml;
    }

    /**
     * Creates the closing HTML of the form with a specific submit button text.
     *
     * @param submitButtonString text for the submit button
     * @param request HTTP request
     * @return HTML builder with form end content
     */
    private StringBuilder getFormEnd(String submitButtonString, HttpServletRequest request) {
        StringBuilder formEndHtml = new StringBuilder();

        formEndHtml.append( FormsService.replaceFields(prop.getText(FORM_END_KEY), this.formName, recipients, null, requiredLabelAdd, isEmailRender, false, firstTimeHeadingSet, prop, request) );

        Tools.replace(formEndHtml, "${submitButtonText}", submitButtonString);

        if(isEmailRender == false) Tools.replace(formEndHtml, "{tech-info}", "");

        return formEndHtml;
    }

    /**
     * Builds and stores the complete multistep form representation for email delivery.
     * Converts it to plain text when configured, optionally encrypts it, and collects
     * stylesheet data for email and PDF variants.
     *
     * @param form  entity whose HTML field receives the rendered email content
     * @param request  current HTTP request
     * @param docId  ID of the document used to resolve template and group CSS
     * @throws IllegalStateException if the entity belongs to a different form than this handler
     */
    public final void setFormHtml(FormsEntity form, HttpServletRequest request, Integer docId) {
        // Check that provided form has same name as formName provided in constructor
        if(form.getFormName().equals(this.formName) == false) throw new IllegalStateException("Provided form has different name taht provided in constructor.");

        StringBuilder formHtml = new StringBuilder("");

        if (Tools.isNotEmpty(emailTextBefore)) formHtml.append(ResponseUtils.filter(emailTextBefore).replaceAll("\\n", "<br/>")).append("<br/>");

        // This is email render format
        this.isEmailRender = true;

        // Form start
        formHtml.append( getFormStart(-1L, request) );

        // prepare data
        this.formData = MultistepFormsService.getFormDataAsMap(form);

        for(FormStepEntity formSteps : formStepsRepository.findAllByFormNameAndDomainIdOrderBySortPriorityAsc(this.formName, CloudToolsForCore.getDomainId()))
            formHtml.append( getStepHtml(request, formSteps) ).append("<hr>");

        // Remove last <hr> tag
        formHtml.delete(formHtml.length() - 4, formHtml.length());

        // End form
        formHtml.append( getFormEnd("", request) );

        if (Tools.isNotEmpty(emailTextAfter)) formHtml.append("<br/>").append(ResponseUtils.filter(emailTextAfter).replaceAll("\\n", "<br/>")).append("<br/>");

        String techInfo = "";
        if(this.addTechInfo) {
            techInfo = prop.getText("components.formsimple.techinfo");
            techInfo = ShowDoc.updateCodes(null, techInfo, docId, request, Constants.getServletContext());
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
        this.formHtmlBeforeCss = formHtmlAsText;

        //
        CryptoFactory cryptoFactory = new CryptoFactory();
        if(Tools.isNotEmpty(this.publicKey))
            form.setHtml( cryptoFactory.encrypt(appendStyle(formHtmlAsText, cssPair.second, null), publicKey) );
        else
            form.setHtml( appendStyle(formHtmlAsText, cssPair.second, null) );
    }

    /**
     * Returns the PDF-oriented form representation,
     * based on the last {@link #setFormHtml(FormsEntity, HttpServletRequest, Integer)} call.
     *
     * @return HTML with inline CSS, plain text when configured, or an empty string if unavailable
     */
    public final String getFormPdfVersion() {
        //Check if form html and css styles were allready set
        if(Tools.isNotEmpty(formHtmlBeforeCss) && cssDataPair != null) {
            return appendStyle(formHtmlBeforeCss, cssDataPair.first, null);
        }
        return "";
    }

    /**
     * Wraps provided HTML with minimal email document structure and appends stylesheet markup.
     * Uses instance flag forcing plain text when configured.
     *
     * @param htmlData  HTML body content
     * @param styleHtml  stylesheet markup such as {@code <style>} or {@code <link>} elements
     * @param emailEncoding  optional character set for the metadata header
     * @return full HTML document, or the original content when plain-text mode is enabled
     */
    private String appendStyle(String htmlData, String styleHtml, String emailEncoding) {
        boolean forceTextPlain = this.formForceTextPlain || Constants.getBoolean("formMailSendPlainText");
        return appendStyle(htmlData, styleHtml, emailEncoding, forceTextPlain);
    }

    /**
     * Wraps HTML with minimal email document structure and appends stylesheet markup.
     *
     * @param htmlData  HTML body content
     * @param styleHtml  stylesheet markup such as {@code <style>} or {@code <link>} elements
     * @param emailEncoding  optional character set for the metadata header
     * @param forceTextPlain  whether to return {@code htmlData} without wrapping
     * @return full HTML document, or {@code htmlData} when {@code forceTextPlain} is true
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
     * @param docId  document ID used to resolve template and group CSS
     * @return pair containing inline {@code <style>} CSS and {@code <link>} elements,
     *         or {@code null} if the document cannot be resolved
     */
    private Pair<String, String> getCssDataLink(Integer docId) {
        return getCssDataLink(docId, this.formForceTextPlain, this.formCss);
    }

    /**
     * Resolves CSS for email/PDF rendering given a document context.
     *
     * @param docId  document ID
     * @param forceTextPlain  whether to return empty CSS
     * @return pair containing inline {@code <style>} CSS and {@code <link>} elements,
     *         an empty pair for plain text, or {@code null} if the document cannot be resolved
     */
    public static Pair<String, String> getCssDataLink(Integer docId, boolean forceTextPlain) {
        return getCssDataLink(docId, forceTextPlain, null);
    }

    /**
     * Resolves CSS for email/PDF rendering including template, editor and form‑specific CSS.
     *
     * @param docId  document ID
     * @param forceTextPlain  whether to return empty CSS
     * @param formSpecificCssStr  newline-separated list of additional CSS paths from form settings
     * @return pair containing inline {@code <style>} CSS and {@code <link>} elements,
     *         an empty pair for plain text, or {@code null} if the document cannot be resolved
     */
    public static Pair<String, String> getCssDataLink(Integer docId, boolean forceTextPlain, String formSpecificCssStr) {
        if(Constants.getBoolean("formMailSendPlainText") == true || forceTextPlain) return new Pair<>("", "");

        String cssData = null;
		String cssLink = null;

        DocDetails doc = DocDB.getInstance().getDoc(docId, -1, false);
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
				String[] baseCssPaths = Tools.getTokens(temp.getBaseCssPath(), "\n");
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
				String[] tempCssLinks =  Tools.getTokens(temp.getCss(), "\n");
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
				String[] editorEditorCsses = Tools.getTokens(Constants.getString("editorEditorCss"), "\n");
                for(String editorEditorCss : editorEditorCsses) {
                    editorEditorCss = FormMailAction.checkEmailCssVersion(editorEditorCss);
                    cssStyle.append(FileTools.readFileContent(editorEditorCss)).append('\n');
                    cssLink += "<link rel='stylesheet' href='" + editorEditorCss + "' type='text/css'/>\n";
                }

                // Form specific csss (from form settings)
                String[] formSpecificCsses = Tools.getTokens(formSpecificCssStr, "\n");
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

    /**
     * Converts interactive controls in a form item to read-only email markup.
     *
     * @param itemHtml  source markup for the form item
     * @param stepItem  item whose submitted value is rendered
     * @param request  current HTTP request used to resolve saved selections
     * @return serialized body markup containing read-only control values, or an empty string for CAPTCHA
     */
    private String editFieldHtmlToEmailRender(String itemHtml, FormItemEntity stepItem, HttpServletRequest request) {
        if("captcha".equals(stepItem.getFieldType())) return "";

        if (itemHtml.contains("!INCLUDE"))
            itemHtml = Tools.replaceRegex(itemHtml, "!INCLUDE\\(.*?\\)!", "<span class=\"form-control emailInput-text\">" + getFieldValue(stepItem.getItemFormId()) + "</span>", true);

        boolean radioCheckboxAsText = Constants.getBoolean("formMailRenderRadioCheckboxText");
        String fieldValue = getFieldValue(stepItem.getItemFormId());

        Document doc = Jsoup.parseBodyFragment(itemHtml);

        // Loop input and handle text, radio and checkbox types
        for (Element input : doc.select("input")) {
            String inputType = input.attr("type");
            if(Tools.isEmpty(inputType)) inputType = "";

            if("radio".equals(inputType) || "checkbox".equals(inputType)) {
                boolean isSelected = isCheckboxOrRadioSelected(input.val(), stepItem.getItemFormId(), request);

                if ("radio".equals(inputType))
                    input.before( getHtmlForRadioInput(isSelected, radioCheckboxAsText) );
                else
                    input.before( getHtmlForCheckboxInput(isSelected, radioCheckboxAsText) );

                input.remove();
            } else {
                input.before("<span class=\"form-control emailInput-text\">" + fieldValue + "</span>");
                input.remove();
            }
        }

        // Loop all textareas
        for (Element textarea : doc.select("textarea")) {
            String textareaValue = fieldValue;
            if (SaveFormService.isFilterHtml(textarea.outerHtml())) {
                if (textareaValue != null) textareaValue = textareaValue.replaceAll("\\n", "<br/>");
            }
            textarea.before("<span class=\"form-control emailInput-textarea\" style=\"height: auto;\">" + textareaValue + "</span>");
            textarea.remove();
        }

        // Loop selects
        for (Element select : doc.select("select")) {
            Element readonlyValue = new Element("span");
            readonlyValue.addClass("form-control").addClass("emailInput-select");

            Element selectedOption = null;
            for (Element option : select.select("option")) {
                if (fieldValue.equals(option.val())) {
                    selectedOption = option;
                    break;
                }
            }

            if (selectedOption != null) readonlyValue.text(selectedOption.text());
            else readonlyValue.html(fieldValue);

            select.before(readonlyValue);
            select.remove();
        }

        // Remove help blocks
        doc.select("div.help-block").remove();

        return doc.body().html();
    }

    /**
     * Determines whether a radio button or checkbox value was selected.
     *
     * <p>Values saved in the current request flow take precedence over persisted form data.</p>
     *
     * @param inputValue  value represented by the rendered control
     * @param itemFormId  logical identifier of the form item
     * @param request  current HTTP request used to resolve saved selections
     * @return {@code true} when the value is selected
     */
    private boolean isCheckboxOrRadioSelected(String inputValue, String itemFormId, HttpServletRequest request) {
        String[] selectedValues = MultistepFormsService.getSavedSelectedValues(this.formName, itemFormId, request);
        if(selectedValues != null) return Arrays.asList(selectedValues).contains(inputValue);

        String values = this.formData.get(itemFormId);
        if(Tools.isEmpty(values)) return false;

        //for radiogroup you can have long text with commas, so we need to check if the whole value is equal to the input value first
        if (values.equals(inputValue)) return true;

        for(String value : Tools.getTokens(values, ",")) {
            if(value.equals(inputValue)) return true;
        }
        return false;
    }

    private String getFieldValue(String itemFormId) {
        String value = this.formData.get(itemFormId);
        if(Tools.isEmpty(value)) value = "&nbsp;";
        return value;
    }

    /**
     * Resolves the back and next button labels for a form step.
     *
     * <p>The final step uses its configured next label or the localized submit label.</p>
     *
     * @param currentStepId  identifier of the current form step
     * @return pair containing the back label followed by the next or submit label
     * @throws IllegalStateException if the requested step does not exist
     */
    private Pair<String, String> getButtonsLabels(Long currentStepId) {
        FormStepEntity actualStep = formStepsRepository.findById(currentStepId).orElse(null);

        if(actualStep == null) throw new IllegalStateException("getButtonsLabels() - form step was not found by provided params");

        String backBtnLabel;
        if(Tools.isNotEmpty(actualStep.getBackStepBtnLabel())) backBtnLabel = actualStep.getBackStepBtnLabel();
        else backBtnLabel = prop.getText("components.mustistep.form.back_step");

        String nextBtnLabel;
        if(Tools.isNotEmpty(actualStep.getNextStepBtnLabel())) nextBtnLabel = actualStep.getNextStepBtnLabel();
        else {
            if(actualStep.isLastStep()) // last step
                nextBtnLabel = prop.getText("components.mustistep.form.save_form");
            else // not last step
                nextBtnLabel = prop.getText("components.mustistep.form.next_step");
        }

        return new Pair<>(backBtnLabel, nextBtnLabel);
    }

    private String getHtmlForRadioInput(boolean isSelected, boolean radioCheckboxAsText) {
        if (radioCheckboxAsText) {
            return isSelected
                ? "<span class='inputradio emailinput-radio input-checked'>[X]</span>"
                : "<span class='inputradio emailinput-radio input-unchecked'>[&nbsp;]</span>";
        } else {
            return isSelected
                ? "<input class='inputradio emailinput-radio input-checked' type='radio' checked disabled>"
                : "<input class='inputradio emailinput-radio input-unchecked' type='radio' disabled>";
        }
    }

    private String getHtmlForCheckboxInput(boolean isSelected, boolean radioCheckboxAsText) {
        if (radioCheckboxAsText) {
            return isSelected
                ? "<span class='inputcheckbox emailinput-cb input-checked'>[X]</span>"
                : "<span class='inputcheckbox emailinput-cb input-unchecked'>[&nbsp;]</span>";
        } else {
            return isSelected
                ? "<input class='inputcheckbox emailinput-cb input-checked' type='checkbox' checked disabled>"
                : "<input class='inputcheckbox emailinput-cb input-unchecked' type='checkbox' disabled>";
        }
    }
}
