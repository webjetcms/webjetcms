

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;


CREATE SCHEMA IF NOT EXISTS "webjet_cms";

CREATE TABLE "webjet_cms"."_adminlog_" (
    "log_id" integer NOT NULL,
    "log_type" integer DEFAULT 0,
    "user_id" integer DEFAULT '-1'::integer NOT NULL,
    "create_date" timestamp with time zone NOT NULL,
    "description" "text",
    "sub_id1" integer DEFAULT 0,
    "sub_id2" integer DEFAULT 0,
    "ip" character varying(128) DEFAULT NULL::character varying,
    "hostname" character varying(255) DEFAULT NULL::character varying,
    "operation_type" integer
);



CREATE SEQUENCE "webjet_cms"."_adminlog__log_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."_adminlog__log_id_seq" OWNED BY "webjet_cms"."_adminlog_"."log_id";



CREATE TABLE "webjet_cms"."_conf_" (
    "name" character varying(255) DEFAULT ''::character varying NOT NULL,
    "value" "text" NOT NULL,
    "date_changed" timestamp with time zone
);



CREATE TABLE "webjet_cms"."_conf_prepared_" (
    "name" character varying(255) DEFAULT ''::character varying NOT NULL,
    "value" "text" NOT NULL,
    "date_changed" timestamp with time zone,
    "date_prepared" timestamp with time zone,
    "user_id" integer,
    "id" integer NOT NULL,
    "date_published" timestamp with time zone
);



CREATE SEQUENCE "webjet_cms"."_conf_prepared__id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."_conf_prepared__id_seq" OWNED BY "webjet_cms"."_conf_prepared_"."id";



CREATE TABLE "webjet_cms"."_db_" (
    "id" integer NOT NULL,
    "create_date" "date" DEFAULT '2000-01-01'::"date" NOT NULL,
    "note" character varying(255) DEFAULT ''::character varying NOT NULL
);



CREATE SEQUENCE "webjet_cms"."_db__id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."_db__id_seq" OWNED BY "webjet_cms"."_db_"."id";



CREATE TABLE "webjet_cms"."_modules_" (
    "module_id" integer NOT NULL,
    "name_key" character varying(128) DEFAULT ''::character varying NOT NULL,
    "item_key" character varying(64) DEFAULT ''::character varying NOT NULL,
    "path" character varying(255) DEFAULT ''::character varying NOT NULL,
    "menu_order" integer DEFAULT '900'::integer
);



CREATE SEQUENCE "webjet_cms"."_modules__module_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."_modules__module_id_seq" OWNED BY "webjet_cms"."_modules_"."module_id";



CREATE TABLE "webjet_cms"."_properties_" (
    "prop_key" character varying(255) DEFAULT ''::character varying NOT NULL,
    "lng" character(3) DEFAULT ''::"bpchar" NOT NULL,
    "prop_value" "text",
    "update_date" timestamp with time zone,
    "id" integer NOT NULL
);



CREATE SEQUENCE "webjet_cms"."_properties__id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."_properties__id_seq" OWNED BY "webjet_cms"."_properties_"."id";



CREATE TABLE "webjet_cms"."admin_message" (
    "admin_message_id" integer NOT NULL,
    "create_date" timestamp with time zone NOT NULL,
    "create_by_user_id" integer NOT NULL,
    "recipient_user_id" integer NOT NULL,
    "only_for_logged" boolean DEFAULT false NOT NULL,
    "message_text" "text" NOT NULL,
    "is_readed" boolean DEFAULT false
);



CREATE TABLE "webjet_cms"."adminlog_notify" (
    "adminlog_notify_id" integer NOT NULL,
    "adminlog_type" integer,
    "email" character varying(255) DEFAULT NULL::character varying,
    "text" character varying(255) DEFAULT NULL::character varying
);



CREATE SEQUENCE "webjet_cms"."adminlog_notify_adminlog_notify_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."adminlog_notify_adminlog_notify_id_seq" OWNED BY "webjet_cms"."adminlog_notify"."adminlog_notify_id";



CREATE TABLE "webjet_cms"."alarm_action" (
    "alarm_id" integer DEFAULT 0 NOT NULL,
    "days" integer,
    "doc_id" integer
);



CREATE TABLE "webjet_cms"."appcache" (
    "id" integer NOT NULL,
    "path" character varying(255) DEFAULT NULL::character varying,
    "filename" character varying(255) DEFAULT NULL::character varying,
    "version" integer,
    "created" timestamp with time zone
);



CREATE TABLE "webjet_cms"."appcache_file" (
    "id" integer NOT NULL,
    "path" character varying(255) DEFAULT NULL::character varying,
    "filename" character varying(255) DEFAULT NULL::character varying,
    "extension" character varying(255) DEFAULT NULL::character varying,
    "created" timestamp with time zone,
    "appcache_id" integer NOT NULL,
    "is_recursive" boolean
);



CREATE SEQUENCE "webjet_cms"."appcache_file_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."appcache_file_id_seq" OWNED BY "webjet_cms"."appcache_file"."id";



CREATE SEQUENCE "webjet_cms"."appcache_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."appcache_id_seq" OWNED BY "webjet_cms"."appcache"."id";



CREATE TABLE "webjet_cms"."appcache_page" (
    "id" integer NOT NULL,
    "path" character varying(255) DEFAULT NULL::character varying,
    "created" timestamp with time zone,
    "appcache_id" integer NOT NULL,
    "is_recursive" boolean
);



CREATE SEQUENCE "webjet_cms"."appcache_page_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."appcache_page_id_seq" OWNED BY "webjet_cms"."appcache_page"."id";



CREATE TABLE "webjet_cms"."banner_banners" (
    "banner_id" integer NOT NULL,
    "banner_type" integer DEFAULT '1'::integer NOT NULL,
    "banner_group" character varying(128) DEFAULT NULL::character varying,
    "priority" integer DEFAULT '1'::integer,
    "active" boolean DEFAULT false NOT NULL,
    "banner_location" character varying(255) DEFAULT NULL::character varying,
    "banner_redirect" character varying(255) DEFAULT NULL::character varying,
    "width" integer,
    "height" integer,
    "html_code" "text",
    "date_from" timestamp with time zone,
    "date_to" timestamp with time zone,
    "max_views" integer,
    "max_clicks" integer,
    "stat_views" integer DEFAULT '0'::integer,
    "stat_clicks" integer DEFAULT '0'::integer,
    "stat_date" timestamp with time zone,
    "name" character varying(255) DEFAULT NULL::character varying,
    "target" character varying(16) DEFAULT NULL::character varying,
    "click_tag" character varying(32) DEFAULT NULL::character varying,
    "frame_rate" integer,
    "client_id" integer DEFAULT '-1'::integer,
    "domain_id" integer DEFAULT '1'::integer,
    "visitor_cookie_group" character varying(100) DEFAULT NULL::character varying,
    "image_link" character varying(255) DEFAULT NULL::character varying,
    "image_link_mobile" character varying(255) DEFAULT NULL::character varying,
    "primary_header" character varying(255) DEFAULT NULL::character varying,
    "secondary_header" character varying(255) DEFAULT NULL::character varying,
    "description_text" "text",
    "primary_link_title" character varying(255) DEFAULT NULL::character varying,
    "primary_link_url" character varying(255) DEFAULT NULL::character varying,
    "primary_link_target" character varying(7) DEFAULT NULL::character varying,
    "secondary_link_title" character varying(255) DEFAULT NULL::character varying,
    "secondary_link_url" character varying(255) DEFAULT NULL::character varying,
    "secondary_link_target" character varying(7) DEFAULT NULL::character varying,
    "campaign_title" character varying(255) DEFAULT NULL::character varying,
    "only_with_campaign" boolean DEFAULT false,
    "field_a" character varying(255) DEFAULT NULL::character varying,
    "field_b" character varying(255) DEFAULT NULL::character varying,
    "field_c" character varying(255) DEFAULT NULL::character varying,
    "field_d" character varying(255) DEFAULT NULL::character varying,
    "field_e" character varying(255) DEFAULT NULL::character varying,
    "field_f" character varying(255) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."banner_doc" (
    "id" integer NOT NULL,
    "doc_id" integer,
    "banner_id" integer
);



CREATE SEQUENCE "webjet_cms"."banner_doc_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."banner_doc_id_seq" OWNED BY "webjet_cms"."banner_doc"."id";



CREATE TABLE "webjet_cms"."banner_gr" (
    "id" integer NOT NULL,
    "group_id" integer,
    "banner_id" integer
);



CREATE SEQUENCE "webjet_cms"."banner_gr_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."banner_gr_id_seq" OWNED BY "webjet_cms"."banner_gr"."id";



CREATE TABLE "webjet_cms"."banner_stat_clicks" (
    "id" integer NOT NULL,
    "banner_id" integer DEFAULT 0 NOT NULL,
    "insert_date" timestamp with time zone,
    "ip" character varying(16) DEFAULT NULL::character varying,
    "host" character varying(128) DEFAULT NULL::character varying,
    "clicks" integer DEFAULT 1,
    "domain_id" integer DEFAULT '1'::integer
);



CREATE SEQUENCE "webjet_cms"."banner_stat_clicks_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."banner_stat_clicks_id_seq" OWNED BY "webjet_cms"."banner_stat_clicks"."id";



CREATE TABLE "webjet_cms"."banner_stat_views" (
    "id" integer NOT NULL,
    "banner_id" integer DEFAULT 0 NOT NULL,
    "insert_date" timestamp with time zone,
    "ip" character varying(16) DEFAULT NULL::character varying,
    "host" character varying(128) DEFAULT NULL::character varying,
    "views" integer DEFAULT 1,
    "domain_id" integer DEFAULT '1'::integer
);



CREATE TABLE "webjet_cms"."banner_stat_views_day" (
    "view_id" integer NOT NULL,
    "banner_id" integer NOT NULL,
    "insert_date" "date" NOT NULL,
    "views" integer DEFAULT '1'::integer NOT NULL,
    "domain_id" integer DEFAULT '1'::integer
);



CREATE SEQUENCE "webjet_cms"."banner_stat_views_day_view_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."banner_stat_views_day_view_id_seq" OWNED BY "webjet_cms"."banner_stat_views_day"."view_id";



CREATE SEQUENCE "webjet_cms"."banner_stat_views_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."banner_stat_views_id_seq" OWNED BY "webjet_cms"."banner_stat_views"."id";



CREATE TABLE "webjet_cms"."basket_invoice" (
    "basket_invoice_id" integer NOT NULL,
    "browser_id" bigint DEFAULT '0'::bigint NOT NULL,
    "logged_user_id" integer NOT NULL,
    "create_date" timestamp with time zone NOT NULL,
    "status_id" integer,
    "delivery_company" character varying(255) DEFAULT NULL::character varying,
    "delivery_name" character varying(255) DEFAULT NULL::character varying,
    "delivery_street" character varying(255) DEFAULT NULL::character varying,
    "delivery_city" character varying(255) DEFAULT NULL::character varying,
    "delivery_zip" character varying(10) DEFAULT NULL::character varying,
    "delivery_country" character varying(255) DEFAULT NULL::character varying,
    "internal_invoice_id" character varying(255) DEFAULT NULL::character varying,
    "user_note" "text",
    "user_lng" character varying(6) DEFAULT NULL::character varying,
    "payment_method" character varying(255) DEFAULT NULL::character varying,
    "delivery_method" character varying(255) DEFAULT NULL::character varying,
    "contact_title" character varying(32) DEFAULT NULL::character varying,
    "contact_first_name" character varying(255) DEFAULT NULL::character varying,
    "contact_last_name" character varying(255) DEFAULT NULL::character varying,
    "contact_email" character varying(255) DEFAULT NULL::character varying,
    "contact_phone" character varying(255) DEFAULT NULL::character varying,
    "contact_company" character varying(255) DEFAULT NULL::character varying,
    "contact_street" character varying(255) DEFAULT NULL::character varying,
    "contact_city" character varying(255) DEFAULT NULL::character varying,
    "contact_zip" character varying(10) DEFAULT NULL::character varying,
    "contact_country" character varying(255) DEFAULT NULL::character varying,
    "contact_ico" character varying(32) DEFAULT NULL::character varying,
    "contact_icdph" character varying(32) DEFAULT NULL::character varying,
    "contact_dic" character varying(32) DEFAULT NULL::character varying,
    "field_a" character varying(255) DEFAULT NULL::character varying,
    "field_b" character varying(255) DEFAULT NULL::character varying,
    "field_c" character varying(255) DEFAULT NULL::character varying,
    "field_d" character varying(255) DEFAULT NULL::character varying,
    "field_e" character varying(255) DEFAULT NULL::character varying,
    "field_f" character varying(255) DEFAULT NULL::character varying,
    "html_code" "text",
    "currency" character varying(8) DEFAULT NULL::character varying,
    "domain_id" integer DEFAULT '1'::integer NOT NULL,
    "delivery_surname" character varying(255) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."basket_invoice_payments" (
    "payment_id" integer NOT NULL,
    "invoice_id" integer NOT NULL,
    "create_date" timestamp with time zone NOT NULL,
    "payed_price" numeric(10,2) DEFAULT 0.00 NOT NULL,
    "payment_method" character varying(255) DEFAULT NULL::character varying,
    "closed_date" timestamp with time zone,
    "confirmed" boolean
);



CREATE SEQUENCE "webjet_cms"."basket_invoice_payments_payment_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."basket_invoice_payments_payment_id_seq" OWNED BY "webjet_cms"."basket_invoice_payments"."payment_id";



CREATE TABLE "webjet_cms"."basket_item" (
    "basket_item_id" integer NOT NULL,
    "browser_id" bigint DEFAULT '0'::bigint NOT NULL,
    "logged_user_id" integer NOT NULL,
    "item_id" integer NOT NULL,
    "item_title" character varying(255) DEFAULT NULL::character varying,
    "item_part_no" character varying(255) DEFAULT NULL::character varying,
    "item_price" double precision NOT NULL,
    "item_vat" double precision,
    "item_qty" integer NOT NULL,
    "item_note" character varying(255) DEFAULT NULL::character varying,
    "date_insert" timestamp with time zone NOT NULL,
    "basket_invoice_id" integer,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE TABLE "webjet_cms"."bazar_advertisements" (
    "ad_id" integer NOT NULL,
    "group_id" integer,
    "user_id" integer,
    "description" "text",
    "contact" character varying(255) DEFAULT NULL::character varying,
    "confirmation" boolean,
    "image" character varying(255) DEFAULT NULL::character varying,
    "price" character varying(255) DEFAULT NULL::character varying,
    "date_insert" timestamp with time zone
);



CREATE SEQUENCE "webjet_cms"."bazar_advertisements_ad_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."bazar_advertisements_ad_id_seq" OWNED BY "webjet_cms"."bazar_advertisements"."ad_id";



CREATE TABLE "webjet_cms"."bazar_groups" (
    "group_id" integer NOT NULL,
    "parent_group_id" integer,
    "group_name" character varying(255) DEFAULT NULL::character varying,
    "allow_ad_insert" boolean,
    "require_approve" boolean
);



CREATE SEQUENCE "webjet_cms"."bazar_groups_group_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."bazar_groups_group_id_seq" OWNED BY "webjet_cms"."bazar_groups"."group_id";



CREATE TABLE "webjet_cms"."cache" (
    "cache_id" integer NOT NULL,
    "data_type" integer NOT NULL,
    "data_value" character varying(2000) DEFAULT NULL::character varying,
    "data_result" "text",
    "refresh_minutes" integer,
    "next_refresh" timestamp with time zone
);



CREATE TABLE "webjet_cms"."calendar" (
    "calendar_id" integer NOT NULL,
    "title" "text" NOT NULL,
    "description" "text",
    "date_from" timestamp with time zone DEFAULT '2000-01-01 00:00:00+01'::timestamp with time zone NOT NULL,
    "date_to" timestamp with time zone DEFAULT '2000-01-01 00:00:00+01'::timestamp with time zone NOT NULL,
    "type_id" integer DEFAULT 0 NOT NULL,
    "time_range" character varying(128) DEFAULT NULL::character varying,
    "area" character varying(255) DEFAULT NULL::character varying,
    "city" character varying(255) DEFAULT NULL::character varying,
    "address" character varying(255) DEFAULT NULL::character varying,
    "info_1" character varying(255) DEFAULT NULL::character varying,
    "info_2" character varying(255) DEFAULT NULL::character varying,
    "info_3" character varying(255) DEFAULT NULL::character varying,
    "info_4" character varying(255) DEFAULT NULL::character varying,
    "info_5" character varying(255) DEFAULT NULL::character varying,
    "notify_hours_before" integer DEFAULT 0,
    "notify_emails" "text",
    "notify_sender" character varying(255) DEFAULT NULL::character varying,
    "notify_sent" boolean DEFAULT false NOT NULL,
    "notify_introtext" "text",
    "notify_sendsms" boolean DEFAULT false,
    "lng" character(3) DEFAULT NULL::"bpchar",
    "creator_id" integer DEFAULT '-1'::integer NOT NULL,
    "approve" integer DEFAULT 1 NOT NULL,
    "suggest" boolean DEFAULT false NOT NULL,
    "domain_id" integer DEFAULT '1'::integer NOT NULL,
    "hash_string" character varying(32) DEFAULT NULL::character varying
);



CREATE SEQUENCE "webjet_cms"."calendar_calendar_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."calendar_calendar_id_seq" OWNED BY "webjet_cms"."calendar"."calendar_id";



CREATE TABLE "webjet_cms"."calendar_invitation" (
    "calendar_invitation_id" integer NOT NULL,
    "calendar_id" integer NOT NULL,
    "user_id" integer NOT NULL,
    "sent_date" timestamp with time zone NOT NULL,
    "status_date" timestamp with time zone,
    "status" character(1) NOT NULL,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE SEQUENCE "webjet_cms"."calendar_invitation_calendar_invitation_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."calendar_invitation_calendar_invitation_id_seq" OWNED BY "webjet_cms"."calendar_invitation"."calendar_invitation_id";



CREATE TABLE "webjet_cms"."calendar_name_in_year" (
    "day" integer DEFAULT 0 NOT NULL,
    "month" integer DEFAULT 0 NOT NULL,
    "name" character varying(200) DEFAULT NULL::character varying,
    "calendar_id" integer NOT NULL,
    "lng" character varying(10) DEFAULT 'SK'::character varying NOT NULL
);



CREATE SEQUENCE "webjet_cms"."calendar_name_in_year_calendar_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."calendar_name_in_year_calendar_id_seq" OWNED BY "webjet_cms"."calendar_name_in_year"."calendar_id";



CREATE TABLE "webjet_cms"."calendar_types" (
    "type_id" integer NOT NULL,
    "name" character varying(128) DEFAULT ''::character varying NOT NULL,
    "schvalovatel_id" integer DEFAULT '-1'::integer NOT NULL,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE SEQUENCE "webjet_cms"."calendar_types_type_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."calendar_types_type_id_seq" OWNED BY "webjet_cms"."calendar_types"."type_id";



CREATE TABLE "webjet_cms"."captcha_dictionary" (
    "id" integer NOT NULL,
    "word" character varying(255) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."chat_rooms" (
    "room_id" integer NOT NULL,
    "room_name" character varying(255) NOT NULL,
    "room_description" "text",
    "room_class" character varying(255) NOT NULL,
    "max_users" integer DEFAULT 50 NOT NULL,
    "allow_similar_names" boolean DEFAULT false NOT NULL,
    "lng" character varying(3) DEFAULT 'sk'::character varying NOT NULL,
    "moderator_name" character varying(128) DEFAULT NULL::character varying,
    "moderator_username" character varying(64) DEFAULT NULL::character varying,
    "moderator_password" character varying(64) DEFAULT NULL::character varying,
    "hide_in_public_list" boolean DEFAULT false NOT NULL
);



COMMENT ON TABLE "webjet_cms"."chat_rooms" IS 'zoznam izieb pre modul chat';



CREATE SEQUENCE "webjet_cms"."chat_rooms_room_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."chat_rooms_room_id_seq" OWNED BY "webjet_cms"."chat_rooms"."room_id";



CREATE TABLE "webjet_cms"."cluster_monitoring" (
    "node" character varying(64) NOT NULL,
    "type" character varying(64) NOT NULL,
    "content" "text",
    "created_at" timestamp with time zone
);



CREATE TABLE "webjet_cms"."cluster_refresher" (
    "cluster_refresh_id" integer NOT NULL,
    "node_name" character varying(255) DEFAULT NULL::character varying,
    "class_name" character varying(255) DEFAULT NULL::character varying,
    "refresh_time" timestamp with time zone
);



CREATE SEQUENCE "webjet_cms"."cluster_refresher_cluster_refresh_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."cluster_refresher_cluster_refresh_id_seq" OWNED BY "webjet_cms"."cluster_refresher"."cluster_refresh_id";



CREATE TABLE "webjet_cms"."contact" (
    "contact_id" integer NOT NULL,
    "name" character varying(255) DEFAULT NULL::character varying,
    "ico" character varying(16) DEFAULT NULL::character varying,
    "vatid" character varying(32) DEFAULT NULL::character varying,
    "street" character varying(128) DEFAULT NULL::character varying,
    "city" character varying(64) DEFAULT NULL::character varying,
    "zip" character varying(8) DEFAULT NULL::character varying,
    "country" character varying(32) DEFAULT NULL::character varying,
    "contact" character varying(255) DEFAULT NULL::character varying,
    "phone" character varying(32) DEFAULT NULL::character varying,
    "fax" character varying(32) DEFAULT NULL::character varying,
    "email" character varying(64) DEFAULT NULL::character varying,
    "web" character varying(255) DEFAULT NULL::character varying,
    "gps" character varying(64) DEFAULT NULL::character varying,
    "category" character varying(255) DEFAULT NULL::character varying,
    "field_a" character varying(255) DEFAULT NULL::character varying,
    "field_b" character varying(255) DEFAULT NULL::character varying,
    "field_c" character varying(255) DEFAULT NULL::character varying,
    "field_d" character varying(255) DEFAULT NULL::character varying,
    "field_e" character varying(255) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."cookies" (
    "cookie_id" integer NOT NULL,
    "domain_id" integer,
    "user_id" integer,
    "save_date" timestamp with time zone,
    "cookie_name" character varying(255) DEFAULT NULL::character varying,
    "description" character varying(1000) DEFAULT NULL::character varying,
    "provider" character varying(255) DEFAULT NULL::character varying,
    "purpouse" character varying(255) DEFAULT NULL::character varying,
    "validity" character varying(255) DEFAULT NULL::character varying,
    "type" character varying(255) DEFAULT NULL::character varying,
    "classification" character varying(255) DEFAULT NULL::character varying,
    "application" character varying(255) DEFAULT NULL::character varying,
    "typical_value" character varying(255) DEFAULT NULL::character varying,
    "party_3rd" boolean
);



CREATE TABLE "webjet_cms"."crontab" (
    "id" integer NOT NULL,
    "task_name" character varying(64) DEFAULT NULL::character varying,
    "second" character varying(64) DEFAULT '0'::character varying,
    "minute" character varying(64) DEFAULT '*'::character varying,
    "hour" character varying(64) DEFAULT '*'::character varying,
    "dayofmonth" character varying(64) DEFAULT '*'::character varying,
    "month" character varying(64) DEFAULT '*'::character varying,
    "dayofweek" character varying(64) DEFAULT '*'::character varying,
    "year" character varying(64) DEFAULT '*'::character varying,
    "task" character varying(255) DEFAULT NULL::character varying,
    "extrainfo" character varying(255) DEFAULT NULL::character varying,
    "businessdays" character varying(6) DEFAULT 'true'::character varying,
    "cluster_node" character varying(64) DEFAULT NULL::character varying,
    "audit_task" character varying(6) DEFAULT NULL::character varying,
    "run_at_startup" boolean DEFAULT false,
    "enable_task" boolean DEFAULT true
);



CREATE SEQUENCE "webjet_cms"."crontab_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."crontab_id_seq" OWNED BY "webjet_cms"."crontab"."id";



CREATE TABLE "webjet_cms"."customer_reviews" (
    "review_id" integer NOT NULL,
    "stars" integer,
    "name" character varying(255) DEFAULT NULL::character varying,
    "surname" character varying(255) DEFAULT NULL::character varying,
    "mail" character varying(255) DEFAULT NULL::character varying,
    "description" "text",
    "url" character varying(255) DEFAULT NULL::character varying,
    "date" "date",
    "show_to_public" boolean,
    "approve_hash" character varying(255) DEFAULT NULL::character varying,
    "domain_id" integer
);



CREATE TABLE "webjet_cms"."dictionary" (
    "dictionary_id" integer NOT NULL,
    "name" character varying(255) NOT NULL,
    "name_orig" character varying(255) NOT NULL,
    "dictionary_group" character varying(128) NOT NULL,
    "value" "text" NOT NULL,
    "language" character varying(3) DEFAULT NULL::character varying,
    "domain" character varying(255) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."dirprop" (
    "dir_url" character varying(255) NOT NULL,
    "index_fulltext" boolean DEFAULT false NOT NULL,
    "password_protected" character varying(255) DEFAULT NULL::character varying,
    "logon_doc_id" integer DEFAULT '-1'::integer NOT NULL
);



CREATE TABLE "webjet_cms"."doc_atr" (
    "doc_id" integer DEFAULT 0 NOT NULL,
    "atr_id" integer DEFAULT 0 NOT NULL,
    "value_string" character varying(255) DEFAULT NULL::character varying,
    "value_int" double precision,
    "value_bool" boolean DEFAULT false,
    "id" integer NOT NULL
);



CREATE TABLE "webjet_cms"."doc_atr_def" (
    "atr_id" integer NOT NULL,
    "atr_name" character varying(255) DEFAULT ''::character varying NOT NULL,
    "order_priority" integer DEFAULT 10,
    "atr_description" character varying(255) DEFAULT NULL::character varying,
    "atr_default_value" "text",
    "atr_type" smallint DEFAULT '0'::smallint NOT NULL,
    "atr_group" character varying(32) DEFAULT 'default'::character varying,
    "true_value" character varying(255) DEFAULT NULL::character varying,
    "false_value" character varying(255) DEFAULT NULL::character varying,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE SEQUENCE "webjet_cms"."doc_atr_def_atr_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."doc_atr_def_atr_id_seq" OWNED BY "webjet_cms"."doc_atr_def"."atr_id";



CREATE SEQUENCE "webjet_cms"."doc_atr_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."doc_atr_id_seq" OWNED BY "webjet_cms"."doc_atr"."id";



CREATE TABLE "webjet_cms"."doc_reactions" (
    "id" integer NOT NULL,
    "user_id" integer NOT NULL,
    "doc_id" integer NOT NULL,
    "reaction" integer NOT NULL,
    "created_at" timestamp with time zone NOT NULL
);



CREATE SEQUENCE "webjet_cms"."doc_reactions_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."doc_reactions_id_seq" OWNED BY "webjet_cms"."doc_reactions"."id";



CREATE TABLE "webjet_cms"."doc_subscribe" (
    "subscribe_id" integer NOT NULL,
    "doc_id" integer,
    "first_name" character varying(255) DEFAULT NULL::character varying,
    "last_name" character varying(255) DEFAULT NULL::character varying,
    "email" character varying(255) NOT NULL,
    "user_id" integer DEFAULT '-1'::integer
);



CREATE SEQUENCE "webjet_cms"."doc_subscribe_subscribe_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."doc_subscribe_subscribe_id_seq" OWNED BY "webjet_cms"."doc_subscribe"."subscribe_id";



CREATE TABLE "webjet_cms"."document_forum" (
    "forum_id" integer NOT NULL,
    "doc_id" integer DEFAULT '0'::integer NOT NULL,
    "parent_id" integer DEFAULT '-1'::integer NOT NULL,
    "subject" character varying(255) DEFAULT NULL::character varying,
    "question" "text",
    "question_date" timestamp with time zone,
    "author_name" character varying(255) DEFAULT NULL::character varying,
    "author_email" character varying(255) DEFAULT NULL::character varying,
    "ip" character varying(255) DEFAULT NULL::character varying,
    "confirmed" boolean DEFAULT true NOT NULL,
    "hash_code" character varying(64) DEFAULT NULL::character varying,
    "send_answer_notif" boolean DEFAULT false,
    "user_id" integer DEFAULT '-1'::integer,
    "flag" character varying(128) DEFAULT NULL::character varying,
    "stat_views" integer,
    "stat_replies" integer,
    "stat_last_post" timestamp with time zone,
    "active" boolean DEFAULT true,
    "deleted" boolean DEFAULT false,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



COMMENT ON TABLE "webjet_cms"."document_forum" IS 'diskusne forum';



CREATE SEQUENCE "webjet_cms"."document_forum_forum_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."document_forum_forum_id_seq" OWNED BY "webjet_cms"."document_forum"."forum_id";



CREATE TABLE "webjet_cms"."document_notes" (
    "id" integer NOT NULL,
    "doc_id" integer,
    "history_id" integer,
    "note" character varying(255) DEFAULT NULL::character varying,
    "user_id" integer,
    "created" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);



CREATE TABLE "webjet_cms"."documents" (
    "doc_id" integer NOT NULL,
    "title" character varying(255) DEFAULT ''::character varying NOT NULL,
    "data" "text" NOT NULL,
    "data_asc" "text" NOT NULL,
    "external_link" character varying(255) DEFAULT NULL::character varying,
    "navbar" "text" NOT NULL,
    "date_created" timestamp with time zone DEFAULT '2000-01-01 00:00:00+01'::timestamp with time zone NOT NULL,
    "publish_start" timestamp with time zone,
    "publish_end" timestamp with time zone,
    "author_id" integer DEFAULT '0'::integer NOT NULL,
    "group_id" integer,
    "temp_id" integer DEFAULT '0'::integer NOT NULL,
    "views_total" integer DEFAULT '0'::integer NOT NULL,
    "views_month" integer DEFAULT '0'::integer NOT NULL,
    "searchable" boolean DEFAULT false NOT NULL,
    "available" boolean DEFAULT false NOT NULL,
    "cacheable" boolean DEFAULT false NOT NULL,
    "file_name" character varying(255) DEFAULT NULL::character varying,
    "file_change" timestamp with time zone,
    "sort_priority" integer DEFAULT '0'::integer NOT NULL,
    "header_doc_id" integer,
    "menu_doc_id" integer DEFAULT '-1'::integer NOT NULL,
    "footer_doc_id" integer DEFAULT '-1'::integer NOT NULL,
    "password_protected" character varying(255) DEFAULT NULL::character varying,
    "html_head" "text",
    "html_data" "text",
    "perex_place" character varying(255) DEFAULT NULL::character varying,
    "perex_image" character varying(255) DEFAULT NULL::character varying,
    "perex_group" character varying(255) DEFAULT NULL::character varying,
    "show_in_menu" boolean DEFAULT true,
    "event_date" timestamp with time zone,
    "virtual_path" character varying(255) DEFAULT NULL::character varying,
    "sync_id" integer DEFAULT '0'::integer,
    "sync_status" integer DEFAULT '0'::integer,
    "logon_page_doc_id" integer DEFAULT '-1'::integer,
    "right_menu_doc_id" integer DEFAULT '-1'::integer,
    "field_a" character varying(255) DEFAULT NULL::character varying,
    "field_b" character varying(255) DEFAULT NULL::character varying,
    "field_c" character varying(255) DEFAULT NULL::character varying,
    "field_d" character varying(255) DEFAULT NULL::character varying,
    "field_e" character varying(255) DEFAULT NULL::character varying,
    "field_f" character varying(255) DEFAULT NULL::character varying,
    "field_g" character varying(255) DEFAULT NULL::character varying,
    "field_h" character varying(255) DEFAULT NULL::character varying,
    "field_i" character varying(255) DEFAULT NULL::character varying,
    "field_j" character varying(255) DEFAULT NULL::character varying,
    "field_k" character varying(255) DEFAULT NULL::character varying,
    "field_l" character varying(255) DEFAULT NULL::character varying,
    "disable_after_end" boolean DEFAULT false,
    "forum_count" integer DEFAULT '0'::integer,
    "field_m" character varying(255) DEFAULT NULL::character varying,
    "field_n" character varying(255) DEFAULT NULL::character varying,
    "field_o" character varying(255) DEFAULT NULL::character varying,
    "field_p" character varying(255) DEFAULT NULL::character varying,
    "field_q" character varying(255) DEFAULT NULL::character varying,
    "field_r" character varying(255) DEFAULT NULL::character varying,
    "field_s" character varying(255) DEFAULT NULL::character varying,
    "field_t" character varying(255) DEFAULT NULL::character varying,
    "require_ssl" boolean DEFAULT false,
    "root_group_l1" integer,
    "root_group_l2" integer,
    "root_group_l3" integer,
    "temp_field_a_docid" integer DEFAULT '-1'::integer,
    "temp_field_b_docid" integer DEFAULT '-1'::integer,
    "temp_field_c_docid" integer DEFAULT '-1'::integer,
    "temp_field_d_docid" integer DEFAULT '-1'::integer,
    "show_in_navbar" boolean,
    "show_in_sitemap" boolean,
    "logged_show_in_menu" boolean,
    "logged_show_in_navbar" boolean,
    "logged_show_in_sitemap" boolean,
    "url_inherit_group" boolean DEFAULT false,
    "generate_url_from_title" boolean DEFAULT false,
    "editor_virtual_path" character varying(255) DEFAULT NULL::character varying,
    "publish_after_start" boolean DEFAULT false
);



CREATE SEQUENCE "webjet_cms"."documents_doc_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."documents_doc_id_seq" OWNED BY "webjet_cms"."documents"."doc_id";



CREATE TABLE "webjet_cms"."documents_history" (
    "history_id" integer NOT NULL,
    "save_date" timestamp with time zone,
    "approved_by" integer DEFAULT '-1'::integer NOT NULL,
    "awaiting_approve" character varying(255) DEFAULT NULL::character varying,
    "actual" boolean DEFAULT false NOT NULL,
    "doc_id" integer DEFAULT '0'::integer NOT NULL,
    "title" character varying(255) DEFAULT ''::character varying NOT NULL,
    "data" "text" NOT NULL,
    "data_asc" "text" NOT NULL,
    "external_link" character varying(255) DEFAULT NULL::character varying,
    "navbar" "text" NOT NULL,
    "date_created" timestamp with time zone DEFAULT '2000-01-01 00:00:00+01'::timestamp with time zone NOT NULL,
    "publish_start" timestamp with time zone,
    "publish_end" timestamp with time zone,
    "author_id" integer DEFAULT '0'::integer NOT NULL,
    "group_id" integer,
    "temp_id" integer DEFAULT '0'::integer NOT NULL,
    "views_total" integer DEFAULT '0'::integer NOT NULL,
    "views_month" integer DEFAULT '0'::integer NOT NULL,
    "searchable" boolean DEFAULT false NOT NULL,
    "available" boolean DEFAULT false NOT NULL,
    "cacheable" boolean DEFAULT false NOT NULL,
    "file_name" character varying(255) DEFAULT NULL::character varying,
    "file_change" timestamp with time zone,
    "sort_priority" integer DEFAULT '0'::integer NOT NULL,
    "header_doc_id" integer,
    "footer_doc_id" integer,
    "menu_doc_id" integer DEFAULT '-1'::integer NOT NULL,
    "password_protected" character varying(255) DEFAULT NULL::character varying,
    "html_head" "text",
    "html_data" "text",
    "publicable" boolean DEFAULT false,
    "perex_place" character varying(255) DEFAULT NULL::character varying,
    "perex_image" character varying(255) DEFAULT NULL::character varying,
    "perex_group" character varying(255) DEFAULT NULL::character varying,
    "show_in_menu" boolean DEFAULT true,
    "event_date" timestamp with time zone,
    "virtual_path" character varying(255) DEFAULT NULL::character varying,
    "sync_id" integer DEFAULT '0'::integer,
    "sync_status" integer DEFAULT '0'::integer,
    "logon_page_doc_id" integer DEFAULT '-1'::integer,
    "right_menu_doc_id" integer DEFAULT '-1'::integer,
    "field_a" character varying(255) DEFAULT NULL::character varying,
    "field_b" character varying(255) DEFAULT NULL::character varying,
    "field_c" character varying(255) DEFAULT NULL::character varying,
    "field_d" character varying(255) DEFAULT NULL::character varying,
    "field_e" character varying(255) DEFAULT NULL::character varying,
    "field_f" character varying(255) DEFAULT NULL::character varying,
    "field_g" character varying(255) DEFAULT NULL::character varying,
    "field_h" character varying(255) DEFAULT NULL::character varying,
    "field_i" character varying(255) DEFAULT NULL::character varying,
    "field_j" character varying(255) DEFAULT NULL::character varying,
    "field_k" character varying(255) DEFAULT NULL::character varying,
    "field_l" character varying(255) DEFAULT NULL::character varying,
    "disable_after_end" boolean DEFAULT false,
    "approve_date" timestamp with time zone,
    "forum_count" integer DEFAULT '0'::integer,
    "field_m" character varying(255) DEFAULT NULL::character varying,
    "field_n" character varying(255) DEFAULT NULL::character varying,
    "field_o" character varying(255) DEFAULT NULL::character varying,
    "field_p" character varying(255) DEFAULT NULL::character varying,
    "field_q" character varying(255) DEFAULT NULL::character varying,
    "field_r" character varying(255) DEFAULT NULL::character varying,
    "field_s" character varying(255) DEFAULT NULL::character varying,
    "field_t" character varying(255) DEFAULT NULL::character varying,
    "require_ssl" boolean DEFAULT false,
    "disapproved_by" integer,
    "temp_field_a_docid" integer DEFAULT '-1'::integer,
    "temp_field_b_docid" integer DEFAULT '-1'::integer,
    "temp_field_c_docid" integer DEFAULT '-1'::integer,
    "temp_field_d_docid" integer DEFAULT '-1'::integer,
    "show_in_navbar" boolean,
    "show_in_sitemap" boolean,
    "logged_show_in_menu" boolean,
    "logged_show_in_navbar" boolean,
    "logged_show_in_sitemap" boolean,
    "url_inherit_group" boolean DEFAULT false,
    "generate_url_from_title" boolean DEFAULT false,
    "editor_virtual_path" character varying(64) DEFAULT NULL::character varying,
    "publish_after_start" boolean DEFAULT false
);



CREATE SEQUENCE "webjet_cms"."documents_history_history_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."documents_history_history_id_seq" OWNED BY "webjet_cms"."documents_history"."history_id";



CREATE TABLE "webjet_cms"."domain_limits" (
    "domain_limit_id" integer NOT NULL,
    "domain" character varying(63) NOT NULL,
    "time_unit" character varying(20) NOT NULL,
    "limit_size" integer NOT NULL,
    "active" boolean DEFAULT true NOT NULL,
    "min_delay" integer NOT NULL,
    "delay_active" boolean DEFAULT true NOT NULL
);



CREATE SEQUENCE "webjet_cms"."domain_limits_domain_limit_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."domain_limits_domain_limit_id_seq" OWNED BY "webjet_cms"."domain_limits"."domain_limit_id";



CREATE TABLE "webjet_cms"."domain_redirects" (
    "redirect_id" integer NOT NULL,
    "redirect_from" character varying(100) DEFAULT NULL::character varying,
    "redirect_to" character varying(512) DEFAULT NULL::character varying,
    "redirect_params" boolean DEFAULT false NOT NULL,
    "redirect_path" boolean DEFAULT false NOT NULL,
    "active" boolean DEFAULT false NOT NULL,
    "http_protocol" character varying(15) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."email_files" (
    "id" integer NOT NULL,
    "subject" character varying(255) NOT NULL,
    "sender" character varying(255) NOT NULL,
    "sent_date" timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    "file_path" character varying(255) NOT NULL,
    "visible" integer NOT NULL
);



CREATE SEQUENCE "webjet_cms"."email_files_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."email_files_id_seq" OWNED BY "webjet_cms"."email_files"."id";



CREATE TABLE "webjet_cms"."emails" (
    "email_id" integer NOT NULL,
    "recipient_email" character varying(128) DEFAULT '0'::character varying NOT NULL,
    "recipient_name" character varying(128) DEFAULT NULL::character varying,
    "sender_name" character varying(128) DEFAULT '0'::character varying,
    "sender_email" character varying(128) DEFAULT '0'::character varying NOT NULL,
    "subject" character varying(255) DEFAULT NULL::character varying,
    "url" character varying(255) DEFAULT ''::character varying NOT NULL,
    "attachments" "text",
    "retry" integer DEFAULT 0 NOT NULL,
    "sent_date" timestamp with time zone,
    "created_by_user_id" integer DEFAULT 0 NOT NULL,
    "create_date" timestamp with time zone,
    "send_at" timestamp with time zone,
    "message" "text",
    "reply_to" character varying(255) DEFAULT NULL::character varying,
    "cc_email" character varying(255) DEFAULT NULL::character varying,
    "bcc_email" character varying(255) DEFAULT NULL::character varying,
    "disabled" boolean DEFAULT false,
    "campain_id" integer,
    "recipient_user_id" integer DEFAULT '-1'::integer,
    "seen_date" timestamp with time zone,
    "click_hash" character varying(64) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."emails_campain" (
    "emails_campain_id" integer NOT NULL,
    "sender_name" character varying(255) DEFAULT NULL::character varying,
    "sender_email" character varying(255) DEFAULT NULL::character varying,
    "subject" character varying(255) DEFAULT NULL::character varying,
    "url" character varying(255) DEFAULT NULL::character varying,
    "created_by_user_id" integer,
    "create_date" timestamp with time zone,
    "count_of_recipients" integer,
    "count_of_sent_mails" integer DEFAULT '0'::integer NOT NULL,
    "last_sent_date" timestamp with time zone,
    "user_groups" character varying(64) DEFAULT NULL::character varying,
    "send_at" timestamp with time zone,
    "attachments" "text",
    "reply_to" character varying(255) DEFAULT NULL::character varying,
    "cc_email" character varying(255) DEFAULT NULL::character varying,
    "bcc_email" character varying(255) DEFAULT NULL::character varying
);



CREATE SEQUENCE "webjet_cms"."emails_campain_emails_campain_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."emails_campain_emails_campain_id_seq" OWNED BY "webjet_cms"."emails_campain"."emails_campain_id";



CREATE SEQUENCE "webjet_cms"."emails_email_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."emails_email_id_seq" OWNED BY "webjet_cms"."emails"."email_id";



CREATE TABLE "webjet_cms"."emails_stat_click" (
    "click_id" integer NOT NULL,
    "email_id" integer NOT NULL,
    "link" character varying(255) NOT NULL,
    "click_date" timestamp with time zone NOT NULL,
    "session_id" bigint DEFAULT '0'::bigint,
    "browser_id" bigint DEFAULT '0'::bigint
);



CREATE SEQUENCE "webjet_cms"."emails_stat_click_click_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."emails_stat_click_click_id_seq" OWNED BY "webjet_cms"."emails_stat_click"."click_id";



CREATE TABLE "webjet_cms"."emails_unsubscribed" (
    "emails_unsubscribed_id" integer NOT NULL,
    "email" character varying(128) DEFAULT NULL::character varying,
    "create_date" timestamp with time zone
);



CREATE SEQUENCE "webjet_cms"."emails_unsubscribed_emails_unsubscribed_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."emails_unsubscribed_emails_unsubscribed_id_seq" OWNED BY "webjet_cms"."emails_unsubscribed"."emails_unsubscribed_id";



CREATE TABLE "webjet_cms"."enumeration_data" (
    "enumeration_data_id" integer NOT NULL,
    "enumeration_type_id" integer NOT NULL,
    "child_enumeration_type_id" integer,
    "string1" character varying(1024) DEFAULT NULL::character varying,
    "string2" character varying(1024) DEFAULT NULL::character varying,
    "string3" character varying(1024) DEFAULT NULL::character varying,
    "string4" character varying(1024) DEFAULT NULL::character varying,
    "decimal1" numeric(10,4) DEFAULT NULL::numeric,
    "decimal2" numeric(10,4) DEFAULT NULL::numeric,
    "decimal3" numeric(10,4) DEFAULT NULL::numeric,
    "decimal4" numeric(10,4) DEFAULT NULL::numeric,
    "boolean1" boolean DEFAULT false NOT NULL,
    "boolean2" boolean DEFAULT false NOT NULL,
    "boolean3" boolean DEFAULT false NOT NULL,
    "boolean4" boolean DEFAULT false NOT NULL,
    "sort_priority" integer,
    "hidden" boolean DEFAULT false NOT NULL,
    "dtype" character varying(31) DEFAULT 'default'::character varying,
    "string5" character varying(1024) DEFAULT NULL::character varying,
    "string6" character varying(1024) DEFAULT NULL::character varying,
    "date1" timestamp with time zone,
    "date2" timestamp with time zone,
    "date3" timestamp with time zone,
    "date4" timestamp with time zone,
    "parent_enumeration_data_id" integer,
    "string7" character varying(1024) DEFAULT NULL::character varying,
    "string8" character varying(1024) DEFAULT NULL::character varying,
    "string9" character varying(1024) DEFAULT NULL::character varying,
    "string10" character varying(1024) DEFAULT NULL::character varying,
    "string11" character varying(1024) DEFAULT NULL::character varying,
    "string12" character varying(1024) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."enumeration_type" (
    "enumeration_type_id" integer NOT NULL,
    "name" character varying(255) DEFAULT NULL::character varying,
    "string1_name" character varying(255) DEFAULT NULL::character varying,
    "string2_name" character varying(255) DEFAULT NULL::character varying,
    "string3_name" character varying(255) DEFAULT NULL::character varying,
    "string4_name" character varying(255) DEFAULT NULL::character varying,
    "string5_name" character varying(255) DEFAULT NULL::character varying,
    "string6_name" character varying(255) DEFAULT NULL::character varying,
    "string7_name" character varying(255) DEFAULT NULL::character varying,
    "string8_name" character varying(255) DEFAULT NULL::character varying,
    "string9_name" character varying(255) DEFAULT NULL::character varying,
    "string10_name" character varying(255) DEFAULT NULL::character varying,
    "string11_name" character varying(255) DEFAULT NULL::character varying,
    "string12_name" character varying(255) DEFAULT NULL::character varying,
    "decimal1_name" character varying(255) DEFAULT NULL::character varying,
    "decimal2_name" character varying(255) DEFAULT NULL::character varying,
    "decimal3_name" character varying(255) DEFAULT NULL::character varying,
    "decimal4_name" character varying(255) DEFAULT NULL::character varying,
    "boolean1_name" character varying(255) DEFAULT NULL::character varying,
    "boolean2_name" character varying(255) DEFAULT NULL::character varying,
    "boolean3_name" character varying(255) DEFAULT NULL::character varying,
    "boolean4_name" character varying(255) DEFAULT NULL::character varying,
    "hidden" boolean,
    "child_enumeration_type_id" integer,
    "dtype" character varying(31) DEFAULT 'default'::character varying,
    "allow_child_enumeration_type" boolean DEFAULT false NOT NULL,
    "date1_name" character varying(255) DEFAULT NULL::character varying,
    "date2_name" character varying(255) DEFAULT NULL::character varying,
    "date3_name" character varying(255) DEFAULT NULL::character varying,
    "date4_name" character varying(255) DEFAULT NULL::character varying,
    "allow_parent_enumeration_data" boolean DEFAULT false NOT NULL
);



CREATE TABLE "webjet_cms"."export_dat" (
    "export_dat_id" integer NOT NULL,
    "url_address" character varying(255) NOT NULL,
    "format" character varying(32) NOT NULL,
    "number_items" integer,
    "group_ids" character varying(255) DEFAULT NULL::character varying,
    "expand_group_ids" boolean NOT NULL,
    "perex_group" character varying(255) DEFAULT NULL::character varying,
    "order_type" character varying(255) NOT NULL,
    "asc_order" boolean NOT NULL,
    "publish_type" character varying(255) NOT NULL,
    "no_perex_check" boolean NOT NULL
);



CREATE TABLE "webjet_cms"."file_archiv" (
    "file_archiv_id" integer NOT NULL,
    "user_id" integer,
    "file_path" character varying(255) DEFAULT NULL::character varying,
    "file_name" character varying(255) DEFAULT NULL::character varying,
    "virtual_file_name" character varying(255) DEFAULT NULL::character varying,
    "show_file" boolean NOT NULL,
    "date_insert" timestamp with time zone,
    "valid_from" timestamp with time zone,
    "valid_to" timestamp with time zone,
    "domain" character varying(255) DEFAULT NULL::character varying,
    "reference_id" integer,
    "order_id" integer,
    "product_code" character varying(255) DEFAULT NULL::character varying,
    "product" character varying(255) DEFAULT NULL::character varying,
    "category" character varying(255) DEFAULT NULL::character varying,
    "md5" character varying(50) DEFAULT NULL::character varying,
    "reference_to_main" character varying(255) DEFAULT NULL::character varying,
    "priority" character varying(255) DEFAULT NULL::character varying,
    "field_a" character varying(255) DEFAULT NULL::character varying,
    "field_b" character varying(255) DEFAULT NULL::character varying,
    "field_c" integer,
    "field_d" bigint,
    "field_e" character varying(255) DEFAULT NULL::character varying,
    "global_id" integer,
    "note" character varying(1100) DEFAULT NULL::character varying,
    "date_upload_later" "date",
    "uploaded" integer DEFAULT '-1'::integer,
    "emails" character varying(1100) DEFAULT NULL::character varying,
    "file_size" integer,
    "domain_id" integer DEFAULT '1'::integer,
    "extended_data_id" integer
);



CREATE TABLE "webjet_cms"."file_archiv_category_node" (
    "id" integer NOT NULL,
    "lft" integer,
    "rgt" integer,
    "lvl" integer,
    "rootid" integer,
    "category_name" character varying(500) DEFAULT NULL::character varying,
    "date_insert" timestamp with time zone,
    "string1name" character varying(500) DEFAULT NULL::character varying,
    "string2name" character varying(500) DEFAULT NULL::character varying,
    "string3name" character varying(500) DEFAULT NULL::character varying,
    "int1val" integer,
    "int2val" integer,
    "int3val" integer,
    "category_type" integer,
    "sort_priority" integer,
    "is_show" integer
);



CREATE SEQUENCE "webjet_cms"."file_archiv_category_node_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."file_archiv_category_node_id_seq" OWNED BY "webjet_cms"."file_archiv_category_node"."id";



CREATE TABLE "webjet_cms"."file_atr" (
    "file_name" character varying(128) DEFAULT ''::character varying NOT NULL,
    "link" character varying(255) DEFAULT ''::character varying NOT NULL,
    "atr_id" integer DEFAULT 0 NOT NULL,
    "value_string" character varying(255) DEFAULT NULL::character varying,
    "value_int" integer,
    "value_bool" boolean DEFAULT false
);



CREATE TABLE "webjet_cms"."file_atr_def" (
    "atr_id" integer NOT NULL,
    "atr_name" character varying(32) DEFAULT ''::character varying NOT NULL,
    "order_priority" integer DEFAULT 10,
    "atr_description" character varying(255) DEFAULT NULL::character varying,
    "atr_default_value" character varying(255) DEFAULT NULL::character varying,
    "atr_type" smallint DEFAULT '0'::smallint NOT NULL,
    "atr_group" character varying(32) DEFAULT 'default'::character varying,
    "true_value" character varying(255) DEFAULT NULL::character varying,
    "false_value" character varying(255) DEFAULT NULL::character varying
);



CREATE SEQUENCE "webjet_cms"."file_atr_def_atr_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."file_atr_def_atr_id_seq" OWNED BY "webjet_cms"."file_atr_def"."atr_id";



CREATE TABLE "webjet_cms"."file_history" (
    "file_history_id" integer NOT NULL,
    "file_url" character varying(255) DEFAULT NULL::character varying,
    "change_date" timestamp with time zone,
    "user_id" integer,
    "deleted" boolean DEFAULT false,
    "history_path" character varying(255) DEFAULT NULL::character varying,
    "ip_address" character varying(32) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."form_attributes" (
    "value" character varying(1024) DEFAULT NULL::character varying,
    "form_name" character varying(255) NOT NULL,
    "param_name" character varying(64) NOT NULL,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE TABLE "webjet_cms"."form_regular_exp" (
    "title" character varying(255) NOT NULL,
    "type" character varying(32) NOT NULL,
    "reg_exp" character varying(255) NOT NULL,
    "id" integer NOT NULL
);



CREATE SEQUENCE "webjet_cms"."form_regular_exp_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."form_regular_exp_id_seq" OWNED BY "webjet_cms"."form_regular_exp"."id";



CREATE TABLE "webjet_cms"."forms" (
    "id" integer NOT NULL,
    "form_name" character varying(255) DEFAULT ''::character varying NOT NULL,
    "data" "text" NOT NULL,
    "files" "text",
    "create_date" timestamp with time zone,
    "html" "text",
    "user_id" integer DEFAULT '-1'::integer NOT NULL,
    "note" "text",
    "doc_id" integer DEFAULT '-1'::integer NOT NULL,
    "last_export_date" timestamp with time zone,
    "domain_id" integer DEFAULT '1'::integer NOT NULL,
    "double_optin_confirmation_date" timestamp with time zone,
    "double_optin_hash" character varying(20) DEFAULT NULL::character varying
);



COMMENT ON TABLE "webjet_cms"."forms" IS 'formulare';



CREATE TABLE "webjet_cms"."forms_archive" (
    "id" integer NOT NULL,
    "form_name" character varying(255) DEFAULT ''::character varying NOT NULL,
    "data" "text" NOT NULL,
    "files" "text",
    "create_date" timestamp with time zone,
    "html" "text",
    "user_id" integer DEFAULT '-1'::integer NOT NULL,
    "note" "text",
    "doc_id" integer DEFAULT '-1'::integer NOT NULL,
    "last_export_date" timestamp with time zone,
    "domain_id" integer DEFAULT '1'::integer NOT NULL,
    "double_optin_confirmation_date" timestamp with time zone,
    "double_optin_hash" character varying(20) DEFAULT NULL::character varying
);



CREATE SEQUENCE "webjet_cms"."forms_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."forms_id_seq" OWNED BY "webjet_cms"."forms"."id";



CREATE TABLE "webjet_cms"."forum" (
    "id" integer NOT NULL,
    "doc_id" integer,
    "active" boolean NOT NULL,
    "date_from" timestamp with time zone,
    "date_to" timestamp with time zone,
    "hours_after_last_message" integer,
    "message_confirmation" boolean DEFAULT false NOT NULL,
    "approve_email" character varying(255) DEFAULT NULL::character varying,
    "notif_email" character varying(255) DEFAULT NULL::character varying,
    "message_board" boolean DEFAULT false NOT NULL,
    "advertisement_type" boolean,
    "admin_groups" character varying(32) DEFAULT NULL::character varying,
    "addmessage_groups" character varying(128) DEFAULT NULL::character varying,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE SEQUENCE "webjet_cms"."forum_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."forum_id_seq" OWNED BY "webjet_cms"."forum"."id";



CREATE TABLE "webjet_cms"."gallery" (
    "image_id" integer NOT NULL,
    "image_path" character varying(255) DEFAULT NULL::character varying,
    "s_description_sk" character varying(255) DEFAULT NULL::character varying,
    "l_description_sk" "text",
    "image_name" character varying(255) DEFAULT NULL::character varying,
    "s_description_en" character varying(255) DEFAULT NULL::character varying,
    "l_description_en" "text",
    "s_description_cz" character varying(255) DEFAULT NULL::character varying,
    "l_description_cz" "text",
    "s_description_de" character varying(255) DEFAULT NULL::character varying,
    "l_description_de" "text",
    "l_description_pl" "text",
    "s_description_pl" character varying(255) DEFAULT ''::character varying,
    "l_description_ru" "text",
    "s_description_ru" character varying(255) DEFAULT ''::character varying,
    "l_description_hu" "text",
    "s_description_hu" character varying(255) DEFAULT ''::character varying,
    "l_description_cho" "text",
    "s_description_cho" character varying(255) DEFAULT ''::character varying,
    "l_description_esp" "text",
    "s_description_esp" character varying(255) DEFAULT ''::character varying,
    "author" character varying(255) DEFAULT ''::character varying,
    "send_count" integer DEFAULT '0'::integer,
    "allowed_domains" character varying(255) DEFAULT NULL::character varying,
    "perex_group" character varying(255) DEFAULT NULL::character varying,
    "selected_x" integer,
    "selected_y" integer,
    "selected_width" integer,
    "selected_height" integer,
    "upload_datetime" timestamp with time zone,
    "sort_priority" integer,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE TABLE "webjet_cms"."gallery_dimension" (
    "dimension_id" integer NOT NULL,
    "image_path" character varying(255) DEFAULT '0'::character varying,
    "image_width" integer DEFAULT 0,
    "image_height" integer DEFAULT 0,
    "normal_width" integer DEFAULT 0 NOT NULL,
    "normal_height" integer DEFAULT 0 NOT NULL,
    "resize_mode" character varying(1) DEFAULT NULL::character varying,
    "gallery_name" character varying(255) DEFAULT NULL::character varying,
    "gallery_perex" "text",
    "create_date" timestamp with time zone,
    "author" character varying(255) DEFAULT NULL::character varying,
    "views" integer DEFAULT '0'::integer NOT NULL,
    "watermark_saturation" integer,
    "watermark" character varying(255) DEFAULT NULL::character varying,
    "watermark_placement" character varying(255) DEFAULT NULL::character varying,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE SEQUENCE "webjet_cms"."gallery_dimension_dimension_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."gallery_dimension_dimension_id_seq" OWNED BY "webjet_cms"."gallery_dimension"."dimension_id";



CREATE SEQUENCE "webjet_cms"."gallery_image_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."gallery_image_id_seq" OWNED BY "webjet_cms"."gallery"."image_id";



CREATE TABLE "webjet_cms"."gdpr_regexp" (
    "gdpr_regexp_id" integer NOT NULL,
    "regexp_name" character varying(255) DEFAULT NULL::character varying,
    "regexp_value" character varying(1024) DEFAULT NULL::character varying,
    "user_id" integer,
    "date_insert" timestamp with time zone,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE TABLE "webjet_cms"."gis" (
    "gis_id" integer NOT NULL,
    "cathegory" character varying(128) DEFAULT NULL::character varying,
    "name" character varying(255) DEFAULT NULL::character varying,
    "description" "text",
    "gps_lat" double precision,
    "gps_lon" double precision,
    "detail_url" character varying(2000) DEFAULT NULL::character varying,
    "image_url" character varying(2000) DEFAULT NULL::character varying,
    "pin_url" character varying(255) DEFAULT NULL::character varying,
    "approved" boolean,
    "approve_hash" character varying(255) DEFAULT NULL::character varying,
    "info_a" character varying(255) DEFAULT NULL::character varying,
    "info_b" character varying(255) DEFAULT NULL::character varying,
    "info_c" character varying(255) DEFAULT NULL::character varying,
    "info_d" character varying(255) DEFAULT NULL::character varying,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE TABLE "webjet_cms"."groups" (
    "group_id" integer NOT NULL,
    "group_name" character varying(255) NOT NULL,
    "internal" boolean DEFAULT false NOT NULL,
    "parent_group_id" integer DEFAULT '0'::integer NOT NULL,
    "navbar" character varying(255) DEFAULT NULL::character varying,
    "default_doc_id" integer,
    "temp_id" integer,
    "sort_priority" integer DEFAULT '0'::integer NOT NULL,
    "password_protected" character varying(255) DEFAULT NULL::character varying,
    "url_dir_name" character varying(255) DEFAULT NULL::character varying,
    "sync_id" integer DEFAULT '0'::integer,
    "sync_status" integer DEFAULT '0'::integer,
    "html_head" "text",
    "logon_page_doc_id" integer DEFAULT '-1'::integer,
    "domain_name" character varying(255) DEFAULT NULL::character varying,
    "new_page_docid_template" integer DEFAULT '0'::integer,
    "install_name" character varying(255) DEFAULT NULL::character varying,
    "field_a" character varying(255) DEFAULT NULL::character varying,
    "field_b" character varying(255) DEFAULT NULL::character varying,
    "field_c" character varying(255) DEFAULT NULL::character varying,
    "field_d" character varying(255) DEFAULT NULL::character varying,
    "link_group_id" integer DEFAULT '-1'::integer,
    "lng" character varying(8) DEFAULT NULL::character varying,
    "show_in_navbar" integer,
    "show_in_sitemap" integer,
    "logged_show_in_navbar" integer,
    "logged_show_in_sitemap" integer,
    "menu_type" smallint DEFAULT 1,
    "logged_menu_type" smallint DEFAULT 1,
    "hidden_in_admin" boolean,
    "force_group_template" boolean DEFAULT false
);



CREATE TABLE "webjet_cms"."groups_approve" (
    "approve_id" integer NOT NULL,
    "group_id" integer DEFAULT '0'::integer,
    "user_id" integer DEFAULT '0'::integer,
    "approve_mode" integer NOT NULL
);



CREATE SEQUENCE "webjet_cms"."groups_approve_approve_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."groups_approve_approve_id_seq" OWNED BY "webjet_cms"."groups_approve"."approve_id";



CREATE SEQUENCE "webjet_cms"."groups_group_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."groups_group_id_seq" OWNED BY "webjet_cms"."groups"."group_id";



CREATE TABLE "webjet_cms"."groups_scheduler" (
    "schedule_id" integer NOT NULL,
    "group_id" integer NOT NULL,
    "group_name" character varying(255) NOT NULL,
    "internal" boolean DEFAULT false NOT NULL,
    "parent_group_id" integer DEFAULT '0'::integer NOT NULL,
    "navbar" character varying(255) DEFAULT NULL::character varying,
    "default_doc_id" integer,
    "temp_id" integer,
    "sort_priority" integer DEFAULT '0'::integer NOT NULL,
    "password_protected" character varying(255) DEFAULT NULL::character varying,
    "url_dir_name" character varying(255) DEFAULT NULL::character varying,
    "sync_id" integer DEFAULT '0'::integer,
    "sync_status" integer DEFAULT '0'::integer,
    "html_head" "text",
    "logon_page_doc_id" integer DEFAULT '-1'::integer,
    "domain_name" character varying(255) DEFAULT NULL::character varying,
    "new_page_docid_template" integer DEFAULT '0'::integer,
    "install_name" character varying(255) DEFAULT NULL::character varying,
    "field_a" character varying(255) DEFAULT NULL::character varying,
    "field_b" character varying(255) DEFAULT NULL::character varying,
    "field_c" character varying(255) DEFAULT NULL::character varying,
    "field_d" character varying(255) DEFAULT NULL::character varying,
    "link_group_id" integer DEFAULT '-1'::integer,
    "when_to_publish" timestamp with time zone,
    "save_date" timestamp with time zone,
    "user_id" integer DEFAULT '-1'::integer,
    "lng" character varying(8) DEFAULT NULL::character varying,
    "date_published" timestamp with time zone,
    "show_in_navbar" boolean,
    "show_in_sitemap" boolean,
    "logged_show_in_navbar" boolean,
    "logged_show_in_sitemap" boolean,
    "menu_type" smallint DEFAULT 1,
    "logged_menu_type" smallint DEFAULT 1,
    "hidden_in_admin" boolean,
    "force_group_template" boolean DEFAULT false
);



CREATE SEQUENCE "webjet_cms"."groups_scheduler_schedule_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."groups_scheduler_schedule_id_seq" OWNED BY "webjet_cms"."groups_scheduler"."schedule_id";



CREATE TABLE "webjet_cms"."inquiry" (
    "question_id" integer NOT NULL,
    "question_text" "text",
    "hours" integer DEFAULT 0,
    "question_group" character varying(255) DEFAULT NULL::character varying,
    "answer_text_ok" "text",
    "answer_text_fail" "text",
    "date_from" timestamp with time zone,
    "date_to" timestamp with time zone,
    "question_active" boolean DEFAULT true,
    "multiple" boolean,
    "total_clicks" integer,
    "domain_id" integer DEFAULT 1
);



CREATE TABLE "webjet_cms"."inquiry_answers" (
    "answer_id" integer NOT NULL,
    "question_id" integer DEFAULT '0'::integer NOT NULL,
    "answer_text" character varying(255) DEFAULT NULL::character varying,
    "answer_clicks" integer DEFAULT 0,
    "image_path" character varying(255) DEFAULT NULL::character varying,
    "url" character varying(255) DEFAULT NULL::character varying,
    "domain_id" integer DEFAULT 1
);



CREATE SEQUENCE "webjet_cms"."inquiry_answers_answer_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."inquiry_answers_answer_id_seq" OWNED BY "webjet_cms"."inquiry_answers"."answer_id";



CREATE SEQUENCE "webjet_cms"."inquiry_question_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."inquiry_question_id_seq" OWNED BY "webjet_cms"."inquiry"."question_id";



CREATE TABLE "webjet_cms"."inquiry_users" (
    "user_id" integer NOT NULL,
    "question_id" integer NOT NULL,
    "answer_id" integer NOT NULL,
    "create_date" timestamp with time zone,
    "ip_address" character varying(255) DEFAULT NULL::character varying,
    "domain_id" integer DEFAULT 1
);



CREATE TABLE "webjet_cms"."inquirysimple_answers" (
    "id" integer NOT NULL,
    "form_id" character varying(255) NOT NULL,
    "question_id" character varying(255) NOT NULL,
    "user_id" character varying(255) NOT NULL
);



CREATE SEQUENCE "webjet_cms"."inquirysimple_answers_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."inquirysimple_answers_id_seq" OWNED BY "webjet_cms"."inquirysimple_answers"."id";



CREATE TABLE "webjet_cms"."insert_script" (
    "insert_script_id" integer NOT NULL,
    "save_date" timestamp with time zone,
    "user_id" integer,
    "script_body" "text",
    "name" character varying(255) DEFAULT NULL::character varying,
    "valid_from" timestamp with time zone,
    "valid_to" timestamp with time zone,
    "position" character varying(60) DEFAULT NULL::character varying,
    "domain_id" integer DEFAULT '1'::integer,
    "cookie_class" character varying(255) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."insert_script_doc" (
    "insert_script_doc_id" integer NOT NULL,
    "doc_id" integer,
    "insert_script" integer
);



CREATE TABLE "webjet_cms"."insert_script_gr" (
    "insert_script_gr_id" integer NOT NULL,
    "group_id" integer,
    "insert_script" integer,
    "domain_id" integer DEFAULT '1'::integer
);



CREATE TABLE "webjet_cms"."inventory" (
    "inventory_id" integer NOT NULL,
    "type" character varying(32) NOT NULL,
    "serial_number" character varying(32) DEFAULT NULL::character varying,
    "inventory_number" character varying(32) DEFAULT NULL::character varying,
    "user_id" integer,
    "room" character varying(32) DEFAULT NULL::character varying,
    "date_deleted" "date",
    "name" character varying(64) DEFAULT NULL::character varying,
    "description" "text",
    "junk_reason" "text",
    "department" character varying(32) DEFAULT NULL::character varying,
    "deleted" boolean
);



CREATE TABLE "webjet_cms"."inventory_detail" (
    "inventory_detail_id" integer NOT NULL,
    "inventory_id" integer NOT NULL,
    "type" character varying(32) NOT NULL,
    "serial_number" character varying(32) DEFAULT NULL::character varying,
    "name" character varying(64) DEFAULT NULL::character varying,
    "description" "text",
    "date_from" "date" NOT NULL,
    "date_till" "date",
    "junk_reason" "text"
);



CREATE TABLE "webjet_cms"."inventory_log" (
    "inventory_log_id" integer NOT NULL,
    "inventory_id" integer NOT NULL,
    "user_id" integer,
    "room" character varying(32) DEFAULT NULL::character varying,
    "date_from" "date" NOT NULL,
    "date_till" "date",
    "department" character varying(32) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."media" (
    "media_id" integer NOT NULL,
    "media_fk_id" integer,
    "media_fk_table_name" character varying(255) DEFAULT NULL::character varying,
    "media_title_sk" character varying(255) DEFAULT NULL::character varying,
    "media_title_cz" character varying(255) DEFAULT NULL::character varying,
    "media_title_de" character varying(255) DEFAULT NULL::character varying,
    "media_title_en" character varying(255) DEFAULT NULL::character varying,
    "media_link" character varying(255) DEFAULT NULL::character varying,
    "media_thumb_link" character varying(255) DEFAULT NULL::character varying,
    "media_group" character varying(255) DEFAULT NULL::character varying,
    "media_info_sk" "text",
    "media_info_cz" "text",
    "media_info_de" "text",
    "media_info_en" "text",
    "media_sort_order" integer DEFAULT '10'::integer NOT NULL,
    "last_update" timestamp with time zone,
    "domain_id" integer DEFAULT '1'::integer NOT NULL,
    "field_a" character varying(255) DEFAULT NULL::character varying,
    "field_b" character varying(255) DEFAULT NULL::character varying,
    "field_c" character varying(255) DEFAULT NULL::character varying,
    "field_d" character varying(255) DEFAULT NULL::character varying,
    "field_e" character varying(255) DEFAULT NULL::character varying,
    "field_f" character varying(255) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."media_group_to_media" (
    "media_group_id" integer NOT NULL,
    "media_id" integer NOT NULL
);



CREATE TABLE "webjet_cms"."media_groups" (
    "media_group_id" integer NOT NULL,
    "media_group_name" character varying(255) NOT NULL,
    "available_groups" character varying(255) DEFAULT NULL::character varying,
    "related_pages" "text"
);



CREATE TABLE "webjet_cms"."monitoring" (
    "monitoring_id" integer NOT NULL,
    "date_insert" timestamp with time zone,
    "node_name" character varying(16) DEFAULT NULL::character varying,
    "db_active" integer,
    "db_idle" integer,
    "mem_free" bigint,
    "mem_total" bigint,
    "cache" integer,
    "sessions" integer,
    "cpu_usage" double precision,
    "process_usage" double precision
);



CREATE SEQUENCE "webjet_cms"."monitoring_monitoring_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."monitoring_monitoring_id_seq" OWNED BY "webjet_cms"."monitoring"."monitoring_id";



CREATE TABLE "webjet_cms"."multigroup_mapping" (
    "doc_id" integer NOT NULL,
    "master_id" integer DEFAULT '-1'::integer NOT NULL,
    "redirect" boolean DEFAULT true NOT NULL
);



CREATE TABLE "webjet_cms"."passwords_history" (
    "passwords_history_id" integer NOT NULL,
    "user_id" integer,
    "password" character varying(128) DEFAULT NULL::character varying,
    "salt" character varying(64) DEFAULT NULL::character varying,
    "save_date" timestamp with time zone
);



CREATE TABLE "webjet_cms"."perex_group_doc" (
    "doc_id" integer NOT NULL,
    "perex_group_id" integer
);



CREATE TABLE "webjet_cms"."perex_groups" (
    "perex_group_id" integer NOT NULL,
    "perex_group_name" character varying(255) NOT NULL,
    "related_pages" "text",
    "available_groups" character varying(255) DEFAULT NULL::character varying
);



CREATE SEQUENCE "webjet_cms"."perex_groups_perex_group_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."perex_groups_perex_group_id_seq" OWNED BY "webjet_cms"."perex_groups"."perex_group_id";



CREATE TABLE "webjet_cms"."pkey_generator" (
    "name" character varying(255) NOT NULL,
    "value" bigint NOT NULL,
    "table_name" character varying(255) DEFAULT NULL::character varying,
    "table_pkey_name" character varying(255) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."proxy" (
    "proxy_id" integer NOT NULL,
    "name" character varying(255) DEFAULT NULL::character varying,
    "local_url" "text",
    "remote_server" character varying(255) DEFAULT NULL::character varying,
    "remote_url" character varying(255) DEFAULT NULL::character varying,
    "remote_port" integer NOT NULL,
    "crop_start" character varying(255) DEFAULT NULL::character varying,
    "crop_end" character varying(255) DEFAULT NULL::character varying,
    "encoding" character varying(16) DEFAULT NULL::character varying,
    "proxy_method" character varying(64) DEFAULT NULL::character varying,
    "include_ext" character varying(255) DEFAULT NULL::character varying,
    "auth_method" character varying(16) DEFAULT NULL::character varying,
    "auth_username" character varying(64) DEFAULT NULL::character varying,
    "auth_password" character varying(64) DEFAULT NULL::character varying,
    "auth_host" character varying(64) DEFAULT NULL::character varying,
    "auth_domain" character varying(64) DEFAULT NULL::character varying,
    "allowed_methods" character varying(64) DEFAULT NULL::character varying,
    "keep_crop_start" boolean,
    "keep_crop_end" boolean
);



CREATE SEQUENCE "webjet_cms"."proxy_proxy_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."proxy_proxy_id_seq" OWNED BY "webjet_cms"."proxy"."proxy_id";



CREATE TABLE "webjet_cms"."questions_answers" (
    "qa_id" integer NOT NULL,
    "group_name" character varying(255) DEFAULT ''::character varying NOT NULL,
    "category_name" character varying(64) DEFAULT NULL::character varying,
    "question_date" timestamp with time zone,
    "answer_date" timestamp with time zone,
    "question" "text",
    "answer" "text",
    "from_name" character varying(255) DEFAULT ''::character varying NOT NULL,
    "from_email" character varying(255) DEFAULT NULL::character varying,
    "to_name" character varying(255) DEFAULT NULL::character varying,
    "to_email" character varying(255) DEFAULT NULL::character varying,
    "publish_on_web" boolean DEFAULT false,
    "hash" character varying(255) DEFAULT NULL::character varying,
    "allow_publish_on_web" boolean DEFAULT true NOT NULL,
    "sort_priority" integer,
    "from_phone" character varying(32) DEFAULT NULL::character varying,
    "from_company" character varying(128) DEFAULT NULL::character varying,
    "field_a" character varying(255) DEFAULT NULL::character varying,
    "field_b" character varying(255) DEFAULT NULL::character varying,
    "field_c" character varying(255) DEFAULT NULL::character varying,
    "field_d" character varying(255) DEFAULT NULL::character varying,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE SEQUENCE "webjet_cms"."questions_answers_qa_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."questions_answers_qa_id_seq" OWNED BY "webjet_cms"."questions_answers"."qa_id";



CREATE TABLE "webjet_cms"."quiz" (
    "id" integer NOT NULL,
    "name" character varying(255) NOT NULL,
    "domain_id" integer,
    "quiz_type" character varying(20) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."quiz_answers" (
    "id" integer NOT NULL,
    "form_id" character varying(255) NOT NULL,
    "quiz_id" integer NOT NULL,
    "quiz_question_id" integer NOT NULL,
    "answer" integer NOT NULL,
    "is_correct" boolean NOT NULL,
    "created" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "right_answer" integer NOT NULL,
    "rate" integer
);



CREATE SEQUENCE "webjet_cms"."quiz_answers_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."quiz_answers_id_seq" OWNED BY "webjet_cms"."quiz_answers"."id";



CREATE SEQUENCE "webjet_cms"."quiz_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."quiz_id_seq" OWNED BY "webjet_cms"."quiz"."id";



CREATE TABLE "webjet_cms"."quiz_questions" (
    "id" integer NOT NULL,
    "quiz_id" integer NOT NULL,
    "sort_order" integer NOT NULL,
    "question" character varying(500) NOT NULL,
    "option1" character varying(255) DEFAULT NULL::character varying,
    "option2" character varying(255) DEFAULT NULL::character varying,
    "option3" character varying(255) DEFAULT NULL::character varying,
    "option4" character varying(255) DEFAULT NULL::character varying,
    "option5" character varying(255) DEFAULT NULL::character varying,
    "option6" character varying(255) DEFAULT NULL::character varying,
    "right_answer" integer NOT NULL,
    "rate1" integer,
    "rate2" integer,
    "rate3" integer,
    "rate4" integer,
    "rate5" integer,
    "rate6" integer,
    "image_url" character varying(255) DEFAULT NULL::character varying
);



CREATE SEQUENCE "webjet_cms"."quiz_questions_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."quiz_questions_id_seq" OWNED BY "webjet_cms"."quiz_questions"."id";



CREATE TABLE "webjet_cms"."quiz_results" (
    "id" integer NOT NULL,
    "quiz_id" integer NOT NULL,
    "sort_order" integer NOT NULL,
    "score_from" integer NOT NULL,
    "score_to" integer,
    "description" "text"
);



CREATE SEQUENCE "webjet_cms"."quiz_results_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."quiz_results_id_seq" OWNED BY "webjet_cms"."quiz_results"."id";



CREATE TABLE "webjet_cms"."rating" (
    "rating_id" integer NOT NULL,
    "doc_id" integer,
    "user_id" integer,
    "rating_value" integer,
    "insert_date" timestamp with time zone
);



COMMENT ON TABLE "webjet_cms"."rating" IS 'rating stranok';



CREATE SEQUENCE "webjet_cms"."rating_rating_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."rating_rating_id_seq" OWNED BY "webjet_cms"."rating"."rating_id";



CREATE TABLE "webjet_cms"."reservation" (
    "reservation_id" integer NOT NULL,
    "reservation_object_id" integer DEFAULT '0'::integer NOT NULL,
    "date_from" timestamp with time zone,
    "date_to" timestamp with time zone,
    "name" character varying(150) DEFAULT NULL::character varying,
    "surname" character varying(155) DEFAULT NULL::character varying,
    "email" character varying(100) DEFAULT NULL::character varying,
    "purpose" "text",
    "accepted" boolean,
    "hash_value" character varying(60) DEFAULT NULL::character varying,
    "phone_number" character varying(255) DEFAULT NULL::character varying,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE TABLE "webjet_cms"."reservation_object" (
    "reservation_object_id" integer NOT NULL,
    "name" character varying(100) DEFAULT ''::character varying NOT NULL,
    "must_accepted" boolean DEFAULT false NOT NULL,
    "email_accepter" character varying(150) DEFAULT NULL::character varying,
    "passwd" character varying(60) DEFAULT NULL::character varying,
    "max_reservations" smallint DEFAULT '1'::smallint,
    "reservation_time_from" timestamp with time zone,
    "reservation_time_to" timestamp with time zone,
    "price_for_day" numeric(7,2) DEFAULT 0.00 NOT NULL,
    "price_for_hour" numeric(7,2) DEFAULT 0.00 NOT NULL,
    "reservation_for_all_day" boolean NOT NULL,
    "photo_link" character varying(255) DEFAULT NULL::character varying,
    "description" character varying(2000) DEFAULT NULL::character varying,
    "domain_id" integer DEFAULT '1'::integer NOT NULL,
    "time_unit" smallint,
    "cancel_time_befor" integer DEFAULT 0
);



CREATE TABLE "webjet_cms"."reservation_object_price" (
    "object_price_id" integer NOT NULL,
    "object_id" integer NOT NULL,
    "datum_od" "date",
    "datum_do" "date",
    "cena" numeric(10,2) DEFAULT NULL::numeric,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE SEQUENCE "webjet_cms"."reservation_object_reservation_object_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."reservation_object_reservation_object_id_seq" OWNED BY "webjet_cms"."reservation_object"."reservation_object_id";



CREATE TABLE "webjet_cms"."reservation_object_times" (
    "object_time_id" integer NOT NULL,
    "object_id" integer NOT NULL,
    "cas_od" timestamp with time zone,
    "cas_do" timestamp with time zone,
    "den" integer,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE SEQUENCE "webjet_cms"."reservation_reservation_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."reservation_reservation_id_seq" OWNED BY "webjet_cms"."reservation"."reservation_id";



CREATE TABLE "webjet_cms"."response_headers" (
    "response_header_id" integer NOT NULL,
    "url" character varying(255) NOT NULL,
    "header_name" character varying(255) NOT NULL,
    "header_value" character varying(255) NOT NULL,
    "change_date" timestamp with time zone NOT NULL,
    "note" character varying(255) DEFAULT NULL::character varying,
    "domain_id" integer NOT NULL
);



CREATE SEQUENCE "webjet_cms"."response_headers_response_header_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."response_headers_response_header_id_seq" OWNED BY "webjet_cms"."response_headers"."response_header_id";



CREATE TABLE "webjet_cms"."restaurant_menu" (
    "menu_id" integer NOT NULL,
    "menu_meals_id" integer,
    "day" "date",
    "priority" smallint,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE TABLE "webjet_cms"."restaurant_menu_meals" (
    "meals_id" integer NOT NULL,
    "cathegory" character varying(128) DEFAULT NULL::character varying,
    "name" character varying(255) DEFAULT NULL::character varying,
    "description" "text",
    "weight" character varying(255) DEFAULT NULL::character varying,
    "price" numeric(10,2) DEFAULT NULL::numeric,
    "alergens" character varying(32) DEFAULT NULL::character varying,
    "domain_id" integer DEFAULT '1'::integer NOT NULL
);



CREATE TABLE "webjet_cms"."seo_bots" (
    "seo_bots_id" integer NOT NULL,
    "name" character varying(255) DEFAULT NULL::character varying,
    "last_visit" timestamp with time zone,
    "visit_count" integer
);



CREATE SEQUENCE "webjet_cms"."seo_bots_seo_bots_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."seo_bots_seo_bots_id_seq" OWNED BY "webjet_cms"."seo_bots"."seo_bots_id";



CREATE TABLE "webjet_cms"."seo_google_position" (
    "seo_google_position_id" integer NOT NULL,
    "keyword_id" integer,
    "position" integer,
    "search_datetime" timestamp with time zone
);



CREATE SEQUENCE "webjet_cms"."seo_google_position_seo_google_position_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."seo_google_position_seo_google_position_id_seq" OWNED BY "webjet_cms"."seo_google_position"."seo_google_position_id";



CREATE TABLE "webjet_cms"."seo_keywords" (
    "seo_keyword_id" integer NOT NULL,
    "name" character varying(100) DEFAULT NULL::character varying,
    "created_time" timestamp with time zone,
    "author" integer,
    "domain" character varying(255) DEFAULT NULL::character varying,
    "search_bot" character varying(150) DEFAULT NULL::character varying,
    "actual_position" integer DEFAULT '-1'::integer
);



CREATE SEQUENCE "webjet_cms"."seo_keywords_seo_keyword_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."seo_keywords_seo_keyword_id_seq" OWNED BY "webjet_cms"."seo_keywords"."seo_keyword_id";



CREATE TABLE "webjet_cms"."sita_parsed_ids" (
    "news_item_id" integer NOT NULL,
    "parse_date" timestamp with time zone NOT NULL,
    "sita_group" character varying(16) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."sms_addressbook" (
    "book_id" integer NOT NULL,
    "user_id" integer DEFAULT 0 NOT NULL,
    "sms_name" character varying(128) DEFAULT ''::character varying NOT NULL,
    "sms_number" character varying(32) DEFAULT ''::character varying NOT NULL
);



CREATE SEQUENCE "webjet_cms"."sms_addressbook_book_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."sms_addressbook_book_id_seq" OWNED BY "webjet_cms"."sms_addressbook"."book_id";



CREATE TABLE "webjet_cms"."sms_log" (
    "log_id" integer NOT NULL,
    "user_id" integer DEFAULT 0 NOT NULL,
    "user_ip" character varying(32) DEFAULT ''::character varying NOT NULL,
    "sent_date" timestamp with time zone DEFAULT '2000-01-01 00:00:00+01'::timestamp with time zone NOT NULL,
    "sms_number" character varying(32) DEFAULT ''::character varying NOT NULL,
    "sms_text" character varying(255) DEFAULT ''::character varying NOT NULL
);



CREATE SEQUENCE "webjet_cms"."sms_log_log_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."sms_log_log_id_seq" OWNED BY "webjet_cms"."sms_log"."log_id";



CREATE TABLE "webjet_cms"."sms_template" (
    "sms_template_id" integer NOT NULL,
    "user_id" integer NOT NULL,
    "text" character varying(255) NOT NULL
);



CREATE TABLE "webjet_cms"."stat_browser" (
    "year" integer DEFAULT '0'::integer NOT NULL,
    "week" integer DEFAULT '0'::integer NOT NULL,
    "browser_id" character varying(32) DEFAULT '0'::character varying,
    "platform" character varying(25) DEFAULT '0'::character varying,
    "subplatform" character varying(20) DEFAULT NULL::character varying,
    "views" integer DEFAULT '0'::integer,
    "group_id" integer DEFAULT 1 NOT NULL
);



CREATE TABLE "webjet_cms"."stat_country" (
    "year" integer DEFAULT '0'::integer NOT NULL,
    "week" integer DEFAULT '0'::integer NOT NULL,
    "country_code" character varying(20) DEFAULT '0'::character varying NOT NULL,
    "views" integer DEFAULT '0'::integer NOT NULL,
    "group_id" integer DEFAULT 1 NOT NULL
);



CREATE TABLE "webjet_cms"."stat_doc" (
    "year" integer DEFAULT '0'::integer,
    "week" integer DEFAULT '0'::integer,
    "doc_id" integer DEFAULT '0'::integer,
    "views" integer DEFAULT '1'::integer,
    "in_count" integer DEFAULT '0'::integer,
    "out_count" integer DEFAULT '0'::integer,
    "view_time_sum" integer DEFAULT '0'::integer,
    "view_time_count" integer DEFAULT '0'::integer
);



CREATE TABLE "webjet_cms"."stat_error" (
    "year" integer DEFAULT '0'::integer,
    "week" integer DEFAULT '0'::integer,
    "url" character varying(255) DEFAULT '0'::character varying,
    "query_string" character varying(255) DEFAULT '0'::character varying,
    "count" integer DEFAULT '0'::integer
);



CREATE TABLE "webjet_cms"."stat_error_2024_2" (
    "year" integer NOT NULL,
    "week" integer NOT NULL,
    "url" character varying(255),
    "query_string" character varying(255),
    "count" integer DEFAULT 0
);



CREATE TABLE "webjet_cms"."stat_from" (
    "from_id" integer NOT NULL,
    "browser_id" bigint,
    "session_id" bigint,
    "referer_server_name" character varying(255) DEFAULT NULL::character varying,
    "referer_url" character varying(255) DEFAULT NULL::character varying,
    "from_time" timestamp with time zone,
    "doc_id" integer,
    "group_id" integer DEFAULT '0'::integer NOT NULL
);



CREATE SEQUENCE "webjet_cms"."stat_from_from_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."stat_from_from_id_seq" OWNED BY "webjet_cms"."stat_from"."from_id";



CREATE TABLE "webjet_cms"."stat_keys" (
    "stat_keys_id" integer NOT NULL,
    "value" character varying(64) NOT NULL
);



CREATE TABLE "webjet_cms"."stat_searchengine" (
    "search_date" timestamp with time zone DEFAULT '2000-01-01 00:00:00+01'::timestamp with time zone NOT NULL,
    "server" character varying(16) DEFAULT ''::character varying NOT NULL,
    "query" character varying(64) DEFAULT ''::character varying NOT NULL,
    "doc_id" integer DEFAULT 0 NOT NULL,
    "remote_host" character varying(255) DEFAULT NULL::character varying,
    "group_id" integer DEFAULT '0'::integer NOT NULL
);



CREATE TABLE "webjet_cms"."stat_site_days" (
    "year" integer DEFAULT '0'::integer,
    "week" integer DEFAULT '0'::integer,
    "views_mo" integer DEFAULT '0'::integer,
    "sessions_mo" integer DEFAULT '0'::integer,
    "views_tu" integer DEFAULT '0'::integer,
    "sessions_tu" integer DEFAULT '0'::integer,
    "views_we" integer DEFAULT '0'::integer,
    "sessions_we" integer DEFAULT '0'::integer,
    "views_th" integer DEFAULT '0'::integer,
    "sessions_th" integer DEFAULT '0'::integer,
    "views_fr" integer DEFAULT '0'::integer,
    "sessions_fr" integer DEFAULT '0'::integer,
    "views_sa" integer DEFAULT '0'::integer,
    "sessions_sa" integer DEFAULT '0'::integer,
    "views_su" integer DEFAULT '0'::integer,
    "sessions_su" integer DEFAULT '0'::integer,
    "view_time_sum" integer DEFAULT '0'::integer,
    "view_time_count" integer DEFAULT '0'::integer,
    "group_id" integer DEFAULT 1 NOT NULL
);



CREATE TABLE "webjet_cms"."stat_site_hours" (
    "year" integer DEFAULT '0'::integer NOT NULL,
    "week" integer DEFAULT '0'::integer NOT NULL,
    "views_0" integer DEFAULT '0'::integer,
    "views_1" integer DEFAULT '0'::integer,
    "views_2" integer DEFAULT '0'::integer,
    "views_3" integer DEFAULT '0'::integer,
    "views_4" integer DEFAULT '0'::integer,
    "views_5" integer DEFAULT '0'::integer,
    "views_6" integer DEFAULT '0'::integer,
    "views_7" integer DEFAULT '0'::integer,
    "views_8" integer DEFAULT '0'::integer,
    "views_9" integer DEFAULT '0'::integer,
    "views_10" integer DEFAULT '0'::integer,
    "views_11" integer DEFAULT '0'::integer,
    "views_12" integer DEFAULT '0'::integer,
    "views_13" integer DEFAULT '0'::integer,
    "views_14" integer DEFAULT '0'::integer,
    "views_15" integer DEFAULT '0'::integer,
    "views_16" integer DEFAULT '0'::integer,
    "views_17" integer DEFAULT '0'::integer,
    "views_18" integer DEFAULT '0'::integer,
    "views_19" integer DEFAULT '0'::integer,
    "views_20" integer DEFAULT '0'::integer,
    "views_21" integer DEFAULT '0'::integer,
    "views_22" integer DEFAULT '0'::integer,
    "views_23" integer DEFAULT '0'::integer,
    "sessions_0" integer DEFAULT '0'::integer,
    "sessions_1" integer DEFAULT '0'::integer,
    "sessions_2" integer DEFAULT '0'::integer,
    "sessions_3" integer DEFAULT '0'::integer,
    "sessions_4" integer DEFAULT '0'::integer,
    "sessions_5" integer DEFAULT '0'::integer,
    "sessions_6" integer DEFAULT '0'::integer,
    "sessions_7" integer DEFAULT '0'::integer,
    "sessions_8" integer DEFAULT '0'::integer,
    "sessions_9" integer DEFAULT '0'::integer,
    "sessions_10" integer DEFAULT '0'::integer,
    "sessions_11" integer DEFAULT '0'::integer,
    "sessions_12" integer DEFAULT '0'::integer,
    "sessions_13" integer DEFAULT '0'::integer,
    "sessions_14" integer DEFAULT '0'::integer,
    "sessions_15" integer DEFAULT '0'::integer,
    "sessions_16" integer DEFAULT '0'::integer,
    "sessions_17" integer DEFAULT '0'::integer,
    "sessions_18" integer DEFAULT '0'::integer,
    "sessions_19" integer DEFAULT '0'::integer,
    "sessions_20" integer DEFAULT '0'::integer,
    "sessions_21" integer DEFAULT '0'::integer,
    "sessions_22" integer DEFAULT '0'::integer,
    "sessions_23" integer DEFAULT '0'::integer,
    "group_id" integer DEFAULT 1 NOT NULL
);



CREATE TABLE "webjet_cms"."stat_userlogon" (
    "year" integer DEFAULT 0 NOT NULL,
    "week" integer DEFAULT 0 NOT NULL,
    "user_id" integer DEFAULT 0 NOT NULL,
    "views" integer DEFAULT 1,
    "logon_time" timestamp with time zone,
    "view_minutes" integer DEFAULT '0'::integer,
    "hostname" character varying(255) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."stat_views" (
    "view_id" integer NOT NULL,
    "browser_id" bigint,
    "session_id" bigint,
    "doc_id" integer,
    "last_doc_id" integer,
    "view_time" timestamp with time zone,
    "group_id" integer DEFAULT '0'::integer NOT NULL,
    "last_group_id" integer DEFAULT '0'::integer NOT NULL,
    "browser_ua_id" integer,
    "platform_id" integer,
    "subplatform_id" integer,
    "country" character varying(4) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."stat_views_2024_2" (
    "view_id" integer NOT NULL,
    "browser_id" integer,
    "session_id" integer,
    "doc_id" integer,
    "last_doc_id" integer,
    "view_time" timestamp without time zone,
    "group_id" integer,
    "last_group_id" integer,
    "browser_ua_id" integer,
    "platform_id" integer,
    "subplatform_id" integer,
    "country" character varying(4)
);



ALTER TABLE "webjet_cms"."stat_views_2024_2" ALTER COLUMN "view_id" ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME "webjet_cms"."stat_views_2024_2_view_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE SEQUENCE "webjet_cms"."stat_views_view_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."stat_views_view_id_seq" OWNED BY "webjet_cms"."stat_views"."view_id";



CREATE TABLE "webjet_cms"."stopword" (
    "word" character varying(255) DEFAULT NULL::character varying,
    "language" character varying(2) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."templates" (
    "temp_id" integer NOT NULL,
    "temp_name" character varying(64) DEFAULT ''::character varying NOT NULL,
    "forward" character varying(64) DEFAULT ''::character varying NOT NULL,
    "lng" character varying(16) DEFAULT 'sk'::character varying NOT NULL,
    "header_doc_id" integer DEFAULT '0'::integer NOT NULL,
    "footer_doc_id" integer DEFAULT '0'::integer NOT NULL,
    "after_body_data" "text",
    "css" character varying(4000) DEFAULT NULL::character varying,
    "menu_doc_id" integer DEFAULT '-1'::integer NOT NULL,
    "right_menu_doc_id" integer DEFAULT '-1'::integer,
    "base_css_path" character varying(4000) DEFAULT NULL::character varying,
    "object_a_doc_id" integer,
    "object_b_doc_id" integer,
    "object_c_doc_id" integer,
    "object_d_doc_id" integer,
    "available_groups" character varying(255) DEFAULT NULL::character varying,
    "template_install_name" character varying(64) DEFAULT NULL::character varying,
    "disable_spam_protection" boolean DEFAULT false,
    "templates_group_id" integer DEFAULT '1'::integer NOT NULL,
    "inline_editing_mode" character varying(16) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."templates_group" (
    "templates_group_id" integer NOT NULL,
    "name" character varying(255) DEFAULT NULL::character varying,
    "directory" character varying(255) DEFAULT NULL::character varying,
    "key_prefix" character varying(128) DEFAULT NULL::character varying,
    "inline_editing_mode" character varying(16) DEFAULT NULL::character varying
);



CREATE SEQUENCE "webjet_cms"."templates_temp_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."templates_temp_id_seq" OWNED BY "webjet_cms"."templates"."temp_id";



CREATE TABLE "webjet_cms"."terminologicky_slovnik" (
    "terminologicky_slovnik_id" integer NOT NULL,
    "termin" character varying(255) DEFAULT NULL::character varying,
    "synonymum" character varying(255) DEFAULT NULL::character varying,
    "kategoria" "text",
    "definicia" "text",
    "poznamka1" "text",
    "poznamka2" "text",
    "zdroj1" "text",
    "zdroj2" "text",
    "priklad" "text"
);



CREATE TABLE "webjet_cms"."tips_of_the_day" (
    "tip_id" integer NOT NULL,
    "tip_group" character varying(255) NOT NULL,
    "tip_text" "text" NOT NULL
);



CREATE SEQUENCE "webjet_cms"."tips_of_the_day_tip_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."tips_of_the_day_tip_id_seq" OWNED BY "webjet_cms"."tips_of_the_day"."tip_id";



CREATE TABLE "webjet_cms"."todo" (
    "todo_id" integer NOT NULL,
    "user_id" integer NOT NULL,
    "create_date" timestamp with time zone NOT NULL,
    "modif_date" timestamp with time zone,
    "text" character varying(128) NOT NULL,
    "is_global" boolean DEFAULT false NOT NULL,
    "is_resolved" boolean DEFAULT false NOT NULL,
    "sort_priority" integer,
    "dead_line" timestamp with time zone,
    "note" character varying(2000) DEFAULT NULL::character varying,
    "group_id" integer DEFAULT '0'::integer
);



CREATE TABLE "webjet_cms"."url_redirect" (
    "url_redirect_id" integer NOT NULL,
    "old_url" character varying(255) NOT NULL,
    "new_url" character varying(255) NOT NULL,
    "redirect_code" integer NOT NULL,
    "insert_date" timestamp with time zone NOT NULL,
    "domain_name" character varying(255) DEFAULT NULL::character varying,
    "publish_date" timestamp with time zone
);



CREATE TABLE "webjet_cms"."user_alarm" (
    "user_id" integer DEFAULT 0 NOT NULL,
    "alarm_id" integer,
    "warning" integer,
    "send_date" timestamp with time zone
);



CREATE TABLE "webjet_cms"."user_disabled_items" (
    "user_id" integer DEFAULT 0 NOT NULL,
    "item_name" character varying(64) NOT NULL
);



CREATE TABLE "webjet_cms"."user_group_verify" (
    "verify_id" integer NOT NULL,
    "user_id" integer DEFAULT 0 NOT NULL,
    "user_groups" character varying(255) DEFAULT ''::character varying NOT NULL,
    "hash" character varying(32) DEFAULT ''::character varying NOT NULL,
    "create_date" timestamp with time zone DEFAULT '2000-01-01 00:00:00+01'::timestamp with time zone NOT NULL,
    "verify_date" timestamp with time zone,
    "email" character varying(255) DEFAULT ''::character varying NOT NULL,
    "hostname" character varying(255) DEFAULT NULL::character varying
);



CREATE SEQUENCE "webjet_cms"."user_group_verify_verify_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."user_group_verify_verify_id_seq" OWNED BY "webjet_cms"."user_group_verify"."verify_id";



CREATE TABLE "webjet_cms"."user_groups" (
    "user_group_id" integer NOT NULL,
    "user_group_name" character varying(255) DEFAULT ''::character varying NOT NULL,
    "user_group_type" smallint DEFAULT '0'::smallint NOT NULL,
    "user_group_comment" "text",
    "require_approve" boolean DEFAULT false NOT NULL,
    "email_doc_id" integer DEFAULT '-1'::integer NOT NULL,
    "allow_user_edit" boolean DEFAULT false,
    "require_email_verification" boolean
);



CREATE SEQUENCE "webjet_cms"."user_groups_user_group_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."user_groups_user_group_id_seq" OWNED BY "webjet_cms"."user_groups"."user_group_id";



CREATE TABLE "webjet_cms"."user_perm_groups" (
    "group_id" integer NOT NULL,
    "group_title" character varying(255) DEFAULT NULL::character varying,
    "writable_folders" character varying(600) DEFAULT NULL::character varying,
    "editable_groups" character varying(255) DEFAULT NULL::character varying,
    "editable_pages" character varying(255) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."user_perm_groups_perms" (
    "perm_group_id" integer NOT NULL,
    "perm_id" integer NOT NULL,
    "permission" character varying(64) NOT NULL
);



CREATE TABLE "webjet_cms"."user_settings" (
    "user_settings_id" integer NOT NULL,
    "user_id" integer NOT NULL,
    "skey" character varying(64) DEFAULT NULL::character varying,
    "svalue1" character varying(2000) DEFAULT NULL::character varying,
    "svalue2" character varying(255) DEFAULT NULL::character varying,
    "svalue3" character varying(255) DEFAULT NULL::character varying,
    "svalue4" character varying(255) DEFAULT NULL::character varying,
    "sint1" integer,
    "sint2" integer,
    "sint3" integer,
    "sint4" integer,
    "sdate" timestamp with time zone
);



CREATE TABLE "webjet_cms"."user_settings_admin" (
    "user_settings_admin_id" integer NOT NULL,
    "user_id" integer NOT NULL,
    "skey" character varying(255) DEFAULT NULL::character varying,
    "value" character varying(4000) DEFAULT NULL::character varying
);



CREATE SEQUENCE "webjet_cms"."user_settings_admin_user_settings_admin_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."user_settings_admin_user_settings_admin_id_seq" OWNED BY "webjet_cms"."user_settings_admin"."user_settings_admin_id";



CREATE SEQUENCE "webjet_cms"."user_settings_user_settings_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."user_settings_user_settings_id_seq" OWNED BY "webjet_cms"."user_settings"."user_settings_id";



CREATE TABLE "webjet_cms"."users" (
    "user_id" integer NOT NULL,
    "title" character varying(16) DEFAULT NULL::character varying,
    "first_name" character varying(128) DEFAULT NULL::character varying,
    "last_name" character varying(255) DEFAULT NULL::character varying,
    "login" character varying(128) NOT NULL,
    "password" character varying(128) DEFAULT NULL::character varying,
    "is_admin" boolean DEFAULT false NOT NULL,
    "user_groups" character varying(255) DEFAULT NULL::character varying,
    "company" character varying(255) DEFAULT NULL::character varying,
    "adress" character varying(255) DEFAULT NULL::character varying,
    "city" character varying(255) DEFAULT NULL::character varying,
    "email" character varying(255) DEFAULT NULL::character varying,
    "psc" character varying(20) DEFAULT NULL::character varying,
    "country" character varying(255) DEFAULT NULL::character varying,
    "phone" character varying(255) DEFAULT NULL::character varying,
    "authorized" boolean,
    "editable_groups" character varying(255) DEFAULT NULL::character varying,
    "editable_pages" character varying(255) DEFAULT NULL::character varying,
    "writable_folders" "text",
    "last_logon" timestamp with time zone,
    "module_perms" character varying(255) DEFAULT NULL::character varying,
    "disabled_items" character varying(255) DEFAULT NULL::character varying,
    "reg_date" timestamp with time zone,
    "field_a" character varying(255) DEFAULT NULL::character varying,
    "field_b" character varying(255) DEFAULT NULL::character varying,
    "field_c" character varying(255) DEFAULT NULL::character varying,
    "field_d" character varying(255) DEFAULT NULL::character varying,
    "field_e" character varying(255) DEFAULT NULL::character varying,
    "date_of_birth" "date",
    "sex_male" boolean DEFAULT true,
    "photo" character varying(255) DEFAULT NULL::character varying,
    "signature" character varying(255) DEFAULT NULL::character varying,
    "forum_rank" integer DEFAULT '0'::integer NOT NULL,
    "rating_rank" integer DEFAULT '0'::integer NOT NULL,
    "allow_login_start" "date",
    "allow_login_end" "date",
    "authorize_hash" character varying(32) DEFAULT NULL::character varying,
    "fax" character varying(255) DEFAULT NULL::character varying,
    "delivery_first_name" character varying(64) DEFAULT NULL::character varying,
    "delivery_last_name" character varying(64) DEFAULT NULL::character varying,
    "delivery_company" character varying(64) DEFAULT NULL::character varying,
    "delivery_adress" character varying(128) DEFAULT NULL::character varying,
    "delivery_city" character varying(64) DEFAULT NULL::character varying,
    "delivery_psc" character varying(8) DEFAULT NULL::character varying,
    "delivery_country" character varying(32) DEFAULT NULL::character varying,
    "delivery_phone" character varying(32) DEFAULT NULL::character varying,
    "position" character varying(255) DEFAULT NULL::character varying,
    "parent_id" integer DEFAULT '0'::integer NOT NULL,
    "password_salt" character varying(64) DEFAULT NULL::character varying,
    "domain_id" integer DEFAULT '1'::integer NOT NULL,
    "mobile_device" character varying(255) DEFAULT NULL::character varying,
    "api_key" character varying(255) DEFAULT NULL::character varying
);



CREATE TABLE "webjet_cms"."users_in_perm_groups" (
    "user_id" integer NOT NULL,
    "perm_group_id" integer NOT NULL
);



CREATE SEQUENCE "webjet_cms"."users_user_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."users_user_id_seq" OWNED BY "webjet_cms"."users"."user_id";



CREATE TABLE "webjet_cms"."zmluvy" (
    "zmluvy_id" integer NOT NULL,
    "cislo_zmluvy" character varying(128) DEFAULT NULL::character varying,
    "cislo_rzupsk" character varying(255) DEFAULT NULL::character varying,
    "nazov" character varying(512) DEFAULT NULL::character varying,
    "predmet" "text",
    "hodnota_zmluvy" double precision,
    "mena" character varying(3) DEFAULT NULL::character varying,
    "podpisanie" timestamp with time zone,
    "platnost_od" timestamp with time zone,
    "ucinnost" timestamp with time zone,
    "zverejnenie" timestamp with time zone,
    "zdroj" character varying(255) DEFAULT NULL::character varying,
    "zdroj_nazov" character varying(255) DEFAULT NULL::character varying,
    "editor" integer,
    "skupina" smallint,
    "status" smallint,
    "platnost_do" timestamp with time zone,
    "viazanost_k_zmluve" integer,
    "zmluvna_strana1" character varying(255) DEFAULT NULL::character varying,
    "zs_nazov1" character varying(255) DEFAULT NULL::character varying,
    "zs_sidlo1" character varying(255) DEFAULT NULL::character varying,
    "zs_ico1" character varying(255) DEFAULT NULL::character varying,
    "zs_osoba1" character varying(255) DEFAULT NULL::character varying,
    "zmluvna_strana2" character varying(255) DEFAULT NULL::character varying,
    "zs_nazov2" character varying(255) DEFAULT NULL::character varying,
    "zs_sidlo2" character varying(255) DEFAULT NULL::character varying,
    "zs_ico2" character varying(255) DEFAULT NULL::character varying,
    "zs_osoba2" character varying(255) DEFAULT NULL::character varying,
    "zmluvna_strana3" character varying(255) DEFAULT NULL::character varying,
    "zs_nazov3" character varying(255) DEFAULT NULL::character varying,
    "zs_sidlo3" character varying(255) DEFAULT NULL::character varying,
    "zs_ico3" character varying(255) DEFAULT NULL::character varying,
    "zs_osoba3" character varying(255) DEFAULT NULL::character varying,
    "cena_s_dph" boolean DEFAULT true,
    "dodatok" boolean DEFAULT false,
    "poznamka" "text",
    "zobrazovat" boolean DEFAULT true,
    "vytvoril" integer DEFAULT '0'::integer NOT NULL,
    "sposob_uhrady" character varying(255) DEFAULT NULL::character varying,
    "domain_id" integer DEFAULT 1,
    "organizacia_id" integer DEFAULT '0'::integer NOT NULL
);



CREATE TABLE "webjet_cms"."zmluvy_organizacia" (
    "zmluvy_organizacia_id" integer NOT NULL,
    "nazov" character varying(255) NOT NULL,
    "domain_id" integer DEFAULT '1'::integer
);



CREATE TABLE "webjet_cms"."zmluvy_organizacia_approvers" (
    "organizacia_id" integer NOT NULL,
    "user_id" integer NOT NULL
);



CREATE TABLE "webjet_cms"."zmluvy_organizacia_users" (
    "organizacia_id" integer NOT NULL,
    "user_id" integer NOT NULL
);



CREATE TABLE "webjet_cms"."zmluvy_prilohy" (
    "zmluvy_prilohy_id" integer NOT NULL,
    "zmluvy_id" integer,
    "zdroj" character varying(255) DEFAULT NULL::character varying,
    "nazov" character varying(255) DEFAULT NULL::character varying
);



CREATE SEQUENCE "webjet_cms"."zmluvy_prilohy_zmluvy_prilohy_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."zmluvy_prilohy_zmluvy_prilohy_id_seq" OWNED BY "webjet_cms"."zmluvy_prilohy"."zmluvy_prilohy_id";



CREATE SEQUENCE "webjet_cms"."zmluvy_zmluvy_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE "webjet_cms"."zmluvy_zmluvy_id_seq" OWNED BY "webjet_cms"."zmluvy"."zmluvy_id";



ALTER TABLE ONLY "webjet_cms"."_adminlog_" ALTER COLUMN "log_id" SET DEFAULT "nextval"('"webjet_cms"."_adminlog__log_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."_conf_prepared_" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."_conf_prepared__id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."_db_" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."_db__id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."_modules_" ALTER COLUMN "module_id" SET DEFAULT "nextval"('"webjet_cms"."_modules__module_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."_properties_" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."_properties__id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."adminlog_notify" ALTER COLUMN "adminlog_notify_id" SET DEFAULT "nextval"('"webjet_cms"."adminlog_notify_adminlog_notify_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."appcache" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."appcache_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."appcache_file" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."appcache_file_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."appcache_page" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."appcache_page_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."banner_doc" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."banner_doc_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."banner_gr" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."banner_gr_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."banner_stat_clicks" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."banner_stat_clicks_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."banner_stat_views" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."banner_stat_views_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."banner_stat_views_day" ALTER COLUMN "view_id" SET DEFAULT "nextval"('"webjet_cms"."banner_stat_views_day_view_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."basket_invoice_payments" ALTER COLUMN "payment_id" SET DEFAULT "nextval"('"webjet_cms"."basket_invoice_payments_payment_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."bazar_advertisements" ALTER COLUMN "ad_id" SET DEFAULT "nextval"('"webjet_cms"."bazar_advertisements_ad_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."bazar_groups" ALTER COLUMN "group_id" SET DEFAULT "nextval"('"webjet_cms"."bazar_groups_group_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."calendar" ALTER COLUMN "calendar_id" SET DEFAULT "nextval"('"webjet_cms"."calendar_calendar_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."calendar_invitation" ALTER COLUMN "calendar_invitation_id" SET DEFAULT "nextval"('"webjet_cms"."calendar_invitation_calendar_invitation_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."calendar_name_in_year" ALTER COLUMN "calendar_id" SET DEFAULT "nextval"('"webjet_cms"."calendar_name_in_year_calendar_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."calendar_types" ALTER COLUMN "type_id" SET DEFAULT "nextval"('"webjet_cms"."calendar_types_type_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."chat_rooms" ALTER COLUMN "room_id" SET DEFAULT "nextval"('"webjet_cms"."chat_rooms_room_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."cluster_refresher" ALTER COLUMN "cluster_refresh_id" SET DEFAULT "nextval"('"webjet_cms"."cluster_refresher_cluster_refresh_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."crontab" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."crontab_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."doc_atr" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."doc_atr_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."doc_atr_def" ALTER COLUMN "atr_id" SET DEFAULT "nextval"('"webjet_cms"."doc_atr_def_atr_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."doc_reactions" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."doc_reactions_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."doc_subscribe" ALTER COLUMN "subscribe_id" SET DEFAULT "nextval"('"webjet_cms"."doc_subscribe_subscribe_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."document_forum" ALTER COLUMN "forum_id" SET DEFAULT "nextval"('"webjet_cms"."document_forum_forum_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."documents" ALTER COLUMN "doc_id" SET DEFAULT "nextval"('"webjet_cms"."documents_doc_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."documents_history" ALTER COLUMN "history_id" SET DEFAULT "nextval"('"webjet_cms"."documents_history_history_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."domain_limits" ALTER COLUMN "domain_limit_id" SET DEFAULT "nextval"('"webjet_cms"."domain_limits_domain_limit_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."email_files" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."email_files_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."emails" ALTER COLUMN "email_id" SET DEFAULT "nextval"('"webjet_cms"."emails_email_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."emails_campain" ALTER COLUMN "emails_campain_id" SET DEFAULT "nextval"('"webjet_cms"."emails_campain_emails_campain_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."emails_stat_click" ALTER COLUMN "click_id" SET DEFAULT "nextval"('"webjet_cms"."emails_stat_click_click_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."emails_unsubscribed" ALTER COLUMN "emails_unsubscribed_id" SET DEFAULT "nextval"('"webjet_cms"."emails_unsubscribed_emails_unsubscribed_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."file_archiv_category_node" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."file_archiv_category_node_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."file_atr_def" ALTER COLUMN "atr_id" SET DEFAULT "nextval"('"webjet_cms"."file_atr_def_atr_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."form_regular_exp" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."form_regular_exp_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."forms" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."forms_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."forum" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."forum_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."gallery" ALTER COLUMN "image_id" SET DEFAULT "nextval"('"webjet_cms"."gallery_image_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."gallery_dimension" ALTER COLUMN "dimension_id" SET DEFAULT "nextval"('"webjet_cms"."gallery_dimension_dimension_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."groups" ALTER COLUMN "group_id" SET DEFAULT "nextval"('"webjet_cms"."groups_group_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."groups_approve" ALTER COLUMN "approve_id" SET DEFAULT "nextval"('"webjet_cms"."groups_approve_approve_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."groups_scheduler" ALTER COLUMN "schedule_id" SET DEFAULT "nextval"('"webjet_cms"."groups_scheduler_schedule_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."inquiry" ALTER COLUMN "question_id" SET DEFAULT "nextval"('"webjet_cms"."inquiry_question_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."inquiry_answers" ALTER COLUMN "answer_id" SET DEFAULT "nextval"('"webjet_cms"."inquiry_answers_answer_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."inquirysimple_answers" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."inquirysimple_answers_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."monitoring" ALTER COLUMN "monitoring_id" SET DEFAULT "nextval"('"webjet_cms"."monitoring_monitoring_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."perex_groups" ALTER COLUMN "perex_group_id" SET DEFAULT "nextval"('"webjet_cms"."perex_groups_perex_group_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."proxy" ALTER COLUMN "proxy_id" SET DEFAULT "nextval"('"webjet_cms"."proxy_proxy_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."questions_answers" ALTER COLUMN "qa_id" SET DEFAULT "nextval"('"webjet_cms"."questions_answers_qa_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."quiz" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."quiz_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."quiz_answers" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."quiz_answers_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."quiz_questions" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."quiz_questions_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."quiz_results" ALTER COLUMN "id" SET DEFAULT "nextval"('"webjet_cms"."quiz_results_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."rating" ALTER COLUMN "rating_id" SET DEFAULT "nextval"('"webjet_cms"."rating_rating_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."reservation" ALTER COLUMN "reservation_id" SET DEFAULT "nextval"('"webjet_cms"."reservation_reservation_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."reservation_object" ALTER COLUMN "reservation_object_id" SET DEFAULT "nextval"('"webjet_cms"."reservation_object_reservation_object_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."response_headers" ALTER COLUMN "response_header_id" SET DEFAULT "nextval"('"webjet_cms"."response_headers_response_header_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."seo_bots" ALTER COLUMN "seo_bots_id" SET DEFAULT "nextval"('"webjet_cms"."seo_bots_seo_bots_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."seo_google_position" ALTER COLUMN "seo_google_position_id" SET DEFAULT "nextval"('"webjet_cms"."seo_google_position_seo_google_position_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."seo_keywords" ALTER COLUMN "seo_keyword_id" SET DEFAULT "nextval"('"webjet_cms"."seo_keywords_seo_keyword_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."sms_addressbook" ALTER COLUMN "book_id" SET DEFAULT "nextval"('"webjet_cms"."sms_addressbook_book_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."sms_log" ALTER COLUMN "log_id" SET DEFAULT "nextval"('"webjet_cms"."sms_log_log_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."stat_from" ALTER COLUMN "from_id" SET DEFAULT "nextval"('"webjet_cms"."stat_from_from_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."stat_views" ALTER COLUMN "view_id" SET DEFAULT "nextval"('"webjet_cms"."stat_views_view_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."templates" ALTER COLUMN "temp_id" SET DEFAULT "nextval"('"webjet_cms"."templates_temp_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."tips_of_the_day" ALTER COLUMN "tip_id" SET DEFAULT "nextval"('"webjet_cms"."tips_of_the_day_tip_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."user_group_verify" ALTER COLUMN "verify_id" SET DEFAULT "nextval"('"webjet_cms"."user_group_verify_verify_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."user_groups" ALTER COLUMN "user_group_id" SET DEFAULT "nextval"('"webjet_cms"."user_groups_user_group_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."user_settings" ALTER COLUMN "user_settings_id" SET DEFAULT "nextval"('"webjet_cms"."user_settings_user_settings_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."user_settings_admin" ALTER COLUMN "user_settings_admin_id" SET DEFAULT "nextval"('"webjet_cms"."user_settings_admin_user_settings_admin_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."users" ALTER COLUMN "user_id" SET DEFAULT "nextval"('"webjet_cms"."users_user_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."zmluvy" ALTER COLUMN "zmluvy_id" SET DEFAULT "nextval"('"webjet_cms"."zmluvy_zmluvy_id_seq"'::"regclass");



ALTER TABLE ONLY "webjet_cms"."zmluvy_prilohy" ALTER COLUMN "zmluvy_prilohy_id" SET DEFAULT "nextval"('"webjet_cms"."zmluvy_prilohy_zmluvy_prilohy_id_seq"'::"regclass");


INSERT INTO "webjet_cms"."_conf_" ("name", "value", "date_changed") VALUES ('defaultDisableUpload', 'false', NULL);
INSERT INTO "webjet_cms"."_conf_" ("name", "value", "date_changed") VALUES ('showDocActionAllowedDocids', '4', NULL);
INSERT INTO "webjet_cms"."_conf_" ("name", "value", "date_changed") VALUES ('inlineEditingEnabled', 'true', NULL);
INSERT INTO "webjet_cms"."_conf_" ("name", "value", "date_changed") VALUES ('disableWebJETToolbar', 'true', NULL);
INSERT INTO "webjet_cms"."_conf_" ("name", "value", "date_changed") VALUES ('logLevel', 'debug', NULL);
INSERT INTO "webjet_cms"."_conf_" ("name", "value", "date_changed") VALUES ('galleryWebJET7Converted', 'true', '2024-02-14 15:00:55.902+01');
INSERT INTO "webjet_cms"."_conf_" ("name", "value", "date_changed") VALUES ('statEnableTablePartitioning', 'true', '2024-02-14 15:00:57.302+01');
INSERT INTO "webjet_cms"."_conf_" ("name", "value", "date_changed") VALUES ('statWebJET7Converted', 'true', '2024-02-14 15:00:57.314+01');
INSERT INTO "webjet_cms"."_conf_" ("name", "value", "date_changed") VALUES ('passwordUseHash', 'true', '2024-02-14 15:01:02.974+01');
INSERT INTO "webjet_cms"."_conf_" ("name", "value", "date_changed") VALUES ('statTablePartitioningDate-stat_error', '14.02.2024 15:04', '2024-02-14 15:04:24.537+01');


INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (1, '2003-12-06', 'ukladanie poznamky a prihlaseneho usera k formularom');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (2, '2003-12-12', 'sposob zobrazenia menu pre adresar');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (3, '2003-12-21', 'atributy suborov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (4, '2003-12-29', 'od teraz sa kontroluje aj admin, ci je autorizovany, takze nastavime default');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (5, '2004-01-04', 'uklada k formularu aj docid (ak sa podari zistit)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (6, '2004-01-09', 'typ skupiny pouzivatelov, 0=perms, 1=email, 2=...');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (7, '2004-01-10', 'email je mozne posielat uz len ako URL, text sa priamo napisat neda');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (8, '2004-01-11', 'verifikacia subscribe k email newslettrom, po autorizacii emailom sa user_groups zapise do tabulky users');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (9, '2004-01-13', 'zoznam foldrov (/images/nieco...) do ktorych ma user pravo nahravat obrazky a subory');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (10, '2004-01-25', 'volne pouzitelne polia pre kalendar podujati');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (11, '2004-02-11', 'casova notifikacia pre kalendar podujati');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (12, '2004-02-15', 'virtualne cesty k strankam, napr. www.server.sk/products');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (13, '2004-02-17', 'uvodny text notifikacie kalendara, moznost poslat SMS');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (14, '2004-02-24', 'ak je true, dava navstevnik suhlas na zobrazenie na webe');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (15, '2004-02-28', 'urychlenie statistiky');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (16, '2004-01-03', 'zvacsenie poli');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (17, '2004-03-03', 'urychlenie nacitania virtual paths');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (18, '2004-03-05', 'konfiguracia webjetu (namiesto web.xml)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (19, '2004-03-07', 'disabled items pouzivatelov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (20, '2004-03-07', 'rozdelenie full name na meno a priezvisko');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (21, '2004-03-08', 'volne pouzitelne polozky');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (22, '2004-03-12', 'url nazov adresara');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (23, '2004-03-15', 'implemetacia rozdelenia full name');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (24, '2004-03-15', 'Konverzia pristupovych prav');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (25, '2004-03-18', 'custom zmena textov v properties suboroch');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (26, '2004-03-27', 'uprava statistik (eviduje sa id adresara)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (27, '2004-03-28', 'statistika query vo vyhladavacoch');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (28, '2004-04-05', 'mod schvalovania adresara (0=approve, 1=notify, 2=none)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (29, '2004-03-05', 'konfiguracia webjetu (namiesto web.xml)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (30, '2004-03-07', 'disabled items pouzivatelov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (31, '2004-03-07', 'rozdelenie full name na meno a priezvisko');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (32, '2004-03-08', 'volne pouzitelne polozky');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (33, '2004-03-12', 'url nazov adresara');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (34, '2004-03-18', 'custom zmena textov v properties suboroch');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (35, '2004-03-27', 'uprava statistik (eviduje sa id adresara)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (36, '2004-03-28', 'statistika query vo vyhladavacoch');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (37, '2004-04-05', 'mod schvalovania adresara (0=approve, 1=notify, 2=none)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (38, '2004-05-01', 'id a stav synchronizacie (status: 0=novy, 1=updated, 2=synchronized)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (39, '2004-05-02', 'konfiguracia custom modulov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (40, '2004-05-03', 'modul posielania SMS sprav');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (41, '2004-05-09', 'vyzadovanie schvalovania registracie, doc_id pre zasielany email');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (42, '2024-01-18', '18.5.2004 [jeeff] vo vyhladavani statistiky sa eviduje remote host');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (43, '2024-01-18', '24.5.2004 [jeeff] tabulka s tipmi dna');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (44, '2024-01-18', '9.6.2004 [joruz] zoznam alarmov pre notifikaciu registracie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (45, '2024-01-18', '9.6.2004 [joruz] alarm pouzivatela pre notifikaciu registracie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (46, '2024-01-18', '9.8.2004 [jeeff] html kod do hlavicky pre adresar');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (47, '2024-01-18', '10.8.2004 [jeeff] kalendar s meninami');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (48, '2024-01-18', '10.8.2004 [jeeff] banner system - banner_banners');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (49, '2024-01-18', '10.8.2004 [jeeff] banner system - banner_stat_clicks');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (50, '2024-01-18', '10.8.2004 [jeeff] banner system - banner_stat_views');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (51, '2024-01-18', '18.8.2004 [joruz] casovanie ankiet-zaciatok');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (52, '2024-01-18', '18.8.2004 [joruz] casovanie ankiet-koniec');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (53, '2024-01-18', '22.8.2004 [jeeff] id stranky s prihlasovacim dialogom');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (54, '2024-01-18', '22.8.2004 [jeeff] id stranky s prihlasovacim dialogom-documents');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (55, '2024-01-18', '22.8.2004 [jeeff] id stranky s prihlasovacim dialogom-documents_history');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (56, '2024-01-18', '11.9.2004 [jeeff] docid menu na pravej strane');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (57, '2024-01-18', '11.9.2004 [jeeff] docid menu na pravej strane-documents');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (58, '2024-01-18', '11.9.2004 [jeeff] docid menu na pravej strane-documents_history');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (59, '2024-01-18', '14.9.2004 [joruz] anketa - aktivny / neaktivny stav');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (60, '2024-01-18', '25.9.2004 [joruz] hodnotenie stranok');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (61, '2024-01-18', '29.9.2004 [jeeff] generator primarnych klucov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (62, '2024-01-18', '29.9.2004 [jeeff] generator primarnych klucov - documents');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (63, '2024-01-18', '4.10.2004 [joruz] tabulka s perex skupinami');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (64, '2024-01-18', '15.10.2004 [jeeff] zoznam miestnosti pre chat');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (65, '2024-01-18', '15.10.2004 [jeeff] default miestnost pre chat');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (66, '2024-01-18', '15.10.2004 [jeeff] moderovana miestnost pre chat');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (67, '2024-01-18', '16.10.2004 [jeeff] crontab');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (68, '2024-01-18', '16.10.2004 [jeeff] crontab - CalendarDB.sendNotify');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (69, '2024-01-18', '16.10.2004 [jeeff] crontab - RegAlarm.regAlarm');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (70, '2024-01-18', '16.11.2004 [jeeff] email - cas a datum, kedy sa ma odoslat');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (71, '2024-01-18', '17.11.2004 [jeeff] email - text emailu (ak sa neodosiela URL)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (72, '2024-01-18', '19.11.2004 [jeeff] email - replyTo');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (73, '2024-01-18', '19.11.2004 [jeeff] email - ccEmail');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (74, '2024-01-18', '19.11.2004 [jeeff] email - bccEmail');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (75, '2024-01-18', '8.12.2004 [jeeff] zakladny CSS styl pre sablonu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (76, '2024-01-18', '10.12.2004 [joruz] komplet statistika - views');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (77, '2024-01-18', '10.12.2004 [joruz] komplet statistika - from');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (78, '2024-01-18', '10.12.2004 [joruz] komplet statistika - stat_browser_id - pkey');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (79, '2024-01-18', '10.12.2004 [joruz] komplet statistika - stat_session_id - pkey');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (80, '2024-01-18', '17.12.2004 [joruz] sprava fora');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (81, '2024-01-18', '21.12.2004 [jeeff] vlastnosti adresara na disku (indexacia, prava)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (82, '2024-01-18', '7.1.2005 [joruz] schvalovanie diskusnych prispevkov (document_forum confirmed)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (83, '2024-01-18', '7.1.2005 [joruz] schvalovanie diskusnych prispevkov (forum message_confirmation)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (84, '2024-01-18', '7.1.2005 [joruz] schvalovanie diskusnych prispevkov (document_forum hash_code)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (85, '2024-01-18', '7.1.2005 [joruz] schvalovanie diskusnych prispevkov (forum approve_email)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (86, '2024-01-18', '25.1.2005 [jeeff] pkey generator - banner_id');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (87, '2024-01-18', '28.1.2005 [joruz] bazar - bazar_groups');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (88, '2024-01-18', '28.1.2005 [joruz] bazar - bazar_advertisements');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (89, '2024-01-18', '6.2.2005 [jeeff] custom fields - documents');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (90, '2024-01-18', '6.2.2005 [jeeff] custom fields - documents_history');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (91, '2024-01-18', '7.2.2005 [jeeff] adminlog');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (92, '2024-01-18', '15.2.2005 [joruz] forum - email nofifikacie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (93, '2024-01-18', '15.2.2005 [jeeff] adminlog - ip a hostname');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (94, '2024-01-18', '31.3.2005 [jeeff] banner_banners - zrusenie identity stlpcov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (95, '2024-01-18', '15.4.2005 [joruz] forum - uprava na message board');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (96, '2024-01-18', '15.4.2005 [jeeff] users - udaje o datume narodenia, pohlavi atd');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (97, '2024-01-18', '20.4.2005 [jeeff] custom fields - documents (dalsich 6)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (98, '2024-01-18', '20.4.2005 [jeeff] custom fields - documents_history (dalsich 6)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (99, '2024-01-18', '12.5.2005 [joruz] users - premenovanie sex fieldu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (100, '2024-01-18', '19.5.2005 [joruz] user_groups - ak je 1, pouzivatel si moze danu skupinu priradit/odobrat');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (101, '2024-01-18', '27.5.2005 [jeeff] emails - ak je 1 tak sa nebude email odosielat (zostane cakat)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (102, '2024-01-18', '22.6.2005 [joruz] doc_subscribe - tabulka pre subscribe notifikacie o zmene stranky (komponenta docSubscribeInfo)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (103, '2024-01-18', '30.6.2005 [joruz] forum - typ prispevku');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (104, '2024-01-18', '30.6.2005 [joruz] user - rank pre forum');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (105, '2024-01-18', '10.7.2005 [jeeff] dictionary - slovnik vysvetlujuci vyrazy / slovicka');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (106, '2024-01-18', '11.7.2005 [jeeff] doc_subscribe - pridanie user_id');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (107, '2024-01-18', '26.8.2005 [miros] groups_approve - premenovanie modu (vadilo to Oracle)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (108, '2024-01-18', '14.9.2005 [jeeff] document_forum - send_answer_notif (chyba v ORACLE verzii)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (109, '2024-01-18', '28.9.2005 [jeeff] templates - 4 volne pouzitelne objekty');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (110, '2024-01-18', '17.10.2005 [miros] dmail - tabulka kampani');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (111, '2024-01-18', '18.10.2005 [jeeff] groups - zvacsenie datovych poloziek');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (112, '2024-01-18', '20.10.2005 [jeeff] media - tabulka s roznymi mediami');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (113, '2024-01-18', '28.10.2005 [jeeff] media - sortovanie a thumb');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (114, '2024-01-18', '9.11.2005 [jeeff] shoppnig basket');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (115, '2024-01-18', '11.11.2005 [jeeff] users - datumy pocas ktorych je platne prihlasovanie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (116, '2024-01-18', '8.12.2005 [jeeff] stat - vytvorenie indexov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (117, '2024-01-18', '19.12.2005 [jeeff] doc_atr - moznostu zadavania necelociselnych hodnot (aj ked atribut sa stale vola value_int)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (118, '2024-01-18', '3.1.2005 [jeeff] groups - nove atributy - domena, ');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (119, '2024-01-18', '19.2.2005 [jeeff] stat_views - doplnene idecka adresarov stranok');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (120, '2024-01-18', '25.1.2006 [jeeff] users - hash pre autorizaciu z emailu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (121, '2024-01-18', '7.2.2006 [jeeff] emails_campain - pocet odoslanych emailov, datum posedneho');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (122, '2024-01-18', '23.3.2006 [jeeff] modules - poradie usporiadania poloziek');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (123, '2024-01-18', '27.2.2006 [jeeff] admin_message - odkazy medzi administratormi');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (124, '2024-01-18', '5.3.2006 [jeeff] admin_message - stav ci je precitana sprava');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (125, '2024-01-18', '11.5.2005 [jeeff] stat - vytvorenie indexov MSSQL');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (126, '2024-01-18', '1.6.2006 [nepso] stat_from - prida stlpec doc_id=na ktoru stranku prisiel');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (127, '2024-01-18', '1.6.2006 [nepso] banner_banners - prida stlpec name = meno banneru');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (128, '2024-01-18', '1.6.2006 [nepso] banner_banners - prida stlpec target');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (129, '2024-01-18', '5.9.2006 [jeeff] sita - zoznam uz parsovanych sprav');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (130, '2024-01-18', '22.9.2006 [nepso] gallery - prida stlpec resize_mode');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (131, '2024-01-18', '3.1.2005 [jeeff] groups - nove atributy - menuType pre prihlaseneho, virtual link');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (132, '2024-01-18', '9.11.2006 [nepso] basket_invoice - prida stlpec html_code');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (133, '2024-01-18', '21.11.2006 [jeeff] url_redirects - presmerovania zmenenych stranok');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (134, '2024-01-18', '24.11.2006 [jeeff] forum - id administratorskych skupin');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (135, '2024-01-18', '2.2.2007 [jeeff] users - rozsirenie stlpca login na 128 znakov, aby mohol obsahovat aj email');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (136, '2024-01-18', '5.2.2007 [jeeff] emails - statistika kliknuti na linku v emaily');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (137, '2024-01-18', '11.2.2007 [jeeff] emails_stat_click - rozsirenie moznosti sledovania');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (138, '2024-01-18', '12.2.2007 [jeeff] forms - datum posledneho exportu zaznamu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (139, '2024-01-18', '14.2.2007 [jeeff] emails - fix na user_id = -1');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (140, '2024-01-18', '6.3.2007 [jeeff] _properties_ - zvacsenie moznosti zadania hodnoty na text');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (141, '2024-01-18', '11.3.2007 [nepso] user_group_verify - prida stlpec hostname');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (142, '2024-01-18', '20.3.2007 [jeeff] groups - oprava -1 hodnoty na logged_menu_type');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (143, '2024-01-18', '2.4.2007 [jeeff] calendar - pridanie tabulky pre evidenciu akceptacii');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (144, '2024-01-18', '13.7.2007 [nepso] emails_campain - pridanie stlpca user_groups');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (145, '2024-01-18', '3.8.2007 [jeeff] emails - index podla kampane, znacne urychli nacitanie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (146, '2024-01-18', '1.8.2007 [nepso] users - pridanie stlpca fax');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (147, '2024-01-18', '5.8.2007 [jeeff] doc_atr_def - zvacsenie pola s nazvom atributu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (148, '2024-01-18', '16.11.2007 [jeeff] emails_campain - doplnenie poli podla tabulky emails');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (149, '2024-01-18', '23.11.2007 [jeeff] basket_item - doplnenie poli title a pn pre zobrazenie v objednavke aj po zmazani stranky');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (150, '2024-01-18', '5.11.2007 [jeeff] banner_banners - sposob vkladania click tagu a frame rate');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (151, '2024-01-18', '25.2.2008 [thaber] banner_stat_views_day - nova tabulka pre statistiku videni bannerov (zabera menej miesta)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (152, '2024-01-18', '4.3.2008 [jeeff] _conf_ - zmena na text pole');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (153, '2024-01-18', '13.4.2008 [pbezak] emails_unsubscribed - zoznam odhlasenych emailov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (154, '2024-01-18', '12.6.2008 [murbanec] BASKET_INVOICE - pridanie pola currency');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (155, '2024-01-18', '16.7.2008 [hric] banner_banners - prida stlpec client_id');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (156, '2024-01-18', '22.8.2008 [murbanec] podpora jazykov pre galeriu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (157, '2024-01-18', '26.7.2008 [murbanec] image gallery - pridanie author stlpca');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (158, '2024-01-18', '3.10.2008 [jeeff] documents - disable_after_end');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (159, '2024-01-18', '23.10.2008 [jeeff] redirects - podpora domen');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (160, '2024-01-18', '7.11.2008 [thaber] proxy - konfiguracia proxy pre externe systemy');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (161, '2024-01-18', '13.11.2008 [jeeff] stat_searchengine - pridanie stlpca group_id pre moznost filtrovania');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (162, '2024-01-18', '17.11.2008 [jeeff] cluster - tabulka so zoznamom refreshov objektov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (163, '2024-01-18', '22.11.2008 [jeeff] stat_from - pridanie stlpca group_id pre moznost filtrovania');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (164, '2024-01-18', '26.11.2008 [jeeff] user_settings - nastavenia pouzivatela (ak treba pre niektory modul)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (165, '2024-01-18', '5.12.2008 [bhric] document_forum - deleted');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (166, '2024-01-18', '23.01.2009 [kmarton] reservation_object - tabulka so zoznamom rezervacnych objektov pre modul rezervacie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (167, '2024-01-18', '23.01.2009 [kmarton] reservation - tabulka so zoznamom rezervacii pre modul rezervacie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (168, '2024-01-18', '29.01.2009 [kmarton] adminlog_notify - tabulka so zoznamom notifikacii pre modul adminlog');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (169, '2024-01-18', '3.2.2008 [kmarton] tabulka inquiry - pridany stlpec multiple - ak sa da hlasovat za viac moznosti');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (170, '2024-01-18', '3.2.2008 [kmarton] tabulka inquiry - pridany stlpec total_clicks - pocet hlasujucich v ankete');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (171, '2024-01-18', '4.2.2008 [thaber] tabulka gallery pridany stlpec pre pocitanie odoslani pohladnice');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (172, '2024-01-18', '6.2.2008 [jeeff] documents_history - pridanie datumu schvalenia web stranky');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (173, '2024-01-18', '7.2.2008 [bhric] tabulka calendar - pridany stlpec creator_id');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (174, '2024-01-18', '7.2.2008 [bhric] tabulka calendar - pridany stlpec approve kvoli schvalovaciemu procesu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (175, '2024-01-18', '7.2.2008 [bhric] tabulka calendar_types - pridany stlpec schvalovatel_id kvoli schvalovaciemu procesu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (176, '2024-01-18', '7.2.2008 [bhric] tabulka calendar - pridany stlpec suggest kvoli odporucaniu udalosti');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (177, '2024-01-18', '5.3.2008 [jeeff] document_forum - pridany index podla doc_id');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (178, '2024-01-18', '20.3.2009 [jeeff] tabulka media - pridany datum poslednej zmeny');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (179, '2024-01-18', '14.03.2009 [kmarton] seo_bots - tabulka so zoznamom vyhladavacich botov, ktori pristupili na stranky WebJET-u');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (180, '2024-01-18', '07.04.2009 [kmarton] seo_keywords - tabulka so zoznamom SEO klucovych slov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (181, '2024-01-18', '07.04.2009 [kmarton] seo_google_position - tabulka so zaznamenanymi poziciami vo vyhladavani na google.com pre klucove slova');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (182, '2024-01-18', '21.4.2009 [kmarton] tabulka seo_keywords - pridany stlpec domain kvoli rozlisovaniu jednotlivych klucovych slov pre rozne domeny');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (183, '2024-01-18', '23.4.2009 [kmarton] tabulka crontab - pridany zaznam, ktory bude na pozadi vykonavat kotrolu pozicii vo vyhladavani vsetkych klucovych slov pre dane domeny na google.com');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (184, '2024-01-18', '6.4.2008 [jeeff] tabulka gallery pridany stlpec pre povolenie domeny pohladnice');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (185, '2024-01-18', '17.4.2009 [jeeff] stat - vytvorenie indexov ORACLE 3');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (186, '2024-01-18', '6.5.2009 [jeeff] vytvorenie indexov MS SQL');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (187, '2024-01-18', '27.04.2009 [murbanec] permission groups - skupiny prav vo WebJETe');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (188, '2024-01-18', '25.5.2009 [kmarton] tabulka seo_keywords - pridany stlpec searchBot kvoli moznosti vyberu vyhladavaca, v ktorom sa bude klucove slovo vyhladavat');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (189, '2024-01-18', '31.7.2009 [jeeff] users - unique index nad loginom');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (190, '2024-01-18', '3.8.2008 [jeeff] user_disabled_items - zvacsenie pola s klucom modulu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (191, '2024-01-18', '24.7.2009 [murbanec] _properties_ - zvacsenie moznosti zadania hodnoty na text / dorabka aj pre ostatne databazovy ako MySQL');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (192, '2024-01-18', '27.7.2009 [murbanec] tabulka form_file_restrictions - obmedzenie uploadnutych suborov na velkost, priponu a pod.');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (193, '2024-01-18', '10.06.2009 [kmarton] monitoring - tabulka, do ktorej sa ukladaju informacie z monitorovania servera');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (194, '2024-01-18', '11.6.2009 [kmarton] tabulka crontab - pridany zaznam, ktory bude na pozadi vykonavat zapis aktualnych hodnot servera do tabulky monitoring kazdych 30 sekund');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (195, '2024-01-18', '13.8.2009 [murbanec] crontab - pridanie stlpca cluster_node pre spustanie na definovanom node clustra');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (196, '2024-01-18', '18.8.2009 [jeeff] forms - zvacsenie pola pre HTML');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (197, '2024-01-18', '31.8.2009 [kmarton] tabulka gallery - pridany stlpec perex_group kvoli moznosti pridavaniu klucoveho slova k obrazku');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (198, '2024-01-18', '2.9.2009 [murbanec] publikovanie adresarov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (199, '2024-01-18', '15.10.2009 [jraska] tabulka documents - pridany stlpec forum_count pre pocitanie poctu prispevkov v diskusii');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (200, '2024-01-18', '21.10.2009 [jeeff] tabulka documents_history - pridany stlpec forum_count pre kompatibility s tabulkou documents (stlpec ale bude prazdny)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (201, '2024-01-18', '27.10.2009 [jeeff] oracle - zmena char stlpcov na varchar pre properties a calendar');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (202, '2024-01-18', '2.11.2009 [jeeff] oracle - doplnenie schemy pre Enterprise verziu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (203, '2024-01-18', '5.11.2009 [kmarton] tabulka gallery_dimension - pridany stlpec pre nazov, perex a datum vytvorenia fotogalerie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (204, '2024-01-18', '10.11.2009 [thaber] tabulka gallery - bod zaujmu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (205, '2024-01-18', '13.11.2009 [kmarton] tabulka inquiry - pridany stlpec image_path - moznost pridania fotky k anketovej odpovedi');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (206, '2024-01-18', '19.11.2009 [kmarton] tabulka gallery_dimension - pridany stlpec pre autora fotogalerie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (207, '2024-01-18', '19.11.2009 [bhric] tabulka gallery_dimension - pridany stlpec views pre pocet videni');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (208, '2024-01-18', '8.12.2009 [jeeff] doplnene indexy');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (209, '2024-01-18', '25.1.2010 [kmarton] tabulka seo_keywords - nastavenie search_bot atributu na default ''google.sk'', tam kde je to NULL kvoli NPE');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (210, '2024-01-18', '1.2.2010 [jeeff] documents_history - index nad doc_id');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (211, '2024-01-18', '5.2.2010 [jraska] tabulka users - pridane stlpce `position` a `parent_id`');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (212, '2024-01-18', '1.3.2010 [jeeff] documents_history - index nad awaiting_approve');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (213, '2024-01-18', '8.4.2010 [jeeff] emails - Oracle nepozna prazdny retazec v DB a ma problem s URL polom ktore realne moze byt prazdne (ked sa posiela priamo text)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (214, '2024-01-18', '8.4.2010 [jeeff] documents - rozsirenie custom fields, pridanie SSL atributu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (215, '2024-01-18', '12.4.2010 [jeeff] sablony - moznost predefinovania install name per sablona');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (216, '2024-01-18', '12.4.2010 [jeeff] sablony - moznost zrusenia spam ochrany per sablona (napr. pre mobil)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (217, '2024-01-18', '23.4.2010 [bhric] basket - tabulka pre evidenciu ciastkovych platieb');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (218, '2024-01-18', '13.5.2010 [jeeff] stat_keys - cache tabulka pre prevod ID na VALUE pre zapis informacie o prehliadaci');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (219, '2024-01-18', '17.5.2010 [jeeff] media - index nad doc_id');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (220, '2024-01-18', '7.6.2010 [kmarton] BASKET_ITEM - zmena cudzieho kluca z -1 na NULL');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (221, '2024-01-18', '5.3.2010 [thaber] sms_templates - pridane tabulka pre sabolny pre sms ');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (222, '2024-01-18', '25.6.2010 [bhric] tabulka perex_groups - pridany stlpec available_groups pre moznost definovania adresarov perex skupinam');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (223, '2024-01-18', '28.6.2010 [jeeff] adminlog - index nad stlpcami pre vyhladavanie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (224, '2024-01-18', '25.6.2010 [murbanec] crontab - HeatMapCleaner');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (225, '2024-01-18', '2.7.2010 [kmarton] user_settings_admin - pridana tabulka pre ukladanie nastaveni pouzivatela v admin casti ');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (226, '2024-01-18', '7.7.2010 [murbanec] crontab - StatWriteBuffer');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (227, '2024-01-18', '9.7.2010 [bhric] basket_invoice_payments - tabulka pre evidenciu ciastkovych platieb - pridany stlpec closed_date, confirmed a upraveny typ stlpca payed_price na decimal');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (228, '2024-01-18', '27.7.2010 [murbanec] Tabulka cluster_monitoring - pouziva sa pri prenose monitorovacich informacii medzi clustermi');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (229, '2024-01-18', '30.7.2010 [murbanec] Tabulka pkey_generator - zmena stlpca z INT na LONG');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (230, '2024-01-18', '3.8.2010 [murbanec] cluster_monitoring - pridany stlpec created_at');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (231, '2024-01-18', '4.8.2010 [murbanec] Watermarky pre galeriu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (232, '2024-01-18', '6.8.2010 [kmarton] gallery - pridany stlpec upload_datetime - datum a cas nahratia fotografie na server');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (233, '2024-01-18', '10.8.2010 [murbanec] Spotlight - FileIndexer');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (234, '2024-01-18', '27.8.2010 [kmarton] Zmena kratkeho pola pre ukladanie ciest k priloham v tabulke emails a emails a emails_campaign');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (235, '2024-01-18', '31.8.2010 [jraska] Tabulka multigroup_mapping - pouziva sa pre mapovanie Master - Slave clankov pre ucely multikategorii/multiadresarov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (236, '2024-01-18', '20.9.2010 [jeeff] documents - indexy pre rychlejsie nacitanie DocDB struktur');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (237, '2024-01-18', '4.1.2011 [murbanec] users - pridanie moznosti jednosmerneho hesla');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (238, '2024-01-18', '2.2.2011 [mhalas] domain_redirects - tabulka presmerovania domen');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (239, '2024-01-18', '21.2.2011 [mhalas] pridanie flagu do crontable na auditovacie ucely');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (240, '2024-01-18', '28.2.2011 [mhalas] pridanie sort_priority do gallery');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (241, '2024-01-18', '23.03.2011 [mrepasky] monitoring - pridanie cpu_usage a process_usage do monitoringu servera ');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (242, '2024-01-18', '27.03.2011 [murbanec] Vymazanie triedy RegAlarm - uz sa nepouziva');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (243, '2024-01-18', '5.4.2011 [jeeff] Oracle - zvacsenie datovy poli pre nazvy adresara');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (244, '2024-01-18', '25.05.2011 [bhric] inquiry - tabulka pre zapis statistiky o hlasovani daneho usera');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (245, '2024-01-18', '27.05.2010 [thaber] pridanie tabulky stopslov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (246, '2024-01-18', '19.05.2011 [murbaenc] Atributy pre formulare + merge restrikcii suborov do spolocnej tabulky');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (247, '2024-01-18', '23.06.2011 [mrepasky] Tabulka regularnych vyrazov pre formulare');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (248, '2024-01-18', '24.6.2011 [mhalas] Pridanie priority do questions_answers');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (249, '2024-01-18', '11.7.2011 [bhric] zmluvy, zmluvy_prilohy - tabulky pre modul Zmluvy');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (250, '2024-01-18', '22.7.2011 [bhric] tabulky zmluvy - rozdelenia stlpca platnost na platnost_od a platnost_do');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (251, '2024-01-18', '25.7.2011 [mrepasky] Pretypovanie stlpca prop_value v tabulke properties');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (252, '2024-01-18', '26.8.2011 [mhalas] premenovanie stlpca audit kvoli oraclu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (253, '2024-01-18', '19.9.2011 [mrepasky] Oracle - pretypovanie stlpca pre publikovanie adresara');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (254, '2024-01-18', '22.9.2011 [mrepasky] Pretypovanie stlpca pre defaultnu hodnotu atributu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (255, '2024-01-18', '4.10.2011 [mhalas] Sledovanie otvorenia emailu pre dmail');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (256, '2024-01-18', '10.11.2011 [jeeff] forum - id skupin z ktorych je mozne pridat novy prispevok');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (257, '2024-01-18', '11.11.2011 [vbur] contact - tabulka s kontaktmi');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (258, '2024-01-18', '21.11.2011 [vbur] inquiry - pridanie url k odpovedi');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (259, '2024-01-18', '16.11.2011 [jeeff] forms - indexy pre rychlejsie nacitanie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (260, '2024-01-18', '13.01.2012 [mrepasky] Pridanie regularnych vyrazov do databazy - tabulka form_regular_exp');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (261, '2024-01-18', '20.1.2012 [jeeff] cache - tabulka pre persistent cache');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (262, '2024-01-18', '7.3.2012 [jeeff] proxy - pridanie autorizacie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (263, '2024-01-18', '18.4.2012 [vbur] inventory - evidencia majetku');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (264, '2024-01-18', '10.5.2012 [vbur] inventory - prislusenstvo');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (265, '2024-01-18', '15.5.2012 [bhric] zmluvy - odstranenie a pridanie dalsich stlpcov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (266, '2024-01-18', '18.5.2012 [bhric] tabulka adminlog_notify - pridany stlpec text');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (267, '2024-01-18', '25.5.2012 [bhric] tabulka zmluvy - pridany stlpec zobrazit, vytvoril');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (268, '2024-01-18', '26.6.2010 [bhric] documents - rozsirenie tabulky o root_group_l1, root_group_l2 a root_group_l3');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (269, '2024-01-18', '17.7.2012 [bhric] Oracle - pretypovanie stlpca html_code v banner_banners');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (270, '2024-01-18', '3.8.2012 [bhric] perex_group_doc - pouziva sa pre mapovanie doc_id a perex_group_id z tabulky documents');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (271, '2024-01-18', '6.8.2012 [bhric] documents - indexy na root_group stlpcami');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (272, '2024-01-18', '22.8.2012 [jeeff] perex_group_doc - indexy');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (273, '2024-01-18', '12.9.2012 [mrepasky] Pridanie regularnych vyrazov do databazy - integer');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (274, '2024-01-18', '10.10.2012 [jeeff] prop - moznost zadat NULL hodnotu pre Oracle (akoze prazdny string)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (275, '2024-01-18', '3.1.2013 [jeeff] questions_answers - rozsirenie tabulky o nove stlpce');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (276, '2024-01-18', '9.1.2013 [bhric] reservation_object - rozsirenie tabulky o nove stlpce');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (277, '2024-01-18', '11.3.2012 [mhalas] WJ Cloud - pridanie domain_id do galerie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (278, '2024-01-18', '11.3.2012 [mhalas] WJ Cloud - pridanie domain_id do gallery_dimension');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (279, '2024-01-18', '14.3.2013 [prau] WebJet Cloud inquiry_answers - rozsirenie tabulky o novy stlpec domain_id ');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (280, '2024-01-18', '14.3.2013 [prau] WebJet Cloud inquiry_users - rozsirenie tabulky o novy stlpec domain_id ');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (281, '2024-01-18', '14.3.2013 [prau] WebJet Cloud inquiry - rozsirenie tabulky o novy stlpec domain_id ');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (282, '2024-01-18', '15.3.2012 [mbocko] WJ Cloud (modul Forum) - pridanie domain_id do forum');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (283, '2024-01-18', '15.3.2012 [mbocko] WJ Cloud (modul Forum) - pridanie domain_id do document_forum');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (284, '2024-01-18', '18.3.2012 [mbocko] WJ Cloud (modul QA) - pridanie domain_id do questions_answers');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (285, '2024-01-18', '21.3.2012 [prau] WJ Cloud (modul formulare) - pridanie domain_id do forms ');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (286, '2024-01-18', '21.3.2012 [prau] WJ Cloud (modul formulare) - pridanie domain_id do form_attributes');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (287, '2024-01-18', '15.4.2013 [mrepasky] Pridany datum zmeny pre konfiguracne premenne - pridanie stlpca date_changed do tabulky _conf_ resp. webjet_conf');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (288, '2024-01-18', '03.06.2013 [mrepasky] Tabulka pre historiu suborov vo webjete - file_history a nastavenie generatora klucov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (289, '2024-01-18', '4.6.2012 [mbocko] WJ Cloud (users) - pridanie domain_id do users');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (290, '2024-01-18', '22.5.2017 [mminda] inventory - evidencia majetku - pridanie dovodu vyradenia ');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (291, '2024-01-18', '11.6.2012 [mbocko] inventory - evidencia majetku - rozsirenie detailu, pridanie dovodu vyradenia pre detail');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (292, '2024-01-18', '19.6.2012 [mbocko] WJ Cloud (Basket) - pridanie domain_id do basket_item');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (293, '2024-01-18', '19.6.2012 [mbocko] WJ Cloud (Basket) - pridanie domain_id do basket_invoice');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (294, '2024-01-18', '25.7.2013 [mbocko] tabulka so zoznamom slov pre slovnikovu captchu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (295, '2024-01-18', '25.7.2013 [mhalas] tabulka pre limity na domain throttling');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (296, '2024-01-18', '29.11.2013 [mhalas] Genericky template pre domenove limity');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (297, '2024-01-18', '13.12.2013 [mrepasky] Uprava tabulky pre historiu suborov vo webjete - file_history');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (298, '2024-01-18', '17.12.2013 [jeeff] Oprava mena stlpcu pkey generatora');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (299, '2024-01-18', '10.1.2013 [bhric] tabulka zmluvy - zmena typu pola pre predmet, poznamka na text / clob');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (300, '2024-01-18', '12.2.2013 [mcacko] #15400 - skutocny archiv, vytvorenie tabulky forms_archiv');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (301, '2024-01-18', '01.07.2014 [mkolejak] Tabulka pre todo');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (302, '2024-01-18', '12.9.2014 [mkolejak] doplnam chybajuci primarny kluc pre mssql');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (303, '2024-01-18', '27.10.2014 [mkolejak] reservation_object - rozsirenie tabulky o nove stlpce');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (304, '2024-01-18', '27.10.2014 [mkolejak] reservation - pridanie stlpca phone_number');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (305, '2024-01-18', '4.11.2014 [mkolejak] WJ Cloud - pridanie domain_id do reservation');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (306, '2024-01-18', '4.11.2014 [mkolejak] WJ Cloud - pridanie domain_id do reservation_object');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (524, '2024-02-14', 'NEW MODULE: cmp_enumerations');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (307, '2024-01-18', '05.11.2014 [mkolejak] Tabulka pre export_dat');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (308, '2024-01-18', '27.10.2014 [mkolejak] _conf_prepared_ - pripravene hodnoty pre config, pridaju sa v prepared case');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (309, '2024-01-18', '12.12.2014 [mkolejak] Automaticky prenesenie predpripravenych konfiguracii z
     _config_prepared_ do _conf_ ked uplynul date_prepared pomocou cronu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (310, '2024-01-18', '19.06.2015 [pbielik] tabulka terminologicky_slovnik');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (311, '2024-01-18', '16.07.2015 [rzapach] pridanie stlpcov pre kalendar');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (312, '2024-01-18', '22.07.2015 [rzapach] Tabulka pre modul GIS');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (313, '2024-01-18', '28.07.2015 [rzapach] Tabulky pre modul denneho menu v restauracii');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (314, '2024-01-18', '12.08.2015 [rzapach] Tabulka pre hodnotenie stranky');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (315, '2024-01-18', '14.9.2015 [rzapach] zmluvy - pridanie stlpca sposob_uhrady');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (316, '2024-01-18', '17.12.2015 [rzapach] tabulka cien pre dany rezervacny objekt');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (317, '2024-01-18', '28.4.2015 [mhalas] WebJet Cloud Zmluvy - rozsirenie tabulky o novy stlpec domain_id ');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (318, '2024-01-18', '23.10.2015 [jeeff] Form regexp - uprava regexpu kontroly emailu pre velke pismena');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (319, '2024-01-18', '18.12.2015 [pbielik] pridanie stlpca synonymum do terminologicky_slovnik');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (320, '2024-01-18', '20.1.2016 [rzapach] tabulka povolenych casov pre dany rezervacny objekt, casova jednotka pre rez. objekt');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (321, '2024-01-18', '30.05.2016 [jeeff] pridanie stlpcov pre calendar_invitation');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (322, '2024-01-18', '14.10.2015 [jeeff] ckeditor - pocitadlo uploadnutych obrazkov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (323, '2024-01-18', '10.12.2015 [jeeff] zmazanie cron tasku magzilly');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (324, '2024-01-18', '12.2.2016 [rzapach] groups_scheduler -> groups history, pridanie stlpca s casom a zmena when_to_publish aby povoloval null hodnoty');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (325, '2024-01-18', '21.6.2016 [jeeff] pkey_generator - nastavenie stlpcov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (326, '2024-01-18', '11.7.2016 [rzapach] pridanie stlpca mobile_device (idcko zariadenia) do tabulky users -> kvoli posielaniu push notifikacii');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (327, '2024-01-18', '30.08.2016 [prau] pridanie stlpcu pre group scheduler kvoli historii');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (328, '2024-01-18', '23.09.2016 [rzapach] pridanie stlpcu pre domain_redirects kvoli pridaniu kontroly protokolu http/https');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (329, '2024-01-18', '15.09.2016 [prau] #20546 - WebJET - Vkladanie scriptov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (330, '2024-01-18', '18.11.2016 [lpasek] Vytvorenie tabulky pre komponentu appcache');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (331, '2024-01-18', '06.02.2017 [bhric] pridanie stlpcu pre jazyk (lng) do groups');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (332, '2024-01-18', '15.02.2017 [mbocko] pridanie stlpca require_email_verification pre user_groups');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (333, '2024-01-18', '26.04.2017 [ryapach] vytvorenie tabulky pre poznmaky pre editorov k strankam');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (334, '2024-01-18', '10.5.2017 [mminda] oracle - doplnenie schemy pre Tooltipy (tatrabanka)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (335, '2024-01-18', '15.05.2017 [bhric] pridanie stlpcu create_date (datum odhlasenia) do emails_unsubscribed');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (336, '2024-01-18', '1.5.2017 [prau] Archiv suborov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (337, '2024-01-18', '29.05.2017 [jeeff] premenovanie stlpcu pre domain_redirects kvoli oracle vyhradenemu menu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (338, '2024-01-18', '30.5.2017 [lpasek] Rozsirenie tabulky appcache_file o stlpec recursive');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (339, '2024-01-18', '30.5.2017 [lpasek] Vytvorenie tabulky appcache_page');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (340, '2024-01-18', '31.5.2017 [lpasek] Uprava tabulky appcache_page');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (341, '2024-01-18', '03.07.2017 [prau] #22201 rozsirenie Baneroveho systemu o domain_id ');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (342, '2024-01-18', '17.07.2017 [prau] #22201 rozsirenie Baneroveho systemu o domain_id - fix mysql');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (343, '2024-01-18', '2.8.2017 [prau] Archiv suborov pridanie stlpcu pre velkost suboru');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (344, '2024-01-18', '2.8.2017 [prau] Archiv suborov oracle skript');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (345, '2024-01-18', '08.09.2017 [pbielik] vytvorenie tabuliek pre kviz');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (346, '2024-01-18', '27.09.2017 [prau] #20546 - WebJET - Vkladanie scriptov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (347, '2024-01-18', '28.09.2017 [prau] pridanie stlpca right_answer pre quiz_answers');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (348, '2024-01-18', '07.11.2017 [prau] #20546 - WebJET - Vkladanie scriptov - oprava');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (349, '2024-01-18', '07.11.2017 [prau] #23245 - Rozsirenie funkcionality skupiny práv');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (350, '2024-01-18', '10.11.2017 [bhric] pridanie flagu do crontable na spustanie ulohy po starte servera a moznost zakazania vykonavania ulohy');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (351, '2024-01-18', '10.11.2017 [pbielik] #22743 - Pridanie domainId pre quiz');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (352, '2024-01-18', '14.11.2017 [bhric] crontab - fix default hodnoty pre run_at_startup, enable_task v MS SQL');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (353, '2024-01-18', '14.11.2017 [bhric] pridanie stlpcu pre jazyk (lng) do groups_scheduler');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (354, '2024-01-18', '15.11.2017 [prau] #22201 - Upravy modulov pre multiweb / pridanie stlpca domain_id do file archivu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (355, '2024-01-18', '15.11.2017 [prau] #23167 - Archiv suborov - kontrola prav podla prav na subory pouzivatela - oprava');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (356, '2024-01-18', '21.09.2017 [lpasek] Uprava tabulky pre komponentu appcache');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (357, '2024-01-18', '21.09.2017 [lpasek] Uprava tabulky pre komponentu appcache file');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (358, '2024-01-18', '10.01.2018 [prau] #23471 - Password security');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (359, '2024-01-18', '29.1.2018 [jeeff] sablony - zvacsenie pola pre zadavanie CSS liniek');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (360, '2024-01-18', '02.02.2018 [jeeff] groups - pridanie moznosti oznacenia adresara pre nezobrazenie v admin casti');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (361, '2024-01-18', '07.03.2018 [bhric] Zmluvy, rozsirenie o organizacie. Moznost pridania organizacii a prav nad zmluvami (#23935)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (362, '2024-01-18', '16.04.2018 [mhruby] #25103 M - SPRÁVA ČÍSELNÍKOV');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (363, '2024-01-18', '11.4.2018 [mhruby] #25103 M - SPRÁVA ČÍSELNÍKOV - pridanie stlpcovych nazvov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (364, '2024-01-18', '11.04.2018 [jeeff] restaurant_menu - oprava datoveho stlpca pre MS SQL');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (365, '2024-01-18', '18.04.2018 [prau] #25043 M - INTERNÉ RIADIACE AKTY');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (366, '2024-01-18', '7.5.2018 [jeeff] presmerovania domen - zvacsenie pola pre zadavanie URL cielovej domeny');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (367, '2024-01-18', '18.5.2018 [mhruby] #25103 M - SPRÁVA ČÍSELNÍKOV - fix diakritiky pre Microsoft SQL');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (368, '2024-01-18', '8.5.2018 [prau] Modul GDPR zadanie regexpov #23673');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (369, '2024-01-18', '15.5.2018 [prau] #23673 Modul GDPR zadanie regexpov - rozsirenie tabulky o stlpec domain_id ');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (370, '2024-01-18', '30.5.2018 [prau] #23673  regularne vyrazy gdpr - zvacsenie pola pre zadavanie regularneho vyrazu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (371, '2024-01-18', '26.5.2018 [prau] #23673  regularne vyrazy gdpr -inicializacia regularnych vyrazov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (372, '2024-01-18', '05.06.2018 [pbielik] pridane customizacie kvizu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (373, '2024-01-18', '04.04.2018 [lzlatohlavek] #23073 - UPRAVA MEDIA KOMPONENTY');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (374, '2024-01-18', '12.06.2018 [mhruby] Tabulky pre modul denneho menu v restauracii (zmena hmotnosti na string)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (375, '2024-01-18', '12.06.2018 [mhruby] Tabulka pre skupiny sablon.');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (376, '2024-01-18', '18.06.2018 [pbielik] obodovany kviz');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (377, '2024-01-18', '2.7.2018 [pbielik] inquirysimple');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (378, '2024-01-18', '11.7.2018 [mhruby] #25103 M - SPRÁVA ČÍSELNÍKOV - fix diakritiky pre Microsoft SQL a Oracle');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (379, '2024-01-18', '11.7.2018 [mhruby] Tabulka pre skupiny sablon. - fix diakritiky pre Microsoft SQL');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (380, '2024-01-18', '13.07.2018 [pbielik] user_settings - nastavenia pouzivatela (ak treba pre niektory modul)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (381, '2024-01-18', '27.7.2018 [pbielik] added email_files table');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (382, '2024-01-18', '17.05.2018 [prau] Vytvorenie tabuky pre cookies');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (383, '2024-01-18', '17.05.2018 [prau] Pridanie riadkov pre cookies');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (384, '2024-01-18', '13.8.2018 [prau] [#23881 - Cookies] - presun do WJ 8');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (385, '2024-01-18', '17.8.2018 [mhruby] rozsirienie intranetoveho widgetu ulohy');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (386, '2024-01-18', '5.9.2018 [jeeff] Pridanie key prefix stlpca na template groups');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (387, '2024-01-18', '13.9.2018 [jeeff] Zmazanie starej konf. premennej kvoli korektnemu updatu na WJ8');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (388, '2024-01-18', '20.9.2018 [pbielik] uprava quiz_questions question na varchar(500)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (389, '2024-01-18', '20.9.2018 [jeeff] oprava identity stlpca na media_groups tabulke');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (390, '2024-01-18', '23.10.2018 [prau] #27127 Archiv suborov, defaultne zapnutie cronu pre nahratie suboru neskorej');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (391, '2024-01-18', '15.11.2018 [mhruby] rozsirienie intranetoveho widgetu ulohy (pre oracle)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (392, '2024-01-18', '30.11.2018 [pbielik] add cookie_visitor_group to banner_banners');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (393, '2024-01-18', '19.12.2018 [jvyskoc] pridanie pageReactions (lajkov)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (394, '2024-01-18', '19.12.2018 [bhric] Sprava ciselnikov - rozsirenie string data hodnot na 1024 znakov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (395, '2024-01-18', '14.12.2018 [lpasek] Premenovanie stlpca priority v tabule todo na sort_priority (priority je klucove slovo)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (396, '2024-01-18', '6.2.2019 [jeeff] index nad media grupami');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (397, '2024-01-18', '08.02.2019 [prau] #28315 - Archiv suborov - problem s vypisom  [MFSR]');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (398, '2024-01-18', '14.02.2019 [mhruby] Rozsirenie ciselnikov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (399, '2024-01-18', '18.02.2019 [pbielik] pridany image url pre quiz bean');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (400, '2024-01-18', '20.02.2019 [mhruby] Pridanie regularnych vyrazov do databazy - tabulka form_regular_exp');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (401, '2024-01-18', '05.03.2019 [pbielik] pkgenerator zmenena cookies_id za cookie_id');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (402, '2024-01-18', '06.03.2019 [mhruby] Nastavenie defaultnej priority existujucim datam');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (403, '2024-01-18', '10.3.2019 [jeeff] Pridanie rezimu inline editacie do skupiny sablon');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (404, '2024-01-18', '18.3.2019 [mpijak] Pridanie pola DTYPE aby sa dala sfunkcnit dedicnost pre enumerations tabulky');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (405, '2024-01-18', '20.3.2019 [mhruby] Bugfix definicie tabulky pri praci s boolean hodnotami');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (406, '2024-01-18', '27.3.2019 [mhruby] Rozsirenie ciselnikov od dalsie stringy pre autokupu/autoweb');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (407, '2024-01-18', '28.3.2019 [mhruby] Rozsirenie ciselnikov o dalsie datumy pre autokupu/autoweb');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (408, '2024-01-18', '7.4.2019 [jeeff] Zmena sequencii na identity na Beanoch, Oracle stale potrebuje sequencie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (409, '2024-01-18', '9.4.2019 [mhruby] Rozsirenie ciselnikov o rodicov pre organizacnu strukturu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (410, '2024-01-18', '13.5.2019 [mhruby] Rozsirenie ciselnikov od dalsie stringy pre autokupu/autoweb');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (411, '2024-01-18', '27.05.2019 [pbielik] Premenovanie stlpca priority a level v tabulke file_archiv_category_node');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (412, '2024-01-18', '27.05.2019 [pbielik] Premenovanie stlpca level v tabulke file_archiv_category_node a inde');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (413, '2024-01-18', '14.06.2019 [lpasek] Vytvorenie stlpcov double_optin_confirmation_date a double_optin_hash nad tabulkami forms a forms_archive');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (414, '2024-01-18', '20.6.2019 [jeeff] fixnutie pkey generatora domain_redirects');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (415, '2024-01-18', '17.9.2019 [mpijak] Rozsirenie ciselnikov od dalsie stringy pre autokupu/autoweb a');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (416, '2024-01-18', '22.09.2019 [jeeff] Formulare - rozsirenie string value hodnot na 1024 znakov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (417, '2024-01-18', '27.09.2019 [jeeff] pkey generator pre crypto modul (poradie kluca)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (418, '2024-01-18', '23.10.2019 [mhruby] Rozsirenie tabulky url_redirect o datum publikacie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (419, '2024-01-18', '7.11.2019 [mhruby] crontab - UrlRedirectDB');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (420, '2024-01-18', '22.11.2019 [bhric] basket_invoice - zvacsenie stlpca currency na 8 znakov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (421, '2024-01-18', '5.12.2019 [mhruby] [#39370 - Vynutene nastavenie sablony v adresari] - uloha #0 => doplnenie forceTheUseOfGroupTemplate do adresara webstranok 2');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (422, '2024-01-18', '10.12.2019 [mhruby] [#39370 - Vynutene nastavenie sablony v adresari] - uloha #0 => doplnenie forceTheUseOfGroupTemplate do groups_scheduler');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (423, '2024-01-18', '13.12.2019 [mhruby] revert: crontab - UrlRedirectDB');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (424, '2024-01-18', '13.12.2019 [bhric] [#39430 - Notifikacia pri dosiahnuti maximalneho poctu DB spojeni] - default notifikacia pre web.spam');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (425, '2024-01-18', '8.1.2020 [mhruby] [#40177 - 404, WebJET neobsahuje žiadne súbory] - uloha #18 => bugfix pre oracle obmedzenie dlkzy stlpca');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (426, '2024-01-18', '31.01.2020 [mhruby] Rozsirenie todo tabulky a group_id');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (427, '2024-01-18', '28.02.2020 [jeeff] Oprava stlpca timestamp na date pre mssql');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (428, '2024-01-18', '11.03.2020 [jeeff] Nastavenie primarneho kluca pre vlastnosti formularu v multidomain prostredi');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (429, '2024-01-18', '04.06.2020 [jeeff] Premenovanie premennej groupCreateBlankWebpageAfterCreate na syncGroupAndWebpageTitle kedze jej povodne pouzitie riesilo 2 veci');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (430, '2024-01-18', '05.06.2020 [pgajdos] Zmena chybnych id skupin sablon (0 -> 1)');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (431, '2024-01-18', '17.06.2020 [bhric] forms_archive - doplneny chybajuci stlpec DOMAIN_ID pre Oracle');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (432, '2024-01-18', '2.7.2020 [mhruby] Pridanie stĺpca do tabulky inventory, prekopírovanie hodnôt z pôvodneho stlpca do nového a nasledne vycistinie stlpca ALTER');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (433, '2024-01-18', '2.7.2020 [mhruby] Pridanie stĺpca do tabulky inventory, prekopírovanie hodnôt z pôvodneho stlpca do nového a nasledne vycistinie stlpca UPDATE');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (434, '2024-01-18', '2.7.2020 [mhruby] Pridanie stĺpca do tabulky inventory, prekopírovanie hodnôt z pôvodneho stlpca do nového a nasledne vycistinie stlpca UPDATE 2');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (435, '2024-01-18', '09.07.2020 [pgajdos] Pridanie stlpca task_name do tabulky crontab, umiestnenie stlpca na druhe miesto');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (436, '2024-01-18', '09.07.2020 [pgajdos] Zapisanie popisov cronjobov do stlpca task_name, pre tabulku crontab');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (437, '2024-01-18', '07.10.2020 [pbielik] [#48232 - Spracovanie modulu FIQ] - uprava varchar stlpcov na nvarchar');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (438, '2024-01-18', '14.10.2020 [jeeff] Oprava nazvov stlpcov pkey generatora-domain_redirects');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (439, '2024-01-18', '13.7.2020 [lpasek] adminlog - operation_type');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (440, '2024-01-18', '30.11.2020 [lpasek] documents_history - disapproved_by');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (441, '2024-01-18', '20.01.2021 [lpasek] Pridanie 3 stlpcov do tabulky cookies #51550');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (442, '2024-01-18', '23.3.2021 [jeeff] pkey generator pre mirrorovanie struktury');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (443, '2024-01-18', '23.03.2021 [lmolcan] banner_banners tabulka - pridanie atributov pre obsahovy banner');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (444, '2024-01-18', '13.04.2021 [lmolcan] banner_banners tabulka - pridanie atributov pre obsahovy banner - mysql');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (445, '2024-01-18', '13.3.2021 [sivan] Pridanie stlpca id a user_id do _conf_prepared_ tabulky');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (446, '2024-01-18', '01.04.2021 [lbalat] Pridanie stlpca date_published do _conf_prepared_ tabulky');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (525, '2024-02-14', 'NEW MODULE: cmp_proxy');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (447, '2024-01-18', '16.04.2021 [lbalat] Pridanie stlpca date_published do groups_scheduler tabulky');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (448, '2024-01-18', '28.05.2021 [lbalat] Pridanie hashu pre kliknutie k emailom');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (449, '2024-01-18', '21.12.2021 [lmolcan] Pridanie stlpca pre priezvisko');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (450, '2024-01-18', '3.10.2022 [bhric] banery - rozsirenie o kampanovy banner');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (451, '2024-01-18', '20.04.2020 [lpasek] pkey_generator - gallery_dimension');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (452, '2024-01-18', '13.5.2020 [pgajdos] Pridanie stlpca update_date do _properties_ tabulky');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (453, '2024-01-18', '08.07.2020 [pgajdos] Pridanie stlpca task_name do tabulky crontab, umiestnenie stlpca na druhe miesto');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (454, '2024-01-18', '14.10.2020 [jeeff] Oprava nazvov stlpcov pkey generatora');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (455, '2024-01-18', '15.4.2021 [jeeff] _conf_prepared_ -> zmena date_prepared aby povoloval null hodnoty');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (456, '2024-01-18', '14.12.2021 [sivan] Pridanie stlpcov temp_field_a_docid ... temp_field_d_docid do tabuľky documents');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (457, '2024-01-18', '14.12.2021 [sivan] Pridanie stlpcov temp_field_a_docid ... temp_field_d_docid do tabuľky documents_history');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (458, '2024-01-18', '14.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_menu, logged_show_in_navbar, logged_show_in_sitemap) do tabulky documents');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (459, '2024-01-18', '18.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_menu, logged_show_in_navbar, logged_show_in_sitemap) do tabulky documents_history');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (460, '2024-01-18', '28.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_navbar, logged_show_in_sitemap) do tabulky groups.');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (461, '2024-01-18', '28.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_navbar, logged_show_in_sitemap) do tabulky groups_scheduler.');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (462, '2024-01-18', '28.12.2021 [sivan] Pridanie boolean stlpcov url_inherit_group, generate_url_from_title a editor_virtual_path do tabulky documents');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (463, '2024-01-18', '4.1.2022 [sivan] Pridanie boolean stlpcov url_inherit_group, generate_url_from_title a editor_virtual_path do tabulky documents_history');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (464, '2024-01-18', '21.1.2022 [lbalat] Nastavenie default hodnoty temp_field_d_docid');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (465, '2024-01-18', '21.12.2021 [sivan] Zmena primarneho kluca tabulky form_regular_exp na nový INT stlpec id');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (466, '2024-01-18', '18.2.2022 [jeeff] users - zmena password aby povoloval null hodnoty');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (467, '2024-01-18', '04.10.2022 [sivan] Pridanie zmena typu stĺpcov cas_od a cas_do z varchar na datetime.');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (468, '2024-01-18', '04.10.2022 [sivan] Uprava typov stĺpcov reservation_time_from a reservation_time_to z varchar na Datetime, time_unit z varchar na tinyint.');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (469, '2024-01-18', '15.12.2022 [lbalat] Pridanie stlpcov field_a-f do tabulky banner_banners');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (470, '2024-01-18', '20.12.2022 [lbalat] inquiry - podpora vkladania HTML kodu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (471, '2024-01-18', '22.12.2022 [lbalat] users - pridanie stlpca api_key');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (472, '2024-01-18', '07.02.2023 [lbalat] pkey-generator - oprava nazvov, fix oracle documents');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (473, '2024-01-18', '11.03.2023 [lbalat] vypnutie FileIndexer cron jobu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (474, '2024-01-18', '27.3.2023 [lbalat] Pridanie stlpcov publish_after_start do tabulky documents_history pre historicke uchovanie stavu publikovania');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (475, '2024-01-18', '13.04.2023 [lbalat] reservation_object_times - fix Oracle');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (476, '2024-01-18', '18.04.2023 [pbielik] JTB-1630 - conf_prepared value length');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (477, '2024-01-18', '21.4.2023 [lbalat] Pridanie stlpca id do doc_atr tabulky');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (478, '2024-01-18', '25.4.2023 [lbalat] doc_atr_def - pridanie domain_id');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (479, '2024-01-18', '11.5.2023 [lbalat] Banner - oprava NULL stlpcov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (480, '2024-01-18', '26.05.2023 [sivan] Pridanie novej tabuľky response_headers');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (481, '2024-01-18', '8.6.2023 [sivan] media - pridanie stlpca domain_id');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (482, '2024-01-18', '05.06.2023 [sivan] #55285 - WebJET9 - Banner adresarova struktura zobrazenia');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (483, '2024-01-18', '14.6.2022 [lbalat] users - zmena password aby povoloval null hodnoty aj pre mysql');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (484, '2024-01-18', '7.7.2023 [lbalat] Oracle - nastavenie COLLATE bez ohladu na velkost pismen pre vyhladavanie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (485, '2024-01-18', '20.07.2023 [sivan] cluster_monitoring - zvacsenie pola pre content');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (486, '2024-01-18', '17.08.2023 [lbalat] export_dat - rename xml format to rss');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (487, '2024-01-18', '10.08.2023 [sivan] seo_keywords - pridanie stlpca actual_position');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (488, '2024-01-18', '14.09.2023 [lbalat] proxy - podpora viacerych lokalnych URL adries');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (489, '2024-01-18', '14.09.2023 [sivan] Pridanie stlpcov field_a-f do tabulky media');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (490, '2024-01-18', '15.11.2023 [lbalat] rating - change class name');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (491, '2024-01-18', '23.12.2023 [lbalat] templates - add inline_editing_mode');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (492, '2024-01-18', 'priprava synchronizacie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (493, '2024-01-18', 'nastavenie reg date pouzivatelov');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (494, '2024-01-18', 'konverzia perex_group v tab. documents z name na id');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (495, '2024-01-18', 'nastavenie hodnot group_id pre tabulku stat_views');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (496, '2024-01-18', '14.3.2009 [kmarton] nastavenie spravnych hodnot browserId kvoli kompatibilite noveho kodu, ovplynuje tabulku stat_from a stat_views');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (497, '2024-01-18', '11.01.2010 [jraska] rozdelenie pristupovych prav ovladacieho panela na jednotlive podkategorie');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (498, '2024-01-18', '14.5.2010 [jeeff] pridanie novych stlpcov do stat_views');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (499, '2024-01-18', '10.03.2018 [lbalat] nastavenie defaultneho map providera podla nastavenia googleMapsApiKey');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (500, '2024-01-18', 'NEW MODULE: editorMiniEdit');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (501, '2024-01-18', 'NEW MODULE: editor_show_hidden_folders');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (502, '2024-01-18', 'NEW MODULE: cmp_contact');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (503, '2024-01-18', 'NEW MODULE: null');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (504, '2024-01-18', 'NEW MODULE: components.news.edit_templates');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (505, '2024-01-18', 'NEW MODULE: editor_unlimited_upload');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (506, '2024-01-18', 'NEW MODULE: conf.show_all_variables');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (507, '2024-01-18', 'NEW MODULE: cmp_adminlog_logging');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (508, '2024-01-18', 'NEW MODULE: cmp_in-memory-logging');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (509, '2024-01-18', 'NEW MODULE: editor_edit_perex');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (510, '2024-01-18', '17.07.2017 [prau] #22201 rozsirenie Baneroveho systemu o domain_id - fix');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (511, '2024-02-14', 'NEW MODULE: cmp_stat_seeallgroups');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (512, '2024-02-14', 'NEW MODULE: fbrowser_delete_directory');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (513, '2024-02-14', 'NEW MODULE: cmp_abtesting');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (514, '2024-02-14', 'NEW MODULE: menuGDPR');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (515, '2024-02-14', 'NEW MODULE: menuGDPRregexp');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (516, '2024-02-14', 'NEW MODULE: menuGDPRDelete');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (517, '2024-02-14', 'NEW MODULE: cmp_file_archiv');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (518, '2024-02-14', 'NEW MODULE: menuFileArchivExportFiles');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (519, '2024-02-14', 'NEW MODULE: menuFileArchivImportFiles');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (520, '2024-02-14', 'NEW MODULE: menuFileArchivManagerCategory');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (521, '2024-02-14', 'NEW MODULE: cmp_fileArchiv_edit_del_rollback');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (522, '2024-02-14', 'NEW MODULE: cmp_fileArchiv_advanced_settings');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (523, '2024-02-14', 'NEW MODULE: cmp_insert_script');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (526, '2024-02-14', 'NEW MODULE: cmp_reservation');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (527, '2024-02-14', 'NEW MODULE: cmp_restaurant_menu');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (528, '2024-02-14', 'NEW MODULE: cmp_seo');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (529, '2024-02-14', 'NEW MODULE: cmp_tooltip');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (530, '2024-02-14', 'NEW MODULE: cmp_blog');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (531, '2024-02-14', 'NEW MODULE: cmp_blog_admin');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (532, '2024-02-14', 'NEW MODULE: cmp_export');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (533, '2024-02-14', 'NEW MODULE: cmp_media');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (534, '2024-02-14', 'NEW MODULE: editor_edit_media_all');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (535, '2024-02-14', 'NEW MODULE: editor_edit_media_group');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (536, '2024-02-14', 'NEW MODULE: prop.show_all_texts');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (537, '2024-02-14', 'NEW MODULE: cmp_response-header');
INSERT INTO "webjet_cms"."_db_" ("id", "create_date", "note") VALUES (538, '2024-02-14', 'NEW MODULE: replaceAll');




INSERT INTO "webjet_cms"."calendar_types" ("type_id", "name", "schvalovatel_id", "domain_id") VALUES (1, 'Výstava', -1, 1);
INSERT INTO "webjet_cms"."calendar_types" ("type_id", "name", "schvalovatel_id", "domain_id") VALUES (2, 'Šport', -1, 1);
INSERT INTO "webjet_cms"."calendar_types" ("type_id", "name", "schvalovatel_id", "domain_id") VALUES (3, 'Kultúra', -1, 1);
INSERT INTO "webjet_cms"."calendar_types" ("type_id", "name", "schvalovatel_id", "domain_id") VALUES (4, 'Rodina', -1, 1);
INSERT INTO "webjet_cms"."calendar_types" ("type_id", "name", "schvalovatel_id", "domain_id") VALUES (5, 'Konferencia', -1, 1);



INSERT INTO "webjet_cms"."chat_rooms" ("room_id", "room_name", "room_description", "room_class", "max_users", "allow_similar_names", "lng", "moderator_name", "moderator_username", "moderator_password", "hide_in_public_list") VALUES (1, 'pokec', 'Volny pokec', 'sk.iway.iwcm.components.chat.HtmlChatRoom', 50, false, 'sk', NULL, NULL, NULL, false);
INSERT INTO "webjet_cms"."chat_rooms" ("room_id", "room_name", "room_description", "room_class", "max_users", "allow_similar_names", "lng", "moderator_name", "moderator_username", "moderator_password", "hide_in_public_list") VALUES (2, 'moderovany pokec', 'Moderovany pokec', 'sk.iway.iwcm.components.chat.ModeratedRoom', 50, false, 'sk', 'Moderator', 'moderator', 'heslo', false);



INSERT INTO "webjet_cms"."cookies" ("cookie_id", "domain_id", "user_id", "save_date", "cookie_name", "description", "provider", "purpouse", "validity", "type", "classification", "application", "typical_value", "party_3rd") VALUES (1, 0, -1, NULL, 'JSESSIONID', NULL, NULL, NULL, NULL, 'http', 'nutne', NULL, NULL, NULL);
INSERT INTO "webjet_cms"."cookies" ("cookie_id", "domain_id", "user_id", "save_date", "cookie_name", "description", "provider", "purpouse", "validity", "type", "classification", "application", "typical_value", "party_3rd") VALUES (2, 0, -1, NULL, 'lng', NULL, NULL, NULL, NULL, 'http', 'nutne', NULL, NULL, NULL);
INSERT INTO "webjet_cms"."cookies" ("cookie_id", "domain_id", "user_id", "save_date", "cookie_name", "description", "provider", "purpouse", "validity", "type", "classification", "application", "typical_value", "party_3rd") VALUES (3, 0, -1, NULL, 'statBrowserIdNew', NULL, NULL, NULL, NULL, 'http', 'statisticke', NULL, NULL, NULL);
INSERT INTO "webjet_cms"."cookies" ("cookie_id", "domain_id", "user_id", "save_date", "cookie_name", "description", "provider", "purpouse", "validity", "type", "classification", "application", "typical_value", "party_3rd") VALUES (4, 0, -1, NULL, 'forumemail', NULL, NULL, NULL, NULL, 'http', 'statisticke', NULL, NULL, NULL);
INSERT INTO "webjet_cms"."cookies" ("cookie_id", "domain_id", "user_id", "save_date", "cookie_name", "description", "provider", "purpouse", "validity", "type", "classification", "application", "typical_value", "party_3rd") VALUES (5, 0, -1, NULL, 'forumname', NULL, NULL, NULL, NULL, 'http', 'statisticke', NULL, NULL, NULL);
INSERT INTO "webjet_cms"."cookies" ("cookie_id", "domain_id", "user_id", "save_date", "cookie_name", "description", "provider", "purpouse", "validity", "type", "classification", "application", "typical_value", "party_3rd") VALUES (6, 0, -1, NULL, '_ga', NULL, NULL, NULL, NULL, 'http', 'marketingove', NULL, NULL, NULL);
INSERT INTO "webjet_cms"."cookies" ("cookie_id", "domain_id", "user_id", "save_date", "cookie_name", "description", "provider", "purpouse", "validity", "type", "classification", "application", "typical_value", "party_3rd") VALUES (7, 0, -1, NULL, '_gat', NULL, NULL, NULL, NULL, 'http', 'marketingove', NULL, NULL, NULL);
INSERT INTO "webjet_cms"."cookies" ("cookie_id", "domain_id", "user_id", "save_date", "cookie_name", "description", "provider", "purpouse", "validity", "type", "classification", "application", "typical_value", "party_3rd") VALUES (8, 0, -1, NULL, '__utmt', NULL, NULL, NULL, NULL, 'http', 'marketingove', NULL, NULL, NULL);
INSERT INTO "webjet_cms"."cookies" ("cookie_id", "domain_id", "user_id", "save_date", "cookie_name", "description", "provider", "purpouse", "validity", "type", "classification", "application", "typical_value", "party_3rd") VALUES (9, 0, -1, NULL, '__utma', NULL, NULL, NULL, NULL, 'http', 'marketingove', NULL, NULL, NULL);
INSERT INTO "webjet_cms"."cookies" ("cookie_id", "domain_id", "user_id", "save_date", "cookie_name", "description", "provider", "purpouse", "validity", "type", "classification", "application", "typical_value", "party_3rd") VALUES (10, 0, -1, NULL, '__utmb', NULL, NULL, NULL, NULL, 'http', 'marketingove', NULL, NULL, NULL);
INSERT INTO "webjet_cms"."cookies" ("cookie_id", "domain_id", "user_id", "save_date", "cookie_name", "description", "provider", "purpouse", "validity", "type", "classification", "application", "typical_value", "party_3rd") VALUES (11, 0, -1, NULL, '__utmc', NULL, NULL, NULL, NULL, 'http', 'marketingove', NULL, NULL, NULL);
INSERT INTO "webjet_cms"."cookies" ("cookie_id", "domain_id", "user_id", "save_date", "cookie_name", "description", "provider", "purpouse", "validity", "type", "classification", "application", "typical_value", "party_3rd") VALUES (12, 0, -1, NULL, '__utmz', NULL, NULL, NULL, NULL, 'http', 'marketingove', NULL, NULL, NULL);
INSERT INTO "webjet_cms"."cookies" ("cookie_id", "domain_id", "user_id", "save_date", "cookie_name", "description", "provider", "purpouse", "validity", "type", "classification", "application", "typical_value", "party_3rd") VALUES (13, 0, -1, NULL, '__utmv', NULL, NULL, NULL, NULL, 'http', 'marketingove', NULL, NULL, NULL);
INSERT INTO "webjet_cms"."cookies" ("cookie_id", "domain_id", "user_id", "save_date", "cookie_name", "description", "provider", "purpouse", "validity", "type", "classification", "application", "typical_value", "party_3rd") VALUES (14, 0, -1, NULL, 'id', NULL, NULL, NULL, NULL, 'http', 'marketingove', NULL, NULL, NULL);
INSERT INTO "webjet_cms"."cookies" ("cookie_id", "domain_id", "user_id", "save_date", "cookie_name", "description", "provider", "purpouse", "validity", "type", "classification", "application", "typical_value", "party_3rd") VALUES (15, 0, -1, NULL, 'drt', NULL, NULL, NULL, NULL, 'http', 'marketingove', NULL, NULL, NULL);


INSERT INTO "webjet_cms"."crontab" ("id", "task_name", "second", "minute", "hour", "dayofmonth", "month", "dayofweek", "year", "task", "extrainfo", "businessdays", "cluster_node", "audit_task", "run_at_startup", "enable_task") VALUES (1, 'Zmazanie starých udalostí v kalendári', '0', '*/10', '*', '*', '*', '*', '*', 'sk.iway.iwcm.calendar.CalendarDB', NULL, 'true', 'all', '1', false, true);
INSERT INTO "webjet_cms"."crontab" ("id", "task_name", "second", "minute", "hour", "dayofmonth", "month", "dayofweek", "year", "task", "extrainfo", "businessdays", "cluster_node", "audit_task", "run_at_startup", "enable_task") VALUES (3, 'Kontrola pozícií kľúčových slov v Google', '0', '0', '2', '*', '*', '*', '*', 'sk.iway.iwcm.components.seo.SeoManager', '', '0', 'all', '1', false, true);
INSERT INTO "webjet_cms"."crontab" ("id", "task_name", "second", "minute", "hour", "dayofmonth", "month", "dayofweek", "year", "task", "extrainfo", "businessdays", "cluster_node", "audit_task", "run_at_startup", "enable_task") VALUES (4, 'Vytvorenie monitoringu systému', '*/30', '*', '*', '*', '*', '*', '*', 'sk.iway.iwcm.system.monitoring.MonitoringManager', '', '0', 'all', '0', false, true);
INSERT INTO "webjet_cms"."crontab" ("id", "task_name", "second", "minute", "hour", "dayofmonth", "month", "dayofweek", "year", "task", "extrainfo", "businessdays", "cluster_node", "audit_task", "run_at_startup", "enable_task") VALUES (5, 'Publikovanie zmien v adresároch web stránok', '0', '*', '*', '*', '*', '*', '*', 'sk.iway.iwcm.doc.GroupPublisher', NULL, '0', NULL, '0', false, true);
INSERT INTO "webjet_cms"."crontab" ("id", "task_name", "second", "minute", "hour", "dayofmonth", "month", "dayofweek", "year", "task", "extrainfo", "businessdays", "cluster_node", "audit_task", "run_at_startup", "enable_task") VALUES (6, 'Zmazanie záznamov heatmapy', '0', '30', '5', '*', '*', '*', '*', 'sk.iway.iwcm.stat.heat_map.HeatMapCleaner', NULL, 'true', NULL, '0', false, true);
INSERT INTO "webjet_cms"."crontab" ("id", "task_name", "second", "minute", "hour", "dayofmonth", "month", "dayofweek", "year", "task", "extrainfo", "businessdays", "cluster_node", "audit_task", "run_at_startup", "enable_task") VALUES (7, 'Zápis štatistiky návštevnosti', '0', '*', '*', '*', '*', '*', '*', 'sk.iway.iwcm.stat.StatWriteBuffer', NULL, 'true', NULL, '0', false, true);
INSERT INTO "webjet_cms"."crontab" ("id", "task_name", "second", "minute", "hour", "dayofmonth", "month", "dayofweek", "year", "task", "extrainfo", "businessdays", "cluster_node", "audit_task", "run_at_startup", "enable_task") VALUES (9, 'Aktualizácia persistent cache', '30', '*/5', '*', '*', '*', '*', '*', 'sk.iway.iwcm.system.cache.PersistentCacheDB', NULL, 'true', 'node3', 'false', false, true);
INSERT INTO "webjet_cms"."crontab" ("id", "task_name", "second", "minute", "hour", "dayofmonth", "month", "dayofweek", "year", "task", "extrainfo", "businessdays", "cluster_node", "audit_task", "run_at_startup", "enable_task") VALUES (10, 'Publikovanie zmien v konfigurácii', '0', '*/5', '*', '*', '*', '*', '2013', 'sk.iway.iwcm.system.ConfPreparedPublisher', NULL, 'false', 'all', 'false', false, true);
INSERT INTO "webjet_cms"."crontab" ("id", "task_name", "second", "minute", "hour", "dayofmonth", "month", "dayofweek", "year", "task", "extrainfo", "businessdays", "cluster_node", "audit_task", "run_at_startup", "enable_task") VALUES (11, 'Publikovanie zmien súborov v Archíve súborov', '0', '*/5', '*', '*', '*', '*', '*', 'sk.iway.iwcm.components.file_archiv.FileArchivatorInsertLater', NULL, 'true', 'all', 'false', false, false);



INSERT INTO "webjet_cms"."documents" ("doc_id", "title", "data", "data_asc", "external_link", "navbar", "date_created", "publish_start", "publish_end", "author_id", "group_id", "temp_id", "views_total", "views_month", "searchable", "available", "cacheable", "file_name", "file_change", "sort_priority", "header_doc_id", "menu_doc_id", "footer_doc_id", "password_protected", "html_head", "html_data", "perex_place", "perex_image", "perex_group", "show_in_menu", "event_date", "virtual_path", "sync_id", "sync_status", "logon_page_doc_id", "right_menu_doc_id", "field_a", "field_b", "field_c", "field_d", "field_e", "field_f", "field_g", "field_h", "field_i", "field_j", "field_k", "field_l", "disable_after_end", "forum_count", "field_m", "field_n", "field_o", "field_p", "field_q", "field_r", "field_s", "field_t", "require_ssl", "root_group_l1", "root_group_l2", "root_group_l3", "temp_field_a_docid", "temp_field_b_docid", "temp_field_c_docid", "temp_field_d_docid", "show_in_navbar", "show_in_sitemap", "logged_show_in_menu", "logged_show_in_navbar", "logged_show_in_sitemap", "url_inherit_group", "generate_url_from_title", "editor_virtual_path", "publish_after_start") VALUES (2, 'Default navigation', '!INCLUDE(/components/menu/menu_ul_li.jsp, rootGroupId=1, startOffset=0, maxLevel=3, classes=basic, openAllItems=true, rootUlId=mainNavigation)!', '', '', 'Default menu', '2003-05-23 00:00:00+02', NULL, NULL, 1, 3, 1, 0, 0, false, true, false, NULL, '2003-08-27 10:36:59+02', 50, -1, -1, -1, NULL, '', '', NULL, NULL, NULL, true, NULL, NULL, 2, 2, -1, -1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, 20, 3, NULL, -1, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, false, false, NULL, false);
INSERT INTO "webjet_cms"."documents" ("doc_id", "title", "data", "data_asc", "external_link", "navbar", "date_created", "publish_start", "publish_end", "author_id", "group_id", "temp_id", "views_total", "views_month", "searchable", "available", "cacheable", "file_name", "file_change", "sort_priority", "header_doc_id", "menu_doc_id", "footer_doc_id", "password_protected", "html_head", "html_data", "perex_place", "perex_image", "perex_group", "show_in_menu", "event_date", "virtual_path", "sync_id", "sync_status", "logon_page_doc_id", "right_menu_doc_id", "field_a", "field_b", "field_c", "field_d", "field_e", "field_f", "field_g", "field_h", "field_i", "field_j", "field_k", "field_l", "disable_after_end", "forum_count", "field_m", "field_n", "field_o", "field_p", "field_q", "field_r", "field_s", "field_t", "require_ssl", "root_group_l1", "root_group_l2", "root_group_l3", "temp_field_a_docid", "temp_field_b_docid", "temp_field_c_docid", "temp_field_d_docid", "show_in_navbar", "show_in_sitemap", "logged_show_in_menu", "logged_show_in_navbar", "logged_show_in_sitemap", "url_inherit_group", "generate_url_from_title", "editor_virtual_path", "publish_after_start") VALUES (7, 'Stranka s nadpisom a 2 stlpcami', '<table cellspacing=''0'' cellpadding=''2'' width=''100%'' border=''0''>
    <tbody>
        <tr>
            <td colspan=''2''>
            <h1>Toto je nadpis stránky</h1>
            </td>
        </tr>
        <tr>
            <td valign=''top'' width=''50%''>
            <p> Toto je stlpec 1</p>
            <p>Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Nam pulvinar sollicitudin est. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae. Duis nulla risus, varius non, condimentum nec, adipiscing vel, nisl. Vestibulum facilisis lorem. Nam tortor tellus, venenatis vitae, tincidunt in, fringilla ut, arcu. Suspendisse imperdiet magna egestas nibh. Sed rutrum. Nulla pellentesque mollis leo. Mauris vel metus eget dolor feugiat consequat. Nam dapibus dapibus felis. Phasellus sit amet tortor vel ante dictum aliquam. Integer vehicula nisi et quam euismod commodo. Nam vel justo. Sed mattis libero non enim. Donec feugiat tortor. Quisque mauris. Nullam urna turpis, aliquam sit amet, posuere eget, consectetuer nec, massa. In non mauris.</p>
            </td>
            <td valign=''top'' width=''50%''>
            <p> Toto je stlpec 2</p>
            <p>Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Suspendisse in libero ac turpis porttitor porta. Aliquam varius massa vitae massa. Pellentesque fringilla diam vitae velit. Nulla facilisi. Sed id enim. Aenean in urna. Vivamus sed urna at elit porttitor ultrices. Morbi dolor felis, facilisis at, congue quis, convallis et, ipsum. Donec rutrum nulla luctus arcu luctus blandit. Mauris vitae ipsum et risus venenatis scelerisque. Duis ipsum. Donec viverra purus. Fusce non libero. Cras elementum. Curabitur fermentum elit at lorem. Etiam facilisis. Pellentesque nec erat ut ipsum consectetuer sodales. Morbi nec dolor ac velit rutrum faucibus. </p>
            </td>
        </tr>
    </tbody>
</table>', '<table cellspacing=''0'' cellpadding=''2'' width=''100%'' border=''0''>
    <tbody>
        <tr>
            <td colspan=''2''>
            <h1>toto je nadpis stranky</h1>
            </td>
        </tr>
        <tr>
            <td valign=''top'' width=''50%''>
            <p> toto je stlpec 1</p>
            <p>lorem ipsum dolor sit amet, consectetuer adipiscing elit. nam pulvinar sollicitudin est. vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae. duis nulla risus, varius non, condimentum nec, adipiscing vel, nisl. vestibulum facilisis lorem. nam tortor tellus, venenatis vitae, tincidunt in, fringilla ut, arcu. suspendisse imperdiet magna egestas nibh. sed rutrum. nulla pellentesque mollis leo. mauris vel metus eget dolor feugiat consequat. nam dapibus dapibus felis. phasellus sit amet tortor vel ante dictum aliquam. integer vehicula nisi et quam euismod commodo. nam vel justo. sed mattis libero non enim. donec feugiat tortor. quisque mauris. nullam urna turpis, aliquam sit amet, posuere eget, consectetuer nec, massa. in non mauris.</p>
            </td>
            <td valign=''top'' width=''50%''>
            <p> toto je stlpec 2</p>
            <p>lorem ipsum dolor sit amet, consectetuer adipiscing elit. suspendisse in libero ac turpis porttitor porta. aliquam varius massa vitae massa. pellentesque fringilla diam vitae velit. nulla facilisi. sed id enim. aenean in urna. vivamus sed urna at elit porttitor ultrices. morbi dolor felis, facilisis at, congue quis, convallis et, ipsum. donec rutrum nulla luctus arcu luctus blandit. mauris vitae ipsum et risus venenatis scelerisque. duis ipsum. donec viverra purus. fusce non libero. cras elementum. curabitur fermentum elit at lorem. etiam facilisis. pellentesque nec erat ut ipsum consectetuer sodales. morbi nec dolor ac velit rutrum faucibus. </p>
            </td>
        </tr>
    </tbody>
</table>', '', 'Stranka s nadpisom a 2 stlpcami', '2003-10-05 22:43:25+02', NULL, NULL, 1, 2, 1, 0, 0, true, true, false, NULL, '2003-10-05 22:43:25+02', 1, -1, -1, -1, NULL, '', NULL, '', '', NULL, true, NULL, NULL, 7, 2, -1, -1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, 20, 2, NULL, -1, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, false, false, NULL, false);
INSERT INTO "webjet_cms"."documents" ("doc_id", "title", "data", "data_asc", "external_link", "navbar", "date_created", "publish_start", "publish_end", "author_id", "group_id", "temp_id", "views_total", "views_month", "searchable", "available", "cacheable", "file_name", "file_change", "sort_priority", "header_doc_id", "menu_doc_id", "footer_doc_id", "password_protected", "html_head", "html_data", "perex_place", "perex_image", "perex_group", "show_in_menu", "event_date", "virtual_path", "sync_id", "sync_status", "logon_page_doc_id", "right_menu_doc_id", "field_a", "field_b", "field_c", "field_d", "field_e", "field_f", "field_g", "field_h", "field_i", "field_j", "field_k", "field_l", "disable_after_end", "forum_count", "field_m", "field_n", "field_o", "field_p", "field_q", "field_r", "field_s", "field_t", "require_ssl", "root_group_l1", "root_group_l2", "root_group_l3", "temp_field_a_docid", "temp_field_b_docid", "temp_field_c_docid", "temp_field_d_docid", "show_in_navbar", "show_in_sitemap", "logged_show_in_menu", "logged_show_in_navbar", "logged_show_in_sitemap", "url_inherit_group", "generate_url_from_title", "editor_virtual_path", "publish_after_start") VALUES (9, 'Default left menu', '!INCLUDE(/components/menu/menu_ul_li.jsp, rootGroupId=1, startOffset=1, maxLevel=3, classes=basic, openAllItems=true, rootUlId=mainNavigation)!', '', '', 'Default menu', '2003-05-23 00:00:00+02', NULL, NULL, 1, 3, 1, 0, 0, false, true, false, NULL, '2003-08-27 10:36:59+02', 50, -1, -1, -1, NULL, '', '', NULL, NULL, NULL, true, NULL, NULL, 9, 2, -1, -1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, 20, 3, NULL, -1, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, false, false, NULL, false);
INSERT INTO "webjet_cms"."documents" ("doc_id", "title", "data", "data_asc", "external_link", "navbar", "date_created", "publish_start", "publish_end", "author_id", "group_id", "temp_id", "views_total", "views_month", "searchable", "available", "cacheable", "file_name", "file_change", "sort_priority", "header_doc_id", "menu_doc_id", "footer_doc_id", "password_protected", "html_head", "html_data", "perex_place", "perex_image", "perex_group", "show_in_menu", "event_date", "virtual_path", "sync_id", "sync_status", "logon_page_doc_id", "right_menu_doc_id", "field_a", "field_b", "field_c", "field_d", "field_e", "field_f", "field_g", "field_h", "field_i", "field_j", "field_k", "field_l", "disable_after_end", "forum_count", "field_m", "field_n", "field_o", "field_p", "field_q", "field_r", "field_s", "field_t", "require_ssl", "root_group_l1", "root_group_l2", "root_group_l3", "temp_field_a_docid", "temp_field_b_docid", "temp_field_c_docid", "temp_field_d_docid", "show_in_navbar", "show_in_sitemap", "logged_show_in_menu", "logged_show_in_navbar", "logged_show_in_sitemap", "url_inherit_group", "generate_url_from_title", "editor_virtual_path", "publish_after_start") VALUES (4, 'Hlavna stranka', '<p>Vitajte na demo stránke systému WebJET.</p>
<p>Viac informácií o systéme nájdete na stránke <a href=''http://www.webjetcms.sk/'' target=''_blank''>www.webjetcms.sk</a>.</p>', '   vitajte na demo stranke systemu webjet.
   viac informacii o systeme najdete na stranke                                                    www.webjetcms.sk    .    <h1>hlavna stranka</h1>
', '', 'Hlavna stranka', '2024-02-14 15:04:14.106+01', NULL, NULL, 1, 1, 1, 0, 0, true, true, false, '/Slovensky', '2003-10-05 22:43:25+02', 1, -1, -1, -1, NULL, '', NULL, '', '', NULL, true, NULL, '/sk/', 4, 1, -1, -1, '', '', '', '', '', '', '', '', '', '', '', '', false, 0, '', '', '', '', '', '', '', '', false, 1, NULL, NULL, -1, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, false, false, NULL, false);
INSERT INTO "webjet_cms"."documents" ("doc_id", "title", "data", "data_asc", "external_link", "navbar", "date_created", "publish_start", "publish_end", "author_id", "group_id", "temp_id", "views_total", "views_month", "searchable", "available", "cacheable", "file_name", "file_change", "sort_priority", "header_doc_id", "menu_doc_id", "footer_doc_id", "password_protected", "html_head", "html_data", "perex_place", "perex_image", "perex_group", "show_in_menu", "event_date", "virtual_path", "sync_id", "sync_status", "logon_page_doc_id", "right_menu_doc_id", "field_a", "field_b", "field_c", "field_d", "field_e", "field_f", "field_g", "field_h", "field_i", "field_j", "field_k", "field_l", "disable_after_end", "forum_count", "field_m", "field_n", "field_o", "field_p", "field_q", "field_r", "field_s", "field_t", "require_ssl", "root_group_l1", "root_group_l2", "root_group_l3", "temp_field_a_docid", "temp_field_b_docid", "temp_field_c_docid", "temp_field_d_docid", "show_in_navbar", "show_in_sitemap", "logged_show_in_menu", "logged_show_in_navbar", "logged_show_in_sitemap", "url_inherit_group", "generate_url_from_title", "editor_virtual_path", "publish_after_start") VALUES (1, 'Default header', '<h1>Headline</h1><p>Lorem ipsum dolor sit amet, consectetur adipisicing elit. Sit aspernatur labore vel rem adipisci commodi odit eum vitae asperiores esse?</p><a href=''#'' class=''btn btn-default''>Lorem ipsum.</a>', '', '', 'Default hlavička', '2002-07-25 00:00:00+02', NULL, NULL, 1, 4, 1, 0, 0, false, true, false, NULL, '2003-04-11 15:51:10+02', 50, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, NULL, true, NULL, NULL, 1, 2, -1, -1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, 20, 4, NULL, -1, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, false, false, NULL, false);
INSERT INTO "webjet_cms"."documents" ("doc_id", "title", "data", "data_asc", "external_link", "navbar", "date_created", "publish_start", "publish_end", "author_id", "group_id", "temp_id", "views_total", "views_month", "searchable", "available", "cacheable", "file_name", "file_change", "sort_priority", "header_doc_id", "menu_doc_id", "footer_doc_id", "password_protected", "html_head", "html_data", "perex_place", "perex_image", "perex_group", "show_in_menu", "event_date", "virtual_path", "sync_id", "sync_status", "logon_page_doc_id", "right_menu_doc_id", "field_a", "field_b", "field_c", "field_d", "field_e", "field_f", "field_g", "field_h", "field_i", "field_j", "field_k", "field_l", "disable_after_end", "forum_count", "field_m", "field_n", "field_o", "field_p", "field_q", "field_r", "field_s", "field_t", "require_ssl", "root_group_l1", "root_group_l2", "root_group_l3", "temp_field_a_docid", "temp_field_b_docid", "temp_field_c_docid", "temp_field_d_docid", "show_in_navbar", "show_in_sitemap", "logged_show_in_menu", "logged_show_in_navbar", "logged_show_in_sitemap", "url_inherit_group", "generate_url_from_title", "editor_virtual_path", "publish_after_start") VALUES (3, 'Default footer', '<p>© !YEAR! InterWay, a. s. All Rights Reserved. Page generated by WebJET CMS.</p>', '', '', 'Default pätička', '2002-07-25 00:00:00+02', NULL, NULL, 1, 4, 1, 0, 0, false, true, false, NULL, '2003-04-11 15:51:10+02', 50, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, NULL, true, NULL, NULL, 3, 2, -1, -1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, 20, 4, NULL, -1, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, false, false, NULL, false);
INSERT INTO "webjet_cms"."documents" ("doc_id", "title", "data", "data_asc", "external_link", "navbar", "date_created", "publish_start", "publish_end", "author_id", "group_id", "temp_id", "views_total", "views_month", "searchable", "available", "cacheable", "file_name", "file_change", "sort_priority", "header_doc_id", "menu_doc_id", "footer_doc_id", "password_protected", "html_head", "html_data", "perex_place", "perex_image", "perex_group", "show_in_menu", "event_date", "virtual_path", "sync_id", "sync_status", "logon_page_doc_id", "right_menu_doc_id", "field_a", "field_b", "field_c", "field_d", "field_e", "field_f", "field_g", "field_h", "field_i", "field_j", "field_k", "field_l", "disable_after_end", "forum_count", "field_m", "field_n", "field_o", "field_p", "field_q", "field_r", "field_s", "field_t", "require_ssl", "root_group_l1", "root_group_l2", "root_group_l3", "temp_field_a_docid", "temp_field_b_docid", "temp_field_c_docid", "temp_field_d_docid", "show_in_navbar", "show_in_sitemap", "logged_show_in_menu", "logged_show_in_navbar", "logged_show_in_sitemap", "url_inherit_group", "generate_url_from_title", "editor_virtual_path", "publish_after_start") VALUES (6, 'Podstránka 2', '<h1>Podstránka 2</h1><p>Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi id sapien ut massa interdum ultricies. Sed eget enim in ante ornare feugiat. Curabitur risus lectus, iaculis sed, pulvinar quis, convallis euismod, massa. Curabitur est enim, varius sed, hendrerit at, convallis elementum, tortor. In hac habitasse platea dictumst. Proin sagittis massa ac massa. Maecenas vel libero. Curabitur vestibulum pellentesque elit. Phasellus aliquet quam quis urna. In ultrices est vel lorem. Aliquam sit amet mi et nulla scelerisque vestibulum. Donec pellentesque tellus vitae massa. Curabitur euismod. Donec quis pede. Vivamus nulla mauris, aliquet sed, aliquet vitae, fringilla id, massa. Etiam id magna sed dolor rhoncus condimentum. Ut lacinia nonummy odio. Pellentesque interdum, tortor vitae congue elementum, nibh ipsum pellentesque quam, sed tempor tortor est sit amet felis. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nullam sagittis tellus vitae lorem.</p>', '    podstranka 2        lorem ipsum dolor sit amet, consectetuer adipiscing elit. morbi id sapien ut massa interdum ultricies. sed eget enim in ante ornare feugiat. curabitur risus lectus, iaculis sed, pulvinar quis, convallis euismod, massa. curabitur est enim, varius sed, hendrerit at, convallis elementum, tortor. in hac habitasse platea dictumst. proin sagittis massa ac massa. maecenas vel libero. curabitur vestibulum pellentesque elit. phasellus aliquet quam quis urna. in ultrices est vel lorem. aliquam sit amet mi et nulla scelerisque vestibulum. donec pellentesque tellus vitae massa. curabitur euismod. donec quis pede. vivamus nulla mauris, aliquet sed, aliquet vitae, fringilla id, massa. etiam id magna sed dolor rhoncus condimentum. ut lacinia nonummy odio. pellentesque interdum, tortor vitae congue elementum, nibh ipsum pellentesque quam, sed tempor tortor est sit amet felis. cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. nullam sagittis tellus vitae lorem.    ', '', 'Podstránka 2', '2024-02-14 15:04:14.341+01', NULL, NULL, 1, 1, 1, 0, 0, true, true, false, '/Slovensky', '2003-10-05 22:43:25+02', 1, -1, -1, -1, NULL, '', NULL, '', '', NULL, true, NULL, '/sk/podstranka-2.html', 6, 1, -1, -1, '', '', '', '', '', '', '', '', '', '', '', '', false, 0, '', '', '', '', '', '', '', '', false, 1, NULL, NULL, -1, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, false, false, NULL, false);
INSERT INTO "webjet_cms"."documents" ("doc_id", "title", "data", "data_asc", "external_link", "navbar", "date_created", "publish_start", "publish_end", "author_id", "group_id", "temp_id", "views_total", "views_month", "searchable", "available", "cacheable", "file_name", "file_change", "sort_priority", "header_doc_id", "menu_doc_id", "footer_doc_id", "password_protected", "html_head", "html_data", "perex_place", "perex_image", "perex_group", "show_in_menu", "event_date", "virtual_path", "sync_id", "sync_status", "logon_page_doc_id", "right_menu_doc_id", "field_a", "field_b", "field_c", "field_d", "field_e", "field_f", "field_g", "field_h", "field_i", "field_j", "field_k", "field_l", "disable_after_end", "forum_count", "field_m", "field_n", "field_o", "field_p", "field_q", "field_r", "field_s", "field_t", "require_ssl", "root_group_l1", "root_group_l2", "root_group_l3", "temp_field_a_docid", "temp_field_b_docid", "temp_field_c_docid", "temp_field_d_docid", "show_in_navbar", "show_in_sitemap", "logged_show_in_menu", "logged_show_in_navbar", "logged_show_in_sitemap", "url_inherit_group", "generate_url_from_title", "editor_virtual_path", "publish_after_start") VALUES (8, 'Kontaktný formulár', '<p>Pred použitím formuláru prosím najskôr naň kliknite pravým tlačítkom, dajte vlastnosti formuláru a zmeňte emailovú adresu.</p>
<p>
<script src=''/components/form/check_form.js'' ENGINE=''text/javascript''></script>
<form name=''formMailForm'' action=''/formmail.do?recipients=test@tester.org&savedb=Kontaktny_formular'' method=''post''>
    <table cellspacing=''0'' cellpadding=''0'' border=''0''>
        <tbody>
            <tr>
                <td>Vaše meno:</td>
                <td><input class=''required'' maxlength=''255'' name=''meno''/> </td>
            </tr>
            <tr>
                <td> Vaša emailová adresa:</td>
                <td><input class=''required email'' maxlength=''255'' name=''email''/> </td>
            </tr>
            <tr>
                <td valign=''top''> Otázka / pripomienka:</td>
                <td><textarea class=''required'' name=''otazka'' rows=''5'' cols=''40''></textarea> </td>
            </tr>
            <tr>
                <td> </td>
                <td align=''right''> <input ENGINE=''submit'' name=''btnSubmit'' value=''Odoslať''/></td>
            </tr>
        </tbody>
    </table>
</form>
</p>
<p> </p>', '<p>pred pouzitim formularu prosim najskor nan kliknite pravym tlacitkom, dajte vlastnosti formularu a zmente emailovu adresu.</p>
<p>
<script src=''/components/form/check_form.js'' ENGINE=''text/javascript''></script>
<form name=''formmailform'' action=''/formmail.do?recipients=test@tester.org&savedb=kontaktny_formular'' method=''post''>
    <table cellspacing=''0'' cellpadding=''0'' border=''0''>
        <tbody>
            <tr>
                <td>vase meno: </td>
                <td><input class=''required'' maxlength=''255'' name=''meno''/> </td>
            </tr>
            <tr>
                <td> vasa emailova adresa:</td>
                <td><input class=''required email'' maxlength=''255'' name=''email''/> </td>
            </tr>
            <tr>
                <td valign=''top''> otazka / pripomienka:</td>
                <td><textarea class=''required'' name=''otazka'' rows=''5'' cols=''40''></textarea> </td>
            </tr>
            <tr>
                <td> </td>
                <td align=''right''> <input ENGINE=''submit'' name=''btnsubmit'' value=''odoslat''/></td>
            </tr>
        </tbody>
    </table>
</form>
</p>
<p> </p>', '', 'Kontaktný formulár', '2003-10-05 22:43:25+02', NULL, NULL, 1, 21, 1, 0, 0, true, true, false, NULL, '2003-10-05 22:43:25+02', 1, -1, -1, -1, NULL, '', NULL, '', '', NULL, true, NULL, NULL, 8, 2, -1, -1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, 20, 21, NULL, -1, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, false, false, NULL, false);
INSERT INTO "webjet_cms"."documents" ("doc_id", "title", "data", "data_asc", "external_link", "navbar", "date_created", "publish_start", "publish_end", "author_id", "group_id", "temp_id", "views_total", "views_month", "searchable", "available", "cacheable", "file_name", "file_change", "sort_priority", "header_doc_id", "menu_doc_id", "footer_doc_id", "password_protected", "html_head", "html_data", "perex_place", "perex_image", "perex_group", "show_in_menu", "event_date", "virtual_path", "sync_id", "sync_status", "logon_page_doc_id", "right_menu_doc_id", "field_a", "field_b", "field_c", "field_d", "field_e", "field_f", "field_g", "field_h", "field_i", "field_j", "field_k", "field_l", "disable_after_end", "forum_count", "field_m", "field_n", "field_o", "field_p", "field_q", "field_r", "field_s", "field_t", "require_ssl", "root_group_l1", "root_group_l2", "root_group_l3", "temp_field_a_docid", "temp_field_b_docid", "temp_field_c_docid", "temp_field_d_docid", "show_in_navbar", "show_in_sitemap", "logged_show_in_menu", "logged_show_in_navbar", "logged_show_in_sitemap", "url_inherit_group", "generate_url_from_title", "editor_virtual_path", "publish_after_start") VALUES (5, 'Podstránka 1', '<p>Toto je podstránka 1</p>
<p>Ak chcete do stránky vložiť predpripravený objekt, kliknite v editore na komponenty a vyberte Predpripravené HTML. Tam si vyberte stránku a kliknutím na OK ju vložte do stránky. Nové predpripravené objekty si môžete vytvoriť v adresári System->HTMLBox.</p>
<p>Pred použitím formuláru prosím najskôr naň kliknite pravým tlačítkom, dajte vlastnosti formuláru a zmeňte emailovú adresu.</p>
<p>
<script src=''/components/form/check_form.js'' ENGINE=''text/javascript''></script>
<script src=''/components/form/fix_e.js'' ENGINE=''text/javascript''></script>
<script src=''/components/form/event_attacher.js'' ENGINE=''text/javascript''></script>
<script src=''/components/form/class_magic.js'' ENGINE=''text/javascript''></script>
<script src=''/components/form/check_form_impl.jsp'' ENGINE=''text/javascript''></script>
<link media=''screen'' href=''/components/form/check_form.css'' ENGINE=''text/css'' rel=''stylesheet''/>
<form name=''formMailForm'' action=''/formmail.do?recipients=test@tester.org&savedb=Kontaktny_formular'' method=''post''>
    <table cellspacing=''0'' cellpadding=''0'' border=''0''>
        <tbody>
            <tr>
                <td>Vaše meno: </td>
                <td><input class=''required invalid'' maxlength=''255'' name=''meno''/> </td>
            </tr>
            <tr>
                <td> Vaša emailová adresa:</td>
                <td><input class=''required email invalid'' maxlength=''255'' name=''email''/> </td>
            </tr>
            <tr>
                <td valign=''top''> Otázka / pripomienka:</td>
                <td><textarea class=''required invalid'' name=''otazka'' rows=''5'' cols=''40''></textarea> </td>
            </tr>
            <tr>
                <td> </td>
                <td align=''right''> <input ENGINE=''submit'' name=''btnSubmit'' value=''Odoslať''/></td>
            </tr>
        </tbody>
    </table>
</form>
</p>
<p> </p>', '   toto je podstranka 1
   ak chcete do stranky vlozit predpripraveny objekt, kliknite v editore na komponenty a vyberte predpripravene html. tam si vyberte stranku a kliknutim na ok ju vlozte do stranky. nove predpripravene objekty si mozete vytvorit v adresari system->htmlbox.
   pred pouzitim formularu prosim najskor nan kliknite pravym tlacitkom, dajte vlastnosti formularu a zmente emailovu adresu.











                    vase meno:



                     vasa emailova adresa:



                                  otazka / pripomienka:










        ', '', 'Podstránka 1', '2024-02-14 15:04:14.298+01', NULL, NULL, 1, 1, 1, 0, 0, true, true, false, '/Slovensky', '2003-10-05 22:43:25+02', 1, -1, -1, -1, NULL, '', NULL, '', '', NULL, true, NULL, '/sk/podstranka-1.html', 5, 1, -1, -1, '', '', '', '', '', '', '', '', '', '', '', '', false, 0, '', '', '', '', '', '', '', '', false, 1, NULL, NULL, -1, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, false, false, NULL, false);






INSERT INTO "webjet_cms"."domain_limits" ("domain_limit_id", "domain", "time_unit", "limit_size", "active", "min_delay", "delay_active") VALUES (1, '*', 'MINUTES', 10, true, 1000, true);



INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.allPhone', 'phone', '^([+]?[s0-9]+)?(d{3}|[(]?[0-9]+[)])?([-]?[s]?[0-9])+$', 1);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.alphabet', 'alphabet', '^[abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZľěščťřžýáíéúůňťóďäôöüĽĚŠČŤŘŽÝÁÍÉÚŮŇŤÓĎÄÔÖÜ ]{1,}$', 2);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.alphabetLowercase', 'alphabetLowercase', '^[abcdefghijklmnopqrstuvwxyzľěščťřžýáíéúůňťóďäô ]{1,}$', 3);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.alphabetUppercase', 'alphabetUppercase', '^[ABCDEFGHIJKLMNOPQRSTUVWXYZĽĚŠČŤŘŽÝÁÍÉÚŮŇŤÓĎÄÔ ]{1,}$', 4);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.alphanumeric', 'alphanumeric', '^[abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ľěščťřžýáíéúůňťóďäôĽĚŠČŤŘŽÝÁÍÉÚŮŇŤÓĎÄÔ ]{1,}$', 5);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.date', 'date', '^(([0]?[123456789])|([12][0-9])|(3[01]))[.]((0?[123456789])|(1[012]))[.][0-9]{4}$', 6);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.datetime', 'datetime', '^[0-9]{1,2}[.][0-9]{1,2}[.][0-9]{4}.[0-9]{1,2}[:][0-9]{1,2}[:][0-9]{1,2}$', 7);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.email', 'email', '^[a-zA-Z0-9]+[a-zA-Z0-9\._+=#$%&-]*[a-zA-Z0-9]+@[a-zA-Z0-9]+[a-zA-Z0-9\._-]*[a-zA-Z0-9]+\.[a-zA-Z]{2,16}$', 8);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.email2', 'email2', '^[a-z0-9]+[a-z0-9\._-]*[a-z0-9]+@[a-z0-9]+[a-z0-9\._-]*[a-z0-9]+\.[a-z]{2,4}$', 9);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.fixedPhone', 'fixedPhone', '(^[0-9]{2,3}[\/][0-9]{3,12}$)|(^[+][0-9]{1,3}[\/][0-9]{1,2}[\/][0-9]{3,12}$)', 10);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.integer', 'integer', '^[-]?[0-9]+$', 11);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.loginChars', 'loginChars', '^[0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_.]{1,}$', 12);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.minLen16', 'minLen16', '.................*', 13);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.minLen2', 'minLen2', '...*', 14);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.minLen3', 'minLen3', '....*', 15);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.minLen4', 'minLen4', '.....*', 16);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.minLen5', 'minLen5', '......*', 17);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.minLen6', 'minLen6', '.......*', 18);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.minLen8', 'minLen8', '.........*', 19);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.mobilePhone', 'mobilePhone', '(^[0-9]{4}[\/][0-9]{6}$)|(^[+][0-9]{1,3}[\/][0-9]{3}[\/][0-9]{6}$)', 20);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.numbers', 'numbers', '^[-]?[0-9]+([,.][0-9]+)?$', 21);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.postalCode', 'postalCode', '^(([0-9]{5})|([0-9]{3} [0-9]{2})|([0-9]{2} [0-9]{3}))$', 22);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.safeChars', 'safeChars', '^[0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_ ]{1,}$', 23);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.safeChars2', 'safeChars2', '^[0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_\\-]{1,}$', 24);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.time', 'time', '^[0-9]{1,2}[:][0-9]{1,2}$', 25);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.trim', 'trim', '^[^ \f\n\r\t\v]+[.]*', 26);
INSERT INTO "webjet_cms"."form_regular_exp" ("title", "type", "reg_exp", "id") VALUES ('checkform.title.url', 'url', '^http[s]?:\/\/[a-zA-Z0-9]+([-_\.]?[a-zA-Z0-9])*\.[a-zA-Z]{2,4}([:0-9]*)(\/{1}[%-_~&=\?\.:a-z0-9]*)*$', 27);


INSERT INTO "webjet_cms"."gallery_dimension" ("dimension_id", "image_path", "image_width", "image_height", "normal_width", "normal_height", "resize_mode", "gallery_name", "gallery_perex", "create_date", "author", "views", "watermark_saturation", "watermark", "watermark_placement", "domain_id") VALUES (5, '/images/gallery', 160, 120, 750, 560, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL, 1);



INSERT INTO "webjet_cms"."gdpr_regexp" ("gdpr_regexp_id", "regexp_name", "regexp_value", "user_id", "date_insert", "domain_id") VALUES (1, 'rodné číslo', '[0-9]{6}/[0-9]{3,4}', -1, NULL, 1);
INSERT INTO "webjet_cms"."gdpr_regexp" ("gdpr_regexp_id", "regexp_name", "regexp_value", "user_id", "date_insert", "domain_id") VALUES (2, 'telefónne číslo 10 samostatných čísel', '[0-9]{10}', -1, NULL, 1);
INSERT INTO "webjet_cms"."gdpr_regexp" ("gdpr_regexp_id", "regexp_name", "regexp_value", "user_id", "date_insert", "domain_id") VALUES (3, 'telefónne číslo iné formáty', '(\+[0-9]{3}) ?[0-9]{3,4} ?[0-9]{3} ?[0-9]{3}', -1, NULL, 1);
INSERT INTO "webjet_cms"."gdpr_regexp" ("gdpr_regexp_id", "regexp_name", "regexp_value", "user_id", "date_insert", "domain_id") VALUES (4, 'rok mesiac deň oddelené znakmi: ./-', '(19|20)dd[- /.](0[1-9]|1[012]|[0-9])[- /.](0[1-9]|[12][0-9]|3[01]|[0-9])', -1, NULL, 1);
INSERT INTO "webjet_cms"."gdpr_regexp" ("gdpr_regexp_id", "regexp_name", "regexp_value", "user_id", "date_insert", "domain_id") VALUES (5, 'deň mesiac rok oddelené znakmi: ./-', '(0[1-9]|[12][0-9]|3[01]|[0-9])[- /.](0[1-9]|1[012]|[0-9])[- /.](19|20)dd', -1, NULL, 1);



INSERT INTO "webjet_cms"."groups" ("group_id", "group_name", "internal", "parent_group_id", "navbar", "default_doc_id", "temp_id", "sort_priority", "password_protected", "url_dir_name", "sync_id", "sync_status", "html_head", "logon_page_doc_id", "domain_name", "new_page_docid_template", "install_name", "field_a", "field_b", "field_c", "field_d", "link_group_id", "lng", "show_in_navbar", "show_in_sitemap", "logged_show_in_navbar", "logged_show_in_sitemap", "menu_type", "logged_menu_type", "hidden_in_admin", "force_group_template") VALUES (2, 'Šablóny', true, 20, 'Šablóny', 4, 1, 1500, NULL, NULL, 2, 2, NULL, -1, NULL, 0, NULL, NULL, NULL, NULL, NULL, -1, NULL, NULL, NULL, NULL, NULL, 1, 1, NULL, false);
INSERT INTO "webjet_cms"."groups" ("group_id", "group_name", "internal", "parent_group_id", "navbar", "default_doc_id", "temp_id", "sort_priority", "password_protected", "url_dir_name", "sync_id", "sync_status", "html_head", "logon_page_doc_id", "domain_name", "new_page_docid_template", "install_name", "field_a", "field_b", "field_c", "field_d", "link_group_id", "lng", "show_in_navbar", "show_in_sitemap", "logged_show_in_navbar", "logged_show_in_sitemap", "menu_type", "logged_menu_type", "hidden_in_admin", "force_group_template") VALUES (3, 'Menu', true, 20, 'Menu', 4, 1, 1501, NULL, NULL, 3, 2, NULL, -1, NULL, 0, NULL, NULL, NULL, NULL, NULL, -1, NULL, NULL, NULL, NULL, NULL, 1, 1, NULL, false);
INSERT INTO "webjet_cms"."groups" ("group_id", "group_name", "internal", "parent_group_id", "navbar", "default_doc_id", "temp_id", "sort_priority", "password_protected", "url_dir_name", "sync_id", "sync_status", "html_head", "logon_page_doc_id", "domain_name", "new_page_docid_template", "install_name", "field_a", "field_b", "field_c", "field_d", "link_group_id", "lng", "show_in_navbar", "show_in_sitemap", "logged_show_in_navbar", "logged_show_in_sitemap", "menu_type", "logged_menu_type", "hidden_in_admin", "force_group_template") VALUES (4, 'Hlavičky-pätičky', true, 20, 'Hlavičky-pätičky', 4, 2, 1502, NULL, NULL, 4, 2, NULL, -1, NULL, 0, NULL, NULL, NULL, NULL, NULL, -1, NULL, NULL, NULL, NULL, NULL, 1, 1, NULL, false);
INSERT INTO "webjet_cms"."groups" ("group_id", "group_name", "internal", "parent_group_id", "navbar", "default_doc_id", "temp_id", "sort_priority", "password_protected", "url_dir_name", "sync_id", "sync_status", "html_head", "logon_page_doc_id", "domain_name", "new_page_docid_template", "install_name", "field_a", "field_b", "field_c", "field_d", "link_group_id", "lng", "show_in_navbar", "show_in_sitemap", "logged_show_in_navbar", "logged_show_in_sitemap", "menu_type", "logged_menu_type", "hidden_in_admin", "force_group_template") VALUES (20, 'System', true, 0, 'System', 4, 1, 1000, NULL, NULL, 20, 2, NULL, -1, NULL, 0, NULL, NULL, NULL, NULL, NULL, -1, NULL, NULL, NULL, NULL, NULL, 1, 1, NULL, false);
INSERT INTO "webjet_cms"."groups" ("group_id", "group_name", "internal", "parent_group_id", "navbar", "default_doc_id", "temp_id", "sort_priority", "password_protected", "url_dir_name", "sync_id", "sync_status", "html_head", "logon_page_doc_id", "domain_name", "new_page_docid_template", "install_name", "field_a", "field_b", "field_c", "field_d", "link_group_id", "lng", "show_in_navbar", "show_in_sitemap", "logged_show_in_navbar", "logged_show_in_sitemap", "menu_type", "logged_menu_type", "hidden_in_admin", "force_group_template") VALUES (21, 'HTMLBox', true, 20, 'HTMLBox', 4, 1, 1503, NULL, NULL, 21, 2, NULL, -1, NULL, 0, NULL, NULL, NULL, NULL, NULL, -1, NULL, NULL, NULL, NULL, NULL, 1, 1, NULL, false);
INSERT INTO "webjet_cms"."groups" ("group_id", "group_name", "internal", "parent_group_id", "navbar", "default_doc_id", "temp_id", "sort_priority", "password_protected", "url_dir_name", "sync_id", "sync_status", "html_head", "logon_page_doc_id", "domain_name", "new_page_docid_template", "install_name", "field_a", "field_b", "field_c", "field_d", "link_group_id", "lng", "show_in_navbar", "show_in_sitemap", "logged_show_in_navbar", "logged_show_in_sitemap", "menu_type", "logged_menu_type", "hidden_in_admin", "force_group_template") VALUES (22, 'Kôš', true, 20, 'Kôš', -1, 1, 10010, NULL, 'Kôš', 0, 0, '', -1, '', 0, '', '', '', '', '', -1, NULL, NULL, NULL, NULL, NULL, 1, 1, false, false);
INSERT INTO "webjet_cms"."groups" ("group_id", "group_name", "internal", "parent_group_id", "navbar", "default_doc_id", "temp_id", "sort_priority", "password_protected", "url_dir_name", "sync_id", "sync_status", "html_head", "logon_page_doc_id", "domain_name", "new_page_docid_template", "install_name", "field_a", "field_b", "field_c", "field_d", "link_group_id", "lng", "show_in_navbar", "show_in_sitemap", "logged_show_in_navbar", "logged_show_in_sitemap", "menu_type", "logged_menu_type", "hidden_in_admin", "force_group_template") VALUES (1, 'Slovensky', false, 0, 'Slovensky', 4, 1, 10, NULL, 'sk', 1, 1, '', 0, '', -1, '', '', '', '', '', 0, '', NULL, NULL, NULL, NULL, 1, 1, false, false);
INSERT INTO "webjet_cms"."groups" ("group_id", "group_name", "internal", "parent_group_id", "navbar", "default_doc_id", "temp_id", "sort_priority", "password_protected", "url_dir_name", "sync_id", "sync_status", "html_head", "logon_page_doc_id", "domain_name", "new_page_docid_template", "install_name", "field_a", "field_b", "field_c", "field_d", "link_group_id", "lng", "show_in_navbar", "show_in_sitemap", "logged_show_in_navbar", "logged_show_in_sitemap", "menu_type", "logged_menu_type", "hidden_in_admin", "force_group_template") VALUES (5, 'English', false, 0, 'English', 0, 1, 30, NULL, 'en', 5, 1, '', 0, '', -1, '', '', '', '', '', 0, '', NULL, NULL, NULL, NULL, 1, 1, false, false);




INSERT INTO "webjet_cms"."inquiry" ("question_id", "question_text", "hours", "question_group", "answer_text_ok", "answer_text_fail", "date_from", "date_to", "question_active", "multiple", "total_clicks", "domain_id") VALUES (1, 'Ako sa vám páči WebJET', 24, 'default', 'Ďakujeme, že ste sa zúčastnili ankety.', 'Ľutujeme, ale tejto ankety ste sa už zúčastnili.', NULL, NULL, true, NULL, NULL, 1);



INSERT INTO "webjet_cms"."inquiry_answers" ("answer_id", "question_id", "answer_text", "answer_clicks", "image_path", "url", "domain_id") VALUES (1, 1, 'Je super', 8, NULL, NULL, 1);
INSERT INTO "webjet_cms"."inquiry_answers" ("answer_id", "question_id", "answer_text", "answer_clicks", "image_path", "url", "domain_id") VALUES (2, 1, 'Neviem, nepoznám', 3, NULL, NULL, 1);




INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('banner_banners', 1, 'banner_banners', 'banner_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('basket_browser_id', 1, 'basket_item', 'browser_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('cache', 1, 'cache', 'cache_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('captcha_dictionary', 1, 'captcha_dictionary', 'id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('ckeditor_upload_counter', 1, '', '');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('contact', 1, 'contact', 'contact_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('cookies', 100, 'cookies', 'cookie_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('crypto_key', 1, NULL, NULL);
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('documents', 1, 'documents', 'doc_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('document_notes', 1, 'document_notes', 'id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('domain_limits', 1, 'domain_limits', 'domain_limit_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('export_dat', 1, 'export_dat', 'export_dat_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('file_archiv', 1, 'file_archiv', 'file_archiv_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('file_archiv_global_id', 1, 'file_archiv', 'global_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('file_history', 1, 'file_history', 'file_history_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('gallery_dimension', 1, 'dimension_id', 'gallery_dimension_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('gdpr_regexp', 6, 'gdpr_regexp', 'gdpr_regexp_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('insert_script', 1, 'insert_script', 'insert_script_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('insert_script_doc', 1, 'insert_script_doc', 'insert_script_doc_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('insert_script_gr', 1, 'insert_script_gr', 'insert_script_gr_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('inventory', 1, 'inventory', 'inventory_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('inventory_detail', 1, 'inventory_detail', 'inventory_detail_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('inventory_log', 1, 'inventory_log', 'inventory_log_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('reservation_object_price', 1, 'reservation_object_price', 'object_price_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('reservation_object_times', 1, 'reservation_object_times', 'object_time_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('stat_browser_id', 1, NULL, NULL);
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('stat_session_id', 1, NULL, NULL);
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('structuremirroring', 1, NULL, NULL);
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('terminologicky_slovnik', 1, 'terminologicky_slovnik', 'terminologicky_slovnik_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('todo', 1, 'todo', 'todo_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('user_perm_groups', 1, 'user_perm_groups', 'group_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('user_perm_groups_perms', 1, 'user_perm_groups_perms', 'perm_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('zmluvy_organizacia', 1, 'zmluvy_organizacia', 'zmluvy_organizacia_id');
INSERT INTO "webjet_cms"."pkey_generator" ("name", "value", "table_name", "table_pkey_name") VALUES ('passwords_history', 21, 'passwords_history', 'passwords_history_id');



INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('a', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('aby', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('aj', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ak', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ako', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ale', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('alebo', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('and', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ani', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ano', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('asi', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('az', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('bez', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('bude', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('budem', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('budes', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('budeme', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('budete', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('budu', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('by', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('bol', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('bola', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('boli', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('bolo', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('byt', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('cez', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('co', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ci', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('dalsi', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('dalsia', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('dalsie', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('dnes', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('do', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ho', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('este', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('for', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('i', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ja', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('je', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jeho', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jej', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ich', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('iba', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ine', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('iny', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('som', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('si', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('sme', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('su', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('k', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('kam', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('kazdy', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('kazda', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('kazde', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('kazdi', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('kde', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ked', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('kto', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ktora', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ktore', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ktorou', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ktory', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ktori', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ku', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('lebo', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('len', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ma', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('mat', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ma', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('mate', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('medzi', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('mi', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('mna', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('mne', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('mnou', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('musiet', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('moct', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('moj', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('moze', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('my', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('na', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nad', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nam', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nas', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nasi', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nie', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nech', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nez', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nic', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('niektory', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nove', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('novy', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nova', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nove', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('novi', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('o', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('od', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('odo', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('of', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('on', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ona', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ono', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('oni', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ony', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('po', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pod', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('podla', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pokial', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('potom', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('prave', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pre', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('preco', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('preto', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pretoze', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('prvy', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('prva', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('prve', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('prvi', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pred', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('predo', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pri', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pyta', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('s', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('sa', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('so', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('si', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('svoje', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('svoj', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('svojich', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('svojim', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('svojimi', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ta', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tak', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('takze', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tato', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('teda', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('te', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('te', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ten', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tento', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('the', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tieto', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tym', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tymto', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tiez', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('to', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('toto', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('toho', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tohoto', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tom', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tomto', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tomuto', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('toto', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tu', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tu', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tuto', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tvoj', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ty', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tvojimi', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('uz', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('v', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vam', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vas', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vase', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vo', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('viac', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vsak', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vsetok', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vy', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('z', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('za', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('zo', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ze', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('peter', 'sk');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('a', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('aby', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('aj', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ale', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('anebo', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ani', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('aniz', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ano', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('asi', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('avska', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('az', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ba', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('bez', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('bude', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('budem', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('budes', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('by', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('byl', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('byla', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('byli', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('bylo', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('byt', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ci', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('clanek', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('clanku', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('clanky', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('co', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('com', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('coz', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('cz', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('dalsi', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('dnes', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('do', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('email', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ho', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('i', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jak', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jake', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jako', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('je', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jeho', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jej', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jeji', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jejich', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jen', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jeste', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jenz', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ji', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jine', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jiz', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jsem', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jses', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jsi', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jsme', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jsou', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('jste', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('k', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('kam', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('kde', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('kdo', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('kdyz', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ke', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ktera', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ktere', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('kteri', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('kterou', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ktery', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ku', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ma', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('mate', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('me', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('mezi', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('mi', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('mit', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('mne', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('mnou', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('muj', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('muze', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('my', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('na', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nad', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nam', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nas', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nasi', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ne', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nebo', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nebot', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('necht', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nejsou', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('není', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('neni', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('net', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nez', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ni', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nic', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nove', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('novy', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nybrz', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('o', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('od', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ode', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('on', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('org', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pak', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('po', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pod', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('podle', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pokud', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pouze', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('prave', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pred', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pres', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pri', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pro', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('proc', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('proto', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('protoze', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('prvni', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('pta', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('re', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('s', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('se', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('si', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('sice', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('spol', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('strana', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('sve', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('svuj', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('svych', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('svym', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('svymi', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ta', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tak', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('take', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('takze', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tamhle', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tato', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tedy', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tema', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('te', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ten', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tedy', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tento', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('teto', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tim', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('timto', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tipy', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('to', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tohle', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('toho', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tohoto', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tom', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tomto', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tomuto', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('totiz', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tu', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tudiz', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tuto', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tvuj', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ty', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('tyto', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('u', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('uz', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('v', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vam', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vas', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vas', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vase', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ve', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vedle', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vice', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vsak', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vsechen', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vy', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('vzdyt', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('z', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('za', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('zda', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('zde', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ze', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('zpet', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('zpravy', 'cz');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('a', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('about', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('above', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('after', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('again', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('against', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('all', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('am', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('an', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('and', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('any', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('are', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('aren''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('as', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('at', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('be', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('because', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('been', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('before', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('being', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('below', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('between', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('both', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('but', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('by', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('can''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('cannot', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('could', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('couldn''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('did', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('didn''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('do', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('does', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('doesn''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('doing', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('don''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('down', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('during', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('each', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('few', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('for', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('from', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('further', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('had', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('hadn''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('has', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('hasn''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('have', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('haven''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('having', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('he', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('he''d', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('he''ll', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('he''s', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('her', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('here', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('here''s', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('hers', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('herself', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('him', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('himself', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('his', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('how', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('how''s', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('i', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('i''d', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('i''ll', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('i''m', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('i''ve', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('if', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('in', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('into', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('is', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('isn''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('it', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('it''s', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('its', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('itself', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('let''s', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('me', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('more', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('most', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('mustn''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('my', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('myself', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('no', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('nor', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('not', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('of', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('off', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('on', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('once', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('only', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('or', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('other', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ought', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('our', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ours', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('ourselves', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('out', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('over', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('own', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('same', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('shan''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('she', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('she''d', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('she''ll', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('she''s', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('should', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('shouldn''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('so', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('some', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('such', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('than', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('that', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('that''s', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('the', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('their', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('theirs', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('them', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('themselves', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('then', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('there', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('there''s', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('these', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('they', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('they''d', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('they''ll', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('they''re', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('they''ve', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('this', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('those', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('through', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('to', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('too', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('under', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('until', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('up', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('very', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('was', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('wasn''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('we', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('we''d', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('we''ll', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('we''re', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('we''ve', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('were', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('weren''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('what', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('what''s', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('when', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('when''s', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('where', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('where''s', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('which', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('while', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('who', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('who''s', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('whom', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('why', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('why''s', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('with', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('won''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('would', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('wouldn''t', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('you', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('you''d', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('you''ll', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('you''re', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('you''ve', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('your', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('yours', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('yourself', 'en');
INSERT INTO "webjet_cms"."stopword" ("word", "language") VALUES ('yourselves', 'en');



INSERT INTO "webjet_cms"."templates" ("temp_id", "temp_name", "forward", "lng", "header_doc_id", "footer_doc_id", "after_body_data", "css", "menu_doc_id", "right_menu_doc_id", "base_css_path", "object_a_doc_id", "object_b_doc_id", "object_c_doc_id", "object_d_doc_id", "available_groups", "template_install_name", "disable_spam_protection", "templates_group_id", "inline_editing_mode") VALUES (1, 'Generic', 'tmp_generic.jsp', 'sk', 1, 3, '', '', 2, -1, '/css/page.css', NULL, NULL, NULL, NULL, NULL, NULL, false, 1, NULL);



INSERT INTO "webjet_cms"."templates_group" ("templates_group_id", "name", "directory", "key_prefix", "inline_editing_mode") VALUES (1, 'nepriradené', '/', NULL, NULL);


INSERT INTO "webjet_cms"."user_disabled_items" ("user_id", "item_name") VALUES (1, 'editorMiniEdit');


INSERT INTO "webjet_cms"."user_groups" ("user_group_id", "user_group_name", "user_group_type", "user_group_comment", "require_approve", "email_doc_id", "allow_user_edit", "require_email_verification") VALUES (1, 'VIP Klienti', 0, NULL, false, -1, false, NULL);
INSERT INTO "webjet_cms"."user_groups" ("user_group_id", "user_group_name", "user_group_type", "user_group_comment", "require_approve", "email_doc_id", "allow_user_edit", "require_email_verification") VALUES (2, 'Obchodní partneri', 0, NULL, false, -1, false, NULL);
INSERT INTO "webjet_cms"."user_groups" ("user_group_id", "user_group_name", "user_group_type", "user_group_comment", "require_approve", "email_doc_id", "allow_user_edit", "require_email_verification") VALUES (3, 'Redaktor', 0, NULL, false, -1, false, NULL);




INSERT INTO "webjet_cms"."users" ("user_id", "title", "first_name", "last_name", "login", "password", "is_admin", "user_groups", "company", "adress", "city", "email", "psc", "country", "phone", "authorized", "editable_groups", "editable_pages", "writable_folders", "last_logon", "module_perms", "disabled_items", "reg_date", "field_a", "field_b", "field_c", "field_d", "field_e", "date_of_birth", "sex_male", "photo", "signature", "forum_rank", "rating_rank", "allow_login_start", "allow_login_end", "authorize_hash", "fax", "delivery_first_name", "delivery_last_name", "delivery_company", "delivery_adress", "delivery_city", "delivery_psc", "delivery_country", "delivery_phone", "position", "parent_id", "password_salt", "domain_id", "mobile_device", "api_key") VALUES (2, '', 'Obchodny', 'Partner', 'partner', 'bcrypt:$2a$12$PLDs60I1iwxVxWUngJM.vu5oaQV87tEGwR.2TTn.pFJIL2i4XoEMu', false, '2', 'InterWay, a. s.', '', '', 'web.spam@interway.sk', '', 'Slovensko', '02/32788888', true, '', NULL, NULL, NULL, NULL, NULL, '2024-01-18 09:42:28+01', NULL, NULL, NULL, NULL, NULL, NULL, true, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 'bcrypt:$2a$12$PLDs60I1iwxVxWUngJM.vu', 1, NULL, NULL);
INSERT INTO "webjet_cms"."users" ("user_id", "title", "first_name", "last_name", "login", "password", "is_admin", "user_groups", "company", "adress", "city", "email", "psc", "country", "phone", "authorized", "editable_groups", "editable_pages", "writable_folders", "last_logon", "module_perms", "disabled_items", "reg_date", "field_a", "field_b", "field_c", "field_d", "field_e", "date_of_birth", "sex_male", "photo", "signature", "forum_rank", "rating_rank", "allow_login_start", "allow_login_end", "authorize_hash", "fax", "delivery_first_name", "delivery_last_name", "delivery_company", "delivery_adress", "delivery_city", "delivery_psc", "delivery_country", "delivery_phone", "position", "parent_id", "password_salt", "domain_id", "mobile_device", "api_key") VALUES (3, '', 'VIP', 'Klient', 'vipklient', 'bcrypt:$2a$12$VbAxo2hKqQ5TUXv7KvGyeO.wXSyMp2/WgeshNNi7RPdzITM6zFfT.', false, '1', 'InterWay, a. s.', '', NULL, 'web.spam@interway.sk', '', 'Slovensko', '02/32788888', true, NULL, NULL, NULL, NULL, NULL, NULL, '2024-01-18 09:42:28+01', NULL, NULL, NULL, NULL, NULL, NULL, true, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 'bcrypt:$2a$12$VbAxo2hKqQ5TUXv7KvGyeO', 1, NULL, NULL);
INSERT INTO "webjet_cms"."users" ("user_id", "title", "first_name", "last_name", "login", "password", "is_admin", "user_groups", "company", "adress", "city", "email", "psc", "country", "phone", "authorized", "editable_groups", "editable_pages", "writable_folders", "last_logon", "module_perms", "disabled_items", "reg_date", "field_a", "field_b", "field_c", "field_d", "field_e", "date_of_birth", "sex_male", "photo", "signature", "forum_rank", "rating_rank", "allow_login_start", "allow_login_end", "authorize_hash", "fax", "delivery_first_name", "delivery_last_name", "delivery_company", "delivery_adress", "delivery_city", "delivery_psc", "delivery_country", "delivery_phone", "position", "parent_id", "password_salt", "domain_id", "mobile_device", "api_key") VALUES (1, '', 'WebJET', 'Administrátor', 'admin', 'bcrypt:$2a$12$kZ7OopAy.sx5MX8gYjbRBOQlzuZ6h6/jsM/XK4Kp2zN90FqrRmog2', true, NULL, 'InterWay, a. s.', '', '', 'web.spam@interway.sk', '', 'Slovakia', '02/32788888', true, '', '', '', '2024-02-14 15:00:39.494+01', NULL, NULL, '2024-02-14 15:03:45.983+01', '', '', '', '', '', NULL, true, '', '', 0, 0, NULL, NULL, NULL, '', '', '', '', '', '', '', '', '', '', 0, 'bcrypt:$2a$12$kZ7OopAy.sx5MX8gYjbRBO', 1, NULL, NULL);



INSERT INTO "webjet_cms"."zmluvy_organizacia" ("zmluvy_organizacia_id", "nazov", "domain_id") VALUES (1, 'default', 1);



SELECT pg_catalog.setval('"webjet_cms"."_adminlog__log_id_seq"', 53, true);



SELECT pg_catalog.setval('"webjet_cms"."_conf_prepared__id_seq"', 3, true);



SELECT pg_catalog.setval('"webjet_cms"."_db__id_seq"', 538, true);



SELECT pg_catalog.setval('"webjet_cms"."_modules__module_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."_properties__id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."adminlog_notify_adminlog_notify_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."appcache_file_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."appcache_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."appcache_page_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."banner_doc_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."banner_gr_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."banner_stat_clicks_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."banner_stat_views_day_view_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."banner_stat_views_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."basket_invoice_payments_payment_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."bazar_advertisements_ad_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."bazar_groups_group_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."calendar_calendar_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."calendar_invitation_calendar_invitation_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."calendar_name_in_year_calendar_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."calendar_types_type_id_seq"', 5, true);



SELECT pg_catalog.setval('"webjet_cms"."chat_rooms_room_id_seq"', 2, true);



SELECT pg_catalog.setval('"webjet_cms"."cluster_refresher_cluster_refresh_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."crontab_id_seq"', 11, true);



SELECT pg_catalog.setval('"webjet_cms"."doc_atr_def_atr_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."doc_atr_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."doc_reactions_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."doc_subscribe_subscribe_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."document_forum_forum_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."documents_doc_id_seq"', 9, true);



SELECT pg_catalog.setval('"webjet_cms"."documents_history_history_id_seq"', 4, true);



SELECT pg_catalog.setval('"webjet_cms"."domain_limits_domain_limit_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."email_files_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."emails_campain_emails_campain_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."emails_email_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."emails_stat_click_click_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."emails_unsubscribed_emails_unsubscribed_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."file_archiv_category_node_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."file_atr_def_atr_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."form_regular_exp_id_seq"', 27, true);



SELECT pg_catalog.setval('"webjet_cms"."forms_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."forum_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."gallery_dimension_dimension_id_seq"', 5, true);



SELECT pg_catalog.setval('"webjet_cms"."gallery_image_id_seq"', 487, true);



SELECT pg_catalog.setval('"webjet_cms"."groups_approve_approve_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."groups_group_id_seq"', 22, true);



SELECT pg_catalog.setval('"webjet_cms"."groups_scheduler_schedule_id_seq"', 4, true);



SELECT pg_catalog.setval('"webjet_cms"."inquiry_answers_answer_id_seq"', 2, true);



SELECT pg_catalog.setval('"webjet_cms"."inquiry_question_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."inquirysimple_answers_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."monitoring_monitoring_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."perex_groups_perex_group_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."proxy_proxy_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."questions_answers_qa_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."quiz_answers_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."quiz_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."quiz_questions_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."quiz_results_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."rating_rating_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."reservation_object_reservation_object_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."reservation_reservation_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."response_headers_response_header_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."seo_bots_seo_bots_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."seo_google_position_seo_google_position_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."seo_keywords_seo_keyword_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."sms_addressbook_book_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."sms_log_log_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."stat_from_from_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."stat_views_2024_2_view_id_seq"', 1, false);



SELECT pg_catalog.setval('"webjet_cms"."stat_views_view_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."templates_temp_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."tips_of_the_day_tip_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."user_group_verify_verify_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."user_groups_user_group_id_seq"', 3, true);



SELECT pg_catalog.setval('"webjet_cms"."user_settings_admin_user_settings_admin_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."user_settings_user_settings_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."users_user_id_seq"', 3, true);



SELECT pg_catalog.setval('"webjet_cms"."zmluvy_prilohy_zmluvy_prilohy_id_seq"', 1, true);



SELECT pg_catalog.setval('"webjet_cms"."zmluvy_zmluvy_id_seq"', 1, true);



ALTER TABLE ONLY "webjet_cms"."adminlog_notify"
    ADD CONSTRAINT "idx_80528_primary" PRIMARY KEY ("adminlog_notify_id");



ALTER TABLE ONLY "webjet_cms"."admin_message"
    ADD CONSTRAINT "idx_80536_primary" PRIMARY KEY ("admin_message_id");



ALTER TABLE ONLY "webjet_cms"."appcache"
    ADD CONSTRAINT "idx_80548_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."appcache_file"
    ADD CONSTRAINT "idx_80557_primary" PRIMARY KEY ("id", "appcache_id");



ALTER TABLE ONLY "webjet_cms"."appcache_page"
    ADD CONSTRAINT "idx_80567_primary" PRIMARY KEY ("id", "appcache_id");



ALTER TABLE ONLY "webjet_cms"."banner_banners"
    ADD CONSTRAINT "idx_80572_primary" PRIMARY KEY ("banner_id");



ALTER TABLE ONLY "webjet_cms"."banner_doc"
    ADD CONSTRAINT "idx_80610_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."banner_gr"
    ADD CONSTRAINT "idx_80615_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."banner_stat_clicks"
    ADD CONSTRAINT "idx_80620_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."banner_stat_views"
    ADD CONSTRAINT "idx_80630_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."banner_stat_views_day"
    ADD CONSTRAINT "idx_80640_primary" PRIMARY KEY ("view_id");



ALTER TABLE ONLY "webjet_cms"."basket_invoice"
    ADD CONSTRAINT "idx_80646_primary" PRIMARY KEY ("basket_invoice_id");



ALTER TABLE ONLY "webjet_cms"."basket_invoice_payments"
    ADD CONSTRAINT "idx_80685_primary" PRIMARY KEY ("payment_id");



ALTER TABLE ONLY "webjet_cms"."basket_item"
    ADD CONSTRAINT "idx_80691_primary" PRIMARY KEY ("basket_item_id");



ALTER TABLE ONLY "webjet_cms"."bazar_advertisements"
    ADD CONSTRAINT "idx_80702_primary" PRIMARY KEY ("ad_id");



ALTER TABLE ONLY "webjet_cms"."bazar_groups"
    ADD CONSTRAINT "idx_80712_primary" PRIMARY KEY ("group_id");



ALTER TABLE ONLY "webjet_cms"."cache"
    ADD CONSTRAINT "idx_80717_primary" PRIMARY KEY ("cache_id");



ALTER TABLE ONLY "webjet_cms"."calendar_invitation"
    ADD CONSTRAINT "idx_80753_primary" PRIMARY KEY ("calendar_invitation_id");



ALTER TABLE ONLY "webjet_cms"."calendar_name_in_year"
    ADD CONSTRAINT "idx_80759_primary" PRIMARY KEY ("calendar_id");



ALTER TABLE ONLY "webjet_cms"."calendar_types"
    ADD CONSTRAINT "idx_80768_primary" PRIMARY KEY ("type_id");



ALTER TABLE ONLY "webjet_cms"."captcha_dictionary"
    ADD CONSTRAINT "idx_80775_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."cluster_monitoring"
    ADD CONSTRAINT "idx_80793_primary" PRIMARY KEY ("node", "type");



ALTER TABLE ONLY "webjet_cms"."cluster_refresher"
    ADD CONSTRAINT "idx_80799_primary" PRIMARY KEY ("cluster_refresh_id");



ALTER TABLE ONLY "webjet_cms"."contact"
    ADD CONSTRAINT "idx_80807_primary" PRIMARY KEY ("contact_id");



ALTER TABLE ONLY "webjet_cms"."cookies"
    ADD CONSTRAINT "idx_80831_primary" PRIMARY KEY ("cookie_id");



ALTER TABLE ONLY "webjet_cms"."crontab"
    ADD CONSTRAINT "idx_80846_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."customer_reviews"
    ADD CONSTRAINT "idx_80867_primary" PRIMARY KEY ("review_id");



ALTER TABLE ONLY "webjet_cms"."dictionary"
    ADD CONSTRAINT "idx_80877_primary" PRIMARY KEY ("dictionary_id");



ALTER TABLE ONLY "webjet_cms"."dirprop"
    ADD CONSTRAINT "idx_80884_primary" PRIMARY KEY ("dir_url");



ALTER TABLE ONLY "webjet_cms"."documents"
    ADD CONSTRAINT "idx_80893_primary" PRIMARY KEY ("doc_id");



ALTER TABLE ONLY "webjet_cms"."documents_history"
    ADD CONSTRAINT "idx_80955_primary" PRIMARY KEY ("history_id");



ALTER TABLE ONLY "webjet_cms"."document_forum"
    ADD CONSTRAINT "idx_81021_primary" PRIMARY KEY ("forum_id");



ALTER TABLE ONLY "webjet_cms"."document_notes"
    ADD CONSTRAINT "idx_81041_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."doc_atr"
    ADD CONSTRAINT "idx_81046_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."doc_atr_def"
    ADD CONSTRAINT "idx_81055_primary" PRIMARY KEY ("atr_id");



ALTER TABLE ONLY "webjet_cms"."doc_reactions"
    ADD CONSTRAINT "idx_81070_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."doc_subscribe"
    ADD CONSTRAINT "idx_81075_primary" PRIMARY KEY ("subscribe_id");



ALTER TABLE ONLY "webjet_cms"."domain_limits"
    ADD CONSTRAINT "idx_81085_primary" PRIMARY KEY ("domain_limit_id");



ALTER TABLE ONLY "webjet_cms"."domain_redirects"
    ADD CONSTRAINT "idx_81091_primary" PRIMARY KEY ("redirect_id");



ALTER TABLE ONLY "webjet_cms"."emails"
    ADD CONSTRAINT "idx_81103_primary" PRIMARY KEY ("email_id");



ALTER TABLE ONLY "webjet_cms"."emails_campain"
    ADD CONSTRAINT "idx_81124_primary" PRIMARY KEY ("emails_campain_id");



ALTER TABLE ONLY "webjet_cms"."emails_stat_click"
    ADD CONSTRAINT "idx_81140_primary" PRIMARY KEY ("click_id");



ALTER TABLE ONLY "webjet_cms"."emails_unsubscribed"
    ADD CONSTRAINT "idx_81149_primary" PRIMARY KEY ("emails_unsubscribed_id");



ALTER TABLE ONLY "webjet_cms"."email_files"
    ADD CONSTRAINT "idx_81155_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."enumeration_data"
    ADD CONSTRAINT "idx_81161_primary" PRIMARY KEY ("enumeration_data_id");



ALTER TABLE ONLY "webjet_cms"."enumeration_type"
    ADD CONSTRAINT "idx_81188_primary" PRIMARY KEY ("enumeration_type_id");



ALTER TABLE ONLY "webjet_cms"."export_dat"
    ADD CONSTRAINT "idx_81221_primary" PRIMARY KEY ("export_dat_id");



ALTER TABLE ONLY "webjet_cms"."file_archiv"
    ADD CONSTRAINT "idx_81228_primary" PRIMARY KEY ("file_archiv_id");



ALTER TABLE ONLY "webjet_cms"."file_archiv_category_node"
    ADD CONSTRAINT "idx_81251_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."file_atr"
    ADD CONSTRAINT "idx_81261_primary" PRIMARY KEY ("link", "atr_id");



ALTER TABLE ONLY "webjet_cms"."file_atr_def"
    ADD CONSTRAINT "idx_81272_primary" PRIMARY KEY ("atr_id");



ALTER TABLE ONLY "webjet_cms"."file_history"
    ADD CONSTRAINT "idx_81286_primary" PRIMARY KEY ("file_history_id");



ALTER TABLE ONLY "webjet_cms"."forms"
    ADD CONSTRAINT "idx_81296_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."forms_archive"
    ADD CONSTRAINT "idx_81307_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."form_attributes"
    ADD CONSTRAINT "idx_81317_primary" PRIMARY KEY ("form_name", "param_name", "domain_id");



ALTER TABLE ONLY "webjet_cms"."form_regular_exp"
    ADD CONSTRAINT "idx_81325_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."forum"
    ADD CONSTRAINT "idx_81332_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."gallery"
    ADD CONSTRAINT "idx_81346_primary" PRIMARY KEY ("image_id");



ALTER TABLE ONLY "webjet_cms"."gallery_dimension"
    ADD CONSTRAINT "idx_81369_primary" PRIMARY KEY ("dimension_id");



ALTER TABLE ONLY "webjet_cms"."gdpr_regexp"
    ADD CONSTRAINT "idx_81387_primary" PRIMARY KEY ("gdpr_regexp_id");



ALTER TABLE ONLY "webjet_cms"."gis"
    ADD CONSTRAINT "idx_81395_primary" PRIMARY KEY ("gis_id");



ALTER TABLE ONLY "webjet_cms"."groups"
    ADD CONSTRAINT "idx_81412_primary" PRIMARY KEY ("group_id");



ALTER TABLE ONLY "webjet_cms"."groups_approve"
    ADD CONSTRAINT "idx_81440_primary" PRIMARY KEY ("approve_id");



ALTER TABLE ONLY "webjet_cms"."groups_scheduler"
    ADD CONSTRAINT "idx_81447_primary" PRIMARY KEY ("schedule_id");



ALTER TABLE ONLY "webjet_cms"."inquirysimple_answers"
    ADD CONSTRAINT "idx_81487_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."inquiry_answers"
    ADD CONSTRAINT "idx_81494_primary" PRIMARY KEY ("answer_id");



ALTER TABLE ONLY "webjet_cms"."insert_script"
    ADD CONSTRAINT "idx_81511_primary" PRIMARY KEY ("insert_script_id");



ALTER TABLE ONLY "webjet_cms"."insert_script_doc"
    ADD CONSTRAINT "idx_81520_primary" PRIMARY KEY ("insert_script_doc_id");



ALTER TABLE ONLY "webjet_cms"."insert_script_gr"
    ADD CONSTRAINT "idx_81523_primary" PRIMARY KEY ("insert_script_gr_id");



ALTER TABLE ONLY "webjet_cms"."inventory"
    ADD CONSTRAINT "idx_81527_primary" PRIMARY KEY ("inventory_id");



ALTER TABLE ONLY "webjet_cms"."inventory_detail"
    ADD CONSTRAINT "idx_81537_primary" PRIMARY KEY ("inventory_detail_id");



ALTER TABLE ONLY "webjet_cms"."inventory_log"
    ADD CONSTRAINT "idx_81544_primary" PRIMARY KEY ("inventory_log_id");



ALTER TABLE ONLY "webjet_cms"."media"
    ADD CONSTRAINT "idx_81549_primary" PRIMARY KEY ("media_id");



ALTER TABLE ONLY "webjet_cms"."media_groups"
    ADD CONSTRAINT "idx_81570_primary" PRIMARY KEY ("media_group_id");



ALTER TABLE ONLY "webjet_cms"."monitoring"
    ADD CONSTRAINT "idx_81580_primary" PRIMARY KEY ("monitoring_id");



ALTER TABLE ONLY "webjet_cms"."multigroup_mapping"
    ADD CONSTRAINT "idx_81585_primary" PRIMARY KEY ("doc_id");



ALTER TABLE ONLY "webjet_cms"."passwords_history"
    ADD CONSTRAINT "idx_81590_primary" PRIMARY KEY ("passwords_history_id");



ALTER TABLE ONLY "webjet_cms"."perex_groups"
    ADD CONSTRAINT "idx_81596_primary" PRIMARY KEY ("perex_group_id");



ALTER TABLE ONLY "webjet_cms"."pkey_generator"
    ADD CONSTRAINT "idx_81606_primary" PRIMARY KEY ("name");



ALTER TABLE ONLY "webjet_cms"."proxy"
    ADD CONSTRAINT "idx_81614_primary" PRIMARY KEY ("proxy_id");



ALTER TABLE ONLY "webjet_cms"."questions_answers"
    ADD CONSTRAINT "idx_81635_primary" PRIMARY KEY ("qa_id");



ALTER TABLE ONLY "webjet_cms"."quiz"
    ADD CONSTRAINT "idx_81658_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."quiz_answers"
    ADD CONSTRAINT "idx_81664_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."quiz_questions"
    ADD CONSTRAINT "idx_81670_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."quiz_results"
    ADD CONSTRAINT "idx_81684_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."rating"
    ADD CONSTRAINT "idx_81691_primary" PRIMARY KEY ("rating_id");



ALTER TABLE ONLY "webjet_cms"."reservation"
    ADD CONSTRAINT "idx_81696_primary" PRIMARY KEY ("reservation_id");



ALTER TABLE ONLY "webjet_cms"."reservation_object"
    ADD CONSTRAINT "idx_81710_primary" PRIMARY KEY ("reservation_object_id");



ALTER TABLE ONLY "webjet_cms"."reservation_object_price"
    ADD CONSTRAINT "idx_81727_primary" PRIMARY KEY ("object_price_id");



ALTER TABLE ONLY "webjet_cms"."reservation_object_times"
    ADD CONSTRAINT "idx_81732_primary" PRIMARY KEY ("object_time_id");



ALTER TABLE ONLY "webjet_cms"."response_headers"
    ADD CONSTRAINT "idx_81737_primary" PRIMARY KEY ("response_header_id");



ALTER TABLE ONLY "webjet_cms"."restaurant_menu"
    ADD CONSTRAINT "idx_81744_primary" PRIMARY KEY ("menu_id");



ALTER TABLE ONLY "webjet_cms"."restaurant_menu_meals"
    ADD CONSTRAINT "idx_81748_primary" PRIMARY KEY ("meals_id");



ALTER TABLE ONLY "webjet_cms"."seo_bots"
    ADD CONSTRAINT "idx_81760_primary" PRIMARY KEY ("seo_bots_id");



ALTER TABLE ONLY "webjet_cms"."seo_google_position"
    ADD CONSTRAINT "idx_81766_primary" PRIMARY KEY ("seo_google_position_id");



ALTER TABLE ONLY "webjet_cms"."seo_keywords"
    ADD CONSTRAINT "idx_81771_primary" PRIMARY KEY ("seo_keyword_id");



ALTER TABLE ONLY "webjet_cms"."sita_parsed_ids"
    ADD CONSTRAINT "idx_81781_primary" PRIMARY KEY ("news_item_id");



ALTER TABLE ONLY "webjet_cms"."sms_addressbook"
    ADD CONSTRAINT "idx_81786_primary" PRIMARY KEY ("book_id");



ALTER TABLE ONLY "webjet_cms"."sms_log"
    ADD CONSTRAINT "idx_81794_primary" PRIMARY KEY ("log_id");



ALTER TABLE ONLY "webjet_cms"."stat_from"
    ADD CONSTRAINT "idx_81846_primary" PRIMARY KEY ("from_id");



ALTER TABLE ONLY "webjet_cms"."stat_views"
    ADD CONSTRAINT "idx_81953_primary" PRIMARY KEY ("view_id");



ALTER TABLE ONLY "webjet_cms"."templates"
    ADD CONSTRAINT "idx_81968_primary" PRIMARY KEY ("temp_id");



ALTER TABLE ONLY "webjet_cms"."templates_group"
    ADD CONSTRAINT "idx_81988_primary" PRIMARY KEY ("templates_group_id");



ALTER TABLE ONLY "webjet_cms"."terminologicky_slovnik"
    ADD CONSTRAINT "idx_81997_primary" PRIMARY KEY ("terminologicky_slovnik_id");



ALTER TABLE ONLY "webjet_cms"."tips_of_the_day"
    ADD CONSTRAINT "idx_82005_primary" PRIMARY KEY ("tip_id");



ALTER TABLE ONLY "webjet_cms"."todo"
    ADD CONSTRAINT "idx_82011_primary" PRIMARY KEY ("todo_id");



ALTER TABLE ONLY "webjet_cms"."url_redirect"
    ADD CONSTRAINT "idx_82020_primary" PRIMARY KEY ("url_redirect_id");



ALTER TABLE ONLY "webjet_cms"."users"
    ADD CONSTRAINT "idx_82027_primary" PRIMARY KEY ("user_id");



ALTER TABLE ONLY "webjet_cms"."users_in_perm_groups"
    ADD CONSTRAINT "idx_82076_primary" PRIMARY KEY ("user_id", "perm_group_id");



ALTER TABLE ONLY "webjet_cms"."user_alarm"
    ADD CONSTRAINT "idx_82079_primary" PRIMARY KEY ("user_id");



ALTER TABLE ONLY "webjet_cms"."user_groups"
    ADD CONSTRAINT "idx_82088_primary" PRIMARY KEY ("user_group_id");



ALTER TABLE ONLY "webjet_cms"."user_group_verify"
    ADD CONSTRAINT "idx_82100_primary" PRIMARY KEY ("verify_id");



ALTER TABLE ONLY "webjet_cms"."user_perm_groups"
    ADD CONSTRAINT "idx_82112_primary" PRIMARY KEY ("group_id");



ALTER TABLE ONLY "webjet_cms"."user_perm_groups_perms"
    ADD CONSTRAINT "idx_82121_primary" PRIMARY KEY ("perm_id");



ALTER TABLE ONLY "webjet_cms"."user_settings"
    ADD CONSTRAINT "idx_82125_primary" PRIMARY KEY ("user_settings_id");



ALTER TABLE ONLY "webjet_cms"."user_settings_admin"
    ADD CONSTRAINT "idx_82137_primary" PRIMARY KEY ("user_settings_admin_id");



ALTER TABLE ONLY "webjet_cms"."zmluvy"
    ADD CONSTRAINT "idx_82146_primary" PRIMARY KEY ("zmluvy_id");



ALTER TABLE ONLY "webjet_cms"."zmluvy_organizacia"
    ADD CONSTRAINT "idx_82180_primary" PRIMARY KEY ("zmluvy_organizacia_id");



ALTER TABLE ONLY "webjet_cms"."zmluvy_prilohy"
    ADD CONSTRAINT "idx_82191_primary" PRIMARY KEY ("zmluvy_prilohy_id");



ALTER TABLE ONLY "webjet_cms"."_adminlog_"
    ADD CONSTRAINT "idx_82200_primary" PRIMARY KEY ("log_id");



ALTER TABLE ONLY "webjet_cms"."_conf_prepared_"
    ADD CONSTRAINT "idx_82219_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."_properties_"
    ADD CONSTRAINT "idx_82243_primary" PRIMARY KEY ("id");



ALTER TABLE ONLY "webjet_cms"."stat_views_2024_2"
    ADD CONSTRAINT "stat_views_2024_2_pkey" PRIMARY KEY ("view_id");



CREATE INDEX "idx_80543_alarm_id" ON "webjet_cms"."alarm_action" USING "btree" ("alarm_id");



CREATE INDEX "idx_80572_banner_id" ON "webjet_cms"."banner_banners" USING "btree" ("banner_id");



CREATE INDEX "idx_80620_i_banner_id" ON "webjet_cms"."banner_stat_clicks" USING "btree" ("banner_id");



CREATE INDEX "idx_80620_i_insert_date" ON "webjet_cms"."banner_stat_clicks" USING "btree" ("insert_date");



CREATE INDEX "idx_80630_i_banner_id" ON "webjet_cms"."banner_stat_views" USING "btree" ("banner_id");



CREATE INDEX "idx_80630_i_insert_date" ON "webjet_cms"."banner_stat_views" USING "btree" ("insert_date");



CREATE INDEX "idx_80724_calendar_id" ON "webjet_cms"."calendar" USING "btree" ("calendar_id");



CREATE INDEX "idx_80768_type_id" ON "webjet_cms"."calendar_types" USING "btree" ("type_id");



CREATE INDEX "idx_80780_room_id" ON "webjet_cms"."chat_rooms" USING "btree" ("room_id");



CREATE UNIQUE INDEX "idx_80884_dir_url" ON "webjet_cms"."dirprop" USING "btree" ("dir_url");



CREATE INDEX "idx_80893_i_group_id" ON "webjet_cms"."documents" USING "btree" ("group_id");



CREATE INDEX "idx_80893_ix_documents_cacheable" ON "webjet_cms"."documents" USING "btree" ("cacheable");



CREATE INDEX "idx_80893_ix_documents_dae" ON "webjet_cms"."documents" USING "btree" ("disable_after_end");



CREATE INDEX "idx_80893_ix_documents_pgl1" ON "webjet_cms"."documents" USING "btree" ("root_group_l1");



CREATE INDEX "idx_80893_ix_documents_pgl2" ON "webjet_cms"."documents" USING "btree" ("root_group_l2");



CREATE INDEX "idx_80893_ix_documents_pgl3" ON "webjet_cms"."documents" USING "btree" ("root_group_l3");



CREATE INDEX "idx_80893_search" ON "webjet_cms"."documents" USING "btree" ("title", "data_asc");



CREATE INDEX "idx_80955_ix_dh_author_id" ON "webjet_cms"."documents_history" USING "btree" ("author_id");



CREATE INDEX "idx_80955_ix_dh_awaiting_approve" ON "webjet_cms"."documents_history" USING "btree" ("awaiting_approve");



CREATE INDEX "idx_80955_ix_dh_docid" ON "webjet_cms"."documents_history" USING "btree" ("doc_id");



CREATE INDEX "idx_80955_ix_documents_hist_publicable" ON "webjet_cms"."documents_history" USING "btree" ("publicable");



CREATE INDEX "idx_81021_forum_id" ON "webjet_cms"."document_forum" USING "btree" ("forum_id");



CREATE INDEX "idx_81021_i_doc_id" ON "webjet_cms"."document_forum" USING "btree" ("doc_id");



CREATE INDEX "idx_81046_doc_id" ON "webjet_cms"."doc_atr" USING "btree" ("doc_id", "atr_id");



CREATE INDEX "idx_81055_atr_id" ON "webjet_cms"."doc_atr_def" USING "btree" ("atr_id");



CREATE UNIQUE INDEX "idx_81070_doc_reactions_id_uindex" ON "webjet_cms"."doc_reactions" USING "btree" ("id");



CREATE INDEX "idx_81103_i_campain_id" ON "webjet_cms"."emails" USING "btree" ("campain_id");



CREATE INDEX "idx_81161_enumeration_data_enumeration_data_enumeration_data_id" ON "webjet_cms"."enumeration_data" USING "btree" ("parent_enumeration_data_id");



CREATE INDEX "idx_81161_enumeration_data_enumeration_type_enumeration_type_id" ON "webjet_cms"."enumeration_data" USING "btree" ("child_enumeration_type_id");



CREATE INDEX "idx_81161_enumeration_type_id_fk" ON "webjet_cms"."enumeration_data" USING "btree" ("enumeration_type_id");



CREATE INDEX "idx_81188_enumeration_type_enumeration_type_enumeration_type_id" ON "webjet_cms"."enumeration_type" USING "btree" ("child_enumeration_type_id");



CREATE INDEX "idx_81261_link" ON "webjet_cms"."file_atr" USING "btree" ("link", "atr_id");



CREATE INDEX "idx_81272_atr_id" ON "webjet_cms"."file_atr_def" USING "btree" ("atr_id");



CREATE INDEX "idx_81296_id" ON "webjet_cms"."forms" USING "btree" ("id");



CREATE INDEX "idx_81296_ix_forms_formname" ON "webjet_cms"."forms" USING "btree" ("form_name");



CREATE INDEX "idx_81307_id" ON "webjet_cms"."forms_archive" USING "btree" ("id");



CREATE INDEX "idx_81307_ix_forms_archive_formname" ON "webjet_cms"."forms_archive" USING "btree" ("form_name");



CREATE INDEX "idx_81346_i_image_path" ON "webjet_cms"."gallery" USING "btree" ("image_path");



CREATE INDEX "idx_81369_i_image_path" ON "webjet_cms"."gallery_dimension" USING "btree" ("image_path");



CREATE UNIQUE INDEX "idx_81476_question_id" ON "webjet_cms"."inquiry" USING "btree" ("question_id");



CREATE INDEX "idx_81476_question_id_2" ON "webjet_cms"."inquiry" USING "btree" ("question_id");



CREATE INDEX "idx_81487_form_id" ON "webjet_cms"."inquirysimple_answers" USING "btree" ("form_id");



CREATE INDEX "idx_81549_ix_media_docis" ON "webjet_cms"."media" USING "btree" ("media_fk_id");



CREATE INDEX "idx_81576_ix_media_id" ON "webjet_cms"."media_group_to_media" USING "btree" ("media_id");



CREATE INDEX "idx_81603_ix_perex_group_doc_id" ON "webjet_cms"."perex_group_doc" USING "btree" ("doc_id");



CREATE INDEX "idx_81603_ix_perex_group_grp_id" ON "webjet_cms"."perex_group_doc" USING "btree" ("perex_group_id");



CREATE INDEX "idx_81635_qa_id" ON "webjet_cms"."questions_answers" USING "btree" ("qa_id");



CREATE INDEX "idx_81664_quiz_id" ON "webjet_cms"."quiz_answers" USING "btree" ("quiz_id");



CREATE INDEX "idx_81664_quiz_question_id" ON "webjet_cms"."quiz_answers" USING "btree" ("quiz_question_id");



CREATE INDEX "idx_81670_quiz_id" ON "webjet_cms"."quiz_questions" USING "btree" ("quiz_id");



CREATE INDEX "idx_81684_quiz_id" ON "webjet_cms"."quiz_results" USING "btree" ("quiz_id");



CREATE INDEX "idx_81696_fk_reservation_object_id" ON "webjet_cms"."reservation" USING "btree" ("reservation_object_id");



CREATE UNIQUE INDEX "idx_81710_unique_name" ON "webjet_cms"."reservation_object" USING "btree" ("name");



CREATE INDEX "idx_81744_menu_meals_id" ON "webjet_cms"."restaurant_menu" USING "btree" ("menu_meals_id");



CREATE INDEX "idx_81786_user_id" ON "webjet_cms"."sms_addressbook" USING "btree" ("user_id");



CREATE INDEX "idx_81806_i_update" ON "webjet_cms"."stat_browser" USING "btree" ("year", "week", "browser_id", "platform", "subplatform", "group_id");



CREATE INDEX "idx_81816_i_update" ON "webjet_cms"."stat_country" USING "btree" ("year", "week", "country_code", "group_id");



CREATE INDEX "idx_81824_i_docid" ON "webjet_cms"."stat_doc" USING "btree" ("doc_id");



CREATE INDEX "idx_81824_i_update" ON "webjet_cms"."stat_doc" USING "btree" ("year", "week", "doc_id");



CREATE INDEX "idx_81835_i_update" ON "webjet_cms"."stat_error" USING "btree" ("year", "week");



CREATE UNIQUE INDEX "idx_81855_ix_sk_value" ON "webjet_cms"."stat_keys" USING "btree" ("value");



CREATE INDEX "idx_81867_i_update" ON "webjet_cms"."stat_site_days" USING "btree" ("year", "week", "group_id");



CREATE INDEX "idx_81889_i_update" ON "webjet_cms"."stat_site_hours" USING "btree" ("year", "week", "group_id");



CREATE INDEX "idx_81943_i_update" ON "webjet_cms"."stat_userlogon" USING "btree" ("user_id", "logon_time");



CREATE INDEX "idx_81943_i_userid" ON "webjet_cms"."stat_userlogon" USING "btree" ("user_id");



CREATE INDEX "idx_81953_i_docid" ON "webjet_cms"."stat_views" USING "btree" ("doc_id");



CREATE INDEX "idx_81953_i_group_id" ON "webjet_cms"."stat_views" USING "btree" ("group_id");



CREATE INDEX "idx_81953_i_last_doc_id" ON "webjet_cms"."stat_views" USING "btree" ("last_doc_id");



CREATE INDEX "idx_81953_i_last_group_id" ON "webjet_cms"."stat_views" USING "btree" ("last_group_id");



CREATE UNIQUE INDEX "idx_82005_tip_id" ON "webjet_cms"."tips_of_the_day" USING "btree" ("tip_id");



CREATE UNIQUE INDEX "idx_82027_ix_login_name" ON "webjet_cms"."users" USING "btree" ("login");



CREATE INDEX "idx_82083_user_id" ON "webjet_cms"."user_disabled_items" USING "btree" ("user_id");



CREATE INDEX "idx_82100_verify_id" ON "webjet_cms"."user_group_verify" USING "btree" ("verify_id");



CREATE INDEX "idx_82200_ix_adminlog_logtype" ON "webjet_cms"."_adminlog_" USING "btree" ("log_type");



CREATE INDEX "idx_82200_ix_adminlog_userid" ON "webjet_cms"."_adminlog_" USING "btree" ("user_id");



CREATE UNIQUE INDEX "idx_82212_name" ON "webjet_cms"."_conf_" USING "btree" ("name");



CREATE INDEX "idx_82227_id" ON "webjet_cms"."_db_" USING "btree" ("id");



CREATE UNIQUE INDEX "idx_82234_module_id" ON "webjet_cms"."_modules_" USING "btree" ("module_id");



CREATE INDEX "idx_82234_module_id_2" ON "webjet_cms"."_modules_" USING "btree" ("module_id");



CREATE UNIQUE INDEX "idx_82243_prop_key" ON "webjet_cms"."_properties_" USING "btree" ("prop_key", "lng");



CREATE INDEX "idx_82243_prop_key_2" ON "webjet_cms"."_properties_" USING "btree" ("prop_key", "lng");



CREATE INDEX "ix_yw_2024_2" ON "webjet_cms"."stat_error_2024_2" USING "btree" ("year", "week");


ALTER TABLE ONLY "webjet_cms"."enumeration_data"
    ADD CONSTRAINT "enumeration_data_enumeration_data_enumeration_data_id_fk" FOREIGN KEY ("parent_enumeration_data_id") REFERENCES "webjet_cms"."enumeration_data"("enumeration_data_id") ON UPDATE RESTRICT ON DELETE RESTRICT;



ALTER TABLE ONLY "webjet_cms"."enumeration_data"
    ADD CONSTRAINT "enumeration_data_enumeration_type_enumeration_type_id_fk_2" FOREIGN KEY ("child_enumeration_type_id") REFERENCES "webjet_cms"."enumeration_type"("enumeration_type_id");



ALTER TABLE ONLY "webjet_cms"."enumeration_type"
    ADD CONSTRAINT "enumeration_type_enumeration_type_enumeration_type_id_fk" FOREIGN KEY ("child_enumeration_type_id") REFERENCES "webjet_cms"."enumeration_type"("enumeration_type_id");



ALTER TABLE ONLY "webjet_cms"."enumeration_data"
    ADD CONSTRAINT "enumeration_type_id_fk" FOREIGN KEY ("enumeration_type_id") REFERENCES "webjet_cms"."enumeration_type"("enumeration_type_id") ON UPDATE CASCADE ON DELETE CASCADE;



ALTER TABLE ONLY "webjet_cms"."reservation"
    ADD CONSTRAINT "fk_reservation_object_id" FOREIGN KEY ("reservation_object_id") REFERENCES "webjet_cms"."reservation_object"("reservation_object_id") ON UPDATE RESTRICT ON DELETE RESTRICT;



ALTER TABLE ONLY "webjet_cms"."restaurant_menu"
    ADD CONSTRAINT "restaurant_menu_ibfk_1" FOREIGN KEY ("menu_meals_id") REFERENCES "webjet_cms"."restaurant_menu_meals"("meals_id") ON UPDATE RESTRICT ON DELETE CASCADE;


CREATE EXTENSION IF NOT EXISTS unaccent SCHEMA "webjet_cms";