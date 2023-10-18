package sk.iway.iwcm.doc;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.beanutils.BeanUtils;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "documents")
public class DocDetails extends DocBasic {

	public DocDetails(){}

	/**
	 * Konstruktor z cesty, napr. /Newsletter/Testovaci newsletter
	 * Pouziva sa pri importe a inych operaciach kde dostaneme cestu k adresaru
	 * @param path
	 */
	public DocDetails(String path) {
		this(DocDetails.getCreateDoc(path));
	}

	private static DocDetails getCreateDoc(String path) {

		//check if path is number, then return doc with this id
		try {
			int id = Integer.parseInt(path);
			return DocDB.getInstance().getDoc(id);
		} catch (Exception e) {
			//not a number, continue
		}

		if (path.contains("/")==false) {
			return null;
		}

		return DocDB.getInstance().getCreateDoc(path);
	}

	/**
	 * skopiruje do seba doc copyFrom
	 * @param copyFrom
	 */
	public DocDetails(DocDetails copyFrom) {
		try
		{
			if (copyFrom != null) BeanUtils.copyProperties(this, copyFrom);
		}
		catch (Exception ex)
		{
			Logger.error(DocDetails.class, ex);
		}
	}

	@PrePersist
    public void prePersist() {
        //log.debug("prePersist");
		//Logger.debug(getClass(), "prePersist, id1"+id);
		if (id != null && (id.intValue()==0 || id.intValue()==-1)) {
			id = null;
			//Logger.debug(getClass(), "prePersist, id2="+id);
		}
    }

	@Id
	@Column(name = "doc_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY, generator="S_documents")
	@DataTableColumn(inputType = DataTableColumnType.ID, title="ID")
	protected Long id;

	@Column(name = "root_group_l1")
	private Integer rootGroupL1;

	@Column(name = "root_group_l2")
	private Integer rootGroupL2;

	@Column(name = "root_group_l3")
	private Integer rootGroupL3;

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
		if(this.id == null) return 0;
		return this.id.intValue();
	}

	@Override
	public void setDocId(int newDocId)
	{
		this.id = Long.valueOf(newDocId);
	}

	public void setDocId(Long newDocId)
	{
		this.id = newDocId;
	}

	//atributy potrebne pre spatnu kompatibilitu pre history tabulku, neukladaju sa
	@Transient
	private boolean publicable = false;
	@Transient
	private boolean historyActual = false;
	@Transient
	private String historyApproveDate;
	@Transient
	private String historyApprovedByName;
	@Transient
	private String historyDisapprovedByName;
	@Transient
	private String historySaveDate;
	@Transient
	private int historyId;
	@Transient
	private int historyApprovedBy;
	@Transient
	private int historyDisapprovedBy;

	//*************** GETTER/SETTERS AUTO GENERATED *****************************/
	public Integer getRootGroupL1() {
		return rootGroupL1;
	}

	public void setRootGroupL1(Integer rootGroupL1) {
		this.rootGroupL1 = rootGroupL1;
	}

	public Integer getRootGroupL2() {
		return rootGroupL2;
	}

	public void setRootGroupL2(Integer rootGroupL2) {
		this.rootGroupL2 = rootGroupL2;
	}

	public Integer getRootGroupL3() {
		return rootGroupL3;
	}

	public void setRootGroupL3(Integer rootGroupL3) {
		this.rootGroupL3 = rootGroupL3;
	}

	public boolean isHistoryActual() {
		return historyActual;
	}

	public void setHistoryActual(boolean historyActual) {
		this.historyActual = historyActual;
	}

	public String getHistoryApproveDate() {
		return historyApproveDate;
	}

	public void setHistoryApproveDate(String historyApproveDate) {
		this.historyApproveDate = historyApproveDate;
	}

	public String getHistoryApprovedByName() {
		return historyApprovedByName;
	}

	public void setHistoryApprovedByName(String historyApprovedByName) {
		this.historyApprovedByName = historyApprovedByName;
	}

	public String getHistoryDisapprovedByName() {
		return historyDisapprovedByName;
	}

	public void setHistoryDisapprovedByName(String historyDisapprovedByName) {
		this.historyDisapprovedByName = historyDisapprovedByName;
	}

	public String getHistorySaveDate() {
		return historySaveDate;
	}

	public void setHistorySaveDate(String historySaveDate) {
		this.historySaveDate = historySaveDate;
	}

	public int getHistoryId() {
		return historyId;
	}

	public void setHistoryId(int historyId) {
		this.historyId = historyId;
	}

	public int getHistoryApprovedBy() {
		return historyApprovedBy;
	}

	public void setHistoryApprovedBy(int historyApprovedBy) {
		this.historyApprovedBy = historyApprovedBy;
	}

	public int getHistoryDisapprovedBy() {
		return historyDisapprovedBy;
	}

	public void setHistoryDisapprovedBy(int historyDisapprovedBy) {
		this.historyDisapprovedBy = historyDisapprovedBy;
	}

	public boolean isPublicable()
	{
		return publicable;
	}

	public void setPublicable(boolean publicable) {
		this.publicable = publicable;
	}

}
