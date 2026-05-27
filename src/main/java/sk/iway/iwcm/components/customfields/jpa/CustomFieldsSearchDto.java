package sk.iway.iwcm.components.customfields.jpa;

import java.lang.reflect.Field;

import org.springframework.beans.BeanWrapperImpl;

import jakarta.persistence.Id;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.customfields.rest.CustomFieldsService;

/**
 * DTO used to pass entity identification data for custom fields lookup.
 *
 * <p>It can be created directly from class name and entity ID, or from an entity-like object.
 * In object mode it resolves the field annotated by {@link Id} and an optional bonus parameter configured
 * in {@link CustomFieldsService#BONUS_PARAMS}.</p>
 */
public class CustomFieldsSearchDto {

    private static final long EMPTY_ENTITY_ID = -1L;

    private String className;
    private String entityIdColumnName;
    private Long entityId = EMPTY_ENTITY_ID;
    private Object bonusParam;

    public CustomFieldsSearchDto(String className, Long entityId) {
        this.className = className;
        if(entityId == null) this.entityId = EMPTY_ENTITY_ID;
        else this.entityId = entityId;
    }

    public CustomFieldsSearchDto(String className, Integer entityId) {
        this.className = className;
        if(entityId == null) this.entityId = EMPTY_ENTITY_ID;
        else this.entityId = entityId.longValue();
    }

    public CustomFieldsSearchDto(Object object) {
        if(isLikeEntity(object) == false) {
            Logger.error(CustomFieldsSearchDto.class, "Provided object is not like an entity, cannot extract class name and id. Object class: " + (object != null ? object.getClass().getName() : "null"));
            return;
        }

        Class<?> objectClass = object.getClass();
        this.className = objectClass.getName();
        this.entityIdColumnName = getIdColumnName(objectClass);

        try {
            BeanWrapperImpl bw = new BeanWrapperImpl(object);
            Object extractValue = bw.getPropertyValue(entityIdColumnName);
            if(extractValue instanceof Number entityIdValue) {
                this.entityId = entityIdValue.longValue();
            }
        } catch (Exception ex) {
            Logger.error(CustomFieldsSearchDto.class, "Could not extract entity id from object of class " + objectClass.getName() + " using @Id field '" + entityIdColumnName + "'.", ex);
        }

        String bonusParamName = CustomFieldsService.BONUS_PARAMS.get(this.className);
        if(Tools.isNotEmpty(bonusParamName)) {
            // Try to get and set bonus param.
            try {
                BeanWrapperImpl bw = new BeanWrapperImpl(object);
                this.bonusParam = bw.getPropertyValue(bonusParamName);
            } catch (Exception ex) {
                Logger.error(CustomFieldsSearchDto.class, "Could not extract bonus param '" + bonusParamName + "' from object of class " + objectClass.getName(), ex);
            }
        }
    }

    /**
     * Checks whether the provided object looks like a domain entity candidate.
     *
     * <p>This is a lightweight guard that filters out primitive wrappers, strings, numbers,
        * enums and {@code null}, and verifies an {@link Id} annotated field exists.
        * Passing this check does not guarantee that the object is a JPA entity, but indicates
        * it is suitable for reflective ID extraction.</p>
     *
     * @param obj object to verify
     * @return {@code true} when the object is a non-null, non-primitive-like candidate
     */
    private boolean isLikeEntity(Object obj) {
        if (obj == null) {
            return false;
        }

        Class<?> clazz = obj.getClass();

        if(clazz.isPrimitive()
                || clazz.isEnum()
                || clazz == String.class
                || clazz == Boolean.class
                || clazz == Character.class
                || Number.class.isAssignableFrom(clazz)) {
            return false;
        }

        return getIdColumnName(clazz) != null;
    }

    private String getIdColumnName(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field.getName();
            }
        }

        return null;
    }

    public boolean isValid() {
        return Tools.isNotEmpty(className);
    }

    public String getClassName() {
        return className;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getEntityIdColumnName() {
        return entityIdColumnName;
    }

    public Object getBonusParam() {
        return bonusParam;
    }
}
