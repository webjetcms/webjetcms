<%@page import="java.util.Map"%><%@page import="java.util.List"%><%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="java.util.Hashtable"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/admin/layout_top.jsp" %>

<%@page import="sk.iway.iwcm.system.*"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%><script language="JavaScript">
var helpLink = "";
</script>
<%
//vypise tabulku so zoznamom vsetkych existujucich modulov

List<ModuleInfo> allModules = Modules.getInstance().getItems();
List<ModuleInfo> filtered = new ArrayList<ModuleInfo>();
for (ModuleInfo m : allModules)
{
	//out.println(m.getItemKey());
	if (m.getNameKey().equals("webjet_cmp_catalog")) continue;
	if (m.getNameKey().equals("webjet_cmp__common")) continue;
	if (m.getNameKey().equals("webjet_cmp_auction")) continue;
	if (m.getNameKey().equals("webjet_cmp_cestovka")) continue;
	if (m.getNameKey().equals("webjet_cmp_kurzy")) continue;
	if (m.getNameKey().equals("webjet_cmp_bazar")) continue;
	if (m.getNameKey().equals("webjet_cmp_page_update_info")) continue;
	if (m.getNameKey().equals("webjet_cmp_tips")) continue;
	if (m.getNameKey().equals("webjet_cmp_chat")) continue;
	if (m.getNameKey().equals("webjet_cmp_counter")) continue;
	if (m.getNameKey().equals("webjet_cmp_pocasie")) continue;
	if (m.getNameKey().equals("webjet_cmp_sms")) continue;
	if (m.getNameKey().equals("webjet_cmp_json_editor")) continue;
	if (m.getNameKey().equals("webjet_cmp_popup")) continue;
	if (m.getNameKey().equals("webjet_cmp_profesia")) continue;
	if (m.getNameKey().equals("webjet_cmp_graph")) continue;
	if (m.getNameKey().equals("webjet_cmp_imagefont")) continue;
	if (m.getNameKey().equals("intranet")) continue;
	if (m.getNameKey().equals("webjet_cmp_intranet")) continue;
	if (m.getNameKey().equals("webjet_cmp_projects_microsite")) continue;
	if (m.getNameKey().equals("webjet_cmp_elfinder")) continue;
	if (m.getNameKey().equals("webjet_cmp_content-block")) continue;

	//NET/DSK/LMS appky
	if (m.getNameKey().equals("webjet_cmp_lms")) continue;
	if (m.getNameKey().equals("webjet_cmp_adresar")) continue;
	if (m.getNameKey().equals("webjet_cmp_sharepoint")) continue;
	if (m.getNameKey().equals("webjet_cmp_inzercia")) continue;
	if (m.getNameKey().equals("webjet_cmp_uschovna")) continue;
	if (m.getNameKey().equals("webjet_cmp_wiki")) continue;
	if (m.getNameKey().equals("webjet_cmp_docman")) continue;
	if (m.getNameKey().equals("webjet_cmp_magzilla")) continue;
	if (m.getNameKey().equals("webjet_cmp_inventory")) continue;
	if (m.getNameKey().equals("webjet_cmp_mail")) continue;
	if (m.getNameKey().equals("webjet_cmp_organization")) continue;
	if (m.getNameKey().equals("webjet_cmp_kanban")) continue;
	if (m.getNameKey().equals("webjet_cmp_page_reactions")) continue;
	if (m.getNameKey().equals("webjet_cmp_mcalendar")) continue;
	if (m.getNameKey().equals("webjet_cmp_helpdesk")) continue;


	//systemove appky
	if (m.getNameKey().equals("webjet_cmp_webjet9")) continue;
	if (m.getNameKey().equals("webjet_cmp_grideditor")) continue;
	if (m.getNameKey().equals("webjet_cmp_sync")) continue;
	if (m.getNameKey().equals("webjet_menuSync")) continue;
	if (m.getNameKey().equals("webjet_cmp_xml")) continue;

	//TODO:
	if (m.getNameKey().equals("webjet_cmp_fileatr")) continue;

	//DEPRECATED
	if (m.getNameKey().equals("webjet_cmp_db_browser")) continue;
	//if (m.getNameKey().equals("webjet_menu.users")) continue; //duplicity with webjet_menuUsers


	System.out.println("Testing: "+m.getNameKey());
	if (m.getNameKey().startsWith("webjet_cmp_"))
	{
		//otestuj ci existuje adresar
		String folderName = m.getNameKey().substring(11);
		System.out.println(folderName);
		IwcmFile f = new IwcmFile(Tools.getRealPath("/components/"+folderName+"/"));

		if (f.exists()==false)
		{
			m.setItemKey("---");
		}
	}

	filtered.add(m);
}
request.setAttribute("modules", filtered);

Prop prop = Prop.getInstance(request);

int basicCount = 0;
int proCount = 0;
int entCount = 0;
int multiCount = 0;
int osCount = 0;
int msgCount = 0;

Map<String, String> keysTable = new Hashtable<String, String>();
keysTable.put("messages", "Posielanie správ");
keysTable.put("fileatr", "Atribúty súborov");
keysTable.put("clone_structure", "Klonovanie štruktúry");
keysTable.put("attributes", "Atribúty stránky");
keysTable.put("fileIndexer", "Vyhľadávanie v súboroch");
keysTable.put("xml", "Import z XML");
keysTable.put("adminlog", "Audit");
keysTable.put("redirects", "Presmerovania stránok a domén");
keysTable.put("form", "Formuláre");
keysTable.put("magzilla", "Help Desk");
keysTable.put("catalog", "catalog - uz sa nepoziva, nahradene modulom Basket");
keysTable.put("imageEditor", "Online editor obrázkov");
keysTable.put("Import/Export", "Synchronizácia dát vrámci WebJET rodiny (Import/Export)");
keysTable.put("menuSync", "Synchronizácia dát medzi WebJETmi");
keysTable.put("_common", "_common - spoločné nástroje");
keysTable.put("crontab", "Úlohy na pozadí");
keysTable.put("menuUsers", "Správa používateľov");
keysTable.put("multiDomain", "Multi Domain");
keysTable.put("cestovka", "Cestovka");
keysTable.put("wiki", "Wiki");
keysTable.put("chat", "Online chat");
keysTable.put("Rating", "Rating - hodnotenie stránok");
keysTable.put("proxy", "Proxy");
keysTable.put("captcha", "Captcha");
keysTable.put("rss", "Rss export a čítačka");
keysTable.put("sharepoint", "Sharepoint zoznam dokumentov");
keysTable.put("insert_script", "Skripty");
keysTable.put("webjet_cmp_elfinder", "Správa súborov");
keysTable.put("uschovna", "Úschovňa súborov");
keysTable.put("export", "Export dát");
keysTable.put("menu.users", "Správa používateľov");
keysTable.put("menu.templates", "Správa šablón");
keysTable.put("abtesting", "A/B testovanie");
keysTable.put("export", "Export dát");
keysTable.put("Vloženie dokumentu", "Zobrazenie dokumentu (word, pdf, xls) v stránke");
keysTable.put("mail", "E-mail klient");
keysTable.put("menu.fbrowser", "Správa súborov a adresárov v administrácii");
keysTable.put("mcalendar", "Pracovný kalendár");
keysTable.put("helpdesk", "HelpDesk");
keysTable.put("file_archiv", "Archív súborov");
keysTable.put("organization", "Organizačná štruktúra");
keysTable.put("kanban", "Kanban");
keysTable.put("enumerations", "Číselníky");
keysTable.put("crypto", "Šifrovanie dát");
keysTable.put("export", "Export noviniek");
keysTable.put("Server monitoring", "Monitoring stavu serveru");
keysTable.put("tooltip", "Tooltip/nápoveda");
keysTable.put("structuremirroring", "Zrkadlenie štruktúry");
keysTable.put("response-header", "HTTP hlavičky");

//desc
keysTable.put("components.enumerations.menu.desc", "Aplikácia Číselníky umožňuje vytvárať pomenované typy číselníkov, do ktorých je následne možné ukladať rôzne údaje (napr. zoznam okresov). Typy a dáta číselníkov je tiež možné exportovať a importovať pomocou .xls súboru. Čiselník môžete použíť v aplikácii Formuláre alebo nastaviť výberové možnosti pre volné polia vo web stránke, alebo ich využiť v zákazníckej implementácii.");
keysTable.put("components.insert_script.menu.desc", "Aplikácia pre jednoduchšie vkladanie meracích kódov a iných scriptov do stránky. Scripty sa vkladajú podľa zvolených adresárov alebo stránok. Ľahko tak máte prehľad o tom, do ktorých sekcií sa vkladá ktorý script.");
keysTable.put("components.redirects.menu.desc", "Umožňuje nastavovať presmerovania URL adries a celých domén. Pre URL adresy je možné nastaviť starú a novú adresu a presmerovací kód. Pre domény pôvodnú a novú doménu a URL adresu presmerovania a protokol (http, https).");
keysTable.put("components.clone_structure.menu.desc", "Klonovanie štruktúry web stránok umožňuje nakopírovať kompletnú štruktúru stránok z jedného adresára do druhého adresára. Napr. ako základ pre anglické verziu sa použije kompletná štruktúra slovenskej verzie. V naklonovanej verzii následne jednoducho upravíte len texty a názvy stránok a adresárov.");
keysTable.put("components.export.menu.desc", "Aplikácia umožňuje exportovať zoznam noviniek alebo článkov zo zadaného adresára vo formáte RSS alebo JSON (napr. pre mobilnú aplikáciu). Exportu definujte URL adresu na ktorej bude export dostupný, formát (json alebo RSS), počet exportovaných položiek, ID adresárov s novinkami/tlačovými správami, spôsob usporiadania a typy stránok (aktuálne, archívne, budúce).");
keysTable.put("components.import_web_pages.menu", "Prenos dát webstránok medzi vývojovými prostrediami (napr. z testovacieho na produkčné prostredie). Pri exporte vygeneruje ZIP súbor s obsahom web stránok vrátane obrázkov. Import zobrazí porovnanie aktuálneho stavu a umožní nastaviť každej importovanej web stránke možnosť importu a prepísania (ak stránka s rovnakým menom už existuje).");
keysTable.put("components.server_monitoring.title.desc", "Zobrazovanie grafov a údajov záťaže servera - využitia pamäte, počtu aktívnych session, počtu cache objektov a počtu databázových spojení.");
keysTable.put("components.proxy.menu.desc", "Umožňuje do stránky vkladať HTML kód generovaný iným serverom v lokálnej sieti. HTTP požiadavka je preposlaná na nastavený server a následne výstup vložený do web stránky. WebJET vystupuje v roli proxy servera.");
keysTable.put("components.captcha.menu.desc", "Aplikácia pre zobrazenie Captcha verifikácie obrázku vo formulároch.  Umožňuje integrovať aj Invisible Captcha systém od Google.");
keysTable.put("components.basket.desc", "Cez aplikáciu E-shop môžete vytvoriť a spravovať jednoduchý elektronický obchod. V rámci aplikácie definujete jednotlivé produkty a ich atribúty (napr. veľkosť, farba), spôsoby doručenia, či platby. Aplikácia evidujete zoznam prijatých objednávok, umožňuje nastavovať ich stav s možnosťou notifikácie zmien v objednávke na zákazníkov email.");
keysTable.put("components.abtesting.menu.desc", "Táto aplikácia vám umožňuje vykonať A/B testovanie dvoch verzií stránky. Je možné vytvoriť kópiu už existujúcej A stránky s upravenou adresou stránky, v ktorej môžete testovať B verziu (upravenú verziu stránky, napr. texty, obrázky, odkazy). WebJET následne automaticky zabezpečí zobrazovanie A a B verzie stránky na pôvodnej adrese v definovanom pomere (napr. pre 70% návštevníkov zobrazí verziu A a pre 30% verziu B, na ktorej testujete úpravu). Návštevník teda stále vidí pôvodnú adresu, ale namiesto A verzie sa mu automaticky zobrazí B verzia stránky.");
keysTable.put("components.form.menu.desc", "Aplikácia Formuláre slúži na pokročilú tvorbu a správu formulárov. Každá web stránka môže obsahovať formulár, ktorý návštevník vyplní. Môžu to byť žiadosti o pracovné miesto, kontaktný formulár a podobne. Vyplnený formulár sa odošle na definované email adresu, pričom v email správe formulár vyzerá rovnako ako na web stránke. Je možné ho vytlačiť a ďalej spracovať. Naviac, všetky formuláre sa ukladajú do databázy v administrátorskej časti Web JET a je možné sa k nim kedykoľvek vrátiť. Položky vo formulároch je možné usporadúvať podľa ľubovoľného stĺpca a exportovať do programu MS Excel.");
keysTable.put("components.imageEditor.title.desc", "Integrovaný editor obrázkov umožňuje meniť veľkosť obrázkov, rotovať ich, či orezávať priamo v prehliadači. Ak potrebujete spraviť rýchlu zmenu v obrázku, viete to vykonať priamo v prehliadači vo WebJETe.");
keysTable.put("components.menuUsers.title.desc", "V správe používateľov môžete komplexne nastavovať oprávnenia používateľov na jednotlivé aplikácie, prístupové práva na súborový systém a časti web sídla. Môžete vytvárať skupiny používateľov a nastaviť skupinové práva. Okrem administrátorov (redaktorov) aplikácia spravuje aj registrovaných návštevníkov web sídla a emailové skupiny. Používateľov je možné importovať aj exportovať vo formáte XLS.");
keysTable.put("components.menu.templates.title.desc", "Umožňuje definovať priradenie hlavičiek, pätičiek, menu, CSS štýlov k fyzickým JSP súborom s HTML kódom dizajnu stránok. Hlavička tak nie je v HTML kóde napevno ale sa ľahko dá upraviť. Šablón je možné definovať ľubovoľné množstvo (napr. hlavná stránka, podstránka) a limitovať ich zobrazenie podľa adresárovej štruktúry (napr. šablóna pre novinky sa bude zobrazovať len v sekcii Tlačové správy).");
keysTable.put("components.dmail.desc", "Aplikácia E-mail - distribučný zoznam vám umožní posielať hromadné personalizované emaily viacerým používateľom naraz. Každá správa sa posiela samostatne a jednotliví príjemcovia nevidia e-mailové adresy ostatných príjemcov. Výhodou je, že každý e-mail môže byť personalizovaný - ak do tela mailu vložíte značku namiesto nej sa vloží skutočné meno príjemcu. E-mail je možné posielať návštevníkom, ktorí sú registrovaní v admin časti systému WebJET, alebo vytvoriť súbor so zoznamom mien a e-mail adries a tie potom naimportovať ako zoznam príjemcov. Pre registráciu návštevníkov na odber správ treba vytvoriť 2 stránky: prihlásenie a odhlásenie. Vloží do stránky komponent pre prihlásenie / odhlásenie z newslettra (distribučného zoznamu). ");
keysTable.put("components.menu.fbrowser.title.desc", "Súborový manažér v administráciu pre pohodlnú správu súborov a priečinkov.");
keysTable.put("components.attributes.menu.desc", "Umožňuje definovať meta dáta priradené k webstránke (napr. farba produktu, veľkosť, váha, rozmer) a následne tieto metadáta zobrazovať na webstránke.");
keysTable.put("components.forum.desc", "Aplikácia Diskusia (fórum) umožňuje do stránky vložiť jednoduchú diskusiu pod článkom, alebo samostatné diskusné fórum s možnosťou vytvárania tém. Diskusnému fóru môžete nastaviť vlastnosti ako spôsob usporiadania príspevkov, spôsob stránkovania a pre fórum nastaviť aj časové obmedzenie pre zmazanie príspevku.");
keysTable.put("components.crontab.menu.desc", "Správa a definícia opakovaných úloh spúšťaných na pozadí (napr. sťahovanie XML súboru organizačnej štruktúry a jej import).");
keysTable.put("components.data_deleting.title.desc", "Aplikácia na retenciu starých dát umožňuje vymazať staré štatistiky (návštevnosť stránky), odoslané emaily, zaznamenané dáta monitoringu servera, históriu prehľadávaných webstránok a auditných záznamov.");
keysTable.put("components.fileIndexer.title.desc", "Táto aplikácia vytvára full text index súborov typu doc(x), xls(x), ppt(x), pdf, xml, txt a umožňuje súbory vyhľadávať vrámci integrovaného vyhľadávania na webstránke.");
keysTable.put("components.tooltip.menu.desc", "Zobrazí pomocný/vysvetľujúci text pri prechode myšou ponad zadanú skratku/výraz.");
keysTable.put("components.multiDomain.title.desc", "Umožní v jednej inštalácii WebJET CMS spravovať viaceré nezávislé domény. Na každej doméne môže byť samostatné web sídlo s oddelenými súbormi a obrázkami. Každá doména môže mať vlastný dizajn a vlastné menu.");
keysTable.put("components.structuremirroring.menu.desc", "Zrkadlenie štruktúry vzájomne preväzuje adresáre a stránky jazykových mutácií. Zmena v jednej jazykovej mutácii sa automaticky prejaví aj v druhej. Zároveň prepája web stránky vo verejnej časti, čiže ak sa nachádzam na stránke SK/O nás a kliknem na EN verziu v hlavičke stránky, dostanem sa na zrkadlenú stránku EN/About Us.");
keysTable.put("components.rss.menu.desc", "Exportuje zoznam stránok/noniviek do RSS formátu. Umožňuje do stránky vložiť odkaz na novinky v inej stránke z rss formátu.");
keysTable.put("components.messages.menu", "Umožňuje poslať správu inému administrátorovi a integráciu odosielania správ do webstránky (napr. vrámci LMS odosielanie správ medzi tútorom a študentom).");
keysTable.put("components.map.desc", "Vloží do stránky interaktívnu mapu (Google maps alebo Open Street Map) podľa zadaných GPS súradníc, alebo adresy. Pre použitie Google maps je potrebné mať zakúpený API kľúč od Google.");
keysTable.put("components.blog.desc", "Aplikácia Blog umožňuje používateľom zjednodušenú tvorbu článkov, rubrík, správu diskusných príspevkov k týmto článkom, a pre administrátora zjednodušenú správu blogerov.");
keysTable.put("webjet_cmp_qa", "Frequently Asked Questions - čato kladené otázky. S touto aplikáciou môžete vytvárať otázky a odpovede a vkladať ich do skupín. Následne tieto otázky a odpovede vkladáte na webstránky. Táto aplikácia vie tiež zbierať otázky návštevníkov z kontaktného formulára, cez ktorý im potom môžete odpovedať.");
keysTable.put("components.crypto.menu.desc", "Touto aplikáciou pridáte podporu šifrovania dát pre aplikáciu Formuláre. Vygenerujete si šifrovací aj dešifrovací kľúč. Šifrovací kľúč nastavíte vo vlastnostiach konkrétnemu formuláru a týmto kľúčom sa budú šifrovať údaje v databáze. Pre zobrazenie dát zadáte dešifrovací kľúč s ktorým sa vám zobrazia dešifrované údaje. V databáze ale stále zostávajú zabezpečené šifrovaním.");
keysTable.put("components.adminlog.menu.desc", "Evidencia zmien v systéme WebJET, umožňuje vyhľadávanie v auditných záznamoch podľa dátumu a času, typu udalosti, autora a popisu.");
keysTable.put("components.response-header.menu.desc", "Aplikácia umožňuje definovať HTTP hlavičky odpovede (HTTP Response Header) na základe URL adries zobrazenej stránky.");
//keysTable.put("", "");

String key = null;
%>
<style>
.center { text-align: center; }
</style>

<display:table class="sort_table" name="modules" uid="m" export="true" defaultsort="1"><%ModuleInfo mi = (ModuleInfo)pageContext.getAttribute("m"); %>

	<display:setProperty name="export.excel.filename" value="modules.xls" />
	<display:setProperty name="export.csv.filename" value="modules.csv" />
	<display:setProperty name="export.xml.filename" value="modules.xml" />
	<display:setProperty name="export.pdf.filename" value="modules.pdf" />

	<display:column title="Modul"><%
		String value = mi.getNameKey();
		key = null;
		if (value.startsWith("webjet_cmp_"))
		{
			key = "components."+value.substring(11)+".title";
			String test = prop.getText(key);
			if (test.startsWith("components.")==false) value = test;
			else
			{
				key = "menu."+value.substring(11);
				test = prop.getText(key);
				if (test.startsWith("menu.")==false) value = test;
				else
				{
					key = "components."+value.substring(11)+".menu";
					test = prop.getText(key);
					if (test.startsWith("components.")==false) value = test;
				}
			}
		}
		if (value.startsWith("webjet_"))
		{
			value = value.substring(7);
		}
		if (value.startsWith("cmp_"))
		{
			value = value.substring(4);
		}
		if (key == null) key = "components."+value+".title";

		//out.println(value+"<br/>");
		String test = keysTable.get(value);
		if (test != null) value = test;

		out.print(value);
	%></display:column>
	<display:column property="nameKey"/>
	<display:column title="Popis"><%
		String descKey = Tools.replace(key, ".title", ".desc");
		String test = prop.getText(descKey);
		if (test.equals(descKey)) {
			descKey = key+".desc";
			test = prop.getText(descKey);
		}
		//out.println(descKey+"<br/>");
		if (test.equals(descKey)==false && keysTable.get(descKey)==null) out.println(test);
		else {
			test = keysTable.get(descKey);
			if (test != null) out.println(test);
			else out.println("<span style='color: red;'>"+descKey+"</span>");
		}
	%></display:column>
	<display:column property="group"/>
	<display:column class="center" title="Basic"><% if (mi.getWjVersions().indexOf("B")!=-1) { out.print("X"); basicCount++; } %></display:column>
	<display:column class="center" title="Professional"><% if (mi.getWjVersions().indexOf("P")!=-1) { out.print("X"); proCount++; } %></display:column>
	<display:column class="center" title="Enterprise"><% if (mi.getWjVersions().indexOf("E")!=-1) { out.print("X"); entCount++; } %></display:column>
	<display:column class="center" title="Community"><% if (mi.getWjVersions().indexOf("O")!=-1) { out.print("X"); osCount++; } %></display:column>
	<display:column class="center" title="MultiWeb"><% if (mi.getWjVersions().indexOf("M")!=-1) { out.print("X"); multiCount++; } %></display:column>
	<display:column class="center" title="MSG"><% if (mi.getWjVersions().indexOf("D")!=-1) { out.print("X"); msgCount++; } %></display:column>
</display:table>

Basic: <%=basicCount %><br/>
Professional: <%=proCount %><br/>
Enterprise: <%=entCount %><br/>
Community/Open Source: <%=osCount %><br/>
MultiWeb: <%=multiCount %><br/>
MSG: <%=msgCount %><br/>


<%@ include file="/admin/layout_bottom.jsp" %>