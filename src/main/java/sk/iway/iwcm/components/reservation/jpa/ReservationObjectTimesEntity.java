package sk.iway.iwcm.components.reservation.jpa;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    private Long objectId;

    @Column(name = "cas_od")
    private Date timeFrom;

    @Column(name = "cas_do")
    private Date timeTo;

    @Column(name = "den")
    private Integer day;

    @Column(name = "domain_id")
    private Integer domainId;

    @ManyToOne
	@JsonBackReference(value="reservationObjectForTime")
	@JoinColumn(name="object_id", insertable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private ReservationObjectEntity reservationObjectForTime;
}
