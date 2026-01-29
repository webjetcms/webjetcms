package sk.iway.iwcm.components.multistep_form.rest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.CryptoFactory;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.forms.FormsEntity;
import sk.iway.iwcm.components.forms.FormsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.support.SaveFormException;
import sk.iway.iwcm.components.upload.XhrFileUploadServlet;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.form.FormMailAction;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;
import sk.iway.iwcm.tags.WriteTag;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@Service
public class SaveFormService {
    /**
     * Service responsible for saving multi-step form answers and sending notification emails.
     * <p>
     * Workflow overview:
     * - Validates spam protection and resolves email recipients
     * - Persists a new {@link FormsEntity} record and prepares HTML content
     * - Handles uploaded files (attachments) and optional PDF generation
     * - Sends the final email with rendered HTML and attachments when allowed
     */

    private FormHtmlHandler htmlHandler;
    private FormsRepository formsRepository;
    private FormMailService formMailService;

    @Autowired
    public SaveFormService(FormHtmlHandler htmlHandler, FormsRepository formsRepository, FormMailService formMailService) {
        this.htmlHandler = htmlHandler;
        this.formMailService = formMailService;
        this.formsRepository = formsRepository;
    }

    @Getter
    public static class FormFiles {
        private final Map<String, String> fileNames = new HashMap<>();
        private final StringBuilder fileNamesSendLater = new StringBuilder("");
        private final List<IwcmFile> attachs = new ArrayList<>();
    }

    /**
     * Saves answers of a multi-step form and sends the notification email.
     * <p>
     * This method performs spam protection, resolves recipients, prepares the {@link FormsEntity},
     * stores uploaded files, optionally generates a PDF summary, and finally sends the email.
     * If an error occurs and {@code forwardFail} is configured in {@code formSettings},
     * the thrown {@link SaveFormException} will contain a forward target.
     *
     * @param formName      technical name of the form (used for session and persistence)
     * @param formSettings  form configuration containing recipients, subject and options
     * @param iLastDocId    optional doc ID used to derive subject and context; may be {@code null}
     * @param request       current HTTP request (used for session and user context)
     * @throws SaveFormException when validation fails or sending logic requires forwarding on failure
     * @throws IOException       when an I/O error occurs during file operations
     */
    public final void saveFormAnswers(String formName, FormSettingsEntity formSettings, Integer iLastDocId, HttpServletRequest request) throws SaveFormException, IOException {

        String forwardFail = null;
		if (Tools.isNotEmpty(formSettings.getForwardFail())) forwardFail = formSettings.getForwardFail();

        if (!SpamProtection.canPost("form", null, request))
                throw new SaveFormException(Prop.getInstance(request).getText("send_mail_error.probablySpamBot"), false, forwardFail);

        String subject = Constants.getString("multistepform_subjectDefaultValue");
		if (Tools.isNotEmpty(formSettings.getSubject()))
			subject = formSettings.getSubject();

		int docId = -1;
        DocDB docDB = DocDB.getInstance();
        if (iLastDocId != null) {
            DocDetails doc = docDB.getDoc(iLastDocId, -1, false);
            docId = iLastDocId.intValue();

            if(doc != null) {
                if(Tools.isEmpty(formSettings.getSubject()))
                    subject = doc.getTitle();
            }
        }

        try {
            saveFormAnswers(formName, formSettings, docId, subject, request);
        } catch (SaveFormException sfe) {
            if(Tools.isNotEmpty(forwardFail)) throw new SaveFormException(sfe.getMessage(), false, forwardFail);
            else throw sfe;
        } catch (Exception ex) {
            if(Tools.isNotEmpty(forwardFail)) throw new SaveFormException(Prop.getInstance(request).getText("datatable.error.unknown"), ex.getCause(), false, forwardFail);
            else throw ex;
        }
    }

    /**
     * Internal save operation that persists form data, processes uploaded files,
     * generates optional PDF, and sends the notification email.
     *
     * @param formName     technical name of the form
     * @param formSettings form configuration
     * @param docId        resolved document ID for context/subject; {@code -1} if unknown
     * @param subject      email subject to be used
     * @param request      current HTTP request
     * @return unused value (returns {@code null}); present for legacy compatibility
     * @throws SaveFormException when validation or email sending fails
     * @throws IOException       when file operations fail
     */
    private final String saveFormAnswers(String formName, FormSettingsEntity formSettings, int docId, String subject, HttpServletRequest request) throws SaveFormException, IOException {

        String recipients = null;
		if (Tools.isNotEmpty(formSettings.getRecipients()))
			recipients = WriteTag.decodeEmailAddress(formSettings.getRecipients());

		if (recipients != null && recipients.indexOf('@') == -1)
			recipients = null;

		if ("cloud".equals(Constants.getInstallName()) && Tools.isEmpty(recipients)) {
            UserDetails admin = CloudToolsForCore.getAdmin();
			if (admin != null && Tools.isEmail(admin.getEmail())) recipients = admin.getEmail();
		}

        int userId = -1;
        Identity currentUser = UsersDB.getCurrentUser(request);
        if(currentUser != null) {
            userId = currentUser.getUserId();
        }

        if(userId > 0 && Tools.isTrue(formSettings.getOverwriteOldForms())) {
            formsRepository.deleteAllUserSubmitted(formName, CloudToolsForCore.getDomainId(), Long.valueOf(userId));
        }

        FormsEntity form = new FormsEntity();
        form.setFormName(formName);
        form.setDomainId(CloudToolsForCore.getDomainId());
        form.setCreateDate(new Date());
        form.setDocId(docId);
        form.setUserId(Long.valueOf(userId));

        // For file save we need formId ... sooo save it as it is and then use id
        form.setData("-");
        form = formsRepository.save(form);

        if(form == null) throw new IllegalStateException("saveFormAnswers - Error during saving FormsEntity.");

        //data MUST be set sooner than HTML
        FormFiles formFiles = new FormFiles(); // in form files we store all needed files, files names etc.
        setFormDataBeforeSave(form, formName, request, formFiles, formSettings);

        // set html
        htmlHandler.setFormHtml(form, request, docId);

        form = formsRepository.save(form);

        if(form == null) throw new IllegalStateException("saveFormAnswers - Error during saving FormsEntity.");

        // NEW PART
        String pdfUrl = "";
        if(Tools.isTrue(formSettings.getIsPdf())) {
			pdfUrl = FormMailAction.saveFormAsPdf(htmlHandler.getFormPdfVersion(), form.getId().intValue(), request);
			IwcmFile pdfFile =  new IwcmFile(pdfUrl);
			formFiles.getFileNames().put(pdfFile.getName(), pdfFile.getName());
			formFiles.getFileNamesSendLater().append(FormMailAction.FORM_FILE_DIR).append(pdfFile.getName()).append(";").append(pdfFile.getName());
            formFiles.getAttachs().add(new IwcmFile(pdfUrl));
		}

        boolean attachFiles = true;
        if(formFiles.getAttachs().size() > 0) {
            //ak je velkost prilohy vacsia ako stanovena hranica, prilohy k mailu nepripojim
            long size = 0;
			for(IwcmFile file : formFiles.getAttachs()) {
				size += file.length();
				if(size > Constants.getLong("maxSizeOfAttachments")) attachFiles = false;
			}

            // Set files into form
            form.setFiles( String.join(",", formFiles.getFileNames().keySet()) );
            formsRepository.save(form);

            if(attachFiles == false) {
                formFiles.getFileNamesSendLater().setLength(0);
                Logger.println(FormMailAction.class, "Nepripajam prilohy do e-mailu: prekorocena max. velkost priloh="+size);
            }
        }

        // SEND MAIL
        formMailService.sendMail(form, recipients, subject, formFiles, attachFiles, htmlHandler.getCssDataPair().getFirst(), new StringBuilder(htmlHandler.getFormHtmlBeforeCss()), request);

        return null;
    }

    /**
     * Builds and assigns the data payload for the given form before saving.
     * <p>
     * Iterates over validated form items, reads values from the session using the
     * multistep prefix, encrypts values when a public key is provided, and assembles
     * a pipe-delimited key-value string expected by the persistence layer. For multi-upload
     * items, files are processed and associated file names are recorded.
     *
     * @param form        persisted {@link FormsEntity} that will receive its data string
     * @param formName    technical name of the form (used for session key prefix)
     * @param request     current HTTP request to access the session
     * @param formFiles   container for associated file names and attachments
     * @param formSettings form configuration containing encryption key and options
     */
    private final void setFormDataBeforeSave(FormsEntity form, String formName, HttpServletRequest request, FormFiles formFiles, FormSettingsEntity formSettings) {
        String prefix = MultistepFormsService.getSessionKey(formName, request) + "_";
        StringBuilder data = new StringBuilder();

        CryptoFactory cryptoFactory = new CryptoFactory();
		String publicKey = formSettings.getEncryptKey();
        Prop prop = Prop.getInstance(request);

        //Fields in order
        for(FormItemEntity stepItem : MultistepFormsService.getFormItemsForValidation(formName)) {
            if("captcha".equals(stepItem.getFieldType())) continue;

            String sessionValue = String.valueOf(request.getSession().getAttribute(prefix + stepItem.getItemFormId()));
            if(sessionValue == null) sessionValue = "";

            //escape HTML code if needed
            String code = prop.getText("components.formsimple.input." + stepItem.getFieldType());
            sessionValue = filterHtml(code, sessionValue);
            //disable pipe as it is used as separator
            sessionValue = Tools.replace(sessionValue, "|", "&#124;");

            if(stepItem.getFieldType().startsWith(MultistepFormsService.MULTIUPLOAD_PREFIX)) {
                //Save files
                List<String> savedFileNames = saveFiles(sessionValue, form.getId(), formFiles);

                String value = savedFileNames.size() > 0 ? String.join(",", savedFileNames) : "";
                data.append(stepItem.getItemFormId()).append("-fileNames");
                data.append("~").append(value).append("|");

                continue;
            }

            // Other fields
            String value = Tools.isNotEmpty(sessionValue) && Tools.isNotEmpty(publicKey) ? cryptoFactory.encrypt(sessionValue, publicKey) : sessionValue;
            data.append(stepItem.getItemFormId()).append("~");
            data.append(value).append("|");
        }

        //Remove last "|"
        data.deleteCharAt(data.length() - 1);
        form.setData( data.toString() );
    }

    public static boolean isFilterHtml(String code) {
        if(code != null && code.contains("-wysiwyg")) return false;
        return true;
    }

    public static String filterHtml(String code, String fieldValue) {
        if (isFilterHtml(code)) {
            return ResponseUtils.filter(fieldValue);
        }
        //for wysiwyg fields (quill) filter at least unsafe HTML code
        return AllowSafeHtmlAttributeConverter.sanitize(fieldValue);
    }

    /**
     * Moves uploaded temporary files into the form directory and renames them using the form ID.
     * <p>
     * The {@code keysString} contains semicolon-separated temporary keys. For each key, the
     * file is moved, renamed to "{formId}_{originalName}", and recorded in {@code formFiles}.
     * When SMTP server usage is disabled, a send-later record (virtual path and file name) is appended.
     *
     * @param keysString semicolon-separated list of temporary upload keys; may be empty
     * @param formId     current form ID used for renaming persisted files
     * @param formFiles  container updated with file names, virtual paths and attachments
     * @return list of original file names that were successfully saved for keysString provided
     */
    private final List<String> saveFiles(String keysString, Long formId, FormFiles formFiles) {
        List<String> savedFileNames = new ArrayList<>();

        if(Tools.isEmpty(keysString)) return savedFileNames;

        String baseDirName = PathFilter.getRealPath(FormMailAction.FORM_FILE_DIR + "/");
		IwcmFile dir = new IwcmFile(baseDirName);
		if (!dir.exists()) dir.mkdirs();

        // value containes files keys joined via ";"
        for(String param : Tools.getTokens(keysString, ";")) {
            try {
                String fileName = XhrFileUploadServlet.getService().moveFile(param, baseDirName + File.separator);
                IwcmFile file = new IwcmFile(dir, fileName);
                if (file.exists()) {
                    IwcmFile dest = new IwcmFile(dir, formId + "_" + fileName);
                    file.renameTo(dest);
                    if (dest.exists()) {

                        formFiles.getFileNames().put(dest.getName(), fileName);
                        savedFileNames.add(fileName);

                        if ("false".equals(Constants.getString("useSMTPServer"))) {
							formFiles.getFileNamesSendLater().append(";").append(dest.getVirtualPath()).append(";").append(dest.getName());
						}

						formFiles.getAttachs().add(dest);

                    }
                }
            } catch (IOException io) {
                //
            }
        }

        return savedFileNames;
    }
}