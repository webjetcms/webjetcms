package sk.iway.iwcm.doc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.common.SearchTools;

public class SearchSnippet {

    private int prepend = 0;
    private int append = 0;

    private SearchDetails doc;
    private String textToFind;
    private HttpServletRequest request;
    private String textToFindAscLC;
    private String dataAsc;

    private List<String> tokens = new ArrayList<>();
    private List<String> tokensFound = new ArrayList<>();
    private List<String> snippets = new ArrayList<>();
    private String snippet;

    public SearchSnippet(SearchDetails doc, String textToFind, HttpServletRequest request) {
        this.doc = doc;
        this.textToFind = textToFind;
        this.request = request;

        this.prepend = Constants.getInt("searchSnippetPrepend");
        if (this.prepend == -1) {
            this.prepend = 100;
        }

        this.append = Constants.getInt("searchSnippetAppend");
        if (this.append == -1) {
            this.append = 100;
        }

        renderSnippet();
        renderHighlights();
    }

    public String getSnippet()
    {
        return snippet;
    }

    private void renderSnippet() {
        textToFindAscLC = DB.internationalToEnglish(textToFind).toLowerCase();

        //odstranme HTML kod a INCLUDE prikazy
        String dataOriginalNoHtml;

        if (SearchAction.shouldDoQuickSnippet(doc, request))
        {
            //tu ponechavame cely HTML kod, bolo by to narozne na odstranenie (velky HTML kod)
            dataOriginalNoHtml = doc.getDataOriginal();

            dataAsc = dataOriginalNoHtml;
            //pridame do ttfAscLC aj povodne slovicka (kedze ASC verziu nam to asi nenajde)
            textToFindAscLC += " " + textToFind;
        }
        else
        {
            //odstranme HTML kod a INCLUDE prikazy
            dataOriginalNoHtml = EditorToolsForCore.removeHtmlTagsKeepLength(doc.getDataOriginal());

            dataAsc = DB.internationalToEnglish(dataOriginalNoHtml).toLowerCase();
        }

        //kvoli efektivite scanovania si z povodneho HTML kodu spravime len kratke casti obsahujuce hladane slova
        //scanujeme nad ASC slovickami ale vytvarame to v diakritikovych hodnotach
        tokens = new ArrayList<>(Arrays.asList(Tools.getTokens(textToFindAscLC, " ")));
        tokensFound = new ArrayList<>();

        Pattern includeReplace = Pattern.compile("!INCLUDE.*?\\)!", Pattern.CASE_INSENSITIVE);
        Set<String> uzSomHladalSlovicka = new HashSet<>();

        for (String token : tokens) {
            if (tokensFound.contains(token) || uzSomHladalSlovicka.contains(token) || token.length() < 3) {
                continue;
            }

            uzSomHladalSlovicka.add(token);
            int i = dataOriginalNoHtml.indexOf(token);
            if (i==-1) i = dataAsc.indexOf(token);
            if (i != -1)
            {
                int start = i - prepend;
                int end = i + append;
                if (start < 0) start = 0;

                if (end > dataOriginalNoHtml.length()) end = dataOriginalNoHtml.length();

                String part = dataOriginalNoHtml.substring(start, end);
                part = clear(replaceINCLUDE(part, includeReplace));

                snippets.add(part);
                tokensFound = containsAny(part, tokens);
            }
        }

        if (snippets.isEmpty())
        {
            String data = (substring(dataOriginalNoHtml, prepend + append));
            //moze nastat situacia ze klucove slova su v title ani v dataOriginal nie su ale je tam !INCLUDE ktory by sa zobrazil vo vysledkoch vyhlavania.
            data = clear(replaceINCLUDE(data,includeReplace));
            snippets.add(data);
        }

        if (snippets.isEmpty())
        {
            String data = clear(substring(doc.getTitle(), prepend + append));
            snippets.add(data);
        }
    }

    private static String replaceINCLUDE(String dataParam, Pattern includeReplace)
    {
        String data = includeReplace.matcher(dataParam).replaceAll(" ");
        int includeIndex = data.indexOf("!INCLUDE");
        if (includeIndex!=-1)
        {
            //regexp nezbehol, zahod vsetko za !INCLUDE
            if (includeIndex<1) {
                data = "";
            }
            else {
                data = data.substring(0, includeIndex);
            }
        }
        return data;
    }

    private void renderHighlights() {
        String separator = "...";
        snippet = separator + Tools.join(snippets, separator + " " + separator) + separator;
        StringBuilder snippetData = new StringBuilder();

        //zvyrazni slova
        StringTokenizer wordTokenizer = new StringTokenizer(snippet);
        while (wordTokenizer.hasMoreTokens())
        {
            String word = wordTokenizer.nextToken();
            String wordAscLC = DB.internationalToEnglish(word).toLowerCase();

            boolean highlight = false;
            StringTokenizer st = new StringTokenizer(textToFindAscLC);
            while (st.hasMoreTokens())
            {
                if (wordAscLC.indexOf(st.nextToken())!=-1)
                {
                    highlight = true;
                    break;
                }
            }

            if (highlight) snippetData.append("<strong>");
            snippetData.append(word);
            if (highlight) snippetData.append("</strong>");
            snippetData.append(' ');
        }

        snippet = snippetData.toString().trim();
    }

    private String toAscLc(String text) {
        return DB.internationalToEnglish(text).toLowerCase();
    }

    private List<String> containsAny(String text, List<String> tokens) {
        String textAscLc = toAscLc(text);
        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            if (textAscLc.contains(token)) {
                result.add(token);
            }
        }

        return result;
    }

    private String substring(String text, int end) {
        String result = text;
        int endier = end;
        if (endier > result.length()) {
            endier = result.length();
        }

        return result.substring(0, endier);
    }

    private String clear(String text) {
        String result = text;

        int openTagIndex = text.indexOf("<");
        int closeTagIndex = text.indexOf(">");

        if (closeTagIndex > -1 && (openTagIndex == -1 || openTagIndex > closeTagIndex)) {
            result = result.substring(closeTagIndex + 1);
        }

        result = SearchTools.htmlToPlain(result);
        result = Tools.replace(result, "\n", " ");
        result = Tools.replace(result, "\r", " ");
        result = Tools.replace(result, "\t", " ");

        return result;
    }
}