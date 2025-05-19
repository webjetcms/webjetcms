package sk.iway.iwcm.calendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.eclipse.persistence.jpa.JpaEntityManager;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;

@Api(description = "Vracia eventy z kalendaru udalosti")
@RestController
@RequestMapping(path = "/rest/events-calendar")
public class CalendarService {
    public List<EventsCalendarBean> getEventsBetweenDates(@RequestParam Date from, @RequestParam Date to)
    {
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        TypedQuery<EventsCalendarBean> query = getTypedQueryForFilterByDates(em, Month.getBeginOfDate(from), Month.getEndOfDay(to));

        return JpaDB.getResultList(query);
    }

    @RequestMapping(path="", method={RequestMethod.GET})
    public List<EventsCalendarBean> getEventsBetweenDatesByType(@RequestParam(required = true, name = "from") String from, @RequestParam(required = true, name = "to") String to, @RequestParam(required = false, name = "types") List<Integer> types)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            List<EventsCalendarBean> result = getEventsBetweenDates(sdf.parse(from), sdf.parse(to));
            if(types != null)
            {
                result = filterByTypes(result, types);
            }

            result.forEach(e-> {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(e.getTo());

                if (calendar.get(Calendar.HOUR_OF_DAY) == 0 && calendar.get(Calendar.MINUTE) == 0 && calendar.get(Calendar.SECOND) == 0 && calendar.get(Calendar.MILLISECOND) == 0) {
                    calendar.add(Calendar.DATE, 1);
                    calendar.add(Calendar.MILLISECOND, -1);
                    e.setTo(calendar.getTime());
                }
            });

            return result;
        }
        catch(ParseException e)
        {
            sk.iway.iwcm.Logger.error(e);
        }

        return Collections.emptyList();
    }

    @ApiOperation(value = "getEventsForMonth", notes = "Vrati eventy pre /rok/mesiac/")
    @RequestMapping(path="/{year}/{month}", method={RequestMethod.GET})
    public List<EventsCalendarBean> getEventsForMonth(
            @ApiParam(value = "Rok, napr. '2018'", required = true) @PathVariable int year,
            @ApiParam(value = "Mesiac, od 1 do 12, napr. '7'", required = true) @PathVariable int month
    )
    {
        Month selectedMonth = new Month(year, month - 1);
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        TypedQuery<EventsCalendarBean> query = getTypedQueryForFilterByDates(em, selectedMonth.getStartDate(), selectedMonth.getEndDate());

        return JpaDB.getResultList(query);
    }

    @ApiOperation(value = "getEventsByTypeForMonth", notes = "Vrati eventy pre /rok/mesiac/id1,id2,id3 kde id1..idX su idcka konkretnych CalendarTypeBean")
    @RequestMapping(path="/{year}/{month}/{types}", method={RequestMethod.GET})
    public List<EventsCalendarBean> getEventsByTypeForMonth(
            @ApiParam(value = "Rok, napr. '2018'", required = true) @PathVariable int year,
            @ApiParam(value = "Mesiac, od 1 do 12, napr. '7'", required = true) @PathVariable int month,
            @ApiParam(value = "Idecka typov udalosti, napr. '1,2,3'", required = true) @PathVariable List<Integer> types
    )
    {
        List<EventsCalendarBean> result = getEventsForMonth(year, month);
        result = filterByTypes(result, types);

        return result;
    }

    private TypedQuery<EventsCalendarBean> getTypedQueryForFilterByDates(EntityManager em, Date from, Date to)
    {
        TypedQuery<EventsCalendarBean> query = em.createQuery(
                "select e from EventsCalendarBean e where " +
                        "e.approve = true and " +
                        "((e.from >= :monthStart and e.from <= :monthEnd) or " +
                        "(e.to >= :monthStart and e.to <= :monthEnd))",
                EventsCalendarBean.class);

        query.setParameter("monthStart", Month.getBeginOfDate(from));
        query.setParameter("monthEnd", Month.getEndOfDay(to));

        return query;
    }

    private List<EventsCalendarBean> filterByTypes(List<EventsCalendarBean> result, List<Integer> types)
    {
        return result.stream().filter(i -> types.indexOf(i.getType().getId()) >= 0).collect(Collectors.toList());
    }

    @ApiOperation(value = "getEventTypes", notes = "Vrati typy eventov")
    @RequestMapping(path="/types", method={RequestMethod.GET})
    public List<CalendarTypeBean> getEventTypes()
    {
        return new JpaDB<CalendarTypeBean>(CalendarTypeBean.class).getAll();
    }
}
