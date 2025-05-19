package sk.iway.iwcm.components.todo;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ActiveRecord;

/**
 * ToDoBean.java - Co treba urobit, Rozlisujeme ulohy pre prihlaseneho usera a
 * ulohy vseobecne pomocou atributu isGlobal. Vyriesene a nevyriesene ulohy
 * rozlisujeme podla atributy isResolved.
 *
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2010
 * @author $Author: mkolejak $
 * @version $Revision: 1.3 $
 * @created Date: 01.07.2014 14:54:26
 * @modified $Date: 2004/08/16 06:26:11 $
 */

@Entity
@Table(name = "todo")
public class ToDoBean extends ActiveRecord implements Serializable
{
	private static final long serialVersionUID = -1L;
	@Id
	@GeneratedValue(generator = "WJGen_todo")
	@TableGenerator(name = "WJGen_todo", pkColumnValue = "todo")
	@Column(name = "todo_id")
	private int todoId;
	@Column(name = "user_id")
	private int userId;
	@Column(name = "create_date")
	@Temporal(TemporalType.TIMESTAMP)
	private Date createDate;
	@Column(name = "modif_date")
	@Temporal(TemporalType.TIMESTAMP)
	private Date modifDate;
	@Column(name = "text")
	private String text;
	@Column(name = "is_global")
	private boolean isGlobal;
	@Column(name = "is_resolved")
	private boolean isResolved;
	@Column(name = "sort_priority")
	private int priority;
	@Column(name = "dead_line")
	@Temporal(TemporalType.TIMESTAMP)
	private Date deadLine;
	@Column(name = "note")
	private String note;
	@Column(name = "group_id")
	private int groupId;

	public int getTodoId()
	{
		return todoId;
	}

	public void setTodoId(int todoId)
	{
		this.todoId = todoId;
	}

	@Override
	public void setId(int id)
	{
		setTodoId(id);
	}

	@Override
	public int getId()
	{
		return getTodoId();
	}

	public int getUserId()
	{
		return userId;
	}

	public void setUserId(int userId)
	{
		this.userId = userId;
	}

	public Date getCreateDate()
	{
		return createDate;
	}

	public void setCreateDate(Date createDate)
	{
		this.createDate = createDate;
	}

	public Date getModifDate()
	{
		return modifDate;
	}

	public void setModifDate(Date modifDate)
	{
		this.modifDate = modifDate;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public boolean getIsGlobal()
	{
		return isGlobal;
	}

	public void setIsGlobal(boolean isGlobal)
	{
		this.isGlobal = isGlobal;
	}

	public boolean getIsResolved()
	{
		return isResolved;
	}

	public void setIsResolved(boolean isResolved)
	{
		this.isResolved = isResolved;
	}

	@Override
	public String toString()
	{
		return "ToDoBean [" + "todoId=" + Integer.toString(todoId) + ", userId=" + Integer.toString(userId) + ", isGlobal="
					+ Boolean.toString(isGlobal) + ", createDate=" + Tools.formatDateTime(createDate) + ", text=" + text
					+ ", isResolved=" + Boolean.toString(isResolved) + "]";
	}

	public boolean getResolved() { //NOSONAR
		return isResolved;
	}

	public void setResolved(boolean resolved) {
		isResolved = resolved;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public Date getDeadLine() {
		return deadLine;
	}

	public void setDeadLine(Date deadLine) {
		this.deadLine = deadLine;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}
}