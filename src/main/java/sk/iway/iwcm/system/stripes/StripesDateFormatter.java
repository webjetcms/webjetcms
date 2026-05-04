package sk.iway.iwcm.system.stripes;

import sk.iway.iwcm.Tools;

import java.util.Date;

import net.sourceforge.stripes.format.DateFormatter;

/**
 * Stripes date formatter using WebJET API Tools.formatDate, Tools.formatTime, Tools.formatDateTime
 */
public class StripesDateFormatter extends DateFormatter {

    @Override
    public String format(Date input) {
        //Logger.debug(StripesDateFormatter.class, "formatting date: " + input + " formatType=" + getFormatType());
        if (getFormatType()=="date") return Tools.formatDate(input);
        else if (getFormatType()=="time") return Tools.formatTime(input);
        else if (getFormatType()=="datetime") return Tools.formatDateTime(input);
        return super.format(input);
    }

}
