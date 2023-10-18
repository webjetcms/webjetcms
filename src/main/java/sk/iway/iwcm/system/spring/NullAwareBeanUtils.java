package sk.iway.iwcm.system.spring;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;

import java.util.*;

public class NullAwareBeanUtils {

    //privatny konstruktor, toto je utility trieda a neinstancuje sa
    private NullAwareBeanUtils() {}

    public static void copyProperties(Object source, Object target) throws BeansException {
        List<String> alwaysCopy = null;
        copyProperties(source, target, alwaysCopy, (String[]) null);
    }

    /**
     * Copies properties from one object to another
     * @param source
     * @destination
     * @return
     */
    public static void copyProperties(Object source, Object destination, String... ignoreProperties){
        List<String> alwaysCopy = null;
        copyProperties(source, destination, alwaysCopy, ignoreProperties);
    }

    /**
     * Skopiruje atributy zo source objektu do destination
     * @param source
     * @param destination
     * @param alwaysCopyProperties - zoznam properties, ktore sa skopiruju aj ked su Null (typicky Date property)
     * @param ignoreProperties - zoznam properties, ktore sa ignoruju a nebudu sa kopirovat
     */
    public static void copyProperties(Object source, Object destination, List<String> alwaysCopyProperties, String... ignoreProperties){
        List<String> ignore = new ArrayList<>();
        if (ignoreProperties != null && ignoreProperties.length>0) {
            ignore.addAll(Arrays.asList(ignoreProperties));
        }
        ignore.addAll(getNullPropertyNames(source));

        if (alwaysCopyProperties!=null && alwaysCopyProperties.isEmpty()==false) {
            for (String property : alwaysCopyProperties) {
                ignore.remove(property);
            }
        }

        BeanUtils.copyProperties(source, destination, ignore.toArray(new String[0]));
    }

    /**
     * Returns an {@link Collection} of null properties of an object
     * @param source
     * @return
     */
    private static Collection<String> getNullPropertyNames (Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for(java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = null;
            try {
                srcValue = src.getPropertyValue(pd.getName());
            } catch (Exception ex) {
                //property sa neda ziskat, asi nema getter, preskocime ju
            }
            if (srcValue == null) emptyNames.add(pd.getName());
        }

        return emptyNames;
    }
}
