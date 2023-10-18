package sk.iway.iwcm.components.todo;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * Test databazy na insert a na vypis id-ciek vlozenych riadkov
 *
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2014
 * @author $Author: jeeff mkolejak $
 * @version $Revision: 1.3 $
 * @created Date: 1.7.2014 14:17:40
 * @modified $Date: 2004/07/01 06:26:11 $
 */
public class ToDoDBTest extends BaseWebjetTest
{
	@BeforeEach
	public void setUp() throws Exception
	{
		ToDoBean toDo = new ToDoBean();
		int userId = 173;
		toDo.setText("daj co mas true 1");
		toDo.setCreateDate(new Date(Tools.getNow()));
		toDo.setUserId(userId);
		toDo.setIsGlobal(true);
		toDo.setIsResolved(true);
		query.execute("INSERT INTO todo(todo_id, text,create_date,user_id,is_resolved,is_global) "
					+ "VALUES (?, ?,?,?,?,?)", 1, toDo.getText(), toDo.getCreateDate(),
					toDo.getUserId(), toDo.getIsResolved(), toDo.getIsGlobal());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void selectAll()
	{
		// vrati list id-ciek
		List<ToDoBean> list = query.forList("select todo_id,text,create_date,user_id,is_resolved,is_global from todo");
		for (int i = 0; i < list.size(); i++)
		{
			Logger.debug(this, "method selectAll() todo: " + list.get(i));
		}
		assertNotEquals(1, list.size());
	}

	@AfterEach
	public void tearDown()
	{
		query.execute("DELETE FROM todo WHERE todo_id=1");
	}
}
