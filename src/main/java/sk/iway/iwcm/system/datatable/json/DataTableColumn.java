package sk.iway.iwcm.system.datatable.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.*;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.DataTableColumnsFactory;

import javax.persistence.Transient;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

/**
 * Trieda pre generovanie JSONu pre DataTable {@see https://datatables.net/} z
 * anotacie {@link sk.iway.iwcm.system.datatable.annotations.DataTableColumn}
 * nad poliami objektu. Trieda je priamo mapovatelna pomocou
 * {@link com.fasterxml.jackson.databind.ObjectMapper}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class DataTableColumn {
    private String data;
    private String name;
    private String title;
    @JsonIgnore
    private String titleKeyOriginal; //originalny prekladovy kluc title

    private String defaultContent;

    private String className;
    private String renderFormat;
    private String renderFormatLinkTemplate;
    private String renderFormatPrefix;

    private DataTableColumnEditor editor;
    private Boolean visible;
    private Boolean hidden;
    private Boolean hiddenEditor;
    private Boolean filter;

    private String sortAfter;
    private String perms;

    private Boolean array;

    private Boolean orderable;

    @SuppressWarnings("rawtypes")
    public DataTableColumn(Class controller, Field field, String fieldPrefix) {
        String fieldPrefixNotNull = fieldPrefix;
        if (fieldPrefixNotNull == null)
            fieldPrefixNotNull = "";
        name = fieldPrefixNotNull + field.getName();
        data = fieldPrefixNotNull + field.getName();

        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes==null) return;

        HttpServletRequest request = requestAttributes.getRequest();
        Prop prop = Prop.getInstance(request);

        setPropertiesFromFieldType(field);
        setPropertiesFromAnnotation(controller, field, prop);
        setEditorPropertiesFromField(field);

        setFinalProperties(field);
        setCellNotEditable(field);
    }

    private void setPropertiesFromFieldType(Field field) {
        // DATE
        if (field.getType().isAssignableFrom(Date.class) || field.getType().isAssignableFrom(java.sql.Date.class)) {
            renderFormat = "dt-format-date-time";
            addClassName("dt-style-date");
        }
        if (field.getType().isArray()) array = Boolean.TRUE;
        else array = Boolean.FALSE;
    }

    /**
     * Metoda nastavi JSON podla anotacie
     * {@link sk.iway.iwcm.system.datatable.annotations.DataTableColumn} na danom
     * poli. Anotacie maju rovnaky nazov a format ako generovany JSON.
     *
     * @param field - reflection field, ktore ma nastavenu anotaciu
     *              {@link sk.iway.iwcm.system.datatable.annotations.DataTableColumn}
     */
    @SuppressWarnings("rawtypes")
    private void setPropertiesFromAnnotation(Class controller, Field field, Prop prop) {
        sk.iway.iwcm.system.datatable.annotations.DataTableColumn annotation = field.getAnnotation(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class);
        if (annotation == null) {
            return;
        }

        DataTableColumnType[] inputType = annotation.inputType();
        if (inputType.length > 0) {
            for (DataTableColumnType type : inputType) {
                setPropertiesFromType(type, annotation);
            }
        }

        if (Tools.isNotEmpty(annotation.data())) {
            data = annotation.data();
        }

        if (Tools.isNotEmpty(annotation.name())) {
            name = annotation.name();
        }

        if (Tools.isNotEmpty(annotation.title())) {
            String[] titleArr = annotation.title().split(";");

            titleKeyOriginal = titleArr[0];

            if(titleArr.length > 1)
                title = prop.getTextWithParams(titleArr[0], Arrays.copyOfRange(titleArr, 1, titleArr.length));
            else
                title = prop.getText(annotation.title());

            // skus implementovat zapis z pug suboru
            title = DataTableColumnsFactory.translate(title);

            if (Tools.isEmpty(title) || "&nbsp;".equals(title)) addClassName("empty-title");
        } else {
            String titleKey = "components." + toLowerUnderscore(controller.getSimpleName()) + "." + toLowerUnderscore(field.getName());
            title = prop.getText(titleKey);
            if (titleKey.equals(title)) title = Tools.replace(toLowerUnderscore(field.getName()), "_", " ");
        }

        if (hidden == null && (Tools.isEmpty(title) || "&nbsp;".equals(title))) {
            // ak nie je nastaveny titulok, tak stlpec nezobraz v DB, asi sa jedna o nejaky
            // boolean atribut pre editor
            hidden = Boolean.TRUE;
        }

        String[] strings = annotation.defaultContent();
        if (strings != null && strings.length > 0) {
            defaultContent = strings[0];
        }

        String annotationClassName = annotation.className();
        if (Tools.isNotEmpty(annotationClassName)) {
            if (annotationClassName.startsWith("!")) className = annotationClassName;
            else addClassName(annotationClassName);
        }

        if (Tools.isNotEmpty(annotation.renderFormat())) {
            renderFormat = annotation.renderFormat();
        }

        if (Tools.isNotEmpty(annotation.renderFormatLinkTemplate())) {
            renderFormatLinkTemplate = annotation.renderFormatLinkTemplate();
        }

        if (Tools.isNotEmpty(annotation.renderFormatPrefix())) {
            renderFormatPrefix = annotation.renderFormatPrefix();
        }

        boolean[] _visible = annotation.visible();
        if (_visible.length > 0) {
            this.visible = _visible[0];
        }

        boolean[] _hidden = annotation.hidden();
        if (_hidden.length > 0) {
            this.hidden = _hidden[0];
        }

        boolean[] _hiddenEditor = annotation.hiddenEditor();
        if (_hiddenEditor.length > 0) {
            this.hiddenEditor = _hiddenEditor[0];
        }

        boolean[] _filter = annotation.filter();
        if (_filter.length > 0) {
            this.filter = _filter[0];
        } else if (field.getAnnotation(Transient.class)!=null) {
            if ("userFullName".equals(field.getName())) {
                //userFullName vieme standardne vyhladavat v DatatableRestControllerV2.addSpecSearchUserFullName
            } else {
                //ak je entita @Transient asi nie je v DB, filter pre istotu vypneme, ak treba musi sa zapnut manualne nastavenim filter: true
                this.filter = Boolean.FALSE;
            }
        } else if (className!=null && className.contains("todo")) {
            //ak ma className todo tiez nezobraz filter, na 99% nefunguje
            this.filter = Boolean.FALSE;
        }

        String tab = annotation.tab();
        if (Tools.isNotEmpty(tab)) {
            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setTab(tab);
        }

        sortAfter = annotation.sortAfter();
        perms = annotation.perms();

        String defaultValue = annotation.defaultValue();
        if (Tools.isNotEmpty(defaultValue)) {
            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            if ("{currentDomain}".equals(defaultValue)) {
                if (Constants.getBoolean("multiDomainEnabled")) {
                    RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
                    defaultValue = rb.getDomain();
                } else {
                    defaultValue = "";
                }
            } else if ("{currentDate}".equals(defaultValue)) {
                defaultValue = Tools.formatDate(Tools.getNow());
            } else if ("{currentDateTimeSeconds}".equals(defaultValue)) {
                defaultValue = Tools.formatDateTimeSeconds(Tools.getNow());
            } else if ("{currentTime}".equals(defaultValue)) {
                defaultValue = Tools.formatTime(Tools.getNow());
            }
            editor.setDef(defaultValue);
        }

        boolean[] _orderable = annotation.orderable();
        if (_orderable.length > 0) {
            this.orderable = _orderable[0];
        } else if (Boolean.FALSE.equals(this.filter)) {
            //ak je vypnuty filter a nenastavim orderable, tak predpokladam, ze nema byt ani orderable
            this.orderable = false;
        }
    }

    private void setEditorPropertiesFromField(Field field) {
        if (editor == null) {
            editor = new DataTableColumnEditor();
        }

        editor.setPropertiesFromField(field);

        if (editor.isRequired()) addClassName("required");

        if (Tools.isEmpty(editor.getMessage()) && Tools.isNotEmpty(titleKeyOriginal) && "&nbsp".equals(titleKeyOriginal)==false && "&nbsp;".equals(titleKeyOriginal)==false) {
            String key = titleKeyOriginal+".tooltip";
            if (key.startsWith("[[#{")) {
                key = Tools.replace(key, "[[#{", "");
                key = Tools.replace(key, "}]]", "");
            }
            String translated = DataTableColumnsFactory.translate(key);
            if (Tools.isNotEmpty(translated) && key.equals(translated)==false) {
                editor.setMessage(translated);
            }
        }

        if (editor.isEmpty()) {
            this.editor = null;
        }
    }

    /**
     * Metoda nastavi do JSONu atributy, podla toho, ako je natavena anotacia
     * inputType {@link DataTableColumnType}
     *
     * @param dataTableColumnType - {@link DataTableColumnType} - nastaveny v
     *                            anotacii inputType
     */
    private void setPropertiesFromType(DataTableColumnType dataTableColumnType, sk.iway.iwcm.system.datatable.annotations.DataTableColumn annotation) {
        if (dataTableColumnType == DataTableColumnType.ID) {
            if (Tools.isEmpty(data)) {
                data = "id";
            }
            defaultContent = "";
            addClassName("dt-select-td");
            renderFormat = "dt-format-selector";
            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("hidden");
        }

        if (dataTableColumnType == DataTableColumnType.TEXT) {
            renderFormat = "dt-format-text";
            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("text");
        }

        if (dataTableColumnType == DataTableColumnType.TEXT_NUMBER || dataTableColumnType == DataTableColumnType.NUMBER) {
            renderFormat = "dt-format-number";
            if (dataTableColumnType == DataTableColumnType.TEXT_NUMBER) renderFormat+="--text";
            addClassName("dt-style-number");

            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("text");
            HashMap<String, String> attrs = new HashMap<>();
            attrs.put("type", "number");
            editor.setAttr(attrs);
        }

        if (dataTableColumnType == DataTableColumnType.TEXT_NUMBER_INVISIBLE) {
            renderFormat = "dt-format-none";
            visible = false;

            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("text");
            HashMap<String, String> attrs = new HashMap<>();
            attrs.put("type", "number");
            editor.setAttr(attrs);
        }

        if (dataTableColumnType == DataTableColumnType.QUILL) {
            renderFormat = "dt-format-text";
            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("quill");
        }

        if (dataTableColumnType == DataTableColumnType.TEXTAREA) {
            renderFormat = "dt-format-text-wrap";
            addClassName("dt-style-text-wrap");

            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("textarea");

            if (annotation.className()!=null && annotation.className().contains("show-html")) editor.addAttr("entityDecode", "false");
        }

        if (dataTableColumnType == DataTableColumnType.DATE) {
            renderFormat = "dt-format-date";
            addClassName("dt-style-date");

            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("date");
        }

        if (dataTableColumnType == DataTableColumnType.DATETIME) {
            renderFormat = "dt-format-date-time";
            addClassName("dt-style-date");

            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("datetime");
        }

        if (dataTableColumnType == DataTableColumnType.TIME_HM) {
            renderFormat = "dt-format-time-hm";
            addClassName("dt-style-date");

            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("timehm");
        }

        if (dataTableColumnType == DataTableColumnType.TIME_HMS) {
            renderFormat = "dt-format-time-hms";
            addClassName("dt-style-date");

            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("timehms");
        }

        if (dataTableColumnType == DataTableColumnType.OPEN_EDITOR) {
            addClassName("dt-row-edit");
            renderFormat = "dt-format-text";
            renderFormatLinkTemplate = "javascript:;";
            renderFormatPrefix = "<i class=\"ti ti-pencil\"></i> ";

            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("text");
        }

        if (dataTableColumnType == DataTableColumnType.DISABLED) {
            if (editor == null) {
                editor = new DataTableColumnEditor();
            }

            editor.addAttr("disabled", "disabled");
        }

        if (dataTableColumnType == DataTableColumnType.GALLERY_IMAGE) {
            title = "";
            hidden = Boolean.FALSE;
        }

        if (dataTableColumnType == DataTableColumnType.SELECT || dataTableColumnType == DataTableColumnType.MULTISELECT) {
            renderFormat = "dt-format-select";
            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("select");

            if (dataTableColumnType == DataTableColumnType.MULTISELECT) {
                editor.setMultiple(Boolean.TRUE);
                //editor.addAttr("multiple", "multiple");
            }

            if (editor.getOptions() == null) {
                editor.setOptions(new ArrayList<>());
            }
        }

        if (dataTableColumnType == DataTableColumnType.BOOLEAN) {
            renderFormat = "dt-format-boolean-true";
            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("checkbox");
        }

        if (dataTableColumnType == DataTableColumnType.CHECKBOX) {
            renderFormat = "dt-format-checkbox";
            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("checkbox");
        }

        if (dataTableColumnType == DataTableColumnType.JSON) {
            renderFormat = "dt-format-json";
            addClassName("dt-style-json");

            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("json");
        }

        if (dataTableColumnType == DataTableColumnType.DATATABLE) {
            renderFormat = "dt-format-datatable";

            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("datatable");
        }

        if (dataTableColumnType == DataTableColumnType.ELFINDER) {
            renderFormat = "dt-format-elfinder";

            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("elfinder");
        }

        if (dataTableColumnType == DataTableColumnType.WYSIWYG) {
            renderFormat = "dt-format-wysiwyg";
            addClassName("dt-style-wysiwyg");
            addClassName("cell-not-editable");

            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("wysiwyg");
        }

        if (dataTableColumnType == DataTableColumnType.JSTREE) {
            renderFormat = "dt-format-jstree";
            addClassName("dt-style-jstree");

            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("jsTree");
        }

        if (dataTableColumnType == DataTableColumnType.HIDDEN) {
            renderFormat = "dt-format-hidden";
            hidden = Boolean.TRUE;
            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("hidden");
        }

        if (dataTableColumnType == DataTableColumnType.RADIO) {
            renderFormat = "dt-format-radio";
            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("radio");
        }

        if (dataTableColumnType == DataTableColumnType.PASSWORD) {
            renderFormat = "dt-format-text";
            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("password");
            editor.addAttr("autocomplete", "off");
        }

        if (dataTableColumnType == DataTableColumnType.ATTRS) {
            renderFormat = "dt-format-attrs";
            addClassName("dt-style-attrs");

            if (editor == null) {
                editor = new DataTableColumnEditor();
            }
            editor.setType("attrs");
        }
    }

    private void setFinalProperties(Field field) {
        sk.iway.iwcm.system.datatable.annotations.DataTableColumn annotation = field.getAnnotation(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class);
        if (annotation == null) {
            return;
        }
        DataTableColumnType[] inputType = annotation.inputType();
        if (inputType.length > 0) {
            DataTableColumnType dataTableColumnType = inputType[0];
            if (dataTableColumnType == DataTableColumnType.DATATABLE) {
                try {
                    String attrName = "data-dt-field-dt-columns";
                    String classNameAttr = editor.getAttr().get(attrName);
                    String json = new DataTableColumnsFactory(classNameAttr).getColumnsJson();
                    editor.addAttr(attrName, json);
                } catch (Exception e) {
                    Logger.error(DataTableColumn.class, e);
                }
            }
        }

    }

    /**
     * Nastavi className na cell-not-editable pre needitovatelne bunky (pre editaciu danej bunky)
     * @param field
     */
    private void setCellNotEditable(Field field) {
        boolean notEditable = false;
        String columnType = "";
        if (editor != null && editor.getType()!=null) columnType = editor.getType();
        if (hiddenEditor!=null && hiddenEditor.booleanValue()==true) notEditable = true;
        else if ("hidden".equals(columnType)) {
            notEditable = true;
        }
        else if (editor != null && editor.getAttr()!=null && ("disabled".equals(editor.getAttr().get("disabled")) || "true".equals(editor.getAttr().get("disabled")))) notEditable = true;

        if (notEditable) {
            addClassName("cell-not-editable");
        }
    }

    private String toLowerUnderscore(String str) {
        String underscored = str.replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase();
        if (underscored.endsWith("_dto")) underscored = underscored.substring(0, underscored.length()-4);
        if (underscored.endsWith("_bean")) underscored = underscored.substring(0, underscored.length()-5);
        if (underscored.endsWith("_entity")) underscored = underscored.substring(0, underscored.length()-7);

        return underscored;
    }

    private void addClassName(String addClassName) {
        if (Tools.isEmpty(addClassName)) return;
        if (Tools.isEmpty(className)) className = addClassName.trim();
        else className += " "+addClassName.trim();
    }

}