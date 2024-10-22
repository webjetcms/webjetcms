package sk.iway.iwcm;

import java.sql.Timestamp;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * Podporne metody
 *
 * @Title WebJET
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2002
 * @author $Author: jeeff mkolejak $
 * @version $Revision: 1.6 $
 * @created Date: 15.4.2014 12:41:45
 * @modified $Date: 2004/08/16 06:26:11 $
 */
public class DateTools
{
	private int poradieTyzdna = 0;
	private boolean hasRun = false;

	/**
	 * Vrati polnoc, koniec dnesneho dna
	 *
	 * @return calendar
	 */
	public static Calendar getMidnightToday()
	{
		Calendar cal = new GregorianCalendar();
		return midnightToday(cal);
	}

	/**
	 * Vrati polnoc, zadaj pocet dni dozadu
	 *
	 * @return calendar
	 */
	public static Calendar getMidnightDaysBefore(int count)
	{
		Calendar cal = new GregorianCalendar();
		return midnightDaysBefore(count, cal);
	}

	/**
	 * Vrati polnoc, ktorou den zacinal
	 *
	 * @param date
	 * @return calendar
	 */
	public static Calendar getMidninght(Date date)
	{
		Calendar cal = dateToCalendar(date);
		return midnightToday(cal);
	}

	/**
	 * Vrati hodinu vybrateho dna
	 *
	 * @param date
	 * @return calendar
	 */
	public static Calendar getHourOfDay(Date date, int hour)
	{
		Calendar cal = dateToCalendar(date);
		return hourOfDay(cal, hour);
	}

	/**
	 * Vrati x hodin pred uvedenym casom
	 *
	 * @param date
	 * @param hour
	 * @return
	 */
	public static Calendar getHoursBefore(Date date, int hour)
	{
		Calendar cal = dateToCalendar(date);
		cal.add(Calendar.HOUR, -hour);
		return cal;
	}

	/**
	 * Vrati polnoc ktora bude dnes
	 *
	 * @param calendar
	 * @return calendar
	 */
	public static Calendar midnightToday(Calendar cal)
	{
		setMidnight(cal);
		cal.add(Calendar.DAY_OF_MONTH, 1);
		return cal;
	}

	/**
	 * Vrati polnoc ktorou vybraty den zacinal
	 *
	 * @param cal
	 * @return
	 */
	private static void setMidnight(Calendar cal)
	{
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
	}

	/**
	 * Vrati polnoc ktorou vybraty den zacinal x dni dozadu
	 *
	 * @param calendar
	 * @return calendar
	 */
	public static Calendar midnightDaysBefore(int daysBefore, Calendar cal)
	{
		int days = daysBefore - 1;
		setMidnight(cal);
		cal.add(Calendar.DAY_OF_MONTH, -days);
		return cal;
	}

	/**
	 *
	 * @return
	 */
	public static Calendar getFirstDateOfPreviousMonth()
	{
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 1);
		setMidnight(cal);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
		return cal;
	}

	/**
	 *
	 * @return
	 */
	public static Calendar getLastDateOfPreviousMonth()
	{
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 1);
		setMidnight(cal);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		return cal;
	}

	/**
	 *
	 * @return
	 */
	public static Calendar getFirstDateOfCurrentMonth()
	{
		Calendar cal = Calendar.getInstance();
		setMidnight(cal);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
		return cal;
	}

	/**
	 *
	 * @return
	 */
	public static Calendar getLastDateOfCurrentMonth()
	{
		Calendar cal = Calendar.getInstance();
		setMidnight(cal);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		return cal;
	}

	/**
	 *
	 * @return
	 */
	public static Calendar getFirstDateOfNextMonth()
	{
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) + 1);
		setMidnight(cal);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
		return cal;
	}

	/**
	 *
	 * @return
	 */
	public static Calendar getLastDateOfNextMonth()
	{
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) + 1);
		setMidnight(cal);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		return cal;
	}

	/**
	 * Vrati hodinu dnesneho dna
	 *
	 * @param calendar
	 * @return calendar
	 */
	public static Calendar hourOfDay(Calendar cal, int hour)
	{
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}

	/**
	 * Konvertuje java.util.Date do java.util.Calendar
	 *
	 * @param date
	 * @return calendar
	 */
	public static Calendar dateToCalendar(Date date)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal;
	}

	/**
	 * Konvertuje java.sql.Timestamp do java.util.Calendar
	 *
	 * @param timestamp
	 * @return
	 */
	public static Calendar timestampToCalendar(Timestamp timestamp)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp.getTime());
		return cal;
	}

	/**
	 * Vrati x dni dopredu, pri zapornom znamienku dni dozadu
	 *
	 * @param calendar
	 * @return calendar
	 */
	public static Calendar getDaysAfter(int daysAfter, Calendar cal)
	{
		cal.add(Calendar.DATE, daysAfter);
		return cal;
	}

	/**
	 * Vrati list dni medzi dateFrom(vcitane) a dateTo(vcitane)
	 *
	 * @param dateFrom
	 * @param dateTo
	 * @return
	 */
	public static List<Date> getDatesBetweenInclude(final Date dateFrom, final Date dateTo)
	{
		List<Date> dates = new ArrayList<>();
		Calendar c1 = new GregorianCalendar();
		c1.setTime(dateFrom);
		Calendar c2 = new GregorianCalendar();
		c2.setTime(dateTo);
		//int a = c1.get(Calendar.DATE);
		//int b = c2.get(Calendar.DATE);
		dates.add(dateFrom);
		while ((c1.get(Calendar.YEAR) != c2.get(Calendar.YEAR)) || (c1.get(Calendar.MONTH) != c2.get(Calendar.MONTH))
					|| (c1.get(Calendar.DATE) != c2.get(Calendar.DATE)))
		{
			c1.add(Calendar.DATE, 1);
			dates.add(new Date(c1.getTimeInMillis()));
		}
		return dates;
	}

	/**
	 * Vrati list dni medzi dateFrom(vcitane) a dateTo(vcitane) vystup String
	 *
	 * skratkaDna.dd.MM.yyyy.skratkaMesiaca
	 *
	 * @param dateFrom
	 * @param dateTo
	 * @return
	 */
	public static List<String> getDatesBetweenIncludeString(Date dateFrom, Date dateTo)
	{
		List<Date> datumListDate = DateTools.getDatesBetweenInclude(dateFrom, dateTo);
		List<String> datumListString = new ArrayList<>();
		for (int i = 0; i < datumListDate.size(); i++)
		{
			Date datum = datumListDate.get(i);
			datumListString.add(getShortDayName(datum, new Locale("sk")) + "." + Tools.formatDate(datum) + "."
						+ getShortMonthName(datum, new Locale("sk")));
		}
		return datumListString;
	}

	/**
	 * Vrati list dni medzi dateFrom(vcitane) a dateTo(vcitane) vystup String
	 *
	 * skratkaDna.dd.MM.yyyy.skratkaMesiaca-poradieDna/poradieTyzdna
	 *
	 * @param dateFrom
	 * @param dateTo
	 * @return
	 */
	public List<String> getDatesBetweenIncludeOrderString(Date dateFrom, Date dateTo)
	{
		List<Date> datumListDate = DateTools.getDatesBetweenInclude(dateFrom, dateTo);
		Calendar calFrom = dateToCalendar(dateFrom);
		int poradieTyzdnaFrom = calFrom.get(Calendar.WEEK_OF_YEAR);
		List<String> datumListString = new ArrayList<>();
		for (int i = 0; i < datumListDate.size(); i++)
		{
			Date datum = datumListDate.get(i);
			String datumSoSkratkami = getShortDayName(datum, new Locale("sk")) + "." + Tools.formatDate(datum) + "."
						+ getShortMonthName(datum, new Locale("sk"));
			Calendar cal = dateToCalendar(datum);
			int poradieDnaVTyzdni = cal.get(Calendar.DAY_OF_WEEK);
			if (poradieDnaVTyzdni == 1)
			{
				poradieDnaVTyzdni = 7;
			}
			else
			{
				poradieDnaVTyzdni = poradieDnaVTyzdni - 1;
			}
			Logger.debug(DateTools.class, "datum: " + datumSoSkratkami + "poradieTyzdna: " + poradieTyzdna);
			if (cal.get(Calendar.WEEK_OF_YEAR) == 1 && hasRun == false)
			{
				poradieTyzdnaFrom = poradieTyzdna;
				hasRun = true;
			}
			if (hasRun == true)
			{
				poradieTyzdna = cal.get(Calendar.WEEK_OF_YEAR) + poradieTyzdnaFrom + 1;
			}
			else
			{
				poradieTyzdna = cal.get(Calendar.WEEK_OF_YEAR) - poradieTyzdnaFrom + 1;
			}
			Logger.debug(DateTools.class, "poradieTyzdna: " + poradieTyzdna + " poradieTyzdnaFrom: " + poradieTyzdnaFrom);
			String poradieDnaATyzdna = "" + poradieDnaVTyzdni + "/" + poradieTyzdna;
			datumListString.add(datumSoSkratkami + "-" + poradieDnaATyzdna);
		}
		return datumListString;
	}

	/**
	 * Vrati skratene nazvy mesiacov pre dany Locale
	 *
	 * @param datum
	 * @param locale
	 * @return
	 */
	public static String getShortMonthName(Date datum, Locale locale)
	{
		Calendar c = dateToCalendar(datum);
		int month = c.get(Calendar.MONTH);
		DateFormatSymbols symbols = new DateFormatSymbols(locale);
		String[] monthNames = symbols.getShortMonths();
		return monthNames[month];
	}

	/**
	 * Vrati nazvy mesiacov pre dany Locale
	 *
	 * @param datum
	 * @param locale
	 * @return
	 */
	public static String getMonthName(Date datum, Locale locale)
	{
		Calendar c = dateToCalendar(datum);
		int month = c.get(Calendar.MONTH);
		DateFormatSymbols symbols = new DateFormatSymbols(locale);
		String[] monthNames = symbols.getMonths();
		return monthNames[month - 1];
	}

	/**
	 * Vrati skratene nazvy dni pre dany Locale
	 *
	 * @param day
	 * @param locale
	 * @return
	 */
	public static String getShortDayName(Date datum, Locale locale)
	{
		Calendar c = dateToCalendar(datum);
		int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
		DateFormatSymbols symbols = new DateFormatSymbols(locale);
		String[] dayNames = symbols.getShortWeekdays();
		return dayNames[dayOfWeek];
	}

	/**
	 * Vrati nazvy dni pre dany Locale
	 *
	 * @param day
	 * @param locale
	 * @return
	 */
	public static String getDayName(Date datum, Locale locale)
	{
		Calendar c = dateToCalendar(datum);
		int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
		DateFormatSymbols symbols = new DateFormatSymbols(locale);
		String[] dayNames = symbols.getWeekdays();
		return dayNames[dayOfWeek];
	}

	public static String[] getShortDayNames(Locale locale)
	{
		DateFormatSymbols symbols = new DateFormatSymbols(locale);
		String[] dayNames = symbols.getShortWeekdays();
		return dayNames;
	}

	/**
	 * Vrati cas v milisekundach bez sekund a milisekund
	 *
	 * @param timeMillisWithSeconds
	 * @return
	 */
	public long getTimeLongWithoutSeconds(long timeMillisWithSeconds){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timeMillisWithSeconds);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		long timeMillisWithoutSeconds = cal.getTimeInMillis();
		return timeMillisWithoutSeconds;
	}

    /**
     * Odcita dni z datumu
     * @param date
     * @param days
     * @return
     */
	public static Date substractDays(Date date, int days) {
	    if (date == null) {
	        return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, days * -1);
        return calendar.getTime();
    }

    /**
     * Prida dni k datumu
     * @param date
     * @param days
     * @return
     */
    public static Date addDays(Date date, int days) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, days);
        return calendar.getTime();
    }

	/**
	 * Validates the range between two dates.
	 *
	 * @param dateFrom the starting date of the range
	 * @param dateTo the ending date of the range
	 * @param canBeInPast a flag indicating whether the range can include past dates
	 * @return an integer value representing the validation result:
	 *         <p> -1 if either dateFrom or dateTo is null,
	 *         <p> 1 if the range is in the past and canBeInPast is false,
	 *         <p> 2 if dateFrom is after dateTo,
	 *         <p> 0 if the range is valid
	 */
	public static int validateRange(Date dateFrom, Date dateTo, boolean canBeInPast) {
		//Check for wrong values
		if(dateFrom == null || dateTo == null) return -1;
		return validateRange(dateFrom.getTime(), dateTo.getTime(), canBeInPast);
	}

	/**
	 * Validates the range between two dates.
	 *
	 * @param dateFrom the starting date of the range
	 * @param dateTo the ending date of the range
	 * @param canBeInPast a flag indicating whether the range can be in the past
	 * @return an integer value representing the validation result:
	 *         <p> -1 if either dateFrom or dateTo is negative,
	 *         <p> 1 if the range is in the past and canBeInPast is false,
	 *         <p> 2 if dateFrom is after dateTo,
	 *         <p> 0 if the range is valid
	 */
	public static int validateRange(long dateFrom, long dateTo, boolean canBeInPast) {
		//Check for wrong values
		if(dateFrom < 0 || dateTo < 0) return -1;

		//Validate id range is in past
		if(canBeInPast == false && (dateFrom < System.currentTimeMillis() || dateTo < System.currentTimeMillis()) ) {
			return 1;
		}

		//Validate if dateFrom is before dateTo
		if(dateFrom > dateTo) {
			return 2;
		}

		return 0;
	}
}