package sk.iway.iwcm.form;

import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

public interface AfterSendInterceptor
{
    public String intercept(MimeMessage message, Multipart multipart, HttpServletRequest request);
}
