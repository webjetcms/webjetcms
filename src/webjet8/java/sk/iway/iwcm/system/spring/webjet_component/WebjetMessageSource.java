package sk.iway.iwcm.system.spring.webjet_component;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.i18n.Prop;

// trieda pre preklady
public class WebjetMessageSource implements MessageSource {

    @Override
    public String getMessage(String s, Object[] objects, String s1, Locale locale) {
        return getProp(locale).getText(s);
    }

    @Override
    public String getMessage(String s, Object[] objects, Locale locale) throws NoSuchMessageException {

        if(!InitServlet.isWebjetConfigured()) {
            RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
            return Prop.getInstance(rb.getLng()).getText(s);
        }

        try {
                if (objects != null && objects.length>0) {
                String[] objString = new String[objects.length];
                for (int i=0; i<objects.length; i++) {
                    if (objects[i] != null) objString[i] = objects[i].toString();
                }
                return getProp(locale).getTextWithParams(s, objString);
            }
        } catch (Exception ex) {
            Logger.error(WebjetMessageSource.class, ex);
        }
        return getProp(locale).getText(s);
    }

    @Override
    public String getMessage(MessageSourceResolvable messageSourceResolvable, Locale locale) throws NoSuchMessageException {
        return getProp(locale).getText(messageSourceResolvable.getDefaultMessage());
    }

    private Prop getProp(Locale locale) {
        return Prop.getInstance(locale.getLanguage());
    }
}