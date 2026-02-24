# Databázový používateľ s právami iba na čítanie

V režime [cluster](README.md#cluster) môžete pre verejne dostupný uzol zvýšiť bezpečnosť nastavením databázového používateľa s právami iba na čítanie vybraných (citlivých) tabuliek. Okrem korektne nastavených práv na databázové tabuľky nastavte pre tento uzol nasledovné konfiguračné premenné:

- `clusterMyNodeType` - nastavte na `public`, aby tento uzol bežal bez administrácie a obmedzil niektoré zápisy do tabuliek.
- `monitoringEnableCountUsersOnAllNodes` - nastavte na `false` aby nedochádzalo k zápisu do tabuľky s konfiguráciou. Viac v [monitoringu servera](../../sysadmin/monitoring/README.md).

## Práva na tabuľky

Príklad nastavenia práv pre používateľa `WEB_WJ_APP` s právami iba na čítanie pre Oracle databázu. Používateľ `WEB_WJ` je používateľ s plnými právami a vlastník schémy s dátami.

```sql
GRANT EXECUTE ON CREATE_STAT_TABLE TO WEB_WJ_APP;

----
GRANT SELECT, UPDATE, INSERT ON ADMINLOG_NOTIFY TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON ADMIN_MESSAGE TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON ALARM_ACTION TO WEB_WJ_APP;
GRANT SELECT ON BANNER_BANNERS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON BANNER_STAT_CLICKS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON BANNER_STAT_VIEWS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON BANNER_STAT_VIEWS_DAY TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON BASKET_INVOICE TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON BASKET_INVOICE_PAYMENTS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON BASKET_ITEM TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON BAZAR_ADVERTISEMENTS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON BAZAR_GROUPS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON CACHE TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON CALENDAR TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON CALENDAR_INVITATION TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON CALENDAR_NAME_IN_YEAR TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON CALENDAR_TYPES TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON CAPTCHA_DICTIONARY TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON CHAT_ROOMS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON CLUSTER_MONITORING TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON CLUSTER_REFRESHER TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON CONTACT TO WEB_WJ_APP;
GRANT SELECT ON CRONTAB TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON CUSTOMER_REVIEWS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON DICTIONARY TO WEB_WJ_APP;
GRANT SELECT ON DIRPROP TO WEB_WJ_APP;
GRANT SELECT ON DOC_ATR TO WEB_WJ_APP;
GRANT SELECT ON DOC_ATR_DEF TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON DOC_SUBSCRIBE TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON DOCUMENT_FORUM TO WEB_WJ_APP;
GRANT SELECT ON DOCUMENT_NOTES TO WEB_WJ_APP;
GRANT SELECT, UPDATE ON DOCUMENTS TO WEB_WJ_APP;
GRANT SELECT ON DOCUMENTS_HISTORY TO WEB_WJ_APP;
GRANT SELECT, UPDATE ON DOMAIN_LIMITS TO WEB_WJ_APP;
GRANT SELECT ON DOMAIN_REDIRECTS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON EMAILS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON EMAILS_CAMPAIN TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON EMAILS_STAT_CLICK TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON EMAILS_UNSUBSCRIBED TO WEB_WJ_APP;
GRANT SELECT ON ENUM_COUNTRY TO WEB_WJ_APP;
GRANT SELECT ON EXPORT_DAT TO WEB_WJ_APP;
GRANT SELECT ON FILE_ARCHIV TO WEB_WJ_APP;
GRANT SELECT ON FILE_ATR TO WEB_WJ_APP;
GRANT SELECT ON FILE_ATR_DEF TO WEB_WJ_APP;
GRANT SELECT ON FILE_HISTORY TO WEB_WJ_APP;
GRANT SELECT ON FORM_ATTRIBUTES TO WEB_WJ_APP;
GRANT SELECT ON FORM_FILE_RESTRICTIONS TO WEB_WJ_APP;
GRANT SELECT ON FORM_REGULAR_EXP TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON FORMS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON FORMS_ARCHIVE TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON FORUM TO WEB_WJ_APP;
GRANT SELECT ON GALLERY TO WEB_WJ_APP;
GRANT SELECT ON GALLERY_DIMENSION TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON GIS TO WEB_WJ_APP;
GRANT SELECT ON GROUPS TO WEB_WJ_APP;
GRANT SELECT ON GROUPS_APPROVE TO WEB_WJ_APP;
GRANT SELECT ON GROUPS_SCHEDULER TO WEB_WJ_APP;
GRANT SELECT ON INQUIRY TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON INQUIRY_ANSWERS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON INQUIRY_USERS TO WEB_WJ_APP;
GRANT SELECT ON INSERT_SCRIPT TO WEB_WJ_APP;
GRANT SELECT ON INSERT_SCRIPT_DOC TO WEB_WJ_APP;
GRANT SELECT ON INSERT_SCRIPT_GR TO WEB_WJ_APP;

GRANT SELECT ON MEDIA TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON MONITORING TO WEB_WJ_APP;
GRANT SELECT ON MULTIGROUP_MAPPING TO WEB_WJ_APP;
GRANT SELECT ON PEREX_GROUP_DOC TO WEB_WJ_APP;
GRANT SELECT ON PEREX_GROUPS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON PKEY_GENERATOR TO WEB_WJ_APP;
GRANT SELECT ON PROXY TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON QUESTIONS_ANSWERS TO WEB_WJ_APP;
GRANT SELECT ON QUIZ TO WEB_WJ_APP;
GRANT SELECT ON QUIZ_ANSWERS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON QUIZ_QUESTIONS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON RATING TO WEB_WJ_APP;

GRANT SELECT, UPDATE, INSERT ON SEO_BOTS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON SEO_GOOGLE_POSITION TO WEB_WJ_APP;
GRANT SELECT ON SEO_KEYWORDS TO WEB_WJ_APP;

GRANT SELECT, UPDATE, INSERT ON SMS_ADDRESSBOOK TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON SMS_LOG TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON SMS_TEMPLATE TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON STAT_ERROR TO WEB_WJ_APP;

GRANT SELECT, UPDATE, INSERT, DELETE ON STAT_FROM TO WEB_WJ_APP;

GRANT SELECT, UPDATE, INSERT ON STAT_KEYS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON STAT_SEARCHENGINE TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON STAT_USERLOGON TO WEB_WJ_APP;

GRANT SELECT, UPDATE, INSERT ON STOPWORD TO WEB_WJ_APP;
GRANT SELECT ON TEMPLATES TO WEB_WJ_APP;

GRANT SELECT, UPDATE, INSERT ON TODO TO WEB_WJ_APP;
GRANT SELECT ON URL_REDIRECT TO WEB_WJ_APP;
GRANT SELECT ON USER_ALARM TO WEB_WJ_APP;
GRANT SELECT ON USER_DISABLED_ITEMS TO WEB_WJ_APP;
GRANT SELECT ON USER_GROUPS TO WEB_WJ_APP;
GRANT SELECT ON USER_GROUP_VERIFY TO WEB_WJ_APP;
GRANT SELECT ON USER_PERM_GROUPS TO WEB_WJ_APP;
GRANT SELECT ON USER_PERM_GROUPS_PERMS TO WEB_WJ_APP;
GRANT SELECT, UPDATE ON USERS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT, DELETE ON USER_SETTINGS TO WEB_WJ_APP;

GRANT SELECT ON USERS_IN_PERM_GROUPS TO WEB_WJ_APP;
GRANT SELECT, UPDATE, INSERT ON WEBJET_ADMINLOG TO WEB_WJ_APP;
GRANT SELECT ON WEBJET_CONF TO WEB_WJ_APP;
GRANT SELECT  ON WEBJET_CONF_PREPARED TO WEB_WJ_APP;
GRANT SELECT ON WEBJET_DB TO WEB_WJ_APP;
GRANT SELECT ON WEBJET_MODULES TO WEB_WJ_APP;
GRANT SELECT ON WEBJET_PROPERTIES TO WEB_WJ_APP;
-----

BEGIN
  FOR x IN
   (SELECT 'GRANT SELECT, UPDATE, INSERT, DELETE ON ' || table_name || ' TO WEB_WJ_APP' AS grant_code
    FROM all_tables
    WHERE owner = 'WEB_TB_WJ'
      AND table_name like 'STAT_VIEWS%')
  LOOP
    EXECUTE IMMEDIATE x.grant_code;
  END LOOP;

  FOR x IN
   (SELECT 'GRANT SELECT, UPDATE, INSERT, DELETE ON ' || table_name || ' TO WEB_WJ_APP' AS grant_code
    FROM all_tables
    WHERE owner = 'WEB_TB_WJ'
      AND table_name like 'STAT_ERROR%')
  LOOP
    EXECUTE IMMEDIATE x.grant_code;
  END LOOP;

  FOR x IN
   (SELECT 'GRANT SELECT, UPDATE, INSERT, DELETE ON ' || table_name || ' TO WEB_WJ_APP' AS grant_code
    FROM all_tables
    WHERE owner = 'WEB_TB_WJ'
      AND table_name like 'STAT_FROM%')
  LOOP
    EXECUTE IMMEDIATE x.grant_code;
  END LOOP;


END;

```

## Oracle

Pre Oracle databázu je potrebné vytvoriť `trigger` pre zmenu schémy po prihlásení a nastaviť procedúru pre vytvorenie štatistických tabuliek.

```sql
CREATE OR REPLACE TRIGGER WEB_WJ_APP.tgg_after_logon
AFTER LOGON ON WEB_WJ_APP.SCHEMA
DECLARE
BEGIN
  EXECUTE IMMEDIATE 'ALTER SESSION SET current_schema = WEB_WJ';
END;

CREATE OR REPLACE PROCEDURE create_stat_table (
  iName VARCHAR2,
  iSuffix VARCHAR2
)
IS
  lv_table VARCHAR(4000);
  lv_table_name VARCHAR(50);
  lv_sequence VARCHAR(1000);
  lv_trigger VARCHAR(1000);
  lv_index VARCHAR(1000);
BEGIN
  -- check suffix (start with "_" then numbers and "_" only)
  IF NOT REGEXP_LIKE(iSuffix, '^_+[0-9_]*$') THEN
    RAISE_APPLICATION_ERROR(-20001, 'Invalid suffix');
    RETURN;
  END IF;

  IF (UPPER(iName) = 'STAT_ERROR') THEN
    lv_table_name := 'stat_error' || iSuffix;
    lv_table := 'CREATE TABLE ' || lv_table_name || '( ' ||
                ' year INTEGER NOT NULL,' ||
                ' week INTEGER NOT NULL,' ||
                ' url nvarchar2(255),' ||
                ' query_string nvarchar2(255),' ||
                ' count INTEGER DEFAULT 0,' ||
                ' browser_ua_id INTEGER DEFAULT 0,' ||
                ' domain_id INTEGER DEFAULT 0' ||
                ')';
    lv_index := 'CREATE INDEX IX_ywse' || iSuffix || ' ON ' || lv_table_name || ' (year, week)';
    lv_sequence := NULL;
    lv_trigger := NULL;
  ELSIF (UPPER(iName) = 'STAT_FROM') THEN
    lv_table_name := 'stat_from' || iSuffix;
    lv_table := 'CREATE TABLE ' || lv_table_name || '( ' ||
                ' from_id INTEGER NOT NULL,' ||
                ' browser_id INTEGER,' ||
                ' session_id INTEGER,' ||
                ' referer_server_name nvarchar2(255),' ||
                ' referer_url nvarchar2(255),' ||
                ' from_time DATE,' ||
                ' doc_id INTEGER,' ||
                ' group_id INTEGER NOT NULL,' ||
                ' CONSTRAINT pk_stat_from' || iSuffix || ' PRIMARY KEY (from_id)' ||
                ')';
    lv_index := NULL;
    lv_sequence := 'CREATE SEQUENCE S_stat_from' || iSuffix || ' START WITH 1';
    lv_trigger := 'CREATE TRIGGER T_stat_from' || iSuffix || CHR(13) || CHR(10) ||
                  'BEFORE INSERT ON ' || lv_table_name || CHR(13) || CHR(10) ||
                  'FOR EACH ROW ' || CHR(13) || CHR(10) ||
                  'DECLARE ' || CHR(13) || CHR(10) ||
                  '  val INTEGER; ' || CHR(13) || CHR(10) ||
                  'BEGIN ' || CHR(13) || CHR(10) ||
                  '  IF :new.from_id IS NULL THEN ' || CHR(13) || CHR(10) ||
                  '    SELECT S_stat_from' || iSuffix || '.nextval into val FROM dual; ' || CHR(13) || CHR(10) ||
                  '    :new.from_id:= val; ' || CHR(13) || CHR(10) ||
                  '  END IF; ' || CHR(13) || CHR(10) ||
                  'END;';
  ELSIF (UPPER(iName) = 'STAT_SEARCHENGINE') THEN
    lv_table_name := 'stat_searchengine' || iSuffix;
    lv_table := 'CREATE TABLE ' || lv_table_name || '( ' ||
                ' search_date date NOT NULL, ' ||
                ' server nvarchar2(16) NOT NULL, ' ||
                ' query nvarchar2(64) NOT NULL, ' ||
                ' doc_id integer NOT NULL, ' ||
                ' remote_host nvarchar2(255), ' ||
                ' group_id integer NOT NULL' ||
                ')';
    lv_index := NULL;
    lv_sequence := NULL;
    lv_trigger := NULL;
  ELSIF (UPPER(iName) = 'STAT_VIEWS') THEN
    lv_table_name := 'stat_views' || iSuffix;
    lv_table := 'CREATE TABLE ' || lv_table_name || '( ' ||
                ' view_id INTEGER NOT NULL, ' ||
                ' browser_id INTEGER, ' ||
                ' session_id INTEGER, ' ||
                ' doc_id INTEGER, ' ||
                ' last_doc_id INTEGER, ' ||
                ' view_time DATE NULL, ' ||
                ' group_id INTEGER, ' ||
                ' last_group_id INTEGER, ' ||
                ' browser_ua_id INTEGER, ' ||
                ' platform_id INTEGER, ' ||
                ' subplatform_id INTEGER, ' ||
                ' country VARCHAR(4), ' ||
                ' CONSTRAINT pk_stat_views' || iSuffix || ' PRIMARY KEY (view_id)' ||
                ')';
    lv_index := NULL;
    lv_sequence := 'CREATE SEQUENCE S_stat_views' || iSuffix || ' START WITH 1';
    lv_trigger := 'CREATE TRIGGER T_stat_views' || iSuffix || CHR(13) || CHR(10) ||
                  'BEFORE INSERT ON ' || lv_table_name || CHR(13) || CHR(10) ||
                  'FOR EACH ROW ' || CHR(13) || CHR(10) ||
                  'DECLARE ' || CHR(13) || CHR(10) ||
                  '  val INTEGER; ' || CHR(13) || CHR(10) ||
                  'BEGIN ' || CHR(13) || CHR(10) ||
                  '  IF :new.view_id IS NULL THEN ' || CHR(13) || CHR(10) ||
                  '    SELECT S_stat_views' || iSuffix || '.nextval into val FROM dual; ' || CHR(13) || CHR(10) ||
                  '    :new.view_id:= val; ' || CHR(13) || CHR(10) ||
                  '  END IF; ' || CHR(13) || CHR(10) ||
                  'END;';
  ELSIF (UPPER(iName) = 'STAT_CLICKS') THEN
    lv_table_name := 'stat_clicks' || iSuffix;
    lv_table := 'CREATE TABLE ' || lv_table_name || '( ' ||
                ' stat_click_id INT NOT NULL, ' ||
                ' document_id INTEGER, ' ||
                ' x INTEGER, ' ||
                ' y INTEGER, ' ||
                ' day_of_month INTEGER' ||
                ')';
    lv_index := 'CREATE INDEX to_document_' || iSuffix || ' ON ' || lv_table_name || ' (document_id)';
    lv_sequence := 'CREATE SEQUENCE S_stat_clicks' || iSuffix || ' START WITH 1';
    lv_trigger := 'CREATE TRIGGER T_stat_clicks' || iSuffix || CHR(13) || CHR(10) ||
                  'BEFORE INSERT ON ' || lv_table_name || CHR(13) || CHR(10) ||
                  'FOR EACH ROW ' || CHR(13) || CHR(10) ||
                  'DECLARE ' || CHR(13) || CHR(10) ||
                  '  val INTEGER; ' || CHR(13) || CHR(10) ||
                  'BEGIN ' || CHR(13) || CHR(10) ||
                  '  IF :new.stat_click_id IS NULL THEN ' || CHR(13) || CHR(10) ||
                  '    SELECT S_stat_clicks' || iSuffix || '.nextval into val FROM dual; ' || CHR(13) || CHR(10) ||
                  '    :new.stat_click_id:= val; ' || CHR(13) || CHR(10) ||
                  '  END IF; ' || CHR(13) || CHR(10) ||
                  'END;';
  ELSE
    RAISE_APPLICATION_ERROR(-20001, 'Invalid table name');
    RETURN;
  END IF;

  IF (lv_table IS NOT NULL) THEN
    EXECUTE IMMEDIATE lv_table;
    EXECUTE IMMEDIATE 'GRANT SELECT, UPDATE, INSERT, DELETE ON ' || lv_table || ' TO WEB_WJ_APP';
  END IF;

  IF (lv_index IS NOT NULL) THEN
    EXECUTE IMMEDIATE lv_index;
  END IF;

  IF (lv_sequence IS NOT NULL) THEN
    EXECUTE IMMEDIATE lv_sequence;
  END IF;

  IF (lv_trigger IS NOT NULL) THEN
    EXECUTE IMMEDIATE lv_trigger;
  END IF;

END;
```