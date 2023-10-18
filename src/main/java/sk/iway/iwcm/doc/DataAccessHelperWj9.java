package sk.iway.iwcm.doc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import sk.iway.iwcm.DB;

/**
 * Rozsirenie modelu documents a groups tabulky pre WJ2022
 */
public class DataAccessHelperWj9 {

    /**
     * Metoda je volana pri citani udajov z tabulky documents, umoznuje donacitat
     * dalsie/specificke data
     * Odporucame kvoli dalsiemu rozsirovaniu si spravit separe triedu a pri
     * rozsireni tejto len volat
     * metodu zo separe triedy, napr. DataAccessHelperMyProjecy.docLoadData(doc,
     * rs);
     *
     * @param rs
     * @param doc
     */
    public static void docLoadData(ResultSet rs, DocDetails doc) {
        try {
            // WJ2021 polia
            doc.setTempFieldADocId(rs.getInt("temp_field_a_docid"));
            doc.setTempFieldBDocId(rs.getInt("temp_field_b_docid"));
            doc.setTempFieldCDocId(rs.getInt("temp_field_c_docid"));
            doc.setTempFieldDDocId(rs.getInt("temp_field_d_docid"));

            doc.setShowInNavbar(DB.getBoolean(rs, "show_in_navbar"));
            doc.setShowInSitemap(DB.getBoolean(rs, "show_in_sitemap"));
            doc.setLoggedShowInMenu(DB.getBoolean(rs, "logged_show_in_menu"));
            doc.setLoggedShowInNavbar(DB.getBoolean(rs, "logged_show_in_navbar"));
            doc.setLoggedShowInSitemap(DB.getBoolean(rs, "logged_show_in_sitemap"));

            doc.setUrlInheritGroup(DB.getBoolean(rs, "url_inherit_group"));
            doc.setGenerateUrlFromTitle(DB.getBoolean(rs, "generate_url_from_title"));
            doc.setEditorVirtualPath(rs.getString("editor_virtual_path"));
        } catch (Exception ex) {
            // ignorujeme, asi nebolo v SQL vybere
        }
    }

    /**
     * Vrati zoznam DODATOCNYCH stlpcov pre SQL select/insert/update do documents
     * tabulky, priklad:
     * [show_in_navbar, show_in_sitemap, logged_show_in_navbar]
     *
     * @return
     */
    public static String[] getDocFields() {
        String[] fields = { "temp_field_a_docid", "temp_field_b_docid", "temp_field_c_docid", "temp_field_d_docid",
                "show_in_navbar", "show_in_sitemap", "logged_show_in_menu", "logged_show_in_navbar", "logged_show_in_sitemap",
                "url_inherit_group", "generate_url_from_title", "editor_virtual_path" };
        return fields;
    }

    /**
     * Metoda je volana pri citani udajov z tabulky groups, umoznuje donacitat
     * dalsie/specificke data
     *
     * @param rs
     * @param group
     */
    public static void groupLoadData(ResultSet rs, GroupDetails group) {
        try {
			//WJ2021 polia
			group.setShowInNavbar(DB.getInteger(rs, "show_in_navbar"));
			group.setLoggedShowInNavbar(DB.getInteger(rs, "logged_show_in_navbar"));
			group.setShowInSitemap(DB.getInteger(rs, "show_in_sitemap"));
			group.setLoggedShowInSitemap(DB.getInteger(rs, "logged_show_in_sitemap"));
		} catch (Exception ex) {
			//ignorujeme, asi nebolo v SQL vybere
		}
    }

    /**
     * Vrati zoznam DODATOCNYCH stlpcov pre SQL insert/update do groups tabulky,
     * priklad:
     * [show_in_navbar, show_in_sitemap, logged_show_in_navbar]
     *
     * @return
     */
    public static String[] getGroupFields() {
        String[] fields = {"show_in_navbar", "show_in_sitemap", "logged_show_in_navbar", "logged_show_in_sitemap"};
        return fields;
    }

    /**
     * Nastavi do SQL prepared statementu hodnoty z group objektu pre zapis do
     * databazy
     *
     * @param ps
     * @param group
     * @param psCounter - pocitadlo poradia parametrov
     * @return
     */
    public static int setGroupPreparedStatement(PreparedStatement ps, GroupDetails group, int psCounter) throws SQLException {
        ps.setObject(psCounter++, group.getShowInNavbar());
        ps.setObject(psCounter++, group.getShowInSitemap());
        ps.setObject(psCounter++, group.getLoggedShowInNavbar());
        ps.setObject(psCounter++, group.getLoggedShowInSitemap());
        return psCounter;
    }
}
