package sk.iway.iwcm.components.reservation.rest;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.reservation.jpa.ReservationRepository;
import sk.iway.iwcm.components.reservation.jpa.ReservationStatDTO;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/reservation/reservation-stat")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_reservation')")
@Datatable
public class ReservationStatRestController extends DatatableRestControllerV2<ReservationStatDTO, Long> {

    private final ReservationRepository reservationRepository;
 
    @Autowired
    public ReservationStatRestController(ReservationRepository reservationRepository) {
        super(null);
        this.reservationRepository = reservationRepository;
    }

    @Override
    public Page<ReservationStatDTO> getAllItems(Pageable pageable) {
        String searchDate = getRequest().getParameter("search-date");
        String reservationType = getRequest().getParameter("reservation-type");

        return new DatatablePageImpl<>( ReservationStatService.getTableData(searchDate, reservationType, reservationRepository) );
    }

    @RequestMapping(
        value="/pie-chart-data",
        params={"search-date", "reservation-type", "wanted-value"})
    public List<ReservationStatService.DoublePieChartData> getPieChartData(
        @RequestParam("search-date") String searchDate,
        @RequestParam("reservation-type") String reservationType,
        @RequestParam("wanted-value") String wantedValue
    ) {
        return ReservationStatService.getPieChartData(searchDate, reservationType, wantedValue, reservationRepository);
    }

    @RequestMapping(
        value="/line-chart-data",
        params={"search-date", "reservation-type"})
    public Map<String, List<ReservationStatService.LineChartData>> getLineChartData(
        @RequestParam("search-date") String searchDate,
        @RequestParam("reservation-type") String reservationType
    ) {
        return ReservationStatService.getLineChartData(searchDate, reservationType, reservationRepository);
    }
}