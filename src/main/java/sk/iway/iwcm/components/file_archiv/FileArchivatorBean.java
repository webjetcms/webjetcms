package sk.iway.iwcm.components.file_archiv;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import org.apache.commons.beanutils.PropertyUtils;
import org.json.JSONObject;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.FileIndexerTools;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 *  FileArchivatorBean.java - Sluzi na archivaciu suborov. Mame cestu k suboru a ostatne parametre si drzime v databaze
 */
@Entity
@Table(name="file_archiv")
@Getter
@Setter
public class FileArchivatorBean extends ActiveRecordRepository implements Serializable
{

	@Id
	@GeneratedValue(generator="WJGen_file_archiv")
	@TableGenerator(name="WJGen_file_archiv",pkColumnValue="file_archiv")
	@Column(name="file_archiv_id")
	@DataTableColumn(inputType = DataTableColumnType.ID, tab = "basic")
	private Long id;

	@Column(name="virtual_file_name")
	@DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.file_archiv.virtualFileName",
		tab = "basic"
    )
	@NotBlank
	private String virtualFileName;

	@Column(name="file_path")
	@DataTableColumn(
        inputType = DataTableColumnType.DISABLED,
        title="components.file_archiv.directory",
		tab = "basic",
		className = "hideOnCreate"
    )
	private String filePath;

	@Column(name="file_name")
	@DataTableColumn(
        inputType = DataTableColumnType.DISABLED,
        title="components.gallery.fileName",
		tab = "basic",
		className = "hideOnCreate"
    )
	private String fileName;

	@Column(name="valid_from")
	@Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="inquiry.valid_since",
		tab = "basic"
    )
	private Date validFrom;

	@Column(name="valid_to")
	@Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="inquiry.valid_till",
		tab = "basic"
    )
	private Date validTo;

	@Column(name="product")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.file_archiv.product",
		tab = "advanced",
		editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/file-archive/autocomplete-product"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
	private String product;

	@Column(name="category")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.bazar.category",
		tab = "advanced",
		editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/file-archive/autocomplete-category"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
	private String category;

	@Column(name="product_code")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.file_archiv.kod_produktu",
		tab = "advanced"
    )
	private String productCode;

	@Column(name="show_file")
	@DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title="components.file_archiv.filter.show_file",
		tab = "advanced",
		visible = false
    )
	private Boolean showFile = true;

	@Column(name="index_file")
	@DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title="editor.searchable_enabled", tab="advanced",
		visible = false,
		editor = {
			@DataTableColumnEditor(
				options = {
					@DataTableColumnEditorAttr(key = "editor.searchable_enabled_label", value = "true")
				}
			)
		}
	)
	Boolean indexFile;

	@Column(name="priority")
	@DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.banner.priority",
		tab = "advanced"
    )
	private Integer priority;

	@Column(name="order_id")
	@DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="editor.sort_order",
		visible = false,
		hiddenEditor = true
    )
	private Integer orderId;

	@Column(name="reference_to_main")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.file_archiv.main_file",
		tab = "advanced",
		visible = false,
		editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/file-archive/autocomplete"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
					@DataTableColumnEditorAttr(key = "data-ac-params", value = "#DTE_Field_filePath,#DTE_Field_fileName"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
	private String referenceToMain;

	@Column(name="reference_id")
	@DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.file_archiv.reference",
		tab = "basic",
		className = "hideOnCreate hideOnEdit",
		visible = false
    )
	private Long referenceId;

	@Column(name="note")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="components.file_archiv.note",
		tab = "advanced",
		visible = false
    )
	private String note;

	@Column(name="date_upload_later")
	@Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        tab = "basic",
        title="components.file_archiv.file_list_cakajuce.nahrat_po",
        visible = false,
		sortAfter = "validTo",
		className = "hideOnCreate"
    )
	private Date dateUploadLater;

	@Column(name="emails")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        tab = "basic",
        title="components.file_archiv.emails",
        visible = false,
		className = "hideOnCreate"
    )
	private String emails;

	@Transient
	@DataTableColumnNested
	private transient FileArchivatorEditorFields editorFields = null;

	@Column(name="field_a")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.file_archiv.field_a",
		visible = false,
		tab = "customFields"
    )
	private String fieldA;

	@Column(name="field_b")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.file_archiv.field_b",
		visible = false,
		tab = "customFields"
    )
	private String fieldB;

	@Column(name="field_c")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.file_archiv.field_c",
		visible = false,
		tab = "customFields"
    )
	private Integer fieldC;

	@Column(name="field_d")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.file_archiv.field_d",
		visible = false,
		tab = "customFields"
    )
	private Long fieldD;

	@Column(name="field_e")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.file_archiv.field_e",
		visible = false,
		tab = "customFields"
    )
	private String fieldE;


	@Column(name="user_id")
	private Integer userId;

	@Column(name="date_insert")
	@Temporal(TemporalType.TIMESTAMP)
	private Date dateInsert;

	@Column(name="domain")
	private String domain;

	@Column(name="md5")
	private String md5;

	@Column(name="global_id")
	private Integer globalId;

	@DataTableColumn(inputType = DataTableColumnType.HIDDEN)
	@Column(name="uploaded")
	private Integer uploaded;

	@Column(name="file_size")
	private Long fileSize;

	@Column(name="domain_id")
	private Integer domainId;

	@Column(name="extended_data_id")
	private Long extendedDataId;

	public FileArchivatorBean()
	{
		uploaded = -1;
		referenceId = Long.valueOf(-1);
		orderId = -1;
		indexFile = true;
	}

	/**
	 * Vrati true ak su platne datumy (alebo su null) pre aktualny cas
	 */
	public boolean isValidDates()
	{
		if (validFrom == null && validTo == null) return true;

		Date now = new Date(Tools.getNow());
		if (validFrom != null && validFrom.after(now)) return false;
		if (validTo != null && validTo.before(now)) return false;

		return true;
	}

	public int getFileArchiveId()
	{
		if (id==null) return -1;
		return getId().intValue();
	}

	public void setFileArchiveId(int fileArchiveId)
	{
		setId(Long.valueOf(fileArchiveId));
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

	public Long saveAndReturnId() {
		save();
		return getId();
	}

	public boolean saveWithDebugLog(Class<?> c, String methodName) {
		boolean result = save();

		if(result == false) {
			StringBuilder msg = new StringBuilder();
			msg.append(c.getName()).append(" ").append(methodName).append(" ").append("FileArchivatorBean save failed : ").append(this.toString());
			Logger.debug(c, msg.toString());
		}

		return result;
	}

	@Override
	public boolean save()
	{
		if(dateInsert == null)
			dateInsert = new Date();
		//musim tu vymazat cache
		FileArchivatorKit.deleteFileArchiveCache();

		//If we are not saving in the future, emails are not needed
		if(dateUploadLater == null)
			emails = null;

		//!! Beacause OLD logic requiures it save path without start slah
		if(filePath != null && filePath.startsWith(FileArchivSupportMethodsService.SEPARATOR))
			filePath = filePath.substring(1);

		boolean save = super.save();

		if(save) {
			//ak je subor validny a je ako zobrazovat
			if(Tools.isTrue(getShowFile()) && isValidDates()) {
				UserDetails user = UsersDB.getUser(userId);
			 	//indexujem bud iba hlavny subor, alebo ked je povolene, tak vsetky
			 	boolean index = getReferenceId().longValue() < 1 || Constants.getBoolean("fileArchivIndexOnlyMainFiles") == false;
				if(index && Tools.isTrue(indexFile))
					FileIndexerTools.indexFile(FileArchivSupportMethodsService.SEPARATOR + getVirtualPath(), user);
				else {
					//remove index file if exist
					FileIndexerTools.deleteIndexedFile(FileArchivSupportMethodsService.SEPARATOR + getVirtualPath());
				}
			}
		}
		return save;
	}

	@Override
	public boolean delete()
	{
		//musim tu vymazat cache
		FileArchivatorKit.deleteFileArchiveCache();

		//zmazem index
		FileIndexerTools.deleteIndexedFile(FileArchivSupportMethodsService.SEPARATOR + getVirtualPath());

      	Adminlog.add(Adminlog.TYPE_FILE_ARCHIVE, "DELETE: File Archiv mazeme subor: "+getVirtualPath()+" "+this, this.getFileArchiveId(), -1);
		return super.delete();
	}

	@Override
	public String toString()
	{
		return toString(false);
	}

	public String toString(boolean newLines)
	{
		StringBuilder toString = new StringBuilder();
		for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(this.getClass()))
		{
			try{
				String property = descriptor.getName();
				if("historyFiles".equals(property))
				    continue;
				Object value = PropertyUtils.getProperty(this, property);
				toString.append(property).append(": ").append(value);

				if (newLines) toString.append("\n");
				else toString.append(", ");
			}
			catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return toString.toString();
	}

	public String getVirtualPath() {
        return getFilePath() + getFileName();
    }

	public String toStringForOption() {
		JSONObject fabObj = new JSONObject();
		fabObj.put("id", this.getId());

		if(this.referenceId.longValue() == -1) {
			fabObj.put("order", 1);
			fabObj.put("path", Prop.getInstance().getText("components.file_archiv.main_file"));
		} else {
			fabObj.put("order", this.getOrderId());
			fabObj.put("path", this.getVirtualPath());
		}

		return fabObj.toString();
	}

	public String getRealPath() {
		return Tools.getRealPath(getVirtualPath());
	}

	public String getFileSizeFormatted() {
		if (fileSize == null) return "0 B";
		return Tools.formatFileSize(fileSize);
	}

	/**
	 * @deprecated use getFileSizeFormatted() instead
	 */
	@Deprecated
	public long getFileSizeKB() {
		if (fileSize == null) return 0;
		return fileSize / 1024;
	}

	/**
	 * @deprecated use getFileSizeFormatted() instead
	 */
	@Deprecated
	public double getFileSizeMB() {
		if (fileSize == null) return 0;
		return getFileSizeKB() / 1024d;
	}

	public String getFileType() {
		if(Tools.isEmpty(getFileName())) return "";
		String[] parts = getFileName().split("\\.");
		if(parts.length > 1)
			return parts[parts.length - 1].toUpperCase();
		return "";
	}

	public Integer getUploaded() { return uploaded == null ? -1 : uploaded; }
	public Long getReferenceId() { return referenceId == null ? -1 : referenceId; }
	public void setReferenceId(Long referenceId) {
		if(referenceId == null) this.referenceId = Long.valueOf(-1);
		else this.referenceId = referenceId;
	}

	public void setReferenceId(int referenceId) {
		this.referenceId = Long.valueOf(referenceId);
	}

	public Integer getOrderId() { return orderId == null ? -1 : orderId; }
	public Integer getPriority() { return priority == null ? 0 : priority; }
	public Boolean getShowFile() { return Tools.isTrue(showFile); }
	public Boolean getIndexFile() { return Tools.isTrue(indexFile); }
}