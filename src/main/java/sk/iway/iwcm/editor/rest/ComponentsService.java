package sk.iway.iwcm.editor.rest;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.WebjetComponentInterface;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.datatable.*;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.spring.DateConverter;
import sk.iway.iwcm.system.spring.components.SpringContext;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Rest controller pre datatabulku zobrazenia parametrov aplikacie (v appstore)
 */
public class ComponentsService {

    public static Map<String, Object> getComponentResponse(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        String className = componentRequest.getClassName();
        if (Tools.isEmpty(className)) {
            result.put("error", "Class name empty");
            return result;
        }

        ApplicationContext applicationContext = SpringContext.getApplicationContext();
        if (!applicationContext.containsBean(className)) {
            result.put("error", "Class with name: " + className + " not found");
            return result;
        }

        WebjetComponentInterface bean = getWebjetComponentBean(applicationContext, className);
        if (bean == null) {
            result.put("error", "Class with name: " + className + " is not webjet component");
            return result;
        }

        try {
            bean.initAppEditor(componentRequest, request);
            setParametersToBean(componentRequest, bean);
            DataTableColumnsFactory dataTableColumnsFactory = new DataTableColumnsFactory(className);
            result.put("data", bean);
            result.put("columns", dataTableColumnsFactory.getColumns(null));
            result.put("tabs", dataTableColumnsFactory.getTabs());
            result.put("title", dataTableColumnsFactory.getTitle());

            //add options for selects
            result.put("options", bean.getAppOptions(componentRequest, request));

            String componentPath = null;
            if (bean.getClass().isAnnotationPresent(WebjetAppStore.class)) {
                WebjetAppStore appStore = bean.getClass().getAnnotation(WebjetAppStore.class);
                componentPath = appStore.componentPath();
            }
            result.put("componentPath", componentPath);
        }
        catch (Exception e) {
            result.put("error", e.getMessage());
            sk.iway.iwcm.Logger.error(e);
        }

        return result;
    }

    /**
     * Nastavenie na FE zadanych hodnot do beanu
     * @param componentRequest ComponentRequest
     * @param bean WebjetComponentInterface
     */
    private static void setParametersToBean(ComponentRequest componentRequest, WebjetComponentInterface bean) {
        PageParams pageParams = new PageParams(componentRequest.getParameters());
        setParametersToBean(bean, pageParams);
    }

    /**
     * Nastavenie na FE zadanych hodnot do beanu
     * @param bean
     * @param pageParams
     */
    public static void setParametersToBean(WebjetComponentInterface bean, PageParams pageParams) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        for (String paramName : pageParams.getParamNames()) {
            PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor(targetClass, paramName);
            if (propertyDescriptor == null) {
                continue;
            }
            Method writeMethod = propertyDescriptor.getWriteMethod();
            Object value = null;
            try {
                java.lang.reflect.Field field = getDeclaredFiledRecursive(targetClass, paramName);
                DataTableColumn annotation = null;
                if (field.isAnnotationPresent(DataTableColumn.class)) {
                    annotation = field.getAnnotation(DataTableColumn.class);
                }

                value = retypeValue(propertyDescriptor.getPropertyType(), pageParams.getValue(paramName, ""), field, annotation);
                writeMethod.invoke(bean, value);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException | IllegalArgumentException e) {
                sk.iway.iwcm.Logger.error(e);
            }
        }
    }

    /**
     * Method will try get Field from class by given fieldName. If field is not found in class, method will try find field in super classes (and in the superClass of superClass ...).
     * If we reach last superClass and field is not found, method will throw NoSuchFieldException.
     *
     * @param initialClass
     * @param fieldName
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchFieldException - if error is catch and there is still superClass do nothing, else return new NoSuchFieldException
     */
    private static java.lang.reflect.Field getDeclaredFiledRecursive(Class<?> initialClass, String fieldName) throws NoSuchFieldException {
        java.lang.reflect.Field field = null;

        int failsafe=0;
        Class<?> targetClass = initialClass;
        while (targetClass != null && failsafe++<15) {
            try {
                field = targetClass.getDeclaredField(fieldName);
                if(field != null) return field;
            } catch (NoSuchFieldException e) {

            }
            // Field not found in current class, continue to superclass
            targetClass = targetClass.getSuperclass();
        }

       throw new NoSuchFieldException("Field " + fieldName + " not found in class " + initialClass.getName() + " or in super classes");
    }

    /**
     * Metoda pre pretypovanie hodnot
     * @param parameterType Class<?>
     * @param value String
     * @return Object
     */
    private static Object retypeValue(Class<?> parameterType, String value, java.lang.reflect.Field field, DataTableColumn annotation) {
        if (value == null || "null".equals(value)) {
            return null;
        }

        try {
            if (parameterType.getTypeName().equalsIgnoreCase("java.math.BigDecimal")) {
                if (Tools.isEmpty(value)) return null;
                return new BigDecimal(value);
            }

            if (parameterType.getTypeName().equalsIgnoreCase("java.lang.Boolean") || parameterType.getTypeName().equalsIgnoreCase("boolean")) {
                if (Tools.isEmpty(value)) return Boolean.FALSE;
                return Boolean.valueOf(value);
            }

            if (parameterType.getTypeName().equalsIgnoreCase("java.lang.Float") || parameterType.getTypeName().equalsIgnoreCase("float")) {
                if (Tools.isEmpty(value)) return Float.valueOf(0);
                return Float.valueOf(value);
            }

            if (parameterType.getTypeName().equalsIgnoreCase("java.lang.Double") || parameterType.getTypeName().equalsIgnoreCase("double")) {
                if (Tools.isEmpty(value)) return Double.valueOf(0);
                return Double.valueOf(value);
            }

            if (parameterType.getTypeName().equalsIgnoreCase("java.lang.Integer") || parameterType.getTypeName().equalsIgnoreCase("int")) {
                if (Tools.isEmpty(value)) return Integer.valueOf(0);
                if (value.contains(".")) {
                    value = value.substring(0, value.indexOf("."));
                }
                return Integer.valueOf(value);
            }

            if (parameterType.getTypeName().equalsIgnoreCase("java.lang.Integer[]") || parameterType.getTypeName().equalsIgnoreCase("int[]")) {
                //get tokens, it's probably + separated list
                int[] tokens = Tools.getTokensInt(value, "+");
                if (parameterType.getTypeName().equalsIgnoreCase("int[]")) {
                    return tokens;
                }
                Integer[] result = Arrays.stream( tokens ).boxed().toArray( Integer[]::new );
                return result;
            }

            if (parameterType.getTypeName().equalsIgnoreCase("java.lang.Long[]") || parameterType.getTypeName().equalsIgnoreCase("long[]")) {
                //get tokens, it's probably + separated list
                long[] tokens = Tools.getTokensLong(value, "+");
                if (parameterType.getTypeName().equalsIgnoreCase("long[]")) {
                    return tokens;
                }
                Long[] result = Arrays.stream( tokens ).boxed().toArray( Long[]::new );
                return result;
            }

            if (parameterType.getTypeName().equalsIgnoreCase("java.lang.String[]")) {
                //get tokens, it's probably + separated list
                String[] tokens = Tools.getTokens(value, "+");
                return tokens;
            }

            if (parameterType.getTypeName().equalsIgnoreCase("java.util.Date")) {
                DateConverter dateConverter = new DateConverter();
                return dateConverter.convert(value);
            }

            if (parameterType.getTypeName().equalsIgnoreCase("sk.iway.iwcm.doc.GroupDetails")) {
                int groupId = Tools.getIntValue(value, 0);
                if (groupId == 0) {
                    return null;
                }
                GroupDetails group = GroupsDB.getInstance().getGroup(groupId);
                return group;
            }

            if (parameterType.getTypeName().equalsIgnoreCase("sk.iway.iwcm.doc.DocDetails") || parameterType.getTypeName().equalsIgnoreCase("sk.iway.iwcm.admin.layout.DocDetailsDto")) {
                int docId = Tools.getIntValue(value, 0);
                if (docId == 0 || docId == -1) {
                    return null;
                }
                DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
                return doc;
            }

            if (parameterType.getTypeName().equalsIgnoreCase("java.util.List")) {
                if (annotation == null) {
                    return null;
                }
                String className = annotation.className();
                if (className == null) return null;

                if (className.contains("dt-tree-page")) {
                    List<DocDetails> docs = new ArrayList<>();
                    String[] docIds = value.split("[+,]");
                    for (String docId : docIds) {
                        DocDetails doc = DocDB.getInstance().getBasicDocDetails(Tools.getIntValue(docId, 0), false);
                        if (doc != null) {
                            docs.add(doc);
                        }
                    }
                    return docs;
                }

                if (className.contains("dt-tree-group")) {
                    List<GroupDetails> groups = new ArrayList<>();
                    String[] groupIds = value.split("[+,]");
                    for (String groupId : groupIds) {
                        GroupDetails group = GroupsDB.getInstance().getGroup(Tools.getIntValue(groupId, 0));
                        if (group != null) {
                            groups.add(group);
                        }
                    }
                    return groups;
                }
            }

            return value;
        } catch (Exception ex) {
            Logger.error(ComponentsService.class, ex);
        }
        return null;
    }

    /**
     * Metoda pre ziskanie triedy webjet komponenty zo springovej komponenty
     * @param applicationContext ApplicationContext
     * @param className String
     * @return WebjetComponentInterface
     */
    private static WebjetComponentInterface getWebjetComponentBean(ApplicationContext applicationContext, String className) {
        WebjetComponentInterface bean = null;
        try {
            bean = applicationContext.getBean(className, WebjetComponentInterface.class);
        }
        catch (Exception e) {
            Logger.error(ComponentsService.class, "exception", e);
        }

        if (bean == null) {
            return null;
        }

        if (Advised.class.isAssignableFrom(bean.getClass())) {
            Advised advised = (Advised) bean;
            TargetSource targetSource = advised.getTargetSource();
            try {
                bean = (WebjetComponentInterface) targetSource.getTarget();
            } catch (Exception e) {
                sk.iway.iwcm.Logger.error(e);
            }
        }

        return bean;
    }
}