/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table _adminlog_
# ------------------------------------------------------------

DROP TABLE IF EXISTS `_adminlog_`;

CREATE TABLE `_adminlog_` (
  `log_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `log_type` int(4) DEFAULT 0,
  `user_id` int(4) NOT NULL DEFAULT -1,
  `create_date` datetime NOT NULL,
  `description` text DEFAULT NULL,
  `sub_id1` int(4) DEFAULT 0,
  `sub_id2` int(4) DEFAULT 0,
  `ip` varchar(128) DEFAULT NULL,
  `hostname` varchar(255) DEFAULT NULL,
  `operation_type` int(11) DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `IX_adminlog_userid` (`user_id`),
  KEY `IX_adminlog_logtype` (`log_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `_adminlog_` WRITE;
/*!40000 ALTER TABLE `_adminlog_` DISABLE KEYS */;

/*!40000 ALTER TABLE `_adminlog_` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table _conf_
# ------------------------------------------------------------

DROP TABLE IF EXISTS `_conf_`;

CREATE TABLE `_conf_` (
  `name` varchar(255) NOT NULL DEFAULT '',
  `value` text NOT NULL,
  `date_changed` datetime DEFAULT NULL,
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `_conf_` WRITE;
/*!40000 ALTER TABLE `_conf_` DISABLE KEYS */;

INSERT INTO `_conf_` (`name`, `value`, `date_changed`)
VALUES
	('defaultDisableUpload','false',NULL),
	('disableWebJETToolbar','true',NULL),
	('inlineEditingEnabled','true',NULL),
	('installName','blank',NULL),
	('logLevel','debug',NULL),
	('showDocActionAllowedDocids','4',NULL),
	('smtpServer','smtp.local',NULL);

/*!40000 ALTER TABLE `_conf_` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table _conf_prepared_
# ------------------------------------------------------------

DROP TABLE IF EXISTS `_conf_prepared_`;

CREATE TABLE `_conf_prepared_` (
  `name` varchar(255) NOT NULL DEFAULT '',
  `value` text NOT NULL,
  `date_changed` datetime DEFAULT NULL,
  `date_prepared` datetime DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `date_published` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table _db_
# ------------------------------------------------------------

DROP TABLE IF EXISTS `_db_`;

CREATE TABLE `_db_` (
  `id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `create_date` date NOT NULL DEFAULT '2000-01-01',
  `note` varchar(255) NOT NULL DEFAULT '',
  KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `_db_` WRITE;
/*!40000 ALTER TABLE `_db_` DISABLE KEYS */;

INSERT INTO `_db_` (`id`, `create_date`, `note`)
VALUES
	(1,'2003-12-06','ukladanie poznamky a prihlaseneho usera k formularom'),
	(2,'2003-12-12','sposob zobrazenia menu pre adresar'),
	(3,'2003-12-21','atributy suborov'),
	(4,'2003-12-29','od teraz sa kontroluje aj admin, ci je autorizovany, takze nastavime default'),
	(5,'2004-01-04','uklada k formularu aj docid (ak sa podari zistit)'),
	(6,'2004-01-09','typ skupiny pouzivatelov, 0=perms, 1=email, 2=...'),
	(7,'2004-01-10','email je mozne posielat uz len ako URL, text sa priamo napisat neda'),
	(8,'2004-01-11','verifikacia subscribe k email newslettrom, po autorizacii emailom sa user_groups zapise do tabulky users'),
	(9,'2004-01-13','zoznam foldrov (/images/nieco...) do ktorych ma user pravo nahravat obrazky a subory'),
	(10,'2004-01-25','volne pouzitelne polia pre kalendar podujati'),
	(11,'2004-02-11','casova notifikacia pre kalendar podujati'),
	(12,'2004-02-15','virtualne cesty k strankam, napr. www.server.sk/products'),
	(13,'2004-02-17','uvodny text notifikacie kalendara, moznost poslat SMS'),
	(14,'2004-02-24','ak je true, dava navstevnik suhlas na zobrazenie na webe'),
	(15,'2004-02-28','urychlenie statistiky'),
	(16,'2004-01-03','zvacsenie poli'),
	(17,'2004-03-03','urychlenie nacitania virtual paths'),
	(18,'2004-03-05','konfiguracia webjetu (namiesto web.xml)'),
	(19,'2004-03-07','disabled items pouzivatelov'),
	(20,'2004-03-07','rozdelenie full name na meno a priezvisko'),
	(21,'2004-03-08','volne pouzitelne polozky'),
	(22,'2004-03-12','url nazov adresara'),
	(23,'2004-03-15','implemetacia rozdelenia full name'),
	(24,'2004-03-15','Konverzia pristupovych prav'),
	(25,'2004-03-18','custom zmena textov v properties suboroch'),
	(26,'2004-03-27','uprava statistik (eviduje sa id adresara)'),
	(27,'2004-03-28','statistika query vo vyhladavacoch'),
	(28,'2004-04-05','mod schvalovania adresara (0=approve, 1=notify, 2=none)'),
	(29,'2004-03-05','konfiguracia webjetu (namiesto web.xml)'),
	(30,'2004-03-07','disabled items pouzivatelov'),
	(31,'2004-03-07','rozdelenie full name na meno a priezvisko'),
	(32,'2004-03-08','volne pouzitelne polozky'),
	(33,'2004-03-12','url nazov adresara'),
	(34,'2004-03-18','custom zmena textov v properties suboroch'),
	(35,'2004-03-27','uprava statistik (eviduje sa id adresara)'),
	(36,'2004-03-28','statistika query vo vyhladavacoch'),
	(37,'2004-04-05','mod schvalovania adresara (0=approve, 1=notify, 2=none)'),
	(38,'2004-05-01','id a stav synchronizacie (status: 0=novy, 1=updated, 2=synchronized)'),
	(39,'2004-05-02','konfiguracia custom modulov'),
	(40,'2004-05-03','modul posielania SMS sprav'),
	(41,'2004-05-09','vyzadovanie schvalovania registracie, doc_id pre zasielany email'),
	(42,'2024-10-24','18.5.2004 [jeeff] vo vyhladavani statistiky sa eviduje remote host'),
	(43,'2024-10-24','24.5.2004 [jeeff] tabulka s tipmi dna'),
	(44,'2024-10-24','9.6.2004 [joruz] zoznam alarmov pre notifikaciu registracie'),
	(45,'2024-10-24','9.6.2004 [joruz] alarm pouzivatela pre notifikaciu registracie'),
	(46,'2024-10-24','9.8.2004 [jeeff] html kod do hlavicky pre adresar'),
	(47,'2024-10-24','10.8.2004 [jeeff] kalendar s meninami'),
	(48,'2024-10-24','10.8.2004 [jeeff] banner system - banner_banners'),
	(49,'2024-10-24','10.8.2004 [jeeff] banner system - banner_stat_clicks'),
	(50,'2024-10-24','10.8.2004 [jeeff] banner system - banner_stat_views'),
	(51,'2024-10-24','18.8.2004 [joruz] casovanie ankiet-zaciatok'),
	(52,'2024-10-24','18.8.2004 [joruz] casovanie ankiet-koniec'),
	(53,'2024-10-24','22.8.2004 [jeeff] id stranky s prihlasovacim dialogom'),
	(54,'2024-10-24','22.8.2004 [jeeff] id stranky s prihlasovacim dialogom-documents'),
	(55,'2024-10-24','22.8.2004 [jeeff] id stranky s prihlasovacim dialogom-documents_history'),
	(56,'2024-10-24','11.9.2004 [jeeff] docid menu na pravej strane'),
	(57,'2024-10-24','11.9.2004 [jeeff] docid menu na pravej strane-documents'),
	(58,'2024-10-24','11.9.2004 [jeeff] docid menu na pravej strane-documents_history'),
	(59,'2024-10-24','14.9.2004 [joruz] anketa - aktivny / neaktivny stav'),
	(60,'2024-10-24','25.9.2004 [joruz] hodnotenie stranok'),
	(61,'2024-10-24','29.9.2004 [jeeff] generator primarnych klucov'),
	(62,'2024-10-24','29.9.2004 [jeeff] generator primarnych klucov - documents'),
	(63,'2024-10-24','4.10.2004 [joruz] tabulka s perex skupinami'),
	(64,'2024-10-24','15.10.2004 [jeeff] zoznam miestnosti pre chat'),
	(65,'2024-10-24','15.10.2004 [jeeff] default miestnost pre chat'),
	(66,'2024-10-24','15.10.2004 [jeeff] moderovana miestnost pre chat'),
	(67,'2024-10-24','16.10.2004 [jeeff] crontab'),
	(68,'2024-10-24','16.10.2004 [jeeff] crontab - CalendarDB.sendNotify'),
	(69,'2024-10-24','16.10.2004 [jeeff] crontab - RegAlarm.regAlarm'),
	(70,'2024-10-24','16.11.2004 [jeeff] email - cas a datum, kedy sa ma odoslat'),
	(71,'2024-10-24','17.11.2004 [jeeff] email - text emailu (ak sa neodosiela URL)'),
	(72,'2024-10-24','19.11.2004 [jeeff] email - replyTo'),
	(73,'2024-10-24','19.11.2004 [jeeff] email - ccEmail'),
	(74,'2024-10-24','19.11.2004 [jeeff] email - bccEmail'),
	(75,'2024-10-24','8.12.2004 [jeeff] zakladny CSS styl pre sablonu'),
	(76,'2024-10-24','10.12.2004 [joruz] komplet statistika - views'),
	(77,'2024-10-24','10.12.2004 [joruz] komplet statistika - from'),
	(78,'2024-10-24','10.12.2004 [joruz] komplet statistika - stat_browser_id - pkey'),
	(79,'2024-10-24','10.12.2004 [joruz] komplet statistika - stat_session_id - pkey'),
	(80,'2024-10-24','17.12.2004 [joruz] sprava fora'),
	(81,'2024-10-24','21.12.2004 [jeeff] vlastnosti adresara na disku (indexacia, prava)'),
	(82,'2024-10-24','7.1.2005 [joruz] schvalovanie diskusnych prispevkov (document_forum confirmed)'),
	(83,'2024-10-24','7.1.2005 [joruz] schvalovanie diskusnych prispevkov (forum message_confirmation)'),
	(84,'2024-10-24','7.1.2005 [joruz] schvalovanie diskusnych prispevkov (document_forum hash_code)'),
	(85,'2024-10-24','7.1.2005 [joruz] schvalovanie diskusnych prispevkov (forum approve_email)'),
	(86,'2024-10-24','25.1.2005 [jeeff] pkey generator - banner_id'),
	(87,'2024-10-24','28.1.2005 [joruz] bazar - bazar_groups'),
	(88,'2024-10-24','28.1.2005 [joruz] bazar - bazar_advertisements'),
	(89,'2024-10-24','6.2.2005 [jeeff] custom fields - documents'),
	(90,'2024-10-24','6.2.2005 [jeeff] custom fields - documents_history'),
	(91,'2024-10-24','7.2.2005 [jeeff] adminlog'),
	(92,'2024-10-24','15.2.2005 [joruz] forum - email nofifikacie'),
	(93,'2024-10-24','15.2.2005 [jeeff] adminlog - ip a hostname'),
	(94,'2024-10-24','31.3.2005 [jeeff] banner_banners - zrusenie identity stlpcov'),
	(95,'2024-10-24','15.4.2005 [joruz] forum - uprava na message board'),
	(96,'2024-10-24','15.4.2005 [jeeff] users - udaje o datume narodenia, pohlavi atd'),
	(97,'2024-10-24','20.4.2005 [jeeff] custom fields - documents (dalsich 6)'),
	(98,'2024-10-24','20.4.2005 [jeeff] custom fields - documents_history (dalsich 6)'),
	(99,'2024-10-24','12.5.2005 [joruz] users - premenovanie sex fieldu'),
	(100,'2024-10-24','19.5.2005 [joruz] user_groups - ak je 1, pouzivatel si moze danu skupinu priradit/odobrat'),
	(101,'2024-10-24','27.5.2005 [jeeff] emails - ak je 1 tak sa nebude email odosielat (zostane cakat)'),
	(102,'2024-10-24','22.6.2005 [joruz] doc_subscribe - tabulka pre subscribe notifikacie o zmene stranky (komponenta docSubscribeInfo)'),
	(103,'2024-10-24','30.6.2005 [joruz] forum - typ prispevku'),
	(104,'2024-10-24','30.6.2005 [joruz] user - rank pre forum'),
	(105,'2024-10-24','10.7.2005 [jeeff] dictionary - slovnik vysvetlujuci vyrazy / slovicka'),
	(106,'2024-10-24','11.7.2005 [jeeff] doc_subscribe - pridanie user_id'),
	(107,'2024-10-24','26.8.2005 [miros] groups_approve - premenovanie modu (vadilo to Oracle)'),
	(108,'2024-10-24','14.9.2005 [jeeff] document_forum - send_answer_notif (chyba v ORACLE verzii)'),
	(109,'2024-10-24','28.9.2005 [jeeff] templates - 4 volne pouzitelne objekty'),
	(110,'2024-10-24','17.10.2005 [miros] dmail - tabulka kampani'),
	(111,'2024-10-24','18.10.2005 [jeeff] groups - zvacsenie datovych poloziek'),
	(112,'2024-10-24','20.10.2005 [jeeff] media - tabulka s roznymi mediami'),
	(113,'2024-10-24','28.10.2005 [jeeff] media - sortovanie a thumb'),
	(114,'2024-10-24','9.11.2005 [jeeff] shoppnig basket'),
	(115,'2024-10-24','11.11.2005 [jeeff] users - datumy pocas ktorych je platne prihlasovanie'),
	(116,'2024-10-24','8.12.2005 [jeeff] stat - vytvorenie indexov'),
	(117,'2024-10-24','19.12.2005 [jeeff] doc_atr - moznostu zadavania necelociselnych hodnot (aj ked atribut sa stale vola value_int)'),
	(118,'2024-10-24','3.1.2005 [jeeff] groups - nove atributy - domena, '),
	(119,'2024-10-24','19.2.2005 [jeeff] stat_views - doplnene idecka adresarov stranok'),
	(120,'2024-10-24','25.1.2006 [jeeff] users - hash pre autorizaciu z emailu'),
	(121,'2024-10-24','7.2.2006 [jeeff] emails_campain - pocet odoslanych emailov, datum posedneho'),
	(122,'2024-10-24','23.3.2006 [jeeff] modules - poradie usporiadania poloziek'),
	(123,'2024-10-24','27.2.2006 [jeeff] admin_message - odkazy medzi administratormi'),
	(124,'2024-10-24','5.3.2006 [jeeff] admin_message - stav ci je precitana sprava'),
	(125,'2024-10-24','11.5.2005 [jeeff] stat - vytvorenie indexov MSSQL'),
	(126,'2024-10-24','1.6.2006 [nepso] stat_from - prida stlpec doc_id=na ktoru stranku prisiel'),
	(127,'2024-10-24','1.6.2006 [nepso] banner_banners - prida stlpec name = meno banneru'),
	(128,'2024-10-24','1.6.2006 [nepso] banner_banners - prida stlpec target'),
	(129,'2024-10-24','5.9.2006 [jeeff] sita - zoznam uz parsovanych sprav'),
	(130,'2024-10-24','22.9.2006 [nepso] gallery - prida stlpec resize_mode'),
	(131,'2024-10-24','3.1.2005 [jeeff] groups - nove atributy - menuType pre prihlaseneho, virtual link'),
	(132,'2024-10-24','9.11.2006 [nepso] basket_invoice - prida stlpec html_code'),
	(133,'2024-10-24','21.11.2006 [jeeff] url_redirects - presmerovania zmenenych stranok'),
	(134,'2024-10-24','24.11.2006 [jeeff] forum - id administratorskych skupin'),
	(135,'2024-10-24','2.2.2007 [jeeff] users - rozsirenie stlpca login na 128 znakov, aby mohol obsahovat aj email'),
	(136,'2024-10-24','5.2.2007 [jeeff] emails - statistika kliknuti na linku v emaily'),
	(137,'2024-10-24','11.2.2007 [jeeff] emails_stat_click - rozsirenie moznosti sledovania'),
	(138,'2024-10-24','12.2.2007 [jeeff] forms - datum posledneho exportu zaznamu'),
	(139,'2024-10-24','14.2.2007 [jeeff] emails - fix na user_id = -1'),
	(140,'2024-10-24','6.3.2007 [jeeff] _properties_ - zvacsenie moznosti zadania hodnoty na text'),
	(141,'2024-10-24','11.3.2007 [nepso] user_group_verify - prida stlpec hostname'),
	(142,'2024-10-24','20.3.2007 [jeeff] groups - oprava -1 hodnoty na logged_menu_type'),
	(143,'2024-10-24','2.4.2007 [jeeff] calendar - pridanie tabulky pre evidenciu akceptacii'),
	(144,'2024-10-24','13.7.2007 [nepso] emails_campain - pridanie stlpca user_groups'),
	(145,'2024-10-24','3.8.2007 [jeeff] emails - index podla kampane, znacne urychli nacitanie'),
	(146,'2024-10-24','1.8.2007 [nepso] users - pridanie stlpca fax'),
	(147,'2024-10-24','5.8.2007 [jeeff] doc_atr_def - zvacsenie pola s nazvom atributu'),
	(148,'2024-10-24','16.11.2007 [jeeff] emails_campain - doplnenie poli podla tabulky emails'),
	(149,'2024-10-24','23.11.2007 [jeeff] basket_item - doplnenie poli title a pn pre zobrazenie v objednavke aj po zmazani stranky'),
	(150,'2024-10-24','5.11.2007 [jeeff] banner_banners - sposob vkladania click tagu a frame rate'),
	(151,'2024-10-24','25.2.2008 [thaber] banner_stat_views_day - nova tabulka pre statistiku videni bannerov (zabera menej miesta)'),
	(152,'2024-10-24','4.3.2008 [jeeff] _conf_ - zmena na text pole'),
	(153,'2024-10-24','13.4.2008 [pbezak] emails_unsubscribed - zoznam odhlasenych emailov'),
	(154,'2024-10-24','12.6.2008 [murbanec] BASKET_INVOICE - pridanie pola currency'),
	(155,'2024-10-24','16.7.2008 [hric] banner_banners - prida stlpec client_id'),
	(156,'2024-10-24','22.8.2008 [murbanec] podpora jazykov pre galeriu'),
	(157,'2024-10-24','26.7.2008 [murbanec] image gallery - pridanie author stlpca'),
	(158,'2024-10-24','3.10.2008 [jeeff] documents - disable_after_end'),
	(159,'2024-10-24','23.10.2008 [jeeff] redirects - podpora domen'),
	(160,'2024-10-24','7.11.2008 [thaber] proxy - konfiguracia proxy pre externe systemy'),
	(161,'2024-10-24','13.11.2008 [jeeff] stat_searchengine - pridanie stlpca group_id pre moznost filtrovania'),
	(162,'2024-10-24','17.11.2008 [jeeff] cluster - tabulka so zoznamom refreshov objektov'),
	(163,'2024-10-24','22.11.2008 [jeeff] stat_from - pridanie stlpca group_id pre moznost filtrovania'),
	(164,'2024-10-24','26.11.2008 [jeeff] user_settings - nastavenia pouzivatela (ak treba pre niektory modul)'),
	(165,'2024-10-24','5.12.2008 [bhric] document_forum - deleted'),
	(166,'2024-10-24','23.01.2009 [kmarton] reservation_object - tabulka so zoznamom rezervacnych objektov pre modul rezervacie'),
	(167,'2024-10-24','23.01.2009 [kmarton] reservation - tabulka so zoznamom rezervacii pre modul rezervacie'),
	(168,'2024-10-24','29.01.2009 [kmarton] adminlog_notify - tabulka so zoznamom notifikacii pre modul adminlog'),
	(169,'2024-10-24','3.2.2008 [kmarton] tabulka inquiry - pridany stlpec multiple - ak sa da hlasovat za viac moznosti'),
	(170,'2024-10-24','3.2.2008 [kmarton] tabulka inquiry - pridany stlpec total_clicks - pocet hlasujucich v ankete'),
	(171,'2024-10-24','4.2.2008 [thaber] tabulka gallery pridany stlpec pre pocitanie odoslani pohladnice'),
	(172,'2024-10-24','6.2.2008 [jeeff] documents_history - pridanie datumu schvalenia web stranky'),
	(173,'2024-10-24','7.2.2008 [bhric] tabulka calendar - pridany stlpec creator_id'),
	(174,'2024-10-24','7.2.2008 [bhric] tabulka calendar - pridany stlpec approve kvoli schvalovaciemu procesu'),
	(175,'2024-10-24','7.2.2008 [bhric] tabulka calendar_types - pridany stlpec schvalovatel_id kvoli schvalovaciemu procesu'),
	(176,'2024-10-24','7.2.2008 [bhric] tabulka calendar - pridany stlpec suggest kvoli odporucaniu udalosti'),
	(177,'2024-10-24','5.3.2008 [jeeff] document_forum - pridany index podla doc_id'),
	(178,'2024-10-24','20.3.2009 [jeeff] tabulka media - pridany datum poslednej zmeny'),
	(179,'2024-10-24','14.03.2009 [kmarton] seo_bots - tabulka so zoznamom vyhladavacich botov, ktori pristupili na stranky WebJET-u'),
	(180,'2024-10-24','07.04.2009 [kmarton] seo_keywords - tabulka so zoznamom SEO klucovych slov'),
	(181,'2024-10-24','07.04.2009 [kmarton] seo_google_position - tabulka so zaznamenanymi poziciami vo vyhladavani na google.com pre klucove slova'),
	(182,'2024-10-24','21.4.2009 [kmarton] tabulka seo_keywords - pridany stlpec domain kvoli rozlisovaniu jednotlivych klucovych slov pre rozne domeny'),
	(183,'2024-10-24','23.4.2009 [kmarton] tabulka crontab - pridany zaznam, ktory bude na pozadi vykonavat kotrolu pozicii vo vyhladavani vsetkych klucovych slov pre dane domeny na google.com'),
	(184,'2024-10-24','6.4.2008 [jeeff] tabulka gallery pridany stlpec pre povolenie domeny pohladnice'),
	(185,'2024-10-24','17.4.2009 [jeeff] stat - vytvorenie indexov ORACLE 3'),
	(186,'2024-10-24','6.5.2009 [jeeff] vytvorenie indexov MS SQL'),
	(187,'2024-10-24','27.04.2009 [murbanec] permission groups - skupiny prav vo WebJETe'),
	(188,'2024-10-24','25.5.2009 [kmarton] tabulka seo_keywords - pridany stlpec searchBot kvoli moznosti vyberu vyhladavaca, v ktorom sa bude klucove slovo vyhladavat'),
	(189,'2024-10-24','31.7.2009 [jeeff] users - unique index nad loginom'),
	(190,'2024-10-24','3.8.2008 [jeeff] user_disabled_items - zvacsenie pola s klucom modulu'),
	(191,'2024-10-24','24.7.2009 [murbanec] _properties_ - zvacsenie moznosti zadania hodnoty na text / dorabka aj pre ostatne databazovy ako MySQL'),
	(192,'2024-10-24','27.7.2009 [murbanec] tabulka form_file_restrictions - obmedzenie uploadnutych suborov na velkost, priponu a pod.'),
	(193,'2024-10-24','10.06.2009 [kmarton] monitoring - tabulka, do ktorej sa ukladaju informacie z monitorovania servera'),
	(194,'2024-10-24','11.6.2009 [kmarton] tabulka crontab - pridany zaznam, ktory bude na pozadi vykonavat zapis aktualnych hodnot servera do tabulky monitoring kazdych 30 sekund'),
	(195,'2024-10-24','13.8.2009 [murbanec] crontab - pridanie stlpca cluster_node pre spustanie na definovanom node clustra'),
	(196,'2024-10-24','18.8.2009 [jeeff] forms - zvacsenie pola pre HTML'),
	(197,'2024-10-24','31.8.2009 [kmarton] tabulka gallery - pridany stlpec perex_group kvoli moznosti pridavaniu klucoveho slova k obrazku'),
	(198,'2024-10-24','2.9.2009 [murbanec] publikovanie adresarov'),
	(199,'2024-10-24','15.10.2009 [jraska] tabulka documents - pridany stlpec forum_count pre pocitanie poctu prispevkov v diskusii'),
	(200,'2024-10-24','21.10.2009 [jeeff] tabulka documents_history - pridany stlpec forum_count pre kompatibility s tabulkou documents (stlpec ale bude prazdny)'),
	(201,'2024-10-24','27.10.2009 [jeeff] oracle - zmena char stlpcov na varchar pre properties a calendar'),
	(202,'2024-10-24','2.11.2009 [jeeff] oracle - doplnenie schemy pre Enterprise verziu'),
	(203,'2024-10-24','5.11.2009 [kmarton] tabulka gallery_dimension - pridany stlpec pre nazov, perex a datum vytvorenia fotogalerie'),
	(204,'2024-10-24','10.11.2009 [thaber] tabulka gallery - bod zaujmu'),
	(205,'2024-10-24','13.11.2009 [kmarton] tabulka inquiry - pridany stlpec image_path - moznost pridania fotky k anketovej odpovedi'),
	(206,'2024-10-24','19.11.2009 [kmarton] tabulka gallery_dimension - pridany stlpec pre autora fotogalerie'),
	(207,'2024-10-24','19.11.2009 [bhric] tabulka gallery_dimension - pridany stlpec views pre pocet videni'),
	(208,'2024-10-24','8.12.2009 [jeeff] doplnene indexy'),
	(209,'2024-10-24','25.1.2010 [kmarton] tabulka seo_keywords - nastavenie search_bot atributu na default \'google.sk\', tam kde je to NULL kvoli NPE'),
	(210,'2024-10-24','1.2.2010 [jeeff] documents_history - index nad doc_id'),
	(211,'2024-10-24','5.2.2010 [jraska] tabulka users - pridane stlpce `position` a `parent_id`'),
	(212,'2024-10-24','1.3.2010 [jeeff] documents_history - index nad awaiting_approve'),
	(213,'2024-10-24','8.4.2010 [jeeff] emails - Oracle nepozna prazdny retazec v DB a ma problem s URL polom ktore realne moze byt prazdne (ked sa posiela priamo text)'),
	(214,'2024-10-24','8.4.2010 [jeeff] documents - rozsirenie custom fields, pridanie SSL atributu'),
	(215,'2024-10-24','12.4.2010 [jeeff] sablony - moznost predefinovania install name per sablona'),
	(216,'2024-10-24','12.4.2010 [jeeff] sablony - moznost zrusenia spam ochrany per sablona (napr. pre mobil)'),
	(217,'2024-10-24','23.4.2010 [bhric] basket - tabulka pre evidenciu ciastkovych platieb'),
	(218,'2024-10-24','13.5.2010 [jeeff] stat_keys - cache tabulka pre prevod ID na VALUE pre zapis informacie o prehliadaci'),
	(219,'2024-10-24','17.5.2010 [jeeff] media - index nad doc_id'),
	(220,'2024-10-24','7.6.2010 [kmarton] BASKET_ITEM - zmena cudzieho kluca z -1 na NULL'),
	(221,'2024-10-24','5.3.2010 [thaber] sms_templates - pridane tabulka pre sabolny pre sms '),
	(222,'2024-10-24','25.6.2010 [bhric] tabulka perex_groups - pridany stlpec available_groups pre moznost definovania adresarov perex skupinam'),
	(223,'2024-10-24','28.6.2010 [jeeff] adminlog - index nad stlpcami pre vyhladavanie'),
	(224,'2024-10-24','25.6.2010 [murbanec] crontab - HeatMapCleaner'),
	(225,'2024-10-24','2.7.2010 [kmarton] user_settings_admin - pridana tabulka pre ukladanie nastaveni pouzivatela v admin casti '),
	(226,'2024-10-24','7.7.2010 [murbanec] crontab - StatWriteBuffer'),
	(227,'2024-10-24','9.7.2010 [bhric] basket_invoice_payments - tabulka pre evidenciu ciastkovych platieb - pridany stlpec closed_date, confirmed a upraveny typ stlpca payed_price na decimal'),
	(228,'2024-10-24','27.7.2010 [murbanec] Tabulka cluster_monitoring - pouziva sa pri prenose monitorovacich informacii medzi clustermi'),
	(229,'2024-10-24','30.7.2010 [murbanec] Tabulka pkey_generator - zmena stlpca z INT na LONG'),
	(230,'2024-10-24','3.8.2010 [murbanec] cluster_monitoring - pridany stlpec created_at'),
	(231,'2024-10-24','4.8.2010 [murbanec] Watermarky pre galeriu'),
	(232,'2024-10-24','6.8.2010 [kmarton] gallery - pridany stlpec upload_datetime - datum a cas nahratia fotografie na server'),
	(233,'2024-10-24','10.8.2010 [murbanec] Spotlight - FileIndexer'),
	(234,'2024-10-24','27.8.2010 [kmarton] Zmena kratkeho pola pre ukladanie ciest k priloham v tabulke emails a emails a emails_campaign'),
	(235,'2024-10-24','31.8.2010 [jraska] Tabulka multigroup_mapping - pouziva sa pre mapovanie Master - Slave clankov pre ucely multikategorii/multiadresarov'),
	(236,'2024-10-24','20.9.2010 [jeeff] documents - indexy pre rychlejsie nacitanie DocDB struktur'),
	(237,'2024-10-24','4.1.2011 [murbanec] users - pridanie moznosti jednosmerneho hesla'),
	(238,'2024-10-24','2.2.2011 [mhalas] domain_redirects - tabulka presmerovania domen'),
	(239,'2024-10-24','21.2.2011 [mhalas] pridanie flagu do crontable na auditovacie ucely'),
	(240,'2024-10-24','28.2.2011 [mhalas] pridanie sort_priority do gallery'),
	(241,'2024-10-24','23.03.2011 [mrepasky] monitoring - pridanie cpu_usage a process_usage do monitoringu servera '),
	(242,'2024-10-24','27.03.2011 [murbanec] Vymazanie triedy RegAlarm - uz sa nepouziva'),
	(243,'2024-10-24','5.4.2011 [jeeff] Oracle - zvacsenie datovy poli pre nazvy adresara'),
	(244,'2024-10-24','25.05.2011 [bhric] inquiry - tabulka pre zapis statistiky o hlasovani daneho usera'),
	(245,'2024-10-24','27.05.2010 [thaber] pridanie tabulky stopslov'),
	(246,'2024-10-24','19.05.2011 [murbaenc] Atributy pre formulare + merge restrikcii suborov do spolocnej tabulky'),
	(247,'2024-10-24','23.06.2011 [mrepasky] Tabulka regularnych vyrazov pre formulare'),
	(248,'2024-10-24','24.6.2011 [mhalas] Pridanie priority do questions_answers'),
	(249,'2024-10-24','11.7.2011 [bhric] zmluvy, zmluvy_prilohy - tabulky pre modul Zmluvy'),
	(250,'2024-10-24','22.7.2011 [bhric] tabulky zmluvy - rozdelenia stlpca platnost na platnost_od a platnost_do'),
	(251,'2024-10-24','25.7.2011 [mrepasky] Pretypovanie stlpca prop_value v tabulke properties'),
	(252,'2024-10-24','26.8.2011 [mhalas] premenovanie stlpca audit kvoli oraclu'),
	(253,'2024-10-24','19.9.2011 [mrepasky] Oracle - pretypovanie stlpca pre publikovanie adresara'),
	(254,'2024-10-24','22.9.2011 [mrepasky] Pretypovanie stlpca pre defaultnu hodnotu atributu'),
	(255,'2024-10-24','4.10.2011 [mhalas] Sledovanie otvorenia emailu pre dmail'),
	(256,'2024-10-24','10.11.2011 [jeeff] forum - id skupin z ktorych je mozne pridat novy prispevok'),
	(257,'2024-10-24','11.11.2011 [vbur] contact - tabulka s kontaktmi'),
	(258,'2024-10-24','21.11.2011 [vbur] inquiry - pridanie url k odpovedi'),
	(259,'2024-10-24','16.11.2011 [jeeff] forms - indexy pre rychlejsie nacitanie'),
	(260,'2024-10-24','13.01.2012 [mrepasky] Pridanie regularnych vyrazov do databazy - tabulka form_regular_exp'),
	(261,'2024-10-24','20.1.2012 [jeeff] cache - tabulka pre persistent cache'),
	(262,'2024-10-24','7.3.2012 [jeeff] proxy - pridanie autorizacie'),
	(263,'2024-10-24','18.4.2012 [vbur] inventory - evidencia majetku'),
	(264,'2024-10-24','10.5.2012 [vbur] inventory - prislusenstvo'),
	(265,'2024-10-24','15.5.2012 [bhric] zmluvy - odstranenie a pridanie dalsich stlpcov'),
	(266,'2024-10-24','18.5.2012 [bhric] tabulka adminlog_notify - pridany stlpec text'),
	(267,'2024-10-24','25.5.2012 [bhric] tabulka zmluvy - pridany stlpec zobrazit, vytvoril'),
	(268,'2024-10-24','26.6.2010 [bhric] documents - rozsirenie tabulky o root_group_l1, root_group_l2 a root_group_l3'),
	(269,'2024-10-24','17.7.2012 [bhric] Oracle - pretypovanie stlpca html_code v banner_banners'),
	(270,'2024-10-24','3.8.2012 [bhric] perex_group_doc - pouziva sa pre mapovanie doc_id a perex_group_id z tabulky documents'),
	(271,'2024-10-24','6.8.2012 [bhric] documents - indexy na root_group stlpcami'),
	(272,'2024-10-24','22.8.2012 [jeeff] perex_group_doc - indexy'),
	(273,'2024-10-24','12.9.2012 [mrepasky] Pridanie regularnych vyrazov do databazy - integer'),
	(274,'2024-10-24','10.10.2012 [jeeff] prop - moznost zadat NULL hodnotu pre Oracle (akoze prazdny string)'),
	(275,'2024-10-24','3.1.2013 [jeeff] questions_answers - rozsirenie tabulky o nove stlpce'),
	(276,'2024-10-24','9.1.2013 [bhric] reservation_object - rozsirenie tabulky o nove stlpce'),
	(277,'2024-10-24','11.3.2012 [mhalas] WJ Cloud - pridanie domain_id do galerie'),
	(278,'2024-10-24','11.3.2012 [mhalas] WJ Cloud - pridanie domain_id do gallery_dimension'),
	(279,'2024-10-24','14.3.2013 [prau] WebJet Cloud inquiry_answers - rozsirenie tabulky o novy stlpec domain_id '),
	(280,'2024-10-24','14.3.2013 [prau] WebJet Cloud inquiry_users - rozsirenie tabulky o novy stlpec domain_id '),
	(281,'2024-10-24','14.3.2013 [prau] WebJet Cloud inquiry - rozsirenie tabulky o novy stlpec domain_id '),
	(282,'2024-10-24','15.3.2012 [mbocko] WJ Cloud (modul Forum) - pridanie domain_id do forum'),
	(283,'2024-10-24','15.3.2012 [mbocko] WJ Cloud (modul Forum) - pridanie domain_id do document_forum'),
	(284,'2024-10-24','18.3.2012 [mbocko] WJ Cloud (modul QA) - pridanie domain_id do questions_answers'),
	(285,'2024-10-24','21.3.2012 [prau] WJ Cloud (modul formulare) - pridanie domain_id do forms '),
	(286,'2024-10-24','21.3.2012 [prau] WJ Cloud (modul formulare) - pridanie domain_id do form_attributes'),
	(287,'2024-10-24','15.4.2013 [mrepasky] Pridany datum zmeny pre konfiguracne premenne - pridanie stlpca date_changed do tabulky _conf_ resp. webjet_conf'),
	(288,'2024-10-24','03.06.2013 [mrepasky] Tabulka pre historiu suborov vo webjete - file_history a nastavenie generatora klucov'),
	(289,'2024-10-24','4.6.2012 [mbocko] WJ Cloud (users) - pridanie domain_id do users'),
	(290,'2024-10-24','22.5.2017 [mminda] inventory - evidencia majetku - pridanie dovodu vyradenia '),
	(291,'2024-10-24','11.6.2012 [mbocko] inventory - evidencia majetku - rozsirenie detailu, pridanie dovodu vyradenia pre detail'),
	(292,'2024-10-24','19.6.2012 [mbocko] WJ Cloud (Basket) - pridanie domain_id do basket_item'),
	(293,'2024-10-24','19.6.2012 [mbocko] WJ Cloud (Basket) - pridanie domain_id do basket_invoice'),
	(294,'2024-10-24','25.7.2013 [mbocko] tabulka so zoznamom slov pre slovnikovu captchu'),
	(295,'2024-10-24','25.7.2013 [mhalas] tabulka pre limity na domain throttling'),
	(296,'2024-10-24','29.11.2013 [mhalas] Genericky template pre domenove limity'),
	(297,'2024-10-24','13.12.2013 [mrepasky] Uprava tabulky pre historiu suborov vo webjete - file_history'),
	(298,'2024-10-24','17.12.2013 [jeeff] Oprava mena stlpcu pkey generatora'),
	(299,'2024-10-24','10.1.2013 [bhric] tabulka zmluvy - zmena typu pola pre predmet, poznamka na text / clob'),
	(300,'2024-10-24','12.2.2013 [mcacko] #15400 - skutocny archiv, vytvorenie tabulky forms_archiv'),
	(301,'2024-10-24','01.07.2014 [mkolejak] Tabulka pre todo'),
	(302,'2024-10-24','12.9.2014 [mkolejak] doplnam chybajuci primarny kluc pre mssql'),
	(303,'2024-10-24','27.10.2014 [mkolejak] reservation_object - rozsirenie tabulky o nove stlpce'),
	(304,'2024-10-24','27.10.2014 [mkolejak] reservation - pridanie stlpca phone_number'),
	(305,'2024-10-24','4.11.2014 [mkolejak] WJ Cloud - pridanie domain_id do reservation'),
	(306,'2024-10-24','4.11.2014 [mkolejak] WJ Cloud - pridanie domain_id do reservation_object'),
	(307,'2024-10-24','05.11.2014 [mkolejak] Tabulka pre export_dat'),
	(308,'2024-10-24','27.10.2014 [mkolejak] _conf_prepared_ - pripravene hodnoty pre config, pridaju sa v prepared case'),
	(309,'2024-10-24','12.12.2014 [mkolejak] Automaticky prenesenie predpripravenych konfiguracii z\n     _config_prepared_ do _conf_ ked uplynul date_prepared pomocou cronu'),
	(310,'2024-10-24','19.06.2015 [pbielik] tabulka terminologicky_slovnik'),
	(311,'2024-10-24','16.07.2015 [rzapach] pridanie stlpcov pre kalendar'),
	(312,'2024-10-24','22.07.2015 [rzapach] Tabulka pre modul GIS'),
	(313,'2024-10-24','28.07.2015 [rzapach] Tabulky pre modul denneho menu v restauracii'),
	(314,'2024-10-24','12.08.2015 [rzapach] Tabulka pre hodnotenie stranky'),
	(315,'2024-10-24','14.9.2015 [rzapach] zmluvy - pridanie stlpca sposob_uhrady'),
	(316,'2024-10-24','17.12.2015 [rzapach] tabulka cien pre dany rezervacny objekt'),
	(317,'2024-10-24','28.4.2015 [mhalas] WebJet Cloud Zmluvy - rozsirenie tabulky o novy stlpec domain_id '),
	(318,'2024-10-24','23.10.2015 [jeeff] Form regexp - uprava regexpu kontroly emailu pre velke pismena'),
	(319,'2024-10-24','18.12.2015 [pbielik] pridanie stlpca synonymum do terminologicky_slovnik'),
	(320,'2024-10-24','20.1.2016 [rzapach] tabulka povolenych casov pre dany rezervacny objekt, casova jednotka pre rez. objekt'),
	(321,'2024-10-24','30.05.2016 [jeeff] pridanie stlpcov pre calendar_invitation'),
	(322,'2024-10-24','14.10.2015 [jeeff] ckeditor - pocitadlo uploadnutych obrazkov'),
	(323,'2024-10-24','10.12.2015 [jeeff] zmazanie cron tasku magzilly'),
	(324,'2024-10-24','12.2.2016 [rzapach] groups_scheduler -> groups history, pridanie stlpca s casom a zmena when_to_publish aby povoloval null hodnoty'),
	(325,'2024-10-24','21.6.2016 [jeeff] pkey_generator - nastavenie stlpcov'),
	(326,'2024-10-24','11.7.2016 [rzapach] pridanie stlpca mobile_device (idcko zariadenia) do tabulky users -> kvoli posielaniu push notifikacii'),
	(327,'2024-10-24','30.08.2016 [prau] pridanie stlpcu pre group scheduler kvoli historii'),
	(328,'2024-10-24','23.09.2016 [rzapach] pridanie stlpcu pre domain_redirects kvoli pridaniu kontroly protokolu http/https'),
	(329,'2024-10-24','15.09.2016 [prau] #20546 - WebJET - Vkladanie scriptov'),
	(330,'2024-10-24','18.11.2016 [lpasek] Vytvorenie tabulky pre komponentu appcache'),
	(331,'2024-10-24','06.02.2017 [bhric] pridanie stlpcu pre jazyk (lng) do groups'),
	(332,'2024-10-24','15.02.2017 [mbocko] pridanie stlpca require_email_verification pre user_groups'),
	(333,'2024-10-24','26.04.2017 [ryapach] vytvorenie tabulky pre poznmaky pre editorov k strankam'),
	(334,'2024-10-24','10.5.2017 [mminda] oracle - doplnenie schemy pre Tooltipy (tatrabanka)'),
	(335,'2024-10-24','15.05.2017 [bhric] pridanie stlpcu create_date (datum odhlasenia) do emails_unsubscribed'),
	(336,'2024-10-24','1.5.2017 [prau] Archiv suborov'),
	(337,'2024-10-24','29.05.2017 [jeeff] premenovanie stlpcu pre domain_redirects kvoli oracle vyhradenemu menu'),
	(338,'2024-10-24','30.5.2017 [lpasek] Rozsirenie tabulky appcache_file o stlpec recursive'),
	(339,'2024-10-24','30.5.2017 [lpasek] Vytvorenie tabulky appcache_page'),
	(340,'2024-10-24','31.5.2017 [lpasek] Uprava tabulky appcache_page'),
	(341,'2024-10-24','03.07.2017 [prau] #22201 rozsirenie Baneroveho systemu o domain_id '),
	(342,'2024-10-24','17.07.2017 [prau] #22201 rozsirenie Baneroveho systemu o domain_id - fix mysql'),
	(343,'2024-10-24','2.8.2017 [prau] Archiv suborov pridanie stlpcu pre velkost suboru'),
	(344,'2024-10-24','2.8.2017 [prau] Archiv suborov oracle skript'),
	(345,'2024-10-24','08.09.2017 [pbielik] vytvorenie tabuliek pre kviz'),
	(346,'2024-10-24','27.09.2017 [prau] #20546 - WebJET - Vkladanie scriptov'),
	(347,'2024-10-24','28.09.2017 [prau] pridanie stlpca right_answer pre quiz_answers'),
	(348,'2024-10-24','07.11.2017 [prau] #20546 - WebJET - Vkladanie scriptov - oprava'),
	(349,'2024-10-24','07.11.2017 [prau] #23245 - Rozsirenie funkcionality skupiny práv'),
	(350,'2024-10-24','10.11.2017 [bhric] pridanie flagu do crontable na spustanie ulohy po starte servera a moznost zakazania vykonavania ulohy'),
	(351,'2024-10-24','10.11.2017 [pbielik] #22743 - Pridanie domainId pre quiz'),
	(352,'2024-10-24','14.11.2017 [bhric] crontab - fix default hodnoty pre run_at_startup, enable_task v MS SQL'),
	(353,'2024-10-24','14.11.2017 [bhric] pridanie stlpcu pre jazyk (lng) do groups_scheduler'),
	(354,'2024-10-24','15.11.2017 [prau] #22201 - Upravy modulov pre multiweb / pridanie stlpca domain_id do file archivu'),
	(355,'2024-10-24','15.11.2017 [prau] #23167 - Archiv suborov - kontrola prav podla prav na subory pouzivatela - oprava'),
	(356,'2024-10-24','21.09.2017 [lpasek] Uprava tabulky pre komponentu appcache'),
	(357,'2024-10-24','21.09.2017 [lpasek] Uprava tabulky pre komponentu appcache file'),
	(358,'2024-10-24','10.01.2018 [prau] #23471 - Password security'),
	(359,'2024-10-24','29.1.2018 [jeeff] sablony - zvacsenie pola pre zadavanie CSS liniek'),
	(360,'2024-10-24','02.02.2018 [jeeff] groups - pridanie moznosti oznacenia adresara pre nezobrazenie v admin casti'),
	(361,'2024-10-24','07.03.2018 [bhric] Zmluvy, rozsirenie o organizacie. Moznost pridania organizacii a prav nad zmluvami (#23935)'),
	(362,'2024-10-24','16.04.2018 [mhruby] #25103 M - SPRÁVA ČÍSELNÍKOV'),
	(363,'2024-10-24','11.4.2018 [mhruby] #25103 M - SPRÁVA ČÍSELNÍKOV - pridanie stlpcovych nazvov'),
	(364,'2024-10-24','11.04.2018 [jeeff] restaurant_menu - oprava datoveho stlpca pre MS SQL'),
	(365,'2024-10-24','18.04.2018 [prau] #25043 M - INTERNÉ RIADIACE AKTY'),
	(366,'2024-10-24','7.5.2018 [jeeff] presmerovania domen - zvacsenie pola pre zadavanie URL cielovej domeny'),
	(367,'2024-10-24','18.5.2018 [mhruby] #25103 M - SPRÁVA ČÍSELNÍKOV - fix diakritiky pre Microsoft SQL'),
	(368,'2024-10-24','8.5.2018 [prau] Modul GDPR zadanie regexpov #23673'),
	(369,'2024-10-24','15.5.2018 [prau] #23673 Modul GDPR zadanie regexpov - rozsirenie tabulky o stlpec domain_id '),
	(370,'2024-10-24','30.5.2018 [prau] #23673  regularne vyrazy gdpr - zvacsenie pola pre zadavanie regularneho vyrazu'),
	(371,'2024-10-24','26.5.2018 [prau] #23673  regularne vyrazy gdpr -inicializacia regularnych vyrazov'),
	(372,'2024-10-24','05.06.2018 [pbielik] pridane customizacie kvizu'),
	(373,'2024-10-24','04.04.2018 [lzlatohlavek] #23073 - UPRAVA MEDIA KOMPONENTY'),
	(374,'2024-10-24','12.06.2018 [mhruby] Tabulky pre modul denneho menu v restauracii (zmena hmotnosti na string)'),
	(375,'2024-10-24','12.06.2018 [mhruby] Tabulka pre skupiny sablon.'),
	(376,'2024-10-24','18.06.2018 [pbielik] obodovany kviz'),
	(377,'2024-10-24','2.7.2018 [pbielik] inquirysimple'),
	(378,'2024-10-24','11.7.2018 [mhruby] #25103 M - SPRÁVA ČÍSELNÍKOV - fix diakritiky pre Microsoft SQL a Oracle'),
	(379,'2024-10-24','11.7.2018 [mhruby] Tabulka pre skupiny sablon. - fix diakritiky pre Microsoft SQL'),
	(380,'2024-10-24','13.07.2018 [pbielik] user_settings - nastavenia pouzivatela (ak treba pre niektory modul)'),
	(381,'2024-10-24','27.7.2018 [pbielik] added email_files table'),
	(382,'2024-10-24','17.05.2018 [prau] Vytvorenie tabuky pre cookies'),
	(383,'2024-10-24','17.05.2018 [prau] Pridanie riadkov pre cookies'),
	(384,'2024-10-24','13.8.2018 [prau] [#23881 - Cookies] - presun do WJ 8'),
	(385,'2024-10-24','17.8.2018 [mhruby] rozsirienie intranetoveho widgetu ulohy'),
	(386,'2024-10-24','5.9.2018 [jeeff] Pridanie key prefix stlpca na template groups'),
	(387,'2024-10-24','13.9.2018 [jeeff] Zmazanie starej konf. premennej kvoli korektnemu updatu na WJ8'),
	(388,'2024-10-24','20.9.2018 [pbielik] uprava quiz_questions question na varchar(500)'),
	(389,'2024-10-24','20.9.2018 [jeeff] oprava identity stlpca na media_groups tabulke'),
	(390,'2024-10-24','23.10.2018 [prau] #27127 Archiv suborov, defaultne zapnutie cronu pre nahratie suboru neskorej'),
	(391,'2024-10-24','15.11.2018 [mhruby] rozsirienie intranetoveho widgetu ulohy (pre oracle)'),
	(392,'2024-10-24','30.11.2018 [pbielik] add cookie_visitor_group to banner_banners'),
	(393,'2024-10-24','19.12.2018 [jvyskoc] pridanie pageReactions (lajkov)'),
	(394,'2024-10-24','19.12.2018 [bhric] Sprava ciselnikov - rozsirenie string data hodnot na 1024 znakov'),
	(395,'2024-10-24','14.12.2018 [lpasek] Premenovanie stlpca priority v tabule todo na sort_priority (priority je klucove slovo)'),
	(396,'2024-10-24','6.2.2019 [jeeff] index nad media grupami'),
	(397,'2024-10-24','08.02.2019 [prau] #28315 - Archiv suborov - problem s vypisom  [MFSR]'),
	(398,'2024-10-24','14.02.2019 [mhruby] Rozsirenie ciselnikov'),
	(399,'2024-10-24','18.02.2019 [pbielik] pridany image url pre quiz bean'),
	(400,'2024-10-24','20.02.2019 [mhruby] Pridanie regularnych vyrazov do databazy - tabulka form_regular_exp'),
	(401,'2024-10-24','05.03.2019 [pbielik] pkgenerator zmenena cookies_id za cookie_id'),
	(402,'2024-10-24','06.03.2019 [mhruby] Nastavenie defaultnej priority existujucim datam'),
	(403,'2024-10-24','10.3.2019 [jeeff] Pridanie rezimu inline editacie do skupiny sablon'),
	(404,'2024-10-24','18.3.2019 [mpijak] Pridanie pola DTYPE aby sa dala sfunkcnit dedicnost pre enumerations tabulky'),
	(405,'2024-10-24','20.3.2019 [mhruby] Bugfix definicie tabulky pri praci s boolean hodnotami'),
	(406,'2024-10-24','27.3.2019 [mhruby] Rozsirenie ciselnikov od dalsie stringy pre autokupu/autoweb'),
	(407,'2024-10-24','28.3.2019 [mhruby] Rozsirenie ciselnikov o dalsie datumy pre autokupu/autoweb'),
	(408,'2024-10-24','7.4.2019 [jeeff] Zmena sequencii na identity na Beanoch, Oracle stale potrebuje sequencie'),
	(409,'2024-10-24','9.4.2019 [mhruby] Rozsirenie ciselnikov o rodicov pre organizacnu strukturu'),
	(410,'2024-10-24','13.5.2019 [mhruby] Rozsirenie ciselnikov od dalsie stringy pre autokupu/autoweb'),
	(411,'2024-10-24','27.05.2019 [pbielik] Premenovanie stlpca priority a level v tabulke file_archiv_category_node'),
	(412,'2024-10-24','27.05.2019 [pbielik] Premenovanie stlpca level v tabulke file_archiv_category_node a inde'),
	(413,'2024-10-24','14.06.2019 [lpasek] Vytvorenie stlpcov double_optin_confirmation_date a double_optin_hash nad tabulkami forms a forms_archive'),
	(414,'2024-10-24','20.6.2019 [jeeff] fixnutie pkey generatora domain_redirects'),
	(415,'2024-10-24','17.9.2019 [mpijak] Rozsirenie ciselnikov od dalsie stringy pre autokupu/autoweb a'),
	(416,'2024-10-24','22.09.2019 [jeeff] Formulare - rozsirenie string value hodnot na 1024 znakov'),
	(417,'2024-10-24','27.09.2019 [jeeff] pkey generator pre crypto modul (poradie kluca)'),
	(418,'2024-10-24','23.10.2019 [mhruby] Rozsirenie tabulky url_redirect o datum publikacie'),
	(419,'2024-10-24','7.11.2019 [mhruby] crontab - UrlRedirectDB'),
	(420,'2024-10-24','22.11.2019 [bhric] basket_invoice - zvacsenie stlpca currency na 8 znakov'),
	(421,'2024-10-24','5.12.2019 [mhruby] [#39370 - Vynutene nastavenie sablony v adresari] - uloha #0 => doplnenie forceTheUseOfGroupTemplate do adresara webstranok 2'),
	(422,'2024-10-24','10.12.2019 [mhruby] [#39370 - Vynutene nastavenie sablony v adresari] - uloha #0 => doplnenie forceTheUseOfGroupTemplate do groups_scheduler'),
	(423,'2024-10-24','13.12.2019 [mhruby] revert: crontab - UrlRedirectDB'),
	(424,'2024-10-24','13.12.2019 [bhric] [#39430 - Notifikacia pri dosiahnuti maximalneho poctu DB spojeni] - default notifikacia pre web.spam'),
	(425,'2024-10-24','8.1.2020 [mhruby] [#40177 - 404, WebJET neobsahuje žiadne súbory] - uloha #18 => bugfix pre oracle obmedzenie dlkzy stlpca'),
	(426,'2024-10-24','31.01.2020 [mhruby] Rozsirenie todo tabulky a group_id'),
	(427,'2024-10-24','28.02.2020 [jeeff] Oprava stlpca timestamp na date pre mssql'),
	(428,'2024-10-24','11.03.2020 [jeeff] Nastavenie primarneho kluca pre vlastnosti formularu v multidomain prostredi'),
	(429,'2024-10-24','04.06.2020 [jeeff] Premenovanie premennej groupCreateBlankWebpageAfterCreate na syncGroupAndWebpageTitle kedze jej povodne pouzitie riesilo 2 veci'),
	(430,'2024-10-24','05.06.2020 [pgajdos] Zmena chybnych id skupin sablon (0 -> 1)'),
	(431,'2024-10-24','17.06.2020 [bhric] forms_archive - doplneny chybajuci stlpec DOMAIN_ID pre Oracle'),
	(432,'2024-10-24','2.7.2020 [mhruby] Pridanie stĺpca do tabulky inventory, prekopírovanie hodnôt z pôvodneho stlpca do nového a nasledne vycistinie stlpca ALTER'),
	(433,'2024-10-24','2.7.2020 [mhruby] Pridanie stĺpca do tabulky inventory, prekopírovanie hodnôt z pôvodneho stlpca do nového a nasledne vycistinie stlpca UPDATE'),
	(434,'2024-10-24','2.7.2020 [mhruby] Pridanie stĺpca do tabulky inventory, prekopírovanie hodnôt z pôvodneho stlpca do nového a nasledne vycistinie stlpca UPDATE 2'),
	(435,'2024-10-24','09.07.2020 [pgajdos] Pridanie stlpca task_name do tabulky crontab, umiestnenie stlpca na druhe miesto'),
	(436,'2024-10-24','09.07.2020 [pgajdos] Zapisanie popisov cronjobov do stlpca task_name, pre tabulku crontab'),
	(437,'2024-10-24','07.10.2020 [pbielik] [#48232 - Spracovanie modulu FIQ] - uprava varchar stlpcov na nvarchar'),
	(438,'2024-10-24','14.10.2020 [jeeff] Oprava nazvov stlpcov pkey generatora-domain_redirects'),
	(439,'2024-10-24','13.7.2020 [lpasek] adminlog - operation_type'),
	(440,'2024-10-24','30.11.2020 [lpasek] documents_history - disapproved_by'),
	(441,'2024-10-24','20.01.2021 [lpasek] Pridanie 3 stlpcov do tabulky cookies #51550'),
	(442,'2024-10-24','23.3.2021 [jeeff] pkey generator pre mirrorovanie struktury'),
	(443,'2024-10-24','23.03.2021 [lmolcan] banner_banners tabulka - pridanie atributov pre obsahovy banner'),
	(444,'2024-10-24','13.04.2021 [lmolcan] banner_banners tabulka - pridanie atributov pre obsahovy banner - mysql'),
	(445,'2024-10-24','13.3.2021 [sivan] Pridanie stlpca id a user_id do _conf_prepared_ tabulky'),
	(446,'2024-10-24','01.04.2021 [lbalat] Pridanie stlpca date_published do _conf_prepared_ tabulky'),
	(447,'2024-10-24','16.04.2021 [lbalat] Pridanie stlpca date_published do groups_scheduler tabulky'),
	(448,'2024-10-24','28.05.2021 [lbalat] Pridanie hashu pre kliknutie k emailom'),
	(449,'2024-10-24','21.12.2021 [lmolcan] Pridanie stlpca pre priezvisko'),
	(450,'2024-10-24','3.10.2022 [bhric] banery - rozsirenie o kampanovy banner'),
	(451,'2024-10-24','20.04.2020 [lpasek] pkey_generator - gallery_dimension'),
	(452,'2024-10-24','13.5.2020 [pgajdos] Pridanie stlpca update_date do _properties_ tabulky'),
	(453,'2024-10-24','08.07.2020 [pgajdos] Pridanie stlpca task_name do tabulky crontab, umiestnenie stlpca na druhe miesto'),
	(454,'2024-10-24','14.10.2020 [jeeff] Oprava nazvov stlpcov pkey generatora'),
	(455,'2024-10-24','15.4.2021 [jeeff] _conf_prepared_ -> zmena date_prepared aby povoloval null hodnoty'),
	(456,'2024-10-24','14.12.2021 [sivan] Pridanie stlpcov temp_field_a_docid ... temp_field_d_docid do tabuľky documents'),
	(457,'2024-10-24','14.12.2021 [sivan] Pridanie stlpcov temp_field_a_docid ... temp_field_d_docid do tabuľky documents_history'),
	(458,'2024-10-24','14.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_menu, logged_show_in_navbar, logged_show_in_sitemap) do tabulky documents'),
	(459,'2024-10-24','18.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_menu, logged_show_in_navbar, logged_show_in_sitemap) do tabulky documents_history'),
	(460,'2024-10-24','28.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_navbar, logged_show_in_sitemap) do tabulky groups.'),
	(461,'2024-10-24','28.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_navbar, logged_show_in_sitemap) do tabulky groups_scheduler.'),
	(462,'2024-10-24','28.12.2021 [sivan] Pridanie boolean stlpcov url_inherit_group, generate_url_from_title a editor_virtual_path do tabulky documents'),
	(463,'2024-10-24','4.1.2022 [sivan] Pridanie boolean stlpcov url_inherit_group, generate_url_from_title a editor_virtual_path do tabulky documents_history'),
	(464,'2024-10-24','21.1.2022 [lbalat] Nastavenie default hodnoty temp_field_d_docid'),
	(465,'2024-10-24','21.12.2021 [sivan] Zmena primarneho kluca tabulky form_regular_exp na nový INT stlpec id'),
	(466,'2024-10-24','18.2.2022 [jeeff] users - zmena password aby povoloval null hodnoty'),
	(467,'2024-10-24','04.10.2022 [sivan] Pridanie zmena typu stĺpcov cas_od a cas_do z varchar na datetime.'),
	(468,'2024-10-24','04.10.2022 [sivan] Uprava typov stĺpcov reservation_time_from a reservation_time_to z varchar na Datetime, time_unit z varchar na tinyint.'),
	(469,'2024-10-24','15.12.2022 [lbalat] Pridanie stlpcov field_a-f do tabulky banner_banners'),
	(470,'2024-10-24','20.12.2022 [lbalat] inquiry - podpora vkladania HTML kodu'),
	(471,'2024-10-24','22.12.2022 [lbalat] users - pridanie stlpca api_key'),
	(472,'2024-10-24','07.02.2023 [lbalat] pkey-generator - oprava nazvov, fix oracle documents'),
	(473,'2024-10-24','11.03.2023 [lbalat] vypnutie FileIndexer cron jobu'),
	(474,'2024-10-24','27.3.2023 [lbalat] Pridanie stlpcov publish_after_start do tabulky documents_history pre historicke uchovanie stavu publikovania'),
	(475,'2024-10-24','13.04.2023 [lbalat] reservation_object_times - fix Oracle'),
	(476,'2024-10-24','18.04.2023 [pbielik] JTB-1630 - conf_prepared value length'),
	(477,'2024-10-24','21.4.2023 [lbalat] Pridanie stlpca id do doc_atr tabulky'),
	(478,'2024-10-24','25.4.2023 [lbalat] doc_atr_def - pridanie domain_id'),
	(479,'2024-10-24','11.5.2023 [lbalat] Banner - oprava NULL stlpcov'),
	(480,'2024-10-24','26.05.2023 [sivan] Pridanie novej tabuľky response_headers'),
	(481,'2024-10-24','8.6.2023 [sivan] media - pridanie stlpca domain_id'),
	(482,'2024-10-24','05.06.2023 [sivan] #55285 - WebJET9 - Banner adresarova struktura zobrazenia'),
	(483,'2024-10-24','14.6.2022 [lbalat] users - zmena password aby povoloval null hodnoty aj pre mysql'),
	(484,'2024-10-24','7.7.2023 [lbalat] Oracle - nastavenie COLLATE bez ohladu na velkost pismen pre vyhladavanie'),
	(485,'2024-10-24','20.07.2023 [sivan] cluster_monitoring - zvacsenie pola pre content'),
	(486,'2024-10-24','17.08.2023 [lbalat] export_dat - rename xml format to rss'),
	(487,'2024-10-24','10.08.2023 [sivan] seo_keywords - pridanie stlpca actual_position'),
	(488,'2024-10-24','14.09.2023 [lbalat] proxy - podpora viacerych lokalnych URL adries'),
	(489,'2024-10-24','14.09.2023 [sivan] Pridanie stlpcov field_a-f do tabulky media'),
	(490,'2024-10-24','15.11.2023 [lbalat] rating - change class name'),
	(491,'2024-10-24','23.12.2023 [lbalat] templates - add inline_editing_mode'),
	(492,'2024-10-24','18.3.2024 [sivan] emails_campain - pridanie domain_id'),
	(493,'2024-10-24','18.3.2024 [sivan] emails - pridanie domain_id'),
	(494,'2024-10-24','18.3.2024 [sivan] emails_unsubscribed - pridanie domain_id'),
	(495,'2024-10-24','09.04.2024 [sivan] questions_answers - pridanie answer_to_email'),
	(496,'2024-10-24','21.04.2024 [sivan] Pridanie stlpca id do dirprop tabulky'),
	(497,'2024-10-24','20.06.2024 [sivan] reservation - pridanie stĺpca price'),
	(498,'2024-10-24','18.6.2024 [lbalat] cookies - update domain_id for not multidomain webs'),
	(499,'2024-10-24','30.07.2024 [sivan] file_history - pridanie domain_id'),
	(500,'2024-10-24','7.8.2024 [lbalat] pgsql - groups_scheduler fixes'),
	(501,'2024-10-24','29.07.2024 [sivan] Nastav stlpec name v tabuľke enumeration_type ako required-pgsql'),
	(502,'2024-10-24','29.07.2024 [sivan] Nastav stlpec name v tabuľke enumeration_type ako required.'),
	(503,'2024-10-24','23.07.2024 [sivan] Pridanie boolean stlpca notify_page_author do tabulky forum.'),
	(504,'2024-10-24','07.08.2024 [sivan] reservation - pridanie stlpca user_id'),
	(505,'2024-10-24','07.08.2024 [sivan] user_groups - pridanie stlpca price_discount'),
	(506,'2024-10-24','19.09.2024 [sivan] gallery - zmena typu stlpca author'),
	(507,'2024-10-24','18.10.2024 [lbalat] adminlog_notify - zmena typu stlpca text'),
	(508,'2024-10-24','priprava synchronizacie'),
	(509,'2024-10-24','nastavenie reg date pouzivatelov'),
	(510,'2024-10-24','konverzia perex_group v tab. documents z name na id'),
	(511,'2024-10-24','nastavenie hodnot group_id pre tabulku stat_views'),
	(512,'2024-10-24','18.3.2024 [sivan] pridanie stlpcu domain_id do tabulky emails_campain'),
	(513,'2024-10-24','14.3.2009 [kmarton] nastavenie spravnych hodnot browserId kvoli kompatibilite noveho kodu, ovplynuje tabulku stat_from a stat_views'),
	(514,'2024-10-24','11.01.2010 [jraska] rozdelenie pristupovych prav ovladacieho panela na jednotlive podkategorie'),
	(515,'2024-10-24','14.5.2010 [jeeff] pridanie novych stlpcov do stat_views'),
	(516,'2024-10-24','10.03.2018 [lbalat] nastavenie defaultneho map providera podla nastavenia googleMapsApiKey'),
	(517,'2024-10-24','20.3.2024 [jeeff] stat_error add domain_id column'),
	(518,'2024-10-24','NEW MODULE: editorMiniEdit'),
	(519,'2024-10-24','NEW MODULE: editor_show_hidden_folders'),
	(520,'2024-10-24','NEW MODULE: cmp_contact'),
	(521,'2024-10-24','NEW MODULE: null'),
	(522,'2024-10-24','NEW MODULE: components.news.edit_templates'),
	(523,'2024-10-24','NEW MODULE: editor_unlimited_upload'),
	(524,'2024-10-24','NEW MODULE: conf.show_all_variables'),
	(525,'2024-10-24','NEW MODULE: cmp_adminlog_logging'),
	(526,'2024-10-24','NEW MODULE: cmp_in-memory-logging'),
	(527,'2024-10-24','NEW MODULE: editor_edit_perex');

/*!40000 ALTER TABLE `_db_` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table _modules_
# ------------------------------------------------------------

DROP TABLE IF EXISTS `_modules_`;

CREATE TABLE `_modules_` (
  `module_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `name_key` varchar(128) NOT NULL DEFAULT '',
  `item_key` varchar(64) NOT NULL DEFAULT '',
  `path` varchar(255) NOT NULL DEFAULT '',
  `menu_order` int(11) DEFAULT 900,
  UNIQUE KEY `module_id` (`module_id`),
  KEY `module_id_2` (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table _properties_
# ------------------------------------------------------------

DROP TABLE IF EXISTS `_properties_`;

CREATE TABLE `_properties_` (
  `prop_key` varchar(255) NOT NULL DEFAULT '',
  `lng` char(3) NOT NULL DEFAULT '',
  `prop_value` longtext DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `prop_key` (`prop_key`,`lng`),
  KEY `prop_key_2` (`prop_key`,`lng`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table admin_message
# ------------------------------------------------------------

DROP TABLE IF EXISTS `admin_message`;

CREATE TABLE `admin_message` (
  `admin_message_id` int(10) unsigned NOT NULL,
  `create_date` datetime NOT NULL,
  `create_by_user_id` int(11) NOT NULL,
  `recipient_user_id` int(11) NOT NULL,
  `only_for_logged` tinyint(1) unsigned NOT NULL DEFAULT 0,
  `message_text` text NOT NULL,
  `is_readed` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`admin_message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table adminlog_notify
# ------------------------------------------------------------

DROP TABLE IF EXISTS `adminlog_notify`;

CREATE TABLE `adminlog_notify` (
  `adminlog_notify_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `adminlog_type` int(11) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `text` text DEFAULT NULL,
  PRIMARY KEY (`adminlog_notify_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `adminlog_notify` WRITE;
/*!40000 ALTER TABLE `adminlog_notify` DISABLE KEYS */;

INSERT INTO `adminlog_notify` (`adminlog_notify_id`, `adminlog_type`, `email`, `text`)
VALUES
	(1,150,'web.spam@interway.sk','waiting for idle object');

/*!40000 ALTER TABLE `adminlog_notify` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table alarm_action
# ------------------------------------------------------------

DROP TABLE IF EXISTS `alarm_action`;

CREATE TABLE `alarm_action` (
  `alarm_id` int(4) NOT NULL DEFAULT 0,
  `days` int(4) DEFAULT NULL,
  `doc_id` int(4) DEFAULT NULL,
  KEY `alarm_id` (`alarm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table appcache
# ------------------------------------------------------------

DROP TABLE IF EXISTS `appcache`;

CREATE TABLE `appcache` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `path` varchar(255) DEFAULT NULL,
  `filename` varchar(255) DEFAULT NULL,
  `version` int(11) DEFAULT NULL,
  `created` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;



# Dump of table appcache_file
# ------------------------------------------------------------

DROP TABLE IF EXISTS `appcache_file`;

CREATE TABLE `appcache_file` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `is_recursive` tinyint(4) DEFAULT NULL,
  `path` varchar(255) DEFAULT NULL,
  `filename` varchar(255) DEFAULT NULL,
  `extension` varchar(255) DEFAULT NULL,
  `created` datetime DEFAULT NULL,
  `appcache_id` int(11) NOT NULL,
  PRIMARY KEY (`id`,`appcache_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;



# Dump of table appcache_page
# ------------------------------------------------------------

DROP TABLE IF EXISTS `appcache_page`;

CREATE TABLE `appcache_page` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `is_recursive` tinyint(4) DEFAULT NULL,
  `path` varchar(255) DEFAULT NULL,
  `created` datetime DEFAULT NULL,
  `appcache_id` int(11) NOT NULL,
  PRIMARY KEY (`id`,`appcache_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table banner_banners
# ------------------------------------------------------------

DROP TABLE IF EXISTS `banner_banners`;

CREATE TABLE `banner_banners` (
  `banner_id` int(11) NOT NULL,
  `banner_type` int(11) NOT NULL DEFAULT 1,
  `banner_group` varchar(128) DEFAULT NULL,
  `priority` int(10) unsigned DEFAULT 1,
  `active` tinyint(1) NOT NULL DEFAULT 0,
  `banner_location` varchar(255) DEFAULT NULL,
  `banner_redirect` varchar(255) DEFAULT NULL,
  `width` int(10) DEFAULT NULL,
  `height` int(10) DEFAULT NULL,
  `html_code` text DEFAULT NULL,
  `date_from` datetime DEFAULT NULL,
  `date_to` datetime DEFAULT NULL,
  `max_views` int(10) DEFAULT NULL,
  `max_clicks` int(10) DEFAULT NULL,
  `stat_views` int(11) DEFAULT 0,
  `stat_clicks` int(11) DEFAULT 0,
  `stat_date` datetime DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `target` varchar(16) DEFAULT NULL,
  `click_tag` varchar(32) DEFAULT NULL,
  `frame_rate` int(11) DEFAULT NULL,
  `client_id` int(11) DEFAULT -1,
  `domain_id` int(11) DEFAULT 1,
  `visitor_cookie_group` varchar(100) DEFAULT NULL,
  `image_link` varchar(255) DEFAULT NULL,
  `image_link_mobile` varchar(255) DEFAULT NULL,
  `primary_header` varchar(255) DEFAULT NULL,
  `secondary_header` varchar(255) DEFAULT NULL,
  `description_text` text DEFAULT NULL,
  `primary_link_title` varchar(255) DEFAULT NULL,
  `primary_link_url` varchar(255) DEFAULT NULL,
  `primary_link_target` varchar(7) DEFAULT NULL,
  `secondary_link_title` varchar(255) DEFAULT NULL,
  `secondary_link_url` varchar(255) DEFAULT NULL,
  `secondary_link_target` varchar(7) DEFAULT NULL,
  `campaign_title` varchar(255) DEFAULT NULL,
  `only_with_campaign` tinyint(1) DEFAULT 0,
  `field_a` varchar(255) DEFAULT NULL,
  `field_b` varchar(255) DEFAULT NULL,
  `field_c` varchar(255) DEFAULT NULL,
  `field_d` varchar(255) DEFAULT NULL,
  `field_e` varchar(255) DEFAULT NULL,
  `field_f` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`banner_id`),
  KEY `banner_id` (`banner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table banner_doc
# ------------------------------------------------------------

DROP TABLE IF EXISTS `banner_doc`;

CREATE TABLE `banner_doc` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `doc_id` int(11) DEFAULT NULL,
  `banner_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table banner_gr
# ------------------------------------------------------------

DROP TABLE IF EXISTS `banner_gr`;

CREATE TABLE `banner_gr` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `group_id` int(11) DEFAULT NULL,
  `banner_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table banner_stat_clicks
# ------------------------------------------------------------

DROP TABLE IF EXISTS `banner_stat_clicks`;

CREATE TABLE `banner_stat_clicks` (
  `id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `banner_id` int(4) unsigned NOT NULL DEFAULT 0,
  `insert_date` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `ip` varchar(16) DEFAULT NULL,
  `host` varchar(128) DEFAULT NULL,
  `clicks` int(4) unsigned DEFAULT 1,
  `domain_id` int(11) DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `i_banner_id` (`banner_id`),
  KEY `i_insert_date` (`insert_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table banner_stat_views
# ------------------------------------------------------------

DROP TABLE IF EXISTS `banner_stat_views`;

CREATE TABLE `banner_stat_views` (
  `id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `banner_id` int(4) unsigned NOT NULL DEFAULT 0,
  `insert_date` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `ip` varchar(16) DEFAULT NULL,
  `host` varchar(128) DEFAULT NULL,
  `views` int(4) unsigned DEFAULT 1,
  `domain_id` int(11) DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `i_banner_id` (`banner_id`),
  KEY `i_insert_date` (`insert_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table banner_stat_views_day
# ------------------------------------------------------------

DROP TABLE IF EXISTS `banner_stat_views_day`;

CREATE TABLE `banner_stat_views_day` (
  `view_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `banner_id` int(10) unsigned NOT NULL,
  `insert_date` date NOT NULL,
  `views` int(10) unsigned NOT NULL DEFAULT 1,
  `domain_id` int(11) DEFAULT 1,
  PRIMARY KEY (`view_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table basket_invoice
# ------------------------------------------------------------

DROP TABLE IF EXISTS `basket_invoice`;

CREATE TABLE `basket_invoice` (
  `basket_invoice_id` int(10) unsigned NOT NULL,
  `browser_id` bigint(20) unsigned NOT NULL DEFAULT 0,
  `logged_user_id` int(11) NOT NULL,
  `create_date` datetime NOT NULL,
  `status_id` int(11) DEFAULT NULL,
  `delivery_company` varchar(255) DEFAULT NULL,
  `delivery_name` varchar(255) DEFAULT NULL,
  `delivery_street` varchar(255) DEFAULT NULL,
  `delivery_city` varchar(255) DEFAULT NULL,
  `delivery_zip` varchar(10) DEFAULT NULL,
  `delivery_country` varchar(255) DEFAULT NULL,
  `internal_invoice_id` varchar(255) DEFAULT NULL,
  `user_note` text DEFAULT NULL,
  `user_lng` varchar(6) DEFAULT NULL,
  `payment_method` varchar(255) DEFAULT NULL,
  `delivery_method` varchar(255) DEFAULT NULL,
  `contact_title` varchar(32) DEFAULT NULL,
  `contact_first_name` varchar(255) DEFAULT NULL,
  `contact_last_name` varchar(255) DEFAULT NULL,
  `contact_email` varchar(255) DEFAULT NULL,
  `contact_phone` varchar(255) DEFAULT NULL,
  `contact_company` varchar(255) DEFAULT NULL,
  `contact_street` varchar(255) DEFAULT NULL,
  `contact_city` varchar(255) DEFAULT NULL,
  `contact_zip` varchar(10) DEFAULT NULL,
  `contact_country` varchar(255) DEFAULT NULL,
  `contact_ico` varchar(32) DEFAULT NULL,
  `contact_icdph` varchar(32) DEFAULT NULL,
  `contact_dic` varchar(32) DEFAULT NULL,
  `field_a` varchar(255) DEFAULT NULL,
  `field_b` varchar(255) DEFAULT NULL,
  `field_c` varchar(255) DEFAULT NULL,
  `field_d` varchar(255) DEFAULT NULL,
  `field_e` varchar(255) DEFAULT NULL,
  `field_f` varchar(255) DEFAULT NULL,
  `html_code` text DEFAULT NULL,
  `currency` varchar(8) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  `delivery_surname` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`basket_invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table basket_invoice_payments
# ------------------------------------------------------------

DROP TABLE IF EXISTS `basket_invoice_payments`;

CREATE TABLE `basket_invoice_payments` (
  `payment_id` int(11) NOT NULL AUTO_INCREMENT,
  `invoice_id` int(11) NOT NULL,
  `create_date` datetime NOT NULL,
  `payed_price` decimal(10,2) NOT NULL DEFAULT 0.00,
  `payment_method` varchar(255) DEFAULT NULL,
  `closed_date` datetime DEFAULT NULL,
  `confirmed` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table basket_item
# ------------------------------------------------------------

DROP TABLE IF EXISTS `basket_item`;

CREATE TABLE `basket_item` (
  `basket_item_id` int(10) unsigned NOT NULL,
  `browser_id` bigint(20) unsigned NOT NULL DEFAULT 0,
  `logged_user_id` int(11) NOT NULL,
  `item_id` int(10) unsigned NOT NULL,
  `item_title` varchar(255) DEFAULT NULL,
  `item_part_no` varchar(255) DEFAULT NULL,
  `item_price` double NOT NULL,
  `item_vat` double DEFAULT NULL,
  `item_qty` int(10) unsigned NOT NULL,
  `item_note` varchar(255) DEFAULT NULL,
  `date_insert` datetime NOT NULL,
  `basket_invoice_id` int(11) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`basket_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table bazar_advertisements
# ------------------------------------------------------------

DROP TABLE IF EXISTS `bazar_advertisements`;

CREATE TABLE `bazar_advertisements` (
  `ad_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `group_id` int(10) unsigned DEFAULT NULL,
  `user_id` int(10) unsigned DEFAULT NULL,
  `description` text DEFAULT NULL,
  `contact` varchar(255) DEFAULT NULL,
  `confirmation` tinyint(1) unsigned DEFAULT NULL,
  `image` varchar(255) DEFAULT NULL,
  `price` varchar(255) DEFAULT NULL,
  `date_insert` datetime DEFAULT NULL,
  PRIMARY KEY (`ad_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table bazar_groups
# ------------------------------------------------------------

DROP TABLE IF EXISTS `bazar_groups`;

CREATE TABLE `bazar_groups` (
  `group_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `parent_group_id` int(10) unsigned DEFAULT NULL,
  `group_name` varchar(255) DEFAULT NULL,
  `allow_ad_insert` tinyint(1) unsigned DEFAULT NULL,
  `require_approve` tinyint(1) unsigned DEFAULT NULL,
  PRIMARY KEY (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table cache
# ------------------------------------------------------------

DROP TABLE IF EXISTS `cache`;

CREATE TABLE `cache` (
  `cache_id` int(11) NOT NULL,
  `data_type` int(11) NOT NULL,
  `data_value` varchar(2000) DEFAULT NULL,
  `data_result` mediumtext DEFAULT NULL,
  `refresh_minutes` int(11) DEFAULT NULL,
  `next_refresh` datetime DEFAULT NULL,
  PRIMARY KEY (`cache_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table calendar
# ------------------------------------------------------------

DROP TABLE IF EXISTS `calendar`;

CREATE TABLE `calendar` (
  `calendar_id` int(11) NOT NULL AUTO_INCREMENT,
  `title` text NOT NULL,
  `description` text DEFAULT NULL,
  `date_from` datetime NOT NULL DEFAULT '2000-01-01 00:00:00',
  `date_to` datetime NOT NULL DEFAULT '2000-01-01 00:00:00',
  `type_id` int(3) NOT NULL DEFAULT 0,
  `time_range` varchar(128) DEFAULT NULL,
  `area` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `info_1` varchar(255) DEFAULT NULL,
  `info_2` varchar(255) DEFAULT NULL,
  `info_3` varchar(255) DEFAULT NULL,
  `info_4` varchar(255) DEFAULT NULL,
  `info_5` varchar(255) DEFAULT NULL,
  `notify_hours_before` int(4) DEFAULT 0,
  `notify_emails` text DEFAULT NULL,
  `notify_sender` varchar(255) DEFAULT NULL,
  `notify_sent` tinyint(1) unsigned NOT NULL DEFAULT 0,
  `notify_introtext` text DEFAULT NULL,
  `notify_sendsms` tinyint(1) unsigned DEFAULT 0,
  `lng` char(3) DEFAULT NULL,
  `creator_id` int(11) NOT NULL DEFAULT -1,
  `approve` int(1) NOT NULL DEFAULT 1,
  `suggest` tinyint(1) NOT NULL DEFAULT 0,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  `hash_string` varchar(32) DEFAULT NULL,
  KEY `calendar_id` (`calendar_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table calendar_invitation
# ------------------------------------------------------------

DROP TABLE IF EXISTS `calendar_invitation`;

CREATE TABLE `calendar_invitation` (
  `calendar_invitation_id` int(11) NOT NULL AUTO_INCREMENT,
  `calendar_id` int(11) unsigned NOT NULL,
  `user_id` int(11) unsigned NOT NULL,
  `sent_date` datetime NOT NULL,
  `status_date` datetime DEFAULT NULL,
  `status` char(1) NOT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`calendar_invitation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table calendar_name_in_year
# ------------------------------------------------------------

DROP TABLE IF EXISTS `calendar_name_in_year`;

CREATE TABLE `calendar_name_in_year` (
  `day` int(4) NOT NULL DEFAULT 0,
  `month` int(4) NOT NULL DEFAULT 0,
  `name` varchar(200) DEFAULT NULL,
  `calendar_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `lng` varchar(10) NOT NULL DEFAULT 'SK',
  PRIMARY KEY (`calendar_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table calendar_types
# ------------------------------------------------------------

DROP TABLE IF EXISTS `calendar_types`;

CREATE TABLE `calendar_types` (
  `type_id` int(3) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL DEFAULT '',
  `schvalovatel_id` int(11) NOT NULL DEFAULT -1,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`type_id`),
  KEY `type_id` (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `calendar_types` WRITE;
/*!40000 ALTER TABLE `calendar_types` DISABLE KEYS */;

INSERT INTO `calendar_types` (`type_id`, `name`, `schvalovatel_id`, `domain_id`)
VALUES
	(1,'Výstava',-1,1),
	(2,'Šport',-1,1),
	(3,'Kultúra',-1,1),
	(4,'Rodina',-1,1),
	(5,'Konferencia',-1,1);

/*!40000 ALTER TABLE `calendar_types` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table captcha_dictionary
# ------------------------------------------------------------

DROP TABLE IF EXISTS `captcha_dictionary`;

CREATE TABLE `captcha_dictionary` (
  `id` int(11) NOT NULL,
  `word` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table cluster_monitoring
# ------------------------------------------------------------

DROP TABLE IF EXISTS `cluster_monitoring`;

CREATE TABLE `cluster_monitoring` (
  `node` varchar(64) NOT NULL,
  `type` varchar(64) NOT NULL,
  `content` mediumtext DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`node`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table cluster_refresher
# ------------------------------------------------------------

DROP TABLE IF EXISTS `cluster_refresher`;

CREATE TABLE `cluster_refresher` (
  `cluster_refresh_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `node_name` varchar(255) DEFAULT NULL,
  `class_name` varchar(255) DEFAULT NULL,
  `refresh_time` datetime DEFAULT NULL,
  PRIMARY KEY (`cluster_refresh_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table contact
# ------------------------------------------------------------

DROP TABLE IF EXISTS `contact`;

CREATE TABLE `contact` (
  `contact_id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `ico` varchar(16) DEFAULT NULL,
  `vatid` varchar(32) DEFAULT NULL,
  `street` varchar(128) DEFAULT NULL,
  `city` varchar(64) DEFAULT NULL,
  `zip` varchar(8) DEFAULT NULL,
  `country` varchar(32) DEFAULT NULL,
  `contact` varchar(255) DEFAULT NULL,
  `phone` varchar(32) DEFAULT NULL,
  `fax` varchar(32) DEFAULT NULL,
  `email` varchar(64) DEFAULT NULL,
  `web` varchar(255) DEFAULT NULL,
  `gps` varchar(64) DEFAULT NULL,
  `category` varchar(255) DEFAULT NULL,
  `field_a` varchar(255) DEFAULT NULL,
  `field_b` varchar(255) DEFAULT NULL,
  `field_c` varchar(255) DEFAULT NULL,
  `field_d` varchar(255) DEFAULT NULL,
  `field_e` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`contact_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table cookies
# ------------------------------------------------------------

DROP TABLE IF EXISTS `cookies`;

CREATE TABLE `cookies` (
  `cookie_id` int(11) NOT NULL,
  `domain_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `save_date` datetime DEFAULT NULL,
  `cookie_name` varchar(255) DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `provider` varchar(255) DEFAULT NULL,
  `purpouse` varchar(255) DEFAULT NULL,
  `validity` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `classification` varchar(255) DEFAULT NULL,
  `application` varchar(255) DEFAULT NULL,
  `typical_value` varchar(255) DEFAULT NULL,
  `party_3rd` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`cookie_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `cookies` WRITE;
/*!40000 ALTER TABLE `cookies` DISABLE KEYS */;

INSERT INTO `cookies` (`cookie_id`, `domain_id`, `user_id`, `save_date`, `cookie_name`, `description`, `provider`, `purpouse`, `validity`, `type`, `classification`, `application`, `typical_value`, `party_3rd`)
VALUES
	(1,1,-1,NULL,'JSESSIONID',NULL,NULL,NULL,NULL,'http','nutne',NULL,NULL,NULL),
	(2,1,-1,NULL,'lng',NULL,NULL,NULL,NULL,'http','nutne',NULL,NULL,NULL),
	(3,1,-1,NULL,'statBrowserIdNew',NULL,NULL,NULL,NULL,'http','statisticke',NULL,NULL,NULL),
	(4,1,-1,NULL,'forumemail',NULL,NULL,NULL,NULL,'http','statisticke',NULL,NULL,NULL),
	(5,1,-1,NULL,'forumname',NULL,NULL,NULL,NULL,'http','statisticke',NULL,NULL,NULL),
	(6,1,-1,NULL,'_ga',NULL,NULL,NULL,NULL,'http','marketingove',NULL,NULL,NULL),
	(7,1,-1,NULL,'_gat',NULL,NULL,NULL,NULL,'http','marketingove',NULL,NULL,NULL),
	(8,1,-1,NULL,'__utmt',NULL,NULL,NULL,NULL,'http','marketingove',NULL,NULL,NULL),
	(9,1,-1,NULL,'__utma',NULL,NULL,NULL,NULL,'http','marketingove',NULL,NULL,NULL),
	(10,1,-1,NULL,'__utmb',NULL,NULL,NULL,NULL,'http','marketingove',NULL,NULL,NULL),
	(11,1,-1,NULL,'__utmc',NULL,NULL,NULL,NULL,'http','marketingove',NULL,NULL,NULL),
	(12,1,-1,NULL,'__utmz',NULL,NULL,NULL,NULL,'http','marketingove',NULL,NULL,NULL),
	(13,1,-1,NULL,'__utmv',NULL,NULL,NULL,NULL,'http','marketingove',NULL,NULL,NULL),
	(14,1,-1,NULL,'id',NULL,NULL,NULL,NULL,'http','marketingove',NULL,NULL,NULL),
	(15,1,-1,NULL,'drt',NULL,NULL,NULL,NULL,'http','marketingove',NULL,NULL,NULL);

/*!40000 ALTER TABLE `cookies` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table crontab
# ------------------------------------------------------------

DROP TABLE IF EXISTS `crontab`;

CREATE TABLE `crontab` (
  `id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `task_name` varchar(64) DEFAULT NULL,
  `second` varchar(64) DEFAULT '0',
  `minute` varchar(64) DEFAULT '*',
  `hour` varchar(64) DEFAULT '*',
  `dayofmonth` varchar(64) DEFAULT '*',
  `month` varchar(64) DEFAULT '*',
  `dayofweek` varchar(64) DEFAULT '*',
  `year` varchar(64) DEFAULT '*',
  `task` varchar(255) DEFAULT NULL,
  `extrainfo` varchar(255) DEFAULT NULL,
  `businessDays` varchar(6) DEFAULT 'true',
  `cluster_node` varchar(64) DEFAULT NULL,
  `audit_task` varchar(6) DEFAULT NULL,
  `run_at_startup` tinyint(1) DEFAULT 0,
  `enable_task` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `crontab` WRITE;
/*!40000 ALTER TABLE `crontab` DISABLE KEYS */;

INSERT INTO `crontab` (`id`, `task_name`, `second`, `minute`, `hour`, `dayofmonth`, `month`, `dayofweek`, `year`, `task`, `extrainfo`, `businessDays`, `cluster_node`, `audit_task`, `run_at_startup`, `enable_task`)
VALUES
	(1,'Zmazanie starých udalostí v kalendári','0','*/10','*','*','*','*','*','sk.iway.iwcm.calendar.CalendarDB',NULL,'true','all','1',0,1),
	(3,'Kontrola pozícií kľúčových slov v Google','0','0','2','*','*','*','*','sk.iway.iwcm.components.seo.SeoManager','','0','all','1',0,1),
	(4,'Vytvorenie monitoringu systému','*/30','*','*','*','*','*','*','sk.iway.iwcm.system.monitoring.MonitoringManager','','0','all','0',0,1),
	(5,'Publikovanie zmien v adresároch web stránok','0','*','*','*','*','*','*','sk.iway.iwcm.doc.GroupPublisher',NULL,'0',NULL,'0',0,1),
	(6,'Zmazanie záznamov heatmapy','0','30','5','*','*','*','*','sk.iway.iwcm.stat.heat_map.HeatMapCleaner',NULL,'true',NULL,'0',0,1),
	(7,'Zápis štatistiky návštevnosti','0','*','*','*','*','*','*','sk.iway.iwcm.stat.StatWriteBuffer',NULL,'true',NULL,'0',0,1),
	(9,'Aktualizácia persistent cache','30','*/5','*','*','*','*','*','sk.iway.iwcm.system.cache.PersistentCacheDB',NULL,'true','node3','false',0,1),
	(10,'Publikovanie zmien v konfigurácii','0','*/5','*','*','*','*','2013','sk.iway.iwcm.system.ConfPreparedPublisher',NULL,'false','all','false',0,1),
	(11,'Publikovanie zmien súborov v Archíve súborov','0','*/5','*','*','*','*','*','sk.iway.iwcm.components.file_archiv.FileArchivatorInsertLater',NULL,'true','all','false',0,0);

/*!40000 ALTER TABLE `crontab` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table customer_reviews
# ------------------------------------------------------------

DROP TABLE IF EXISTS `customer_reviews`;

CREATE TABLE `customer_reviews` (
  `review_id` int(11) NOT NULL,
  `stars` int(11) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `surname` varchar(255) DEFAULT NULL,
  `mail` varchar(255) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `date` date DEFAULT NULL,
  `show_to_public` tinyint(1) DEFAULT NULL,
  `approve_hash` varchar(255) DEFAULT NULL,
  `domain_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table dictionary
# ------------------------------------------------------------

DROP TABLE IF EXISTS `dictionary`;

CREATE TABLE `dictionary` (
  `dictionary_id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `name_orig` varchar(255) NOT NULL,
  `dictionary_group` varchar(128) NOT NULL,
  `value` longtext NOT NULL,
  `language` varchar(3) CHARACTER SET utf8mb3 COLLATE utf8mb3_slovak_ci DEFAULT NULL,
  `domain` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_slovak_ci DEFAULT NULL,
  PRIMARY KEY (`dictionary_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table dirprop
# ------------------------------------------------------------

DROP TABLE IF EXISTS `dirprop`;

CREATE TABLE `dirprop` (
  `dir_url` varchar(255) NOT NULL,
  `index_fulltext` tinyint(1) unsigned NOT NULL DEFAULT 0,
  `password_protected` varchar(255) DEFAULT NULL,
  `logon_doc_id` int(11) NOT NULL DEFAULT -1,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `dir_url` (`dir_url`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table doc_atr
# ------------------------------------------------------------

DROP TABLE IF EXISTS `doc_atr`;

CREATE TABLE `doc_atr` (
  `doc_id` int(4) unsigned NOT NULL DEFAULT 0,
  `atr_id` int(4) unsigned NOT NULL DEFAULT 0,
  `value_string` varchar(255) DEFAULT NULL,
  `value_int` double DEFAULT NULL,
  `value_bool` tinyint(1) unsigned DEFAULT 0,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  KEY `doc_id` (`doc_id`,`atr_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table doc_atr_def
# ------------------------------------------------------------

DROP TABLE IF EXISTS `doc_atr_def`;

CREATE TABLE `doc_atr_def` (
  `atr_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `atr_name` varchar(255) NOT NULL DEFAULT '',
  `order_priority` int(4) DEFAULT 10,
  `atr_description` varchar(255) DEFAULT NULL,
  `atr_default_value` longtext DEFAULT NULL,
  `atr_type` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `atr_group` varchar(32) DEFAULT 'default',
  `true_value` varchar(255) DEFAULT NULL,
  `false_value` varchar(255) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`atr_id`),
  KEY `atr_id` (`atr_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table doc_reactions
# ------------------------------------------------------------

DROP TABLE IF EXISTS `doc_reactions`;

CREATE TABLE `doc_reactions` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `USER_ID` int(11) NOT NULL,
  `DOC_ID` int(11) NOT NULL,
  `REACTION` int(11) NOT NULL,
  `CREATED_AT` datetime NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `doc_reactions_ID_uindex` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table doc_subscribe
# ------------------------------------------------------------

DROP TABLE IF EXISTS `doc_subscribe`;

CREATE TABLE `doc_subscribe` (
  `subscribe_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `doc_id` int(11) DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `user_id` int(11) DEFAULT -1,
  PRIMARY KEY (`subscribe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table document_forum
# ------------------------------------------------------------

DROP TABLE IF EXISTS `document_forum`;

CREATE TABLE `document_forum` (
  `forum_id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `doc_id` int(11) unsigned NOT NULL DEFAULT 0,
  `parent_id` int(11) NOT NULL DEFAULT -1,
  `subject` varchar(255) DEFAULT NULL,
  `question` text DEFAULT NULL,
  `question_date` datetime DEFAULT NULL,
  `author_name` varchar(255) DEFAULT NULL,
  `author_email` varchar(255) DEFAULT NULL,
  `ip` varchar(255) DEFAULT NULL,
  `confirmed` tinyint(1) NOT NULL DEFAULT 1,
  `hash_code` varchar(64) DEFAULT NULL,
  `send_answer_notif` tinyint(1) unsigned DEFAULT 0,
  `user_id` int(11) DEFAULT -1,
  `flag` varchar(128) DEFAULT NULL,
  `stat_views` int(10) unsigned DEFAULT NULL,
  `stat_replies` int(10) unsigned DEFAULT NULL,
  `stat_last_post` datetime DEFAULT NULL,
  `active` tinyint(1) unsigned DEFAULT 1,
  `deleted` tinyint(1) DEFAULT 0,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`forum_id`),
  KEY `forum_id` (`forum_id`),
  KEY `i_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='diskusne forum';



# Dump of table document_notes
# ------------------------------------------------------------

DROP TABLE IF EXISTS `document_notes`;

CREATE TABLE `document_notes` (
  `id` int(11) NOT NULL,
  `doc_id` int(11) DEFAULT NULL,
  `history_id` int(11) DEFAULT NULL,
  `note` varchar(255) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `created` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table documents
# ------------------------------------------------------------

DROP TABLE IF EXISTS `documents`;

CREATE TABLE `documents` (
  `doc_id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL DEFAULT '',
  `data` mediumtext NOT NULL,
  `data_asc` mediumtext NOT NULL,
  `external_link` varchar(255) DEFAULT NULL,
  `navbar` text NOT NULL,
  `date_created` datetime NOT NULL DEFAULT '2000-01-01 00:00:00',
  `publish_start` datetime DEFAULT NULL,
  `publish_end` datetime DEFAULT NULL,
  `author_id` int(11) NOT NULL DEFAULT 0,
  `group_id` int(11) DEFAULT NULL,
  `temp_id` int(11) NOT NULL DEFAULT 0,
  `views_total` int(11) NOT NULL DEFAULT 0,
  `views_month` int(11) NOT NULL DEFAULT 0,
  `searchable` tinyint(1) NOT NULL DEFAULT 0,
  `available` tinyint(1) NOT NULL DEFAULT 0,
  `cacheable` tinyint(1) NOT NULL DEFAULT 0,
  `file_name` varchar(255) DEFAULT NULL,
  `file_change` datetime DEFAULT NULL,
  `sort_priority` int(11) NOT NULL DEFAULT 0,
  `header_doc_id` int(11) DEFAULT NULL,
  `menu_doc_id` int(11) NOT NULL DEFAULT -1,
  `footer_doc_id` int(11) NOT NULL DEFAULT -1,
  `password_protected` varchar(255) DEFAULT NULL,
  `html_head` text DEFAULT NULL,
  `html_data` text DEFAULT NULL,
  `perex_place` varchar(255) DEFAULT NULL,
  `perex_image` varchar(255) DEFAULT NULL,
  `perex_group` varchar(255) DEFAULT NULL,
  `show_in_menu` tinyint(1) unsigned DEFAULT 1,
  `event_date` datetime DEFAULT NULL,
  `virtual_path` varchar(255) DEFAULT NULL,
  `sync_id` int(11) DEFAULT 0,
  `sync_status` int(11) DEFAULT 0,
  `logon_page_doc_id` int(4) DEFAULT -1,
  `right_menu_doc_id` int(4) DEFAULT -1,
  `field_a` varchar(255) DEFAULT NULL,
  `field_b` varchar(255) DEFAULT NULL,
  `field_c` varchar(255) DEFAULT NULL,
  `field_d` varchar(255) DEFAULT NULL,
  `field_e` varchar(255) DEFAULT NULL,
  `field_f` varchar(255) DEFAULT NULL,
  `field_g` varchar(255) DEFAULT NULL,
  `field_h` varchar(255) DEFAULT NULL,
  `field_i` varchar(255) DEFAULT NULL,
  `field_j` varchar(255) DEFAULT NULL,
  `field_k` varchar(255) DEFAULT NULL,
  `field_l` varchar(255) DEFAULT NULL,
  `disable_after_end` tinyint(1) DEFAULT 0,
  `forum_count` int(11) DEFAULT 0,
  `field_m` varchar(255) DEFAULT NULL,
  `field_n` varchar(255) DEFAULT NULL,
  `field_o` varchar(255) DEFAULT NULL,
  `field_p` varchar(255) DEFAULT NULL,
  `field_q` varchar(255) DEFAULT NULL,
  `field_r` varchar(255) DEFAULT NULL,
  `field_s` varchar(255) DEFAULT NULL,
  `field_t` varchar(255) DEFAULT NULL,
  `require_ssl` tinyint(1) DEFAULT 0,
  `root_group_l1` int(11) DEFAULT NULL,
  `root_group_l2` int(11) DEFAULT NULL,
  `root_group_l3` int(11) DEFAULT NULL,
  `temp_field_a_docid` int(11) DEFAULT -1,
  `temp_field_b_docid` int(11) DEFAULT -1,
  `temp_field_c_docid` int(11) DEFAULT -1,
  `temp_field_d_docid` int(11) DEFAULT -1,
  `show_in_navbar` tinyint(1) DEFAULT NULL,
  `show_in_sitemap` tinyint(1) DEFAULT NULL,
  `logged_show_in_menu` tinyint(1) DEFAULT NULL,
  `logged_show_in_navbar` tinyint(1) DEFAULT NULL,
  `logged_show_in_sitemap` tinyint(1) DEFAULT NULL,
  `url_inherit_group` tinyint(1) DEFAULT 0,
  `generate_url_from_title` tinyint(1) DEFAULT 0,
  `editor_virtual_path` varchar(255) DEFAULT NULL,
  `publish_after_start` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`doc_id`),
  KEY `i_group_id` (`group_id`),
  KEY `IX_documents_cacheable` (`cacheable`),
  KEY `IX_documents_dae` (`disable_after_end`),
  KEY `IX_documents_pgl1` (`root_group_l1`),
  KEY `IX_documents_pgl2` (`root_group_l2`),
  KEY `IX_documents_pgl3` (`root_group_l3`),
  FULLTEXT KEY `search` (`title`,`data_asc`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;

INSERT INTO `documents` (`doc_id`, `title`, `data`, `data_asc`, `external_link`, `navbar`, `date_created`, `publish_start`, `publish_end`, `author_id`, `group_id`, `temp_id`, `views_total`, `views_month`, `searchable`, `available`, `cacheable`, `file_name`, `file_change`, `sort_priority`, `header_doc_id`, `menu_doc_id`, `footer_doc_id`, `password_protected`, `html_head`, `html_data`, `perex_place`, `perex_image`, `perex_group`, `show_in_menu`, `event_date`, `virtual_path`, `sync_id`, `sync_status`, `logon_page_doc_id`, `right_menu_doc_id`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`, `field_g`, `field_h`, `field_i`, `field_j`, `field_k`, `field_l`, `disable_after_end`, `forum_count`, `field_m`, `field_n`, `field_o`, `field_p`, `field_q`, `field_r`, `field_s`, `field_t`, `require_ssl`, `root_group_l1`, `root_group_l2`, `root_group_l3`, `temp_field_a_docid`, `temp_field_b_docid`, `temp_field_c_docid`, `temp_field_d_docid`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_menu`, `logged_show_in_navbar`, `logged_show_in_sitemap`, `url_inherit_group`, `generate_url_from_title`, `editor_virtual_path`, `publish_after_start`)
VALUES
	(1,'Default header','<h1>Headline</h1><p>Lorem ipsum dolor sit amet, consectetur adipisicing elit. Sit aspernatur labore vel rem adipisci commodi odit eum vitae asperiores esse?</p><a href=\'#\' class=\'btn btn-default\'>Lorem ipsum.</a>','','','Default hlavička','2002-07-25 00:00:00',NULL,NULL,1,4,1,0,0,0,1,0,'1_default_hlavicka.html','2003-04-11 15:51:10',50,-1,-1,-1,NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,NULL,1,2,-1,-1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,-1,-1,-1,-1,NULL,NULL,NULL,NULL,NULL,0,0,NULL,0),
	(2,'Default navigation','!INCLUDE(/components/menu/menu_ul_li.jsp, rootGroupId=1, startOffset=0, maxLevel=3, classes=basic, openAllItems=true, rootUlId=mainNavigation)!','','','Default menu','2003-05-23 00:00:00',NULL,NULL,1,3,1,0,0,0,1,0,'2_default_menu.html','2003-08-27 10:36:59',50,-1,-1,-1,NULL,'','',NULL,NULL,NULL,1,NULL,NULL,2,2,-1,-1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,-1,-1,-1,-1,NULL,NULL,NULL,NULL,NULL,0,0,NULL,0),
	(3,'Default footer','<p>© !YEAR! InterWay, a. s. All Rights Reserved. Page generated by WebJET CMS.</p>','','','Default pätička','2002-07-25 00:00:00',NULL,NULL,1,4,1,0,0,0,1,0,'3_default_paticka.html','2003-04-11 15:51:10',50,-1,-1,-1,NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,NULL,3,2,-1,-1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,-1,-1,-1,-1,NULL,NULL,NULL,NULL,NULL,0,0,NULL,0),
	(4,'Hlavna stranka','<p>Vitajte na demo stránke systému WebJET.</p>\r\n<p>Viac informácií o systéme nájdete na stránke <a href=\'http://www.webjetcms.sk/\' target=\'_blank\'>www.webjetcms.sk</a>.</p>','<p>vitajte na demo stranke systemu webjet.</p>\r\n<p>viac informacii o systeme najdete na stranke <a href=\'http://www.webjetcms.sk/\' target=\'_blank\'>www.webjetcms.sk</a>.</p>','','Hlavna stranka','2003-10-05 22:43:25',NULL,NULL,1,1,1,0,0,1,1,0,'4_hlavna_stranka.html','2003-10-05 22:43:25',1,-1,-1,-1,NULL,'',NULL,'','',NULL,1,NULL,NULL,4,2,-1,-1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,-1,-1,-1,-1,NULL,NULL,NULL,NULL,NULL,0,0,NULL,0),
	(5,'Podstránka 1','<p>Toto je podstránka 1</p>\r\n<p>Ak chcete do stránky vložiť predpripravený objekt, kliknite v editore na komponenty a vyberte Predpripravené HTML. Tam si vyberte stránku a kliknutím na OK ju vložte do stránky. Nové predpripravené objekty si môžete vytvoriť v adresári System->HTMLBox.</p>\r\n<p>Pred použitím formuláru prosím najskôr naň kliknite pravým tlačítkom, dajte vlastnosti formuláru a zmeňte emailovú adresu.</p>\r\n<p>\r\n<script src=\'/components/form/check_form.js\' ENGINE=\'text/javascript\'></script>\r\n<script src=\'/components/form/fix_e.js\' ENGINE=\'text/javascript\'></script>\r\n<script src=\'/components/form/event_attacher.js\' ENGINE=\'text/javascript\'></script>\r\n<script src=\'/components/form/class_magic.js\' ENGINE=\'text/javascript\'></script>\r\n<script src=\'/components/form/check_form_impl.jsp\' ENGINE=\'text/javascript\'></script>\r\n<link media=\'screen\' href=\'/components/form/check_form.css\' ENGINE=\'text/css\' rel=\'stylesheet\'/>\r\n<form name=\'formMailForm\' action=\'/formmail.do?recipients=test@tester.org&savedb=Kontaktny_formular\' method=\'post\'>\r\n    <table cellspacing=\'0\' cellpadding=\'0\' border=\'0\'>\r\n        <tbody>\r\n            <tr>\r\n                <td>Vaše meno: </td>\r\n                <td><input class=\'required invalid\' maxlength=\'255\' name=\'meno\'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> Vaša emailová adresa:</td>\r\n                <td><input class=\'required email invalid\' maxlength=\'255\' name=\'email\'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td valign=\'top\'> Otázka / pripomienka:</td>\r\n                <td><textarea class=\'required invalid\' name=\'otazka\' rows=\'5\' cols=\'40\'></textarea> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> </td>\r\n                <td align=\'right\'> <input ENGINE=\'submit\' name=\'btnSubmit\' value=\'Odoslať\'/></td>\r\n            </tr>\r\n        </tbody>\r\n    </table>\r\n</form>\r\n</p>\r\n<p> </p>','<p>toto je podstranka 1</p>\r\n<p>ak chcete do stranky vlozit predpripraveny objekt, kliknite v editore na komponenty a vyberte predpripravene html. tam si vyberte stranku a kliknutim na ok ju vlozte do stranky. nove predpripravene objekty si mozete vytvorit v adresari system->htmlbox.</p>\r\n<p>pred pouzitim formularu prosim najskor nan kliknite pravym tlacitkom, dajte vlastnosti formularu a zmente emailovu adresu.</p>\r\n<p>\r\n<script src=\'/components/form/check_form.js\' ENGINE=\'text/javascript\'></script>\r\n<script src=\'/components/form/fix_e.js\' ENGINE=\'text/javascript\'></script>\r\n<script src=\'/components/form/event_attacher.js\' ENGINE=\'text/javascript\'></script>\r\n<script src=\'/components/form/class_magic.js\' ENGINE=\'text/javascript\'></script>\r\n<script src=\'/components/form/check_form_impl.jsp\' ENGINE=\'text/javascript\'></script>\r\n<link media=\'screen\' href=\'/components/form/check_form.css\' ENGINE=\'text/css\' rel=\'stylesheet\'/>\r\n<form name=\'formmailform\' action=\'/formmail.do?recipients=test@tester.org&savedb=kontaktny_formular\' method=\'post\'>\r\n    <table cellspacing=\'0\' cellpadding=\'0\' border=\'0\'>\r\n        <tbody>\r\n            <tr>\r\n                <td>vase meno: </td>\r\n                <td><input class=\'required invalid\' maxlength=\'255\' name=\'meno\'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> vasa emailova adresa:</td>\r\n                <td><input class=\'required email invalid\' maxlength=\'255\' name=\'email\'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td valign=\'top\'> otazka / pripomienka:</td>\r\n                <td><textarea class=\'required invalid\' name=\'otazka\' rows=\'5\' cols=\'40\'></textarea> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> </td>\r\n                <td align=\'right\'> <input ENGINE=\'submit\' name=\'btnsubmit\' value=\'odoslat\'/></td>\r\n            </tr>\r\n        </tbody>\r\n    </table>\r\n</form>\r\n</p>\r\n<p> </p>','','Podstránka 1','2003-10-05 22:43:25',NULL,NULL,1,1,1,0,0,1,1,0,'5_podstranka_1.html','2003-10-05 22:43:25',1,-1,-1,-1,NULL,'',NULL,'','',NULL,1,NULL,NULL,5,2,-1,-1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,-1,-1,-1,-1,NULL,NULL,NULL,NULL,NULL,0,0,NULL,0),
	(6,'Podstránka 2','<h1>Podstránka 2</h1><p>Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi id sapien ut massa interdum ultricies. Sed eget enim in ante ornare feugiat. Curabitur risus lectus, iaculis sed, pulvinar quis, convallis euismod, massa. Curabitur est enim, varius sed, hendrerit at, convallis elementum, tortor. In hac habitasse platea dictumst. Proin sagittis massa ac massa. Maecenas vel libero. Curabitur vestibulum pellentesque elit. Phasellus aliquet quam quis urna. In ultrices est vel lorem. Aliquam sit amet mi et nulla scelerisque vestibulum. Donec pellentesque tellus vitae massa. Curabitur euismod. Donec quis pede. Vivamus nulla mauris, aliquet sed, aliquet vitae, fringilla id, massa. Etiam id magna sed dolor rhoncus condimentum. Ut lacinia nonummy odio. Pellentesque interdum, tortor vitae congue elementum, nibh ipsum pellentesque quam, sed tempor tortor est sit amet felis. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nullam sagittis tellus vitae lorem.</p>','<h1>podstranka 2</h1><p>Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi id sapien ut massa interdum ultricies. Sed eget enim in ante ornare feugiat. Curabitur risus lectus, iaculis sed, pulvinar quis, convallis euismod, massa. Curabitur est enim, varius sed, hendrerit at, convallis elementum, tortor. In hac habitasse platea dictumst. Proin sagittis massa ac massa. Maecenas vel libero. Curabitur vestibulum pellentesque elit. Phasellus aliquet quam quis urna. In ultrices est vel lorem. Aliquam sit amet mi et nulla scelerisque vestibulum. Donec pellentesque tellus vitae massa. Curabitur euismod. Donec quis pede. Vivamus nulla mauris, aliquet sed, aliquet vitae, fringilla id, massa. Etiam id magna sed dolor rhoncus condimentum. Ut lacinia nonummy odio. Pellentesque interdum, tortor vitae congue elementum, nibh ipsum pellentesque quam, sed tempor tortor est sit amet felis. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nullam sagittis tellus vitae lorem.</p>','','Podstránka 2','2003-10-05 22:43:25',NULL,NULL,1,1,1,0,0,1,1,0,'6_podstranka_2.html','2003-10-05 22:43:25',1,-1,-1,-1,NULL,'',NULL,'','',NULL,1,NULL,NULL,6,2,-1,-1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,-1,-1,-1,-1,NULL,NULL,NULL,NULL,NULL,0,0,NULL,0),
	(7,'Stranka s nadpisom a 2 stlpcami','<table cellspacing=\'0\' cellpadding=\'2\' width=\'100%\' border=\'0\'>\r\n    <tbody>\r\n        <tr>\r\n            <td colspan=\'2\'>\r\n            <h1>Toto je nadpis stránky</h1>\r\n            </td>\r\n        </tr>\r\n        <tr>\r\n            <td valign=\'top\' width=\'50%\'>\r\n            <p> Toto je stlpec 1</p>\r\n            <p>Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Nam pulvinar sollicitudin est. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae. Duis nulla risus, varius non, condimentum nec, adipiscing vel, nisl. Vestibulum facilisis lorem. Nam tortor tellus, venenatis vitae, tincidunt in, fringilla ut, arcu. Suspendisse imperdiet magna egestas nibh. Sed rutrum. Nulla pellentesque mollis leo. Mauris vel metus eget dolor feugiat consequat. Nam dapibus dapibus felis. Phasellus sit amet tortor vel ante dictum aliquam. Integer vehicula nisi et quam euismod commodo. Nam vel justo. Sed mattis libero non enim. Donec feugiat tortor. Quisque mauris. Nullam urna turpis, aliquam sit amet, posuere eget, consectetuer nec, massa. In non mauris.</p>\r\n            </td>\r\n            <td valign=\'top\' width=\'50%\'>\r\n            <p> Toto je stlpec 2</p>\r\n            <p>Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Suspendisse in libero ac turpis porttitor porta. Aliquam varius massa vitae massa. Pellentesque fringilla diam vitae velit. Nulla facilisi. Sed id enim. Aenean in urna. Vivamus sed urna at elit porttitor ultrices. Morbi dolor felis, facilisis at, congue quis, convallis et, ipsum. Donec rutrum nulla luctus arcu luctus blandit. Mauris vitae ipsum et risus venenatis scelerisque. Duis ipsum. Donec viverra purus. Fusce non libero. Cras elementum. Curabitur fermentum elit at lorem. Etiam facilisis. Pellentesque nec erat ut ipsum consectetuer sodales. Morbi nec dolor ac velit rutrum faucibus. </p>\r\n            </td>\r\n        </tr>\r\n    </tbody>\r\n</table>','<table cellspacing=\'0\' cellpadding=\'2\' width=\'100%\' border=\'0\'>\r\n    <tbody>\r\n        <tr>\r\n            <td colspan=\'2\'>\r\n            <h1>toto je nadpis stranky</h1>\r\n            </td>\r\n        </tr>\r\n        <tr>\r\n            <td valign=\'top\' width=\'50%\'>\r\n            <p> toto je stlpec 1</p>\r\n            <p>lorem ipsum dolor sit amet, consectetuer adipiscing elit. nam pulvinar sollicitudin est. vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae. duis nulla risus, varius non, condimentum nec, adipiscing vel, nisl. vestibulum facilisis lorem. nam tortor tellus, venenatis vitae, tincidunt in, fringilla ut, arcu. suspendisse imperdiet magna egestas nibh. sed rutrum. nulla pellentesque mollis leo. mauris vel metus eget dolor feugiat consequat. nam dapibus dapibus felis. phasellus sit amet tortor vel ante dictum aliquam. integer vehicula nisi et quam euismod commodo. nam vel justo. sed mattis libero non enim. donec feugiat tortor. quisque mauris. nullam urna turpis, aliquam sit amet, posuere eget, consectetuer nec, massa. in non mauris.</p>\r\n            </td>\r\n            <td valign=\'top\' width=\'50%\'>\r\n            <p> toto je stlpec 2</p>\r\n            <p>lorem ipsum dolor sit amet, consectetuer adipiscing elit. suspendisse in libero ac turpis porttitor porta. aliquam varius massa vitae massa. pellentesque fringilla diam vitae velit. nulla facilisi. sed id enim. aenean in urna. vivamus sed urna at elit porttitor ultrices. morbi dolor felis, facilisis at, congue quis, convallis et, ipsum. donec rutrum nulla luctus arcu luctus blandit. mauris vitae ipsum et risus venenatis scelerisque. duis ipsum. donec viverra purus. fusce non libero. cras elementum. curabitur fermentum elit at lorem. etiam facilisis. pellentesque nec erat ut ipsum consectetuer sodales. morbi nec dolor ac velit rutrum faucibus. </p>\r\n            </td>\r\n        </tr>\r\n    </tbody>\r\n</table>','','Stranka s nadpisom a 2 stlpcami','2003-10-05 22:43:25',NULL,NULL,1,2,1,0,0,1,1,0,'7_s_nadpisom.html','2003-10-05 22:43:25',1,-1,-1,-1,NULL,'',NULL,'','',NULL,1,NULL,NULL,7,2,-1,-1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,-1,-1,-1,-1,NULL,NULL,NULL,NULL,NULL,0,0,NULL,0),
	(8,'Kontaktný formulár','<p>Pred použitím formuláru prosím najskôr naň kliknite pravým tlačítkom, dajte vlastnosti formuláru a zmeňte emailovú adresu.</p>\r\n<p>\r\n<script src=\'/components/form/check_form.js\' ENGINE=\'text/javascript\'></script>\r\n<form name=\'formMailForm\' action=\'/formmail.do?recipients=test@tester.org&savedb=Kontaktny_formular\' method=\'post\'>\r\n    <table cellspacing=\'0\' cellpadding=\'0\' border=\'0\'>\r\n        <tbody>\r\n            <tr>\r\n                <td>Vaše meno:</td>\r\n                <td><input class=\'required\' maxlength=\'255\' name=\'meno\'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> Vaša emailová adresa:</td>\r\n                <td><input class=\'required email\' maxlength=\'255\' name=\'email\'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td valign=\'top\'> Otázka / pripomienka:</td>\r\n                <td><textarea class=\'required\' name=\'otazka\' rows=\'5\' cols=\'40\'></textarea> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> </td>\r\n                <td align=\'right\'> <input ENGINE=\'submit\' name=\'btnSubmit\' value=\'Odoslať\'/></td>\r\n            </tr>\r\n        </tbody>\r\n    </table>\r\n</form>\r\n</p>\r\n<p> </p>','<p>pred pouzitim formularu prosim najskor nan kliknite pravym tlacitkom, dajte vlastnosti formularu a zmente emailovu adresu.</p>\r\n<p>\r\n<script src=\'/components/form/check_form.js\' ENGINE=\'text/javascript\'></script>\r\n<form name=\'formmailform\' action=\'/formmail.do?recipients=test@tester.org&savedb=kontaktny_formular\' method=\'post\'>\r\n    <table cellspacing=\'0\' cellpadding=\'0\' border=\'0\'>\r\n        <tbody>\r\n            <tr>\r\n                <td>vase meno: </td>\r\n                <td><input class=\'required\' maxlength=\'255\' name=\'meno\'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> vasa emailova adresa:</td>\r\n                <td><input class=\'required email\' maxlength=\'255\' name=\'email\'/> </td>\r\n            </tr>\r\n            <tr>\r\n                <td valign=\'top\'> otazka / pripomienka:</td>\r\n                <td><textarea class=\'required\' name=\'otazka\' rows=\'5\' cols=\'40\'></textarea> </td>\r\n            </tr>\r\n            <tr>\r\n                <td> </td>\r\n                <td align=\'right\'> <input ENGINE=\'submit\' name=\'btnsubmit\' value=\'odoslat\'/></td>\r\n            </tr>\r\n        </tbody>\r\n    </table>\r\n</form>\r\n</p>\r\n<p> </p>','','Kontaktný formulár','2003-10-05 22:43:25',NULL,NULL,1,21,1,0,0,1,1,0,'8_kontakt.html','2003-10-05 22:43:25',1,-1,-1,-1,NULL,'',NULL,'','',NULL,1,NULL,NULL,8,2,-1,-1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,-1,-1,-1,-1,NULL,NULL,NULL,NULL,NULL,0,0,NULL,0),
	(9,'Default left menu','!INCLUDE(/components/menu/menu_ul_li.jsp, rootGroupId=1, startOffset=1, maxLevel=3, classes=basic, openAllItems=true, rootUlId=mainNavigation)!','','','Default menu','2003-05-23 00:00:00',NULL,NULL,1,3,1,0,0,0,1,0,'9_default_menu.html','2003-08-27 10:36:59',50,-1,-1,-1,NULL,'','',NULL,NULL,NULL,1,NULL,NULL,9,2,-1,-1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,-1,-1,-1,-1,NULL,NULL,NULL,NULL,NULL,0,0,NULL,0);

/*!40000 ALTER TABLE `documents` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table documents_history
# ------------------------------------------------------------

DROP TABLE IF EXISTS `documents_history`;

CREATE TABLE `documents_history` (
  `history_id` int(11) NOT NULL AUTO_INCREMENT,
  `save_date` datetime DEFAULT NULL,
  `approved_by` int(11) NOT NULL DEFAULT -1,
  `awaiting_approve` varchar(255) DEFAULT NULL,
  `actual` tinyint(1) NOT NULL DEFAULT 0,
  `doc_id` int(11) NOT NULL DEFAULT 0,
  `title` varchar(255) NOT NULL DEFAULT '',
  `data` mediumtext NOT NULL,
  `data_asc` mediumtext NOT NULL,
  `external_link` varchar(255) DEFAULT NULL,
  `navbar` text NOT NULL,
  `date_created` datetime NOT NULL DEFAULT '2000-01-01 00:00:00',
  `publish_start` datetime DEFAULT NULL,
  `publish_end` datetime DEFAULT NULL,
  `author_id` int(11) NOT NULL DEFAULT 0,
  `group_id` int(11) DEFAULT NULL,
  `temp_id` int(11) NOT NULL DEFAULT 0,
  `views_total` int(11) NOT NULL DEFAULT 0,
  `views_month` int(11) NOT NULL DEFAULT 0,
  `searchable` tinyint(1) NOT NULL DEFAULT 0,
  `available` tinyint(1) NOT NULL DEFAULT 0,
  `cacheable` tinyint(1) NOT NULL DEFAULT 0,
  `file_name` varchar(255) DEFAULT NULL,
  `file_change` datetime DEFAULT NULL,
  `sort_priority` int(11) NOT NULL DEFAULT 0,
  `header_doc_id` int(11) DEFAULT NULL,
  `footer_doc_id` int(11) DEFAULT NULL,
  `menu_doc_id` int(11) NOT NULL DEFAULT -1,
  `password_protected` varchar(255) DEFAULT NULL,
  `html_head` text DEFAULT NULL,
  `html_data` text DEFAULT NULL,
  `publicable` tinyint(1) unsigned DEFAULT 0,
  `perex_place` varchar(255) DEFAULT NULL,
  `perex_image` varchar(255) DEFAULT NULL,
  `perex_group` varchar(255) DEFAULT NULL,
  `show_in_menu` tinyint(1) unsigned DEFAULT 1,
  `event_date` datetime DEFAULT NULL,
  `virtual_path` varchar(255) DEFAULT NULL,
  `sync_id` int(11) DEFAULT 0,
  `sync_status` int(11) DEFAULT 0,
  `logon_page_doc_id` int(4) DEFAULT -1,
  `right_menu_doc_id` int(4) DEFAULT -1,
  `field_a` varchar(255) DEFAULT NULL,
  `field_b` varchar(255) DEFAULT NULL,
  `field_c` varchar(255) DEFAULT NULL,
  `field_d` varchar(255) DEFAULT NULL,
  `field_e` varchar(255) DEFAULT NULL,
  `field_f` varchar(255) DEFAULT NULL,
  `field_g` varchar(255) DEFAULT NULL,
  `field_h` varchar(255) DEFAULT NULL,
  `field_i` varchar(255) DEFAULT NULL,
  `field_j` varchar(255) DEFAULT NULL,
  `field_k` varchar(255) DEFAULT NULL,
  `field_l` varchar(255) DEFAULT NULL,
  `disable_after_end` tinyint(1) DEFAULT 0,
  `approve_date` datetime DEFAULT NULL,
  `forum_count` int(11) DEFAULT 0,
  `field_m` varchar(255) DEFAULT NULL,
  `field_n` varchar(255) DEFAULT NULL,
  `field_o` varchar(255) DEFAULT NULL,
  `field_p` varchar(255) DEFAULT NULL,
  `field_q` varchar(255) DEFAULT NULL,
  `field_r` varchar(255) DEFAULT NULL,
  `field_s` varchar(255) DEFAULT NULL,
  `field_t` varchar(255) DEFAULT NULL,
  `require_ssl` tinyint(1) DEFAULT 0,
  `disapproved_by` int(11) DEFAULT NULL,
  `temp_field_a_docid` int(11) DEFAULT -1,
  `temp_field_b_docid` int(11) DEFAULT -1,
  `temp_field_c_docid` int(11) DEFAULT -1,
  `temp_field_d_docid` int(11) DEFAULT -1,
  `show_in_navbar` tinyint(1) DEFAULT NULL,
  `show_in_sitemap` tinyint(1) DEFAULT NULL,
  `logged_show_in_menu` tinyint(1) DEFAULT NULL,
  `logged_show_in_navbar` tinyint(1) DEFAULT NULL,
  `logged_show_in_sitemap` tinyint(1) DEFAULT NULL,
  `url_inherit_group` tinyint(1) DEFAULT 0,
  `generate_url_from_title` tinyint(1) DEFAULT 0,
  `editor_virtual_path` varchar(64) DEFAULT NULL,
  `publish_after_start` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`history_id`),
  KEY `IX_dh_docid` (`doc_id`),
  KEY `IX_dh_awaiting_approve` (`awaiting_approve`),
  KEY `IX_dh_author_id` (`author_id`),
  KEY `IX_documents_hist_publicable` (`publicable`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table domain_limits
# ------------------------------------------------------------

DROP TABLE IF EXISTS `domain_limits`;

CREATE TABLE `domain_limits` (
  `domain_limit_id` int(9) NOT NULL AUTO_INCREMENT,
  `domain` varchar(63) NOT NULL,
  `time_unit` varchar(20) NOT NULL,
  `limit_size` int(9) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `min_delay` int(11) NOT NULL,
  `delay_active` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`domain_limit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `domain_limits` WRITE;
/*!40000 ALTER TABLE `domain_limits` DISABLE KEYS */;

INSERT INTO `domain_limits` (`domain_limit_id`, `domain`, `time_unit`, `limit_size`, `active`, `min_delay`, `delay_active`)
VALUES
	(1,'*','MINUTES',10,1,1000,1);

/*!40000 ALTER TABLE `domain_limits` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table domain_redirects
# ------------------------------------------------------------

DROP TABLE IF EXISTS `domain_redirects`;

CREATE TABLE `domain_redirects` (
  `redirect_id` int(11) NOT NULL,
  `redirect_from` varchar(100) DEFAULT NULL,
  `redirect_to` varchar(512) DEFAULT NULL,
  `redirect_params` tinyint(1) NOT NULL DEFAULT 0,
  `redirect_path` tinyint(1) NOT NULL DEFAULT 0,
  `active` tinyint(1) NOT NULL DEFAULT 0,
  `http_protocol` varchar(15) DEFAULT NULL,
  PRIMARY KEY (`redirect_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table email_files
# ------------------------------------------------------------

DROP TABLE IF EXISTS `email_files`;

CREATE TABLE `email_files` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `subject` varchar(255) NOT NULL,
  `sender` varchar(255) NOT NULL,
  `sent_date` timestamp NOT NULL,
  `file_path` varchar(255) NOT NULL,
  `visible` int(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table emails
# ------------------------------------------------------------

DROP TABLE IF EXISTS `emails`;

CREATE TABLE `emails` (
  `email_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `recipient_email` varchar(128) NOT NULL DEFAULT '0',
  `recipient_name` varchar(128) DEFAULT NULL,
  `sender_name` varchar(128) DEFAULT '0',
  `sender_email` varchar(128) NOT NULL DEFAULT '0',
  `subject` varchar(255) DEFAULT NULL,
  `url` varchar(255) NOT NULL DEFAULT '',
  `attachments` text DEFAULT NULL,
  `retry` int(4) NOT NULL DEFAULT 0,
  `sent_date` datetime DEFAULT NULL,
  `created_by_user_id` int(4) NOT NULL DEFAULT 0,
  `create_date` datetime DEFAULT NULL,
  `send_at` datetime DEFAULT NULL,
  `message` text DEFAULT NULL,
  `reply_to` varchar(255) DEFAULT NULL,
  `cc_email` varchar(255) DEFAULT NULL,
  `bcc_email` varchar(255) DEFAULT NULL,
  `disabled` tinyint(1) unsigned DEFAULT 0,
  `campain_id` int(11) DEFAULT NULL,
  `recipient_user_id` int(11) DEFAULT -1,
  `seen_date` datetime DEFAULT NULL,
  `click_hash` varchar(64) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`email_id`),
  KEY `i_campain_id` (`campain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table emails_campain
# ------------------------------------------------------------

DROP TABLE IF EXISTS `emails_campain`;

CREATE TABLE `emails_campain` (
  `emails_campain_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sender_name` varchar(255) DEFAULT NULL,
  `sender_email` varchar(255) DEFAULT NULL,
  `subject` varchar(255) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `created_by_user_id` int(11) DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `count_of_recipients` int(11) DEFAULT NULL,
  `count_of_sent_mails` int(11) NOT NULL DEFAULT 0,
  `last_sent_date` datetime DEFAULT NULL,
  `user_groups` varchar(64) DEFAULT NULL,
  `send_at` datetime DEFAULT NULL,
  `attachments` text DEFAULT NULL,
  `reply_to` varchar(255) DEFAULT NULL,
  `cc_email` varchar(255) DEFAULT NULL,
  `bcc_email` varchar(255) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`emails_campain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table emails_stat_click
# ------------------------------------------------------------

DROP TABLE IF EXISTS `emails_stat_click`;

CREATE TABLE `emails_stat_click` (
  `click_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `email_id` int(10) unsigned NOT NULL,
  `link` varchar(255) NOT NULL,
  `click_date` datetime NOT NULL,
  `session_id` bigint(20) unsigned DEFAULT 0,
  `browser_id` bigint(20) unsigned DEFAULT 0,
  PRIMARY KEY (`click_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table emails_unsubscribed
# ------------------------------------------------------------

DROP TABLE IF EXISTS `emails_unsubscribed`;

CREATE TABLE `emails_unsubscribed` (
  `emails_unsubscribed_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `email` varchar(128) DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`emails_unsubscribed_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table enumeration_data
# ------------------------------------------------------------

DROP TABLE IF EXISTS `enumeration_data`;

CREATE TABLE `enumeration_data` (
  `enumeration_data_id` int(11) NOT NULL,
  `enumeration_type_id` int(11) NOT NULL,
  `child_enumeration_type_id` int(11) DEFAULT NULL,
  `string1` varchar(1024) DEFAULT NULL,
  `string2` varchar(1024) DEFAULT NULL,
  `string3` varchar(1024) DEFAULT NULL,
  `string4` varchar(1024) DEFAULT NULL,
  `decimal1` decimal(10,4) DEFAULT NULL,
  `decimal2` decimal(10,4) DEFAULT NULL,
  `decimal3` decimal(10,4) DEFAULT NULL,
  `decimal4` decimal(10,4) DEFAULT NULL,
  `boolean1` tinyint(1) NOT NULL DEFAULT 0,
  `boolean2` tinyint(1) NOT NULL DEFAULT 0,
  `boolean3` tinyint(1) NOT NULL DEFAULT 0,
  `boolean4` tinyint(1) NOT NULL DEFAULT 0,
  `sort_priority` int(11) DEFAULT NULL,
  `hidden` tinyint(1) NOT NULL DEFAULT 0,
  `DTYPE` varchar(31) DEFAULT 'default',
  `string5` varchar(1024) DEFAULT NULL,
  `string6` varchar(1024) DEFAULT NULL,
  `date1` datetime DEFAULT NULL,
  `date2` datetime DEFAULT NULL,
  `date3` datetime DEFAULT NULL,
  `date4` datetime DEFAULT NULL,
  `parent_enumeration_data_id` int(11) DEFAULT NULL,
  `string7` varchar(1024) DEFAULT NULL,
  `string8` varchar(1024) DEFAULT NULL,
  `string9` varchar(1024) DEFAULT NULL,
  `string10` varchar(1024) DEFAULT NULL,
  `string11` varchar(1024) DEFAULT NULL,
  `string12` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`enumeration_data_id`),
  KEY `enumeration_type_id_fk` (`enumeration_type_id`),
  KEY `enumeration_data_enumeration_type_enumeration_type_id_fk_2` (`child_enumeration_type_id`),
  KEY `enumeration_data_enumeration_data_enumeration_data_id_fk` (`parent_enumeration_data_id`),
  CONSTRAINT `enumeration_data_enumeration_data_enumeration_data_id_fk` FOREIGN KEY (`parent_enumeration_data_id`) REFERENCES `enumeration_data` (`enumeration_data_id`),
  CONSTRAINT `enumeration_data_enumeration_type_enumeration_type_id_fk_2` FOREIGN KEY (`child_enumeration_type_id`) REFERENCES `enumeration_type` (`enumeration_type_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `enumeration_type_id_fk` FOREIGN KEY (`enumeration_type_id`) REFERENCES `enumeration_type` (`enumeration_type_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;



# Dump of table enumeration_type
# ------------------------------------------------------------

DROP TABLE IF EXISTS `enumeration_type`;

CREATE TABLE `enumeration_type` (
  `enumeration_type_id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `string1_name` varchar(255) DEFAULT NULL,
  `string2_name` varchar(255) DEFAULT NULL,
  `string3_name` varchar(255) DEFAULT NULL,
  `string4_name` varchar(255) DEFAULT NULL,
  `string5_name` varchar(255) DEFAULT NULL,
  `string6_name` varchar(255) DEFAULT NULL,
  `string7_name` varchar(255) DEFAULT NULL,
  `string8_name` varchar(255) DEFAULT NULL,
  `string9_name` varchar(255) DEFAULT NULL,
  `string10_name` varchar(255) DEFAULT NULL,
  `string11_name` varchar(255) DEFAULT NULL,
  `string12_name` varchar(255) DEFAULT NULL,
  `decimal1_name` varchar(255) DEFAULT NULL,
  `decimal2_name` varchar(255) DEFAULT NULL,
  `decimal3_name` varchar(255) DEFAULT NULL,
  `decimal4_name` varchar(255) DEFAULT NULL,
  `boolean1_name` varchar(255) DEFAULT NULL,
  `boolean2_name` varchar(255) DEFAULT NULL,
  `boolean3_name` varchar(255) DEFAULT NULL,
  `boolean4_name` varchar(255) DEFAULT NULL,
  `hidden` tinyint(1) DEFAULT NULL,
  `child_enumeration_type_id` int(11) DEFAULT NULL,
  `DTYPE` varchar(31) DEFAULT 'default',
  `allow_child_enumeration_type` tinyint(1) NOT NULL DEFAULT 0,
  `date1_name` varchar(255) DEFAULT NULL,
  `date2_name` varchar(255) DEFAULT NULL,
  `date3_name` varchar(255) DEFAULT NULL,
  `date4_name` varchar(255) DEFAULT NULL,
  `allow_parent_enumeration_data` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`enumeration_type_id`),
  KEY `enumeration_type_enumeration_type_enumeration_type_id_fk` (`child_enumeration_type_id`),
  CONSTRAINT `enumeration_type_enumeration_type_enumeration_type_id_fk` FOREIGN KEY (`child_enumeration_type_id`) REFERENCES `enumeration_type` (`enumeration_type_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;



# Dump of table export_dat
# ------------------------------------------------------------

DROP TABLE IF EXISTS `export_dat`;

CREATE TABLE `export_dat` (
  `export_dat_id` int(11) NOT NULL,
  `url_address` varchar(255) NOT NULL,
  `format` varchar(32) NOT NULL,
  `number_items` int(11) DEFAULT NULL,
  `group_ids` varchar(255) DEFAULT NULL,
  `expand_group_ids` bit(1) NOT NULL DEFAULT b'1',
  `perex_group` varchar(255) DEFAULT NULL,
  `order_type` varchar(255) NOT NULL,
  `asc_order` bit(1) NOT NULL DEFAULT b'0',
  `publish_type` varchar(255) NOT NULL,
  `no_perex_check` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`export_dat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_slovak_ci;



# Dump of table file_archiv
# ------------------------------------------------------------

DROP TABLE IF EXISTS `file_archiv`;

CREATE TABLE `file_archiv` (
  `file_archiv_id` int(11) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `file_path` varchar(255) DEFAULT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `virtual_file_name` varchar(255) DEFAULT NULL,
  `show_file` tinyint(1) NOT NULL,
  `date_insert` datetime DEFAULT NULL,
  `valid_from` datetime DEFAULT NULL,
  `valid_to` datetime DEFAULT NULL,
  `domain` varchar(255) DEFAULT NULL,
  `reference_id` int(11) DEFAULT NULL,
  `order_id` int(11) DEFAULT NULL,
  `product_code` varchar(255) DEFAULT NULL,
  `product` varchar(255) DEFAULT NULL,
  `category` varchar(255) DEFAULT NULL,
  `md5` varchar(50) DEFAULT NULL,
  `reference_to_main` varchar(255) DEFAULT NULL,
  `priority` varchar(255) DEFAULT NULL,
  `field_a` varchar(255) DEFAULT NULL,
  `field_b` varchar(255) DEFAULT NULL,
  `field_c` int(11) DEFAULT NULL,
  `field_d` bigint(20) DEFAULT NULL,
  `field_e` varchar(255) DEFAULT NULL,
  `global_id` int(11) DEFAULT NULL,
  `note` varchar(1100) DEFAULT NULL,
  `date_upload_later` date DEFAULT NULL,
  `uploaded` int(2) DEFAULT -1,
  `emails` varchar(1100) DEFAULT NULL,
  `file_size` int(11) DEFAULT NULL,
  `domain_id` int(11) DEFAULT 1,
  `extended_data_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`file_archiv_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table file_archiv_category_node
# ------------------------------------------------------------

DROP TABLE IF EXISTS `file_archiv_category_node`;

CREATE TABLE `file_archiv_category_node` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `lft` int(11) DEFAULT NULL,
  `rgt` int(11) DEFAULT NULL,
  `lvl` int(11) DEFAULT NULL,
  `rootid` int(11) DEFAULT NULL,
  `category_name` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `date_insert` datetime DEFAULT NULL,
  `string1Name` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `string2Name` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `string3Name` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `int1Val` int(11) DEFAULT NULL,
  `int2Val` int(11) DEFAULT NULL,
  `int3Val` int(11) DEFAULT NULL,
  `category_type` int(11) DEFAULT NULL,
  `sort_priority` int(11) DEFAULT NULL,
  `is_show` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table file_atr
# ------------------------------------------------------------

DROP TABLE IF EXISTS `file_atr`;

CREATE TABLE `file_atr` (
  `file_name` varchar(128) NOT NULL DEFAULT '',
  `link` varchar(255) NOT NULL DEFAULT '',
  `atr_id` int(4) unsigned NOT NULL DEFAULT 0,
  `value_string` varchar(255) DEFAULT NULL,
  `value_int` int(4) unsigned DEFAULT NULL,
  `value_bool` tinyint(1) unsigned DEFAULT 0,
  PRIMARY KEY (`link`,`atr_id`),
  KEY `link` (`link`,`atr_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table file_atr_def
# ------------------------------------------------------------

DROP TABLE IF EXISTS `file_atr_def`;

CREATE TABLE `file_atr_def` (
  `atr_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `atr_name` varchar(32) NOT NULL DEFAULT '',
  `order_priority` int(4) DEFAULT 10,
  `atr_description` varchar(255) DEFAULT NULL,
  `atr_default_value` varchar(255) DEFAULT NULL,
  `atr_type` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `atr_group` varchar(32) DEFAULT 'default',
  `true_value` varchar(255) DEFAULT NULL,
  `false_value` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`atr_id`),
  KEY `atr_id` (`atr_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table file_history
# ------------------------------------------------------------

DROP TABLE IF EXISTS `file_history`;

CREATE TABLE `file_history` (
  `file_history_id` int(11) NOT NULL,
  `file_url` varchar(255) DEFAULT NULL,
  `change_date` datetime DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `deleted` tinyint(1) DEFAULT 0,
  `history_path` varchar(255) DEFAULT NULL,
  `ip_address` varchar(32) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`file_history_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table form_attributes
# ------------------------------------------------------------

DROP TABLE IF EXISTS `form_attributes`;

CREATE TABLE `form_attributes` (
  `value` varchar(1024) DEFAULT NULL,
  `form_name` varchar(255) NOT NULL,
  `param_name` varchar(64) NOT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`form_name`,`param_name`,`domain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table form_regular_exp
# ------------------------------------------------------------

DROP TABLE IF EXISTS `form_regular_exp`;

CREATE TABLE `form_regular_exp` (
  `title` varchar(255) NOT NULL,
  `type` varchar(32) NOT NULL,
  `reg_exp` varchar(255) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `form_regular_exp` WRITE;
/*!40000 ALTER TABLE `form_regular_exp` DISABLE KEYS */;

INSERT INTO `form_regular_exp` (`title`, `type`, `reg_exp`, `id`)
VALUES
	('checkform.title.allPhone','phone','^([+]?[s0-9]+)?(d{3}|[(]?[0-9]+[)])?([-]?[s]?[0-9])+$',1),
	('checkform.title.alphabet','alphabet','^[abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZľěščťřžýáíéúůňťóďäôöüĽĚŠČŤŘŽÝÁÍÉÚŮŇŤÓĎÄÔÖÜ ]{1,}$',2),
	('checkform.title.alphabetLowercase','alphabetLowercase','^[abcdefghijklmnopqrstuvwxyzľěščťřžýáíéúůňťóďäô ]{1,}$',3),
	('checkform.title.alphabetUppercase','alphabetUppercase','^[ABCDEFGHIJKLMNOPQRSTUVWXYZĽĚŠČŤŘŽÝÁÍÉÚŮŇŤÓĎÄÔ ]{1,}$',4),
	('checkform.title.alphanumeric','alphanumeric','^[abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ľěščťřžýáíéúůňťóďäôĽĚŠČŤŘŽÝÁÍÉÚŮŇŤÓĎÄÔ ]{1,}$',5),
	('checkform.title.date','date','^(([0]?[123456789])|([12][0-9])|(3[01]))[.]((0?[123456789])|(1[012]))[.][0-9]{4}$',6),
	('checkform.title.datetime','datetime','^[0-9]{1,2}[.][0-9]{1,2}[.][0-9]{4}.[0-9]{1,2}[:][0-9]{1,2}[:][0-9]{1,2}$',7),
	('checkform.title.email','email','^[a-zA-Z0-9]+[a-zA-Z0-9\\._+=#$%&-]*[a-zA-Z0-9]+@[a-zA-Z0-9]+[a-zA-Z0-9\\._-]*[a-zA-Z0-9]+\\.[a-zA-Z]{2,16}$',8),
	('checkform.title.email2','email2','^[a-z0-9]+[a-z0-9\\._-]*[a-z0-9]+@[a-z0-9]+[a-z0-9\\._-]*[a-z0-9]+\\.[a-z]{2,4}$',9),
	('checkform.title.fixedPhone','fixedPhone','(^[0-9]{2,3}[\\/][0-9]{3,12}$)|(^[+][0-9]{1,3}[\\/][0-9]{1,2}[\\/][0-9]{3,12}$)',10),
	('checkform.title.integer','integer','^[-]?[0-9]+$',11),
	('checkform.title.loginChars','loginChars','^[0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_.]{1,}$',12),
	('checkform.title.minLen16','minLen16','.................*',13),
	('checkform.title.minLen2','minLen2','...*',14),
	('checkform.title.minLen3','minLen3','....*',15),
	('checkform.title.minLen4','minLen4','.....*',16),
	('checkform.title.minLen5','minLen5','......*',17),
	('checkform.title.minLen6','minLen6','.......*',18),
	('checkform.title.minLen8','minLen8','.........*',19),
	('checkform.title.mobilePhone','mobilePhone','(^[0-9]{4}[\\/][0-9]{6}$)|(^[+][0-9]{1,3}[\\/][0-9]{3}[\\/][0-9]{6}$)',20),
	('checkform.title.numbers','numbers','^[-]?[0-9]+([,.][0-9]+)?$',21),
	('checkform.title.postalCode','postalCode','^(([0-9]{5})|([0-9]{3} [0-9]{2})|([0-9]{2} [0-9]{3}))$',22),
	('checkform.title.safeChars','safeChars','^[0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_ ]{1,}$',23),
	('checkform.title.safeChars2','safeChars2','^[0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_\\\\-]{1,}$',24),
	('checkform.title.time','time','^[0-9]{1,2}[:][0-9]{1,2}$',25),
	('checkform.title.trim','trim','^[^ \\f\\n\\r\\t\\v]+[.]*',26),
	('checkform.title.url','url','^http[s]?:\\/\\/[a-zA-Z0-9]+([-_\\.]?[a-zA-Z0-9])*\\.[a-zA-Z]{2,4}([:0-9]*)(\\/{1}[%-_~&=\\?\\.:a-z0-9]*)*$',27);

/*!40000 ALTER TABLE `form_regular_exp` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table forms
# ------------------------------------------------------------

DROP TABLE IF EXISTS `forms`;

CREATE TABLE `forms` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `form_name` varchar(255) NOT NULL DEFAULT '',
  `data` text NOT NULL,
  `files` text DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `html` mediumtext DEFAULT NULL,
  `user_id` int(4) NOT NULL DEFAULT -1,
  `note` text DEFAULT NULL,
  `doc_id` int(4) NOT NULL DEFAULT -1,
  `last_export_date` datetime DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  `double_optin_confirmation_date` datetime DEFAULT NULL,
  `double_optin_hash` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `id` (`id`),
  KEY `IX_forms_formname` (`form_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='formulare';



# Dump of table forms_archive
# ------------------------------------------------------------

DROP TABLE IF EXISTS `forms_archive`;

CREATE TABLE `forms_archive` (
  `id` int(11) unsigned NOT NULL,
  `form_name` varchar(255) NOT NULL DEFAULT '''',
  `data` text NOT NULL,
  `files` text DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `html` mediumtext DEFAULT NULL,
  `user_id` int(4) NOT NULL DEFAULT -1,
  `note` text DEFAULT NULL,
  `doc_id` int(4) NOT NULL DEFAULT -1,
  `last_export_date` datetime DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  `double_optin_confirmation_date` datetime DEFAULT NULL,
  `double_optin_hash` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `id` (`id`),
  KEY `IX_forms_archive_formname` (`form_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table forum
# ------------------------------------------------------------

DROP TABLE IF EXISTS `forum`;

CREATE TABLE `forum` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `doc_id` int(10) unsigned DEFAULT NULL,
  `active` tinyint(1) unsigned NOT NULL,
  `date_from` datetime DEFAULT NULL,
  `date_to` datetime DEFAULT NULL,
  `hours_after_last_message` int(11) DEFAULT NULL,
  `message_confirmation` tinyint(1) NOT NULL DEFAULT 0,
  `approve_email` varchar(255) DEFAULT NULL,
  `notif_email` varchar(255) DEFAULT NULL,
  `message_board` tinyint(1) unsigned NOT NULL DEFAULT 0,
  `advertisement_type` tinyint(1) unsigned DEFAULT NULL,
  `admin_groups` varchar(32) DEFAULT NULL,
  `addmessage_groups` varchar(128) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  `notify_page_author` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table gallery
# ------------------------------------------------------------

DROP TABLE IF EXISTS `gallery`;

CREATE TABLE `gallery` (
  `image_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `image_path` varchar(255) DEFAULT NULL,
  `s_description_sk` varchar(255) DEFAULT NULL,
  `l_description_sk` text DEFAULT NULL,
  `image_name` varchar(255) DEFAULT NULL,
  `s_description_en` varchar(255) DEFAULT NULL,
  `l_description_en` text DEFAULT NULL,
  `s_description_cz` varchar(255) DEFAULT NULL,
  `l_description_cz` text DEFAULT NULL,
  `s_description_de` varchar(255) DEFAULT NULL,
  `l_description_de` text DEFAULT NULL,
  `l_description_pl` text DEFAULT NULL,
  `s_description_pl` varchar(255) DEFAULT '',
  `l_description_ru` text DEFAULT NULL,
  `s_description_ru` varchar(255) DEFAULT '',
  `l_description_hu` text DEFAULT NULL,
  `s_description_hu` varchar(255) DEFAULT '',
  `l_description_cho` text DEFAULT NULL,
  `s_description_cho` varchar(255) DEFAULT '',
  `l_description_esp` text DEFAULT NULL,
  `s_description_esp` varchar(255) DEFAULT '',
  `author` text DEFAULT NULL,
  `send_count` int(11) DEFAULT 0,
  `allowed_domains` varchar(255) DEFAULT NULL,
  `perex_group` varchar(255) DEFAULT NULL,
  `selected_x` int(11) DEFAULT NULL,
  `selected_y` int(11) DEFAULT NULL,
  `selected_width` int(11) DEFAULT NULL,
  `selected_height` int(11) DEFAULT NULL,
  `upload_datetime` datetime DEFAULT NULL,
  `sort_priority` int(11) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`image_id`),
  KEY `i_image_path` (`image_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table gallery_dimension
# ------------------------------------------------------------

DROP TABLE IF EXISTS `gallery_dimension`;

CREATE TABLE `gallery_dimension` (
  `dimension_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `image_path` varchar(255) DEFAULT '0',
  `image_width` int(4) unsigned DEFAULT 0,
  `image_height` int(4) unsigned DEFAULT 0,
  `normal_width` int(4) unsigned NOT NULL DEFAULT 0,
  `normal_height` int(4) unsigned NOT NULL DEFAULT 0,
  `resize_mode` varchar(1) DEFAULT NULL,
  `gallery_name` varchar(255) DEFAULT NULL,
  `gallery_perex` text DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `author` varchar(255) DEFAULT NULL,
  `views` int(11) NOT NULL DEFAULT 0,
  `watermark_saturation` int(11) DEFAULT NULL,
  `watermark` varchar(255) DEFAULT NULL,
  `watermark_placement` varchar(255) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`dimension_id`),
  KEY `i_image_path` (`image_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `gallery_dimension` WRITE;
/*!40000 ALTER TABLE `gallery_dimension` DISABLE KEYS */;

INSERT INTO `gallery_dimension` (`dimension_id`, `image_path`, `image_width`, `image_height`, `normal_width`, `normal_height`, `resize_mode`, `gallery_name`, `gallery_perex`, `create_date`, `author`, `views`, `watermark_saturation`, `watermark`, `watermark_placement`, `domain_id`)
VALUES
	(5,'/images/gallery',160,120,750,560,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,1);

/*!40000 ALTER TABLE `gallery_dimension` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table gdpr_regexp
# ------------------------------------------------------------

DROP TABLE IF EXISTS `gdpr_regexp`;

CREATE TABLE `gdpr_regexp` (
  `gdpr_regexp_id` int(11) NOT NULL,
  `regexp_name` varchar(255) DEFAULT NULL,
  `regexp_value` varchar(1024) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `date_insert` datetime DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`gdpr_regexp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `gdpr_regexp` WRITE;
/*!40000 ALTER TABLE `gdpr_regexp` DISABLE KEYS */;

INSERT INTO `gdpr_regexp` (`gdpr_regexp_id`, `regexp_name`, `regexp_value`, `user_id`, `date_insert`, `domain_id`)
VALUES
	(1,'rodné číslo','[0-9]{6}/[0-9]{3,4}',-1,NULL,1),
	(2,'telefónne číslo 10 samostatných čísel','[0-9]{10}',-1,NULL,1),
	(3,'telefónne číslo iné formáty','(\\+[0-9]{3}) ?[0-9]{3,4} ?[0-9]{3} ?[0-9]{3}',-1,NULL,1),
	(4,'rok mesiac deň oddelené znakmi: ./-','(19|20)dd[- /.](0[1-9]|1[012]|[0-9])[- /.](0[1-9]|[12][0-9]|3[01]|[0-9])',-1,NULL,1),
	(5,'deň mesiac rok oddelené znakmi: ./-','(0[1-9]|[12][0-9]|3[01]|[0-9])[- /.](0[1-9]|1[012]|[0-9])[- /.](19|20)dd',-1,NULL,1);

/*!40000 ALTER TABLE `gdpr_regexp` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table gis
# ------------------------------------------------------------

DROP TABLE IF EXISTS `gis`;

CREATE TABLE `gis` (
  `gis_id` int(11) NOT NULL,
  `cathegory` varchar(128) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `gps_lat` double(9,6) DEFAULT NULL,
  `gps_lon` double(9,6) DEFAULT NULL,
  `detail_url` varchar(2000) DEFAULT NULL,
  `image_url` varchar(2000) DEFAULT NULL,
  `pin_url` varchar(255) DEFAULT NULL,
  `approved` tinyint(1) DEFAULT NULL,
  `approve_hash` varchar(255) DEFAULT NULL,
  `info_a` varchar(255) DEFAULT NULL,
  `info_b` varchar(255) DEFAULT NULL,
  `info_c` varchar(255) DEFAULT NULL,
  `info_d` varchar(255) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`gis_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table groups
# ------------------------------------------------------------

DROP TABLE IF EXISTS `groups`;

CREATE TABLE `groups` (
  `group_id` int(11) NOT NULL AUTO_INCREMENT,
  `group_name` varchar(255) NOT NULL,
  `internal` tinyint(1) NOT NULL DEFAULT 0,
  `parent_group_id` int(11) NOT NULL DEFAULT 0,
  `navbar` varchar(255) DEFAULT NULL,
  `default_doc_id` int(11) DEFAULT NULL,
  `temp_id` int(11) DEFAULT NULL,
  `sort_priority` int(11) NOT NULL DEFAULT 0,
  `password_protected` varchar(255) DEFAULT NULL,
  `menu_type` tinyint(1) unsigned DEFAULT 1,
  `url_dir_name` varchar(255) DEFAULT NULL,
  `sync_id` int(11) DEFAULT 0,
  `sync_status` int(11) DEFAULT 0,
  `html_head` text DEFAULT NULL,
  `logon_page_doc_id` int(4) DEFAULT -1,
  `domain_name` varchar(255) DEFAULT NULL,
  `new_page_docid_template` int(11) DEFAULT 0,
  `install_name` varchar(255) DEFAULT NULL,
  `field_a` varchar(255) DEFAULT NULL,
  `field_b` varchar(255) DEFAULT NULL,
  `field_c` varchar(255) DEFAULT NULL,
  `field_d` varchar(255) DEFAULT NULL,
  `logged_menu_type` tinyint(1) DEFAULT -1,
  `link_group_id` int(11) DEFAULT -1,
  `lng` varchar(8) DEFAULT NULL,
  `hidden_in_admin` tinyint(4) DEFAULT NULL,
  `force_group_template` tinyint(4) DEFAULT 0,
  `show_in_navbar` tinyint(1) DEFAULT NULL,
  `show_in_sitemap` tinyint(1) DEFAULT NULL,
  `logged_show_in_navbar` tinyint(1) DEFAULT NULL,
  `logged_show_in_sitemap` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `groups` WRITE;
/*!40000 ALTER TABLE `groups` DISABLE KEYS */;

INSERT INTO `groups` (`group_id`, `group_name`, `internal`, `parent_group_id`, `navbar`, `default_doc_id`, `temp_id`, `sort_priority`, `password_protected`, `menu_type`, `url_dir_name`, `sync_id`, `sync_status`, `html_head`, `logon_page_doc_id`, `domain_name`, `new_page_docid_template`, `install_name`, `field_a`, `field_b`, `field_c`, `field_d`, `logged_menu_type`, `link_group_id`, `lng`, `hidden_in_admin`, `force_group_template`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_navbar`, `logged_show_in_sitemap`)
VALUES
	(1,'Slovensky',0,0,'Slovensky',4,1,0,NULL,2,NULL,1,2,NULL,-1,NULL,0,NULL,NULL,NULL,NULL,NULL,-1,-1,NULL,NULL,0,NULL,NULL,NULL,NULL),
	(2,'Šablóny',1,20,'Šablóny',4,1,1500,NULL,2,NULL,2,2,NULL,-1,NULL,0,NULL,NULL,NULL,NULL,NULL,-1,-1,NULL,NULL,0,NULL,NULL,NULL,NULL),
	(3,'Menu',1,20,'Menu',4,1,1501,NULL,2,NULL,3,2,NULL,-1,NULL,0,NULL,NULL,NULL,NULL,NULL,-1,-1,NULL,NULL,0,NULL,NULL,NULL,NULL),
	(4,'Hlavičky-pätičky',1,20,'Hlavičky-pätičky',4,2,1502,NULL,1,NULL,4,2,NULL,-1,NULL,0,NULL,NULL,NULL,NULL,NULL,-1,-1,NULL,NULL,0,NULL,NULL,NULL,NULL),
	(5,'English',0,0,'English',0,1,500,NULL,2,NULL,5,2,NULL,-1,NULL,0,NULL,NULL,NULL,NULL,NULL,-1,-1,NULL,NULL,0,NULL,NULL,NULL,NULL),
	(20,'System',1,0,'System',4,1,1000,NULL,2,NULL,20,2,NULL,-1,NULL,0,NULL,NULL,NULL,NULL,NULL,-1,-1,NULL,NULL,0,NULL,NULL,NULL,NULL),
	(21,'HTMLBox',1,20,'HTMLBox',4,1,1503,NULL,2,NULL,21,2,NULL,-1,NULL,0,NULL,NULL,NULL,NULL,NULL,-1,-1,NULL,NULL,0,NULL,NULL,NULL,NULL);

/*!40000 ALTER TABLE `groups` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table groups_approve
# ------------------------------------------------------------

DROP TABLE IF EXISTS `groups_approve`;

CREATE TABLE `groups_approve` (
  `approve_id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `group_id` int(11) unsigned DEFAULT 0,
  `user_id` int(11) unsigned DEFAULT 0,
  `approve_mode` int(4) unsigned NOT NULL,
  PRIMARY KEY (`approve_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table groups_scheduler
# ------------------------------------------------------------

DROP TABLE IF EXISTS `groups_scheduler`;

CREATE TABLE `groups_scheduler` (
  `schedule_id` int(11) NOT NULL AUTO_INCREMENT,
  `group_id` int(11) NOT NULL,
  `group_name` varchar(255) NOT NULL,
  `internal` tinyint(1) NOT NULL DEFAULT 0,
  `parent_group_id` int(11) NOT NULL DEFAULT 0,
  `navbar` varchar(255) DEFAULT NULL,
  `default_doc_id` int(11) DEFAULT NULL,
  `temp_id` int(11) DEFAULT NULL,
  `sort_priority` int(11) NOT NULL DEFAULT 0,
  `password_protected` varchar(255) DEFAULT NULL,
  `menu_type` tinyint(1) unsigned DEFAULT 1,
  `url_dir_name` varchar(255) DEFAULT NULL,
  `sync_id` int(11) DEFAULT 0,
  `sync_status` int(11) DEFAULT 0,
  `html_head` text DEFAULT NULL,
  `logon_page_doc_id` int(4) DEFAULT -1,
  `domain_name` varchar(255) DEFAULT NULL,
  `new_page_docid_template` int(11) DEFAULT 0,
  `install_name` varchar(255) DEFAULT NULL,
  `field_a` varchar(255) DEFAULT NULL,
  `field_b` varchar(255) DEFAULT NULL,
  `field_c` varchar(255) DEFAULT NULL,
  `field_d` varchar(255) DEFAULT NULL,
  `logged_menu_type` tinyint(1) DEFAULT -1,
  `link_group_id` int(11) DEFAULT -1,
  `when_to_publish` timestamp NULL DEFAULT NULL,
  `save_date` datetime DEFAULT NULL,
  `user_id` int(11) DEFAULT -1,
  `lng` varchar(8) DEFAULT NULL,
  `hidden_in_admin` tinyint(4) DEFAULT NULL,
  `force_group_template` tinyint(4) DEFAULT 0,
  `date_published` datetime DEFAULT NULL,
  `show_in_navbar` tinyint(1) DEFAULT NULL,
  `show_in_sitemap` tinyint(1) DEFAULT NULL,
  `logged_show_in_navbar` tinyint(1) DEFAULT NULL,
  `logged_show_in_sitemap` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table chat_rooms
# ------------------------------------------------------------

DROP TABLE IF EXISTS `chat_rooms`;

CREATE TABLE `chat_rooms` (
  `room_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `room_name` varchar(255) NOT NULL,
  `room_description` text DEFAULT NULL,
  `room_class` varchar(255) NOT NULL,
  `max_users` int(4) unsigned NOT NULL DEFAULT 50,
  `allow_similar_names` tinyint(1) unsigned NOT NULL DEFAULT 0,
  `lng` varchar(3) NOT NULL DEFAULT 'sk',
  `moderator_name` varchar(128) DEFAULT NULL,
  `moderator_username` varchar(64) DEFAULT NULL,
  `moderator_password` varchar(64) DEFAULT NULL,
  `hide_in_public_list` tinyint(1) unsigned NOT NULL DEFAULT 0,
  KEY `room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='zoznam izieb pre modul chat';

LOCK TABLES `chat_rooms` WRITE;
/*!40000 ALTER TABLE `chat_rooms` DISABLE KEYS */;

INSERT INTO `chat_rooms` (`room_id`, `room_name`, `room_description`, `room_class`, `max_users`, `allow_similar_names`, `lng`, `moderator_name`, `moderator_username`, `moderator_password`, `hide_in_public_list`)
VALUES
	(1,'pokec','Volny pokec','sk.iway.iwcm.components.chat.HtmlChatRoom',50,0,'sk',NULL,NULL,NULL,0),
	(2,'moderovany pokec','Moderovany pokec','sk.iway.iwcm.components.chat.ModeratedRoom',50,0,'sk','Moderator','moderator','heslo',0);

/*!40000 ALTER TABLE `chat_rooms` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table inquiry
# ------------------------------------------------------------

DROP TABLE IF EXISTS `inquiry`;

CREATE TABLE `inquiry` (
  `question_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `question_text` text DEFAULT NULL,
  `hours` int(4) unsigned DEFAULT 0,
  `question_group` varchar(255) DEFAULT NULL,
  `answer_text_ok` tinytext DEFAULT NULL,
  `answer_text_fail` tinytext DEFAULT NULL,
  `date_from` datetime DEFAULT NULL,
  `date_to` datetime DEFAULT NULL,
  `question_active` int(1) DEFAULT 1,
  `multiple` tinyint(1) DEFAULT NULL,
  `total_clicks` int(11) DEFAULT NULL,
  `domain_id` int(6) DEFAULT 1,
  UNIQUE KEY `question_id` (`question_id`),
  KEY `question_id_2` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `inquiry` WRITE;
/*!40000 ALTER TABLE `inquiry` DISABLE KEYS */;

INSERT INTO `inquiry` (`question_id`, `question_text`, `hours`, `question_group`, `answer_text_ok`, `answer_text_fail`, `date_from`, `date_to`, `question_active`, `multiple`, `total_clicks`, `domain_id`)
VALUES
	(1,'Ako sa vám páči WebJET',24,'default','Ďakujeme, že ste sa zúčastnili ankety.','Ľutujeme, ale tejto ankety ste sa už zúčastnili.',NULL,NULL,1,NULL,NULL,1);

/*!40000 ALTER TABLE `inquiry` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table inquiry_answers
# ------------------------------------------------------------

DROP TABLE IF EXISTS `inquiry_answers`;

CREATE TABLE `inquiry_answers` (
  `answer_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `question_id` int(11) NOT NULL DEFAULT 0,
  `answer_text` varchar(255) DEFAULT NULL,
  `answer_clicks` int(4) unsigned DEFAULT 0,
  `image_path` varchar(255) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `domain_id` int(6) DEFAULT 1,
  PRIMARY KEY (`answer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `inquiry_answers` WRITE;
/*!40000 ALTER TABLE `inquiry_answers` DISABLE KEYS */;

INSERT INTO `inquiry_answers` (`answer_id`, `question_id`, `answer_text`, `answer_clicks`, `image_path`, `url`, `domain_id`)
VALUES
	(1,1,'Je super',8,NULL,NULL,1),
	(2,1,'Neviem, nepoznám',3,NULL,NULL,1);

/*!40000 ALTER TABLE `inquiry_answers` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table inquiry_users
# ------------------------------------------------------------

DROP TABLE IF EXISTS `inquiry_users`;

CREATE TABLE `inquiry_users` (
  `user_id` int(11) NOT NULL,
  `question_id` int(4) unsigned NOT NULL,
  `answer_id` int(4) unsigned NOT NULL,
  `create_date` datetime DEFAULT NULL,
  `ip_address` varchar(255) DEFAULT NULL,
  `domain_id` int(6) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table inquirysimple_answers
# ------------------------------------------------------------

DROP TABLE IF EXISTS `inquirysimple_answers`;

CREATE TABLE `inquirysimple_answers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `form_id` varchar(255) NOT NULL,
  `question_id` varchar(255) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `form_id` (`form_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table insert_script
# ------------------------------------------------------------

DROP TABLE IF EXISTS `insert_script`;

CREATE TABLE `insert_script` (
  `insert_script_id` int(11) NOT NULL,
  `save_date` datetime DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `script_body` text DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `valid_from` datetime DEFAULT NULL,
  `valid_to` datetime DEFAULT NULL,
  `position` varchar(60) DEFAULT NULL,
  `domain_id` int(11) DEFAULT 1,
  `cookie_class` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`insert_script_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table insert_script_doc
# ------------------------------------------------------------

DROP TABLE IF EXISTS `insert_script_doc`;

CREATE TABLE `insert_script_doc` (
  `insert_script_doc_id` int(11) NOT NULL,
  `doc_id` int(11) DEFAULT NULL,
  `insert_script` int(11) DEFAULT NULL,
  PRIMARY KEY (`insert_script_doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table insert_script_gr
# ------------------------------------------------------------

DROP TABLE IF EXISTS `insert_script_gr`;

CREATE TABLE `insert_script_gr` (
  `insert_script_gr_id` int(11) NOT NULL,
  `group_id` int(11) DEFAULT NULL,
  `insert_script` int(11) DEFAULT NULL,
  `domain_id` int(11) DEFAULT 1,
  PRIMARY KEY (`insert_script_gr_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table inventory
# ------------------------------------------------------------

DROP TABLE IF EXISTS `inventory`;

CREATE TABLE `inventory` (
  `inventory_id` int(11) NOT NULL,
  `type` varchar(32) NOT NULL,
  `serial_number` varchar(32) DEFAULT NULL,
  `inventory_number` varchar(32) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `room` varchar(32) DEFAULT NULL,
  `deleted` tinyint(4) DEFAULT NULL,
  `date_deleted` date DEFAULT NULL,
  `name` varchar(64) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `junk_reason` text DEFAULT NULL,
  `department` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`inventory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table inventory_detail
# ------------------------------------------------------------

DROP TABLE IF EXISTS `inventory_detail`;

CREATE TABLE `inventory_detail` (
  `inventory_detail_id` int(11) NOT NULL,
  `inventory_id` int(11) NOT NULL,
  `type` varchar(32) NOT NULL,
  `serial_number` varchar(32) DEFAULT NULL,
  `name` varchar(64) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `date_from` date NOT NULL,
  `date_till` date DEFAULT NULL,
  `junk_reason` text DEFAULT NULL,
  PRIMARY KEY (`inventory_detail_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table inventory_log
# ------------------------------------------------------------

DROP TABLE IF EXISTS `inventory_log`;

CREATE TABLE `inventory_log` (
  `inventory_log_id` int(11) NOT NULL,
  `inventory_id` int(11) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `room` varchar(32) DEFAULT NULL,
  `date_from` date NOT NULL,
  `date_till` date DEFAULT NULL,
  `department` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`inventory_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table media
# ------------------------------------------------------------

DROP TABLE IF EXISTS `media`;

CREATE TABLE `media` (
  `media_id` int(10) unsigned NOT NULL,
  `media_fk_id` int(10) unsigned DEFAULT NULL,
  `media_fk_table_name` varchar(255) DEFAULT NULL,
  `media_title_sk` varchar(255) DEFAULT NULL,
  `media_title_cz` varchar(255) DEFAULT NULL,
  `media_title_de` varchar(255) DEFAULT NULL,
  `media_title_en` varchar(255) DEFAULT NULL,
  `media_link` varchar(255) DEFAULT NULL,
  `media_thumb_link` varchar(255) DEFAULT NULL,
  `media_group` varchar(255) DEFAULT NULL,
  `media_info_sk` text DEFAULT NULL,
  `media_info_cz` text DEFAULT NULL,
  `media_info_de` text DEFAULT NULL,
  `media_info_en` text DEFAULT NULL,
  `media_sort_order` int(10) NOT NULL DEFAULT 10,
  `last_update` datetime DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  `field_a` varchar(255) DEFAULT NULL,
  `field_b` varchar(255) DEFAULT NULL,
  `field_c` varchar(255) DEFAULT NULL,
  `field_d` varchar(255) DEFAULT NULL,
  `field_e` varchar(255) DEFAULT NULL,
  `field_f` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`media_id`),
  KEY `IX_media_docis` (`media_fk_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table media_group_to_media
# ------------------------------------------------------------

DROP TABLE IF EXISTS `media_group_to_media`;

CREATE TABLE `media_group_to_media` (
  `media_group_id` int(11) NOT NULL,
  `media_id` int(11) NOT NULL,
  KEY `ix_media_id` (`media_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table media_groups
# ------------------------------------------------------------

DROP TABLE IF EXISTS `media_groups`;

CREATE TABLE `media_groups` (
  `media_group_id` int(11) NOT NULL,
  `media_group_name` varchar(255) NOT NULL,
  `available_groups` varchar(255) DEFAULT NULL,
  `related_pages` text DEFAULT NULL,
  PRIMARY KEY (`media_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table monitoring
# ------------------------------------------------------------

DROP TABLE IF EXISTS `monitoring`;

CREATE TABLE `monitoring` (
  `monitoring_id` int(11) NOT NULL AUTO_INCREMENT,
  `date_insert` datetime DEFAULT NULL,
  `node_name` varchar(16) DEFAULT NULL,
  `db_active` int(11) DEFAULT NULL,
  `db_idle` int(11) DEFAULT NULL,
  `mem_free` bigint(20) DEFAULT NULL,
  `mem_total` bigint(20) DEFAULT NULL,
  `cache` int(11) DEFAULT NULL,
  `sessions` int(11) DEFAULT NULL,
  `cpu_usage` double(5,2) DEFAULT NULL,
  `process_usage` double(5,2) DEFAULT NULL,
  PRIMARY KEY (`monitoring_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table multigroup_mapping
# ------------------------------------------------------------

DROP TABLE IF EXISTS `multigroup_mapping`;

CREATE TABLE `multigroup_mapping` (
  `doc_id` int(11) NOT NULL,
  `master_id` int(11) NOT NULL DEFAULT -1,
  `redirect` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table passwords_history
# ------------------------------------------------------------

DROP TABLE IF EXISTS `passwords_history`;

CREATE TABLE `passwords_history` (
  `passwords_history_id` int(11) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `password` varchar(128) DEFAULT NULL,
  `salt` varchar(64) DEFAULT NULL,
  `save_date` datetime DEFAULT NULL,
  PRIMARY KEY (`passwords_history_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table perex_group_doc
# ------------------------------------------------------------

DROP TABLE IF EXISTS `perex_group_doc`;

CREATE TABLE `perex_group_doc` (
  `doc_id` int(11) NOT NULL,
  `perex_group_id` int(11) DEFAULT NULL,
  KEY `IX_perex_group_doc_id` (`doc_id`),
  KEY `IX_perex_group_grp_id` (`perex_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table perex_groups
# ------------------------------------------------------------

DROP TABLE IF EXISTS `perex_groups`;

CREATE TABLE `perex_groups` (
  `perex_group_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `perex_group_name` varchar(255) NOT NULL,
  `related_pages` text DEFAULT NULL,
  `available_groups` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`perex_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table pkey_generator
# ------------------------------------------------------------

DROP TABLE IF EXISTS `pkey_generator`;

CREATE TABLE `pkey_generator` (
  `name` varchar(255) NOT NULL,
  `value` bigint(20) NOT NULL,
  `table_name` varchar(255) DEFAULT NULL,
  `table_pkey_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `pkey_generator` WRITE;
/*!40000 ALTER TABLE `pkey_generator` DISABLE KEYS */;

INSERT INTO `pkey_generator` (`name`, `value`, `table_name`, `table_pkey_name`)
VALUES
	('banner_banners',1,'banner_banners','banner_id'),
	('basket_browser_id',1,'basket_item','browser_id'),
	('cache',1,'cache','cache_id'),
	('captcha_dictionary',1,'captcha_dictionary','id'),
	('ckeditor_upload_counter',1,'',''),
	('contact',1,'contact','contact_id'),
	('cookies',100,'cookies','cookie_id'),
	('crypto_key',1,NULL,NULL),
	('documents',1,'documents','doc_id'),
	('document_notes',1,'document_notes','id'),
	('domain_limits',1,'domain_limits','domain_limit_id'),
	('export_dat',1,'export_dat','export_dat_id'),
	('file_archiv',1,'file_archiv','file_archiv_id'),
	('file_archiv_global_id',1,'file_archiv','global_id'),
	('file_history',1,'file_history','file_history_id'),
	('gallery_dimension',1,'dimension_id','gallery_dimension_id'),
	('gdpr_regexp',6,'gdpr_regexp','gdpr_regexp_id'),
	('insert_script',1,'insert_script','insert_script_id'),
	('insert_script_doc',1,'insert_script_doc','insert_script_doc_id'),
	('insert_script_gr',1,'insert_script_gr','insert_script_gr_id'),
	('inventory',1,'inventory','inventory_id'),
	('inventory_detail',1,'inventory_detail','inventory_detail_id'),
	('inventory_log',1,'inventory_log','inventory_log_id'),
	('passwords_history',1,'passwords_history','passwords_history_id'),
	('reservation_object_price',1,'reservation_object_price','object_price_id'),
	('reservation_object_times',1,'reservation_object_times','object_time_id'),
	('stat_browser_id',1,NULL,NULL),
	('stat_session_id',1,NULL,NULL),
	('structuremirroring',1,NULL,NULL),
	('terminologicky_slovnik',1,'terminologicky_slovnik','terminologicky_slovnik_id'),
	('todo',1,'todo','todo_id'),
	('user_perm_groups',1,'user_perm_groups','group_id'),
	('user_perm_groups_perms',1,'user_perm_groups_perms','perm_id'),
	('zmluvy_organizacia',1,'zmluvy_organizacia','zmluvy_organizacia_id');

/*!40000 ALTER TABLE `pkey_generator` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table proxy
# ------------------------------------------------------------

DROP TABLE IF EXISTS `proxy`;

CREATE TABLE `proxy` (
  `proxy_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `local_url` text DEFAULT NULL,
  `remote_server` varchar(255) DEFAULT NULL,
  `remote_url` varchar(255) DEFAULT NULL,
  `remote_port` int(11) NOT NULL,
  `crop_start` varchar(255) DEFAULT NULL,
  `crop_end` varchar(255) DEFAULT NULL,
  `encoding` varchar(16) DEFAULT NULL,
  `proxy_method` varchar(64) DEFAULT NULL,
  `include_ext` varchar(255) DEFAULT NULL,
  `auth_method` varchar(16) DEFAULT NULL,
  `auth_username` varchar(64) DEFAULT NULL,
  `auth_password` varchar(64) DEFAULT NULL,
  `auth_host` varchar(64) DEFAULT NULL,
  `auth_domain` varchar(64) DEFAULT NULL,
  `allowed_methods` varchar(64) DEFAULT NULL,
  `keep_crop_start` tinyint(1) DEFAULT NULL,
  `keep_crop_end` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`proxy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table questions_answers
# ------------------------------------------------------------

DROP TABLE IF EXISTS `questions_answers`;

CREATE TABLE `questions_answers` (
  `qa_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `group_name` varchar(255) NOT NULL DEFAULT '',
  `category_name` varchar(64) DEFAULT NULL,
  `question_date` datetime DEFAULT NULL,
  `answer_date` datetime DEFAULT NULL,
  `question` text DEFAULT NULL,
  `answer` text DEFAULT NULL,
  `from_name` varchar(255) NOT NULL DEFAULT '',
  `from_email` varchar(255) DEFAULT NULL,
  `to_name` varchar(255) DEFAULT NULL,
  `to_email` varchar(255) DEFAULT NULL,
  `publish_on_web` tinyint(1) unsigned DEFAULT 0,
  `hash` varchar(255) DEFAULT NULL,
  `allow_publish_on_web` tinyint(1) unsigned NOT NULL DEFAULT 1,
  `sort_priority` int(11) DEFAULT NULL,
  `from_phone` varchar(32) DEFAULT NULL,
  `from_company` varchar(128) DEFAULT NULL,
  `field_a` varchar(255) DEFAULT NULL,
  `field_b` varchar(255) DEFAULT NULL,
  `field_c` varchar(255) DEFAULT NULL,
  `field_d` varchar(255) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  `answer_to_email` text DEFAULT NULL,
  PRIMARY KEY (`qa_id`),
  KEY `qa_id` (`qa_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table quiz
# ------------------------------------------------------------

DROP TABLE IF EXISTS `quiz`;

CREATE TABLE `quiz` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `domain_id` int(11) DEFAULT NULL,
  `quiz_type` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table quiz_answers
# ------------------------------------------------------------

DROP TABLE IF EXISTS `quiz_answers`;

CREATE TABLE `quiz_answers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `form_id` varchar(255) NOT NULL,
  `quiz_id` int(11) NOT NULL,
  `quiz_question_id` int(11) NOT NULL,
  `answer` int(11) NOT NULL,
  `is_correct` int(11) NOT NULL,
  `created` timestamp NOT NULL DEFAULT current_timestamp(),
  `right_answer` int(11) NOT NULL,
  `rate` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `quiz_id` (`quiz_id`),
  KEY `quiz_question_id` (`quiz_question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table quiz_questions
# ------------------------------------------------------------

DROP TABLE IF EXISTS `quiz_questions`;

CREATE TABLE `quiz_questions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `quiz_id` int(11) NOT NULL,
  `sort_order` int(11) NOT NULL,
  `question` varchar(500) NOT NULL,
  `option1` varchar(255) DEFAULT NULL,
  `option2` varchar(255) DEFAULT NULL,
  `option3` varchar(255) DEFAULT NULL,
  `option4` varchar(255) DEFAULT NULL,
  `option5` varchar(255) DEFAULT NULL,
  `option6` varchar(255) DEFAULT NULL,
  `right_answer` int(11) NOT NULL,
  `rate1` int(11) DEFAULT NULL,
  `rate2` int(11) DEFAULT NULL,
  `rate3` int(11) DEFAULT NULL,
  `rate4` int(11) DEFAULT NULL,
  `rate5` int(11) DEFAULT NULL,
  `rate6` int(11) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `quiz_id` (`quiz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table quiz_results
# ------------------------------------------------------------

DROP TABLE IF EXISTS `quiz_results`;

CREATE TABLE `quiz_results` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `quiz_id` int(11) NOT NULL,
  `sort_order` int(11) NOT NULL,
  `score_from` int(11) NOT NULL,
  `score_to` int(11) DEFAULT NULL,
  `description` text DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `quiz_id` (`quiz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table rating
# ------------------------------------------------------------

DROP TABLE IF EXISTS `rating`;

CREATE TABLE `rating` (
  `rating_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `doc_id` int(4) unsigned DEFAULT NULL,
  `user_id` int(4) DEFAULT NULL,
  `rating_value` int(4) unsigned DEFAULT NULL,
  `insert_date` datetime DEFAULT NULL,
  PRIMARY KEY (`rating_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='rating stranok';



# Dump of table reservation
# ------------------------------------------------------------

DROP TABLE IF EXISTS `reservation`;

CREATE TABLE `reservation` (
  `reservation_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `reservation_object_id` int(10) unsigned NOT NULL DEFAULT 0,
  `date_from` datetime DEFAULT NULL,
  `date_to` datetime DEFAULT NULL,
  `name` varchar(150) DEFAULT NULL,
  `surname` varchar(155) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `purpose` text DEFAULT NULL,
  `accepted` tinyint(1) DEFAULT NULL,
  `hash_value` varchar(60) DEFAULT NULL,
  `phone_number` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_slovak_ci DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  `price` decimal(10,2) DEFAULT NULL,
  `user_id` int(11) NOT NULL DEFAULT -1,
  PRIMARY KEY (`reservation_id`),
  KEY `FK_reservation_object_id` (`reservation_object_id`),
  CONSTRAINT `FK_reservation_object_id` FOREIGN KEY (`reservation_object_id`) REFERENCES `reservation_object` (`reservation_object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table reservation_object
# ------------------------------------------------------------

DROP TABLE IF EXISTS `reservation_object`;

CREATE TABLE `reservation_object` (
  `reservation_object_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `must_accepted` tinyint(1) NOT NULL DEFAULT 0,
  `email_accepter` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `passwd` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `max_reservations` tinyint(2) DEFAULT 1,
  `cancel_time_befor` tinyint(2) DEFAULT 0,
  `reservation_time_from` datetime DEFAULT NULL,
  `reservation_time_to` datetime DEFAULT NULL,
  `price_for_day` decimal(7,2) NOT NULL DEFAULT 0.00,
  `price_for_hour` decimal(7,2) NOT NULL DEFAULT 0.00,
  `reservation_for_all_day` bit(1) NOT NULL DEFAULT b'0',
  `photo_link` varchar(255) DEFAULT NULL,
  `description` varchar(2000) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  `time_unit` tinyint(2) DEFAULT NULL,
  PRIMARY KEY (`reservation_object_id`),
  UNIQUE KEY `unique_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_slovak_ci;



# Dump of table reservation_object_price
# ------------------------------------------------------------

DROP TABLE IF EXISTS `reservation_object_price`;

CREATE TABLE `reservation_object_price` (
  `object_price_id` int(11) NOT NULL,
  `object_id` int(10) unsigned NOT NULL,
  `datum_od` date DEFAULT NULL,
  `datum_do` date DEFAULT NULL,
  `cena` decimal(10,2) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`object_price_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table reservation_object_times
# ------------------------------------------------------------

DROP TABLE IF EXISTS `reservation_object_times`;

CREATE TABLE `reservation_object_times` (
  `object_time_id` int(11) NOT NULL,
  `object_id` int(10) unsigned NOT NULL,
  `cas_od` datetime DEFAULT NULL,
  `cas_do` datetime DEFAULT NULL,
  `den` int(11) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`object_time_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table response_headers
# ------------------------------------------------------------

DROP TABLE IF EXISTS `response_headers`;

CREATE TABLE `response_headers` (
  `response_header_id` int(11) NOT NULL AUTO_INCREMENT,
  `url` varchar(255) NOT NULL,
  `header_name` varchar(255) NOT NULL,
  `header_value` varchar(255) NOT NULL,
  `change_date` datetime NOT NULL,
  `note` varchar(255) DEFAULT NULL,
  `domain_id` int(11) NOT NULL,
  PRIMARY KEY (`response_header_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table restaurant_menu
# ------------------------------------------------------------

DROP TABLE IF EXISTS `restaurant_menu`;

CREATE TABLE `restaurant_menu` (
  `menu_id` int(11) NOT NULL,
  `menu_meals_id` int(11) DEFAULT NULL,
  `day` date DEFAULT NULL,
  `priority` tinyint(4) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`menu_id`),
  KEY `menu_meals_id` (`menu_meals_id`),
  CONSTRAINT `restaurant_menu_ibfk_1` FOREIGN KEY (`menu_meals_id`) REFERENCES `restaurant_menu_meals` (`meals_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table restaurant_menu_meals
# ------------------------------------------------------------

DROP TABLE IF EXISTS `restaurant_menu_meals`;

CREATE TABLE `restaurant_menu_meals` (
  `meals_id` int(11) NOT NULL,
  `cathegory` varchar(128) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `weight` varchar(255) DEFAULT NULL,
  `price` decimal(10,2) DEFAULT NULL,
  `alergens` varchar(32) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`meals_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table seo_bots
# ------------------------------------------------------------

DROP TABLE IF EXISTS `seo_bots`;

CREATE TABLE `seo_bots` (
  `seo_bots_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `last_visit` datetime DEFAULT NULL,
  `visit_count` int(11) DEFAULT NULL,
  PRIMARY KEY (`seo_bots_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table seo_google_position
# ------------------------------------------------------------

DROP TABLE IF EXISTS `seo_google_position`;

CREATE TABLE `seo_google_position` (
  `seo_google_position_id` int(11) NOT NULL AUTO_INCREMENT,
  `keyword_id` int(11) DEFAULT NULL,
  `position` int(11) DEFAULT NULL,
  `search_datetime` datetime DEFAULT NULL,
  PRIMARY KEY (`seo_google_position_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table seo_keywords
# ------------------------------------------------------------

DROP TABLE IF EXISTS `seo_keywords`;

CREATE TABLE `seo_keywords` (
  `seo_keyword_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `author` int(11) DEFAULT NULL,
  `domain` varchar(255) DEFAULT NULL,
  `search_bot` varchar(150) DEFAULT NULL,
  `actual_position` int(11) DEFAULT -1,
  PRIMARY KEY (`seo_keyword_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table sita_parsed_ids
# ------------------------------------------------------------

DROP TABLE IF EXISTS `sita_parsed_ids`;

CREATE TABLE `sita_parsed_ids` (
  `news_item_id` int(10) unsigned NOT NULL,
  `parse_date` datetime NOT NULL,
  `sita_group` varchar(16) DEFAULT NULL,
  PRIMARY KEY (`news_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table sms_addressbook
# ------------------------------------------------------------

DROP TABLE IF EXISTS `sms_addressbook`;

CREATE TABLE `sms_addressbook` (
  `book_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(4) unsigned NOT NULL DEFAULT 0,
  `sms_name` varchar(128) NOT NULL DEFAULT '',
  `sms_number` varchar(32) NOT NULL DEFAULT '',
  PRIMARY KEY (`book_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table sms_log
# ------------------------------------------------------------

DROP TABLE IF EXISTS `sms_log`;

CREATE TABLE `sms_log` (
  `log_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(4) NOT NULL DEFAULT 0,
  `user_ip` varchar(32) NOT NULL DEFAULT '',
  `sent_date` datetime NOT NULL DEFAULT '2000-01-01 00:00:00',
  `sms_number` varchar(32) NOT NULL DEFAULT '',
  `sms_text` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table sms_template
# ------------------------------------------------------------

DROP TABLE IF EXISTS `sms_template`;

CREATE TABLE `sms_template` (
  `sms_template_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `text` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table stat_browser
# ------------------------------------------------------------

DROP TABLE IF EXISTS `stat_browser`;

CREATE TABLE `stat_browser` (
  `year` int(11) unsigned NOT NULL DEFAULT 0,
  `week` int(11) unsigned NOT NULL DEFAULT 0,
  `browser_id` varchar(32) DEFAULT '0',
  `platform` varchar(25) DEFAULT '0',
  `subplatform` varchar(20) DEFAULT NULL,
  `views` int(11) unsigned DEFAULT 0,
  `group_id` int(4) unsigned NOT NULL DEFAULT 1,
  KEY `i_update` (`year`,`week`,`browser_id`,`platform`,`subplatform`,`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table stat_country
# ------------------------------------------------------------

DROP TABLE IF EXISTS `stat_country`;

CREATE TABLE `stat_country` (
  `year` int(11) unsigned NOT NULL DEFAULT 0,
  `week` int(11) unsigned NOT NULL DEFAULT 0,
  `country_code` varchar(20) NOT NULL DEFAULT '0',
  `views` int(11) unsigned NOT NULL DEFAULT 0,
  `group_id` int(4) unsigned NOT NULL DEFAULT 1,
  KEY `i_update` (`year`,`week`,`country_code`,`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table stat_doc
# ------------------------------------------------------------

DROP TABLE IF EXISTS `stat_doc`;

CREATE TABLE `stat_doc` (
  `year` int(11) unsigned DEFAULT 0,
  `week` int(11) unsigned DEFAULT 0,
  `doc_id` int(11) DEFAULT 0,
  `views` int(11) unsigned DEFAULT 1,
  `in_count` int(11) unsigned DEFAULT 0,
  `out_count` int(11) unsigned DEFAULT 0,
  `view_time_sum` int(11) unsigned DEFAULT 0,
  `view_time_count` int(11) unsigned DEFAULT 0,
  KEY `i_docid` (`doc_id`),
  KEY `i_update` (`year`,`week`,`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table stat_error
# ------------------------------------------------------------

DROP TABLE IF EXISTS `stat_error`;

CREATE TABLE `stat_error` (
  `year` int(11) unsigned DEFAULT 0,
  `week` int(11) unsigned DEFAULT 0,
  `url` varchar(255) DEFAULT '0',
  `query_string` varchar(255) DEFAULT '0',
  `count` int(11) unsigned DEFAULT 0,
  `domain_id` int(11) NOT NULL DEFAULT 0,
  KEY `i_update` (`year`,`week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table stat_from
# ------------------------------------------------------------

DROP TABLE IF EXISTS `stat_from`;

CREATE TABLE `stat_from` (
  `from_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `browser_id` bigint(20) unsigned DEFAULT NULL,
  `session_id` bigint(20) unsigned DEFAULT NULL,
  `referer_server_name` varchar(255) DEFAULT NULL,
  `referer_url` varchar(255) DEFAULT NULL,
  `from_time` datetime DEFAULT NULL,
  `doc_id` int(11) DEFAULT NULL,
  `group_id` int(10) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`from_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table stat_keys
# ------------------------------------------------------------

DROP TABLE IF EXISTS `stat_keys`;

CREATE TABLE `stat_keys` (
  `stat_keys_id` int(11) NOT NULL,
  `value` varchar(64) NOT NULL,
  UNIQUE KEY `IX_sk_value` (`value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table stat_searchengine
# ------------------------------------------------------------

DROP TABLE IF EXISTS `stat_searchengine`;

CREATE TABLE `stat_searchengine` (
  `search_date` datetime NOT NULL DEFAULT '2000-01-01 00:00:00',
  `server` varchar(16) NOT NULL DEFAULT '',
  `query` varchar(64) NOT NULL DEFAULT '',
  `doc_id` int(4) unsigned NOT NULL DEFAULT 0,
  `remote_host` varchar(255) DEFAULT NULL,
  `group_id` int(10) unsigned NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table stat_site_days
# ------------------------------------------------------------

DROP TABLE IF EXISTS `stat_site_days`;

CREATE TABLE `stat_site_days` (
  `year` int(11) unsigned DEFAULT 0,
  `week` int(11) unsigned DEFAULT 0,
  `views_mo` int(11) unsigned DEFAULT 0,
  `sessions_mo` int(11) unsigned DEFAULT 0,
  `views_tu` int(11) unsigned DEFAULT 0,
  `sessions_tu` int(11) unsigned DEFAULT 0,
  `views_we` int(11) unsigned DEFAULT 0,
  `sessions_we` int(11) unsigned DEFAULT 0,
  `views_th` int(11) unsigned DEFAULT 0,
  `sessions_th` int(11) unsigned DEFAULT 0,
  `views_fr` int(11) unsigned DEFAULT 0,
  `sessions_fr` int(11) unsigned DEFAULT 0,
  `views_sa` int(11) unsigned DEFAULT 0,
  `sessions_sa` int(11) unsigned DEFAULT 0,
  `views_su` int(11) unsigned DEFAULT 0,
  `sessions_su` int(11) unsigned DEFAULT 0,
  `view_time_sum` int(11) unsigned DEFAULT 0,
  `view_time_count` int(11) unsigned DEFAULT 0,
  `group_id` int(4) unsigned NOT NULL DEFAULT 1,
  KEY `i_update` (`year`,`week`,`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table stat_site_hours
# ------------------------------------------------------------

DROP TABLE IF EXISTS `stat_site_hours`;

CREATE TABLE `stat_site_hours` (
  `year` int(11) unsigned NOT NULL DEFAULT 0,
  `week` int(11) unsigned NOT NULL DEFAULT 0,
  `views_0` int(11) unsigned DEFAULT 0,
  `views_1` int(11) unsigned DEFAULT 0,
  `views_2` int(11) unsigned DEFAULT 0,
  `views_3` int(11) unsigned DEFAULT 0,
  `views_4` int(11) unsigned DEFAULT 0,
  `views_5` int(11) unsigned DEFAULT 0,
  `views_6` int(11) unsigned DEFAULT 0,
  `views_7` int(11) unsigned DEFAULT 0,
  `views_8` int(11) unsigned DEFAULT 0,
  `views_9` int(11) unsigned DEFAULT 0,
  `views_10` int(11) unsigned DEFAULT 0,
  `views_11` int(11) unsigned DEFAULT 0,
  `views_12` int(11) unsigned DEFAULT 0,
  `views_13` int(11) unsigned DEFAULT 0,
  `views_14` int(11) unsigned DEFAULT 0,
  `views_15` int(11) unsigned DEFAULT 0,
  `views_16` int(11) unsigned DEFAULT 0,
  `views_17` int(11) unsigned DEFAULT 0,
  `views_18` int(11) unsigned DEFAULT 0,
  `views_19` int(11) unsigned DEFAULT 0,
  `views_20` int(11) unsigned DEFAULT 0,
  `views_21` int(11) unsigned DEFAULT 0,
  `views_22` int(11) unsigned DEFAULT 0,
  `views_23` int(11) unsigned DEFAULT 0,
  `sessions_0` int(11) unsigned DEFAULT 0,
  `sessions_1` int(11) unsigned DEFAULT 0,
  `sessions_2` int(11) unsigned DEFAULT 0,
  `sessions_3` int(11) unsigned DEFAULT 0,
  `sessions_4` int(11) unsigned DEFAULT 0,
  `sessions_5` int(11) unsigned DEFAULT 0,
  `sessions_6` int(11) unsigned DEFAULT 0,
  `sessions_7` int(11) unsigned DEFAULT 0,
  `sessions_8` int(11) unsigned DEFAULT 0,
  `sessions_9` int(11) unsigned DEFAULT 0,
  `sessions_10` int(11) unsigned DEFAULT 0,
  `sessions_11` int(11) unsigned DEFAULT 0,
  `sessions_12` int(11) unsigned DEFAULT 0,
  `sessions_13` int(11) unsigned DEFAULT 0,
  `sessions_14` int(11) unsigned DEFAULT 0,
  `sessions_15` int(11) unsigned DEFAULT 0,
  `sessions_16` int(11) unsigned DEFAULT 0,
  `sessions_17` int(11) unsigned DEFAULT 0,
  `sessions_18` int(11) unsigned DEFAULT 0,
  `sessions_19` int(11) unsigned DEFAULT 0,
  `sessions_20` int(11) unsigned DEFAULT 0,
  `sessions_21` int(11) unsigned DEFAULT 0,
  `sessions_22` int(11) unsigned DEFAULT 0,
  `sessions_23` int(11) unsigned DEFAULT 0,
  `group_id` int(4) unsigned NOT NULL DEFAULT 1,
  KEY `i_update` (`year`,`week`,`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table stat_userlogon
# ------------------------------------------------------------

DROP TABLE IF EXISTS `stat_userlogon`;

CREATE TABLE `stat_userlogon` (
  `year` int(4) unsigned NOT NULL DEFAULT 0,
  `week` int(4) unsigned NOT NULL DEFAULT 0,
  `user_id` int(4) unsigned NOT NULL DEFAULT 0,
  `views` int(4) unsigned DEFAULT 1,
  `logon_time` datetime DEFAULT NULL,
  `view_minutes` int(11) unsigned DEFAULT 0,
  `hostname` varchar(255) DEFAULT NULL,
  KEY `i_userid` (`user_id`),
  KEY `i_update` (`user_id`,`logon_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table stat_views
# ------------------------------------------------------------

DROP TABLE IF EXISTS `stat_views`;

CREATE TABLE `stat_views` (
  `view_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `browser_id` bigint(20) unsigned DEFAULT NULL,
  `session_id` bigint(20) unsigned DEFAULT NULL,
  `doc_id` int(11) DEFAULT NULL,
  `last_doc_id` int(11) DEFAULT NULL,
  `view_time` datetime DEFAULT NULL,
  `group_id` int(10) unsigned NOT NULL DEFAULT 0,
  `last_group_id` int(10) unsigned NOT NULL DEFAULT 0,
  `browser_ua_id` int(11) DEFAULT NULL,
  `platform_id` int(11) DEFAULT NULL,
  `subplatform_id` int(11) DEFAULT NULL,
  `country` varchar(4) DEFAULT NULL,
  PRIMARY KEY (`view_id`),
  KEY `i_docid` (`doc_id`),
  KEY `i_last_doc_id` (`last_doc_id`),
  KEY `i_group_id` (`group_id`),
  KEY `i_last_group_id` (`last_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table stopword
# ------------------------------------------------------------

DROP TABLE IF EXISTS `stopword`;

CREATE TABLE `stopword` (
  `word` varchar(255) DEFAULT NULL,
  `language` varchar(2) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `stopword` WRITE;
/*!40000 ALTER TABLE `stopword` DISABLE KEYS */;

INSERT INTO `stopword` (`word`, `language`)
VALUES
	('a','sk'),
	('aby','sk'),
	('aj','sk'),
	('ak','sk'),
	('ako','sk'),
	('ale','sk'),
	('alebo','sk'),
	('and','sk'),
	('ani','sk'),
	('ano','sk'),
	('asi','sk'),
	('az','sk'),
	('bez','sk'),
	('bude','sk'),
	('budem','sk'),
	('budes','sk'),
	('budeme','sk'),
	('budete','sk'),
	('budu','sk'),
	('by','sk'),
	('bol','sk'),
	('bola','sk'),
	('boli','sk'),
	('bolo','sk'),
	('byt','sk'),
	('cez','sk'),
	('co','sk'),
	('ci','sk'),
	('dalsi','sk'),
	('dalsia','sk'),
	('dalsie','sk'),
	('dnes','sk'),
	('do','sk'),
	('ho','sk'),
	('este','sk'),
	('for','sk'),
	('i','sk'),
	('ja','sk'),
	('je','sk'),
	('jeho','sk'),
	('jej','sk'),
	('ich','sk'),
	('iba','sk'),
	('ine','sk'),
	('iny','sk'),
	('som','sk'),
	('si','sk'),
	('sme','sk'),
	('su','sk'),
	('k','sk'),
	('kam','sk'),
	('kazdy','sk'),
	('kazda','sk'),
	('kazde','sk'),
	('kazdi','sk'),
	('kde','sk'),
	('ked','sk'),
	('kto','sk'),
	('ktora','sk'),
	('ktore','sk'),
	('ktorou','sk'),
	('ktory','sk'),
	('ktori','sk'),
	('ku','sk'),
	('lebo','sk'),
	('len','sk'),
	('ma','sk'),
	('mat','sk'),
	('ma','sk'),
	('mate','sk'),
	('medzi','sk'),
	('mi','sk'),
	('mna','sk'),
	('mne','sk'),
	('mnou','sk'),
	('musiet','sk'),
	('moct','sk'),
	('moj','sk'),
	('moze','sk'),
	('my','sk'),
	('na','sk'),
	('nad','sk'),
	('nam','sk'),
	('nas','sk'),
	('nasi','sk'),
	('nie','sk'),
	('nech','sk'),
	('nez','sk'),
	('nic','sk'),
	('niektory','sk'),
	('nove','sk'),
	('novy','sk'),
	('nova','sk'),
	('nove','sk'),
	('novi','sk'),
	('o','sk'),
	('od','sk'),
	('odo','sk'),
	('of','sk'),
	('on','sk'),
	('ona','sk'),
	('ono','sk'),
	('oni','sk'),
	('ony','sk'),
	('po','sk'),
	('pod','sk'),
	('podla','sk'),
	('pokial','sk'),
	('potom','sk'),
	('prave','sk'),
	('pre','sk'),
	('preco','sk'),
	('preto','sk'),
	('pretoze','sk'),
	('prvy','sk'),
	('prva','sk'),
	('prve','sk'),
	('prvi','sk'),
	('pred','sk'),
	('predo','sk'),
	('pri','sk'),
	('pyta','sk'),
	('s','sk'),
	('sa','sk'),
	('so','sk'),
	('si','sk'),
	('svoje','sk'),
	('svoj','sk'),
	('svojich','sk'),
	('svojim','sk'),
	('svojimi','sk'),
	('ta','sk'),
	('tak','sk'),
	('takze','sk'),
	('tato','sk'),
	('teda','sk'),
	('te','sk'),
	('te','sk'),
	('ten','sk'),
	('tento','sk'),
	('the','sk'),
	('tieto','sk'),
	('tym','sk'),
	('tymto','sk'),
	('tiez','sk'),
	('to','sk'),
	('toto','sk'),
	('toho','sk'),
	('tohoto','sk'),
	('tom','sk'),
	('tomto','sk'),
	('tomuto','sk'),
	('toto','sk'),
	('tu','sk'),
	('tu','sk'),
	('tuto','sk'),
	('tvoj','sk'),
	('ty','sk'),
	('tvojimi','sk'),
	('uz','sk'),
	('v','sk'),
	('vam','sk'),
	('vas','sk'),
	('vase','sk'),
	('vo','sk'),
	('viac','sk'),
	('vsak','sk'),
	('vsetok','sk'),
	('vy','sk'),
	('z','sk'),
	('za','sk'),
	('zo','sk'),
	('ze','sk'),
	('peter','sk'),
	('a','cz'),
	('aby','cz'),
	('aj','cz'),
	('ale','cz'),
	('anebo','cz'),
	('ani','cz'),
	('aniz','cz'),
	('ano','cz'),
	('asi','cz'),
	('avska','cz'),
	('az','cz'),
	('ba','cz'),
	('bez','cz'),
	('bude','cz'),
	('budem','cz'),
	('budes','cz'),
	('by','cz'),
	('byl','cz'),
	('byla','cz'),
	('byli','cz'),
	('bylo','cz'),
	('byt','cz'),
	('ci','cz'),
	('clanek','cz'),
	('clanku','cz'),
	('clanky','cz'),
	('co','cz'),
	('com','cz'),
	('coz','cz'),
	('cz','cz'),
	('dalsi','cz'),
	('dnes','cz'),
	('do','cz'),
	('email','cz'),
	('ho','cz'),
	('i','cz'),
	('jak','cz'),
	('jake','cz'),
	('jako','cz'),
	('je','cz'),
	('jeho','cz'),
	('jej','cz'),
	('jeji','cz'),
	('jejich','cz'),
	('jen','cz'),
	('jeste','cz'),
	('jenz','cz'),
	('ji','cz'),
	('jine','cz'),
	('jiz','cz'),
	('jsem','cz'),
	('jses','cz'),
	('jsi','cz'),
	('jsme','cz'),
	('jsou','cz'),
	('jste','cz'),
	('k','cz'),
	('kam','cz'),
	('kde','cz'),
	('kdo','cz'),
	('kdyz','cz'),
	('ke','cz'),
	('ktera','cz'),
	('ktere','cz'),
	('kteri','cz'),
	('kterou','cz'),
	('ktery','cz'),
	('ku','cz'),
	('ma','cz'),
	('mate','cz'),
	('me','cz'),
	('mezi','cz'),
	('mi','cz'),
	('mit','cz'),
	('mne','cz'),
	('mnou','cz'),
	('muj','cz'),
	('muze','cz'),
	('my','cz'),
	('na','cz'),
	('nad','cz'),
	('nam','cz'),
	('nas','cz'),
	('nasi','cz'),
	('ne','cz'),
	('nebo','cz'),
	('nebot','cz'),
	('necht','cz'),
	('nejsou','cz'),
	('není','cz'),
	('neni','cz'),
	('net','cz'),
	('nez','cz'),
	('ni','cz'),
	('nic','cz'),
	('nove','cz'),
	('novy','cz'),
	('nybrz','cz'),
	('o','cz'),
	('od','cz'),
	('ode','cz'),
	('on','cz'),
	('org','cz'),
	('pak','cz'),
	('po','cz'),
	('pod','cz'),
	('podle','cz'),
	('pokud','cz'),
	('pouze','cz'),
	('prave','cz'),
	('pred','cz'),
	('pres','cz'),
	('pri','cz'),
	('pro','cz'),
	('proc','cz'),
	('proto','cz'),
	('protoze','cz'),
	('prvni','cz'),
	('pta','cz'),
	('re','cz'),
	('s','cz'),
	('se','cz'),
	('si','cz'),
	('sice','cz'),
	('spol','cz'),
	('strana','cz'),
	('sve','cz'),
	('svuj','cz'),
	('svych','cz'),
	('svym','cz'),
	('svymi','cz'),
	('ta','cz'),
	('tak','cz'),
	('take','cz'),
	('takze','cz'),
	('tamhle','cz'),
	('tato','cz'),
	('tedy','cz'),
	('tema','cz'),
	('te','cz'),
	('ten','cz'),
	('tedy','cz'),
	('tento','cz'),
	('teto','cz'),
	('tim','cz'),
	('timto','cz'),
	('tipy','cz'),
	('to','cz'),
	('tohle','cz'),
	('toho','cz'),
	('tohoto','cz'),
	('tom','cz'),
	('tomto','cz'),
	('tomuto','cz'),
	('totiz','cz'),
	('tu','cz'),
	('tudiz','cz'),
	('tuto','cz'),
	('tvuj','cz'),
	('ty','cz'),
	('tyto','cz'),
	('u','cz'),
	('uz','cz'),
	('v','cz'),
	('vam','cz'),
	('vas','cz'),
	('vas','cz'),
	('vase','cz'),
	('ve','cz'),
	('vedle','cz'),
	('vice','cz'),
	('vsak','cz'),
	('vsechen','cz'),
	('vy','cz'),
	('vzdyt','cz'),
	('z','cz'),
	('za','cz'),
	('zda','cz'),
	('zde','cz'),
	('ze','cz'),
	('zpet','cz'),
	('zpravy','cz'),
	('a','en'),
	('about','en'),
	('above','en'),
	('after','en'),
	('again','en'),
	('against','en'),
	('all','en'),
	('am','en'),
	('an','en'),
	('and','en'),
	('any','en'),
	('are','en'),
	('aren\'t','en'),
	('as','en'),
	('at','en'),
	('be','en'),
	('because','en'),
	('been','en'),
	('before','en'),
	('being','en'),
	('below','en'),
	('between','en'),
	('both','en'),
	('but','en'),
	('by','en'),
	('can\'t','en'),
	('cannot','en'),
	('could','en'),
	('couldn\'t','en'),
	('did','en'),
	('didn\'t','en'),
	('do','en'),
	('does','en'),
	('doesn\'t','en'),
	('doing','en'),
	('don\'t','en'),
	('down','en'),
	('during','en'),
	('each','en'),
	('few','en'),
	('for','en'),
	('from','en'),
	('further','en'),
	('had','en'),
	('hadn\'t','en'),
	('has','en'),
	('hasn\'t','en'),
	('have','en'),
	('haven\'t','en'),
	('having','en'),
	('he','en'),
	('he\'d','en'),
	('he\'ll','en'),
	('he\'s','en'),
	('her','en'),
	('here','en'),
	('here\'s','en'),
	('hers','en'),
	('herself','en'),
	('him','en'),
	('himself','en'),
	('his','en'),
	('how','en'),
	('how\'s','en'),
	('i','en'),
	('i\'d','en'),
	('i\'ll','en'),
	('i\'m','en'),
	('i\'ve','en'),
	('if','en'),
	('in','en'),
	('into','en'),
	('is','en'),
	('isn\'t','en'),
	('it','en'),
	('it\'s','en'),
	('its','en'),
	('itself','en'),
	('let\'s','en'),
	('me','en'),
	('more','en'),
	('most','en'),
	('mustn\'t','en'),
	('my','en'),
	('myself','en'),
	('no','en'),
	('nor','en'),
	('not','en'),
	('of','en'),
	('off','en'),
	('on','en'),
	('once','en'),
	('only','en'),
	('or','en'),
	('other','en'),
	('ought','en'),
	('our','en'),
	('ours','en'),
	('ourselves','en'),
	('out','en'),
	('over','en'),
	('own','en'),
	('same','en'),
	('shan\'t','en'),
	('she','en'),
	('she\'d','en'),
	('she\'ll','en'),
	('she\'s','en'),
	('should','en'),
	('shouldn\'t','en'),
	('so','en'),
	('some','en'),
	('such','en'),
	('than','en'),
	('that','en'),
	('that\'s','en'),
	('the','en'),
	('their','en'),
	('theirs','en'),
	('them','en'),
	('themselves','en'),
	('then','en'),
	('there','en'),
	('there\'s','en'),
	('these','en'),
	('they','en'),
	('they\'d','en'),
	('they\'ll','en'),
	('they\'re','en'),
	('they\'ve','en'),
	('this','en'),
	('those','en'),
	('through','en'),
	('to','en'),
	('too','en'),
	('under','en'),
	('until','en'),
	('up','en'),
	('very','en'),
	('was','en'),
	('wasn\'t','en'),
	('we','en'),
	('we\'d','en'),
	('we\'ll','en'),
	('we\'re','en'),
	('we\'ve','en'),
	('were','en'),
	('weren\'t','en'),
	('what','en'),
	('what\'s','en'),
	('when','en'),
	('when\'s','en'),
	('where','en'),
	('where\'s','en'),
	('which','en'),
	('while','en'),
	('who','en'),
	('who\'s','en'),
	('whom','en'),
	('why','en'),
	('why\'s','en'),
	('with','en'),
	('won\'t','en'),
	('would','en'),
	('wouldn\'t','en'),
	('you','en'),
	('you\'d','en'),
	('you\'ll','en'),
	('you\'re','en'),
	('you\'ve','en'),
	('your','en'),
	('yours','en'),
	('yourself','en'),
	('yourselves','en');

/*!40000 ALTER TABLE `stopword` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table templates
# ------------------------------------------------------------

DROP TABLE IF EXISTS `templates`;

CREATE TABLE `templates` (
  `temp_id` int(11) NOT NULL AUTO_INCREMENT,
  `temp_name` varchar(64) NOT NULL DEFAULT '',
  `forward` varchar(64) NOT NULL DEFAULT '',
  `lng` varchar(16) NOT NULL DEFAULT 'sk',
  `header_doc_id` int(11) NOT NULL DEFAULT 0,
  `footer_doc_id` int(11) NOT NULL DEFAULT 0,
  `after_body_data` text DEFAULT NULL,
  `css` varchar(4000) DEFAULT NULL,
  `menu_doc_id` int(11) NOT NULL DEFAULT -1,
  `right_menu_doc_id` int(4) DEFAULT -1,
  `base_css_path` varchar(4000) DEFAULT NULL,
  `object_a_doc_id` int(4) DEFAULT NULL,
  `object_b_doc_id` int(4) DEFAULT NULL,
  `object_c_doc_id` int(4) DEFAULT NULL,
  `object_d_doc_id` int(4) DEFAULT NULL,
  `available_groups` varchar(255) DEFAULT NULL,
  `template_install_name` varchar(64) DEFAULT NULL,
  `disable_spam_protection` tinyint(1) DEFAULT 0,
  `templates_group_id` int(11) NOT NULL DEFAULT 1,
  `inline_editing_mode` varchar(16) DEFAULT NULL,
  PRIMARY KEY (`temp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `templates` WRITE;
/*!40000 ALTER TABLE `templates` DISABLE KEYS */;

INSERT INTO `templates` (`temp_id`, `temp_name`, `forward`, `lng`, `header_doc_id`, `footer_doc_id`, `after_body_data`, `css`, `menu_doc_id`, `right_menu_doc_id`, `base_css_path`, `object_a_doc_id`, `object_b_doc_id`, `object_c_doc_id`, `object_d_doc_id`, `available_groups`, `template_install_name`, `disable_spam_protection`, `templates_group_id`, `inline_editing_mode`)
VALUES
	(1,'Generic','tmp_generic.jsp','sk',1,3,'','',2,-1,'/css/page.css',NULL,NULL,NULL,NULL,NULL,NULL,0,1,NULL);

/*!40000 ALTER TABLE `templates` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table templates_group
# ------------------------------------------------------------

DROP TABLE IF EXISTS `templates_group`;

CREATE TABLE `templates_group` (
  `templates_group_id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `directory` varchar(255) DEFAULT NULL,
  `key_prefix` varchar(128) DEFAULT NULL,
  `inline_editing_mode` varchar(16) DEFAULT NULL,
  PRIMARY KEY (`templates_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `templates_group` WRITE;
/*!40000 ALTER TABLE `templates_group` DISABLE KEYS */;

INSERT INTO `templates_group` (`templates_group_id`, `name`, `directory`, `key_prefix`, `inline_editing_mode`)
VALUES
	(1,'nepriradené','/',NULL,NULL);

/*!40000 ALTER TABLE `templates_group` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table terminologicky_slovnik
# ------------------------------------------------------------

DROP TABLE IF EXISTS `terminologicky_slovnik`;

CREATE TABLE `terminologicky_slovnik` (
  `terminologicky_slovnik_id` int(11) NOT NULL,
  `termin` varchar(255) DEFAULT NULL,
  `synonymum` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `kategoria` text DEFAULT NULL,
  `definicia` text DEFAULT NULL,
  `poznamka1` text DEFAULT NULL,
  `poznamka2` text DEFAULT NULL,
  `zdroj1` text DEFAULT NULL,
  `zdroj2` text DEFAULT NULL,
  `priklad` text DEFAULT NULL,
  PRIMARY KEY (`terminologicky_slovnik_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table tips_of_the_day
# ------------------------------------------------------------

DROP TABLE IF EXISTS `tips_of_the_day`;

CREATE TABLE `tips_of_the_day` (
  `tip_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `tip_group` varchar(255) NOT NULL,
  `tip_text` text NOT NULL,
  PRIMARY KEY (`tip_id`),
  UNIQUE KEY `tip_id` (`tip_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table todo
# ------------------------------------------------------------

DROP TABLE IF EXISTS `todo`;

CREATE TABLE `todo` (
  `todo_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `create_date` datetime NOT NULL,
  `modif_date` datetime DEFAULT NULL,
  `text` varchar(128) NOT NULL,
  `is_global` tinyint(1) NOT NULL DEFAULT 0,
  `is_resolved` tinyint(1) NOT NULL DEFAULT 0,
  `sort_priority` int(11) DEFAULT NULL,
  `dead_line` datetime DEFAULT NULL,
  `note` varchar(2000) DEFAULT NULL,
  `group_id` int(11) DEFAULT 0,
  PRIMARY KEY (`todo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `todo` WRITE;
/*!40000 ALTER TABLE `todo` DISABLE KEYS */;

INSERT INTO `todo` (`todo_id`, `user_id`, `create_date`, `modif_date`, `text`, `is_global`, `is_resolved`, `sort_priority`, `dead_line`, `note`, `group_id`)
VALUES
	(42,1,'2020-12-31 13:27:53',NULL,'Viete o tom, že môžete v hornom menu kliknúť na ikonu a prepnúť sa na starú verziu administrácie?',0,0,0,NULL,'',0),
	(43,1,'2020-12-31 13:28:37',NULL,'Roadmapu nájdete na docs.webjetcms.sk/v2021',0,0,0,NULL,'',0),
	(52,18,'2023-02-28 10:10:49',NULL,'asdfasdf',0,0,0,NULL,'',0);

/*!40000 ALTER TABLE `todo` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table url_redirect
# ------------------------------------------------------------

DROP TABLE IF EXISTS `url_redirect`;

CREATE TABLE `url_redirect` (
  `url_redirect_id` int(11) unsigned NOT NULL,
  `old_url` varchar(255) NOT NULL,
  `new_url` varchar(255) NOT NULL,
  `redirect_code` int(11) unsigned NOT NULL,
  `insert_date` datetime NOT NULL,
  `domain_name` varchar(255) DEFAULT NULL,
  `publish_date` datetime DEFAULT NULL,
  PRIMARY KEY (`url_redirect_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table user_alarm
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_alarm`;

CREATE TABLE `user_alarm` (
  `user_id` int(4) NOT NULL DEFAULT 0,
  `alarm_id` int(4) DEFAULT NULL,
  `warning` int(4) DEFAULT NULL,
  `send_date` datetime DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table user_disabled_items
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_disabled_items`;

CREATE TABLE `user_disabled_items` (
  `user_id` int(4) unsigned NOT NULL DEFAULT 0,
  `item_name` varchar(64) NOT NULL,
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `user_disabled_items` WRITE;
/*!40000 ALTER TABLE `user_disabled_items` DISABLE KEYS */;

INSERT INTO `user_disabled_items` (`user_id`, `item_name`)
VALUES
	(1,'editorMiniEdit'),
	(1,'editor_show_hidden_folders'),
	(1,'cmp_contact'),
	(1,'components.news.edit_templates'),
	(1,'editor_unlimited_upload'),
	(1,'conf.show_all_variables'),
	(1,'cmp_adminlog_logging'),
	(1,'cmp_in-memory-logging'),
	(1,'editor_edit_perex');

/*!40000 ALTER TABLE `user_disabled_items` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table user_group_verify
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_group_verify`;

CREATE TABLE `user_group_verify` (
  `verify_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(4) unsigned NOT NULL DEFAULT 0,
  `user_groups` varchar(255) NOT NULL DEFAULT '',
  `hash` varchar(32) NOT NULL DEFAULT '',
  `create_date` datetime NOT NULL DEFAULT '2000-01-01 00:00:00',
  `verify_date` datetime DEFAULT NULL,
  `email` varchar(255) NOT NULL DEFAULT '',
  `hostname` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`verify_id`),
  KEY `verify_id` (`verify_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table user_groups
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_groups`;

CREATE TABLE `user_groups` (
  `user_group_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_group_name` varchar(255) NOT NULL DEFAULT '',
  `user_group_type` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `user_group_comment` text DEFAULT NULL,
  `require_approve` tinyint(1) NOT NULL DEFAULT 0,
  `email_doc_id` int(4) NOT NULL DEFAULT -1,
  `allow_user_edit` tinyint(1) unsigned DEFAULT 0,
  `require_email_verification` tinyint(1) DEFAULT NULL,
  `price_discount` int(11) DEFAULT 0,
  PRIMARY KEY (`user_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `user_groups` WRITE;
/*!40000 ALTER TABLE `user_groups` DISABLE KEYS */;

INSERT INTO `user_groups` (`user_group_id`, `user_group_name`, `user_group_type`, `user_group_comment`, `require_approve`, `email_doc_id`, `allow_user_edit`, `require_email_verification`, `price_discount`)
VALUES
	(1,'VIP Klienti',0,'',0,68583,0,0,0),
	(2,'Obchodní partneri',0,NULL,0,-1,0,NULL,0),
	(3,'Redaktori',0,'',0,68580,0,0,0),
	(4,'Bankári',0,'',1,-1,1,0,0),
	(5,'Newsletter',1,'',1,-1,0,0,0),
	(6,'Vianočné pozdravy',1,'Zoznam na vianoce',1,22,0,1,0),
	(529,'noApprove_allowUserEdit_1',0,'Used for testing register form.\nUser will be auto logged after register action.',0,-1,1,0,0),
	(532,'noApprove_allowUserEdit_2',0,'Used for testing register form. \nUser will be forced to auth,  that is set in register form.',0,-1,1,0,0),
	(802,'Blog',0,'',0,-1,0,0,0),
	(952,'TestCamp',1,'',0,-1,0,0,0),
	(959,'Autotest Nletter Group',1,'Group for autotest newsletter subscribe/unsubscribe',0,-1,1,0,0),
	(1273,'ReservationDiscount',1,'',0,-1,0,0,0),
	(1334,'Discount_25',1,'',0,-1,0,0,25),
	(1335,'Discount_40',1,'',0,-1,0,0,40);

/*!40000 ALTER TABLE `user_groups` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table user_perm_groups
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_perm_groups`;

CREATE TABLE `user_perm_groups` (
  `group_id` int(11) NOT NULL,
  `group_title` varchar(255) DEFAULT NULL,
  `writable_folders` varchar(600) DEFAULT NULL,
  `editable_groups` varchar(255) DEFAULT NULL,
  `editable_pages` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table user_perm_groups_perms
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_perm_groups_perms`;

CREATE TABLE `user_perm_groups_perms` (
  `perm_group_id` int(11) NOT NULL,
  `perm_id` int(11) NOT NULL,
  `permission` varchar(64) NOT NULL,
  PRIMARY KEY (`perm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table user_settings
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_settings`;

CREATE TABLE `user_settings` (
  `user_settings_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(10) unsigned NOT NULL,
  `skey` varchar(64) DEFAULT NULL,
  `svalue1` varchar(2000) DEFAULT NULL,
  `svalue2` varchar(255) DEFAULT NULL,
  `svalue3` varchar(255) DEFAULT NULL,
  `svalue4` varchar(255) DEFAULT NULL,
  `sint1` int(11) DEFAULT NULL,
  `sint2` int(11) DEFAULT NULL,
  `sint3` int(11) DEFAULT NULL,
  `sint4` int(11) DEFAULT NULL,
  `sdate` datetime DEFAULT NULL,
  PRIMARY KEY (`user_settings_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table user_settings_admin
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_settings_admin`;

CREATE TABLE `user_settings_admin` (
  `user_settings_admin_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `skey` varchar(255) DEFAULT NULL,
  `value` varchar(4000) DEFAULT NULL,
  PRIMARY KEY (`user_settings_admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table users
# ------------------------------------------------------------

DROP TABLE IF EXISTS `users`;

CREATE TABLE `users` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(16) DEFAULT NULL,
  `first_name` varchar(128) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `login` varchar(128) NOT NULL,
  `password` varchar(128) DEFAULT NULL,
  `is_admin` tinyint(1) NOT NULL DEFAULT 0,
  `user_groups` varchar(255) DEFAULT NULL,
  `company` varchar(255) DEFAULT NULL,
  `adress` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `PSC` varchar(20) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `authorized` tinyint(1) DEFAULT NULL,
  `editable_groups` varchar(255) DEFAULT NULL,
  `editable_pages` varchar(255) DEFAULT NULL,
  `writable_folders` text DEFAULT NULL,
  `last_logon` datetime DEFAULT NULL,
  `module_perms` varchar(255) DEFAULT NULL,
  `disabled_items` varchar(255) DEFAULT NULL,
  `reg_date` datetime DEFAULT NULL,
  `field_a` varchar(255) DEFAULT NULL,
  `field_b` varchar(255) DEFAULT NULL,
  `field_c` varchar(255) DEFAULT NULL,
  `field_d` varchar(255) DEFAULT NULL,
  `field_e` varchar(255) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `sex_male` tinyint(1) unsigned DEFAULT 1,
  `photo` varchar(255) DEFAULT NULL,
  `signature` varchar(255) DEFAULT NULL,
  `forum_rank` int(11) unsigned NOT NULL DEFAULT 0,
  `rating_rank` int(11) unsigned NOT NULL DEFAULT 0,
  `allow_login_start` date DEFAULT NULL,
  `allow_login_end` date DEFAULT NULL,
  `authorize_hash` varchar(32) DEFAULT NULL,
  `fax` varchar(255) DEFAULT NULL,
  `delivery_first_name` varchar(64) DEFAULT NULL,
  `delivery_last_name` varchar(64) DEFAULT NULL,
  `delivery_company` varchar(64) DEFAULT NULL,
  `delivery_adress` varchar(128) DEFAULT NULL,
  `delivery_city` varchar(64) DEFAULT NULL,
  `delivery_psc` varchar(8) DEFAULT NULL,
  `delivery_country` varchar(32) DEFAULT NULL,
  `delivery_phone` varchar(32) DEFAULT NULL,
  `position` varchar(255) DEFAULT NULL,
  `parent_id` int(11) NOT NULL DEFAULT 0,
  `password_salt` varchar(64) DEFAULT NULL,
  `domain_id` int(11) NOT NULL DEFAULT 1,
  `mobile_device` varchar(255) DEFAULT NULL,
  `api_key` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `IX_login_name` (`login`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;

INSERT INTO `users` (`user_id`, `title`, `first_name`, `last_name`, `login`, `password`, `is_admin`, `user_groups`, `company`, `adress`, `city`, `email`, `PSC`, `country`, `phone`, `authorized`, `editable_groups`, `editable_pages`, `writable_folders`, `last_logon`, `module_perms`, `disabled_items`, `reg_date`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `date_of_birth`, `sex_male`, `photo`, `signature`, `forum_rank`, `rating_rank`, `allow_login_start`, `allow_login_end`, `authorize_hash`, `fax`, `delivery_first_name`, `delivery_last_name`, `delivery_company`, `delivery_adress`, `delivery_city`, `delivery_psc`, `delivery_country`, `delivery_phone`, `position`, `parent_id`, `password_salt`, `domain_id`, `mobile_device`, `api_key`)
VALUES
	(1,'','WebJET','Administrátor','admin','d7ed8dc6fc9b4a8c3b442c3dcc35bfe4',1,NULL,'InterWay, a. s.','','','web.spam@interway.sk','','Slovakia','02/32788888',1,'',NULL,NULL,NULL,NULL,NULL,'2024-10-24 08:49:51',NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,1,NULL,NULL);

/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table users_in_perm_groups
# ------------------------------------------------------------

DROP TABLE IF EXISTS `users_in_perm_groups`;

CREATE TABLE `users_in_perm_groups` (
  `user_id` int(11) NOT NULL,
  `perm_group_id` int(11) NOT NULL,
  PRIMARY KEY (`user_id`,`perm_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table zmluvy
# ------------------------------------------------------------

DROP TABLE IF EXISTS `zmluvy`;

CREATE TABLE `zmluvy` (
  `zmluvy_id` int(11) NOT NULL AUTO_INCREMENT,
  `cislo_zmluvy` varchar(128) DEFAULT NULL,
  `cislo_rzupsk` varchar(255) DEFAULT NULL,
  `nazov` varchar(512) DEFAULT NULL,
  `predmet` text DEFAULT NULL,
  `hodnota_zmluvy` double DEFAULT NULL,
  `mena` varchar(3) DEFAULT NULL,
  `podpisanie` datetime DEFAULT NULL,
  `platnost_od` datetime DEFAULT NULL,
  `ucinnost` datetime DEFAULT NULL,
  `zverejnenie` datetime DEFAULT NULL,
  `zdroj` varchar(255) DEFAULT NULL,
  `zdroj_nazov` varchar(255) DEFAULT NULL,
  `editor` int(11) DEFAULT NULL,
  `skupina` smallint(3) DEFAULT NULL,
  `status` smallint(6) DEFAULT NULL,
  `platnost_do` datetime DEFAULT NULL,
  `viazanost_k_zmluve` int(11) DEFAULT NULL,
  `zmluvna_strana1` varchar(255) DEFAULT NULL,
  `zs_nazov1` varchar(255) DEFAULT NULL,
  `zs_sidlo1` varchar(255) DEFAULT NULL,
  `zs_ico1` varchar(255) DEFAULT NULL,
  `zs_osoba1` varchar(255) DEFAULT NULL,
  `zmluvna_strana2` varchar(255) DEFAULT NULL,
  `zs_nazov2` varchar(255) DEFAULT NULL,
  `zs_sidlo2` varchar(255) DEFAULT NULL,
  `zs_ico2` varchar(255) DEFAULT NULL,
  `zs_osoba2` varchar(255) DEFAULT NULL,
  `zmluvna_strana3` varchar(255) DEFAULT NULL,
  `zs_nazov3` varchar(255) DEFAULT NULL,
  `zs_sidlo3` varchar(255) DEFAULT NULL,
  `zs_ico3` varchar(255) DEFAULT NULL,
  `zs_osoba3` varchar(255) DEFAULT NULL,
  `cena_s_dph` tinyint(1) DEFAULT 1,
  `dodatok` tinyint(1) DEFAULT 0,
  `poznamka` text DEFAULT NULL,
  `zobrazovat` tinyint(1) DEFAULT 1,
  `vytvoril` int(11) NOT NULL DEFAULT 0,
  `sposob_uhrady` varchar(255) DEFAULT NULL,
  `domain_id` int(6) DEFAULT 1,
  `organizacia_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`zmluvy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table zmluvy_organizacia
# ------------------------------------------------------------

DROP TABLE IF EXISTS `zmluvy_organizacia`;

CREATE TABLE `zmluvy_organizacia` (
  `zmluvy_organizacia_id` int(11) NOT NULL,
  `nazov` varchar(255) NOT NULL,
  `domain_id` int(11) DEFAULT 1,
  PRIMARY KEY (`zmluvy_organizacia_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

LOCK TABLES `zmluvy_organizacia` WRITE;
/*!40000 ALTER TABLE `zmluvy_organizacia` DISABLE KEYS */;

INSERT INTO `zmluvy_organizacia` (`zmluvy_organizacia_id`, `nazov`, `domain_id`)
VALUES
	(1,'default',1);

/*!40000 ALTER TABLE `zmluvy_organizacia` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table zmluvy_organizacia_approvers
# ------------------------------------------------------------

DROP TABLE IF EXISTS `zmluvy_organizacia_approvers`;

CREATE TABLE `zmluvy_organizacia_approvers` (
  `organizacia_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table zmluvy_organizacia_users
# ------------------------------------------------------------

DROP TABLE IF EXISTS `zmluvy_organizacia_users`;

CREATE TABLE `zmluvy_organizacia_users` (
  `organizacia_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



# Dump of table zmluvy_prilohy
# ------------------------------------------------------------

DROP TABLE IF EXISTS `zmluvy_prilohy`;

CREATE TABLE `zmluvy_prilohy` (
  `zmluvy_prilohy_id` int(11) NOT NULL AUTO_INCREMENT,
  `zmluvy_id` int(11) DEFAULT NULL,
  `zdroj` varchar(255) DEFAULT NULL,
  `nazov` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`zmluvy_prilohy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


INSERT INTO `domain_redirects` (`redirect_id`, `redirect_from`, `redirect_to`, `redirect_params`, `redirect_path`, `active`, `http_protocol`)
VALUES
	(2582, 'iwcm.interway.sk', 'demo.webjetcms.sk', 1, 1, 1, 'alias');
INSERT INTO `groups` (`group_id`, `group_name`, `internal`, `parent_group_id`, `navbar`, `default_doc_id`, `temp_id`, `sort_priority`, `password_protected`, `menu_type`, `url_dir_name`, `sync_id`, `sync_status`, `html_head`, `logon_page_doc_id`, `domain_name`, `new_page_docid_template`, `install_name`, `field_a`, `field_b`, `field_c`, `field_d`, `logged_menu_type`, `link_group_id`, `lng`, `hidden_in_admin`, `force_group_template`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_navbar`, `logged_show_in_sitemap`)
VALUES
	(67, 'Test stavov', 0, 0, 'Test stavov', 92, 4, 30, NULL, 2, 'test-stavov', 0, 1, '', 0, 'demo.webjetcms.sk', -1, '', '', '', '', '', -1, 0, 'sk', 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `documents` (`doc_id`, `title`, `data`, `data_asc`, `external_link`, `navbar`, `date_created`, `publish_start`, `publish_end`, `author_id`, `group_id`, `temp_id`, `views_total`, `views_month`, `searchable`, `available`, `cacheable`, `file_name`, `file_change`, `sort_priority`, `header_doc_id`, `menu_doc_id`, `footer_doc_id`, `password_protected`, `html_head`, `html_data`, `perex_place`, `perex_image`, `perex_group`, `show_in_menu`, `event_date`, `virtual_path`, `sync_id`, `sync_status`, `logon_page_doc_id`, `right_menu_doc_id`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`, `field_g`, `field_h`, `field_i`, `field_j`, `field_k`, `field_l`, `disable_after_end`, `forum_count`, `field_m`, `field_n`, `field_o`, `field_p`, `field_q`, `field_r`, `field_s`, `field_t`, `require_ssl`, `root_group_l1`, `root_group_l2`, `root_group_l3`, `temp_field_a_docid`, `temp_field_b_docid`, `temp_field_c_docid`, `temp_field_d_docid`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_menu`, `logged_show_in_navbar`, `logged_show_in_sitemap`, `url_inherit_group`, `generate_url_from_title`, `editor_virtual_path`, `publish_after_start`)
VALUES
	(92, 'Hlavná stránka adresára', '<p>&nbsp;</p>', '   &nbsp;    <h1>hlavna stranka adresara</h1>\n', '', 'Hlavná stránka adresára', '2020-12-12 17:05:33', '2020-07-14 22:53:00', NULL, 1, 67, 4, 0, 0, 1, 1, 0, '/Test stavov', NULL, 10, -1, -1, -1, NULL, '', NULL, '', '', NULL, 1, NULL, '/unknown.html', 0, 1, -1, -1, '', '', '', '', '', '', '', '', '', '', '', '', 0, 0, '', '', '', '', '', '', '', '', 0, 67, NULL, NULL, -1, -1, -1, -1, 1, 1, 1, 1, 1, 0, 0, NULL, 0);



/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;