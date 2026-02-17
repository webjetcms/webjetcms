package sk.iway.iwcm.components.domainRedirects;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.validation.constraints.NotBlank;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

/**
 *  RedirectBean.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: Marián Halaš $
 *@version      $Revision: 1.3 $
 *@created      Date: 9.11.2010 15:35:43
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@Entity
@Table(name="domain_redirects")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_REDIRECT_UPDATE)
public class DomainRedirectBean implements Serializable
{

	private static final long serialVersionUID = -6434400704509503505L;

	@Id
	@GeneratedValue(generator="WJGen_domain_redirects")
	@TableGenerator(name="WJGen_domain_redirects",pkColumnValue="domain_redirects")
   @Column(name="redirect_id")
	@DataTableColumn(inputType = DataTableColumnType.ID)
   private Integer redirectId;

	@Column(name="redirect_from")
	@NotBlank
	@DataTableColumn(
		inputType = DataTableColumnType.OPEN_EDITOR,
		title = "components.redirect.label.from"
	)
   private String redirectFrom;

	@Column(name="redirect_to")
	@NotBlank
	@DataTableColumn(
		inputType = DataTableColumnType.TEXT,
		title = "components.redirect.label.to"
	)
   private String redirectTo;

	@Column(name="http_protocol")
	@DataTableColumn(
		inputType = DataTableColumnType.SELECT,
		title = "components.redirect.label.protocol",
		editor = {
			@DataTableColumnEditor(
					options = {
						@DataTableColumnEditorAttr(key = "", value = ""),
						@DataTableColumnEditorAttr(key = "http", value = "http"),
						@DataTableColumnEditorAttr(key = "https", value = "https"),
						@DataTableColumnEditorAttr(key = "alias", value = "alias")
					}
			)
		}
	)
   private String protocol;

   @Column(name="active")
	@DataTableColumn(
		inputType = DataTableColumnType.BOOLEAN,
		title = "components.redirect.label.active",
		defaultValue = "true"
	)
   private boolean active;

   @Column(name="redirect_params")
	@DataTableColumn(
		inputType = DataTableColumnType.BOOLEAN,
		title = "components.redirect.label.params",
		defaultValue = "true"
	)
   private boolean redirectParams;

   @Column(name="redirect_path")
	@DataTableColumn(
		inputType = DataTableColumnType.BOOLEAN,
		title = "components.redirect.label.path",
		defaultValue = "true"
	)
   private boolean redirectPath;

   public boolean isRedirectPath()
	{
		return redirectPath;
	}

	public void setRedirectPath(boolean redirectPath)
	{
		this.redirectPath = redirectPath;
	}

	public Integer getRedirectId() {
		return redirectId;
	}

	public void setRedirectId(Integer redirectId) {
		this.redirectId = redirectId;
	}

	public boolean getActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getRedirectFrom() {
		return redirectFrom;
	}

	public void setRedirectFrom(String redirectFrom) {
		this.redirectFrom = redirectFrom;
	}

	public String getRedirectTo() {
		return redirectTo;
	}

	public void setRedirectTo(String redirectTo) {
		this.redirectTo = Tools.trim(redirectTo);
	}


	public boolean getRedirectParams() {
		return redirectParams;
	}

	public void setRedirectParams(boolean redirectParams) {
		this.redirectParams = redirectParams;
	}

	public DomainRedirectBean(){

	}

	public DomainRedirectBean(int redirectId, boolean active, String redirectFrom,
			String redirectTo, boolean redirectSource, boolean redirectParams, boolean redirectPath, String protocol) {
		super();
		this.redirectId = redirectId;
		this.active = active;
		this.redirectFrom = redirectFrom;
		this.redirectTo = redirectTo;
		this.redirectParams = redirectParams;
		this.redirectPath = redirectPath;
		this.protocol = protocol;
	}

	@Override
	 public String toString() {
	   return new StringBuilder().append("Redirect = { redirectFrom: ").append(getRedirectFrom()).append(", redirectTo: ").append(getRedirectTo()).append(" ,redirectId: ").append(getRedirectId()).append(", redirectPath: ").append(isRedirectPath()).append(" , redirectParams: ").append(getRedirectParams()).append(" , active: ").append(getActive()).append(" , protocol: ").append(getProtocol()).toString();
	 }

	public String getProtocol()
	{
		return protocol;
	}

	public void setProtocol(String protocol)
	{
		this.protocol = protocol;
	}


}
