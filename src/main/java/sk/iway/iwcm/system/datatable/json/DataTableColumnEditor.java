package sk.iway.iwcm.system.datatable.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.beans.BeanWrapperImpl;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.enumerations.EnumerationDataDB;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.system.datatable.DataTableColumnsFactory;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Trieda pre generovanie JSONu pre DataTableEditor {@see https://datatables.net/} z anotacie {@link sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor} nad poliami objektu.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class DataTableColumnEditor {
    private String type;
    private String tab;
    private String label;
    private String message;
    private Map<String, String> attr;
    private List<LabelValue> options;

    String format;
    String wireFormat;
    private Map<String, String> opts;
    private boolean required = false;
    private Boolean multiple = null;
    private String separator = null;

    String def;

    public DataTableColumnEditor() {
    }

    public DataTableColumnEditor(Field field) {
        setPropertiesFromField(field);
    }

    public void setPropertiesFromField(Field field) {
        if (Tools.isEmpty(type)) {
            setPropertiesFromFieldType(field);
        }
        setPropertiesFromAnnotation(field);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return Tools.isEmpty(label) && Tools.isEmpty(message) && Tools.isEmpty(format) && Tools.isEmpty(wireFormat) && Tools.isEmpty(type) && Tools.isEmpty(tab) && (attr == null || attr.isEmpty()) && (opts == null || opts.isEmpty());
    }

    /**
     * Metoda na ziskanie typu z datoveho typu pola
     * @param field
     * @return hodnotu type, podla datoveho typu daneho fieldu {@link Field}
     */
    private void setPropertiesFromFieldType(Field field) {
        // DATE
        if (field.getType().isAssignableFrom(Date.class)
            || field.getType().isAssignableFrom(java.sql.Date.class)) {
                type = "datetime";
                format = "L HH:mm:ss";
                wireFormat = "x";
        }
    }


    /**
     * Metoda nastavi v JSON objekte atribut editor podla anotacie {@link sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor} na danom poli
     * @param field - reflection field objektu, ktore ma nastavenu anotaciu {@link sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor}
     */
    private void setPropertiesFromAnnotation(Field field) {
        sk.iway.iwcm.system.datatable.annotations.DataTableColumn annotation = field.getAnnotation(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class);
        sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor[] editor = annotation.editor();

        if (field.isAnnotationPresent(NotBlank.class) || field.isAnnotationPresent(NotEmpty.class) || field.isAnnotationPresent(NotNull.class)) {
            required = true;
        }

        if (annotation == null || editor == null || editor.length == 0) {
            return;
        }

        String type = editor[0].type();
        if (Tools.isNotEmpty(type)) {
            this.type = type;
        }

        String label = editor[0].label();
        if (Tools.isNotEmpty(label)) {
            this.label = DataTableColumnsFactory.translate(label);
        }

        String message = editor[0].message();
        if (Tools.isNotEmpty(message)) {
            this.message = DataTableColumnsFactory.translate(message);
        }

        String tab = editor[0].tab();
        if (Tools.isNotEmpty(tab)) {
            this.tab = tab;
        }

        String format = editor[0].format();
        if (Tools.isNotEmpty(format)) {
            this.format = format;
        }

        String wireFormat = editor[0].wireFormat();
        if (Tools.isNotEmpty(wireFormat)) {
            this.wireFormat = wireFormat;
        }

        DataTableColumnEditorAttr[] options = editor[0].options();
        if (options.length > 0) {
            if (this.options == null) {
                this.options = new ArrayList<>();
            }

            for (DataTableColumnEditorAttr option : options) {
                //moznost zadat options ako volanie API metody
                //@DataTableColumnEditorAttr(key = "method:sk.iway.basecms.contact.ContactRestController.getCountries", value = "label:value"),
                if (option.key().startsWith("method:")) {
                    try{
                        String fqdn = option.key().substring(option.key().indexOf(":")+1);
                        String className = fqdn.substring(0, fqdn.lastIndexOf("."));
                        String methodName = fqdn.substring(fqdn.lastIndexOf(".") + 1);

                        Class<?> clazz = Class.forName(className);
                        Method method = clazz.getMethod(methodName);
                        Object returned = method.invoke(null);
                        if (returned instanceof List) {
                            @SuppressWarnings("rawtypes")
                            List list = (List)returned;
                            String labelProperty = null;
                            String valueProperty = null;

                            if (Tools.isNotEmpty(option.value())) {
                                int i = option.value().indexOf(":");
                                if (i>0) {
                                    labelProperty = option.value().substring(0, i);
                                    valueProperty = option.value().substring(i+1);
                                }
                            }

                            addOptions(list, labelProperty, valueProperty);
                        }

                        continue;
                    } catch (Exception ex) {
                        Logger.error(DataTableColumnEditor.class, ex);
                    }
                }

                //moznost zadania optionov pomocou odkazu na ciselnik
                //@DataTableColumnEditorAttr(key = "enumeration:Okresne Mestá", value = "string1:string2")
                if (option.key().startsWith("enumeration:")) {
                    try{
                        String name = option.key().substring(option.key().indexOf(":")+1);
                        int id = Tools.getIntValue(name, -1);
                        List<EnumerationDataBean> list;
                        if (id > 0) list = EnumerationDataDB.getEnumerationDataByType(id);
                        else list = EnumerationDataDB.getEnumerationDataByType(name);

                        String labelProperty = null;
                        String valueProperty = null;

                        if (Tools.isNotEmpty(option.value())) {
                            int i = option.value().indexOf(":");
                            if (i>0) {
                                labelProperty = option.value().substring(0, i);
                                valueProperty = option.value().substring(i+1);
                            }
                        }

                        addOptions(list, labelProperty, valueProperty);

                        continue;
                    } catch (Exception ex) {
                        Logger.error(DataTableColumnEditor.class, ex);
                    }
                }

                this.options.add(new LabelValue(DataTableColumnsFactory.translate(option.key()), DataTableColumnsFactory.translate(option.value()) ));
            }
        }

        DataTableColumnEditorAttr[] attrs = editor[0].attr();
        if (attrs.length > 0) {
            if (this.attr == null) {
                this.attr = new HashMap<>();
            }

            for (DataTableColumnEditorAttr attr : attrs) {
                String value = attr.value();
                String key = attr.key();

                if(value.startsWith("constant:")) {
                    String constant = value.substring(value.indexOf(":")+1);
                    if(Tools.isNotEmpty(constant)) {
                        value = Constants.getString(constant);
                    }
                }

                if("data-dt-field-root".equals(key)==true) {
                    //Make sure path starts with slash (do not apply to numbers)
                    int valueNumber = Tools.getIntValue(value, -5);
                    if(Tools.isNotEmpty(value) && "/".equals(value)==false && valueNumber == -5) {
                        if(value.startsWith("/")==false)
                            value = "/" + value;

                        if(value.endsWith("/")==true)
                            value = value.substring(0, value.length()-1);
                    }
                }

                if ("data-dt-field-dt-columns".equals(key)==false) {
                    value = DataTableColumnsFactory.translate(value);
                }

                this.attr.put(key, value);
            }
        }

        DataTableColumnEditorAttr[] opts = editor[0].opts();
        if (opts.length > 0) {
            if (this.opts == null) {
                this.opts = new HashMap<>();
            }

            for (DataTableColumnEditorAttr opt : opts) {
                this.opts.put(opt.key(), DataTableColumnsFactory.translate(opt.value()));
            }
        }

        String separator = editor[0].separator();
        if (Tools.isNotEmpty(separator)) {
            this.separator = separator;
        }
    }

    @SuppressWarnings("rawtypes")
    private void addOptions(List genericOptions, String labelProperty, String valueProperty) {
        for (Object o : genericOptions) {
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

            this.options.add(new LabelValue(label, value));
        }
    }

    public void addAttr(String key, String value) {
        if (attr == null) {
            attr = new HashMap<>();
        }
        attr.put(key, value);
    }
}