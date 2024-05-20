# Konfigurácia

Pre správne fungovanie je potrebné dodržať nasledovné požiadavky:

- [Nastavenie šablón](../../frontend/setup/README.md) pre správu viacerých domén
- Prvá doméne je tzv. riadiaca, nemala by obsahovať reálnu web stránku, slúži na nastavovanie globálnych parametrov.
- Pre každú doménu odporúčame vytvoriť doménový alias. Vytvoríte ho v konfigurácii ako kľúč `multiDomainAlias:DOMAIN-NAME` s vhodnou hodnotou doménového mena bez medzier, www a prípony, napr. `interway`. Doménový alias vám umožní pripraviť web na pracovnej doméne a následne ho presunúť na produkčnú doménu. Zadaný doménový alias sa použije na hľadanie súborov šablón a upravených súborov aplikácií.

## Zriadenie novej domény

Aktuálne je potrebné novú doménu vytvoriť nižšie uvedeným SQL príkazom. Výraz `DOMAIN-NAME` nahraďte za doménové meno. Prihlasovacie meno je `Heslo1`. Po pridaní je potrebné buď cez riadiacu doménu zmazať cache v Ovládací panel->Cache objekty kliknutím na zmazať všetko, alebo reštartovať aplikačný server.

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