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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import sk.iway.iwcm.i18n.Prop;

/**
 *  Superclass pre import udajov z excelu do databazy, prvy riadok obsahuje nazvy stlpcov
 *  Pracuje len s XLSX, pre XLS treba pouzit sk.iway.iwcm.ExcelImport
 *
 *@Title        webjet8
 *@Company      Interway a. s. (www.interway.sk)
 *@Copyright    Interway a. s. (c) 2001-2019
 *@author       bhric $
 *@version      1.3 $
 *@created      2019/01/23 15:21
 *@modified     2004/08/16 06:26:11 $
 */
public abstract class ExcelXImport
{
   protected Map<String, Integer> header;
   protected InputStream in;
   protected HttpServletRequest request;
   protected PrintWriter out;
   protected int rowCounter;

   public ExcelXImport(InputStream in, HttpServletRequest request, PrintWriter out) //NOSONAR
   {
      this.in = in;
      this.request = request;
      this.out = out;
      rowCounter = 0;
      header = new Hashtable<>();
   }

   /**
    * Nastavi stlpec updated v databaze v tabulke tableName na false
    * @param tableName - nazov tabulky
    * @param dbName - nazov databazy, ak prazdne, berie iwcm
    * @return - true ak OK, inak false
    */
   public boolean setUpdateField(String tableName, String dbName)
   {
      boolean ret;
      Connection db_conn = null;
      PreparedStatement ps = null;
      try
      {
         db_conn = DBPool.getConnection(Tools.isNotEmpty(dbName) ? dbName : DBPool.getDBName(request));

         //nastav updated na 0, aby sme vedeli co sa v exceli nenachadza...
         ps = db_conn.prepareStatement("UPDATE "+tableName+" SET updated=?");
         ps.setBoolean(1, false);
         ps.execute();

         ps.close();
         ps = null;
         db_conn.close();
         db_conn = null;

         ret = true;
      }
      catch (Exception ex)
      {
         sk.iway.iwcm.Logger.error(ex);
         ret = false;
      }
      finally
      {
         try
         {
            if (ps != null) ps.close();
            if (db_conn != null) db_conn.close();
         }
         catch (Exception e)
         {
            sk.iway.iwcm.Logger.error(e);
         }
      }
      return ret;
   }

   /**
    * vymaze z databazy zaznamy, ktore nie su updatnute
    * @param tableName - nazov tabulky
    * @param dbName - nazov databazy, ak prazdne, berie iwcm
    * @return - true ak OK, inak false
    */
   public boolean deleteNotUpdated(String tableName, String dbName)
   {
      boolean ret;
      Connection db_conn = null;
      PreparedStatement ps = null;
      ResultSet rs = null;
      try
      {
         db_conn = DBPool.getConnection(Tools.isNotEmpty(dbName) ? dbName : DBPool.getDBName(request));

         println(out, "", -1);
         //vymaz polozky
         ps = db_conn.prepareStatement("SELECT * FROM "+tableName+" WHERE updated=?");
         ps.setBoolean(1, false);
         rs = ps.executeQuery();
         while (rs.next())
            printlnError(out, "Vymazávam: "+rs.getString(1)+", "+rs.getString(2)+", "+rs.getString(3)+", "+rs.getString(4), -1);

         rs.close();
         rs = null;
         ps.close();
         ps = null;

         ps = db_conn.prepareStatement("DELETE FROM "+tableName+" WHERE updated=?");
         ps.setBoolean(1, false);
         ps.execute();

         ps.close();
         ps = null;
         db_conn.close();
         db_conn = null;

         ret = true;
      }
      catch (Exception ex)
      {
         sk.iway.iwcm.Logger.error(ex);
         ret = false;
      }
      finally
      {
         try
         {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            if (db_conn != null) db_conn.close();
         }
         catch (Exception e)
         {
            sk.iway.iwcm.Logger.error(e);
         }
      }
      return ret;
   }

   /**
    * Nastavi hashtable, ktora prevadza meno na cislo stlpca. Meno je vzdy lowercase
    * @param row - riadok
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
         if (str!=null) str = str.toLowerCase();
         pos = cell.getColumnIndex();

         header.put(str, pos);
      }
   }

   /**
    * pridanie riadku do zoznamu riadkov
    * @param row - riadok
    */
   protected abstract void saveRow(Connection db_conn, Row row, XSSFSheet sheet, Prop prop) throws Exception;

   /**
    * moznost vykonania akcie po importovani
    * @param prop - prop
    */
   protected abstract void afterImportJob(Prop prop);

   public void doImport(Prop prop)
   {
      doImport(null, prop);
   }

   /**
    * vykonanie importu konkretneho harku
    * @param sheetName - nazov harku, alebo null pre import vsetkych harkov
    * @param prop - prop
    */
   public void doImport(String sheetName, Prop prop)
   {
      try
      {
         Logger.println(this,"doImport sheet="+sheetName);

         XSSFWorkbook wb = new XSSFWorkbook(in);

         Logger.println(this,"doImport mam wb");

         if (sheetName==null || sheetName.trim().length()<1)
         {
            //iteruj po sheetoch a importuj ich
            int counter = 0;
            XSSFSheet sheet = wb.getSheetAt(counter);
            while (sheet!=null)
            {
               Logger.println(this,"doImport: sheet="+wb.getSheetName(counter));
               println(out, "<big><b>"+prop.getText("file.importing_sheet")+" "+(counter+1)+": "+wb.getSheetName(counter)+"</b></big><br><br>", -1);
               importSheet(sheet, prop);
               counter++;
               try
               {
                  sheet = wb.getSheetAt(counter);
               }
               catch (IllegalArgumentException iobex) //Sheet index (1) is out of range (0..0)
               {
                  sheet = null;
               }
            }
         }
         else
         {
            Logger.println(this,"doImport sheet="+sheetName);

            XSSFSheet sheet = wb.getSheet(sheetName);
            importSheet(sheet, prop);
         }

         wb.close();

         afterImportJob(prop);
      }
      catch (Exception ex)
      {
         sk.iway.iwcm.Logger.error(ex);
      }

      println(out, "", -1);
   }

   /**
    * vykonanie importu harku
    * @param sheet - XSSFSheet objekt
    */
   public void importSheet(XSSFSheet sheet, Prop prop)
   {
      try
      {
         Row row;
         Iterator<Row> rows = sheet.rowIterator();

         if (!rows.hasNext()) return;

         rowCounter = 0;
         row = rows.next();
         setHeader(row);

         Connection db_conn = sk.iway.iwcm.DBPool.getConnection(request);
         try
         {
            while (rows.hasNext())
            {
               rowCounter++;
               try
               {
                  row = rows.next();
                  Logger.println(this,"save row");
                  saveRow(db_conn, row, sheet, prop);
               }
               catch (Exception ex)
               {
                  sk.iway.iwcm.Logger.error(ex);
                  printlnError(out, "Chyba importu: "+ex.getMessage(), rowCounter);
               }
            }
         }
         finally
         {
            db_conn.close();
         }
      }
      catch (Exception ex)
      {
         sk.iway.iwcm.Logger.error(ex);
         printlnError(out, "Vyskytla sa chyba - " + ex.getMessage(), rowCounter);
      }
   }

   /**
    * vypis do frontendu
    * @param out - PrintWriter
    * @param message - text
    * @param row - riadok
    */
   public static void println(PrintWriter out, String message, int row)
   {
      if (row>=0) out.print(row+": ");
      out.println(message+"<br>");
      out.flush();
   }

   /**
    * vypis chyby na frontend
    * @param out - PrintWriter
    * @param message - text
    * @param row - riadok
    */
   public static void printlnError(PrintWriter out, String message, int row)
   {
      if (row>=0) out.print(row+": ");
      out.println("<font color='red'>"+message+"</font><br>");
      out.flush();
   }

   /**
    * zmaze uvodzovky a apostrofy z textu
    * @param textPar - text z ktoreho maze
    * @return - vrateny text
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

   /**
    * ziska bunku
    * @param row - riadok
    * @param name - nazov hlavicky
    * @return - vrati objekt XSSFCell
    */
   public Cell getCell(Row row, String name)
   {
      Integer index = header.get(name.toLowerCase());

      if (index==null) return(null);

      //Logger.println(this,"get cell name="+name+" index="+index.intValue());
      return(row.getCell(index));
   }

   /**
    * ziska hodnotu bunky
    * @param row - riadok
    * @param name - nazov
    * @return - vrati hodnotu bunky
    */
   public String getValue(XSSFRow row, String name)
   {
      return(getValue(getCell(row, name)));
   }

   /**
    * Ziska hodnotu bunky ako String
    * @param cell - bunka
    * @return - vrati hodnotu bunky
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
            value = Double.toString(cell.getNumericCellValue());
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
            value = value.substring(0, value.length()-2);
      }
      catch (Exception ex)
      {
         sk.iway.iwcm.Logger.error(ex);
      }

      return value;
   }

   /**
    * vrati celociselnu hodnotu bunky
    * @param row - riadok
    * @param name - nazov
    * @return - vratena int hodnota
    */
   public int getIntValue(Row row, String name)
   {
      return(getIntValue(getCell(row, name)));
   }

   /**
    * vrati celociselnu hodnotu bunky
    * @param cell - bunka
    * @return - vratena int hodnotu
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
         sk.iway.iwcm.Logger.error(ex);
      }
      return(value);
   }

   /**
    * vrati celociselnu hodnotu bunky
    * @param row - riadok
    *     * @param name - nazov
    * @return - vratena double hodnotu
    */
   public double getDoubleValue(Row row, String name)
   {
      return(getDoubleValue(getCell(row, name)));
   }

   /**
    * vrati celociselnu hodnotu bunky
    * @param cell - bunka
    * @return - vratena double hodnotu
    */
   public static double getDoubleValue(Cell cell)
   {
      double value = 0;
      try
      {
         value = Double.parseDouble(getValue(cell));
      }
      catch (Exception ex)
      {
         sk.iway.iwcm.Logger.error(ex);
      }
      return(value);
   }

   /**
    * vrati boolean hodnotu bunky
    * @param row - riadok
    * @param name - nazov bunky
    * @return - vrati true ak je v bunke napisane ano, pravda, alebo nieco podobne
    */
   public boolean getBooleanValue(Row row, String name)
   {
      return(getBooleanValue(getCell(row, name)));
   }

   /**
    * vrati boolean hodnotu bunky
    * @param cell - bunka
    * @return - vrati true ak je v bunke napisane ano, pravda, alebo nieco podobne
    */
   public static boolean getBooleanValue(Cell cell)
   {
      boolean value = false;
      String sValue = getValue(cell);
      if ("ano".equalsIgnoreCase(sValue) || "áno".equalsIgnoreCase(sValue) || "pravda".equalsIgnoreCase(sValue)  || "x".equalsIgnoreCase(sValue) || "a".equalsIgnoreCase(sValue))
         value = true;

      return(value);
   }

   /**
    * vrati datumovu hodnotu
    * @param row - row
    * @param name - nazov stlpca
    * @return - hodnota ako Date alebo null
    */
   public Date getDateValue(XSSFRow row, String name)
   {
      Date ret = null;
      try {
         GregorianCalendar gc = new GregorianCalendar(1900, Calendar.JANUARY, 1);
         gc.add(Calendar.DATE, Integer.parseInt(getValue(row,name))-1);
         gc.add(Calendar.DAY_OF_MONTH, -1);
         ret = new Date(gc.getTimeInMillis());
      }
      catch(Exception e)
      {
         sk.iway.iwcm.Logger.error(e);
      }
      return ret;
   }

   /**
    * trimne aj tvrde medzery
    * @param sPar - retazec
    * @return - trimnuty text
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
