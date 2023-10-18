package sk.iway.iwcm.doc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Trieda po jej prepisani umoznuje nahravat dodatocne stlpce z databazy pre documents/groups tabulky
 * Povodne som uvazoval spravit to cez eventy, ale kvoli performance som sa rozhodol takto (skaredo)
 */
public class DataAccessHelper {

   /**
    * Metoda je volana pri citani udajov z tabulky documents, umoznuje donacitat dalsie/specificke data
    * Odporucame kvoli dalsiemu rozsirovaniu si spravit separe triedu a pri rozsireni tejto len volat
    * metodu zo separe triedy, napr. DataAccessHelperMyProjecy.docLoadData(doc, rs);
    * @param rs
    * @param doc
    */
   public static void docLoadData(ResultSet rs, DocDetails doc) {
      DataAccessHelperWj9.docLoadData(rs, doc);
   }

   /**
    * Vrati zoznam DODATOCNYCH stlpcov pre SQL select/insert/update do documents tabulky, priklad:
    * [show_in_navbar, show_in_sitemap, logged_show_in_navbar]
    * @return
    */
    public static String[] getDocFields() {
      return DataAccessHelperWj9.getDocFields();
   }

   /**
    * Metoda je volana pri citani udajov z tabulky groups, umoznuje donacitat dalsie/specificke data
    * @param rs
    * @param group
    */
   public static void groupLoadData(ResultSet rs, GroupDetails group) {
      DataAccessHelperWj9.groupLoadData(rs, group);
   }

   /**
    * Vrati zoznam DODATOCNYCH stlpcov pre SQL insert/update do groups tabulky, priklad:
    * [show_in_navbar, show_in_sitemap, logged_show_in_navbar]
    * @return
    */
   public static String[] getGroupFields() {
      return DataAccessHelperWj9.getGroupFields();
   }

   /**
    * Nastavi do SQL prepared statementu hodnoty z group objektu pre zapis do databazy
    * @param ps
    * @param group
    * @param psCounter - pocitadlo poradia parametrov
    * @return
    */
   public static int setGroupPreparedStatement(PreparedStatement ps, GroupDetails group, int psCounter) throws SQLException {
      return DataAccessHelperWj9.setGroupPreparedStatement(ps, group, psCounter);
   }

}
