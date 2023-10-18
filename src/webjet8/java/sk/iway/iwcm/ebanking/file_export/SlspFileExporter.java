package sk.iway.iwcm.ebanking.file_export;

import java.text.SimpleDateFormat;
import java.util.Date;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.ebanking.Payment;

/**
 *  SlspFileExporter.java
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.2 $
 *@created      Date: 25.9.2009 16:47:59
 *@modified     $Date: 2009/10/01 07:39:06 $
 */
public class SlspFileExporter extends PaymentFileExporter
{
static final String SINGLE_PAYMENT_CHAR = "1";
	
	static final String REPETITIVE_PAYMENT_CHAR = "2";
	
	static final String DELIMITER_SEQUENCE = ";";
	
	private boolean isSourceAccountIncorrect()
	{
		return getSourceAccount() == null || 
			Tools.isAnyEmpty(getSourceAccount().getAccountPrefix(), getSourceAccount().getAccountNumber());
	}

	@Override
	protected String getExporterIdentifierForFileName()
	{
		return "slsp";
	}
	
	@Override
	public String createRowFrom(Payment payment)
	{
		if (isSourceAccountIncorrect())
			throw new IllegalStateException("Call setAccount() first - source account as Payment's account ");
				
		return new StringBuilder().
			append(new SimpleDateFormat("dd.MM.yyyy").format(new Date())).
			append(DELIMITER_SEQUENCE).
			append(getSourceAccount().getAccountPrefix()).
			append(DELIMITER_SEQUENCE).
			append(getSourceAccount().getAccountNumber()).
			append(DELIMITER_SEQUENCE).
			append(payment.getAccountPrefix()).
			append(DELIMITER_SEQUENCE).
			append(payment.getAccountNumber()).
			append(DELIMITER_SEQUENCE).
			append('+').append(payment.getAmountString()).
			append(DELIMITER_SEQUENCE).
			append(payment.getCurrency()).
			append(DELIMITER_SEQUENCE).
			append(payment.getBankCode()).
			append(DELIMITER_SEQUENCE).
			append(payment.getVariableSymbol()).
			append(DELIMITER_SEQUENCE).
			append(payment.getConstantSymbol()).
			append(DELIMITER_SEQUENCE).
			append(payment.getSpecificSymbol()).
			append(DELIMITER_SEQUENCE).
			append(payment.getDescription()).
			append(";N;00;Nie"). //dummy end - not even specified in their internal documents
			append("\r\n").
			toString();
	}

	@Override
	protected String getFileExtension()
	{
		return "txt";
	}
}
