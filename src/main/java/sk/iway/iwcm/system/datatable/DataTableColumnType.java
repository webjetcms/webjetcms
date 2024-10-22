package sk.iway.iwcm.system.datatable;

public enum DataTableColumnType {
    /**
     * Typ pola ID
     */
    ID,
    /**
     * Typ pola TEXT
     */
    TEXT,
    /**
     * Typ pola TEXT_NUMBER
     */
    TEXT_NUMBER,
    /**
     * Typ pola TEXT_NUMBER_FORMAT_NONE
     */
    TEXT_NUMBER_INVISIBLE,
    /**
     * Typ pola QUILL
     */
    QUILL,
    /**
     * Typ pola TEXTAREA
     */
    TEXTAREA,
    /**
     * Typ pola DATE
     */
    DATE,
    /**
     * Typ pola DATETIME
     */
    DATETIME,
    /**
     * Typ pola DISABLED
     */
    DISABLED,
    /**
     * Typ pola OPEN_EDITOR
     */
    OPEN_EDITOR,
    /**
     * Typ pola GALLERY_IMAGE
     */
    GALLERY_IMAGE,
    /**
     * Typ pola SELECT
     */
    SELECT,
    /**
     * Typ pola checkbox true/false
     */
    BOOLEAN,
    /**
     * Checkbox with title on right side
     */
    BOOLEAN_TEXT,
    /**
     * Typ pola checkbox
     */
    CHECKBOX,
    /**
     * JSON pole
     */
    JSON,
    /**
     * vnorena datatable
     */
    DATATABLE,
    /**
     * vyber odkazu na subor/web stranku - elfinder
     */
    ELFINDER,
    /**
     * Klasicke cislo
     */
    NUMBER,
    /**
     * WYSIWYG editor, defaultne ckeditor
     */
    WYSIWYG,
    /**
     * stromova struktura - jstree.com
     */
    JSTREE,
    /**
     * Skryte pole
     */
    HIDDEN,
    /**
     * Radio button
     */
    RADIO,
    /**
     * Password
     */
    PASSWORD,
    /**
     * Select s vyberom viacerych moznosti
     */
    MULTISELECT,
    /**
     * Typ poľa DATE s možnosťou výberu iba času pomocu hodín a minút
     */
    TIME_HM,
    /**
     * Typ poľa DATE s možnosťou výberu iba času pomocu hodín, minút a sekúnd
     */
    TIME_HMS,
    /**
     * Atributy stranky
     */
    ATTRS,
    /**
     * input type="color"
     */
    COLOR,
    /**
     * IFRAME type, String value will be used as URL, can contains placeholders
     */
    IFRAME,
    /**
     * it will encode/decode value to/from base64
     */
    BASE64,
    /**
     * Static text (no input field)
     */
    STATIC_TEXT,
}
