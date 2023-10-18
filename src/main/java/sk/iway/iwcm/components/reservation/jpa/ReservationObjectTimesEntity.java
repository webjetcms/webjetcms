package sk.iway.iwcm.components.reservation.jpa;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;

@Entity
@Table(name = "reservation_object_times")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_RESERVATION_TIMES)
public class ReservationObjectTimesEntity implements Serializable {
    
    @Id
    @GeneratedValue(generator = "WJGen_reservation_object_times")
    @TableGenerator(name = "WJGen_reservation_object_times", pkColumnValue = "reservation_object_times")
    @Column(name = "object_time_id")
    private Long id;

    @Column(name = "object_id")
    private Integer objectId;

    @Column(name = "cas_od")
    private Date timeFrom;

    @Column(name = "cas_do")
    private Date timeTo;

    @Column(name = "den")
    private Integer day;

    @Column(name = "domain_id")
    private Integer domainId;
}
