package sk.iway.iwcm.system.spring;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.core.convert.converter.Converter;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

public class DateConverter implements Converter<String, Date> {

    private List<String> formats;

    public DateConverter() {
        formats = new ArrayList<>();

        formats.add("dd.MM.yyyy HH:mm:ss");
        formats.add("dd.MM.yyyy HH:mm");
        formats.add("dd.MM.yyyy");
        formats.add("yyyy-MM-dd HH:mm:ss");
        formats.add("yyyy-MM-dd HH:mm");
        formats.add("yyyy-MM-dd");

        String formatsString = Constants.getString("SpringDateFormats");
        if (Tools.isNotEmpty(formatsString)) {
            formats.addAll(Tools.getStringListValue(Tools.getTokens(formatsString, "|")));
        }
    }

    @Override
    public Date convert(String source) {

        if (source == null || source.isEmpty()) {
            return null;
        }

        try {
            long timestamp = Long.parseLong(source);
            return new Date(timestamp);
        } catch (Exception ex) {
            //continue
        }

        for (String format : formats) {
            try {
                return new SimpleDateFormat(format).parse(source);
            } catch (ParseException e) {
                //sk.iway.iwcm.Logger.error(e);
            }
        }

        Logger.debug(DateConverter.class, "Unparseable date: " + source);
        return null;
    }
}