package sk.iway.iwcm.components.monitoring.rest;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.monitoring.jpa.MonitoringActualBean;
import sk.iway.iwcm.components.monitoring.jpa.MonitoringEntity;
import sk.iway.iwcm.components.monitoring.jpa.MonitoringRepository;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/**
 * REST sluzba pre vratenie hodnot aktualneho stavu monitoringu
 */
@RestController
@RequestMapping("/admin/rest/monitoring")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_server_monitoring')")
public class MonitoringRestController extends DatatableRestControllerV2<MonitoringEntity, Long>{

    private MonitoringRepository monitoringRepository;
    private Date dateFrom;
    private Date dateTo;
    private Integer maxDataCount = 500;

    @Autowired
    public MonitoringRestController(MonitoringRepository monitoringRepository) {
        super(monitoringRepository);
        this.monitoringRepository = monitoringRepository;
    }

    @Override
    public void beforeSave(MonitoringEntity entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<MonitoringEntity> getAllItems(Pageable pageable){
        //Generate 14 days date range (dateFrom, dateTo)
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, -14);
        dateFrom = cal.getTime();
        dateTo = new Date();

        //If page size is bigger than set constant (in this case maxDataCount = 500) first aggregate data of one month range then return. Or return directly.
        if(pageable.getPageSize() >= this.maxDataCount) {
            Page<MonitoringEntity> pageableData = monitoringRepository.findAllByDayDateGreaterThanEqualAndDayDateLessThanEqual(pageable, dateFrom, dateTo);
            MonitoringManager monitoringManger = new MonitoringManager(pageableData, dateFrom, dateTo);
            return new PageImpl<>(monitoringManger.returnAggregatedData());
        } else {
            Page<MonitoringEntity> pageableData = monitoringRepository.findAll(pageable);
            return pageableData;
        }
    }

    @Override
    public Page<MonitoringEntity> searchItem(Map<String, String> params, Pageable pageable, MonitoringEntity search) {

        String stringRange = "";
        //Get dateRange param from map
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if(entry.getKey().equals("searchDayDate")) {
                stringRange = (String) entry.getValue();
                stringRange = stringRange.substring("daterange:".length());
                break;
            }
        }

        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);
        dateFrom = dateRangeArr[0];
        dateTo = dateRangeArr[1];

        /*If pagination param is set to true or page size is bigger than set constant (in this case maxDataCount = 500) first aggregate data in set date
        range then return. Or return data directly withou aggregation.*/
        String paggination = params.get("pagination");
        if( (paggination != null && paggination.equals("true")) || pageable.getPageSize() >= this.maxDataCount) {
            Page<MonitoringEntity> pageableData = monitoringRepository.findAllByDayDateGreaterThanEqualAndDayDateLessThanEqual(pageable, dateFrom, dateTo);
            MonitoringManager monitoringManger = new MonitoringManager(pageableData, dateFrom, dateTo);
            return new PageImpl<>(monitoringManger.returnAggregatedData());
        } else {
            return monitoringRepository.findAllByDayDateGreaterThanEqualAndDayDateLessThanEqual(pageable, dateFrom, dateTo);
        }
    }

    @RequestMapping("/actual")
    public MonitoringActualBean getActualValues() {
        MonitoringActualBean actual = new MonitoringActualBean();
        return actual;
    }
}
