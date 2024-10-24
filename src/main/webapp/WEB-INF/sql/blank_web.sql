#
# Table structure for table '_conf_'
#

CREATE TABLE _conf_ (
  name varchar(255) NOT NULL default '',
  value varchar(255) NOT NULL default '',
  UNIQUE KEY name (name)
) ENGINE=InnoDB;



#
# Dumping data for table '_conf_'
#
INSERT INTO _conf_ VALUES ('defaultDisableUpload','false');
INSERT INTO _conf_ VALUES ('showDocActionAllowedDocids','4');
INSERT INTO _conf_ VALUES ('inlineEditingEnabled','true');
INSERT INTO _conf_ VALUES ('disableWebJETToolbar','true');
INSERT INTO _conf_ VALUES ('logLevel','debug');

#
# Table structure for table '_db_'
#

CREATE TABLE _db_ (
  id int(4) unsigned NOT NULL auto_increment,
  create_date date NOT NULL default '2000-01-01',
  note varchar(255) NOT NULL default '',
  KEY id (id)
) ENGINE=InnoDB;



#
# Dumping data for table '_db_'
#

INSERT INTO _db_ VALUES("1", "2003-12-06", "ukladanie poznamky a prihlaseneho usera k formularom");
INSERT INTO _db_ VALUES("2", "2003-12-12", "sposob zobrazenia menu pre adresar");
INSERT INTO _db_ VALUES("3", "2003-12-21", "atributy suborov");
INSERT INTO _db_ VALUES("4", "2003-12-29", "od teraz sa kontroluje aj admin, ci je autorizovany, takze nastavime default");
INSERT INTO _db_ VALUES("5", "2004-01-04", "uklada k formularu aj docid (ak sa podari zistit)");
INSERT INTO _db_ VALUES("6", "2004-01-09", "typ skupiny pouzivatelov, 0=perms, 1=email, 2=...");
INSERT INTO _db_ VALUES("7", "2004-01-10", "email je mozne posielat uz len ako URL, text sa priamo napisat neda");
INSERT INTO _db_ VALUES("8", "2004-01-11", "verifikacia subscribe k email newslettrom, po autorizacii emailom sa user_groups zapise do tabulky users");
INSERT INTO _db_ VALUES("9", "2004-01-13", "zoznam foldrov (/images/nieco...) do ktorych ma user pravo nahravat obrazky a subory");
INSERT INTO _db_ VALUES("10", "2004-01-25", "volne pouzitelne polia pre kalendar podujati");
INSERT INTO _db_ VALUES("11", "2004-02-11", "casova notifikacia pre kalendar podujati");
INSERT INTO _db_ VALUES("12", "2004-02-15", "virtualne cesty k strankam, napr. www.server.sk/products");
INSERT INTO _db_ VALUES("13", "2004-02-17", "uvodny text notifikacie kalendara, moznost poslat SMS");
INSERT INTO _db_ VALUES("14", "2004-02-24", "ak je true, dava navstevnik suhlas na zobrazenie na webe");
INSERT INTO _db_ VALUES("15", "2004-02-28", "urychlenie statistiky");
INSERT INTO _db_ VALUES("16", "2004-01-03", "zvacsenie poli");
INSERT INTO _db_ VALUES("17", "2004-03-03", "urychlenie nacitania virtual paths");
INSERT INTO _db_ VALUES("18", "2004-03-05", "konfiguracia webjetu (namiesto web.xml)");
INSERT INTO _db_ VALUES("19", "2004-03-07", "disabled items pouzivatelov");
INSERT INTO _db_ VALUES("20", "2004-03-07", "rozdelenie full name na meno a priezvisko");
INSERT INTO _db_ VALUES("21", "2004-03-08", "volne pouzitelne polozky");
INSERT INTO _db_ VALUES("22", "2004-03-12", "url nazov adresara");
INSERT INTO _db_ VALUES("23", "2004-03-15", "implemetacia rozdelenia full name");
INSERT INTO _db_ VALUES("24", "2004-03-15", "Konverzia pristupovych prav");
INSERT INTO _db_ VALUES("25", "2004-03-18", "custom zmena textov v properties suboroch");
INSERT INTO _db_ VALUES("26", "2004-03-27", "uprava statistik (eviduje sa id adresara)");
INSERT INTO _db_ VALUES("27", "2004-03-28", "statistika query vo vyhladavacoch");
INSERT INTO _db_ VALUES("28", "2004-04-05", "mod schvalovania adresara (0=approve, 1=notify, 2=none)");
INSERT INTO _db_ VALUES("29", "2004-03-05", "konfiguracia webjetu (namiesto web.xml)");
INSERT INTO _db_ VALUES("30", "2004-03-07", "disabled items pouzivatelov");
INSERT INTO _db_ VALUES("31", "2004-03-07", "rozdelenie full name na meno a priezvisko");
INSERT INTO _db_ VALUES("32", "2004-03-08", "volne pouzitelne polozky");
INSERT INTO _db_ VALUES("33", "2004-03-12", "url nazov adresara");
INSERT INTO _db_ VALUES("34", "2004-03-18", "custom zmena textov v properties suboroch");
INSERT INTO _db_ VALUES("35", "2004-03-27", "uprava statistik (eviduje sa id adresara)");
INSERT INTO _db_ VALUES("36", "2004-03-28", "statistika query vo vyhladavacoch");
INSERT INTO _db_ VALUES("37", "2004-04-05", "mod schvalovania adresara (0=approve, 1=notify, 2=none)");
INSERT INTO _db_ VALUES("38", "2004-05-01", "id a stav synchronizacie (status: 0=novy, 1=updated, 2=synchronized)");
INSERT INTO _db_ VALUES("39", "2004-05-02", "konfiguracia custom modulov");
INSERT INTO _db_ VALUES("40", "2004-05-03", "modul posielania SMS sprav");
INSERT INTO _db_ VALUES("41", "2004-05-09", "vyzadovanie schvalovania registracie, doc_id pre zasielany email");


#
# Table structure for table '_modules_'
#

CREATE TABLE _modules_ (
  module_id int(4) unsigned NOT NULL auto_increment,
  name_key varchar(128) NOT NULL default '',
  item_key varchar(64) NOT NULL default '',
  path varchar(255) NOT NULL default '',
  UNIQUE KEY module_id (module_id),
  KEY module_id_2 (module_id)
) ENGINE=InnoDB;



#
# Dumping data for table '_modules_'
#



#
# Table structure for table '_properties_'
#

CREATE TABLE _properties_ (
  prop_key varchar(255) NOT NULL default '',
  lng char(3) NOT NULL default '',
  prop_value varchar(255) NOT NULL default '',
  UNIQUE KEY prop_key (prop_key,lng),
  KEY prop_key_2 (prop_key,lng)
) ENGINE=InnoDB;



#
# Dumping data for table '_properties_'
#



#
# Table structure for table 'calendar'
#

CREATE TABLE calendar (
  calendar_id int(11) NOT NULL auto_increment,
  title text NOT NULL,
  description text,
  date_from datetime NOT NULL default '2000-01-01 00:00:00',
  date_to datetime NOT NULL default '2000-01-01 00:00:00',
  type_id int(3) NOT NULL default '0',
  time_range varchar(128) default NULL,
  area varchar(255) default NULL,
  city varchar(255) default NULL,
  address varchar(255) default NULL,
  info_1 varchar(255) default NULL,
  info_2 varchar(255) default NULL,
  info_3 varchar(255) default NULL,
  info_4 varchar(255) default NULL,
  info_5 varchar(255) default NULL,
  notify_hours_before int(4) default '0',
  notify_emails text,
  notify_sender varchar(255) default NULL,
  notify_sent tinyint(1) unsigned NOT NULL default '0',
  notify_introtext text,
  notify_sendsms tinyint(1) unsigned default '0',
  lng char(3) default NULL,
  KEY calendar_id (calendar_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'calendar'
#



#
# Table structure for table 'calendar_types'
#

CREATE TABLE calendar_types (
  type_id int(3) unsigned NOT NULL auto_increment,
  name varchar(128) NOT NULL default '',
  PRIMARY KEY  (type_id),
  KEY type_id (type_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'calendar_types'
#

INSERT INTO calendar_types VALUES("1", "Výstava");
INSERT INTO calendar_types VALUES("2", "Šport");
INSERT INTO calendar_types VALUES("3", "Kultúra");
INSERT INTO calendar_types VALUES("4", "Rodina");
INSERT INTO calendar_types VALUES("5", "Konferencia");


#
# Table structure for table 'doc_atr'
#

CREATE TABLE doc_atr (
  doc_id int(4) unsigned NOT NULL default '0',
  atr_id int(4) unsigned NOT NULL default '0',
  value_string varchar(255) default NULL,
  value_int int(4) unsigned default NULL,
  value_bool tinyint(1) unsigned default '0',
  PRIMARY KEY  (doc_id,atr_id),
  KEY doc_id (doc_id,atr_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'doc_atr'
#



#
# Table structure for table 'doc_atr_def'
#

CREATE TABLE doc_atr_def (
  atr_id int(4) unsigned NOT NULL auto_increment,
  atr_name varchar(32) NOT NULL default '',
  order_priority int(4) default '10',
  atr_description varchar(255) default NULL,
  atr_default_value varchar(255) default NULL,
  atr_type tinyint(3) unsigned NOT NULL default '0',
  atr_group varchar(32) default 'default',
  true_value varchar(255) default NULL,
  false_value varchar(255) default NULL,
  PRIMARY KEY  (atr_id),
  KEY atr_id (atr_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'doc_atr_def'
#



#
# Table structure for table 'document_forum'
#

CREATE TABLE document_forum (
  forum_id int(11) unsigned NOT NULL auto_increment,
  doc_id int(11) unsigned NOT NULL default '0',
  parent_id int(11) NOT NULL default '-1',
  subject varchar(255) default NULL,
  question text,
  question_date datetime default NULL,
  author_name varchar(255) default NULL,
  author_email varchar(255) default NULL,
  ip varchar(255) default NULL,
  PRIMARY KEY  (forum_id),
  KEY forum_id (forum_id)
) ENGINE=InnoDB COMMENT='diskusne forum';



#
# Dumping data for table 'document_forum'
#



#
# Table structure for table 'documents'
#

CREATE TABLE documents (
  doc_id int(11) NOT NULL auto_increment,
  title varchar(255) NOT NULL default '',
  data mediumtext NOT NULL,
  data_asc mediumtext NOT NULL,
  external_link varchar(255) default NULL,
  navbar text NOT NULL,
  date_created datetime NOT NULL default '2000-01-01 00:00:00',
  publish_start datetime default NULL,
  publish_end datetime default NULL,
  author_id int(11) NOT NULL default '0',
  group_id int(11) default NULL,
  temp_id int(11) NOT NULL default '0',
  views_total int(11) NOT NULL default '0',
  views_month int(11) NOT NULL default '0',
  searchable tinyint(1) NOT NULL default '0',
  available tinyint(1) NOT NULL default '0',
  cacheable tinyint(1) NOT NULL default '0',
  file_name varchar(255) default NULL,
  file_change datetime default NULL,
  sort_priority int(11) NOT NULL default '0',
  header_doc_id int(11) default NULL,
  menu_doc_id int(11) NOT NULL default '-1',
  footer_doc_id int(11) NOT NULL default '-1',
  password_protected varchar(255) default NULL,
  html_head text,
  html_data text,
  perex_place varchar(255) default NULL,
  perex_image varchar(255) default NULL,
  perex_group varchar(255) default NULL,
  show_in_menu tinyint(1) unsigned default '1',
  event_date datetime default NULL,
  virtual_path varchar(255) default NULL,
  sync_id int(11) default '0',
  sync_status int(11) default '0',
  PRIMARY KEY  (doc_id),
  KEY i_group_id (group_id),
  FULLTEXT KEY search (title,data_asc)
) ENGINE=InnoDB;



#
# Dumping data for table 'documents'
#

INSERT INTO documents VALUES("1", "Default header", "<h1>Headline</h1><p>Lorem ipsum dolor sit amet, consectetur adipisicing elit. Sit aspernatur labore vel rem adipisci commodi odit eum vitae asperiores esse?</p><a href='#' class='btn btn-default'>Lorem ipsum.</a>", "", "", "Default hlavička", "2002-07-25 00:00:00", NULL, NULL, "1", "4", "1", "0", "0", "0", "1", "0", "1_default_hlavicka.html", "2003-04-11 15:51:10", "50", "-1", "-1", "-1", NULL, NULL, NULL, NULL, NULL, NULL, "1", NULL, NULL, "0", "0");
INSERT INTO documents VALUES("2", "Default navigation", "!INCLUDE(/components/menu/menu_ul_li.jsp, rootGroupId=1, startOffset=0, maxLevel=3, classes=basic, openAllItems=true, rootUlId=mainNavigation)!", "", "", "Default menu", "2003-05-23 00:00:00", NULL, NULL, "1", "3", "1", "0", "0", "0", "1", "0", "2_default_menu.html", "2003-08-27 10:36:59", "50", "-1", "-1", "-1", NULL, "", "", NULL, NULL, NULL, "1", NULL, NULL, "0", "0");
INSERT INTO documents VALUES("3", "Default footer", "<p>© !YEAR! InterWay, a. s. All Rights Reserved. Page generated by WebJET CMS.</p>", "", "", "Default pätička", "2002-07-25 00:00:00", NULL, NULL, "1", "4", "1", "0", "0", "0", "1", "0", "3_default_paticka.html", "2003-04-11 15:51:10", "50", "-1", "-1", "-1", NULL, NULL, NULL, NULL, NULL, NULL, "1", NULL, NULL, "0", "0");
INSERT INTO documents VALUES("4", "Hlavna stranka", "<p>Vitajte na demo stránke systému WebJET.</p>\r\n<p>Viac informácií o systéme nájdete na stránke <a href='http://www.webjetcms.sk/' target='_blank'>www.webjetcms.sk</a>.</p>", "<p>vitajte na demo stranke systemu webjet.</p>\r\n<p>viac informacii o systeme najdete na stranke <a href='http://www.webjetcms.sk/' target='_blank'>www.webjetcms.sk</a>.</p>", "", "Hlavna stranka", "2003-10-05 22:43:25", NULL, NULL, "1", "1", "3", "0", "0", "1", "1", "0", "4_hlavna_stranka.html", "2003-10-05 22:43:25", "1", "-1", "-1", "-1", NULL, "", NULL, "", "", NULL, "1", NULL, NULL, "0", "0");
INSERT INTO documents VALUES("5", "Podstránka 1", "<p>Toto je podstránka 1</p>\r\n<p>Ak chcete do stránky vložiť predpripravený objekt, kliknite v editore na komponenty a vyberte Predpripravené HTML. Tam si vyberte stránku a kliknutím na OK ju vložte do stránky. Nové predpripravené objekty si môžete vytvoriť v adresári System->HTMLBox.</p>\r\n<p>Pred použitím formuláru prosím najskôr naň kliknite pravým tlačítkom, dajte vlastnosti formuláru a zmeňte emailovú adresu.</p>\r\n<p>\r\n<script src='/components/form/check_form.js' ENGINE='text/javascript'></script>\r\n<script src='/components/form/fix_e.js' ENGINE='text/javascript'></script>\r\n<script src='/components/form/event_attacher.js' ENGINE='text/javascript'></script>\r\n<script src='/components/form/class_magic.js' ENGINE='text/javascript'></script>\r\n<script src='/components/form/check_form_impl.jsp' ENGINE='text/javascript'></script>\r\n<link media='screen' href='/components/form/check_form.css' ENGINE='text/css' rel='stylesheet'/>\r\n<form name='formMailForm' action='/formmail.do?recipients=test@tester.org&savedb=Kontaktny_formular' method='post'>\r\n    <table cellspacing='0' cellpadding='0' border='0'>\r\n        <tbody>\r\n            <tr>\r\n                <td>Vaše meno: </td>\r\n                <td><input class='required invalid' maxlength='255' name='meno'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> Vaša emailová adresa:</td>\r\n                <td><input class='required email invalid' maxlength='255' name='email'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td valign='top'> Otázka / pripomienka:</td>\r\n                <td><textarea class='required invalid' name='otazka' rows='5' cols='40'></textarea> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> </td>\r\n                <td align='right'> <input ENGINE='submit' name='btnSubmit' value='Odoslať'/></td>\r\n            </tr>\r\n        </tbody>\r\n    </table>\r\n</form>\r\n</p>\r\n<p> </p>", "<p>toto je podstranka 1</p>\r\n<p>ak chcete do stranky vlozit predpripraveny objekt, kliknite v editore na komponenty a vyberte predpripravene html. tam si vyberte stranku a kliknutim na ok ju vlozte do stranky. nove predpripravene objekty si mozete vytvorit v adresari system->htmlbox.</p>\r\n<p>pred pouzitim formularu prosim najskor nan kliknite pravym tlacitkom, dajte vlastnosti formularu a zmente emailovu adresu.</p>\r\n<p>\r\n<script src='/components/form/check_form.js' ENGINE='text/javascript'></script>\r\n<script src='/components/form/fix_e.js' ENGINE='text/javascript'></script>\r\n<script src='/components/form/event_attacher.js' ENGINE='text/javascript'></script>\r\n<script src='/components/form/class_magic.js' ENGINE='text/javascript'></script>\r\n<script src='/components/form/check_form_impl.jsp' ENGINE='text/javascript'></script>\r\n<link media='screen' href='/components/form/check_form.css' ENGINE='text/css' rel='stylesheet'/>\r\n<form name='formmailform' action='/formmail.do?recipients=test@tester.org&savedb=kontaktny_formular' method='post'>\r\n    <table cellspacing='0' cellpadding='0' border='0'>\r\n        <tbody>\r\n            <tr>\r\n                <td>vase meno: </td>\r\n                <td><input class='required invalid' maxlength='255' name='meno'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> vasa emailova adresa:</td>\r\n                <td><input class='required email invalid' maxlength='255' name='email'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td valign='top'> otazka / pripomienka:</td>\r\n                <td><textarea class='required invalid' name='otazka' rows='5' cols='40'></textarea> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> </td>\r\n                <td align='right'> <input ENGINE='submit' name='btnsubmit' value='odoslat'/></td>\r\n            </tr>\r\n        </tbody>\r\n    </table>\r\n</form>\r\n</p>\r\n<p> </p>", "", "Podstránka 1", "2003-10-05 22:43:25", NULL, NULL, "1", "1", "3", "0", "0", "1", "1", "0", "5_podstranka_1.html", "2003-10-05 22:43:25", "1", "-1", "-1", "-1", NULL, "", NULL, "", "", NULL, "1", NULL, NULL, "0", "0");
INSERT INTO documents VALUES("6", "Podstránka 2", "<h1>Podstránka 2</h1><p>Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi id sapien ut massa interdum ultricies. Sed eget enim in ante ornare feugiat. Curabitur risus lectus, iaculis sed, pulvinar quis, convallis euismod, massa. Curabitur est enim, varius sed, hendrerit at, convallis elementum, tortor. In hac habitasse platea dictumst. Proin sagittis massa ac massa. Maecenas vel libero. Curabitur vestibulum pellentesque elit. Phasellus aliquet quam quis urna. In ultrices est vel lorem. Aliquam sit amet mi et nulla scelerisque vestibulum. Donec pellentesque tellus vitae massa. Curabitur euismod. Donec quis pede. Vivamus nulla mauris, aliquet sed, aliquet vitae, fringilla id, massa. Etiam id magna sed dolor rhoncus condimentum. Ut lacinia nonummy odio. Pellentesque interdum, tortor vitae congue elementum, nibh ipsum pellentesque quam, sed tempor tortor est sit amet felis. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nullam sagittis tellus vitae lorem.</p>", "<h1>podstranka 2</h1><p>Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi id sapien ut massa interdum ultricies. Sed eget enim in ante ornare feugiat. Curabitur risus lectus, iaculis sed, pulvinar quis, convallis euismod, massa. Curabitur est enim, varius sed, hendrerit at, convallis elementum, tortor. In hac habitasse platea dictumst. Proin sagittis massa ac massa. Maecenas vel libero. Curabitur vestibulum pellentesque elit. Phasellus aliquet quam quis urna. In ultrices est vel lorem. Aliquam sit amet mi et nulla scelerisque vestibulum. Donec pellentesque tellus vitae massa. Curabitur euismod. Donec quis pede. Vivamus nulla mauris, aliquet sed, aliquet vitae, fringilla id, massa. Etiam id magna sed dolor rhoncus condimentum. Ut lacinia nonummy odio. Pellentesque interdum, tortor vitae congue elementum, nibh ipsum pellentesque quam, sed tempor tortor est sit amet felis. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nullam sagittis tellus vitae lorem.</p>", "", "Podstránka 2", "2003-10-05 22:43:25", NULL, NULL, "1", "1", "3", "0", "0", "1", "1", "0", "6_podstranka_2.html", "2003-10-05 22:43:25", "1", "-1", "-1", "-1", NULL, "", NULL, "", "", NULL, "1", NULL, NULL, "0", "0");
INSERT INTO documents VALUES("7", "Stranka s nadpisom a 2 stlpcami", "<table cellspacing='0' cellpadding='2' width='100%' border='0'>\r\n    <tbody>\r\n        <tr>\r\n            <td colspan='2'>\r\n            <h1>Toto je nadpis stránky</h1>\r\n            </td>\r\n        </tr>\r\n        <tr>\r\n            <td valign='top' width='50%'>\r\n            <p> Toto je stlpec 1</p>\r\n            <p>Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Nam pulvinar sollicitudin est. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae. Duis nulla risus, varius non, condimentum nec, adipiscing vel, nisl. Vestibulum facilisis lorem. Nam tortor tellus, venenatis vitae, tincidunt in, fringilla ut, arcu. Suspendisse imperdiet magna egestas nibh. Sed rutrum. Nulla pellentesque mollis leo. Mauris vel metus eget dolor feugiat consequat. Nam dapibus dapibus felis. Phasellus sit amet tortor vel ante dictum aliquam. Integer vehicula nisi et quam euismod commodo. Nam vel justo. Sed mattis libero non enim. Donec feugiat tortor. Quisque mauris. Nullam urna turpis, aliquam sit amet, posuere eget, consectetuer nec, massa. In non mauris.</p>\r\n            </td>\r\n            <td valign='top' width='50%'>\r\n            <p> Toto je stlpec 2</p>\r\n            <p>Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Suspendisse in libero ac turpis porttitor porta. Aliquam varius massa vitae massa. Pellentesque fringilla diam vitae velit. Nulla facilisi. Sed id enim. Aenean in urna. Vivamus sed urna at elit porttitor ultrices. Morbi dolor felis, facilisis at, congue quis, convallis et, ipsum. Donec rutrum nulla luctus arcu luctus blandit. Mauris vitae ipsum et risus venenatis scelerisque. Duis ipsum. Donec viverra purus. Fusce non libero. Cras elementum. Curabitur fermentum elit at lorem. Etiam facilisis. Pellentesque nec erat ut ipsum consectetuer sodales. Morbi nec dolor ac velit rutrum faucibus. </p>\r\n            </td>\r\n        </tr>\r\n    </tbody>\r\n</table>", "<table cellspacing='0' cellpadding='2' width='100%' border='0'>\r\n    <tbody>\r\n        <tr>\r\n            <td colspan='2'>\r\n            <h1>toto je nadpis stranky</h1>\r\n            </td>\r\n        </tr>\r\n        <tr>\r\n            <td valign='top' width='50%'>\r\n            <p> toto je stlpec 1</p>\r\n            <p>lorem ipsum dolor sit amet, consectetuer adipiscing elit. nam pulvinar sollicitudin est. vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae. duis nulla risus, varius non, condimentum nec, adipiscing vel, nisl. vestibulum facilisis lorem. nam tortor tellus, venenatis vitae, tincidunt in, fringilla ut, arcu. suspendisse imperdiet magna egestas nibh. sed rutrum. nulla pellentesque mollis leo. mauris vel metus eget dolor feugiat consequat. nam dapibus dapibus felis. phasellus sit amet tortor vel ante dictum aliquam. integer vehicula nisi et quam euismod commodo. nam vel justo. sed mattis libero non enim. donec feugiat tortor. quisque mauris. nullam urna turpis, aliquam sit amet, posuere eget, consectetuer nec, massa. in non mauris.</p>\r\n            </td>\r\n            <td valign='top' width='50%'>\r\n            <p> toto je stlpec 2</p>\r\n            <p>lorem ipsum dolor sit amet, consectetuer adipiscing elit. suspendisse in libero ac turpis porttitor porta. aliquam varius massa vitae massa. pellentesque fringilla diam vitae velit. nulla facilisi. sed id enim. aenean in urna. vivamus sed urna at elit porttitor ultrices. morbi dolor felis, facilisis at, congue quis, convallis et, ipsum. donec rutrum nulla luctus arcu luctus blandit. mauris vitae ipsum et risus venenatis scelerisque. duis ipsum. donec viverra purus. fusce non libero. cras elementum. curabitur fermentum elit at lorem. etiam facilisis. pellentesque nec erat ut ipsum consectetuer sodales. morbi nec dolor ac velit rutrum faucibus. </p>\r\n            </td>\r\n        </tr>\r\n    </tbody>\r\n</table>", "", "Stranka s nadpisom a 2 stlpcami", "2003-10-05 22:43:25", NULL, NULL, "1", "2", "3", "0", "0", "1", "1", "0", "7_s_nadpisom.html", "2003-10-05 22:43:25", "1", "-1", "-1", "-1", NULL, "", NULL, "", "", NULL, "1", NULL, NULL, "0", "0");
INSERT INTO documents VALUES("8", "Kontaktný formulár", "<p>Pred použitím formuláru prosím najskôr naň kliknite pravým tlačítkom, dajte vlastnosti formuláru a zmeňte emailovú adresu.</p>\r\n<p>\r\n<script src='/components/form/check_form.js' ENGINE='text/javascript'></script>\r\n<form name='formMailForm' action='/formmail.do?recipients=test@tester.org&savedb=Kontaktny_formular' method='post'>\r\n    <table cellspacing='0' cellpadding='0' border='0'>\r\n        <tbody>\r\n            <tr>\r\n                <td>Vaše meno:</td>\r\n                <td><input class='required' maxlength='255' name='meno'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> Vaša emailová adresa:</td>\r\n                <td><input class='required email' maxlength='255' name='email'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td valign='top'> Otázka / pripomienka:</td>\r\n                <td><textarea class='required' name='otazka' rows='5' cols='40'></textarea> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> </td>\r\n                <td align='right'> <input ENGINE='submit' name='btnSubmit' value='Odoslať'/></td>\r\n            </tr>\r\n        </tbody>\r\n    </table>\r\n</form>\r\n</p>\r\n<p> </p>", "<p>pred pouzitim formularu prosim najskor nan kliknite pravym tlacitkom, dajte vlastnosti formularu a zmente emailovu adresu.</p>\r\n<p>\r\n<script src='/components/form/check_form.js' ENGINE='text/javascript'></script>\r\n<form name='formmailform' action='/formmail.do?recipients=test@tester.org&savedb=kontaktny_formular' method='post'>\r\n    <table cellspacing='0' cellpadding='0' border='0'>\r\n        <tbody>\r\n            <tr>\r\n                <td>vase meno: </td>\r\n                <td><input class='required' maxlength='255' name='meno'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> vasa emailova adresa:</td>\r\n                <td><input class='required email' maxlength='255' name='email'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td valign='top'> otazka / pripomienka:</td>\r\n                <td><textarea class='required' name='otazka' rows='5' cols='40'></textarea> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> </td>\r\n                <td align='right'> <input ENGINE='submit' name='btnsubmit' value='odoslat'/></td>\r\n            </tr>\r\n        </tbody>\r\n    </table>\r\n</form>\r\n</p>\r\n<p> </p>", "", "Kontaktný formulár", "2003-10-05 22:43:25", NULL, NULL, "1", "21", "3", "0", "0", "1", "1", "0", "8_kontakt.html", "2003-10-05 22:43:25", "1", "-1", "-1", "-1", NULL, "", NULL, "", "", NULL, "1", NULL, NULL, "0", "0");
INSERT INTO documents VALUES("9", "Default left menu", "!INCLUDE(/components/menu/menu_ul_li.jsp, rootGroupId=1, startOffset=1, maxLevel=3, classes=basic, openAllItems=true, rootUlId=mainNavigation)!", "", "", "Default menu", "2003-05-23 00:00:00", NULL, NULL, "1", "3", "1", "0", "0", "0", "1", "0", "9_default_menu.html", "2003-08-27 10:36:59", "50", "-1", "-1", "-1", NULL, "", "", NULL, NULL, NULL, "1", NULL, NULL, "0", "0");

# INSERT INTO documents VALUES("docid", "Hlavna stranka", "data", "data asc", "", "Hlavna stranka", "2003-10-05 22:43:25", NULL, NULL, "1", "1", "3", "0", "0", "1", "1", "0", "4_hlavna_stranka.html", "2003-10-05 22:43:25", "1", "-1", "-1", "-1", NULL, "", NULL, "", "", NULL, "1", NULL, NULL, "0", "0");

update documents set temp_id=1;

#
# Table structure for table 'documents_history'
#

CREATE TABLE documents_history (
  history_id int(11) NOT NULL auto_increment,
  save_date datetime default NULL,
  approved_by int(11) NOT NULL default '-1',
  awaiting_approve varchar(255) default NULL,
  actual tinyint(1) NOT NULL default '0',
  doc_id int(11) NOT NULL default '0',
  title varchar(255) NOT NULL default '',
  data mediumtext NOT NULL,
  data_asc mediumtext NOT NULL,
  external_link varchar(255) default NULL,
  navbar text NOT NULL,
  date_created datetime NOT NULL default '2000-01-01 00:00:00',
  publish_start datetime default NULL,
  publish_end datetime default NULL,
  author_id int(11) NOT NULL default '0',
  group_id int(11) default NULL,
  temp_id int(11) NOT NULL default '0',
  views_total int(11) NOT NULL default '0',
  views_month int(11) NOT NULL default '0',
  searchable tinyint(1) NOT NULL default '0',
  available tinyint(1) NOT NULL default '0',
  cacheable tinyint(1) NOT NULL default '0',
  file_name varchar(255) default NULL,
  file_change datetime default NULL,
  sort_priority int(11) NOT NULL default '0',
  header_doc_id int(11) default NULL,
  footer_doc_id int(11) default NULL,
  menu_doc_id int(11) NOT NULL default '-1',
  password_protected varchar(255) default NULL,
  html_head text,
  html_data text,
  publicable tinyint(1) unsigned default '0',
  perex_place varchar(255) default NULL,
  perex_image varchar(255) default NULL,
  perex_group varchar(255) default NULL,
  show_in_menu tinyint(1) unsigned default '1',
  event_date datetime default NULL,
  virtual_path varchar(255) default NULL,
  sync_id int(11) default '0',
  sync_status int(11) default '0',
  PRIMARY KEY  (history_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'documents_history'
#



#
# Table structure for table 'emails'
#

CREATE TABLE emails (
  email_id int(4) unsigned NOT NULL auto_increment,
  recipient_email varchar(128) NOT NULL default '0',
  recipient_name varchar(128) default NULL,
  sender_name varchar(128) default '0',
  sender_email varchar(128) NOT NULL default '0',
  subject varchar(255) default NULL,
  url varchar(255) NOT NULL default '',
  attachments tinytext,
  retry int(4) NOT NULL default '0',
  sent_date datetime default NULL,
  created_by_user_id int(4) unsigned NOT NULL default '0',
  create_date datetime default NULL,
  PRIMARY KEY  (email_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'emails'
#



#
# Table structure for table 'file_atr'
#

CREATE TABLE file_atr (
  file_name varchar(128) NOT NULL default '',
  link varchar(255) NOT NULL default '',
  atr_id int(4) unsigned NOT NULL default '0',
  value_string varchar(255) default NULL,
  value_int int(4) unsigned default NULL,
  value_bool tinyint(1) unsigned default '0',
  PRIMARY KEY  (link,atr_id),
  KEY link (link,atr_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'file_atr'
#



#
# Table structure for table 'file_atr_def'
#

CREATE TABLE file_atr_def (
  atr_id int(4) unsigned NOT NULL auto_increment,
  atr_name varchar(32) NOT NULL default '',
  order_priority int(4) default '10',
  atr_description varchar(255) default NULL,
  atr_default_value varchar(255) default NULL,
  atr_type tinyint(3) unsigned NOT NULL default '0',
  atr_group varchar(32) default 'default',
  true_value varchar(255) default NULL,
  false_value varchar(255) default NULL,
  PRIMARY KEY  (atr_id),
  KEY atr_id (atr_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'file_atr_def'
#



#
# Table structure for table 'forms'
#

CREATE TABLE forms (
  id int(11) unsigned NOT NULL auto_increment,
  form_name varchar(255) NOT NULL default '',
  data text NOT NULL,
  files text,
  create_date datetime default NULL,
  html text,
  user_id int(4) NOT NULL default '-1',
  note text,
  doc_id int(4) NOT NULL default '-1',
  PRIMARY KEY  (id),
  KEY id (id)
) ENGINE=InnoDB COMMENT='formulare';



#
# Dumping data for table 'forms'
#

#
# Table structure for table 'gallery'
#

CREATE TABLE gallery (
  image_id int(4) unsigned NOT NULL auto_increment,
  image_path varchar(255) default NULL,
  s_description_sk varchar(255) default NULL,
  l_description_sk text,
  image_name varchar(255) default NULL,
  s_description_en varchar(255) default NULL,
  l_description_en text,
  s_description_cz varchar(255) default NULL,
  l_description_cz text,
  s_description_de varchar(255) default NULL,
  l_description_de text,
  PRIMARY KEY  (image_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'gallery'
#



#
# Table structure for table 'gallery_dimension'
#

CREATE TABLE gallery_dimension (
  dimension_id int(4) unsigned NOT NULL auto_increment,
  image_path varchar(255) default '0',
  image_width int(4) unsigned default '0',
  image_height int(4) unsigned default '0',
  normal_width int(4) unsigned NOT NULL default '0',
  normal_height int(4) unsigned NOT NULL default '0',
  PRIMARY KEY  (dimension_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'gallery_dimension'
#

INSERT INTO gallery_dimension VALUES("5", "/images/gallery", "160", "120", "750", "560");


#
# Table structure for table 'groups'
#

CREATE TABLE groups (
  group_id int(11) NOT NULL auto_increment,
  group_name varchar(64) NOT NULL default '',
  internal tinyint(1) NOT NULL default '0',
  parent_group_id int(11) NOT NULL default '0',
  navbar text,
  default_doc_id int(11) default NULL,
  temp_id int(11) default NULL,
  sort_priority int(11) NOT NULL default '0',
  password_protected varchar(255) default NULL,
  menu_type tinyint(1) unsigned default '1',
  url_dir_name varchar(64) default NULL,
  sync_id int(11) default '0',
  sync_status int(11) default '0',
  PRIMARY KEY  (group_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'groups'
#

INSERT INTO groups VALUES("1", "Slovensky", "0", "0", "Slovensky", "4", "1", "0", NULL, "2", NULL, "0", "0");
INSERT INTO groups VALUES("2", "Šablóny", "1", "20", "Šablóny", "4", "1", "1500", NULL, "2", NULL, "0", "0");
INSERT INTO groups VALUES("3", "Menu", "1", "20", "Menu", "4", "1", "1501", NULL, "2", NULL, "0", "0");
INSERT INTO groups VALUES("4", "Hlavičky-pätičky", "1", "20", "Hlavičky-pätičky", "4", "2", "1502", NULL, "1", NULL, "0", "0");
INSERT INTO groups VALUES("5", "English", "0", "0", "English", "0", "1", "500", NULL, "2", NULL, "0", "0");
INSERT INTO groups VALUES("20", "System", "1", "0", "System", "4", "1", "1000", NULL, "2", NULL, "0", "0");
INSERT INTO groups VALUES("21", "HTMLBox", "1", "20", "HTMLBox", "4", "1", "1503", NULL, "2", NULL, "0", "0");


#
# Table structure for table 'groups_approve'
#

CREATE TABLE groups_approve (
  approve_id int(11) unsigned NOT NULL auto_increment,
  group_id int(11) unsigned default '0',
  user_id int(11) unsigned default '0',
  mode int(4) unsigned default '0',
  PRIMARY KEY  (approve_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'groups_approve'
#



#
# Table structure for table 'inquiry'
#

CREATE TABLE inquiry (
  question_id int(4) unsigned NOT NULL auto_increment,
  question_text varchar(255) default NULL,
  hours int(4) unsigned default '0',
  question_group varchar(255) default NULL,
  answer_text_ok tinytext,
  answer_text_fail tinytext,
  UNIQUE KEY question_id (question_id),
  KEY question_id_2 (question_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'inquiry'
#

INSERT INTO inquiry VALUES("1", "Ako sa vám páči WebJET", "24", "default", "Ďakujeme, že ste sa zúčastnili ankety.", "Ľutujeme, ale tejto ankety ste sa už zúčastnili.");


#
# Table structure for table 'inquiry_answers'
#

CREATE TABLE inquiry_answers (
  answer_id int(4) unsigned NOT NULL auto_increment,
  question_id int(11) NOT NULL default '0',
  answer_text varchar(255) default NULL,
  answer_clicks int(4) unsigned default '0',
  PRIMARY KEY  (answer_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'inquiry_answers'
#

INSERT INTO inquiry_answers VALUES("1", "1", "Je super", "8");
INSERT INTO inquiry_answers VALUES("2", "1", "Neviem, nepoznám", "3");


#
# Table structure for table 'questions_answers'
#

CREATE TABLE questions_answers (
  qa_id int(4) unsigned NOT NULL auto_increment,
  group_name varchar(255) NOT NULL default '',
  category_name varchar(64) default NULL,
  question_date datetime default NULL,
  answer_date datetime default NULL,
  question text,
  answer text,
  from_name varchar(255) NOT NULL default '',
  from_email varchar(255) default NULL,
  to_name varchar(255) default NULL,
  to_email varchar(255) default NULL,
  publish_on_web tinyint(1) unsigned default '0',
  hash varchar(255) default NULL,
  allow_publish_on_web tinyint(1) unsigned NOT NULL default '1',
  PRIMARY KEY  (qa_id),
  KEY qa_id (qa_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'questions_answers'
#



#
# Table structure for table 'sms_addressbook'
#

CREATE TABLE sms_addressbook (
  book_id int(10) unsigned NOT NULL auto_increment,
  user_id int(4) unsigned NOT NULL default '0',
  sms_name varchar(128) NOT NULL default '',
  sms_number varchar(32) NOT NULL default '',
  PRIMARY KEY  (book_id),
  KEY user_id (user_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'sms_addressbook'
#



#
# Table structure for table 'sms_log'
#

CREATE TABLE sms_log (
  log_id int(10) unsigned NOT NULL auto_increment,
  user_id int(4) NOT NULL default '0',
  user_ip varchar(32) NOT NULL default '',
  sent_date datetime NOT NULL default '2000-01-01 00:00:00',
  sms_number varchar(32) NOT NULL default '',
  sms_text varchar(255) NOT NULL default '',
  PRIMARY KEY  (log_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'sms_log'
#



#
# Table structure for table 'stat_browser'
#

CREATE TABLE stat_browser (
  year int(11) unsigned NOT NULL default '0',
  week int(11) unsigned NOT NULL default '0',
  browser_id varchar(32) default '0',
  platform varchar(25) default '0',
  subplatform varchar(20) default NULL,
  views int(11) unsigned default '0',
  group_id int(4) unsigned NOT NULL default '1'
) ENGINE=InnoDB;



#
# Dumping data for table 'stat_browser'
#

#
# Table structure for table 'stat_country'
#

CREATE TABLE stat_country (
  year int(11) unsigned NOT NULL default '0',
  week int(11) unsigned NOT NULL default '0',
  country_code varchar(20) NOT NULL default '0',
  views int(11) unsigned NOT NULL default '0',
  group_id int(4) unsigned NOT NULL default '1'
) ENGINE=InnoDB;



#
# Dumping data for table 'stat_country'
#

#
# Table structure for table 'stat_doc'
#

CREATE TABLE stat_doc (
  year int(11) unsigned default '0',
  week int(11) unsigned default '0',
  doc_id int(11) default '0',
  views int(11) unsigned default '1',
  in_count int(11) unsigned default '0',
  out_count int(11) unsigned default '0',
  view_time_sum int(11) unsigned default '0',
  view_time_count int(11) unsigned default '0',
  KEY i_docid (doc_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'stat_doc'
#


#
# Table structure for table 'stat_error'
#

CREATE TABLE stat_error (
  year int(11) unsigned default '0',
  week int(11) unsigned default '0',
  url varchar(255) default '0',
  query_string varchar(255) default '0',
  count int(11) unsigned default '0'
) ENGINE=InnoDB;



#
# Dumping data for table 'stat_error'
#



#
# Table structure for table 'stat_searchengine'
#

CREATE TABLE stat_searchengine (
  search_date datetime NOT NULL default '2000-01-01 00:00:00',
  server varchar(16) NOT NULL default '',
  query varchar(64) NOT NULL default '',
  doc_id int(4) unsigned NOT NULL default '0'
) ENGINE=InnoDB;



#
# Dumping data for table 'stat_searchengine'
#



#
# Table structure for table 'stat_site_days'
#

CREATE TABLE stat_site_days (
  year int(11) unsigned default '0',
  week int(11) unsigned default '0',
  views_mo int(11) unsigned default '0',
  sessions_mo int(11) unsigned default '0',
  views_tu int(11) unsigned default '0',
  sessions_tu int(11) unsigned default '0',
  views_we int(11) unsigned default '0',
  sessions_we int(11) unsigned default '0',
  views_th int(11) unsigned default '0',
  sessions_th int(11) unsigned default '0',
  views_fr int(11) unsigned default '0',
  sessions_fr int(11) unsigned default '0',
  views_sa int(11) unsigned default '0',
  sessions_sa int(11) unsigned default '0',
  views_su int(11) unsigned default '0',
  sessions_su int(11) unsigned default '0',
  view_time_sum int(11) unsigned default '0',
  view_time_count int(11) unsigned default '0',
  group_id int(4) unsigned NOT NULL default '1'
) ENGINE=InnoDB;



#
# Dumping data for table 'stat_site_days'
#

#
# Table structure for table 'stat_site_hours'
#

CREATE TABLE stat_site_hours (
  year int(11) unsigned NOT NULL default '0',
  week int(11) unsigned NOT NULL default '0',
  views_0 int(11) unsigned default '0',
  views_1 int(11) unsigned default '0',
  views_2 int(11) unsigned default '0',
  views_3 int(11) unsigned default '0',
  views_4 int(11) unsigned default '0',
  views_5 int(11) unsigned default '0',
  views_6 int(11) unsigned default '0',
  views_7 int(11) unsigned default '0',
  views_8 int(11) unsigned default '0',
  views_9 int(11) unsigned default '0',
  views_10 int(11) unsigned default '0',
  views_11 int(11) unsigned default '0',
  views_12 int(11) unsigned default '0',
  views_13 int(11) unsigned default '0',
  views_14 int(11) unsigned default '0',
  views_15 int(11) unsigned default '0',
  views_16 int(11) unsigned default '0',
  views_17 int(11) unsigned default '0',
  views_18 int(11) unsigned default '0',
  views_19 int(11) unsigned default '0',
  views_20 int(11) unsigned default '0',
  views_21 int(11) unsigned default '0',
  views_22 int(11) unsigned default '0',
  views_23 int(11) unsigned default '0',
  sessions_0 int(11) unsigned default '0',
  sessions_1 int(11) unsigned default '0',
  sessions_2 int(11) unsigned default '0',
  sessions_3 int(11) unsigned default '0',
  sessions_4 int(11) unsigned default '0',
  sessions_5 int(11) unsigned default '0',
  sessions_6 int(11) unsigned default '0',
  sessions_7 int(11) unsigned default '0',
  sessions_8 int(11) unsigned default '0',
  sessions_9 int(11) unsigned default '0',
  sessions_10 int(11) unsigned default '0',
  sessions_11 int(11) unsigned default '0',
  sessions_12 int(11) unsigned default '0',
  sessions_13 int(11) unsigned default '0',
  sessions_14 int(11) unsigned default '0',
  sessions_15 int(11) unsigned default '0',
  sessions_16 int(11) unsigned default '0',
  sessions_17 int(11) unsigned default '0',
  sessions_18 int(11) unsigned default '0',
  sessions_19 int(11) unsigned default '0',
  sessions_20 int(11) unsigned default '0',
  sessions_21 int(11) unsigned default '0',
  sessions_22 int(11) unsigned default '0',
  sessions_23 int(11) unsigned default '0',
  group_id int(4) unsigned NOT NULL default '1'
) ENGINE=InnoDB;



#
# Dumping data for table 'stat_site_hours'
#

#
# Table structure for table 'stat_userlogon'
#

CREATE TABLE stat_userlogon (
  year int(4) unsigned NOT NULL default '0',
  week int(4) unsigned NOT NULL default '0',
  user_id int(4) unsigned NOT NULL default '0',
  views int(4) unsigned default '1',
  logon_time datetime default NULL,
  view_minutes int(11) unsigned default '0',
  hostname varchar(255) default NULL,
  KEY i_userid (user_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'stat_userlogon'
#



#
# Table structure for table 'templates'
#

CREATE TABLE templates (
  temp_id int(11) NOT NULL auto_increment,
  temp_name varchar(64) NOT NULL default '',
  forward varchar(64) NOT NULL default '',
  lng varchar(16) NOT NULL default 'sk',
  header_doc_id int(11) NOT NULL default '0',
  footer_doc_id int(11) NOT NULL default '0',
  after_body_data text,
  css varchar(64) default NULL,
  menu_doc_id int(11) NOT NULL default '-1',
  PRIMARY KEY  (temp_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'templates'
#

INSERT INTO templates VALUES("1", "Generic", "tmp_generic.jsp", "sk", "1", "3", "", "", "2");


#
# Table structure for table 'user_disabled_items'
#

CREATE TABLE user_disabled_items (
  user_id int(4) unsigned NOT NULL default '0',
  item_name varchar(32) NOT NULL default '',
  KEY user_id (user_id)
) ENGINE=InnoDB;

#
# Dumping data for table 'user_disabled_items'
#

INSERT INTO user_disabled_items VALUES("1", "editorMiniEdit");

#
# Table structure for table 'user_group_verify'
#

CREATE TABLE user_group_verify (
  verify_id int(4) unsigned NOT NULL auto_increment,
  user_id int(4) unsigned NOT NULL default '0',
  user_groups varchar(255) NOT NULL default '',
  hash varchar(32) NOT NULL default '',
  create_date datetime NOT NULL default '2000-01-01 00:00:00',
  verify_date datetime default NULL,
  email varchar(255) NOT NULL default '',
  PRIMARY KEY  (verify_id),
  KEY verify_id (verify_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'user_group_verify'
#



#
# Table structure for table 'user_groups'
#

CREATE TABLE user_groups (
  user_group_id int(11) NOT NULL auto_increment,
  user_group_name varchar(255) NOT NULL default '',
  user_group_type tinyint(3) unsigned NOT NULL default '0',
  user_group_comment text,
  require_approve tinyint(1) NOT NULL default '0',
  email_doc_id int(4) NOT NULL default '-1',
  PRIMARY KEY  (user_group_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'user_groups'
#

INSERT INTO user_groups VALUES("1", "VIP Klienti", "0", NULL, "0", "-1");
INSERT INTO user_groups VALUES("2", "Obchodní partneri", "0", NULL, "0", "-1");
INSERT INTO user_groups VALUES("3", "Redaktor", "0", NULL, "0", "-1");


#
# Table structure for table 'users'
#

CREATE TABLE users (
  user_id int(11) NOT NULL auto_increment,
  title varchar(16) default NULL,
  first_name varchar(128) default NULL,
  last_name varchar(255) default NULL,
  login varchar(16) NOT NULL default '',
  password varchar(128) NOT NULL default '',
  is_admin tinyint(1) NOT NULL default '0',
  user_groups varchar(255) default NULL,
  company varchar(255) default NULL,
  adress varchar(255) default NULL,
  city varchar(255) default NULL,
  email varchar(255) default NULL,
  PSC varchar(20) default NULL,
  country varchar(255) default NULL,
  phone varchar(255) default NULL,
  authorized tinyint(1) default NULL,
  editable_groups varchar(255) default NULL,
  editable_pages varchar(255) default NULL,
  writable_folders text,
  last_logon datetime default NULL,
  module_perms varchar(255) default NULL,
  disabled_items varchar(255) default NULL,
  reg_date datetime default NULL,
  field_a varchar(255) default NULL,
  field_b varchar(255) default NULL,
  field_c varchar(255) default NULL,
  field_d varchar(255) default NULL,
  field_e varchar(255) default NULL,
  PRIMARY KEY  (user_id)
) ENGINE=InnoDB;



#
# Dumping data for table 'users'
#

INSERT INTO users VALUES("1", "", "WebJET", "Administrátor", "admin", "d7ed8dc6fc9b4a8c3b442c3dcc35bfe4", "1", NULL, "InterWay, a. s.", "", "", "web.spam@interway.sk", "", "Slovakia", "02/32788888", "1", "", NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);