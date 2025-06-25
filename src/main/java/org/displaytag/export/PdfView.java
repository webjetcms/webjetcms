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
package org.displaytag.export;

//import java.awt.Color;
import java.io.OutputStream;
//import java.util.Iterator;
//import java.util.List;

import jakarta.servlet.jsp.JspException;

/*
import com.Xlowagie.text.BadElementException;
import com.Xlowagie.text.Cell;
import com.Xlowagie.text.Chunk;
import com.Xlowagie.text.Document;
import com.Xlowagie.text.Element;
import com.Xlowagie.text.Font;
import com.Xlowagie.text.FontFactory;
import com.Xlowagie.text.HeaderFooter;
import com.Xlowagie.text.PageSize;
import com.Xlowagie.text.Paragraph;
import com.Xlowagie.text.Phrase;
import com.Xlowagie.text.Rectangle;
import com.Xlowagie.text.Table;
import com.Xlowagie.text.pdf.PdfWriter;
*/

//import org.apache.commons.lang.ObjectUtils;
//import org.apache.commons.lang.StringUtils;
import org.displaytag.Messages;
import org.displaytag.exception.BaseNestableJspTagException;
import org.displaytag.exception.SeverityEnum;
//import org.displaytag.model.Column;
//import org.displaytag.model.ColumnIterator;
//import org.displaytag.model.HeaderCell;
//import org.displaytag.model.Row;
//import org.displaytag.model.RowIterator;
import org.displaytag.model.TableModel;
//import org.displaytag.properties.TableProperties;
//import org.displaytag.util.TagConstants;

//import sk.iway.iwcm.Tools;
//import sk.iway.iwcm.common.SearchTools;


/**
 * PDF exporter using IText. This class is provided more as an example than as a "production ready" class: users
 * probably will need to write a custom export class with a specific layout.
 * @author Ivan Markov
 * @author Fabrizio Giustina
 * @version $Revision: 1.3 $ ($Author: jeeff $)
 */
public class PdfView implements BinaryExportView
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
     * This is the table, added as an Element to the PDF document. It contains all the data, needed to represent the
     * visible table into the PDF
     */
    //private Table tablePDF;

    /**
     * The default font used in the document.
     */
    //private Font smallFont;

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
     * Initialize the main info holder table.
     * @throws BadElementException for errors during table initialization
     */
    protected void initTable() //throws BadElementException
    {
        /*tablePDF = new Table(this.model.getNumberOfColumns());
        tablePDF.setDefaultVerticalAlignment(Element.ALIGN_TOP);
        tablePDF.setCellsFitPage(true);
        tablePDF.setWidth(100);
        tablePDF.setPadding(2);
        tablePDF.setSpacing(0);
        FontFactory.defaultEncoding="windows-1250";
        smallFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.NORMAL, new Color(0, 0, 0));*/

    }

    /**
     * @see org.displaytag.export.BaseExportView#getMimeType()
     * @return "application/pdf"
     */
    @Override
    public String getMimeType()
    {
        return "application/pdf"; //$NON-NLS-1$
    }

    /**
     * The overall PDF table generator.
     * @throws JspException for errors during value retrieving from the table model
     * @throws BadElementException IText exception
     */
    protected void generatePDFTable() throws JspException //, BadElementException
    {
        /*
        if (this.header)
        {
            generateHeaders();
        }
        tablePDF.endHeaders();
        generateRows();
        */
    }

    /**
     * @see org.displaytag.export.BinaryExportView#doExport(OutputStream)
     */
    @Override
    public void doExport(OutputStream out) throws JspException
    {
        /*
        try
        {
            // Initialize the table with the appropriate number of columns
            initTable();

            // Initialize the Document and register it with PdfWriter listener and the OutputStream
            Document document = new Document(PageSize.A4.rotate(), 60, 60, 40, 40);
            document.addCreationDate();
            HeaderFooter footer = new HeaderFooter(new Phrase(TagConstants.EMPTY_STRING, smallFont), true);
            footer.setBorder(Rectangle.NO_BORDER);
            footer.setAlignment(Element.ALIGN_CENTER);

            PdfWriter.getInstance(document, out);
            document.setFooter(footer);

            // Fill the virtual PDF table with the necessary data
            generatePDFTable();
            document.open();


            //MBO: Umoznuje pridat riadky pred a za tabulku do exportu

            TableProperties tableProperties = this.model.getProperties();
            for (int i=0;i<10;i++)
            {
            	String tableCaption = Tools.getStringValue(tableProperties.getProperty("export.pdf.table.before"+(i>0?i:"")),
            			tableProperties.getProperty("export.table.before"+(i>0?i:"")));
            	if (Tools.isNotEmpty(tableCaption))
            		document.add(new Paragraph(tableCaption));
            	else
            		if (i==0)
            			continue;
            		else
            			break;
            }

            document.add(this.tablePDF);

            for (int i=0; i<10; i++)
            {
	            String tableFooter = Tools.getStringValue(tableProperties.getProperty("export.pdf.table.after"+(i>0?i:"")),
	            		tableProperties.getProperty("export.table.after"+(i>0?i:"")));
	            if (Tools.isNotEmpty(tableFooter))
	            	document.add(new Paragraph(tableFooter));
	            else
	            	if (i==0)
            			continue;
            		else
            			break;
            }

            document.close();

        }
        catch (Exception e)
        {
            throw new PdfGenerationException(e);
        }
        */
    }

    /**
     * Generates the header cells, which persist on every page of the PDF document.
     * @throws BadElementException IText exception
     */
    @SuppressWarnings("unchecked")
	protected void generateHeaders() //throws BadElementException
    {
        /*
        Iterator<HeaderCell> iterator = ((List<HeaderCell>) this.model.getHeaderCellList()).iterator();

        while (iterator.hasNext())
        {
            HeaderCell headerCell = (HeaderCell) iterator.next();

            String columnHeader = SearchTools.htmlToPlain(headerCell.getTitle());

            if (columnHeader == null)
            {
                columnHeader = StringUtils.capitalize(headerCell.getBeanPropertyName());
            }

            Cell hdrCell = getCell(columnHeader);
            hdrCell.setGrayFill(0.9f);
            hdrCell.setHeader(true);
            tablePDF.addCell(hdrCell);

        }
        */
    }

    /**
     * Generates all the row cells.
     * @throws JspException for errors during value retrieving from the table model
     * @throws BadElementException errors while generating content
     */
    protected void generateRows() throws JspException //, BadElementException
    {
        /*
        // get the correct iterator (full or partial list according to the exportFull field)
        RowIterator rowIterator = this.model.getRowIterator(this.exportFull);
        // iterator on rows
        while (rowIterator.hasNext())
        {
            Row row = rowIterator.next();

            // iterator on columns
            ColumnIterator columnIterator = row.getColumnIterator(this.model.getHeaderCellList());

            while (columnIterator.hasNext())
            {
                Column column = columnIterator.nextColumn();

                // Get the value to be displayed for the column
               Object value = column.getValue(this.decorated);
//***************************************************************************************PRIDANE
               String text = "";
         		if(value != null) {
         			text = ObjectUtils.toString(value);
         			text = Tools.unescapeHtmlEntities(text);
         			text = SearchTools.htmlToPlain(text).trim();
         			if(text.endsWith("&nbsp;")) {
            			text=text.substring(0,text.length()-6);
         			}
         		}
//*****************************************************************************************
                Cell cell = getCell(text);//Cell cell = getCell(value);
                tablePDF.addCell(cell);
            }
        }
        */
    }

    /**
     * Returns a formatted cell for the given value.
     * @param value cell value
     * @return Cell
     * @throws BadElementException errors while generating content
     */
    /*private Cell getCell(String value) throws BadElementException
    {
        Cell cell = new Cell(new Chunk(StringUtils.trimToEmpty(value), smallFont));
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setLeading(8);
        return cell;
    }*/

    /**
     * Wraps IText-generated exceptions.
     * @author Fabrizio Giustina
     * @version $Revision: 1.3 $ ($Author: jeeff $)
     */
    static class PdfGenerationException extends BaseNestableJspTagException
    {

        /**
         * D1597A17A6.
         */
        private static final long serialVersionUID = 899149338534L;

        /**
         * Instantiate a new PdfGenerationException with a fixed message and the given cause.
         * @param cause Previous exception
         */
        public PdfGenerationException(Throwable cause)
        {
            super(PdfView.class, Messages.getString("PdfView.errorexporting"), cause); //$NON-NLS-1$
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