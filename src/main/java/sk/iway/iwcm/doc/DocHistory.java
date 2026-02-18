package sk.iway.iwcm.doc;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.users.UserBasicDto;
import sk.iway.iwcm.users.jpa.UserBasicDtoConverter;

@Entity
@Table(name = "documents_history")
@Getter
@Setter
public class DocHistory extends DocBasic {

	@PrePersist
    public void prePersist() {
        //log.debug("prePersist");
		Logger.debug(getClass(), "prePersist, id1"+id);
		if (id != null && (id.intValue()==0 || id.intValue()==-1)) {
			id = null;
			Logger.debug(getClass(), "prePersist, id2="+id);
		}
    }

    @Id
	@Column(name = "history_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY, generator="S_documents_history")
	@DataTableColumn(inputType = DataTableColumnType.ID)
	protected Long id;

    @Column(name = "doc_id")
	private Integer docId;

	@Column(name = "awaiting_approve")
	private String awaitingApprove;

    @Column(name = "publicable")
	private Boolean publicable;

	@Column(name = "approved_by")
	private Integer approvedBy;

    @Column(name = "disapproved_by")
	private Integer disapprovedBy;

	//deprecated, not need anymore @Temporal(TemporalType.TIMESTAMP)
	@Column(name = "approve_date")
	private Date approveDate;

	@Transient
	private String historyApprovedByName;

	@Transient
	private String historyDisapprovedByName;

	//deprecated, not need anymore @Temporal(TemporalType.TIMESTAMP)
	@Column(name = "save_date")
	private Date saveDate;

	@Column(name = "actual")
	private Boolean actual;

	//Get author as UserDetails object
	@Column(name = "author_id", insertable = false, updatable = false) // author_id is allready used, so this one must be read onl
    @Convert(converter = UserBasicDtoConverter.class)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) //toto nepotrebujeme deserializovat pri post requeste
    private UserBasicDto userDetails;

	@Override
	public Long getId()
	{
		if (this.id != null && this.id.longValue()<1) return null;
		return this.id;
	}

	@Override
	public void setId(Long id)
	{
		this.id = id;
	}

    @Override
	public int getDocId()
	{
		if (docId == null) return 0;
		return this.docId.intValue();
	}

	@Override
	public void setDocId(int docId)
	{
		this.docId = Integer.valueOf(docId);
	}

	public void setHistoryId(int historyId)
	{
		if (historyId == 0) return;
		this.id = Long.valueOf(historyId);
	}

	public void setHistoryApprovedBy(int historyApprovedBy)
	{
		this.approvedBy = historyApprovedBy;
	}

	public void setHistoryDisapprovedBy(int historyDisapprovedBy)
	{
		this.disapprovedBy = historyDisapprovedBy;
	}

	public int getHistoryId() {
		if (id == null) return 0;
		return id.intValue();
	}

	public int getHistoryApprovedBy() {
		if (approvedBy==null) return 0;
		return approvedBy.intValue();
	}

	public int getHistoryDisapprovedBy() {
		if (disapprovedBy==null) return 0;
		return disapprovedBy.intValue();
	}

	public void setHistorySaveDate(String historySaveDate)
	{
		if (Tools.isEmpty(historySaveDate)) return;
		this.saveDate = new Date(DB.getTimestamp(historySaveDate));
	}

	public String getHistoryApproveDate() {
		return Tools.formatDateTime(approveDate);
	}

	public void setHistoryApproveDate(String historyApproveDate) {
		if (Tools.isEmpty(historyApproveDate)) return;
		this.approveDate = new Date(DB.getTimestamp(historyApproveDate));
	}

	public String getHistoryApprovedByName() {
		return historyApprovedByName;
	}

	public void setHistoryApprovedByName(String historyApprovedByName) {
		if (Tools.isEmpty(historyApprovedByName)) return;
		this.historyApprovedByName = historyApprovedByName;
	}

	public String getHistoryDisapprovedByName() {
		return historyDisapprovedByName;
	}

	public void setHistoryDisapprovedByName(String historyDisapprovedByName) {
		if (Tools.isEmpty(historyDisapprovedByName)) return;
		this.historyDisapprovedByName = historyDisapprovedByName;
	}

	public String getHistorySaveDate() {
		return Tools.formatDateTime(saveDate);
	}

	public boolean isHistoryActual()
	{
		if (actual == null) return false;
		return actual.booleanValue();
	}

	public void setHistoryActual(boolean historyActual) {
		this.actual = historyActual;
	}

	@Override
	public String getPublishStartStringExtra() {
		if (Boolean.TRUE.equals(publicable)) {
			return getPublishStartString();
		}
		return "";
	}
}
