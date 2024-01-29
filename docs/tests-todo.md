# Zoznam testovacích scenárov na dokončenie

- [ ] #53561 - Galéria - doplniť test práce e editorom fotiek (keď klikneš na fotku v karte Editor). Identifikoval a snáď opravil som problém, kedy po úprave fotky a následne jej znova otvorení sa znova odosielala na server zmena z editora obrázkov (prejavilo sa pridaním položky do dialógového okna so zoznamom nahrávaných súborov vpravo dole).
- [ ] #53561 - Galéria - kompletne otestovať prácu so stromovou štruktúrou, je prerobená na Datatables API z pôvodného VUE komponentu. Otestovať správanie Pre-generovať obrázky a Aplikovať na všetky podpriečinky a Vodotlač (pozor, aplikuje sa len na novo nahraté obrázky, alebo by sa mala aplikovať po zaškrtnutí možnosti Pregenerovať obrázky).
- [ ] #53561 - Galéria - otestovať aj prístupové práva, používateľ môže mať nastavené len práva zápisu do vybraného adresára, Galéria nesmie povoliť zápis do adresára kde používateľ nemá práva. Overiť toto správanie pre nahratie fotky, ale aj prácu so štruktúrou (zvlášť overiť drag&drop).
- [ ] #53825 - Datatabuľky - pridaná všeobecná funkčnosť duplikovania záznamov. Bolo by super doplniť testovanie tejto funkčnosti priamo do všeobecného testu datatabuliek (napr. pred zmazaním záznamu vyskúšať ešte jeho duplikovanie a overiť, že sa tam záznam nachádza 2x) - ```src/test/webapp/pages/DataTables.js```.
- [ ] #53825 - Web stránky - opravená multi editácia viacerých záznamov (keď označím viac riadkov a kliknem na upraviť). V prvej karte sa už zobrazuje editor so štandardnou DT funkčnosťou (žltý obdĺžnik) a možnosťou ponechať záznamom rozdielny text stránky alebo všetkým nastaviť rovnaký text stránky.
- [ ] #54133 - Používatelia - pridať test slabého hesla - existuje používateľ ```testerslabeheslo```, jemu je potrebne s pouzivatelom ```tester``` v admine nastaviť slabé heslo ```tentousermavelmislabeheslo```, následne sa odhlásiť, prihlásiť sa s kontom ```testerslabeheslo```. Mala by sa zobraziť výzva na zadanie nového hesla, zadať kvalitnejšie heslo, odskúšať hlásenia o požadovaných vlastnostiach hesla. Následne sa s kvalitným heslom pokúsiť prihlásiť. Po úspechu znova v admine s kontrom ```tester``` nastaviť heslo ```tentousermavelmislabeheslo``` pre použitie v iných testoch.
- [x] Web stránky - 24008 Media - editujem stránku, kliknem na kartu Media, otvorím Media, zatvorim krížikom hore, kliknem na Uložiť vo web stránke, okno sa nezatvorí. (opravené, test v webpages.js)
- [ ] #39751-46 Galéria - test nahratia obrázka, jeho otočenia (overenie otočenia porovnaním screenshotu).
- [ ] Media - test keď používateľ nemá práva na všetky média, že ide editovať médium v stránke a pridať nové.

## Java 17

https://gitlab.web.iway.local/webjet/webjet8v9/-/merge_requests/250/diffs?commit_id=a81b6e01ae4911519cd3c17c5c286a478234de1b

```
devcontainer.json
        "version": "17",

compile.xml
        <javac
                includeAntRuntime="false"
                source="17"
                target="17"
        <iajc
                source="17"
                target="17"
```

## TODO

- Auditovanie v Oracle - nastavenie ```factory.getProperties().put("eclipselink.session.customizer", "sk.iway.webjet.v9.JpaSessionCustomizer");```


## Zmazať

```
/admin/usergroupedit.jsp
/components/calendar/admin_events.jsp
/components/calendar/admin_edit.jsp
/admin/temps_list.jsp
/admin/temp_edit.jsp
/admin/temps_merge.jsp - TODO ZLUCENIE
/admin/users.jsp
/servlet/sk.iway.iwcm.users.XLSServlet
/admin/users_import.jsp
/admin/users_groups.jsp
/admin/users_advanced_list.jsp
/admin/users_admin_list.jsp
/admin/useredit.jsp
/admin/useredit_self.jsp - TODO mame v hlavicke, prerobit do Spring
/components/calendar/admin_events.jsp
/components/calendar/admin_edit.jsp
/components/banner/banner_editor_popup.jsp
/components/gallery/admin_gallery_list.jsp
/admin/file_browser/move_result.jsp
/admin/file_browser/backup_result.jsp
/admin/fbrowser_browse.jsp
        /admin/fbrowser_dirprop.jsp - POUZIVA ELFINDER NEMAZAT
/admin/fbrowser_editor.jsp
/admin/fbrowser_bin.jsp
/admin/fbrowser_prop.jsp
/admin/fbrowser_browse.jsp
/gallery_fbrowse.jsp
/components/dmail/admin_email.jsp
/components/dmail/admin_email_emailsframe.jsp
/components/dmail/admin_send.jsp
/components/dmail/admin_sending_email.jsp
/admin/temps_import.jsp
/admin/welcome/get_wall_list_ajax.jsp
/admin/welcome/wall_list.jsp
```

Nahradené linky:

```
/admin/linkcheck.do
/logonsms.do
/reguser.do
/changeuser.do
/admin/editusergroup.do
/admin/saveusergroup.do
/admin/listtemps.do
/admin/savetemp.do
/admin/listusers.do
/admin/edituser.do
/admin/saveuser.do
/admin/importusers.do
/admin/statchart.do
/admin/statchartnew.do
/graph.do
/admin/listevents.do
/admin/editevent.do
/admin/saveevent.do
/admin/delevent.do
/admin/saveBazarGroup.do
/saveBazarAd.do
/admin/dbbrowser.do
/components/pageUpdateInfo.do
/xlschart.do
/admin/crontab.do
/components/chatEdit.do
/chat.do
/components/banner.do
/components/tips.do
/admin/uploadfilegallery.do
/admin/photogallery.do
/admin/fbrowser.moveto.do
/admin/fbrowser.backup.do
/admin/fbrowser.unzip.do
/admin/fbrowser.delete.do
/admin/fbrowser.upload.do
/admin/fbrowser.save.do
/admin/fbrowser.edit.do
/admin/fbrowser.browse.do
/admin/email.do
```

Zrušené triedy:

```
sk.iway.iwcm.doc.TemplatesForm
sk.iway.iwcm.users.UserForm
sk.iway.iwcm.calendar.EventForm
sk.iway.iwcm.components.banner.model.BannerForm
sk.iway.iwcm.components.chat.ChatRoomForm
sk.iway.iwcm.components.bazar.BazarGroupBean
sk.iway.iwcm.components.bazar.BazarAdvertisementBean
sk.iway.iwcm.components.pageUpdateInfo.PageUpdateInfoForm
sk.iway.iwcm.doc.LinkCheckAction
sk.iway.iwcm.UsrLogonSMSAction
sk.iway.iwcm.users.RegUserAction
sk.iway.iwcm.users.ChangeUserAction
sk.iway.iwcm.users.EditUserGroupAction
sk.iway.iwcm.users.SaveUserGroupAction
sk.iway.iwcm.doc.ListTempsAction
sk.iway.iwcm.editor.SaveTemplateAction
sk.iway.iwcm.users.ListUsersAction
sk.iway.iwcm.users.EditUserAction
sk.iway.iwcm.users.SaveUserAction
sk.iway.iwcm.users.ImportAction
sk.iway.iwcm.stat.ChartAction
sk.iway.iwcm.stat.ChartNewAction
sk.iway.iwcm.components.graph.GraphAction
sk.iway.iwcm.calendar.ListEventsAction
sk.iway.iwcm.calendar.EditEventAction
sk.iway.iwcm.calendar.SaveEventAction
sk.iway.iwcm.calendar.DeleteEventAction
sk.iway.iwcm.components.banner.BannerAction
sk.iway.iwcm.components.tips.TipBean
sk.iway.iwcm.components.tips.TipsAction
sk.iway.iwcm.gallery.UploadAction
sk.iway.iwcm.gallery.PhotoGalleryAction
sk.iway.iwcm.filebrowser.EditAction
sk.iway.iwcm.filebrowser.UnzipAction
sk.iway.iwcm.filebrowser.DeleteAction
sk.iway.iwcm.filebrowser.UploadAction
sk.iway.iwcm.filebrowser.SaveAction
sk.iway.iwcm.filebrowser.BrowseAction
sk.iway.iwcm.dmail.EMailAction
sk.iway.iwcm.components.tips.TipsAction
sk.iway.iwcm.components.chat.ChatController
sk.iway.iwcm.system.cron.CrontabAction
sk.iway.iwcm.components.chat.ChatRoomAction
sk.iway.iwcm.components.bazar.BazarGroupAction
sk.iway.iwcm.components.bazar.BazarAdvertisementAction
sk.iway.iwcm.components.db_browser.DBbrowserAction
sk.iway.iwcm.components.pageUpdateInfo.PageUpdateInfoAction
sk.iway.iwcm.xls.XlsChartAction
sk.iway.iwcm.editor.templates.*
sk.iway.iwcm.components.welcome.WallAjaxAction
sk.iway.iwcm.components.KurzyDB
sk.iway.iwcm.components.KurzBean
sk.iway.iwcm.doc.GroupsListAction
Constants.INSTALL_NAME
GroupsController.BASE_URL
```

## Zmena API

```
ArrayList ... ForumDB.
ArrayList ... SendMail.
ArrayList ... DocDB.
ArrayList ... TemplatesDB.
ArrayList ... EmailDB.
ArrayList ... ProxyDB.
ArrayList ... AtrDB.
ArrayList ... InquiryDB.
Hashtable ... ForumDB.
Hashtable ... GroupsDB.
Hashtable ... TemplatesDB.
Hashtable ... EmailDB.
```

## Zmena SQL

Po prekliknuti na Kos v domene webjet9 ziskat ID priecinka Kos a nastavit:

```
update groups set parent_group_id=45428 where parent_group_id=4574;
update documents set group_id=45428 where group_id=4574
```