package sk.iway.iwcm.components.insertScript;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.adminlog.EntityListenersType;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 *  InsertScriptDocBean.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2016
 *@author       $Author: jeeff prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 24.10.2016 13:05:32
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@Entity
@Table(name="insert_script_doc")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_INSERT_SCRIPT)
public class InsertScriptDocBean extends ActiveRecordRepository implements Serializable {

	@JsonIgnore
	private static final long serialVersionUID = -1L;

	@Id
	@GeneratedValue(generator="WJGen_insert_script_doc")
	@TableGenerator(name="WJGen_insert_script_doc",pkColumnValue="insert_script_doc")
	@Column(name="insert_script_doc_id")
	private Long id;

	@Column(name="doc_id")
	int docId;

	@ManyToOne
	@JsonBackReference(value="insertScriptBeanDoc")
	@JoinColumn(name="insert_script")
	private InsertScriptBean insertScriptBeanDoc;

    public InsertScriptDocBean() {

    }

    public InsertScriptDocBean(String path) {
        DocDetails doc = DocDB.getInstance().getCreateDoc(path);
        if (doc != null) docId = doc.getDocId();
    }

	@Override
	public void setId(Long id)
	{
		setInsertScriptDocId(id);
	}

	@Override
	public Long getId()
	{
		return getInsertScriptDocId();
	}

	public Long getInsertScriptDocId() {
		return id;
	}

	public void setInsertScriptDocId(Long insertScriptDocId) {
		this.id = insertScriptDocId;
	}

	public int getDocId() {
		return docId;
	}

	public void setDocId(int docId) {
		this.docId = docId;
	}

	@JsonIgnore
	public InsertScriptBean getInsertScript() {
		return insertScriptBeanDoc;
	}

	public void setInsertScript(InsertScriptBean insertScriptBean) {
		this.insertScriptBeanDoc = insertScriptBean;
	}

	/**
     * Vratenie cesty pre vue komponentu
     * @return
     */
	@JsonProperty(access = Access.READ_ONLY)
    public String getFullPath() {
		DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
		if (doc != null) {
			return doc.getFullPath();
		}
		return "";
	}

	@JsonIgnore
	public String toString() {
		return "InsertScriptDocBean id="+getId()+" path="+getFullPath();
	}
}
