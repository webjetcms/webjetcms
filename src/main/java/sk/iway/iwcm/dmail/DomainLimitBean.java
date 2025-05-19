package sk.iway.iwcm.dmail;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Transient;
import javax.validation.constraints.NotBlank;

import sk.iway.iwcm.database.ActiveRecord;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

/**
 *  DomainLimitBean.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2013
 *@author       $Author: jeeff mhalas $
 *@version      $Revision: 1.3 $
 *@created      Date: 24.7.2013 14:39:18
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@Entity
@Table(name="domain_limits")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_DMAIL_DOMAINLIMITS )
public class DomainLimitBean extends ActiveRecord implements Serializable
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -5776093287890776273L;

	@Id
	@GeneratedValue(generator="WJGen_domain_limits")
	@TableGenerator(name="WJGen_domain_limits",pkColumnValue="domain_limit_id")
	@Column(name="domain_limit_id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
	private int id;

	@Column(name="domain")
	@NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.dmail.domainlimits.domain"
    )
	private String domain;

    @Column(name="active")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="components.dmail.domainlimits.active"
    )
	private boolean active = true;

	@Column(name="limit_size")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.dmail.domainlimits.limit",
		editor = { @DataTableColumnEditor( attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before")
        } )}
    )
	private int limit = 10;

	@Column(name="time_unit")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="components.dmail.domainlimits.timeUnit",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "NANOSECONDS", value = "NANOSECONDS"),
                    @DataTableColumnEditorAttr(key = "MICROSECONDS", value = "MICROSECONDS"),
                    @DataTableColumnEditorAttr(key = "MILLISECONDS", value = "MILLISECONDS"),
                    @DataTableColumnEditorAttr(key = "SECONDS", value = "SECONDS"),
                    @DataTableColumnEditorAttr(key = "MINUTES", value = "MINUTES"),
                    @DataTableColumnEditorAttr(key = "HOURS", value = "HOURS"),
                    @DataTableColumnEditorAttr(key = "DAYS", value = "DAYS")
                }
            )
        }
    )
	private String timeUnit = "MINUTES";

	//minDelay in millis
	@Column(name="min_delay")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.dmail.domainlimits.minDelay",
		editor = { @DataTableColumnEditor( attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before")
        } )}
    )
	private int minDelay = 5000;

    @Column(name="delay_active")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="components.dmail.domainlimits.delayActive"
    )
	private boolean delayActive = true;

	@Transient
	private transient BaseEditorFields editorFields;

	public boolean isDelayActive()
	{
		return delayActive;
	}

	public void setDelayActive(boolean delayActive)
	{
		this.delayActive = delayActive;
	}

	public int getMinDelay()
	{
		return minDelay;
	}

	public void setMinDelay(int minDelay)
	{
		this.minDelay = minDelay;
	}

	public TimeUnit getTimeUnit()
	{
		return TimeUnit.valueOf(timeUnit);
	}

	public void setTimeUnit(TimeUnit unit)
	{
		this.timeUnit = unit.name();
	}

	public int getDomainLimitId()
	{
		return id;
	}

	public void setDomainLimitId(int domainLimitId)
	{
		this.id = domainLimitId;
	}

	public String getDomain()
	{
		return domain;
	}

	public void setDomain(String domain)
	{
		this.domain = domain;
	}

	public int getLimit()
	{
		return limit;
	}

	public void setLimit(int limit)
	{
		this.limit = limit;
	}

	public boolean isActive()
	{
		return active;
	}

	public void setActive(boolean active)
	{
		this.active = active;
	}

	@Override
	public int getId()
	{
		return getDomainLimitId();
	}

	@Override
	public void setId(int id)
	{
		setDomainLimitId(id);
	}

	@Override
	public boolean save()
	{
		boolean ret = super.save();
		DomainThrottle.getInstance().refresh();
		DomainLimitsDB.getInstance(true);
		return ret;
	}

	@Override
	public boolean delete()
	{
		boolean ret = super.delete();
		DomainThrottle.getInstance().refresh();
		DomainLimitsDB.getInstance(true);
		return ret;
	}

	public BaseEditorFields getEditorFields() {
		return editorFields;
	}

	public void setEditorFields(BaseEditorFields editorFields) {
		this.editorFields = editorFields;
	}
}
