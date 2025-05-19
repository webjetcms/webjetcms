package sk.iway.iwcm.components.gdpr.model;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import javax.validation.constraints.NotBlank;

import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/**
 *  GdprRegExpBean.java - Uklada regularne vyrazy zadane v module gdpr
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 09.05.2018 09:25:32
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@Entity
@Table(name="gdpr_regexp")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_GDPR_REGEXP)
public class GdprRegExpBean extends ActiveRecordRepository implements Serializable
{
	private static final long serialVersionUID = -1L;

	@Id
	@GeneratedValue(generator="WJGen_gdpr_regexp")
	@TableGenerator(name="WJGen_gdpr_regexp",pkColumnValue="gdpr_regexp")
	@Column(name="gdpr_regexp_id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
	private Long id;

	@Column(name="regexp_name")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="[[#{components.tooltip.name}]]"
    )
	@NotBlank
	String regexpName;

	@Column(name="regexp_value")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{components.gdpr.value}]]"
    )
	@NotBlank
	String regexpValue;

	@Column(name="user_id")
	Integer userId;

	@Column(name="date_insert")
	@Temporal(TemporalType.TIMESTAMP)
    Date dateInsert;

    @Column(name="domain_id")
    Integer domainId;

    public GdprRegExpBean(String regexpValue) {
        this.regexpValue = regexpValue;
    }
    public GdprRegExpBean() {
    }

	@Override
	public void setId(Long id)
	{
		this.id = id;
	}

	@Override
	public Long getId()
	{
		return id;
	}

	/** WJ8 kompatibilita */
	public int getGdprRegexpId()
	{
		if(id == null) return 0;
		return id.intValue();
	}

	public void setGdprRegexpId(int gdprRegexpId)
	{
		this.id = Long.valueOf(gdprRegexpId);
	}
	public String getRegexpName() {
		return regexpName;
	}
	public void setRegexpName(String regexpName) {
		this.regexpName = regexpName;
	}
	public String getRegexpValue() {
		return regexpValue;
	}
	public void setRegexpValue(String regexpValue) {
		this.regexpValue = regexpValue;
	}
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	public Date getDateInsert() {
		return dateInsert;
	}
	public void setDateInsert(Date dateInsert) {
		this.dateInsert = dateInsert;
	}
	public Integer getDomainId() {
		return domainId;
	}
	public void setDomainId(Integer domainId) {
		this.domainId = domainId;
	}
}