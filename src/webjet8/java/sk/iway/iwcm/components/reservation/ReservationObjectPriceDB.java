package sk.iway.iwcm.components.reservation;

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

/**
 *  ReservationObjectPriceDB.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author       $Author: jeeff rzapach $
 *@version      $Revision: 1.3 $
 *@created      Date: 11.12.2015 15:03:17
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ReservationObjectPriceDB extends JpaDB<ReservationObjectPriceBean>
{
	private static ReservationObjectPriceDB instance = new ReservationObjectPriceDB();

	public ReservationObjectPriceDB()
	{
		super(ReservationObjectPriceBean.class);
	}

	public static ReservationObjectPriceDB getInstance()
	{
		return instance;
	}

	public List<ReservationObjectPriceBean> getByReservationObjectId(int id)
	{
		return super.findBy(filterEquals("object_id", id),filterEquals("domain_id", CloudToolsForCore.getDomainId()));
	}

	public List<ReservationObjectPriceBean> getOverlapping(Date datumOd, Date datumDo, int objectId, int domain, int editPriceId)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		try{
			ExpressionBuilder builder = new ExpressionBuilder();
			ReadAllQuery dbQuery = new ReadAllQuery(ReservationObjectPriceBean.class, builder);

			Expression expr = builder.get("domainId").equal(domain);
			expr = expr.and(builder.get("objectId").equal(objectId));

			if(editPriceId>-1)
				expr = expr.and(builder.get("objectPriceId").notEqual(editPriceId));

			// (StartA <= EndB) and (EndA >= StartB)
			expr = expr.and(builder.get("datumOd").lessThanEqual(datumDo));
			expr = expr.and(builder.get("datumDo").greaterThanEqual(datumOd));

			dbQuery.setSelectionCriteria(expr);
			dbQuery.addOrdering(builder.get("datumOd").ascending());

			Query query = em.createQuery(dbQuery);
			return JpaDB.getResultList(query);

		}catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}finally{
			em.close();
		}
		throw new IllegalStateException("Query did not complete regularly");
	}
}
