package sk.iway.iwcm.admin;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.upload.AdminUploadServlet;
import sk.iway.iwcm.helpers.MailHelper;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;
import sk.iway.iwcm.tags.support.ResponseUtils;
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
        MailHelper mailHelper = new MailHelper()
            .setSubject("Spätná väzba k WebJET CMS - " + Tools.getServerName(request))
            .setToEmail(recipient);

        //Get params from request
        Map<String, String[]> params =  request.getParameterMap();

        // Preserve basic editor formatting while retaining support for plain-text clients.
        String message = prepareFeedbackText(request.getParameter(textKey), "true".equals(request.getParameter("data[isHtml]")));
        StringBuilder feedbackText = new StringBuilder();
        feedbackText.append(message).append("\n<p>");

        feedbackText.append("Version: " + ResponseUtils.filter(InitServlet.getActualVersionLong()) + "<br/>");
        String type = request.getParameter("data[type]");
        if ("idea".equals(type) || "problem".equals(type) || "praise".equals(type)) {
            feedbackText.append("Type: ").append(type).append("<br/>");
        }
        String pageUrl = request.getParameter("data[pageUrl]");
        if (Tools.isNotEmpty(pageUrl)) feedbackText.append("Page: ").append(ResponseUtils.filter(pageUrl)).append("<br/>");
        feedbackText.append("User-Agent: ").append(ResponseUtils.filter(request.getHeader("User-Agent"))).append("<br/>");

        //Handle isAnonymous
        if(!"true".equals(request.getParameter(isAnonymousKey))) {
            Identity user = UsersDB.getCurrentUser(request);

            //Use user name and email
            mailHelper.setFromName(user.getFullName());
            mailHelper.setFromEmail(user.getEmail());

            //Add info about sender (user) to feedback text
            feedbackText.append("Login: " + ResponseUtils.filter(user.getLogin()) + "<br/>");
            feedbackText.append("Name: " + ResponseUtils.filter(user.getFullName()) + "<br/>");
            feedbackText.append("Email: " + ResponseUtils.filter(user.getEmail()) + "<br/>");
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
     * Validates the message and sanitizes rich text using the shared HTML policy.
     *
     * @param text submitted message
     * @param isHtml whether the client sends editor HTML instead of plain text
     * @return safe HTML suitable for the feedback email
     */
    static String prepareFeedbackText(String text, boolean isHtml) {
        String message = isHtml
            ? AllowSafeHtmlAttributeConverter.sanitize(text)
            : "<p>" + Tools.replace(ResponseUtils.filter(text == null ? "" : text), "\n", "<br/>\n") + "</p>";
        if (Jsoup.parse(message).text().replace('\u00a0', ' ').trim().isEmpty()) {
            throw new IllegalArgumentException("Feedback text is required.");
        }
        return message;
    }

    /**
	 * Vrati prvy email so zoznamu (email1@domena.sk,email2@domena.sk vrati email1@domena.sk)
	 * @param emails
	 * @return
	 */
	private static String getFirstEmail(String emails)
	{
		if (Tools.isEmpty(emails)) return "";

		String[] emailsArr = Tools.getTokens(emails, ",", true);
		if (emailsArr.length>0) return emailsArr[0];

		return "";
	}
}
