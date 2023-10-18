package sk.iway.iwcm.components.monitoring.rest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import sk.iway.iwcm.components.monitoring.jpa.MonitoringEntity;

/**
 * 50053-WJ9--monitorovanie-servera-zaznmenaie-hodnoty
 * Agregracia dat servera, vráti maximálne hodnoty za určitý časový úsek.
 * Autor: Sebastian Ivan
 */
public class MonitoringAggregator {

    private  Page<MonitoringEntity> data;
    private int dateRange;
    private HashMap<String, HashMap<Date, MonitoringEntity>> map = new HashMap<String, HashMap<Date, MonitoringEntity>>();
    private Calendar calendarInstance = Calendar.getInstance();

    //Constructor. Set element data and compute data range in days
    public MonitoringAggregator(Page<MonitoringEntity> data, Date fromDate, Date toDate) {
        this.data = data;

        int diffInDays = (int)( (fromDate.getTime() - toDate.getTime()) / (1000 * 60 * 60 * 24) );
        this.dateRange = Math.abs(diffInDays);

        this.aggregateData();
    }

    private void aggregateData() {

        MonitoringEntity tmpEntity = new MonitoringEntity();
        //Loop MonitoringEntity array data
        for(MonitoringEntity entity : data) {
            Date dateToRound = new Date();
            String nodeName = entity.getNodeName();
            //If node name is empty, set nodeName to "empty" (just for FrontEnd use)
            if(nodeName == null || nodeName.trim().isEmpty() || nodeName == "") {
                nodeName = "empty";
                entity.setNodeName("empty");
            }

            //Call roundDate fn to round date (later data with same round date will be aggregated together)
            dateToRound = roundDate(entity);
            entity.setDayDate(dateToRound);

            /*Set HasMap, where nodeName is key (every key is uniqe) and value is another HashMap.
            In fact every nodeName (key) has his own HashMap -> key is round date and value is MonitoringEntity*/
            if(map.get(nodeName) == null) {
                HashMap<Date, MonitoringEntity> tmpHashMap = new HashMap<Date, MonitoringEntity>();
                map.put(nodeName, tmpHashMap);
            }

            //If nodeName's HashMap already contain this round date as a key combine this two Entities using setEntityWithMaxAtributes fn
            tmpEntity = map.get(nodeName).get(dateToRound);
            if(tmpEntity == null) {
                map.get(nodeName).put(dateToRound, entity);
            } else {
                setEntityWithMaxAtributes(entity, tmpEntity);
            }
        }
    }

    //Use dateRange param to call right date rounding function
    private Date roundDate(MonitoringEntity entity) {
        if(this.dateRange <= 5) {
            return entity.getDayDate();
        } else if(this.dateRange > 5 && this.dateRange <= 10) {
            return tenMinutesAggregation(entity.getDayDate());
        } else if(this.dateRange > 10 && this.dateRange <= 14) {
            return thirtyMinutesAggregation(entity.getDayDate());
        } else if(this.dateRange > 14 && this.dateRange <= 30) {
            return oneHourAggregation(entity.getDayDate());
        } else if(this.dateRange > 30 && this.dateRange <= 60) {
            return fourHoursAggregation(entity.getDayDate());
        } else {
            return twelveHoursAggregation(entity.getDayDate());
        }
    }

    //Compare same params from 2 Entities, and return new Entity where every param had set bigger value from this 2 Entiries
    private void setEntityWithMaxAtributes(MonitoringEntity entity1, MonitoringEntity entity2 ) {
        entity1.setCache(entity1.getCache() > entity2.getCache() ? entity1.getCache() : entity2.getCache());
        entity1.setCpuUsage(entity1.getCpuUsage() > entity2.getCpuUsage() ? entity1.getCpuUsage() : entity2.getCpuUsage());
        entity1.setDbActive(entity1.getDbActive() > entity2.getDbActive() ? entity1.getDbActive() : entity2.getDbActive());
        entity1.setDbIdle(entity1.getDbIdle() > entity2.getDbIdle() ? entity1.getDbIdle() : entity2.getDbIdle());
        entity1.setId(entity1.getId() > entity2.getId() ? entity1.getId() : entity2.getId());
        entity1.setMemFree(entity1.getMemFree() > entity2.getMemFree() ? entity1.getMemFree() : entity2.getMemFree());
        entity1.setMemTotal(entity1.getMemTotal() > entity2.getMemTotal() ? entity1.getMemTotal() : entity2.getMemTotal());
        entity1.setProcessUsage(entity1.getProcessUsage() > entity2.getProcessUsage() ? entity1.getProcessUsage() : entity2.getProcessUsage());
        entity1.setSessions(entity1.getSessions() > entity2.getSessions() ? entity1.getSessions() : entity2.getSessions());
    }

    //10 minut date round
    private Date tenMinutesAggregation(Date dateToRound) {
        Calendar calendar = calendarInstance;
        calendar.setTime(dateToRound);

        int unroundedMinutes = calendar.get(Calendar.MINUTE);
        int mod = unroundedMinutes % 10;
        calendar.add(Calendar.MINUTE, mod < 5 ? -mod : (10-mod));
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    //30 minut date round
    private Date thirtyMinutesAggregation(Date dateToRound) {
        Calendar calendar = calendarInstance;
        calendar.setTime(dateToRound);

        int actualMinutes = calendar.get(Calendar.MINUTE);
        if(actualMinutes < 15) {
            calendar.set(Calendar.MINUTE, 0);
        } else if(actualMinutes > 15 && actualMinutes < 45) {
            calendar.set(Calendar.MINUTE, 30);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, Calendar.HOUR_OF_DAY + 1);
            calendar.set(Calendar.MINUTE, 0);
        }
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    //1 hour date round
    private Date oneHourAggregation(Date dateToRound) {
        Calendar calendar = calendarInstance;
        calendar.setTime(dateToRound);

        int actualMinutes = calendar.get(Calendar.MINUTE);
        int actualHour = calendar.get(Calendar.HOUR_OF_DAY);
        calendar.set(Calendar.HOUR_OF_DAY, actualMinutes < 30 ? actualHour : actualHour + 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    //4 hours date round
    private Date fourHoursAggregation(Date dateToRound) {
        Calendar calendar = calendarInstance;
        calendar.setTime(dateToRound);

        int actualHour = calendar.get(Calendar.HOUR_OF_DAY);
        int mod = actualHour % 4;
        calendar.add(Calendar.HOUR_OF_DAY, mod < 2 ? -mod : (4-mod));
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    //12 hours date round
    private Date twelveHoursAggregation(Date dateToRound) {
        Calendar calendar = calendarInstance;
        calendar.setTime(dateToRound);

        int actualHour = calendar.get(Calendar.HOUR_OF_DAY);
        calendar.set(Calendar.HOUR_OF_DAY, actualHour < 12 ? actualHour : actualHour + 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    //Transform this data which containt two HashMap into List of MonitoringEntities (we need List format to return this data to FrontEnd)
    private List<MonitoringEntity> transformDataToList() {
        List<MonitoringEntity> transformedData = new ArrayList<>();
        for (Map.Entry<String, HashMap<Date, MonitoringEntity>> entry1 : this.map.entrySet()) {
            for(Map.Entry<Date, MonitoringEntity> entry2 : entry1.getValue().entrySet()) {
                transformedData.add(entry2.getValue());
            }
        }
        return transformedData;
    }

    public List<MonitoringEntity> returnAggregatedData() {
        return this.transformDataToList();
    }
}
