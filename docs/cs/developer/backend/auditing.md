# Audit

## Subjekt JPA

> Automatické generování auditních záznamů z entit JPA (obsahujících seznam změn ve tvaru název\_atributu: stará hodnota -> nová hodnota) pomocí jednoduché anotace `@EntityListeners(AuditEntityListener.class)`

Auditování změn entit JPA lze automatizovat přidáním anotace. `@EntityListeners(AuditEntityListener.class)`, kde nastavíte typ auditního záznamu poznámkou. `@EntityListenersType(Adminlog.TYPE_GALLERY)`:

```java
@Entity
@Table(name = "gallery")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_GALLERY)
@Getter
@Setter
public class GalleryEntity {
```

Audit se provádí v [AuditEntityListener](../../../src/main/java/sk/iway/iwcm/system/audit/AuditEntityListener.java) Metody:
- `postPersist` pro nově vytvořený záznam
- `preUpdate` a `postUpdate` aktualizovat existující záznam (v preUpdate se načte aktuální entita v databázi, aby se porovnaly změny).
- `preRemove` při mazání záznamu

v metodě `private String getChangedProperties(Object entity)` seznam změněných hodnot při změně záznamu nebo seznam aktuálních hodnot pro nový záznam. Pro existující záznam se nejprve v metodě `prePersist` ukládá do mapy `private Hashtable<Long, String> preUpdateChanges;` porovnání existujícího záznamu v databázi. Z této mapy se po úspěšném uložení v metodě `postUpdate` získá seznam změn pro auditování z mapy.

**Získání aktuální fazole pro porovnání** z databáze před zápisem změn je poměrně složité. Řešením je **získání nového správce entit EntityManager** a načtení objektu pomocí tohoto nového EntityManageru. Objekty spravované prostřednictvím SpringData tímto problémem netrpěly, ale standardní entity JPA vrácené z databáze v rámci aktuální relace se změnily na nové hodnoty. To je způsobeno tím, že SpringData používá vlastní EntityManager.

V konfigurační proměnné `auditHideProperties` je seznam atributů, které budou v auditu nahrazeny značkou `*****`. Ve výchozím nastavení je atribut nastaven `password,password2,password_salt`, ale v konfiguraci můžete přidat **další citlivé atributy, které se v auditu nemají zobrazovat**.

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

Příklad auditu změněného entita. V auditu jsou zaznamenána pouze změněná pole (redirect\_to, redirect\_params a redirect\_path):

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

Příklad auditu vymazání položky. Audit zaznamenává všechny údaje o smazané entitě:

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
