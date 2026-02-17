package sk.iway.iwcm.form;

import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;

public interface AfterSendInterceptor
{
    public String intercept(MimeMessage message, Multipart multipart, HttpServletRequest request);
}
