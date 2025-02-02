package sk.iway.iwcm.stat.rest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.stat.ChartType;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.FilterHeaderDto;
import sk.iway.iwcm.stat.StatTableDB;
import sk.iway.iwcm.stat.jpa.SearchEnginesDTO;

/**
 * Main goal of this Service is help with extended filters that all Stat section pages contains and to reduce dudplicity codes.
 */
public class StatService {

    private static final int MAX_ROWS = 100;
    private static final int MAX_BAR_COLUMNS = 10;

    private StatService() {
        //it's just tools class
    }

    /**
     * Function will set default range into dateRangeArr. Default range is 1 month.
     * @param dateRangeArr - [0] dateFrom, [1] dateTo
     * @param statType - type of stat, can be days, weeks, months
     */
    private static void setDefaultRange(Date [] dateRangeArr, String statType) {
        Calendar cal = Calendar.getInstance();

        //If date to is not set, set it
        if(dateRangeArr[1] == null) {
            cal.setTime(new Date());
            dateRangeArr[1] = cal.getTime(); //To NOW
        } else {
            //Use allready preapred dateTo
            cal.setTime(dateRangeArr[1]);
        }

        if(Tools.isNotEmpty(statType) && "months".equals(statType)) {
            //Stat type is set to months, set default range 6 month
            cal.add(Calendar.MONTH, -6); //From 6 month's ago
        } else {
            //Stat type is not set OR its days/weeks, set default range 1 month
            cal.add(Calendar.MONTH, -1); //From 1 month ago
        }

        dateRangeArr[0] = cal.getTime();
    }

    public static Date[] processDateRangeString(String stringRange) {
        return processDateRangeString(stringRange, null);
    }

    /**
     * Function will handle String on input and retun array of 2 Date values. This values represent date range from-to.
     * In case of input that contain "daterange:" prefix (added by date picker), function can handle this prefix.
     * If input is empty 1 month range is returned.
     * If input contain two values in milliseconds separeted by "-", this values will be used for date range.
     * If input contain only one value without "-", this value will represent dateFrom, dateTo will be computed and range
     * will be 1 month.
     * If input contain only one value that start with "-", this represent dateTo, dateFrom will be computed and range
     * will be 1 month.
     * Any other input will be clasified as invalid and default 1 month range will be returned.
     * @param stringRange  string of milliseconds represent date
     * @return array with 2 Date values
     */
    public static Date[] processDateRangeString(String stringRange, String statType) {
        //Represent dateFrom, dateTo
        Date [] dateRangeArr = {null, null};

        //Remove prefix "daterange:" is is there
        if(stringRange != null)
            stringRange =  stringRange.replaceFirst("^daterange:", "");
        else stringRange = "";

        String stringTo;
        String stringFrom;
        Calendar cal = Calendar.getInstance();

        if(stringRange.contains("-")) {
            if(stringRange.startsWith("-")) {
                //We have only set dateTo
                stringTo = stringRange.split("-")[1];

                //First check if its valid string
                if(Tools.isEmpty(stringTo)) {
                    //String is no valid, set default month range
                    setDefaultRange(dateRangeArr, statType);
                } else {
                    //We have set only dateTo
                    cal.setTimeInMillis(Long.parseLong(stringTo));
                    dateRangeArr[1] = cal.getTime();

                    //dateFrom set at default range
                    setDefaultRange(dateRangeArr, statType);
                }
            } else {
                //We have set both dateFrom and dateTo
                stringFrom = stringRange.split("-")[0];
                cal.setTimeInMillis(Long.parseLong(stringFrom));
                dateRangeArr[0] = cal.getTime();

                stringTo = stringRange.split("-")[1];
                cal.setTimeInMillis(Long.parseLong(stringTo));
                dateRangeArr[1] = cal.getTime();
            }
        } else {
            //We have set only dateFrom
            stringFrom = stringRange;

            //First check if its valid string
            if(Tools.isEmpty(stringFrom)) {
                //String is no valid, set default range
                setDefaultRange(dateRangeArr, statType);
            } else {
                //Range is dateFrom - now
                cal.setTimeInMillis(Long.parseLong(stringFrom));
                dateRangeArr[0] = cal.getTime();
                dateRangeArr[1] = new Date(); //To actual date
            }
        }

        //align dateRangeArr[0] to start of day
        if(dateRangeArr[0] != null) {
            cal.setTime(dateRangeArr[0]);
            if (cal.get(Calendar.HOUR_OF_DAY)==0 && cal.get(Calendar.MINUTE)==0 && cal.get(Calendar.SECOND)==0) {
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
            }

            dateRangeArr[0] = cal.getTime();
        }

        //align dateRangeArr[1] to end of day
        if(dateRangeArr[1] != null) {
            cal.setTime(dateRangeArr[1]);
            if (cal.get(Calendar.HOUR_OF_DAY)==0 && cal.get(Calendar.MINUTE)==0 && cal.get(Calendar.SECOND)==0) {
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
            }

            dateRangeArr[1] = cal.getTime();
        }

        return dateRangeArr;
    }

    /**
     * Function is case-insensitive, and if input string value is equal to one of ChartType enum, this enum value
     * will be returned. If input is null/empty/or does not match any of ChartType values, function will return
     * NOT_CHART enum value as default value.
     * @param chartType String value that represent one of ChartTzpe enum values
     * @return ChartType enum value
     */
    public static ChartType stringToChartTypeEnum(String chartType) {
        if(chartType == null || chartType.equalsIgnoreCase("notChart"))
            return ChartType.NOT_CHART;
        else if("pie".equalsIgnoreCase(chartType))
            return ChartType.PIE;
        else if("line".equalsIgnoreCase(chartType))
            return ChartType.LINE;
        else if("bar".equalsIgnoreCase(chartType))
            return ChartType.BAR;
        else return ChartType.NOT_CHART;
    }

    /**
     * Function handle Map of parameters returned by Overrided searchItem and use them for creating new
     * FilterHeaderDto variable (where this handled params are set). If any param is not found in map, default value will be set.
     *  Default values are defined by FilterHeaderDto itself.
     * Because not every class has same name for date range value (mostly refered as dayDate), there is option use input parameter
     * specialDateName and set what param we want find in map, that repsent dayDate. If specialDateName isnt set, function will
     * use default param name dayDate.
     * @param params map of params return by overrided function searchItem
     * @param specialDateName name of param that is in map and represent date range param dayDate
     * @return FilterHeaderDto variable that contain handled params
     */
    public static FilterHeaderDto processMapToStatFilter(Map<String, String> params, String specialDateName) {
        FilterHeaderDto filter = new FilterHeaderDto();
        String stringRange = "";

        //Check if we want to look for special named date variable
        String searchDate = "";
        if(specialDateName != null) {
            if(!specialDateName.startsWith("search")) searchDate = "search" + specialDateName;
            else searchDate = specialDateName;
        } else {
            searchDate = "searchDayDate";
        }

        //Get params from map
        for (Map.Entry<String, String> entry : params.entrySet()) {

            //In specific case, FE returned value as String "undefined"
            String value = entry.getValue();
            if("undefined".equals(value)) value = "";

            if(entry.getKey().equalsIgnoreCase(searchDate)) {
                stringRange = value;
            } else if("searchRootDir".equalsIgnoreCase(entry.getKey())) {
                String rootGroupIdString = value;
                filter.setRootGroupId(Tools.getIntValue(rootGroupIdString, -1));
            } else if("searchFilterBotsOut".equalsIgnoreCase(entry.getKey())) {
                String filterBotsOutString = value;
                filter.setFilterBotsOut(Boolean.parseBoolean(filterBotsOutString));
            }  else if("chartType".equalsIgnoreCase(entry.getKey())) {
                String chartTypString = value;
                filter.setChartType(stringToChartTypeEnum(chartTypString));
            } else if("searchUrl".equalsIgnoreCase(entry.getKey())) {
                filter.setUrl(value);
            } else if("searchEngine".equalsIgnoreCase(entry.getKey())) {
                filter.setSearchEngineName(value);
            } else if("searchWebPage".equalsIgnoreCase(entry.getKey())) {
                String webPageIdString = value;
                filter.setWebPageId(Tools.getIntValue(webPageIdString, -1));
            } else if("statType".equalsIgnoreCase(entry.getKey())) {
                filter.setStatType( value );
            }
        }

        //Process dateString
        Date[] dateRange = processDateRangeString(stringRange, filter.getStatType());

        //Set date range into filter
        filter.setDateFrom(dateRange[0]);
        filter.setDateTo(dateRange[1]);

        return filter;
    }

    /**
     * Function handle parameters from request and use them for creating new FilterHeaderDto variable (where thi handled params are set).
     * If any param is not found in map, default value will be set. Default values are defined by FilterHeaderDto itself.
     * Because not every class has same name for date range value (mostly refered as dayDate), there is option use input parameter
     * specialDateName and set what param we want find in map, that repsent dayDate. If specialDateName isnt set, function will
     * use default param name dayDate.
     * @param request request that contain params to handle
     * @param specialDateName name of param that is in map and represent date range param dayDate
     * @return  FilterHeaderDto variable that contain handled params
     */
    public static FilterHeaderDto processRequestToStatFilter(HttpServletRequest request, String specialDateName) {
        FilterHeaderDto filter = new FilterHeaderDto();

        //Check if we want to look for special named date variable
        String searchDate = "";
        if(specialDateName != null)
            searchDate = specialDateName;
        else {
            if (Tools.getStringValue(request.getParameter("searchdayDate"), null) != null) searchDate = "searchdayDate";
            else if (Tools.getStringValue(request.getParameter("searchDayDate"), null) != null) searchDate = "searchDayDate";
            else searchDate = "dayDate";
        }

        //Process dateString
        Date[] dateRange = processDateRangeString(request.getParameter(searchDate));

        //Set date range into filter
        filter.setDateFrom(dateRange[0]);
        filter.setDateTo(dateRange[1]);

        //Set rootGroupId with query
        filter.setRootGroupId(Tools.getIntValue(request.getParameter("searchRootDir"), -1));
        if(filter.getRootGroupId() == -1)
            filter.setRootGroupId(Tools.getIntValue(request.getParameter("rootDir"), -1));

        //Set filter bots out
        filter.setFilterBotsOut(Tools.getBooleanValue(request.getParameter("searchFilterBotsOut"), false));
        if(Boolean.FALSE.equals(filter.getFilterBotsOut()))
            filter.setFilterBotsOut(Tools.getBooleanValue(request.getParameter("filterBotsOut"), false));

        //Set chart type
        String chartType = Tools.getStringValue(request.getParameter("searchChartType"), "notChart");
        if("notChart".equals(chartType)) chartType = Tools.getStringValue(request.getParameter("chartType"), "notChart");

        filter.setChartType(stringToChartTypeEnum(chartType));

        //Set url
        filter.setUrl(Tools.getStringValue(request.getParameter("searchUrl"), ""));
        if("".equals( filter.getUrl() ))
            filter.setUrl(Tools.getStringValue(request.getParameter("url"), ""));
        if("".equals( filter.getUrl() ))
            filter.setUrl(Tools.getStringValue(request.getParameter("searchurl"), ""));

        //Set search engine name
        filter.setSearchEngineName(Tools.getStringValue(request.getParameter("searchEngine"), ""));
        if("".equals( filter.getSearchEngineName() ))
            filter.setSearchEngineName(Tools.getStringValue(request.getParameter("engine"), ""));

        //Set web page id
        filter.setWebPageId(Tools.getIntValue(request.getParameter("searchWebPage"), -1));
        if(filter.getWebPageId() == -1)
            filter.setWebPageId(Tools.getIntValue(request.getParameter("webPage"), -1));

        //Set stat type
        filter.setStatType( Tools.getStringValue(request.getParameter("searchStatType"), "days") );
        if("days".equals( filter.getStatType() ))
            filter.setStatType( Tools.getStringValue(request.getParameter("statType"), "days") );

        return filter;
    }

    public static List<SearchEnginesDTO> getSearchEnginesTableData(FilterHeaderDto filter) {
        List<Column> columns;

        updateFilter(filter);

        if(filter.getChartType() == ChartType.PIE)
            columns = StatTableDB.getSearchEnginesCount(MAX_ROWS,  filter.getDateFrom(), filter.getDateTo(), filter.getRootGroupIdQuery());
        else
            columns = StatTableDB.getSearchEnginesQuery(MAX_ROWS, filter.getDateFrom(), filter.getDateTo(), filter.getRootGroupIdQuery());

        return columnsToItems(columns, filter.getChartType());
    }

    public static List<SearchEnginesDTO> getSearchEnginesPieChartData(Date from, Date to, String rootGroupIdQuery) {
        List<Column> columns = StatTableDB.getSearchEnginesCount(MAX_ROWS, from, to, rootGroupIdQuery);
        return columnsToItems(columns, ChartType.PIE);
    }

    private static FilterHeaderDto updateFilter(FilterHeaderDto filter) {
        if(!filter.getSearchEngineName().isEmpty())
            filter.setRootGroupIdQuery(filter.getRootGroupIdQuery() + " AND s.server='" + filter.getSearchEngineName() + "' ");

        if(filter.getWebPageId() != -1)
            filter.setRootGroupIdQuery(filter.getRootGroupIdQuery() + " AND s.doc_id = '" + filter.getWebPageId() + "' ");

        filter.setRootGroupIdQuery(filter.getRootGroupIdQuery());
        return filter;
    }

    private static List<SearchEnginesDTO> columnsToItems(List<Column> columns, ChartType chartType) {
        List<SearchEnginesDTO> items = new ArrayList<>();
        int order = 1;

        if(chartType == ChartType.PIE) {
            for(Column column : columns) {
                SearchEnginesDTO item = new SearchEnginesDTO();
                item.setOrder(order);
                item.setAccesCount(column.getIntColumn2());
                item.setServerName(column.getColumn1());
                items.add(item);
                order++;
            }
        } else {
            //Compute sum of all visits
            int sum = 0;
            for(Column column : columns) {
                sum += column.getIntColumn1();
            }

            if (sum > 0) {
                for(Column column : columns) {
                    SearchEnginesDTO item = new SearchEnginesDTO();
                    item.setOrder(order);
                    item.setQueryCount(column.getIntColumn1());
                    item.setQueryName(column.getColumn2());
                    item.setPercentage((double) item.getQueryCount() * 100 / sum);
                    items.add(item);

                    if(chartType == ChartType.BAR && order >= MAX_BAR_COLUMNS) break;

                    order++;
                }
            }
        }
        return items;
    }
}
