package sk.iway.tags;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import jakarta.servlet.jsp.JspTagException;
import jakarta.servlet.jsp.tagext.BodyContent;
import jakarta.servlet.jsp.tagext.BodyTagSupport;

import org.displaytag.tags.TableTagParameters;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/**
 *  Will format body of tag (number) to currency value
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jraska $
 *@version      $Revision: 1.10 $
 *@created      $Date: 2009/08/18 09:35:41 $
 */
public class CurrencyTag extends BodyTagSupport
{
	private static final long serialVersionUID = -1098391991357460522L;
	private Boolean round = null;
	private String currency = null;
	private String format = null;
	private int groupingSize = 3;
	private static DecimalFormat nf;
	private static DecimalFormatSymbols symbols;

	static
	{
		  symbols = null;

		  String locale = Constants.getString("currencyFormatLocale");
		  if ("UK".equals(locale)) symbols = new DecimalFormatSymbols(java.util.Locale.UK);
		  else if ("US".equals(locale)) symbols = new DecimalFormatSymbols(java.util.Locale.US);
		  else if ("FRANCE".equals(locale)) symbols = new DecimalFormatSymbols(java.util.Locale.FRANCE);
		  else symbols = new DecimalFormatSymbols(java.util.Locale.GERMANY);

		  if (Constants.getString("currencyFormatDecimalSeparator")!=null && Constants.getString("currencyFormatDecimalSeparator").length()>0) symbols.setDecimalSeparator(Constants.getString("currencyFormatDecimalSeparator").charAt(0));
		  if (Constants.getString("currencyFormatGroupingSeparator")!=null && Constants.getString("currencyFormatGroupingSeparator").length()>0)  symbols.setGroupingSeparator(Constants.getString("currencyFormatGroupingSeparator").charAt(0));

		  nf = new DecimalFormat(Constants.getString("currencyFormat"), symbols);
		  nf.setGroupingSize(3);
		  nf.setGroupingUsed(true);
	}

   /**
    *  Description of the Method
    *
    *@return                      Description of the Return Value
    *@exception  JspTagException  Description of the Exception
    */
   @Override
	public int doAfterBody() throws JspTagException
   {
   	DecimalFormat formater;
	   if(Tools.isEmpty(format))
	   {
	   	if (groupingSize > -1)
	   	{
	   		formater = new DecimalFormat(Constants.getString("currencyFormat"), symbols);
	   	}
	   	else
	   	{
	   		formater = nf;
	   	}
	   }
	   else
	   {
		   formater = new DecimalFormat(format, symbols);
	   }
	   if(groupingSize > -1)
	   {
		   formater.setGroupingSize(groupingSize);
		   formater.setGroupingUsed(true);
	   }

      BodyContent bc = getBodyContent();
      String body = bc.getString();
      bc.clearBody();
      //NumberFormat nf = NumberFormat.getCurrencyInstance();

		if (round == null) round = Boolean.valueOf(Constants.getBoolean("currencyTagRound"));

		boolean isExport = (pageContext.getRequest().getParameter(TableTagParameters.PARAMETER_EXPORTING) != null);

      try
      {
         String out = body;
         try
         {
            double number = Double.parseDouble(body);

            if (round)
            {
            	//zaokruhli cislo
            	int celeCislo = (int)number;
            	double zvysok = number - celeCislo;

            	if (zvysok < 0.3) number = celeCislo;
            	else if (zvysok < 0.7) number = celeCislo + 0.5;
            	else if (zvysok < 1) number = celeCislo + 1;
            }

            out = formater.format(number);

            if (Tools.isNotEmpty(currency))
            {
            	out += " "+getLabelFromCurrencyCode(currency);
            }
            if (isExport==false) out = Tools.replace(out, " ", "&nbsp;");
         }
         catch (Exception ex)
         {
         }

			if (isExport) out = Tools.replace(out, " ", "");

         getPreviousOut().print(out);
      }
      catch (IOException e)
      {
         throw new JspTagException("CurrencyTag: " +
               e.getMessage());
      }
      return SKIP_BODY;
   }

   /**
    * Naformatuje <code>double</code> cislo
    * @param number
    * @return
    */
   public static String formatNumber(double number)
   {
   	return nf.format(number);
   }

   /**
    * Naformatuje <code>BigDecimal</code> cislo
    * @param number
    * @return
    */
   public static String formatNumber(BigDecimal number)
   {
   	return nf.format(number);
   }

   public boolean isRound()
	{
		return round;
	}

	public void setRound(boolean round)
	{
		this.round = round;
	}

	@Override
	public void release()
	{
		super.release();
		round = null;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public int getGroupingSize() {
		return groupingSize;
	}

	public void setGroupingSize(int groupingSize) {
		this.groupingSize = groupingSize;
	}

	public String getCurrency()
	{
		return this.currency;
	}

	public void setCurrency(String currency)
	{
		this.currency = currency;
	}

	/**
	 * Skonvertuje kod meny na jej verejne oznacenie (napriklad eur ¨)
	 * @param currency
	 * @return
	 */
	public static String getLabelFromCurrencyCode(String currency) {
		if ("eur".equalsIgnoreCase(currency))
			return  "€";
		if ("czk".equalsIgnoreCase(currency))
			return  "Kč";
		if ("kč".equalsIgnoreCase(currency))
			return  "Kč";
		if ("kc".equalsIgnoreCase(currency))
			return  "Kč";
		if ("gbp".equalsIgnoreCase(currency))
			return  "Ł";
		if ("usd".equalsIgnoreCase(currency))
			return  "$";
		return currency;
	}
}
