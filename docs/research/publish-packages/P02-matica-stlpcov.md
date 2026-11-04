# P02 — matica stĺpcov a kanonické mapovanie

Dátum: 4. november 2026. Kontrakt P02 pre MariaDB 12.2.2/InnoDB. Overené zdroje: DocBasic/DocDetails, GroupsDB.fillGroupDetails, Media a aktuálna schéma P01 po startup migráciách. Kód matice je `BranchFields`; `verifyColumns` kontroluje presnú množinu fyzických stĺpcov.

WORK vstupuje do snapshotu a konfliktu celej položky. ODVODENÉ sa prepočíta z účinného výsledku. PREVÁDZKOVÉ a MIMO ROZSAHU sa pri existujúcom objekte čítajú z aktuálneho LIVE; pri novom objekte z uvedených počiatočných hodnôt. Identita je nemenná hodnota mimo editovateľného obsahu. Žiadny mapper nemení produkčný bean ani cache.

Počiatočné hodnoty pripravujú riadok pred redakčnou validáciou. Názov, referencie, URL a odvodené údaje doplní save tok P04/P05. Výber šablóny nesmie zdediť individuálny plán.

## Dokumenty

77 fyzických stĺpcov.

| Stĺpec | Kategória | Nový objekt / zdroj |
| --- | --- | --- |
| `doc_id` | MIMO ROZSAHU | rezervované ID |
| `title` | WORK | "" |
| `data` | WORK | "" |
| `data_asc` | ODVODENÉ | ""; pred použitím odvodiť z výsledných vstupov |
| `external_link` | WORK | NULL |
| `navbar` | WORK | "" |
| `date_created` | WORK | čas vytvorenia pracovnej stránky |
| `publish_start` | MIMO ROZSAHU | čas vytvorenia pri editorAutoFillPublishStart, inak NULL |
| `publish_end` | MIMO ROZSAHU | NULL |
| `author_id` | WORK | autor pracovnej úpravy |
| `group_id` | WORK | 0 |
| `temp_id` | WORK | 0 |
| `views_total` | PREVÁDZKOVÉ | 0 |
| `views_month` | PREVÁDZKOVÉ | 0 |
| `searchable` | WORK | false |
| `available` | WORK | false |
| `cacheable` | WORK | false |
| `file_name` | ODVODENÉ | NULL; pred použitím odvodiť z výsledných vstupov |
| `file_change` | PREVÁDZKOVÉ | NULL |
| `sort_priority` | WORK | 0 |
| `header_doc_id` | WORK | -1 |
| `menu_doc_id` | WORK | -1 |
| `footer_doc_id` | WORK | -1 |
| `password_protected` | WORK | "" |
| `html_head` | WORK | NULL |
| `html_data` | WORK | NULL |
| `perex_place` | WORK | "" |
| `perex_image` | WORK | "" |
| `perex_group` | WORK | "" |
| `show_in_menu` | WORK | false |
| `event_date` | WORK | NULL |
| `virtual_path` | WORK | "" |
| `sync_id` | MIMO ROZSAHU | 0 |
| `sync_status` | MIMO ROZSAHU | 0 |
| `logon_page_doc_id` | WORK | -1 |
| `right_menu_doc_id` | WORK | -1 |
| `field_a` | WORK | NULL |
| `field_b` | WORK | NULL |
| `field_c` | WORK | NULL |
| `field_d` | WORK | NULL |
| `field_e` | WORK | NULL |
| `field_f` | WORK | NULL |
| `field_g` | WORK | NULL |
| `field_h` | WORK | NULL |
| `field_i` | WORK | NULL |
| `field_j` | WORK | NULL |
| `field_k` | WORK | NULL |
| `field_l` | WORK | NULL |
| `disable_after_end` | MIMO ROZSAHU | false |
| `forum_count` | PREVÁDZKOVÉ | 0 |
| `field_m` | WORK | NULL |
| `field_n` | WORK | NULL |
| `field_o` | WORK | NULL |
| `field_p` | WORK | NULL |
| `field_q` | WORK | NULL |
| `field_r` | WORK | NULL |
| `field_s` | WORK | NULL |
| `field_t` | WORK | NULL |
| `require_ssl` | WORK | false |
| `root_group_l1` | ODVODENÉ | NULL; pred použitím odvodiť z výsledných vstupov |
| `root_group_l2` | ODVODENÉ | NULL; pred použitím odvodiť z výsledných vstupov |
| `root_group_l3` | ODVODENÉ | NULL; pred použitím odvodiť z výsledných vstupov |
| `temp_field_a_docid` | WORK | -1 |
| `temp_field_b_docid` | WORK | -1 |
| `temp_field_c_docid` | WORK | -1 |
| `temp_field_d_docid` | WORK | -1 |
| `show_in_navbar` | WORK | NULL |
| `show_in_sitemap` | WORK | NULL |
| `logged_show_in_menu` | WORK | NULL |
| `logged_show_in_navbar` | WORK | NULL |
| `logged_show_in_sitemap` | WORK | NULL |
| `url_inherit_group` | WORK | false |
| `generate_url_from_title` | WORK | false |
| `editor_virtual_path` | WORK | "" |
| `publish_after_start` | MIMO ROZSAHU | false |
| `planned_media_initialized` | PREVÁDZKOVÉ | false |
| `follow_links` | WORK | 0 |

## Priečinky

31 fyzických stĺpcov.

| Stĺpec | Kategória | Nový objekt / zdroj |
| --- | --- | --- |
| `group_id` | MIMO ROZSAHU | rezervované ID |
| `group_name` | WORK | "" |
| `internal` | WORK | false |
| `parent_group_id` | WORK | 0 |
| `navbar` | WORK | "" |
| `default_doc_id` | WORK | 0 |
| `temp_id` | WORK | 0 |
| `sort_priority` | WORK | 0 |
| `password_protected` | WORK | "" |
| `menu_type` | WORK | 1 |
| `url_dir_name` | WORK | "" |
| `sync_id` | MIMO ROZSAHU | 0 |
| `sync_status` | MIMO ROZSAHU | 0 |
| `html_head` | WORK | "" |
| `logon_page_doc_id` | WORK | -1 |
| `domain_name` | MIMO ROZSAHU | overená doména vetvy |
| `new_page_docid_template` | WORK | 0 |
| `install_name` | MIMO ROZSAHU | NULL |
| `field_a` | WORK | "" |
| `field_b` | WORK | "" |
| `field_c` | WORK | "" |
| `field_d` | WORK | "" |
| `logged_menu_type` | WORK | -1 |
| `link_group_id` | WORK | -1 |
| `lng` | WORK | NULL |
| `hidden_in_admin` | WORK | false |
| `force_group_template` | WORK | false |
| `show_in_navbar` | WORK | NULL |
| `show_in_sitemap` | WORK | NULL |
| `logged_show_in_navbar` | WORK | NULL |
| `logged_show_in_sitemap` | WORK | NULL |

## Médiá

23 fyzických stĺpcov.

| Stĺpec | Kategória | Nový objekt / zdroj |
| --- | --- | --- |
| `media_id` | MIMO ROZSAHU | rezervované ID |
| `media_fk_id` | WORK | stabilné ID vlastníckej stránky |
| `media_fk_table_name` | WORK | "documents" |
| `media_title_sk` | WORK | NULL |
| `media_title_cz` | WORK | NULL |
| `media_title_de` | WORK | NULL |
| `media_title_en` | WORK | NULL |
| `media_link` | WORK | NULL |
| `media_thumb_link` | WORK | NULL |
| `media_group` | WORK | NULL |
| `media_info_sk` | WORK | NULL |
| `media_info_cz` | WORK | NULL |
| `media_info_de` | WORK | NULL |
| `media_info_en` | WORK | NULL |
| `media_sort_order` | WORK | 10 |
| `last_update` | ODVODENÉ | NULL; pred použitím odvodiť z výsledných vstupov |
| `domain_id` | MIMO ROZSAHU | serverový tenant ID, nie hranica domény webu |
| `field_a` | WORK | NULL |
| `field_b` | WORK | NULL |
| `field_c` | WORK | NULL |
| `field_d` | WORK | NULL |
| `field_e` | WORK | NULL |
| `field_f` | WORK | NULL |

## Doplnkové väzby a súbory

- `perex_group` je WORK. Snapshot ho prevádza na zoradenú jedinečnú množinu kladných ID; writer používa tokeny `,1,3,`. Prázdna hodnota a NULL znamenajú prázdne priradenie. Definície perex skupín a odvodený `perex_group_doc` sa nemenia.
- `group_ids_snapshot` dopĺňa podporovaný snapshot média mimo fyzických stĺpcov LIVE `media`. Obsahuje zoradenú jedinečnú množinu kategórií z `media_group_to_media`. Prázdny nový zoznam je `[]`. Produkčná zmena členstva vstupuje do konfliktu celého média. Definície kategórií sú LIVE read-only.
- `last_update` sa pri zmenenom WORK UPSERT média odvodí z výsledného `date_created` vlastníka, zaokrúhleného na sekundy nadol. Voľba LIVE a no-op používajú pôvodný LIVE cutoff; `BranchMediaMapper.project` je určený len pre zmenený WORK. Vlastníka `documents`, doménu a práva overuje volajúca služba.
- Súborový snapshot má WORK hodnoty `logical_path`, `entry_kind` a `content_hash`. Druh je FILE alebo DIRECTORY. Hash je SHA-256 bajtov iba pri FILE; pri DIRECTORY je NULL. `size_bytes` a `mime_type` sú prevádzkové metadáta, nie samostatné obsahové konflikty. Prázdny adresár je ACTIVE/DIRECTORY v `branch_files`, bez bajtov. Adresárové DELETE nie je podporované.
- `path_key`, `row_id`, `branch_id`, operácia, BASE/resolved hashe a checkpointy sú metadáta pracovného riadka. Fyzickú cestu k bajtom neurčuje klient. Kanonickú logickú cestu a povolený koreň overuje súborová služba.

## Snapshot a B/L/W API

`BranchSnapshot` rozlišuje `State.ACTIVE`, `TRASH`, `ABSENT`; identita položky je samostatná. ABSENT má prázdny obsah, SQL NULL v poli ostáva hodnotou aktívneho obsahu. TRASH zachováva podporované polia a osobitný stav. JSON má obal `version=1`, `state`, `fields`; kľúče sú zoradené rekurzívne, čísla normalizované, dátumy sú epoch milisekundy. Poradie množín normalizuje konkrétny mapper. SHA-256 zahŕňa celý kanonický JSON vrátane stavu a verzie. Transient a neklasifikované runtime properties sa nekopírujú. Neúplný podporovaný riadok sa odmietne, aby list s nenahraným HTML nebol obsahovým snapshotom.

`BranchDecision.resolve(baseHash, liveHash, workHash, storedChoice)` je čisté rozhodovanie bez DB a publikovania:

| Podmienka | Zdroj | Konflikt | Zápis |
| --- | --- | --- | --- |
| W=B alebo L=W | LIVE | nie | nie |
| L=B | WORK | nie | áno |
| inak | WORK pre náhľad | áno | nie, kým nie je voľba |
| platná ručná LIVE | LIVE | nie | nie |
| platná ručná WORK a L≠W | WORK | nie | áno |
| platná ručná WORK a L=W | LIVE | nie | nie |
| neplatné vstupné hashe ručnej voľby | `StaleChoiceException` | nové porovnanie | nie |

Platnosť ručnej voľby sa kontroluje pred no-op normalizáciou. P03 mapuje stale choice na 409 a nevyriešený konflikt označí pri WORK náhľade. P06 použije rovnaký výsledok; pri konflikte nesmie vykonať zápis. Uložený `resolved_snapshot` nie je zdrojom nového náhľadu.

`BranchChange(branchId,rowId,type,objectKey,operation,base,work,choice)` je spoločný obal; `identity()` vracia napr. `document:42`. `BranchType` má iba DOCUMENT/GROUP/MEDIA/FILE; SQL tabuľky sú pevné serverové konštanty. Číselný objectKey je kanonický kladný int. Súbor používa normalizovanú logickú cestu bez koncovej lomky. Nový objekt sa odstraňuje odobratím pracovného riadka, nie DELETE nad ABSENT základom. Obal nepridáva register handlerov, merge ani storage budúcich modulov.

## Odovzdanie pre P03

Detail aj SQL projekcia musia použiť rovnakú maticu. Mapové mappery sú čistá príprava. P03 ešte musí zapojiť explicitný row mapper na oddelený `DocDetails`/`GroupDetails`, výpočet `data_asc`/`file_name`/koreňov z účinného stromu a SQL projekcie. `BranchDocumentMapper.project` vyžaduje všetky odvodené vstupy a odmietne neúplný výpočet; nepoužíva automatický LIVE fallback pre odvodené polia. Čisté P02 mapovanie nevykonáva JSP, zákaznícke fulltext hooky, audit, históriu, synchronizáciu ani cache invalidáciu.

`BranchFields.verifyColumns` pri aktivácii kontroluje presnú množinu stĺpcov voči aktuálnej DB; unit test navyše vyžaduje klasifikáciu každého `@Column` z `DocBasic`, `DocDetails` a `Media`. Nové fyzické alebo mapované pole sa musí vedome zaradiť. Nejde o všeobecné kopírovanie beanu pri merge.
