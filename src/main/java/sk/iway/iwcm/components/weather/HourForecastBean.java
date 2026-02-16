package sk.iway.iwcm.components.weather;

import java.util.Calendar;
import java.util.Date;

/**
 * HourForecastBean.java
 *
 * Class HourForecastBean is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2018
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      8.8.2018 13:13
 * modified     8.8.2018 13:13
 */

public class HourForecastBean {

    private Integer temperature;
    private String symbol;
    private int symbolId;
    private Date dateFrom;
    private Date dateTo;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

    public Integer getTemperature() {
        return temperature;
    }

    public void setTemperature(Integer temperature) {
        this.temperature = temperature;
    }

    public int getSymbolId() {
        return symbolId;
    }

    public void setSymbolId(int symbolId) {
        this.symbolId = symbolId;
    }

    public int isNight() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateFrom);
        if (cal.get(Calendar.HOUR_OF_DAY) >= 18 ||  cal.get(Calendar.HOUR_OF_DAY) <= 6)
            return 1;
        return 0;
    }
}
