package sk.iway.iwcm.components.restaurant_menu;

import java.util.List;

import javax.persistence.Query;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;

public class MealDB extends JpaDB<MealBean>
{
	private static MealDB instance = new MealDB();

	public MealDB()
	{
		super(MealBean.class);
	}

	public static MealDB getInstance()
	{
		return instance;
	}

	public List<MealBean> findAll()
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		try{
			ExpressionBuilder builder = new ExpressionBuilder();
			ReadAllQuery dbQuery = new ReadAllQuery(MealBean.class, builder);

			Expression expr = builder.get("domainId").equal(CloudToolsForCore.getDomainId());
			dbQuery.setSelectionCriteria(expr);

			dbQuery.addOrdering(builder.get("cathegory").ascending());
			dbQuery.addOrdering(builder.get("name").ascending());

			Query query = em.createQuery(dbQuery);
			List<MealBean> records = JpaDB.getResultList(query);

			return records;
		}catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}finally{
			em.close();
		}
		throw new IllegalStateException("Query did not complete regularly");
	}

	public List<MealBean> findByName(String name)
	{
		return super.findBy(filterSubstring("name", name),filterEquals("domain_id", CloudToolsForCore.getDomainId()));
	}
}
