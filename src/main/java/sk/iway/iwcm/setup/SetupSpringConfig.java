package sk.iway.iwcm.setup;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;

@Configuration
public class SetupSpringConfig implements WebMvcConfigurer {

    @Bean
    public AcceptHeaderLocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver() {
            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                String lng = Prop.getLng(request, false);
                if (Tools.isEmpty(lng)) lng = "sk";
                if ("cz".equals(lng)) lng = "cs";
                return org.springframework.util.StringUtils.parseLocaleString(PageLng.getUserLngIso(lng).replace("-", "_"));
            }
        };

        localeResolver.setDefaultLocale(new Locale(PageLng.getUserLngIso(Constants.getString("defaultLanguage")).replace("-", "_")));
        return localeResolver;
    }

}
