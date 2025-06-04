package sk.iway.iwcm.system.datatable;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import sk.iway.iwcm.Tools;

public class DatatablePageImpl<T> extends PageImpl<T> {

    private static final long serialVersionUID = 1L;

    private Map<String, List<OptionDto>> options = null; //NOSONAR

    private List<NotifyBean> notify;

    public DatatablePageImpl(List<T> content) {
        //we can't use super(content) because Pageable.unpaged() throws exception on Json serialization
        super(content, PageRequest.of(0, content.size(), Sort.unsorted()), content.size());
    }

    public DatatablePageImpl(Page<T> page) {
        super(page.getContent(), page.getPageable(), page.getTotalElements());
        if (page instanceof DatatablePageImpl) {
            //ak uz je to PageImpl tak prenes options, mozno uz su nejake nastavene
            DatatablePageImpl<T> pageImpl = (DatatablePageImpl<T>)page;
            this.options = pageImpl.options;
        }
    }

    private List<OptionDto> getFieldOptions(String field) {
        if (options == null) {
            options = new Hashtable<>();
        }
        List<OptionDto> fieldOptions = options.get(field);
        if (fieldOptions == null) {
            fieldOptions = new ArrayList<>();
            options.put(field, fieldOptions);
        }
        return fieldOptions;
    }

    /**
     * Add to output OptionDto value
     * @param field - name od DT field
     * @param label - value (text) that will be shown
     * @param value - value that will be used on BE
     */
    public void addDefaultOption(String field, String label, String value) {
        addOption(field, label, value, null);
    }

    /**
     * Prida do vystupu OptionDto
     * @param field - meno DT fieldu
     * @param label
     * @param value
     */
    public void addOption(String field, String label, String value, Object original) {
        List<OptionDto> fieldOptions = getFieldOptions(field);
        fieldOptions.add(new OptionDto(label, value, original));
    }

    /**
     * Prida do vystupu OptionDto skonvertovane so zadaneho listu objektov
     * @param field - meno DT fieldu
     * @param options - list objektov ktore pridavame
     * @param labelProperty - nazov label property v options liste
     * @param valueProperty - nazov value property v options liste
     */
    @SuppressWarnings("rawtypes")
    public void addOptions(String field, List options, String labelProperty, String valueProperty, boolean includeOriginalObject) {
        List<OptionDto> fieldOptions = getFieldOptions(field);
        for (Object o : options) {
            BeanWrapperImpl bw = new BeanWrapperImpl(o);

            String label;
            String value;

            if(Tools.isEmpty(labelProperty) && Tools.isEmpty(valueProperty)) {
                label = (String)o;
                value = (String)o;
            } else {
                label = String.valueOf(bw.getPropertyValue(labelProperty));
                value = String.valueOf(bw.getPropertyValue(valueProperty));
            }

            Object original;
            if (includeOriginalObject) original = o;
            else original = null;
            fieldOptions.add(new OptionDto(label, value, original));
        }
    }

    public void addOptions(String field, List<String> options) {
        for(String option : options)
            addOption(field, option, option, null);
    }

    /**
     * Add options from map type id:label, id will be converted to string and used as value for option
     * @param field
     * @param options
     */
    public void addOptions(String field, Map<? extends Number, String> options) {
        for(Map.Entry<? extends Number,String> entry : options.entrySet())
            addOption(field, entry.getValue(), String.valueOf(entry.getKey()), null);
    }

    public Map<String, List<OptionDto>> getOptions() {
        return options;
    }

    public List<NotifyBean> getNotify() {
        return notify;
    }

    public void setNotify(List<NotifyBean> notify) {
        this.notify = notify;
    }
}
