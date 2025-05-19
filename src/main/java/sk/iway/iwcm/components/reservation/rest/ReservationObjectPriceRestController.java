package sk.iway.iwcm.components.reservation.rest;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.DateTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectPriceEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectPriceRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/reservation/reservation-object-price")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_reservation')")
@Datatable
public class ReservationObjectPriceRestController  extends DatatableRestControllerV2<ReservationObjectPriceEntity, Long> {

    private final ReservationObjectPriceRepository reservationObjectPriceRepository;

    @Autowired
    public ReservationObjectPriceRestController( ReservationObjectPriceRepository reservationObjectPriceRepository) {
        super(reservationObjectPriceRepository);
        this.reservationObjectPriceRepository = reservationObjectPriceRepository;
    }

    @Override
    public Page<ReservationObjectPriceEntity> getAllItems(Pageable pageable) {
        List<ReservationObjectPriceEntity> items;
        Long objectId = Tools.getLongValue(getRequest().getParameter("object-id"), -1);

        if(objectId == -1) items = reservationObjectPriceRepository.findAllByDomainId(CloudToolsForCore.getDomainId());
        else items = reservationObjectPriceRepository.findAllByObjectIdAndDomainId(objectId, CloudToolsForCore.getDomainId());

        return new DatatablePageImpl<>(items);
    }

    @Override
    public ReservationObjectPriceEntity getOneItem(long id) {
        if(id == -1) {
            ReservationObjectPriceEntity entity = new ReservationObjectPriceEntity();
            Long objectId = Tools.getLongValue(getRequest().getParameter("object-id"), -1);
            if(objectId != -1) entity.setObjectId(objectId);
            return entity;
        }
        return reservationObjectPriceRepository.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElse(null);
    }

    @Override
    public void beforeSave(ReservationObjectPriceEntity entity) {
        entity.setDomainId(CloudToolsForCore.getDomainId());
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, ReservationObjectPriceEntity> target, Identity user, Errors errors, Long id, ReservationObjectPriceEntity entity) {

        if(target.getAction().equals("create") || target.getAction().equals("edit")) {

            if(entity.getDateFrom() == null)
                errors.rejectValue("errorField.dateFrom", null, getProp().getText("javax.validation.constraints.NotBlank.message"));
            if(entity.getDateTo() == null)
                errors.rejectValue("errorField.dateTo", null, getProp().getText("javax.validation.constraints.NotBlank.message"));

            //both dates must be set
            if(entity.getDateFrom() == null || entity.getDateTo() == null) return;

            //validate date range
            switch (DateTools.validateRange(entity.getDateFrom(), entity.getDateTo(), false, true) ){
                case -1: throw new IllegalArgumentException( getProp().getText("datatable.error.fieldErrorMessage") );
                case 1 : throw new IllegalArgumentException( getProp().getText("components.reservation.reservation_object_prices.date_in_past_error") );
                case 2 : throw new IllegalArgumentException( getProp().getText("components.reservation.reservation_object_prices.bad_order_error") );
                default: break;
            }

            //Check overlapping of date ranges for different prices
            List<ReservationObjectPriceEntity> allPrices = reservationObjectPriceRepository.findAllByObjectIdAndDomainId(entity.getObjectId(), CloudToolsForCore.getDomainId());

            boolean isOverlapping = false;
            ReservationService service = new ReservationService(getProp());
            for(ReservationObjectPriceEntity price : allPrices) {
                if(target.getAction().equals("edit") && price.getId().equals(entity.getId())) continue; //Skip the current price (we are editing it
                if(service.checkOverlap(price.getDateFrom(), price.getDateTo(), entity.getDateFrom(), entity.getDateTo(), null)) {
                    isOverlapping = true;
                    break;
                }
            }

            if(isOverlapping) {
                throw new IllegalArgumentException( getProp().getText("components.reservation.reservation_object_prices.overlapping_error") );
            }
        }

        super.validateEditor(request, target, user, errors, id, entity);
    }
}
