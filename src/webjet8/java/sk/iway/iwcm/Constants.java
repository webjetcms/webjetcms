package sk.iway.iwcm;

import org.apache.commons.lang.StringUtils;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Konstanty aplikacie
 *
 * @Title Interway Content Management
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2002
 * @author $Author: jeeff $
 * @version $Revision: 1.15 $
 * @created $Date: 2004/03/12 10:16:44 $
 * @modified $Date: 2004/03/12 10:16:44 $
 */

public class Constants {
	private static String INSTALL_NAME = "webjet"; //NOSONAR
	private static boolean CONSTANTS_ALIAS_SEARCH = false; //NOSONAR

	public static final String NON_BREAKING_SPACE = "\u00A0";

	private static Map<String, Object> constantsMap = new Hashtable<>();

	private static ArrayList<ConfDetails> allValues = new ArrayList<>();

	public static final String MOD_BASKET = "basket";
	public static final String MOD_CONFIG = "config";
	public static final String MOD_DBSTORAGE = "dbstorage";
	public static final String MOD_EDITOR = "editor";
	public static final String MOD_GALLERY = "gallery";
	public static final String MOD_MAGZILLA = "magzilla";
	public static final String MOD_MICROSITE = "microsite";
	public static final String MOD_NTLM = "ntlm";
	public static final String MOD_PERFORMANCE = "performance";
	public static final String MOD_SEARCH = "search";
	public static final String MOD_SECURITY = "security";
	public static final String MOD_SMS = "sms";
	public static final String MOD_STAT = "stat";
	public static final String MOD_USER = "user";
	public static final String SERVER_NAME_MACRO = "SERVER_NAME";
	public static final String MOD_CAPTCHA = "captcha";
	public static final String MOD_FILE_ARCHIV = "file_archiv";
	public static final String MOD_DMAIL = "dmail";
	public static final String MOD_RESERVATION = "reservation";
	public static final String MOD_FORMMAIL = "formmail";
	public static final String MOD_AI_ASSISTANTS = "ai_assistants";

	public static final String MOD_OBSOLETE = "obsolete";

	static {
		clearValues();
	}

	protected Constants() {
		//utility class
	}

	/**
	 * Nastavi string a ulozi do zoznamu dostupnych konfiguracnych premennych pre
	 * pouzitie v GUI
	 *
	 * @param name
	 * @param value
	 * @param modules
	 * @param description
	 */
	public static void setString(String name, String value, String modules, String description) {
		ConfDetails conf = new ConfDetails();
		conf.setName(name);
		conf.setValue(value);
		conf.setModules(modules);
		conf.setDescription(description);

		setAllValues(conf);

		setString(name, value);
	}

	public static void setLong(String name, long value, String modules, String description) {
		ConfDetails conf = new ConfDetails();
		conf.setName(name);
		conf.setValue(Long.toString(value));
		conf.setModules(modules);
		conf.setDescription(description);

		setAllValues(conf);

		setLong(name, value);
	}

	public static void setInt(String name, int value, String modules, String description) {
		ConfDetails conf = new ConfDetails();
		conf.setName(name);
		conf.setValue(Integer.toString(value));
		conf.setModules(modules);
		conf.setDescription(description);

		setAllValues(conf);

		setInt(name, value);
	}

	public static void setBoolean(String name, boolean value, String modules, String description) {
		ConfDetails conf = new ConfDetails();
		conf.setName(name);
		conf.setValue(Boolean.toString(value));
		conf.setModules(modules);
		conf.setDescription(description);

		setAllValues(conf);

		setBoolean(name, value);
	}

	public static String mods(String... mod) {
		return StringUtils.join(mod, ";");
	}

	public static void clearValues() {
		Logger.info(Constants.class, "Constants - clearValues");

		allValues = new ArrayList<>();

		// default response encoding
		setString("defaultEncoding", "utf-8");

		// format je: nazov, hodnota, relevantne moduly oddelene znakom;, popis
		setInt("rootGroupId", 1, MOD_CONFIG, "ID predvoleneho korenoveho adresara");

		setInt("tempGroupId", 2, MOD_CONFIG, "ID adresara System->Sablony");

		setInt("menuGroupId", 3, MOD_CONFIG, "ID adresara System->Menu");

		setInt("headerFooterGroupId", 4, MOD_CONFIG, "ID adresara System->Hlavicky paticky");

		setString("statNoLogIP", "", mods(MOD_STAT, MOD_CONFIG, MOD_PERFORMANCE),
				"zoznam IP adries (alebo ich začiatkov) z ktorých sa nebude evidovať štatistika, napr. 192.168.1.,62.65.161.,147.175.111.112");

		setInt("linkType", LINK_TYPE_HTML, MOD_CONFIG,
				"Typ liniek systemu - hodnota html pre html linky a docid pre showdoc.do linky");

		// deprecated hodnoty
		setString("htmlImportStart", "", "", "");
		setString("htmlImportEnd", "");

		setString("dateFormat", "dd.MM.yyyy", MOD_CONFIG, "Format datumu pre standardny vypis");

		// deprecated
		setString("dateFormatEn", "MM.dd.yyyy");

		setString("dateTimeFormat", "dd.MM.yyyy H:mm:ss", MOD_CONFIG, "Format datumu a casu pre standardny vypis");
		setString("timeFormat", "HH:mm", MOD_CONFIG, "Format casu pre standardny vypis");

		setString("smtpServer", "localhost", MOD_CONFIG, "Adresa SMTP servera pre odosielanie emailov");

		// deprecated
		setString("editorWidth", "100%");

		setInt("newDocumentId", -1, MOD_EDITOR, "defaultne document id pre novy dokument (moze smerovat na sablonu)");

		setString("defaultLanguage", "", MOD_CONFIG, "prednastavený jazyk admin časti, napr. cz");

		setString("smsSendCmd", "", MOD_SMS,
				"Format pre odosielanie SMS, pre Orange:email e2sms@e2sms.orange.sk smtp.iway.sk tomas.kosec@interway.sk, pre T-Mobile:email sms.eurotel.sk obsidian.interway.sk");
		setString("smsSendInternational", "", MOD_SMS, "Format cisla pre SMS, moze mat hodnoty ++42, +42, 42");
		setInt("smsSendMaxlength", 140, MOD_SMS, "maximalna dlzka SMS spravy");

		setString("imagesRootDir", "/images", MOD_CONFIG, "Hlavny adresar pre obrazky");
		setString("galleryDirName", "gallery", MOD_CONFIG, "adresar v adresari images, v ktorom je foto galeria");
		setString("filesRootDir", "/files", MOD_CONFIG, "Hlavny adresar pre subory");
		setString("editorPageCss", "/css/page.css", MOD_CONFIG,
				"Hlavny CSS styl pre editor (stranku), sablona ho ale moze predefinovat");
		setString("editorEditorCss", "/css/editor.css", MOD_CONFIG, "Doplnkovy CSS styl pre editor (stranku)");

		setInt("imageThumbsWidth", 96);
		setInt("imageThumbsHeight", 96);

		setString("proxyHost", "", MOD_CONFIG, "Proxy pre pripojenie servera do internetu (ak je vyzadovane)");
		setString("proxyPort", "", MOD_CONFIG, "Port pre proxy pre pripojenie servera do internetu (ak je vyzadovane)");
		setString("proxyUser", "", MOD_CONFIG,
				"Prihlasovacie meno pre proxy pre pripojenie servera do internetu (ak je vyzadovane)");
		setString("proxyPassword", "", MOD_CONFIG,
				"Heslo pre proxy pre pripojenie servera do internetu (ak je vyzadovane)");

		setString("syncRemoteServer", "", "sync", "Adresa servera, s ktorym sa tento synchronizuje");

		setString("adminEnableIPs", "", MOD_SECURITY,
				"zoznam IP adries (alebo ich začiatkov) z ktorých je možné sa prihlásiť do admin časti, napr. 192.168.1,62.65.161");
		setBoolean("adminCheckUserGroups", false, "editor;webpages;security",
				"true || false - ak je nastavené na true bude sa vykonávať kontrola práv na zaheslované stránky aj administrátorom (štandardne sa nekontroluje, administrátor má prístup ku všetkým stránkam)");
		setBoolean("adminRequireSSL", false, MOD_SECURITY,
				"ak je nastavené na true a na serveri je nastavené SSL pre prístup do admin časti bude vyžadovaný httpS protokol");

		// server pre aktualizaciu WebJETu
		setString("updateRemoteServer", "http://license.interway.sk");

		setString("statDisableUserAgent",
				"bot,holmes,search,dig,godzilla,ocelli,crawl,slurp,spider,wauuu,http://,teoma,ia_archiver,libwww-perl,atlas factory,xenu,indy library,link verifier,froogle,inktomi,looksmart,firefly,nationaldirectory,ask jeeves,seek,galaxy.com,scooter,appie,webbug,spade,zyborg,rabaz,googleother",
				MOD_STAT, "zoznam User Agent názvov oddelených čiarkami, ktoré sa nebudú evidovať v štatistike.");
		setBoolean("exportFlash", false, MOD_EDITOR,
				"Ak je true, bude sa obsah stranok exportovat do XML pre Flash (do adresara /flash_xml/DOCID.xml)");

		setString("multilang", "", MOD_CONFIG, "overwrite string na ziskanie moznych hodnot MultilangDB");

		setInt("pkeyGenIncrement", 1, mods(MOD_CONFIG, MOD_PERFORMANCE),
				"Nastavenie PkeyGeneratora - hodnota o ktoru sa zvysuje");
		setInt("pkeyGenOffset", 0, mods(MOD_CONFIG, MOD_PERFORMANCE),
				"Nastavenie PkeyGeneratora - hodnota offsetu pre cluster");
		setInt("pkeyGenBlockSize", 10, mods(MOD_CONFIG, MOD_PERFORMANCE),
				"veľkosť výberu bloku pre generátor primárnych kľúčov. Štandardne nastavené na hodnotu 10, pre servre s vysokou záťažou odporúčame nastaviť na vyššiu hodnotu (100 - 1000)");

		// deprecated - ak je true, pri ukladani stranky sa generuje HTML do
		// /WEB-INF/docs_html
		setBoolean("exportDocsHtml", false);

		setString("navbarSeparator", "&gt;", "webpages;config",
				"oddelovací znak pre generovanie navigačnej lišty. Štandardne nastavené na znak >");

		// verzia webJetu
		setString("wjVersion", "P");

		setString("statNoReferer", "", MOD_STAT,
				"zoznam domén serverov oddelených čiarkou, ktoré sa nebudú evidovať v štatistike referer. Napríklad www.zoznam.sk,www.sme.sk");

		setString("currencyFormat", "0.00", MOD_BASKET, "pattern na formatovanie meny");

		// FCK: ak je true zapne sa XHTML mod formatovania HTML
		setBoolean("editorEnableXHTML", true, MOD_OBSOLETE, "xhtml mode for Struts framework");
		setString("editorFontColors", "", MOD_EDITOR,
				"umožňuje definovať kódy farieb, ktoré sa zobrazia v popup menu pre výber farby v editore. Kódy sú oddelené čiarkou, napríklad 000000,00ff00,ff0000,0000ff");

		// deprecated - ak je true, tak sa zobrazuju v statistike linky na staru
		// statistiku
		setBoolean("statDisableNew", false);

		setString("NTLMDomainController", "", MOD_NTLM, "Adresa domenoveho radica pre NTLM autorizaciu");
		setString("NTLMAdminGroupName", "WebJETAdminGroup", MOD_NTLM, "Nazov AD skupiny pre admina");
		setBoolean("NTLMldapIsSlovak", false, MOD_NTLM,
				"Ak je nastavene TRUE NTLM LDAP vracia vysledky v SK locales vo formate Meno Priezvisko");
		setString("passwordProtectedAutoId", "", "ntlm;security",
				"IDecka skupin, ktore budu pouzite pri pristupe ku vsetkym strankam bez potreby heslovania v admin casti");
		setBoolean("passwordProtectedAutoIdDontCreateUser", false, "ntlm;security",
				"Umozni zrusit vytvorenie pouzivatela v databaze, ktory ma len skupinu podla passwordProtectedAutoId");
		setString("NTLMForbiddenURL", "", MOD_NTLM, "URL adresa stranky zamietnuty pristup");

		// toolbarset pre editor, zadavaju sa tam taby, ktore sa nemaju zobrazit vo
		// forme tabLink4,tabLink5
		setString("FCKConfig.ToolbarSets[Default]", "", MOD_EDITOR,
				"zoznam tlačítok nástrojovej lišty v editore. Štandardne nastavené na hodnotu:\n[\n\n    ['Cut','Copy','Paste','PasteText','PasteWord','-','RemoveFormat'],\n    ['WJUndo','WJRedo','-','Find','Replace'],\n    ['Style','FontSize','-','Bold','Italic','Underline','StrikeThrough','-','Subscript','Superscript'],\n    ['JustifyLeft','JustifyCenter','JustifyRight','JustifyFull'],\n    ['OrderedList','UnorderedList','-','Outdent','Indent'],\n    ['Link','Unlink','Anchor'],\n    ['Image','WJTable','Rule','SpecialChar'],\n    ['TextColor','BGColor'],\n    ['WJForm','WJComponents','WJToogleBorders'] \n\n] ;\nPOZOR: jedná sa o definíciu JavaScript pola, takže je potrebné dodržať syntax a čiarky. ");
		setString("FCKConfig.ToolbarSets[Basic]", "", MOD_EDITOR,
				"zoznam tlačítok nástrojovej lišty v editore pri zapnutom skrátenom menu. Štandardne nastavené na hodnotu:\n[\n\n    ['Cut','Copy','Paste','PasteText','PasteWord', '-', 'RemoveFormat'],\n    ['WJUndo','WJRedo','-','Find','Replace'],\n    ['Style','Bold','Italic','Underline','-','Subscript','Superscript'],\n    ['JustifyLeft','JustifyCenter','JustifyRight','JustifyFull'],\n    ['OrderedList','UnorderedList','-','Outdent','Indent'],\n    ['Link','Unlink'],\n    ['Image','WJTable','SpecialChar'] \n\n] ;\nPOZOR: jedná sa o definíciu JavaScript pola, takže je potrebné dodržať syntax a čiarky. ");
		setString("FCKConfig.ToolbarSets[Standalone]", "", MOD_EDITOR,
				"Zoznam tlacitok pre zobrazenie v nastrojovej liste editora v rezime standalone (pre aplikacie)");
		setString("FCKConfig.ToolbarSets[Wiki]", "", MOD_EDITOR,
				"Zoznam tlacitok pre zobrazenie v nastrojovej liste editora v rezime Wiki");
		setString("FCKConfig.ToolbarSets[Blog]", "", MOD_EDITOR,
				"Zoznam tlacitok pre zobrazenie v nastrojovej liste editora v rezime Blog");
		setString("FCKConfig.ToolbarSets[Inline]", "", MOD_EDITOR,
				"Zoznam tlacitok pre zobrazenie v nastrojovej liste editora v rezime Inline editacie");

		// deprecated - ak je true maily sa neposielaju
		setBoolean("disableTheMailServer", false);

		// zoznam vulgarizmov
		setString("vulgarizmy",
				"kokot, pica, piča, piče, pice, chuj, jebat, jebať, kurv, jebko, vyjeban, vijeban, prijeban, " +
						"ujeban, pojeban, picu, piču, pici, piči, skurven, drbn, drbo, <iframe, <script",
				"forum", "Zoznam vulgarizmov, ktore forum nedovoli pridat");

		setBoolean("defaultDisableUpload", false, mods(MOD_EDITOR, MOD_SECURITY),
				"ak je nastavené na true a redaktor nemá nastavené žiadne cesty pre nahrávanie súborov nemôže nahrávať súbory na server. Štandardne nastavené na false.");

		setString("formmailAllowedRecipients", "", "form;security",
				"Zoznam koncových častí email adries na ktoré je možné odoslať formuláre, napríklad: @interway.sk,podpora@demo.webjet.sk. Štandardne prázdne, čo znamená, že formulár je možné odoslať na ľubovoľnú adresu");

		setBoolean("disableWebJETToolbar", false, "editor;webpages",
				"ak je redaktor prihlásený v admin časti a zobrazí si stránku tak v pravej hornej časti sa zobrazí informácia o stránke a možnosť editácie stránky. Ak túto hodnotu nastavíte na false, tak sa to zobrazovať nebude.");

		setString("imageMagickDir", "/usr/bin", "editor;performance;gallery",
				"Ak je nastavene pouzije sa na resize obrazkov prikaz convert z balika ImageMagick");

		setBoolean("disableDMailSender", false, "dmail",
				"Ak je nastavene na true nebude sa na posielanie emailov pouzivat systemovy sender");

		setString("loggerDateTimeFormat", "dd.MM H:mm:ss");

		// deprecated - ak je nastavene na true bude sa virtualna cesta forwardovat
		// namiesto redirectovat (ked nie je pouzity LinkType=HTML)
		setBoolean("forwardVirtualPath", false);

		setString("basketPriceField", "fieldK", MOD_BASKET,
				"Názov poľa v ktorom je v stránke uvedená cena produktu. Štandardne nastavené na hodnotu fieldK");
		setString("basketProductTypeField", "fieldI", MOD_BASKET,
				"Názov poľa v ktorom je v stránke uvedený typ produktu. Štandardne nastavené na hodnotu fieldI");
		setString("basketVatField", "fieldL", MOD_BASKET,
				"Názov poľa v ktorom je v stránke uvedená výška DPH produktu. Štandardne nastavené na hodnotu fieldL");
		setBoolean("basketCumulateItems", true, MOD_BASKET,
				"Ak je nastavené na true (štandardná hodnota) tak ak už v košíku zákazník má produkt a znova ho pridá, tak sa len zvýši počet kusov daného produktu v košíku");
		setBoolean("currencyTagRound", false, MOD_BASKET,
				"ak je nastavené na true tak sa pri výpise cien bude zaokrúhlovať na 50 halierov. Štandardne nastavené na false");

		setBoolean("editorAutoFillPublishStart", false, MOD_EDITOR,
				"ak je nastavene na true tak sa pri vytvoreni stranky automaticky predvyplni datum zaciatku publikovania na aktualny");

		// default log level
		setString("logLevel", "normal", "config;performance", "Uroven logovania, moze mat hodnotu debug alebo normal");
		setBoolean("logShowClassName", false);
		setBoolean("logShowInstallName", true);

		// dmail - casovy interval odosielania emailov
		setInt("dmailWaitTimeout", 1000, "dmail;config",
				"rýchlosť odosielania emailov z hromadného emailu v milisekundách. Štandardne nastavené na 5000, čo znamená že sa email odošle raz za 5 sekúnd. Ak hodnotu znížite tak pri odosielaní emailov bude viac zaťažený web server a SMTP server. Hodnota sa prejavi az po restarte servera");

		// deprecated - ak je nastavene na true bude v editore aj pri skratenom menu
		// mozne nastavit sablonu a rozsirene vlastnosti
		setBoolean("editorMiniEditEnableAdvancedProp", false);

		// porty pre HTTP a HTTPS
		setInt("httpServerPort", 80, MOD_CONFIG,
				"port na ktorom beží web server. V niektorých prípadoch môže interne bežať web server na inom porte ako je vonkajší port a potom nedokáže odoslať email. V takom prípade je potrebné nastaviť vnútorný port web servera. Štandardne nastavené na hodnotu 80.");
		setInt("httpsServerPort", 443, MOD_CONFIG, "Vonkajsi port httpS servera");

		// deprecated - pre staru verziu jTDS drivera treba nastavit na true
		setBoolean("jtdsCommit", false);

		setString("usrLogonMethod", "", "user;config",
				"logon metoda (ak sa pouziva ina ako standardna WJ databaza) - napr. sk.iway.klient.specialLogon, pricom metoda ma signaturu: public static ActionErrors specialLogon(String username, String password, HttpServletRequest request)");

		setString("emailEncoding", "", "dmail;config",
				"znaková sada pre odosielanie emailov. Štandardne nastavené na prázdnu hodnotu, čo znamená, že sa použije rovnaké kódovanie znakov ako je nastavené pre web stránky.");
		setString("formMailEncoding", "", "form;config",
				"znaková sada pre odosielanie emailov z formulárov. Štandardne nastavené na prázdnu hodnotu, čo znamená, že sa použije rovnaké kódovanie znakov ako je nastavené pre web stránky.");

		// vypne WJResponseWrapper pre WriteTag (ak by robilo problem)
		setBoolean("disableWJResponseWrapper", false);

		setBoolean("usrLogonRequireSMS", false, "user;security",
				"ak je true, tak prihlasenie na stranke vyzaduje SMS autorizaciu, vyzaduje nastavenie modulu SMS");

		setInt("bannerCacheTime", 0, "banner;performance",
				"ak je nastavené na hodnotu > 0 (v minútach) tak sa výber bannerov z DB cachuje, čo znamená, že sa nekontroluje zoznam bannerov pri každom zobrazení stránky (môže ale dôjsť k prekročeniu limitov videní)");
		// deprecated
		setBoolean("bannerDisableViewIP", false);

		setString("statMode", "new", "stat;performance",
				"ak je nastavené na hodnotu none tak sa vypne zapisovanie štatistiky do databázy, čo môže v prípade krízového stavu výrazne pomôcť výkonu servera. Inak môže nadobúdať hodnoty old (dostupná iba stará štatistika - zaberá menej miesta v databáze, ale neposkytuje všetky údaje), new (dostupná iba nová štatistika poskytujúca najpodrobnejšie údaje), old-new (v menu je najskôr zobrazená stará štatistika a je možné si zobraziť dáta aj novej, stará štatstika menej zaťažuje databázu pri zobrazení údajov), new-old (pri zobrazení štatistiky je najskôr zobrazená nová štatistika, je ale možné si zobraziť aj starú verziu štatistiky). Štandardne nastavené na new.");

		setInt("sessionRemoveTimeout", 40, "stat;performance",
				"počet minút, po ktorých bude session vyradena pamäti servera.");

		setString("dmailStatParam", "webjetDmsp", "dmail",
				"nazov prarametra pre statistiku kliknuti z DMailu, nema zmysel menit");

		setInt("RelatedPagesDBCacheMinutes", 30, "related-pages;performance",
				"počet minút cachovanie informácie o súvisiacich stránkach. Štandardne nastavené na 30 minút.");

		setString("dmailUnsubscribeMode", "disable", "dmail;users",
				"režim odhlásenia sa z mailing listu ak už nezostane povolená žiadna skupina. Možné hodnoty sú: disable - používateľa zneaktívni, ale zostanú mu posledné skupiny, delete - používateľa vymaže, removeGroups - používateľa zneaktívni a aj mu odstráni skupiny. Štandardne nastavená je hodnota disable.");

		setString("pathFilterBypassPath", "", MOD_PERFORMANCE,
				"cesty, ktoré preskočia processing cez PathFilter, vhodné nastaviť na /images,/css,/jscripts. POZOR k takýmto súborom sa nebudú kontrolovať práva.");

		setString("forumDefaultApproveEmail", "", "forum",
				"predvolené email adresy pre schválenie po zadaní príspevku do fóra, ak nie sú zadané pre fórum samostatne. Štandardne nie je zadaná žiadna hodnota");
		setString("forumDefaultNotifyEmail", "", "forum",
				"predvolené email adresy pre notifikáciu po zadaní príspevku do fóra, ak nie sú zadané pre fórum samostatne. Štandardne nie je zadaná žiadna hodnota");

		setBoolean("spamProtection", true, "security;form",
				"ak je nastavené na true, bude zapnutá ochrana proti spamu vo formulároch. Štandardne nastavené na true.");

		setBoolean("stripesEnableDirectActionCall", false, MOD_SECURITY,
				"ak je nastavené na true bude možné priame volanie Stripes action tried. Štandardne nastavené na false.");

		setString("calendarIcalDir", "/files/ical/", "calendar",
				"cesta, kam sa budu ukladat iCal (ics) subory pre kalendar");

		// nastavenia pre formatovanie meny
		setString("currencyFormatDecimalSeparator", ",", MOD_BASKET,
				"oddelovač desatinných miest pre formátovač meny. Štandardne nastavené na znak \",\".");
		setString("currencyFormatGroupingSeparator", " ", MOD_BASKET,
				"oddelovač tisícok pre formátovač meny. Štandardne nastavené na znak \" \" (medzera).");
		setString("currencyFormatLocale", "GERMANY", MOD_BASKET, "systemovy formater cisel");

		setBoolean("galleryAlwaysUseImageMagick", true, "gallery;performance",
				"ak je nastavené na true, tak zmena veľkosti obrázkov rozmeru < 500 bodov sa tiež bude vykonávať volaním externého programu ImageMagick.");

		setString("ffmpegPath", "", "video", "ffmpeg sluzi na konverziu videa, tu je nastavena cesta k suboru ffmpeg");
		setString("yamdiPath", "", "video", "cesta k programu yamdi pre vlozenie meta info do FLV suborov");

		// default skin pre WebJET
		setString("defaultSkin", "webjet8");

		setString("spamProtectionJavascript", "all formtagCsrf formmailCsrf formmail mailto", "form;security",
				"ak je nastavené na all (budú chránené všetky formuláre) alebo formmail (chránené budú len formuláre odosielané na email), budú formuláre chránené javascriptom, pre deaktivovanie funkcie je potrebné zadať none. Štandardne nastavené na all.");
		setInt("spamProtectionSendInterval", 30, "form;security",
				"čas v sekundách počas ktorého nie je možné znova odoslať formulár na email. Štandardne nastavené na 30.");

		setBoolean("statEnableTablePartitioning", false, "performance;stat",
				"ak je nastavené na true, tak sa štatistiky ukladajú do menších tabuliek podľa mesiacov. Po nastavení je potrebné skonvertovať už existujúcu databázu volaním stránky konverzia štatistiky - /components/stat/admin_db_convert.jsp. Štandardne nastavené na false.");

		setInt("cacheDocDetailsNewerThanDays", 0, MOD_PERFORMANCE,
				"ak je nastavene na inú hodnotu ako 0, tak v pamäti cachuje stránky nie staršie ako zadaný počet dní.");

		setBoolean("sessionStealingCheck", true, MOD_SECURITY,
				"ochrana pred ukradnitím session z inej IP adresy (pri každej požiadavke sa kontroluje IP adresa session). Štandardne nastavené na true.");

		// WebJET6 - ak je nastavene na true tak nebude prebiehat kontrola na
		// nezatvorene DB spojenia
		// potrebne pri dlhej konverzii databazy
		setBoolean("disableRemoveAbandoned", false);

		setBoolean("disableReverseDns", false, "performance;stat",
				"ak je nastavené na true nebude prekladaná IP adresa na doménový záznam pre štatistiku krajín");

		setString("fbrowserFileEditor", "editarea", MOD_OBSOLETE, "typ editora suborov - normal, alebo editarea");

		setInt("logonBlockedDelay", 10, MOD_SECURITY,
				"cas v sekundach, pocas ktorych nebude mozne znova sa prihlasit po zadani zleho hesla");

		setString("chartDateFormat", "d.M.yy", "stat;config", "format datumov pre grafy statistiky (Open Flash Chart)");

		setString("chartColors", "#5C5CF7,#F75C5C,#5CF75C,#FFC165,#E463E4,#A13600,#55FFFF,#FFAFAF,#B10505,#065BD8",
				"stat;config", "farby ciar a textov pre grafy statistiky (Open Flash Chart)");

		setInt("cacheRemoveCheckSeconds", 300, MOD_PERFORMANCE, "pocet sekund kontroly exspirovanych dat v cache");
		setInt("cacheSmartRefreshSeconds", 30, MOD_PERFORMANCE, "pocet sekund kontroly smart cache");

		setBoolean("serverBeyoundProxy", false, MOD_CONFIG,
				"ak je nastavene na true, tak sa IP adresa bere z hlavicky x-forwarded-for");

		setInt("cacheStaticContentSeconds", 300, MOD_PERFORMANCE,
				"nastavenie casu v sekundach (standardne nastavene na 0), na kory server nastavi exspiraciu statickych suborov nastavenych konfiguracnou premennou cacheStaticContentSuffixes (standardne .gif,.jpg,.png,.swf,.css,.js). Ak je prihlaseny administrator, alebo sa jedna o pristup z IP adries pre ktore sa neeviduje statistika (premenna statNoLogIP) tak sa cache hlavicka nenastavi.");
		setString("cacheStaticContentSuffixes", ".gif,.jpg,.png,.swf,.css,.js,.woff,.svg,.woff2", MOD_PERFORMANCE,
				"pripony suborov, na ktore bude aplikovana cache hlavicka");

		setString("basketCurrencyField", "fieldJ", MOD_BASKET,
				"Názov poľa v ktorom je v stránke uvedená mena produktu. Štandardne nastavené na hodnotu fieldJ");
		setString("basketProductCurrency", "eur", MOD_BASKET,
				"štandartne eur, určuje, v akej mene je cena tovaru, ak nie je žiadna mena zadaná.");
		setString("basketDisplayCurrency", "eur", MOD_BASKET,
				"štandartne eur, určuje, v akej mene sa tovar zobrazí v košíku, a pri objednávke. Toto zobrazovanie sa môže zmeniť lokálne podľa používateľa, ak to bude potrebné");

		setString("mapGoogleLicense", "", "map", "predvoleny licencny kluc pre google mapy");

		setString("supportedCurrencies", "eur,czk,usd,gbp", MOD_BASKET, "ake meny rozpoznava a dokaze prepocitat");
		setString("kurz_gpb_czk", "28.002", MOD_BASKET,"kurz_AAA_BBB - určuje, akým kurzom sa prepočítava mena AAA na menu BBB. Ak táto konštanta nie je uvedená, tak systém nevie prepočítať tovary v mene AAA na menu BBB, a ako kurz berie hodnotu 1");
		setString("kurz_gbp_eur", "1.153", MOD_BASKET,"kurz_AAA_BBB - určuje, akým kurzom sa prepočítava mena AAA na menu BBB. Ak táto konštanta nie je uvedená, tak systém nevie prepočítať tovary v mene AAA na menu BBB, a ako kurz berie hodnotu 1");
		setString("kurz_gbp_usd", "1.344", MOD_BASKET,"kurz_AAA_BBB - určuje, akým kurzom sa prepočítava mena AAA na menu BBB. Ak táto konštanta nie je uvedená, tak systém nevie prepočítať tovary v mene AAA na menu BBB, a ako kurz berie hodnotu 1");
		setString("kurz_eur_usd", "1.166", MOD_BASKET,"kurz_AAA_BBB - určuje, akým kurzom sa prepočítava mena AAA na menu BBB. Ak táto konštanta nie je uvedená, tak systém nevie prepočítať tovary v mene AAA na menu BBB, a ako kurz berie hodnotu 1");
		setString("kurz_eur_czk", "24.230", MOD_BASKET,"kurz_AAA_BBB - určuje, akým kurzom sa prepočítava mena AAA na menu BBB. Ak táto konštanta nie je uvedená, tak systém nevie prepočítať tovary v mene AAA na menu BBB, a ako kurz berie hodnotu 1");
		setString("kurz_usd_czk", "20.842", MOD_BASKET,"kurz_AAA_BBB - určuje, akým kurzom sa prepočítava mena AAA na menu BBB. Ak táto konštanta nie je uvedená, tak systém nevie prepočítať tovary v mene AAA na menu BBB, a ako kurz berie hodnotu 1");

		setInt("inquiryIpTimeout", 0, "inquiry",
				"nastavenie dlzky uchovania objektu v cache pre kontrolu IP adries pre ankety, ak hodnota je < 1 tak sa kontrola vypusti");

		setInt("NTLMDefaultAdminUserId", 0, MOD_NTLM,
				"ak sa pouziva NTLM autorizacia tu je mozne zadat ID pouzivatela, ktory sa pouzije pre nastavenie neexistujucich adminov");

		setString("stripesAddPackages", "", MOD_CONFIG, "dodatocne packages pre Stripes filter");

		setBoolean("xssProtection", true, MOD_SECURITY,
				"ak je nastavene na true je vykonavana automaticka ochrana pred XSS");

		setString("statLanguageDomain", "cs=cz,gb=uk,en=us,da=dk,ja=jp,ko=kr,fa=ir,el=gr", MOD_STAT,
				"mapovanie jazyka na domenu pri ziskani krajiny podla Accept Language hlavicky");

		setBoolean("iwfs_useDB", false, "dbstorage;config", "aktivovanie db storage");
		setBoolean("iwfs_useVersioning", false, MOD_DBSTORAGE, "verzovanie suborov");
		setInt("iwfs_blockSize", 1048576, MOD_DBSTORAGE, "velkost bloku pre storage");
		setBoolean("iwfs_writeToDisk", false, MOD_DBSTORAGE, "povolenie zapisu na disk pre nasledne rychlejsie citanie");
		setBoolean("iwfs_writeToDB", false, MOD_DBSTORAGE, "povolenie zapisu novsieho suboru z disku do databazy, vyzaduje aj zapnute iwfs_writeToDisk na true");
		setInt("iwfs_timeInCache", 15, MOD_DBSTORAGE, "kolko minut zostane v cache hash tabulky so zoznamov suborov");
		setString("iwfs_dirsInDB", "/css/;/files/;/images/;/jscripts/;", MOD_DBSTORAGE,
				"zoznam adresarov ktore su v db storage");
		setString("iwfs_tempDir", "", MOD_DBSTORAGE, "temporary adresar pre zapis docasnych suborov");
		setInt("iwfs_maxFileSize", 100 * 1024, MOD_DBSTORAGE,
				"nastavenia pre cache suborov zo storage, maximalna velkost suboru ktory sa este uklada do cache");
		setInt("iwfs_maxCacheSize", 100 * 1024 * 1024, MOD_DBSTORAGE, "maximalna celkova velkost cache");
		setBoolean("iwfs_useFileCache", false, MOD_DBSTORAGE, "zapnutie cache suborov zo storage");
		setInt("iwfs_timeInCacheInMinutes", 15, MOD_DBSTORAGE, "cas ktory sa drzia v cache subory v minutach");

		setBoolean("multiDomainEnabled", false, "multidomain;config",
				"ak je nastavene na true, bezi WebJET v rezime MultiDomain");
		setString("multiDomainAdminHost", "", "multidomain",
				"Adresa admin casti pre multidomain system (mala by byt ina ako jeden z webov, napr. admin.domena.sk)");

		setBoolean("uploadDisableNotAllowedDirs", false, "security;editor",
				"ak je nastavene na true, tak pouzivatel nevidi adresare, na ktore nema prava v dialogu pre vlozenie linky a obrazku");

		setString("pdfFontDirectory", "file://{ROOT_PATH}WEB-INF/fonts", MOD_CONFIG, "adresar s fontami pre generovanie PDF");

		setString("propInstallNames", "", MOD_CONFIG,
				"ak je nastavene na neprazdnu hodnotu bude sa pri starte nacitavat takyto zoznam custom properties suborov, ak je prazdne pouzije sa podla INSTALL_NAME");

		setString("ldapLoginChars", "@_.-", MOD_NTLM,
				"nealfanumericke znaky, ktore sa mozu nachadzat v LDAP filtri bez nutnosti ich escapovat");

		setString("clusterNames", "", "cluster", "zoznam nodov clustra oddeleny ciarkami");
		setInt("clusterRefreshTimeout", 5000, "cluster", "cas ako casto sa bude kontrolovat aktualnost objektov clustra");

		//deprecated
		setString("galleryDefaultStyle", "photoSwipe", MOD_GALLERY,
				"ak je potrebne zmenit vsetky galerie na webe na novy styl, staci nastavit do tejto premennej");

		setBoolean("editorAutomaticWordClean", false, MOD_EDITOR,
				"ak je nastavené na true je pri vkladaní textu automaticky vykonané čistenie HTML kódu. Používateľ má možnosť text vložiť ako čistý text.");

		setString("NTLMiisTrustedDomains", "", MOD_NTLM,
				"obsahuje zoznam domen ktorym sa doveruje pri IIS NTLM autorizacii");
		setString("NTLMldapQueryMethod", "", MOD_NTLM,
				"metoda pre vykonanie LDAP dotazu pre NTLM autorizaciu, ak je prazdne pouzije sa default");
		setString("NTLMisMemberOfMethod", "", MOD_NTLM,
				"metoda pre vykonanie LDAP dotazu zaclenienia do skupiny pre NTLM autorizaciu, ak je prazdne pouzije sa default");

		setBoolean("urlRemoveSpojky", true, "editor;config",
				"ak je nastavene na true budu pri vytvarani URL odstranovane spojky -a- -i-...");

		setBoolean("docTitleIncludePath", false, "webpages;editor",
				"ak je nastavene na true bude do doc_title pre stranku generovat aj spatnu cestu");

		setInt("fileIndexerSortPriority", 999, "config;files",
				"priorita root adresara file indexera, dalsie adresare sa nasobia hodnotou 10");

		setInt("auditExceptionTimeout", 24 * 60, MOD_PERFORMANCE,
				"urcuje timeout pre logovanie rovnakej spravy pre audit (standardne 24 hodin)");

		setBoolean("sortPriorityIncremental", true, "editor;config",
				"ak je nastavene na true je priorita vytvarana zvysovanim podla hlbky zanorenia");

		setString("FCKConfig.Tabs[Default]", "", MOD_EDITOR,
				"zakazane zalozky pre editor podla standardneho menu, prazdna hodnota znamena vsetko, inak zoznam povolenych vo formate tabLink1,tabLink2...");
		setString("FCKConfig.Tabs[Basic]", "tabLink2,tabLink3,tabLink4,tabLink5,tabLink6,tabLink7,tabLink8", MOD_EDITOR,
				"zakazane zalozky pre editor podla skrateneho menu, prazdna hodnota znamena vsetko, inak zoznam povolenych vo formate tabLink1,tabLink2...");
		setString("FCKConfig.Tabs[Inline]", "", MOD_EDITOR,
				"zakazane zalozky pre editor pre inline editaciu, prazdna hodnota znamena vsetko, inak zoznam povolenych vo formate tabLink1,tabLink2...");

		setString("clusterMyNodeType", "full", "cluster",
				"typ nodu clustra - full = admin+prezentacna, admin=iba admin, public=iba prezentacna cast");

		setBoolean("filesHasFieldPermsAttributes", false, "config;editor;files",
				"ak je nastavene na true bude pri uploade suborov mozne nastavit hodnoty prav a field_a az field_l ako pre web stranku (pouziva sa na to FileIndexer)");

		setInt("adminWelcomeDocId", -1, MOD_CONFIG, "docId stranky ktora sa moze zobrazit na welcome obrazovke");

		setBoolean("NTLMdontCreateAutoIdUser", true, MOD_NTLM,
				"ak je nastavene na true nevytvara sa pouizvatel v DB patriaci LEN do skupiny passwordProtectedAutoId");

		setBoolean("pixlrEnable", false, "editor;imageeditor;security",
				"ak je true, bude sa pri editor obrazkov zobrazovat odkaz na pixlr - rozsireny online editor obrazkov");

		setString("adminLogoffLink", "/logoff.do?forward=/admin/logon/", MOD_CONFIG,
				"linka na odhlasenie z admin casti (pri NTLM treba smerovat inak)");

		setString("FCKConfig.Heading[Default]", "h1,h2,h3,h4,h5,h6", MOD_EDITOR,
				"povolene nadpisy v editore pre standardne skratene menu");
		setString("FCKConfig.Heading[Basic]", "h1,h2,h3", MOD_EDITOR, "povolene nadpisy v editore pre skratene menu");

		setBoolean("FCKConfig.DenyImageSize[Default]", false, MOD_EDITOR,
				"ci je mozne menit rozmery obrazkov zadanim cisla v dialogu obrazkov pre standardne menu");
		setBoolean("FCKConfig.DenyImageSize[Basic]", true, MOD_EDITOR,
				"ci je mozne menit rozmery obrazkov zadanim cisla v dialogu obrazkov pre skratene menu");

		setInt("editorImageMaxWidth", 9999, MOD_EDITOR,
				"maximalna sirka obrazku co sa da vlozit do stranky (editor vacsiu nepovoli)");

		setInt("FCKConfig.UploadMaxSize[Default][image]", 0, mods(MOD_EDITOR, MOD_SECURITY),
				"limity pre upload suborov cez editor v kB pre obrazky a standardne menu");
		setInt("FCKConfig.UploadMaxSize[Basic][image]", 1024 * 2, mods(MOD_EDITOR, MOD_SECURITY),
				"limity pre upload suborov cez editor v kB pre obrazky a skratene menu");
		setInt("FCKConfig.UploadMaxSize[Default][file]", 0, mods(MOD_EDITOR, MOD_SECURITY),
				"limity pre upload suborov cez editor v kB pre subory a standardne menu");
		setInt("FCKConfig.UploadMaxSize[Basic][file]", 1024 * 20, mods(MOD_EDITOR, MOD_SECURITY),
				"limity pre upload suborov cez editor v kB pre subory a skratene menu");
		setString("FCKConfig.UploadFileTypes[Default][image]", "", mods(MOD_EDITOR, MOD_SECURITY),
				"limity typov suborov pre upload cez editor pre obrazky a standardne menu"); // jpg,jpeg,png,gif,swf,mpg,mov,wmv,flv,avi,mp3,vma,wav
		setString("FCKConfig.UploadFileTypes[Basic][image]", "jpg,jpeg,png,gif,mp4", mods(MOD_EDITOR, MOD_SECURITY),
				"limity typov suborov pre upload cez editor pre obrazky a skratene menu, prednastavene na jpg,jpeg,png,gif,swf");
		setString("FCKConfig.UploadFileTypes[Default][file]", "", mods(MOD_EDITOR, MOD_SECURITY),
				"limity typov suborov pre upload cez editor pre subory a standardne menu");
		setString("FCKConfig.UploadFileTypes[Basic][file]", "doc,docx,xls,xlsx,pdf,zip,rtf",
				mods(MOD_EDITOR, MOD_SECURITY),
				"limity typov suborov pre upload cez editor pre subory a skratene menu, prednastavene na doc,xls,pdf,zip,rtf");

		setString("urlRemoveSpojkyList",
				"a,e,i,o,u,s,v,ze,ale,na,pre,len,no,sa,to,uz,and,or,the,in,of,for,with,to,by,at,a.s", "config;editor",
				"zoznam spojok ktore sa vymazavaju z URl");

		setBoolean("passwordProtectedRenderMulti", true, MOD_EDITOR,
				"sposob renderovania skupin prav - ak je false ako checkboxy, ak je true ako multiselect s prepinanim nalavo a napravo (ako perex skupiny)");

		setBoolean("fileIndexerIndexAllFiles", false, "files;config",
				"ak je nastavene na true bude fileIndexer indexovat vsetky subory aj ked nevie ziskat ich text (vtedy ulozi len ich nazov). Pouziva sa v pripade, ked zaindexovana stranka sluzi aj na ine ucely (prava, metadata)");

		setInt("loggedUserBrowserId", 500000, MOD_CONFIG,
				"konstanta browserId, od ktorej zaciname identifikovat prihlasenych pouzivatelov, neodporucame menit");
		setInt("unloggedUserBrowserId", 1500000, MOD_CONFIG,
				"konstanta browserId, od ktorej zaciname identifikovat neprihlasenych pouzivatelov");

		setBoolean("smtpUseSSL", false, "config;dmail;security", "SMTP autentifikacia - aktivacia");
		setString("smtpUser", "", "config;dmail;security", "SMTP autentifikacia - login");
		setString("smtpPassword", "", "config;dmail;security", "SMTP autentifikacia - heslo");
		setString("smtpPort", "", "config;dmail;security", "SMTP autentifikacia - port");

		setBoolean("fullNameIncludeTitle", false, MOD_USER,
				"ak je nastavene na true, bude volanie getFullName() na UserDetails vkladat pred meno aj titul");

		setBoolean("fulltextIncludeKeywords", false, "config;search",
				"ak je nastavene na true, fulltext bude obsahovat aj keywords");

		setString("forumWysiwygIcons",
				"bold,italic,createlink,unlink,insertimage,inserthorizontalrule,insertunorderedlist,insertorderedlist",
				"config;security;forum;gallery", "povolene ikony pre editor fora / pohladnice");

		// zoznam XSS hodnot pre test filtra
		// hodnoty <xml|<?xml som zmazal kvoli detekcii pri vkladani
		setString("xssTestValues",
				"<script|javascript:|javascript&colon;|onmouse|onload|onerror|onfocus|onblur|onclick|onchange|onselect|ondoubleclick|ondblclick|onkeydown|onkeypress|onkeyup|set-cookie|expression(|&#|<meta|<iframe|<layer|<link|<style|<frame|<base|<object|<embed|<jscript|activexobject|ecmascript|vbscript:|.fromcharcode|x-javascript|@import|alert(|/web-inf/|onwebkit",
				MOD_SECURITY, "zoznam testovanych XSS vyrazov");

		setString("webEnableIPs", "", MOD_SECURITY,
				"zoznam IP adries (alebo ich začiatkov), z ktorých je možné si prezerať verejnú časť sídla. Vhodné nastaviť pri testovaní alebo tvorbe obsahu webu pred oficiálnym spustením. Príklad použitia: 192.168.1,62.65.161");
		setString("webAllowedDomains", "", "security;webpages",
				"zoznam domen, na ktorých je možné si prezerať verejnú časť sídla. Príklad použitia: interway.sk,test.iway.sk");

		setString("searchActionOmitCharacters", "_?%';", MOD_SECURITY,
				"znaky, ktore budu vyhodene z vyhladavania kvoli tomu, ze ide o wildcardy");

		setString("emailProtectionSenderEmail", "", "security;dmail",
				"nasilne vlozenie tejto hodnoty ako odosielatela e-mailu ku kazdemu odoslanemu e-mailu");

		setBoolean("disableWysiwyg", false, "config;security;forum;gallery",
				"Vypnutie moznosti pouzivat wysiwyg editor pre frontend (forum, send_link)");

		setInt("spamProtectionHourlyLimit", 20, MOD_SECURITY,
				"maximalny mozny pocet pouzivatelskych akcii za hodinu (ratane pre kazdy modul zvlast), pre vypnutie ochrany nastavte na hodnotu -2");
		setInt("spamProtectionTimeout", 30, MOD_SECURITY,
				"minimalne rozpatie v sekundach medzi dvoma pouzivatelskymi akciami, pre vypnutie ochrany nastavte na hodnotu -2");
		setInt("spamProtectionIgnoreFirstRequests", 0, MOD_SECURITY,
				"pocet ignorovanych prvych requestov z IP adresy bez vyvolania spam ochrany. Pre konkretny modul sa nastavuje cez premennu s -MENO_MODULU na konci, napr. spamProtectionIgnoreFirstRequests-search");
		setString("spamProtectionDisabledIPs", "", MOD_SECURITY, "Ciarkou oddelene zaciatky IP adries, pre ktore je spam ochrana vypnuta");

		setString("baseHrefLoopback", "", "config;dmail",
				"loopback HTTP adresa pre stahovanie suborov/stranok, ak je prazdne pouzije sa rovnaka ako vonkajsia");

		setBoolean("formMailSendPlainText", false, "form;security",
				"ak je nastavene na true je email z formularu odoslany ako plain text (nie HTML format)");

		setBoolean("formMailRenderRadioCheckboxText", true, "form",
				"ak je nastavene na true bude rendrovat radia a checkboxy ako text [X] alebo [ ]");

		setBoolean("formMailCropForm", false, "form",
				"ak je nastavene na true bude sa formmailom odosielat len cast obalena do tagov form");

		setBoolean("editorAdvancedImageAlignment", false, "editor;imageeditor",
				"ak je nastavené na false (prednastavená hodnota), zobrazujú sa možností zarovnania obrázku pri vkladacom dialógovom okne iba vpravo a vľavo. Ak túto hodnotu zmeníte na true, zobrazí sa ďalších 5 možností zarovnania - na spodok textu, hore, na horný okraj textu, vertikálne na stred a dole");

		setString("httpServerName", "", MOD_CONFIG,
				"ak je nastavene na nejaky hodnotu pouzije sa ako vonkajsi nazov servera (napr. www.interway.sk)");

		setString("natUrlTranslate", "", MOD_CONFIG,
				"ak je nastavene na nejaku hodnoty vykonava sa nahrada zadanych URL pri stahovani cez Tools.downloadURL. Hodnoty sa ukladaju na novy riadok vo formate stara_url|nova_url");

		// NEFUNGUJE kvoli loadovaniu stripes (globalnemu) - v nastavenych url
		// oddelenych ';' sa nebude v Stripes action beanoch filtrovat HTML kod
		setString("stripesXssNoFilteringPaths", "");

		setBoolean("statEnableDocCount", false, "stat;performance",
				"ak je nastavene na true bude sa aktualizovat celkovy pocet videni stranky (stlpec view_total tabulky documents), znizuje to ale vykon servera");

		setBoolean("serverMonitoringEnable", false, "server_monitoring;performance",
				"ak je nastavene na true, spusta monitorovanie servera kazdych 30 sekund a zapisuje tieto hodnoty do tabulky monitoring");

		setString("backupDirectory", "/files/backup", "files;security",
				"cesta, kam sa budu ukladat subory po tom, co pouzivatel stlaci backup tlacitko");

		setBoolean("flvEnableStreaming", false, "video;performance",
				"ak je nastavene na true je povoleny HTTP streaming FLV videa");

		setBoolean("cacheStaticContentForAdmin", false, MOD_PERFORMANCE,
				"ak je nastavene na false (standardne) a je prihlaseny administrator neposielaju sa cache hlavicky pre staticke subory");

		setBoolean("appendQueryStringWhenMonitoringDocuments", false, "server_monitoring",
				"zachytavat pri monitorovani aj parametre ?");

		setString("imageEditorSizeTemplates", "80x80;640x480;800x600;", "editor;imageeditor",
				"';' oddelene sablony velkost v tvare WidthxHeight; pre editor obrazkov");
		setString("imageEditorDefaultTool", "scale", "editor;imageeditor",
				"nastroj ktory bude predvoleny pri otvoreni editora obrazkov, moznosti : crop,scale,rotate");
		setString("imageEditorSizeTemplate", "80x80", "editor;imageeditor", "predvolena sablona");

		setBoolean("replaceExternalLinks", false, "dmail",
				"ak je nastavene na true budu sa nahradzat aj externe linky preklikom cez dmail system (pre sledovanie statistiky)");
		setInt("emailActionMaxItemsInList", 50, "dmail",
				"maximalny pocet zaznamov pre select box vyberu stranky pre mailing (ak sa vyberie user grupa ktora je vo vsetkych strankach mohol by byt zbytocne dlhy)");

		setInt("EventTypeDBCacheInMinutes", 15, "calendar;performance",
				"default nastavenie poctu minut, pre triedu EventTypeDB");

		setInt("searchTextAllLimit", 2000, "performance;webpages",
				"limit pre vyhladavanie cez searchTextAll - je to ochrana pred out of memmory na velkych DB");
		setBoolean("searchDetaultInTitle", false, "search;performance",
				"vyhladavanie - ak je true defaultne sa hlada aj v title stlpci (je potrebne pre starsie instalacie)");
		setInt("searchQuickSnippetSize", 40000, "search;performance",
				"vyhladavanie - velkost stranky pre pouzitie rychlejsieho generovania snippetu");

		setInt("fileAccessDeniedDocId", -1, MOD_SECURITY,
				"docId stranky ktora sa zobrazi ak prihlaseny pouzivatel nema pristup k zaheslovanemu suboru (alebo -1 = zobrazi sa standardna forbidden stranka)");

		setBoolean("forumReallyDeleteMessages", false, "forum",
				"ak je hodnota nastavena na true prispevky sa z DB skutocne vymazu (inak sa len oznacia za vymazane)");

		// aktivovanie modulu DB Browser, je mozne ho pouzit len na MySQL a je urceny
		// pre specialne pripady, preto je standardne vypnuty
		setBoolean("dbBrowserEnabled", false);

		setString("senderRunOnNode", "", "cluster",
				"ak je nastavene na inu ako prazdnu hodnotu obsahuje zoznam nodov clustra na ktorych ma sender bezat (napr. node1 alebo node1,node2)");

		// POZOR: tieto hodnoty sa nacitavaju z web.xml, su tu len uvedene pre poriadok
		// a jednotny zoznam
		setString("clusterMyNodeName", "");

		setBoolean("docAuthorLazyLoad", false, MOD_PERFORMANCE,
				"lazy load informacii o autorovi - bezne sa robi JOIN medzi tabulkami users a documents");
		setBoolean("searchActionOptimize", false, "performance;search",
				"vykonava iba 1 SQL dotaz miesto 2 pri vyhladavani - za cenu toho, ze nevieme celkovy pocet zaznamov");

		setBoolean("magzillaChangeEmailRecipients", true, MOD_MAGZILLA,
				"flag ci sa v magzille pri commente k bugu zobrazi moznost zmenit komu sa posle email, alebo sa nezobrazi a maily sa poslu podla default nastaveni");
		setString("magzilla_mail_sufix", "interway.sk", MOD_MAGZILLA, "suffix emailu pre komunikaciu s magzillou");
		setString("magzilla_web", "", MOD_MAGZILLA,
				"base URL adresa pre magzilla stranky (napr. http://www.interway.sk/helpdesk)");
		setString("magzilla_attachments_dir", "/files/helpdesk/", MOD_MAGZILLA, "adresar pre prilohy magzilly");
		setString("mgz_mailbox", "INBOX", MOD_MAGZILLA, "udaje pre mail parser helpdesku - priecinok s inboxom");
		setString("mgz_user_name", "", MOD_MAGZILLA, "udaje pre mail parser helpdesku - prihlasovacie meno");
		setString("mgz_host", "", MOD_MAGZILLA, "udaje pre mail parser helpdesku - adresa POP3 servera");
		setInt("mgz_port", -1, MOD_MAGZILLA, "udaje pre mail parser helpdesku - port POP3 servera");
		setBoolean("mgz_use_ssl", false, MOD_MAGZILLA,
				"udaje pre mail parser helpdesku - pouzitie SSL pri pristupe k POP3 serveru");

		setString("mgz_password", "", MOD_MAGZILLA, "udaje pre mail parser helpdesku - heslo");
		setString("mgz_client_type", "pop3", MOD_MAGZILLA, "udaje pre mail parser helpdesku - typ pripojenia");
		setString("mgz_strings",
				"__________ Informacia od ESET NOD32 Antivirus|--\nS pozdravom|<br>--\n<br>|<pre class=\"moz-signature\"|<p class=MsoAutoSig>",
				MOD_MAGZILLA, "zoznam retazcov oddelenych znakom | za ktorymi sa povazuje sprava za ukoncenu");
		setString("mgz_subjects", "Re:|Fwd:|FW:", MOD_MAGZILLA, "prefixy, ktore sa odstranuje z predmetu spravy");

		setString("mgzManagerGroupName", "Prava-manazeri", MOD_MAGZILLA, "Nazov hlavnej skupiny manazerov");
		setString("mgzClientGroupName", "HD Klienti", MOD_MAGZILLA, "Nazov hlavnej skupiny klientov");
		setString("mgzSolverGroupName", "Prava-solveri", MOD_MAGZILLA, "Nazov hlavnej skupiny solverov");
		setString("mgzTesterGroupName", "Prava-testeri", MOD_MAGZILLA, "Nazov hlavnej skupiny testerov");

		setString("mgzManagerBugViews", "managerAllBugs,managerWaitingBugs,allNewBugs,allBugs,notSolvedBugs,solvedBugs",
				MOD_MAGZILLA,
				"predvolene pohlady bugov generovane do selectBoxu v MagZille pri parametri allowBugs=all, generuju sa do selectBoxu podla poradia - pre manazera");
		setString("mgzClientBugViews", "clientAllBugs,clientWaitingBugs,clientSolvedBugs", MOD_MAGZILLA,
				"predvolene pohlady bugov generovane do selectBoxu v MagZille pri parametri allowBugs=all, generuju sa do selectBoxu podla poradia -  pre klienta");
		setString("mgzSolverBugViews", "assignedSolverBugs,allNewBugs,notSolvedBugs,solvedBugs", MOD_MAGZILLA,
				"predvolene pohlady bugov generovane do selectBoxu v MagZille pri parametri allowBugs=all, generuju sa do selectBoxu podla poradia - pre solvera");
		setString("mgzTesterBugViews", "assignedTesterBugs,newTesterBugs,allTesterBugs,notSolvedBugs,solvedBugs",
				MOD_MAGZILLA,
				"predvolene pohlady bugov generovane do selectBoxu v MagZille pri parametri allowBugs=all, generuju sa do selectBoxu podla poradia - pre testera");

		setString("microsite_web", "", MOD_MICROSITE,
				"base URL adresa pre microsite stranky (napr. http://www.interway.sk/microsite)");

		setBoolean("editorQuickUrlFix", true, mods(MOD_EDITOR, MOD_PERFORMANCE),
				"ak je nastavene na true nedochadza pri zmene URL stranky k aktualizacii existujucich liniek na danu stranku v ostatnych strankach ale sa len nastavi presmerovanie (je podstatne rychlejsie pri velkych databazach)");

		setString("defaultUserGroupsSelected", "none", MOD_EDITOR,
				"defaultne spravanie vyberoveho pola pre skupiny pouzivatelov v admin casti, none=nevybrane nic ale vyhladava sa vsade");

		setInt("KurzyDBCacheInMinutes", 60, "kurzy;performance", "default nastavenie poctu minut, pre KurzyDB");

		setInt("spamProtectionTimeout-ThumbServlet", -2, MOD_SECURITY,
				"ochrana pre zahltenie thumb servletu - interval medzi dotazmi nepouzijeme");
		setInt("spamProtectionHourlyLimit-ThumbServlet", 300, MOD_SECURITY,
				"ochrana pre zahltenie thumb servletu - maximalny pocet obrazkov generovanych za hodinu");

		setInt("inquiryGroupCacheInMinutes", 10, "inquiry;performance", "cas ktory sa drzia v cache inquiry podla group");

		setBoolean("multiDomainEnableNested", false, "multidomain",
				"ak je nastavene na true je povolene nastavovanie subdomen vramci stromovej struktury (cize domena nemusi byt len root folder)");
		setString("multiDomainFolders", "images,files,css,jscripts", "multidomain",
				"zoznam adresarov, ktore sa pouziju pre MultiDomain aliasy");

		setInt("imageMaxThumbsWidth", 2000, MOD_PERFORMANCE,
				"maximalna velkost thumb obrazku (aby nebol problem s vykonom / pamatou servera)");
		setInt("imageMaxThumbsHeight", 2000, MOD_PERFORMANCE,
				"maximalna velkost thumb obrazku (aby nebol problem s vykonom / pamatou servera)");
		setInt("relatedPagesMaxSize", 10, "performance;related-pages",
				"maximalny pocet zaznamov, ktory sa z DB vyberie pre related pages (pre jednu skupinu)");

		setInt("basketDepositPercentage", 100, MOD_BASKET,
				"kolko percent musi zakaznik zaplatit za produkt (meni sa napriklad pri zalohovych platbach)");

		setBoolean("fulltextIncludePerex", true, "config;search",
				"ak je nastavene na true bude sa do fulltextu vkladat aj perex clanku");

		setString("captchaComponents", "send_link,dmail", "security;send_link",
				"zoznam komponent oddelenych ciarkou pre ktore je vyzadovana captcha");

		setString("mediaCheckAvailabilityMode", "full", "security;webpages;media",
				"rezim kontroly existencie a dostupnosti liniek v mediach, moze mat hodnoty none pre vypnutie kontroly, fast pre kontrolu existencie linky bez kontroly prav a full pre plnu kontrolu existencie linky aj prav");

		setString("usersPositionList", "", MOD_CONFIG,
				"Pre verziu Intranet definuje zoznam moznych pozicii zamestnanca, jednotlive pozicie sa oddeluju znakom |.");

		setBoolean("magmaSaveAttendance", false, "magma",
				"Nastavenim na true sa bude zaznamenavat prihlasenie pouzivatela do prace");

		setString("magma_defaultApproverEmail", "", "magma",
				"email adresa pre schvalovanie dovoleniek ak nie je nikto nastaveny");
		setString("magma_holidaysApproveCC", "", "magma",
				"CC email so spravou o schvaleni dovolenky (napr. email ekonomky)");

		setString("magmaCalendarLoginEnabled", "", "magma",
				"ciarkou oddeleny zoznam IP adries z ktorych sa zaznamena prichod a odchod do prace (aby sa nezaznamenal prichod do prace napr. z domu");

		setString("usersPositionListReport", "", MOD_MAGZILLA,
				"Pre helpdesk definuje zoznam pozicii zamestnanca, pre ktore sa budu generovat reporty. Jednotlive pozicie sa oddeluju znakom |.");

		setString("magma_meetingUserGroup", "", "magma",
				"Názov skupiny z ktorej sa budú zobrazovat pouzivatelia pri vytvarani schodzky");

		setString("magzilla_private_web", "", MOD_MAGZILLA,
				"interna base URL adresa pre magzilla stranky(napr. http://intra.iway.sk/helpdesk)");
		setString("magzillaNotifyOnTicket", "", MOD_MAGZILLA,
				"Zoznam bodkociarkou oddelenych zaznamov v tvare id ticketu:email napr. (5555:helpdesk@iway.sk;). Nasledne ak si pouzivatel prida ticket 5555 do kalendara, odosle sa mail na helpdesk@iway.sk");

		setInt("passwordMinLength", 5, mods(MOD_SECURITY, MOD_USER),
				"Určuje minimálne akej dĺžky ma byť heslo pre bežného užívateľa");
		setInt("passwordAdminMinLength", 5, mods(MOD_SECURITY, MOD_USER),
				"Určuje minimálne akej dĺžky ma byť heslo pre administrátora");
		setInt("passwordMinCountOfSpecialSigns", 0, mods(MOD_SECURITY, MOD_USER),
				"Určuje minimálny počet výskytu špecálnych znakov v hesle pre bežného užívateľa");
		setInt("passwordAdminMinCountOfSpecialSigns", 0, mods(MOD_SECURITY, MOD_USER),
				"Určuje minimálny počet výskytu špecálnych znakov v hesle pre administrátora");
		setInt("passwordMinUpperCaseLetters", 0, mods(MOD_SECURITY, MOD_USER),
				"Určuje minimálny počet výskytu veľkých písmen v hesle pre bežného užívateľa");
		setInt("passwordAdminMinUpperCaseLetters", 1, mods(MOD_SECURITY, MOD_USER),
				"Určuje minimálny počet výskytu veľkých písmen v hesle pre administrátora");
		setInt("passwordMinLowerCaseLetters", 0, mods(MOD_SECURITY, MOD_USER),
				"Určuje minimálny počet výskytu malých písmen v hesle pre bežného užívateľa");
		setInt("passwordAdminMinLowerCaseLetters", 0, mods(MOD_SECURITY, MOD_USER),
				"Určuje minimálny počet výskytu malých písmen v hesle pre administrátora");
		setInt("passwordMinCountOfDigits", 0, mods(MOD_SECURITY, MOD_USER),
				"Určuje minimálny počet výskytu čísel v hesle pre bežného užívateľa");
		setInt("passwordAdminMinCountOfDigits", 1, mods(MOD_SECURITY, MOD_USER),
				"Určuje minimálny počet výskytu čísel v hesle pre administrátora");
		setInt("passwordExpiryDays", 0, mods(MOD_SECURITY, MOD_USER),
				"Určuje počet dní platnosti hesla pre bežného užívateľa. Po uplynutí času, bude užívateľ vyzvaný si zmeniť heslo.");
		setInt("passwordAdminExpiryDays", 365, mods(MOD_SECURITY, MOD_USER),
				"Určuje počet dní platnosti hesla pre administrátora. Po uplynutí času, bude užívateľ vyzvaný si zmeniť heslo.");

		setString("stripes.MultipartWrapper.Class", "sk.iway.iwcm.system.stripes.MultipartWrapper", MOD_CONFIG,
				"Vlastná implementácia Multipart, keďže pôvodný pri Stripes nie je možné použiť, potom by nefungovali veci v admin časti WebJETu (používajúce Struts).");
		setString("stripes.LocalizationBundleFactory.Class", "sk.iway.iwcm.system.stripes.LocalizationBundleFactory",
				MOD_CONFIG, "Factory trieda pre prácu s IwayResourceBundle.");
		setString("stripes.ActionBeanPropertyBinder.Class", "sk.iway.iwcm.system.stripes.XssSafeActionBeanPropertyBinder",
				MOD_CONFIG, "Pri nastavení parametrov z requestu filtruje html kód.");
		setString("stripes.FileUpload.MaximumPostSize", "5000m", MOD_CONFIG,
				"Maximálny upload suboru pomocou Stripes, defaultne 5GB.");

		setBoolean("webdavActive", false, "webdav", "Určuje či je prístup cez WebDav aktívny");
		setString("webdavUrlPrefix", "webdav", "webdav", "Určuje prefix cez ktorý je WebDav pristupny napr 'webdav' ");
		setString("webdavSharedDirectories", "/css/;/jscripts/;/files/;/images/", "webdav",
				"Urcuje, ktore adresare su pristupne cez WebDav. Priklad : /images/;/files/;");

		setBoolean("httpsAvailable", true, "security;webpages",
				"nastavenim na true sa indikuje, ze na serveri je instalovany SSL certifikat a je mozne zobrazovat httpS stranky");

		setString("forumNotifySenderEmail", "", "forum",
				"E-mail odosielateľa pre fórum. Pokiaľ je premenná prázdna, tak sa ako odosielateľ použije e-mail autora príspevku.");
		setString("forumNotifySenderName", "", "forum",
				"Meno odosielateľa pre fórum. Pokiaľ je premenná prázdna, tak sa ako meno použije meno autora príspevku.");

		setBoolean("dmailDisableInlineImages", false, "dmail",
				"Pokiaľ je nastavené na true, tak sa obrázky nepošlú ako inline prílohy, ale použije sa pôvodná URL obrázku.");
		setBoolean("statEnableClickTracking", true, "stat;performance",
				"Umožňuje ukladanie informacii o používateľských klikoch pre potreby vytvárania teplotných máp klikov");

		setString("mgzAutoreplyWords", "Out Of Office|Automatická odpoveď|AutoReply", MOD_MAGZILLA,
				"Slovička, ktorými sa definujú autoreply maily v predmete správy. Takéto správy sa odstraňujú. Pokiaľ je hodnota prázdna, tak sa funkcia odstraňovania takýchto e-mailov vypne.");
		setString("smallImageLinkToOriginalSize", "<a href=\"{2}\" rel=\"wjimageviewer\"><img src=\"{1}\" alt=\"\"></a>",
				MOD_EDITOR,
				"Odkaz generovaný do editora pri vložení malého obrázku do stránky. {1} je cesta k malému obrázku, {2} k veľkému, {3} je výška nového okna a {4} je šírka.");

		setInt("galleryWatermarkSaturation", 70, MOD_GALLERY,
				"Nastavuje transparentnosť vodotlače vo výslednom obrázku. Číslo 0-100, 0 znamená úplnú priesvitnosť, 100 nepriesvitnosť.");
		setString("galleryWatermarkGravity", "Center", MOD_GALLERY,
				"Pozícia vodotlače vo výslednom obrázku. Možnosti podľa svetových strán v angličtine: NorthWest, North, NorthEast, West, Center, East, SouthWest, South, SouthEast");

		setString("magzillaAutomaticChangeStatus", "RIESI SA,In progress", MOD_MAGZILLA,
				"Automatická zmena statusu ticketu pri vytvorení nového záznamu do kalendára z prostredia helpdesk v intranete");
		setInt("statHeatMapImageTimeout", 300, MOD_STAT,
				"Určuje čas existencie obrázku teplotnej mapy. Ak prekročí tento čas, bude zmazaný. Meria sa v sekundách.");
		setInt("editorFastHTMLSize", 300000, mods(MOD_EDITOR, MOD_PERFORMANCE),
				"Dlzka HTML kodu pre ktoru sa moze pouzit rychle ziskanie bez konverzie do XHTML");
		setInt("statBufferSuspicionThreshold", 1000, MOD_STAT,
				"Hranica záznamov v bufferi, po prekročení ktorej sa považuje asynchrónny zápis do databázy za nefunkčný");

		setBoolean("editorEnableMultipleFilesUpload", true, MOD_EDITOR,
				"Povolenie / zakázanie hromadného nahrávanie súborov v dialogu v editore.");
		setBoolean("docBlockPublicHistory", true, mods(MOD_EDITOR, MOD_SECURITY),
				"Defaultne je umožnené vidieť históriu stránky všetkým používateľom. Nastavením tejto premennej na true sa toto znemožní.");

		setBoolean("cayenneEnabled", false, MOD_PERFORMANCE,
				"Prepnutim na true sa aktivuje podpora pre Apache Cayenne - ak projekt vyzaduje)");
		setBoolean("spotlightIndexFiles", false, MOD_PERFORMANCE,
				"Globálne vyhľadávanie(spotlight) je výrazne spomaľované načívaním mien súborov. Pri ich indexácii sa výrazne proces zrýchli. Na druhú stranu to skonzumuje viac pamäte");
		setBoolean("galleryEnableWatermarking", true, "gallery;performance",
				"Vypne / zapne vodotlač pre obrázky. Vodotlač môže výrazne spomaľovať veľké importy obrázkov kvôli rekurzívnemu hľadaniu nastavenia vodotlače.");
		setString("spotlightIndexedDirectories", "/css, /images, /jscripts, /templates, /files", MOD_PERFORMANCE,
				"Zoznam adresárov, v ktorých sa vyhľadáva pri rýchlom vyhľadávaní");

		setBoolean("dragDropUploadEnabled", true, MOD_OBSOLETE,
				"Povolenia Drag & Drop nahrávania súborov z desktopu do WebJETu vo FireFoxe");

		setString("useSMTPServer", "true", "dmail", "Povolenie moznosti odosielania emailov");

		setBoolean("statWebJET7Converted", false, "system",
				"Urcuje, ci uz prebehla konverzia databazy do formatu pre WebJET7");

		setString("statHeatMapMainContainerSelector", "div.mainContainer", MOD_STAT,
				"jQuery selector pre main container pre ukladanie pozicie Heat Map");

		setInt("editorTemplateComboWidth", 165, MOD_EDITOR, "Sirka vyberoveho pola sablony v editore");

		setBoolean("ntlmAllowEmptyGroups", false, "users;security",
				"Ak je nastavene na true su vramci NTLM povolene pristupy pouzivatelov u ktorych nie je sparovana ziadna user skupina");

		setString("ofcXLabelOrientation", "2", MOD_STAT,
				"Orientacia popisu X-ovej osi, viz http://teethgrinder.co.uk/open-flash-chart/gallery-x-axis-2.php");

		setInt("spamProtectionTimeout-search", 10, "search;security",
				"Vyhladavanie - ochrana pred DOS utokmi - timeout v sekundach pocas ktorych nie je mozne zadat dalsie hladanie");
		setInt("spamProtectionHourlyLimit-search", 200, "search;security",
				"Vyhladavanie - ochrana pred DOS utokmi - pocet hladani z jednej IP adresy za hodinu");

		setBoolean("useUserIdAsBrowserId", true, MOD_STAT,
				"Ak je nastavene na true a pouzivatel sa prihlasi je jeho userId pouzite ako browserId a je mozne usera trackovat v statistike stranok");

		setBoolean("searchUseOracleText", false, "search;performance",
				"Ak je nastavene na true pouziva sa pre vyhladavanie v pripade Oracle databazy Oracle Text. Vyzaduje nastavenie prav a indexu, pre efektivne vyuzitie musi byt nastavena aj konfiguracna premenna searchDetaultInTitle na hodnotu false.");

		setBoolean("passwordUseHash", false, mods(MOD_SECURITY, MOD_USER),
				"Prepína medzi použitím SHA512 hashu a AES. Po prepnuti na true je nutne zavolat /admin/update/update_passwords.jsp. Tato zmena je NEVRATNA!!");
		setInt("passwordResetValidityInMinutes", 30, mods(MOD_SECURITY, MOD_USER),
				"Ak sa na šifrovanie hesla použije SHA512, tento parameter určuje, ako dlho je platný token na zmenu hesla");

		setString("jpaAddPackages", "", "jpa;performance", "Čiarkou oddelené zoznamy balíčkov, kde sa hľadajú JPA Beany");

		setString("formmailHttpsDomains", "", "form;security",
				"Zoznam domen oddelenych ciarkou, pre ktore sa budu formulare vzdy odosielat cez zabezpecene httpS spojenie");

		setBoolean("adminLogonShowSimpleErrorMessage", true, MOD_SECURITY,
				"Ak je nastavene na true nezobrazuje pri prihlasovani v admin casti podrobnu informaciu o tom, ci je zle zadane meno, alebo heslo a neponuka moznost poslat heslo na email");

		setBoolean("googleDocsEnabled", false, "Googel Docs",
				"Ak je nastavene na true, spristupni sa upload a otvorenie prilohy v ticketoch cez Google Docs.");
		setString("googleDocsConsumerKey", "", "Googel Docs",
				"Kluc pre domenu, cez ktoru sa bude ku Google Docs pristupovat. Ak je prazdne, Google Docs v tiketoch nie je pristupny.");
		setString("googleDocsConsumerSecret", "", "Googel Docs",
				"Secret pre domenu, cez ktoru sa bude ku Google Docs pristupovat. Ak je prazdne, Google Docs v tiketoch nie je pristupny.");

		setBoolean("cacheUrlRedirects", false, MOD_PERFORMANCE,
				"Po nastaveni na true sposobi uchovavanie presmerovani v cache pamati, moze zvysit vykon vysoko zatazeneho servera pri DOS utoku");

		setString("galleryAllowUploadGroupIds", "", "gallery;security",
				"Obsahuje zoznam užívateľských skupín oddelených čiarkou, ktoré majú povolené nahrávať obrázky aj mimo admin časti.");

		setString("mgzBugErrorStrings", "bug,chyba", MOD_MAGZILLA, " Vyhladavane texty pri nefakturovanom case");

		setInt("editorNotAjaxSaveSize", 200000, mods(MOD_EDITOR, MOD_PERFORMANCE),
				"Dlzka HTML kodu pre ktoru sa uz nepouzije ulozenie cez AJAX ale klasicky submit formu (ajax ulozenie je narocne na serializaciu HTML kodu)");

		setBoolean("xssProtectionStrictGet", true, MOD_SECURITY,
				"ak je nastavene na true je vykonavana kontrola na specificke znaky (apostrof, uvodzovky atd) vo vsetkych GET parametroch");
		setString("xssProtectionStrictGetUrlExceptionSystem",
				"/images,/components/_common/flash/open-flash-chart,/admin/FCKeditor/,/admin/dialogframe.jsp,%editor_component.jsp,/components/_common/banner,/components/reloadParentClose.jsp,/admin/fbrowser/fileprop/,/components/data_deleting/admin_deleting_,/components/editor_component_universal.jsp,/admin/prop_,/admin/fbrowser.,/admin/skins/webjet6/left_files-tree.jsp,/admin/imageeditor/load_image.jsp,/components/adminlog/adminlog.jsp,%_autocomplete.jsp,/admin/editor.do,%admin_news_list.jsp,/admin/inline/dialogframe_inline.jsp,/admin/searchall.jsp,/admin/offline.do,/admin/swfupload/multiple_files_upload_popup.jsp,/components/basket/admin_products_list.jsp,/admin/archive.jsp,/admin/inline/form_report_problem.jsp,/components/gallery/ajax/getimagedata.jsp,/components/basket/admin_transports_list.jsp,/admin/rest/document,/admin/v9/files/dialog,/admin/v9/apps/image-editor/",
				MOD_SECURITY,
				"casti URL adries (ich zaciatky) pre ktore sa nepouzije xssProtectionStrictGet, jednotlive URL su oddelene ciarkou");
		setString("xssProtectionStrictGetUrlException", "", MOD_SECURITY,
				"casti URL adries (ich zaciatky) pre ktore sa nepouzije xssProtectionStrictGet, jednotlive URL su oddelene ciarkou");

		setBoolean("cayenneSynchronized", false, MOD_PERFORMANCE,
				"Nastavuje synchornized vytvaranie cayenne objektu pre session, nastavenie na true je bezpecnejsie z pohladu transakcnosti, moze ale sposobovat lock threadov");

		setBoolean("navbarRenderAllLinks", true, "webpages",
				"Upravene zobrazenie navigacnej listy, kedy sa aj pre aktualnu stranku v navigacnej liste zobrazuje linka, povodne spravanie ako vo WebJET6 sa da nastavit vypnutim tejto premennej na hodnotu false");
		setString("luceneIndexDir", "/WEB-INF/lucene_index/", MOD_SEARCH, "cesta k adresáru s Lucene indexom");
		setBoolean("luceneIncrementalDocumentIndexing", false, MOD_SEARCH,
				"Ak je true, stránky sa indexujú priebežne pri zmene");
		setBoolean("luceneTicketsSearch", false, MOD_SEARCH,
				"Ak je true, tak sa pre vyhladavanie v ticketoch pouzije Lucene");
		setBoolean("luceneIncrementalTicketIndexing", false, MOD_SEARCH, "Ak je true, tickety sa indexuju priebezne");
		setBoolean("luceneForumsSearch", false, MOD_SEARCH, "Ak je true, tak sa pre vyhladavanie vo fóre použije Lucene");
		setBoolean("luceneIncrementalForumIndexing", false, MOD_SEARCH, "Ak je true, fora sa indexuju priebezne");
		setBoolean("luceneIndexingSkAlgorithmicStemming", true, MOD_SEARCH,
				"Ak je true, pouzije sa algoritmicky stemmer pre slovensky jazyk");
		setBoolean("luceneIndexingSkSynonymExpansion", false, MOD_SEARCH,
				"Ak je true, pouzije sa pri indexovani expanzia synonym ( kazde slovo sa nahradi base synonymom ) ");

		setString("htmlToPdfReadonlyDocIds", "", MOD_SECURITY,
				"zoznam docId stranok oddeleny medzerou, +, alebo ;, ktorym sa pri konverzii do PDF nastavi readonly, tzn. obsah PDF sa nebude moct kopirovat, editovat.");
		setString("editorSingleCharNbsp", "a,i,o,u,s,so,z,zo,v,vo,na,do,od,pre,k,\\d+\\.?", MOD_EDITOR,
				"Pre kazdu spojku v zozname sa medzera za nou nahradza &nbsp;");

		setBoolean("zmluvyApprovEditChanges", false, "zmluvy",
				"Pokial je nastavena hodnota na true, tak editovanu zmluvu bude treba opatovne schvalit na publikaciu. E-mail o zmene sa posle schvalovatelom ci je hodnota true alebo false.");

		setInt("passwordUseHashIterations", 100, MOD_SECURITY,
				"Pocet iteracii pre hashovanie hesla - POZOR po zahashovani sa uz nesmie zmenit");
		setString("dmailBadEmailSmtpReplyStatuses",
				"Invalid Addresses,Recipient address rejected,Bad recipient address,Local address contains control or whitespace,Domain ends with dot in string,Domain contains illegal character in string",
				"dmail",
				"Zoznam ciarkou oddelenych vyrazov vratenych zo SMTP servera pre ktore sa email nebude znova pokusat odoslat");
		setInt("linkCheckEmptyPageSize", 100, "webpages;editor",
				"Dlzka HTML kodu stranky, ktora sa pri kontrole povazuje za prazdnu");

		setString("imageEditorRatio", "3:2, 4:3, 5:4, 7:5, 16:9", "editor;imageeditor",
				"Pomer strán pre orezanie v image editore.");

		setBoolean("fulltextIncludeAttributes", false, "config;search",
				"ak je nastavene na true, fulltext bude obsahovat aj atributy stranky");

		setInt("statHeatMapMaxWidth", 2000, "stat;performance",
				"Maximalna sirka generovaneho obrazku heatmapy, cim vacsie cislo tym vacsie zatazenie na pamat servera pri generovani");
		setInt("statHeatMapMaxHeight", 6000, "stat;performance",
				"Maximalna vyska generovaneho obrazku heatmapy, cim vacsie cislo tym vacsie zatazenie na pamat servera pri generovani");

		setString("dmailTrackopenGif", "/components/dmail/trackopen.gif", "dmail",
				"Virtualna cesta ktora indikuje trackovanie otvorenia emailu");

		// Amazon SES
		setBoolean("useAmazonSES", false, "dmail;config",
				"Ak je true posielaju sa emaily cez Amazon Simple Email Service");
		setString("amazonAccessKey", "", "dmail;config", "pristupovy kluc pre Amazon SES");
		setString("amazonSecretKey", "", "dmail;config", "heslo pre Amazon SES");

		setString("editorBodyClass", "", MOD_EDITOR, "Trieda pre hlavny body element v editore stranok");

		setString("editorLinkRelValues",
				"alternate,appendix,bookmark,contents,copyright,friend,glossary,help,chapter,index,license,next,nofollow,prev,section,start,stylesheet,subsection,tag",
				MOD_EDITOR, "Zoznam hodnot pre atribut REL znacky A");

		setString("saturFtpUsername", "proviz", "cestovka", "FTP prihlasovacie meno pre SATUR stahovanie obrazkov");
		setString("saturFtpPassword", "proviz11", "cestovka", "FTP heslo pre SATUR stahovanie obrazkov");

		setString("forumDefaultAddmessageGroups", "", "forum",
				"predvolené skupiny pouivatelov, ktory mozu pridat prispevok do fora (ak nie sú zadané pre fórum samostatne). Štandardne nie je zadaná žiadna hodnota");

		setBoolean("galleryEnableExifDate", true, MOD_GALLERY,
				"Pri uploade fotky sa ziska ako jej datum datum vytvorenia z exif informacie, pre vypnutie je potrebne nastavit tuto premennu na false");
		setLong("maxSizeOfAttachments", 8000000, "email",
				"Nastaví maximálnu veľkosť príloh priložených k e-mailu. Velkosť sa udáva v bajtoch");

		// 7.2
		setBoolean("galleryStripExif", true, MOD_GALLERY,
				"Ak je nastavene na true (standardne) tak sa z fotky odstranuju exif informacie");

		setString("NTLMldapEncoding", "", "ntlm", "Kodovanie pouzite pre komunikaciu s LDAP");

		setBoolean("galleryCheckUserPerms", true, "ntlm;security",
				"Možnosť vypnutia kontroly práv pre obrázky galérie (/images/gallery...) v PathFilter");

		// /admin/FCKeditor/editor/skins/webjet/fck_dialog.css,
		// /admin/css/tabpane-luna.css je tam kvoli zalozkam na frontende (docman)
		setString("allowAdminUrls",
				"^/admin/$, /admin/images/, /admin/m/images/, /admin/logon.jsp, ^/admin/m/$, /admin/m/logon.jsp, /admin/m/content/logon_body.jsp, /admin/index.jsp, /admin/logon.do, /admin/approve.jsp, /admin/skins/webjet6/css/, /admin/skins/webjet6/images/, /admin/skins/webjet6/logon, /admin/css/, /admin/m/css/, /admin/scripts/, /admin/m/jscripts/, /admin/license.jsp, /admin/licence.do, /admin/setup, /admin/FCKeditor/editor_set_user.jsp, /components/messages/refresher-ac.jsp, /admin/multiplefileupload.do, /admin/mem, /admin/FCKeditor/editor/skins/webjet/fck_dialog.css, /admin/css/tabpane-luna.css, /admin/imageeditor/load_image.jsp, /admin/skins/webjet8/assets/, /admin/mobile-api/,/components/_common/fulltext_preview.jsp,^/admin/logon$,^/admin/logon/$,^/admin/logon/changePassword$,/admin/scripts/qrcode.js,/admin/v9/dist/fonts/,/admin/v9/dist/images/logo-",
				"admin",
				"URL prefixy stranok a suborov v admin casti, ktore budu pristupne aj pre neprihlaseneho pouzivatela.");

		setInt("editorTitleSize", 33, MOD_EDITOR, "Sirka pola pre nadpis stranky v editore stranok");

		setString("galleryImageQuality", "", MOD_GALLERY,
				"Parameter klality obrazkov pre konverziu cez ImageMagick, zapisuje sa vo formate sirka_px:kvalita;sirka_px:kvalita, teda napr. 0:30;100:50;400:70, pouzije sa najlepsi alebo koncovy interval");

		setString("dmailWhitelistImageDomains", "track.adform.net", "dmail",
				"Zoznam domen oddelenych ciarkou, pre ktore sa nebude inlinovat obrazok do emailu (napr. kvoli statistike otvorenia emailu)");

		// 7.3
		setBoolean("luceneUpdateSpellCheck", false, MOD_SEARCH + ";" + MOD_SECURITY,
				"Ak je true a dany vyraz sa nenajde prida sa do slovnika, vyrazne spomaluje vyhladavanie kvoli reindexacii slovnika po pridani slovicka");
		setString("xsrfReferers",
				SERVER_NAME_MACRO + ",iwcm.interway.sk,cdn.pixlr.com,%pixlr.com,%.google.com,%.google.%,%.seznam.cz,docs.webjetcms.sk,docs.webjetcms.com",
				MOD_SECURITY,
				"Ochrana pred XSRF. Zoznam trusted serverov oddelenych ciarkou, ak je prazdne, ochrana je vypnuta, SERVER_NAME_MACRO sa pri kontrole nahradi za aktualny server");
		setString("adminLogonMethod", "", "user;config",
				"logon metoda (ak sa pouziva ina ako standardna WJ databaza) - napr. sk.iway.klient.specialLogon, pricom metoda ma signaturu: public static String logon(String username, String password, Identity user, Map<String, String> errors, HttpServletRequest request, sk.iway.iwcm.i18n.Prop prop), vramci metody je potrebne nastavit aj disabled items, takze po zalogovani odporucame zavolat UsersDB.setDisabledItems(user); a aj nastavit ostatne prava (odporucema nacitat usera z DB WebJETu)");

		setInt("monitorTestDocId", 1, "server_monitoring;performance",
				"Doc ID stranky, ktorej databazove spojenie (ziskanie nazvu) sa testuje v komponente /components/server_monitoring/monitor.jsp ktoru moze testovat dohladovy SW");

		// Portal
		setBoolean("portalStartup", false, "portal", "Spustanie portalu pri starte WJ");

		// 7.4
		setBoolean("xssDeleteSpaces", false, MOD_SECURITY,
				"Ak sa ma pri testovani XSS vyrazov mazat aj medzera nastavte na true, zvysi to moznu bezpecnost ale moze obmedzit navstevnikov v moznych textoch, nepojde zadat napr. on selected");

		setString("defaultVideoFormat", "mp4", "video;config",
				"Formát videa, do ktorého sa budú enkodovat videa. Mozne hodnoty: 'flv' alebo 'mp4'");
		setBoolean("mp4EnableStreaming", false, "video;performance",
				"ak je nastavene na true je povoleny HTTP streaming MP4 videa");
		setString("ffmpegParams",
				"ffmpeg  -i from -ar 22050 -b:a 32k -f format -s dimension -b:v bitrate -strict experimental -movflags +faststart -y to",
				"video;config",
				"Formát pre parametre ffmpeg, pre starsie verzie ffmpeg napr : ffmpeg -y -i from -ar 22050 -ab 32 -f format -s dimension -b bitrate to ");

		setBoolean("perexGroupUseJoin", false, MOD_SEARCH,
				"Možnosť aby sa používalo vyhľadávanie perex skupín miesto IN v documents na JOIN s mapovacou tabuľkou perex skupín - perex_group_doc");

		// TB AuditLogFile
		setString("auditFileLocation", "", MOD_CONFIG,
				"Cesta k log suboru FileLoggera pre audit. Medzi Dvoma bodkami je pattern pre rotaciu logsuborov kompatibilny s triedou SimpleDateFormat. Príklad: /www/webjet/log/audit.YYYYMMddHH.log");
		setString("auditLogGateway", "AuditLogGateway", MOD_CONFIG, "Konštanta auditLogGateway pre FileLogger");
		setString("auditLogtype", "audit", MOD_CONFIG, "Konštanta auditLogType pre FileLogger");
		setString("auditLogSeparator", "|", MOD_CONFIG, "Oddelovac zaznamov pre auditLogger");
		setString("auditLogClient", "browser", MOD_CONFIG, "Konštanta auditLogClient pre FileLogger TB");
		setString("auditLogEncoding", "windows-1250", MOD_CONFIG, "Kodovanie znakov suboru logu");

		setString("contextPathAdmin", "", MOD_CONFIG,
				"Context path pre CMS node v cluster konfiguracii ak je instalovany na iny ako ROOT kontext");
		setBoolean("contextPathDebug", false, MOD_CONFIG,
				"Ak je nastavene na true zapne sa debug rezim pre ContextPath filter");

		setString("statTableCreateProcedureName", "", mods(MOD_STAT, MOD_CONFIG),
				"Meno stored procedury, ktora sa ma pouzit namiesto create_table prikazov pre vytvorenie tabuliek statistiky");
		setInt("writeTagFileExistsCacheMinutes", 0, MOD_PERFORMANCE,
				"Pre zvysenie vykonu je mozne nastavit na pocet minut pre cachovanie vysledku testovania existencie custom komponenty");
		setBoolean("luceneAsDefaultSearch", false, MOD_SEARCH,
				"Ak je na serveri nasadeny Lucene search je mozne nastavenim tejto premennej prepnut vsetky vyhladavacie komponenty na pouzitie Lucene, nie je potrebne upravovat JSP komponenty");

		setString("pdfBaseUrl", "NULL", MOD_CONFIG,
				"Base URL pre nastavenie generovania PDF, hodnota NULL znamena, ze sa subory (obrazky) pokusi nacitat priamo zo suborov, hodnota LOOPBACK pouzije ziskanu loopback hodnotu a priamo zadane URL pouzije dane URL ako base URL pre stiahnutie obrazkov");
		setString("pdfAuthorName", "WebJET Content Management - www.interway.sk", MOD_CONFIG,
				"Hodnota pola Author pre generovane PDF");

		setString("stripes.LocalePicker.Locales", "sk_SK:UTF-8", MOD_CONFIG,
				"Locale pouzite pre Stripes, nastavuje konfiguraciu LocalePicker.Locales, ak bude hodnota prazdna, pouzije sa systemovy DefaultLocale");
		setBoolean("perexGroupIncludeId", true, MOD_EDITOR,
				"Ak je nastavene na true bude sa v zozname perex skupin zobrazovat aj ID danej skuipny");
		setBoolean("checkFormValidateOnInit", false, "forms",
				"Nastavenie validacie formularu pri jeho inicializacii, pri nastaveni na false sa pri zobrazeni nevaliduje cely formular, pri nastaveni na true sa validuje (tak to bolo aj doteraz)");

		setString("newsComponentsDirs", "", MOD_EDITOR,
				"Zoznam URL adries adresarov, v ktorych su custom news komponenty pre ich zobrazenie v dialogu pre vlozenie news komponenty. Automaticky sa hladaju v /components/INSTALL_NAME/news/ ak su aj v inych adresaroch nastavte do tejto premennej ich zoznam oddeleny ciarkou.");
		setString("proxyHostsException", "", MOD_CONFIG, "Zoznam domen oddelenych ciarkou, pre ktore sa proxy nepouzije");

		setBoolean("editorImageDialogCopyAltToTitle", false, MOD_EDITOR,
				"Ak je nastavene na true bude hodnota ALT atributu kopirovana do TITLE atributu obrazku");
		setBoolean("editorNewDocDefaultAvailableChecked", true, MOD_EDITOR,
				"Ak je nastavene na false bude pri vytvarani novej stranky v editore odskrtnuta moznost zobrazovat");

		setString("xssProtectionStrictPostUrlExceptionSystem",
				"/components/blog/blog,/components/lms/ucitel,/components/gallery/send_card,/components/send_link/send_link,/components/magzilla/,/components/tips/tips_editor,/components/user/change_password,/components/wiki/save,%_autocomplete.jsp",
				MOD_SECURITY,
				"casti URL adries (ich zaciatky) pre ktore sa nepouzije xssProtectionStrict POST, jednotlive URL su oddelene ciarkou");
		setString("xssProtectionStrictPostUrlException", "", MOD_SECURITY,
				"casti URL adries (ich zaciatky) pre ktore sa nepouzije xssProtectionStrict POST, jednotlive URL su oddelene ciarkou");

		setBoolean("serverMonitoringEnablePerformance", false, "server_monitoring;performance",
				"ak je nastavene na true, spusta monitorovanie rychlosti SQL dotazov, web stranok a komponent");
		setBoolean("serverMonitoringEnableJPA", false, "server_monitoring;performance",
				"ak je nastavene na true, spusta monitorovanie rychlosti vykonavania SQL dotazov pre JPA, ma ale za nasledok zvysenie zataze na pamat servera");

		setString("cacheOnDocCahngeMode", "", "performance",
				"Rezim aktualizacie cache po ulozeni web stranky, moznosti: prazdne/none=ziadna zmena, include=vymazanie vsetkych !INCLUDE cache, groupid=vymazanie cache kde sa zhoduje hodnota groupid s ID adresara stranky, all=vymazanie celej cache");

		setString("requestIsSecureHeaderName", "x-forwarded-proto", MOD_SECURITY,
				"Meno HTTP hlavicky, ktora ak je nastavena povazuje sa request za zabezpeceny (https)");

		setString("editorTableDefaultWidth", "200", MOD_EDITOR,
				"Prednastavena sirka pre vytvorenie novej tabulky, ak je zadane cislo, je to v pixeloch, ak je zadane 100% je hodnota v percentach");
		setString("editorTableDefaultHeight", "", MOD_EDITOR,
				"Prednastavena vyska pre vytvorenie novej tabulky, prazdna hodnota znamena auto, cislo je zadane v pixeloch");
		setInt("editorTableDefaultColumns", 2, MOD_EDITOR, "Prednastaveny pocet stlpcov pre vytvorenie novej tabulky");
		setInt("editorTableDefaultRows", 3, MOD_EDITOR, "Prednastaveny pocet riadkov pre vytvorenie novej tabulky");

		setString("httpsRedirectToNonSSLNodeNames", "", MOD_SECURITY,
				"Zoznam nodov clustra na ktorych bude vykonavany redirect zo zabezpecenej na nezabezpecenu verziu zobrazenia web stranky. Prazdna hodnota znamena, ze sa redirect nevykona nikdy, znak * znamena, ze sa vykona na vsetkych nodoch (alebo ak nie je cluster). Pre fungovanie musi byt zapnuty SSL rezim nastavenim httpsAvailable na true");

		setString("galleryVideoMode", "big", MOD_GALLERY,
				"Nastavenie rezimu konverzie videa pre foto galeriu, mozne hodnoty: all=vygeneruje sa male aj velke video, big=vygeneruje sa len velke video, small=vygeneruje sa len male video");

		setString("thumbServletCacheDir", "/WEB-INF/imgcache/", mods(MOD_GALLERY, MOD_PERFORMANCE),
				"Cesta k adresaru pre cache /thumb obrazkov, pre server s vysokym mnozstvom obrazkov odporucame presunut na ine miesto ako /WEB-INF/ kvoli rychlosti startu Tomcatu");

		setString("xRobotsTagValue", "noindex, nofollow", "editor,seo",
				"Hodnota hlavicky X-Robots-Tag pre adresare nasatavene v premennej xRobotsTagUrls");
		setString("xRobotsTagUrls", "/components/,NOT_SEARCHABLE_PAGE", "editor,seo",
				"Zoznam prefixov adresarov oddelenych ciarkou pre nastavenie hlavicky X-Robots-Tag, ak zoznam obsahuje hodnotu NOT_SEARCHABLE_PAGE nastavi sa hlavicka aj pre stranky, ktore maju vypnute vyhladavanie");

		setString("navbarDefaultType", "normal", "editor,seo",
				"Typ generovanej navigacnej listy, moze mat hodnoty normal pre standardny rezim, alebo RDF pre generovanie vo forme RDF, alebo schema.org pre generovanie vo forme schema.org");

		setInt("defaultVideoWidth", 854, mods(MOD_CONFIG, MOD_GALLERY), "Prednastavena sirka videa");
		setInt("defaultVideoHeight", 480, mods(MOD_CONFIG, MOD_GALLERY), "Prednastavena vyska videa");
		setInt("defaultVideoBitrate", 2048, mods(MOD_CONFIG, MOD_GALLERY), "Prednastaveny bitrate videa");

		setBoolean("enableSessionSerializableCheck", true, MOD_CONFIG,
				"Prepinac na zapnutie/vypnutie kontroly serializovatelnosti objektov v session, tato je obmedzena este aj na domenu iwcm.interway.sk");

		setBoolean("multiGroupKeepSortPriority", true, MOD_EDITOR,
				"Možnosť zachovať poradie zobrazovania stránok v multi-kategóriách (pre kliknuti na kontextove menu posunut vyssie / nizsie).");

		setBoolean("adminLogonMethodAllowNormalTry", true, mods(MOD_USER, MOD_CONFIG, MOD_SECURITY),
				"Ak je nastavene na true je mozne parametricky zapnut standardnu metodu pre prihlasenie namiesto customizovanej");

		setString("ntlmDefaultUserPhoto", "", MOD_NTLM, "URL adresa defaultneho obrazka");

		setBoolean("mp4StreamingDebugSlowdown", false, mods(MOD_CONFIG, "video", "mp4"),
				"Ak je nastavene na true je spomaleny streaming mp4 suborov pre testovanie");
		setInt("mp4StreamingBufferSize", 64000, mods(MOD_CONFIG, "video", "mp4"),
				"Velkost interneho buffera pre streaming mp4 suborov");

		setString("disableCookiesCookieName", "cc_cookie_decline", MOD_CONFIG,
				"Hodnotou konstanty je nazov cookie premennej ktora ked ma hodnotu ako ma nastavena konfiguracna premenna disableCookiesCookieValue, tak nedovoli generovat nove cookies premenne ( Ak su generovane cez -> Tools.addCookie() )");
		setString("disableCookiesCookieValue", "cc_cookie_decline", MOD_CONFIG,
				"Hodnota ktoru musi obsahovat cookie pre konfiguracnu premennu disableCookiesCookieName");

		setString("contextFilterAddPathReplaces", "", MOD_CONFIG,
				"Zoznam nahrad URL adries pre ContextFilter.addContextPath vo formate stara_url|nova_url, kazdy zaznam na novom riadku, vyraz {CP} sa nahradza za ContextPath");
		setString("contextFilterRemovePathReplaces", "", MOD_CONFIG,
				"Zoznam nahrad URL adries pre ContextFilter.removeContextPath vo formate stara_url|nova_url, kazdy zaznam na novom riadku, vyraz {CP} sa nahradza za ContextPath");

		setString("componentsDirectCallExceptionsSystem",
				"/components/_common/clk.jsp,/components/_common/combine.jsp,/components/_common/html_tags_support.jsp,/components/_common/wysiwyg/,%.js.jsp,/components/banner/banner.jsp,%/invoice_email.jsp,/components/basket/order_payment_bank_reply.jsp,/components/blog/blog_save.jsp,%/detail.jsp,/components/cestovka/,%/upload.jsp,/components/forum/iframe.jsp,%new.jsp,%/saveok.jsp,%/savefail.jsp,%/new_file.jsp,%/send_card.jsp,%/show_gallery_image,%videoplayer.jsp,%ajax,%voteResultsDiv,%check_form,%/forum-open,%/forum_mb_open,%/forum_mb,/components/magzilla/,/components/helpdesk/,%-approve.jsp,/components/mail/,/components/mcalendar/,%send_link_form,/components/reservation/,/components/user/change_password.jsp,/components/user/authorize.jsp,%import.jsp,/components/zmluvy/,/components/server_monitoring/monitor.jsp,/components/chat/js.jsp,/components/page_update_info/add_subscriber,/components/page_update_info/remove_subscriber,%jscript.jsp,%addbasket_popup.jsp,/components/messages/refresher-ac.jsp,%/iframe_blank,/components/user/fileforward.jsp,%spamprotectiondisable.jsp,%/htmlarea/,%json,/components/qa/admin_answer.jsp,%popcalendar.jsp,%jscripts.jsp,%chart.jsp,%date_locale.jsp,%click.jsp,/components/user/logon.jsp,/components/cloud/calendar/potvrd_akciu_verejnost.jsp,/components/customer_reviews/potvrd_review.jsp,/components/gis/potvrd_gis.jsp,/components/gallery/photoswipe/photoswipe.jsp",
				MOD_SECURITY, "Zoznam zaciatkov URL adries pre ktore je povolena vynimka priameho volania JSP komponenty");
		setString("componentsDirectCallExceptions", "", MOD_SECURITY,
				"Zoznam zaciatkov URL adries pre ktore je povolena vynimka priameho volania JSP komponenty");

		setString("serverMonitoringEnableIPs", "127.0.0.1,192.168.,10.,62.65.161.,85.248.107.,195.168.35.4,195.168.35.5,62.168.118.66,62.168.118.90,62.168.118.67,172.17.0.1",
				mods(MOD_SECURITY, "server_monitoring"),
				"Zoznam IP adries z ktorych je dostupna komponenta monitor.jsp pre monitorovanie servera");

		// cloud WebJET
		setString("cloudStaticFilesDir", "", "cloud",
				"Hlavny adresar v ktorom su ukladane staticke subory jenodlitvych webov v cloude (musi koncit znakom /), mozne zadat hodnotu {FILE_ROOT}static-files/ pre umiestnenie do korenoveho-priecinka/static-files/");
		setBoolean("templatesUseDomainLocalSystemFolder", false, mods(MOD_EDITOR, "templates"),
				"Ak je nastavene na true su hladane objekty pre sablonu v korenovom priecinku aktualnej domeny v pod adresari System");

		setBoolean("editorEscapeInvalidCharacters", false, MOD_EDITOR,
				"Pri ukladani stranky odstranuje nepovolene znaky napr. z kopirovania z PDF, pre titulok, obsah a perex.");

		setString("xUaCompatibleAdminValue", "IE=edge", MOD_CONFIG,
				"Nastavenie HTTP hlavicky X-UA-Compatible pre MS IE pre admin cast");

		setBoolean("inlineEditingEnabled", false, MOD_EDITOR,
				"Ak je nastavene na true je povolena inline editacia stranok a komponent");
		setString("inlineEditingComponentsSystem",
				"/components/gallery/gallery.jsp,/components/news/news.jsp,/components/news/news-,/components/inquiry/inquiry,/components/site_browser/site_browser,/components/map/,/components/basket/",
				MOD_EDITOR, "Zoznam komponent pre ktore je povolena inline editacia vlastnosti");
		setString("inlineEditingComponents", "", MOD_EDITOR,
				"Zoznam custom komponent pre ktore je povolena inline editacia vlastnosti");
		setString("inlineEditingDisabledUrls", "", MOD_EDITOR,
				"Zoznam URL adries pre ktore sa nezobrazi cela inline editacia vratane tlacitok komponent, pouzije sa URL zacinajuce na dany vyraz, ak je potrebna presna zhoda pouzite na konci hodnoty znak ! (napr. pre uvodnu stranku zadajne /!)");
		setString("inlineEditingToolbarDisabledUrls", "", MOD_EDITOR,
				"Zoznam URL adries pre ktore sa nezobrazi page toolbar (budu sa ale zobrazovat toolbary komponent), pouzije sa URL zacinajuce na dany vyraz, ak je potrebna presna zhoda pouzite na konci hodnoty znak ! (napr. pre uvodnu stranku zadajne /!)");
		setBoolean("inlineEditingToolbarMoveTop", true, MOD_EDITOR,
				"Ak je nastavene na true presunie sa toolbar editacie web stranky do hornej casti okna");
		setString("inlineEditingToolbarSelectorPrepend", "body", MOD_EDITOR,
				"Element pred ktory je spravene prepend pre MoveTop");
		setBoolean("editorReplaceComponentsPlaceholdersWithPreview", false, MOD_EDITOR,
				"Ak je nastavene na true bude sa v editore nahradzat placeholder pre komponenty za realny nahlad");
		setBoolean("inlineEditingAllowDelete", true, MOD_EDITOR,
				"Nastavenie moznosti zobrazovania vymazania komponenty pri inline editacii, nastavenim na false sa vypne zobrazenie zmazania komponenty");

		// default parametre metadataCleaner
		setString("metadataRemoverCommand", "", "editor;files;gallery",
				"Ak je nastavene aktivuje sa odstranovanie metadat z uploadovanych suborov, alebo sa pouzije imageMagick na zmensenie velkosti - nastavte na /usr/bin/convert");
		setString("metadataRemoverParams", "{filePath}", "editor;files;gallery",
				"Parametre metadata removera, pre zmensenie obrazka cez imageMagick nastavte na {filePath} -resize 1920x1080 {filePath}");
		setString("metadataRemoverExtensions", "jpg,jpeg,png,gif", "editor;files;gallery",
				"Povolene pripony, z ktorych sa budu odstranovat metadata.");

		setBoolean("groupCreateBlankWebpageAfterCreate", true, MOD_EDITOR,
				"Ak je nastavene na true bude po vytvoreni noveho adresara automaticky vytvorena aj prazdna web stranka s rovnakym nazvom");

		setBoolean("fileDialogDoNotReadProps", false, mods(MOD_EDITOR, MOD_PERFORMANCE),
				"Urychlenie nacitania zoznamu obrazkov do dialogu pre editor - necitaju sa z disku udaje datum zmeny, velkost, rozmer obrazku. Funkcia sa zapne nastavenim na true.");

		// convert CMYK do RGB
		setBoolean("galleryConvertCmykToRgb", false, MOD_GALLERY,
				"Ak je nastavene na true tak sa zistuje, ci je fotka v CMYK a ak ano, konvertuje sa do RGB.");
		setString("galleryConvertCmykToRgbInputProfilePath", "", MOD_GALLERY,
				"Cesta (RealPath) k vstupnemu ICC profilu na disku.");
		setString("galleryConvertCmykToRgbOutputProfilePath", "", MOD_GALLERY,
				"Cesta (RealPath) k vystupnemu ICC profilu na disku.");

		setString("projectSpecificComponentExporterResolverClass", "", MOD_CONFIG,
				"Project specific trieda pre exportovanie custom komponentov do xml v ramci sycnhronizacie WJ, musi implementovat Interface sk.iway.iwcm.sync.export.ComponentExporterResolver");

		setString("fileHistoryPath", "/WEB-INF/libfilehistory/", "file history", "Cesta k historii suborov");

		setString("bazarApproveNotificationEmail", "", "bazar", "Email adresa notifikacie pre schvalenie inzeratu");

		setString("beforeFileIndexerEditorSaveMathod", "", "config;files",
				"Metoda, ktora sa zavola po zaindexovani suboru");

		setBoolean("galleryUseFastLoading", false, mods(MOD_GALLERY, MOD_PERFORMANCE),
				"Ak je nastavene na true pouzije sa pre vypis galerie zjednoduseny test suborov, zryhchluje zobrazenie na sietovych file systemoch");

		setBoolean("captchaDictionaryEnabled", false, MOD_CAPTCHA, "Aktivacia slovnika pre captchu");
		setInt("captchaMinLength", 5, MOD_CAPTCHA, "Minimálna dĺžka Captcha reťazca");
		setInt("captchaMaxLength", 8, MOD_CAPTCHA, "Maximálna dĺžka Captcha reťazca");

		setString("changePasswordPageUrl", "/components/user/change_password.jsp", MOD_USER,
				"cesta k suboru (alebo stranke) na zmenu hesla");

		setString("customContextListeners", "com.github.pukkaone.jsp.EscapeXmlELResolverListener", mods(MOD_CONFIG),
				"Zoznam fully qualified names tried ktore sa maju dynamicky pridat do web.xml ako listenery.");

		setString("captchaFontColorRGB", "", MOD_CAPTCHA,
				"RGB hodnota farby pisma pre captchu, ak nieje nastavena, pouziva sa default random color, ocakava 3 ciarkou oddelene cisla 0-255");
		setString("captchaBackgroundColorRGB", "", MOD_CAPTCHA,
				"RGB hodnota pozadia pre captchu, ak nieje nastavena, pouziva sa default FunkyBackgroundGenerator, ocakava 3 ciarkou oddelene cisla 0-255");

		setString("editorInvalidCharactersInDecFormat", "", MOD_EDITOR,
				"Doplnok ku editorEscapeInvalidCharacters, kde je mozne zadat konkretne znaky v decimal formate oddelene ciarkou, ktore sa budu odstranovat.");

		setInt("editorImageWidthSizeCheck", 800, MOD_EDITOR,
				"Sirka obrazka pre ktoru sa zobrazi hlasenie o moznosti jeho zmensenia pri vkladani do stranky");
		setInt("editorImageHeightSizeCheck", 500, MOD_EDITOR,
				"Vyska obrazka pre ktoru sa zobrazi hlasenie o moznosti jeho zmensenia pri vkladani do stranky");

		setBoolean("editorDisableHistory", false, MOD_EDITOR,
				"Po nastaveni na true vypne zapisovanie stranok do documents_history tabulky pri pre-ulozeni");

		setBoolean("nginxProxyMode", false, MOD_PERFORMANCE,
				"Po nastaveni na true zapne rezim WebJETu s vyuzitim nginx proxy modu, generuje cookie nc (no cache) s hodnotou 1 akonahle je prihlaseny pouzivatel alebo je nestandardny forceBrowserDetector");

		setBoolean("fbrowserShowOnlyWritableFolders", false, MOD_SECURITY,
				"Po nastaveni na true sa budu v zalozke subory zobrazovat len adresare na ktore ma pouzivatel nastavene prava");

		setString("captchaMode", "nonlinear", MOD_SECURITY,
				"Rezim generovania captcha obrazku, mozne hodnoty random, line, double, doubleRandom, decorated, baffle, nonlinear");
		setString("captchaBackgroundMode", "funky", MOD_SECURITY,
				"Rezim pozadia captcha obrazku, mozne hodnoty funky, multishape, elipse");
		setInt("captchaWaveSize", 40, MOD_SECURITY, "Dlzka vlny pre generovanie captcha");

		setBoolean("formLoginProtect", true, MOD_SECURITY,
				"Po nastaveni na true zrusi moznost zapamatania mena a hesla na prihlasovacich formularoch, zapne zobrazenie jednoduchyc chybovych hlaseni pri zlom mene / hesle, vypne zobrazenie emailu na ktory sa posle zabudnute heslo");

		setString("xFrameOptions", "SAMEORIGIN", MOD_SECURITY,
				"Hodnota hlavicky X-Frame-Options pre ochranu pred CSRF utokom");

		setBoolean("auditDontLogUsrlogon", false, MOD_SECURITY,
				"Po nastaveni na true sa nebude auditovat bezne prihlasenie pouzivatela, vhodne na vysoko zatazeny intranet kde to zbytocne zahlcuje audit");

		setInt("welcomeDataCacheMinutes", 60, MOD_PERFORMANCE, "pocet minut ulozenia dat do cache");
		setInt("welcomeDataBackTime", 7 * 24 * 60, MOD_PERFORMANCE, "pocet minut za ktore sa budu data z DB vyberat");
		setInt("welcomeWallDocId", 9800900, MOD_PERFORMANCE, "fiktivne docid pre nastenku WJ8");
		setInt("welcomeDataSize", 10, MOD_PERFORMANCE, "pocet poloziek na uvitacej stranke");
		setInt("welcomeDataSizeAudit", 20, MOD_PERFORMANCE, "pocet poloziek na uvitacej stranke v audite");
		setInt("welcomeDataProjectId", 0, MOD_PERFORMANCE, "project id na welcome stranke");
		setString("welcomeDataUrlTicket", "https://helpdesk.interway.sk/components/magzilla/tickets_ajax.jsp");

		setInt("galleryCacheResultMinutes", 0, mods(MOD_PERFORMANCE, MOD_GALLERY),
				"Pocet minut pocas ktorych sa cachuje zoznam obrazkov v galerii, zmena sa detekuje podla zmeny datumu adresara (dostupne len na OS Linux)");

		setBoolean("uploadSimulateSlowSpeed", false, MOD_PERFORMANCE,
				"Ak sa nastavi na true spomali sa upload pre jeho simulaciu");
		setString("showDocActionAllowedDocids", "*", MOD_SECURITY,
				" Nastavene na \"*\" nerobi ziadnu akciu, nastavene na \" \" (medzeru) blokuje vsetky volania showdoc.do a nastavne na cisla (docId) oddelene ciarkami, povoluje iba konkretne docId ");

		setString("amazonEmailServiceHost", "", "dmail;config",
				"V pripade ze je zapnute posielanie dmailu cez Amazon SES tak sa tymto specifikuje konkretny region pre odosielanie, inak sa region urcuje dynamicky");

		setString("xXssProtection", "1; mode=block", MOD_SECURITY,
				"Hodnota hlavicky X-XSS-Protection pre ochranu pred XSS utokom");
		setString("serverName", "unknown", MOD_SECURITY, "Hodnota hlavicky Server v HTTP odpovediach");
		setString("strictTransportSecurity", "max-age=31536000", MOD_SECURITY,
				"Hodnota hlavicky Strict-Transport-Security v HTTP odpovediach, odporucame nastavit na hodnotu: max-age=31536000; includeSubDomains");
		setString("xContentTypeOptions", "nosniff", MOD_SECURITY,
				"Hodnota hlavicky X-Content-Type-Options pre nastavenie urcovania typov suborov podla obsahu (ignorovanie pripony)");

		setBoolean("formAllowOnlyExistingFormsOnPublicNode", true, MOD_SECURITY,
				"Povolenie odoslania len existujucich formularov na public nodoch clustra");

		setInt("dmailMaxRetryCount", 5, "dmail",
				"Maximalny pocet pokusov odoslania emailu pre hromadny email pri chybe odoslania");
		setInt("dmailSleepTimeAfterException", 20000, "dmail",
				"Pocet milisekund zastavenia odosielania emailov pri chybe odoslania");
		setInt("smtpConnectionTimeoutMillis", 30 * 1000, "dmail",
				"Pocet milisekund pre timeout cakania na vytvorenie SMTP spojenia");

		setInt("fileIndexerMaxFileSize", 8 * 1024 * 1024, "config;performance",
				"Maximalna velkost suboru ktory sa bude indexovat");

		setString("basketInvoiceSenderName", "", "basket",
				"Ak je nastavene pouzije sa ako meno odosielatela objednavky, ak je prazdne pouzije sa podla mena servera");

		setBoolean("pixabayEnabled", true, MOD_GALLERY,
				"Povolenie založky fotobanka v dialogu vloženia obrázku do CKEditora. Musí byť nastavená konštanta pixabayApiKey");
		setString("pixabayApiKey", "2181892-fb57b175b61866ccf90884a0c", MOD_GALLERY, "Api kľúč pre fotobanku pixabay.");
		setString("pixabayDefaultWidth", "1280", MOD_GALLERY,
				"Základná šírka obrázku pri vložení obrazku z fotobanky pixabay.");

		// WebJET 8 konfiguracia
		setString("elfinderLibraryFolders", "/images,/images/gallery,/images/video,/files", "files",
				"Zoznam adresarov, ktore sa zobrazia v suboroch v Kniznici");
		setString("ckeditor_toolbar",
				"{ name: 'clipboard', items: ['Paste', '|', 'Cut', 'Copy', ';' , 'PasteText', 'PasteFromWord']},\n" +
						"\t\t\t\t{ name: 'fontAndStyle', items: ['Styles', 'RemoveFormat', ';', 'Bold', 'Italic', 'Underline', 'Strike', 'Subscript', 'Superscript', 'TextColor', 'BGColor']},\n"
						+
						"\t\t\t\t{ name: 'paragraphAndAlign', items: ['BulletedList', 'NumberedList', 'Outdent', 'Indent', '-', 'ShowBlocks', ';', 'JustifyLeft', 'JustifyCenter', 'JustifyRight', 'JustifyBlock', '-', 'Table']},\n"
						+
						"\t\t\t\t{ name: 'insert', items: ['Image', '|', 'Unlink', 'SpecialChar', ';', 'Link', 'WJForms', '|', 'Components', '|', 'Htmlbox']},\n"
						+
						"\t\t\t\t{ name: 'tools', items: ['Undo', 'Redo', ';', 'Find', 'Replace']}",
				MOD_EDITOR, "Konfiguracia poloziek ckeditor toolbaru");

		// WebJET 8 appka
		setString("mobileAppPerexImage", "/images/mobileAppPerex/");
		setInt("mobileAppDefaultGroup", 1);
		setString("mobileAppGalleryImage", "/images/mobileApp/");

		setInt("systemPagesIdsStart", 99999990);
		setInt("systemPagesIdsEnd", 99999999);

		setInt("systemPagesNotApprovedDocs", getInt("systemPagesIdsStart") + 6);
		setInt("systemPagesRecentPages", getInt("systemPagesIdsStart") + 7);
		setInt("systemPagesMyPages", getInt("systemPagesIdsStart") + 8);
		setInt("systemPagesDocsToApprove", getInt("systemPagesIdsStart") + 9);

		setString("whiteListedUA", "chrome, firefox, msie");

		setInt("editorUploadActualPageMaxDeep", 4, MOD_EDITOR,
				"Maximálna hĺbka štruktúry pre upload obrázkov a súborov v editore.");

		// import, export suborov
		setString("doNotExport", "/images?/files?/jscripts?/templates");
		setString("exportArchivePath", "/files/protected/dataexport");
		setString("importArchivePath", "/files/protected/dataimport");
		setString("rollbackArchivePath", "/files/protected/datarollback");
		setString("exportStandardDirs",
				"/components/INSTALL_NAME?/WEB-INF/classes/sk/iway/INSTALL_NAME?/WEB-INF/sql/autoupdate-INSTALL_NAME.xml?/WEB-INF/classes/text-INSTALL_NAME.properties");

		// ABTesting
		setBoolean("ABTesting", false, "abtest", "Po nastaveni na true sa aktivuje AB testovanie web stranok");
		setString("ABTestingRatio", "50:50", "abtest", "Pomer pri generovani stranok medzi verziou A a B");
		setString("ABTestingName", "abtestvariant", "abtest",
				"Nazov, ktory sa pridava do URL adresy B verzie stranky na jej odlisenie - pouziva sa aj na dohladanie B verzie stranky");
		setString("ABTestingCookieName", "wjabtesting", "abtest",
				"Meno cookie pouzitej pre zapamatanie si verzie testu pri split testoch - ak sa rozhodne pre B variantu, zapamata sa v cookie s tymto nazvom a podla toho sa dalej budu poskytovat B verzie stranok");
		setInt("ABTestingCookieDays", 30, "abtest",
				"Pocet dni zapamatania si zvolenej verzie pri AB testovani - cas exspiracie cookie");

		//setInt("logonBlockedDelaySecondsMax", 5 * 60, MOD_SECURITY,
		//		"maximalne mozne zablokovanie prihlasenia pri opakovanych neuspesnych pokusoch o prihlasenie");
		//nahradene logonBlockedAfterUnsuccessCount setInt("errLogonMaxCount", 5, MOD_SECURITY,
		//		"maximalny pocet nespravnych prihlaseni, po dosiahnuti tohto poctu sa zacne predlzovat cas do dalsieho mozneho prihlasenia, ak je nastavene na 0 tak sa cas vobec nepredlzuje");

		setBoolean("reCaptchaEnabled", false, MOD_CAPTCHA,
				"nastavenim na true, prepne defaultnu WJ captchu na reCaptchu");
		setString("captchaType", "internal", MOD_CAPTCHA, "Typ capthy. internal / reCaptcha / reCaptchaV3 / invisible");
		setBoolean("captchaLoadAfterFocus", true, MOD_CAPTCHA,
				"Nacitavanie JS pre captchu az po kliknuti do pola formularu");

		setBoolean("multiDomainUseAliasAsInstallName", false, "multidomain",
				"Nastavenim na true sa pre MultiDomain bude pouzivat hodnota z konfiguracnej premennej multiDomainAlias:domena=alias ako hodnota installName pre HTTP requesty");
		setString("fbrowserAlwaysShowFolders", "", "multiweb",
				"V premennej je mozne nastavit adresare oddelene znakom noveho riadka ktore sa budu zobrazovat na citanie vzdy aj pri nastavenej premennej fbrowserShowOnlyWritableFolders na true");

		setString("AngularCDNVersion", "2.0.0-beta.0");
		setBoolean("sassCompilerEnabled", false, MOD_CONFIG,
				"nastavenim na true, sa zapne kompilacia sass a scss suborov");

		setBoolean("imageAlwaysCreateGalleryBean", false, MOD_GALLERY,
				"Ak je zapnute na true bude sa zaznam v gallery DB tabulke vytvarat aj pre obrazky mimo foto galerie");

		setString("thumbWriteServer", "", "thumb",
				"Pre cluster na ktorom je mozny zapis len na jednom node urcuje http adresu tohto nodu na ktory sa da pripojit z public nodov a obrazok vygenerovat");
		setString("thumbWriteNodeName", "", "thumb",
				"Pre cluster na ktorom je mozny zapis len na jednom node urcuje meno tohto nodu na ktory sa da pripojit z public nodov a obrazok vygenerovat");

		setBoolean("editorGroupReplaceNumberWithDashInUrl", false, MOD_EDITOR,
				"Ak je nastavene na true bude sa pre adresare ktorych nazov je cislo nahradzat ich URL za znak - co sposobi, ze sa nebudu do URL stranok generovat");
		setBoolean("qaCheckedPublishOnWeb", false, "",
				"Ak je true, tak sa v module Otazky a odpovede pri novom/editovanom zazname defaultne zaskrtne 'Zobrazovať na web stránke'");

		setBoolean("galleryUploadDirVirtualPath", false, MOD_GALLERY,
				"ak je nastavene na true pouzije sa URL adresa web stranky ako adresar pre upload suborov (normalne sa pouziva len struktura adresarov)");

		setBoolean("showOnlyActualPublishedDoc", false, "webpages",
				"Ak je true, zobrazi beznemu pouzivatelovi stranku platnu na zaklade datumov publikovania od-do");
		setBoolean("editorShowTitleLettersCount", false, MOD_EDITOR,
				"Po nastaveni na true bude v editore za titulkom zobrazovat pocet znakov titulku");
		setString("adminAfterLogonRedirect", "", MOD_CONFIG,
				"Moznost zadania presmerovacej URL po prihlaseni sa do adminu napr. rovno na WEB Stranky");

		setString("symlinkTranslate", "", MOD_CONFIG,
				"Preklad symlinkovanych adresarov nazad na standardne cesty akoby boli priamo realne v adresari, format rovnaky ako natUrlTranslate");
		setBoolean("perexGroupsFastLoad", false, MOD_PERFORMANCE,
				"Ak je nastavene na true nebudu sa pri zmene clanku v editore reloadovat perex skupiny, ma zmysel nastavit na true na weboch kde je radovo 1000 perex skupin a vsetky su dostupne vsade");

		setString("elfinderRedirectFolders", "/images,/files", "files",
				"Pri premenovani/vystrihnuti sa pri zadanych adresaroch a ich podadresaroch budu vytvarat presmerovania na subory");
		setBoolean("elfinderUseFastLoading", false, "files,performance",
				"Ak je true nebude elfinder ziskavat last modified, size a nebude sa kontrolovat, ci priecinky maju podpriecinky(bude sa predpokladat, ze ano)");

		setString("contentSecurityPolicy", "", MOD_SECURITY,
				"Obmedzenie ako stránka ma stranka nacitavat rôzne zdroje, ak mate httpS certifikat odporucame nastavit na hodnotu: default-src 'none'; script-src https: blob: data: 'unsafe-inline' 'unsafe-eval'; worker-src https: blob:; child-src https: blob:; style-src https: data: 'unsafe-inline' 'unsafe-eval'; img-src https: data: 'unsafe-inline'; font-src https: data:; object-src blob: 'self'; base-uri 'none'; frame-ancestors 'self'; connect-src blob: 'self'; frame-src 'self';"); // default-src
																																																																																																																																																																		// https:
																																																																																																																																																																		// data:
																																																																																																																																																																		// 'unsafe-inline'
																																																																																																																																																																		// 'unsafe-eval'
		setString("contentSecurityPolicySvg", "default-src 'self' 'unsafe-inline'", MOD_SECURITY,
				"Obmedzenie pre SVG obrazky, ktore mozu obsahovat vlozeny javascript kod, odporucame nastavit na hodnotu: default-src 'self' 'unsafe-inline'");
		setString("refererPolicy", "same-origin", MOD_SECURITY,
				"Nastavenie HTTP hlavicky Referrer-Policy, odporucame nastavit na hodnotu same-origin");

		setBoolean("constantsAliasSearch", false, "",
				"Ak je true, vsetky nazvy konstant sa budu menit na domena-nazovKonstanty (pouzitelne napr. pri multiwebe). Prejavy sa az po restarte wj.");

		setString("editorMagiclineElements",
				"table: 1, hr: 1, div: 1, ul: 1, ol: 1, dl: 1, form: 1, blockquote: 1, iframe: 1, p: 1, img: 1, h1: 1, h2: 1, h3: 1, h4: 1, h5: 1, h6: 1, header: 1, section: 1",
				MOD_EDITOR, "Zoznam elementov pre ktore sa zobrazuje magicline v editore");

		setBoolean("usersBigList", false, MOD_PERFORMANCE,
				"Pre instalacie kde je vysoky pocet pouzivatelov nastavenim na true budu pouzivatelia zobrazeny starsim sposobom so serverovym strankovanim");

		setBoolean("userPermsActualPageAutomatic", true, MOD_CONFIG,
				"Po prihlaseni sa automaticky pridaju do prav na subory prava podla povolenych adresarov vo web strankach");

		setString("AngularCDNVersion", "", "angular", "Verzia angular.io ktora sa ma vlozit do stranky");
		setString("wundergroundWeatherApiKey", "", "app-weather", "Api kluc k Wunderground napr. 9f95eb106c4734fe");

		setBoolean("templatesShowUsage", true, MOD_PERFORMANCE,
				"Ak je nastavene na true zobrazuje v zozname sablon pocet pouziti danej sablony");

		setInt("logonLoginBlockedDelay", 60, MOD_SECURITY,
				"Cas v sekundach, pocas ktorych nebude mozne znova sa prihlasit po zadani zleho hesla a 'logonBlockedAfterUnsuccessCount' poctu neuspesnych prihlaseni z daneho loginu");
		setInt("logonBlockedAfterUnsuccessCount", 5, MOD_SECURITY,
				"Pocet neuspesnych prihlaseni po ktorych sa aplikuje delay 'logonBlockedDelayAfterUnsuccessCount' ");

		setString("adminLoaderBannedUrls",
				"/admin/FCKeditor/editor_set_user.jsp|/admin/refresher.jsp|/components/media/get_media.jsp", MOD_EDITOR,
				"Zoznam URL pre ktore sa nebude zobrazovat loader pri ajax dotazoch");

		setString("googleMapsApiKey", "", MOD_CONFIG, "Možnosť globálneho nastavenia API kľúču pre Google mapy");
		setString("canBeShownForUserAgent", "", MOD_CONFIG,
				"Výnimka pre konštantu showOnlyActualPublishedDoc na hlavičku User-Agent obsahujúcu reťazce v tejto premennej oddelenej , alebo |, napr. facebookexternalhit,Facebot");

		setBoolean("basketDiscountEnabled", false, MOD_BASKET,
				"Ak je nastavené na true (defaultne je false) tak z field_e user profilu zoberie zlavu v percentach na vsetky polozky");
		setInt("basketVatPercentage", 0, MOD_BASKET,
				"Nastavuje sa v percentach. Zvolene percento sa priratava ku sume.(Sluzi na nastavenie DPH)");

		setString("pageFunctionsInclude", "", MOD_CONFIG,
				"Zoznam js suborov vlozenych do page_functions.js.jsp, oddelene znakom |");

		setString("downloadUrlUserAgent",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10.13; rv:65.0) Gecko/20100101 Firefox/65.0 (+http://webjetcms.sk)",
				MOD_CONFIG,
				"Nastaví hodnotu User-Agent pri sťahovaní cez Tools.downloadUrl, ak je hodnota prázdna, nenastaví atribút vôbec.");

		setBoolean("bootstrapEnabled", true, MOD_EDITOR,
				"Nastavenie dostupnosti bootstrap v sablonach, pouzivaju sa potom ine CSS triedy pre zarovnanie textov atd");
		setString("problemReportEmail", "info@webjet.eu", MOD_EDITOR,
				"Email adresa pre nahlasenie problemu z inline editacie");
		setBoolean("alwaysStoreUploadedFileToRequest", false, "upload",
				"Ak je true, po uloade vzdy ulozi uploadovane subory do requestu, do atributu 'MultipartWrapper.files' => sk.iway.iwcm.system.stripes.MultipartWrapper.build(HttpServletRequest, File, long)");

		setInt("uploadProtectionInterval", 900, "upload",
				"Cas ktory sa caka po kazdom uploade aby nenastalo Too Many Connections");

		setString("restAllowedIpAddresses", "127.0.0.1", null,
				"povolene IP adresy (oddelene ciarkou), pouziva sa v sk.iway.iwcm.rest.RestController");

		setBoolean("SpringRequiresSecure", false, MOD_CONFIG,
				"Ak je nastavene na true, vyzaduje httpS pripojenie pre Spring volania");

		setString("appstorePromo", "cmp_news,menuGallery,menuBanner,cmp_video", MOD_EDITOR, "zoznam klucov promo aplikacii v appstore");

		setBoolean("elfinderMetadataEnabled", false, mods(MOD_SECURITY, "files"),
				"Ak je nastavene na true, tak sa povoli metadata plugin pre elfinder (nastavenie opravneni na subory)");
		setBoolean("elfinerMetadataAutopopup", false, mods(MOD_SECURITY, "files"),
				"Ak je nastavene na true, tak sa automaticky po uploade suboru zobrazi dialog metadát");

		setString("uploadFileActionReflectionLoader", "", MOD_CONFIG,
				"Po uploade sa zavolaju nastavene triedy oddelene zankom | (sk.iway.vubintra.upload.UploadFile.checkFileClass|sk.iway.vubintra.upload.UploadFile2.checkFileClass). Metode sa poslu parametre - HttpServletRequest requsest, Identity user, String fileUrl");

		setString("logInstallName", "", MOD_CONFIG,
				"Meno instalacie ktore sa zobrazi v logu, pouziva sa ked na jednom tomcate mame viac hostov s rovnakym install name");

		setString("wjImageViewer", "photoswipe", MOD_GALLERY,
				"Konfiguracia typu zobrazenia nahladu obrazku vlozeneho do stranky, moze byt wjimageviewer alebo photoswipe");

		setInt("redirectRegExpCacheMinutes", 60 * 24, MOD_PERFORMANCE,
				"Pocet minut pre drzanie cache pri presmerovani regularnych vyrazov");

		setBoolean("fileArchivCanEdit", true, MOD_FILE_ARCHIV,
				"Ak je false nie je mozne vkladat subory (napr. ked je update)");
		setString("fileArchivAllowExt", ".pdf,.xlsx,.xls,.doc,.docx,.asc,.xml,.csv", MOD_FILE_ARCHIV,
				"Pripony súborov, ktoré file archiv nahrať");
		setInt("fileArchivatorCacheTime", 400, MOD_FILE_ARCHIV, "Cache v minutach pre vypis suborov na frontende");
		setBoolean("fileArchiv-delete-cache-public-node", true, MOD_FILE_ARCHIV, "Ak je true maze aj public nody");
		setString("fileArchivFromMail", "", MOD_FILE_ARCHIV,
				"Emailova adresa z ktorej chodia notifikacie, napr o nahrati suboru v urcity cas");
		setString("fileArchivDefaultDirPath", "files/archiv/", MOD_FILE_ARCHIV,
				"Priecinok do ktoreho sa defualtne nahravaju subory z archivu. Default : files/archiv/");
		setString("fileArchivInsertLaterDirPath", "files/archiv_insert_later/", MOD_FILE_ARCHIV,
				"Priecinok do ktoreho sa defualtne nahravaju subory pre nahratie neskor. Default : files/archiv_insert_later/");

		setString("inlineEditableObjects", "", MOD_EDITOR,
				"Zoznam objektov ktore je mozne inline editovat (okrem doc_data), napr. doc_header,doc_footer,doc_right_menu");

		setString("ckeditor_toolbar-standalone",
				"{ name: 'clipboard', items: ['Paste', '|', 'Cut', 'Copy', ';' , 'PasteText', 'PasteFromWord']},\n" +
						"\t\t\t\t{ name: 'fontAndStyle', items: ['Styles', 'RemoveFormat', ';', 'Bold', 'Italic', 'Underline', 'Strike', 'Subscript', 'Superscript', 'TextColor', 'BGColor']},\n"
						+
						"\t\t\t\t{ name: 'paragraphAndAlign', items: ['BulletedList', 'NumberedList', 'Outdent', 'Indent', '-', 'ShowBlocks', ';', 'JustifyLeft', 'JustifyCenter', 'JustifyRight', 'JustifyBlock', '-', 'Table']},\n"
						+
						"\t\t\t\t{ name: 'insert', items: ['Image', '|', 'Unlink', 'SpecialChar', ';', 'Link', 'WJForms']},\n"
						+
						"\t\t\t\t{ name: 'tools', items: ['Undo', 'Redo', ';', 'Find', 'Replace']}",
				MOD_EDITOR, "Konfiguracia poloziek ckeditor toolbaru pre standalone verziu");

		setInt("metadataWaitTime", 0, "metadata,GFS",
				"Pocet milisekund cakania pred vykonanim cistenia metadat, potrebne na GFS kvoli moznemu konfliktu v subore, kedze sa modifikuje existujuci subor po uploade");

		setBoolean("enableStaticFilesExternalDir", false, MOD_CONFIG,
				"Zapnutie pouzivania externeho adresara pre /images, /files a /WEB-INF/imgcache/ adresare, cesta k externemu adresaru sa nastavuje v premennej cloudStaticFilesDir, standardne sa pre domenu vytvori v tomto adresari podadresar podla mena domeny");
		setString("fulltextDataAscMethod", "", MOD_EDITOR,
				"Pridanie custom dat do fulltext indexu vid: EditorDB.getDataAsc()");
		setInt("webpagesTreePagesMaxSize", 10, MOD_EDITOR,
				"Maximalny pocet stranok v menu, ostatne sa zbalia pod tlacidlo");

		setBoolean("enableAdminInWebLogon", false, MOD_SECURITY,
				"Povolenie nastavenie admin atributu pri prihlaseni na webe (user sa potom prihlasi ako keby do admin casti), pouzivane v intranete pre pristup k blogom a inline editacii");

		setBoolean("multigroupRedirectDefault", false, MOD_EDITOR,
				"Ak je true, tak sa pri pridani dalsieho adresara v editore stranky, vzdy zaskrtne 'Presmerovať ďalšie adresáre'");

		setBoolean("templatesUseRecursiveSystemFolder", false, mods(MOD_EDITOR, "templates"),
				"Ak je nastavene na true su hladane objekty pre sablonu vo vsetkych podadresaroch od root adresara ku aktualnemu adresaru.");
		setInt("allSystemFoldersCacheMinutes", 5, MOD_PERFORMANCE,
				"Pocet minut pre drzanie cache vsetkych systemovych adresarov vid. templatesUseRecursiveSystemFolder");

		setString("basicNtlmLogonAttrs",
				"mail,title,givenName,sn,streetAddress,l,postalCode,co,company,telephoneNumber,mobile,description,memberOf,distinguishedName,thumbnailPhoto",
				"ldap,ntlm,logon",
				"Zoznam atributov ktore sa maju citat z LDAP servera pri prihlasovani, ak je prazdne overi sa len prihlasenie a pouzivatel sa neaktualizuje");
		setString("ldapProviderUrl", "", "ldap,ntlm,logon",
				"URL adresa LDAP providera pre prihlasovanie cez LDAP - sk.iway.iwcm.system.ntlm.BasicLdapLogon.logon, hodnota v tvare ldap://ldap.local:389/DC=firma,DC=com??base");
		setString("ldapFilter", "(&(objectClass=Person) (&(sAMAccountName=!USERNAME!)))", "ldap,ntlm,logon",
				"prihlasovaci filter pre LDAP prihlasovanie s ktorym sa vykona vyhladanie konta");
		setString("ldapUsername", "", MOD_NTLM, "Prihlasovacie meno technickeho pouzivatela pre LDAP overovanie");
		setString("ldapPassword", "", MOD_NTLM, "Prihlasovacie heslo technickeho pouzivatela pre LDAP overovanie");
		setString("ldapDomainAppend", "", "ldap,ntlm,logon",
				"Ak je potrebne prihlasovanie s celou domenou tu je mozne zadat jej doplnenie k zadanemu loginu");

		setString("domainIdFixedUrls",
				"/admin/saveuser,/admin/edituser,/admin/authorize,/reguser,/changeuser,/admin/authorize,/admin/listusers,/admin/logon,/components/user/change_password.jsp,/admin/saveevent,/sk/iway/iwcm/components/welcome/WallAjax.action,/thumb/images/gallery/user/,/images/gallery/user/,/admin/ajax_users.jsp",
				MOD_CONFIG,
				"Zoznam URL adries, pre ktore sa ulozi domain_id=1, aby zaznamy boli dostupne zo vsetkych domen portalu napr. pouzivatelia, nastenka.");

		setBoolean("galleryWatermarkApplyOnUpload", false, MOD_GALLERY,
				"Sluzi na automaticke aplikovanie vodotlace pri upload obrazkov do galerie.");
		setString("galleryWatermarkApplyOnUploadDir", "/templates/{INSTALL_NAME}/assets/watermark/", MOD_GALLERY,
				"Adresar kde su umiestnene obrazky pre automaticku vodotlac pri uploade. Nazov default obrazku musi byt default.png, pri multidomain je moznost mat pre kazdu domenu iny, v tvare domena.png (napr. www.interway.sk.png)");

		setString("searchClasses",
				"sk.iway.iwcm.search.DocDetailsSearch, sk.iway.iwcm.search.AplicationSearch, sk.iway.iwcm.search.PropertiesSearch, sk.iway.iwcm.search.FileSearch",
				MOD_CONFIG, "Kategorie pre vyhladavenie vo Webjete, zobrazia sa v admin/skins/webjet6/searchall.jsp");

		setString("adminUsersAllowNormalLogon", "", MOD_NTLM,
				"Zoznam loginov s ktorymi sa viem prihlasit aj priamo do WebJETu - obchadzam LDAP autorizaciu");

		setInt("monitoringPreheatTime", 0, "server_monitoring,performance",
				"Pocet sekund potrebnych pre zahriatie web sidla (nacitanie cache) po restarte pocas ktoreho bude monitor.jsp komponenta vracat nedostupnost nodu clustra");

		setString("currencyDecoratorFormat", "0.00", MOD_CONFIG,
				"Nastavenie formatu vypisu menovej hodnotu (CurrencyDecorator) displaytagu");

		setBoolean("authenticationTrustIIS", false, MOD_NTLM,
				"Default hodnota doverovania IIS / Tomcatu pre autentifikaciu pouzivatela - doverovanie voci getUserPrincipal");

		setBoolean("webpagesTreeShowPages", true, MOD_EDITOR,
				"Sposob zobrazenia stromovej struktury. Ak je nastavene na true, zobrazia sa v stromovej strukture web stranok aj web stranky, ak false len adresare.");
		setString("galleryWatermarkApplyOnUploadExceptions", "logo,nowatermark,system,funkcionari", MOD_GALLERY,
				"Zoznam nazvov ciest pre ktore sa nebude aplikovat vodoznak pri nahrati suboru do WebJETu");
		setInt("galleryWatermarkSvgSizePercent", 5, MOD_GALLERY,
				"Vyska v percentach ktoru bude zaberat SVG vodoznak z vysky obrazku");
		setInt("galleryWatermarkSvgMinHeight", 30, MOD_GALLERY, "Minimalna vyska SVG vodoznaku");

		setBoolean("editorImageAutoTitle", true, MOD_EDITOR,
				"Automatické generovanie pomocneho titulku (title) pre obrazky vložené do stránky v editore.");

		setBoolean("enableUnsafeHttpMethods", false, "security",
				"Ak je nastavene na true, povolia sa aj HTTP metody OPTIONS,HEAD,TRACE ktore standardne nie su potrebne");
		setString("adminlogCustomLogger", "", MOD_CONFIG,
				"Custom Adminlog trieda - umoznuje pracovat s auditnym zaznamom, napr ulozit do suboru. Ocakava FQN triedy ktora implementuje interface sk.iway.iwcm.AdminlogCustomLogger");

		setString("newsAdminGroupIds", "", "news", "Zoznam ID adresárov s novinkami. ID sú oddelené čiarkami.");

		setBoolean("fulltextExecuteApps", false, MOD_SEARCH,
				"Ak je nastavene na true pokusi sa pri ulozeni stranky vykonat vlozene aplikacie a do fulltext indexu pridat aj text vlozenych aplikacii");

		setBoolean("webpagesAvailableInInternalFolders", false, MOD_SECURITY,
				"Ak je stranka v adresari oznacenom ako interny nebude dostupna bez prihlasenia do administracie. Nastavenim na true sa kontrola vypne a stranka bude verejne dostupna");
		setString("tooltipReplacerAttributes",
				"data-tooltip-class=\"tooltip\" data-toggle=\"tooltip\" data-placement=\"top\" data-html=\"true\"",
				"tooltip", "Kód ktorý sa vloží do html pri nahradzovani tooltip requestu");

		setString("robotsNoindexDomains", ".iway.sk,www2.,new.,test", "seo",
				"Zoznam domen, alebo ich cast, pre ktore bude zakazane robotmi ich indexovanie cez robots.txt");

		setString("jpaFilterByDomainIdBeanList",
				"sk.iway.iwcm.components.file_archiv.FileArchivatorBean,sk.iway.iwcm.components.insertScript.InsertScriptBean,sk.iway.iwcm.components.gdpr.CookieManagerBean,sk.iway.iwcm.components.gdpr.model.GdprRegExpBean,",
				MOD_CONFIG,
				"Ak je zapnuta premenna enableStaticFilesExternalDir tak v JPA v metode getAll bude vracat zaznamy vyfiltrovane podla domain_id. Predpokladame ze Bean v tomto zozname ma property domainId");
		setInt("passwordHistoryLength", 6, MOD_CONFIG,
				"Pocet pouzitych hesiel usera v db ktore sa ma ukladat aby user nepouzil pri zmene stare heslo");

		setString("proxyHostHttps", "", MOD_CONFIG,
				"Proxy pre pripojenie servera do internetu (ak je vyzadovane) pre httpS spojenie");
		setString("proxyPortHttps", "", MOD_CONFIG,
				"Port pre proxy pre pripojenie servera do internetu (ak je vyzadovane) pre httpS spojenie");

		setString("basketInvoiceServerName", "", MOD_BASKET,
				"Nazov domeny ktory sa ma pouzit v tele objednavky, napr. ak stahujeme obsah objednavky na inom node.");

		setInt("insertScriptCacheMinutes", 10, MOD_PERFORMANCE,
				"Pocet minut cachovania zoznamu scriptov aplikacie Skripty, predpoklad je, ze sa pouzivaju na kazdej stranke, preto sa musia cachovat");
		setString("insertScriptPositions", "", "insert_script",
				"Zoznam pozicii v JSP sablonach pre zobrazenie select boxu pri vlozeni noveho scriptu, ak je prazdne pouzije sa autocomplete na uz existujuco definovanych scriptoch");

		setBoolean("passwordHistoryEnabled", true, MOD_SECURITY,
				"Ak je nastavene na true je kontrolovana v databaze aj historia hesiel a nie je povolene pri zmene hesla pouzit take, ktore bolo v minulosti.");

		setString("propertiesEnabledKeys", "{INSTALL_NAME}.,components.{INSTALL_NAME},default.,checkform.", "properties",
				"Zoznam klucov, ktore je mozne prezerat a editovat v editacii textov");

		setString("editorPageExtension", ".html", MOD_EDITOR,
				"Typ pripony pre stranky v adresari, ktore nie su hlavne, standardne .html, moze byt zmenene na / pre vytvaranie adresarovych URL");
		setBoolean("CacheTagDisable", false, "", "Moznost vypnut tag iwcm:cache");
		setString("showOnHomepagePerexGroup", "", "news",
				"Názov perex skupiny ktorou určíme, či sa novinka zobrazí na úvodnej stránke.");

		setString("configEnabledKeys", "{INSTALL_NAME},smtp,captchaType,google,propertiesEnabledKeys,reCaptcha",
				MOD_CONFIG,
				"Zoznam zaciatkov nazvovo konfiguracnych premennych, ktore je mozne upravovat v konfiguracii so zakladnymi pravami");

		setBoolean("ldapUseSslProtocol", false, MOD_NTLM,
				"Pouzije SSL pri komunikacii s LDAP serverom. Je potrebne mat povolene SSL na porte 636 LDAP servera. Ak sa pouziva ldapS://, nechat hodnotu na false.");

		setString("searchallPropLanguages", "sk,cz,en", MOD_CONFIG,
				"Zoznam jazykov pre vyhladavanie v prekladovych klucoch v admin casti");
		setString("searchIndexReplaceStrings", "", MOD_SEARCH,
				"Zoznam vyrazov, ktore sa nahradia v indexe full text hladania. Kazdy vyraz na novom riadku vo formate stary|novy");

		setString("countryForLng", "sk|SK\ncs|CZ\nen|GB", MOD_CONFIG,
				"Nastavenie krajiny, ktora sa generuje z jazyka sablony do HTTP hlavicky Content-Language, kazdy riadok reprezentuje par jazyk|krajina");

		setBoolean("inlineEditingEnabledDefaultValue", false, MOD_EDITOR,
				"Nastavenie defultnej hodnoty zapnutia/vypnutia editovacieho toolbaru");
		setInt("searchActionMinimumWordLength", 3, MOD_SEARCH, "Minimalna dlzka slova na vyhladavanie");
		setInt("gdprDeleteUserAfterDays", 730, MOD_CONFIG,
				"Kontrola Gdpr spustena userom alebo cronom zmaze user profily starsie ako X dni");
		setInt("gdprDeleteFormDataAfterDays", 730, MOD_CONFIG,
				"Kontrola Gdpr spustena userom alebo cronom zmaze data formularov starsie ako X dni");
		setInt("gdprDeleteUserBasketOrdersAfterYears", 10, MOD_CONFIG,
				"Kontrola Gdpr spustena userom alebo cronom zmaze z eshopu objednavky starsie ako X rokov");
		setInt("gdprDeleteEmailsAfterDays", 186, MOD_CONFIG,
				"Kontrola Gdpr spustena userom alebo cronom zmaze odoslane emaily starsie ako X dni");

		setString("formMailFixedSenderEmail", "", "formmail",
				"Ak je nastavene na email adresu, pouzije sa ako fixna hodnota emailu odosielatela z formmailu. Na rozdiel od emailProtectionSenderEmail nenastavi povodny email do pola reply-to, takze odosielatel nie je ziadno norifikovany pri chybnom doruceni (co kvoli bezpecnosti moze byt niekedy potrebne)");

		setBoolean("formmailShowClassicErrorMessage", false, "formmail",
				"Ak je nastavene na true bude zobrazovat klasicku validacnu hlasku formularu hore nad formularom namiesto hlasky pri jednotlivych poliach");

		setString("combineEnabledJspsSystem",
				"/components/_common/javascript/page_functions.js.jsp,%jscript.jsp,/components/gallery/",
				mods("combine", MOD_SECURITY),
				"Zoznam JSP suborov, ktore je mozne vkladat cez combine.jsp, definovane pre system, odporucame nemenit");
		setString("combineEnabledJsps", "", mods("combine", MOD_SECURITY),
				"Zoznam JSP suborov, ktore je mozne vkladat cez combine.jsp");
		setString("combineEnabledPathsSystem",
				"/css/,/jscripts/,/templates/,/components/_common/,/components/form/,/components/gallery/,%/js/,%/scripts/,%/javascript/,%jquery,%jscript.jsp",
				mods("combine", MOD_SECURITY),
				"Zoznam JSP suborov, ktore je mozne vkladat cez combine.jsp, definovane pre system, odporucame nemenit");
		setString("combineEnabledPaths", "", mods("combine", MOD_SECURITY),
				"Zoznam JSP suborov, ktore je mozne vkladat cez combine.jsp");

		setInt("csrfMaxTokensInSession", 60, MOD_SECURITY, "Maximalny pocet CSRF tokenov drzanych v session");
		setInt("adminLogonBgTime", 9000, MOD_PERFORMANCE,
				"cas striedania obrazkov v pozadi na prihlasovacej obrazovke v admin casti, ak sa pripajate cez RDP a spomaluje vam to vykreslenie zvyste hodnotu");
		setString("userEditableTreeMenuType", "rows", mods("combine", MOD_EDITOR),
				"Zobrazenie stomu vo web strankach pokial ma pouzivatel nastavene prava na adresar. Momentalne mozne hodnoty su tree alebo rows");

		setString("prepositions", "bez,cez,do,k,medzi,na,o,od,okrem,po,pod,pre,pred,pri,proti,s,so,u,z,zo,v",
				"config;editor", "zoznam predloziek");

		setString("emailAttachmentsPublisher.pop3.host", "", "emailAttachmentsPublisher", "adresa na pop3");
		setString("emailAttachmentsPublisher.pop3.user", "", "emailAttachmentsPublisher", "pouzivatel na pop3");
		setString("emailAttachmentsPublisher.pop3.password", "", "emailAttachmentsPublisher", "heslo na pop3");

		setBoolean("elfinderFileArchiveEnabled", false, mods(MOD_FILE_ARCHIV, "fbrowser", "elfinder"),
				"Po nastaveni na true sa integruje archivu suborov do okna pri vytvarani odkazu vo web stranke alebo nastavovani externej linky");

		setString("gdprCookieClassifications", "nutne,preferencne,marketingove,statisticke,neklasifikovane", MOD_CONFIG,
				"Typy cookie ktore user moze / musi odsuhlasit");
		setString("gdprCookieName", "enableCookieCategory", MOD_CONFIG,
				"Nazov cookie, v ktorej su ulozene POVOLENE kategorie (klasifikacie) z premennej gdprCookieClassifications");
		setBoolean("gdprInsertAllScriptsBeforeAccept", false, MOD_CONFIG,
				"Vkladanie vsetkych skriptov pred odsuhlasenim cookies");

		setBoolean("formmailScrollTopAfterSend", true, "formmail",
				"Ak je nastavene na true odscroluje sa stranka po odoslani na vrch formularu (aby bolo vidno hlasku o odoslani)");
		setBoolean("formmailResetFormAfterSend", true, "formmail",
				"Ak je nastavene na true po uspesnom odoslani sa vycisti formular");

		setBoolean("fileArchivUseSubStringSearchDomain", false, MOD_FILE_ARCHIV,
				"Ak je nastavene na true, vyhladava aj substring nad stlpcom domain");
		setBoolean("fileArchivUseSubStringSearchProduct", false, MOD_FILE_ARCHIV,
				"Ak je nastavene na true, vyhladava aj substring nad stlpcom product");
		setBoolean("fileArchivUseSubStringSearchCategory", false, MOD_FILE_ARCHIV,
				"Ak je nastavene na true, vyhladava aj substring nad stlpcom category");
		setBoolean("fileArchivUseSubStringSearchProductCode", false, MOD_FILE_ARCHIV,
				"Ak je nastavene na true, vyhladava aj substring nad stlpcom product_code");

		setString("springAddPackages", "", MOD_CONFIG,
				"Zoznam packages oddelenych ciarkou pre scanovanie Spring beanov pri inicializacii WebJETu");

		setBoolean("fileArchivUseCategoryAsLink", false, MOD_FILE_ARCHIV,
				"Ak je nastavene na true,použije kategoriu ako cestu k suboru. Napr: 'defaultna cesta k archivu'/kategoria/subor.pdf");
		setInt("nginxProxyModePageCacheTime", 0, MOD_PERFORMANCE,
				"Cas v sekundach cachovania vygenerovanej web stranky v rezime nginxProxy (nastavena premenna nginxProxyMode na true)");

		setBoolean("smtpUseTLS", false, "config;dmail;security",
				"TLS autentifikacia - ak je port 587, smtpUseTLS musi byt true");

		setString("smtpTLSVersion", "TLSv1.2", "config;dmail;security",
				"TLS autentifikacia - verzia TLS pre smtp spojenie, vyzaduje nastavit aj smtpUseTLS na true");

		setBoolean("gridEditorEnabled", false, MOD_CONFIG, "Povolenie grid editora - editora layoutu(gridu) v editore.");
		setString("bootstrapVersion", "4", MOD_CONFIG, "Verzia bootstrap v danom projekte.");

		setBoolean("monitorMaintenanceMode", false, MOD_CONFIG,
				"Ak je nastavena na true tak bude komponenta /components/server_monitoring/monitor.jsp vracat chybu 'UNAVAILABLE' ");

		setString("xsrfUrlExceptionSystem",
				"%.gif!,%.jpg!,%.png!,%.css!,%.woff2!,%.js!,%.map!,/admin/mem.jsp!,/admin/!,/admin/index.jsp!,/admin/logon.jsp!,/admin/logon.do!,/sync/getobject,%approve,%Approve,/localconf.jsp,/admin/elFinder/elfinder-dialog.jsp!,/admin/tail.jsp,/admin/skins/webjet8/ckeditor/plugins/webjetcomponents/component_preview.jsp,/admin/skins/webjet8/ckeditor/dist/plugins/webjetcomponents/component_preview.jsp,/components/qa/admin_answer.jsp,/components/user/change_password.jsp",
				MOD_SECURITY,
				"Zoznam systemovych povolenych URL pre ktore sa nekontroluje referer (ODPORUCAME NEMENIT, pre nastavenie klientskej hodnoty pre danu instalaciu pouzite xsrfUrlException)");

		setString("xsrfUrlException", "", MOD_SECURITY, "Zoznam povolenych URL pre ktore sa nekontroluje referer");
		setString("xsrfParamNameExceptionSystem",
				"docid,historyid,_logLevel,combineEnabled,combineEnabledRequest,groupid,forward,forceBrowserDetector,_writePerfStat,_disableCache,printTrace,isPopup,isDmail,NO_WJTOOLBAR,userlngr,page,words,datumOd,datumDo,btnSubmit,search,language,__lng,lng,userid,userId,webjetDmsp,formId,hash,invoiceId,auth,loginName,t,f,v,forum,NO_WJTOOLBAR,isPdfVersion,fbclid,utm_source,utm_medium,utm_campaign,utm_term,utm_content,formName,showTextKeys,extURL,id,removePerm,showBanner",
				MOD_SECURITY,
				"Zoznam systemovych povolenych parametrov, ktore sa preskakuju pri GET poziadavke na kontrolu existencie parametra (bez ohladu na velkost pismen)");
		setString("xsrfParamNameException", "", MOD_SECURITY,
				"Zoznam povolenych parametrov, ktore sa preskakuju pri GET poziadavke na kontrolu existencie parametra (bez ohladu na velkost pismen)");

		setString("fbrowserDefaultWritableFolders", "", MOD_SECURITY,
				"Zoznam adresarov (kazdy na novom riadku) na ktore ziska pouzivatel prava ak nema ziadne definovane a konf. premenna fbrowserShowOnlyWritableFolders je nastavena na true. Vhodne na multiweb, kde viete takto nastavit defaultne prava (vie vykonat aj makro {DOMAIN_ALIAS}.");

		setString("forceDownloadSuffixes", "", MOD_CONFIG,
				"Zoznam pripon oddelenych ciarkamy (napr. .doc,.xls,.pdf) pre ktore sa bude generovat HTTP hlavicka Content-Disposition: attachment pre vyvolanie stiahnutia suboru (namiesto jeho zobrazenia v prehliadaci)");

		setBoolean("elfinderCreateFolderForPages", true, MOD_EDITOR,
				"Vytvara podadresare suborov pre Media tejto stranky pre podstranky v adresari (cize ak ma adresar podstranky tak kazda podstranka ma vlastny adresar pre Media tejto stranky)");

		setString("ckeditor_removeButtons", "", MOD_EDITOR,
				"Zoznam tlacitok, ktore chcete v editore schovat (nezobrazit), nie je potrebne rekonfigurovat toolbar, staci sem nastavit ciarkou oddeleny zoznam. Aplikuje sa aj na plavajuci toolbar");
		setString("ckeditor_floatingToolsGroups", "{ name: 'styles',    items: [ 'Font','FontSize' ]},\n" +
				"\t\t\t\t\t{ name: 'format',    items: [ 'Bold','Italic', 'Link', 'RemoveFormat' ]},\n" +
				"\t\t\t\t\t{ name: 'paragraph', items: [ 'JustifyLeft', 'JustifyCenter', 'JustifyRight']},\n" +
				"\t\t\t\t\t{ name: 'image', \t items: [ 'FloatImage', 'FloatImageEdit', 'JustifyLeft', 'JustifyCenter', 'JustifyRight', 'FloatImageDelete' ]},\n"
				+
				"\t\t\t\t\t{ name: 'table', \t items: [ 'FloatTable', 'FloatTableDelete' ]},\n" +
				"\t\t\t\t\t{ name: 'form', \t items: [ 'FloatForm', 'FloatFormDelete' ]},\n" +
				"\t\t\t\t\t{ name: 'div', \t\t items: [ 'FloatDiv', 'FloatDivCopy', 'FloatDivDelete','FloatDivDeleteContent' ]}",
				MOD_EDITOR, "Zoznam poloziek pre floatingtoolbar");

		setBoolean("isGoogleAuthRequiredForAdmin", false, MOD_CONFIG,
				"Ak je nastavene na true tak je pri prihlaseni uzivatel nuteny vytvorit si google auth. token a prihlasovat sa vzdy aj s kodom ");

		setInt("regUserDefaultUserId", -1, MOD_USER,
				"Pri registrovani noveho pouzivatela pomocou RegUserAction, pridana moznost nastavit prava na admin moduly podla pouzivatela (user_id) zadaneho v tejto konfiguracnej premennej");

		setBoolean("springEnableShowdoc", true, MOD_PERFORMANCE,
				"Nastavuje, ci sa ma pouzivat Spring pre komponenty, pre vysoko zatazene servre pri nepouzivani Spring komponent je mozne vypnut nastavenim na false");
		setString("mapProvider", "OpenStreetMap", MOD_CONFIG,
				"Nastavenie typu mapy pre komponentu map.jsp, moznosti su GoogleMap alebo OpenStreetMap");

		setString("fileIndexerNoDataFileExtension", "", "config;files",
				"Pripony suborov pre ktore nechceme ziskavat data napr. .zip,.rar, ale chceme ich indexovat kvoli statistike.");

		setBoolean("mssqlUseOldTopQuery", false, MOD_OBSOLETE,
				"Zapnutie starej verzie TOP query pre MSSQL, je potrebne pre MSSQL < 2012, pouziva sa pri news query");

		setString("userAfterSaveMethod", "", MOD_CONFIG,
				"Meno metody, ktora sa zavola po ulozeni pouzivatela. Napr. sk.iway.package.UserFormTools.afterSaveMethod, metoda ma predpis: public static void afterSaveMethod(HttpServletRequest request, UserDetails oldUser, UserDetails user)");

		setString("ampObjects", "doc_data", MOD_CONFIG,
				"Zoznam objektov v requeste pre ktore sa aplikuje uprava HTML kodu pre AMP verziu");

		setString("ldapSecurityPrincipalDn", "", MOD_NTLM,
				"Nastavi pre LDAP custom SECURITY_PRINCIPAL napr. cn=!USERNAME!,dc=ad,dc=interway,dc=sk s tym, ze !USERNAME! zameni za login. Pokial je prazdne pouzije sa ldapUsername+ldapDomainAppend");

		setBoolean("gdprAllowAllCookies", false, MOD_SECURITY,
				"Nastavenim na true sa povoli odosielanie vsetkych cookies bez ohladu na GDPR cookies policy. Je potrebne nastavit na true, ak sa pouziva externy GDPR modul na spravu cookies (alebo sa jedna napr. o intranet a podobne)");
		setString("allowUploadToDirUserGroupId", "", MOD_CONFIG,
				"Povolenie uploadu pouzivatelskej skupine pre adresar, napr. /files/project/:25, kde /files/project/ je adresar a 25 je id pouzivatelskej skupiny. Pre vsetky podadresare staci nastavit url s *, napr. /files/project/*:25, je mozne zadat aj viacero definicii oddelenych ciarkou, napr. /files/project/*:25,/files/project/*:26");

		setString("xssHtmlAllowedFieldsSystem",
				"value,prop_value,message_text,text,description,user_note,title,value_string,question,message,string1,string2,string3,string4,string5,string6,reg_exp,gallery_perex,watermark,regexp_value,body,room_description,question_text,answer_text_ok,answer_text_fail,script_body,junk_reason,media_info_sk,password,salt,crop_start,crop_end,auth_username,auth_password,question,answer,hash,kategoria,definicia,poznamka1,poznamka2,zdroj1,zdroj2,priklad,tip_text,svalue1,svalue2,svalue3,svalue4,signature,authorize_hash,password_salt,navbar,external_link,group_name,url",
				MOD_SECURITY,
				"Zoznam stlpcov v databaze, ktore mozu obsahovat HTML kod (nebudu pri citani escapovane specialne znaky). Pre zakaznicke projekty nastavte premennu xssHtmlAllowedFields");
		setString("xssHtmlAllowedFields", "", MOD_SECURITY,
				"Zoznam stlpcov v databaze, ktore mozu obsahovat HTML kod (nebudu pri citani escapovane specialne znaky)");

		setString("formmailSendUserInfoSenderName", "", "formmail",
				"Posle sa ako meno odosielatela v e-maile pouzivatelovi pre potvrdenie suhlasu so spracovanim osobnych udajov. Ak je prazdne, posle sa meno autora stranky, ktorej obsah sa posiela do e-mailu.");
		setString("formmailSendUserInfoSenderEmail", "", "formmail",
				"Posle sa ako e-mail odosielatela v e-maile pouzivatelovi pre potvrdenie suhlasu so spracovanim osobnych udajov. Ak je prazdne, posle sa e-mail autora stranky, ktorej obsah sa posiela do e-mailu.");
		setBoolean("zmluvyEnableVo", false, "zmluvy",
				"Ak je nastavena na true, tak sa budu zobrazovat aj skupiny pre verejne obstaravanie.");
		setInt("socialMediaUserGroupId", -1, MOD_SECURITY,
				"ID skupiny pre prihlasovanie cez socialne siete, pre pouzivatelov v tejto skupine nebude pozadovane zadanie existujuceho hesla pri zmene profilu");

		setBoolean("editorDisableAutomaticRedirect", false, MOD_EDITOR,
				"Ak je nastavena na true, tak sa nebudu automaticky vytvarat presmerovania po zmene URL stranky");

		setString("logonPreservedSessionObjects", "adminAfterLogonRedirect,BasketDB.browserIdSession,afterLogonRedirect,last_doc_id,iwcm_useriwcm_changepassword", MOD_SECURITY,
				"Zoznam session objektov, ktore sa zachovaju po prihlaseni pouzivatela");

		setBoolean("enableJspJarPackaging", false, MOD_CONFIG,
				"Povolenie hladania statickych a JSP suborov zabalenych v JAR archivoch");

		setString("logLevels", "sk.iway.iwcm.io=INFO", MOD_CONFIG,
				"Zoznam packages s urovnou logovania (kazdy na novom riadku), napr. sk.iway=DEBUG sk.iway.iwcm=WARN org.springframework=WARN");
		setString("logWebjetPackages", "sk.iway,cz.webactive", MOD_CONFIG,
				"Zoznam packages, ktorych log level sa prepina cez konf. premennu logLevel alebo URL parameter _logLevel");

		setBoolean("authorizeRegeneratePassword", true, MOD_SECURITY,
				"Predvolene sa pri autorizacii pouzivatela generuje nove heslo, nastavenim na false sa nove heslo nebude generovat ale zarovne v autorizacnom maile nemoze byt posielane heslo, kedze WebJET ho nevie zistit.");

		setString("XMLDecoderAllowedClasses",
				"java.beans.XMLDecoder, sk.iway.iwcm.sync.export.Content, java.util.ArrayList, java.util.List, java.lang.String, sk.iway.iwcm.doc.DocDetails, sk.iway.iwcm.doc.GroupDetails, sk.iway.spirit.model.Media, java.util.Vector, java.util.Date, org.eclipse.persistence.indirection.IndirectList, sk.iway.spirit.model.MediaGroupBean, sk.iway.iwcm.editor.DocNoteBean, sk.iway.iwcm.doc.TemplateDetails, sk.iway.iwcm.users.UserGroupDetails, sk.iway.iwcm.system.UpdateDBBean, sk.iway.iwcm.system.ConfDetails, sk.iway.iwcm.update.VersionBean, java.util.Hashtable, java.util.Map, sk.iway.iwcm.doc.PerexGroupBean, sk.iway.iwcm.components.monitoring.jpa.ExecutionEntry, sk.iway.iwcm.components.banner.model.BannerBean, java.lang.Integer, sk.iway.iwcm.gallery.GalleryBean, java.sql.Timestamp, java.util.HashMap, sk.iway.iwcm.inquiry.InquiryBean, sk.iway.iwcm.gallery.GalleryDimension, java.awt.Dimension, sk.iway.iwcm.inquiry.AnswerForm, sk.iway.iwcm.doc.TemplateDetailEditorFields, sk.iway.iwcm.doc.DocEditorFields, sk.iway.iwcm.doc.GroupEditorField, sk.iway.iwcm.doc.TemplateDetailEditorFields, sk.iway.iwcm.components.file_archiv.FileArchivatorBean",
				MOD_CONFIG, "Povolene triedy pre XMLDecoder");

		setBoolean("swaggerEnabled", false, MOD_SECURITY, "Povolenie zobrazovania swagger API dokumentacie na URL adrese /admin/swagger-ui/index.html");
		setBoolean("swaggerRequireAdmin", true, MOD_SECURITY,
				"Ak je nastavene na true je vyzadovane admin konto pre zobrazenie swagger dokumentacie");

		setInt("bootstrapColumns", 12, MOD_EDITOR, "Pocet stlpcov bootstrap gridu (pre PageBuilder).");
		setString("pageBuilderPrefix", "pb", MOD_EDITOR,
				"Prefix pre PageBuilder, musi voci nemu korespondovat aj CSS PageBuildera (napr. pre CSS PREFIX-column-text-wrapper, predvolene pb-column-text-wrapper)");
		setString("pageBuilderGrid", "", MOD_EDITOR,
				"Zoznam grid elementov/selectorov pre pageBuilder, pouziva sa ak je potrebne mat specialne selectory, format je section: 'section', container: 'div.container', row: 'div.row', column: 'div[class*=\"col-\"]', column_content: 'div.column-content'");

		setString("analyticsTrackerConf", "", MOD_CONFIG,
				"Slúži na konfiguráciu trackovania analytics eventov z backendu. Uvádajú sa dvojice vzorUrl:trieda oddelené ;. Napr: '/files/filearchiv/:sk.iway.iwcm.FileArchiveAnalytics;/images/trackovane/:sk.iway.iwcm.TrackujObrazok'");
		setString("analyticsTrackingId", "", MOD_CONFIG, "TrackingId pre GA pripadne inu analytics sluzby");
		setString("analyticsTrackingVersion", "", MOD_CONFIG, "Verzia, napr 'GA1.2'");

		setString("featurePolicyHeader", "", MOD_SECURITY,
				"Hodnota HTTP hlavicky Feature-Policy (napr.: microphone 'none'; geolocation 'none'), viac na: https://developer.mozilla.org/en-US/docs/Web/HTTP/Feature_Policy");

		setString("cryptoAlg", "CryptoTink", MOD_SECURITY,
				"Nastavenie predvoleneho sifrovacieho algoritmu, mozne hodnoty CryptoTink a CryptoRsa2048. Tink je Google kniznica, ktora pouziva asymetricke sifrovanie, Rsa2048 je mozne pouzit len na data kratsie ako 2048b.");

		setBoolean("basketUseFirstUserGroupPrice", false, MOD_BASKET,
				"Po nastaveni na true sa pri parsovani cien podla user skupin (napr. 779.0;4:708.0;5:590.0) pouzije prva najdena cena a nie najnizsia cena.");

		setBoolean("syncGroupAndWebpageTitle", true, MOD_EDITOR,
				"Aktivuje synchronizaciu mena adresara a hlavnej stranky adresara");

		setString("responseHeaders", "/admin:X-Accel-Buffering:no", MOD_CONFIG, "nastavi HTTP hlavicky pre url");
		setString("auditHideProperties", "password,password2,password_salt,html_data", MOD_SECURITY,
				"Zoznam nazvov vlastnosti objektu, ktorych hodnota sa nebude auditovat, napr. password, do auditu sa pre taketo property vypisu len *****");
		setInt("auditMaxChangeLength", 100, MOD_SECURITY,
				"Maximalny pocet znakov auditu pri porovnani zmien v databazovych entitach");
		setString("auditJpaDisabledEntities",
				"sk.iway.iwcm.components.domainRedirects.DomainRedirectBean,sk.iway.iwcm.system.UrlRedirectBean",
				MOD_SECURITY,
				"Zoznam JPA entit (meno triedy vratane package) pre ktore sa vypne auditovanie zmien. Znakom * sa uplne vypne auditing.");

		setString("gdprCookieClassificationsDefault", "nutne,preferencne,marketingove,statisticke,neklasifikovane",
				MOD_SECURITY,
				"Zoznam cookies kategorii ktore maju byt defaultne oznacene, cize ked uzivatel akceptuje cookies bez toho ze by si zobrazil detail, setnu sa kategorie z tejto konf premennej. Zoznam cookies sa do premennej oddeluje ciarkou, napr nutne,statisticke,marketingove");

		setBoolean("editor.unpublishBeforeSaveAs", false, MOD_EDITOR,
				"Ak je true, tak pri ulozit ako sa zduplikovana stranka automaticky nastavi ako nepublikovana.");
		setString("sk.iway.iwcm.qa.AddAction.sendAdminMail.url", "", MOD_CONFIG, "url na qa/admin_answer.jsp komponentu");

		setString("ninjaNbspReplaceRegex", "(\\s.{1,2})\\s\n$1&nbsp;", MOD_EDITOR,
				"Regex vyraz a replacement pre nahradenie medzery za nbsp entitu po spojke.");
		setString("httpsDnsNameMatchingExceptions", "", MOD_SECURITY,
				"Zoznam domen pre ktore je povolena vynimka verifikacie DNS nazvov s SSL certifikatom pri stahovani cez Tools.downloadUrl. Moznost zadat len suffix.");
		setString("logoffRedirectUrl", "/", MOD_CONFIG, "Umozni presmerovanie na inu stranku po odhlaseni z WJ");

		setBoolean("sendMailSaveEmail", false, "dmail;config", "Umozni ulozenie emailov na file system");
		setString("sendMailSaveEmailPath", "/WEB-INF/tmp/emails", MOD_CONFIG, "Adresar pre ulozenie emailov");

		setInt("domainIdCommon", 0, "multidomain;config",
				"Nastavuje ID spolocnej domeny, ktora obsahuje spolocne objekty medzi domenami (napr. bannery, file archiv a podobne). Je potrebne len pre specificke ucely, bezne nie je potrebne hodnotu nastavit. Technicky pridava OR podmienky do SQl prikazov WHERE (domain_id=XXX OR domain_id=${domainIdCommon})");

		setString("ldapNewAdminTemplatePermsUserLogin", "", MOD_NTLM,
				"Moznost povolit automaticke vytvorenie admin pouzivatela pri LDAP prihlasovani. Pre zapnutie je potrebne do hodnoty nastavit login pouzivatela, z ktoreho sa budu synchronizovat prava na moduly pre noveho admin pouzivatela.");

		setString("weatherSourceApi", "https://api.met.no/weatherapi/locationforecast/2.0/classic.xml", MOD_CONFIG,
				"URL API pre stiahnutie pocasia pre intranet");

		setString("structureMirroringConfig", "", "structuremirroring",
				"Nastavenie zrkadlenia jazykovych verzii stranok. Na kazdom riadku je zoznam ID adresarov oddelenych ciarkou, ktore sa maju zrkadlit, riadok moze mat za znakom : poznamku. Priklad: 773,774,775:docmirroring.webjet.sk");

		setString("mariaDbDefaultEngine", "MyISAM", MOD_CONFIG,
				"Predvoleny typ engine pre MariaDB SQL server, moze byt nastavene na MyISAM alebo InnoDB. Pre pouzitie utf8mb4 je potrebny engine InnoDB.");

		setBoolean("fileArchivIndexOnlyMainFiles", true, MOD_CONFIG,
				"Ak je true, budu sa pre archiv suborov indexovat len hlavne subory, teda bez verzii.");

		setString("basketQuantityField", "fieldM", MOD_BASKET,
				"Názov poľa v ktorom je v stránke uvedený počet kusov produktu. Štandardne nastavené na hodnotu fieldM");

		setString("thumbServletMissingImg", "", "thumb,gallery",
				"Obrazok, ktory sa zobrazi pri volani /thumb ak zadane URL neexistuje. Na kazdom riadku je vo formate /images/nejaka/cesta|/images/cesta/obrazok.jpg. Hlada sa najlepsia zhoda (najdlhsi vyraz).");

		setString("propAllowedTags", "p,div,a,sub,sup,br,strong", MOD_SECURITY,
				"Zoznam HTML tagov, ktore su povolene pri editacii prekladovych textov bez opravnenia zobrazenie vsetkych textov");
		setString("propAllowedAttrs", "href,src,style,class,rel", MOD_SECURITY,
				"Zoznam atributov HTML tagov, ktore su povolene pri editacii prekladovych textov bez opravnenia zobrazenie vsetkych textov");

		setString("basketQuantityTypeField", "", MOD_BASKET,
				"Názov poľa v ktorom je v stránke uvedený typ množstva kusov produktu.");
		setInt("basketMaxQty", 1000, MOD_BASKET, "Maximálna hodnota množstva pre jeden kus produktu");

		setString("accessControlAllowOriginValue", "{HTTP_PROTOCOL}://{SERVER_NAME}:{HTTP_PORT}", MOD_SECURITY,
				"Hodnota hlavicky Access-Control-Allow-Origin pre URL nastavene v premennej accessControlAllowOriginUrls. Mozne pouzit makro {HTTP_PROTOCOL}, {SERVER_NAME}/{DOMAIN_NAME}/{DOMAIN_ALIAS}, {HTTP_PORT}");
		setString("accessControlAllowOriginUrls", "/rest/,/private/rest/,/admin/rest/", MOD_SECURITY,
				"Zoznam URL oddelenych ciarkou pre nastavenie hlavicky Access-Control-Allow-Origin");
		setString("accessControlAllowHeaders", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers, x-csrf-token", MOD_SECURITY,
				"Hodnota pre nastavenie hlavicky Access-Control-Allow-Headers, nastavuje sa len pri generovani hlavicky Access-Control-Allow-Origin");
		setString("accessControlAllowMethods", "HEAD,POST,GET,OPTIONS,PUT", MOD_SECURITY,
				"Hodnota pre nastavenie hlavicky Access-Control-Allow-Methods, nastavuje sa len pri generovani hlavicky Access-Control-Allow-Origin");
		setString("accessControlMaxAge", "1800", MOD_SECURITY,
				"Hodnota pre nastavenie hlavicky Access-Control-Max-Age, nastavuje sa len pri generovani hlavicky Access-Control-Allow-Origin");
		setString("accessControlAllowedOrigins", "", MOD_SECURITY,
				"Ak nie je prazdne, vyzaduje pri requeste hlavicku origin, ktorej hodnota sa musi nachadzat v tomto zozname (ciarkou oddeleny zoznam bez medzier). Nastavuje sa len pri generovani hlavicky Access-Control-Allow-Origin");

		setString("bannerCampaignParamName", "utm_campaign", "banner",
					"Nazov parametra pri kampanovom banneri na zaklade jeho hodnoty viem zobrazit prislusny banner. Pri zmene vychodzej hodnoty je potrebne pridat vynimku do xsrfParamNameException.");
		setString("bannerDefaultImageUrl", "", "banner",
					"URL pre obrazok bannera, ktory sa zobrazi ak nenajde ziaden iny banner na zaklade include parametrov.");

		setString("reCaptchaSiteKey", "", MOD_CAPTCHA, "Site Key pre Google re-captcha");
		setString("reCaptchaSecret", "", MOD_CAPTCHA, "Secret pre Google re-captcha");
		setString("recaptchaMinScore", "0.5", MOD_CAPTCHA, "Minimalne score pre akceptovanie recaptcha v3");

		setString("logonTokenHeaderName", "x-auth-token", MOD_SECURITY, "Meno HTTP hlavicky v ktorej sa nachadza prihlasovaci token pre API pristup");

		setInt("GroupsDB.parentGroupsCacheMinutes", 60, MOD_PERFORMANCE, "Pocet minut cachovania zoznamu parent priecinkov.");
	}

	/**
	 * Description of the Field
	 */
	public static final String A_DOC_DB = "iwcm_doc_db";

	/**
	 * 1
	 * Description of the Field
	 */
	public static final String A_USER_GROUPS_DB = "iwcm_user_groups_db";

	/**
	 * Description of the Field
	 */
	public static final String A_TEMP_DB = "iwcm_temp_db";
	/**
	 * Description of the Field
	 */
	public static final String A_GROUPS_DB = "iwcm_groups_db";

	/**
	 * Description of the Field
	 */

	public static final int DB_MSSQL = 1;

	/**
	 * Description of the Field
	 */
	public static final int DB_MYSQL = 3;

	/**
	 * System bezi na ORACLE databaze
	 */
	public static final int DB_ORACLE = 4;

	/**
	 * PostgreSQL DB engine
	 */
	public static final int DB_PGSQL = 5;

	/**
	 * default hodnota
	 */
	public static int DB_TYPE = DB_MYSQL; //NOSONAR

	/**
	 * Description of the Field
	 */
	// public static String MAIL_RECODE = "windows-1250";

	/**
	 * Description of the Field
	 */
	public static final String USER_KEY = "iwcm_useriwcm";

	public static final String USER_KEY_IMPERSONATE = "iwcm_useriwcm_impersonate";

	/**
	 * Description of the Field
	 */
	public static final String SESSION_GROUP_ID = "iwcm_group_id";

	/**
	 * Description of the Field
	 */
	public static final String BENZIN_DB = "benzin_db";
	/**
	 * Description of the Field
	 */
	public static final String OBCE_DB = "obce_db";

	/**
	 * Description of the Field
	 */
	public static final String STAT_START_DATE = "stat_start_date";
	/**
	 * Description of the Field
	 */
	public static final String STAT_END_DATE = "stat_end_date";
	/**
	 * Description of the Field
	 */
	public static final String STAT_MAX_ROWS = "stat_max_rows";

	/**
	 * defaultny encoding suborov
	 */
	public static final String FILE_ENCODING = "windows-1250";

	public static final int LINK_TYPE_DOCID = 1;
	public static final int LINK_TYPE_HTML = 2;

	/**
	 * Gets the int attribute of the Constants class
	 *
	 * @param constName Description of the Parameter
	 * @return The int value
	 */
	public static int getInt(String constName) {
		constName = getConstantNameDomainAlias(constName);
		// skus ziskat hodnotu z hashtable
		try {
			if (constName != null && constName.length() > 1) {
				Object obj = constantsMap.get(constName);
				if (obj != null) {
					if (obj instanceof Integer) {
						return (((Integer) obj).intValue());
					} else if (obj instanceof String) {
						return (Integer.parseInt(((String) obj)));
					} else if (obj instanceof Boolean) {
						Boolean val = (Boolean) obj;
						if (val.booleanValue() == true) {
							return (1);
						}
						return (0);
					}
				}
				Logger.debug(Constants.class, "Constants.getInt NOT FOUND (" + constName + ") = -1");
			}
		} catch (Exception ex) {
			Logger.println(Constants.class, "Constants.getInt FAILED (" + constName + ") = -1");
			sk.iway.iwcm.Logger.error(ex);
		}
		return (-1);
	}

	/**
	 * Sets the int attribute of the Constants class
	 *
	 * @param constName The new int value
	 * @param VALUE     The new int value
	 */
	public static void setInt(String constName, int VALUE) {
		// Logger.println(this,"Constants: setInt(" + dbName + ", " + constName + ", " +
		// VALUE + ")");
		try {
			if (constName != null && constName.length() > 1) {
				constantsMap.put(constName, Integer.valueOf(VALUE));
			}
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
			Logger.println(Constants.class, "    -> set failed (const=" + constName + ")");
		}
	}

	public static int getInt(String constName, int defaultValue) {
		return getInt(constName) == -1 ? defaultValue : getInt(constName);
	}

	/**
	 * Gets the int attribute of the Constants class
	 *
	 * @param constName Description of the Parameter
	 * @return The int value
	 */
	public static long getLong(String constName) {
		constName = getConstantNameDomainAlias(constName);
		// skus ziskat hodnotu z hashtable
		try {
			if (constName != null && constName.length() > 1) {
				Object obj = constantsMap.get(constName);
				if (obj != null) {
					if (obj instanceof Long) {
						return (((Long) obj).longValue());
					} else if (obj instanceof String) {
						return (Long.parseLong(((String) obj)));
					} else if (obj instanceof Boolean) {
						Boolean val = (Boolean) obj;
						if (val.booleanValue() == true) {
							return (1);
						}
						return (0);
					}
				}
				Logger.debug(Constants.class, "Constants.getLong NOT FOUND (" + constName + ") = -1");
			}
		} catch (Exception ex) {
			Logger.println(Constants.class, "Constants.getLong FAILED (" + constName + ") = -1");
			sk.iway.iwcm.Logger.error(ex);
		}
		return (-1);
	}

	public static long getLong(String constName, long defaultValue) {
		return getLong(constName) == -1 ? defaultValue : getLong(constName);
	}

	/**
	 * Sets the long attribute of the Constants class
	 *
	 * @param constName The new int value
	 * @param VALUE     The new int value
	 */
	public static void setLong(String constName, long VALUE) {
		// Logger.println(this,"Constants: setInt(" + dbName + ", " + constName + ", " +
		// VALUE + ")");
		try {
			if (constName != null && constName.length() > 1) {
				constantsMap.put(constName, Long.valueOf(VALUE));
			}
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
			Logger.println(Constants.class, "    -> set failed (const=" + constName + ")");
		}
	}

	/**
	 * Gets the string attribute of the Constants class
	 *
	 * @param constName Description of the Parameter
	 * @return The string value, in case of any error or when
	 *         value is not defined returns empty string
	 */
	public static String getString(String constName) {
		if ("wjVersion".equals(constName) == false) {
			constName = getConstantNameDomainAlias(constName);
		}
		// skus ziskat hodnotu z hashtable
		try {
			if (constName != null && constName.length() > 1) {
				Object obj = constantsMap.get(constName);
				if (obj != null) {
					if (obj instanceof String) {
						// Logger.println(this,"Constants.getString(" + constName + ") = " + ((String)
						// obj));
						return ((String) obj);
					} else if (obj instanceof Integer) {
						return ((Integer) obj).toString();
					} else if (obj instanceof Long) {
						return ((Long) obj).toString();
					} else if (obj instanceof Boolean) {
						return ((Boolean) obj).toString();
					}
				}
				// Logger.println(Constants.class,"Constants.getString NOT FOUND (" + constName
				// + ")");
			}
			// Logger.println(this,"Constants.getString(" + dbName + ", " + constName + ") =
			// DEFAULT_VALUE (" + DEFAULT_VALUE + ")");
			return ("");
		} catch (Exception ex) {
			Logger.println(Constants.class, "Constants.getString FAILED (" + constName + ")");
			sk.iway.iwcm.Logger.error(ex);
		}
		return ("");
	}

	/**
	 * Ziska hodnotu konstanty s vykonanim makra (napr. nahrada {INSTALL_NAME} za
	 * realnu hodnotu)
	 *
	 * @param constName
	 * @return
	 */
	public static String getStringExecuteMacro(String constName) {
		String value = getString(constName);
		value = executeMacro(value);
		return value;
	}

	/**
	 * V hodnote vykona makra (napr. nahrada {INSTALL_NAME} za
	 * realnu hodnotu). Je mozne pouzit:
	 * - {INSTALL_NAME}
	 * - {DOMAIN_NAME}
	 * - {DOMAIN_ALIAS}
	 * - {HTTP_PROTOCOL}
	 * - {HTTP_PORT}
	 * - {SERVER_NAME}
	 * - {ROOT_PATH}
	 */
	public static String executeMacro(String value) {
		if (Tools.isEmpty(value)) return value;

		value = Tools.replace(value, "{INSTALL_NAME}", getInstallName());
		value = Tools.replace(value, "{ROOT_PATH}", Tools.getRealPath("/"));

		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb != null) {
			String domainName = rb.getDomain();
			value = Tools.replace(value, "{DOMAIN_NAME}", domainName);
			String domainAlias = MultiDomainFilter.getDomainAlias(domainName);
			value = Tools.replace(value, "{DOMAIN_ALIAS}", domainAlias);

			value = Tools.replace(value, "{HTTP_PROTOCOL}", rb.getHttpProtocol());
			value = Tools.replace(value, "{HTTP_PORT}", String.valueOf(rb.getHttpPort()));
			value = Tools.replace(value, "{SERVER_NAME}", String.valueOf(rb.getServerName()));

			//hlavicky
			value = Tools.replace(value, "{HEADER_ORIGIN}", rb.getHeaderOrigin());
		}
		return value;
	}

	public static String getString(String constName, String defaultValue) {
		String value = getString(constName);
		if (Tools.isEmpty(value))
			return defaultValue;
		return value;
	}

	/**
	 * Returns coma separated value as Array
	 * @param name - configuration name
	 * @return
	 */
	public static String[] getArray(String name) {
		String[] arr = Tools.getTokens(Constants.getString(name), ",\n");
		return arr;
	}

	/**
	 * Sets the string attribute of the Constants class
	 *
	 * @param constName The new string value
	 * @param VALUE     The new string value
	 */
	public static void setString(String constName, String VALUE) {
		// Logger.println(this,"Constants: setString(" + dbName + ", " + constName + ",
		// " + VALUE + ")");
		try {
			if (constName != null && constName.length() > 1) {
				constantsMap.put(constName, VALUE);
			}
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
			Logger.println(Constants.class, "    -> set failed (const=" + constName + ", value="+VALUE+")");
		}
	}

	/**
	 * Gets the string attribute of the Constants class
	 *
	 * @param constName Description of the Parameter
	 * @return The string value
	 */
	public static boolean getBoolean(String constName) {
		constName = getConstantNameDomainAlias(constName);
		// skus ziskat hodnotu z hashtable
		try {
			if (constName != null && constName.length() > 1) {
				Object obj = constantsMap.get(constName);
				if (obj != null) {
					if (obj instanceof Boolean) {
						// Logger.println(this,"Constants.getBoolean(" + dbName + ", " + constName + ")
						// = " + ((Boolean) obj).booleanValue());
						return (((Boolean) obj).booleanValue());
					} else if (obj instanceof String) {
						return "true".equals(obj) || "yes".equals(obj);
					} else {
						Logger.println(Constants.class, constName + " HODNOTA NIE JE BOOLEAN");
					}
				}
				if ("loggingInMemoryEnabled".equals(constName)==false) Logger.debug(Constants.class, "Constants.getBoolean NOT FOUND (" + constName + ") = DEFAULT_VALUE (false)");
			}
			// Logger.println(this,"Constants.getBoolean(" + dbName + ", " + constName + ")
			// = DEFAULT_VALUE (" + DEFAULT_VALUE + ")");
			return (false);
		} catch (Exception ex) {
			Logger.println(Constants.class, "Constants.getBoolean FAILED (" + constName + ") = DEFAULT_VALUE (false)");
			sk.iway.iwcm.Logger.error(ex);
		}
		return (false);
	}

	/**
	 * Sets the string attribute of the Constants class
	 *
	 * @param constName The new string value
	 * @param VALUE     The new string value
	 */
	public static void setBoolean(String constName, boolean VALUE) {
		// Logger.println(this,"Constants: setBoolean(" + dbName + ", " + constName + ",
		// " + VALUE + ")");
		try {
			if (constName != null && constName.length() > 1) {
				constantsMap.put(constName, Boolean.valueOf(VALUE));
			}
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
			Logger.println(Constants.class, "    -> set failed (const=" + constName + ")");
		}
	}

	public static void setBoolean(String constName, String VALUE) {
		boolean val = false;
		if ("yes".equals(VALUE) || "true".equals(VALUE)) {
			val = true;
		}
		setBoolean(constName, val);
	}

	private static javax.servlet.ServletContext servletContext = null;

	public static void setServletContext(javax.servlet.ServletContext newServletContext) {
		servletContext = newServletContext;
	}

	public static javax.servlet.ServletContext getServletContext() {
		return servletContext;
	}

	public static boolean deleteConstant(String name) {
		boolean result = false;
		if (constantsMap.containsKey(name)) {
			result = true;
			constantsMap.remove(name);
		}
		return (result);
	}

	public static boolean containsKey(String key) {
		return constantsMap.containsKey(key);
	}

	public static List<ConfDetails> getAllValues() {
		return allValues;
	}

	public static List<String> getAllKeys() {
		return new ArrayList<>(constantsMap.keySet());
	}

	/**
	 * Nastavi/prepise novu konfiguracnu hodnotu do listu
	 *
	 * @param conf
	 */
	public static void setAllValues(ConfDetails conf) {
		try {
			// nastav datum zmeny
			conf.setDateChanged(new Date());

			// prehladaj na hodnotu a zmaz ju, potom pridaj
			for (int i = 0; i < allValues.size(); i++) {
				if (allValues.get(i).getName().equals(conf.getName())) {
					allValues.set(i, conf);
					return;
				}
			}
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
		}
		allValues.add(conf);
	}

	public static void setInstallName(String installName) {
		Constants.INSTALL_NAME = installName;
	}

	public static String getInstallName() {
		if (Constants.getBoolean("multiDomainUseAliasAsInstallName")) {
			String domainAlias = MultiDomainFilter.getDomainAlias(CloudToolsForCore.getDomainName());
			if (Tools.isNotEmpty(domainAlias)) {
				return domainAlias;
			}
		} else if (Constants.getBoolean("constantsAliasSearch")) {
			return getString("installName");
		}

		return Constants.INSTALL_NAME;
	}

	public static String getLogInstallName() {
		return getString("logInstallName");
	}

	/**
	 * k zadanemu menu konstanty prida prefix domeny (iba ak je tato nova konstanta
	 * ulozena v hashtabulke, inak vrati orginalne meno).
	 * Da sa vypnut pomocou constantsAliasSearch (prejavy sa az po restarte)
	 *
	 * @param name
	 * @return
	 */
	private static String getConstantNameDomainAlias(String name) {
		if (isConstantsAliasSearch() == false || name == null)
			return name;

		// kvoli performance, toto nemoze byt prefixovane
		if ("multiDomainUseAliasAsInstallName".equals(name) || "constantsAliasSearch".equals(name)
				|| name.startsWith("multiDomainAlias:"))
			return name;

		// multiDomainAlias - pouziva sa v MultiDomainFilter.getDomainAlias
		if (Tools.isNotEmpty(name)) {
			RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
			if (requestBean != null && Tools.isNotEmpty(requestBean.getDomain())) {
				String domainAlias = MultiDomainFilter.getDomainAlias(requestBean.getDomain());
				if (Tools.isEmpty(domainAlias)) {
					domainAlias = requestBean.getDomain();
				}
				if (Tools.isNotEmpty(domainAlias)) {
					String nameWithAlias = domainAlias + "-" + name;
					// ak taky zaznam s aliasom v hashtabulke existuje, tak sa vrati jeho meno, inak
					// vrati defaultne meno
					if (constantsMap.get(nameWithAlias) != null)
						name = nameWithAlias;
				}
			}
		}
		return name;
	}

	public static boolean isConstantsAliasSearch() {
		return CONSTANTS_ALIAS_SEARCH;
	}

	public static void setConstantsAliasSearch(boolean constantsAliasSearch) {
		// nastavenim constantsAliasSearch na true
		CONSTANTS_ALIAS_SEARCH = constantsAliasSearch;
	}

	/**
	 * Gets description of constant
	 *
	 * @param constName Name of the constant
	 * @return The string value
	 */
	public static String getDescription(String constName) {
		String description = "";

		for (ConfDetails conf : allValues) {
			if (conf.getName().equals(constName)) {
				description = conf.getDescription();
				break;
			}
		}

		return description;
	}

	/**
	 * Gets type of constant value
	 *
	 * @param constName Name of the constant
	 * @return The string value (integer, string, boolean)
	 */
	public static String getConstType(String constName) {
		String value = getString(constName);
		if (Tools.isNotEmpty(value)) {
			try {
				Integer.parseInt(value);
				return "integer";
			} catch (Exception e) {
				// nieje integer
				if ("true".equals(value) || "false".equals(value))
					return "boolean";
				return "string";
			}
		}
		return "";
	}

	/**
	 * Vrati hodnotu konstanty skonvertovanu do hashtabulky, format zapisu konf.
	 * premennej je
	 * kluc1|hodnota1
	 * kluc2|hodnota2
	 * kluc3|hodnota3
	 *
	 * @param name
	 * @return
	 */
	public static Map<String, String> getHashtable(String name) {
		Map<String, String> table = new Hashtable<>();

		try {
			String value = Constants.getString(name);

			String[] values = Tools.getTokens(value, "\n");
			if (values != null && values.length > 0) {
				for (String pair : values) {
					if (Tools.isEmpty(pair))
						continue;
					String[] pairArray = pair.split("\\|");
					if (pairArray == null || pairArray.length != 2)
						continue;

					table.put(pairArray[0], pairArray[1]);
				}
			}
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
		}
		return table;
	}
}
