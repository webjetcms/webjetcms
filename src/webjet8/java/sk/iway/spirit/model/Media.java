package sk.iway.spirit.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

/**
 * JPA bean pre zaznam z tabulky media
 *  Media.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 21.7.2011 9:27:20
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@Entity
@Table(name="media")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_MEDIA)
public class Media extends ActiveRecordRepository implements Serializable
{
	private static final long serialVersionUID = -1L;

	@Id
	@GeneratedValue(generator="WJGen_media")
	@TableGenerator(name="WJGen_media",pkColumnValue="media")
	@Column(name="media_id")
	@DataTableColumn(inputType = DataTableColumnType.ID)
	private Long id;

	@NotBlank
	@Column(name="media_title_sk")
	@DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "editor.media.title", sortAfter = "mediaLink", tab = "basic",
	editor = {
        @DataTableColumnEditor(type = "text") })
	String mediaTitleSk;

	@Column(name="media_sort_order")
	@DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "editor.media.sortOrder", sortAfter = "mediaThumbLink", tab = "basic",
	editor = {
        @DataTableColumnEditor(type = "text") })
	Integer mediaSortOrder;

	@Column(name="media_thumb_link")
	@DataTableColumn(inputType = DataTableColumnType.ELFINDER, title = "editor.perex.image", className = "image", renderFormat = "dt-format-image", sortAfter = "editorFields.groups", tab = "basic")
	String mediaThumbLink;

	@JsonIgnore
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "media_group_to_media",
			joinColumns = { @JoinColumn(name = "media_id", referencedColumnName = "media_id") },
			inverseJoinColumns = { @JoinColumn(name = "media_group_id", referencedColumnName = "media_group_id"),  })
	private List<MediaGroupBean> groups;

	@Column(name="media_link")
	@DataTableColumn(inputType = DataTableColumnType.ELFINDER, title = "editor.media.link", renderFormat = "dt-format-link", tab = "basic")
	String mediaLink;

	@Column(name="media_fk_table_name")
	String mediaFkTableName;

	@JsonIgnore
	@Column(name="media_group")
	String mediaGroup;

	@Column(name="media_fk_id")
	Integer mediaFkId;

	@JsonIgnore
	@Lob
	@Column(name="media_info_cz")
	String mediaInfoCz;

	@JsonIgnore
	@Lob
	@Column(name="media_info_de")
	String mediaInfoDe;

	@JsonIgnore
	@Lob
	@Column(name="media_info_en")
	String mediaInfoEn;

	@JsonIgnore
	@Lob
	@Column(name="media_info_sk")
	String mediaInfoSk;

	@JsonIgnore
	@Column(name="media_title_cz")
	String mediaTitleCz;

	@JsonIgnore
	@Column(name="media_title_de")
	String mediaTitleDe;

	@JsonIgnore
	@Column(name="media_title_en")
	String mediaTitleEn;

	@Column(name="last_update")
	@Temporal(TemporalType.TIMESTAMP)
	Date lastUpdate;

	@Column(name = "field_a")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.media.field_a",
		visible = false,
		tab = "fields"
    )
	private String fieldA;

	@Column(name = "field_b")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.media.field_b",
		visible = false,
		tab = "fields"
    )
	private String fieldB;

	@Column(name = "field_c")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.media.field_c",
		visible = false,
		tab = "fields"
    )
	private String fieldC;

	@Column(name = "field_d")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.media.field_d",
		visible = false,
		tab = "fields"
    )
	private String fieldD;

	@Column(name = "field_e")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.media.field_e",
		visible = false,
		tab = "fields"
    )
	private String fieldE;

	@Column(name = "field_f")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.media.field_f",
		visible = false,
		tab = "fields"
    )
	private String fieldF;

	@Transient
	@DataTableColumnNested
	private MediaEditorFields editorFields = null;

	@Column(name = "domain_id")
	@DataTableColumn(inputType = DataTableColumnType.HIDDEN)
	private Integer domainId;

	public List<MediaGroupBean> getGroups() {
		return groups;
	}

	public void setGroups(List<MediaGroupBean> groups) {
		this.groups = groups;
	}

	public int getMediaId()
	{
		if (id == null) return 0;
		return id.intValue();
	}

	public void setMediaId(int mediaId)
	{
		this.id = Long.valueOf(mediaId);
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

	public Integer getMediaFkId()
	{
		return mediaFkId;
	}

	public void setMediaFkId(Integer mediaFkId)
	{
		this.mediaFkId = mediaFkId;
	}

	public String getMediaFkTableName()
	{
		return mediaFkTableName;
	}

	public void setMediaFkTableName(String mediaFkTableName)
	{
		this.mediaFkTableName = mediaFkTableName;
	}

	public String getMediaGroup()
	{
		return mediaGroup;
	}

	public void setMediaGroup(String mediaGroup)
	{
		this.mediaGroup = mediaGroup;
	}

	public String getMediaInfoCz()
	{
		return mediaInfoCz;
	}

	public void setMediaInfoCz(String mediaInfoCz)
	{
		this.mediaInfoCz = mediaInfoCz;
	}

	public String getMediaInfoDe()
	{
		return mediaInfoDe;
	}

	public void setMediaInfoDe(String mediaInfoDe)
	{
		this.mediaInfoDe = mediaInfoDe;
	}

	public String getMediaInfoEn()
	{
		return mediaInfoEn;
	}

	public void setMediaInfoEn(String mediaInfoEn)
	{
		this.mediaInfoEn = mediaInfoEn;
	}

	public String getMediaInfoSk()
	{
		return mediaInfoSk;
	}

	public void setMediaInfoSk(String mediaInfoSk)
	{
		this.mediaInfoSk = mediaInfoSk;
	}

	public String getMediaLink()
	{
		return mediaLink;
	}

	public void setMediaLink(String mediaLink)
	{
		this.mediaLink = mediaLink;
	}

	public Integer getMediaSortOrder()
	{
		//if (mediaSortOrder==null) return Integer.valueOf(0);
		return mediaSortOrder;
	}

	public void setMediaSortOrder(Integer mediaSortOrder)
	{
		this.mediaSortOrder = mediaSortOrder;
	}

	public String getMediaThumbLink()
	{
		return mediaThumbLink;
	}

	public void setMediaThumbLink(String mediaThumbLink)
	{
		this.mediaThumbLink = mediaThumbLink;
	}

	public String getMediaTitleCz()
	{
		return mediaTitleCz;
	}

	public void setMediaTitleCz(String mediaTitleCz)
	{
		this.mediaTitleCz = mediaTitleCz;
	}

	public String getMediaTitleDe()
	{
		return mediaTitleDe;
	}

	public void setMediaTitleDe(String mediaTitleDe)
	{
		this.mediaTitleDe = mediaTitleDe;
	}

	public String getMediaTitleEn()
	{
		return mediaTitleEn;
	}

	public void setMediaTitleEn(String mediaTitleEn)
	{
		this.mediaTitleEn = mediaTitleEn;
	}

	public String getMediaTitleSk()
	{
		return mediaTitleSk;
	}

	public void setMediaTitleSk(String mediaTitleSk)
	{
		this.mediaTitleSk = mediaTitleSk;
	}

	public Date getLastUpdate()
	{
		if (lastUpdate == null) return null;
		return new Date(lastUpdate.getTime());
	}

	@JsonIgnore
	public String getGroupsToString(){
		String groupsString = "";
		if (groups==null || groups.size()<1) return groupsString;
		for(int groupIt=0; groupIt<groups.size();groupIt++ ){
			MediaGroupBean group = groups.get(groupIt);
			groupsString+=group.getMediaGroupName();
			if(groupIt != groups.size()-1){
				groupsString+=", ";
			}
		}
		return groupsString;
	}
	public void addGroup(MediaGroupBean group){
	    if (groups == null) {
	        groups = new ArrayList<>();
        }
		for(MediaGroupBean g:groups){
			if(g.getMediaGroupId() == group.getMediaGroupId()) return;
		}
		groups.add(group);
	}

	public void setLastUpdate(Date lastUpdate)
	{
	    if (lastUpdate==null) this.lastUpdate = null;
		else this.lastUpdate = new Date(lastUpdate.getTime());
	}

	public MediaEditorFields getEditorFields() {
		if(editorFields == null) {
			//make MediaEditorFields instance and call fromMedia to initialize MediaEditorFields.groups
			editorFields = new MediaEditorFields();
			editorFields.fromMedia(this);
		}
		return editorFields;
	}

	public void setEditorFields(MediaEditorFields editorFields) {
		this.editorFields = editorFields;
	}

	//Set entity domain id
	@PrePersist
	public void prePersist() {
		if(domainId == null) domainId = CloudToolsForCore.getDomainId();
	}

	public Integer getDomainId() {
		return domainId;
	}

	public void setDomainId(Integer domainId) {
		this.domainId = domainId;
	}

	public String getFieldA() {
		return fieldA;
	}

	public void setFieldA(String fieldA) {
		this.fieldA = fieldA;
	}

	public String getFieldB() {
		return fieldB;
	}

	public void setFieldB(String fieldB) {
		this.fieldB = fieldB;
	}

	public String getFieldC() {
		return fieldC;
	}

	public void setFieldC(String fieldC) {
		this.fieldC = fieldC;
	}

	public String getFieldD() {
		return fieldD;
	}

	public void setFieldD(String fieldD) {
		this.fieldD = fieldD;
	}

	public String getFieldE() {
		return fieldE;
	}

	public void setFieldE(String fieldE) {
		this.fieldE = fieldE;
	}

	public String getFieldF() {
		return fieldF;
	}

	public void setFieldF(String fieldF) {
		this.fieldF = fieldF;
	}


}



