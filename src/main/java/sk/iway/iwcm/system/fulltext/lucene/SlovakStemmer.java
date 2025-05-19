package sk.iway.iwcm.system.fulltext.lucene;

/**
 *  SlovakStemmer.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff thaber $
 *@version      $Revision: 1.3 $
 *@created      Date: 29.5.2012 14:19:04
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SlovakStemmer {


	private static List<String []> pripony = vytvorPripony();

	private static String [] ei = new String[] {"e", "i", "iam", "iach", "iami", "í", "ia", "ie", "iu", "ím"};

	private static HashMap<String, String> dtnl = vytvorDTNL();

	private static HashMap<String, String> dlheKratke = vytvorDlheKratke();

	private static String [] cudzieSlovaPredIa = new String[] {"c", "z", "g"};

	private static String [] samohlasky = new String[] {"a", "á", "ä", "e", "é",
		"i", "í", "o", "ó", "u", "ú", "y", "ý", "ô", "ia", "ie", "iu"};

	private static String [] lr = new String [] {"r", "ŕ", "l", "ĺ"};

	protected SlovakStemmer() {
		//utility class
	}

	/**
	 * Ostemuje zadane slovo.
	 *
	 * @param in slovo
	 * @return jeho ostemovany tvar
	 */
	public static String stem(String in) {

		return zbavSaPripon(in);

	}




	private static String zbavSaPripon(String in) {

		for(String [] prip : pripony) {

			for(String pripona : prip) {

				//nasli sme priponu
				if(in.endsWith(pripona)) {

					//detenele, ditinili
					if(ei(pripona)) {
						return zmenDTNL(odstranPriponu(in, pripona));
					}

					//cudzie -cia, -gia...
					if(pripona.startsWith("i")) {
						if(cudzie(in, pripona)) {
							return odstranPriponu(in, pripona).concat("i");
						}
					}

					//ci nepride k overstemmingu
					if(overstemming(in, pripona))
						return in;

					//inak odstranime priponu
					return odstranPriponu(in, pripona);
				}
			}
		}

		//konci na er -> peter, sveter....
		if(in.endsWith("er")) {
			return (odstranPriponu(in, "er")).concat("r");
		}

		//konci na ok -> sviatok, odpadok....
		if(in.endsWith("ok")) {
			return (odstranPriponu(in, "ok")).concat("k");
		}

		//konci na zen -> podobizen, bielizen....
		if(in.endsWith("zeň")) {
			return (odstranPriponu(in, "eň")).concat("n");
		}

		//konci na ol -> kotol....
		if(in.endsWith("ol")) {
			return (odstranPriponu(in, "ol")).concat("l");
		}

		//konci na ic -> matematic (matematik, matematici)... (pracovnici vs slnecnic)
		if(in.endsWith("ic")) {
			return (odstranPriponu(in, "c")).concat("k");
		}

		//konci na ec -> tanec, obec....
		if(in.endsWith("ec")) {
			return (odstranPriponu(in, "ec")).concat("c");
		}

		//konci na um -> studium, stadium....
		if(in.endsWith("um")) {
			return (odstranPriponu(in, "um"));
		}

		//genitiv pluralu pre vzory zena, ulica, gazdina, mesto, srdce ???
		return poriesGenitivPluralu(in);

	}


	//problem = napriklad pes - psa, den - dna a podobne TODO
	private static boolean overstemming(String in, String pripona) {

		//overstemming zrejme vtedy, ked nam ostane koren bez samohlasky / bez l/r v strede slova

		String s = odstranPriponu(in, pripona);

		for(String samohlaska : samohlasky) {
			if(s.contains(samohlaska))
				return false;
		}

		for(String rl : lr) {
			if(s.contains(rl) && !s.endsWith(rl))
				return false;
		}

		return true;
	}



	//problem = ako rozoznat, ci je to zensky/stredny rod. pr: lama - lam vs. pan - panov TODO




	/**
	 *
	 * @param in
	 * @return ak je to genitiv pluralu, vrat spravny tvar, ak nie je, vrat in
	 */
	private static String poriesGenitivPluralu(String in) {

		//v poslednej slabike musi byt dlha samohlaska / dlhe r/l

		for(Map.Entry<String, String> entry : dlheKratke.entrySet()) {
			String dlha = entry.getKey();

			if(in.contains(dlha)) {

				if(poslednaSlabika(in, dlha)) {

					in = nahradPosledne(in, dlha, entry.getValue());

					break;
				}
			}

		}

		return in;
	}


	/**
	 * posledna slabika - ak sa za danym substringom uz nenaxadza uz ziadna samohlaska
	 * @param s string
	 * @param t substring
	 * @return
	 */
	private static boolean poslednaSlabika(String s, String t) {

		int pokial = s.lastIndexOf(t);
		String koniec = s.substring(pokial);
		koniec = koniec.substring(t.length());

		for(String samohlaska : samohlasky) {

			if(koniec.contains(samohlaska))
				return false;

		}

		return true;
	}







	/**
	 * nahradi posledny vyskyt podretazca v retazci inym podretazcom
	 * @param s
	 * @param co
	 * @param cim
	 * @return
	 */
	private static String nahradPosledne(String s, String co, String cim) {

		int pokial = s.lastIndexOf(co);

		String koniec = s.substring(pokial);

		koniec = koniec.substring(co.length());

		koniec = cim + koniec;

		s = s.substring(0, pokial) + koniec;

		return s;
	}



	//problem - srdcia TODO
	private static boolean cudzie(String in, String pripona) {
		String s = odstranPriponu(in, pripona);

		for(String koncovka : cudzieSlovaPredIa) {
			if(s.endsWith(koncovka))
				return true;
		}

		return false;
	}




	private static String odstranPriponu(String s, String p) {

		if(!s.endsWith(p))
			return s;

		return s.substring(0, s.length() - p.length());
	}

	//TODO problem - napr sused - sudedia vs. priatel - priatelia
	private static boolean ei(String s1) {
		for(String s2 : ei) {
			if(s1.equals(s2))
				return true;
		}
		return false;
	}


	private static String zmenDTNL(String in) {

		for(Map.Entry<String, String> entry : dtnl.entrySet()) {
			String tvrdy = entry.getKey();
			if(in.endsWith(tvrdy)) {
				in = in.substring(0, in.length() - 1);
				in = in.concat(entry.getValue());
			}
		}

		return in;
	}






	/**
	 * Vytvori zoznam pripon podstatnych mien od najdlhsich po najkratsie tak,
	 * 	ze ak je nejaka pripona obsiahnuta v inej, je kratsia.
	 *
	 * Pripony pre vzory:
	 * 		chlap, hrdina, dub, stroj, hostinsky
	 * 		zena, ulica, dlan, kost, gazdina
	 * 		mesto, srdce, vysvedcenie, dievca (+ holuba)
	 *
	 * @return zoznam pripon
	 */
	private static List<String[]> vytvorPripony() {

		List<String[]> p = new ArrayList<>();

		// od najdlhsich po najkratsie tak, ze ak je nejaka pripona obsiahnuta v inej, je kratsia

		//najdlhsie (nie su v ziadnej inej obsiahnute)
		p.add(new String[] {"encami", "atami", "ätami", "iami", "ými", "ovi", "ati", "äti",
				"eniec", "ence", "ie", "aťom", "äťom", "encom", "atám", "ätám", "iam", "ím",
				"ým", "encoch", "atách", "ätách", "iach", "ých", "aťa", "äťa", "ovia", "atá",
				"ätá", "aťu", "äťu", "ému", "iu", "iou", "ov", "at", "ät", "ä", "ého", "ý",
				"y", "ií", "ej", "ú", "é"});

		p.add(new String[] {"e", "om", "ami", "ám", "och", "ach", "ách", "ia", "á", "ou", "o", "ii", "í"});

		p.add(new String[] {"mi", "a", "u"});

		p.add(new String[] {"i"});

		return p;

	}


	/**
	 * vramcizenskeho a stredneho rodu - ideme riesit genitiv pluralu
	 * @return
	 */
	private static HashMap<String, String> vytvorDlheKratke() {

		HashMap<String, String> h = new HashMap<>();

		h.put("á", "a");
		h.put("ie", "e");
		h.put("ŕ", "r");
		h.put("ĺ", "l");
		h.put("í", "i");
		h.put("ú", "u");
		h.put("ô", "o");

		return h;
	}



	/**
	 * d -> ď, t -> ť, ...
	 * @return
	 */
	private static HashMap<String, String> vytvorDTNL() {

		HashMap<String, String> h = new HashMap<>();

		h.put("d", "ď");
		h.put("t", "ť");
		h.put("n", "ň");
		h.put("l", "ľ");

		return h;
	}






}
