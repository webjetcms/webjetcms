package sk.iway.iwcm.components.weather;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.components.enumerations.EnumerationDataDB;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.database.SimpleQuery;

/**
 * WeatherForecast.java
 *
 * Class WeatherForecast is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2018
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      15.8.2018 13:12
 * modified     15.8.2018 13:12
 */

public class WeatherForecast {

    private List<DayForecastBean> dayForecastBeanList;
    private List<HourForecastBean> hourForecastBeanList;    // zoradene 2-hodinove predpovede pocasia (na dnesok a zajtrasok) kedze o polnoci vyprsi tato predpoved z cache
    private EnumerationDataBean city;

    private static final String WEATHER_CACHE_PREFIX = "WEATHER_";
    private static final String HOUR = "HOUR_";

    /**
     * Konstruktor, pocas ktoreho sa inicializuje objekt z cache pamate alebo nanovo z xml suboru ktory nasledne ulozi do cache.
     * @param id dat ciselnika ktore reprezentuju mesto s GPS suradnicami
     */
    public WeatherForecast(int id) {
        // ak nenajde ziaden setting pouzi toto mesto
        EnumerationDataBean cityBean = EnumerationDataDB.getInstance().getById(id);
        if (cityBean == null) {
            cityBean = EnumerationDataDB.getInstance().getById(new SimpleQuery().forInt("SELECT enumeration_data_id FROM enumeration_data WHERE string1 LIKE 'Bratislava'"));
        }

        initialize(cityBean);
    }

    public WeatherForecast(String cityName, BigDecimal lat, BigDecimal lon)
    {
        EnumerationDataBean cityBean = new EnumerationDataBean();
        cityBean.setString1(cityName);
        cityBean.setDecimal1(lat);
        cityBean.setDecimal2(lon);

        initialize(cityBean);
    }

    @SuppressWarnings("unchecked")
    private void initialize(EnumerationDataBean city)
    {
        this.city = city;
        if (this.city == null) return;

        Cache c = Cache.getInstance();

        BigDecimal lat = city.getDecimal1();
        BigDecimal lon = city.getDecimal2();

        String CACHE_KEY_SUFFIX = lat.toString()+","+lon.toString();

        this.dayForecastBeanList = (List<DayForecastBean>)c.getObject(WEATHER_CACHE_PREFIX + CACHE_KEY_SUFFIX);
        this.hourForecastBeanList = (List<HourForecastBean>)c.getObject(WEATHER_CACHE_PREFIX + HOUR + CACHE_KEY_SUFFIX);

        if (this.dayForecastBeanList == null || this.hourForecastBeanList == null) {
            ImportWeather importWeather = new ImportWeather();
            this.dayForecastBeanList = importWeather.readWeather(city.getDecimal1(), city.getDecimal2());// lat, lon
            c.setObjectByExpiry(WEATHER_CACHE_PREFIX + CACHE_KEY_SUFFIX, dayForecastBeanList, dayForecastBeanList.get(1).getDate().getTime(),false);
            this.hourForecastBeanList = importWeather.getHourForecastBeanList();
            c.setObjectByExpiry(WEATHER_CACHE_PREFIX + HOUR + CACHE_KEY_SUFFIX, hourForecastBeanList, dayForecastBeanList.get(1).getDate().getTime(),false);
        }
    }


    public List<DayForecastBean> getDayForecastBeanList() {
        return dayForecastBeanList;
    }

    public List<HourForecastBean> getHourForecastBeanList() {
        return hourForecastBeanList;
    }

    public EnumerationDataBean getCity() {
        return city;
    }

    public void setCity(EnumerationDataBean city) {
        this.city = city;
    }

    /**
     * Vrati 8 dvojhodinovych predpovedi podla aktualneho casu.
     * @return
     */
    public List<HourForecastBean> getCurrentHourForecast() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY,3);
        HourForecastBean start = null;
        for (HourForecastBean item : this.hourForecastBeanList) {
            if (cal.getTime().after(item.getDateFrom())) { // cas sedi
                start = item;
                break;
            }
        }
        int i = this.hourForecastBeanList.indexOf(start);
        if (i+8<this.hourForecastBeanList.size())
            return this.hourForecastBeanList.subList(i, i+8);
        else
            return this.hourForecastBeanList.subList(i, this.hourForecastBeanList.size()-1);
    }
}
