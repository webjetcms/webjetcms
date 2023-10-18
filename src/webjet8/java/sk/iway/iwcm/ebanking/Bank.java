package sk.iway.iwcm.ebanking;


/**
 *  Banky.java
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 24.8.2009 13:03:51
 *@modified     $Date: 2009/08/26 10:14:01 $
 */
public enum Bank 
{
	NEZARADENY,
	SLOVENSKA_SPORITELNA,
	VUB,
	M_BANK,
	TATRA_BANKA,
	OTP_BANKA,
	UNI_CREDIT,
	DEXIA,
	CSOB,
	POSTOVA_BANKA;
	
	public static Bank getPodlaKoduBanky(String kod)
	{
		//TODO spravit nejake rozumne riesenie - je to duplikovane s dolnou metodou
		if ("0900".equals(kod))
			return SLOVENSKA_SPORITELNA;		
		if ("0200".equals(kod))
			return VUB;
		if ("5200".equals(kod))
			return OTP_BANKA;
		if ("1100".equals(kod))
			return TATRA_BANKA;
		if ("1111".equals(kod))
			return UNI_CREDIT;
		if ("5600".equals(kod))
			return DEXIA;
		if ("7500".equals(kod))
			return CSOB;
		if ("6500".equals(kod))
			return POSTOVA_BANKA;
		if ("8360".equals(kod))
			return M_BANK;
		if ("0000".equals(kod))
			return NEZARADENY;
		
		throw new IllegalArgumentException("Nenasiel banku s kodom: "+kod);
	}
	
	public static String getKod(Bank banka)
	{
		switch (banka)
		{
			case SLOVENSKA_SPORITELNA : return "0900";
			case VUB : return "0200";
			case OTP_BANKA : return "5200";
			case TATRA_BANKA : return "1100";
			case UNI_CREDIT : return "1111";
			case DEXIA : return "5600";
			case CSOB : return "7500";
			case POSTOVA_BANKA : return "6500";
			case M_BANK: return "8360";
			case NEZARADENY: return "0000";
			default :
				throw new IllegalArgumentException("Banka "+banka+"nema zadefinovany svoj kod"); 
		}
	}
	
	public String getKod()
	{
		return getKod(this);
	}
}