package sk.iway.iwcm.components.multistep_form.rest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import sk.iway.iwcm.tags.WriteTag;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@Service
public class SaveFormService {

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
        private final List<String> fileNames = new ArrayList<>();
        private final StringBuilder fileNamesSendLater = new StringBuilder("");
        private final List<IwcmFile> attachs = new ArrayList<>();
    }

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

    private final String saveFormAnswers(String formName, FormSettingsEntity formSettings, int docId, String subject, HttpServletRequest request) throws SaveFormException, IOException {

        String recipients = null;
		if (Tools.isNotEmpty(formSettings.getRecipients()))
			recipients = WriteTag.decodeEmailAddress(formSettings.getRecipients());

		if (recipients != null && recipients.indexOf('@') == -1)
			recipients = null;

		//ak aktualizujeme zaznam, toto potom nastavime na false, aby sa nic neposlalo
		boolean emailAllowed = false;

        String allowedRecipients = Constants.getString("formmailAllowedRecipients");
		if (Tools.isEmpty(allowedRecipients)) allowedRecipients = "@"+Tools.getServerName(request); //aby sa nahodou nestalo, ze je niekde zabudnute nastavenie email adresy
		if (emailAllowed == false && Tools.isNotEmpty(allowedRecipients) && Tools.isNotEmpty(recipients)) {
			try {
                for(String recipient : recipients.split(",")) {
                    boolean emailFound = false;
                    for(String allowedRecipient : allowedRecipients.split(",")) {
                        if(recipient.toLowerCase().endsWith(allowedRecipient)) {
                            emailFound = true;
                            break;
                        }
                    }

                    if(emailFound) {
                        emailAllowed = true;
                        break;
                    }
                }
			}
			catch (Exception ex) { Logger.error(FormMailAction.class, ex); }
		}

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

        if(form == null) throw new IllegalStateException("ou shiiiii");

        //data MUST be set sooner than HTML
        FormFiles formFiles = new FormFiles(); // in form files we store all needed files, files names etc.
        setFormDataBeforeSave(form, formName, request, formFiles, formSettings);

        // set html
        htmlHandler.setFormHtml(form, request, docId);

        form = formsRepository.save(form);

        if(form == null) throw new IllegalStateException("ou shiiiii");

        // NEW PART
        String pdfUrl = "";
        if(Tools.isTrue(formSettings.getIsPdf())) {
			pdfUrl = FormMailAction.saveFormAsPdf(htmlHandler.getFormPdfVersion(), form.getId().intValue(), request);
			IwcmFile pdfFile =  new IwcmFile(pdfUrl);
			formFiles.getFileNames().add(pdfFile.getName());
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
            form.setFiles( String.join(",", formFiles.getFileNames()) );
            formsRepository.save(form);

            if(attachFiles == false) {
                formFiles.getFileNamesSendLater().setLength(0);
                Logger.println(FormMailAction.class, "Nepripajam prilohy do e-mailu: prekorocena max. velkost priloh="+size);
            }
        }

        // SEND MAIL
        formMailService.mailShit(form, emailAllowed, recipients, subject, formFiles, attachFiles, htmlHandler.getCssDataPair().getFirst(), new StringBuilder(htmlHandler.getFormHtmlBeforeCss()), request);

        return null;
    }

    private final void setFormDataBeforeSave(FormsEntity form, String formName, HttpServletRequest request, FormFiles formFiles, FormSettingsEntity formSettings) {
        String prefix = MultistepFormsService.getSessionKey(formName, request) + "_";
        StringBuilder data = new StringBuilder();

        CryptoFactory cryptoFactory = new CryptoFactory();
		String publicKey = formSettings.getEncryptKey();

        //Fields in order
        for(FormItemEntity stepItem : MultistepFormsService.getFormItemsForValidation(formName)) {
            if("captcha".equals(stepItem.getFieldType())) continue;

            String sessionValue = String.valueOf(request.getSession().getAttribute(prefix + stepItem.getItemFormId()));
            if(sessionValue == null) sessionValue = "";

            if(stepItem.getFieldType().startsWith(MultistepFormsService.MULTIUPLOAD_PREFIX)) {
                //Save files
                saveFiles(sessionValue, form.getId(), formFiles);

                String value = formFiles.getFileNames().size() > 0 ? String.join(",", formFiles.getFileNames()) : "";
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

    private final void saveFiles(String keysString, Long formId, FormFiles formFiles) {
        if(Tools.isEmpty(keysString)) return;

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

                        formFiles.getFileNames().add(dest.getName());

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
    }
}