package sk.iway.iwcm.system;

import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;

public class ConstantsV9 {

	private ConstantsV9() {
	}

	/**
	 * Nastavi predvolene hodnoty pre verziu 9
	 * Tato trieda je vo W9 reimplementovana, tu nic nemente
	 */
	public static void clearValuesWebJet9() {
		Constants.setString("defaultSkin", "webjet9");
		Constants.setString("auditJpaDisabledEntities", "");
		Constants.setString("mariaDbDefaultEngine", "InnoDB");
		Constants.setString("chunksQuantity", "25");
		Constants.setString("propertiesAdminKeys",
			  "button.*\ncomponent.calendar.month.*\ndayfull.*\ncomponents.forms.alert.gdpr\ngroupslist.docid_url\nwebstranky.folder_id\ndefault.project.name\neditor.paragraph\neditor.h1\neditor.h2\neditor.h3\neditor.h4\neditor.h5\neditor.h6,admin.conf_editor.do_you_really_want_to_restart,admin.conf_editor.restarted\neditor.preview\neditor.tab.*,components.import_web_pages.menu,editor.newDocumentName,history.editPage,history.showPage,groupslist.compare,groupslist.edit_dir,editor.save_as_abtest.confirm.title,editor.save_as_abtest.confirm.text,editor.save_as_abtest,stat_doc.pageStat,web_pages_list.link_check_button,menu.logout,pagebuilder.modal.tab.size,pagebuilder.modal.visibility.*,datatable.tab.*,text.warning,text.info,editor.directory_name");
		Constants.setInt("webpagesTreeAutoOpenLimit", 2);
		Constants.setString("deepl_auth_key", "", "translations", "Preklady - authentifikacny kluc k sluzbe deepl.com pre preklad textov");
		Constants.setString("deepl_api_url", "https://api-free.deepl.com/v2/translate", "translations", "URL adresa pre API volanie DeepL prekladu, ak mate PRO ucet pouzite https://api.deepl.com/v2/translate");
		Constants.setString("deepl_api_usage_url", "/v2/usage", "translations", "URL adresa pre API volanie DeepL statistiky pouzitia");

		Constants.setInt("formsDatatableServerSizeLimit", 5000, Constants.mods(Constants.MOD_PERFORMANCE, "forms"),
				"Minimalny pocet zaznamov formularu pre ktore sa uz pouzije serverove strankovanie.");

		Constants.setString("sk.iway.iwcm.qa.AddAction.sendAdminMail.url", "/apps/qa/admin/");

		Constants.setBoolean("loggingInMemoryEnabled", false, "adminlog",
				"Aktivovanie ukladania posledných log záznamov do pamäte pre jednoduchšiu kontrolu logov.");
		Constants.setInt("loggingInMemoryQueueSize", 200, "adminlog",
				"Maximalny počet log záznamov držaných v pamäti.");
		Constants.setString("passwordHashAlgorithm", "bcrypt", Constants.MOD_SECURITY, "Meno algoritmu pre hashovanie, možné hodnoty bcrypt alebo sha-512");
		Constants.setInt("bcryptSaltRounds", 12, Constants.MOD_SECURITY, "log2 počtu opakovaní saltovania pri bcrypt algoritme hashovania hesiel");

		//pentesty, vylucene /components/user/logon.jsp
		Constants.setString("componentsDirectCallExceptionsSystem",
				"/components/_common/clk.jsp,/components/_common/combine.jsp,/components/_common/html_tags_support.jsp,/components/_common/wysiwyg/,%.js.jsp,/components/banner/banner.jsp,%/invoice_email.jsp,/components/basket/order_payment_bank_reply.jsp,/components/blog/blog_save.jsp,%/detail.jsp,%/upload.jsp,/components/forum/iframe.jsp,%new.jsp,%/saveok.jsp,%/savefail.jsp,%/new_file.jsp,%/send_card.jsp,%/show_gallery_image,%videoplayer.jsp,%ajax,%voteResultsDiv,%check_form,%/forum-open,%/forum_mb_open,%/forum_mb,%-approve.jsp,%send_link_form,/components/reservation/,/components/user/change_password.jsp,/components/user/authorize.jsp,%import.jsp,/components/zmluvy/,/components/server_monitoring/monitor.jsp,/components/page_update_info/add_subscriber,/components/page_update_info/remove_subscriber,%jscript.jsp,%addbasket_popup.jsp,/components/messages/refresher-ac.jsp,%/iframe_blank,/components/user/fileforward.jsp,%spamprotectiondisable.jsp,%/htmlarea/,%json,/components/qa/admin_answer.jsp,%popcalendar.jsp,%jscripts.jsp,%chart.jsp,%date_locale.jsp,%click.jsp,/components/cloud/calendar/potvrd_akciu_verejnost.jsp,/components/customer_reviews/potvrd_review.jsp,/components/gis/potvrd_gis.jsp,/components/gallery/photoswipe/photoswipe.jsp",
				Constants.MOD_SECURITY, "Zoznam zaciatkov URL adries pre ktore je povolena vynimka priameho volania JSP komponenty");

		//domena a priecinok s novinkami pre dashboard
		Constants.setString("overviewJsonUrl", "https://docs.webjetcms.sk/json/");
		Constants.setString("languages", "sk,cz,en,de,pl,hu,cho,ru,esp", Constants.MOD_CONFIG, "Zoznam jazykov pre webjet cms");

		Constants.setString("xssHtmlAllowedFieldsSystem",
				"value,prop_value,message_text,text,description,user_note,title,value_string,question,message,string1,string2,string3,string4,string5,string6,reg_exp,gallery_perex,watermark,regexp_value,body,room_description,question_text,answer_text_ok,answer_text_fail,script_body,junk_reason,media_info_sk,password,salt,crop_start,crop_end,auth_username,auth_password,question,answer,hash,kategoria,definicia,poznamka1,poznamka2,zdroj1,zdroj2,priklad,tip_text,svalue1,svalue2,svalue3,svalue4,signature,authorize_hash,password_salt,navbar,external_link,group_name,url,question_text",
				Constants.MOD_SECURITY,
				"Zoznam stlpcov v databaze, ktore mozu obsahovat HTML kod (nebudu pri citani escapovane specialne znaky). Pre zakaznicke projekty nastavte premennu xssHtmlAllowedFields");

		Constants.setString("xsrfParamNameExceptionSystem", Constants.getString("xsrfParamNameExceptionSystem")+",tempId,redirectId,dir,bid,actualDir,pId,origUrl,week,w,h,ip,c,noip,rnd,login,auth,reservationDate,iID,name,act,datum,basketAct,invoicePaymentId,email,save");

		Constants.setString("jpaToLowerFields", "description,questionText,notifyIntrotext,question,data,dataAsc,htmlHead,htmlData,attachments,message,files,html,note,descriptionLong*,answer,afterBodyData,value,mediaInfo*,userNote,messageText,htmlCode,purpose,content,propValue,defaultValue,dataResult,descriptionText,scriptBody,relatedPages,name", Constants.MOD_CONFIG, "Zoznam nazvov CLOB stlpcov pre ktore sa v pripade Oracle pouzije LOWER funkcia pri vyhladavani");

		Constants.setInt("datatablesExportMaxRows", 50000, Constants.MOD_PERFORMANCE, "Datatables - maximalny pocet riadkov, ktore je mozne naraz exportovat.");
		Constants.setInt("javaMinimalVersion", 17, Constants.MOD_CONFIG, "Minimalna podporovana verzia jazyka Java");

		Constants.setBoolean("2factorAuthEnabled", true, Constants.MOD_SECURITY, "Umoznuje vypnut moznost pridania 2factor autorizacie pre administratorov. Pouziva sa v pripade SSO integracie, kedy sa prihlasovania vykonava na externom serveri.");

		Constants.setString("seo.serpApiKey", "", Constants.MOD_STAT, "API kluc k SerpAPI pre vyhladavanie pozicii klucovych slov");
		Constants.setInt("seo.serpApiGoogleMaxResult", 10, Constants.MOD_STAT, "Maximalny pocet zaznamov ktore ma Google vratit pri prehladavani pozicie SEO keywords");

		Constants.setString("structureMirroringAutoTranslatorLogin", "autotranslator", Constants.MOD_USER, "Login usera, ktorý sa využíva na auto prekladanie. Slúži pri prekladaní (názvu, tela) web stránok a pri indikácií že do web stránky ešte nezasiahol normálny používateľ.");

		Constants.setBoolean("multigroupRedirectSlavesToMaster", false, Constants.MOD_EDITOR, "Pri ulozeni stranky vo viacerych priecinkoch nastavuje sposob zobrazenia slave stranok. Pri hodnote true su presmerovane na master, pri hodnote slave su zobrazene ako samostatne stranky.");

		Constants.setBoolean("clusterHostnameTrimFromEnd", false, "cluster", "Rezim ziskania hostname pre cluster. Ak je hodnota true, tak sa hostname ziskava ako 16 znakov od konca, inak 16 znakov od zaciatku.");

		Constants.setBoolean("attrAlwaysCleanOnSave", false, Constants.MOD_EDITOR, "Po nastaveni na true sa vzdy pri ulozeni stranky premaze zoznam atributov");

		Constants.setString("DocTools.removeCharsDir", "[^a-zA-Z/_0-9\\-\\.=]", Constants.MOD_SECURITY, "Regex pre znaky, ktore sa maju odstranit z nazvu suboru/adresara. Pouziva sa v metode DocTools.removeCharsDir(). Hodnota premennej sa reloadne za behu.");

		Constants.setString("FileBrowserTools.forbiddenSymbols", "@,#,+,(,),{,},=", Constants.MOD_SECURITY, "Zoznam zakazanych znakov v nazve suboru/adresara. Okrem defaultne zakazanych znakov sa pomocou konfiguracnej premennej definuju dalsie zakazane znaky. Oddelujeme ich ciarkou. Pouziva sa v metode FileBrowserTools.hasForbiddenSymbol(). Hodnota premennej sa reloadne iba pri starte.");

		Constants.setBoolean("structureMirroringDisabledOnCreate", true, "structuremirroring", "Pri hodnote true budu novo vytvorene zrkadlene stranky mat vypnute zobrazenie aby sa nezacali ihned zobrazovat");

		Constants.setInt("restaurantMenu.alergensCount", 14, "restaurant_menu", "Maximalny pocet alergenov, tie sa ziskavaju z prekladovych klucov s prefixom components.restaurant_menu.alergen");

		Constants.setString("ntlmLogonAction.charsetEncoding", "windows-1250", "ntlm", "Nazov kodovania, ktore sa pouzije pre prekodovanie znakov ziskanych z LDAP servera. Ak je hodnota prazdna, tak sa nevykona ziadne prekodovanie.");

		Constants.setString("webpagesFunctionsPerms", "menuWebpages|cmp_blog|cmp_blog_admin|cmp_news|cmp_abtesting|cmp_basket", Constants.MOD_SECURITY, "Zoznam prav pre funkcie web stranok, ktore sa pouzivaju aj v inych moduloch ako blog, novinky atd.");
		Constants.setString("bloggerAppPermissions", "menuGallery,menuInquiry,cmp_quiz,cmp_form", "blog", "Prava k apllikáciam, ktoré sa majú pridať vytvorenému používateľovi typu BLOGGER");

		Constants.setString("dmailListUnsubscribeBaseHref", "", "dmail", "Zakladna URL adresa pre odhlasovanie sa z mailing listu. Ak je prazdna, tak sa pouzije URL adresa podla domeny odosielaneho emailu.");

		Constants.setString("uaParserYamlPath", "", "stat", "Cesta k YAML suboru s konfiguraciou pre UA parser. Ak je prazdna, tak sa pouzije defaultna konfiguracia.");
		Constants.setBoolean("usersSplitByDomain", false, Constants.MOD_USER, "Pri nastaveni na true su oddeleny pouzivatelia podla domen podobne ako v MultiWeb instalacii");

		Constants.setString("amchartLicense", "", Constants.MOD_STAT, "Licencny kluc pre amchart kniznicu");

		Constants.setString("basketInvoiceBonusStatuses", "", Constants.MOD_CONFIG, "Bonusove statusy pre objednávku v sekcií Elektonický obchod. Formát: ID_STATUSU|PREKALDOVÝ_KĽÚČ\nID_STATUSU|PREKALDOVÝ_KĽÚČ. ID_STATUSU musí byť číslo väčšie rovné 10.");

		Constants.setBoolean("ABTestingAllowVariantUrl", false, "abtest", "Nastavenim na true povoli priame zobrazenie variantnej URL adresy aj ne-administratorom, napr. volanie /investicie/abtestvariantb.html");

		Constants.setBoolean("forumAlwaysNotifyPageAuthor", false, Constants.MOD_CONFIG, "Pri nastaveni na true sa budú odosielať notifikácie autorovi stránky s Diskusiou, vždy keď bude pridaná nová téma alebo odpoveď.");

		Constants.setLong("licenseExpiryDate", 0L, Constants.MOD_STAT, "Datum exspiracie licencie v milisekundach");

		Constants.setString("propertiesRestControllerAllowedKeysPrefixes", "", Constants.MOD_CONFIG, "Povolene prefixi prekladových kľúčov");

		Constants.setBoolean("monitoringEnableCountUsersOnAllNodes", true, "server_monitoring;performance", "Ak verejne nody nedokazu zapisovat do tabuly _conf_ nastavte na false pre vypnutie zapisu celkove poctu sessions");

		Constants.setBoolean("webpagesNotifyAutorOnPublish", true, Constants.MOD_CONFIG, "Nastavenie odosielania notifikacie autorovi pri publikovani stranky.");

		Constants.setInt("perexGroupsRenderAsSelect", 30, Constants.MOD_EDITOR, "Počet perex skupín pri ktorom sa perex začne zobrazovať ako multiselect");

		Constants.setString("changePasswordPageUrlAdmin", "/admin/logon/changePassword", Constants.MOD_USER, "cesta k suboru (alebo stranke) na zmenu hesla z admin sekcie");

		Constants.setInt("dashboardRecentSize", 8, "dashboard", "Pocet zaznamov v zozname poslednych stranok/auditu na uvodnej obrazovke");

		Constants.setInt("contentBlockTypeCount", 5, "contentBlock", "Pocet dostupnych typov blokov pre obsah");

		Constants.setBoolean("elfinderMoveConfirm", true, "files", "Zobrazi potvrdzovaci dialog pri presune priecinkov a suborov");

		Constants.setString("editorFontAwesomeCssPath", "", Constants.MOD_EDITOR, "Cesta k CSS suborom s fontawesome ikonami, ak je CSS suborov viac oddelenych znakom ciarka alebo novy riadok");
		Constants.setString("editorFontAwesomeCustomIcons", "", Constants.MOD_EDITOR, "Doplnkove FontAwesome ikony, kazda na novom riadku vo formate cssStyl:nazov");

		Constants.setString("stripes.FormatterFactory.Class", "sk.iway.iwcm.system.stripes.StripesFormatterFactory", Constants.MOD_CONFIG, "Stripes custom FormatterFactory to use WebJET Tools.formatDate/Time formatters");

		Constants.setString("springSecurityAllowedAuths", "basic,api-token", Constants.MOD_SECURITY, "Allowed auth methods for REST services, after change restart server.");
		Constants.setBoolean("logoffRequireCsrfToken", false, Constants.MOD_SECURITY, "If true, /logoff.do requires CSRF token");
		Constants.setString("csrfRequiredUrls", "", Constants.MOD_SECURITY, "Comma separated list of URLs that require CSRF token");

		Constants.setString("reservationAllDayStartTime", "14:00", "reservations", "Hodina, od ktorej sa začína celodenná rezervácia");
		Constants.setString("reservationAllDayEndTime", "10:30", "reservations", "Hodina, do ktorej sa končí celodenná rezervácia");

		Constants.setBoolean("virtualPathLastSlash", true, Constants.MOD_CONFIG, "If true, virtual path will have last slash for main pages in folder");

		Constants.setString("defaultSenderName", "", Constants.mods(Constants.MOD_DMAIL, Constants.MOD_RESERVATION, Constants.MOD_FORMMAIL), "If set, it will be used as sender name. For modules dmail,reservation,formmail you can specify custom value by prefix. e.g. dmailDefaultSenderName.");
		Constants.setString("defaultSenderEmail", "", Constants.mods(Constants.MOD_DMAIL, Constants.MOD_RESERVATION, Constants.MOD_FORMMAIL), "If set, it will be used as sender email. For modules dmail,reservation,formmail you can specify custom value by prefix. e.g. dmailDefaultSenderEmail.");

		Constants.setInt("sortPriorityIncrementGroup", 10, Constants.MOD_EDITOR, "Increment for sort priority for groups");
		Constants.setInt("sortPriorityIncrementDoc", 10, Constants.MOD_EDITOR, "Increment for sort priority for pages");

		Constants.setBoolean("loggerUseAnsiColors", false, "adminlog", "If true, logger will use ANSI colors for console output");
		Constants.setBoolean("fileArchivAllowPatternVersion", true, Constants.MOD_FILE_ARCHIV, "Ak je povolene (true), manažér dokumentov povolí vytváranie verzií pre súbory typu VZOR.");
		Constants.setString("basketInvoiceSupportedCountries", ".sk,.cz,.pl", Constants.MOD_BASKET, "Which countries are supported for delivery. Format is TLD: .sk,.cz,.pl");

		Constants.setString("basketNewCategoryHtmlCode", "!INCLUDE(/components/basket/bootstrap_products.jsp, style=01 ,groupIds=, orderType=priority, asc=yes, publishType=all, paging=yes, pageSize=15,pagingPosition=both, thumbWidth=190, thumbHeight=190, showCategory=yes, showSort=yes, testRun=no, katalogProduktov=no, overeneZakaznikmi=)!", Constants.MOD_BASKET, "HTML kód pre novú kategóriu v košíku.");

		Constants.setString("translationEngineClasses", "sk.iway.iwcm.system.translation.DeepL", "translations", "Čiarkou oddelený zoznam tried, ktoré implemetujú prekaldový modul (napr. DeepL alebo Google)");
		Constants.setString("deepl_model_type", "prefer_quality_optimized", "translations", "Špecifikuje, ktorý DeepL model by sa mal použiť na preklad.");

		Constants.setString("mirroringMode", "mirror", "structuremirroring", "Used to signalize that mirroring is running in basic mode mirroring or clonning");
		Constants.setInt("cloneActionSrcId", -1, Constants.MOD_CONFIG,"Used to set SRC group id when performing clonning.");
		Constants.setInt("cloneActionDestId", -1, Constants.MOD_CONFIG, "Used to set DEST group id when performing clonning.");

		Constants.setBoolean("ABTestingForLoggedUser", false, "abtest", "Ak je nastavené na true, tak sa pre neprihláseného používateľa vždy použije variant A, pre prihláseného vždy variant B.");

		Constants.setString("ai_openAiAuthKey", "", Constants.MOD_AI_ASSISTANTS, "Authentication key to platform.openai.com");
		Constants.setBoolean("ai_browserAiEnabled", true, Constants.MOD_AI_ASSISTANTS, "Enable AI assistants in browser - Chrome Built-in AI");
		Constants.setString("ai_geminiAuthKey", "", Constants.MOD_AI_ASSISTANTS, "Authentifikacny kluc k aistudio.google.com");
		Constants.setString("ai_openRouterAuthKey", "", Constants.MOD_AI_ASSISTANTS, "Authentifikacny kluc k openrouter.ai");
		Constants.setString("ai_generateFileNamePrompt", "Generate VERY short, filesystem-safe name for file (lowercase, hyphens). Try generate name as short as possible but meningfull to USER. Do not add explanations, return ONLY generated name.", Constants.MOD_AI_ASSISTANTS, "");
	}

	/**
	 * Returns coma separated value as Array
	 * @param name
	 * @return
	 * @deprecated use Constants.getArray(name)
	 */
	@Deprecated
	public static String[] getArray(String name) {
		return Constants.getArray(name);
	}

	/**
	 * Returns coma separated value as Array and cache it for faster access
	 * @param name
	 * @param cacheMinutes
	 * @return
	 */
	public static String[] getArrayCached(String name, int cacheMinutes) {
		String CACHE_KEY = name+".arrayCache-"+cacheMinutes;
		Cache c = Cache.getInstance();
		String[] arr = (String[])c.getObject(CACHE_KEY);
		if (arr == null) {
			arr = Constants.getArray(name);
			c.setObject(CACHE_KEY, arr, cacheMinutes);
		}
		return arr;
	}

	/**
	 * Returns config list with name starts with prefix
	 * @param prefix
	 * @return
	 */
	public static List<ConfDetails> getValuesStartsWith(String prefix) {
		List<ConfDetails> filtered = new ArrayList<>();
		for (String key : Constants.getAllKeys()) {
			if (key!=null && key.startsWith(prefix)) {
				ConfDetails conf = new ConfDetails();
				conf.setName(key);
				conf.setValue(Constants.getString(key));
				filtered.add(conf);
			}
		}
		return filtered;
	}

}
