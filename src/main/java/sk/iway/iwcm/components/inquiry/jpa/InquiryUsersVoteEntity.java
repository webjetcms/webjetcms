package sk.iway.iwcm.components.inquiry.jpa;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "inquiry_users")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_INQUIRY)
public class InquiryUsersVoteEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_inquiry_users")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "answer_id")
    private Long answerId;

    @Column(name = "domain_id")
    private Integer domainId;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.inquiry.inquiry_statistics.user_name", filter = true)
    private String userFullName;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.inquiry.inquiry_statistics.answer_text", filter = true)
    private String answerText;

    // It's called dayDate and not createDate soo we can use logic for stats !!
    @Column(name = "create_date")
    @DataTableColumn(inputType = DataTableColumnType.DATE, title = "components.inquiry.inquiry_statistics.create_date")
    private Date dayDate;

    @Column(name = "ip_address")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.inquiry.inquiry_statistics.ip")
    private String ipAddress;

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public void setAnswerId(Long answerId) {
        this.answerId = answerId;
    }

    public void setUserId(int userId) {
        this.userId = (long) userId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = Long.valueOf(questionId);
    }

    public void setAnswerId(int answerId) {
        this.answerId = Long.valueOf(answerId);
    }
}