package sk.iway.iwcm.components.gdpr;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.gdpr.model.DocumentsDB;
import sk.iway.iwcm.components.gdpr.model.FormsDB;
import sk.iway.iwcm.components.gdpr.model.ForumDB;
import sk.iway.iwcm.components.gdpr.model.GdprDB;
import sk.iway.iwcm.components.gdpr.model.GdprRegExpBean;
import sk.iway.iwcm.components.gdpr.model.QuestionsAnswersDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.tags.support_logic.ResponseUtils;

public enum GdprModule {
    WEB_STRANKY,
    FORMULARE,
    QA,
    FORUM;

    @Override
    public String toString() {
        String lng = (SetCharacterEncodingFilter.getCurrentRequestBean() != null && SetCharacterEncodingFilter.getCurrentRequestBean().getLng() != null) ? SetCharacterEncodingFilter.getCurrentRequestBean().getLng() : "sk";
        Prop prop = Prop.getInstance(lng);
        return prop.getText("components.gdpr.module."+this.name().toLowerCase());
    }

    public String getDisabledItemName() {
        if (this.equals(WEB_STRANKY)) {
            return "menuWebpages";
        }

        if (this.equals(FORMULARE)) {
            return "cmp_form";
        }

        if (this.equals(QA)) {
            return "menuQa";
        }

        if (this.equals(FORUM)) {
            return "cmp_diskusia";
        }

        throw new IllegalArgumentException("No Disabled Item Name for GdprModule " + this.name());
    }

    public GdprDB getDB() {
        if (this.equals(WEB_STRANKY)) {
            return DocumentsDB.getInstance();
        }

        if (this.equals(FORMULARE)) {
            return FormsDB.getInstance();
        }

        if (this.equals(QA)) {
            return QuestionsAnswersDB.getInstance();
        }

        if (this.equals(FORUM)) {
            return ForumDB.getInstance();
        }

        throw new IllegalArgumentException("No DB class for GdprModule " + this.name());
    }

    public static String getText(List<GdprRegExpBean> regexps, List<String> texts) {
        int prepend = Constants.getInt("gdprPrependCharacters") != -1 ? Constants.getInt("gdprPrependCharacters") : 30;
        int append = Constants.getInt("gdprAppendCharacters") != -1 ? Constants.getInt("gdprAppendCharacters") : 30;
        StringBuilder sb = new StringBuilder();

        int wordBegin = -1;
        int wordEnd = -1;

        for (GdprRegExpBean regexp : regexps) {

//            if (Constants.DB_TYPE == Constants.DB_MSSQL && Tools.isNotEmpty(regexp) && regexp.length() > 4)
//                regexp = regexp.substring(2,regexp.length()-2);

            Pattern pattern = Pattern.compile(regexp.getRegexpValue(), Pattern.CASE_INSENSITIVE);

            for (String text : texts) {
                if (Tools.isEmpty(text)) {
                    continue;
                }

                Matcher matcher = pattern.matcher(text);

                while (matcher.find()) {
                    wordBegin = matcher.start();
                    int start = wordBegin - prepend;
                    if (start < 0) {
                        start = 0;
                    }

                    wordEnd = matcher.end();
                   // wordEnd += append;
                    int end = wordEnd + append ;
                    if (end > text.length()) {
                        end = text.length();
                    }

                    if(wordEnd > wordBegin && wordBegin > 0)
                    {
                        if (start > 0)
                        {
                            sb.append("...");
                        }

                        //sb.append(text.substring(start, end));
                        sb.append(ResponseUtils.filter(text.substring(start, wordBegin )));
                        sb.append("<span title=\"").append(regexp.getRegexpName()).append("\" class=\"yellow_color\">").append(ResponseUtils.filter(text.substring(wordBegin , wordEnd)));
                        sb.append("</span>").append(ResponseUtils.filter(text.substring(wordEnd, end)));

                        if (end < text.length())
                        {
                            sb.append("...<br>");
                        }
                    }
                }
            }
        }

        return sb.toString();
    }
}
