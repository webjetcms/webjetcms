package sk.iway.iwcm.editor;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.database.ActiveRecord;

/**
 *  DocNote.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2017
 *@author       $Author: jeeff rzapach $
 *@version      $Revision: 1.3 $
 *@created      Date: 12.4.2017 10:11:50
 *@modified     $Date: 2004/08/16 06:26:11 $
 */

@Entity
@Table(name="document_notes")
public class DocNoteBean extends ActiveRecord
{
	@Id
	@GeneratedValue(generator="WJGen_document_notes")
	@TableGenerator(name="WJGen_document_notes",pkColumnValue="document_notes")	
	@Column(name="id")
	int id;
	@Column(name="doc_id")
	int docId;
	@Column(name="history_id")
	int historyId;
	@Column(name="note")
	String note;
	@Column(name="user_id")
	int userId;
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="created")
	Date created;
	
	public int getId()
	{
		return id;
	}
	public void setId(int id)
	{
		this.id = id;
	}
	public int getDocId()
	{
		return docId;
	}
	public void setDocId(int docId)
	{
		this.docId = docId;
	}
	public int getHistoryId()
	{
		return historyId;
	}
	public void setHistoryId(int historyId)
	{
		this.historyId = historyId;
	}
	public String getNote()
	{
		return note;
	}
	public void setNote(String note)
	{
		this.note = DB.prepareString(note, 255);
	}
	public int getUserId()
	{
		return userId;
	}
	public void setUserId(int userId)
	{
		this.userId = userId;
	}
	public Date getCreated()
	{
		return created;
	}
	public void setCreated(Date created)
	{
		this.created = created;
	}
}
