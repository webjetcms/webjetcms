package sk.iway.iwcm.common;

import java.util.StringTokenizer;
import java.util.regex.Pattern;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

public class SearchTools {

    // ze ak je (String)output prazdny tak sa tam da tolko charov ...
    public final static String[] checkInputParams = {"html_head", "html_data", "field_a", "field_b", "field_c", "field_d", "field_e", "field_f", "field_g", "field_h", "field_i", "field_j", "field_k", "field_l", "field_o", "field_n", "publish_start", "publish_end", "title", "publish_start_lt","publish_start_gt", "publish_end_gt", "publish_end_lt","temp_id", "perex_place", "keyword"};

    /**
     * Odstrani z HTML kodu riadiace bloky typu !INCLUDE(...)!, !PARAM(...)!
     * @param html
     * @return
     */
    public static String removeCommands(String html)
    {
        if (html != null)
        {
            if (html.contains(")!"))
            {
                Pattern replace = Pattern.compile(
                "(!INCLUDE.*?\\)!)|(!REMAP_PAGE.*?\\)!)|(!PARAMETER.*?\\)!)|(!REQUEST.*?\\)!)|(!LOGGED_USER.*?\\)!)", Pattern.CASE_INSENSITIVE);
                html = replace.matcher(html).replaceAll("");
            }
            return html;
        }
        return null;
    }

    /**
     * Vrati true ak posledny riadok html kodu obsahuje zadany text
     * @param html
     * @param text
     * @return
     */
    private static boolean lastLineContains(String html, String text)
    {
        try
        {
            //najdi posledny riadok
            int i = html.lastIndexOf('\n');
            if (i>0 && html.substring(i).indexOf(text)!=-1) return true;
        } catch (Exception ex) {}
        return false;
    }

    public static String htmlToPlain(String html)
    {
        if (html == null) return "";
        html = removeCommands(html);
        html = Tools.replace(html, "&nbsp;", " ");
        int failsafe = 0;
        if (html != null)
        {
            //toto nefungovalo na nekorektny HTML kod (napr. useknuty pre fastSnippet)
            //return new Source(html).getTextExtractor().toString();

            StringTokenizer sTok = new StringTokenizer(html, "<>", true);
            String pom = "";
            StringBuilder plain = new StringBuilder();
            while (sTok.hasMoreElements() && failsafe++ < 10000)
            {
                try
                {
                    pom = sTok.nextToken();
                    if (pom.equals("<") && sTok.hasMoreElements())
                    {
                        String tagName = sTok.nextToken().toLowerCase();

                        if (tagName.startsWith("/p") || tagName.startsWith("/h") || tagName.startsWith("/ul") || tagName.startsWith("/div"))
                        {
                            //ten trim odstrani poslednu medzeru na konci riadku
                            plain.append("\n\n");
                        }
                        else if (tagName.startsWith("br") || tagName.startsWith("/tr") || tagName.startsWith("/li") || tagName.startsWith("ul") || tagName.startsWith("h") )
                        {
                            //ten trim odstrani poslednu medzeru na konci riadku
                            plain.append("\n");
                        }
                        else if (tagName.startsWith("li")) plain.append("* ");
                        else if (tagName.startsWith("span class='emailinput-") && plain.toString().trim().endsWith(":")==false && lastLineContains(plain.toString(), ":")==false)
                        {
                            //doplnime znak : za posledny text
                            plain.append(":");
                        }

                        if (sTok.hasMoreTokens())
                            sTok.nextToken();
                    }
                    else
                    {
						/*
						 * if (pom.equals(&nbsp;))
						 */
                        if (pom!=null && Tools.isNotEmpty(pom.trim()))
                        {
                            //ak to nekonci na \n pridaj medzeru
                            if (plain.length()>0 && plain.toString().endsWith("\n")==false) plain.append(" ");
                            plain.append(pom.trim());
                        }
                    }
                }
                catch (Exception ex)
                {
                    plain.append("-&nbsp;");
                    Logger.error(SearchTools.class,"CHYBA PARSOVANIA HTML: " + html);
                    sk.iway.iwcm.Logger.error(ex);
                }
            }

            plain = Tools.replace(plain, "\n\n\n", "\n\n");
            plain = Tools.replace(plain, "&nbsp;", " ");
            plain = Tools.replace(plain, "&amp;", "&");
            plain = Tools.replace(plain, "   \n", "\n");
            plain = Tools.replace(plain, "  \n", "\n");
            plain = Tools.replace(plain, " \n", "\n");

            String plainString = plain.toString().trim();
            plainString = plainString.trim();
            plainString = plainString.trim();

            return plainString;

        }
        return null;
    }

    public static String[] getCheckInputParams()
    {
        return checkInputParams;
    }
}
