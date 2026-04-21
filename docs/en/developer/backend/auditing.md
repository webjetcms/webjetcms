# Auditing

## JPA entities

>Automatically generate audit records from JPA entities (containing a list of changes in the form attribute_name: old value -> new value) using a simple annotation ```@EntityListeners(AuditEntityListener.class)```

Auditing changes in JPA entities can be automated by adding the ```@EntityListeners(AuditEntityListener.class)``` annotation, while setting the audit record type with the ```@EntityListenersType(Adminlog.TYPE_GALLERY)``` annotation:

```java
@Entity
@Table(name = "gallery")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_GALLERY)
@Getter
@Setter
public class GalleryEntity {
```

Auditing is performed in the [AuditEntityListener](../../../src/main/java/sk/iway/iwcm/system/audit/AuditEntityListener.java) methods:

- ```postPersist``` for a newly created record
- ```preUpdate``` and ```postUpdate``` for updating an existing record (in preUpdate the current Entity in the database is obtained for comparing changes)
- ```preRemove``` when deleting a record

In the method ```private String getChangedProperties(Object entity)```, a list of changed values ​​is obtained when changing a record, or a list of current values ​​for a new record. For an existing record, a comparison of the existing record in the database is first saved in the method ```prePersist``` to the map ```private Hashtable<Long, String> preUpdateChanges;```. From this map, after successful saving in the method ```postUpdate```, a list of changes for auditing is obtained from the map.

**Getting the current bean for comparison** from the database before writing changes is quite complicated. This is solved by **getting a new EntityManager** and loading the object with this new EntityManager. Objects managed via SpringData did not suffer from this problem, but standard JPA entities were returned from the database within the current session with changed values. This is because SpringData uses its own EntityManager.

The configuration variable ```auditHideProperties``` contains a list of attributes that will be replaced with the `*****` tag in the audit. The default attribute is ```password,password2,password_salt```, but you can add **additional sensitive attributes that you do not want to be displayed in the audit** in the configuration.

Example of an audit of a newly created entity:

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

Example of a changed entity audit. Only changed fields (redirect_to, redirect_params, and redirect_path) are recorded in the audit:

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

Example of an audit of an item deletion. All data of the deleted entity is recorded in the audit:

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