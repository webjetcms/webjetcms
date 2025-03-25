# Auditing

## JPA entity

> Automatické generování auditních záznamů z JPA entit (obsahujících seznam změn ve formě jméno\_atributu: stará hodnota -> nová hodnota) pomocí jednoduché anotace `@EntityListeners(AuditEntityListener.class)`

Auditing změn v JPA entitách lze automatizovat přidáním anotace `@EntityListeners(AuditEntityListener.class)`, přičemž typ auditního záznamu nastavíte anotací `@EntityListenersType(Adminlog.TYPE_GALLERY)`:

```java
@Entity
@Table(name = "gallery")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_GALLERY)
@Getter
@Setter
public class GalleryEntity {
```

Auditing se provádí v [AuditEntityListener](../../../src/main/java/sk/iway/iwcm/system/audit/AuditEntityListener.java) metodách:
- `postPersist` pro nově vytvořený záznam
- `preUpdate` a `postUpdate` pro aktualizaci stávajícího záznamu (v preUpdate se získá aktuální Entita v databázi pro porovnání změn)
- `preRemove` při smazání záznamu

v metodě `private String getChangedProperties(Object entity)` se získá seznam změněných hodnot při změně záznamu, respektive seznam aktuálních hodnot pro nový záznam. Pro již existující záznam se nejprve v metodě `prePersist` uloží do mapy `private Hashtable<Long, String> preUpdateChanges;` srovnání existujícího záznamu v databázi. Z této mapy se následně po úspěšném uložení v metodě `postUpdate` získá z mapy seznam změn pro auditing.

**Získání aktuálního beanu k porovnání** z databáze před zápisem změn je poměrně komplikované. Vyřešeno to je **získáním nového EntityManagera** a načtením objektu s tímto novým EntityManagerem. Objekty manažované přes SpringData s tímto problémem netrpěly, ale standardní JPA entity se v rámci aktuální session vracely z databáze změněny na nové hodnoty. Je to způsobeno tím, že SpringData používá vlastní EntityManager.

V konfigurační proměnné `auditHideProperties` je seznam atributů, které se v auditu nahradí značkou `*****`. Ve výchozím nastavení je nastaven atribut `password,password2,password_salt`, v konfiguraci ale můžete přidat **další citlivé atributy, které nechcete mít zobrazeny v auditu**.

Příklad auditu nově vytvořené entity:

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

Příklad auditu změněné entity. Do auditu se zaznamenají pouze změněná pole (redirect\_to, redirect\_params a redirect\_path):

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

Příklad auditu smazání položky. Do auditu se zaznamenají všechny údaje mazané entity:

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
