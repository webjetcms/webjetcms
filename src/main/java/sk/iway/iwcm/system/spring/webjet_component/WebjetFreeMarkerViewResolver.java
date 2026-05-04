package sk.iway.iwcm.system.spring.webjet_component;

import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

// extednuty InternalResourceViewResolver aby bolo mozne nastavit a vybrat prefix a suffix
public class WebjetFreeMarkerViewResolver extends FreeMarkerViewResolver {
    @Override
    public String getPrefix() {
        return super.getPrefix();
    }

    @Override
    public void setPrefix(String prefix) {
        super.setPrefix(prefix);
    }

    @Override
    public String getSuffix() {
        return super.getSuffix();
    }

    @Override
    public void setSuffix(String suffix) {
        super.setSuffix(suffix);
    }
}
