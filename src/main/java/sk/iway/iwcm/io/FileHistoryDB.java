package sk.iway.iwcm.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Query;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;

/**
 *  FileHistoryDB.java - praca s historiou suborov
 *  tiket 13373
 *
 *	DAO class for manipulating with FileHistoryBean
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: mrepasky $
 *@version      $Revision: 1.3 $
 *@created      Date: 17.05.2013 14:40:47
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class FileHistoryDB extends JpaDB<FileHistoryBean>
{
	public FileHistoryDB()
	{
		super(FileHistoryBean.class);
	}

	/**
	 * Preposle subor z historie na vystup
	 * @param path
	 * @param historyId
	 * @param response
	 * @return
	 */
	public static boolean sendFileFromHistory(String path, int historyId, HttpServletResponse response)
	{
		FileHistoryBean fhb = (new FileHistoryDB()).getById(historyId);
		if (fhb != null)
		{
			if (fhb.getFileUrl().equals(path))
			{
				IwcmFile file = new IwcmFile(Tools.getRealPath(fhb.getHistoryPath()+fhb.getFileHistoryId()));
				try
				{
					//nastav HTTP hlavicky
					String mimeType = Constants.getServletContext().getMimeType(path.toLowerCase());
					if (Tools.isEmpty(mimeType)) mimeType = "application/octet-stream";
					String ext = FileTools.getFileExtension(path);
					if (ext.equals("jsp") || ext.equals("js")) mimeType = "text/plain";

					Logger.debug(FileHistoryDB.class, "setting content type:"+mimeType);

					response.setContentType(mimeType);

					writeFileOut(file, response);

					return true;
				}
				catch (Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
				}

			}
		}
		return false;
	}

	private static void writeFileOut(IwcmFile f, HttpServletResponse res) throws IOException
	{
		IwcmInputStream fis = null;
		ServletOutputStream out = null;
		try
		{
			out = res.getOutputStream();
			byte[] buff = new byte[64000];
			fis = new IwcmInputStream(f);
			int len;
			while ((len = fis.read(buff)) != -1)
			{
				out.write(buff, 0, len);
			}

		}
		finally
		{
			if (fis != null) fis.close();
			if(out != null) out.close();
		}
	}


	public List<FileHistoryBean> findByFileUrl(String fileUrl)
	{
		return JpaTools.findByMatchingProperty(FileHistoryBean.class, "fileUrl", fileUrl);
	}

	public FileHistoryBean findFirstByFileUrl(String fileUrl)
	{
		return JpaTools.findFirstByMatchingProperty(FileHistoryBean.class, "fileUrl", fileUrl);
	}
	public List<FileHistoryBean> findByChangeDate(Date changeDate)
	{
		return JpaTools.findByMatchingProperty(FileHistoryBean.class, "changeDate", changeDate);
	}

	public FileHistoryBean findFirstByChangeDate(Date changeDate)
	{
		return JpaTools.findFirstByMatchingProperty(FileHistoryBean.class, "changeDate", changeDate);
	}
	public List<FileHistoryBean> findByUserId(int userId)
	{
		return JpaTools.findByMatchingProperty(FileHistoryBean.class, "userId", userId);
	}

	public FileHistoryBean findFirstByUserId(int userId)
	{
		return JpaTools.findFirstByMatchingProperty(FileHistoryBean.class, "userId", userId);
	}

	public List<FileHistoryBean> getHistoryByPath(String virtualPath)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		List<FileHistoryBean> records = new ArrayList<>();
		try{
			ExpressionBuilder builder = new ExpressionBuilder();
			ReadAllQuery dbQuery = new ReadAllQuery(FileHistoryBean.class, builder);
			if(Tools.isNotEmpty(virtualPath))
			{
				Expression expr = builder.get("fileUrl").equal(virtualPath);
				dbQuery.setSelectionCriteria(expr);
			}
			Expression expr1 = builder.get("changeDate");
			List<Expression> expressions = new ArrayList<>();
			expressions.add(expr1);

			dbQuery.setOrderByExpressions(expressions);

			Query query = em.createQuery(dbQuery);
			records = JpaDB.getResultList(query);
		}catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}finally{
			em.close();
		}
		return records;
	}
}