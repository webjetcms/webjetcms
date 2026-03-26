package sk.iway.iwcm.system.adminlog;

import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.jpa.JpaHelper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.core.annotation.AnnotationUtils;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.DataSource;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.PerexGroupBean;

import jakarta.persistence.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

/**
 * Listener auditujuci zmeny v JPA entitach. Pouziva sa nastavenim anotacii na JPA entite:
 *
 * @EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
 * @EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_GALLERY)
 *
 * viac je v dokumentacii v auditing.md
 * ticket: #46891
 */
public class AuditEntityListener {

    private HashMap<Long, String> preUpdateChanges;

    @PrePersist
    public void prePersist(Object entity) {
        //log.debug("prePersist");
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (isDisabled(entity)) return;
        try {
            saveChanges(entity);
        } catch (Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
        }
    }

    @PreRemove
    public void preRemove(Object entity) {
        //log.debug("preRemove: " + getId(entity));
    }

    @PostPersist
    public void postPersist(Object entity) {
        if (isDisabled(entity)) return;
        try {
            //log.debug("postPersist");
            String changes = getChangedProperties(entity, false);
            Number id = getId(entity);
            Adminlog.add(getType(entity), "CREATE:\n" + changes, id.intValue(), -1);
        } catch (Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
        }
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        if (isDisabled(entity)) return;
        try {
            Logger.debug(getClass(), "postUpdate");
            String changes = getAndClearSavedChanges(entity);
            Number id = getId(entity);
            Logger.debug(getClass(), "postUpdate: id: " + id + ", changeds: " + changes);
            Adminlog.add(getType(entity), "UPDATE:\nid: " + id.longValue() + "\n" + changes, id.intValue(), -1);
        } catch (Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
        }
    }

    @PostRemove
    public void postRemove(Object entity) {
        if (isDisabled(entity)) return;
        try {
            //log.debug("postRemove");
            Number id = getId(entity);
            String changes = getChangedProperties(entity, false);
            Adminlog.add(getType(entity), "DELETE:\nid: " + id.longValue() +  "\n" + changes, id.intValue(), -1);
        } catch (Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
        }
    }

    @PostLoad
    public void postLoad(Object entity) {
        //nothing here
    }

    /**
     * Overi, ci pre danu entitu je povoleny auditing
     * @param entity
     * @return
     */
    private boolean isDisabled(Object entity) {

        //do not audit during startup/UpdateDatabase call
        if (InitServlet.isSpringInitialized() == false) return true;

        String auditJpaDisabledEntities = Constants.getString("auditJpaDisabledEntities");
        if ("*".equals(auditJpaDisabledEntities)) return true;
        if (Tools.isEmpty(auditJpaDisabledEntities)) return false;

        String fqdn = entity.getClass().getName();
        if (auditJpaDisabledEntities.contains(fqdn)) return true;

        return false;
    }

    /**
     * Ziska typ auditneho zaznamu podla anotacie @EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_GALLERY)
     * @param entity
     * @return
     */
    private int getType(Object entity) {
        int result = Adminlog.TYPE_CLIENT_SPECIFIC;
        EntityListenersType annotation = AnnotationUtils.findAnnotation(entity.getClass(), EntityListenersType.class);
        if (annotation != null && annotation.value() > 0) {
            result = annotation.value();
        }

        return result;
    }

    /**
     * Ziska primarny kluc z entity
     * @param entity
     * @return
     */
    private Number getId(Object entity) {
        try {
            BeanWrapperImpl entityWrapper = new BeanWrapperImpl(entity);
            Object id = entityWrapper.getPropertyValue("id");
            if (id instanceof Number) return (Number)id;
            return 0;
        } catch (NotReadablePropertyException ex) {
            //neexistuje property s menom id
        }

        //skus najst property podla anotacie @Id
        try {
            Field[] declaredFields = getDeclaredFieldsTwoLevels(entity.getClass());
            for (Field declaredField : declaredFields) {
                if (declaredField.isAnnotationPresent(Id.class)) {
                    BeanWrapperImpl entityWrapper = new BeanWrapperImpl(entity);
                    Object id = entityWrapper.getPropertyValue(declaredField.getName());
                    if (id instanceof Number) return (Number)id;
                }
            }
        } catch (Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
        }

        //lepsie vratit 0 ako Exception, aspon sa zaaudituje nieco
        return 0;
    }


    /**
     * chcel by som, aby to zauditovalo aj zoznam zmien, volakedy ked sme pouzivali Cayenne sme mali metodu, ktorej sme dali zmeneny bean, ten si fetchol podla IDecka aktualny z DB, preiteroval property a zmeny pekne vypisal na kazdy riadok s menom property.
     * @param entity
     * @param compareToDb - ak je true nacita sa aktualna entita z DB a spravi sa porovnanie
     * @return
     */
    private String getChangedProperties(Object entity, boolean compareToDb) {
        BeanWrapperImpl entityWrapper = new BeanWrapperImpl(entity);

        Number id = getId(entity);
        StringBuilder log = new StringBuilder();

        //add auditValues from RequestBean
        RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (requestBean != null) {
            log.append(Adminlog.getRequestBeanAuditLog(requestBean));
        }

        List<String> auditHideProperties = Tools.getStringListValue(Tools.getTokens(Constants.getString("auditHideProperties", ""), ","));

        Object dbEntity = null;
        if (compareToDb && id != null && id.longValue() > 0) {
            //try this as Spring DATA
            SpringDataHelper springDataHelper = Tools.getSpringBean("springDataHelper", SpringDataHelper.class);
            if (springDataHelper != null) {
                try {
                    dbEntity = springDataHelper.getSpringDataEntity(entity, id.longValue());
                } catch (Exception ex) {
                    //it is not Spring DATA entity
                    //sk.iway.iwcm.Logger.error(ex);
                }
            }

            if (dbEntity == null) {
                try {
                    //musime otvotit novy entitymanager aby sme ziskali aktualne data v DB
                    String dbName = "iwcm";
                    Class<?> clazz = entity.getClass();
                    if (clazz.isAnnotationPresent(DataSource.class)) {
                        Annotation annotation = clazz.getAnnotation(DataSource.class);
                        DataSource dataSource = (DataSource) annotation;
                        dbName = dataSource.name();
                    }
                    EntityManagerFactory factory = DBPool.getEntityManagerFactory(dbName);
                    //factory.getProperties().put("eclipselink.session.customizer", "sk.iway.webjet.v9.JpaSessionCustomizer");

                    JpaEntityManager em = JpaHelper.getEntityManager(factory.createEntityManager());
                    dbEntity = em.find(entity.getClass(), id);
                    em.close();
                } catch (Exception ex) {
                    //it is not JPA entity
                    //sk.iway.iwcm.Logger.error(ex);
                }
            }

            //entity not found, for safety show all fields in audit log (same as new entity, do not compare)
            if (dbEntity == null) compareToDb = false;
        }

        BeanWrapperImpl dbEntityWrapper = null;
        if (dbEntity!=null) dbEntityWrapper = new BeanWrapperImpl(dbEntity);

        Field[] declaredFields = getDeclaredFieldsTwoLevels(entity.getClass());
        for (Field declaredField : declaredFields) {
            try {
                String name = declaredField.getName();
                if ("serialVersionUID".equals(name)) continue;
                if ("dataAsc".equals(name) || "data_asc".equals(name)) continue;

                String entityValue = getStringValue(entityWrapper.getPropertyValue(name), name);
                String dbEntityValue = null;
                if (dbEntityWrapper!=null) dbEntityValue = getStringValue(dbEntityWrapper.getPropertyValue(name), name);

                //key mame kvoli TranslationKeys
                if (compareToDb==false || !entityValue.equals(dbEntityValue) || "key".equals(name)) {
                    // Tu by sme to este vedeli vylepsit, pretoze z anotacii aj vieme ziskat pekne meno ako sa pouziva v datatabulke.
                    Column annotation = AnnotationUtils.getAnnotation(declaredField, Column.class);
                    String columnName = annotation != null ? annotation.name() : name;
                    String value;
                    if (dbEntity!=null) value = dbEntityValue + " -> " + entityValue;
                    else value = entityValue;

                    if (auditHideProperties.contains(name) || auditHideProperties.contains(columnName)) {
                        value = "*****";
                    }

                    if (log.length()>0) log.append("\n");

                    log.append(columnName).append(": ");
                    log.append(value);
                }
            } catch (Exception ex) {
                //sk.iway.iwcm.Logger.error(ex);
            }
        }
        return log.toString();
    }

    /**
     * Format Object propertyValue to String representation
     * @param entityPropertyValue
     * @return
     */
    private String getStringValue(Object entityPropertyValue, String name) {
        String entityValue = null;
        if (entityPropertyValue == null) {
            entityValue = "null";
        } else {
            if (entityPropertyValue.getClass().isArray()) {
                Object[] array = (Object[]) entityPropertyValue;
                StringBuilder builder = new StringBuilder();
                for (Object o : array) {
                    if (builder.isEmpty()==false) builder.append(",");

                    if (name.equals("perexGroups") && o instanceof Integer) {
                        Integer i = (Integer) o;
                        PerexGroupBean pgb = DocDB.getInstance().getPerexGroup(i, null);
                        if (pgb.getPerexGroupId()>0) builder.append(pgb.getPerexGroupNameId());
                        else builder.append(i);
                    } else {
                        builder.append(String.valueOf(o));
                    }
                }
                entityValue = builder.toString();
            } else if (entityPropertyValue instanceof Date) {
                entityValue = Tools.formatDateTimeSeconds((Date) entityPropertyValue);
            } else if (entityPropertyValue instanceof Long && name.contains("date")) {
                entityValue = Tools.formatDateTimeSeconds((Long) entityPropertyValue);
            } else {
                entityValue = shrinkValue(String.valueOf(entityPropertyValue));
            }
        }
        return shrinkValue(entityValue);
    }

    /**
     * Skrati hodnotu, aby sa neauditovali dlhe zaznamy a audit nezaberal vela miesta
     * @param value
     * @return
     */
    private String shrinkValue(String value) {
        if (value == null || "null".equals(value)) return "null";
        int maxLength = Constants.getInt("auditMaxChangeLength", 100);
        if (value.length()>maxLength) value = value.substring(0, maxLength-1)+"...";
        return value;
    }

    /**
     * Ziska zoznam zmien entity v databaze voci entite ako parameter
     * zmenu ulozi do mapy preUpdateChanges pre neskorsie ziskanie
     * @param entity - aktualna entita, ktoru ideme ukladat
     */
    private void saveChanges(Object entity) {
        Number id = getId(entity);
        if (id.longValue() > 0) {
            String changes = getChangedProperties(entity, true);
            if (preUpdateChanges == null) {
                preUpdateChanges = new HashMap<>();
            }
            preUpdateChanges.put(id.longValue(), changes);
        }
    }

    /**
     * Vrati ulozeny zoznam zmien z hashmapy a zmaze ho z hashmapy (aby sa zbytocne nezaplnala pamat)
     * @param entity
     * @return
     */
    private String getAndClearSavedChanges(Object entity) {
        Number id = getId(entity);
        if (id.longValue() > 0 && preUpdateChanges != null) {
            String changes = preUpdateChanges.get(id.longValue());
            if (changes!=null) {
                preUpdateChanges.remove(id);
                return changes;
            }
        }
        return "";
    }

    /**
     * Returns o.getDeclaredFields also from extended object, for use in DataTables (we are traversing max to one level parent)
     * @param o
     * @return
     */
    public static Field[] getDeclaredFieldsTwoLevels(Class<?> o) {
        Field[] declaredFields;
        if(o.getSuperclass() != null) {
            declaredFields = Stream.concat(Arrays.stream(o.getDeclaredFields()), Arrays.stream(o.getSuperclass().getDeclaredFields())).toArray(Field[]::new);
        } else {
            declaredFields = o.getDeclaredFields();
        }
        return declaredFields;
    }
}
