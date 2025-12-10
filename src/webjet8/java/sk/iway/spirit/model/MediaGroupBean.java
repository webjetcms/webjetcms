package sk.iway.spirit.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name="media_groups")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_MEDIA_GROUP)
public class MediaGroupBean extends ActiveRecordRepository implements Serializable {

	private static final long serialVersionUID = -1L;

	public MediaGroupBean() {}

	@Id
	@GeneratedValue(generator="WJGen_MediaGroup")
	@TableGenerator(name="WJGen_MediaGroup",pkColumnValue="media_groups")
	@Column(name = "media_group_id")
	@DataTableColumn(inputType = DataTableColumnType.ID)
	private Long id;

	@NotBlank
	@Column(name = "media_group_name")
	@DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, renderFormat = "dt-format-text", title = "editor.perex_group_name", editor = {
        @DataTableColumnEditor(type = "text") })
	private String mediaGroupName;

	@Column(name = "available_groups")
	private String availableGroups;

	@Transient
	@DataTableColumnNested
	private MediaGroupEditorFields editorFields = null;

	@Lob
	@Column(name = "related_pages")
	String relatedPages;

	public int getMediaGroupId() {
		if (id==null) return 0;
		return id.intValue();
	}

	public void setMediaGroupId(int mediaGroupId) {
		this.id = Long.valueOf(mediaGroupId);
	}

	public String getMediaGroupName() {
		return mediaGroupName;
	}

	public void setMediaGroupName(String mediaGroupName) {
		this.mediaGroupName = mediaGroupName;
	}


	public String getAvailableGroups() {
		return availableGroups;
	}

	public void setAvailableGroups(String availableGroups) {
		this.availableGroups = availableGroups;
	}

	public String getRelatedPages() {
		return relatedPages;
	}

	public void setRelatedPages(String relatedPages) {
		this.relatedPages = relatedPages;
	}

	public MediaGroupEditorFields getEditorFields() {
		if(editorFields == null) {
			//call fromMediaGroupBean to inicialize MediaGroupEditorFields.availableGroups
			editorFields = new MediaGroupEditorFields();
			editorFields.fromMediaGroupBean(this);
		}
		return editorFields;
	}

	public void setEditorFields(MediaGroupEditorFields editorFields) {
		if(editorFields != null) {
			//call toMediaGroupBean to return String of group id's, separeted by "," and set this AvailableGroups
			String newGroupIds = editorFields.toMediaGroupBean(this);
			this.setAvailableGroups(newGroupIds);
		}
		this.editorFields = editorFields;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public Long getId() {
		return id;
	}
}