package sk.iway.iwcm.editor;

import java.util.List;

import javax.persistence.Query;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;

/**
 *  DocNoteDB.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2017
 *@author       $Author: jeeff rzapach $
 *@version      $Revision: 1.3 $
 *@created      Date: 12.4.2017 10:49:04
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class DocNoteDB extends JpaDB<DocNoteBean>
{
	private static DocNoteDB instance = new DocNoteDB();

	public DocNoteDB()
	{
		super(DocNoteBean.class);
	}

	public static DocNoteDB getInstance()
	{
		return instance;
	}

	public String getNoteText(int docId, int historyId)
	{
		try {
			DocNoteBean note = getInstance().getDocNote(docId, historyId);
			if (note != null)
				return note.getNote();
			else
				return "";
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return "";
	}

	public DocNoteBean getDocNote(int docId, int historyId)
	{
		if(historyId>1)
			return super.findFirst("historyId", historyId);
		else if(docId>0)
			return super.findFirst("docId", docId);
		else
			return null;
	}

	/**
	 * vrati iba aktualne DocNotes (bez historie)
	 * @return
	 */
	public List<DocNoteBean> getCurrentDocNotes()
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		try{
			ExpressionBuilder builder = new ExpressionBuilder();
			ReadAllQuery dbQuery = new ReadAllQuery(DocNoteBean.class, builder);

			Expression expr = builder.get("docId").notEqual(-1);
			dbQuery.setSelectionCriteria(expr);
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
