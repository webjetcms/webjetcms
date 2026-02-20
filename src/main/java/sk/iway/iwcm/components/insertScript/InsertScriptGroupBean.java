package sk.iway.iwcm.components.insertScript;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.adminlog.EntityListenersType;

import jakarta.persistence.*;
import java.io.Serializable;


/**
 *  InsertScriptGroupBean.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 14.09.2016 15:04:15
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@Entity
@Table(name="insert_script_gr")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_INSERT_SCRIPT)
public class InsertScriptGroupBean extends ActiveRecordRepository implements Serializable
{
	@JsonIgnore
	private static final long serialVersionUID = -1L;

	@Id
	@GeneratedValue(generator="WJGen_insert_script_gr")
	@TableGenerator(name="WJGen_insert_script_gr",pkColumnValue="insert_script_gr")
	@Column(name="insert_script_gr_id")
	private Long id;

	@Column(name="group_id")
	int groupId;

	@Column(name="domain_id")
	int domainId;

	@ManyToOne
	@JsonBackReference(value="insertScriptBeanGr")
	@JoinColumn(name="insert_script")
	private InsertScriptBean insertScriptBeanGr;

	public InsertScriptGroupBean() {

    }

    public InsertScriptGroupBean(String path) {
        GroupDetails group = GroupsDB.getInstance().getCreateGroup(path);
        if (group != null) groupId = group.getGroupId();
    }

    @JsonIgnore
	public InsertScriptBean getInsertScript() {
		return insertScriptBeanGr;
	}

	public void setInsertScript(InsertScriptBean insertScriptBean) {
		this.insertScriptBeanGr = insertScriptBean;
	}

	public Long getInsertScriptGrId()
	{
		return id;
	}

	public void setInsertScriptGrId(Long insertScriptGrId)
	{
		this.id = insertScriptGrId;
	}

	@Override
	public void setId(Long id)
	{
		setInsertScriptGrId(id);
	}

	@Override
	public Long getId()
	{
		return getInsertScriptGrId();
	}

	public int getGroupId()
	{
		return groupId;
	}

	public void setGroupId(int groupId)
	{
		this.groupId = groupId;
	}

	public int getDomainId() {
		return domainId;
	}

	public void setDomainId(int domainId) {
		this.domainId = domainId;
	}

	@Override
	public boolean save()
	{
		this.domainId = CloudToolsForCore.getDomainId();
		return super.save();
	}

	/**
     * Vratenie cesty pre vue komponentu
     * @return
     */
	@JsonProperty(access = Access.READ_ONLY)
    public String getFullPath() {
		GroupDetails grp = GroupsDB.getInstance().getGroup(groupId);
		if (grp != null) return grp.getFullPath();
		return "";
	}

	@JsonIgnore
	public String toString() {
		return "InsertScriptGroupBean id="+getId()+" path="+getFullPath();
	}
}
/*
mysql

CREATE TABLE insert_script_gr (
insert_script_gr_id INT NOT NULL PRIMARY KEY,
group_id INT,
insert_script INT);
INSERT INTO pkey_generator VALUES('insert_script_gr', 1 , 'insert_script_gr', 'insert_script_gr_id')

mssql

CREATE TABLE insert_script_gr (
[insert_script_gr_id] [INT] NOT NULL PRIMARY KEY,
group_id [INT],
insert_script [INT]);
INSERT INTO pkey_generator VALUES('insert_script_gr', 1 , 'insert_script_gr', 'insert_script_gr_id')

oracle

CREATE TABLE insert_script_gr (
insert_script_gr_id INT NOT NULL PRIMARY KEY,
group_id INTEGER,
insert_script INTEGER);
INSERT INTO pkey_generator VALUES('insert_script_gr', 1 , 'insert_script_gr', 'insert_script_gr_id')
PKEY:
INSERT INTO pkey_generator VALUES('insert_script_gr', 1 , 'insert_script_gr', 'insert_script_gr_id');
 */
