package sk.iway.iwcm.components.insertScript; //NOSONAR

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;


/**
 *  InsertScriptBean.java - >>>POPIS MA<<<<
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 14.09.2016 14:46:13
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@Entity
@Table(name="insert_script")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_INSERT_SCRIPT)
public class InsertScriptBean extends ActiveRecordRepository implements Serializable
{
    @JsonIgnore
    private static final long serialVersionUID = -1L;

    @Id
    @GeneratedValue(generator="WJGen_insert_script")
	@TableGenerator(name="WJGen_insert_script",pkColumnValue="insert_script")
    @Column(name="insert_script_id")
    @DataTableColumn(inputType=DataTableColumnType.ID)
    private Long id;

    @Column(name="name")
    @NotBlank
    @DataTableColumn(inputType=DataTableColumnType.OPEN_EDITOR, tab="main")
    String name;

    @Column(name="position")
    @NotBlank
    @DataTableColumn(
        inputType=DataTableColumnType.TEXT,
        tab="main",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/components/insert-script/positions"),
                    @DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
    String position;

    @Column(name="cookie_class")
    @NotBlank
    @DataTableColumn(inputType=DataTableColumnType.SELECT, tab="main")
    String cookieClass;

    @Column(name="valid_from")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(inputType=DataTableColumnType.DATETIME, tab="scriptPerms", title="user.admin.allowLoginStart")
    Date validFrom;

    @Column(name="valid_to")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(inputType=DataTableColumnType.DATETIME, tab="scriptPerms", title="user.admin.allowLoginEnd")
    Date validTo;

    @JsonManagedReference(value="insertScriptBeanGr")
    @OneToMany(mappedBy="insertScriptBeanGr",fetch=FetchType.LAZY,cascade={CascadeType.ALL},orphanRemoval=true)
    @DataTableColumn(inputType=DataTableColumnType.JSON, tab="scriptPerms", title="grouptree.title", className="dt-tree-group-array",
        editor = { @DataTableColumnEditor( attr = {
            @DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "editor.json.addGroup")
        } )}
    )
    List<InsertScriptGroupBean> groupIds; //NOSONAR

    @JsonManagedReference(value="insertScriptBeanDoc")
    @OneToMany(mappedBy="insertScriptBeanDoc",fetch=FetchType.LAZY,cascade={CascadeType.ALL},orphanRemoval=true)
    @DataTableColumn(inputType=DataTableColumnType.JSON, tab="scriptPerms", title="components.insert_script.choose_pages", className="dt-tree-page-array",
        editor = { @DataTableColumnEditor( attr = {
            @DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "editor.json.addPage")
        } )}
    )
    List<InsertScriptDocBean> docIds; //NOSONAR

    @Lob
    @Column(name="script_body")
    @Convert(converter = AllowHtmlAttributeConverter.class)
    @DataTableColumn(inputType=DataTableColumnType.TEXTAREA, tab="scriptBody", title="components.insert_script.body", className = "textarea-code show-html")
    String scriptBody;

    @Column(name="save_date")
    @Temporal(TemporalType.TIMESTAMP)
    Date saveDate;

    @Column(name="user_id")
    int user;

    @Column(name="domain_id")
    int domainId;

    public InsertScriptBean() {
        super();
        validFrom = null;
        validTo = null;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId()
    {
        return id;
    }

    public int getInsertScriptId()
    {
        if (id==null) return 0;
        return getId().intValue();
    }

    public void setInsertScriptId(int insertScriptId)
    {
        setId(Long.valueOf(insertScriptId));
    }

    public int getUser()
    {
        return user;
    }

    public void setUser(int user)
    {
        this.user = user;
    }

    public String getScriptBody()
    {
        return scriptBody;
    }

    public void setScriptBody(String scriptBody)
    {
        this.scriptBody = scriptBody;
    }

    public String getPosition()
    {
        return position;
    }

    public void setPosition(String position)
    {
        this.position = position;
    }

    public Date getSaveDate() {
        return saveDate;
    }

    public void setSaveDate(Date saveDate) {
        this.saveDate = saveDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public List<InsertScriptGroupBean> getGroupIds() {
        return groupIds;
    }

    //@JsonManagedReference(value="insertScriptBeanGr")
    public void setGroupIds(List<InsertScriptGroupBean> groupIds) {
        this.groupIds = groupIds;
    }

    public List<InsertScriptDocBean> getDocIds() {
        return docIds;
    }

    //@JsonManagedReference(value="insertScriptBeanDoc")
    public void setDocIds(List<InsertScriptDocBean> docIds) {
        this.docIds = docIds;
    }

    @Override
    public boolean save()
    {
        setSaveDate(new Date());
        InsertScriptDB.deleteCache();
        return super.save();
    }

    @Override
    public boolean delete()
    {
        InsertScriptDB.deleteCache();
        return super.delete();
    }

    public int getDomainId() {
        return domainId;
    }

    public void setDomainId(int domainId) {
        this.domainId = domainId;
    }

    public String getCookieClass() {
        return cookieClass;
    }

    public void setCookieClass(String cookieClass) {
        this.cookieClass = cookieClass;
    }

    public List<String> getGroupsAndDocs() {
        List<String> groups = null;

        try {
            groups = getGroupIds().stream().map(g ->
                GroupsDB.getInstance().getGroupNamePath(g.groupId) + " (" + g.groupId + ")"
            ).collect(Collectors.toList());
        } catch (Exception e) {

        }

        List<String> docs = null;
        try {
            docs = getDocIds().stream().map(d -> {
                DocDetails doc = DocDB.getInstance().getDoc(d.docId);
                try {
                    return doc.getVirtualPath() + " (" + doc.getDocId() + ")";
                } catch(Exception e) {
                    return "";
                }

            }).collect(Collectors.toList());
        } catch (Exception e) {

        }

        List<String> result = new ArrayList<>();
        if(groups != null) {
            result.addAll(groups);
        }
        if(docs != null) {
            result.addAll(docs);
        }

        return result;
    }
}
/*

!!!!!
alter table insert_script add cookie_class NVARCHAR2(255);
!!!!!

GRANT SELECT, UPDATE, INSERT, DELETE ON INSERT_SCRIPT TO NN_WEB_PUBLIC;
GRANT SELECT, UPDATE, INSERT, DELETE ON INSERT_SCRIPT_DOC TO NN_WEB_PUBLIC;
GRANT SELECT, UPDATE, INSERT, DELETE ON INSERT_SCRIPT_GR TO NN_WEB_PUBLIC;

GRANT SELECT, UPDATE, INSERT, DELETE ON insert_script TO NN_WEB_PUBLIC;
GRANT SELECT, UPDATE, INSERT, DELETE ON insert_script_doc TO NN_WEB_PUBLIC;
GRANT SELECT, UPDATE, INSERT, DELETE ON insert_script_gr TO NN_WEB_PUBLIC;

mysql
java.sql.SQLSyntaxErrorException: ORA-00900: neplatnďż˝ prďż˝kaz SQL

CREATE TABLE insert_script (
insert_script_id INT NOT NULL PRIMARY KEY,
create_date DATETIME,
user INT,
script_body VARCHAR(255),
position INT);
INSERT INTO pkey_generator VALUES('insert_script', 1 , 'insert_script', 'insert_script_id')

mssql
java.sql.SQLSyntaxErrorException: ORA-00900: neplatnďż˝ prďż˝kaz SQL

CREATE TABLE insert_script (
[insert_script_id] [INT] NOT NULL PRIMARY KEY,
create_date [DATETIME],
user [INT],
script_body [NVARCHAR](255),
position [INT]);
INSERT INTO pkey_generator VALUES('insert_script', 1 , 'insert_script', 'insert_script_id')

oracle
java.sql.SQLSyntaxErrorException: ORA-00900: neplatnďż˝ prďż˝kaz SQL

CREATE TABLE insert_script (
insert_script_id INT NOT NULL PRIMARY KEY,
create_date DATE,
user INTEGER,
script_body NVARCHAR2(255),
position INTEGER);
INSERT INTO pkey_generator VALUES('insert_script', 1 , 'insert_script', 'insert_script_id')
PKEY:
INSERT INTO pkey_generator VALUES('insert_script', 1 , 'insert_script', 'insert_script_id');
*/
