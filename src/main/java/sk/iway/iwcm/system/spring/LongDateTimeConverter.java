package sk.iway.iwcm.system.spring;

import java.util.Date;

import org.springframework.core.convert.converter.Converter;

import sk.iway.iwcm.Logger;

public class LongDateTimeConverter implements Converter<Long, Date> {

    @Override
    public Date convert(Long source) {

        if (source == null) {
            return null;
        }

        try {
            return new Date(source);
        }
        catch (Exception ex) {
            //sk.iway.iwcm.Logger.error(ex);
        }

        Logger.debug(LongDateTimeConverter.class, "Unparseable date: " + source);
        return null;
    }
}