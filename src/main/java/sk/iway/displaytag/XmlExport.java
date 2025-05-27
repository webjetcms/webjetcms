package sk.iway.displaytag;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jakarta.servlet.jsp.JspException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.displaytag.export.ExportView;
import org.displaytag.export.TextExportView;
import org.displaytag.model.Column;
import org.displaytag.model.ColumnIterator;
import org.displaytag.model.HeaderCell;
import org.displaytag.model.Row;
import org.displaytag.model.RowIterator;
import org.displaytag.model.TableModel;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.SearchTools;
/**
 *  XmlExport.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: jeeff $
 *@version      $Revision: 1.7 $
 *@created      Date: 30.6.2006 11:49:45
 *@modified     $Date: 2008/12/11 08:40:28 $
 */





/**
 * spravne nazvy atributy podla hlavicky tabulky a odstrani anchory z exportu
 *
 *
 */
public class XmlExport implements TextExportView {


/**
     * TableModel to render.
     */
    private TableModel model;

/**
     * export full list?
     */
    private boolean exportFull;

/**
     * decorate export?
     */
    private boolean decorated;
    private List<String> headerTitles = new ArrayList<String>();


    /**
     * @see ExportView#setParameters(org.displaytag.model.TableModel, boolean, boolean, boolean)
     */
    @Override
    public void setParameters(TableModel tableModel, boolean exportFullList, boolean includeHeader,
                              boolean decorateValues) {
        this.model = tableModel;
        this.exportFull = exportFullList;
        this.decorated = decorateValues;
    }

/**
     * @see org.displaytag.export.BaseExportView#getRowStart()
     */
    protected String getRowStart() {
        return "<row>\n"; //$NON-NLS-1$
    }

/**
     * @see org.displaytag.export.BaseExportView#getRowEnd()
     */
    protected String getRowEnd() {
        return "</row>\n"; //$NON-NLS-1$
    }

/**
     * @see org.displaytag.export.BaseExportView#getCellStart()
     */
    protected String getCellStart(String columnName) {
        if (Tools.isEmpty(columnName)) columnName="column";

        columnName = DocTools.removeChars(columnName, false);

        return "<" + columnName.replace('\n', '-') + ">"; //$NON-NLS-1$
    }

/**
     * @see org.displaytag.export.BaseExportView#getCellEnd()
     */
    protected String getCellEnd(String columnName) {
        if (Tools.isEmpty(columnName)) columnName="column";

        columnName = DocTools.removeChars(columnName, false);

        return "</" + columnName.replace('\n', '-') + ">\n"; //$NON-NLS-1$
    }

/**
     * @see org.displaytag.export.BaseExportView#getDocumentStart()
     */
    protected String getDocumentStart() {
        return "<?xml version=\"1.0\" encoding=\"windows-1250\"?>\n<table>\n"; //$NON-NLS-1$
    }

/**
     * @see org.displaytag.export.BaseExportView#getDocumentEnd()
     */
    protected String getDocumentEnd() {
        return "</table>\n"; //$NON-NLS-1$
    }

/**
     * @see org.displaytag.export.BaseExportView#getAlwaysAppendCellEnd()
     */
    protected boolean getAlwaysAppendCellEnd() {
        return true;
    }

/**
     * @see org.displaytag.export.BaseExportView#getAlwaysAppendRowEnd()
     */
    protected boolean getAlwaysAppendRowEnd() {
        return true;
    }

/**
     * @see org.displaytag.export.ExportView#getMimeType()
     */
    @Override
    public String getMimeType() {
        return "text/xml"; //$NON-NLS-1$
    }

/**
     * @see org.displaytag.export.BaseExportView#escapeColumnValue(java.lang.Object)
     */
    protected String escapeColumnValue(Object value) {
    	String text="";
		if(value != null) {
			text=value.toString().trim();
			if(text.endsWith("&nbsp;"))
   			text=text.substring(0,text.length()-6);
			text = SearchTools.htmlToPlain(text);
		}
            return StringEscapeUtils.escapeXml(text.trim());
    }

/**
     * @see TextExportView#doExport(java.io.Writer)
     */
    @Override
    public void doExport(Writer out, String characterEncoding) throws IOException, JspException {


final String DOCUMENT_START = getDocumentStart();
        final String DOCUMENT_END = getDocumentEnd();
        final String ROW_START = getRowStart();
        final String ROW_END = getRowEnd();
        final boolean ALWAYS_APPEND_CELL_END = getAlwaysAppendCellEnd();
        final boolean ALWAYS_APPEND_ROW_END = getAlwaysAppendRowEnd();

buildHeaders();

// document start
        write(out, DOCUMENT_START);

// get the correct iterator (full or partial list according to the exportFull field)
        RowIterator rowIterator = this.model.getRowIterator(this.exportFull);

// iterator on rows
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

if (this.model.getTableDecorator() != null) {

String stringStartRow = this.model.getTableDecorator().startRow();
                write(out, stringStartRow);
            }

// iterator on columns
            ColumnIterator columnIterator = row.getColumnIterator(this.model.getHeaderCellList());

write(out, ROW_START);

int columnNumber = 0;
            while (columnIterator.hasNext()) {
                Column column = columnIterator.nextColumn();
                String columnHeader = DB.internationalToEnglish(headerTitles.get(columnNumber));
                String cellStart = getCellStart(columnHeader);
                String cellEnd = getCellEnd(columnHeader);

// Get the value to be displayed for the column
                String value = escapeColumnValue(column.getValue(this.decorated));

write(out, cellStart);

write(out, value);

if (ALWAYS_APPEND_CELL_END || columnIterator.hasNext()) {
                    write(out, cellEnd);
                }
                columnNumber++;
            }
            if (ALWAYS_APPEND_ROW_END || rowIterator.hasNext()) {
                write(out, ROW_END);
            }
        }

// document end

        write(out, DOCUMENT_END);

}

/**
     * Write a String, checking for nulls value.
     *
     * @param out output writer
     * @param string String to be written
     * @throws java.io.IOException thrown by out.write
     */
    private void write(Writer out, String string) throws IOException {
        if (string != null) {
            out.write(string);
        }
    }

/**
     * @see TextExportView#outputPage()
     */
    @Override
    public boolean outputPage() {
        return false;
    }

/**
     * iterates through the headers once, placing the titles in an ArrayList for
     * quick'n'easy access as we iterate through each row
     */
    private void buildHeaders()
    {
        headerTitles.clear();
        @SuppressWarnings("unchecked")
        Iterator<HeaderCell> iterator = this.model.getHeaderCellList().iterator();

while (iterator.hasNext())
        {
            HeaderCell headerCell = iterator.next();
            String columnHeader = DB.internationalToEnglish(headerCell.getTitle());
            if (columnHeader == null)
            {
                columnHeader = headerCell.getBeanPropertyName();
            }

if (columnHeader != null)
            {
                columnHeader = StringUtils.deleteWhitespace(WordUtils.capitalizeFully(columnHeader));
                columnHeader = escapeColumnValue(columnHeader);
            }

headerTitles.add(columnHeader);

}

}

}
