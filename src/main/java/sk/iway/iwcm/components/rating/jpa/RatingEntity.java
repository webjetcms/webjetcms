package sk.iway.iwcm.components.rating.jpa;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.adminlog.EntityListenersType;

@Entity
@Table(name = "rating")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_INQUIRY)
public class RatingEntity {

    @Id
    @Column(name = "rating_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_rating")
    private Long id;

    @Column(name = "doc_id")
    private Integer docId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "rating_value")
    private Integer ratingValue;

    @Column(name = "insert_date")
	@Temporal(TemporalType.TIMESTAMP)
	private Date insertDate;

    @Transient
    private int totalSum;

    @Transient
	private double ratingValueDouble;

    @Transient
	private int totalUsers;

    @Transient
	private int ratingStat;

    @Transient
	private String docTitle;

    public String getInsertDate() {
        if(insertDate == null) return "";
        return Tools.formatDate(insertDate);
    }

    public String getInsertTime() {
        if(insertDate == null) return "";
        return Tools.formatTime(insertDate);
    }

    public long getInsertDateLong() {
        if(insertDate != null) return insertDate.getTime();
        return 0;
    }
}
