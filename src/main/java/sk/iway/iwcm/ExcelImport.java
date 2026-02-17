package sk.iway.iwcm;

import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import sk.iway.iwcm.i18n.Prop;

/**
 *  Superclass pre import udajov z excelu do databazy, prvy riadok obsahuje nazvy stlpcov
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.8 $
 *@created      Štvrtok, 2003, august 21
 *@modified     $Date: 2004/03/23 19:23:05 $
 */
public abstract class ExcelImport
{
	protected Map<String, Integer> header;
	protected InputStream in;
	protected HttpServletRequest request;
	protected PrintWriter out;
	protected int rowCounter;

	public ExcelImport(InputStream in, HttpServletRequest request, PrintWriter out) //NOSONAR
	{
		this.in = in;
		this.request = request;
		this.out = out;
		rowCounter = 0;
		header = new Hashtable<>();
	}

	/**
	 * Nastavi stlpec updated v databaze v tabulke tableName na false
	 * @param tableName
	 * @return
	 */
	public boolean setUpdateField(String tableName)
	{
		boolean ret = false;
		java.sql.Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = sk.iway.iwcm.DBPool.getConnection(request);


			//nastav updated na 0, aby sme vedeli co sa v exceli nenachadza...
			ps = db_conn.prepareStatement("UPDATE "+tableName+" SET updated=?");
			ps.setBoolean(1, false);
			ps.execute();
			ps.close();

			db_conn.close();
			ret = true;
			db_conn = null;
			ps = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return(ret);
	}

	/**
	 * vymaze z databazy zaznamy, ktore nie su updatnute
	 * @param tableName
	 * @return
	 */
	public boolean deleteNotUpdated(String tableName)
	{
		boolean ret = false;
		try
		{
			java.sql.Connection db_conn = sk.iway.iwcm.DBPool.getConnection(request);
			try
			{
				String sql;

				println(out, "", -1);
				//vymaz polozky
				sql = "SELECT * FROM "+tableName+" WHERE updated=?";
				PreparedStatement ps = db_conn.prepareStatement(sql);
				try
				{
					ps.setBoolean(1, false);
					ResultSet rs = ps.executeQuery();
					try
					{
						while (rs.next())
						{
							printlnError(out, "Vymazávam: "+rs.getString(1)+", "+rs.getString(2)+", "+rs.getString(3)+", "+rs.getString(4), -1);
						}
					}
					finally { rs.close(); }
				}
				finally { ps.close(); }
				sql = "DELETE FROM "+tableName+" WHERE updated=?";
				ps = db_conn.prepareStatement(sql);
				try
				{
					ps.setBoolean(1, false);
					ps.execute();
				}
				finally { ps.close(); }
			}
			finally { db_conn.close(); }
			ret = true;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		return(ret);
	}

	/**
	 * Nastavi hashtable, ktora prevadza meno na cislo stlpca. Meno je vzdy lowercase
	 * @param row
	 */
	protected void setHeader(Row row)
	{
		Iterator<Cell> cels = row.cellIterator();
		Cell cell;
		String str;
		int pos;
		while (cels.hasNext())
		{
			cell = cels.next();

			if (cell == null)
			{
				continue;
			}

			str = getValue(cell);
			if (str != null) str = str.toLowerCase();
			pos = cell.getColumnIndex();

			header.put(str, Integer.valueOf(pos));
		}
	}

	/**
	 * pridanie riadku do zoznamu riadkov
	 * @param row
	 */
	protected abstract void saveRow(Connection db_conn, Row row) throws Exception;

	public void doImport(Prop prop)
	{
		doImport(null, prop);
	}

	public void doImport(String sheetName, Prop prop)
	{
		try
		{
			Logger.println(this,"doImport sheet="+sheetName);

			POIFSFileSystem fs = new POIFSFileSystem(in);
			HSSFWorkbook wb = new HSSFWorkbook(fs);

			Logger.println(this,"doImport mam wb");

			if (sheetName==null || sheetName.trim().length()<1)
			{
				//iteruj po sheetoch a importuj ich
				int counter = 0;
				HSSFSheet sheet = wb.getSheetAt(counter);
				while (sheet!=null)
				{
					Logger.println(this,"doImport: sheet="+wb.getSheetName(counter));

					println(out, "<big><b>"+prop.getText("file.importing_sheet")+" "+(counter+1)+": "+wb.getSheetName(counter)+"</b></big><br><br>", -1);
					importSheet(sheet);
					counter++;
					try
					{
						sheet = wb.getSheetAt(counter);
					}
					catch (IndexOutOfBoundsException iobex)
					{
						sheet = null;
					}
				}
			}
			else
			{
				Logger.println(this,"doImport sheet="+sheetName);

				HSSFSheet sheet = wb.getSheet(sheetName);
				importSheet(sheet);
			}

			wb.close();
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		println(out, "", -1);
	}

	/**
	 * vykonanie importu
	 */
	public void importSheet(HSSFSheet sheet)
	{
		try
		{
			Row row;

			Iterator<Row> rows = sheet.rowIterator();

			if (rows.hasNext()==false) return;

			rowCounter = 0;

			row = rows.next();

			setHeader(row);

			java.sql.Connection db_conn = sk.iway.iwcm.DBPool.getConnection(request);
			try
			{
				while (rows.hasNext())
				{
					rowCounter++;
					try
					{
						row = rows.next();
						Logger.println(this,"save row");
						saveRow(db_conn, row);
					}
					catch (Exception ex)
					{
						sk.iway.iwcm.Logger.error(ex);
						printlnError(out, "Chyba importu: "+ex.getMessage(), rowCounter);
					}
				}
			}
			finally { db_conn.close(); }
		}
		catch (Exception ex){
			sk.iway.iwcm.Logger.error(ex);
			printlnError(out, "Vyskytla sa chyba - " + ex.getMessage(), rowCounter);
		}
	}



	public static void println(PrintWriter out, String message, int row)
	{
		if (row>=0) out.print(row+": ");
		out.println(message+"<br>");
		out.flush();
	}

	public static void printlnError(PrintWriter out, String message, int row)
	{
		if (row>=0) out.print(row+": ");
		out.println("<font color='red'>"+message+"</font><br>");
		out.flush();
	}

	/**
	 * zmaze uvodzovky a apostrofy z textu
	 * @param textPar
	 * @return
	 */
	public String dropQuote(String textPar)
	{
		String text = textPar;
		if (text==null || text.trim().length()<1) return(text);

		try
		{
			if (text.charAt(0)==('\"')) text = text.substring(1);
			if (text.charAt(0)=='\'') text = text.substring(1);
			if (text.endsWith("\"")) text = text.substring(0, text.length()-1);
			if (text.endsWith("'")) text = text.substring(0, text.length()-1);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return(text);
	}

	public HSSFCell getCell(HSSFRow row, String name)
	{
		Integer index = header.get(name.toLowerCase());

		if (index==null) return(null);

		//Logger.println(this,"get cell name="+name+" index="+index.intValue());
		return(row.getCell(index.intValue()));
	}

	public String getValue(HSSFRow row, String name)
	{
		return(getValue(getCell(row, name)));
	}

	/**
	 *  Ziska hodnotu bunky ako String
	 *
	 *@param  cell  Description of the Parameter
	 *@return       The value value
	 */
	public static String getValue(Cell cell)
	{
		String value = "";

		if (cell != null)
		{
			CellType cellType = cell.getCellType();
			if (cellType.equals(CellType.STRING))
				value = cell.getStringCellValue();
			else if (cellType.equals(CellType.NUMERIC))
				value = Integer.toString(((int)cell.getNumericCellValue()));
			else if (cellType.equals(CellType.BLANK))
				value = "";
			else if (cellType.equals(CellType.BOOLEAN))
				value = Boolean.toString(cell.getBooleanCellValue());
			else
				Logger.error(ExcelImport.class,"nejaky cudny celltype: " + cell.getCellType());
		}

		try
		{
			//osetri numeric field z 200.0 na 200
			if (value!=null && value.endsWith(".0"))
			{
				value = value.substring(0, value.length()-2);
			}
		}
		catch (Exception ex)
		{

		}


		return value;
	}

	public int getIntValue(HSSFRow row, String name)
	{
		return(getIntValue(getCell(row, name)));
	}

	/**
	 * vrati celociselnu hodnotu bunky
	 * @param cell
	 * @return
	 */
	public static int getIntValue(HSSFCell cell)
	{
		int value = 0;
		try
		{
			value = Integer.parseInt(getValue(cell));
		}
		catch (Exception ex)
		{

		}
		return(value);
	}

	public boolean getBooleanValue(HSSFRow row, String name)
	{
		return(getBooleanValue(getCell(row, name)));
	}

	/**
	 * vrati true ak je v bunke napisane ano, pravda, alebo nieco podobne
	 * @param cell
	 * @return
	 */
	public static boolean getBooleanValue(HSSFCell cell)
	{
		boolean value = false;
		String sValue = getValue(cell);
		if ("ano".equalsIgnoreCase(sValue) || "áno".equalsIgnoreCase(sValue) || "pravda".equalsIgnoreCase(sValue)  || "x".equalsIgnoreCase(sValue) || "a".equalsIgnoreCase(sValue))
		{
			value = true;
		}
		return(value);
	}

	/**
	 * @return
	 */
	public Date getDateValue(HSSFRow row, String name)
	{
		Date ret = null;
		try {
			 GregorianCalendar gc = new GregorianCalendar(1900, Calendar.JANUARY, 1);
		    gc.add(Calendar.DATE, Integer.parseInt(getValue(row,name))-1);
		    gc.add(Calendar.DAY_OF_MONTH, -1);
		    ret = new Date(gc.getTimeInMillis());
		} catch(Exception e) {

		}
		return ret;
	}

	/**
	 * trimne aj tvrde medzery
	 * @param sPar
	 * @return
	 */
	public String trim(String sPar)
	{
		String s = sPar;
		if(s == null) return null;
		s = s.trim();
		if (s.length() == 0 ) return s;
		char[] space={160};
		while(s.endsWith(new String(space)) || s.endsWith(" "))
			s = s.substring(0,s.length()-1);
		while(s.startsWith(new String(space)) || s.charAt(0) == (' '))
			s = s.substring(0,s.length()-1);
		s = s.trim();
		return s;
	}

}
