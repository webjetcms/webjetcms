package sk.iway.iwcm.ebanking.file_export;

import java.text.SimpleDateFormat;
import java.util.Date;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.ebanking.Payment;


/**
 *  VubPaymentFileExporter.java
 *
 *		Exporter with VUB specific behaviour
 *
 * 	for public API, @see PaymentFileExporter
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.5 $
 *@created      Date: 25.8.2009 16:30:05
 *@modified     $Date: 2009/10/21 13:21:19 $
 */
class VubPaymentFileExporter extends PaymentFileExporter
{
	
	static final String SINGLE_PAYMENT_CHAR = "1";
	
	static final String REPETITIVE_PAYMENT_CHAR = "2";
	
	static final String CSV_DELIMITER_SEQUENCE = "\",\"";
	
	private boolean isSourceAccountIncorrect()
	{
		return getSourceAccount() == null || 
			Tools.isAnyEmpty(getSourceAccount().getAccountPrefix(), getSourceAccount().getAccountNumber());
	}

	@Override
	protected String getExporterIdentifierForFileName()
	{
		return "vub_eplatby";
	}
	
	@Override
	public String createRowFrom(Payment payment)
	{
		if (isSourceAccountIncorrect())
			throw new IllegalStateException("Call setAccount() first - source account as Payment's account ");
				
		return new StringBuilder().
			append(SINGLE_PAYMENT_CHAR).
			append(',').
			append(new SimpleDateFormat("dd.MM.yy").format(new Date())).
			append(',').
			append(getSourceAccount().getAccountPrefix()).append('-').append(getSourceAccount().getAccountNumber()).
			append(',').
			append(payment.getAccountPrefix()).append('-').append(payment.getAccountNumber()).
			append(',').
			append(payment.getBankCode()).
			append(",\"").
			append(payment.getAmountString().replace('.', ',')).
			append("\",").
			append(payment.getCurrency()).
			append(',').
			append(payment.getVariableSymbol()).
			append(',').
			append(payment.getConstantSymbol()).
			append(',').
			append(payment.getSpecificSymbol()).
			append(",\"").
			append(DB.internationalToEnglish(payment.getDescription())).
			append("\"").
			append(System.getProperty("line.separator")).
			toString();
	}

	@Override
	protected String getFileExtension()
	{
		return "txt";
	}
}
