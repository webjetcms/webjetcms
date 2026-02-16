package sk.iway.iwcm.components.basket;

import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import jxl.Cell;
import jxl.Sheet;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.xls.ExcelImportJXL;

/**
 *
 * Naimportuje z exceluje cennik a updatuje polozky, ktore sa v dokumente nachadzali
 *
 *
 *  PricelistExcelImport.java
 *
 *@Title        webjet6
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 19.6.2008 11:24:42
 *@modified     $Date: 2010/01/20 10:12:27 $
 */
public final class PricelistExcelImport extends ExcelImportJXL
{
	//rozpoznavane meny
	private static final List<String> supportedCurrencies = Arrays.asList(Constants.getString("supportedCurrencies").split(","));

	private String titleFieldName;

	private String quantityFieldName;

	private String priceFieldName;

	private String vatFieldName;

	private String currencyFieldName;

	private String docIdFieldName;

	private String partNumberFieldName;

	private boolean isImportedByDocId;

	public PricelistExcelImport(InputStream in, HttpServletRequest request, PrintWriter out)
	{
		super(in, request, out);
		//------------------------ZISTI, AKE SU HLAVICKY STLPCOV--------------------
		titleFieldName 		= trimHeaderName(request.getParameter("title"));
		quantityFieldName 	= trimHeaderName(request.getParameter("quantity"));
		currencyFieldName 	= trimHeaderName(request.getParameter("currency"));
		priceFieldName 		= trimHeaderName(request.getParameter("price"));
		vatFieldName	 		= trimHeaderName(request.getParameter("vat"));
		docIdFieldName			= trimHeaderName(request.getParameter("docId"));
		partNumberFieldName	= trimHeaderName(request.getParameter("partNumber"));
		isImportedByDocId = docIdFieldName != null;
	}

	@Override
	protected void saveRow(Connection db_conn, Cell[] row, Sheet sheet, Prop prop) throws Exception
	{
		checkHeaderNames();
		//musia byt uvedene udaje titul, cena, dph, mena
		if (row==null || row.length < 4 || !areHeaderNamesOk)
			return;

		PreparedStatement ps = null;

		if (!isRowValid(row, prop))
			return;

		String vat = getValue(row, vatFieldName).trim();
		vat = vat.endsWith("%") ? vat.substring(0, vat.length() -1).trim() : vat;
		try
		{
			println("Importujem "+getValue(row, titleFieldName), rowCounter);

			//------------------------ZAPIS V DATABAZU--------------------------
			//TODO mozna SQL injection, treba zistit, ci sa to inak neda
			String sql = "UPDATE documents SET "+
				formatFieldName(Constants.getString("basketPriceField"))+"=?, "+
				formatFieldName(Constants.getString("basketVatField"))+"=?, "+
				formatFieldName(Constants.getString("basketCurrencyField"))+"=?, "+
				formatFieldName(Constants.getString("basketQuantityField"))+"=? ";
			//dva mozne pripady - update podla docId alebo podla partNo
			sql += isImportedByDocId ? "WHERE doc_id = ?" : "WHERE "+formatFieldName(Constants.getString("basketPartNoField"))+" = ?";
			ps = db_conn.prepareStatement(sql);

			double price = getDouble(row, priceFieldName, 0);
			String priceStr;
			if (price != 0) {
				priceStr = Tools.replace( String.valueOf(price).replaceAll("\\s+", "") , ".", ",");
			} else {
				priceStr = getValue(row, priceFieldName).replaceAll("\\s+", "");
			}

			ps.setString(1, priceStr);
			ps.setString(2, vat);
			ps.setString(3, getValue(row, currencyFieldName).toLowerCase().replaceAll("\\s+", ""));
			//musime sa rozhodnut, ci budeme modifikovat zaznamy v databaze podla docid alebo podla partNo
			String updateKey = isImportedByDocId ? docIdFieldName : partNumberFieldName;
			ps.setString(4, String.valueOf(getIntValue(row, quantityFieldName)));
			ps.setString(5, getValue(row, updateKey));
			int returnedRows = ps.executeUpdate();
			//------------------------KONIEC ZAPISU DAT--------------------------
			if (returnedRows == 0)
				printlnError(prop.getText("components.basket.price_import.title_not_found", getValue(row, updateKey)),rowCounter);
			if (returnedRows > 1)
				println(prop.getText("components.basket.price_import.more_rows_affected", getValue(row, updateKey),Integer.toString(returnedRows)),rowCounter);
			ps.close();
			ps = null;
		}
		catch (Exception ex)
		{
			printlnError("Error, not importing");
			sk.iway.iwcm.Logger.error(ex);
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

	@SuppressWarnings("java:S6353")
	private boolean isRowValid(Cell[] row,Prop prop)
	{
		try
		{
			//-------------------------------VALIDACIE---------------------------
			if (Tools.isEmpty(getValue(row, titleFieldName)))
				return false;
			if (Tools.getIntValue(getValue(row, quantityFieldName), -2) < 0)
				return false;
			String currency = getValue(row, currencyFieldName).toLowerCase().trim();
			if (!supportedCurrencies.contains(currency))
			{
				printlnError(prop.getText("components.basket.price_import.unknown_currency", currency,supportedCurrencies.toString()),rowCounter);
				return false;
			}
			String price = getValue(row, priceFieldName).trim().replaceAll("\\s+", "");
			if (!price.matches("^[0-9]+([,.][0-9]+)?$"))
			{
				printlnError(prop.getText("components.basket.price_import.bad_price_format", price),rowCounter);
				return false;
			}
			String vat = getValue(row, vatFieldName).trim();
			vat = vat.endsWith("%") ? vat.substring(0, vat.length() -1).trim() : vat;
			if (!vat.matches("^[0-9]+([,.][0-9]+)?$"))
			{
				printlnError(prop.getText("components.basket.price_import.bad_vat_format", vat),rowCounter);
				return false;
			}
			//----------------------KONIEC VALIDACII-------------------------------
			return true;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			return false;
		}
	}


	private boolean alreadyChecked = false;
	private boolean areHeaderNamesOk = false;

	/**
	 * Skontroluje, ci zadane hlavicky stlpcov naozaj su v spracovavanom
	 * excelovskom subore.
	 */
	private void checkHeaderNames()
	{
		if (!alreadyChecked)
		{
			//--------------------SEDIA MENA HEADROV?------------------------
			areHeaderNamesOk = true;
			List<String> headerNames = Arrays.asList(headerNamesOriginal);
			//prehodime mena stlpcov na male pismena, aby sme mali obe v lowerCase
			for (int columnIndex = 0; columnIndex < headerNames.size(); columnIndex++)
				headerNames.set(columnIndex, headerNames.get(columnIndex).toLowerCase().trim() );

			if (!(headerNames.contains(titleFieldName) && headerNames.contains(quantityFieldName) && headerNames.contains(currencyFieldName) &&
			 		headerNames.contains(priceFieldName) && headerNames.contains(vatFieldName) &&
					(headerNames.contains(docIdFieldName) || headerNames.contains(partNumberFieldName)) ) )
			{
				printlnError(Prop.getInstance(request).getText("components.basket.price_import.column_header_mismatch"),rowCounter);
				areHeaderNamesOk = false;
			}
			//--------------------KONIEC KONTROLY------------------------
		}
		alreadyChecked =true;
	}


	/**
	 * Mena, cena a dph sa budu zapisovat zrejme do field_x v tabulke documents, ale
	 * my o ich vieme iba, ze sa ukladaju vo fieldJ, fieldK a podobne. tato metoda sluzi na
	 * namapovanie fieldX na field_x.
	 * @param fieldName
	 * @return
	 */
	private String formatFieldName (String fieldName)
	{
		String fieldNameCopy = fieldName.toLowerCase().split("\\s")[0];

		if (!fieldNameCopy.toLowerCase().startsWith("field"))
			return fieldName;

		return "field_"+fieldNameCopy.charAt( fieldNameCopy.length() - 1 );
	}

	/**
	 * Sluzi na premazanie stavu kontroly po kazdom spracovanom sheete.
	 */
	@Override
	public void importSheet(Sheet sheet, Prop prop)
	{
		super.importSheet(sheet, prop);
		alreadyChecked = false;
		areHeaderNamesOk = false;
	}

	private String trimHeaderName(String headerName)
	{
		return (headerName == null ? headerName : headerName.toLowerCase().trim());
	}
}
