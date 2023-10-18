CREATE TABLE webjet_conf (
  name nvarchar2(255) NOT NULL,
  value nvarchar2(255) NOT NULL
);

ALTER TABLE webjet_conf MODIFY (name DEFAULT '');
ALTER TABLE webjet_conf MODIFY (value DEFAULT '');
ALTER TABLE webjet_conf add CONSTRAINT name UNIQUE  (name);

INSERT INTO webjet_conf VALUES ('editorEnableXHTML','true');
INSERT INTO webjet_conf VALUES ('adminEnableIPs','127.0.0.1,10.,192.168.,195.168.35.4,195.168.35.5');
INSERT INTO webjet_conf VALUES ('statNoLogIP','127.0.0.1,192.168.');
INSERT INTO webjet_conf VALUES ('statEnableOld','false');
INSERT INTO webjet_conf VALUES ('defaultDisableUpload','false');
INSERT INTO webjet_conf VALUES ('showDocActionAllowedDocids','4');
INSERT INTO webjet_conf VALUES ('webEnableIPs','#localhost,127.0.0.1,10.,192.168.,#interway,85.248.107.8,195.168.35.4,195.168.35.5,#klient,');
INSERT INTO webjet_conf VALUES ('inlineEditingEnabled','true');
INSERT INTO webjet_conf VALUES ('disableWebJETToolbar','true');

CREATE TABLE webjet_db (
  id INTEGER NOT NULL,
  create_date date NOT NULL,
  note nvarchar2(255) NOT NULL
);

INSERT INTO webjet_db VALUES(1, to_date('06.12.2003', 'DD.MM.YYYY'), 'ukladanie poznamky a prihlaseneho usera k formularom');
INSERT INTO webjet_db VALUES(2, to_date('12.12.2003', 'DD.MM.YYYY'), 'sposob zobrazenia menu pre adresar');
INSERT INTO webjet_db VALUES(3, to_date('21.12.2003', 'DD.MM.YYYY'), 'atributy suborov');
INSERT INTO webjet_db VALUES(4, to_date('29.12.2003', 'DD.MM.YYYY'), 'od teraz sa kontroluje aj admin, ci je autorizovany, takze nastavime default');
INSERT INTO webjet_db VALUES(5, to_date('04.01.2004', 'DD.MM.YYYY'), 'uklada k formularu aj docid (ak sa podari zistit)');
INSERT INTO webjet_db VALUES(6, to_date('09.01.2004', 'DD.MM.YYYY'), 'typ skupiny pouzivatelov, 0=perms, 1=email, 2=...');
INSERT INTO webjet_db VALUES(7, to_date('10.01.2004', 'DD.MM.YYYY'), 'email je mozne posielat uz len ako URL, text sa priamo napisat neda');
INSERT INTO webjet_db VALUES(8, to_date('11.01.2004', 'DD.MM.YYYY'), 'verifikacia subscribe k email newslettrom, po autorizacii emailom sa user_groups zapise do tabulky users');
INSERT INTO webjet_db VALUES(9, to_date('13.01.2004', 'DD.MM.YYYY'), 'zoznam foldrov (/images/nieco...) do ktorych ma user pravo nahravat obrazky a subory');
INSERT INTO webjet_db VALUES(10, to_date('25.01.2004', 'DD.MM.YYYY'), 'volne pouzitelne polia pre kalendar podujati');
INSERT INTO webjet_db VALUES(11, to_date('11.02.2004', 'DD.MM.YYYY'), 'casova notifikacia pre kalendar podujati');
INSERT INTO webjet_db VALUES(12, to_date('15.02.2004', 'DD.MM.YYYY'), 'virtualne cesty k strankam, napr. www.server.sk/products');
INSERT INTO webjet_db VALUES(13, to_date('17.02.2004', 'DD.MM.YYYY'), 'uvodny text notifikacie kalendara, moznost poslat SMS');
INSERT INTO webjet_db VALUES(14, to_date('24.02.2004', 'DD.MM.YYYY'), 'ak je true, dava navstevnik suhlas na zobrazenie na webe');
INSERT INTO webjet_db VALUES(15, to_date('28.02.2004', 'DD.MM.YYYY'), 'urychlenie statistiky');
INSERT INTO webjet_db VALUES(16, to_date('03.01.2004', 'DD.MM.YYYY'), 'zvacsenie poli');
INSERT INTO webjet_db VALUES(17, to_date('03.03.2004', 'DD.MM.YYYY'), 'urychlenie nacitania virtual paths');
INSERT INTO webjet_db VALUES(18, to_date('05.03.2004', 'DD.MM.YYYY'), 'konfiguracia webjetu (namiesto web.xml)');
INSERT INTO webjet_db VALUES(19, to_date('07.03.2004', 'DD.MM.YYYY'), 'disabled items pouzivatelov');
INSERT INTO webjet_db VALUES(20, to_date('07.03.2004', 'DD.MM.YYYY'), 'rozdelenie full name na meno a priezvisko');
INSERT INTO webjet_db VALUES(21, to_date('08.03.2004', 'DD.MM.YYYY'), 'volne pouzitelne polozky');
INSERT INTO webjet_db VALUES(22, to_date('12.03.2004', 'DD.MM.YYYY'), 'url nazov adresara');
INSERT INTO webjet_db VALUES(23, to_date('15.03.2004', 'DD.MM.YYYY'), 'implemetacia rozdelenia full name');
INSERT INTO webjet_db VALUES(24, to_date('15.03.2004', 'DD.MM.YYYY'), 'Konverzia pristupovych prav');
INSERT INTO webjet_db VALUES(25, to_date('18.03.2004', 'DD.MM.YYYY'), 'custom zmena textov v properties suboroch');
INSERT INTO webjet_db VALUES(26, to_date('27.03.2004', 'DD.MM.YYYY'), 'uprava statistik (eviduje sa id adresara)');
INSERT INTO webjet_db VALUES(27, to_date('28.03.2004', 'DD.MM.YYYY'), 'statistika query vo vyhladavacoch');
INSERT INTO webjet_db VALUES(28, to_date('05.04.2004', 'DD.MM.YYYY'), 'mod schvalovania adresara (0=approve, 1=notify, 2=none)');
INSERT INTO webjet_db VALUES(29, to_date('05.03.2004', 'DD.MM.YYYY'), 'konfiguracia webjetu (namiesto web.xml)');
INSERT INTO webjet_db VALUES(30, to_date('07.03.2004', 'DD.MM.YYYY'), 'disabled items pouzivatelov');
INSERT INTO webjet_db VALUES(31, to_date('07.03.2004', 'DD.MM.YYYY'), 'rozdelenie full name na meno a priezvisko');
INSERT INTO webjet_db VALUES(32, to_date('08.03.2004', 'DD.MM.YYYY'), 'volne pouzitelne polozky');
INSERT INTO webjet_db VALUES(33, to_date('12.03.2004', 'DD.MM.YYYY'), 'url nazov adresara');
INSERT INTO webjet_db VALUES(34, to_date('18.03.2004', 'DD.MM.YYYY'), 'custom zmena textov v properties suboroch');
INSERT INTO webjet_db VALUES(35, to_date('27.03.2004', 'DD.MM.YYYY'), 'uprava statistik (eviduje sa id adresara)');
INSERT INTO webjet_db VALUES(36, to_date('28.03.2004', 'DD.MM.YYYY'), 'statistika query vo vyhladavacoch');
INSERT INTO webjet_db VALUES(37, to_date('05.04.2004', 'DD.MM.YYYY'), 'mod schvalovania adresara (0=approve, 1=notify, 2=none)');
INSERT INTO webjet_db VALUES(38, to_date('01.05.2004', 'DD.MM.YYYY'), 'id a stav synchronizacie (status: 0=novy, 1=updated, 2=synchronized)');
INSERT INTO webjet_db VALUES(39, to_date('02.05.2004', 'DD.MM.YYYY'), 'konfiguracia custom modulov');
INSERT INTO webjet_db VALUES(40, to_date('03.05.2004', 'DD.MM.YYYY'), 'modul posielania SMS sprav');
INSERT INTO webjet_db VALUES(41, to_date('09.05.2004', 'DD.MM.YYYY'), 'vyzadovanie schvalovania registracie, doc_id pre zasielany email');

CREATE SEQUENCE S_webjet_db START WITH 42;

CREATE TRIGGER T_webjet_db BEFORE INSERT ON webjet_db
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.id IS NULL THEN
			SELECT S_webjet_db.nextval into val FROM dual|
			:new.id:= val|
		END IF|
	END|
;

CREATE TABLE webjet_modules (
  module_id INTEGER  NOT NULL,
  name_key nvarchar2(128) NOT NULL,
  item_key nvarchar2(64) NOT NULL,
  path nvarchar2(255) NOT NULL
);

ALTER TABLE webjet_modules  MODIFY (name_key DEFAULT '');
ALTER TABLE webjet_modules  MODIFY (item_key DEFAULT '');
ALTER TABLE webjet_modules  MODIFY (path DEFAULT '');
ALTER TABLE webjet_modules add CONSTRAINT module_id UNIQUE  (module_id);

CREATE SEQUENCE S_webjet_modules START WITH 1;

CREATE TRIGGER T_webjet_modules BEFORE INSERT ON webjet_modules
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.module_id IS NULL THEN
			SELECT S_webjet_modules.nextval into val FROM dual|
			:new.module_id:= val|
		END IF|
	END|
;


CREATE TABLE webjet_properties (
  prop_key nvarchar2(255) NOT NULL,
  lng char(3) NOT NULL,
  prop_value nvarchar2(255) NOT NULL
);

ALTER TABLE webjet_properties  MODIFY (prop_key DEFAULT '');
ALTER TABLE webjet_properties  MODIFY (lng DEFAULT '');
ALTER TABLE webjet_properties  MODIFY (prop_value DEFAULT '');
ALTER TABLE webjet_properties add CONSTRAINT prop_key UNIQUE (prop_key,lng);

CREATE TABLE webjet_adminlog (
  log_id INTEGER NOT NULL,
  log_type INTEGER,
  user_id INTEGER NOT NULL,
  create_date DATE NOT NULL,
  description clob,
  sub_id1 INTEGER,
  sub_id2 INTEGER,
  ip nvarchar2(128),
  hostname nvarchar2(255)
);

ALTER TABLE webjet_adminlog MODIFY (log_type DEFAULT 0);
ALTER TABLE webjet_adminlog MODIFY (user_id DEFAULT -1);
ALTER TABLE webjet_adminlog MODIFY (sub_id1 DEFAULT 0);
ALTER TABLE webjet_adminlog MODIFY (sub_id2 DEFAULT 0);
ALTER TABLE webjet_adminlog add CONSTRAINT pk_webjet_adminlog PRIMARY KEY (log_id);

CREATE SEQUENCE S_webjet_adminlog START WITH 1;

CREATE TRIGGER T_webjet_adminlog BEFORE INSERT ON webjet_adminlog
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.log_id IS NULL THEN
			SELECT S_webjet_adminlog.nextval into val FROM dual|
			:new.log_id:= val|
		END IF|
	END|
;


CREATE TABLE calendar (
  calendar_id INTEGER NOT NULL,
  title nvarchar2(255) NOT NULL,
  description clob,
  date_from DATE NOT NULL,
  date_to DATE NOT NULL,
  type_id INTEGER NOT NULL,
  time_range nvarchar2(128),
  area nvarchar2(255),
  city nvarchar2(255),
  address nvarchar2(255),
  info_1 nvarchar2(255),
  info_2 nvarchar2(255),
  info_3 nvarchar2(255),
  info_4 nvarchar2(255),
  info_5 nvarchar2(255),
  notify_hours_before INTEGER,
  notify_emails nvarchar2(2000),
  notify_sender nvarchar2(255),
  notify_sent SMALLINT  NOT NULL,
  notify_introtext clob,
  notify_sendsms SMALLINT ,
  lng char(3)
);

ALTER TABLE calendar  MODIFY (date_from DEFAULT NULL);
ALTER TABLE calendar  MODIFY (date_to DEFAULT NULL);
ALTER TABLE calendar  MODIFY (type_id DEFAULT 0);
ALTER TABLE calendar  MODIFY (time_range DEFAULT NULL);
ALTER TABLE calendar  MODIFY (area DEFAULT NULL);
ALTER TABLE calendar  MODIFY (city DEFAULT NULL);
ALTER TABLE calendar  MODIFY (address DEFAULT NULL);
ALTER TABLE calendar  MODIFY (info_1 DEFAULT NULL);
ALTER TABLE calendar  MODIFY (info_2 DEFAULT NULL);
ALTER TABLE calendar  MODIFY (info_3 DEFAULT NULL);
ALTER TABLE calendar  MODIFY (info_4 DEFAULT NULL);
ALTER TABLE calendar  MODIFY (info_5 DEFAULT NULL);
ALTER TABLE calendar  MODIFY (notify_hours_before DEFAULT 0);
ALTER TABLE calendar  MODIFY (notify_sender DEFAULT NULL);
ALTER TABLE calendar  MODIFY (notify_sent DEFAULT 0);
ALTER TABLE calendar  MODIFY (notify_sendsms DEFAULT 0);
ALTER TABLE calendar  MODIFY (lng DEFAULT NULL);

CREATE SEQUENCE S_calendar START WITH 1;

CREATE TRIGGER T_calendar BEFORE INSERT ON calendar
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.calendar_id IS NULL THEN
			SELECT S_calendar.nextval into val FROM dual|
			:new.calendar_id:= val|
		END IF|
	END|
;


CREATE TABLE calendar_types (
  type_id INTEGER  NOT NULL,
  name nvarchar2(128) NOT NULL
);

ALTER TABLE calendar_types  MODIFY (name DEFAULT '');
ALTER TABLE calendar_types add CONSTRAINT pk_calendar_types PRIMARY KEY (type_id);

INSERT INTO calendar_types VALUES(1, 'Výstava');
INSERT INTO calendar_types VALUES(2, 'Šport');
INSERT INTO calendar_types VALUES(3, 'Kultúra');
INSERT INTO calendar_types VALUES(4, 'Rodina');
INSERT INTO calendar_types VALUES(5, 'Konferencia');

CREATE SEQUENCE S_calendar_types START WITH 6;

CREATE TRIGGER T_calendar_types BEFORE INSERT ON calendar_types
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.type_id IS NULL THEN
			SELECT S_calendar_types.nextval into val FROM dual|
			:new.type_id:= val|
		END IF|
	END|
;


CREATE TABLE doc_atr (
  doc_id INTEGER  NOT NULL,
  atr_id INTEGER  NOT NULL,
  value_string nvarchar2(255),
  value_int INTEGER ,
  value_bool SMALLINT
 );

ALTER TABLE doc_atr  MODIFY (doc_id DEFAULT 0);
ALTER TABLE doc_atr  MODIFY (atr_id DEFAULT 0);
ALTER TABLE doc_atr  MODIFY (value_string DEFAULT NULL);
ALTER TABLE doc_atr  MODIFY (value_int DEFAULT NULL);
ALTER TABLE doc_atr  MODIFY (value_bool DEFAULT 0);
ALTER TABLE doc_atr add CONSTRAINT pk_doc_atr PRIMARY KEY (doc_id,atr_id);



CREATE TABLE doc_atr_def (
  atr_id INTEGER  NOT NULL,
  atr_name nvarchar2(32) NOT NULL,
  order_priority INTEGER,
  atr_description nvarchar2(255),
  atr_default_value nvarchar2(255),
  atr_type SMALLINT  NOT NULL,
  atr_group nvarchar2(32),
  true_value nvarchar2(255),
  false_value nvarchar2(255)
) ;

ALTER TABLE doc_atr_def  MODIFY (atr_name DEFAULT '');
ALTER TABLE doc_atr_def  MODIFY (order_priority DEFAULT 10);
ALTER TABLE doc_atr_def  MODIFY (atr_description DEFAULT NULL);
ALTER TABLE doc_atr_def  MODIFY (atr_default_value DEFAULT NULL);
ALTER TABLE doc_atr_def  MODIFY (atr_type DEFAULT 0);
ALTER TABLE doc_atr_def  MODIFY (atr_group DEFAULT 'default');
ALTER TABLE doc_atr_def  MODIFY (true_value DEFAULT NULL);
ALTER TABLE doc_atr_def  MODIFY (false_value DEFAULT NULL);
ALTER TABLE doc_atr_def add CONSTRAINT pk_doc_atr_def PRIMARY KEY (atr_id);

CREATE SEQUENCE S_doc_atr_def START WITH 1;

CREATE TRIGGER T_doc_atr_def BEFORE INSERT ON doc_atr_def
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.atr_id IS NULL THEN
			SELECT S_doc_atr_def.nextval into val FROM dual|
			:new.atr_id:= val|
		END IF|
	END|
;


CREATE TABLE document_forum (
  forum_id INTEGER  NOT NULL,
  doc_id INTEGER  NOT NULL,
  parent_id INTEGER NOT NULL,
  subject nvarchar2(255),
  question clob,
  question_date DATE,
  author_name nvarchar2(255),
  author_email nvarchar2(255),
  ip nvarchar2(255),
  confirmed SMALLINT NOT NULL,
  hash_code nvarchar2(64) NULL,
  user_id INTEGER,
  flag nvarchar2(128),
  stat_views INTEGER,
  stat_replies INTEGER,
  stat_last_post DATE,
  active SMALLINT
);

ALTER TABLE document_forum  MODIFY (doc_id DEFAULT 0);
ALTER TABLE document_forum  MODIFY (parent_id DEFAULT -1);
ALTER TABLE document_forum  MODIFY (subject DEFAULT NULL);
ALTER TABLE document_forum  MODIFY (question_date DEFAULT NULL);
ALTER TABLE document_forum  MODIFY (author_name DEFAULT NULL);
ALTER TABLE document_forum  MODIFY (author_email DEFAULT NULL);
ALTER TABLE document_forum  MODIFY (ip DEFAULT NULL);
ALTER TABLE document_forum  MODIFY (confirmed DEFAULT 1);
ALTER TABLE document_forum  MODIFY (user_id DEFAULT -1);
ALTER TABLE document_forum  MODIFY (active DEFAULT 1);
ALTER TABLE document_forum add CONSTRAINT pk_document_forum PRIMARY KEY (forum_id);

COMMENT ON TABLE document_forum IS 'diskusne forum';

CREATE SEQUENCE S_document_forum START WITH 1;

CREATE TRIGGER T_document_forum BEFORE INSERT ON document_forum
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.forum_id IS NULL THEN
			SELECT S_document_forum.nextval into val FROM dual|
			:new.forum_id:= val|
		END IF|
	END|
;


CREATE TABLE documents (
  doc_id INTEGER NOT NULL,
  title nvarchar2(255) NOT NULL,
  data clob NOT NULL,
  data_asc clob NOT NULL,
  external_link nvarchar2(255),
  navbar nvarchar2(255) NOT NULL,
  date_created DATE NOT NULL,
  publish_start DATE,
  publish_end DATE,
  author_id INTEGER NOT NULL,
  group_id INTEGER,
  temp_id INTEGER NOT NULL,
  views_total INTEGER NOT NULL,
  views_month INTEGER NOT NULL,
  searchable SMALLINT NOT NULL,
  available SMALLINT NOT NULL,
  cacheable SMALLINT NOT NULL,
  file_name nvarchar2(255),
  file_change DATE,
  sort_priority INTEGER NOT NULL,
  header_doc_id INTEGER,
  menu_doc_id INTEGER NOT NULL,
  footer_doc_id INTEGER NOT NULL,
  password_protected nvarchar2(255),
  html_head clob,
  html_data clob,
  perex_place nvarchar2(255),
  perex_image nvarchar2(255),
  perex_group nvarchar2(255),
  show_in_menu SMALLINT ,
  event_date DATE,
  virtual_path nvarchar2(255),
  sync_id INTEGER,
  sync_status INTEGER,
  logon_page_doc_id INTEGER,
  right_menu_doc_id INTEGER,
  field_a nvarchar2(255),
  field_b nvarchar2(255),
  field_c nvarchar2(255),
  field_d nvarchar2(255),
  field_e nvarchar2(255),
  field_f nvarchar2(255),
  field_g nvarchar2(255),
  field_h nvarchar2(255),
  field_i nvarchar2(255),
  field_j nvarchar2(255),
  field_k nvarchar2(255),
  field_l nvarchar2(255)
);

ALTER TABLE documents  MODIFY (title DEFAULT '');
ALTER TABLE documents  MODIFY (external_link DEFAULT NULL);
ALTER TABLE documents  MODIFY (date_created DEFAULT NULL);
ALTER TABLE documents  MODIFY (publish_start DEFAULT NULL);
ALTER TABLE documents  MODIFY (publish_end DEFAULT NULL);
ALTER TABLE documents  MODIFY (author_id DEFAULT 0);
ALTER TABLE documents  MODIFY (group_id DEFAULT NULL);
ALTER TABLE documents  MODIFY (temp_id DEFAULT 0);
ALTER TABLE documents  MODIFY (views_total DEFAULT 0);
ALTER TABLE documents  MODIFY (views_month DEFAULT 0);
ALTER TABLE documents  MODIFY (searchable DEFAULT 0);
ALTER TABLE documents  MODIFY (available DEFAULT 0);
ALTER TABLE documents  MODIFY (cacheable DEFAULT 0);
ALTER TABLE documents  MODIFY (file_name DEFAULT NULL);
ALTER TABLE documents  MODIFY (file_change DEFAULT NULL);
ALTER TABLE documents  MODIFY (sort_priority DEFAULT 0);
ALTER TABLE documents  MODIFY (header_doc_id DEFAULT NULL);
ALTER TABLE documents  MODIFY (menu_doc_id DEFAULT -1);
ALTER TABLE documents  MODIFY (footer_doc_id DEFAULT -1);
ALTER TABLE documents  MODIFY (password_protected DEFAULT NULL);
ALTER TABLE documents  MODIFY (perex_place DEFAULT NULL);
ALTER TABLE documents  MODIFY (perex_image DEFAULT NULL);
ALTER TABLE documents  MODIFY (perex_group DEFAULT NULL);
ALTER TABLE documents  MODIFY (show_in_menu DEFAULT 1);
ALTER TABLE documents  MODIFY (event_date DEFAULT NULL);
ALTER TABLE documents  MODIFY (virtual_path DEFAULT NULL);
ALTER TABLE documents  MODIFY (sync_id DEFAULT 0);
ALTER TABLE documents  MODIFY (sync_status DEFAULT 0);
ALTER TABLE documents  MODIFY (logon_page_doc_id DEFAULT -1);
ALTER TABLE documents  MODIFY (right_menu_doc_id DEFAULT -1);
ALTER TABLE documents add CONSTRAINT pk_documents PRIMARY KEY (doc_id);



INSERT INTO documents(doc_id,title,data,data_asc,external_link,navbar,date_created,publish_start,publish_end,author_id,group_id,temp_id,views_total,views_month,searchable,available,cacheable,file_name,file_change,sort_priority,header_doc_id,menu_doc_id,footer_doc_id,password_protected,html_head,html_data,perex_place,perex_image,perex_group,show_in_menu,event_date,virtual_path,sync_id,sync_status) VALUES(1,'Default header','<h1>Headline</h1><p>Lorem ipsum dolor sit amet, consectetur adipisicing elit. Sit aspernatur labore vel rem adipisci commodi odit eum vitae asperiores esse?</p><a href=# class=\"btn btn-default\">Lorem ipsum.</a>','-','','Default hlavička', to_date('25.07.2002', 'DD.MM.YYYY'), NULL, NULL,1, 4, 1, 0, 0, 0, 1, 0,'1_default_hlavicka.html', to_date('11.04.2003', 'DD.MM.YYYY'), 50, -1, -1, -1,NULL, NULL, NULL, NULL, NULL, NULL, 1, NULL, NULL, 0, 0);
INSERT INTO documents(doc_id,title,data,data_asc,external_link,navbar,date_created,publish_start,publish_end,author_id,group_id,temp_id,views_total,views_month,searchable,available,cacheable,file_name,file_change,sort_priority,header_doc_id,menu_doc_id,footer_doc_id,password_protected,html_head,html_data,perex_place,perex_image,perex_group,show_in_menu,event_date,virtual_path,sync_id,sync_status) VALUES(2, 'Default navigation', '!INCLUDE(/components/menu/menu_ul_li.jsp, rootGroupId=1, startOffset=0, maxLevel=3, classes=basic, openAllItems=true, rootUlId=mainNavigation)!', '-', '', 'Default menu', to_date('23.05.2003', 'DD.MM.YYYY'), NULL, NULL, 1, 3, 1, 0, 0, 0, 1, 0, '2_default_menu.html', to_date('27.08.2003', 'DD.MM.YYYY'), 50, -1, -1, -1, NULL, '', '', NULL, NULL, NULL, 1, NULL, NULL, 0, 0);
INSERT INTO documents(doc_id,title,data,data_asc,external_link,navbar,date_created,publish_start,publish_end,author_id,group_id,temp_id,views_total,views_month,searchable,available,cacheable,file_name,file_change,sort_priority,header_doc_id,menu_doc_id,footer_doc_id,password_protected,html_head,html_data,perex_place,perex_image,perex_group,show_in_menu,event_date,virtual_path,sync_id,sync_status) VALUES(3, 'Default footer', '<p>Copyright !YEAR! InterWay, a. s. All Rights Reserved. Page generated by WebJET CMS.</p>', '-', '', 'Default pätička', to_date('25.07.2002', 'DD.MM.YYYY'), NULL, NULL, 1, 4, 1, 0, 0, 0, 1, 0, '3_default_paticka.html', to_date('11.04.2003', 'DD.MM.YYYY'), 50, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, NULL, 1, NULL, NULL, 0, 0);
INSERT INTO documents(doc_id,title,data,data_asc,external_link,navbar,date_created,publish_start,publish_end,author_id,group_id,temp_id,views_total,views_month,searchable,available,cacheable,file_name,file_change,sort_priority,header_doc_id,menu_doc_id,footer_doc_id,password_protected,html_head,html_data,perex_place,perex_image,perex_group,show_in_menu,event_date,virtual_path,sync_id,sync_status) VALUES(4, 'Hlavna stranka', '<P>Vitajte na demo stránke systému WebJET.</P><P>Viac informácií o systéme nájdete na stránke <A href=\"http://www.webjetcms.sk\" target=_blank>www.webjetcms.sk</A>.</P>', '<p>vitajte na demo stranke systemu webjet.</p><p>viac informacii o systeme najdete na stranke <a href=\"http://www.webjetcms.sk\" target=_blank>www.webjetcms.sk</a>.</p>', '', 'Hlavna stranka', to_date('05.10.2003', 'DD.MM.YYYY'), NULL, NULL, 1, 1, 1, 0, 0, 1, 1, 0, '4_hlavna_stranka.html', to_date('05.10.2003', 'DD.MM.YYYY'), 1, 24, 25, -1, NULL, '', NULL, '', '', NULL, 1, NULL, NULL, 0, 0);
INSERT INTO documents(doc_id,title,data,data_asc,external_link,navbar,date_created,publish_start,publish_end,author_id,group_id,temp_id,views_total,views_month,searchable,available,cacheable,file_name,file_change,sort_priority,header_doc_id,menu_doc_id,footer_doc_id,password_protected,html_head,html_data,perex_place,perex_image,perex_group,show_in_menu,event_date,virtual_path,sync_id,sync_status) VALUES(5, 'Default left menu', '!INCLUDE(/components/menu/menu_ul_li.jsp, rootGroupId=1, startOffset=1, maxLevel=3, classes=basic, openAllItems=true, rootUlId=mainNavigation)!', '-', '', 'Default left menu', to_date('23.05.2003', 'DD.MM.YYYY'), NULL, NULL, 1, 3, 1, 0, 0, 0, 1, 0, '2_default_menu.html', to_date('27.08.2003', 'DD.MM.YYYY'), 50, -1, -1, -1, NULL, '', '', NULL, NULL, NULL, 1, NULL, NULL, 0, 0);

UPDATE documents set temp_id=1;

CREATE SEQUENCE S_documents START WITH 6;

CREATE TRIGGER T_documents BEFORE INSERT ON documents
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.doc_id IS NULL THEN
			SELECT S_documents.nextval into val FROM dual|
			:new.doc_id:= val|
		END IF|
	END|
;


CREATE TABLE documents_history (
  history_id INTEGER NOT NULL,
  save_date DATE,
  approved_by INTEGER NOT NULL,
  awaiting_approve nvarchar2(255),
  actual SMALLINT NOT NULL,
  doc_id INTEGER NOT NULL,
  title nvarchar2(255) NOT NULL,
  data clob NOT NULL,
  data_asc clob NOT NULL,
  external_link nvarchar2(255),
  navbar nvarchar2(255) NOT NULL,
  date_created DATE NOT NULL,
  publish_start DATE,
  publish_end DATE,
  author_id INTEGER NOT NULL,
  group_id INTEGER,
  temp_id INTEGER NOT NULL,
  views_total INTEGER NOT NULL,
  views_month INTEGER NOT NULL,
  searchable SMALLINT NOT NULL,
  available SMALLINT NOT NULL,
  cacheable SMALLINT NOT NULL,
  file_name nvarchar2(255),
  file_change DATE,
  sort_priority INTEGER NOT NULL,
  header_doc_id INTEGER,
  footer_doc_id INTEGER,
  menu_doc_id INTEGER NOT NULL,
  password_protected nvarchar2(255),
  html_head clob,
  html_data clob,
  publicable SMALLINT ,
  perex_place nvarchar2(255),
  perex_image nvarchar2(255),
  perex_group nvarchar2(255),
  show_in_menu SMALLINT ,
  event_date DATE,
  virtual_path nvarchar2(255),
  sync_id INTEGER,
  sync_status INTEGER,
  logon_page_doc_id INTEGER,
  right_menu_doc_id INTEGER,
  field_a nvarchar2(255),
  field_b nvarchar2(255),
  field_c nvarchar2(255),
  field_d nvarchar2(255),
  field_e nvarchar2(255),
  field_f nvarchar2(255),
  field_g nvarchar2(255),
  field_h nvarchar2(255),
  field_i nvarchar2(255),
  field_j nvarchar2(255),
  field_k nvarchar2(255),
  field_l nvarchar2(255)
);

ALTER TABLE documents_history  MODIFY (save_date DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (approved_by DEFAULT -1);
ALTER TABLE documents_history  MODIFY (awaiting_approve DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (actual DEFAULT 0);
ALTER TABLE documents_history  MODIFY (doc_id DEFAULT 0);
ALTER TABLE documents_history  MODIFY (title DEFAULT '');
ALTER TABLE documents_history  MODIFY (external_link DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (date_created DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (publish_start DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (publish_end DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (author_id DEFAULT 0);
ALTER TABLE documents_history  MODIFY (group_id DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (temp_id DEFAULT 0);
ALTER TABLE documents_history  MODIFY (views_total DEFAULT 0);
ALTER TABLE documents_history  MODIFY (views_month DEFAULT 0);
ALTER TABLE documents_history  MODIFY (searchable DEFAULT 0);
ALTER TABLE documents_history  MODIFY (available DEFAULT 0);
ALTER TABLE documents_history  MODIFY (cacheable DEFAULT 0);
ALTER TABLE documents_history  MODIFY (file_name DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (file_change DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (sort_priority DEFAULT 0);
ALTER TABLE documents_history  MODIFY (header_doc_id DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (footer_doc_id DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (menu_doc_id DEFAULT -1);
ALTER TABLE documents_history  MODIFY (password_protected DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (publicable DEFAULT 0);
ALTER TABLE documents_history  MODIFY (perex_place DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (perex_image DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (perex_group DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (show_in_menu DEFAULT 1);
ALTER TABLE documents_history  MODIFY (event_date DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (virtual_path DEFAULT NULL);
ALTER TABLE documents_history  MODIFY (sync_id DEFAULT 0);
ALTER TABLE documents_history  MODIFY (sync_status DEFAULT 0);
ALTER TABLE documents_history  MODIFY (logon_page_doc_id DEFAULT -1);
ALTER TABLE documents_history  MODIFY (right_menu_doc_id DEFAULT -1);
ALTER TABLE documents_history add CONSTRAINT pk_documents_history PRIMARY KEY (history_id);

CREATE SEQUENCE S_documents_history START WITH 1;

CREATE TRIGGER T_documents_history BEFORE INSERT ON documents_history
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.history_id IS NULL THEN
			SELECT S_documents_history.nextval into val FROM dual|
			:new.history_id:= val|
		END IF|
	END|
;


CREATE TABLE emails (
  email_id INTEGER  NOT NULL,
  recipient_email nvarchar2(128) NOT NULL,
  recipient_name nvarchar2(128),
  sender_name nvarchar2(128),
  sender_email nvarchar2(128) NOT NULL,
  subject nvarchar2(255),
  url nvarchar2(1024) NOT NULL,
  attachments clob,
  retry INTEGER NOT NULL,
  sent_date DATE,
  created_by_user_id INTEGER  NOT NULL,
  create_date DATE,
  send_at DATE,
  message clob,
  reply_to nvarchar2(255),
  cc_email nvarchar2(255),
  bcc_email nvarchar2(255),
  disabled SMALLINT
);

ALTER TABLE emails  MODIFY (recipient_email DEFAULT 0);
ALTER TABLE emails  MODIFY (recipient_name DEFAULT NULL);
ALTER TABLE emails  MODIFY (sender_name DEFAULT 0);
ALTER TABLE emails  MODIFY (sender_email DEFAULT 0);
ALTER TABLE emails  MODIFY (subject DEFAULT NULL);
ALTER TABLE emails  MODIFY (url DEFAULT '');
ALTER TABLE emails  MODIFY (retry DEFAULT 0);
ALTER TABLE emails  MODIFY (sent_date DEFAULT NULL);
ALTER TABLE emails  MODIFY (created_by_user_id DEFAULT 0);
ALTER TABLE emails  MODIFY (create_date DEFAULT NULL);
ALTER TABLE emails  MODIFY (disabled DEFAULT 0);
ALTER TABLE emails add CONSTRAINT pk_emails PRIMARY KEY (email_id);

CREATE SEQUENCE S_emails START WITH 1;

CREATE TRIGGER T_emails BEFORE INSERT ON emails
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.email_id IS NULL THEN
			SELECT S_emails.nextval into val FROM dual|
			:new.email_id:= val|
		END IF|
	END|
;


CREATE TABLE file_atr (
  file_name nvarchar2(128) NOT NULL,
  link nvarchar2(255) NOT NULL,
  atr_id INTEGER  NOT NULL,
  value_string nvarchar2(255),
  value_int INTEGER ,
  value_bool SMALLINT
);

ALTER TABLE file_atr  MODIFY (file_name DEFAULT '');
ALTER TABLE file_atr  MODIFY (link DEFAULT '');
ALTER TABLE file_atr  MODIFY (atr_id DEFAULT 0);
ALTER TABLE file_atr  MODIFY (value_string DEFAULT NULL);
ALTER TABLE file_atr  MODIFY (value_int DEFAULT NULL);
ALTER TABLE file_atr  MODIFY (value_bool DEFAULT 0);
ALTER TABLE file_atr add CONSTRAINT pk_file_atr PRIMARY KEY (link,atr_id);



CREATE TABLE file_atr_def (
  atr_id INTEGER  NOT NULL,
  atr_name nvarchar2(32) NOT NULL,
  order_priority INTEGER,
  atr_description nvarchar2(255),
  atr_default_value nvarchar2(255),
  atr_type SMALLINT  NOT NULL,
  atr_group nvarchar2(32),
  true_value nvarchar2(255),
  false_value nvarchar2(255)
);

ALTER TABLE file_atr_def  MODIFY (atr_name DEFAULT '');
ALTER TABLE file_atr_def  MODIFY (order_priority DEFAULT 10);
ALTER TABLE file_atr_def  MODIFY (atr_description DEFAULT NULL);
ALTER TABLE file_atr_def  MODIFY (atr_default_value DEFAULT NULL);
ALTER TABLE file_atr_def  MODIFY (atr_type DEFAULT 0);
ALTER TABLE file_atr_def  MODIFY (atr_group DEFAULT 'default');
ALTER TABLE file_atr_def  MODIFY (true_value DEFAULT NULL);
ALTER TABLE file_atr_def  MODIFY (false_value DEFAULT NULL);
ALTER TABLE file_atr_def add CONSTRAINT pk_file_atr_def PRIMARY KEY (atr_id);

CREATE SEQUENCE S_file_atr_def START WITH 1;

CREATE TRIGGER T_file_atr_def BEFORE INSERT ON file_atr_def
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.atr_id IS NULL THEN
			SELECT S_file_atr_def.nextval into val FROM dual|
			:new.atr_id:= val|
		END IF|
	END|
;


CREATE TABLE forms (
  id INTEGER  NOT NULL,
  form_name nvarchar2(255) NOT NULL,
  data clob NOT NULL,
  files clob,
  create_date DATE,
  html clob,
  user_id INTEGER NOT NULL,
  note clob,
  doc_id INTEGER NOT NULL
);

ALTER TABLE forms  MODIFY (form_name DEFAULT '');
ALTER TABLE forms  MODIFY (create_date DEFAULT NULL);
ALTER TABLE forms  MODIFY (user_id DEFAULT -1);
ALTER TABLE forms  MODIFY (doc_id DEFAULT -1);
ALTER TABLE forms add CONSTRAINT pk_forms PRIMARY KEY (id);

COMMENT ON TABLE forms IS 'formulare';

CREATE SEQUENCE S_forms START WITH 1;

CREATE TRIGGER T_forms BEFORE INSERT ON forms
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.id IS NULL THEN
			SELECT S_forms.nextval into val FROM dual|
			:new.id:= val|
		END IF|
	END|
;


CREATE TABLE gallery (
  image_id INTEGER  NOT NULL,
  image_path nvarchar2(255),
  s_description_sk nvarchar2(255),
  l_description_sk clob,
  image_name nvarchar2(255),
  s_description_en nvarchar2(255),
  l_description_en clob,
  s_description_cz nvarchar2(255),
  l_description_cz clob,
  s_description_de nvarchar2(255),
  l_description_de clob
);

ALTER TABLE gallery  MODIFY (image_path DEFAULT NULL);
ALTER TABLE gallery  MODIFY (s_description_sk DEFAULT NULL);
ALTER TABLE gallery  MODIFY (image_name DEFAULT NULL);
ALTER TABLE gallery  MODIFY (s_description_en DEFAULT NULL);
ALTER TABLE gallery  MODIFY (s_description_cz DEFAULT NULL);
ALTER TABLE gallery  MODIFY (s_description_de DEFAULT NULL);
ALTER TABLE gallery add CONSTRAINT pk_gallery PRIMARY KEY (image_id);

CREATE SEQUENCE S_gallery START WITH 1;

CREATE TRIGGER T_gallery BEFORE INSERT ON gallery
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.image_id IS NULL THEN
			SELECT S_gallery.nextval into val FROM dual|
			:new.image_id:= val|
		END IF|
	END|
;


CREATE TABLE gallery_dimension (
  dimension_id INTEGER  NOT NULL,
  image_path nvarchar2(255),
  image_width INTEGER ,
  image_height INTEGER ,
  normal_width INTEGER  NOT NULL,
  normal_height INTEGER  NOT NULL
);

ALTER TABLE gallery_dimension  MODIFY (image_path DEFAULT 0);
ALTER TABLE gallery_dimension  MODIFY (image_width DEFAULT 0);
ALTER TABLE gallery_dimension  MODIFY (image_height DEFAULT 0);
ALTER TABLE gallery_dimension  MODIFY (normal_width DEFAULT 0);
ALTER TABLE gallery_dimension  MODIFY (normal_height DEFAULT 0);
ALTER TABLE gallery_dimension add CONSTRAINT pk_gallery_dimension PRIMARY KEY (dimension_id);

INSERT INTO gallery_dimension VALUES(5, '/images/gallery', 160, 120, 750, 560);

CREATE SEQUENCE S_gallery_dimension START WITH 6;

CREATE TRIGGER T_gallery_dimension BEFORE INSERT ON gallery_dimension
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.dimension_id IS NULL THEN
			SELECT S_gallery_dimension.nextval into val FROM dual|
			:new.dimension_id:= val|
		END IF|
	END|
;


CREATE TABLE groups (
  group_id INTEGER NOT NULL,
  group_name nvarchar2(64) NOT NULL,
  internal SMALLINT NOT NULL,
  parent_group_id INTEGER NOT NULL,
  navbar nvarchar2(255),
  default_doc_id INTEGER,
  temp_id INTEGER,
  sort_priority INTEGER NOT NULL,
  password_protected nvarchar2(255),
  menu_type SMALLINT,
  url_dir_name nvarchar2(64),
  sync_id INTEGER,
  sync_status INTEGER,
  html_head clob,
  logon_page_doc_id INTEGER
);

ALTER TABLE groups  MODIFY (group_name DEFAULT '');
ALTER TABLE groups  MODIFY (internal DEFAULT 0);
ALTER TABLE groups  MODIFY (parent_group_id DEFAULT 0);
ALTER TABLE groups  MODIFY (default_doc_id DEFAULT NULL);
ALTER TABLE groups  MODIFY (temp_id DEFAULT NULL);
ALTER TABLE groups  MODIFY (sort_priority DEFAULT 0);
ALTER TABLE groups  MODIFY (password_protected DEFAULT NULL);
ALTER TABLE groups  MODIFY (menu_type DEFAULT 1);
ALTER TABLE groups  MODIFY (url_dir_name DEFAULT NULL);
ALTER TABLE groups  MODIFY (sync_id DEFAULT 0);
ALTER TABLE groups  MODIFY (sync_status DEFAULT 0);
ALTER TABLE groups  MODIFY (logon_page_doc_id DEFAULT -1);
ALTER TABLE groups add CONSTRAINT pk_groups PRIMARY KEY (group_id);

INSERT INTO groups VALUES(1, 'Slovensky', 0, 0, 'Slovensky', 4, 1, 0, NULL, 1, NULL, 0, 0,NULL,-1);
INSERT INTO groups VALUES(2, 'Šablóny', 1, 20, '--', 6, 1, 1500, NULL, 1, NULL, 0, 0,NULL,-1);
INSERT INTO groups VALUES(3, 'Menu', 1, 20, '--', 19, 1, 1501, NULL, 1, NULL, 0, 0,NULL,-1);
INSERT INTO groups VALUES(4, 'Hlavičky-pätičky', 1, 20, '--', 21, 1, 1502, NULL, 1, NULL, 0, 0,NULL,-1);
INSERT INTO groups VALUES(5, 'English', 0, 0, 'English', 32, 1, 500, NULL, 1, NULL, 0, 0,NULL,-1);
INSERT INTO groups VALUES(20, 'System', 1, 0, 'System', 0, 1, 1000, NULL, 1, NULL, 0, 0,NULL,-1);

CREATE SEQUENCE S_groups START WITH 21;

CREATE TRIGGER T_groups BEFORE INSERT ON groups
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.group_id IS NULL THEN
			SELECT S_groups.nextval into val FROM dual|
			:new.group_id:= val|
		END IF|
	END|
;


CREATE TABLE groups_approve (
  approve_id INTEGER  NOT NULL,
  group_id INTEGER,
  user_id INTEGER,
  approve_mode INTEGER
);

ALTER TABLE groups_approve  MODIFY (group_id DEFAULT 0);
ALTER TABLE groups_approve  MODIFY (user_id DEFAULT 0);
ALTER TABLE groups_approve  MODIFY (approve_mode DEFAULT 0);
ALTER TABLE groups_approve add CONSTRAINT pk_groups_approve PRIMARY KEY (approve_id);

CREATE SEQUENCE S_groups_approve START WITH 1;

CREATE TRIGGER T_groups_approve BEFORE INSERT ON groups_approve
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.approve_id IS NULL THEN
			SELECT S_groups_approve.nextval into val FROM dual|
			:new.approve_id:= val|
		END IF|
	END|
;


CREATE TABLE inquiry (
  question_id INTEGER  NOT NULL,
  question_text nvarchar2(2000),
  hours INTEGER ,
  question_group nvarchar2(255),
  answer_text_ok nvarchar2(2000),
  answer_text_fail nvarchar2(2000),
  date_to DATE,
  question_active SMALLINT,
  date_from DATE
);

ALTER TABLE inquiry  MODIFY (question_text DEFAULT NULL);
ALTER TABLE inquiry  MODIFY (hours DEFAULT 0);
ALTER TABLE inquiry  MODIFY (question_group DEFAULT NULL);
ALTER TABLE inquiry  MODIFY (question_active DEFAULT -1);
ALTER TABLE inquiry add CONSTRAINT question_id UNIQUE (question_id);

INSERT INTO inquiry VALUES(1, 'Ako sa vám páči WebJET', 24, 'default', 'Ďakujeme, že ste sa zúčastnili ankety.', 'Ľutujeme, ale tejto ankety ste sa už zúčastnili.',NULL,-1,NULL);

CREATE SEQUENCE S_inquiry START WITH 2;

CREATE TRIGGER T_inquiry BEFORE INSERT ON inquiry
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.question_id IS NULL THEN
			SELECT S_inquiry.nextval into val FROM dual|
			:new.question_id:= val|
		END IF|
	END|
;


CREATE TABLE inquiry_answers (
  answer_id INTEGER  NOT NULL,
  question_id INTEGER NOT NULL,
  answer_text nvarchar2(255),
  answer_clicks INTEGER
);

ALTER TABLE inquiry_answers  MODIFY (question_id DEFAULT 0);
ALTER TABLE inquiry_answers  MODIFY (answer_text DEFAULT NULL);
ALTER TABLE inquiry_answers  MODIFY (answer_clicks DEFAULT 0);
ALTER TABLE inquiry_answers add CONSTRAINT pk_inquiry_answers PRIMARY KEY (answer_id);

INSERT INTO inquiry_answers VALUES(1, 1, 'Je super', 8);
INSERT INTO inquiry_answers VALUES(2, 1, 'Neviem, nepoznám', 3);

CREATE SEQUENCE S_inquiry_answers START WITH 3;

CREATE TRIGGER T_inquiry_answers BEFORE INSERT ON inquiry_answers
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.answer_id IS NULL THEN
			SELECT S_inquiry_answers.nextval into val FROM dual|
			:new.answer_id:= val|
		END IF|
	END|
;


CREATE TABLE questions_answers (
  qa_id INTEGER  NOT NULL,
  group_name nvarchar2(255) NOT NULL,
  category_name nvarchar2(64),
  question_date DATE,
  answer_date DATE,
  question clob,
  answer clob,
  from_name nvarchar2(255) NOT NULL,
  from_email nvarchar2(255),
  to_name nvarchar2(255),
  to_email nvarchar2(255),
  publish_on_web SMALLINT ,
  hash nvarchar2(255),
  allow_publish_on_web SMALLINT  NOT NULL
);

ALTER TABLE questions_answers  MODIFY (group_name DEFAULT '');
ALTER TABLE questions_answers  MODIFY (category_name DEFAULT NULL);
ALTER TABLE questions_answers  MODIFY (question_date DEFAULT NULL);
ALTER TABLE questions_answers  MODIFY (answer_date DEFAULT NULL);
ALTER TABLE questions_answers  MODIFY (from_name DEFAULT '');
ALTER TABLE questions_answers  MODIFY (from_email DEFAULT NULL);
ALTER TABLE questions_answers  MODIFY (to_name DEFAULT NULL);
ALTER TABLE questions_answers  MODIFY (to_email DEFAULT NULL);
ALTER TABLE questions_answers  MODIFY (publish_on_web DEFAULT 0);
ALTER TABLE questions_answers  MODIFY (hash DEFAULT NULL);
ALTER TABLE questions_answers  MODIFY (allow_publish_on_web DEFAULT 1);
ALTER TABLE questions_answers add CONSTRAINT pk_questions_answers PRIMARY KEY (qa_id);

CREATE SEQUENCE S_questions_answers START WITH 1;

CREATE TRIGGER T_questions_answers BEFORE INSERT ON questions_answers
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.qa_id IS NULL THEN
			SELECT S_questions_answers.nextval into val FROM dual|
			:new.qa_id:= val|
		END IF|
	END|
;



CREATE TABLE sms_addressbook (
  book_id INTEGER  NOT NULL,
  user_id INTEGER  NOT NULL,
  sms_name nvarchar2(128) NOT NULL,
  sms_number nvarchar2(32) NOT NULL
);

ALTER TABLE sms_addressbook  MODIFY (user_id DEFAULT 0);
ALTER TABLE sms_addressbook  MODIFY (sms_name DEFAULT '');
ALTER TABLE sms_addressbook  MODIFY (sms_number DEFAULT '');
ALTER TABLE sms_addressbook add CONSTRAINT pk_sms_addressbook PRIMARY KEY (book_id);

CREATE SEQUENCE S_sms_addressbook START WITH 1;

CREATE TRIGGER T_sms_addressbook BEFORE INSERT ON sms_addressbook
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.book_id IS NULL THEN
			SELECT S_sms_addressbook.nextval into val FROM dual|
			:new.book_id:= val|
		END IF|
	END|
;


CREATE TABLE sms_log (
  log_id INTEGER  NOT NULL,
  user_id INTEGER NOT NULL,
  user_ip nvarchar2(32) NOT NULL,
  sent_date DATE NOT NULL,
  sms_number nvarchar2(32) NOT NULL,
  sms_text nvarchar2(255) NOT NULL
);

ALTER TABLE sms_log  MODIFY (user_id DEFAULT 0);
ALTER TABLE sms_log  MODIFY (user_ip DEFAULT '');
ALTER TABLE sms_log  MODIFY (sent_date DEFAULT NULL);
ALTER TABLE sms_log  MODIFY (sms_number DEFAULT '');
ALTER TABLE sms_log  MODIFY (sms_text DEFAULT '');
ALTER TABLE sms_log add CONSTRAINT pk_sms_log PRIMARY KEY (log_id);

CREATE SEQUENCE S_sms_log START WITH 1;

CREATE TRIGGER T_sms_log BEFORE INSERT ON sms_log
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.log_id IS NULL THEN
			SELECT S_sms_log.nextval into val FROM dual|
			:new.log_id:= val|
		END IF|
	END|
;


CREATE TABLE stat_browser (
  year INTEGER  NOT NULL,
  week INTEGER  NOT NULL,
  browser_id nvarchar2(32),
  platform nvarchar2(25),
  subplatform nvarchar2(20),
  views INTEGER ,
  group_id INTEGER  NOT NULL
);

ALTER TABLE stat_browser  MODIFY (year DEFAULT 0);
ALTER TABLE stat_browser  MODIFY (week DEFAULT 0);
ALTER TABLE stat_browser  MODIFY (browser_id DEFAULT '0');
ALTER TABLE stat_browser  MODIFY (platform DEFAULT '0');
ALTER TABLE stat_browser  MODIFY (subplatform DEFAULT NULL);
ALTER TABLE stat_browser  MODIFY (views DEFAULT 0);
ALTER TABLE stat_browser  MODIFY (group_id DEFAULT 1);



CREATE TABLE stat_country (
  year INTEGER  NOT NULL,
  week INTEGER  NOT NULL,
  country_code nvarchar2(20) NOT NULL,
  views INTEGER  NOT NULL,
  group_id INTEGER  NOT NULL
);

ALTER TABLE stat_country  MODIFY (year DEFAULT 0);
ALTER TABLE stat_country  MODIFY (week DEFAULT 0);
ALTER TABLE stat_country  MODIFY (country_code DEFAULT 0);
ALTER TABLE stat_country  MODIFY (views DEFAULT 0);
ALTER TABLE stat_country  MODIFY (group_id DEFAULT 1);



CREATE TABLE stat_doc (
  year INTEGER ,
  week INTEGER ,
  doc_id INTEGER,
  views INTEGER ,
  in_count INTEGER ,
  out_count INTEGER ,
  view_time_sum INTEGER ,
  view_time_count INTEGER
);

ALTER TABLE stat_doc  MODIFY (year DEFAULT 0);
ALTER TABLE stat_doc  MODIFY (week DEFAULT 0);
ALTER TABLE stat_doc  MODIFY (doc_id DEFAULT 0);
ALTER TABLE stat_doc  MODIFY (views DEFAULT 1);
ALTER TABLE stat_doc  MODIFY (in_count DEFAULT 0);
ALTER TABLE stat_doc  MODIFY (out_count DEFAULT 0);
ALTER TABLE stat_doc  MODIFY (view_time_sum DEFAULT 0);
ALTER TABLE stat_doc  MODIFY (view_time_count DEFAULT 0);



CREATE TABLE stat_error (
  year INTEGER  default 0,
  week INTEGER  default 0,
  url nvarchar2(255),
  query_string nvarchar2(255),
  count INTEGER  default 0
);

ALTER TABLE stat_error  MODIFY (year DEFAULT 0);
ALTER TABLE stat_error  MODIFY (week DEFAULT 0);
ALTER TABLE stat_error  MODIFY (url DEFAULT '0');
ALTER TABLE stat_error  MODIFY (query_string DEFAULT '0');
ALTER TABLE stat_error  MODIFY (count DEFAULT 0);



CREATE TABLE stat_searchengine (
  search_date DATE NOT NULL,
  server nvarchar2(16) NOT NULL,
  query nvarchar2(64) NOT NULL,
  doc_id INTEGER  NOT NULL,
  remote_host nvarchar2(255)
);

ALTER TABLE stat_searchengine  MODIFY (search_date DEFAULT NULL);
ALTER TABLE stat_searchengine  MODIFY (server DEFAULT '');
ALTER TABLE stat_searchengine  MODIFY (query DEFAULT '');
ALTER TABLE stat_searchengine  MODIFY (doc_id DEFAULT 0);



CREATE TABLE stat_site_days (
  year INTEGER,
  week INTEGER,
  views_mo INTEGER,
  sessions_mo INTEGER,
  views_tu INTEGER,
  sessions_tu INTEGER,
  views_we INTEGER,
  sessions_we INTEGER,
  views_th INTEGER,
  sessions_th INTEGER,
  views_fr INTEGER,
  sessions_fr INTEGER,
  views_sa INTEGER,
  sessions_sa INTEGER,
  views_su INTEGER,
  sessions_su INTEGER,
  view_time_sum INTEGER,
  view_time_count INTEGER,
  group_id INTEGER  NOT NULL
);

ALTER TABLE stat_site_days  MODIFY (year DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (week DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (views_mo DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (sessions_mo DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (views_tu DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (sessions_tu DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (views_we DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (sessions_we DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (views_th DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (sessions_th DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (views_fr DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (sessions_fr DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (views_sa DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (sessions_sa DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (views_su DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (sessions_su DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (view_time_sum DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (view_time_count DEFAULT 0);
ALTER TABLE stat_site_days  MODIFY (group_id DEFAULT 1);



CREATE TABLE stat_site_hours (
  year INTEGER NOT NULL,
  week INTEGER NOT NULL,
  views_0 INTEGER,
  views_1 INTEGER,
  views_2 INTEGER,
  views_3 INTEGER,
  views_4 INTEGER,
  views_5 INTEGER,
  views_6 INTEGER,
  views_7 INTEGER,
  views_8 INTEGER,
  views_9 INTEGER,
  views_10 INTEGER,
  views_11 INTEGER,
  views_12 INTEGER,
  views_13 INTEGER,
  views_14 INTEGER,
  views_15 INTEGER,
  views_16 INTEGER,
  views_17 INTEGER,
  views_18 INTEGER,
  views_19 INTEGER,
  views_20 INTEGER,
  views_21 INTEGER,
  views_22 INTEGER,
  views_23 INTEGER,
  sessions_0 INTEGER,
  sessions_1 INTEGER,
  sessions_2 INTEGER,
  sessions_3 INTEGER,
  sessions_4 INTEGER,
  sessions_5 INTEGER,
  sessions_6 INTEGER,
  sessions_7 INTEGER,
  sessions_8 INTEGER,
  sessions_9 INTEGER,
  sessions_10 INTEGER,
  sessions_11 INTEGER,
  sessions_12 INTEGER,
  sessions_13 INTEGER,
  sessions_14 INTEGER,
  sessions_15 INTEGER,
  sessions_16 INTEGER,
  sessions_17 INTEGER,
  sessions_18 INTEGER,
  sessions_19 INTEGER,
  sessions_20 INTEGER,
  sessions_21 INTEGER,
  sessions_22 INTEGER,
  sessions_23 INTEGER,
  group_id INTEGER NOT NULL
);

ALTER TABLE stat_site_hours  MODIFY (year DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (week DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_0 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_1 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_2 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_3 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_4 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_5 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_6 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_7 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_8 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_9 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_10 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_11 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_12 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_13 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_14 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_15 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_16 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_17 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_18 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_19 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_20 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_21 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_22 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (views_23 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_0 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_1 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_2 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_3 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_4 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_5 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_6 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_7 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_8 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_9 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_10 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_11 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_12 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_13 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_14 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_15 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_16 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_17 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_18 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_19 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_20 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_21 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_22 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (sessions_23 DEFAULT 0);
ALTER TABLE stat_site_hours  MODIFY (group_id DEFAULT 1);



CREATE TABLE stat_userlogon (
  year INTEGER NOT NULL,
  week INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  views INTEGER,
  logon_time DATE,
  view_minutes INTEGER,
  hostname nvarchar2(255)
);

ALTER TABLE stat_userlogon  MODIFY (year DEFAULT 0);
ALTER TABLE stat_userlogon  MODIFY (week DEFAULT 0);
ALTER TABLE stat_userlogon  MODIFY (user_id DEFAULT 0);
ALTER TABLE stat_userlogon  MODIFY (views DEFAULT 1);
ALTER TABLE stat_userlogon  MODIFY (logon_time DEFAULT NULL);
ALTER TABLE stat_userlogon  MODIFY (view_minutes DEFAULT 0);
ALTER TABLE stat_userlogon  MODIFY (hostname DEFAULT NULL);



CREATE TABLE templates (
  temp_id INTEGER NOT NULL,
  temp_name nvarchar2(64) NOT NULL,
  forward nvarchar2(64) NOT NULL,
  lng nvarchar2(16) NOT NULL,
  header_doc_id INTEGER NOT NULL,
  footer_doc_id INTEGER NOT NULL,
  after_body_data clob,
  css nvarchar2(64),
  menu_doc_id INTEGER NOT NULL,
  right_menu_doc_id INTEGER,
  base_css_path nvarchar2(255) NOT NULL
);

ALTER TABLE templates  MODIFY (temp_name DEFAULT '');
ALTER TABLE templates  MODIFY (forward DEFAULT '');
ALTER TABLE templates  MODIFY (lng DEFAULT 'sk');
ALTER TABLE templates  MODIFY (header_doc_id DEFAULT 0);
ALTER TABLE templates  MODIFY (footer_doc_id DEFAULT 0);
ALTER TABLE templates  MODIFY (after_body_data DEFAULT NULL);
ALTER TABLE templates  MODIFY (menu_doc_id DEFAULT -1);
ALTER TABLE templates  MODIFY (right_menu_doc_id DEFAULT -1);
ALTER TABLE templates  MODIFY (base_css_path DEFAULT '/css/page.css');
ALTER TABLE templates add CONSTRAINT pk_templates PRIMARY KEY (temp_id);

INSERT INTO templates VALUES(1, 'Generic', 'tmp_generic.jsp', 'sk', 1, 3, '', '', 2,-1,'/css/page.css');

CREATE SEQUENCE S_templates START WITH 2;

CREATE TRIGGER T_templates BEFORE INSERT ON templates
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.temp_id IS NULL THEN
			SELECT S_templates.nextval into val FROM dual|
			:new.temp_id:= val|
		END IF|
	END|
;

CREATE TABLE user_disabled_items (
  user_id INTEGER NOT NULL,
  item_name nvarchar2(32) NOT NULL
);

ALTER TABLE user_disabled_items  MODIFY (user_id DEFAULT 0);
ALTER TABLE user_disabled_items  MODIFY (item_name DEFAULT '');

INSERT INTO user_disabled_items VALUES(1, 'editorMiniEdit');



CREATE TABLE user_group_verify (
  verify_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  user_groups nvarchar2(255) NOT NULL,
  hash nvarchar2(32) NOT NULL,
  create_date DATE NOT NULL,
  verify_date DATE,
  email nvarchar2(255) NOT NULL
);

ALTER TABLE user_group_verify  MODIFY (user_id DEFAULT 0);
ALTER TABLE user_group_verify  MODIFY (user_groups DEFAULT '');
ALTER TABLE user_group_verify  MODIFY (hash DEFAULT '');
ALTER TABLE user_group_verify  MODIFY (create_date DEFAULT NULL);
ALTER TABLE user_group_verify  MODIFY (verify_date DEFAULT NULL);
ALTER TABLE user_group_verify  MODIFY (email DEFAULT '');
ALTER TABLE user_group_verify add CONSTRAINT pk_user_group_verify PRIMARY KEY (verify_id);

CREATE SEQUENCE S_user_group_verify START WITH 1;

CREATE TRIGGER T_user_group_verify BEFORE INSERT ON user_group_verify
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.verify_id IS NULL THEN
			SELECT S_user_group_verify.nextval into val FROM dual|
			:new.verify_id:= val|
		END IF|
	END|
;


CREATE TABLE user_groups (
  user_group_id INTEGER NOT NULL,
  user_group_name nvarchar2(255) NOT NULL,
  user_group_type SMALLINT NOT NULL,
  user_group_comment nvarchar2(2000),
  require_approve SMALLINT NOT NULL,
  email_doc_id INTEGER NOT NULL,
  allow_user_edit SMALLINT
);

ALTER TABLE user_groups  MODIFY (user_group_name DEFAULT '');
ALTER TABLE user_groups  MODIFY (user_group_type DEFAULT 0);
ALTER TABLE user_groups  MODIFY (require_approve DEFAULT 0);
ALTER TABLE user_groups  MODIFY (email_doc_id DEFAULT -1);
ALTER TABLE user_groups  MODIFY (allow_user_edit DEFAULT 0);
ALTER TABLE user_groups add CONSTRAINT pk_user_groups PRIMARY KEY (user_group_id);

INSERT INTO user_groups VALUES(1, 'VIP Klienti', 0, NULL, 0, -1,0);
INSERT INTO user_groups VALUES(2, 'Obchodní partneri', 0, NULL, 0, -1,0);
INSERT INTO user_groups VALUES(3, 'Redaktor', 0, NULL, 0, -1,0);

CREATE SEQUENCE S_user_groups START WITH 4;

CREATE TRIGGER T_user_groups BEFORE INSERT ON user_groups
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.user_group_id IS NULL THEN
			SELECT S_user_groups.nextval into val FROM dual|
			:new.user_group_id:= val|
		END IF|
	END|
;


CREATE TABLE users (
  user_id INTEGER NOT NULL,
  title nvarchar2(16),
  first_name nvarchar2(128),
  last_name nvarchar2(255),
  login nvarchar2(16) NOT NULL,
  password nvarchar2(128) NOT NULL,
  is_admin SMALLINT NOT NULL,
  user_groups nvarchar2(255),
  company nvarchar2(255),
  adress nvarchar2(255),
  city nvarchar2(255),
  email nvarchar2(255),
  PSC nvarchar2(20),
  country nvarchar2(255),
  phone nvarchar2(255),
  authorized SMALLINT,
  editable_groups nvarchar2(255),
  editable_pages nvarchar2(255),
  writable_folders nvarchar2(2000),
  last_logon DATE,
  module_perms nvarchar2(255),
  disabled_items nvarchar2(255),
  reg_date DATE,
  field_a nvarchar2(255),
  field_b nvarchar2(255),
  field_c nvarchar2(255),
  field_d nvarchar2(255),
  field_e nvarchar2(255),
  date_of_birth DATE,
  sex_male SMALLINT,
  photo nvarchar2(255),
  signature nvarchar2(255),
  forum_rank INTEGER NOT NULL,
  rating_rank INTEGER NOT NULL
);

ALTER TABLE users  MODIFY (title DEFAULT NULL);
ALTER TABLE users  MODIFY (first_name DEFAULT NULL);
ALTER TABLE users  MODIFY (last_name DEFAULT NULL);
ALTER TABLE users  MODIFY (login DEFAULT '');
ALTER TABLE users  MODIFY (password DEFAULT '');
ALTER TABLE users  MODIFY (is_admin DEFAULT 0);
ALTER TABLE users  MODIFY (user_groups DEFAULT NULL);
ALTER TABLE users  MODIFY (company DEFAULT NULL);
ALTER TABLE users  MODIFY (adress DEFAULT NULL);
ALTER TABLE users  MODIFY (city DEFAULT NULL);
ALTER TABLE users  MODIFY (email DEFAULT NULL);
ALTER TABLE users  MODIFY (PSC DEFAULT NULL);
ALTER TABLE users  MODIFY (country DEFAULT NULL);
ALTER TABLE users  MODIFY (phone DEFAULT NULL);
ALTER TABLE users  MODIFY (authorized DEFAULT NULL);
ALTER TABLE users  MODIFY (editable_groups DEFAULT NULL);
ALTER TABLE users  MODIFY (editable_pages DEFAULT NULL);
ALTER TABLE users  MODIFY (last_logon DEFAULT NULL);
ALTER TABLE users  MODIFY (module_perms DEFAULT NULL);
ALTER TABLE users  MODIFY (disabled_items DEFAULT NULL);
ALTER TABLE users  MODIFY (reg_date DEFAULT NULL);
ALTER TABLE users  MODIFY (field_a DEFAULT NULL);
ALTER TABLE users  MODIFY (field_b DEFAULT NULL);
ALTER TABLE users  MODIFY (field_c DEFAULT NULL);
ALTER TABLE users  MODIFY (field_d DEFAULT NULL);
ALTER TABLE users  MODIFY (field_e DEFAULT NULL);
ALTER TABLE users  MODIFY (sex_male DEFAULT 1);
ALTER TABLE users  MODIFY (forum_rank DEFAULT 0);
ALTER TABLE users  MODIFY (rating_rank DEFAULT 0);
ALTER TABLE users add CONSTRAINT pk_users PRIMARY KEY (user_id);

INSERT INTO users VALUES(1, '', 'WebJET', 'Administrátor', 'admin', 'd7ed8dc6fc9b4a8c3b442c3dcc35bfe4', 1, NULL, 'InterWay, a. s.', '', '', 'web.spam@interway.sk', '', 'Slovakia', '02/32788888', 1, '', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,NULL,1,NULL,NULL,0,0);

CREATE SEQUENCE S_users START WITH 2;

CREATE TRIGGER T_users BEFORE INSERT ON users
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.user_id IS NULL THEN
			SELECT S_users.nextval into val FROM dual|
			:new.user_id:= val|
		END IF|
	END|
;


CREATE TABLE tips_of_the_day (
  tip_id INTEGER,
  tip_group nvarchar2(255) NOT NULL,
  tip_text clob NOT NULL
);

ALTER TABLE tips_of_the_day add CONSTRAINT pk_tips_of_the_day PRIMARY KEY (tip_id);

CREATE SEQUENCE S_tips_of_the_day START WITH 1;

CREATE TRIGGER T_tips_of_the_day BEFORE INSERT ON tips_of_the_day
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.tip_id IS NULL THEN
			SELECT S_tips_of_the_day.nextval into val FROM dual|
			:new.tip_id:= val|
		END IF|
	END|
;


CREATE TABLE alarm_action (
  alarm_id INTEGER NOT NULL,
  days INTEGER, doc_id INTEGER
);



CREATE TABLE user_alarm (
  user_id INTEGER NOT NULL,
  alarm_id INTEGER,
  warning INTEGER,
  send_date DATE
);

ALTER TABLE user_alarm add CONSTRAINT pk_user_alarm PRIMARY KEY (user_id);



CREATE TABLE calendar_name_in_year (
  day INTEGER NOT NULL,
  month INTEGER NOT NULL,
  name nvarchar2(200),
  calendar_id INTEGER NOT NULL,
  lng nvarchar2(10) NOT NULL
);


ALTER TABLE calendar_name_in_year add CONSTRAINT pk_calendar_name_in_year PRIMARY KEY (calendar_id);

CREATE SEQUENCE S_calendar_name_in_year START WITH 1;

CREATE TRIGGER T_calendar_name_in_year BEFORE INSERT ON calendar_name_in_year
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.calendar_id IS NULL THEN
			SELECT S_calendar_name_in_year.nextval into val FROM dual|
			:new.calendar_id:= val|
		END IF|
	END|
;


CREATE TABLE banner_banners (
  banner_id INTEGER NOT NULL,
  banner_type INTEGER NOT NULL,
  banner_group nvarchar2(128),
  priority INTEGER,
  active SMALLINT NOT NULL,
  banner_location nvarchar2(255),
  banner_redirect nvarchar2(255),
  width INTEGER,
  height INTEGER,
  html_code nvarchar2(2000),
  date_from DATE,
  date_to DATE,
  max_views INTEGER,
  max_clicks INTEGER,
  stat_views INTEGER,
  stat_clicks INTEGER,
  stat_date DATE
);

ALTER TABLE banner_banners add CONSTRAINT pk_banner_banners PRIMARY KEY (banner_id);

CREATE TABLE banner_stat_clicks(
  id INTEGER NOT NULL,
  banner_id INTEGER NOT NULL,
  insert_date DATE NOT NULL,
  ip nvarchar2(16),
  host nvarchar2(128),
  clicks INTEGER
);

ALTER TABLE banner_stat_clicks add CONSTRAINT pk_banner_stat_clicks PRIMARY KEY (id);

CREATE SEQUENCE S_banner_stat_clicks START WITH 1;

CREATE TRIGGER T_banner_stat_clicks BEFORE INSERT ON banner_stat_clicks
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.id IS NULL THEN
			SELECT S_banner_stat_clicks.nextval into val FROM dual|
			:new.id:= val|
		END IF|
	END|
;


CREATE TABLE banner_stat_views(
  id INTEGER NOT NULL,
  banner_id INTEGER NOT NULL,
  insert_date DATE NOT NULL,
  ip nvarchar2(16),
  host nvarchar2(128),
  views INTEGER
);

ALTER TABLE banner_stat_views add CONSTRAINT pk_banner_stat_views PRIMARY KEY (id);

CREATE SEQUENCE S_banner_stat_views START WITH 1;

CREATE TRIGGER T_banner_stat_views BEFORE INSERT ON banner_stat_views
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.id IS NULL THEN
			SELECT S_banner_stat_views.nextval into val FROM dual|
			:new.id:= val|
		END IF|
	END|
;


CREATE TABLE rating (
  rating_id INTEGER NOT NULL,
  doc_id INTEGER,
  user_id INTEGER,
  rating_value INTEGER,
  insert_date DATE
);

ALTER TABLE rating add CONSTRAINT pk_rating PRIMARY KEY (rating_id);

CREATE SEQUENCE S_rating START WITH 1;

CREATE TRIGGER T_rating BEFORE INSERT ON rating
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.rating_id IS NULL THEN
			SELECT S_rating.nextval into val FROM dual|
			:new.rating_id:= val|
		END IF|
	END|
;


CREATE TABLE pkey_generator (
  name nvarchar2(255) NOT NULL,
  value INTEGER default 1,
  table_name nvarchar2(255),
  table_pkey_name nvarchar2(255)
);

ALTER TABLE pkey_generator add CONSTRAINT pk_pkey_generator PRIMARY KEY (name);

INSERT INTO pkey_generator VALUES('documents', 1, 'documents', 'doc_id');
INSERT INTO pkey_generator (name, value, table_name, table_pkey_name) VALUES ('stat_browser_id', 1, NULL, NULL);
INSERT INTO pkey_generator (name, value, table_name, table_pkey_name) VALUES ('stat_session_id', 1, NULL, NULL);
INSERT INTO pkey_generator (name, value, table_name, table_pkey_name) VALUES ('banner_banners', 1, 'banner_banners', 'banner_id');



CREATE TABLE perex_groups (
  perex_group_id INTEGER NOT NULL,
  perex_group_name nvarchar2(255) NOT NULL,
  related_pages nvarchar2(2000)
);


ALTER TABLE perex_groups add CONSTRAINT pk_perex_groups PRIMARY KEY (perex_group_id);

CREATE SEQUENCE S_perex_groups START WITH 1;

CREATE TRIGGER T_perex_groups BEFORE INSERT ON perex_groups
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.perex_group_id IS NULL THEN
			SELECT S_perex_groups.nextval into val FROM dual|
			:new.perex_group_id:= val|
		END IF|
	END|
;


CREATE TABLE chat_rooms (
  room_id INTEGER NOT NULL,
  room_name nvarchar2 (255) NOT NULL,
  room_description clob,
  room_class nvarchar2 (255) NOT NULL,
  max_users INTEGER  NOT NULL,
  allow_similar_names SMALLINT NOT NULL,
  lng nvarchar2 (3) NOT NULL,
  moderator_name nvarchar2 (128),
  moderator_username nvarchar2 (64),
  moderator_password nvarchar2 (64),
  hide_in_public_list SMALLINT NOT NULL
);

ALTER TABLE chat_rooms  MODIFY (max_users DEFAULT 50);
ALTER TABLE chat_rooms  MODIFY (allow_similar_names DEFAULT 0);
ALTER TABLE chat_rooms  MODIFY (lng DEFAULT 'sk');
ALTER TABLE chat_rooms  MODIFY (hide_in_public_list DEFAULT 0);


CREATE SEQUENCE S_chat_rooms START WITH 1;

CREATE TRIGGER T_chat_rooms BEFORE INSERT ON chat_rooms
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.room_id IS NULL THEN
			SELECT S_chat_rooms.nextval into val FROM dual|
			:new.room_id:= val|
		END IF|
	END|
;

INSERT INTO chat_rooms (room_name, room_description, room_class, max_users, allow_similar_names, lng, moderator_name, moderator_username, moderator_password) VALUES ('pokec', 'Volný pokec', 'sk.iway.iwcm.components.chat.HtmlChatRoom', 50, 0, 'sk', NULL, NULL, NULL);
INSERT INTO chat_rooms (room_name, room_description, room_class, max_users, allow_similar_names, lng, moderator_name, moderator_username, moderator_password) VALUES ('moderovaný pokec', 'Moderovaný pokec', 'sk.iway.iwcm.components.chat.ModeratedRoom', 50, 0, 'sk', 'Moderátor', 'moderator', 'heslo');


CREATE TABLE crontab (
  id INTEGER NOT NULL,
  second nvarchar2(64),
  minute nvarchar2(64),
  hour nvarchar2(64) ,
  dayofmonth nvarchar2(64),
  month nvarchar2(64),
  dayofweek nvarchar2(64),
  year nvarchar2(64),
  task nvarchar2(255),
  extrainfo nvarchar2(255),
  businessDays SMALLINT
);

ALTER TABLE crontab  MODIFY (second DEFAULT '0');
ALTER TABLE crontab  MODIFY (minute DEFAULT '*');
ALTER TABLE crontab  MODIFY (hour DEFAULT '*');
ALTER TABLE crontab  MODIFY (dayofmonth DEFAULT '*');
ALTER TABLE crontab  MODIFY (month DEFAULT '*');
ALTER TABLE crontab  MODIFY (dayofweek DEFAULT '*');
ALTER TABLE crontab  MODIFY (year DEFAULT '*');
ALTER TABLE crontab  MODIFY (task DEFAULT NULL);
ALTER TABLE crontab  MODIFY (extrainfo DEFAULT NULL);
ALTER TABLE crontab  MODIFY (businessDays DEFAULT 1);
ALTER TABLE crontab add CONSTRAINT pk_crontab PRIMARY KEY (id);

CREATE SEQUENCE S_crontab START WITH 1;

CREATE TRIGGER T_crontab BEFORE INSERT ON crontab
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.id IS NULL THEN
			SELECT S_crontab.nextval into val FROM dual|
			:new.id:= val|
		END IF|
	END|
;

INSERT INTO crontab (second, minute, hour, dayofmonth, month, dayofweek, year, task, extrainfo, businessDays) VALUES ('0', '*/10', '*', '*', '*', '*', '*', 'sk.iway.iwcm.calendar.CalendarDB', NULL, 1);
INSERT INTO crontab (second, minute, hour, dayofmonth, month, dayofweek, year, task, extrainfo, businessDays) VALUES ('0', '10', '6', '*', '*', '*', '*', 'sk.iway.iwcm.users.RegAlarm', NULL, 1);


CREATE TABLE stat_views (
view_id INTEGER NOT NULL,
browser_id INTEGER,
session_id INTEGER,
doc_id INTEGER,
last_doc_id INTEGER,
view_time DATE NULL
);

ALTER TABLE stat_views add CONSTRAINT pk_stat_views PRIMARY KEY (view_id);

CREATE SEQUENCE S_stat_views START WITH 1;

CREATE TRIGGER T_stat_views BEFORE INSERT ON stat_views
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.view_id IS NULL THEN
			SELECT S_stat_views.nextval into val FROM dual|
			:new.view_id:= val|
		END IF|
	END|
;


CREATE TABLE stat_from (
  from_id INTEGER NOT NULL,
  browser_id INTEGER,
  session_id  INTEGER,
  referer_server_name nvarchar2(255),
  referer_url nvarchar2(255),
  from_time DATE
);

ALTER TABLE stat_from add CONSTRAINT pk_stat_from PRIMARY KEY (from_id);

CREATE SEQUENCE S_stat_from START WITH 1;

CREATE TRIGGER T_stat_from BEFORE INSERT ON stat_from
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.from_id IS NULL THEN
			SELECT S_stat_from.nextval into val FROM dual|
			:new.from_id:= val|
		END IF|
	END|
;


CREATE TABLE forum (
  id INTEGER NOT NULL,
  doc_id INTEGER,
  active SMALLINT NOT NULL,
  date_from DATE,
  date_to DATE,
  hours_after_last_message INTEGER,
  message_confirmation SMALLINT NOT NULL,
  approve_email nvarchar2(255),
  notif_email nvarchar2(255),
  send_answer_notif SMALLINT,
  advertisement_type SMALLINT,
  message_board SMALLINT NOT NULL
);

ALTER TABLE forum  MODIFY (message_confirmation DEFAULT 0);
ALTER TABLE forum  MODIFY (send_answer_notif DEFAULT 0);
ALTER TABLE forum add CONSTRAINT pk_forum PRIMARY KEY (id);
ALTER TABLE forum  MODIFY (message_board DEFAULT 0);


CREATE SEQUENCE S_forum START WITH 1;

CREATE TRIGGER T_forum BEFORE INSERT ON forum
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.id IS NULL THEN
			SELECT S_forum.nextval into val FROM dual|
			:new.id:= val|
		END IF|
	END|
;


CREATE TABLE dirprop (
  dir_url nvarchar2 (255) NOT NULL,
  index_fulltext SMALLINT NOT NULL,
  password_protected nvarchar2 (255),
  logon_doc_id INTEGER NOT NULL
);

ALTER TABLE dirprop  MODIFY (index_fulltext DEFAULT 0);
ALTER TABLE dirprop  MODIFY (logon_doc_id DEFAULT -1);
ALTER TABLE dirprop add CONSTRAINT pk_dirprop PRIMARY KEY (dir_url);


CREATE TABLE bazar_groups (
  group_id INTEGER NOT NULL,
  parent_group_id INTEGER,
  group_name nvarchar2(255),
  allow_ad_insert SMALLINT,
  require_approve SMALLINT
);

ALTER TABLE bazar_groups add CONSTRAINT pk_bazar_groups PRIMARY KEY (group_id);

CREATE SEQUENCE S_bazar_groups START WITH 1;

CREATE TRIGGER T_bazar_groups BEFORE INSERT ON bazar_groups
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.group_id IS NULL THEN
			SELECT S_bazar_groups.nextval into val FROM dual|
			:new.group_id:= val|
		END IF|
	END|
;


CREATE TABLE bazar_advertisements (
  ad_id INTEGER NOT NULL,
  group_id INTEGER,
  user_id INTEGER,
  description clob,
  contact nvarchar2(255),
  confirmation SMALLINT,
  image nvarchar2(255),
  price nvarchar2(255),
  date_insert DATE
);

ALTER TABLE bazar_advertisements add CONSTRAINT pk_bazar_advertisements PRIMARY KEY (ad_id);

CREATE SEQUENCE S_bazar_advertisements START WITH 1;

CREATE TRIGGER T_bazar_advertisements BEFORE INSERT ON bazar_advertisements
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.ad_id IS NULL THEN
			SELECT S_bazar_advertisements.nextval into val FROM dual|
			:new.ad_id:= val|
		END IF|
	END|
;



CREATE TABLE doc_subscribe (
  subscribe_id INTEGER NOT NULL,
  doc_id INTEGER, first_name nvarchar2(255),
  last_name nvarchar2(255),
  email nvarchar2(255) NOT NULL,
  user_id INTEGER
);

ALTER TABLE doc_subscribe  MODIFY (user_id DEFAULT -1);
ALTER TABLE doc_subscribe add CONSTRAINT pk__doc_subscribe PRIMARY KEY (subscribe_id);

CREATE SEQUENCE S_doc_subscribe START WITH 1;

CREATE TRIGGER T_doc_subscribe BEFORE INSERT ON doc_subscribe
FOR EACH ROW
	DECLARE
	    val INTEGER|
	BEGIN
		IF :new.subscribe_id IS NULL THEN
			SELECT S_doc_subscribe.nextval into val FROM dual|
			:new.subscribe_id:= val|
		END IF|
	END|
;


CREATE TABLE dictionary (
  dictionary_id INTEGER NOT NULL,
  name nvarchar2(255) NOT NULL,
  dictionary_group nvarchar2(128) NOT NULL,
  value clob NOT NULL
);


ALTER TABLE dictionary add CONSTRAINT pk_dictionary PRIMARY KEY (dictionary_id);
