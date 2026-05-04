/*
 * Copyright (C) 2002-2024 Fabrizio Giustina, the Displaytag team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.displaytag.export.excel;

import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import jakarta.servlet.jsp.JspException;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.displaytag.Messages;
import org.displaytag.exception.BaseNestableJspTagException;
import org.displaytag.exception.SeverityEnum;
import org.displaytag.export.BinaryExportView;
import org.displaytag.model.Column;
import org.displaytag.model.ColumnIterator;
import org.displaytag.model.HeaderCell;
import org.displaytag.model.Row;
import org.displaytag.model.RowIterator;
import org.displaytag.model.TableModel;
import org.displaytag.properties.TableProperties;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.SearchTools;


/**
 * Excel exporter using POI HSSF.
 */
public class ExcelHssfView implements BinaryExportView
{

    /**
     * TableModel to render.
     */
    private TableModel model;

    /**
     * export full list?
     */
    private boolean exportFull;

    /**
     * include header in export?
     */
    private boolean header;

    /**
     * decorate export?
     */
    private boolean decorated;

    /**
     * Generated sheet.
     */
    private HSSFSheet sheet;

    /**
     * @see org.displaytag.export.ExportView#setParameters(TableModel, boolean, boolean, boolean)
     */
    @Override
    public void setParameters(TableModel tableModel, boolean exportFullList, boolean includeHeader,
        boolean decorateValues)
    {
        this.model = tableModel;
        this.exportFull = exportFullList;
        this.header = includeHeader;
        this.decorated = decorateValues;
    }

    /**
     * @return "application/vnd.ms-excel"
     * @see org.displaytag.export.BaseExportView#getMimeType()
     */
    @Override
    public String getMimeType()
    {
        return "application/vnd.ms-excel"; //$NON-NLS-1$
    }

    /**
     * Do export.
     *
     * @param out
     *            the out
     *
     * @throws JspException
     *             the jsp exception
     *
     * @see org.displaytag.export.BinaryExportView#doExport(OutputStream)
     */
    @Override
    public void doExport(OutputStream out) throws JspException
    {
        try
        {
            HSSFWorkbook wb = new HSSFWorkbook();
            sheet = wb.createSheet("-");

            int rowNum = 0;
            int colNum = 0;

            HSSFCellStyle highLightStyle = wb.createCellStyle();
            highLightStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            highLightStyle.setFillForegroundColor(HSSFColorPredefined.BLUE_GREY.getIndex());

            HSSFFont bold = wb.createFont();
            bold.setBold(true);
            bold.setColor(HSSFColorPredefined.BLACK.getIndex());
            highLightStyle.setFont(bold);

            HSSFCellStyle headerStyle = wb.createCellStyle();

            headerStyle.setFont(bold);

            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setFillForegroundColor(HSSFColorPredefined.GREY_25_PERCENT.getIndex());
            //headerStyle.setFillBackgroundColor(new HSSFColor.RED().getIndex());


            // pozri ci nieje treba pridat nastaveny text pred a za tabulku
            TableProperties tableProperties = this.model.getProperties();
            boolean pridajPrazdnyRiadok=false;
            for (int i=0;i<10;i++)
            {
	            String caption = Tools.getStringValue(tableProperties.getProperty("export.excel.table.before"+(i>0?i:"")),
	            		tableProperties.getProperty("export.table.before"+(i>0?i:"")));
	            if (Tools.isNotEmpty(caption))
	            {
	            	HSSFRow xlsRow = sheet.createRow(rowNum++);
	            	HSSFCell cell =  xlsRow.createCell(0);
	            	cell.setCellValue(caption);
	            	pridajPrazdnyRiadok=true;
	            }
	            else
	            {
	            	if (i==0)
            			continue;
            		else
            		{
            			if (pridajPrazdnyRiadok)
    	            	{
    	            		//za posledny riadok pridaj jeden prazdny riadok
    	            		HSSFRow xlsRow2 = sheet.createRow(rowNum++);
    		            	HSSFCell cell2 =  xlsRow2.createCell(0);
    		            	cell2.setCellValue("");
    	            	}
            			break;
            		}


	            }
            }

            if (this.header)
            {
                // Create an header row
                HSSFRow xlsRow = sheet.createRow(rowNum++);

                Iterator<HeaderCell> iterator = (Iterator<HeaderCell>)this.model.getHeaderCellList().iterator();

                while (iterator.hasNext())
                {
                    HeaderCell headerCell = (HeaderCell) iterator.next();

                    String columnHeader = SearchTools.htmlToPlain(headerCell.getTitle());

                    if (columnHeader == null)
                    {
                        columnHeader = StringUtils.capitalize(headerCell.getBeanPropertyName());
                    }

                    HSSFCell cell = xlsRow.createCell(colNum++);
                    //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
                    cell.setCellValue(columnHeader);
                    cell.setCellStyle(headerStyle);
                }
            }

            // get the correct iterator (full or partial list according to the exportFull field)
            RowIterator rowIterator = this.model.getRowIterator(this.exportFull);
            // iterator on rows

            while (rowIterator.hasNext())
            {
                Row row = rowIterator.next();
                HSSFRow xlsRow = sheet.createRow(rowNum++);
                boolean highLight = false;
                colNum = 0;

                try
					 {
               	 if (row.getCellList().size()>0 && row.getCellList().get(0)!=null && row.getCellList().get(0).toString().contains("\"excel.highlight\""))
                   {
                  	 highLight  = true;
                   }
					 }
					 catch (Exception e)
					 {
						sk.iway.iwcm.Logger.error(e);
					 }

                // iterator on columns
                ColumnIterator columnIterator = row.getColumnIterator(this.model.getHeaderCellList());

                while (columnIterator.hasNext())
                {
                    Column column = columnIterator.nextColumn();

                    // Get the value to be displayed for the column
                    Object value = column.getValue(this.decorated);

                    HSSFCell cell = xlsRow.createCell(colNum++);
                    //cell.setEncoding(HSSFCell.ENCODING_UTF_16);

                    if (highLight)
                    {
                  	  cell.setCellStyle(highLightStyle);
                    }

                    if (value instanceof Number)
                    {
                        Number num = (Number) value;
                        cell.setCellValue(num.doubleValue());
                    }
                    else if (value instanceof Date)
                    {
                        cell.setCellValue((Date) value);
                    }
                    else if (value instanceof Calendar)
                    {
                        cell.setCellValue((Calendar) value);
                    }
                    else
                    {
                  	  String valueStr = escapeColumnValue(value);

                  	  if (valueStr.contains("=") && valueStr.toLowerCase().contains("cmd"))
                      {
                          //ochrana pred generovanim bunky s =cmd|/c calc.exe!A1 a vykonanie prikazu po stiahnuti XLS
                          valueStr = Tools.replace(valueStr, "=", "-");
                      }

                  	  if (valueStr.startsWith("="))
                  	  {
                  		  try
                  		  {
                  		    cell.setCellFormula(valueStr.substring(1));
                  		  }
                  		  catch (Exception ex)
                  		  {
                  			  sk.iway.iwcm.Logger.error(ex);
                  			  cell.setCellValue(valueStr);
                  		  }
                  	  }
                  	  else
                  	  {
                  		  //testni, ci to nahodou nie je cislo
                     	  double cislo = Tools.getDoubleValue(valueStr, Double.MIN_VALUE);

                     	  //prilis velke cisla asi budu skor telefonne, tu vypnem autodetekciu 0903123456
                     	  if (valueStr.length()>8 || (valueStr.startsWith("0") && cislo > 1))
                     	  {
                     		  cislo = Double.MIN_VALUE;
                     	  }

                     	  if (cislo != Double.MIN_VALUE)
                     	  {
                     		  Number num = Double.valueOf(cislo);
                     		  cell.setCellValue(num.doubleValue());
                     	  }
                     	  else
                     	  {
                     		  cell.setCellValue(DB.prepareString(valueStr, 32760));
                     	  }
                  	  }
                    }
                }
            }
            pridajPrazdnyRiadok=true;
            for (int i=0;i<10;i++)
            {
	            String afterTable = Tools.getStringValue(tableProperties.getProperty("export.table.after"+(i>0?i:"")),
	            		tableProperties.getProperty("export.excel.table.after"+(i>0?i:"")));
	            if (Tools.isNotEmpty(afterTable))
	            {
	            	if (pridajPrazdnyRiadok)
	            	{
	            		HSSFRow xlsRow2 = sheet.createRow(rowNum++);
		            	HSSFCell cell2 =  xlsRow2.createCell(0);
		            	cell2.setCellValue("");
		            	pridajPrazdnyRiadok=false;
	            	}
	            	HSSFRow xlsRow = sheet.createRow(rowNum++);
	            	HSSFCell cell =  xlsRow.createCell(0);
	            	cell.setCellValue(afterTable);

	            }
	            else
	            {
	            	if (i==0)
            			continue;
            		else
            			break;
	            }
            }
            wb.write(out);
            wb.close();
        }
        catch (Exception e)
        {
            throw new ExcelGenerationException(e);
        }
    }

    // patch from Karsten Voges
    /**
     * Escape certain values that are not permitted in excel cells.
     * @param rawValue the object value
     * @return the escaped value
     */
    protected String escapeColumnValue(Object rawValue)
    {
        if (rawValue == null)
        {
            return null;
        }
        String returnString = ObjectUtils.toString(rawValue);
        // escape the String to get the tabs, returns, newline explicit as \t \r \n
        returnString = StringEscapeUtils.escapeJava(StringUtils.trimToEmpty(returnString));
        // remove tabs, insert four whitespaces instead
        returnString = Strings.CS.replace(StringUtils.trim(returnString), "\\t", "    ");
        // remove the return, only newline valid in excel
        returnString = Strings.CS.replace(StringUtils.trim(returnString), "\\r", " ");
        // unescape so that \n gets back to newline
        returnString = StringEscapeUtils.unescapeJava(returnString);

        //WebJET - vrat zobaciky nazad
        returnString = Tools.unescapeHtmlEntities(returnString);
        return returnString;
    }

    /**
     * Wraps IText-generated exceptions.
     * @author Fabrizio Giustina
     * @version $Revision: 1.4 $ ($Author: thaber $)
     */
    static class ExcelGenerationException extends BaseNestableJspTagException
    {

        /**
         * D1597A17A6.
         */
        private static final long serialVersionUID = 899149338534L;

        /**
         * Instantiate a new PdfGenerationException with a fixed message and the given cause.
         * @param cause Previous exception
         */
        public ExcelGenerationException(Throwable cause)
        {
            super(ExcelHssfView.class, Messages.getString("ExcelView.errorexporting"), cause); //$NON-NLS-1$
        }

        /**
         * @see org.displaytag.exception.BaseNestableJspTagException#getSeverity()
         */
        @Override
        public SeverityEnum getSeverity()
        {
            return SeverityEnum.ERROR;
        }
    }

}
