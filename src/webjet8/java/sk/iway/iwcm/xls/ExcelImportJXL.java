package sk.iway.iwcm.xls;

import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import jxl.Cell;
import jxl.CellType;
import jxl.NumberCell;
import jxl.Sheet;
import jxl.Workbook;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;

/**
 * ExcelImportJXL.java - Superclass pre import udajov z excelu do databazy,
 * prvy riadok obsahuje nazvy stlpcov, importuje sa pomocou JXL API (pozna Excel 95)
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.36 $
 *@created      $Date: 2010/01/20 11:15:27 $
 *@modified     $Date: 2010/01/20 11:15:27 $
 */
public class ExcelImportJXL
{
	protected Map<String, Integer> header;
	protected Cell[] headerCells;
	protected String[] headerNames;
	protected String[] headerNamesOriginal;

	protected InputStream in;
	protected HttpServletRequest request;
	protected PrintWriter out;
	protected int rowCounter;
	protected int rowsInSheet = 0;
	protected String fileName;

	public ExcelImportJXL(InputStream in, HttpServletRequest request, PrintWriter out)
	{
		this.in = in;
		this.request = request;
		this.out = out;
		rowCounter = 0;
		header = new Hashtable<>();
	}

	/**
	 * Upravi header name - je to Lower Case, zrusi dvojite medzery
	 * @param name
	 * @return
	 */
	public static String formatHeaderName(String name)
	{
		if (name == null) {
			return null;
		}

		name = name.toLowerCase();
		//zrus dvojite medzery
		name = Tools.replace(name, "   ", " ");
		name = Tools.replace(name, "   ", " ");
		name = Tools.replace(name, "   ", " ");
		name = Tools.replace(name, "   ", " ");
		name = Tools.replace(name, "   ", " ");
		name = Tools.replace(name, "  ", " ");
		name = Tools.replace(name, "  ", " ");
		name = Tools.replace(name, "  ", " ");
		name = Tools.replace(name, "  ", " ");
		name = Tools.replace(name, "  ", " ");
		name = Tools.replace(name, "  ", " ");
		name = Tools.replace(name, "  ", " ");
		name = DB.internationalToEnglish(name);
		//Logger.println(this,"fhn:" + name + ":");
		return(name);
	}

	public static String formatHeaderNameOriginal(String name)
	{
		//zrus dvojite medzery
		name = Tools.replace(name, "   ", " ");
		name = Tools.replace(name, "   ", " ");
		name = Tools.replace(name, "   ", " ");
		name = Tools.replace(name, "   ", " ");
		name = Tools.replace(name, "   ", " ");
		name = Tools.replace(name, "  ", " ");
		name = Tools.replace(name, "  ", " ");
		name = Tools.replace(name, "  ", " ");
		name = Tools.replace(name, "  ", " ");
		name = Tools.replace(name, "  ", " ");
		name = Tools.replace(name, "  ", " ");
		name = Tools.replace(name, "  ", " ");

		return name;
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
			db_conn = sk.iway.iwcm.DBPool.getConnection();


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
		java.sql.Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = sk.iway.iwcm.DBPool.getConnection();

			String sql;

			println("", -1);
			//vymaz polozky
			sql = "SELECT * FROM "+tableName+" WHERE updated=?";
			ps = db_conn.prepareStatement(sql);
			ps.setBoolean(1, false);
			rs = ps.executeQuery();
			while (rs.next())
			{
				printlnError("Vymazávam: "+rs.getString(1)+", "+rs.getString(2)+", "+rs.getString(3)+", "+rs.getString(4), -1);
			}
			rs.close();
			rs = null;
			ps.close();

			sql = "DELETE FROM "+tableName+" WHERE updated=?";
			ps = db_conn.prepareStatement(sql);
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
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return(ret);
	}

	/**
	 * Nastavi Map, ktora prevadza meno na cislo stlpca. Meno je vzdy lowercase
	 * @param row
	 */
	protected void setHeader(Cell[] row)
	{
		headerCells = row;
		int pos;
		Cell cell;
		String str;
		headerNames = new String[row.length];
		headerNamesOriginal = new String[row.length];
		for (pos = 0; pos<row.length; pos++)
		{
			cell = row[pos];

			if (cell == null)
			{
				headerNames[pos] = null;
				headerNamesOriginal[pos] = null;
				continue;
			}

			str = formatHeaderName(getValue(cell));

			header.put(str, Integer.valueOf(pos));
			headerNames[pos] = str;
			headerNamesOriginal[pos] = formatHeaderNameOriginal(getValue(cell));

			Logger.println(this,"header["+pos+"]="+str+";");
		}
	}

	/**
	 * pridanie riadku do zoznamu riadkov
	 * @param row
	 */
	@SuppressWarnings("java:S1130")
	protected void saveRow(Connection db_conn, Cell[] row, Sheet sheet, Prop prop) throws Exception
	{
		//toto treba implementovat
		Logger.error(this,"ExcelImportJXL.saveRow NOT IMPLEMENTED");
	}

	protected void afterImportJob(Prop prop)
	{
		//need to be implemented
	}

	public void doImport(Prop prop)
	{
		doImport(null, prop);
	}

	public void doImport(String sheetName, Prop prop)
	{
		try
		{
			System.setProperty("jxl.encoding", "cp1250");

			Workbook wb = Workbook.getWorkbook(in);

			if (sheetName==null || sheetName.trim().length()<1)
			{
				//iteruj po sheetoch a importuj ich
				int counter = 0;
				//HSSFSheet sheet = wb.getSheetAt(counter);
				for (counter = 0; counter < wb.getNumberOfSheets(); counter++)
		      {
					Sheet sheet = wb.getSheet(counter);

					Logger.println(this,"ExcelImportJXL doImport: sheet="+sheet.getName());

					println("<big><b>"+prop.getText("file.importing_sheet")+" "+(counter+1)+": "+sheet.getName()+"</b></big><br><br>", -1);
					importSheet(sheet, prop);
				}
			}
			else
			{
				Logger.println(this,"doImport sheet="+sheetName);

				Sheet sheet = wb.getSheet(sheetName);
				importSheet(sheet, prop);
			}

			afterImportJob(prop);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		println("", -1);
	}

	/**
	 * vykonanie importu
	 */
	public void importSheet(Sheet sheet, Prop prop)
	{
		java.sql.Connection db_conn = null;
		try
		{
			Cell[] row;

			rowCounter = 0;

			row = sheet.getRow(0);

			setHeader(row);

			db_conn = sk.iway.iwcm.DBPool.getConnection();

			for (int i = 1 ; i < sheet.getRows() ; i++)
			{
				rowCounter++;
				try
				{
					row = sheet.getRow(i);
					rowsInSheet = sheet.getRows();
					saveRow(db_conn, row, sheet, prop);
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
					printlnError("Chyba importu: "+ex.getMessage(), rowCounter);
				}
			}

			db_conn.close();
			db_conn = null;
		}
		catch (Exception ex){
			sk.iway.iwcm.Logger.error(ex);
			printlnError("Vyskytla sa chyba - " + ex.getMessage(), rowCounter);
		}
		finally{
			try{
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
	}

	public void print(String message)
	{
		if (out == null)
		{
			Logger.println(this,message);
			return;
		}
		out.print(message);
		out.flush();
	}

	public void println(String message)
	{
		println(message, rowCounter+1);
	}

	public void println(String message, int row)
	{
		if (row>=0) Logger.debug(this,row+": "+message);
		else Logger.debug(this,message);

		if (out == null) return;

		if (row>=0) out.print(row+": ");
		out.println(message+"<br>\n");
		out.flush();

		if (row % 10 == 0)
		{
			//odscroluj stranku
			out.println("<script type='text/javascript'>window.scrollBy(0, 1000);</script>");
		}
	}

	public void printlnError(String message)
	{
		printlnError(message, rowCounter+1);
	}

	public void printlnError(String message, int row)
	{
		if (row>=0) Logger.debug(this,row+": ERROR: " + message);
		else Logger.debug(this,"ERROR: " + message);

		if (out == null) return;

		if (row>=0) out.print(row+": ");
		out.println("<font color='red'>"+message+"</font><br>");
		out.flush();
	}

	public void scrollWindow(int amount)
	{
		if (out == null) return;

		out.println("<script type='text/javascript'>window.scrollBy(0, "+amount+");</script>");
	}

	/**
	 * zmaze uvodzovky a apostrofy z textu
	 * @param text
	 * @return
	 */
	public String dropQuote(String text)
	{
		if (text==null || text.trim().length()<1) return(text);

		try
		{
			if (text.startsWith("\"")) text = text.substring(1);
			if (text.startsWith("'")) text = text.substring(1);
			if (text.endsWith("\"")) text = text.substring(0, text.length()-1);
			if (text.endsWith("'")) text = text.substring(0, text.length()-1);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return(text);
	}

	@SuppressWarnings("java:S1130")
	public Cell getCell(Cell[] row, String name) throws Exception
	{
		Integer index = header.get(formatHeaderName(name));

		if (index==null) return(null);
		if (index.intValue()>=row.length) return(null);
		return(row[index.intValue()]);
	}

	public String getValue(Cell[] row, String name) throws Exception
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
			value = cell.getContents().trim();
			value = Tools.replace(value , Constants.NON_BREAKING_SPACE, " ");
			value = value.trim();
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

	public int getIntValue(Cell[] row, String name) throws Exception
	{
		return(getIntValue(getCell(row, name)));
	}

	/**
	 * vrati celociselnu hodnotu bunky
	 * @param cell
	 * @return
	 */
	public static int getIntValue(Cell cell)
	{
		int value = 0;
		try
		{
			value = Integer.parseInt(getValue(cell));
		}
		catch (Exception ex)
		{
			Logger.debug(ExcelImportJXL.class, "Failed to parse int from: " +cell);
		}
		return(value);
	}

	public boolean getBooleanValue(Cell[] row, String name) throws Exception
	{
		if(Tools.isEmpty(name)) {
			return false;
		}
		return(getBooleanValue(getCell(row, name)));
	}

	/**
	 * vrati true ak je v bunke napisane ano, pravda, alebo nieco podobne
	 * @param cell
	 * @return
	 */
	public static boolean getBooleanValue(Cell cell)
	{
		boolean value = false;
		String sValue = getValue(cell);
		if ("ano".equalsIgnoreCase(sValue) || "áno".equalsIgnoreCase(sValue) ||
			 "pravda".equalsIgnoreCase(sValue)  || "x".equalsIgnoreCase(sValue) ||
			 "a".equalsIgnoreCase(sValue) || "1".equalsIgnoreCase(sValue) ||
			 "true".equalsIgnoreCase(sValue))
		{
			value = true;
		}
		return(value);
	}

	/**
	 * @return Returns the fileName.
	 */
	public String getFileName()
	{
		return fileName;
	}
	/**
	 * @param fileName The fileName to set.
	 */
	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	public double getDouble(Cell[] row, String name) throws Exception
	{
		return(getDouble(getCell(row, name)));
	}

	public double getDouble(Cell[] row, String name, double defaultValue) throws Exception
	{
		return(getDouble(getCell(row, name),defaultValue));
	}

	protected double getDouble(Cell cell)
	{
		return(getDouble(cell,0));
	}

	protected double getDouble(Cell cell, double defaultValue)
	{
		double ret = defaultValue;
		try
		{
			if (cell.getType() == CellType.NUMBER)
			{
			  NumberCell nc = (NumberCell) cell;
			  ret = nc.getValue();
			}
			else
			{
				String str = cell.getContents().trim();
			   str = str.replace(',', '.');
			   str = Tools.replace(str, " ", "");
			   //tvrda medzera
			   str = Tools.replace(str, Constants.NON_BREAKING_SPACE, "");
			   str = str.trim();
			   //System.out.println("parsing number: " + str + " row="+rowCounter);
				ret = Double.parseDouble(str);
			}
		}
		catch (Exception e)
		{

		}
		//Logger.println(this,"getDouble()="+ret);
		return(ret);
	}

	public void log(String text)
	{
		out.println(rowCounter + ". " + text + "<br>");
	}

	public void logErr(String text)
	{
		out.println("<span class='error'>" + rowCounter + ". " + text + "</span><br>");
	}

	/**
	 * Vrati nazov stlpca pre zadanu poziciu, alebo null
	 * @param pos
	 * @return
	 */
	public String getHeaderName(int pos)
	{
		if (pos > headerNames.length)
		{
			return(null);
		}
		return(headerNames[pos]);
	}

	public String getHeaderNameOriginal(int pos)
	{
		if (pos > headerNamesOriginal.length)
		{
			return(null);
		}
		return(headerNamesOriginal[pos]);
	}
	/** vrati Date pre zadanu bunku
	 * @param cell
	 * @return
	 */
	public Date getDateValue(Cell[] row, String name) throws Exception
	{
		return(getDateValue(getCell(row, name)));
	}
	public static Date getDateValue(Cell cell)
	{
		return getDateValue(getValue(cell));
	}
	static Date getDateValue(String cellValue)
	{
		if (cellValue==null || Tools.isEmpty(cellValue)) return null;
		Date date = null;
		String val = trim(cellValue);
		try
		{
			int dni = Integer.parseInt(val);
			//je to cislo, excel format
			GregorianCalendar gc = new GregorianCalendar(1900, Calendar.JANUARY, 1);
		   gc.add(Calendar.DATE, dni-1);
		   gc.add(Calendar.DAY_OF_MONTH, -1);
		   date = new Date(gc.getTimeInMillis());
		}
		catch(Exception e)
		{
			if(val.indexOf('/')>0) //NOSONAR
			{
				//americky format
				SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
				try
				{
					date = sdf.parse(val);

					//test na short format
					Calendar cal = Calendar.getInstance();
					int yearAdd = 2000;
					if (cal.get(Calendar.YEAR) >= 2100) yearAdd = 2100;

					cal.setTimeInMillis(date.getTime());
					if (cal.get(Calendar.YEAR) < 100)
					{
						cal.set(Calendar.YEAR, cal.get(Calendar.YEAR)+yearAdd);
						date = cal.getTime();
					}

				}catch(Exception ex) {}
			}
			else
			{
				date = new Date(DB.getTimestamp(val));
			}
		}
		return date;
	}

	/**
	 * trimne aj tvrde medzery
	 * @param s
	 * @return
	 */
	public static String trim(String s)
	{
		if(s == null) return null;
		s = s.trim();
		char[] space={160};
		while(s.endsWith(new String(space)) || s.endsWith(" ") || s.endsWith("\t"))
			s = s.substring(0,s.length()-1);
		while(s.startsWith(new String(space)) || s.startsWith(" ") || s.startsWith("\t"))
			s = s.substring(0,s.length()-1);
		s = s.trim();
		return s;
	}
}

