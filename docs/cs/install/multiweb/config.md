# Konfigurace

Pro správné fungování je třeba dodržet následující požadavky:
- [Nastavení šablon](../../frontend/setup/README.md) pro správu více domén
- První doméně je tzn. řídící, neměla by obsahovat reálnou web stránku, slouží k nastavování globálních parametrů.
- Pro každou doménu doporučujeme vytvořit doménový alias. Vytvoříte jej v konfiguraci jako klíč `multiDomainAlias:DOMAIN-NAME` s vhodnou hodnotou doménového jména bez mezer, www a přípony. `interway`. Doménový alias vám umožní připravit web na pracovní doméně a následně jej přesunout na produkční doménu. Zadaný doménový alias se použije pro hledání souborů šablon a upravených souborů aplikací.

## Zřízení nové domény

Aktuálně je třeba novou doménu vytvořit níže uvedeným SQL příkazem. Výraz `DOMAIN-NAME` nahraďte za doménové jméno. Přihlašovací jméno je `Heslo1`. Po přidání je třeba buď přes řídicí doménu smazat cache v Ovládací panely->Cache objekty kliknutím na smazat vše, nebo restartovat aplikační server.

```sql
INSERT INTO `groups` (`group_name`, `internal`, `parent_group_id`, `navbar`, `default_doc_id`, `temp_id`, `sort_priority`, `password_protected`, `menu_type`, `url_dir_name`, `sync_id`, `sync_status`, `html_head`, `logon_page_doc_id`, `domain_name`, `new_page_docid_template`, `install_name`, `field_a`, `field_b`, `field_c`, `field_d`, `logged_menu_type`, `link_group_id`)
VALUES
	('DOMAIN-NAME', 0, 0, 'DOMAIN-NAME', -1, 1, 0, '', 2, '/', 1, 1, '', -1, 'DOMAIN-NAME', -1, '', '', '', '', '', -1, -1);

INSERT INTO `groups` (`group_name`, `internal`, `parent_group_id`, `navbar`, `default_doc_id`, `temp_id`, `sort_priority`, `password_protected`, `menu_type`, `url_dir_name`, `sync_id`, `sync_status`, `html_head`, `logon_page_doc_id`, `domain_name`, `new_page_docid_template`, `install_name`, `field_a`, `field_b`, `field_c`, `field_d`, `logged_menu_type`, `link_group_id`)
VALUES
	('System', 1, 0, 'System', -1, 1, 10, NULL, 0, 'system', 0, 1, '', -1, 'DOMAIN-NAME', -1, NULL, '', '', '', '', -1, -1);

INSERT INTO `users` (`title`, `first_name`, `last_name`, `login`, `password`, `is_admin`, `user_groups`, `company`, `adress`, `city`, `email`, `PSC`, `country`, `phone`, `authorized`, `editable_groups`, `editable_pages`, `writable_folders`, `last_logon`, `module_perms`, `disabled_items`, `reg_date`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `date_of_birth`, `sex_male`, `photo`, `signature`, `forum_rank`, `rating_rank`, `allow_login_start`, `allow_login_end`, `authorize_hash`, `fax`, `delivery_first_name`, `delivery_last_name`, `delivery_company`, `delivery_adress`, `delivery_city`, `delivery_psc`, `delivery_country`, `delivery_phone`, `position`, `parent_id`, `password_salt`, `domain_id`)
VALUES
	('', 'DOMAIN-NAME', 'Admin', 'admin', 'ead38302881122ab70592e113663c475', 1, NULL, '', '', '', 'info@webjet.eu', '', '', '', 1, '', '', '', '2014-08-07 14:43:29', NULL, NULL, '2014-02-05 14:55:46', '', '', '', '', '', NULL, 1, '', '', 0, 0, NULL, NULL, NULL, '', '', '', '', '', '', '', '', '', '', 0, NULL, 230);
```
