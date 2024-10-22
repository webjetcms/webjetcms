# Auditing

## JPA entity

> **tl;dr** Automatic generation of audit records from JPA entities (containing a list of changes in the form attribute\_name: old value -> new value) using a simple annotation `@EntityListeners(AuditEntityListener.class)`

Auditing changes to JPA entities can be automated by adding annotation `@EntityListeners(AuditEntityListener.class)`, where you set the type of audit record by annotating `@EntityListenersType(Adminlog.TYPE_GALLERY)`:

```java
@Entity
@Table(name = "gallery")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_GALLERY)
@Getter
@Setter
public class GalleryEntity {
```

Auditing is carried out in [AuditEntityListener](../../../src/main/java/sk/iway/iwcm/system/audit/AuditEntityListener.java) Methods:
- `postPersist` for a newly created record
- `preUpdate` a `postUpdate` to update an existing record (in preUpdate the current Entity in the database is retrieved to compare changes)
- `preRemove` when deleting a record
in the method `private String getChangedProperties(Object entity)` list of changed values when the record is changed or list of current values for the new record is obtained. For an existing record, first in the method `prePersist` saves to the map `private Hashtable<Long, String> preUpdateChanges;` comparison of an existing record in the database. From this map, after successful saving in the method `postUpdate` gets the list of changes for auditing from the map.

**Getting the current bean for comparison** from the database before writing changes is quite complicated. The solution to this is **getting a new EntityManager** and loading the object with this new EntityManager. Objects managed via SpringData did not suffer from this problem, but the standard JPA entities returned from the database within the current session changed to the new values. This is due to the fact that SpringData uses its own EntityManager.
In the configuration variable `auditHideProperties` is a list of attributes that will be replaced in the audit by a tag **\***. By default the attribute is set `password,password2,password_salt`, but in the configuration you can add **other sensitive attributes that you do not want to be shown in the audit**.

Example of auditing a newly created entity:

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

Example of a changed entity audit. Only the changed fields (redirect\_to, redirect\_params and redirect\_path) are recorded in the audit:

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

Example of an item deletion audit. The audit records all data of the deleted entity:

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
