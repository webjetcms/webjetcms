package sk.iway.iwcm.components.restaurant_menu.rest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.restaurant_menu.jpa.AlergenBean;
import sk.iway.iwcm.components.restaurant_menu.jpa.RestaurantMenuEditorFields;
import sk.iway.iwcm.components.restaurant_menu.jpa.RestaurantMenuEntity;
import sk.iway.iwcm.components.restaurant_menu.jpa.RestaurantMenuRepository;
import sk.iway.iwcm.i18n.Prop;

public class RestaurantMenuService {

    /**
     * Set time part of calendar to 00:00:00.000
     * @param cal
     */
    private static void nullTimePart(Calendar cal) {
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    /**
     * NULL - return actual date,
     * String - parse to date,
     * Date - keep date,
     * else - return actual date,
     *
     * ALL DATE ARE WITHOUT TIME PART -> just like in DB
     * @param menuDay
     * @return
     */
    public static Date getMenuDate(Object menuDay) {
        Calendar cal = Calendar.getInstance();

        if(menuDay == null) {
            cal.setTime(new Date());
        } else if(menuDay instanceof String) {
            if(Tools.isEmpty((String) menuDay)) {
                cal.setTime( new Date() );
            } else cal.setTimeInMillis( Long.parseLong((String) menuDay) );
        } else if(menuDay instanceof Date) {
            cal.setTime((Date) menuDay);
        } else { cal.setTime(new Date()); }

        nullTimePart(cal);
        return cal.getTime();
    }

    /**
     * Get list of all alergens as List<AlergenBean>
     * @param request
     * @return
     */
    public static List<AlergenBean> getAlergenBeans(HttpServletRequest request) {
        List<AlergenBean> alergens = new ArrayList<>();
        Prop prop = Prop.getInstance(request);

        for(int i = 1; i <= Constants.getInt("restaurantMenu.alergensCount"); i++) {
            alergens.add(
                new AlergenBean(i, prop.getText("components.restaurant_menu.alergen" + i))
            );
        }

        return alergens;
    }

    /**
     * Sort menu by dayDate then by mealCathegory then by priority
     * @param menuEntities
     * @param addStyle - if true, add addRowClass (for FE in week mode)
     * @return
     */
    public static List<RestaurantMenuEntity> sortMenu(List<RestaurantMenuEntity> menuEntities, boolean addStyle) {
        if (menuEntities == null || menuEntities.isEmpty()) return menuEntities;

        //Perform sort
        menuEntities = menuEntities.stream().sorted(
            Comparator.comparing(
                (RestaurantMenuEntity entity) -> entity.getDayDate()
            ).thenComparing(
                (RestaurantMenuEntity entity) -> entity.getEditorFields().getMealCathegory()
            ).thenComparing(
                (RestaurantMenuEntity entity) -> entity.getPriority()
            )
        ).collect(Collectors.toList());

        //Add style's -> style is changed when we came from one day to another day menuStyle_0 / menuStyle_1
        if(addStyle) {
            String dayOfWeek = null;
            int type = 0;
            for(RestaurantMenuEntity menu : menuEntities) {
                if(dayOfWeek == null)
                    dayOfWeek = menu.getEditorFields().getDayOfWeek();

                if(dayOfWeek.equals(menu.getEditorFields().getDayOfWeek())) {
                    menu.getEditorFields().addRowClass("menuStyle_" + type);
                } else {
                    dayOfWeek = menu.getEditorFields().getDayOfWeek();
                    type = type == 0 ? 1 : 0;
                    menu.getEditorFields().addRowClass("menuStyle_" + type);
                }
            }
        }

        return menuEntities;
    }

    /**
     * Process params and prepare date range based on menuType
     * @param params
     */
    public static void processParams(Map<String, String> params) {
        String dateRange = null;
        String menuType = null;

        //Get values from params entry
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if(entry.getKey().equalsIgnoreCase("searchDayDate")) {
                dateRange = entry.getValue();
            } else if(entry.getKey().equalsIgnoreCase("menuType")) {
                menuType = entry.getValue();
            }
        }

        //Cant work with empty date range
        if(dateRange == null) return;

        //Because records are set in DB without time part, it's enough to add 1 minute, to get records for this date
        //Prepare range gonna have 7 days (week menuType) or 1 day
        dateRange = prepareDateRange(dateRange, menuType, true); //-> with prefix because its for params map

        params.put("searchDayDate", dateRange);
    }

    /**
     * Prepare date range based on menuType.
     *
     * menuType = null or days -> 1 day range,
     * menuType = week -> 7 days range,
     *
     * Range is allways + 1m (minute) -> because records are set in DB without time part
     * @param dateRange - can be with or without prefix ":daterange" MUST BE ALLWAY only FROM value
     * @param menuType
     * @param withPrefix - true add prefix ":daterange" (its for params map)
     * @return - return range FROM - TO based on input value and menuType
     */
    private static String prepareDateRange(String dateRange, String menuType, boolean withPrefix) {
        //Can contain daterange: prefix + value FROM (only FROM)
        dateRange = dateRange.replaceFirst("daterange:", "");

        //Prepare calendar instance and set time to 00:00:00.000
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(Long.parseLong( dateRange ));
        nullTimePart(cal);

        //If type is not set or is set to days -> 1 day range
        if(menuType == null || menuType.equalsIgnoreCase("days")) {
            //Just one day -> we must do a range with just 1 minute
            cal.add(Calendar.MINUTE, 1);
            dateRange += "-" + cal.getTimeInMillis();
        } else {
            //Week range based allways from MONDAY to SUNDAY (allways 7 days) -> doe not matter, what day of weeek was selected
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

            //Preapre ranged based on day of week
            if(dayOfWeek == 2) { //Monday
                dateRange = cal.getTimeInMillis() + "-";
                cal.add( Calendar.DAY_OF_YEAR, 6);
                cal.add(Calendar.MINUTE, 1);  //Must be add
                dateRange += cal.getTimeInMillis();
            } else if(dayOfWeek == 1) { //Sunday
                cal.add(Calendar.DAY_OF_YEAR, -6);
                dateRange = cal.getTimeInMillis() + "-";
                cal.add(Calendar.DAY_OF_YEAR, 6);
                cal.add(Calendar.MINUTE, 1);  //Must be add
                dateRange += cal.getTimeInMillis();
            } else { //Else
                int diff = dayOfWeek - 2;
                cal.add(Calendar.DAY_OF_YEAR, -diff);
                dateRange = cal.getTimeInMillis() + "-";
                cal.add(Calendar.DAY_OF_YEAR, 6);
                cal.add(Calendar.MINUTE, 1); //Must be add
                dateRange += cal.getTimeInMillis();
            }
        }

        if(withPrefix) return "daterange:" + dateRange;
        else return dateRange;
    }

    /**
     * Get list of RestaurantMenuEntity's (menu for one day) based on day. EditorFields is iniialized. Values in list are sorted.
     * @param day
     * @param prop
     * @return
     */
    public static List<RestaurantMenuEntity> getByDate(Date day, Prop prop) {
        //Get data for day
        RestaurantMenuRepository rmr = Tools.getSpringBean("restaurantMenuRepository", RestaurantMenuRepository.class);
        List<RestaurantMenuEntity> menuEntities = rmr.findAllByDayDateAndDomainId( getMenuDate(day), CloudToolsForCore.getDomainId() );

        //Inicialize editor fields
        for(RestaurantMenuEntity menuEntity : menuEntities) {
            RestaurantMenuEditorFields editorFields = new RestaurantMenuEditorFields();
            editorFields.fromRestaurantMenuEntity(menuEntity, prop);
        }

        //Return sorted list
        return sortMenu(menuEntities, false);
    }

    /**
     * Based on input datepickerWeek, get all records in week. EditorFields is iniialized. Values in list are sorted.
     * @param datepickerWeek - accepted formats: yyyy-Www, ww-yyyy (for back compatibility)
     * @param prop
     * @return
     */
    private static List<RestaurantMenuEntity> getWeekByDate(String datepickerWeek, Prop prop) {
        Calendar cal = Calendar.getInstance();

        //Prepare calendar instance based on week-year / year-week string
        if(Tools.isEmpty(datepickerWeek)) {
            //Default today
            cal.setTime(new Date());
        } else if(datepickerWeek.matches("[0-9]+-W[0-9]+")) {
            //Format yyyy-Www
            String yearWeekArr[] = datepickerWeek.split("-W");
            if(yearWeekArr.length == 2) {
                cal.set(Calendar.YEAR, Integer.parseInt(yearWeekArr[0]));
                cal.set(Calendar.WEEK_OF_YEAR, Integer.parseInt(yearWeekArr[1]));
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            } else cal.setTime(new Date());
        } else if(datepickerWeek.matches("[0-9]+-[0-9]+")) {
            //Format ww-yyyy
            String weekYearArr[] = datepickerWeek.split("-");
            if(weekYearArr.length == 2) {
                cal.set(Calendar.YEAR, Integer.parseInt(weekYearArr[1]));
                cal.set(Calendar.WEEK_OF_YEAR, Integer.parseInt(weekYearArr[0]));
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            } else cal.setTime(new Date());
        } else cal.setTime(new Date());

        //Prepare date range arr
        String dateRange = prepareDateRange("" + cal.getTimeInMillis(), "week", false);
        String dateRangeArr[] = dateRange.split("-");

        //Get all records in date range
        RestaurantMenuRepository rmr = Tools.getSpringBean("restaurantMenuRepository", RestaurantMenuRepository.class);
        List<RestaurantMenuEntity> weekMenus = rmr.findAllByDayDateBetweenAndDomainId(
            new Date(Long.parseLong(dateRangeArr[0])),
            new Date(Long.parseLong(dateRangeArr[1])),
            CloudToolsForCore.getDomainId()
        );

        //Inicialize editor fields
        for(RestaurantMenuEntity menuEntity : weekMenus) {
            RestaurantMenuEditorFields editorFields = new RestaurantMenuEditorFields();
            editorFields.fromRestaurantMenuEntity(menuEntity, prop);
        }

        //Return sorted list
        return sortMenu(weekMenus, false);
    }

    /**
     *  Based on input datepickerWeek, get all records in week. EditorFields is iniialized. Values in list are sorted.
     *
     * List of entities is grouped by day. Each day is one list in list.
     *
     * @param datepickerWeek - accepted formats: yyyy-Www, ww-yyyy (for back compatibility)
     * @param prop
     * @return
     */
    public static List<List<RestaurantMenuEntity>> getParsedWeekByDate(String datepickerWeek, Prop prop) {
        //Get data for week taht are sorted and editor fields are inicialized
        List<RestaurantMenuEntity> all = getWeekByDate(datepickerWeek, prop);
        if(all == null) return new ArrayList<>();

        //Group by day (meke list of lists)
        List<List<RestaurantMenuEntity>> week = new ArrayList<>();
        int actualIndex = 0;
        Date actualDate = null;

        for(RestaurantMenuEntity entity : all) {
            if(actualDate == null) {
                actualDate = entity.getDayDate();
                week.add(new ArrayList<>());
            }

            if(actualDate.compareTo(entity.getDayDate()) == 0) {
                week.get(actualIndex).add(entity);
            } else {
                actualDate = entity.getDayDate();
                actualIndex++;
                week.add(new ArrayList<>());
                week.get(actualIndex).add(entity);
            }
        }
        return week;
    }

    /***************************** FE METHODS *************************************/

    /**
     * Prepare value for week datpicekr. Accepted formats: yyyy-Www, ww-yyyy (for back compatibility).
     *
     * If value is null/empty -> return actual week,
     * @param value
     * @return Allways return yyyy-Www
     */
    public static String getWeekDateValue(String value) {
        if(Tools.isEmpty(value)) return getDefaulWeekDateValue();

        if(value.matches("[0-9]+-[0-9]+")) {
            //FROM ww-yyyy (50-2023) to yyyy-Www (2023-W50)
            String[] valueArr = value.split("-");
            if(valueArr.length == 2)
                return valueArr[1] + "-W" + valueArr[0];
        } else return value;

        //Some problem, return default date
        return getDefaulWeekDateValue();
    }

    /**
     * Return actual week in format yyyy-Www
     * @return
     */
    private static String getDefaulWeekDateValue() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());

        return cal.get(Calendar.YEAR) + "-W" + cal.get(Calendar.WEEK_OF_YEAR);
    }
}
