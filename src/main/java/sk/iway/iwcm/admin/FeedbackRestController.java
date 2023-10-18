package sk.iway.iwcm.admin;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.util.ResponseUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.upload.AdminUploadServlet;
import sk.iway.iwcm.helpers.MailHelper;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.users.UsersDB;

/**
 * 54177 - Zalozky - odosle email so spatnou vazbou k WebJET CMS
 */
@RestController
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class FeedbackRestController {

    private static String textKey = "data[text]";
    private static String filesKeys = "data[fileKeys][]";
    private static String isAnonymousKey = "data[isAnonymous]";

    @RequestMapping(path={"/admin/rest/feedback"})
    public String sendFeedback(HttpServletRequest request) {

        String recipient = Constants.getString("problemReportEmail");

        //Create mail helper
        MailHelper mailHelper = new MailHelper();

        //Set default params
        mailHelper.setSubject("Spätná väzba k WebJET CMS - " + Tools.getServerName(request));
        mailHelper.setRecipients(recipient);

        //Get params from request
        Map<String, String[]> params =  request.getParameterMap();

        //Handle text
        StringBuilder feedbackText = new StringBuilder();
        feedbackText.append("<p>")
            .append(Tools.replace(ResponseUtils.filter(params.get(textKey)[0]), "\n", "<br/>\n"))
            .append("</p>\n<p>");

        //Handle isAnonymous
        if(!params.get(isAnonymousKey)[0].equals("true")) {
            Identity user = UsersDB.getCurrentUser(request);

            //Use user name and email
            mailHelper.setFromName(user.getFullName());
            mailHelper.setFromEmail(user.getEmail());

            //Add info about sender (user) to feedback text
            feedbackText.append("Login: " + ResponseUtils.filter(user.getLogin()) + "<br/>");
            feedbackText.append("Name: " + ResponseUtils.filter(user.getFullName()) + "<br/>");
            feedbackText.append("Email: " + ResponseUtils.filter(user.getEmail()) + "<br/>");
            feedbackText.append("User-Agent: " + ResponseUtils.filter(request.getHeader("User-Agent")) + "<br/>");
        } else {
            //Set anonymous
            mailHelper.setFromName("Anonymous");
            mailHelper.setFromEmail(getFirstEmail(recipient));
        }

        //Add more info
        feedbackText.append("Domain: ").append(ResponseUtils.filter(Tools.getServerName(request))).append("<br/>");
        feedbackText.append("IP: ").append(ResponseUtils.filter(Tools.getRemoteIP(request))).append("<br/>");
        feedbackText.append("Date: ").append(ResponseUtils.filter(Tools.formatDateTimeSeconds(Tools.getNow()))).append("<br/>");

        feedbackText.append("\n</p>");

        //Set feddback text to email
        mailHelper.setMessage(feedbackText.toString());

        //Handle files
        if(params.get(filesKeys) != null) {
            for(String fileKey : params.get(filesKeys)) {

                //Get file path
                String filePath = AdminUploadServlet.getTempFilePath(fileKey);

                IwcmFile file = new IwcmFile(filePath);

                //Add file path to email
                mailHelper.addAttachment(file);

            }
        }

        //Chceck
        if(SpamProtection.canPost("form", feedbackText.toString(), request) && mailHelper.send()) {
            //Delete temporal files
            if(params.get(filesKeys) != null) {
                for(String fileKey : params.get(filesKeys)) {
                    AdminUploadServlet.deleteTempFile(fileKey);
                }
            }
        } else {
            throw new IllegalArgumentException("Sending mail failed.");
        }

        return "OK";
    }

    /**
	 * Vrati prvy email so zoznamu (email1@domena.sk,email2@domena.sk vrati email1@domena.sk)
	 * @param emails
	 * @return
	 */
	private static String getFirstEmail(String emails)
	{
		if (Tools.isEmpty(emails)) return "";

		String emailsArr[] = Tools.getTokens(emails, ",", true);
		if (emailsArr.length>0) return emailsArr[0];

		return "";
	}
}
