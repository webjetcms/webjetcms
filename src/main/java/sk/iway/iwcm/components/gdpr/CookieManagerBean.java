package sk.iway.iwcm.components.gdpr;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotBlank;

import sk.iway.iwcm.database.ActiveRecord;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;


/**
 *  CookieManagerBean.java - #23881 Modul pre spravu cookies banneru
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 19.05.2018 10:43:50
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@Entity
@Table(name="cookies")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_GDPR_COOKIES)
public class CookieManagerBean extends ActiveRecord implements Serializable
{
	private static final long serialVersionUID = -1L;

	@Id
	@GeneratedValue(generator="WJGen_cookies")
	@TableGenerator(name="WJGen_cookies",pkColumnValue="cookies")
	@Column(name="cookie_id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
	private int cookieId;

    @Column(name="cookie_name")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.cookies.cookie_manager.cookie_name",
		tab = "basic"
    )
	@NotBlank
	String cookieName;

    @Column(name="classification")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="components.cookies.cookie_manager.classification",
		tab = "basic"
    )
	String classification;

    @Column(name="provider")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="components.cookies.cookie_manager.provider",
        tab = "advanced"
    )
	String provider;

    @Column(name="purpouse")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="components.cookies.cookie_manager.purpouse",
        tab = "advanced"
    )
	String purpouse;

    @Column(name="validity")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.cookies.cookie_manager.validity",
        tab = "advanced"
    )
	String validity;

    @Column(name="type")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="components.cookies.cookie_manager.type",
		tab = "basic",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "http", value = "http"),
                    @DataTableColumnEditorAttr(key = "html", value = "html"),
                    @DataTableColumnEditorAttr(key = "pixel", value = "pixel"),
                }
            )
        }
    )
	String type;

    @Column(name="application")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.cookies.cookie_manager.application",
		tab = "basic"
    )
	private String application;

    @Column(name="typical_value")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.cookies.cookie_manager.typicalValue",
		tab = "basic"
    )
	private String typicalValue;

    @Column(name="party_3rd")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="components.cookies.cookie_manager.party3rd",
		tab = "basic"
    )
	private boolean party3rd;

    @Column(name="save_date")
	@Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.date.last_update",
        hiddenEditor = true
    )
	Date saveDate;

	@Column(name="domain_id")
	int domainId;

	@Column(name="user_id")
	int userId;

	@Column(name="description")
	String description;

	public int getcookieId()
	{
		return cookieId;
	}

	public void setcookieId(int cookieId)
	{
		this.cookieId = cookieId;
	}

	@Override
	public void setId(int id)
	{
		setcookieId(id);
	}

	@Override
	public int getId()
	{
		return getcookieId();
	}

	public int getDomainId()
	{
		return domainId;
	}

	public void setDomainId(int domainId)
	{
		this.domainId = domainId;
	}

	public int getUserId()
	{
		return userId;
	}

	public void setUserId(int userId)
	{
		this.userId = userId;
	}

	public Date getSaveDate()
	{
		return saveDate;
	}

	public void setSaveDate(Date saveDate)
	{
		this.saveDate = saveDate;
	}

	public String getCookieName()
	{
		return cookieName;
	}

	public void setCookieName(String cookieName)
	{
		this.cookieName = cookieName;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getProvider()
	{
		return provider;
	}

	public void setProvider(String provider)
	{
		this.provider = provider;
	}

	public String getPurpouse()
	{
		return purpouse;
	}

	public void setPurpouse(String purpouse)
	{
		this.purpouse = purpouse;
	}

	public String getValidity()
	{
		return validity;
	}

	public void setValidity(String validity)
	{
		this.validity = validity;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getClassification()
	{
		return classification;
	}

	public void setClassification(String classification)
	{
		this.classification = classification;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getTypicalValue() {
		return typicalValue;
	}

	public void setTypicalValue(String typicalValue) {
		this.typicalValue = typicalValue;
	}

	public boolean isParty3rd() {
		return party3rd;
	}

	public void setParty3rd(boolean party3rd) {
		this.party3rd = party3rd;
	}
}