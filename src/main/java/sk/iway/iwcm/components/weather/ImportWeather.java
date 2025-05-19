package sk.iway.iwcm.components.weather;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.weather.met.no.generated.LocationType;
import sk.iway.iwcm.components.weather.met.no.generated.Temperature;
import sk.iway.iwcm.components.weather.met.no.generated.TimeType;
import sk.iway.iwcm.components.weather.met.no.generated.Weatherdata;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.*;

/**
 * ImportWeather.java
 *
 * Class ImportWeather is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2018
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      2.8.2018 10:50
 * modified     2.8.2018 10:50
 */

public class ImportWeather {

    private Map<Date,HourForecastBean> hourForecastBeanMap = new LinkedHashMap<>();

    /**
     * Nacita pocasie z api.met.no.
     * V XMLku su udaje o hodinovej a sesthodinovej predpovedi pocasia.
     * Pracujem iba s udajmi pre sesthodinovu predpoved kazdy den o 12:00 a 18:00 z ktorych ukladam max a min teplotu.
     *
     */
    public List<DayForecastBean> readWeather(BigDecimal lat, BigDecimal lon) {
        List<DayForecastBean> sorted = null;
        try {
                String source = Tools.downloadUrl(Tools.addParameterToUrlNoAmp(Tools.addParameterToUrlNoAmp(Constants.getString("weatherSourceApi"), "lat", String.valueOf(lat)), "lon", String.valueOf(lon)));
            if (null == source) {
                throw new FileNotFoundException("Nepodarilo sa nacitat xml na adrese ");
            }
            JAXBContext jaxbContext = JAXBContext.newInstance(Weatherdata.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            Weatherdata weatherdata = (Weatherdata) jaxbUnmarshaller.unmarshal(new ByteArrayInputStream(source.getBytes("UTF-8")));
            sorted = this.mapForecast(weatherdata.getProduct().get(0).getTime());
        } catch (Exception e) {
            Logger.println(ImportWeather.class, "CHYBA pri stahovani");
            sk.iway.iwcm.Logger.error(e);
        }
        return sorted;
    }

    @SuppressWarnings("rawtypes")
    public List<DayForecastBean> mapForecast(List<TimeType> timeTypeList) {
        Map<Date,DayForecastBean> dayForecastBeanMap = new LinkedHashMap<>();
        Calendar today = Calendar.getInstance();
        for (TimeType timeType : timeTypeList) {
            // start kontrola ci ma vlastne zmysel tento zaznam dako spracovavat
            Calendar dateFrom = Calendar.getInstance();
            dateFrom.setTime(timeType.getFrom().toGregorianCalendar(new GregorianCalendar().getTimeZone(), null, null).getTime());
            Calendar dateTo = Calendar.getInstance();
            dateTo.setTime(timeType.getTo().toGregorianCalendar(new GregorianCalendar().getTimeZone(), null, null).getTime());

            if (dateFrom.get(Calendar.HOUR) % 2 != 0)
                continue;
            if ((this.calculateHours(dateFrom.getTime(),dateTo.getTime())/24) > 1) {
                //ak predpoved na pozajtra alebo viac tak zober 6-hodinovu predpoved ktora uz obsahuje symbol a min/max templotu
                if (dateFrom.get(Calendar.HOUR) % 6 != 0 || dateFrom.get(Calendar.HOUR) == 0)
                    continue;
            } else {
                //ak predpoved je z dneska/zajtrajska zober 2-hodinovu predpoved (obsahuje symbol) ktoru neskor spojis s presnou predpovedou na urcitu hodinu (obsahuje teplotu)
                int diferenceInHours = this.calculateHours(dateFrom.getTime(), dateTo.getTime());
                if (diferenceInHours != 2 && diferenceInHours != 0 && diferenceInHours != 6) {
                    continue;
                }
            }
            // end kontrola

            Calendar midNight = Calendar.getInstance();
            midNight.setTime(timeType.getTo().toGregorianCalendar(new GregorianCalendar().getTimeZone(), null, null).getTime());
            midNight.set(Calendar.HOUR_OF_DAY, midNight.getActualMinimum(Calendar.HOUR_OF_DAY));
            midNight.set(Calendar.MINUTE, midNight.getActualMinimum(Calendar.MINUTE));
            midNight.set(Calendar.SECOND, midNight.getActualMinimum(Calendar.SECOND));
            midNight.set(Calendar.MILLISECOND, midNight.getActualMinimum(Calendar.MILLISECOND));

            DayForecastBean dayForecastBean = dayForecastBeanMap.get(midNight.getTime());
            if (dayForecastBean == null) {
                dayForecastBean = new DayForecastBean();
                dayForecastBean.setDate(midNight.getTime());
            }

            HourForecastBean hourForecastBean = this.hourForecastBeanMap.get(timeType.getTo().toGregorianCalendar(new GregorianCalendar().getTimeZone(), null, null).getTime());

            boolean add = false;
            if (hourForecastBean == null) {
                add = true;
                hourForecastBean = new HourForecastBean();
                hourForecastBean.setDateFrom(timeType.getFrom().toGregorianCalendar(new GregorianCalendar().getTimeZone(), null, null).getTime());
                hourForecastBean.setDateTo(timeType.getTo().toGregorianCalendar(new GregorianCalendar().getTimeZone(), null, null).getTime());
            }

            for (JAXBElement jaxbElemtimeTypeElement : timeType.getLocation().get(0).getGroundCoverAndPressureAndMaximumPrecipitation()) {
                switch(jaxbElemtimeTypeElement.getName().getLocalPart()) {
                    case "minTemperature" :
                        if (jaxbElemtimeTypeElement.getValue() instanceof Temperature) {
                            if (dayForecastBean.getMinTemperature() > (int)Math.round(((Temperature) jaxbElemtimeTypeElement.getValue()).getValue().doubleValue()) )
                                dayForecastBean.setMinTemperature((int)Math.round(((Temperature) jaxbElemtimeTypeElement.getValue()).getValue().doubleValue()));
                        }
                        break;
                    case "maxTemperature" :
                        if (jaxbElemtimeTypeElement.getValue() instanceof Temperature)
                            if (dayForecastBean.getMaxTemperature() < (int)Math.round(((Temperature) jaxbElemtimeTypeElement.getValue()).getValue().doubleValue()))
                                dayForecastBean.setMaxTemperature((int)Math.round(((Temperature) jaxbElemtimeTypeElement.getValue()).getValue().doubleValue()));
                        break;
                    case "symbol" :
                        if (jaxbElemtimeTypeElement.getValue() instanceof LocationType.Symbol) {
                            String symbol = ((LocationType.Symbol) jaxbElemtimeTypeElement.getValue()).getId();
                            int symbolId = ((LocationType.Symbol) jaxbElemtimeTypeElement.getValue()).getNumber().intValue();
                            if (dateFrom.get(Calendar.HOUR_OF_DAY) == 12 && dateTo.get(Calendar.HOUR_OF_DAY) == 18) {
                                dayForecastBean.setSymbol(symbol);
                                dayForecastBean.setSymbolId(symbolId);
                            }
                            hourForecastBean.setSymbol(symbol);
                            hourForecastBean.setSymbolId(symbolId);
                        }
                        break;
                    case "temperature" :
                        if (jaxbElemtimeTypeElement.getValue() instanceof Temperature)
                            hourForecastBean.setTemperature((int)Math.round(((Temperature) jaxbElemtimeTypeElement.getValue()).getValue().doubleValue()));
                    default:
                        break;
                }
            }
            if (hourForecastBean.getTemperature() != null || hourForecastBean.getSymbol() != null) {
                if (add && (this.calculateHours(today.getTime(), dateTo.getTime())) < 32)
                    this.hourForecastBeanMap.put(timeType.getTo().toGregorianCalendar(new GregorianCalendar().getTimeZone(), null, null).getTime(),hourForecastBean);
                dayForecastBeanMap.put(midNight.getTime(),dayForecastBean);
            }
        }
        return new ArrayList<>(dayForecastBeanMap.values());
    }

    public int calculateHours(Date from, Date to)
    {
        return (int) (to.getTime() - from.getTime()) / (1000 * 60 * 60);
    }

    public List<HourForecastBean> getHourForecastBeanList() {
        return  new ArrayList<>(this.hourForecastBeanMap.values());
    }
}
