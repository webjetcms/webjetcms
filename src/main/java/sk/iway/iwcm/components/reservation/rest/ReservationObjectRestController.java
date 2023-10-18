package sk.iway.iwcm.components.reservation.rest;

import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectEditorFields;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectPriceEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectPriceRepository;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectRepository;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectTimesEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectTimesRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/reservation/reservation_object")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_reservation')")
@Datatable
public class ReservationObjectRestController extends DatatableRestControllerV2<ReservationObjectEntity, Long> {

    private final ReservationObjectRepository reservationObjectRepository;
    private final ReservationObjectTimesRepository reservationObjectTimesRepository;
    private final ReservationObjectPriceRepository reservationObjectPriceRepository;

    @Autowired
    public ReservationObjectRestController(ReservationObjectRepository reservationObjectRepository,
    ReservationObjectTimesRepository reservationObjectTimesRepository, ReservationObjectPriceRepository reservationObjectPriceRepository) {
        super(reservationObjectRepository);
        this. reservationObjectRepository = reservationObjectRepository;
        this.reservationObjectTimesRepository = reservationObjectTimesRepository;
        this.reservationObjectPriceRepository = reservationObjectPriceRepository;
    }

    @Override
    public Page<ReservationObjectEntity> getAllItems(Pageable pageable) {
        //Use custom select with domain id
        Page<ReservationObjectEntity> items = reservationObjectRepository.findAllByDomainId(CloudToolsForCore.getDomainId(), pageable);

        DatatablePageImpl<ReservationObjectEntity> page = new DatatablePageImpl<>(items);
        //In case of .GETALL we set only column addPassword from editorFields
        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public ReservationObjectEntity getOneItem(long id) {
        ReservationObjectEntity entity;
        if(id == -1) {
            entity = new ReservationObjectEntity();
            processFromEntity(entity, ProcessItemAction.CREATE);
        } else {
            entity = reservationObjectRepository.getById(id);
            processFromEntity(entity, ProcessItemAction.EDIT);
        }

        return entity;
    }

    @Override
    public ReservationObjectEntity processFromEntity(ReservationObjectEntity entity, ProcessItemAction action) {

        //Special situation, saved entity with allready set editorFields
        //We dont want processFromEntity because it can re-write our data
        if(entity.getEditorFields() != null) return entity;

        //If editorFields if null create new
        ReservationObjectEditorFields roef = entity.getEditorFields() == null ? new ReservationObjectEditorFields() : entity.getEditorFields();

        //Set editor fields values and this editorFields set into entity
        if(action == ProcessItemAction.EDIT) //Only if we editing entity that exist, we use bind reservationObjectTimes from DB
            roef.fromReservationObjectEntity(entity, action, reservationObjectTimesRepository.findAllByObjectIdAndDomainId(entity.getId().intValue(), CloudToolsForCore.getDomainId()));
        else
            roef.fromReservationObjectEntity(entity, action, null);

        return entity;
    }

    @Override
    public ReservationObjectEntity processToEntity(ReservationObjectEntity entity, ProcessItemAction action) {
        if(entity.getEditorFields() != null)
            entity.getEditorFields().toReservationObjectEntity(entity, action, reservationObjectTimesRepository);

        return entity;
    }

    @Override
    public boolean beforeDelete(ReservationObjectEntity entity) {
        //Before delete remove bind ReservationObjectTimes records from DB
        List<ReservationObjectTimesEntity> timeEntitiesToDelete = reservationObjectTimesRepository.findAllByObjectIdAndDomainId(entity.getId().intValue(), CloudToolsForCore.getDomainId());
        if(timeEntitiesToDelete != null && timeEntitiesToDelete.size() > 0)
            reservationObjectTimesRepository.deleteAll(timeEntitiesToDelete);

        //Before delete remove bind ReservationObjectPrices records from DB
        List<ReservationObjectPriceEntity> pricesToDelete = reservationObjectPriceRepository.findAllByObjectIdAndDomainId(entity.getId().intValue(), CloudToolsForCore.getDomainId());
        if(pricesToDelete != null && pricesToDelete.size() > 0)
            reservationObjectPriceRepository.deleteAll(pricesToDelete);

        return true;
    }

	@Override
	public void beforeSave(ReservationObjectEntity entity) {
        ReservationObjectEditorFields roef = entity.getEditorFields();
        //Check password validations
        if(roef.getAddPassword()) {
            if(roef.getNewPassword() == null || roef.getNewPassword().isEmpty()) throwError("reservation.reservation_object.password_error_empty");
            if(roef.getPasswordCheck() == null || roef.getPasswordCheck().isEmpty()) throwError("reservation.reservation_object.password_error_empty");
            if(!roef.getNewPassword().equals(roef.getPasswordCheck())) throwError("passwordsNotMatch");
        }

        //Email validation
        if(entity.getMustAccepted()) {
            if(entity.getEmailAccepter() == null || entity.getEmailAccepter().isEmpty()) throwError("components.reservation.reservation_manager.addReservationObject.one");
            //Email validation
            try {
                InternetAddress emailAddr = new InternetAddress(entity.getEmailAccepter());
                emailAddr.validate();
            } catch (AddressException ex) {
                throwError("components.form.email.not_valid");
            }
	    }
    }

    @Override
    public void afterSave(ReservationObjectEntity entity, ReservationObjectEntity saved) {
        //We must call toReservationObjectEntity after save with action EDIT
        //Reason, we must save ReservationObjectTimes but we need entity.ID (and this ID is set after save)
        ReservationObjectEditorFields roef = entity.getEditorFields();
        roef.toReservationObjectEntity(entity, ProcessItemAction.EDIT, reservationObjectTimesRepository);
    }
}
