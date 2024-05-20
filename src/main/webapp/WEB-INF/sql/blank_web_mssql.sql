CREATE TABLE [dbo].[_conf_] (
	[name] [nvarchar] (255) NOT NULL ,
	[value] [ntext] NOT NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[_db_] (
	[id] [int] IDENTITY (1, 1) NOT NULL ,
	[create_date] [datetime] NOT NULL ,
	[note] [nvarchar] (255)  NOT NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[_modules_] (
	[module_id] [int] IDENTITY (1, 1) NOT NULL ,
	[name_key] [nvarchar] (128) NOT NULL ,
	[item_key] [nvarchar] (64) NOT NULL ,
	[path] [nvarchar] (255) NOT NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[_properties_] (
	[prop_key] [nvarchar] (255) NOT NULL ,
	[lng] [nvarchar] (3) NOT NULL ,
	[prop_value] [nvarchar] (4000) NOT NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[alarm_action] (
	[alarm_id] [int] NOT NULL ,
	[days] [int] NULL ,
	[doc_id] [int] NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[banner_banners] (
	[banner_id] [int] NOT NULL ,
	[banner_type] [int] NOT NULL ,
	[banner_group] [nvarchar] (128) NULL ,
	[priority] [int] NULL ,
	[active] [bit] NOT NULL ,
	[banner_location] [nvarchar] (255) NULL ,
	[banner_redirect] [nvarchar] (255) NULL ,
	[width] [int] NULL ,
	[height] [int] NULL ,
	[html_code] [ntext] NULL ,
	[date_from] [datetime] NULL ,
	[date_to] [datetime] NULL ,
	[max_views] [int] NULL ,
	[max_clicks] [int] NULL ,
	[stat_views] [int] NULL ,
	[stat_clicks] [int] NULL ,
	[stat_date] [datetime] NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
;

CREATE TABLE [dbo].[banner_stat_clicks] (
	[id] [int] IDENTITY (1, 1) NOT NULL ,
	[banner_id] [int] NOT NULL ,
	[insert_date] [datetime] NOT NULL ,
	[ip] [nvarchar] (16) NULL ,
	[host] [nvarchar] (128) NULL ,
	[clicks] [int] NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[banner_stat_views] (
	[id] [int] IDENTITY (1, 1) NOT NULL ,
	[banner_id] [int] NOT NULL ,
	[insert_date] [datetime] NOT NULL ,
	[ip] [nvarchar] (16) NULL ,
	[host] [nvarchar] (128) NULL ,
	[views] [int] NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[calendar] (
	[calendar_id] [int] IDENTITY (1, 1) NOT NULL ,
	[title] [ntext]  NOT NULL ,
	[description] [ntext]  NULL ,
	[date_from] [datetime] NULL ,
	[date_to] [datetime] NULL ,
	[type_id] [int] NOT NULL ,
	[time_range] [nvarchar] (128)  NULL ,
	[area] [nvarchar] (255)  NULL ,
	[city] [nvarchar] (255)  NULL ,
	[address] [nvarchar] (255)  NULL ,
	[info_1] [nvarchar] (255)  NULL ,
	[info_2] [nvarchar] (255)  NULL ,
	[info_3] [nvarchar] (255)  NULL ,
	[info_4] [nvarchar] (255)  NULL ,
	[info_5] [nvarchar] (255)  NULL ,
	[notify_hours_before] [int] NULL ,
	[notify_emails] [ntext]  NULL ,
	[notify_sender] [nvarchar] (255)  NULL ,
	[notify_sent] [bit] NOT NULL ,
	[notify_introtext] [ntext]  NULL ,
	[notify_sendsms] [bit] NULL ,
	[lng] [nvarchar] (3)  NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
;

CREATE TABLE [dbo].[calendar_types] (
	[type_id] [int] IDENTITY (1, 1) NOT NULL ,
	[name] [nvarchar] (128)  NOT NULL
) ON [PRIMARY]
;

INSERT INTO calendar_types (name) VALUES('Výstava');
INSERT INTO calendar_types (name) VALUES('Šport');
INSERT INTO calendar_types (name) VALUES('Kultúra');
INSERT INTO calendar_types (name) VALUES('Rodina');
INSERT INTO calendar_types (name) VALUES('Konferencia');


CREATE TABLE [dbo].[doc_atr] (
	[doc_id] [int] NOT NULL ,
	[atr_id] [int] NOT NULL ,
	[value_string] [nvarchar] (255)  NULL ,
	[value_int] [FLOAT] NULL ,
	[value_bool] [bit] NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[doc_atr_def] (
	[atr_id] [int] IDENTITY (1, 1) NOT NULL ,
	[atr_name] [nvarchar] (255)  NOT NULL ,
	[order_priority] [int] NULL ,
	[atr_description] [nvarchar] (255)  NULL ,
	[atr_default_value] [nvarchar] (255)  NULL ,
	[atr_type] [int] NOT NULL ,
	[atr_group] [nvarchar] (32)  NULL ,
	[true_value] [nvarchar] (255)  NULL ,
	[false_value] [nvarchar] (255)  NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[document_forum] (
	[forum_id] [int] IDENTITY (1, 1) NOT NULL ,
	[doc_id] [int] NOT NULL ,
	[parent_id] [int] NOT NULL ,
	[subject] [nvarchar] (255)  NULL ,
	[question] [ntext]  NULL ,
	[question_date] [datetime] NULL ,
	[author_name] [nvarchar] (255)  NULL ,
	[author_email] [nvarchar] (255)  NULL ,
	[ip] [nvarchar] (255)  NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
;

CREATE TABLE [dbo].[documents] (
	[doc_id] [int] IDENTITY (1, 1) NOT NULL ,
	[title] [nvarchar] (255) NOT NULL ,
	[data] [ntext] NOT NULL ,
	[data_asc] [ntext] NOT NULL ,
	[external_link] [nvarchar] (255) NULL ,
	[navbar] [nvarchar] (512) NOT NULL ,
	[date_created] [datetime] NOT NULL ,
	[publish_start] [datetime] NULL ,
	[publish_end] [datetime] NULL ,
	[author_id] [int] NOT NULL ,
	[group_id] [int] NULL ,
	[temp_id] [int] NOT NULL ,
	[views_total] [int] NULL ,
	[views_month] [int] NULL ,
	[searchable] [bit] NOT NULL ,
	[available] [bit] NOT NULL ,
	[cacheable] [bit] NOT NULL ,
	[file_name] [nvarchar] (255) NULL ,
	[file_change] [datetime] NULL ,
	[sort_priority] [int] NOT NULL ,
	[header_doc_id] [int] NULL ,
	[menu_doc_id] [int] NULL ,
	[footer_doc_id] [int] NULL ,
	[password_protected] [nvarchar] (255) NULL ,
	[html_head] [ntext] NULL ,
	[html_data] [ntext] NULL ,
	[perex_place] [nvarchar] (255) NULL ,
	[perex_image] [nvarchar] (255) NULL ,
	[perex_group] [nvarchar] (255) NULL ,
	[show_in_menu] [bit] NULL ,
	[event_date] [datetime] NULL ,
	[virtual_path] [nvarchar] (255) NULL ,
	[sync_id] [int] NULL ,
	[sync_status] [int] NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
;

SET IDENTITY_INSERT documents ON
;

INSERT INTO [dbo].[documents]([doc_id], [title], [data], [data_asc], [external_link], [navbar], [date_created], [publish_start], [publish_end], [author_id], [group_id], [temp_id], [views_total], [views_month], [searchable], [available], [cacheable], [file_name], [file_change], [sort_priority], [header_doc_id], [menu_doc_id], [footer_doc_id], [password_protected], [html_head], [html_data], [perex_place], [perex_image], [perex_group], [show_in_menu], [event_date], [virtual_path])
VALUES(1, 'Default hlavička', 'Vitajte na default stránke Web JET', 'data asc', '', 'Default hlavička', '2005-01-01', NULL, NULL, 1, 4, 1, 0, 0, 0, 1, 0, '1_default_hlavicka.html', '2005-01-01', 50, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, NULL, 1, NULL, NULL)
;

INSERT INTO [dbo].[documents]([doc_id], [title], [data], [data_asc], [external_link], [navbar], [date_created], [publish_start], [publish_end], [author_id], [group_id], [temp_id], [views_total], [views_month], [searchable], [available], [cacheable], [file_name], [file_change], [sort_priority], [header_doc_id], [menu_doc_id], [footer_doc_id], [password_protected], [html_head], [html_data], [perex_place], [perex_image], [perex_group], [show_in_menu], [event_date], [virtual_path])
VALUES(2, 'Default menu', '!menu!', '!menu!', '', 'Default menu', '2005-01-01', NULL, NULL, 1, 3, 1, 0, 0, 0, 1, 0, '2_default_menu.html', '2005-01-01', 50, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, NULL, 1, NULL, NULL)
;

INSERT INTO [dbo].[documents]([doc_id], [title], [data], [data_asc], [external_link], [navbar], [date_created], [publish_start], [publish_end], [author_id], [group_id], [temp_id], [views_total], [views_month], [searchable], [available], [cacheable], [file_name], [file_change], [sort_priority], [header_doc_id], [menu_doc_id], [footer_doc_id], [password_protected], [html_head], [html_data], [perex_place], [perex_image], [perex_group], [show_in_menu], [event_date], [virtual_path])
VALUES(3, 'Default paticka', 'paticka', 'paticka', '', 'Default paticka', '2005-01-01', NULL, NULL, 1, 4, 1, 0, 0, 0, 1, 0, '3_default_paticka.html', '2005-01-01', 50, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, NULL, 1, NULL, NULL)
;

INSERT INTO [dbo].[documents]([doc_id], [title], [data], [data_asc], [external_link], [navbar], [date_created], [publish_start], [publish_end], [author_id], [group_id], [temp_id], [views_total], [views_month], [searchable], [available], [cacheable], [file_name], [file_change], [sort_priority], [header_doc_id], [menu_doc_id], [footer_doc_id], [password_protected], [html_head], [html_data], [perex_place], [perex_image], [perex_group], [show_in_menu], [event_date], [virtual_path])
VALUES(4, 'Hlavna stranka', 'Hlavna stranka', 'Hlavna stranka', '', 'Hlavna stranka', '2005-01-01', NULL, NULL, 1, 1, 1, 0, 0, 0, 1, 0, '4_hlavna_stranka.html', '2005-01-01', 50, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, NULL, 1, NULL, NULL)
;

SET IDENTITY_INSERT documents OFF
;

CREATE TABLE [dbo].[documents_history] (
	[history_id] [int] IDENTITY (1, 1) NOT NULL ,
	[save_date] [datetime] NULL ,
	[approved_by] [int] NULL ,
	[awaiting_approve] [nvarchar] (255) NULL ,
	[actual] [bit] NULL ,
	[doc_id] [int] NULL ,
	[title] [nvarchar] (255) NULL ,
	[data] [ntext] NULL ,
	[data_asc] [ntext] NULL ,
	[external_link] [nvarchar] (255) NULL ,
	[navbar] [nvarchar] (512) NULL ,
	[date_created] [datetime] NULL ,
	[publish_start] [datetime] NULL ,
	[publish_end] [datetime] NULL ,
	[author_id] [int] NULL ,
	[group_id] [int] NULL ,
	[temp_id] [int] NULL ,
	[views_total] [int] NULL ,
	[views_month] [int] NULL ,
	[searchable] [bit] NULL ,
	[available] [bit] NULL ,
	[cacheable] [bit] NULL ,
	[file_name] [nvarchar] (255) NULL ,
	[file_change] [nvarchar] (255) NULL ,
	[sort_priority] [int] NULL ,
	[header_doc_id] [int] NULL ,
	[footer_doc_id] [int] NULL ,
	[menu_doc_id] [int] NULL ,
	[password_protected] [nvarchar] (255) NULL ,
	[html_head] [ntext] NULL ,
	[html_data] [ntext] NULL ,
	[publicable] [bit] NULL ,
	[perex_place] [nvarchar] (255) NULL ,
	[perex_image] [nvarchar] (255) NULL ,
	[perex_group] [nvarchar] (255) NULL ,
	[show_in_menu] [bit] NULL ,
	[event_date] [datetime] NULL ,
	[virtual_path] [nvarchar] (255) NULL ,
	[sync_id] [int] NULL ,
	[sync_status] [int] NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
;

CREATE TABLE [dbo].[emails] (
	[email_id] [int] IDENTITY (1, 1) NOT NULL ,
	[recipient_email] [nvarchar] (128)  NOT NULL ,
	[recipient_name] [nvarchar] (128)  NULL ,
	[sender_name] [nvarchar] (128)  NULL ,
	[sender_email] [nvarchar] (128)  NOT NULL ,
	[subject] [nvarchar] (255)  NULL ,
	[url] [nvarchar] (255)  NULL ,
	[attachments] [nvarchar] (4000)  NULL ,
	[retry] [int] NOT NULL ,
	[sent_date] [datetime] NULL ,
	[created_by_user_id] [int] NOT NULL ,
	[create_date] [datetime] NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[file_atr] (
	[file_name] [nvarchar] (128)  NOT NULL ,
	[link] [nvarchar] (255)  NOT NULL ,
	[atr_id] [int] NOT NULL ,
	[value_string] [nvarchar] (255)  NULL ,
	[value_int] [int] NULL ,
	[value_bool] [bit] NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[file_atr_def] (
	[atr_id] [int] IDENTITY (1, 1) NOT NULL ,
	[atr_name] [nvarchar] (32)  NOT NULL ,
	[order_priority] [int] NULL ,
	[atr_description] [nvarchar] (255)  NULL ,
	[atr_default_value] [nvarchar] (255)  NULL ,
	[atr_type] [int] NOT NULL ,
	[atr_group] [nvarchar] (32)  NULL ,
	[true_value] [nvarchar] (255)  NULL ,
	[false_value] [nvarchar] (255)  NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[forms] (
	[id] [int] IDENTITY (1, 1) NOT NULL ,
	[form_name] [nvarchar] (255)  NOT NULL ,
	[data] [ntext]  NULL ,
	[files] [ntext]  NULL ,
	[create_date] [datetime] NULL ,
	[html] [ntext]  NULL ,
	[user_id] [int] NOT NULL ,
	[note] [ntext]  NULL ,
	[doc_id] [int] NOT NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
;

CREATE TABLE [dbo].[gallery] (
	[image_id] [int] IDENTITY (1, 1) NOT NULL ,
	[image_path] [nvarchar] (255)  NULL ,
	[s_description_sk] [nvarchar] (1000)  NULL ,
	[l_description_sk] [ntext]  NULL ,
	[image_name] [nvarchar] (255)  NULL ,
	[s_description_en] [nvarchar] (1000)  NULL ,
	[l_description_en] [ntext]  NULL ,
	[s_description_cz] [nvarchar] (1000)  NULL ,
	[l_description_cz] [ntext]  NULL ,
	[s_description_de] [nvarchar] (1000)  NULL ,
	[l_description_de] [ntext]  NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
;

CREATE TABLE [dbo].[gallery_dimension] (
	[dimension_id] [int] IDENTITY (1, 1) NOT NULL ,
	[image_path] [nvarchar] (255)  NULL ,
	[image_width] [int] NULL ,
	[image_height] [int] NULL ,
	[normal_width] [int] NOT NULL ,
	[normal_height] [int] NOT NULL
) ON [PRIMARY]
;

INSERT INTO gallery_dimension (image_path, image_width, image_height, normal_width, normal_height) VALUES('/images/gallery', 160, 120, 750, 560);

CREATE TABLE [dbo].[groups] (
	[group_id] [int] IDENTITY (1, 1) NOT NULL ,
	[group_name] [nvarchar] (255) NOT NULL ,
	[internal] [bit] NOT NULL ,
	[parent_group_id] [int] NOT NULL ,
	[navbar] [nvarchar] (512) NULL ,
	[default_doc_id] [int] NULL ,
	[temp_id] [int] NULL ,
	[sort_priority] [int] NOT NULL ,
	[password_protected] [nvarchar] (255) NULL ,
	[menu_type] [int] NULL ,
	[url_dir_name] [nvarchar] (255) NULL ,
	[sync_id] [int] NULL ,
	[sync_status] [int] NULL ,
	[html_head] [ntext] NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
;

SET IDENTITY_INSERT groups ON
;

INSERT INTO [dbo].[groups]([group_id], [group_name], [internal], [parent_group_id], [navbar], [default_doc_id], [temp_id], [sort_priority], [password_protected], [menu_type])
VALUES(1, 'Hlavná', 0, 0, 'Hlavná', 4, 1, 0, NULL, 2)
;

INSERT INTO [dbo].[groups]([group_id], [group_name], [internal], [parent_group_id], [navbar], [default_doc_id], [temp_id], [sort_priority], [password_protected], [menu_type])
VALUES(2, 'Šablóny', 1, 6, '--', 4, 1, 1500, NULL, 2)
;

INSERT INTO [dbo].[groups]([group_id], [group_name], [internal], [parent_group_id], [navbar], [default_doc_id], [temp_id], [sort_priority], [password_protected], [menu_type])
VALUES(3, 'Menu', 1, 6, '--', 4, 1, 1501, NULL, 2)
;

INSERT INTO [dbo].[groups]([group_id], [group_name], [internal], [parent_group_id], [navbar], [default_doc_id], [temp_id], [sort_priority], [password_protected], [menu_type])
VALUES(4, 'Hlavičky-pätičky', 1, 6, '--', 4, 1, 1502, NULL, 2)
;

INSERT INTO [dbo].[groups]([group_id], [group_name], [internal], [parent_group_id], [navbar], [default_doc_id], [temp_id], [sort_priority], [password_protected], [menu_type])
VALUES(5, 'English', 0, 0, '--', 4, 1, 500, NULL, 2)
;

INSERT INTO [dbo].[groups]([group_id], [group_name], [internal], [parent_group_id], [navbar], [default_doc_id], [temp_id], [sort_priority], [password_protected], [menu_type])
VALUES(6, 'System', 1, 0, 'System', 4, 1, 1000, NULL, 2)
;

SET IDENTITY_INSERT groups OFF
;

CREATE TABLE [dbo].[groups_approve] (
	[approve_id] [int] IDENTITY (1, 1) NOT NULL ,
	[group_id] [int] NULL ,
	[user_id] [int] NULL,
	[mode] [int] NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[inquiry] (
	[question_id] [int] IDENTITY (1, 1) NOT NULL ,
	[question_text] [nvarchar] (255)  NULL ,
	[hours] [int] NULL ,
	[question_group] [nvarchar] (255)  NULL ,
	[answer_text_ok] [ntext]  NULL ,
	[answer_text_fail] [ntext]  NULL,
	[date_from] [datetime] NULL ,
	[date_to] [datetime] NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
;

INSERT INTO inquiry (question_text, hours, question_group, answer_text_ok, answer_text_fail) VALUES ('Ako sa vám páči WebJET', 24, 'default', 'Ďakujeme, že ste sa zúčastnili ankety.', 'Ľutujeme, ale tejto ankety ste sa už zúčastnili.');

CREATE TABLE [dbo].[inquiry_answers] (
	[answer_id] [int] IDENTITY (1, 1) NOT NULL ,
	[question_id] [int] NOT NULL ,
	[answer_text] [nvarchar] (255)  NULL ,
	[answer_clicks] [int] NULL
) ON [PRIMARY]
;


INSERT INTO inquiry_answers (question_id, answer_text, answer_clicks) VALUES (1, 'Je super', 8);
INSERT INTO inquiry_answers (question_id, answer_text, answer_clicks) VALUES (1, 'Neviem, nepoznám', 3);

CREATE TABLE [dbo].[questions_answers] (
	[qa_id] [int] IDENTITY (1, 1) NOT NULL ,
	[group_name] [nvarchar] (255)  NOT NULL ,
	[category_name] [nvarchar] (64)  NULL ,
	[question_date] [datetime] NULL ,
	[answer_date] [datetime] NULL ,
	[question] [ntext]  NULL ,
	[answer] [ntext]  NULL ,
	[from_name] [nvarchar] (255)  NOT NULL ,
	[from_email] [nvarchar] (255)  NULL ,
	[to_name] [nvarchar] (255)  NULL ,
	[to_email] [nvarchar] (255)  NULL ,
	[publish_on_web] [bit] NULL ,
	[hash] [nvarchar] (255)  NULL ,
	[allow_publish_on_web] [bit] NOT NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
;

CREATE TABLE [dbo].[sms_addressbook] (
	[book_id] [int] IDENTITY (1, 1) NOT NULL ,
	[user_id] [int] NOT NULL ,
	[sms_name] [nvarchar] (128) NOT NULL ,
	[sms_number] [nvarchar] (32) NOT NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[sms_log] (
	[log_id] [int] IDENTITY (1, 1) NOT NULL ,
	[user_id] [int] NOT NULL ,
	[user_ip] [nvarchar] (32) NOT NULL ,
	[sent_date] [datetime] NOT NULL ,
	[sms_number] [nvarchar] (32) NOT NULL ,
	[sms_text] [nvarchar] (255) NOT NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[stat_browser] (
	[year] [int] NOT NULL ,
	[week] [int] NOT NULL ,
	[browser_id] [nvarchar] (32)  NULL ,
	[platform] [nvarchar] (25)  NULL ,
	[subplatform] [nvarchar] (20)  NULL ,
	[views] [int] NULL ,
	[group_id] [int] NOT NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[stat_country] (
	[year] [int] NOT NULL ,
	[week] [int] NOT NULL ,
	[country_code] [nvarchar] (20)  NOT NULL ,
	[views] [int] NOT NULL ,
  [group_id] [int] NOT NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[stat_day] (
	[day] [datetime] NULL ,
	[views] [int] NULL ,
	[sessions] [int] NULL ,
	[doc_id] [int] NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[stat_doc] (
	[year] [int] NULL ,
	[week] [int] NULL ,
	[doc_id] [int] NULL ,
	[views] [int] NULL ,
	[in_count] [int] NULL ,
	[out_count] [int] NULL ,
	[view_time_sum] [int] NULL ,
	[view_time_count] [int] NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[stat_error] (
	[year] [int] NULL ,
	[week] [int] NULL ,
	[url] [nvarchar] (255)  NULL ,
	[query_string] [nvarchar] (255)  NULL ,
	[count] [int] NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[stat_searchengine] (
	[search_date] [datetime] NOT NULL ,
	[server] [nvarchar] (16) NOT NULL ,
	[query] [nvarchar] (64) NOT NULL ,
	[doc_id] [int] NOT NULL ,
	[remote_host] [nvarchar] (255) NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[stat_site_days] (
	[year] [int] NULL ,
	[week] [int] NULL ,
	[views_mo] [int] NULL ,
	[sessions_mo] [int] NULL ,
	[views_tu] [int] NULL ,
	[sessions_tu] [int] NULL ,
	[views_we] [int] NULL ,
	[sessions_we] [int] NULL ,
	[views_th] [int] NULL ,
	[sessions_th] [int] NULL ,
	[views_fr] [int] NULL ,
	[sessions_fr] [int] NULL ,
	[views_sa] [int] NULL ,
	[sessions_sa] [int] NULL ,
	[views_su] [int] NULL ,
	[sessions_su] [int] NULL ,
	[view_time_sum] [int] NULL ,
	[view_time_count] [int] NULL ,
	[group_id] [int] NOT NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[stat_site_hours] (
	[year] [int] NOT NULL ,
	[week] [int] NOT NULL ,
	[views_0] [int] NULL ,
	[views_1] [int] NULL ,
	[views_2] [int] NULL ,
	[views_3] [int] NULL ,
	[views_4] [int] NULL ,
	[views_5] [int] NULL ,
	[views_6] [int] NULL ,
	[views_7] [int] NULL ,
	[views_8] [int] NULL ,
	[views_9] [int] NULL ,
	[views_10] [int] NULL ,
	[views_11] [int] NULL ,
	[views_12] [int] NULL ,
	[views_13] [int] NULL ,
	[views_14] [int] NULL ,
	[views_15] [int] NULL ,
	[views_16] [int] NULL ,
	[views_17] [int] NULL ,
	[views_18] [int] NULL ,
	[views_19] [int] NULL ,
	[views_20] [int] NULL ,
	[views_21] [int] NULL ,
	[views_22] [int] NULL ,
	[views_23] [int] NULL ,
	[sessions_0] [int] NULL ,
	[sessions_1] [int] NULL ,
	[sessions_2] [int] NULL ,
	[sessions_3] [int] NULL ,
	[sessions_4] [int] NULL ,
	[sessions_5] [int] NULL ,
	[sessions_6] [int] NULL ,
	[sessions_7] [int] NULL ,
	[sessions_8] [int] NULL ,
	[sessions_9] [int] NULL ,
	[sessions_10] [int] NULL ,
	[sessions_11] [int] NULL ,
	[sessions_12] [int] NULL ,
	[sessions_13] [int] NULL ,
	[sessions_14] [int] NULL ,
	[sessions_15] [int] NULL ,
	[sessions_16] [int] NULL ,
	[sessions_17] [int] NULL ,
	[sessions_18] [int] NULL ,
	[sessions_19] [int] NULL ,
	[sessions_20] [int] NULL ,
	[sessions_21] [int] NULL ,
	[sessions_22] [int] NULL ,
	[sessions_23] [int] NULL ,
	[group_id] [int] NOT NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[stat_userlogon] (
	[year] [int] NOT NULL ,
	[week] [int] NOT NULL ,
	[user_id] [int] NOT NULL ,
	[views] [int] NULL ,
	[logon_time] [datetime] NULL ,
	[view_minutes] [int] NULL ,
	[hostname] [nvarchar] (255)  NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[templates] (
	[temp_id] [int] IDENTITY (1, 1) NOT NULL ,
	[temp_name] [nvarchar] (64)  NOT NULL ,
	[forward] [nvarchar] (64)  NOT NULL ,
	[lng] [nvarchar] (16)  NOT NULL,
	[header_doc_id] [int] NOT NULL ,
	[footer_doc_id] [int] NOT NULL ,
	[after_body_data] [ntext]  NULL ,
	[css] [nvarchar] (64)  NULL ,
	[menu_doc_id] [int] NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
;

SET IDENTITY_INSERT templates ON
;

INSERT INTO [dbo].[templates]([temp_id], [temp_name], [forward], [lng], [header_doc_id], [footer_doc_id], [after_body_data], [css], [menu_doc_id])
VALUES(1, 'Generic', 'tmp_generic.jsp', 'sk', 1, 3, '', '', 2)
;

SET IDENTITY_INSERT templates OFF
;

CREATE TABLE [dbo].[tips_of_the_day] (
	[tip_id] [int] IDENTITY (1, 1) NOT NULL ,
	[tip_group] [nvarchar] (255) NOT NULL ,
	[tip_text] [ntext] NOT NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
;

CREATE TABLE [dbo].[user_alarm] (
	[user_id] [int] NOT NULL ,
	[alarm_id] [int] NULL ,
	[warning] [int] NULL ,
	[send_date] [datetime] NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[user_disabled_items] (
	[user_id] [int] NOT NULL ,
	[item_name] [nvarchar] (32) NOT NULL
) ON [PRIMARY]
;


INSERT INTO user_disabled_items VALUES(1, 'editorMiniEdit');

CREATE TABLE [dbo].[user_group_verify] (
	[verify_id] [int] IDENTITY (1, 1) NOT NULL ,
	[user_id] [int] NOT NULL ,
	[user_groups] [nvarchar] (255)  NOT NULL ,
	[hash] [nvarchar] (32)  NOT NULL ,
	[create_date] [datetime] NOT NULL ,
	[verify_date] [datetime] NULL ,
	[email] [nvarchar] (255)  NOT NULL
) ON [PRIMARY]
;

CREATE TABLE [dbo].[user_groups] (
	[user_group_id] [int] IDENTITY (1, 1) NOT NULL ,
	[user_group_name] [nvarchar] (255)  NOT NULL ,
	[user_group_type] [int] NOT NULL ,
	[user_group_comment] [ntext]  NULL ,
	[require_approve] [bit] NOT NULL ,
	[email_doc_id] [int] NOT NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
;

INSERT INTO [dbo].[user_groups]([user_group_name], [user_group_type], [user_group_comment], [require_approve], [email_doc_id])
VALUES('Klienti', 0, '', 0, -1)
;

CREATE TABLE [dbo].[users] (
	[user_id] [int] IDENTITY (1, 1) NOT NULL ,
	[title] [nvarchar] (16) NULL ,
	[first_name] [nvarchar] (128) NULL ,
	[last_name] [nvarchar] (255) NULL ,
	[login] [nvarchar] (16)  NOT NULL ,
	[password] [nvarchar] (128)  NOT NULL ,
	[is_admin] [bit] NOT NULL ,
	[user_groups] [nvarchar] (255)  NULL ,
	[company] [nvarchar] (255)  NULL ,
	[adress] [nvarchar] (255)  NULL ,
	[city] [nvarchar] (255)  NULL ,
	[email] [nvarchar] (255)  NULL ,
	[PSC] [nvarchar] (20)  NULL ,
	[country] [nvarchar] (255)  NULL ,
	[phone] [nvarchar] (255)  NULL ,
	[authorized] [bit] NULL ,
	[editable_groups] [nvarchar] (512)  NULL ,
	[editable_pages] [nvarchar] (255)  NULL ,
	[writable_folders] [ntext]  NULL ,
	[last_logon] [datetime] NULL ,
	[module_perms] [nvarchar] (255)  NULL ,
	[disabled_items] [nvarchar] (255)  NULL ,
	[reg_date] [datetime] NULL,
	[field_a] [nvarchar] (255) NULL ,
	[field_b] [nvarchar] (255) NULL ,
	[field_c] [nvarchar] (255) NULL ,
	[field_d] [nvarchar] (255) NULL ,
	[field_e] [nvarchar] (255) NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
;

SET IDENTITY_INSERT users ON
;

INSERT INTO [dbo].[users]([user_id], title, first_name, last_name, [login], [password], [is_admin], [user_groups], [company], [adress], [city], [email], [PSC], [country], [phone], [authorized], [editable_groups], [editable_pages], [writable_folders], [last_logon], [module_perms], [disabled_items], [reg_date])
VALUES(1, '', 'WebJET', 'Administrátor', 'admin', 'd7ed8dc6fc9b4a8c3b442c3dcc35bfe4', 1, '4,3', 'InterWay, a. s.', '', '', 'web.spam@interway.sk', '', '', '02/32788888', 1, '', '', '', NULL, NULL, NULL, NULL)
;

SET IDENTITY_INSERT users OFF
;

ALTER TABLE [dbo].[_db_] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[_modules_] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[module_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[banner_banners] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[banner_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[banner_stat_clicks] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[banner_stat_views] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[calendar_types] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[type_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[doc_atr] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[doc_id],
		[atr_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[doc_atr_def] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[atr_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[document_forum] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[forum_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[documents] ADD
	CONSTRAINT [PK_documents] PRIMARY KEY  CLUSTERED
	(
		[doc_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[documents_history] ADD
	CONSTRAINT [PK_documents_history] PRIMARY KEY  CLUSTERED
	(
		[history_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[emails] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[email_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[file_atr] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[link],
		[atr_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[file_atr_def] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[atr_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[gallery] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[image_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[gallery_dimension] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[dimension_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[inquiry_answers] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[answer_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[questions_answers] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[qa_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[sms_addressbook] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[book_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[sms_log] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[log_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[tips_of_the_day] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[tip_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[user_alarm] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[user_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[user_group_verify] ADD
	 PRIMARY KEY  CLUSTERED
	(
		[verify_id]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[_conf_] ADD
	 UNIQUE  NONCLUSTERED
	(
		[name]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[_properties_] ADD
	 UNIQUE  NONCLUSTERED
	(
		[prop_key],
		[lng]
	)  ON [PRIMARY]
;

ALTER TABLE [dbo].[calendar] ADD
	CONSTRAINT [DF__calendar__type_i__6B24EA82] DEFAULT (0) FOR [type_id],
	CONSTRAINT [DF__calendar__notify__0D7A0286] DEFAULT (0) FOR [notify_hours_before],
	CONSTRAINT [DF__calendar__notify__0E6E26BF] DEFAULT (0) FOR [notify_sent],
	CONSTRAINT [DF__calendar__notify__0F624AF8] DEFAULT (0) FOR [notify_sendsms]
;

ALTER TABLE [dbo].[calendar_types] ADD
	CONSTRAINT [DF__calendar_t__name__6A30C649] DEFAULT ('') FOR [name]
;

ALTER TABLE [dbo].[doc_atr] ADD
	CONSTRAINT [DF__doc_atr__value_b__778AC167] DEFAULT (0) FOR [value_bool]
;

ALTER TABLE [dbo].[doc_atr_def] ADD
	CONSTRAINT [DF__doc_atr_d__order__72C60C4A] DEFAULT (10) FOR [order_priority],
	CONSTRAINT [DF__doc_atr_d__atr_t__73BA3083] DEFAULT (0) FOR [atr_type],
	CONSTRAINT [DF__doc_atr_d__atr_g__74AE54BC] DEFAULT ('default') FOR [atr_group]
;

ALTER TABLE [dbo].[document_forum] ADD
	CONSTRAINT [DF__document___paren__7A672E12] DEFAULT ((-1)) FOR [parent_id]
;

ALTER TABLE [dbo].[documents] ADD
	CONSTRAINT [DF_documents_views_total] DEFAULT (0) FOR [views_total],
	CONSTRAINT [DF_documents_views_month] DEFAULT (0) FOR [views_month],
	CONSTRAINT [DF__documents__show___6C190EBB] DEFAULT (1) FOR [show_in_menu],
	CONSTRAINT [DF__documents__sync___0E6E26BF] DEFAULT (0) FOR [sync_id],
	CONSTRAINT [DF__documents__sync___0F624AF8] DEFAULT (0) FOR [sync_status]
;

 CREATE  INDEX [IX_documents] ON [dbo].[documents]([group_id]) ON [PRIMARY]
;

ALTER TABLE [dbo].[documents_history] ADD
	CONSTRAINT [DF__documents__publi__4BAC3F29] DEFAULT (0) FOR [publicable],
	CONSTRAINT [DF__documents__show___6D0D32F4] DEFAULT (1) FOR [show_in_menu],
	CONSTRAINT [DF__documents__sync___10566F31] DEFAULT (0) FOR [sync_id],
	CONSTRAINT [DF__documents__sync___114A936A] DEFAULT (0) FOR [sync_status]
;

 CREATE  INDEX [IX_documents_history] ON [dbo].[documents_history]([doc_id]) ON [PRIMARY]
;

ALTER TABLE [dbo].[emails] ADD
	CONSTRAINT [DF__emails__recipien__5FB337D6] DEFAULT ('0') FOR [recipient_email],
	CONSTRAINT [DF__emails__sender_t__60A75C0F] DEFAULT ('0') FOR [sender_name],
	CONSTRAINT [DF__emails__sender__619B8048] DEFAULT ('0') FOR [sender_email],
	CONSTRAINT [DF__emails__retry__628FA481] DEFAULT ('0') FOR [retry],
	CONSTRAINT [DF__emails__created___09A971A2] DEFAULT (0) FOR [created_by_user_id]
;

ALTER TABLE [dbo].[file_atr] ADD
	CONSTRAINT [DF__file_atr__value___06CD04F7] DEFAULT (0) FOR [value_bool]
;

ALTER TABLE [dbo].[file_atr_def] ADD
	CONSTRAINT [DF__file_atr___order__02084FDA] DEFAULT (10) FOR [order_priority],
	CONSTRAINT [DF__file_atr___atr_t__02FC7413] DEFAULT (0) FOR [atr_type],
	CONSTRAINT [DF__file_atr___atr_g__03F0984C] DEFAULT ('default') FOR [atr_group]
;

ALTER TABLE [dbo].[forms] ADD
	CONSTRAINT [DF__forms__user_id__7E37BEF6] DEFAULT ((-1)) FOR [user_id],
	CONSTRAINT [DF__forms__doc_id__07C12930] DEFAULT ((-1)) FOR [doc_id]
;

ALTER TABLE [dbo].[gallery_dimension] ADD
	CONSTRAINT [DF__gallery_d__image__5AEE82B9] DEFAULT ('0') FOR [image_path],
	CONSTRAINT [DF__gallery_d__image__5BE2A6F2] DEFAULT ('0') FOR [image_width],
	CONSTRAINT [DF__gallery_d__image__5CD6CB2B] DEFAULT ('0') FOR [image_height],
	CONSTRAINT [DF__gallery_d__norma__6E01572D] DEFAULT (0) FOR [normal_width],
	CONSTRAINT [DF__gallery_d__norma__6EF57B66] DEFAULT (0) FOR [normal_height]
;

ALTER TABLE [dbo].[groups] ADD
	CONSTRAINT [DF__groups__menu_typ__7F2BE32F] DEFAULT (1) FOR [menu_type],
	CONSTRAINT [DF__groups__sync_id__0C85DE4D] DEFAULT (0) FOR [sync_id],
	CONSTRAINT [DF__groups__sync_sta__0D7A0286] DEFAULT (0) FOR [sync_status]
;

ALTER TABLE [dbo].[groups_approve] ADD
	CONSTRAINT [DF__groups_app__mode__06CD04F7] DEFAULT (0) FOR [mode]
;

ALTER TABLE [dbo].[inquiry] ADD
	CONSTRAINT [DF__inquiry__hours__4D94879B] DEFAULT (0) FOR [hours]
;

ALTER TABLE [dbo].[inquiry_answers] ADD
	CONSTRAINT [DF__inquiry_a__quest__5070F446] DEFAULT (0) FOR [question_id],
	CONSTRAINT [DF__inquiry_a__answe__5165187F] DEFAULT ('0') FOR [answer_clicks]
;

ALTER TABLE [dbo].[questions_answers] ADD
	CONSTRAINT [DF__questions__group__5441852A] DEFAULT ('') FOR [group_name],
	CONSTRAINT [DF__questions__from___5535A963] DEFAULT ('') FOR [from_name],
	CONSTRAINT [DF__questions__publi__5629CD9C] DEFAULT (0) FOR [publish_on_web],
	CONSTRAINT [DF__questions__allow__10566F31] DEFAULT (1) FOR [allow_publish_on_web]
;

ALTER TABLE [dbo].[stat_browser] ADD
	CONSTRAINT [DF_stat_browser_views] DEFAULT (0) FOR [views],
	CONSTRAINT [DF__stat_brow__group__7A672E12] DEFAULT (1) FOR [group_id]
;

ALTER TABLE [dbo].[stat_country] ADD
	CONSTRAINT [DF_stat_country_views] DEFAULT (0) FOR [views],
	CONSTRAINT [DF__stat_coun__group__778AC167] DEFAULT (1) FOR [group_id]
;

ALTER TABLE [dbo].[stat_doc] ADD
	CONSTRAINT [DF_stat_doc_views] DEFAULT (1) FOR [views],
	CONSTRAINT [DF_stat_doc_in_count] DEFAULT (0) FOR [in_count],
	CONSTRAINT [DF_stat_doc_out_count] DEFAULT (0) FOR [out_count],
	CONSTRAINT [DF_stat_doc_view_time_sum] DEFAULT (0) FOR [view_time_sum],
	CONSTRAINT [DF_stat_doc_view_time_count] DEFAULT (0) FOR [view_time_count]
;

ALTER TABLE [dbo].[stat_error] ADD
	CONSTRAINT [DF_stat_error_count] DEFAULT (0) FOR [count]
;

ALTER TABLE [dbo].[stat_site_days] ADD
	CONSTRAINT [DF_stat_site_days_year] DEFAULT (0) FOR [year],
	CONSTRAINT [DF_stat_site_days_week] DEFAULT (0) FOR [week],
	CONSTRAINT [DF_stat_site_days_views_mo] DEFAULT (0) FOR [views_mo],
	CONSTRAINT [DF_stat_site_days_sessions_mo] DEFAULT (0) FOR [sessions_mo],
	CONSTRAINT [DF_stat_site_days_views_tu] DEFAULT (0) FOR [views_tu],
	CONSTRAINT [DF_stat_site_days_sessions_tu] DEFAULT (0) FOR [sessions_tu],
	CONSTRAINT [DF_stat_site_days_views_we] DEFAULT (0) FOR [views_we],
	CONSTRAINT [DF_stat_site_days_sessions_we] DEFAULT (0) FOR [sessions_we],
	CONSTRAINT [DF_stat_site_days_views_th] DEFAULT (0) FOR [views_th],
	CONSTRAINT [DF_stat_site_days_sessions_th] DEFAULT (0) FOR [sessions_th],
	CONSTRAINT [DF_stat_site_days_views_fr] DEFAULT (0) FOR [views_fr],
	CONSTRAINT [DF_stat_site_days_sessions_fr] DEFAULT (0) FOR [sessions_fr],
	CONSTRAINT [DF_stat_site_days_views_sa] DEFAULT (0) FOR [views_sa],
	CONSTRAINT [DF_stat_site_days_sessions_sa] DEFAULT (0) FOR [sessions_sa],
	CONSTRAINT [DF_stat_site_days_views_su] DEFAULT (0) FOR [views_su],
	CONSTRAINT [DF_stat_site_days_sessions_su] DEFAULT (0) FOR [sessions_su],
	CONSTRAINT [DF_stat_site_days_view_time_sum] DEFAULT (0) FOR [view_time_sum],
	CONSTRAINT [DF_stat_site_days_view_time_count] DEFAULT (0) FOR [view_time_count],
	CONSTRAINT [DF__stat_site__group__787EE5A0] DEFAULT (1) FOR [group_id]
;

ALTER TABLE [dbo].[stat_site_hours] ADD
	CONSTRAINT [DF_stat_site_hours_year] DEFAULT (0) FOR [year],
	CONSTRAINT [DF_stat_site_hours_week] DEFAULT (0) FOR [week],
	CONSTRAINT [DF_stat_site_hours_views_0] DEFAULT (0) FOR [views_0],
	CONSTRAINT [DF_stat_site_hours_views_1] DEFAULT (0) FOR [views_1],
	CONSTRAINT [DF_stat_site_hours_views_2] DEFAULT (0) FOR [views_2],
	CONSTRAINT [DF_stat_site_hours_views_3] DEFAULT (0) FOR [views_3],
	CONSTRAINT [DF_stat_site_hours_views_4] DEFAULT (0) FOR [views_4],
	CONSTRAINT [DF_stat_site_hours_views_5] DEFAULT (0) FOR [views_5],
	CONSTRAINT [DF_stat_site_hours_views_6] DEFAULT (0) FOR [views_6],
	CONSTRAINT [DF_stat_site_hours_views_7] DEFAULT (0) FOR [views_7],
	CONSTRAINT [DF_stat_site_hours_views_8] DEFAULT (0) FOR [views_8],
	CONSTRAINT [DF_stat_site_hours_views_9] DEFAULT (0) FOR [views_9],
	CONSTRAINT [DF_stat_site_hours_views_10] DEFAULT (0) FOR [views_10],
	CONSTRAINT [DF_stat_site_hours_views_11] DEFAULT (0) FOR [views_11],
	CONSTRAINT [DF_stat_site_hours_views_12] DEFAULT (0) FOR [views_12],
	CONSTRAINT [DF_stat_site_hours_views_13] DEFAULT (0) FOR [views_13],
	CONSTRAINT [DF_stat_site_hours_views_14] DEFAULT (0) FOR [views_14],
	CONSTRAINT [DF_stat_site_hours_views_15] DEFAULT (0) FOR [views_15],
	CONSTRAINT [DF_stat_site_hours_views_16] DEFAULT (0) FOR [views_16],
	CONSTRAINT [DF_stat_site_hours_views_17] DEFAULT (0) FOR [views_17],
	CONSTRAINT [DF_stat_site_hours_views_18] DEFAULT (0) FOR [views_18],
	CONSTRAINT [DF_stat_site_hours_views_19] DEFAULT (0) FOR [views_19],
	CONSTRAINT [DF_stat_site_hours_views_20] DEFAULT (0) FOR [views_20],
	CONSTRAINT [DF_stat_site_hours_views_21] DEFAULT (0) FOR [views_21],
	CONSTRAINT [DF_stat_site_hours_views_22] DEFAULT (0) FOR [views_22],
	CONSTRAINT [DF_stat_site_hours_views_23] DEFAULT (0) FOR [views_23],
	CONSTRAINT [DF_stat_site_hours_sessions_0] DEFAULT (0) FOR [sessions_0],
	CONSTRAINT [DF_stat_site_hours_sessions_1] DEFAULT (0) FOR [sessions_1],
	CONSTRAINT [DF_stat_site_hours_sessions_2] DEFAULT (0) FOR [sessions_2],
	CONSTRAINT [DF_stat_site_hours_sessions_3] DEFAULT (0) FOR [sessions_3],
	CONSTRAINT [DF_stat_site_hours_sessions_4] DEFAULT (0) FOR [sessions_4],
	CONSTRAINT [DF_stat_site_hours_sessions_5] DEFAULT (0) FOR [sessions_5],
	CONSTRAINT [DF_stat_site_hours_sessions_6] DEFAULT (0) FOR [sessions_6],
	CONSTRAINT [DF_stat_site_hours_sessions_7] DEFAULT (0) FOR [sessions_7],
	CONSTRAINT [DF_stat_site_hours_sessions_8] DEFAULT (0) FOR [sessions_8],
	CONSTRAINT [DF_stat_site_hours_sessions_9] DEFAULT (0) FOR [sessions_9],
	CONSTRAINT [DF_stat_site_hours_sessions_10] DEFAULT (0) FOR [sessions_10],
	CONSTRAINT [DF_stat_site_hours_sessions_11] DEFAULT (0) FOR [sessions_11],
	CONSTRAINT [DF_stat_site_hours_sessions_12] DEFAULT (0) FOR [sessions_12],
	CONSTRAINT [DF_stat_site_hours_sessions_13] DEFAULT (0) FOR [sessions_13],
	CONSTRAINT [DF_stat_site_hours_sessions_14] DEFAULT (0) FOR [sessions_14],
	CONSTRAINT [DF_stat_site_hours_sessions_15] DEFAULT (0) FOR [sessions_15],
	CONSTRAINT [DF_stat_site_hours_sessions_16] DEFAULT (0) FOR [sessions_16],
	CONSTRAINT [DF_stat_site_hours_sessions_17] DEFAULT (0) FOR [sessions_17],
	CONSTRAINT [DF_stat_site_hours_sessions_18] DEFAULT (0) FOR [sessions_18],
	CONSTRAINT [DF_stat_site_hours_sessions_19] DEFAULT (0) FOR [sessions_19],
	CONSTRAINT [DF_stat_site_hours_sessions_20] DEFAULT (0) FOR [sessions_20],
	CONSTRAINT [DF_stat_site_hours_sessions_21] DEFAULT (0) FOR [sessions_21],
	CONSTRAINT [DF_stat_site_hours_sessions_22] DEFAULT (0) FOR [sessions_22],
	CONSTRAINT [DF_stat_site_hours_sessions_23] DEFAULT (0) FOR [sessions_23],
	CONSTRAINT [DF__stat_site__group__797309D9] DEFAULT (1) FOR [group_id]
;

ALTER TABLE [dbo].[stat_userlogon] ADD
	CONSTRAINT [DF__stat_userl__year__6477ECF3] DEFAULT (0) FOR [year],
	CONSTRAINT [DF__stat_userl__week__656C112C] DEFAULT (0) FOR [week],
	CONSTRAINT [DF__stat_user__user___66603565] DEFAULT (0) FOR [user_id],
	CONSTRAINT [DF__stat_user__views__6754599E] DEFAULT (1) FOR [views],
	CONSTRAINT [DF__stat_user__view___6FE99F9F] DEFAULT (0) FOR [view_minutes]
;

ALTER TABLE [dbo].[templates] ADD
	CONSTRAINT [DF__templates__lng__7B5B524B] DEFAULT ('sk') FOR [lng]
;

 CREATE  INDEX [IX_user_disabled_items] ON [dbo].[user_disabled_items]([user_id]) ON [PRIMARY]
;

ALTER TABLE [dbo].[user_group_verify] ADD
	CONSTRAINT [DF__user_grou__user___0C85DE4D] DEFAULT (0) FOR [user_id]
;

ALTER TABLE [dbo].[user_groups] ADD
	CONSTRAINT [DF__user_grou__user___08B54D69] DEFAULT (0) FOR [user_group_type],
	CONSTRAINT [DF__user_grou__requi__607251E5] DEFAULT (0) FOR [require_approve],
	CONSTRAINT [DF__user_grou__email__6166761E] DEFAULT ((-1)) FOR [email_doc_id]
;

ALTER TABLE [dbo].[users] ADD
	CONSTRAINT [DF_users_authorized] DEFAULT (1) FOR [authorized]
;

if (select DATABASEPROPERTY(DB_NAME(), N'IsFullTextEnabled')) <> 1
exec sp_fulltext_database N'enable'

;

if not exists (select * from dbo.sysfulltextcatalogs where name = N'ft_documents')
exec sp_fulltext_catalog N'ft_documents', N'create'

;

exec sp_fulltext_table N'[dbo].[documents]', N'create', N'ft_documents', N'PK_documents'
;

exec sp_fulltext_column N'[dbo].[documents]', N'title', N'add', 1033
;

exec sp_fulltext_column N'[dbo].[documents]', N'data_asc', N'add', 1033
;

exec sp_fulltext_table N'[dbo].[documents]', N'activate'
;

insert into _db_ (create_date, note) values ('2005-1-1', 'zvacsenie poli')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'konfiguracia webjetu (namiesto web.xml)')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'disabled items pouzivatelov')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'Konverzia pristupovych prav')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'rozdelenie full name na meno a priezvisko')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'Rozdelenie celeho mena na titul, meno, priezvisko')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'volne pouzitelne polozky')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'implemetacia rozdelenia full name')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'url nazov adresara')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'custom zmena textov v properties suboroch')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'uprava statistik (eviduje sa id adresara)')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'statistika query vo vyhladavacoch')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'mod schvalovania adresara (0=approve, 1=notify, 2=none)')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'id a stav synchronizacie (status: 0=novy, 1=updated, 2=synchronized)')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'konfiguracia custom modulov')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'modul posielania SMS sprav')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'vyzadovanie schvalovania registracie, doc_id pre zasielany email')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'priprava synchronizacie')
;

insert into _db_ (create_date, note) values ('2005-1-1', '18.5.2004 [jeeff] vo vyhladavani statistiky sa eviduje remote host')
;

insert into _db_ (create_date, note) values ('2005-1-1', '24.5.2004 [jeeff] tabulka s tipmi dna')
;

insert into _db_ (create_date, note) values ('2005-1-1', '9.6.2004 [joruz] zoznam alarmov pre notifikaciu registracie')
;

insert into _db_ (create_date, note) values ('2005-1-1', '9.6.2004 [joruz] alarm pouzivatela pre notifikaciu registracie')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'nastavenie reg date pouzivatelov')
;

insert into _db_ (create_date, note) values ('2005-1-1', 'zmena hodnoty casu statistiky')
;

insert into _db_ (create_date, note) values ('2005-1-1', '9.8.2004 [jeeff] html kod do hlavicky pre adresar')
;

insert into _db_ (create_date, note) values ('2005-1-1', '10.8.2004 [jeeff] banner system - banner_banners')
;

insert into _db_ (create_date, note) values ('2005-1-1', '10.8.2004 [jeeff] banner system - banner_stat_clicks')
;

insert into _db_ (create_date, note) values ('2005-1-1', '10.8.2004 [jeeff] banner system - banner_stat_views')
;

insert into _db_ (create_date, note) values ('2005-1-1', '18.8.2004 [joruz] casovanie ankiet-zaciatok')
;

insert into _db_ (create_date, note) values ('2005-1-1', '18.8.2004 [joruz] casovanie ankiet-koniec')
;

insert into _db_ (create_date, note) values ('2005-1-1', '31.3.2005 [jeeff] banner_banners - zrusenie identity stlpcov')
;

INSERT INTO _conf_ VALUES ('defaultDisableUpload','false');
INSERT INTO _conf_ VALUES ('showDocActionAllowedDocids','4');
INSERT INTO _conf_ VALUES ('inlineEditingEnabled','true');
INSERT INTO _conf_ VALUES ('disableWebJETToolbar','true');
INSERT INTO _conf_ VALUES ('logLevel','debug');
