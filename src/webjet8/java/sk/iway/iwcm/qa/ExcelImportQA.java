package sk.iway.iwcm.qa;

import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

import javax.servlet.http.HttpServletRequest;

import jxl.Cell;
import jxl.Sheet;
import sk.iway.Password;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.xls.ExcelImportJXL;

/**
 *  ExcelImport.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Date: 20.9.2006 15:45:59
 *@modified     $Date: 2006/11/30 09:47:04 $
 */
public class ExcelImportQA extends ExcelImportJXL
{
	public ExcelImportQA(InputStream in, HttpServletRequest request, PrintWriter out)
	{
		super(in, request, out);
	}

	@Override
	protected void saveRow(Connection db_conn, Cell[] row, Sheet sheet, Prop prop) throws Exception
	{
		if (row.length < 2)
			return;

		String question = getValue(row, "question");
		String answer = getValue(row, "answer");

		if (Tools.isNotEmpty(question) && Tools.isNotEmpty(answer))
		{
			println(prop.getText("components.qa.excel_import.importing") + " " + question, rowCounter);

			boolean publishOnWeb = true;
			if (Tools.isNotEmpty(getValue(row, "publish_on_web")))
			{
				publishOnWeb = getBooleanValue(row, "publish_on_web");
			}

			PreparedStatement ps = null;
			try
			{
				ps = db_conn.prepareStatement("INSERT INTO questions_answers (group_name, category_name, question_date, answer_date, question, answer, from_name, from_email, to_name, to_email, publish_on_web, hash, allow_publish_on_web, from_phone, from_company, field_a, field_b, field_c, field_d, domain_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
				int psCounter = 1;
				ps.setString(psCounter++, getValue(row, "group_name"));
				ps.setString(psCounter++, getValue(row, "category_name"));
				ps.setTimestamp(psCounter++, new Timestamp(DB.getTimestamp(getValue(row, "question_date"))));
				ps.setTimestamp(psCounter++, new Timestamp(DB.getTimestamp(getValue(row, "answer_date"))));
				DB.setClob(ps, psCounter++, question);
				DB.setClob(ps, psCounter++, answer);
				ps.setString(psCounter++, getValue(row, "from_name"));
				ps.setString(psCounter++, getValue(row, "from_email"));
				ps.setString(psCounter++, getValue(row, "to_name"));
				ps.setString(psCounter++, getValue(row, "to_email"));
				ps.setBoolean(psCounter++, publishOnWeb);
				ps.setString(psCounter++, Password.generatePassword(10));
				ps.setBoolean(psCounter++, publishOnWeb);

				ps.setString(psCounter++, getValue(row, "from_phone"));
				ps.setString(psCounter++, getValue(row, "from_company"));
				ps.setString(psCounter++, getValue(row, "field_a"));
				ps.setString(psCounter++, getValue(row, "field_b"));
				ps.setString(psCounter++, getValue(row, "field_c"));
				ps.setString(psCounter++, getValue(row, "field_d"));
				ps.setInt(psCounter++, CloudToolsForCore.getDomainId());

				ps.execute();
				ps.close();
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
				printlnError("ERROR: " + ex.getMessage(), rowCounter);
			}
			finally
			{
				try
				{
					if (ps != null)
						ps.close();
				}
				catch (Exception ex2)
				{
					sk.iway.iwcm.Logger.error(ex2);
				}
			}
		}
	}

	@Override
	public String getValue(Cell[] row, String name) throws Exception
	{
		String value = super.getValue(row, name);
		if (Tools.isEmpty(value))
			value = "";

		return(value);
	}
}
