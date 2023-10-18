package sk.iway.iwcm.ebanking.file_export;

import java.text.SimpleDateFormat;
import java.util.Date;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.ebanking.Payment;

/**
 *  CsobPaymentFileExporter.java
 *
 *	 Exporter with CSOB specific behaviour
 *
 * 	for public API, @see PaymentFileExporter
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 25.9.2009 15:27:06
 *@modified     $Date: 2009/09/30 13:17:33 $
 */
class CsobPaymentFileExporter extends PaymentFileExporter
{

	static final String SINGLE_PAYMENT_ID = "11";
	
	static final String REPETITIVE_PAYMENT_ID = "32";
	
	static final String CSV_DELIMITER_SEQUENCE = "|";
	
	private boolean isSourceAccountIncorrect()
	{
		return getSourceAccount() == null || 
			Tools.isAnyEmpty(getSourceAccount().getAccountPrefix(), getSourceAccount().getAccountNumber());
	}

	@Override
	protected String getExporterIdentifierForFileName()
	{
		return "csob_tps";
	}
	
	@Override
	public String createRowFrom(Payment payment)
	{
		if (isSourceAccountIncorrect())
			throw new IllegalStateException("Call setAccount() first - source account as Payment's account ");
		
		return new StringBuilder().
			append(SINGLE_PAYMENT_ID).
			append(CSV_DELIMITER_SEQUENCE).
			append(getSourceAccount().getAccountNumber()).
			append(CSV_DELIMITER_SEQUENCE).
			append(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())).			
			append(CSV_DELIMITER_SEQUENCE).
			append(payment.getAmountString()).
			append(CSV_DELIMITER_SEQUENCE).
			append(payment.getAccountNumber()).
			append(CSV_DELIMITER_SEQUENCE).
			append(payment.getBankCode()).
			append(CSV_DELIMITER_SEQUENCE).
			append(payment.getConstantSymbol()).
			append(CSV_DELIMITER_SEQUENCE).
			append(payment.getVariableSymbol()).
			append(CSV_DELIMITER_SEQUENCE).
			append(' '). //should be a "payer's variable symbol", no clue what does that actually mean
			append(CSV_DELIMITER_SEQUENCE).
			append(payment.getSpecificSymbol()).
			append(CSV_DELIMITER_SEQUENCE).
			append(' ').//should be a "payer's specific symbol", no clue again, left blank
			append(CSV_DELIMITER_SEQUENCE).
			append(payment.getDescriptionFormatted(40, true)).
			append(CSV_DELIMITER_SEQUENCE).
			append(' ').//ZPRAVA_PRIJEMCI2
			append(CSV_DELIMITER_SEQUENCE).
			append(' ').//ZPRAVA_PRIJEMCI3
			append(CSV_DELIMITER_SEQUENCE).
			append(' ').//ZPRAVA_PRIJEMCI4
			append(CSV_DELIMITER_SEQUENCE).
			append(' ').//ZPRAVA_PLATCI
			append(System.getProperty("line.separator")).
			toString();
	}

	@Override
	protected String getFileExtension()
	{
		return "txt";
	}
	
}