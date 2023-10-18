package sk.iway.iwcm.components.basket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.Tools;

/**
 *  StavyObjednavok.java
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jraska $
 *@version      $Revision: 1.2 $
 *@created      Date: 20.11.2009 16:26:45
 *@modified     $Date: 2010/01/18 08:49:51 $
 */
public class StavyObjednavok
{
	private static final Map<Integer, String> stavy = new HashMap<>();

	public static final int NOVA = 1;
	public static final int ZAPLATENA = 2;
	public static final int ZRUSENA = 3;
	public static final int OVEROVANA = 4;
	public static final int REALIZOVANA = 5;
	public static final int REZERVOVANA_DO = 6;
	public static final int NIE_JE_VOLNA = 7;
	public static final int ZAPLATENA_ZALOHA = 8;

	static
	{
		stavy.put(NOVA, "Nová");
		stavy.put(ZAPLATENA, "Zaplatená");
		stavy.put(ZRUSENA, "Zrušená");
		stavy.put(OVEROVANA, "Overovaná");
		stavy.put(REALIZOVANA, "Realizovaná");
		stavy.put(REZERVOVANA_DO, "Rezervovaná");
		stavy.put(NIE_JE_VOLNA, "Nie je voľná");
		stavy.put(ZAPLATENA_ZALOHA, "Zaplatená záloha");
	}

	private StavyObjednavok() {

	}

	public static String toString(BasketInvoiceBean invoice)
	{
		switch (invoice.getStatusId())
		{
			case NOVA : 			return "Nová";
			case ZAPLATENA : 		return "Zaplatená";
			case ZRUSENA : 			return "Zrušená";
			case OVEROVANA : 		return "Overuje sa dostupnosť";
			case REALIZOVANA:		return "Zrealizovaná";
			case REZERVOVANA_DO:	return "Rezervácia potvrdená do "+invoice.getFieldD();
			case NIE_JE_VOLNA : 	return "Vybraný termín nie je voľný";
			case ZAPLATENA_ZALOHA :	return "Zaplatená záloha";
			default : return "";
		}
	}

	public static String nazovPre(int idStavu)
	{
		return stavy.get(idStavu);
	}

	public static List<Integer> mozneStavy()
	{
		List<Integer> mozneStavy = new ArrayList<Integer>(stavy.keySet());
		/*Collections.sort(mozneStavy, new Comparator<Integer>(){
			@Override
			public int compare(Integer o1, Integer o2)
			{
				return Tools.slovakCollator.compare(nazovPre(o1), nazovPre(o2));
			}
		});*/
		mozneStavy.sort((o1, o2) -> Tools.slovakCollator.compare(nazovPre(o1), nazovPre(o2)));
		return mozneStavy;
	}
}
