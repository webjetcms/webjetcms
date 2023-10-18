package sk.iway.iwcm.components.dictionary;

import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import jxl.Cell;
import jxl.Sheet;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.dictionary.model.DictionaryBean;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.xls.ExcelImportJXL;

public class DictionaryTooltipXLSImport extends ExcelImportJXL
{
    List<DictionaryBean> deictionaryToSave = new ArrayList<>();

    public DictionaryTooltipXLSImport(InputStream in, HttpServletRequest request, PrintWriter out)
    {
        super(in, request, out);
    }

    @Override
    public void doImport(String sheetName, Prop prop)
    {
        boolean replace = Tools.getBooleanValue(request.getParameter("replace"),false);
        super.doImport(sheetName, prop);

        if(replace){
            //delete existing tooltips
            Connection db_conn = null;
            PreparedStatement ps;
            try
            {
                db_conn = DBPool.getConnection();
                ps = db_conn.prepareStatement("DELETE FROM dictionary where dictionary_group='tooltip'");
                ps.execute();
                ps.close();
            }

            catch (Exception ex)
            {
                sk.iway.iwcm.Logger.error(ex);
            }
        }

        for(DictionaryBean dic : deictionaryToSave)
        {
            if(dic.save())
                log("Importing: " + dic.getName());
        }

    }

    @Override
    protected void saveRow(Connection db_conn, Cell[] row, Sheet sheet, Prop prop)
    {
                DictionaryBean dictionaryRow = new DictionaryBean();

                dictionaryRow.setName(getValue(row[0]));
                dictionaryRow.setNameOrig(getValue(row[0]));
                dictionaryRow.setDictionaryGroup("tooltip");
                dictionaryRow.setLanguage(getValue(row[1]));
                dictionaryRow.setDomain(getValue(row[2]));
                dictionaryRow.setValue(getValue(row[3]));


                deictionaryToSave.add(dictionaryRow);


    }
}
