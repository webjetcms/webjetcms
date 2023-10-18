package sk.iway.iwcm.components.restaurant_menu;

import java.util.Date;
import java.util.List;

import javax.persistence.Query;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;

public class MenuDB extends JpaDB<MenuBean>
{
	private static MenuDB instance = new MenuDB();

	public MenuDB()
	{
		super(MenuBean.class);
	}

	public static MenuDB getInstance()
	{
		return instance;
	}

	public List<MenuBean> getByDate(Date date)
	{
		java.sql.Date day = new java.sql.Date(date.getTime());
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		try{
			ExpressionBuilder builder = new ExpressionBuilder();
			ReadAllQuery dbQuery = new ReadAllQuery(MenuBean.class, builder);

			Expression expr = builder.get("day").equal(day.toString());
			expr = expr.and(builder.get("domainId").equal(CloudToolsForCore.getDomainId()));
			dbQuery.setSelectionCriteria(expr);

			dbQuery.addOrdering(builder.get("meal").get("cathegory").ascending());
			dbQuery.addOrdering(builder.get("priority").ascending());
			dbQuery.addOrdering(builder.get("meal").get("name").ascending());

			Query query = em.createQuery(dbQuery);
			List<MenuBean> records = JpaDB.getResultList(query);

			return records;
		}catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}finally{
			em.close();
		}
		throw new IllegalStateException("Query did not complete regularly");
	}
}
