package sk.iway.iwcm.system;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;

/**
 *  UrlRedirectBean.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 14.04.2010 16:43:36
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@Entity
@Table(name="url_redirect")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_REDIRECT_UPDATE)
public class UrlRedirectBean extends ActiveRecordRepository implements Serializable
{
	private static final long serialVersionUID = -1L;

	public UrlRedirectBean() {}

	public UrlRedirectBean(String oldUrl, String newUrl, Integer redirectCode, String domainName) {
		this.oldUrl = oldUrl;
		this.newUrl = newUrl;
		this.redirectCode = redirectCode;
		this.domainName = domainName;
	}

	@Id
	@GeneratedValue(generator="WJGen_url_redirect")
	@TableGenerator(name="WJGen_url_redirect",pkColumnValue="url_redirect")
	@Column(name="url_redirect_id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
	private Long urlRedirectId;

	@Column(name="insert_date")
	@Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title ="components.redirect.admin_list.datum_vlozenia",
		sortAfter = "validTo",
        disabled = true
    )
	Date insertDate;

	@Column(name="old_url")
	@NotBlank
	@Size(max=255)
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title = "components.redirect.admin_list.stare_url",
		sortAfter = "urlRedirectId"
    )
	String oldUrl;

	@Column(name="redirect_code",nullable=false)
	@NotNull
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT_NUMBER,
        title = "components.redirect.admin_list.presmerovaci_kod",
		sortAfter = "newUrl",
		defaultValue = "302"
    )
	Integer redirectCode;

	@Column(name="domain_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "groupedit.domain",
        perms = "multiDomain",
		sortAfter = "redirectCode",
		defaultValue = "{currentDomain}",
		className = "multiweb-noteditable"
    )
	String domainName;

	@Column(name="new_url")
	@NotBlank
	@Size(max=255)
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "components.redirect.admin_list.nove_url",
		sortAfter = "oldUrl"
    )
	String newUrl;

	@Column(name="publish_date")
	@Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title = "components.banner.dateFrom",
		sortAfter = "domainName",
        editor = {
            @DataTableColumnEditor(
				message="components.redirect.publishDateNote"
			)
        }
    )
	Date publishDate;

	@Column(name="valid_to")
	@Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title = "components.banner.dateTo",
		editor = {
            @DataTableColumnEditor(
				message="components.redirect.publishEndDateNote"
			)
        }
    )
	Date validTo;

	@Lob
	@Column(name="description")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "groupedit.comment"
    )
	String description;

	public Long getUrlRedirectId() {
		return urlRedirectId;
	}

	public void setUrlRedirectId(Long urlRedirectId) {
		this.urlRedirectId = urlRedirectId;
	}

	public Date getInsertDate()
	{
		return insertDate == null ? null : (Date) insertDate.clone();
	}

	public void setInsertDate(Date insertDate)
	{
		this.insertDate = insertDate == null ? null : (Date) insertDate.clone();
	}

	public String getOldUrl()
	{
		return oldUrl;
	}

	public void setOldUrl(String oldUrl)
	{
		this.oldUrl = oldUrl;
	}

	public Integer getRedirectCode()
	{
		return redirectCode;
	}

	public void setRedirectCode(Integer redirectCode)
	{
		this.redirectCode = redirectCode;
	}

	public String getDomainName()
	{
		return domainName;
	}

	public void setDomainName(String domainName)
	{
		this.domainName = domainName;
	}

	public String getNewUrl()
	{
		return newUrl;
	}

	public void setNewUrl(String newUrl)
	{
		this.newUrl = newUrl;
	}

	@Override
	public Long getId()
	{
		return getUrlRedirectId();
	}

	@Override
	public void setId(Long id)
	{
		setUrlRedirectId(id);
	}

	public Date getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(Date publishDate) {
		this.publishDate = publishDate;
	}

	@JsonIgnore
	public String getPublishTime() {
		return Tools.formatTime(this.getPublishDate());
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getValidTo() {
		return validTo;
	}

	public void setValidTo(Date validTo) {
		this.validTo = validTo;
	}
}