package sk.iway.iwcm.components.dictionary.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;

import sk.iway.iwcm.database.ActiveRecord;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name="dictionary")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_TOOLTIP)
public class DictionaryBean extends ActiveRecord implements Serializable
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -1016135704649424129L;

	@Id
	@GeneratedValue(generator="WJGen_dictionary")
	@TableGenerator(name="WJGen_dictionary", pkColumnValue="dictionary")
	@Column(name="dictionary_id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
	private int dictionaryId;

	@Column(name="dictionary_group")
	private String dictionaryGroup;

	@Column(name="name")
    @NotBlank
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title="[[#{components.tooltip.name}]]")
	private String name;

	@Column(name="name_orig")
	private String nameOrig;

	/**** TOOLTIPS  *****/
	@Column(name="language")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="[[#{components.news.language_version}]]"
    )
	private String language;

	@Column(name="domain")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="[[#{components.news.domain}]]"
    )
	private String domain;
	/********************/

	@Lob
    @Column(name="value")
	@NotBlank
    @DataTableColumn(inputType = DataTableColumnType.QUILL, title="[[#{components.htmlbox.basic}]]")
	private String value;

	public int getId() { return getDictionaryId(); }

	public void setId(int dictionaryId)
	{
		setDictionaryId(dictionaryId);
	}

	public int getDictionaryId()
	{
		return dictionaryId;
	}

	public void setDictionaryId(int dictionaryId)
	{
		this.dictionaryId = dictionaryId;
	}

	public String getDictionaryGroup()
	{
		return dictionaryGroup;
	}

	public void setDictionaryGroup(String dictionaryGroup)
	{
		this.dictionaryGroup = dictionaryGroup;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getNameOrig()
	{
		return nameOrig;
	}

	public void setNameOrig(String nameOrig)
	{
		this.nameOrig = nameOrig;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getLanguage()
	{
		return language;
	}

	public void setLanguage(String language)
	{
		this.language = language;
	}

	public String getDomain()
	{
		return domain;
	}

	public void setDomain(String domain)
	{
		this.domain = domain;
	}




	/**
	 * Vrati html link na toto slovo v slovniku
	 * @return
	 */
    @JsonIgnore //Because seLink doesnt exist we must use JsonIgnore or auto test will failed
	public String getLink()
	{
		return new StringBuilder("<a class='dictionaryLink' onmouseover='!INCLUDE(/components/dictionary/mouseover.jsp,dictionaryId=").append(this.getDictionaryId()).append(" )!' onmouseout='hideDictionaryTooltip();' > ").append(this.getName()).append("</a>").toString();
	}
}