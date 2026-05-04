package sk.iway.iwcm.findexer;

import java.io.InputStream;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.CellType;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.io.IwcmInputStream;

/**
 *  Vytiahne z Excelu vsetky texty
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Utorok, 2004, január 20
 *@modified     $Date: 2004/01/22 12:30:40 $
 */
public class Excel
{

   /**
    *  Gets the text attribute of the XLS class
    *
    *@param  fileName  Description of the Parameter
    *@return           The text value
    */
   public static String getText(String fileName)
   {
   	StringBuilder sb = new StringBuilder();
      try
      {
         InputStream is = new IwcmInputStream(fileName);
         // parse Excel sheet
         POIFSFileSystem fs = new POIFSFileSystem(is);
         HSSFWorkbook wb = new HSSFWorkbook(fs);

         try
         {

            for (int k = 0; k < wb.getNumberOfSheets(); k++)
            {
               HSSFSheet sheet = wb.getSheetAt(k);
               int rows = sheet.getPhysicalNumberOfRows();

               for (int r = 0; r < rows; r++)
               {
                  HSSFRow row = sheet.getRow(r);
                  if (row != null)
                  {
                     int cells = row.getPhysicalNumberOfCells();

                     for (int c = 0; c < cells; c++)
                     {
                        HSSFCell cell = row.getCell(c);
                        String value = null;
                        if (cell != null)
                        {
                           CellType cellType = cell.getCellType();
                           {
                              if (cellType.equals(CellType.STRING))
                                 value = cell.getStringCellValue();
                              else if (cellType.equals(CellType.NUMERIC))
                                 value = Integer.toString(((int)cell.getNumericCellValue()));
                              else if (cellType.equals(CellType.BLANK))
                                 value = "";
                              else if (cellType.equals(CellType.BOOLEAN))
                                 value = Boolean.toString(cell.getBooleanCellValue());
                              else
                                 Logger.error(Excel.class,"nejaky cudny celltype: " + cell.getCellType());
                           }
                        }
                        if (value != null)
                        {
                           sb.append(' ');
                           sb.append(value);
                        }
                     }
                  }
               }
            }
         }
         catch (Exception e)
         {
            sk.iway.iwcm.Logger.error(e);
         }
         is.close();
         wb.close();

         return(sb.toString());
      }
      catch (Exception ex)
      {

      }

      return (null);
   }
}
