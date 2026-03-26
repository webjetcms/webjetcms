package sk.iway.iwcm.components.weather;

import java.util.Date;

/**
 * DayForecastBean2.java
 *
 * Class DayForecastBean is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2018
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      8.8.2018 13:56
 * modified     8.8.2018 13:56
 */

public class DayForecastBean {

    private Date date;
    private Integer minTemperature = Integer.MAX_VALUE;
    private Integer maxTemperature = Integer.MIN_VALUE;
    private String symbol;
    private int symbolId;

    public Integer getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(Integer minTemperature) {
        this.minTemperature = minTemperature;
    }

    public Integer getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(Integer maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getSymbolId() {
        return symbolId;
    }

    public void setSymbolId(int symbolId) {
        this.symbolId = symbolId;
    }
}
