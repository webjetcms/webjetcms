# Auditing

## JPA entity

> **tl;dr** Automatické generovanie auditných záznamov z JPA entít (obsahujúcich zoznam zmien vo forme meno_atribútu: stará hodnota -> nová hodnota) pomocou jednoduchej anotácie ```@EntityListeners(AuditEntityListener.class)```

Auditing zmien v JPA entitách je možné automatizovať pridaním anotácie ```@EntityListeners(AuditEntityListener.class)```, pričom typ auditného záznamu nastavíte anotáciou ```@EntityListenersType(Adminlog.TYPE_GALLERY)```:

```java
@Entity
@Table(name = "gallery")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_GALLERY)
@Getter
@Setter
public class GalleryEntity {
```

Auditing sa vykonáva v [AuditEntityListener](../../../src/main/java/sk/iway/iwcm/system/audit/AuditEntityListener.java) metódach:

- ```postPersist``` pre novo vytvorený záznam
- ```preUpdate``` a ```postUpdate``` pre aktualizáciu existujúceho záznamu (v preUpdate sa získa aktuálna Entita v databáze pre porovnanie zmien)
- ```preRemove``` pri zmazaní záznamu

v metóde ```private String getChangedProperties(Object entity)``` sa získa zoznam zmenených hodnôt pri zmene záznamu, respektíve zoznam aktuálnych hodnôt pre nový záznam. Pre už existujúci záznam sa najskôr v metóde ```prePersist``` uloží do mapy ```private Hashtable<Long, String> preUpdateChanges;``` porovnanie existujúceho záznamu v databáze. Z tejto mapy sa následne po úspešnom uložení v metóde ```postUpdate``` získa z mapy zoznam zmien pre auditing.

**Získanie aktuálneho beanu na porovnanie** z databázy pred zápisom zmien je pomerne komplikované. Vyriešené to je **získaním nového EntityManagera** a načítaním objektu s týmto novým EntityManagerom. Objekty manažované cez SpringData s týmto problémom netrpeli, ale štandardné JPA entity sa vrámci aktuálnej session vracali z databázy zmenené na nové hodnoty. Je to spôsoboné tým, že SpringData používa vlastný EntityManager.

V konfiguračnej premennej ```auditHideProperties``` je zoznam atribútov, ktoré sa v audite nahradia značkou *****. Predvolene je nastavený atribút ```password,password2,password_salt```, v konfigurácii ale môžete pridať **ďalšie citlivé atribúty, ktoré nechcete mať zobrazené v audite**.

Príklad auditu novo vytvorenej entity:

```
CREATE:
redirect_id: 1482
active: true
redirect_from: www.stara-domena.sk
redirect_to: www.nova-domena.sk
redirect_params: false
redirect_path: false
http_protocol: http

URI: /admin/rest/settings/domain-redirect/editor
Domain: iwcm.interway.sk
User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.80 Safari/537.36
```

Príklad auditu zmenenej entity. Do auditu sa zaznamenajú len zmenené polia (redirect_to, redirect_params a redirect_path):

```
UPDATE:
id: 1482
redirect_to: www.nova-domena.sk -> www.nova-domena-uprava.sk
redirect_params: false -> true
redirect_path: false -> true

URI: /admin/rest/settings/domain-redirect/editor
Domain: iwcm.interway.sk
User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.80 Safari/537.36
```

Príklad auditu zmazania položky. Do auditu sa zaznamenajú všetky údaje mazanej entity:

```
DELETE:
id: 1482
redirect_id: 1482
active: true
redirect_from: www.stara-domena.sk
redirect_to: www.nova-domena-uprava.sk
redirect_params: true
redirect_path: true
http_protocol: http

URI: /admin/rest/settings/domain-redirect/editor
Domain: iwcm.interway.sk
User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.80 Safari/537.36
```