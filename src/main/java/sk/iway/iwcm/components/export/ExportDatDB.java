package sk.iway.iwcm.components.export;

import java.util.List;

import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import net.sourceforge.stripes.action.ActionBeanContext;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;

/**
 * ExportDatDB.java
 *
 * DAO class for manipulating with ExportDatBean
 *
 * Ticket: Export dat (#16902)
 *
 * @Title webjet7
 * @ExportDat Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2010
 * @author $Author: mkolejak $
 * @version $Revision: 1.3 $
 * @created Date: 05.11.2014 13:48:16
 * @modified $Date: 2004/08/16 06:26:11 $
 */
public class ExportDatDB extends JpaDB<ExportDatBean>
{
	private static ExportDatDB INSTANCE = new ExportDatDB(); //NOSONAR

	public static ExportDatDB getInstance()
	{
		return INSTANCE;
	}

	public ExportDatDB()
	{
		super(ExportDatBean.class);
	}

	public List<ExportDatBean> findByExportDatId(int exportDatId)
	{
		return JpaTools.findByMatchingProperty(ExportDatBean.class, "exportDatId", exportDatId);
	}

	public static ExportDatBean getExportDatById(int id)
	{
		return new JpaDB<ExportDatBean>(ExportDatBean.class).getById(id);
	}

	public static boolean saveExportDat(ExportDatBean exportDat)
	{
		return new JpaDB<ExportDatBean>(ExportDatBean.class).save(exportDat);
	}

	public static boolean deleteExportDat(int id)
	{
		return JpaTools.delete(ExportDatBean.class, id);
	}

	public static List<ExportDatBean> getAllExportDat()
	{
		return JpaTools.getAll(ExportDatBean.class);
	}

	public ExportDatBean findFirstExportByUrlAddress(String urlAddress)
	{
		return JpaTools.findFirstByMatchingProperty(ExportDatBean.class, "urlAddress", urlAddress);
	}

	public List<ExportDatBean> findExportByUrlAddress(String urlAddress)
	{
		return JpaTools.findByMatchingProperty(ExportDatBean.class, "urlAddress", urlAddress);
	}

	public List<ExportDatBean> findExportByUrlAddressAndFormatAndGroupIds(String urlAddress, String format, String groupIds)
	{
		Logger.debug(this, "method findExportDatByStateAndCity()");
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		List<ExportDatBean> results = null;
		try
		{
			ExpressionBuilder builder = new ExpressionBuilder();
			ReadAllQuery dbQuery = new ReadAllQuery(ExportDatBean.class, builder);
			Expression expr2 = null;
			if (Tools.isNotEmpty(urlAddress))
			{
				Expression expr = builder.get("urlAddress").containsSubstring(urlAddress);
				expr2 = JpaDB.and(expr2, expr);
			}
			if (Tools.isNotEmpty(format))
			{
				Expression expr = builder.get("format").containsSubstring(format);
				expr2 = JpaDB.and(expr2, expr);
			}
			if (Tools.isNotEmpty(groupIds))
			{
				Expression expr = builder.get("groupIds").containsSubstring(groupIds);
				expr2 = JpaDB.and(expr2, expr);
			}
			if (expr2 != null)
				dbQuery.setSelectionCriteria(expr2);
			Logger.debug(this, "method findExportByUrlAddressAndFormatAndGroupIds() q: " + dbQuery.toString());
			Query query = em.createQuery(dbQuery);
			results = JpaDB.getResultList(query);
			Logger.debug(this, "method findExportByUrlAddressAndFormatAndGroupIds() results: " + results.toString());
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

	public String getActualUrlAddress(ActionBeanContext context)
	{
		HttpServletRequest request = context.getRequest();
		String urlAddress = PathFilter.getOrigPath(request);
		return urlAddress;
	}
}