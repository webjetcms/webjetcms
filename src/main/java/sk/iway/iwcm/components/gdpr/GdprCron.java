package sk.iway.iwcm.components.gdpr;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.velocity.VelocityContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.gdpr.model.GdprRegExpBean;
import sk.iway.iwcm.components.gdpr.model.GdprResults;
import sk.iway.iwcm.components.news.VelocityEngine;
import sk.iway.iwcm.components.news.VelocityTools;
import sk.iway.iwcm.helpers.MailHelper;

public class GdprCron {
    public static void main(String[] args)
    {
        Logger.debug(GdprCron.class, "Spúšťam GDPR cron");

        List<String> emails = new ArrayList<>();
        List<GdprRegExpBean> regexps = new ArrayList<>();
        List<GdprModule> modules = new ArrayList<>();

        for (String arg : args) {
            String[] argArray = arg.split("=");
            String[] valuesArray = argArray[1].split(",");

            if (argArray[0].equalsIgnoreCase("emails")) {
                emails = new ArrayList<>(Arrays.asList(valuesArray));
                continue;
            }

            if (argArray[0].equalsIgnoreCase("regexps")) {
                regexps = new ArrayList<>();
                for(String value:valuesArray)
                {
                    regexps.add(new GdprRegExpBean(value));
                }
                //regexps = new ArrayList<GdprRegExpBean>(Arrays.asList(valuesArray));
                continue;
            }

            if (argArray[0].equalsIgnoreCase("modules")) {
                for (String value : valuesArray) {
                    modules.add(GdprModule.valueOf(value));
                }
            }
        }

        Logger.debug(GdprCron.class, "Emails: " + Tools.join(emails, ", "));
        Logger.debug(GdprCron.class, "Regexps: " + Tools.join(regexps, ", "));
        Logger.debug(GdprCron.class, "Modules: " + Tools.join(modules, ", "));

        GdprSearch gdprSearch = new GdprSearch(regexps, modules);
        GdprResults results = gdprSearch.search(null);

        StringWriter message = new StringWriter();
        VelocityEngine ve = new VelocityEngine();
        VelocityContext vc = new VelocityContext();

        vc.put("results", results.getResults());

        ve.init();
        ve.evaluate(vc, message, "GdprCron evaluate", VelocityTools.upgradeTemplate(FileTools.readFileContent("/components/gdpr/admin_list_search_detail_velocity.jsp")));

        MailHelper mailer = new MailHelper()
            .setFromEmail(Constants.getString("GdprCron.fromEmail", "web@interway.sk"))
            .setFromName(Constants.getString("GdprCron.fromName", "WebJET CMS"))
            .setMessage(message.toString())
            .setSubject(Constants.getString("GdprCron.subject", "GDPR Cron"));

        for (String email : emails) {
            mailer.addRecipient(email);
        }

        mailer.send();
    }
}