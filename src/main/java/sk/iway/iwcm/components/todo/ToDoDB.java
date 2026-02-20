package sk.iway.iwcm.components.todo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Query;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;

/**
 * ToDoDB.java
 *
 * DAO class for manipulating with ToDoBean
 *
 *
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2010
 * @author $Author: mkolejak $
 * @version $Revision: 1.3 $
 * @created Date: 01.07.2014 14:54:26
 * @modified $Date: 2004/08/16 06:26:11 $
 */
public class ToDoDB extends JpaDB<ToDoBean>
{
	public ToDoDB()
	{
		super(ToDoBean.class);
	}

	public static boolean saveToDo(ToDoBean toDo)
	{
		return new JpaDB<>(ToDoBean.class).save(toDo);
	}

	public ToDoBean findFirstByToDoId(int todoId)
	{
		return JpaTools.findFirstByMatchingProperty(ToDoBean.class, "todoId", todoId);
	}

	public boolean resolveToDo(int todoId)
	{
		Logger.debug(this, "method resolveToDo() todoId: " + todoId);
		ToDoBean toDo = findFirstByToDoId(todoId);
		toDo.setIsResolved(true);
		toDo.setModifDate(new Date());
		boolean wasSaved = saveToDo(toDo);
		Logger.debug(this, "method resolveToDo() wasSaved: " + wasSaved);
		return wasSaved;
	}

	public boolean remove(ToDoBean toDoBean) {
		return new JpaDB<>(ToDoBean.class).delete(toDoBean);
	}

	public List<ToDoBean> getToDo(int userId)
	{
		//Logger.debug(this, "method getToDo()");
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		List<ToDoBean> results = new ArrayList<>();
		try
		{
			ExpressionBuilder builder = new ExpressionBuilder();
			ReadAllQuery dbQuery = new ReadAllQuery(ToDoBean.class, builder);
			Expression isGlobal = builder.get("isGlobal").equal(true);
			Expression isResolved = builder.get("isResolved").equal(false);
			Expression isUserId = builder.get("userId").equal(userId);
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.HOUR,-24);
			Expression isBetween = builder.get("modifDate").between(cal.getTime(), new Date());
//			why name to do why?!?
//			"SELECT * FROM to do WHERE is_global=1 OR user_id=? AND (is_resolved=0 OR modif_date BETWEEN ? AND ?) ORDER BY priority, is_resolved";
			dbQuery.setSelectionCriteria(isGlobal.or(isUserId.and(isResolved.or(isBetween))));
			//Logger.debug(this, "method getToDo() q: " + dbQuery.toString());
			Query query = em.createQuery(dbQuery);
			results = JpaDB.getResultList(query);
			//Logger.debug(this, "method getToDo() results: " + results.toString());
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		finally
		{
			em.close();
		}
		return results;
	}
}