package sk.iway.iwcm.components.enumerations.rest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

public class EnumerationService {

    private EnumerationService() {}

    private static final String DELETED_PREFIX_KEY = "enum_type.deleted_type_mark.js";
    private static final String HIDDEN = "hidden";
    private static final String STRING_1 = "string1";

    private static class EnumBasic {
        private Long id;
        private String name;
        private Boolean hidden;
    
        public EnumBasic(Long id, String name, Boolean hidden) {
            this.id = id;
            this.name = name;
            this.hidden = hidden;
        }
    
        @SuppressWarnings("unused")
        public Long getId() { return id; } //its is used in page.addOptions
        public String getName() { return name; }
        public Boolean getHidden() { return hidden; }
        public void setName(String name) { this.name = name; }
    }

    public static void prepareEnumTypesOptions(DatatablePageImpl<?> page, Prop prop) {
        List<EnumBasic> enumTypes = new ArrayList<>();
        String prefixForHidden = prop.getText(DELETED_PREFIX_KEY);
        String sql = "SELECT enumeration_type_id, name, hidden FROM enumeration_type";
		new ComplexQuery().setSql(sql).list(new Mapper<EnumBasic>() {
			@Override
			public EnumBasic map(ResultSet rs) throws SQLException {
                EnumBasic enumType = new EnumBasic(rs.getLong("enumeration_type_id"), rs.getString("name"), rs.getBoolean(HIDDEN));

                if(Tools.isTrue(enumType.getHidden()))
                    enumType.setName(prefixForHidden + enumType.getName());
                    
                enumTypes.add(enumType);

				return null;
			}
		});

        page.addDefaultOption("editorFields.childEnumTypeId", "", "-1");
        page.addOptions("editorFields.childEnumTypeId", enumTypes, "name", "id", false);
    }

    public static void prepareEnumDataByTypeOptions(DatatablePageImpl<EnumerationDataBean> page, Prop prop, Integer enumTypeId) {
        List<EnumBasic> enumDatasByType = new ArrayList<>();
        String prefixForHidden = prop.getText(DELETED_PREFIX_KEY);
        String sql = "SELECT enumeration_data_id, string1, hidden FROM enumeration_data WHERE enumeration_type_id=?";
        new ComplexQuery().setSql(sql).setParams(enumTypeId).list(new Mapper<EnumBasic>() {
			@Override
			public EnumBasic map(ResultSet rs) throws SQLException {
                EnumBasic enumType = new EnumBasic(rs.getLong("enumeration_data_id"), rs.getString(STRING_1), rs.getBoolean(HIDDEN));

                if(Tools.isTrue(enumType.getHidden()))
                    enumType.setName(prefixForHidden + enumType.getName());
                    
                enumDatasByType.add(enumType);

				return null;
			}
		});

        page.addDefaultOption("editorFields.parentEnumDataId", "-", "-1");
        page.addOptions("editorFields.parentEnumDataId", enumDatasByType, "name", "id", false);
    }

    public static List<String> getEnumDataAutocomplete(String wantedText, Integer typeId, String name, Prop prop) {
        List<String> enumDataParent = new ArrayList<>();
        String prefixForHidden = prop.getText(DELETED_PREFIX_KEY);
        String sql = "SELECT string1, hidden FROM enumeration_data WHERE string1 LIKE ? AND enumeration_type_id=?";

        List<Object> params = new ArrayList<>();
        params.add('%' + wantedText + '%');
        params.add(typeId);
        
        if(Tools.isNotEmpty(name)) {
            //String to exclude, entity CANT select itself
            sql += " AND NOT string1 = ?";
            params.add(name);
        }

        new ComplexQuery().setSql(sql).setParams(params.toArray()).list(new Mapper<EnumBasic>() {
			@Override
			public EnumBasic map(ResultSet rs) throws SQLException {
                if(rs.getBoolean(HIDDEN) == false) {
                    enumDataParent.add(rs.getString(STRING_1));
                } else {
                    enumDataParent.add(prefixForHidden + rs.getString(STRING_1));
                }

				return null;
			}
		});

        return enumDataParent;
    }

    public static List<String> getEnumTypeAutocomplete(String wantedText, Prop prop) {
        List<String> enumTypeChild = new ArrayList<>();
        String prefixForHidden = prop.getText(DELETED_PREFIX_KEY);
        String sql = "SELECT name, hidden FROM enumeration_type WHERE name LIKE ?";
        new ComplexQuery().setSql(sql).setParams('%' + wantedText + '%').list(new Mapper<EnumBasic>() {
			@Override
			public EnumBasic map(ResultSet rs) throws SQLException {
                if(rs.getBoolean(HIDDEN) == false) {
                    enumTypeChild.add(rs.getString("name"));
                } else {
                    enumTypeChild.add(prefixForHidden + rs.getString("name"));
                }

				return null;
			}
		});

        return enumTypeChild;
    }
}