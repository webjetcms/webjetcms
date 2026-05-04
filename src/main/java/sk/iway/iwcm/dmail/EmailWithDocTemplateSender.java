package sk.iway.iwcm.dmail;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.tools.generic.DateTool;
import org.jsoup.parser.Parser;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;

/**
 * Date: 21.02.2018
 * Time: 11:01
 * Project: webjet8
 * Company: InterWay a. s. (www.interway.sk)
 * Copyright: InterWay a. s. (c) 2001-2018
 *
 * @author mpijak
 */
public class EmailWithDocTemplateSender {
    private String templateProcessor ;

    public EmailWithDocTemplateSender() {
        this.templateProcessor = "freemarker";
    }

    public EmailWithDocTemplateSender(String templateProcessor) {
        this.templateProcessor = templateProcessor;
    }

    public boolean send(String toEmail, String docUrl, String jsonData) throws IOException {
        Map<String, Object> dataMap = this.converJsonStringToDataMap(jsonData);
        return this.send(toEmail, docUrl, dataMap, null);
    }

    public boolean send(String toEmail, String docUrl, Map<String, Object> params) {
        return (send(toEmail, docUrl, params, null));
    }

    public boolean send(String toEmail, String docUrl, Map<String, Object> params, String attachmentsList) {
        DocDetails docDetails = this.getDocDetails(docUrl);

        if (docDetails == null) {
            Logger.debug(getClass(), "Document with docurl " + docUrl + "not found !!!");
            return false;
        }

        String mailBody = "";
        String fromEmail = "";
        String title = "";
        String fromName = "";

        mailBody = this.getParsedTemplate(docDetails.getData(), params, docUrl);
        title = this.getParsedTemplate(docDetails.getFieldA(), params, docUrl+"fieldA");
        fromEmail = this.getParsedTemplate(docDetails.getFieldB(), params, docUrl+"fieldB");
        fromName = this.getParsedTemplate(docDetails.getFieldC(), params, docUrl+"fieldC");

        return SendMail.send(fromName, fromEmail, toEmail, title, mailBody, attachmentsList);
    }

    public boolean sendLater(String toEmail, String docUrl, String jsonData, HttpServletRequest request) throws IOException {
        Map<String, Object> dataMap = this.converJsonStringToDataMap(jsonData);
        return this.sendLater(toEmail, docUrl, dataMap, request);
    }

    public boolean sendLater(String toEmail, String docUrl, Map<String, Object> params, HttpServletRequest request) {
        DocDetails docDetails = this.getDocDetails(docUrl);

        String mailBody = "";
        String fromEmail = "";
        String title = "";
        String fromName = "";

        mailBody = this.getParsedTemplate(docDetails.getData(), params, docUrl);
        title = this.getParsedTemplate(docDetails.getFieldA(), params, docUrl+"fieldA");
        fromEmail = this.getParsedTemplate(docDetails.getFieldB(), params, docUrl+"fieldB");
        fromName = this.getParsedTemplate(docDetails.getFieldC(), params, docUrl+"fieldC");

        String baseHref = Tools.getBaseHref(request)+"/";
        return SendMail.sendLater(fromName, fromEmail, toEmail, null, null, null, title, mailBody, baseHref, null, null);
    }

    public DocDetails getDocDetails(String url) {
        String domainName = null;
        RequestBean currentRequestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (currentRequestBean != null) {
            domainName = currentRequestBean.getDomain();
        }
        int docId = DocDB.getDocIdFromURL(url, domainName);
        return DocDB.getInstance().getDoc(docId);
    }

    private Map<String, Object> converJsonStringToDataMap(String jsonData) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(jsonData, new TypeReference<Map<String, Object>>(){});
        return map;
    }

    private VelocityContext convertDataMapToVelocityContext(Map<String, Object> data) {
        VelocityContext context = new VelocityContext();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }
        return context;
    }

    private String getParsedVelocityTemplate(String html, Map<String, Object> params, String templateName) throws ParseException {
        VelocityContext context = convertDataMapToVelocityContext(params);
        context.put("date", new DateTool());
        String htmlClean = this.getCleanHtml(html);
        Reader reader = new StringReader(htmlClean);

        Template templateByName = new Template();
        templateByName.setName(templateName);

        Template template = new Template();
        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
        template.setRuntimeServices(runtimeServices);
        template.setData(runtimeServices.parse(reader, templateByName));
        template.initDocument();

        StringWriter writer = new StringWriter();
        template.merge(context, writer);

        return writer.toString();
    }

    private String getParsedFreemarkerTemplate(String freemarkerTemplateHtml, Map<String, Object> params, String templateName) throws IOException, TemplateException {
        Configuration configuration = createFreemarkerConfiguration();
        String htmlClean = this.getCleanHtml(freemarkerTemplateHtml);
        freemarker.template.Template template = new freemarker.template.Template(templateName, htmlClean, configuration);

        Writer out = new StringWriter();
        template.process(params, out);

        out.close();

        return out.toString();
    }

    private Configuration createFreemarkerConfiguration() {
        Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        config.setDefaultEncoding("windows-1250");
        config.setEncoding(Locale.getDefault(), "windows-1250");
        return config;
    }

    private String getCleanHtml(String html) {
        String htmlClean = html;
        htmlClean = Tools.replace(htmlClean, "<pre>", "");
        htmlClean = Tools.replace(htmlClean, "</pre>", "");
        htmlClean = Tools.replace(htmlClean, "<code>", "");
        htmlClean = Tools.replace(htmlClean, "</code>", "");
        htmlClean = Parser.unescapeEntities(htmlClean, true);
        return htmlClean;
    }

    public String getParsedTemplate(String templateName, Map<String, Object> params, String templateHtml) {
        String res = "";
        if (this.templateProcessor.equals("freemarker")) {
            try {
                res = this.getParsedFreemarkerTemplate(templateName, params, templateHtml);
            } catch (IOException e) {
                sk.iway.iwcm.Logger.error(e);
            } catch (TemplateException e) {
                sk.iway.iwcm.Logger.error(e);
            }
        } else {
            try {
                res = this.getParsedVelocityTemplate(templateName, params, templateHtml);
            } catch (ParseException e) {
                sk.iway.iwcm.Logger.error(e);
            }
        }
        return res;
    }

    public String getTemplateProcessor() {
        return templateProcessor;
    }

    public void setTemplateProcessor(String templateProcessor) {
        this.templateProcessor = templateProcessor;
    }
}
