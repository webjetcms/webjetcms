 package sk.iway.iwcm.system.jpa;

import java.util.Calendar;
import java.util.Date;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class DefaultTimeValueConverter implements AttributeConverter<Date, Date> {

    @Override
    public Date convertToDatabaseColumn(Date oldDate) {
        return getValidTimeValue(oldDate);
    }

    @Override
    public Date convertToEntityAttribute(Date oldDate) {
        return getValidTimeValue(oldDate);
    }

    // All Date values of type DataTableColumnType.TIME_HM or DataTableColumnType.TIME_HMS must have same day/month/year (01.01.2000)

    /**
     * If input oldValue is null, it will be returned default Date 01-01-2000 00:00:00. If input oldValue isnt null, 
     * value of hours/minutes/seconds will remain, but 
     * value of day/month/year will be set on default value 01-01-2000 (rest is same).
     * @param oldDate value of date we need to correct
     * @return valid date for TIME_HM and TIME_HMS DataTableColumnTypes
     */
    public static Date getValidTimeValue(Date oldDate) {

        if(oldDate == null) {
            Calendar newInstance = Calendar.getInstance();
            newInstance.set(2000, 0, 1, 0, 0, 0);
            newInstance.set(Calendar.MILLISECOND, 0);
            return newInstance.getTime();
        }

        Calendar oldInstance = Calendar.getInstance();
        oldInstance.setTime(oldDate);

        Calendar newInstance = Calendar.getInstance();
        newInstance.set(2000, 0, 1, oldInstance.get(Calendar.HOUR_OF_DAY), oldInstance.get(Calendar.MINUTE), oldInstance.get(Calendar.SECOND));
        newInstance.set(Calendar.MILLISECOND, 0);
        //Return valid date time
        return newInstance.getTime();
    }

    /**
     * Create valid date where day/month/year is set to 01.01.2000, seconds are set at 0 and 
     * hours/minutes are set using input values.
     * @param hours number of hours we want to set into date
     * @param minutes number of minutes we want to set into date
     * @return valid date for TIME_HM and TIME_HMS DataTableColumnTypes
     */
    public static Date getValidTimeValue(int hours, int minutes) { 
        return getValidTimeValue(hours, minutes, 0);
    }

    /**
     * Create valid date where day/month/year is set to 01.01.2000 and
     * seconds/hours/minutes are set using input values.
     * @param hours number of hours we want to set into date
     * @param minutes number of minutes we want to set into date
     * @param seconds number of seconds we want to set into date
     * @return valid date for TIME_HM and TIME_HMS DataTableColumnTypes
     */
    public static Date getValidTimeValue(int hours, int minutes, int seconds) { 
        Calendar newInstance = Calendar.getInstance();
        newInstance.set(2000, 0, 1, hours, minutes, seconds);
        newInstance.set(Calendar.MILLISECOND, 0);
        //Return valid date time
        return newInstance.getTime();
    }

    /**
     * Combine yyyy-mm-dd from input "date" with hh:mm:ss from input "time"
     * and return new date. Milliseconds are default set to 0. 
     * If any of iput values is null, null will be returned. 
     * @param date date value representing yyyy-mm-dd in new date
     * @param time date value representing hh:mm:ss  in new date
     * @return date value represent date time combination
     */
    public static Date combineDateWithTime(Date date, Date time) {
        if(date == null || time == null)  return null;

        Calendar dateInstance = Calendar.getInstance();
        dateInstance.setTime(date);
        Calendar timeInstance = Calendar.getInstance();
        timeInstance.setTime(time);

        //Move hh:mm:ss from time to date
        dateInstance.set(Calendar.HOUR_OF_DAY, timeInstance.get(Calendar.HOUR_OF_DAY));
        dateInstance.set(Calendar.MINUTE, timeInstance.get(Calendar.MINUTE));
        dateInstance.set(Calendar.SECOND, timeInstance.get(Calendar.SECOND));
        dateInstance.set(Calendar.MILLISECOND, 0);

        return dateInstance.getTime();
    }
}
