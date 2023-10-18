package sk.iway.iwcm.ebanking.file_export;

import java.text.SimpleDateFormat;
import java.util.Date;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.ebanking.Payment;

/**
 *  TatraPayFileExporter.java
 * 	Exporter with Tatrapay specific behaviour
 *
 * 	for public API, @see PaymentFileExporter
 * 
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 25.8.2009 16:46:15
 *@modified     $Date: 2009/09/30 13:17:33 $
 */
public class TatraPayFileExporter extends PaymentFileExporter
{
	
	static final String SINGLE_PAYMENT_SEQUENCE = "11";
	
	static final String DELIMITER_SEQUENCE = ";";
	
	@Override
	protected String getExporterIdentifierForFileName()
	{
		return "tatra_pay";
	}
	
	@Override
	public String createRowFrom(Payment payment)
	{
		if (isSourceAccountIncorrect())
			throw new IllegalStateException("Call setAccount() first - source account as Payment's account ");
				
		return new StringBuilder().
			append(SINGLE_PAYMENT_SEQUENCE).
			append(DELIMITER_SEQUENCE).
			append(getSourceAccount().getAccountNumber()).
			append(DELIMITER_SEQUENCE).
			append(payment.getAccountPrefix()).
			append(DELIMITER_SEQUENCE).
			append(payment.getAccountNumber()).
			append(DELIMITER_SEQUENCE).
			append(payment.getBankCode()).
			append(DELIMITER_SEQUENCE).
			append(payment.getCurrency()).
			append(DELIMITER_SEQUENCE).
			append(payment.getAmountString()).
			append(DELIMITER_SEQUENCE).
			append(new SimpleDateFormat("yyyy-MM-dd").format(new Date())).
			append(DELIMITER_SEQUENCE).
			append(payment.getConstantSymbol()).			
			append(DELIMITER_SEQUENCE).
			append(payment.getVariableSymbol()).
			append(DELIMITER_SEQUENCE).
			append(payment.getSpecificSymbol()).			
			append(DELIMITER_SEQUENCE).			
			append(DELIMITER_SEQUENCE).
			append(DELIMITER_SEQUENCE).
			append(DELIMITER_SEQUENCE).
			append(DELIMITER_SEQUENCE).
			append(DELIMITER_SEQUENCE).
			append(DB.internationalToEnglish(payment.getDescription())).
			append(System.getProperty("line.separator")).
			toString();
	}	

	private boolean isSourceAccountIncorrect()
	{
		return getSourceAccount() == null || 
			Tools.isAnyEmpty(getSourceAccount().getAccountPrefix(), getSourceAccount().getAccountNumber());
	}

	@Override
	protected String getFileExtension()
	{
		return "csv";
	}	
}
