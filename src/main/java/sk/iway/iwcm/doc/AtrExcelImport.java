package sk.iway.iwcm.doc;

import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;

import javax.servlet.http.HttpServletRequest;

import jxl.Cell;
import jxl.Sheet;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.xls.ExcelImportJXL;

/**
 *  AtrExcelImport.java - import atributov do tabulky doc_atr_def, format excelu je:
 *  nazov, klucovy udaj (ak je x nastavi sa atr_description na list_table), typ
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 25.10.2005 11:37:25
 *@modified     $Date: 2007/09/07 13:39:29 $
 */
public class AtrExcelImport extends ExcelImportJXL
{
	PreparedStatement ps;
	String sql;
	String sTmp;
	int atrType = 0;
	String lastSheetName;
	String sheetName;

	public AtrExcelImport(InputStream in, HttpServletRequest request, PrintWriter out)
	{
		super(in, request, out);
	}

	@Override
	protected void saveRow(Connection db_conn, Cell[] row, Sheet sheet, Prop prop) throws Exception
	{
		println("Importujem: "+sheet.getName()+" "+row[0].getContents(), rowCounter);

		sql = "INSERT INTO doc_atr_def (atr_name, order_priority, atr_description, atr_default_value, atr_type, atr_group, true_value, false_value) ";
		sql += "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

		int index = 1;
		if (row.length>2 && row[0].getContents().length()>0 && row[2].getContents().length()>1)
		{
			sheetName = DB.internationalToEnglish(DocTools.removeChars(sheet.getName()).trim());
			if (lastSheetName == null || lastSheetName.equals(sheetName)==false)
			{
				println("Deleting sheet: "+sheetName);

				ps = db_conn.prepareStatement("DELETE FROM doc_atr_def WHERE atr_group = ?");
				ps.setString(1, sheetName);
				ps.execute();
				ps.close();

				lastSheetName = sheetName;
			}

	   		ps = db_conn.prepareStatement(sql);
			ps.setString(index++, getValue(row[0])); //atr_name
			ps.setInt(index++, (rowCounter-1) * 10); //order_priority
			if (Tools.isNotEmpty(getValue(row[1])))
			{
				ps.setString(index++, getValue(row[1]));	//atr_description
			}
			else
			{
				ps.setNull(index++, Types.VARCHAR);
			}
			sTmp = getValue(row[2]);						//atr_default_value
			if (sTmp.indexOf("více řádků")!=-1 || sTmp.indexOf("viac riadkov")!=-1)
			{
				ps.setString(index++, "multiline-40-4");
			}
			else
			{
				ps.setNull(index++, Types.VARCHAR);
			}
			atrType = AtrDB.TYPE_STRING;
			if (sTmp.indexOf("Číslo")!=-1) atrType = AtrDB.TYPE_INT;
			if (sTmp.indexOf("Boolean")!=-1) atrType = AtrDB.TYPE_INT;
			if (sTmp.indexOf("Double")!=-1) atrType = AtrDB.TYPE_DOUBLE;
			ps.setInt(index++, atrType);					//atr_type
			ps.setString(index++, sheetName);			//atr_group
			ps.setNull(index++, Types.BOOLEAN);			//true_value
			ps.setNull(index++, Types.BOOLEAN);			//false_value

			ps.execute();
			ps.close();

	   	}
	}
}
