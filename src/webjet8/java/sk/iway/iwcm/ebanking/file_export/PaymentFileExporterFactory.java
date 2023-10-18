package sk.iway.iwcm.ebanking.file_export;

import sk.iway.iwcm.ebanking.Bank;

/**
 *  PaymentFileExporterFactory.java
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.2 $
 *@created      Date: 25.8.2009 16:04:08
 *@modified     $Date: 2009/09/30 13:17:33 $
 */
public class PaymentFileExporterFactory
{
	public static PaymentFileExporter getExporterFor(Bank banka)
	{
		switch (banka)
		{
			case VUB : return new VubPaymentFileExporter();
			case TATRA_BANKA : return new TatraPayFileExporter();
			case CSOB : return new CsobPaymentFileExporter();
			case SLOVENSKA_SPORITELNA: return new SlspFileExporter();
			default :
				throw new UnsupportedOperationException("Not supported for "+banka);
		}
	}
}