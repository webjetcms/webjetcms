package sk.iway.iwcm.components.reservation.rest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectPriceEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectPriceRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/reservation/reservation_object_price")
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
        List<ReservationObjectPriceEntity> items = new ArrayList<>();
        Integer objectId = Tools.getIntValue(getRequest().getParameter("objectId"), -1);

        if(objectId == -1) items = reservationObjectPriceRepository.findAllByDomainId(CloudToolsForCore.getDomainId());
        else items = reservationObjectPriceRepository.findAllByObjectIdAndDomainId(objectId, CloudToolsForCore.getDomainId());

        DatatablePageImpl<ReservationObjectPriceEntity> page = new DatatablePageImpl<>(items);
        return page;
    }

    @Override
    public ReservationObjectPriceEntity getOneItem(long id) {
        if(id == -1) {
            ReservationObjectPriceEntity entity = new ReservationObjectPriceEntity();
            Integer objectId = Tools.getIntValue(getRequest().getParameter("objectId"), -1);
            if(objectId != -1) entity.setObjectId(objectId);
            return entity;
        }
        return reservationObjectPriceRepository.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElse(null);
    }

    @Override
    public void beforeSave(ReservationObjectPriceEntity entity) {
        entity.setDomainId(CloudToolsForCore.getDomainId());
    }
}
